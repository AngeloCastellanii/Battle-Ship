import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class ServerLogger {
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private ServerLogger() {
    }

    public static void info(String message) {
        System.out.println("[" + LocalDateTime.now().format(TS) + "] [INFO] " + message);
    }

    public static void error(String message) {
        System.err.println("[" + LocalDateTime.now().format(TS) + "] [ERROR] " + message);
    }
}
