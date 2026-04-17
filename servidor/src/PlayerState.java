import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

// Estado completo de un jugador dentro de una partida.
// Guarda barcos colocados, celdas ocupadas y ataques recibidos.
final class PlayerState {
    // Nombre visible del jugador.
    private final String name;
    // Barcos por nombre, manteniendo orden de insercion para depuracion.
    private final Map<String, ShipPlacement> shipsByName = new LinkedHashMap<>();
    // Indice rapido de celda -> barco para resolver impactos en O(1).
    private final Map<Point, ShipPlacement> occupiedCells = new HashMap<>();
    // Celdas ya atacadas por el rival.
    private final Set<Point> attackedCells = new HashSet<>();

    PlayerState(String name) {
        this.name = name;
    }

    String name() {
        return name;
    }

    // Un jugador esta listo cuando coloco toda la flota definida por protocolo.
    boolean isReady() {
        return shipsByName.size() == ProtocolConfig.SHIP_SIZES.size();
    }

    boolean hasAlreadyBeenAttacked(Point point) {
        return attackedCells.contains(point);
    }

    void registerAttack(Point point) {
        attackedCells.add(point);
    }

    boolean hasShip(String shipName) {
        return shipsByName.containsKey(shipName);
    }

    // Registra barco y mapea todas sus celdas ocupadas.
    void addShip(ShipPlacement ship) {
        shipsByName.put(ship.name(), ship);
        for (Point point : ship.cells()) {
            occupiedCells.put(point, ship);
        }
    }

    boolean hasOccupiedCell(Point point) {
        return occupiedCells.containsKey(point);
    }

    ShipPlacement shipAt(Point point) {
        return occupiedCells.get(point);
    }

    // Determina condicion de derrota de este jugador.
    boolean allShipsSunk() {
        return shipsByName.values().stream().allMatch(ShipPlacement::isSunk);
    }
}
