import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;

public class ProducerThread extends Thread {
    private final String consumerIp;
    private final int consumerPort;
    private final int id;
    private final BlockingQueue<Path> sharedQueue;

    public ProducerThread(String consumerIp, int consumerPort, int id, BlockingQueue<Path> queue) {
        this.consumerIp = consumerIp;
        this.consumerPort = consumerPort;
        this.id = id;
        this.sharedQueue = queue;
    }

    @Override
    public void run() {
        while (true) {
            try (Socket socket = new Socket(consumerIp, consumerPort)) {
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                System.out.println("ProducerThread-" + id + " connected to Consumer.");

                while (true) {
                    Path file = sharedQueue.take(); // Blocks until a file is available

                    try {
                        String fileName = file.getFileName().toString();
                        byte[] content = Files.readAllBytes(file);

                        out.writeUTF(fileName);
                        out.writeInt(content.length);
                        out.write(content);
                        out.flush();

                        System.out.println("ProducerThread-" + id + " sent: " + fileName);
                        Files.delete(file); // Clean up after sending
                    } catch (IOException e) {
                        System.err.println("Error sending file: " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                System.out.println("ProducerThread-" + id + " lost connection. Retrying...");
            }
        }
    }
}
