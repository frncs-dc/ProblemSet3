import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;

public class Producer {

    private static final String MULTICAST_GROUP = "230.0.0.0";
    private static final int DISCOVERY_PORT = 4446;

    private String consumerIp;
    private int consumerPort;

    public void startClientForTesting() {
        discoverConsumer();
        if (consumerIp != null) {
            connectToConsumer();
        } else {
            System.err.println("Consumer not discovered.");
        }
    }

    private void discoverConsumer() {
        try (MulticastSocket socket = new MulticastSocket(DISCOVERY_PORT)) {
            socket.setSoTimeout(10000); // Wait max 10s for discovery
            InetAddress group = InetAddress.getByName(MULTICAST_GROUP);
            socket.joinGroup(group);

            System.out.println("Waiting for consumer broadcast...");

            byte[] buffer = new byte[256];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);

            String message = new String(packet.getData(), 0, packet.getLength());
            System.out.println("Received multicast: " + message);

            consumerIp = packet.getAddress().getHostAddress();

            // Extract port from message (assumes format: "Consumer is listening on port XXXX")
            String[] parts = message.split(" ");
            consumerPort = Integer.parseInt(parts[parts.length - 1]);

            socket.leaveGroup(group);

        } catch (SocketTimeoutException e) {
            System.out.println("Timed out waiting for consumer.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void connectToConsumer() {
        try (Socket socket = new Socket(consumerIp, consumerPort)) {
            System.out.println("Connected to Consumer at " + consumerIp + ":" + consumerPort);

            // Optional: send sample message
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println("Hello from Producer!");
            String response = in.readLine();
            System.out.println("Consumer replied: " + response);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
