import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.util.Map;

// Capa visual JavaFX del cliente.
// Construye escenas, tableros y controla interacciones de usuario.
public class UIManager {
    // Referencias principales para callbacks y cambio de escena.
    private BattleshipClient client;
    private Stage primaryStage;
    // Escena de login y escena principal de combate.
    private Scene connectionScene, combatScene;
    // Grillas visuales de tablero propio y enemigo.
    private GridPane ownGrid, enemyGrid;
    private Button[][] ownButtons, enemyButtons;
    // Estado visual global.
    private Label statusLabel;
    private VBox placementBox;
    private ListView<String> shipListView;
    private Label selectedShipLabel;
    // Orientacion actual elegida por usuario al colocar barcos.
    private char currentOrientation = 'H';

    // Tamanos usados para pintar y remover items de la lista de barcos.
    private static final Map<String, Integer> SHIP_SIZES = Map.of(
        "PORTAAVIONES", 5,
        "ACORAZADO", 4,
        "SUBMARINO", 3,
        "DESTRUCTOR", 2,
        "LANCHA", 1
    );

    // ESTILOS EN JAVA PURO (CSS INLINE)
    private final String BG_COLOR = "#0f172a";
    private final String ACCENT_COLOR = "#00e5ff";
    private final String PANEL_STYLE = "-fx-background-color: #1e293b; -fx-padding: 20; -fx-background-radius: 10; -fx-border-color: #334155; -fx-border-width: 1;";
    private final String STATUS_STYLE = "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #00e5ff; -fx-background-color: rgba(0, 229, 255, 0.1); -fx-padding: 10; -fx-background-radius: 5;";
    private final String WATER_STYLE = "-fx-background-color: #1e293b; -fx-border-color: rgba(255,255,255,0.05); -fx-background-radius: 2;";
    private final String SHIP_STYLE = "-fx-background-color: linear-gradient(to bottom right, #475569, #1e293b); -fx-border-color: #00e5ff; -fx-border-width: 1;";
    private final String HIT_STYLE = "-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold;";
    private final String MISS_STYLE = "-fx-background-color: #334155; -fx-text-fill: #94a3b8;";
    private final String SUNK_STYLE = "-fx-background-color: #000000; -fx-text-fill: white; -fx-border-color: #ef4444;";
    private final String BTN_PRIMARY = "-fx-background-color: #00e5ff; -fx-text-fill: #0f172a; -fx-font-weight: bold; -fx-cursor: hand;";
    private final String COORD_STYLE = "-fx-text-fill: #64748b; -fx-font-size: 11px; -fx-font-weight: bold;";

    public UIManager(BattleshipClient client, Stage primaryStage) {
        this.client = client;
        this.primaryStage = primaryStage;
    }

    // Inicializa todas las escenas de la app.
    public void createScenes() {
        createConnectionScene();
        createCombatScene();
    }

