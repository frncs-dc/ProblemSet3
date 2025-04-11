import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.nio.file.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Stream;

public class Producer {
    private static final String MULTICAST_GROUP = "230.0.0.0";
    private static final int DISCOVERY_PORT = 4446;
    private static final int TCP_PORT = 5000;
    private static final String PRODUCER_DIRECTORY = "producer_directory";

    private String consumerIp;
    private int consumerPort;
    private final BlockingQueue<Path> videoQueue = new LinkedBlockingQueue<>();

    public void startClientForTesting(int numberOfThreads) {
        discoverConsumer();

        if (consumerIp != null) {
            System.out.println("Consumer found at: " + consumerIp + ":" + consumerPort);

            try {
                preloadFiles(Paths.get(PRODUCER_DIRECTORY), videoQueue);
                watchDirectory(Paths.get(PRODUCER_DIRECTORY));  // Start watching for new files
            } catch (IOException e) {
                System.err.println("Error preloading files: " + e.getMessage());
                return;
            }

            for (int i = 0; i < numberOfThreads; i++) {
                new ProducerThread(consumerIp, consumerPort, i, videoQueue).start();
            }
        } else {
            System.err.println("Consumer not discovered.");
        }
    }

    private void discoverConsumer() {
        try (DatagramSocket socket = new DatagramSocket(DISCOVERY_PORT)) {
            socket.setBroadcast(true);
            byte[] buffer = new byte[256];
            DatagramPacket packet = new DatagramPacket(buffer,buffer.length);

            System.out.println("Listening for broadcast...");
            socket.receive(packet);

            String message = new String(packet.getData(), 0, packet.getLength());
            consumerIp = packet.getAddress().getHostAddress();
            consumerPort = TCP_PORT;

            System.out.println("Found Consumer: " + consumerIp + ":" + consumerPort);
        } catch (IOException e) {
            System.out.println("Fallback: Using hardcoded IP (192.168.56.102)");
            consumerIp = "192.168.56.102";
            consumerPort = TCP_PORT;
        }
    }

    private void watchDirectory(Path dir) throws IOException {
        WatchService watchService = FileSystems.getDefault().newWatchService();
        dir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);

        Thread watcherThread = new Thread(() -> {
            System.out.println("Watching for new files in: " + dir);
            while (true) {
                try {
                    WatchKey key = watchService.take(); // blocking
                    for (WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent.Kind<?> kind = event.kind();
                        if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                            Path createdPath = dir.resolve((Path) event.context());
                            if (Files.isRegularFile(createdPath)) {
                                System.out.println("New video file detected: " + createdPath.getFileName());
                                if (isVideoFile(createdPath)) {
                                    waitForFileToBeReady(createdPath);
                                    videoQueue.offer(createdPath);
                                    System.out.println("New video file detected and queued: " + createdPath.getFileName());
                                }
                            }
                        }
                    }
                    key.reset();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        watcherThread.setDaemon(true); // Optional: ends with main thread
        watcherThread.start();
    }

    private void waitForFileToBeReady(Path path) {
        long previousSize = -1;
        int stableCount = 0;

        while (true) {
            try {
                long currentSize = Files.size(path);

                if (currentSize == previousSize) {
                    stableCount++;
                } else {
                    stableCount = 0;
                    previousSize = currentSize;
                }

                // After being stable for 3 checks, try to read entire file
                if (stableCount >= 3) {
                    try (InputStream in = Files.newInputStream(path)) {
                        byte[] buffer = new byte[8192];
                        while (in.read(buffer) != -1) {
                            // Read all content to ensure file is fully unlocked
                        }
                    }
                    break; // Successfully read whole file = it's ready
                }

                Thread.sleep(1000); // 1s delay between checks (more stable for large files)
            } catch (IOException | InterruptedException e) {
                // Reset and retry
                stableCount = 0;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {}
            }
        }
    }




    private void preloadFiles(Path dir, BlockingQueue<Path> queue) throws IOException {
        try (Stream<Path> files = Files.list(dir)) {
            files.filter(path -> Files.isRegularFile(path) && isVideoFile(path))
                    .forEach(queue::offer);
        }
    }

    private boolean isVideoFile(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".mp4") || name.endsWith(".avi") || name.endsWith(".mov");
    }
}
