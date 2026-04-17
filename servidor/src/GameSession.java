import java.io.BufferedWriter;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class GameSession {
    private final Map<Integer, PlayerConnection> connections = new HashMap<>();
    private final Map<Integer, PlayerState> players = new HashMap<>();
    private boolean setupStarted;
    private boolean battleStarted;
    private boolean matchFinished;
    private int currentTurn;

    synchronized void connect(int clientId, Socket socket, BufferedWriter out, String playerName) throws IOException {
        PlayerConnection connection = connections.computeIfAbsent(
            clientId,
            id -> new PlayerConnection(clientId, socket, out)
        );

        if (connection.playerId() != null) {
            connection.send(ProtocolConfig.error("ALREADY_CONNECTED", "Ya estas conectado"));
            return;
        }

        if (matchFinished) {
            resetState();
            connection = connections.computeIfAbsent(
                clientId,
                id -> new PlayerConnection(clientId, socket, out)
            );
        }

        if (players.size() >= ProtocolConfig.MAX_PLAYERS) {
            connection.send(ProtocolConfig.error("ROOM_FULL", "La sala esta llena. Espera a que finalice la partida."));
            connection.closeQuietly();
            return;
        }

        int playerId = players.containsKey(1) ? 2 : 1;
        connection.assignPlayer(playerId, playerName);
        players.put(playerId, new PlayerState(playerName));

        ServerLogger.info(playerName + " se ha conectado como jugador " + playerId + " (cliente #" + clientId + ")");
        connection.send("CONNECTED " + playerId);

        if (players.size() < 2) {
            connection.send("WAITING_FOR_OPPONENT");
            return;
        }

        setupStarted = true;
        battleStarted = false;
        currentTurn = 0;
        broadcast("START 1");
    }

    synchronized void place(int clientId, BufferedWriter out, String shipNameRaw, String xRaw, String yRaw, String orientationRaw) throws IOException {
        PlayerConnection connection = connections.get(clientId);
        if (connection == null || connection.playerId() == null) {
            sendToRawClient(out, ProtocolConfig.error("NOT_CONNECTED", "Debes ejecutar CONNECT primero"));
            return;
        }

        if (!setupStarted || players.size() < 2) {
            sendToRawClient(out, ProtocolConfig.error("WAITING_PLAYER", "Espera a que se conecte el segundo jugador"));
            return;
        }

        if (battleStarted) {
            sendToRawClient(out, ProtocolConfig.error("BATTLE_STARTED", "La fase de combate ya comenzo"));
            return;
        }

        PlayerState state = players.get(connection.playerId());
        String shipName = ProtocolConfig.canonicalShipName(shipNameRaw);
        Integer shipSize = ProtocolConfig.SHIP_SIZES.get(shipName);

        if (shipSize == null) {
            sendToRawClient(out, ProtocolConfig.error("INVALID_SHIP", "Barco no valido: " + shipNameRaw));
            return;
        }

        if (state.hasShip(shipName)) {
            sendToRawClient(out, ProtocolConfig.error("DUPLICATE_SHIP", "Ya colocaste el barco " + shipName));
            return;
        }

        int startX;
        int startY;
        try {
            startX = ProtocolConfig.parseCoordinate(xRaw, "x");
            startY = ProtocolConfig.parseCoordinate(yRaw, "y");
        } catch (IllegalArgumentException e) {
            sendToRawClient(out, ProtocolConfig.error("BAD_COORDINATE", e.getMessage()));
            return;
        }

        String orientation = orientationRaw.toUpperCase();
        if (!orientation.equals("H") && !orientation.equals("V")) {
            sendToRawClient(out, ProtocolConfig.error("BAD_ORIENTATION", "La orientacion debe ser H o V"));
            return;
        }

        List<Point> cells = buildShipCells(startX, startY, shipSize, orientation);
        if (cells.isEmpty()) {
            sendToRawClient(out, ProtocolConfig.error("INVALID_PLACEMENT", "Posicion invalida para " + shipName));
            return;
        }

        for (Point point : cells) {
            if (state.hasOccupiedCell(point)) {
                sendToRawClient(out, ProtocolConfig.error("SHIP_OVERLAP", "El barco se superpone con otro existente"));
                return;
            }
        }

        state.addShip(new ShipPlacement(shipName, shipSize, cells));
        sendToRawClient(out, "PLACE_OK " + shipName);

        if (state.isReady()) {
            sendToRawClient(out, "READY " + connection.playerId());
            ServerLogger.info(state.name() + " completo su tablero");
        }

        if (allPlayersReady()) {
            startBattle();
        }
    }

    synchronized void attack(int clientId, BufferedWriter out, String xRaw, String yRaw) throws IOException {
        PlayerConnection connection = connections.get(clientId);
        if (connection == null || connection.playerId() == null) {
            sendToRawClient(out, ProtocolConfig.error("NOT_CONNECTED", "Debes ejecutar CONNECT primero"));
            return;
        }

        if (!battleStarted) {
            sendToRawClient(out, ProtocolConfig.error("BATTLE_NOT_READY", "La batalla aun no puede comenzar"));
            return;
        }

        int attackerId = connection.playerId();
        if (attackerId != currentTurn) {
            sendToRawClient(out, ProtocolConfig.error("NOT_YOUR_TURN", "No es tu turno"));
            return;
        }

        int targetX;
        int targetY;
        try {
            targetX = ProtocolConfig.parseCoordinate(xRaw, "x");
            targetY = ProtocolConfig.parseCoordinate(yRaw, "y");
        } catch (IllegalArgumentException e) {
            sendToRawClient(out, ProtocolConfig.error("BAD_COORDINATE", e.getMessage()));
            return;
        }

        if (!ProtocolConfig.isInsideBoard(targetX, targetY)) {
            sendToRawClient(out, ProtocolConfig.error("OUT_OF_BOARD", "Coordenada fuera del tablero"));
            return;
        }

        int defenderId = opponentOf(attackerId);
        PlayerState defender = players.get(defenderId);
        if (defender == null) {
            sendToRawClient(out, ProtocolConfig.error("NO_OPPONENT", "No hay oponente disponible"));
            return;
        }

        Point target = new Point(targetX, targetY);
        if (defender.hasAlreadyBeenAttacked(target)) {
            sendToRawClient(out, ProtocolConfig.error("ALREADY_ATTACKED", "Esa coordenada ya fue atacada"));
            return;
        }

        defender.registerAttack(target);
        ShipPlacement ship = defender.shipAt(target);
        String result;

        if (ship == null) {
            result = "MISS";
        } else {
            ship.registerHit(target);
            result = ship.isSunk() ? "SUNK" : "HIT";
        }

        broadcast("RESULT " + targetX + " " + targetY + " " + result);

        if (defender.allShipsSunk()) {
            broadcast("VICTORY " + attackerId);
            endMatch();
            return;
        }

        currentTurn = defenderId;
        broadcast("TURN " + currentTurn);
    }

    synchronized void handleDisconnect(int clientId) {
        PlayerConnection connection = connections.remove(clientId);
        if (connection == null) {
            return;
        }

        Integer playerId = connection.playerId();
        if (playerId == null) {
            return;
        }

        players.remove(playerId);
        setupStarted = players.size() == 2;

        if (!battleStarted) {
            ServerLogger.info("Jugador " + playerId + " se desconecto antes de comenzar la batalla");
            return;
        }

        if (!matchFinished) {
            try {
                broadcast(ProtocolConfig.error("OPPONENT_DISCONNECTED", "El oponente se desconecto. La partida se reiniciara."));
            } catch (IOException e) {
                ServerLogger.error("No se pudo notificar la desconexion: " + e.getMessage());
            }
            endMatch();
        }
    }

    synchronized void shutdown() {
        try {
            broadcast(ProtocolConfig.error("SERVER_SHUTDOWN", "Servidor en cierre"));
        } catch (IOException e) {
            ServerLogger.error("No se pudo notificar cierre de servidor: " + e.getMessage());
        }

        for (PlayerConnection connection : new ArrayList<>(connections.values())) {
            connection.closeQuietly();
        }

        resetState();
    }

    private List<Point> buildShipCells(int startX, int startY, int size, String orientation) {
        List<Point> cells = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            int x = orientation.equals("H") ? startX + i : startX;
            int y = orientation.equals("V") ? startY + i : startY;

            if (!ProtocolConfig.isInsideBoard(x, y)) {
                return List.of();
            }

            cells.add(new Point(x, y));
        }
        return cells;
    }

    private boolean allPlayersReady() {
        return players.size() == 2 && players.values().stream().allMatch(PlayerState::isReady);
    }

    private void startBattle() throws IOException {
        if (battleStarted) {
            return;
        }

        battleStarted = true;
        currentTurn = 1;
        broadcast("TURN 1");
        ServerLogger.info("La batalla comenzo. Juega primero el jugador 1.");
    }

    private void endMatch() {
        matchFinished = true;
        battleStarted = false;
        setupStarted = false;
        currentTurn = 0;

        for (PlayerConnection connection : new ArrayList<>(connections.values())) {
            connection.closeQuietly();
        }

        resetState();
    }

    private void resetState() {
        connections.clear();
        players.clear();
        setupStarted = false;
        battleStarted = false;
        matchFinished = false;
        currentTurn = 0;
    }

    private void broadcast(String response) throws IOException {
        for (PlayerConnection connection : new ArrayList<>(connections.values())) {
            connection.send(response);
        }
    }

    private int opponentOf(int playerId) {
        return playerId == 1 ? 2 : 1;
    }

    private void sendToRawClient(BufferedWriter out, String response) throws IOException {
        out.write(response);
        out.newLine();
        out.flush();
        ServerLogger.info("Servidor -> " + response);
    }
}
