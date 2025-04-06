import java.io.*;
import java.net.Socket;
import java.nio.file.*;
import java.util.UUID;

public class ProducerThread extends Thread {
    private final String consumerIp;
    private final int consumerPort;
    private final int id;
    private final Path threadDirectory;

    public ProducerThread(String consumerIp, int consumerPort, int id) {
        this.consumerIp = consumerIp;
        this.consumerPort = consumerPort;
        this.id = id;
        this.threadDirectory = createThreadDirectory();
    }

    private Path createThreadDirectory() {
        String dirName = "ProducerThread_" + UUID.randomUUID();
        Path path = Paths.get(dirName);
        try {
            Files.createDirectories(path);
            System.out.println("Created directory for thread-" + id + ": " + dirName);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return path;
    }

    @Override
    public void run() {
        while (true) {
            try (Socket socket = new Socket(consumerIp, consumerPort)) {
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                System.out.println("ProducerThread-" + id + " connected to Consumer.");

                while (true) {
                    Files.list(threadDirectory)
                            .filter(file -> file.toString().endsWith(".mp4"))
                            .forEach(file -> {
                                try {
                                    String fileName = file.getFileName().toString();
                                    byte[] content = Files.readAllBytes(file);

                                    // Send metadata
                                    out.writeUTF(fileName);
                                    out.writeInt(content.length);
                                    out.write(content);
                                    out.flush();

                                    System.out.println("Sent video file: " + fileName);
                                    Files.delete(file);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            });

                    Thread.sleep(5000);
                }
            } catch (IOException | InterruptedException e) {
                System.out.println("ProducerThread-" + id + " lost connection. Retrying...");
            }
        }
    }

    public Path getThreadDirectory() {
        return threadDirectory;
    }
}
