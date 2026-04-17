import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class ShipPlacement {
    private final String name;
    private final int size;
    private final List<Point> cells;
    private final Set<Point> hits = new HashSet<>();

    ShipPlacement(String name, int size, List<Point> cells) {
        this.name = name;
        this.size = size;
        this.cells = List.copyOf(cells);
    }

    String name() {
        return name;
    }

    List<Point> cells() {
        return cells;
    }

    void registerHit(Point point) {
        hits.add(point);
    }

    boolean isSunk() {
        return hits.size() >= size;
    }
}
