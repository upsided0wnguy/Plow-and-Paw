package main.ui;

import database.DatabaseConnector;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import utils.SessionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ChangePasswordController {
    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label statusLabel;

    private String currentUsername;

    @FXML
    public void initialize() {
        // --- Always use singleton for session ---
        currentUsername = SessionManager.getSavedUsername();
        statusLabel.setText(""); // Clear on open
        statusLabel.setStyle("-fx-text-fill: #e63946; -fx-font-family: 'Poppins'; -fx-font-size: 15px;");
    }

    @FXML
    public void handleUpdatePassword() {
        String currentPass = currentPasswordField.getText();
        String newPass = newPasswordField.getText();
        String confirmPass = confirmPasswordField.getText();

        if (currentPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
            statusLabel.setText("⚠ Please fill in all fields.");
            return;
        }

        if (!newPass.equals(confirmPass)) {
            statusLabel.setText("❌ New passwords do not match.");
            return;
        }

        javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<Void>() {
            @Override
            protected Void call() {
                try (Connection conn = DatabaseConnector.getConnection()) {
                    PreparedStatement ps = conn.prepareStatement(
                            "SELECT * FROM users WHERE username = ? AND password = ?"
                    );
                    ps.setString(1, currentUsername);
                    ps.setString(2, currentPass);
                    ResultSet rs = ps.executeQuery();

                    if (rs.next()) {
                        PreparedStatement update = conn.prepareStatement(
                                "UPDATE users SET password = ? WHERE username = ?"
                        );
                        update.setString(1, newPass);
                        update.setString(2, currentUsername);
                        update.executeUpdate();
                        update.close();
                        javafx.application.Platform.runLater(() -> {
                            statusLabel.setStyle("-fx-text-fill: green;");
                            statusLabel.setText("✅ Password updated successfully.");
                        });
                    } else {
                        javafx.application.Platform.runLater(() -> {
                            statusLabel.setStyle("-fx-text-fill: red;");
                            statusLabel.setText("❌ Current password incorrect.");
                        });
                    }
                    rs.close();
                    ps.close();
                } catch (Exception e) {
                    javafx.application.Platform.runLater(() -> {
                        statusLabel.setStyle("-fx-text-fill: red;");
                        statusLabel.setText("❌ Error updating password.");
                    });
                    e.printStackTrace();
                }
                return null;
            }
        };

        new Thread(task).start();
    }

    @FXML
    private void handleClose(javafx.event.ActionEvent event) {
        ((Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow()).close();
    }
}
