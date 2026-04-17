import java.io.*;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class BattleshipClient extends Application {

    private ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private Scanner scanner = new Scanner(System.in);

    private boolean placementPhase = false;
    private final java.util.List<String> shipsToPlace = java.util.List.of("PORTAAVIONES", "ACORAZADO", "SUBMARINO", "DESTRUCTOR", "LANCHA");
    private volatile boolean waitingForPlace = false;
    private String currentShip;
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
        primaryStage.setTitle("Battleship Client");
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

    public void showError(String message) {
        Platform.runLater(() -> uiManager.showAlert("Server Error", message));
    }

    public void showVictory(String message) {
        Platform.runLater(() -> {
            uiManager.setStatus("Game over: " + message);
            uiManager.showAlert("Game Over", message);
        });
    }

    public void setTurn(boolean turn) {
        myTurn = turn;
        Platform.runLater(() -> uiManager.setStatus(myTurn ? "Your turn!" : "Opponent's turn."));
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

    public int getCurrentPlayerId() {
        return currentPlayerId;
    }

    public void setCurrentPlayerId(int id) {
        currentPlayerId = id;
    }

    public void sendMessage(String message) {
        networkManager.sendMessage(message);
    }

    // Legacy methods (not used in GUI version)
    private void startPlacement() {
        System.out.println("Ship placement phase. Place your ships:");
        for (String ship : shipsToPlace) {
            if (gameManager.isShipPlaced(ship)) continue;
            placeShip(ship);
        }
        System.out.println("All ships placed. Waiting for opponent...");
    }

    private void placeShip(String ship) {
        while (true) {
            System.out.println("Placing " + ship + " (size " + gameManager.getShipSize(ship) + ")");
            System.out.print("X (0-9): ");
            int x = scanner.nextInt();
            System.out.print("Y (0-9): ");
            int y = scanner.nextInt();
            scanner.nextLine();
            System.out.print("Orientation (H/V): ");
            String ori = scanner.nextLine().trim().toUpperCase();

            if (validatePlacement(ship, x, y, ori)) {
                placeOnBoard(ship, x, y, ori);
                currentShip = ship;
                waitingForPlace = true;
                networkManager.sendMessage("PLACE " + ship + " " + x + " " + y + " " + ori);
                while (waitingForPlace) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                break;
            } else {
                System.out.println("Invalid placement. Try again.");
            }
        }
    }
}
