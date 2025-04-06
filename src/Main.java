import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        // Create a Scanner object for user input
        Scanner scanner = new Scanner(System.in);

        // The string to compare with
        String consumerSTRING = "CONSUMER";
        String producerSTRING = "PRODUCER";



        String userInput = "";

        // Keep asking for input until it's correct
        while (true) {
            // Prompt the user for input
            System.out.print("Who are you? ");
            userInput = scanner.nextLine();

            // Validate the input
            if (userInput.isEmpty()) {
                System.out.println("Input cannot be empty. Please enter a valid string.");
            } else {
                // Check if the input matches the target string
                if (userInput.toUpperCase().equals(consumerSTRING)) {
                    Consumer consumer = new Consumer();
                    //consumer.startServerForTesting();
                    new Thread(() -> consumer.startServerForTesting()).start();
                    ConsumerGUI.launch(ConsumerGUI.class);
                    break;  // Exit the loop when the input is correct
                } else if (userInput.toUpperCase().equals(producerSTRING)) {
                    Producer producer = new Producer();
                    producer.startClientForTesting(3);
                    break;  // Exit the loop when the input is correct
                } else {
                    System.out.println("The input is not equal to the target string. Please try again.");
                }
            }
        }

        // Close the scanner
        scanner.close();
    }

}