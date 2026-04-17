import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class UIManager {
    private BattleshipClient client;
    private Stage primaryStage;
    private Scene connectionScene, combatScene;
    private GridPane ownGrid, enemyGrid;
    private Button[][] ownButtons, enemyButtons;
    private Label statusLabel;
    private VBox placementBox;

    public UIManager(BattleshipClient client, Stage primaryStage) {
        this.client = client;
        this.primaryStage = primaryStage;
    }

    public void createScenes() {
        createConnectionScene();
        createCombatScene();
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
                client.connectToServer(host, port, name);
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

        // Placement controls
        VBox placementBox = new VBox(10);
        placementBox.setPadding(new Insets(10));
        placementBox.setAlignment(Pos.TOP_LEFT);
        Label placeLabel = new Label("Place Ships:");
        ComboBox<String> shipCombo = new ComboBox<>();
        shipCombo.getItems().addAll("PORTAAVIONES", "ACORAZADO", "SUBMARINO", "DESTRUCTOR", "LANCHA");
        shipCombo.setValue("PORTAAVIONES");
        TextField xField = new TextField();
        xField.setPromptText("X (0-9)");
        TextField yField = new TextField();
        yField.setPromptText("Y (0-9)");
        ComboBox<String> oriCombo = new ComboBox<>();
        oriCombo.getItems().addAll("H", "V");
        oriCombo.setValue("H");
        Button placeButton = new Button("Place Ship");
        placeButton.setOnAction(e -> {
            String ship = shipCombo.getValue();
            String xText = xField.getText().trim();
            String yText = yField.getText().trim();
            String ori = oriCombo.getValue();
            try {
                int x = Integer.parseInt(xText);
                int y = Integer.parseInt(yText);
                if (client.validatePlacement(ship, x, y, ori)) {
                    client.placeOnBoard(ship, x, y, ori);
                    client.setCurrentShip(ship);
                    client.setWaitingForPlace(true);
                    client.sendMessage("PLACE " + ship + " " + x + " " + y + " " + ori);
                    updateOwnBoard(x, y, ori, client.getShipSize(ship));
                    shipCombo.getItems().remove(ship);
                    if (shipCombo.getItems().isEmpty()) {
                        placementBox.setVisible(false);
                        enemyGrid.setDisable(false);
                        setStatus("All ships placed. Waiting for opponent...");
                    }
                } else {
                    showAlert("Invalid Placement", "Position invalid or overlapping.");
                }
            } catch (NumberFormatException ex) {
                showAlert("Error", "Invalid coordinates.");
            }
        });
        placementBox.getChildren().addAll(placeLabel, shipCombo, xField, yField, oriCombo, placeButton);
        placementBox.setVisible(false);
        this.placementBox = placementBox;

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
                ownButtons[i][j].setStyle("-fx-background-color: lightblue;");
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
                enemyButtons[i][j].setOnAction(e -> client.handleAttack(row, col));
                enemyGrid.add(enemyButtons[i][j], j, i);
            }
        }
        enemyPanel.getChildren().addAll(enemyLabel, enemyGrid);
        enemyGrid.setDisable(true);
        boardsBox.getChildren().add(enemyPanel);

        HBox mainBox = new HBox(20);
        mainBox.getChildren().addAll(placementBox, boardsBox);

        borderPane.setCenter(mainBox);
        combatScene = new Scene(borderPane, 900, 600);
    }

    public void switchToCombat() {
        Platform.runLater(() -> {
            primaryStage.setScene(combatScene);
            setStatus("Connected! Waiting for opponent...");
        });
    }

    public void showPlacement() {
        Platform.runLater(() -> {
            setStatus("Game started! Place your ships.");
            placementBox.setVisible(true);
        });
    }

    public void setStatus(String text) {
        Platform.runLater(() -> statusLabel.setText(text));
    }

    public void updateEnemyBoard(int x, int y, String result) {
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

    private void updateOwnBoard(int x, int y, String ori, int size) {
        Platform.runLater(() -> {
            if (ori.equals("H")) {
                for (int i = 0; i < size; i++) {
                    ownButtons[x][y + i].setStyle("-fx-background-color: green;");
                }
            } else {
                for (int i = 0; i < size; i++) {
                    ownButtons[x + i][y].setStyle("-fx-background-color: green;");
                }
            }
        });
    }

    public Scene getConnectionScene() {
        return connectionScene;
    }

    public void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}