import java.util.*;

public class GameManager {
    private BattleshipClient client;
    private final char[][] board = new char[10][10];
    private final java.util.Set<String> placedShips = new java.util.HashSet<>();
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

    public void addPlacedShip(String ship) {
        placedShips.add(ship);
    }

    public boolean isShipPlaced(String ship) {
        return placedShips.contains(ship);
    }
}