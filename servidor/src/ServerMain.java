import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

public class ServerMain {
    private static final int DEFAULT_PORT = 5000;
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final AtomicInteger CLIENT_COUNTER = new AtomicInteger(0);

    public static void main(String[] args) {
        int port = parsePort(args);

        log("Iniciando servidor en el puerto " + port + "...");

        try (ServerSocket serverSocket = new ServerSocket(port);
             ExecutorService clientExecutor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {

            log("Servidor listo. Esperando conexiones...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                int clientId = CLIENT_COUNTER.incrementAndGet();
                clientExecutor.submit(() -> handleClient(clientSocket, clientId));
            }
        } catch (IOException e) {
            logError("Error fatal en el servidor: " + e.getMessage());
        }
    }

    private static void handleClient(Socket socket, int clientId) {
        String remote = socket.getRemoteSocketAddress().toString();
        log("Cliente #" + clientId + " conectado desde " + remote);

        try (socket;
             BufferedReader in = new BufferedReader(
                 new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter out = new BufferedWriter(
                 new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {

            String line;
            while ((line = in.readLine()) != null) {
                String message = line.trim();
                if (message.isEmpty()) {
                    continue;
                }

                log("Cliente #" + clientId + " -> " + message);
                processCommand(message, clientId, out);
            }

            log("Cliente #" + clientId + " se desconecto");
        } catch (IOException e) {
            logError("Conexion con cliente #" + clientId + " finalizada por error: " + e.getMessage());
        }
    }

    private static void processCommand(String message, int clientId, BufferedWriter out) throws IOException {
        String[] parts = message.split("\\s+");
        String command = parts[0].toUpperCase();

        switch (command) {
            case "CONNECT" -> {
                if (parts.length < 2) {
                    send(out, "ERROR CONNECT requiere nombre");
                    return;
                }

                String name = message.substring("CONNECT".length()).trim();
                log(name + " se ha conectado (cliente #" + clientId + ")");

                // Parte 1: respuesta minima de inicio para validar el flujo basico.
                send(out, "START 1");
            }
            default -> send(out, "ERROR Comando no soportado en MVP: " + command);
        }
    }

    private static void send(BufferedWriter out, String response) throws IOException {
        out.write(response);
        out.newLine();
        out.flush();
        log("Servidor -> " + response);
    }

    private static int parsePort(String[] args) {
        if (args.length == 0) {
            return DEFAULT_PORT;
        }

        try {
            int parsed = Integer.parseInt(args[0]);
            if (parsed <= 0 || parsed > 65535) {
                logError("Puerto invalido. Se usara el puerto por defecto " + DEFAULT_PORT);
                return DEFAULT_PORT;
            }
            return parsed;
        } catch (NumberFormatException e) {
            logError("Puerto no numerico. Se usara el puerto por defecto " + DEFAULT_PORT);
            return DEFAULT_PORT;
        }
    }

    private static void log(String msg) {
        System.out.println("[" + LocalDateTime.now().format(TS) + "] [INFO] " + msg);
    }

    private static void logError(String msg) {
        System.err.println("[" + LocalDateTime.now().format(TS) + "] [ERROR] " + msg);
    }
}
