package main.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class GameModeSelectionController {

    private main.models.User currentUser;

    public void setCurrentUser(main.models.User user) {
        this.currentUser = user;
    }

    @FXML
    private Button soloBtn;

    @FXML
    private Button multiplayerBtn;

    private Runnable onSolo;
    private Runnable onMultiplayer;
    @FXML private javafx.scene.image.ImageView coinIcon;
    @FXML private javafx.scene.image.ImageView gemIcon;
    @FXML private javafx.scene.image.ImageView xpIcon;
    @FXML private javafx.scene.control.Label coinLabel;
    @FXML private javafx.scene.control.Label gemLabel;
    @FXML private javafx.scene.control.Label xpLabel;


    @FXML
    public void initialize() {
        soloBtn.setOnAction(e -> {
            if (onSolo != null) onSolo.run();
            close();
        });
        multiplayerBtn.setOnAction(e -> {
            if (onMultiplayer != null) onMultiplayer.run();
            close();
        });
        if (coinIcon != null)
            coinIcon.setImage(new Image(getClass().getResourceAsStream("/images/ui/coin.png")));
        if (gemIcon != null)
            gemIcon.setImage(new Image(getClass().getResourceAsStream("/images/ui/gem.png")));
        if (xpIcon != null)
            xpIcon.setImage(new Image(getClass().getResourceAsStream("/images/ui/xp.png")));

    }

    public void setOnSolo(Runnable action) {
        this.onSolo = action;
    }

    public void setOnMultiplayer(Runnable action) {
        this.onMultiplayer = action;
    }

    private void close() {
        Stage stage = (Stage) soloBtn.getScene().getWindow();
        stage.close();
    }
}