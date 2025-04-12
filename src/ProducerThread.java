import java.io.DataInputStream;
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
                DataInputStream in = new DataInputStream(socket.getInputStream());

                String response = in.readUTF();
                if (!"CONSUMER_OK".equals(response)) {
                    System.out.println("Unexpected consumer response: " + response);
                    continue; // retry
                }

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
                        Files.delete(file);
                    } catch (IOException e) {
                        System.err.println("Error sending file: " + e.getMessage());
                        break; // Break inner loop and reconnect
                    }
                }
            } catch (Exception e) {
                System.out.println("ProducerThread-" + id + " lost connection. Retrying...");
                try {
                    Thread.sleep(2000); // Prevent spamming reconnections
                } catch (InterruptedException ignored) {}
            }
        }
    }
}
