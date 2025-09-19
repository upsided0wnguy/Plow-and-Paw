package main.ui;

import database.DatabaseConnector;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import main.models.User;
import utils.SessionManager;
import database.BarnDAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel; // <-- corrected to match FXML

    @FXML
    private void handleLogin(ActionEvent event) {
        String username = usernameField.getText().trim().toLowerCase();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please fill in all fields.");
            return;
        }
        try (Connection conn = DatabaseConnector.getConnection()) {
            String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, username);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String fullName = rs.getString("name");
                int level = rs.getInt("level");
                int coins = rs.getInt("coins");
                int gems = rs.getInt("gems");
                int xp = rs.getInt("xp");
                int barnLevel = rs.getInt("barn_level");
                User user = new User(fullName, username, level, coins, gems, xp, null);
                SessionManager.setCurrentUser(user);
                BarnDAO.loadBarn(user);
                errorLabel.setText("Login successful!");

                // Switch to dashboard
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dashboard.fxml"));
                Parent root = loader.load();
                main.ui.DashboardController dashboardController = loader.getController();
                dashboardController.setUser(user);

                Stage stage = (Stage) usernameField.getScene().getWindow();
                stage.setScene(new Scene(root, 1100, 750));
                stage.setTitle("Dashboard - Plow and Paw");
            } else {
                errorLabel.setText("Invalid username or password.");
            }
        } catch (Exception e) {
            errorLabel.setText("Error: " + e.getMessage());
        }
    }

    @FXML
    private void handleForgotPassword(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/reset_password.fxml"));
            Stage stage = (Stage) usernameField.getScene().getWindow(); // get current window
            stage.setScene(new Scene(root, 900, 600)); // use your app size
            stage.setTitle("Reset Password - Plow and Paw");
        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText("Unable to load reset password screen");
        }
    }

    @FXML
    private void handleSignup(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/signup.fxml"));
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(root, 980, 640));
            stage.setTitle("Signup - Plow and Paw");
        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText("Unable to load signup page");
        }
    }
}