    // Pantalla inicial para host, puerto y nombre de jugador.
    private void createConnectionScene() {
        VBox vbox = new VBox(20);
        vbox.setPadding(new Insets(40));
        vbox.setAlignment(Pos.CENTER);
        vbox.setStyle("-fx-background-color: " + BG_COLOR + ";");

        VBox card = new VBox(20);
        card.setStyle(PANEL_STYLE);
        card.setMaxWidth(350);
        card.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("BATTLESHIP");
        titleLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: " + ACCENT_COLOR + ";");

        GridPane form = new GridPane();
        form.setHgap(15);
        form.setVgap(15);
        form.setAlignment(Pos.CENTER);

        TextField hostField = createStyledTextField("localhost", "Host");
        TextField portField = createStyledTextField("5000", "Port");
        TextField nameField = createStyledTextField("", "Your Name");

        form.add(new Label("HOST:"), 0, 0);
        form.add(hostField, 1, 0);
        form.add(new Label("PORT:"), 0, 1);
        form.add(portField, 1, 1);
        form.add(new Label("NAME:"), 0, 2);
        form.add(nameField, 1, 2);

        Button connectButton = new Button("INITIALIZE SYSTEMS");
        connectButton.setStyle(BTN_PRIMARY + "-fx-font-size: 14px; -fx-padding: 12 25;");
        connectButton.setMaxWidth(Double.MAX_VALUE);
        
        // Al presionar conectar, valido datos basicos y delego a NetworkManager.
        connectButton.setOnAction(e -> {
            String host = hostField.getText().trim();
            String portText = portField.getText().trim();
            String name = nameField.getText().trim();
            if (name.isEmpty()) { showAlert("Error", "Identifier required."); return; }
            try {
                int port = Integer.parseInt(portText);
                client.connectToServer(host, port, name);
            } catch (NumberFormatException ex) { showAlert("Error", "Invalid port."); }
        });

        card.getChildren().addAll(titleLabel, form, connectButton);
        vbox.getChildren().add(card);
        
        connectionScene = new Scene(vbox, 450, 450);
    }

    // Factor comun de estilo para campos de texto.
    private TextField createStyledTextField(String text, String prompt) {
        TextField tf = new TextField(text);
        tf.setPromptText(prompt);
        tf.setStyle("-fx-background-color: #0f172a; -fx-text-fill: white; -fx-border-color: #334155; -fx-border-radius: 3; -fx-padding: 5;");
        return tf;
    }

