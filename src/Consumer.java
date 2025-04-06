// Consumer.java
import java.io.*;
import java.net.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.*;

public class Consumer {
    private static final String MULTICAST_GROUP = "230.0.0.0";
    private static final int DISCOVERY_PORT = 4446;
    private static final int TCP_PORT = 5000;
    private static final String CONSUMER_DIRECTORY = "consumer_directory"; // Directory to save received videos
    private ExecutorService connectionPool = Executors.newCachedThreadPool();
    private BlockingQueue<VideoFile> videoQueue = new LinkedBlockingQueue<>();
    private ExecutorService videoProcessingPool;
    public Consumer() {
        File saveDir = new File(CONSUMER_DIRECTORY);
        if (!saveDir.exists()) {
            saveDir.mkdir();
        }

        // Initialize a pool of threads to process video files
        videoProcessingPool = Executors.newFixedThreadPool(2); // Adjust the number of threads as necessary
    }

    public void start() {
        new Thread(this::broadcastContinuously).start();
        new Thread(this::startServer).start();
        new Thread(this::processVideoQueue).start();
    }

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
        try (DataInputStream in = new DataInputStream(socket.getInputStream())) {
            while (true) {
                String fileName = in.readUTF();
                int length = in.readInt();
                byte[] data = new byte[length];
                in.readFully(data);

                videoQueue.offer(new VideoFile(fileName, data));
                System.out.println("Queued file: " + fileName);
            }
        } catch (IOException e) {
            System.out.println("Producer disconnected.");
        }
    }


    private void processVideoQueue() {
        while (true) {
            try {
                VideoFile video = videoQueue.take();
                System.out.println("Processing video: " + video.fileName);
                saveFile(video);
                System.out.println("Finished processing: " + video.fileName);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    // Inner class to hold video file info
    private static class VideoFile {
        String fileName;
        byte[] content;

        VideoFile(String fileName, byte[] content) {
            this.fileName = fileName;
            this.content = content;
        }
    }

    private void saveFile(VideoFile videoFile) {
        Path filePath = Paths.get(CONSUMER_DIRECTORY, videoFile.fileName);

        try (FileOutputStream fos = new FileOutputStream(String.valueOf(filePath))) {
            fos.write(videoFile.content);  // Write content to file
            System.out.println("Saved video file to consumer directory: " + videoFile.fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
