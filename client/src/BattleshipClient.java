import java.util.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

public class BattleshipClient extends Application {

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

    private final java.util.List<String> shipsToPlace = java.util.List.of("PORTAAVIONES", "ACORAZADO", "SUBMARINO", "DESTRUCTOR", "LANCHA");
    private volatile boolean waitingForPlace = false;
    private String currentShip;
    private PendingPlacement pendingPlacement;
    private boolean myTurn = false;
    private int currentPlayerId = -1;

    private UIManager uiManager;
    private NetworkManager networkManager;
    private GameManager gameManager;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        uiManager = new UIManager(this, primaryStage);
        networkManager = new NetworkManager(this);
        gameManager = new GameManager(this);

        uiManager.createScenes();
        primaryStage.setTitle("Battleship Client - JavaFX");
        primaryStage.setScene(uiManager.getConnectionScene());
        primaryStage.show();
    }

    public void connectToServer(String host, int port, String name) {
        networkManager.connectToServer(host, port, name);
    }

    public void switchToCombat() {
        uiManager.switchToCombat();
    }

    public void showPlacement() {
        uiManager.showPlacement();
    }

    public void handleAttack(int row, int col) {
        if (!myTurn) {
            uiManager.showAlert("Not your turn", "Wait for your turn.");
            return;
        }
        networkManager.sendMessage("ATTACK " + row + " " + col);
        myTurn = false;
        uiManager.setStatus("Attack sent. Waiting for result...");
    }

    public void updateEnemyBoard(int x, int y, String result) {
        uiManager.updateEnemyBoard(x, y, result);
    }

    public void showConnectionError(String message) {
        Platform.runLater(() -> uiManager.showAlert("Connection Failed", message));
    }

    public void showConnectionLost(String message) {
        Platform.runLater(() -> {
            uiManager.showAlert("Connection Lost", message);
            uiManager.showReconnectOption();
        });
    }

    public void showError(String message) {
        Platform.runLater(() -> uiManager.showAlert("Server Error", message));
    }

    public void showVictory(String message) {
        Platform.runLater(() -> {
            uiManager.setStatus("Game over: " + message);
            uiManager.showAlert("Game Over", message);
        });
    }

    public void setStatus(String text) {
        uiManager.setStatus(text);
    }

    public void setTurn(boolean turn) {
        myTurn = turn;
        Platform.runLater(() -> {
            uiManager.setStatus(myTurn ? "Your turn!" : "Opponent's turn.");
            uiManager.setEnemyGridEnabled(myTurn);
        });
    }

    public boolean validatePlacement(String ship, int x, int y, String ori) {
        return gameManager.validatePlacement(ship, x, y, ori);
    }

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

    public void rejectCurrentPlacement() {
        pendingPlacement = null;
    }

    public int getCurrentPlayerId() {
        return currentPlayerId;
    }

    public void setCurrentPlayerId(int id) {
        currentPlayerId = id;
    }

    public void sendMessage(String message) {
        networkManager.sendMessage(message);
    }
}
