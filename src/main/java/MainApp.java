package main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            System.out.println("[DEBUG] Trying to load login.fxml...");
            Parent loginRoot = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
            Scene scene = new Scene(loginRoot, 900, 600);
            primaryStage.setScene(scene);
            primaryStage.setTitle("Login - Plow and Paw");
            primaryStage.show();
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to load login.fxml:");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
