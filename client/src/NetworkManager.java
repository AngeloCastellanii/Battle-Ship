import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Capa de red del cliente.
// Maneja conexion TCP, escucha de mensajes y traduccion de protocolo a acciones de UI.
public class NetworkManager {
    // Timeout de conexion para evitar bloqueos largos cuando la red falla.
    private static final int CONNECT_TIMEOUT_MS = 15000;

    // Referencia al cliente principal para invocar callbacks de estado.
    private BattleshipClient client;
    // Recursos de comunicacion TCP.
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    // Listener dedicado en hilo virtual para no bloquear UI.
    private ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    // Marca si el handshake CONNECTED ya fue exitoso.
    private volatile boolean connected = false;

    public NetworkManager(BattleshipClient client) {
        this.client = client;
    }

    // Abre conexion con servidor y envia comando CONNECT del protocolo.
    public void connectToServer(String host, int port, String name) {
        try {
            // Siempre parto de un estado limpio por si venia de intento anterior.
            close();

            // Intento conexion por todas las direcciones resueltas del host.
            socket = connectWithFallback(host, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            connected = false;

            out.println("CONNECT " + name);
            client.setStatus("Connection request sent. Waiting for server...");

            executor.submit(this::listenForMessages);

        } catch (SocketTimeoutException e) {
            close();
            client.showConnectionError("Connection timeout to " + host + ":" + port + ". Verify IP and that server is running.");
        } catch (IOException e) {
            close();
            client.showConnectionError(e.getMessage());
        }
    }

    // Prueba conectarse por cada IP asociada al host (IPv4/IPv6), devolviendo la primera valida.
    private Socket connectWithFallback(String host, int port) throws IOException {
        InetAddress[] addresses = InetAddress.getAllByName(host);
        List<String> errors = new ArrayList<>();

        for (InetAddress address : addresses) {
            Socket candidate = new Socket();
            try {
                candidate.connect(new InetSocketAddress(address, port), CONNECT_TIMEOUT_MS);
                return candidate;
            } catch (IOException e) {
                errors.add(address.getHostAddress() + " -> " + e.getMessage());
                try {
                    candidate.close();
                } catch (IOException ignored) {
                }
            }
        }

        if (errors.isEmpty()) {
            throw new IOException("Could not resolve host: " + host);
        }

        // Si ninguna IP funciono, retorno un detalle acumulado para diagnostico.
        throw new IOException("Could not connect to " + host + ":" + port + " (" + String.join(" | ", errors) + ")");
    }

    // Bucle de escucha continuo del canal de entrada.
    private void listenForMessages() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("Received: " + message);
                processMessage(message);
            }

            // Si el stream termina, diferencio si fue durante login o en partida activa.
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

    // Traduce mensajes del protocolo a acciones concretas del cliente.
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
                    // Handshake exitoso: ahora si entro a pantalla principal de juego.
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

                // Si aun no conecto formalmente, trato este error como fallo de login.
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

    // Envio de comandos al servidor (CONNECT ya se envia en connectToServer).
    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    // Cierra recursos de red y deja el manager en estado reutilizable.
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