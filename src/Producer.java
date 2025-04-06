import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.UUID;

public class Producer {

    private static final String MULTICAST_GROUP = "230.0.0.0";
    private static final int DISCOVERY_PORT = 4446;

    private String consumerIp;
    private int consumerPort;
    private int numProducers;

    public void startClientForTesting(int numberOfThreads) {
        discoverConsumer();

        if (consumerIp != null) {
            for (int i = 0; i < numberOfThreads; i++) {
                ProducerThread producerThread = new ProducerThread(consumerIp, consumerPort, i);
                producerThread.start();
            }
        } else {
            System.err.println("Consumer not discovered.");
        }
    }

    private void discoverConsumer() {
        try (MulticastSocket socket = new MulticastSocket(DISCOVERY_PORT)) {
            socket.setSoTimeout(10000);
            InetAddress group = InetAddress.getByName(MULTICAST_GROUP);
            socket.joinGroup(group);

            System.out.println("Waiting for consumer broadcast...");

            byte[] buffer = new byte[256];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);

            String message = new String(packet.getData(), 0, packet.getLength());
            System.out.println("Received multicast: " + message);

            consumerIp = packet.getAddress().getHostAddress();
            String[] parts = message.split(" ");
            consumerPort = Integer.parseInt(parts[parts.length - 1]);

            socket.leaveGroup(group);

        } catch (SocketTimeoutException e) {
            System.out.println("Timed out waiting for consumer.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

