import java.io.*;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BattleshipClient {

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        new BattleshipClient().run();
    }

    public void run() {
        System.out.println("=== Battleship Client - Connection ===");

        // Connection form simulation
        System.out.print("Host (default: localhost): ");
        String host = scanner.nextLine().trim();
        if (host.isEmpty()) host = "localhost";

        System.out.print("Port (default: 5000): ");
        String portText = scanner.nextLine().trim();
        if (portText.isEmpty()) portText = "5000";

        System.out.print("Player Name: ");
        String name = scanner.nextLine().trim();

        if (name.isEmpty()) {
            System.out.println("Name cannot be empty");
            return;
        }

        try {
            int port = Integer.parseInt(portText);
            connectToServer(host, port, name);
        } catch (NumberFormatException e) {
            System.out.println("Invalid port number");
        }
    }

    private void connectToServer(String host, int port, String name) {
        try {
            System.out.println("Connecting to " + host + ":" + port + "...");
            socket = new Socket(host, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            System.out.println("Connected to server successfully!");

            // Send CONNECT message
            out.println("CONNECT " + name);
            System.out.println("Sent: CONNECT " + name);

            // Start listening for messages
            executor.submit(this::listenForMessages);

            // Keep the main thread alive
            System.out.println("Waiting for server response... (Press Ctrl+C to exit)");
            Thread.currentThread().join();

        } catch (IOException e) {
            System.out.println("Connection failed: " + e.getMessage());
        } catch (InterruptedException e) {
            // Exit gracefully
        }
    }

    private void listenForMessages() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("Received: " + message);
                processMessage(message);
            }
        } catch (IOException e) {
            System.out.println("Connection lost: " + e.getMessage());
        }
    }

    private void processMessage(String message) {
        String[] parts = message.split(" ");
        if (parts.length == 0) return;

        String command = parts[0];
        switch (command) {
            case "START" -> {
                if (parts.length >= 2) {
                    String firstPlayer = parts[1];
                    System.out.println("Game started! First player: " + firstPlayer);
                    // TODO: Transition to ship placement phase
                }
            }
            case "ERROR" -> {
                String errorMsg = message.substring(6); // Remove "ERROR "
                System.out.println("Server error: " + errorMsg);
            }
            // Add other message handlers as needed
        }
    }
}
