import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

// Punto de entrada del servidor multijugador.
// Esta clase se encarga de abrir el puerto, aceptar clientes y derivar cada conexion
// a un hilo virtual para que el procesamiento sea concurrente.
public class ServerMain {
    // Contador incremental para identificar facilmente cada socket conectado en logs.
    private static final AtomicInteger CLIENT_COUNTER = new AtomicInteger(0);

    public static void main(String[] args) {
        // Determino el puerto desde argumentos o uso valor por defecto del protocolo.
        int port = parsePort(args);
        // Session mantiene todo el estado de partida y reglas del juego.
        GameSession session = new GameSession();
        // Processor interpreta comandos de texto recibidos por socket.
        CommandProcessor commandProcessor = new CommandProcessor(session);

        ServerLogger.info("Iniciando servidor en el puerto " + port + "...");

        try (ServerSocket serverSocket = new ServerSocket(port);
             ExecutorService clientExecutor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {

            // Al cerrar la JVM, intento cerrar ordenadamente sockets y estado.
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                ServerLogger.info("Ejecutando cierre ordenado del servidor...");
                session.shutdown();
            }));

            ServerLogger.info("Servidor listo. Esperando conexiones...");

            while (true) {
                // Espera bloqueante: cada accept devuelve un nuevo cliente.
                Socket clientSocket = serverSocket.accept();
                int clientId = CLIENT_COUNTER.incrementAndGet();
                // Cada cliente se atiende en paralelo para no bloquear nuevas conexiones.
                clientExecutor.submit(() -> handleClient(clientSocket, clientId, commandProcessor, session));
            }
        } catch (IOException e) {
            ServerLogger.error("Error fatal en el servidor: " + e.getMessage());
        }
    }

    // Bucle de lectura por cliente: recibe comandos y los envia al procesador.
    private static void handleClient(Socket socket, int clientId, CommandProcessor commandProcessor, GameSession session) {
        String remote = socket.getRemoteSocketAddress().toString();
        ServerLogger.info("Cliente #" + clientId + " conectado desde " + remote);

        try (socket;
             BufferedReader in = new BufferedReader(
                 new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter out = new BufferedWriter(
                 new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {

            // Si un cliente queda inactivo, libero recursos y estado de partida.
            socket.setSoTimeout(ProtocolConfig.CLIENT_READ_TIMEOUT_MS);

            String line;
            while ((line = in.readLine()) != null) {
                String message = line.trim();
                if (message.isEmpty()) {
                    continue;
                }

                ServerLogger.info("Cliente #" + clientId + " -> " + message);
                // Delego la logica de protocolo para mantener ServerMain liviano.
                commandProcessor.processCommand(message, clientId, socket, out);
            }

            // Si readLine devolvio null, el cliente cerro su conexion voluntariamente.
            session.handleDisconnect(clientId);
            ServerLogger.info("Cliente #" + clientId + " se desconecto");
        } catch (SocketTimeoutException e) {
            session.handleDisconnect(clientId);
            ServerLogger.error("Cliente #" + clientId + " inactivo por timeout de lectura");
        } catch (SocketException e) {
            session.handleDisconnect(clientId);
            ServerLogger.info("Socket de cliente #" + clientId + " cerrado");
        } catch (IOException e) {
            session.handleDisconnect(clientId);
            ServerLogger.error("Conexion con cliente #" + clientId + " finalizada por error: " + e.getMessage());
        }
    }

    // Valida y parsea el puerto de linea de comandos.
    private static int parsePort(String[] args) {
        if (args.length == 0) {
            return ProtocolConfig.DEFAULT_PORT;
        }

        try {
            int parsed = Integer.parseInt(args[0]);
            if (parsed <= 0 || parsed > 65535) {
                ServerLogger.error("Puerto invalido. Se usara el puerto por defecto " + ProtocolConfig.DEFAULT_PORT);
                return ProtocolConfig.DEFAULT_PORT;
            }
            return parsed;
        } catch (NumberFormatException e) {
            ServerLogger.error("Puerto no numerico. Se usara el puerto por defecto " + ProtocolConfig.DEFAULT_PORT);
            return ProtocolConfig.DEFAULT_PORT;
        }
    }
}
