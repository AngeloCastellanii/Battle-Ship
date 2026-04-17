import java.io.*;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NetworkManager {
    private BattleshipClient client;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public NetworkManager(BattleshipClient client) {
        this.client = client;
    }

    public void connectToServer(String host, int port, String name) {
        try {
            socket = new Socket(host, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            out.println("CONNECT " + name);

            executor.submit(this::listenForMessages);

            client.switchToCombat();

        } catch (IOException e) {
            client.showConnectionError(e.getMessage());
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
                    client.showPlacement();
                }
            }
            case "TURN" -> {
                if (parts.length >= 2) {
                    int playerId = Integer.parseInt(parts[1]);
                    client.setTurn(playerId == client.getCurrentPlayerId());
                }
            }
            case "RESULT" -> {
                if (parts.length >= 4) {
                    int x = Integer.parseInt(parts[1]);
                    int y = Integer.parseInt(parts[2]);
                    String result = parts[3];
                    client.updateEnemyBoard(x, y, result);
                }
            }
            case "VICTORY" -> {
                if (parts.length >= 2) {
                    int winnerId = Integer.parseInt(parts[1]);
                    String msg = (winnerId == client.getCurrentPlayerId()) ? "You win!" : "You lose!";
                    client.showVictory(msg);
                }
            }
            case "CONNECTED" -> {
                if (parts.length >= 2) {
                    client.setCurrentPlayerId(Integer.parseInt(parts[1]));
                }
            }
            case "PLACE_OK" -> {
                if (parts.length >= 2 && parts[1].equals(client.getCurrentShip())) {
                    client.addPlacedShip(client.getCurrentShip());
                    System.out.println("Barco " + client.getCurrentShip() + " colocado exitosamente.");
                    client.setWaitingForPlace(false);
                }
            }
            case "ERROR" -> {
                String errorMsg = message.substring(6);
                client.showError(errorMsg);
                if (client.isWaitingForPlace()) {
                    client.setWaitingForPlace(false);
                }
            }
        }
    }

    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    public void close() {
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            // Ignore
        }
    }
}