    // Escena principal: panel de colocacion + tablero propio + tablero enemigo.
    private void createCombatScene() {
        BorderPane borderPane = new BorderPane();
        borderPane.setStyle("-fx-background-color: " + BG_COLOR + ";");
        borderPane.setPadding(new Insets(20));

        statusLabel = new Label("WAITING FOR COMMANDS...");
        statusLabel.setStyle(STATUS_STYLE);
        statusLabel.setMaxWidth(Double.MAX_VALUE);
        statusLabel.setAlignment(Pos.CENTER);
        borderPane.setTop(statusLabel);

        // Panel de colocacion para seleccionar barco y orientacion.
        VBox placementControls = new VBox(15);
        placementControls.setStyle(PANEL_STYLE);
        placementControls.setMinWidth(280);
        
        Label placeLabel = new Label("STRATEGIC DEPLOYMENT");
        placeLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");

        Label availableShipsLabel = new Label("AVAILABLE SHIPS:");
        availableShipsLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: bold;");

        shipListView = new ListView<>();
        shipListView.getItems().addAll(
            "PORTAAVIONES (5)",
            "ACORAZADO (4)",
            "SUBMARINO (3)",
            "DESTRUCTOR (2)",
            "LANCHA (1)"
        );
        shipListView.setPrefHeight(150);
        shipListView.setStyle("-fx-control-inner-background: #0f172a; -fx-background-color: #0f172a; -fx-text-fill: white;");
        shipListView.getSelectionModel().selectFirst();

        selectedShipLabel = new Label("SELECTED: PORTAAVIONES");
        selectedShipLabel.setStyle("-fx-text-fill: #00e5ff; -fx-font-size: 11px; -fx-font-weight: bold;");

        // Reflejo inmediato del item activo de la lista.
        shipListView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                selectedShipLabel.setText("SELECTED: " + newV.split(" ")[0]);
            }
        });
        
        ToggleGroup orientationGroup = new ToggleGroup();
        RadioButton hRadio = new RadioButton("Horizontal");
        hRadio.setToggleGroup(orientationGroup);
        hRadio.setSelected(true);
        hRadio.setStyle("-fx-text-fill: white;");
        hRadio.setOnAction(e -> currentOrientation = 'H');
        
        RadioButton vRadio = new RadioButton("Vertical");
        vRadio.setToggleGroup(orientationGroup);
        vRadio.setStyle("-fx-text-fill: white;");
        vRadio.setOnAction(e -> currentOrientation = 'V');
        
        HBox oriBox = new HBox(15, hRadio, vRadio);
        Label hintLabel = new Label("Ajuste rumbo y haga clic en el mapa");
        hintLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");

        Label orientationLabel = new Label("ORIENTATION:");
        orientationLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: bold;");

        placementControls.getChildren().addAll(
            placeLabel,
            availableShipsLabel,
            shipListView,
            selectedShipLabel,
            orientationLabel,
            oriBox,
            hintLabel
        );
        placementControls.setVisible(false);
        this.placementBox = placementControls;

        // Los dos tableros se muestran lado a lado.
        HBox boardsBox = new HBox(40);
        boardsBox.setAlignment(Pos.CENTER);
        boardsBox.getChildren().addAll(createBoardWithLabels("COMMANDER SECTOR", true), createBoardWithLabels("ENGAGEMENT SECTOR", false));
        // Al inicio, no se puede atacar hasta que haya turno.
        enemyGrid.setDisable(true);

        HBox mainContainer = new HBox(25);
        mainContainer.setAlignment(Pos.CENTER);
        mainContainer.getChildren().addAll(placementControls, boardsBox);

        borderPane.setCenter(mainContainer);
        combatScene = new Scene(borderPane, 1200, 750);
    }

    // Crea un tablero con etiquetas de coordenadas.
    private VBox createBoardWithLabels(String title, boolean isOwn) {
        VBox panel = new VBox(10);
        panel.setAlignment(Pos.CENTER);
        
        Label boardTitle = new Label(title);
        boardTitle.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        GridPane container = new GridPane();
        container.setStyle("-fx-background-color: #0f172a; -fx-padding: 10; -fx-border-color: " + ACCENT_COLOR + "; -fx-border-width: 0.5;");
        
        GridPane grid = new GridPane();
        grid.setHgap(3);
        grid.setVgap(3);

        // Matriz de botones para actualizar celdas por coordenada.
        Button[][] buttons = new Button[10][10];

        for (int j = 0; j < 10; j++) {
            Label label = new Label(String.valueOf(j + 1));
            label.setStyle(COORD_STYLE);
            label.setPrefSize(40, 20);
            label.setAlignment(Pos.CENTER);
            container.add(label, j + 1, 0);
        }

        for (int i = 0; i < 10; i++) {
            Label label = new Label(String.valueOf((char)('A' + i)));
            label.setStyle(COORD_STYLE);
            label.setPrefSize(25, 40);
            label.setAlignment(Pos.CENTER);
            container.add(label, 0, i + 1);
            
            for (int j = 0; j < 10; j++) {
                buttons[i][j] = new Button();
                buttons[i][j].setStyle(WATER_STYLE);
                buttons[i][j].setPrefSize(40, 40);
                
                final int row = i, col = j;
                // La grilla propia coloca barcos; la enemiga envia ataques.
                if (isOwn) {
                    buttons[i][j].setOnAction(e -> handlePlacementClick(row, col));
                } else {
                    buttons[i][j].setOnAction(e -> handleAttackClick(row, col));
                }
                grid.add(buttons[i][j], j, i);
            }
        }
        
        container.add(grid, 1, 1, 10, 10);
        if (isOwn) { this.ownGrid = grid; this.ownButtons = buttons; }
        else { this.enemyGrid = grid; this.enemyButtons = buttons; }

        panel.getChildren().addAll(boardTitle, container);
        return panel;
    }

    // Click sobre tablero propio durante fase de colocacion.
    private void handlePlacementClick(int x, int y) {
        if (!placementBox.isVisible()) return;
        String selection = shipListView.getSelectionModel().getSelectedItem();
        if (selection == null) return;

        if (client.isWaitingForPlace()) {
            setStatus("WAITING FOR SERVER CONFIRMATION...");
            return;
        }
        
        String shipName = selection.split(" ")[0];
        String ori = String.valueOf(currentOrientation);

        // Primero valido localmente, luego envio PLACE al servidor.
        if (client.validatePlacement(shipName, x, y, ori)) {
            client.setCurrentShip(shipName);
            client.setWaitingForPlace(true);
            client.setPendingPlacement(shipName, x, y, ori, client.getShipSize(shipName));
            client.sendMessage("PLACE " + shipName + " " + x + " " + y + " " + ori);
            setStatus("SENDING DEPLOYMENT FOR " + shipName + "...");
        } else {
            setStatus("INVALID POSITION FOR " + shipName);
        }
    }

    // Confirmacion final de colocacion despues de PLACE_OK del servidor.
    public void confirmShipPlacement(String shipName, int x, int y, String ori, int size) {
        Platform.runLater(() -> {
            updateOwnBoard(x, y, ori, size);

            String itemToRemove = shipName + " (" + size + ")";
            shipListView.getItems().remove(itemToRemove);

            if (shipListView.getItems().isEmpty()) {
                placementBox.setVisible(false);
                setStatus("FLEET READY. WAITING FOR BATTLE START.");
            } else {
                shipListView.getSelectionModel().selectFirst();
                setStatus("SHIP CONFIRMED: " + shipName + ". PLACE NEXT SHIP.");
            }
        });
    }

    // Click sobre tablero enemigo: intento de ataque.
    private void handleAttackClick(int x, int y) {
        client.handleAttack(x, y);
    }

    // Vuelve a pantalla de conexion tras una perdida de enlace.
    public void showReconnectOption() {
        Platform.runLater(() -> { primaryStage.setScene(connectionScene); setStatus("SIGNAL LOST. RECONNECTING..."); });
    }

    // Habilita panel de despliegue cuando comienza fase de setup.
    public void showPlacement() {
        Platform.runLater(() -> {
            setStatus("POSITION YOUR FLEET ON THE GRID.");
            placementBox.setVisible(true);
            if (!shipListView.getItems().isEmpty() && shipListView.getSelectionModel().getSelectedItem() == null) {
                shipListView.getSelectionModel().selectFirst();
            }
        });
    }

    // Cambia a la escena de juego luego de handshake CONNECTED.
    public void switchToCombat() {
        Platform.runLater(() -> { primaryStage.setScene(combatScene); setStatus("SYSTEMS ONLINE. SYNCING..."); });
    }

    // Pinta el resultado de un ataque en la grilla enemiga.
    public void updateEnemyBoard(int x, int y, String result) {
        Platform.runLater(() -> {
            Button btn = enemyButtons[x][y];
            switch (result) {
                case "HIT" -> { btn.setStyle(HIT_STYLE); btn.setText("H"); }
                case "MISS" -> { btn.setStyle(MISS_STYLE); btn.setText("M"); }
                case "SUNK" -> { btn.setStyle(SUNK_STYLE); btn.setText("S"); }
            }
            btn.setDisable(true);
        });
    }

    // Actualiza texto de estado superior.
    public void setStatus(String text) { Platform.runLater(() -> statusLabel.setText(text.toUpperCase())); }

    // Habilita/disabilita grilla enemiga segun turno.
    public void setEnemyGridEnabled(boolean enabled) { Platform.runLater(() -> enemyGrid.setDisable(!enabled)); }

    // Pinta visualmente un barco en tablero propio.
    public void updateOwnBoard(int x, int y, String ori, int size) {
        Platform.runLater(() -> {
            for (int i = 0; i < size; i++) {
                int r = x + (ori.equals("V") ? i : 0);
                int c = y + (ori.equals("H") ? i : 0);
                if (r < 10 && c < 10) ownButtons[r][c].setStyle(SHIP_STYLE);
            }
        });
    }

    public Scene getConnectionScene() { return connectionScene; }

    // Dialogo generico para notificaciones al usuario.
    public void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}