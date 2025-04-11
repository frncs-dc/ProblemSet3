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
            System.out.print("Enter input (Consumer/Producer): ");
            userInput = scanner.nextLine().trim();

            // Validate the input
            if (userInput.isEmpty()) {
                System.out.println("Input cannot be empty. Please enter a valid string.");
                continue;
            }

            if (userInput.equalsIgnoreCase(consumerSTRING)) {
                int consumerNumThreads = -1;
                int maxQueueLength = -1;

                // Validate number of consumer threads
                while (consumerNumThreads <= 0) {
                    System.out.print("How many consumer threads? ");
                    String input = scanner.nextLine();
                    try {
                        consumerNumThreads = Integer.parseInt(input);
                        if (consumerNumThreads <= 0) {
                            System.out.println("Please enter a positive integer.");
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid number. Please enter a valid integer.");
                    }
                }

                // Validate max queue length
                while (maxQueueLength <= 0) {
                    System.out.print("What is the max queue length? ");
                    String input = scanner.nextLine();
                    try {
                        maxQueueLength = Integer.parseInt(input);
                        if (maxQueueLength <= 0) {
                            System.out.println("Please enter a positive integer.");
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid number. Please enter a valid integer.");
                    }
                }

                Consumer consumer = new Consumer(consumerNumThreads, maxQueueLength);
                consumer.start();
                ConsumerGUI.launch(ConsumerGUI.class);
                break;

            } else if (userInput.equalsIgnoreCase(producerSTRING)) {
                int producerNumThreads = -1;

                // Validate number of producer threads
                while (producerNumThreads <= 0) {
                    System.out.print("How many producer threads? ");
                    String input = scanner.nextLine();
                    try {
                        producerNumThreads = Integer.parseInt(input);
                        if (producerNumThreads <= 0) {
                            System.out.println("Please enter a positive integer.");
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid number. Please enter a valid integer.");
                    }
                }

                Producer producer = new Producer();
                producer.startClientForTesting(producerNumThreads);
                break;

            } else {
                System.out.println("The input is not valid. Please enter either 'Consumer' or 'Producer'.");
            }
        }

    }
}
