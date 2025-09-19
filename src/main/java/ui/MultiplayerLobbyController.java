package main.ui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import main.models.User;
import main.network.NetworkClient;
import java.io.IOException;
import java.util.*;

public class MultiplayerLobbyController {
    @FXML private ListView<String> playerList;
    @FXML private Button inviteButton;
    @FXML private Button startGameButton;
    @FXML private Label statusLabel;

    private NetworkClient networkClient;
    private User currentUser;
    private String selectedPlayer = null;
    private Set<String> acceptedPlayers = new HashSet<>();
    private String currentUsername;
    private java.util.function.Consumer<List<String>> onStartGame;

    public void setUserAndNetwork(User user, NetworkClient client) {
        this.currentUser = user;
        this.networkClient = client;
        try {
            client.connect("localhost", 44444, user.getUsername(), msg -> handleMessageOnFXThread(msg));
            client.send("LOGIN:" + user.getUsername());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setCurrentUsername(String username) {
        this.currentUsername = username;
    }

    public void setOnStartGame(java.util.function.Consumer<List<String>> callback) {
        this.onStartGame = callback;
    }

    @FXML
    public void initialize() {
        inviteButton.setOnAction(e -> {
            String target = playerList.getSelectionModel().getSelectedItem();
            if (target != null && networkClient != null && currentUser != null) {
                networkClient.send("INVITE:" + currentUser.getUsername() + ":" + target);
                statusLabel.setText("Invitation sent to " + target);
            }
        });

        startGameButton.setOnAction(e -> {
            if (networkClient != null && currentUser != null) {
                networkClient.send("START_GAME:" + currentUser.getUsername());
                statusLabel.setText("Starting game as host...");
            }
        });

        playerList.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> selectedPlayer = newV);
    }

    // Make sure all message handling runs on JavaFX thread
    private void handleMessageOnFXThread(String msg) {
        if (Platform.isFxApplicationThread()) {
            handleMessage(msg);
        } else {
            Platform.runLater(() -> handleMessage(msg));
        }
    }

    private void handleMessage(String msg) {
        if (msg.startsWith("PLAYERS:")) {
            String[] users = msg.substring(8).split(",");
            Set<String> unique = new LinkedHashSet<>();
            for (String u : users) {
                String s = u.trim();
                if (!s.isEmpty()) unique.add(s);
            }
            playerList.getItems().setAll(unique);
        } else if (msg.startsWith("START_GAME:")) {
            String hostUsername = msg.substring(11).trim();
            launchMultiplayerGame(hostUsername);
        } else if (msg.startsWith("INVITED_BY:")) {
            String from = msg.substring(11);
            boolean accepted = showInviteAcceptDialog(from);
            if (accepted) {
                networkClient.send("INVITE_ACCEPT:" + from + ":" + currentUser.getUsername());
                statusLabel.setText("Accepted invitation from " + from);
            } else {
                statusLabel.setText("Declined invitation from " + from);
            }
        } else if (msg.startsWith("INVITE_ACCEPTED:")) {
            String who = msg.substring(16);
            acceptedPlayers.add(who);
            statusLabel.setText(who + " accepted your invitation!");
        }
    }

    private boolean showInviteAcceptDialog(String inviter) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                inviter + " has invited you to play. Accept?",
                ButtonType.YES, ButtonType.NO);
        alert.setTitle("Game Invite");
        alert.setHeaderText("Multiplayer Invitation");
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.YES;
    }

    private void launchMultiplayerGame(String hostUsername) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/farming_game.fxml"));
            Parent root = loader.load();
            main.ui.FarmGameController farmGameController = loader.getController();

            farmGameController.setCurrentUser(currentUser);
            farmGameController.startMultiplayer(currentUser.getUsername(), networkClient);
            farmGameController.startGame(hostUsername);

            Stage stage = (Stage) playerList.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}