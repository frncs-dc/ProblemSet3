import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;

public class Consumer {
    private static final String MULTICAST_GROUP = "230.0.0.0";
    private static final int DISCOVERY_PORT = 4446;
    private static final int TCP_PORT = 5000;

    private volatile boolean connected = false;

    // Call this from your app logic to start everything
    public void startServerForTesting() {
        // Start broadcasting in a separate thread
        new Thread(this::broadcastUntilConnected).start();

        // Start TCP server and wait for producer
        waitForProducerConnection();
    }

    private void broadcastUntilConnected() {
        try {
            InetAddress group = InetAddress.getByName(MULTICAST_GROUP);
            try (MulticastSocket socket = new MulticastSocket()) {
                socket.setTimeToLive(1); // Stay within local network

                while (!connected) {
                    String message = "Consumer is listening on port " + TCP_PORT;
                    DatagramPacket packet = new DatagramPacket(
                            message.getBytes(),
                            message.length(),
                            group,
                            DISCOVERY_PORT
                    );
                    socket.send(packet);
                    System.out.println("Broadcasting: " + message);

                    Thread.sleep(2000); // Sleep before next broadcast
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void waitForProducerConnection() {
        try (ServerSocket serverSocket = new ServerSocket(TCP_PORT)) {
            System.out.println("TCP server is waiting for producer...");

            Socket socket = serverSocket.accept();
            connected = true;

            System.out.println("Producer connected: " + socket.getInetAddress());

            // Optional: handle communication
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            String msg;
            while ((msg = in.readLine()) != null) {
                System.out.println("Received from producer: " + msg);
                out.println("ACK: " + msg);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
