package main.ui;

import database.BarnDAO;
import database.DatabaseConnector;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.ArcType;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import main.models.User;
import utils.SessionManager;

import java.io.IOException;
import java.sql.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class DashboardController {
    @FXML private Label welcomeLabel;
    @FXML private Label playerNameLabel;
    @FXML private Label levelLabel;
    @FXML private Label coinsLabel;
    @FXML private Label gemsLabel;
    @FXML private Label xpLabel;
    @FXML private Label cropsLabel;
    @FXML private Label animalsLabel;
    @FXML private Label fruitsLabel;
    @FXML private Label eggsLabel;
    @FXML private Label rewardStatusLabel;
    @FXML private Button profileBtn;
    @FXML private AnchorPane rootPane;
    @FXML private Canvas animatedBg;
    @FXML private VBox messageList;
    @FXML private TextField messageInput;
    @FXML private Button messageBtn;
    @FXML private Button settingsBtn;
    @FXML private Button dailyRewardBtn;
    @FXML private Button marketButton;
    @FXML private Button barnButton;
    @FXML private Button myFarmButton;
    private User currentUser;
    private main.network.NetworkClient networkClient;

    @FXML
    private void initialize() {
        animatedBg.widthProperty().bind(rootPane.widthProperty());
        animatedBg.heightProperty().bind(rootPane.heightProperty());
        refreshUserData();
        if (settingsBtn != null) settingsBtn.setOnAction(e -> openSettings());
        if (profileBtn != null)  profileBtn.setOnAction(e -> openProfile());
        if (messageBtn != null)  messageBtn.setOnAction(e -> openMessagePopup());
        if (dailyRewardBtn != null) dailyRewardBtn.setOnAction(e -> handleCollectDailyReward());
        if (animatedBg != null) animateFarmBackground();

        if (messageList != null) {
            loadMessagesFromDatabase();
            Timeline refreshTimeline = new Timeline(
                    new KeyFrame(Duration.seconds(5), e -> {
                        messageList.getChildren().clear();
                        loadMessagesFromDatabase();
                    })
            );
            refreshTimeline.setCycleCount(Timeline.INDEFINITE);
            refreshTimeline.play();
        }

        // --- Bottom Dock Buttons ---
        if (marketButton != null) {
            marketButton.setOnAction(e -> openMarketPopup());
        }
        if (barnButton != null) {
            barnButton.setOnAction(e -> openBarnPopup());
        }
        if (myFarmButton != null) {
            myFarmButton.setOnAction(e -> showFarmStatsPopup());
        }
    }

    // ----------- Farm Dock Button Logic -----------
    private void openMarketPopup() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MarketPanel.fxml"));
            Parent root = loader.load();
            main.ui.MarketController controller = loader.getController();
            controller.setUser(currentUser);
            controller.setUpdateHudCallback(this::refreshUserData);
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setScene(new Scene(root));
            dialog.setTitle("Market - Buy & Sell");
            dialog.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Cannot open Market panel.");
        }
    }
    private void openBarnPopup() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/barn_panel.fxml"));
            Parent root = loader.load();
            main.ui.BarnPanelController controller = loader.getController();
            controller.setBarnAndType(currentUser.getBarn(), null, currentUser, this::refreshUserData);
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setScene(new Scene(root));
            dialog.setTitle("Barn Inventory");
            dialog.showAndWait();
            BarnDAO.saveBarn(currentUser);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Cannot open Barn panel.");
        }
    }
    private void showFarmStatsPopup() {
        if (currentUser == null || currentUser.getBarn() == null) {
            showError("User/Barn not loaded!");
            return;
        }
        int crops = currentUser.getBarn().getElements(main.models.FarmElement.Type.CROP).size();
        int animals = currentUser.getBarn().getElements(main.models.FarmElement.Type.ANIMAL).size();
        int fruits = currentUser.getBarn().getElements(main.models.FarmElement.Type.FRUIT).size();
        int flowers = currentUser.getBarn().getElements(main.models.FarmElement.Type.FLOWER).size();
        int fish = currentUser.getBarn().getElements(main.models.FarmElement.Type.FISH).size();

        String msg = "Your Farm:\n"
                + crops + " Crops\n"
                + animals + " Animals\n"
                + fruits + " Fruits\n"
                + flowers + " Flowers\n"
                + fish + " Fish";
        showInfo(msg);
    }

    // ----------- All Previous Logic Below (unchanged) -----------

    public void animateFarmBackground() {
        GraphicsContext gc = animatedBg.getGraphicsContext2D();
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.millis(30), e -> {
                    double t = (System.currentTimeMillis() % 50000) / 1000.0;

                    gc.clearRect(0, 0, animatedBg.getWidth(), animatedBg.getHeight());
                    gc.setFill(new LinearGradient(0,0,0,1,true, CycleMethod.NO_CYCLE,
                            new Stop(0, Color.web("#fff9d6")),
                            new Stop(1, Color.web("#fff1ae"))));
                    gc.fillRect(0,0,1600,1000);
                    gc.setFill(Color.web("#ffe476"));
                    gc.fillOval(200 + Math.sin(t/5)*20, 140 + Math.cos(t/7)*10, 100, 100);
                    gc.setGlobalAlpha(0.85);
                    gc.setFill(Color.web("#fff"));
                    for(int i=0; i<3; ++i) {
                        double cx = 200+400*i + (t*35 + i*100)%1600;
                        gc.fillOval(cx, 100+20*i, 120, 50);
                        gc.fillOval(cx+40, 110+15*i, 80, 38);
                    }
                    gc.setGlobalAlpha(1.0);
                    gc.setFill(Color.web("#cffc7b"));
                    gc.fillArc(-400,300,1800,800,0,180, ArcType.ROUND);
                    gc.setFill(Color.web("#b0e86e"));
                    gc.fillArc(0,400,1800,900,0,180,ArcType.ROUND);
                    drawBarn(gc, 1200, 400, 1);
                    gc.setStroke(Color.web("#aa7d2f"));
                    gc.setLineWidth(10);
                    gc.strokeLine(310, 420, 310, 520);
                    gc.strokeLine(300, 520, 320, 520);
                    double angle = (t*120)%360;
                    gc.setStroke(Color.web("#ffc446"));
                    gc.setLineWidth(5);
                    for (int i=0; i<8; ++i) {
                        double a = Math.toRadians(angle + i*45);
                        gc.strokeLine(310,430, 310+Math.cos(a)*40, 430+Math.sin(a)*40);
                    }
                    gc.setFill(Color.web("#ffe476"));
                    gc.fillOval(305, 425, 12, 12);
                    for(int i=0; i<14; ++i) {
                        double gx = 250+i*80;
                        double gy = 900+Math.sin(t*1.4 + i)*18;
                        gc.save();
                        gc.translate(gx,gy);
                        gc.rotate(Math.sin(t*1.8 + i)*9);
                        gc.setFill(Color.web("#97cc59"));
                        gc.fillRect(-8,0,16,56);
                        gc.restore();
                    }
                })
        );
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void drawBarn(GraphicsContext gc, double x, double y, double scale) {
        gc.save();
        gc.translate(x,y);
        gc.scale(scale,scale);
        gc.setFill(Color.web("#e9524f"));
        gc.fillRect(0,0,110,75);
        gc.setFill(Color.web("#fff"));
        gc.fillRect(10,40,90,32);
        gc.setStroke(Color.web("#fff"));
        gc.setLineWidth(7);
        gc.strokeRect(0,0,110,75);
        gc.strokeRect(10,40,90,32);
        gc.setFill(Color.web("#f5e4d8"));
        gc.fillPolygon(new double[]{-8,55,118}, new double[]{0,-35,0}, 3);
        gc.restore();
    }

    private void showBlankUI() {
        if (welcomeLabel != null) welcomeLabel.setText("Welcome!");
        if (playerNameLabel != null) playerNameLabel.setText("Farmer Name (username)");
        if (levelLabel != null) levelLabel.setText("Level 1");
        if (coinsLabel != null) coinsLabel.setText("0");
        if (gemsLabel != null) gemsLabel.setText("0");
        if (xpLabel != null) xpLabel.setText("0");
        if (cropsLabel != null) cropsLabel.setText("Crops: 0");
        if (animalsLabel != null) animalsLabel.setText("Animals: 0");
        if (fruitsLabel != null) fruitsLabel.setText("Fruits: 0");
        if (eggsLabel != null) eggsLabel.setText("Eggs: 0");
        if (rewardStatusLabel != null) rewardStatusLabel.setText("");
    }

    private void refreshUserData() {
        this.currentUser = SessionManager.getCurrentUser();
        if (currentUser != null) {
            if (welcomeLabel != null) welcomeLabel.setText("Welcome, " + currentUser.getFullName() + "!");
            if (playerNameLabel != null) playerNameLabel.setText("Farmer " + currentUser.getFullName() + " (" + currentUser.getUsername() + ")");
            if (levelLabel != null) levelLabel.setText("Level " + currentUser.getLevel());
            if (xpLabel != null) xpLabel.setText("" + currentUser.getXp());
            try (Connection conn = DatabaseConnector.getConnection()) {
                PreparedStatement ps = conn.prepareStatement(
                        "SELECT coins, gems FROM users WHERE username = ?"
                );
                ps.setString(1, currentUser.getUsername());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    if (coinsLabel != null) coinsLabel.setText("" + rs.getInt("coins"));
                    if (gemsLabel != null) gemsLabel.setText("" + rs.getInt("gems"));
                }
            } catch (Exception e) { e.printStackTrace(); }
            updateFarmStats();
        } else {
            showBlankUI();
        }
    }

    private void updateFarmStats() {
        if (currentUser == null) return;
        try (Connection conn = DatabaseConnector.getConnection()) {
            cropsLabel.setText("Crops: " + getTotalByType(conn, "CROP"));
            animalsLabel.setText("Animals: " + getTotalByType(conn, "ANIMAL"));
            fruitsLabel.setText("Fruits: " + getTotalByType(conn, "FRUIT"));
            eggsLabel.setText("Eggs: " + getTotalByProduct(conn, "Egg"));
        } catch (Exception e) { e.printStackTrace(); }
    }
    private int getTotalByType(Connection conn, String type) {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT SUM(amount) FROM user_barn WHERE username = ? AND type = ?"
        )) {
            ps.setString(1, currentUser.getUsername());
            ps.setString(2, type);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (Exception ignored) {}
        return 0;
    }

    private int getTotalByProduct(Connection conn, String productName) {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT SUM(amount) FROM user_barn WHERE username = ? AND name = ?"
        )) {
            ps.setString(1, currentUser.getUsername());
            ps.setString(2, productName);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (Exception ignored) {}
        return 0;
    }

    @FXML private void handleStartGame(ActionEvent event) { showGameModeSelection(); }
    private void showGameModeSelection() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/game_mode_selection.fxml"));
            Parent root = loader.load();
            main.ui.GameModeSelectionController controller = loader.getController();
            controller.setCurrentUser(currentUser);
            controller.setOnSolo(this::startSoloGame);
            controller.setOnMultiplayer(this::showMultiplayerLobby);

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setScene(new Scene(root));
            dialog.setTitle("Select Game Mode");
            dialog.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setUser(User user) {
        this.currentUser = user;
        SessionManager.setCurrentUser(user);
        refreshUserData();
        if (networkClient == null && currentUser != null) {
            networkClient = new main.network.NetworkClient();
            try {
                networkClient.connect("localhost", 44444, currentUser.getUsername(), msg -> {});
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private void showMultiplayerLobby() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/multiplayer_lobby.fxml"));
            Parent root = loader.load();
            main.ui.MultiplayerLobbyController controller = loader.getController();
            controller.setUserAndNetwork(currentUser, networkClient);
            controller.setOnStartGame(players -> loadFarmGameScene(currentUser, players));
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setScene(new Scene(root));
            dialog.setTitle("Multiplayer Lobby");
            dialog.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void loadFarmGameScene(User user, List<String> players) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/farming_game.fxml"));
            Parent root = loader.load();
            main.ui.FarmGameController controller = loader.getController();
            controller.setCurrentUser(user);
            controller.setPlayers(players);
            if (players.size() == 1) {
                controller.startGame(null);
            } else {
                controller.startGame(players.get(0));
            }
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Plow and Paw - Farm");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void startSoloGame() {
        List<String> players = new ArrayList<>();
        players.add(currentUser.getUsername());
        loadFarmGameScene(currentUser, players);
    }

    @FXML private void handleOpenSettings(ActionEvent event) { openSettings(); }
    @FXML private void handleMessageBtn(ActionEvent event)   { openMessagePopup(); }
    @FXML private void handleProfileBtn(ActionEvent event)   { openProfile(); }
    @FXML
    private void handleLogout(ActionEvent event) {
        if (networkClient != null) {
            networkClient.disconnect();
            networkClient = null;
        }
        SessionManager.clearSession();
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 900, 600));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void openSettings() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/settings.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Settings");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void openMessagePopup() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/chat_popup.fxml"));
            Parent root = loader.load();
            main.ui.ChatController controller = loader.getController();
            controller.setCurrentUser(currentUser);
            Stage stage = new Stage();
            stage.setTitle("Global Chat");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void openProfile() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/profile_popup.fxml"));
            Parent root = loader.load();
            main.ui.ProfileController controller = loader.getController();
            controller.setUser(utils.SessionManager.getCurrentUser());
            Stage stage = new Stage();
            stage.setTitle("Your Profile");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void loadMessagesFromDatabase() {
        try (Connection conn = DatabaseConnector.getConnection()) {
            String sql = "SELECT * FROM messages ORDER BY timestamp ASC";
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String sender = rs.getString("sender");
                String content = rs.getString("content");
                String timestamp = rs.getTimestamp("timestamp")
                        .toLocalDateTime()
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                Label messageLabel = new Label("[" + timestamp + "] " + sender + ": " + content);
                messageLabel.setWrapText(true);
                messageList.getChildren().add(messageLabel);
            }
            rs.close();
            ps.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @FXML
    private void handleSendMessage() {
        String messageText = messageInput.getText().trim();
        if (messageText.isEmpty() || currentUser == null) return;
        try (Connection conn = DatabaseConnector.getConnection()) {
            String sql = "INSERT INTO messages (sender, content) VALUES (?, ?)";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, currentUser.getUsername());
            ps.setString(2, messageText);
            ps.executeUpdate();
            ps.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        messageInput.clear();
        messageList.getChildren().clear();
        loadMessagesFromDatabase();
    }

    public User getCurrentUser()
    {
        return this.currentUser;
    }

    @FXML
    private void handleCollectDailyReward() {
        if (currentUser == null) return;
        try (Connection conn = database.DatabaseConnector.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT last_daily_claim FROM users WHERE username = ?"
            );
            ps.setString(1, currentUser.getUsername());
            ResultSet rs = ps.executeQuery();
            boolean canClaim = false;
            if (rs.next()) {
                java.sql.Timestamp lastClaim = rs.getTimestamp("last_daily_claim");
                if (lastClaim == null ||
                        System.currentTimeMillis() - lastClaim.getTime() >= 24 * 60 * 60 * 1000) {
                    canClaim = true;
                }
            }
            if (canClaim) {
                PreparedStatement upd = conn.prepareStatement(
                        "UPDATE users SET coins = coins + 100, gems = gems + 2, last_daily_claim = NOW() WHERE username = ?"
                );
                upd.setString(1, currentUser.getUsername());
                upd.executeUpdate();
                if (rewardStatusLabel != null)
                    rewardStatusLabel.setText("Collected! Come back tomorrow!");
            } else {
                if (rewardStatusLabel != null)
                    rewardStatusLabel.setText("Reward already claimed today!");
            }
            refreshUserData();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg);
        alert.showAndWait();
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void showInfo(String msg) {
        showCustomInfo("Message", msg);
    }
}