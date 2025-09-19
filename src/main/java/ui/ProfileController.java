package main.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.event.ActionEvent;
import main.models.User;

public class ProfileController {

    @FXML private Label nameLabel;
    @FXML private Label usernameLabel;
    @FXML private Label levelLabel;
    @FXML private Label coinsLabel;
    @FXML private Label gemsLabel;
    @FXML private Label xpLabel;
    @FXML private Label achievementsLabel;

    private User user;

    public void setUser(User user) {
        this.user = user;
        if (user == null) return;
        nameLabel.setText("Farmer Name: " + user.getFullName());
        usernameLabel.setText("Username: " + user.getUsername());
        levelLabel.setText(String.valueOf(user.getLevel()));
        coinsLabel.setText(String.valueOf(user.getCoins()));
        gemsLabel.setText(String.valueOf(user.getGems()));
        xpLabel.setText(String.valueOf(user.getXp()));
        achievementsLabel.setText("0"); // You can later calculate from xp or add your logic
    }

    @FXML
    public void handleClose(ActionEvent event) {
        Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        stage.close();
    }
}