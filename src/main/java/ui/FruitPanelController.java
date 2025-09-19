package main.ui;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import main.models.Barn;
import main.models.FarmElement;
import main.models.User;
import database.BarnDAO;

public class FruitPanelController {
    @FXML private ListView<FarmElement> fruitList;
    @FXML private Button collectBtn, waterBtn, upgradeBtn, closeBtn;
    @FXML private Label feedbackLabel, upgradeCostLabel;

    private Barn barn;
    private User user;
    private Runnable onHUDUpdate;

    public void setData(Barn barn, User user, Runnable onHUDUpdate) {
        this.barn = barn;
        this.user = user;
        this.onHUDUpdate = onHUDUpdate;
        refresh();
        updateUpgradeCost();
        setFeedback("", true);
    }

    @FXML private void initialize() {
        fruitList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(FarmElement elem, boolean empty) {
                super.updateItem(elem, empty);
                if (empty || elem == null) setText(null);
                else setText(elem.getName() + " (Lvl " + elem.getLevel() + ")  x" + elem.getAmount() + " [" + elem.getStatus() + "]");
            }
        });
        setFeedback("", true);
    }

    private void refresh() {
        if (barn == null) return;
        fruitList.getItems().setAll(barn.getElements(FarmElement.Type.FRUIT));
    }

    @FXML private void handleCollect() {
        int count = 0;
        for (FarmElement elem : barn.getElements(FarmElement.Type.FRUIT)) {
            int r = elem.collect(user, true);
            count += r;
        }
        BarnDAO.saveBarn(user);
        refresh();
        setFeedback("Collected all fruits. +" + count, count > 0);
        if (onHUDUpdate != null) onHUDUpdate.run();
    }

    @FXML private void handleWater() {
        int w = 0;
        for (FarmElement elem : barn.getElements(FarmElement.Type.FRUIT))
            if (elem.feedOrWaterOrFertilize()) w++;
        BarnDAO.saveBarn(user);
        refresh();
        setFeedback("Watered all fruit trees. (" + w + ")", w > 0);
        if (onHUDUpdate != null) onHUDUpdate.run();
    }

    @FXML private void handleUpgrade() {
        int cost = 200 * (barn.getElements(FarmElement.Type.FRUIT).stream().mapToInt(FarmElement::getLevel).max().orElse(1));
        if (user.getCoins() < cost) {
            setFeedback("Not enough coins (need " + cost + ")", false); return;
        }
        for (FarmElement elem : barn.getElements(FarmElement.Type.FRUIT)) elem.upgrade();
        user.setCoins(user.getCoins() - cost);
        BarnDAO.saveBarn(user);
        refresh();
        updateUpgradeCost();
        setFeedback("All fruits upgraded!", true);
        if (onHUDUpdate != null) onHUDUpdate.run();
    }

    @FXML private void handleClose() { ((Stage) closeBtn.getScene().getWindow()).close(); }

    private void updateUpgradeCost() {
        int cost = 200 * (barn != null ? barn.getElements(FarmElement.Type.FRUIT).stream().mapToInt(FarmElement::getLevel).max().orElse(1) : 1);
        if (upgradeCostLabel != null) upgradeCostLabel.setText("Upgrade All Cost: " + cost + " coins");
    }

    private void setFeedback(String msg, boolean ok) {
        if (feedbackLabel != null) {
            feedbackLabel.setText(msg);
            feedbackLabel.setStyle("-fx-text-fill:" + (ok ? "#3f8421" : "#bf2c2c") + ";");
        }
    }
}
