package main.ui;

import database.DatabaseConnector;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import utils.SessionManager;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.prefs.Preferences;

public class SettingsController {
    @FXML private CheckBox musicCheck;
    @FXML private Slider volumeSlider;
    @FXML private CheckBox fullscreenCheck;
    @FXML private ChoiceBox<String> themeChoice;

    @FXML
    public void initialize() {
        Preferences prefs = Preferences.userNodeForPackage(SettingsController.class);
        String savedTheme = prefs.get("theme", "Light");
        boolean isFullscreen = prefs.getBoolean("fullscreen", false);
        boolean musicOn = prefs.getBoolean("music", true);
        double volume = prefs.getDouble("volume", 60.0);

        themeChoice.getItems().addAll("Light", "Dark");
        themeChoice.setValue(savedTheme);
        fullscreenCheck.setSelected(isFullscreen);
        musicCheck.setSelected(musicOn);
        volumeSlider.setValue(volume);
    }

    @FXML
    public void handleSave() {
        String username = utils.SessionManager.getSavedUsername(); // now always available, static
        boolean musicEnabled = musicCheck.isSelected();
        double volume = volumeSlider.getValue();
        boolean fullscreen = fullscreenCheck.isSelected();
        String theme = themeChoice.getValue();

        try (Connection conn = DatabaseConnector.getConnection()) {
            String sql = "UPDATE settings SET music_enabled=?, volume=?, fullscreen=?, theme=? WHERE username=?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setBoolean(1, musicEnabled);
            ps.setDouble(2, volume);
            ps.setBoolean(3, fullscreen);
            ps.setString(4, theme);
            ps.setString(5, username != null ? username.toLowerCase() : null); // always lowercase
            ps.executeUpdate();
            ps.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Stage stage = (Stage) musicCheck.getScene().getWindow();
        stage.setFullScreen(fullscreen);

        Scene scene = stage.getScene();
        scene.getStylesheets().clear();
        scene.getStylesheets().add(getClass().getResource("/css/" + theme.toLowerCase() + ".css").toExternalForm());

        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Settings applied and saved!", ButtonType.OK);
        alert.showAndWait();
    }

    @FXML
    public void handleChangePassword() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/change_password.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Change Password");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleClose(javafx.event.ActionEvent event) {
        ((Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow()).close();
    }
}
