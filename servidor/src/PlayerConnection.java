import java.io.BufferedWriter;
import java.io.IOException;
import java.net.Socket;

// Representa la conexion de red de un cliente y su asociacion con un jugador.
final class PlayerConnection {
    // Identificador interno del socket (no es el id de jugador).
    private final int clientId;
    // Socket TCP activo de este cliente.
    private final Socket socket;
    // Canal de salida para respuestas del protocolo.
    private final BufferedWriter out;
    // Se asigna en CONNECT cuando el cliente ocupa jugador 1 o 2.
    private Integer playerId;
    // Nombre de jugador informado por el cliente.
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

    // Envio sincronizado para evitar mezcla de mensajes si hay concurrencia.
    synchronized void send(String response) throws IOException {
        out.write(response);
        out.newLine();
        out.flush();
        ServerLogger.info("Servidor -> cliente #" + clientId + ": " + response);
    }

    // Cierre silencioso de socket para operaciones de limpieza.
    void closeQuietly() {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }
}
