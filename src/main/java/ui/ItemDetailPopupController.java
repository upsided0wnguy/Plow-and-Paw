package main.ui;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import main.models.FarmElement;
import main.models.User;
import database.BarnDAO;

public class ItemDetailPopupController {

    @FXML private Label titleLabel, itemNameLabel, itemLevelLabel, itemAmountLabel, itemProductLabel, upgradeCostLabel, feedbackLabel;
    @FXML private Button collectBtn, feedBtn, plantBtn, upgradeBtn, sellBtn, closeBtn;

    private User user;
    private FarmElement element;
    private Stage stage;
    private Runnable onChange; // Callback to refresh the previous screen.

    public void setData(User user, FarmElement element, Stage stage, Runnable onChange) {
        this.user = user;
        this.element = element;
        this.stage = stage;
        this.onChange = onChange;
        refreshUIandSave(); // Initial setup

        // Attach handlers
        collectBtn.setOnAction(e -> handleCollect());
        feedBtn.setOnAction(e -> handleFeedOrFertilize());
        plantBtn.setOnAction(e -> handlePlant());
        upgradeBtn.setOnAction(e -> handleUpgrade());
        sellBtn.setOnAction(e -> handleSell());
        closeBtn.setOnAction(e -> stage.close());
    }

    /**
     * KEY CHANGE: Handles collecting products from a FarmElement.
     * It now adds the specific product (e.g., "Wheat", "Milk") to the barn.
     */
    private void handleCollect() {
        if (!element.isReadyToHarvest()) {
            setFeedback("Not ready to collect yet. Please wait.", false);
            return;
        }
        if (element.getAmount() <= 0) {
            setFeedback("No items to collect from.", false);
            return;
        }

        // The 'collect' method returns the yield and handles internal state changes (XP, timers).
        int collectedYield = element.collect(user, false);

        if (collectedYield > 0) {
            // Get the specific product name (e.g., "Milk", "Wheat", "Apple").
            String productName = element.getProduct();

            // Add the collected PRODUCT to the barn.
            boolean addedToBarn = user.getBarn().addElement(FarmElement.Type.PRODUCE, productName, collectedYield, 1);

            if (addedToBarn) {
                StringBuilder msg = new StringBuilder("Collected " + collectedYield + "x " + productName + ".");
                if (element.isGoldenReady()) {
                    msg.append(" You found a GOLDEN ").append(element.getName()).append("!");
                    element.clearGolden(); // Clear the flag after acknowledging.
                }
                setFeedback(msg.toString(), true);
            } else {
                setFeedback("Barn is full! Could not store " + productName + ".", false);
            }
        } else {
            setFeedback("Nothing to collect.", false);
        }
        refreshUIandSave();
    }

    private void handleFeedOrFertilize() {
        boolean success = element.feedOrWaterOrFertilize();
        if (success) {
            setFeedback("Action successful! Next collection may have a bonus.", true);
        } else {
            setFeedback("Already fed/fertilized recently! Please wait.", false);
        }
        refreshUIandSave();
    }

    private void handlePlant() {
        int cost = element.getSeedCost();
        if (user.getCoins() < cost) {
            setFeedback("Not enough coins to plant! Need " + cost, false);
            return;
        }
        // Assuming a 2-minute growth time from the popup
        if (element.plant(2 * 60 * 1000)) {
            user.setCoins(user.getCoins() - cost);
            setFeedback("Seed planted! Harvest will be ready soon.", true);
        } else {
            setFeedback("Already planted or not a valid seed.", false);
        }
        refreshUIandSave();
    }

    private void handleUpgrade() {
        int cost = element.getUpgradeCost();
        if (user.getCoins() < cost) {
            setFeedback("Not enough coins to upgrade (need " + cost + ")", false);
            return;
        }
        user.setCoins(user.getCoins() - cost);
        element.upgrade();
        setFeedback(element.getName() + " upgraded to Level " + element.getLevel(), true);
        refreshUIandSave();
    }

    private void handleSell() {
        int amt = element.getAmount();
        if (amt <= 0) {
            setFeedback("No items left to sell!", false);
            return;
        }
        int sellPrice = 10 * element.getLevel(); // Example sell price
        user.setCoins(user.getCoins() + sellPrice * amt);
        user.getBarn().removeElement(element.getTypeEnum(), element.getName(), element.getLevel(), amt);
        setFeedback("Sold " + amt + "x " + element.getName() + " for " + (sellPrice * amt) + " coins.", true);

        // Close the popup after selling, as the item is gone.
        stage.close();
        if (onChange != null) onChange.run(); // Ensure the inventory list updates.
        BarnDAO.saveBarn(user);
    }

    private void refreshUIandSave() {
        // Update all UI labels
        titleLabel.setText(element.getName() + " Details");
        itemNameLabel.setText("Name: " + element.getName());
        itemLevelLabel.setText("Level: " + element.getLevel());
        itemAmountLabel.setText("Amount: " + element.getAmount());
        itemProductLabel.setText("Product: " + element.getProduct());
        upgradeCostLabel.setText("Upgrade Cost: " + element.getUpgradeCost());

        // Update button states based on the element's status
        collectBtn.setDisable(!element.isReadyToHarvest() || element.getAmount() <= 0);
        feedBtn.setDisable(element.getType() == FarmElement.Type.SEED || element.getType() == FarmElement.Type.PRODUCE);
        plantBtn.setDisable(element.getType() != FarmElement.Type.SEED);
        upgradeBtn.setDisable(element.getType() == FarmElement.Type.PRODUCE); // Can't upgrade products

        // Save progress to the database
        BarnDAO.saveBarn(user);

        // Run the callback to refresh the calling screen (e.g., inventory)
        if (onChange != null) {
            onChange.run();
        }
    }

    private void setFeedback(String msg, boolean success) {
        feedbackLabel.setText(msg);
        feedbackLabel.setStyle("-fx-text-fill:" + (success ? "#2B8A3E" : "#C92A2A") + "; -fx-font-weight: bold;");
    }
}
