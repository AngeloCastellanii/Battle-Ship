import java.util.*;

// Logica local de tablero del cliente.
// Se usa para validacion previa y render inmediato tras confirmaciones del servidor.
public class GameManager {
    // Referencia al cliente principal por si se requiere coordinacion futura.
    private BattleshipClient client;
    // Tablero local de 10x10 (\0 = vacio, letra = barco).
    private final char[][] board = new char[10][10];
    // Conjunto de barcos ya confirmados.
    private final java.util.Set<String> placedShips = new java.util.HashSet<>();
    // Catalogo de tamanos por nombre de barco.
    private final java.util.Map<String, Integer> shipSizes = java.util.Map.of(
        "PORTAAVIONES", 5,
        "ACORAZADO", 4,
        "SUBMARINO", 3,
        "DESTRUCTOR", 2,
        "LANCHA", 1
    );

    public GameManager(BattleshipClient client) {
        this.client = client;
    }

    // Verifica limites, orientacion y superposicion antes de enviar PLACE al servidor.
    public boolean validatePlacement(String ship, int x, int y, String ori) {
        int size = shipSizes.get(ship);
        if (x < 0 || x >= 10 || y < 0 || y >= 10) return false;
        if (!ori.equals("H") && !ori.equals("V")) return false;
        if (ori.equals("H")) {
            if (y + size > 10) return false;
            for (int i = 0; i < size; i++) {
                if (board[x][y + i] != '\0') return false;
            }
        } else {
            if (x + size > 10) return false;
            for (int i = 0; i < size; i++) {
                if (board[x + i][y] != '\0') return false;
            }
        }
        return true;
    }

    // Marca en tablero local las celdas ocupadas por el barco confirmado.
    public void placeOnBoard(String ship, int x, int y, String ori) {
        int size = shipSizes.get(ship);
        if (ori.equals("H")) {
            for (int i = 0; i < size; i++) {
                board[x][y + i] = ship.charAt(0);
            }
        } else {
            for (int i = 0; i < size; i++) {
                board[x + i][y] = ship.charAt(0);
            }
        }
    }

    public int getShipSize(String ship) {
        return shipSizes.get(ship);
    }

    // Registra que ese barco ya fue colocado correctamente.
    public void addPlacedShip(String ship) {
        placedShips.add(ship);
    }

    public boolean isShipPlaced(String ship) {
        return placedShips.contains(ship);
    }
}