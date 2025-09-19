package main.ui;

import database.DatabaseConnector;
import email.EmailSender;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Random;

public class SignupController {
    @FXML private TextField nameField;
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField otpField;
    @FXML private Label errorLabel;

    private String sentOTP;

    @FXML
    private void handleSendOtp(ActionEvent event) {
        String email = emailField.getText().trim();
        if (email.isEmpty() || !email.contains("@")) {
            errorLabel.setText("Enter a valid email address!");
            return;
        }
        sentOTP = String.format("%06d", new Random().nextInt(999999));
        boolean success = EmailSender.sendOTP(email, sentOTP);
        errorLabel.setText(success ? "OTP sent to your email!" : "Failed to send OTP. Try again.");
    }

    @FXML
    private void handleRegister(ActionEvent event) {
        String fullName = nameField.getText().trim();
        String username = usernameField.getText().trim().toLowerCase();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String otp = otpField.getText();

        if (fullName.isEmpty() || username.isEmpty() || email.isEmpty() ||
                password.isEmpty() || confirmPassword.isEmpty() || otp.isEmpty()) {
            errorLabel.setText("Please fill in all fields.");
            return;
        }
        if (!password.equals(confirmPassword)) {
            errorLabel.setText("Passwords do not match!");
            return;
        }
        if (sentOTP == null || !otp.equals(sentOTP)) {
            errorLabel.setText("Incorrect OTP!");
            return;
        }
        try (Connection conn = DatabaseConnector.getConnection()) {
            // Check if username or email is already taken
            String checkSql = "SELECT COUNT(*) FROM users WHERE username=? OR email=?";
            try (PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
                checkPs.setString(1, username);
                checkPs.setString(2, email);
                var rs = checkPs.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    errorLabel.setText("Username or email already exists.");
                    return;
                }
            }

            String sql = "INSERT INTO users (name, username, password, email, level, coins, gems, xp, barn_level) VALUES (?, ?, ?, ?, 1, 100, 0, 0, 1)";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, fullName);
            ps.setString(2, username);
            ps.setString(3, password);
            ps.setString(4, email);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                // Also create user_progress entry
                String progSql = "INSERT INTO user_progress (username, coins, gems, xp, level) VALUES (?, 100, 0, 0, 1)";
                try (PreparedStatement progPs = conn.prepareStatement(progSql)) {
                    progPs.setString(1, username);
                    progPs.executeUpdate();
                }
                errorLabel.setText("Signup successful! Redirecting to login...");
                // Return to login after short pause
                javafx.application.Platform.runLater(() -> {
                    try {
                        Thread.sleep(800); // brief pause for user to see message
                    } catch (InterruptedException ignored) {}
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
                        Parent root = loader.load();
                        Stage stage = (Stage) nameField.getScene().getWindow();
                        stage.setScene(new Scene(root, 980, 640));
                        stage.setTitle("Login - Plow and Paw");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            } else {
                errorLabel.setText("Signup failed. Try another username.");
            }
        } catch (Exception e) {
            errorLabel.setText("Error: " + e.getMessage());
        }
    }

    @FXML
    private void handleBack(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) nameField.getScene().getWindow();
            stage.setScene(new Scene(root, 980, 640));
            stage.setTitle("Login - Plow and Paw");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}