import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Modelo inmutable de un barco colocado sobre el tablero.
final class ShipPlacement {
    // Nombre canonico del barco.
    private final String name;
    // Cantidad de celdas que ocupa.
    private final int size;
    // Coordenadas ocupadas por este barco.
    private final List<Point> cells;
    // Coordenadas de impacto sobre este barco.
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

    // Registra un impacto confirmado sobre una celda del barco.
    void registerHit(Point point) {
        hits.add(point);
    }

    // Un barco se considera hundido cuando se impactaron todas sus celdas.
    boolean isSunk() {
        return hits.size() >= size;
    }
}
