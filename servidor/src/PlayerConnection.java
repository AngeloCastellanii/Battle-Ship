import java.io.BufferedWriter;
import java.io.IOException;
import java.net.Socket;

final class PlayerConnection {
    private final int clientId;
    private final Socket socket;
    private final BufferedWriter out;
    private Integer playerId;
    private String name;

    PlayerConnection(int clientId, Socket socket, BufferedWriter out) {
        this.clientId = clientId;
        this.socket = socket;
        this.out = out;
    }

    int clientId() {
        return clientId;
    }

    Integer playerId() {
        return playerId;
    }

    String name() {
        return name;
    }

    void assignPlayer(int playerId, String name) {
        this.playerId = playerId;
        this.name = name;
    }

    synchronized void send(String response) throws IOException {
        out.write(response);
        out.newLine();
        out.flush();
        ServerLogger.info("Servidor -> cliente #" + clientId + ": " + response);
    }

    void closeQuietly() {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }
}
