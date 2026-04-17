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

public class ServerMain {
    private static final AtomicInteger CLIENT_COUNTER = new AtomicInteger(0);

    public static void main(String[] args) {
        int port = parsePort(args);
        GameSession session = new GameSession();
        CommandProcessor commandProcessor = new CommandProcessor(session);

        ServerLogger.info("Iniciando servidor en el puerto " + port + "...");

        try (ServerSocket serverSocket = new ServerSocket(port);
             ExecutorService clientExecutor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                ServerLogger.info("Ejecutando cierre ordenado del servidor...");
                session.shutdown();
            }));

            ServerLogger.info("Servidor listo. Esperando conexiones...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                int clientId = CLIENT_COUNTER.incrementAndGet();
                clientExecutor.submit(() -> handleClient(clientSocket, clientId, commandProcessor, session));
            }
        } catch (IOException e) {
            ServerLogger.error("Error fatal en el servidor: " + e.getMessage());
        }
    }

    private static void handleClient(Socket socket, int clientId, CommandProcessor commandProcessor, GameSession session) {
        String remote = socket.getRemoteSocketAddress().toString();
        ServerLogger.info("Cliente #" + clientId + " conectado desde " + remote);

        try (socket;
             BufferedReader in = new BufferedReader(
                 new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter out = new BufferedWriter(
                 new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {

            socket.setSoTimeout(ProtocolConfig.CLIENT_READ_TIMEOUT_MS);

            String line;
            while ((line = in.readLine()) != null) {
                String message = line.trim();
                if (message.isEmpty()) {
                    continue;
                }

                ServerLogger.info("Cliente #" + clientId + " -> " + message);
                commandProcessor.processCommand(message, clientId, socket, out);
            }

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
