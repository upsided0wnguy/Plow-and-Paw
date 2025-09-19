package main.ui;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import main.models.User;
import database.BarnDAO;

public class GemBuyPopupController {
    @FXML private Spinner<Integer> gemSpinner;
    @FXML private Label coinCostLabel;
    @FXML private Button buyBtn;
    @FXML private Button cancelBtn;
    @FXML private Label feedbackLabel;

    private User user;
    private Stage dialog;
    private Runnable updateHUD;
    private static final int GEM_PRICE = 60; // 1 gem = 60 coins

    /**
     * Set user, stage, and HUD callback for this popup.
     */
    public void setData(User user, Stage dialog, Runnable updateHUD) {
        this.user = user;
        this.dialog = dialog;
        this.updateHUD = updateHUD;

        // Spinner setup: 1-50 gems, default 1
        gemSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 50, 1));
        gemSpinner.valueProperty().addListener((obs, oldVal, newVal) -> updatePrice());
        updatePrice();

        buyBtn.setOnAction(e -> handleBuy());
        cancelBtn.setOnAction(e -> dialog.close());
        feedbackLabel.setText("");
    }

    /**
     * Update the coin cost label based on spinner value.
     */
    private void updatePrice() {
        int nGems = gemSpinner.getValue();
        coinCostLabel.setText(String.valueOf(nGems * GEM_PRICE));
    }

    /**
     * Handles buying gems: checks balance, updates User, calls HUD update, closes popup.
     */
    private void handleBuy() {
        int nGems = gemSpinner.getValue();
        int cost = nGems * GEM_PRICE;

        if (user.getCoins() < cost) {
            feedbackLabel.setText("Not enough coins!");
            feedbackLabel.setStyle("-fx-text-fill: #e74c3c;");
            return;
        }

        user.setCoins(user.getCoins() - cost);
        user.setGems(user.getGems() + nGems);
        BarnDAO.saveBarn(user); // Persist changes
        feedbackLabel.setText("Purchased " + nGems + " gems!");
        feedbackLabel.setStyle("-fx-text-fill: #27ae60;");

        if (updateHUD != null) updateHUD.run();
        // Optional: Close after short delay for UX, or close immediately:
        dialog.close();
    }
}