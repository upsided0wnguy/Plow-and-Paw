package main.ui;

import database.BarnDAO;
import javafx.scene.layout.GridPane;
import main.models.MapSource;
import database.MapSourceDAO;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import main.models.FarmElement;
import main.models.User;
import main.models.Entity;
import main.models.TileMap;

import java.io.InputStream;
import main.network.NetworkClient;
import main.models.TileMapLoader;

import java.util.*;
import database.MapDAO;

public class FarmGameController {
    @FXML private Button saveMapButton;
    @FXML private Pane viewport;
    @FXML private Label playersLabel;
    @FXML private Button exitButton;
    @FXML private Button dashboardButton;
    @FXML private Button buyGemsButton;
    @FXML private Button loanButton;
    @FXML private ImageView coinIcon;
    @FXML private ImageView gemIcon;
    @FXML private ImageView xpIcon;
    @FXML private Label coinLabel;
    @FXML private Label gemLabel;
    @FXML private Label xpLabel;
    @FXML private Pane mapEditOverlay;
    @FXML private Label mapEditModeLabel;

    private Set<KeyCode> keysHeld = new HashSet<>();
    private boolean mKeyHandled = false;
    private boolean eKeyHandled = false;


    @FXML private Label levelLabel;

    @FXML private Label barnCapLabel;
    @FXML private Canvas gameCanvas;
    @FXML private Button tilePickerButton;

    private boolean multiplayer = false;
    private NetworkClient client;
    private String username;
    private final Map<String, int[]> playerPositions = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, main.ui.AnimatedAvatar> avatarMap = new java.util.concurrent.ConcurrentHashMap<>();

    private boolean mapEditMode = false;
    private String selectedTileCode = "GR";
    private static final int TILE_SIZE = 64;
    private TileMap tileMap;
    private main.ui.AnimatedAvatar playerAnimatedAvatar;
    private Image petAvatar = null;
    private int playerRow = 15, playerCol = 20;
    private int petRow = playerRow + 1, petCol = playerCol;
    private int cameraRow = 0, cameraCol = 0;
    private int lastDirRow = 0;
    private boolean isMoving = false;
    private List<Entity> pets = new ArrayList<>();
    private Set<KeyCode> keysPressed = new HashSet<>();
    private final long MOVE_DELAY_NS = 60_000_000L;
    private long lastMoveTime = 0;
    private List<MapSource> sources = new ArrayList<>();
    private User currentUser;
    // Add a reference for the mouse handler so we can add/remove it
    private javafx.event.EventHandler<javafx.scene.input.MouseEvent> tileEditMouseHandler;
    // Add a reference for the mouse handler so we can add/remove it


    private String getWritableMapPath() {
        // If running from IDE, use resources for easy editing
        String devPath = "src/main/resources/map/field1.csv";
        if (new java.io.File(devPath).exists()) return devPath;
        // Otherwise, fallback to user data directory (works in JAR and production)
        new java.io.File("user_data/maps").mkdirs();
        return "user_data/maps/field1.csv";
    }

    /*** ------ MULTIPLAYER SETUP ------ ***/
    public void startMultiplayer(String username, NetworkClient client) {
        this.username = username;
        this.client = client;
        this.multiplayer = true;

        playerPositions.putIfAbsent(username, new int[]{playerRow, playerCol});
        if (!avatarMap.containsKey(username) && playerAnimatedAvatar != null) {
            avatarMap.put(username, playerAnimatedAvatar);
        }

        // --- THIS IS THE KEY LINE: Attach network handler ---
        client.setListener(msg -> Platform.runLater(() -> handleNetworkMessage(msg)));
        client.send("LOGIN:" + username);

        sendMyPosition();

        if (playersLabel != null) playersLabel.setText("Multiplayer: " + username);
    }


    // You must have a setListener method in NetworkClient
// Add to NetworkClient.java:

    private void handleNetworkMessage(String msg) {
        if (msg.startsWith("POS:")) {
            String[] p = msg.split(":");
            String otherUser = p[1];
            int row = Integer.parseInt(p[2]);
            int col = Integer.parseInt(p[3]);
            if (!otherUser.equals(username)) {
                playerPositions.put(otherUser, new int[]{row, col});
                // If first time seeing this user, create an avatar and show it
                if (!avatarMap.containsKey(otherUser)) {
                    avatarMap.put(otherUser, makeNewAvatar());
                }
            }
        } else if (msg.startsWith("PLAYERS:")) {
            String[] parts = msg.split(":");
            if (parts.length > 1) {
                String[] all = parts[1].split(",");
                Platform.runLater(() -> {
                    if (playersLabel != null) playersLabel.setText("Players: " + String.join(", ", all));
                    for (String user : all) {
                        if (!avatarMap.containsKey(user)) {
                            avatarMap.put(user, makeNewAvatar());
                        }
                        if (!playerPositions.containsKey(user)) {
                            // Default spawn: center
                            playerPositions.put(user, new int[]{playerRow, playerCol});
                        }
                    }
                    // Remove avatars/positions for users who are gone
                    Set<String> toRemove = new HashSet<>(avatarMap.keySet());
                    toRemove.removeAll(Arrays.asList(all));
                    for (String gone : toRemove) {
                        avatarMap.remove(gone);
                        playerPositions.remove(gone);
                    }
                });
            }
        } else if (msg.startsWith("LEFT:")) {
            String leftUser = msg.substring(5);
            playerPositions.remove(leftUser);
            avatarMap.remove(leftUser);
        }
    }


