import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// Logger minimo del servidor con marca temporal uniforme.
public final class ServerLogger {
    // Formato unico para todos los mensajes de consola.
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private ServerLogger() {
    }

    // Mensajes informativos de flujo normal.
    public static void info(String message) {
        System.out.println("[" + LocalDateTime.now().format(TS) + "] [INFO] " + message);
    }

    // Mensajes de error para diagnostico rapido.
    public static void error(String message) {
        System.err.println("[" + LocalDateTime.now().format(TS) + "] [ERROR] " + message);
    }
}
