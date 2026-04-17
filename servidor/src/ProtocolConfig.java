import java.util.Map;

public final class ProtocolConfig {
    public static final int DEFAULT_PORT = 5000;
    public static final int BOARD_SIZE = 10;
    public static final int MAX_PLAYERS = 2;
    public static final int CLIENT_READ_TIMEOUT_MS = 120_000;

    public static final Map<String, Integer> SHIP_SIZES = Map.of(
        "PORTAAVIONES", 5,
        "ACORAZADO", 4,
        "SUBMARINO", 3,
        "DESTRUCTOR", 2,
        "LANCHA", 1
    );

    public static final Map<String, String> SHIP_ALIASES = Map.of(
        "CARRIER", "PORTAAVIONES",
        "PORTAAVIONES", "PORTAAVIONES",
        "BATTLESHIP", "ACORAZADO",
        "ACORAZADO", "ACORAZADO",
        "SUBMARINE", "SUBMARINO",
        "SUBMARINO", "SUBMARINO",
        "DESTROYER", "DESTRUCTOR",
        "DESTRUCTOR", "DESTRUCTOR",
        "PATROL", "LANCHA",
        "LANCHA", "LANCHA"
    );

    private ProtocolConfig() {
    }

    public static int parseCoordinate(String raw, String fieldName) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " debe ser numerico");
        }
    }

    public static String canonicalShipName(String rawShipName) {
        String key = rawShipName.toUpperCase();
        return SHIP_ALIASES.getOrDefault(key, key);
    }

    public static boolean isInsideBoard(int x, int y) {
        return x >= 0 && x < BOARD_SIZE && y >= 0 && y < BOARD_SIZE;
    }

    public static String error(String code, String message) {
        return "ERROR " + code + " " + message;
    }
}
