import java.io.*;
import java.net.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class Consumer {
    private static final String MULTICAST_GROUP = "230.0.0.0";
    private static final int DISCOVERY_PORT = 4446;
    private static final int TCP_PORT = 5000;
    private static final String CONSUMER_DIRECTORY = "consumer_directory";

    private final ExecutorService connectionPool = Executors.newCachedThreadPool();
    private final BlockingQueue<VideoFile> videoQueue;
    private final ExecutorService videoProcessingPool;
    private List<ConsumerThread> consumerThreads;

    public Consumer(int numConsumerThreads, int maxQueueLength) {
        File saveDir = new File(CONSUMER_DIRECTORY);
        if (!saveDir.exists()) {
            saveDir.mkdir();
        }

        // Leaky bucket: if queue is full, videos will be dropped
        videoQueue = new LinkedBlockingQueue<>(maxQueueLength);
        videoProcessingPool = Executors.newFixedThreadPool(numConsumerThreads);
    }

    public void start() {
        System.setProperty("java.net.preferIPv4Stack", "true");
        new Thread(this::broadcastContinuously).start();
        new Thread(this::startServer).start();

        // Create consumer threads and keep references for logging
        consumerThreads = new ArrayList<>();
        for (int i = 0; i < ((ThreadPoolExecutor) videoProcessingPool).getCorePoolSize(); i++) {
            ConsumerThread thread = new ConsumerThread(i, videoQueue, CONSUMER_DIRECTORY);
            consumerThreads.add(thread);
            videoProcessingPool.submit(thread);
        }

        startQueueLogger();
    }

    private void broadcastContinuously() {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setBroadcast(true);
            InetAddress broadcastAddress = InetAddress.getByName("255.255.255.255");
            while (true) {
                String message = "Consumer is listening on port " + TCP_PORT;
                byte[] buffer = message.getBytes();
                DatagramPacket packet = new DatagramPacket(
                  buffer, buffer.length, broadcastAddress, DISCOVERY_PORT
                );
                socket.send(packet);
                System.out.println("Broadcasted: " + message);
                Thread.sleep(2000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
    private void broadcastContinuously() {
        try {
            InetAddress group = InetAddress.getByName(MULTICAST_GROUP);
            try (MulticastSocket socket = new MulticastSocket()) {
                socket.setTimeToLive(1);
                while (true) {
                    String message = "Consumer is listening on port " + TCP_PORT;
                    DatagramPacket packet = new DatagramPacket(message.getBytes(), message.length(), group, DISCOVERY_PORT);
                    socket.send(packet);
                    System.out.println("Broadcasting: " + message);
                    Thread.sleep(2000);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
     */

    private void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(TCP_PORT)) {
            System.out.println("Consumer listening for producers on port " + TCP_PORT);
            while (true) {
                Socket producerSocket = serverSocket.accept();
                connectionPool.submit(() -> handleProducer(producerSocket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleProducer(Socket socket) {
        try (DataInputStream in = new DataInputStream(socket.getInputStream());
        DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {

            out.writeUTF("CONSUMER_OK");
            out.flush();

            while (true) {
                String fileName = in.readUTF();
                int length = in.readInt();
                byte[] data = new byte[length];
                in.readFully(data);

                VideoFile videoFile = new VideoFile(fileName, data);
                boolean offered = videoQueue.offer(videoFile);

                if (offered) {
                    System.out.println("Queued file: " + fileName);
                } else {
                    System.out.println("Dropped file (queue full): " + fileName);
                }
            }
        } catch (IOException e) {
            System.out.println("Producer disconnected.");
        }
    }

    private void startQueueLogger() {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(5000); // every 5 seconds
                    System.out.println("\n--- [Queue Status] ---");
                    System.out.println("Current files in queue: " + videoQueue.size());

                    for (ConsumerThread thread : consumerThreads) {
                        System.out.println("Thread-" + thread.getId() + " is processing: " + thread.getCurrentFileName());
                    }

                    System.out.println("----------------------\n");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }).start();
    }


    // Inner class for video data
    public static class VideoFile {
        public final String fileName;
        public final byte[] content;

        public VideoFile(String fileName, byte[] content) {
            this.fileName = fileName;
            this.content = content;
        }
    }

}
