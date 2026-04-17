import java.util.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

// Aplicacion cliente principal.
// Esta clase coordina UI, red y logica local de tablero.
public class BattleshipClient extends Application {

    // Estructura temporal para recordar una colocacion pendiente hasta recibir PLACE_OK.
    private static final class PendingPlacement {
        final String ship;
        final int x;
        final int y;
        final String orientation;
        final int size;

        PendingPlacement(String ship, int x, int y, String orientation, int size) {
            this.ship = ship;
            this.x = x;
            this.y = y;
            this.orientation = orientation;
            this.size = size;
        }
    }

    // Flota esperada por el cliente (mismo orden visual de colocacion).
    private final java.util.List<String> shipsToPlace = java.util.List.of("PORTAAVIONES", "ACORAZADO", "SUBMARINO", "DESTRUCTOR", "LANCHA");
    // Evita enviar mas de una solicitud PLACE simultanea.
    private volatile boolean waitingForPlace = false;
    // Barco actualmente seleccionado por el usuario.
    private String currentShip;
    // Ultima colocacion enviada y aun no confirmada por servidor.
    private PendingPlacement pendingPlacement;
    // Estado de turno local para habilitar/disabling ataques en UI.
    private boolean myTurn = false;
    // Id asignado por servidor (1 o 2).
    private int currentPlayerId = -1;

    // Modulos principales del cliente.
    private UIManager uiManager;
    private NetworkManager networkManager;
    private GameManager gameManager;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    // Inicializa escena inicial y dependencias de la app.
    public void start(Stage primaryStage) {
        uiManager = new UIManager(this, primaryStage);
        networkManager = new NetworkManager(this);
        gameManager = new GameManager(this);

        uiManager.createScenes();
        primaryStage.setTitle("Battleship Client - JavaFX");
        primaryStage.setScene(uiManager.getConnectionScene());
        primaryStage.show();
    }

    // Solicita conexion de red al servidor con host/puerto/nombre.
    public void connectToServer(String host, int port, String name) {
        networkManager.connectToServer(host, port, name);
    }

    // Cambia de pantalla cuando el login ya fue aceptado por servidor.
    public void switchToCombat() {
        uiManager.switchToCombat();
    }

    // Muestra panel de colocacion al recibir START.
    public void showPlacement() {
        uiManager.showPlacement();
    }

    // Envia ataque solo si es turno propio.
    public void handleAttack(int row, int col) {
        if (!myTurn) {
            uiManager.showAlert("Not your turn", "Wait for your turn.");
            return;
        }
        networkManager.sendMessage("ATTACK " + row + " " + col);
        myTurn = false;
        uiManager.setStatus("Attack sent. Waiting for result...");
    }

    // Refleja resultado de ataque en el tablero enemigo.
    public void updateEnemyBoard(int x, int y, String result) {
        uiManager.updateEnemyBoard(x, y, result);
    }

    // Error de etapa de conexion/login.
    public void showConnectionError(String message) {
        Platform.runLater(() -> uiManager.showAlert("Connection Failed", message));
    }

    // Error de desconexion durante partida.
    public void showConnectionLost(String message) {
        Platform.runLater(() -> {
            uiManager.showAlert("Connection Lost", message);
            uiManager.showReconnectOption();
        });
    }

    // Error funcional enviado por servidor (comando invalido, turno, etc).
    public void showError(String message) {
        Platform.runLater(() -> uiManager.showAlert("Server Error", message));
    }

    // Muestra pantalla final de victoria/derrota.
    public void showVictory(String message) {
        Platform.runLater(() -> {
            uiManager.setStatus("Game over: " + message);
            uiManager.showAlert("Game Over", message);
        });
    }

    // Actualiza barra de estado de la UI.
    public void setStatus(String text) {
        uiManager.setStatus(text);
    }

    // Actualiza indicador de turno y habilita/disabilita grilla de ataque.
    public void setTurn(boolean turn) {
        myTurn = turn;
        Platform.runLater(() -> {
            uiManager.setStatus(myTurn ? "Your turn!" : "Opponent's turn.");
            uiManager.setEnemyGridEnabled(myTurn);
        });
    }

    // Validacion local previa para dar feedback instantaneo al usuario.
    public boolean validatePlacement(String ship, int x, int y, String ori) {
        return gameManager.validatePlacement(ship, x, y, ori);
    }

    // Aplica la colocacion confirmada sobre el tablero local.
    public void placeOnBoard(String ship, int x, int y, String ori) {
        gameManager.placeOnBoard(ship, x, y, ori);
    }

    public int getShipSize(String ship) {
        return gameManager.getShipSize(ship);
    }

    public void addPlacedShip(String ship) {
        gameManager.addPlacedShip(ship);
    }

    public boolean isWaitingForPlace() {
        return waitingForPlace;
    }

    public void setWaitingForPlace(boolean waiting) {
        waitingForPlace = waiting;
    }

    public String getCurrentShip() {
        return currentShip;
    }

    public void setCurrentShip(String ship) {
        currentShip = ship;
    }

    public void setPendingPlacement(String ship, int x, int y, String orientation, int size) {
        pendingPlacement = new PendingPlacement(ship, x, y, orientation, size);
    }

    // Solo confirmo visual y logica local cuando el servidor responde PLACE_OK.
    public void confirmCurrentPlacement(String serverShipName) {
        if (pendingPlacement == null || !pendingPlacement.ship.equals(serverShipName)) {
            return;
        }

        placeOnBoard(
            pendingPlacement.ship,
            pendingPlacement.x,
            pendingPlacement.y,
            pendingPlacement.orientation
        );
        addPlacedShip(pendingPlacement.ship);
        uiManager.confirmShipPlacement(
            pendingPlacement.ship,
            pendingPlacement.x,
            pendingPlacement.y,
            pendingPlacement.orientation,
            pendingPlacement.size
        );
        pendingPlacement = null;
    }

    // Limpia solicitud pendiente al recibir ERROR o cancelar flujo.
    public void rejectCurrentPlacement() {
        pendingPlacement = null;
    }

    public int getCurrentPlayerId() {
        return currentPlayerId;
    }

    public void setCurrentPlayerId(int id) {
        currentPlayerId = id;
    }

    // Punto unico para envio de comandos al servidor.
    public void sendMessage(String message) {
        networkManager.sendMessage(message);
    }
}
