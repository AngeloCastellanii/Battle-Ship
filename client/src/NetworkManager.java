import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NetworkManager {
    private static final int CONNECT_TIMEOUT_MS = 5000;

    private BattleshipClient client;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private volatile boolean connected = false;

    public NetworkManager(BattleshipClient client) {
        this.client = client;
    }

    public void connectToServer(String host, int port, String name) {
        try {
            close();

            socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            connected = false;

            out.println("CONNECT " + name);
            client.setStatus("Connection request sent. Waiting for server...");

            executor.submit(this::listenForMessages);

        } catch (IOException e) {
            close();
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

            if (!connected) {
                client.showConnectionError("Server closed the connection during login.");
            } else {
                client.showConnectionLost("Connection closed by server.");
            }
        } catch (IOException e) {
            System.out.println("Connection lost: " + e.getMessage());
            if (!connected) {
                client.showConnectionError("Could not complete login: " + e.getMessage());
            } else {
                client.showConnectionLost("Connection lost. Please reconnect.");
            }
        } finally {
            close();
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
                    client.setStatus("Both players connected. Place your ships.");
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
                    connected = true;
                    client.switchToCombat();
                    client.setStatus("Connected as player " + parts[1] + ". Waiting for opponent...");
                }
            }
            case "WAITING_FOR_OPPONENT" -> client.setStatus("Waiting for second player...");
            case "PLACE_OK" -> {
                if (parts.length >= 2 && parts[1].equals(client.getCurrentShip())) {
                    client.confirmCurrentPlacement(parts[1]);
                    System.out.println("Barco " + parts[1] + " colocado exitosamente.");
                    client.setWaitingForPlace(false);
                }
            }
            case "READY" -> {
                client.setStatus("Fleet deployed. Waiting for opponent...");
            }
            case "ERROR" -> {
                String errorMsg = message.substring(6);
                client.rejectCurrentPlacement();

                if (!connected) {
                    client.showConnectionError(errorMsg);
                    close();
                    return;
                }

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
        connected = false;
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            // Ignore
        } finally {
            socket = null;
            in = null;
            out = null;
        }
    }
}