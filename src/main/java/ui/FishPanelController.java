package main.ui;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import main.models.Barn;
import main.models.FarmElement;
import main.models.User;
import database.BarnDAO;

public class FishPanelController {
    @FXML private ListView<FarmElement> fishList;
    @FXML private Button collectBtn, feedBtn, upgradeBtn, closeBtn;
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
        fishList.setCellFactory(list -> new ListCell<>() {
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
        fishList.getItems().setAll(barn.getElements(FarmElement.Type.FISH));
    }

    @FXML private void handleCollect() {
        int count = 0;
        for (FarmElement elem : barn.getElements(FarmElement.Type.FISH)) {
            int r = elem.collect(user, true);
            count += r;
        }
        BarnDAO.saveBarn(user);
        refresh();
        setFeedback("Collected from all fish. +" + count, count > 0);
        if (onHUDUpdate != null) onHUDUpdate.run();
    }

    @FXML private void handleFeed() {
        int fed = 0;
        for (FarmElement elem : barn.getElements(FarmElement.Type.FISH))
            if (elem.feedOrWaterOrFertilize()) fed++;
        BarnDAO.saveBarn(user);
        refresh();
        setFeedback("Fed all fish. (" + fed + ")", fed > 0);
        if (onHUDUpdate != null) onHUDUpdate.run();
    }

    @FXML private void handleUpgrade() {
        int cost = 200 * (barn.getElements(FarmElement.Type.FISH).stream().mapToInt(FarmElement::getLevel).max().orElse(1));
        if (user.getCoins() < cost) {
            setFeedback("Not enough coins (need " + cost + ")", false); return;
        }
        for (FarmElement elem : barn.getElements(FarmElement.Type.FISH)) elem.upgrade();
        user.setCoins(user.getCoins() - cost);
        BarnDAO.saveBarn(user);
        refresh();
        updateUpgradeCost();
        setFeedback("All fish upgraded!", true);
        if (onHUDUpdate != null) onHUDUpdate.run();
    }

    @FXML private void handleClose() { ((Stage) closeBtn.getScene().getWindow()).close(); }

    private void updateUpgradeCost() {
        int cost = 200 * (barn != null ? barn.getElements(FarmElement.Type.FISH).stream().mapToInt(FarmElement::getLevel).max().orElse(1) : 1);
        if (upgradeCostLabel != null) upgradeCostLabel.setText("Upgrade All Cost: " + cost + " coins");
    }

    private void setFeedback(String msg, boolean ok) {
        if (feedbackLabel != null) {
            feedbackLabel.setText(msg);
            feedbackLabel.setStyle("-fx-text-fill:" + (ok ? "#3f8421" : "#bf2c2c") + ";");
        }
    }
}
