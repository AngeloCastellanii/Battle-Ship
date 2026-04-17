import java.io.BufferedWriter;
import java.io.IOException;
import java.net.Socket;

// Encapsula el protocolo de texto y delega cada comando al modulo de sesion.
// Aqui se valida formato basico, no reglas completas de juego.
final class CommandProcessor {
    // Referencia central a la sesion activa del juego.
    private final GameSession session;

    CommandProcessor(GameSession session) {
        this.session = session;
    }

    // Punto unico de entrada para cualquier mensaje de cliente.
    void processCommand(String message, int clientId, Socket socket, BufferedWriter out) throws IOException {
        String[] parts = message.split("\\s+");
        String command = parts[0].toUpperCase();

        switch (command) {
            case "CONNECT" -> processConnect(parts, message, clientId, socket, out);
            case "PLACE" -> processPlace(parts, clientId, out);
            case "ATTACK" -> processAttack(parts, clientId, out);
            default -> send(out, "ERROR Comando no soportado: " + command);
        }
    }

    // CONNECT permite nombre con espacios, por eso uso substring del mensaje original.
    private void processConnect(String[] parts, String message, int clientId, Socket socket, BufferedWriter out) throws IOException {
        if (parts.length < 2) {
            send(out, ProtocolConfig.error("BAD_CONNECT", "CONNECT requiere nombre"));
            return;
        }

        String name = message.substring("CONNECT".length()).trim();
        session.connect(clientId, socket, out, name);
    }

    // PLACE exige exactamente 4 argumentos ademas del comando.
    private void processPlace(String[] parts, int clientId, BufferedWriter out) throws IOException {
        if (parts.length != 5) {
            send(out, ProtocolConfig.error("BAD_PLACE", "Formato esperado: PLACE <Barco> <x> <y> <H/V>"));
            return;
        }

        session.place(clientId, out, parts[1], parts[2], parts[3], parts[4]);
    }

    // ATTACK recibe coordenada destino dentro del tablero.
    private void processAttack(String[] parts, int clientId, BufferedWriter out) throws IOException {
        if (parts.length != 3) {
            send(out, ProtocolConfig.error("BAD_ATTACK", "Formato esperado: ATTACK <x> <y>"));
            return;
        }

        session.attack(clientId, out, parts[1], parts[2]);
    }

    // Escribe una respuesta estandar al cliente y deja traza en logs del servidor.
    private void send(BufferedWriter out, String response) throws IOException {
        out.write(response);
        out.newLine();
        out.flush();
        ServerLogger.info("Servidor -> " + response);
    }
}