    private main.ui.AnimatedAvatar makeNewAvatar() {
        Image farmerSheet = loadImage("/images/sprites/farmer_spritesheet.png");
        if (farmerSheet != null) {
            return new main.ui.AnimatedAvatar(new main.ui.SpriteSheet(farmerSheet, 256, 256), 0, 4, 10);
        }
        return null;
    }


    private void onMessage(String msg) {
        if (msg.startsWith("POS:")) {
            String[] p = msg.split(":");
            String otherUser = p[1];
            int row = Integer.parseInt(p[2]);
            int col = Integer.parseInt(p[3]);
            if (!otherUser.equals(username)) {
                playerPositions.put(otherUser, new int[]{row, col});
                if (!avatarMap.containsKey(otherUser)) {
                    avatarMap.put(otherUser, makeNewAvatar());
                }
            }
        } else if (msg.startsWith("JOINED:")) {
            String joined = msg.substring(7);
            Platform.runLater(() -> showInfo(joined + " joined!"));
        } else if (msg.startsWith("LEFT:")) {
            String left = msg.substring(5);
            playerPositions.remove(left);
            avatarMap.remove(left);
            Platform.runLater(() -> showInfo(left + " left."));
        } else if (msg.startsWith("PLAYERS:")) {
            String[] parts = msg.split(":");
            if (parts.length > 1) {
                String[] all = parts[1].split(",");
                Platform.runLater(() -> {
                    playersLabel.setText("Players: " + String.join(", ", all));
                    for (String user : all) {
                        // Always ensure every player has a position and avatar
                        if (!avatarMap.containsKey(user)) {
                            avatarMap.put(user, makeNewAvatar());
                        }
                        if (!playerPositions.containsKey(user)) {
                            // Place new players at center or default spawn (could improve to use their last known pos)
                            playerPositions.put(user, new int[]{playerRow, playerCol});
                        }
                    }
                });
            }
        }
    }

    private void sendMyPosition() {
        if (client != null) {
            client.send("POS:" + username + ":" + playerRow + ":" + playerCol);
            playerPositions.put(username, new int[]{playerRow, playerCol});
            if (!avatarMap.containsKey(username) && playerAnimatedAvatar != null) {
                avatarMap.put(username, playerAnimatedAvatar);
            }
        }
    }

    /*** ------ FX INITIALIZE ------ ***/
    @FXML
    public void initialize() {
        // --------- Mouse handler for tile editing (but only attach when edit mode is ON) ----------
        tileEditMouseHandler = e -> {
            if (mapEditMode) {
                int col = (int) (e.getX() / TILE_SIZE) + cameraCol;
                int row = (int) (e.getY() / TILE_SIZE) + cameraRow;
                if (selectedTileCode != null && tileMap != null) {
                    tileMap.replaceTile(row, col, selectedTileCode);
                    render(System.nanoTime());
                }
            }
            gameCanvas.requestFocus();
        };
        if (gameCanvas != null) {
            gameCanvas.setFocusTraversable(true);
            gameCanvas.setOnMouseClicked(null); // Only attach handler when in edit mode!
            Platform.runLater(() -> gameCanvas.requestFocus());
        }

        // --------- Scene key handling - attach ONCE after scene is available ----------
        Platform.runLater(() -> {
            Scene scene = gameCanvas.getScene();
            if (scene != null) {
                // Clean up any previous handlers
                scene.setOnKeyPressed(this::handleKeyPressed);
                scene.setOnKeyReleased(this::handleKeyReleased);
            }
        });

        // --------- Make viewport click always focus the canvas ----------
        if (viewport != null) {
            viewport.setOnMouseClicked(e -> {
                if (gameCanvas != null) {
                    gameCanvas.requestFocus();
                }
            });
        }

        // --------- Tile Picker Button ----------
        if (tilePickerButton != null) {
            tilePickerButton.setOnAction(e -> showTilePickerDialog());
        }

        // --------- Edit overlay (label) default state ----------
        if (mapEditOverlay != null) {
            mapEditOverlay.setVisible(false);
            mapEditOverlay.setMouseTransparent(true); // Always transparent
        }
        if (mapEditModeLabel != null) {
            mapEditModeLabel.setText("");
        }

        // --------- UI Resource Icons & Labels ----------
        if (coinIcon != null) coinIcon.setImage(loadImage("/images/ui/coin.png"));
        if (gemIcon != null) gemIcon.setImage(loadImage("/images/ui/gem.png"));
        if (xpIcon != null) xpIcon.setImage(loadImage("/images/ui/xp.png"));
        if (coinLabel != null) coinLabel.setText("0");
        if (gemLabel != null) gemLabel.setText("0");
        if (xpLabel != null) xpLabel.setText("0");
        if (levelLabel != null) levelLabel.setText("Lvl 1");
        if (playersLabel != null) playersLabel.setText("Players: ");
    }


