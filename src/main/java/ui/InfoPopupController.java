package main.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class InfoPopupController {
    @FXML private Label titleLabel;
    @FXML private Label messageLabel;
    @FXML private Button okBtn;
    @FXML private Button closeBtn;

    public void setTitle(String title) {
        titleLabel.setText(title);
    }
    public void setMessage(String msg) {
        messageLabel.setText(msg);
    }
    @FXML
    private void handleClose() {
        Stage stage = (Stage) okBtn.getScene().getWindow();
        stage.close();
    }
}