package main.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class InteractPopupController {

    @FXML private Label titleLabel;
    @FXML private Label descLabel;
    @FXML private Button collectButton;
    @FXML private Button feedButton;
    @FXML private Button upgradeButton;
    @FXML private Button closeButton;
    @FXML private Label feedbackLabel;
    @FXML private Label upgradeCostLabel;

    private Stage stage;
    private Runnable onCollect, onFeed, onUpgrade;
    private int upgradeCost = 0;
    private int currentLevel = 1;
    private java.util.function.Supplier<Integer> coinSupplier; // For live coin checking
    private java.util.function.IntSupplier levelSupplier;      // For getting current level after upgrade

    @FXML
    private void initialize() {
        collectButton.setOnAction(e -> {
            if (onCollect != null) onCollect.run();
            if (stage != null) stage.close();
        });
        feedButton.setOnAction(e -> {
            if (onFeed != null) onFeed.run();
            if (stage != null) stage.close();
        });
        upgradeButton.setOnAction(e -> handleUpgrade());
        closeButton.setOnAction(e -> {
            if (stage != null) stage.close();
        });
    }

    public void setData(
            String title, String desc,
            String collectLbl, String feedLbl, String upgradeLbl,
            Stage stage,
            Runnable onCollect, Runnable onFeed, Runnable onUpgrade,
            int level, // << ADD THIS: the current level of the thing
            java.util.function.Supplier<Integer> coinSupplier // << ADD THIS: pass a function to get the current coins
    ) {
        this.stage = stage;
        this.onCollect = onCollect;
        this.onFeed = onFeed;
        this.onUpgrade = onUpgrade;
        this.currentLevel = level;
        this.coinSupplier = coinSupplier;

        if (titleLabel != null) titleLabel.setText(title);
        if (descLabel != null) descLabel.setText(desc);
        if (collectButton != null) collectButton.setText(collectLbl);
        if (feedButton != null) feedButton.setText(feedLbl);
        if (upgradeButton != null) upgradeButton.setText(upgradeLbl);
        if (feedbackLabel != null) feedbackLabel.setText("");

        updateUpgradeCostLabel();
    }

    private void handleUpgrade() {
        int coins = coinSupplier != null ? coinSupplier.get() : 0;
        upgradeCost = 200 * currentLevel; // match cost formula to your system
        if (coins < upgradeCost) {
            setFeedback("Not enough coins to upgrade! (Need " + upgradeCost + ")", false);
            return; // DO NOT close the dialog
        }
        // Run the actual upgrade
        if (onUpgrade != null) onUpgrade.run();

        // Assume level increased by 1 after upgrade
        currentLevel++;
        updateUpgradeCostLabel();
        setFeedback("Upgrade successful!", true);
    }

    private void updateUpgradeCostLabel() {
        upgradeCost = 200 * currentLevel;
        if (upgradeCostLabel != null)
            upgradeCostLabel.setText("Upgrade Cost: " + upgradeCost + " coins");
    }

    public void setFeedback(String msg, boolean success) {
        if (feedbackLabel != null) {
            feedbackLabel.setText(msg);
            feedbackLabel.setStyle("-fx-text-fill:" + (success ? "#3f8421" : "#bf2c2c") + ";");
        }
    }
}