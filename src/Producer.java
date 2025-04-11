import java.io.IOException;
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
