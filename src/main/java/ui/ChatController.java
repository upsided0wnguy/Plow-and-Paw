package main.ui;

import database.DatabaseConnector;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import main.models.User;
import javafx.stage.Stage;
import javafx.scene.Node;
import utils.SessionManager;


import java.sql.*;
import java.time.format.DateTimeFormatter;

public class ChatController {

    @FXML private VBox messageList;
    @FXML private TextField messageInput;
    @FXML private ScrollPane messageScroll;

    private User currentUser;

    public void setCurrentUser(User user) {
        this.currentUser = user;
        loadChatHistory();
    }

    @FXML
    public void initialize() {
        loadChatHistory();

        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(5), e -> {
            messageList.getChildren().clear();
            loadChatHistory();
        }));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    private void loadChatHistory() {
        try (Connection conn = DatabaseConnector.getConnection()) {
            String sql = "SELECT sender, content, timestamp FROM messages ORDER BY timestamp ASC";
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String sender = rs.getString("sender");
                String content = rs.getString("content");
                String timestamp = rs.getTimestamp("timestamp")
                        .toLocalDateTime()
                        .format(DateTimeFormatter.ofPattern("HH:mm"));

                VBox bubble = new VBox();
                bubble.setSpacing(2);
                bubble.setStyle("-fx-padding: 8 12; -fx-background-radius: 14; -fx-max-width: 280;");

                Label userLabel = new Label(sender);
                userLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #333;");

                Label msgLabel = new Label(content);
                msgLabel.setWrapText(true);
                msgLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #222;");

                Label timeLabel = new Label(timestamp);
                timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: gray;");

                bubble.getChildren().addAll(userLabel, msgLabel, timeLabel);

                Label avatar = new Label(sender.substring(0, 1).toUpperCase());
                avatar.setStyle("-fx-background-color: #43cea2; -fx-text-fill: white; -fx-font-weight: bold; -fx-alignment: center; -fx-min-width: 32; -fx-min-height: 32; -fx-max-width: 32; -fx-max-height: 32; -fx-background-radius: 16;");
                avatar.setPrefSize(32, 32);

                HBox messageBox = new HBox(8);
                messageBox.setStyle("-fx-padding: 4 0 4 0;");
                messageBox.setMaxWidth(Double.MAX_VALUE);

                if (currentUser.getUsername().equals(sender)) {
                    bubble.setStyle(bubble.getStyle() + "-fx-background-color: #dcf8c6;");
                    messageBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
                    messageBox.getChildren().addAll(bubble, avatar);
                } else {
                    bubble.setStyle(bubble.getStyle() + "-fx-background-color: #ffffff;");
                    messageBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    messageBox.getChildren().addAll(avatar, bubble);
                }

                messageList.getChildren().add(messageBox);
            }

            rs.close();
            ps.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @FXML
    public void handleSendMessage() {
        String messageText = messageInput.getText().trim();
        if (messageText.isEmpty() || currentUser == null) return;

        try (Connection conn = DatabaseConnector.getConnection()) {
            String sql = "INSERT INTO messages (sender, content) VALUES (?, ?)";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, currentUser.getUsername());
            ps.setString(2, messageText);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        messageInput.clear();
        messageList.getChildren().clear();  // refresh list
        loadChatHistory();
    }

    // --------- ADD THIS -----------
    @FXML
    private void handleClose(javafx.event.ActionEvent event) {
        ((Stage) ((Node) event.getSource()).getScene().getWindow()).close();
    }
}
