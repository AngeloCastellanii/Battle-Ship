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

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private Scanner scanner = new Scanner(System.in);

    private boolean placementPhase = false;
    private final java.util.List<String> shipsToPlace = java.util.List.of("PORTAAVIONES", "ACORAZADO", "SUBMARINO", "DESTRUCTOR", "LANCHA");
    private final java.util.Set<String> placedShips = new java.util.HashSet<>();
    private final java.util.Map<String, Integer> shipSizes = java.util.Map.of(
        "PORTAAVIONES", 5,
        "ACORAZADO", 4,
        "SUBMARINO", 3,
        "DESTRUCTOR", 2,
        "LANCHA", 1
    );
    private final char[][] board = new char[10][10];
    private volatile boolean waitingForPlace = false;
    private String currentShip;

    // JavaFX elements
    private Stage primaryStage;
    private Scene connectionScene, combatScene;
    private GridPane ownGrid, enemyGrid;
    private Button[][] ownButtons, enemyButtons;
    private Label statusLabel;
    private boolean myTurn = false;
    private int currentPlayerId = -1;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Battleship Client");

        createConnectionScene();
        createCombatScene();

        primaryStage.setScene(connectionScene);
        primaryStage.show();
    }

    private void createConnectionScene() {
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(20));
        vbox.setAlignment(Pos.CENTER);

        Label hostLabel = new Label("Host:");
        TextField hostField = new TextField("localhost");

        Label portLabel = new Label("Port:");
        TextField portField = new TextField("5000");

        Label nameLabel = new Label("Name:");
        TextField nameField = new TextField();

        Button connectButton = new Button("Connect");
        connectButton.setOnAction(e -> {
            String host = hostField.getText().trim();
            String portText = portField.getText().trim();
            String name = nameField.getText().trim();

            if (name.isEmpty()) {
                showAlert("Error", "Name cannot be empty");
                return;
            }

            try {
                int port = Integer.parseInt(portText);
                connectToServer(host, port, name);
            } catch (NumberFormatException ex) {
                showAlert("Error", "Invalid port number");
            }
        });

        vbox.getChildren().addAll(hostLabel, hostField, portLabel, portField, nameLabel, nameField, connectButton);
        connectionScene = new Scene(vbox, 300, 250);
    }

    private void createCombatScene() {
        BorderPane borderPane = new BorderPane();

        statusLabel = new Label("Waiting for game to start...");
        borderPane.setTop(statusLabel);
        BorderPane.setAlignment(statusLabel, Pos.CENTER);

        HBox boardsBox = new HBox(20);
        boardsBox.setPadding(new Insets(10));
        boardsBox.setAlignment(Pos.CENTER);

        // Own board
        VBox ownPanel = new VBox(5);
        ownPanel.setAlignment(Pos.CENTER);
        Label ownLabel = new Label("Your Board");
        ownGrid = new GridPane();
        ownButtons = new Button[10][10];
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                ownButtons[i][j] = new Button();
                ownButtons[i][j].setStyle("-fx-background-color: cyan;");
                ownButtons[i][j].setPrefSize(30, 30);
                ownGrid.add(ownButtons[i][j], j, i);
            }
        }
        ownPanel.getChildren().addAll(ownLabel, ownGrid);
        boardsBox.getChildren().add(ownPanel);

        // Enemy board
        VBox enemyPanel = new VBox(5);
        enemyPanel.setAlignment(Pos.CENTER);
        Label enemyLabel = new Label("Enemy Board");
        enemyGrid = new GridPane();
        enemyButtons = new Button[10][10];
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                enemyButtons[i][j] = new Button();
                enemyButtons[i][j].setStyle("-fx-background-color: lightgray;");
                enemyButtons[i][j].setPrefSize(30, 30);
                final int row = i, col = j;
                enemyButtons[i][j].setOnAction(e -> handleAttack(row, col));
                enemyGrid.add(enemyButtons[i][j], j, i);
            }
        }
        enemyPanel.getChildren().addAll(enemyLabel, enemyGrid);
        boardsBox.getChildren().add(enemyPanel);

        borderPane.setCenter(boardsBox);
        combatScene = new Scene(borderPane, 800, 600);
    }

    private void handleAttack(int row, int col) {
        if (!myTurn) {
            showAlert("Not your turn", "Wait for your turn.");
            return;
        }
        out.println("ATTACK " + row + " " + col);
        myTurn = false;
        Platform.runLater(() -> statusLabel.setText("Attack sent. Waiting for result..."));
    }

    private void updateEnemyBoard(int x, int y, String result) {
        Platform.runLater(() -> {
            Button button = enemyButtons[x][y];
            switch (result) {
                case "HIT" -> button.setStyle("-fx-background-color: red;");
                case "MISS" -> button.setStyle("-fx-background-color: white;");
                case "SUNK" -> button.setStyle("-fx-background-color: black;");
            }
            button.setDisable(true);
        });
    }

    private void connectToServer(String host, int port, String name) {
        try {
            socket = new Socket(host, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            out.println("CONNECT " + name);

            executor.submit(this::listenForMessages);

            Platform.runLater(() -> {
                primaryStage.setScene(combatScene);
                statusLabel.setText("Connected! Waiting for opponent...");
            });

        } catch (IOException e) {
            Platform.runLater(() -> showAlert("Connection Failed", e.getMessage()));
        }
    }

    private void listenForMessages() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("Received: " + message);
                processMessage(message);
            }
        } catch (IOException e) {
            System.out.println("Connection lost: " + e.getMessage());
        }
    }

    private void processMessage(String message) {
        String[] parts = message.split(" ");
        if (parts.length == 0) return;

        String command = parts[0];
        switch (command) {
            case "START" -> {
                if (parts.length >= 2) {
                    String firstPlayer = parts[1];
                    Platform.runLater(() -> statusLabel.setText("Game started! First player: " + firstPlayer));
                }
            }
            case "TURN" -> {
                if (parts.length >= 2) {
                    int playerId = Integer.parseInt(parts[1]);
                    myTurn = (playerId == currentPlayerId);
                    Platform.runLater(() -> statusLabel.setText(myTurn ? "Your turn!" : "Opponent's turn."));
                }
            }
            case "RESULT" -> {
                if (parts.length >= 4) {
                    int x = Integer.parseInt(parts[1]);
                    int y = Integer.parseInt(parts[2]);
                    String result = parts[3];
                    updateEnemyBoard(x, y, result);
                }
            }
            case "VICTORY" -> {
                if (parts.length >= 2) {
                    int winnerId = Integer.parseInt(parts[1]);
                    String msg = (winnerId == currentPlayerId) ? "You win!" : "You lose!";
                    Platform.runLater(() -> {
                        statusLabel.setText("Game over: " + msg);
                        showAlert("Game Over", msg);
                    });
                }
            }
            case "CONNECTED" -> {
                if (parts.length >= 2) {
                    currentPlayerId = Integer.parseInt(parts[1]);
                }
            }
            case "PLACE_OK" -> {
                if (parts.length >= 2 && parts[1].equals(currentShip)) {
                    placedShips.add(currentShip);
                    System.out.println("Barco " + currentShip + " colocado exitosamente.");
                    waitingForPlace = false;
                }
            }
            case "ERROR" -> {
                String errorMsg = message.substring(6);
                Platform.runLater(() -> showAlert("Server Error", errorMsg));
                if (waitingForPlace) {
                    waitingForPlace = false;
                }
            }
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void startPlacement() {
        System.out.println("Fase de colocación de barcos. Coloca tus barcos:");
        for (String ship : shipsToPlace) {
            if (placedShips.contains(ship)) continue;
            placeShip(ship);
        }
        System.out.println("Todos los barcos colocados. Esperando al oponente...");
    }

    private void placeShip(String ship) {
        while (true) {
            System.out.println("Colocando " + ship + " (tamaño " + shipSizes.get(ship) + ")");
            System.out.print("X (0-9): ");
            int x = scanner.nextInt();
            System.out.print("Y (0-9): ");
            int y = scanner.nextInt();
            scanner.nextLine();
            System.out.print("Orientación (H/V): ");
            String ori = scanner.nextLine().trim().toUpperCase();

            if (validatePlacement(ship, x, y, ori)) {
                placeOnBoard(ship, x, y, ori);
                currentShip = ship;
                waitingForPlace = true;
                out.println("PLACE " + ship + " " + x + " " + y + " " + ori);
                System.out.println("Enviado: PLACE " + ship + " " + x + " " + y + " " + ori);
                while (waitingForPlace) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                break;
            } else {
                System.out.println("Colocación inválida. Intenta de nuevo.");
            }
        }
    }

    private boolean validatePlacement(String ship, int x, int y, String ori) {
        int size = shipSizes.get(ship);
        if (ori.equals("H")) {
            if (y + size > 10) return false;
            for (int i = 0; i < size; i++) {
                if (board[x][y + i] != '\0') return false;
            }
        } else if (ori.equals("V")) {
            if (x + size > 10) return false;
            for (int i = 0; i < size; i++) {
                if (board[x + i][y] != '\0') return false;
            }
        } else {
            return false;
        }
        return true;
    }

    private void placeOnBoard(String ship, int x, int y, String ori) {
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
}
