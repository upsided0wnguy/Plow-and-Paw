package main.ui;

import javafx.fxml.FXML;
import javafx.scene.image.ImageView;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import javafx.scene.input.MouseEvent;

public class PetSelectionController {
    @FXML private ImageView dogImage, catImage;
    @FXML private Button chooseBtn;
    private String selectedPet = "dog";

    @FXML
    public void initialize() {
        dogImage.setOnMouseClicked(e -> selectPet("dog"));
        catImage.setOnMouseClicked(e -> selectPet("cat"));
    }

    private void selectPet(String pet) {
        selectedPet = pet;
        // Highlight selection, etc.
        dogImage.setStyle(selectedPet.equals("dog") ? "-fx-effect: dropshadow(gaussian,#43cea2,12,0.7,0,2);" : "");
        catImage.setStyle(selectedPet.equals("cat") ? "-fx-effect: dropshadow(gaussian,#fa709a,12,0.7,0,2);" : "");
    }

    @FXML
    private void handleChoose() {
        // Save chosen pet to user profile, etc.
        System.out.println("Selected pet: " + selectedPet);
        Stage stage = (Stage)chooseBtn.getScene().getWindow();
        stage.close();
    }
}
