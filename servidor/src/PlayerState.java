import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

final class PlayerState {
    private final String name;
    private final Map<String, ShipPlacement> shipsByName = new LinkedHashMap<>();
    private final Map<Point, ShipPlacement> occupiedCells = new HashMap<>();
    private final Set<Point> attackedCells = new HashSet<>();

    PlayerState(String name) {
        this.name = name;
    }

    String name() {
        return name;
    }

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

    boolean allShipsSunk() {
        return shipsByName.values().stream().allMatch(ShipPlacement::isSunk);
    }
}
