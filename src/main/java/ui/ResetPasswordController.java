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

public class ResetPasswordController {
    @FXML private TextField emailField;
    @FXML private TextField otpField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
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
    private void handleResetPassword(ActionEvent event) {
        String email = emailField.getText().trim();
        String otp = otpField.getText();
        String newPass = newPasswordField.getText();
        String confirmPass = confirmPasswordField.getText();

        if (email.isEmpty() || otp.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
            errorLabel.setText("Please fill in all fields.");
            return;
        }
        if (!otp.equals(sentOTP)) {
            errorLabel.setText("Incorrect OTP!");
            return;
        }
        if (!newPass.equals(confirmPass)) {
            errorLabel.setText("Passwords do not match!");
            return;
        }
        try (Connection conn = DatabaseConnector.getConnection()) {
            String sql = "UPDATE users SET password=? WHERE email=?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, newPass);
            ps.setString(2, email);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                errorLabel.setText("Password reset successfully!");
                // Switch back to login
                handleBackToLogin(event);
            } else {
                errorLabel.setText("Email not found.");
            }
        } catch (Exception e) {
            errorLabel.setText("Error: " + e.getMessage());
        }
    }

    @FXML
    private void handleBackToLogin(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(root, 980, 640));
            stage.setTitle("Login - Plow and Paw");
        } catch (Exception e) {
            errorLabel.setText("Unable to load login screen");
        }
    }
}