    private void setEditingMode(boolean on) {
        System.out.println("PREV: " + mapEditMode + ", NEW: " + on);
        mapEditMode = on;
        if (mapEditOverlay != null) {
            mapEditOverlay.setVisible(on);
            mapEditOverlay.setMouseTransparent(true);
        }
        if (mapEditModeLabel != null) {
            mapEditModeLabel.setText(on ? "EDITING MODE: Click tiles to change map!" : "");
        }
        if (gameCanvas != null) {
            if (on) {
                gameCanvas.setOnMouseClicked(tileEditMouseHandler); // Enable
            } else {
                gameCanvas.setOnMouseClicked(null); // Disable
                saveMapAll();
            }
        }
        showInfo(mapEditMode ? "Map Edit Mode: ON" : "Map Edit Mode: OFF");
    }


    private void toggleEditingMode() {
        mapEditMode = !mapEditMode;
        showInfo(mapEditMode ? "Map Edit Mode: ON" : "Map Edit Mode: OFF");
        if (!mapEditMode) {
            saveMapAll();
        }
    }

    private void render(long now) {
        if (tileMap == null || gameCanvas == null) return;
        GraphicsContext gc = gameCanvas.getGraphicsContext2D();
        double w = gameCanvas.getWidth(), h = gameCanvas.getHeight();
        int viewTilesW = (int)Math.ceil(w / TILE_SIZE);
        int viewTilesH = (int)Math.ceil(h / TILE_SIZE);
        gc.setFill(javafx.scene.paint.Color.web("#AEEFFF"));
        gc.fillRect(0, 0, w, h);
        tileMap.render(gc, cameraRow, cameraCol, viewTilesH, viewTilesW);

        // Draw ALL player avatars
        for (Map.Entry<String, int[]> entry : playerPositions.entrySet()) {
            String user = entry.getKey();
            int[] pos = entry.getValue();
            int drawX = (pos[1] - cameraCol) * TILE_SIZE;
            int drawY = (pos[0] - cameraRow) * TILE_SIZE;
            main.ui.AnimatedAvatar avatar = avatarMap.get(user);
            if (avatar == null) avatar = playerAnimatedAvatar;
            if (avatar != null) {
                avatar.update(now);
                gc.drawImage(avatar.getCurrentFrame(), drawX, drawY, TILE_SIZE, TILE_SIZE);
            }
            gc.setFill(javafx.scene.paint.Color.BLACK);
            gc.fillText(user, drawX + TILE_SIZE/6.0, drawY - 4);
        }

        for (Entity pet : pets) {
            drawEntity(gc, pet);
        }

        gc.setStroke(javafx.scene.paint.Color.rgb(0, 0, 0, 0.12));
        for (int x = 0; x <= viewTilesW; x++)
            gc.strokeLine(x * TILE_SIZE, 0, x * TILE_SIZE, viewTilesH * TILE_SIZE);
        for (int y = 0; y <= viewTilesH; y++)
            gc.strokeLine(0, y * TILE_SIZE, viewTilesW * TILE_SIZE, y * TILE_SIZE);
    }

    // Load the right tile background image based on code
    private Image getTileImageForCode(String code) {
        // Always uses TileMapLoader.TILE_TYPES for all tile/entity images
        TileMapLoader.TileType type = TileMapLoader.TILE_TYPES.getOrDefault(code, TileMapLoader.TILE_TYPES.get("--"));
        String path = type.imagePath;
        return loadImage(path);
    }

    // For animals/trees/crops shown on top of base tile
    private Image getEntityImage(String code) {
        // Point to entity images; you can use the same images as tiles if needed
        return getTileImageForCode(code);
    }

    private void saveMapAll() {
        database.MapDAO.saveMapToDB(tileMap, currentUser.getUsername());
        showInfo("Map saved!");
    }

    @FXML
    private void handleSaveMap() {
        saveMapAll();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        BarnDAO.loadBarn(this.currentUser);
    }

    public void setMultiplayer(boolean isMultiplayer) {
        this.multiplayer = isMultiplayer;
        if (playersLabel != null) playersLabel.setText(multiplayer ? "Multiplayer Mode" : "Solo Mode");
    }

    public void setPlayers(List<String> players) {
        if (playersLabel != null && players != null && !players.isEmpty()) {
            playersLabel.setText("Players: " + String.join(", ", players));
        }
    }

