import java.util.Map;

// Configuracion central del protocolo y utilidades de validacion.
// Mantener estas constantes aqui evita valores magicos repartidos en el codigo.
public final class ProtocolConfig {
    // Puerto por defecto del servidor.
    public static final int DEFAULT_PORT = 5000;
    // Tamano fijo del tablero cuadrado.
    public static final int BOARD_SIZE = 10;
    // Partida uno contra uno.
    public static final int MAX_PLAYERS = 2;
    // Tiempo maximo sin mensajes de un cliente antes de marcarlo inactivo.
    public static final int CLIENT_READ_TIMEOUT_MS = 120_000;

    // Flota oficial del juego: nombre canonico -> longitud.
    public static final Map<String, Integer> SHIP_SIZES = Map.of(
        "PORTAAVIONES", 5,
        "ACORAZADO", 4,
        "SUBMARINO", 3,
        "DESTRUCTOR", 2,
        "LANCHA", 1
    );

    // Alias para aceptar nombres en ingles y normalizarlos internamente.
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

    // Parseo seguro de coordenadas numericas con mensaje de error claro.
    public static int parseCoordinate(String raw, String fieldName) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " debe ser numerico");
        }
    }

    // Convierte cualquier alias valido al nombre canonico del barco.
    public static String canonicalShipName(String rawShipName) {
        String key = rawShipName.toUpperCase();
        return SHIP_ALIASES.getOrDefault(key, key);
    }

    // Verifica que una coordenada pertenezca al tablero.
    public static boolean isInsideBoard(int x, int y) {
        return x >= 0 && x < BOARD_SIZE && y >= 0 && y < BOARD_SIZE;
    }

    // Formato estandar de error del protocolo.
    public static String error(String code, String message) {
        return "ERROR " + code + " " + message;
    }
}