    public void startGame(String multiplayerHostUsername) {
        resetState();

        // Determine which map to use: host's in multiplayer, otherwise current user (solo)
        String mapOwner = (multiplayer && multiplayerHostUsername != null)
                ? multiplayerHostUsername
                : (currentUser != null ? currentUser.getUsername() : null);

        if (username == null && currentUser != null) {
            username = currentUser.getUsername();
        }
        if (currentUser == null || mapOwner == null) {
            showError("User not loaded! You must setCurrentUser() before showing the game.");
            return;
        }

        // UI resource icons
        if (coinIcon != null) coinIcon.setImage(loadImage("/images/ui/coin.png"));
        if (gemIcon != null) gemIcon.setImage(loadImage("/images/ui/gem.png"));
        if (xpIcon != null) xpIcon.setImage(loadImage("/images/ui/xp.png"));

        try {
            // Make sure map CSV exists
            try (InputStream is = getClass().getResourceAsStream("/map/field1.csv")) {
                if (is == null) {
                    showError("Could not find /map/field1.csv! Check your resources folder!");
                    return;
                }
            } catch (Exception e) {
                showError("Error accessing /map/field1.csv: " + e.getMessage());
                return;
            }

            // Load tilemap from CSV, then load *from database* for the right owner
            // Load tilemap from CSV only if user has NO map in DB yet!
            tileMap = TileMapLoader.loadFromCSV("/map/field1.csv", TILE_SIZE);
            if (!MapDAO.hasMapInDB(mapOwner)) {
                MapDAO.saveMapToDB(tileMap, mapOwner); // save template to DB
            }
            MapDAO.loadMapFromDB(tileMap, mapOwner); // always load from DB

            // Load MapSources for the owner (host for MP, self for solo)
            sources = MapSourceDAO.loadSources(mapOwner);
            if (sources.isEmpty()) {
                for (int r = 0; r < tileMap.getRows(); r++) {
                    for (int c = 0; c < tileMap.getCols(); c++) {
                        String code = tileMap.getTileCode(r, c);
                        String name = getDisplayNameForTile(code);
                        String type = getElementTypeForTile(code);
                        if (name != null && type != null) {
                            MapSource ms = new MapSource(mapOwner, r, c, code, type, name, 1);
                            sources.add(ms);
                            MapSourceDAO.saveOrUpdate(ms);
                        }
                    }
                }
            }

            // Player initial position (center of map)
            playerRow = tileMap.getRows() / 2;
            playerCol = tileMap.getCols() / 2;

            // Find a walkable tile for pet (prioritize adjacent)
            int[][] adjacent = { {1,0}, {0,1}, {-1,0}, {0,-1} };
            petRow = playerRow; petCol = playerCol;
            boolean foundWalkable = false;
            for (int[] d : adjacent) {
                int tr = playerRow + d[0], tc = playerCol + d[1];
                if (tileMap.isInBounds(tr, tc) && tileMap.isWalkable(tr, tc)) {
                    petRow = tr; petCol = tc;
                    foundWalkable = true;
                    break;
                }
            }
            if (!foundWalkable) {
                petRow = playerRow;
                petCol = playerCol;
            }
            pets.clear();

            // Load main player avatar
            Image farmerSheet = loadImage("/images/sprites/farmer_spritesheet.png");
            if (farmerSheet != null) {
                playerAnimatedAvatar = new main.ui.AnimatedAvatar(new main.ui.SpriteSheet(farmerSheet, 256, 256), 0, 4, 10);
                avatarMap.put(username, playerAnimatedAvatar);
            } else {
                playerAnimatedAvatar = null;
                showError("Could not load /images/sprites/farmer_spritesheet.png.\nYour avatar will not be shown in multiplayer!");
            }

            // Load pet
            petAvatar = loadImage("/images/pets/cat_idle.png");
            if (petAvatar != null) {
                pets.add(new Entity(Entity.Type.PET, petRow, petCol, petAvatar));
            } else {
                showError("Could not load pet avatar image!");
            }

            playerPositions.put(username, new int[]{playerRow, playerCol});

        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to load game assets or map: " + e.getMessage());
            return;
        }

        // Canvas and viewport setup (resize listeners, event handlers)
        if (gameCanvas == null) {
            gameCanvas = new Canvas();
            viewport.getChildren().add(gameCanvas);
        } else if (!viewport.getChildren().contains(gameCanvas)) {
            viewport.getChildren().add(gameCanvas);
        }

        ChangeListener<Number> resizeListener = (obs, oldVal, newVal) -> resizeCanvas();
        viewport.widthProperty().addListener(resizeListener);
        viewport.heightProperty().addListener(resizeListener);
        Platform.runLater(this::resizeCanvas);

        viewport.setFocusTraversable(true);
        Platform.runLater(() -> viewport.requestFocus());
        viewport.setOnKeyPressed(this::handleKeyPressed);
        viewport.setOnKeyReleased(this::handleKeyReleased);

        exitButton.setOnAction(e -> goToDashboard());
        dashboardButton.setOnAction(e -> goToDashboard());
        updateHUD();

        // Main game loop
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                processInput(now);
                movePetTowardPlayer();
                updateCamera();
                render(now);
            }
        };
        timer.start();
    }

    @FXML
    private void handleShowLoan() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/loan_panel.fxml"));
            Parent root = loader.load();
            main.ui.LoanController controller = loader.getController();
            Stage dialog = new Stage();
            controller.setData(currentUser, dialog, this::updateHUD);
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setScene(new Scene(root));
            dialog.setTitle("Loan Management");
            dialog.showAndWait();
            Platform.runLater(() -> gameCanvas.requestFocus());
            updateHUD();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateHUD() {
        if (coinLabel != null && currentUser != null) coinLabel.setText("" + currentUser.getCoins());
        if (gemLabel != null && currentUser != null) gemLabel.setText("" + currentUser.getGems());
        if (xpLabel != null && currentUser != null) xpLabel.setText("" + currentUser.getXp());
        if (levelLabel != null && currentUser != null) levelLabel.setText("Lvl " + currentUser.getLevel());
    }
    private void handleKeyPressed(KeyEvent e) {
        KeyCode code = e.getCode();
        if (!keysHeld.contains(code)) {
            keysHeld.add(code);
        }
        if (code == KeyCode.M && !mKeyHandled) {
            setEditingMode(!mapEditMode);
            mKeyHandled = true;
        }
        if (code == KeyCode.E && !eKeyHandled && !mapEditMode) {
            checkForCollectable();
            eKeyHandled = true;
        }
    }

    private void handleKeyReleased(KeyEvent e) {
        keysHeld.remove(e.getCode());
        if (e.getCode() == KeyCode.M) {
            mKeyHandled = false;
        }
        if (e.getCode() == KeyCode.E) {
            eKeyHandled = false;
        }
    }





    private void showTilePickerDialog() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Select Tile Type");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL);
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);

        GridPane grid = new GridPane();
        grid.setHgap(16); grid.setVgap(16); grid.setPadding(new javafx.geometry.Insets(18));
        int col = 0, row = 0;
        for (Map.Entry<String, TileMapLoader.TileType> entry : TileMapLoader.TILE_TYPES.entrySet()) {
            String code = entry.getKey();
            TileMapLoader.TileType type = entry.getValue();

            Image img = TileMapLoader.safeLoadImage(type.imagePath, code);
            javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(img);
            iv.setFitWidth(54); iv.setFitHeight(54);

            Button btn = new Button();
            btn.setGraphic(iv);
            btn.setPrefSize(64, 64);
            btn.setStyle("-fx-background-color: #f5f7f5; -fx-background-radius: 19;");
            btn.setTooltip(new Tooltip(type.name + " [" + code + "]"));

            btn.setOnAction(ev -> {
                selectedTileCode = code;
                dialog.setResult(code);
                dialog.close();
            });

            grid.add(btn, col, row);
            col++;
            if (col >= 5) { col = 0; row++; }
        }

        scroll.setContent(grid);
        dialog.getDialogPane().setContent(scroll);

        dialog.showAndWait();
    }
    private void processInput(long now) {
        int dRow = 0, dCol = 0;
        if (keysHeld.contains(KeyCode.W) || keysHeld.contains(KeyCode.UP)) dRow = -1;
        if (keysHeld.contains(KeyCode.S) || keysHeld.contains(KeyCode.DOWN)) dRow = 1;
        if (keysHeld.contains(KeyCode.A) || keysHeld.contains(KeyCode.LEFT)) dCol = -1;
        if (keysHeld.contains(KeyCode.D) || keysHeld.contains(KeyCode.RIGHT)) dCol = 1;

        // Only move if not editing the map and delay has passed!
        if (!mapEditMode && (dRow != 0 || dCol != 0)) {
            if (now - lastMoveTime > MOVE_DELAY_NS) {
                movePlayer(dRow, dCol);
                lastMoveTime = now;
            }
        }
    }

    private void movePlayer(int dRow, int dCol) {
        int newRow = playerRow + dRow;
        int newCol = playerCol + dCol;
        if (tileMap.isWalkable(newRow, newCol)) {
            playerRow = newRow;
            playerCol = newCol;
            playerPositions.put(username, new int[]{playerRow, playerCol});
            sendMyPosition(); // <--- THIS is essential!
            movePetTowardPlayer();
        }
    }


    private void movePetTowardPlayer() {
        if (Math.abs(playerRow - petRow) + Math.abs(playerCol - petCol) <= 1) return;

        int dRow = 0, dCol = 0;
        if (playerRow != petRow) {
            dRow = Integer.compare(playerRow, petRow);
        } else if (playerCol != petCol) {
            dCol = Integer.compare(playerCol, petCol);
        }

        int tryRow = petRow + dRow;
        int tryCol = petCol + dCol;

        System.out.println("Trying to move PET: dRow=" + dRow + " dCol=" + dCol);
        System.out.println("Try row move to: (" + tryRow + "," + petCol + ") walkable? " + tileMap.isWalkable(tryRow, petCol));
        System.out.println("Try col move to: (" + petRow + "," + tryCol + ") walkable? " + tileMap.isWalkable(petRow, tryCol));

        boolean moved = false;

        if (dRow != 0 && tileMap.isWalkable(tryRow, petCol) && !(tryRow == playerRow && petCol == playerCol)) {
            petRow = tryRow;
            moved = true;
        } else if (dCol != 0 && tileMap.isWalkable(petRow, tryCol) && !(petRow == playerRow && tryCol == playerCol)) {
            petCol = tryCol;
            moved = true;
        }

        if (moved && !pets.isEmpty()) {
            pets.get(0).row = petRow;
            pets.get(0).col = petCol;
        }
    }

    private void drawEntity(GraphicsContext gc, Entity entity) {
        int sx = (entity.col - cameraCol) * TILE_SIZE;
        int sy = (entity.row - cameraRow) * TILE_SIZE;
        if (entity.image != null && sx + TILE_SIZE > 0 && sy + TILE_SIZE > 0
                && sx < gameCanvas.getWidth() && sy < gameCanvas.getHeight()) {
            if (entity.type == Entity.Type.PET) {
                double scale = 0.5;
                double petSize = TILE_SIZE * scale;
                gc.drawImage(entity.image,
                        sx + (TILE_SIZE - petSize) / 2,
                        sy + (TILE_SIZE - petSize) / 2,
                        petSize,
                        petSize);
            } else {
                gc.drawImage(entity.image, sx, sy, TILE_SIZE, TILE_SIZE);
            }
        }
    }

    // ----- ALL POPUPS regain focus after closing -----
    @FXML
    private void handleShowBarn() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/barn_panel.fxml"));
            Parent root = loader.load();
            main.ui.BarnPanelController controller = loader.getController();
            controller.setBarnAndType(currentUser.getBarn(), null, currentUser, this::updateHUD);
            Stage stage = new Stage();
            stage.setTitle("Barn");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
            Platform.runLater(() -> {
                keysPressed.clear();
                isMoving = false;
                gameCanvas.requestFocus();
            });
            BarnDAO.saveBarn(currentUser);
            updateHUD();
        } catch (Exception e) {
            showError("Error opening barn panel: " + e.getMessage());
        }
    }

    @FXML
    private void handleShowInventory() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/inventory.fxml"));
            Parent root = loader.load();
            main.ui.InventoryController controller = loader.getController();
            controller.setUser(currentUser);
            Stage popup = new Stage();
            popup.setScene(new Scene(root));
            popup.initModality(Modality.APPLICATION_MODAL);
            popup.setTitle("Barn Inventory");
            popup.showAndWait();
            Platform.runLater(() -> gameCanvas.requestFocus());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBuyGems() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/gem_buy_popup.fxml"));
            Parent root = loader.load();
            main.ui.GemBuyPopupController ctrl = loader.getController();
            Stage dialog = new Stage();
            ctrl.setData(currentUser, dialog, this::updateHUD);
            dialog.setScene(new Scene(root));
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Buy Gems");
            dialog.showAndWait();
            Platform.runLater(() -> gameCanvas.requestFocus());
            updateHUD();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML private void handleAnimals() { openElementPanel(FarmElement.Type.ANIMAL); }
    @FXML private void handleFish()    { openElementPanel(FarmElement.Type.FISH); }
    @FXML private void handleFruits()  { openElementPanel(FarmElement.Type.FRUIT); }
    @FXML private void handleFlowers() { openElementPanel(FarmElement.Type.FLOWER); }
    @FXML private void handleCrops()   { openElementPanel(FarmElement.Type.CROP); }

    @FXML private void handleMarket()
    {
        showMarketPanel();
    }
    @FXML private void handleUpgradeBarn()
    {
        upgradeBarn();
    }
    private void openElementPanel(FarmElement.Type type) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/barn_panel.fxml"));
            Parent root = loader.load();
            main.ui.BarnPanelController controller = loader.getController();
            controller.setBarnAndType(currentUser.getBarn(), type, currentUser, this::updateHUD);
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setScene(new Scene(root));
            dialog.setTitle(type.name().charAt(0) + type.name().substring(1).toLowerCase() + " Inventory");
            dialog.showAndWait();
            Platform.runLater(() -> {
                keysPressed.clear();
                isMoving = false;
                gameCanvas.requestFocus();
            });
            BarnDAO.saveBarn(currentUser);
        } catch (Exception e) {
            showError("Cannot open panel for " + type.name().toLowerCase());
        }
    }


    private void showMarketPanel() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MarketPanel.fxml"));
            Parent root = loader.load();
            main.ui.MarketController controller = loader.getController();
            controller.setUser(currentUser);
            controller.setUpdateHudCallback(this::updateHUD);
            controller.setFarmGameController(this);
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setScene(new Scene(root));
            dialog.setTitle("Market - Buy & Sell");
            dialog.showAndWait();
            Platform.runLater(() -> gameCanvas.requestFocus());
            BarnDAO.saveBarn(currentUser);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Cannot open Market panel.\n" + e.getMessage());
        }
    }

    private void upgradeBarn() {
        if (currentUser == null) {
            showError("No user loaded.");
            return;
        }
        // The upgradeBarn(user) method now handles coin checking and deduction internally.
        // It returns true on success and false on failure (e.g., not enough coins).
        if (currentUser.getBarn().upgradeBarn(currentUser)) {
            // Success case
            BarnDAO.saveBarn(currentUser);
            showInfo("Barn upgraded! New capacity: " + currentUser.getBarn().getMaxCapacity());
            updateHUD();
        } else {
            // Failure case
            showError("Not enough coins to upgrade! You need " + currentUser.getBarn().getUpgradeCost() + " coins.");
        }
    }

    @FXML
    private void goToDashboard() {
        saveMapAll();
        resetState();
        BarnDAO.saveBarn(currentUser);
        Stage stage = (Stage) exitButton.getScene().getWindow();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dashboard.fxml"));
            Parent dashboardRoot = loader.load();
            main.ui.DashboardController ctrl = loader.getController();
            // Key: Always set the current user when switching scenes!
            ctrl.setUser(currentUser);
            stage.setScene(new Scene(dashboardRoot));
            stage.setTitle("Dashboard - Plow and Paw");
        } catch (Exception ex) {
            showError("Cannot go to Dashboard.");
        }
        Platform.runLater(() -> gameCanvas.requestFocus());
    }


    // ---- NO CHANGES BELOW, you already have the best logic for interact ----
    // ... All your interact, feed, collect etc logic remains as in your source ...
    // ... If you want me to re-post them, say so. ...

    // Utility focus regainer for external call (if you need it)
    public void requestViewportFocus() {
        Platform.runLater(() -> gameCanvas.requestFocus());
    }
    private void showError(String msg) {
        Platform.runLater(() -> showCustomInfo("Error", msg));
    }

    private void showInfo(String msg) {
        Platform.runLater(() -> showCustomInfo("Info", msg));
    }

    private void resetState() {
        playerRow = 15; playerCol = 20;

        // Find the first walkable tile adjacent to the player for pet spawn
        int[][] adj = {{1,0},{0,1},{-1,0},{0,-1}};
        petRow = playerRow;
        petCol = playerCol;
        for (int[] d : adj) {
            int tr = playerRow + d[0], tc = playerCol + d[1];
            if (tileMap != null && tileMap.isWalkable(tr, tc)) {
                petRow = tr; petCol = tc;
                break;
            }
        }
        cameraRow = 0; cameraCol = 0;
        keysPressed.clear();
        lastMoveTime = 0;
        lastDirRow = 0;
        isMoving = false;
        pets.clear();
        if (petAvatar != null)
            pets.add(new Entity(Entity.Type.PET, petRow, petCol, petAvatar));
    }

    // Helper for render() and startGame()
    private final Map<String, Image> imageCache = new HashMap<>();

    private Image loadImage(String path) {
        if (imageCache.containsKey(path)) return imageCache.get(path);
        InputStream is = getClass().getResourceAsStream(path);
        if (is == null) return null;
        Image img = new Image(is);
        imageCache.put(path, img);
        return img;
    }

    private String getDisplayNameForTile(String code) {
        return switch (code) {
            case "AR" -> "Apple Tree";
            case "CO" -> "Cow";
            case "CH" -> "Chicken";
            case "TU" -> "Turkey";
            case "GO" -> "Goat";
            case "GP" -> "Sheep";
            case "HR" -> "Horse";
            case "FP" -> "Fish Pond";
            case "FL" -> "Flowers";
            case "TT" -> "Tomato Field";
            case "PP" -> "Corn Field";
            case "QQ" -> "Wheat Field";
            case "XX" -> "Potato Field";
            default -> null;
        };
    }

    private String getElementTypeForTile(String code) {
        return switch (code) {
            case "AR" -> "FRUIT";
            case "CO", "CH", "TU", "GO", "GP", "HR" -> "ANIMAL";
            case "FP" -> "FISH";
            case "FL" -> "FLOWER";
            case "TT", "PP", "QQ", "XX" -> "CROP";
            default -> null;
        };
    }

    private void resizeCanvas() {
        double w = viewport.getWidth();
        double h = viewport.getHeight();
        if (w <= 0 || h <= 0) return;
        gameCanvas.setWidth(w);
        gameCanvas.setHeight(h);
        render(System.nanoTime());
    }

    private void updateCamera() {
        if (tileMap == null || gameCanvas == null) return;
        int viewTilesW = (int)Math.ceil(gameCanvas.getWidth() / TILE_SIZE);
        int viewTilesH = (int)Math.ceil(gameCanvas.getHeight() / TILE_SIZE);
        cameraRow = playerRow - viewTilesH / 2;
        cameraCol = playerCol - viewTilesW / 2;
        cameraRow = clamp(cameraRow, 0, Math.max(0, tileMap.getRows() - viewTilesH));
        cameraCol = clamp(cameraCol, 0, Math.max(0, tileMap.getCols() - viewTilesW));
    }

    private int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private void checkForCollectable() {
        int[][] adj = {{-1,0},{1,0},{0,-1},{0,1}};
        for (int[] d : adj) {
            int r = playerRow + d[0], c = playerCol + d[1];
            if (tileMap == null || !tileMap.isInBounds(r, c)) continue;
            String code = tileMap.getTileCode(r, c);
            switch (code) {
                case "CH": showItemDetailPopupFromWorld(FarmElement.Type.ANIMAL, "Chicken", r, c); return;
                case "TU": showItemDetailPopupFromWorld(FarmElement.Type.ANIMAL, "Turkey", r, c); return;
                case "CO": showItemDetailPopupFromWorld(FarmElement.Type.ANIMAL, "Cow", r, c); return;
                case "GO": showItemDetailPopupFromWorld(FarmElement.Type.ANIMAL, "Goat", r, c); return;
                case "GP": showItemDetailPopupFromWorld(FarmElement.Type.ANIMAL, "Sheep", r, c); return;
                case "AR": showItemDetailPopupFromWorld(FarmElement.Type.FRUIT, "Apple Tree", r, c); return;
                case "FP": showItemDetailPopupFromWorld(FarmElement.Type.FISH, "Fish Pond", r, c); return;
                case "FL": showItemDetailPopupFromWorld(FarmElement.Type.FLOWER, "Flowers", r, c); return;
                case "QQ": showItemDetailPopupFromWorld(FarmElement.Type.CROP, "Wheat Field", r, c); return;
                case "TT": showItemDetailPopupFromWorld(FarmElement.Type.CROP, "Tomato Field", r, c); return;
                case "XX": showItemDetailPopupFromWorld(FarmElement.Type.CROP, "Potato Field", r, c); return;
                case "PP": showItemDetailPopupFromWorld(FarmElement.Type.CROP, "Corn Field", r, c); return;
            }
        }
    }


    private void showItemDetailPopupFromWorld(
            FarmElement.Type type, String displayName, int row, int col
    ) {
        try {
            // Find or create the MapSource for this tile
            MapSource mapSrc = null;
            for (MapSource ms : sources) {
                if (ms.getRow() == row && ms.getCol() == col && ms.getName().equals(displayName)) {
                    mapSrc = ms;
                    break;
                }
            }
            if (mapSrc == null) {
                mapSrc = new MapSource(
                        currentUser.getUsername(), row, col,
                        tileMap.getTileCode(row, col), type.name(), displayName, 1
                );
                sources.add(mapSrc);
                MapSourceDAO.saveOrUpdate(mapSrc);
            }

            // Find the corresponding FarmElement in the barn
            FarmElement barnElem = currentUser.getBarn().getAllElements().stream()
                    .filter(e -> e.getType() == type && e.getName().equalsIgnoreCase(displayName))
                    .findFirst()
                    .orElse(null);

            // If the element is not in the barn, add it first. This is crucial because
            // barn.addElement creates the correct specialized subclass (e.g., FruitTree).
            if (barnElem == null) {
                currentUser.getBarn().addElement(type, displayName, 1, mapSrc.getLevel());
                BarnDAO.saveBarn(currentUser); // Save the change

                // Now, retrieve the newly created, correct instance from the barn
                barnElem = currentUser.getBarn().getAllElements().stream()
                        .filter(e -> e.getType() == type && e.getName().equalsIgnoreCase(displayName))
                        .findFirst()
                        .orElse(null);

                // This check ensures we don't proceed with a null element if something went wrong
                if (barnElem == null) {
                    showError("Failed to create and retrieve " + displayName + " from the barn.");
                    return;
                }
            }


            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/item_detail_popup.fxml"));
            Parent root = loader.load();
            main.ui.ItemDetailPopupController ctrl = loader.getController();

            Stage popupStage = new Stage();
            // Pass the correctly instantiated barnElem to the popup
            ctrl.setData(currentUser, barnElem, popupStage, () -> {
                updateHUD();
                BarnDAO.saveBarn(currentUser);
            });

            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.setScene(new Scene(root));
            popupStage.setTitle(displayName + " Details");
            popupStage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to open item detail popup.");
        }
    }


    // Helper method used inside showInteractPopup
    private int feedOrFertilizeAll(FarmElement.Type type, String name) {
        int total = 0;
        for (FarmElement elem : currentUser.getBarn().getElements(type)) {
            if (elem.getName().equalsIgnoreCase(name)) {
                elem.feedOrWaterOrFertilize();
                total++;
            }
        }
        return total;
    }

    private void showCustomInfo(String title, String msg) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/info_popup.fxml"));
            Parent root = loader.load();
            main.ui.InfoPopupController controller = loader.getController();
            controller.setTitle(title);
            controller.setMessage(msg);

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setScene(new Scene(root));
            dialog.setTitle(title);
            dialog.showAndWait();
            requestViewportFocus();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
