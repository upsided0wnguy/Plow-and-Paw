package main.ui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.stage.Modality;
import javafx.stage.Stage;
import main.models.Barn;
import main.models.FarmElement;
import main.models.User;
import database.BarnDAO;
import database.DatabaseConnector;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class BarnPanelController {

    @FXML private TableView<FarmElement> elementTable;
    @FXML private TableColumn<FarmElement, String> nameCol;
    @FXML private TableColumn<FarmElement, Number> levelCol;
    @FXML private TableColumn<FarmElement, Number> amountCol;
    @FXML private Label barnCapLabel;
    @FXML private Label barnLevelLabel;
    @FXML private Label feedbackLabel;
    @FXML private Label upgradeCostLabel;
    @FXML private Button upgradeBarnBtn;
    @FXML private Button removeBtn;
    @FXML private Button closeBtn;

    // Add these FXML labels for live stats:
    @FXML private Label cropsLabel;
    @FXML private Label animalsLabel;
    @FXML private Label fruitsLabel;
    @FXML private Label flowersLabel;
    @FXML private Label fishLabel;
    @FXML private Label eggsLabel;

    private Barn barn;
    private User user;
    private Runnable hudRefreshCallback;
    private FarmElement.Type filterType = null;

    private static BarnPanelController currentInstance = null;
    public static BarnPanelController getCurrentInstance() { return currentInstance; }
    public BarnPanelController() { currentInstance = this; }

    public void setBarnAndType(Barn barn, FarmElement.Type type, User user, Runnable hudRefreshCallback) {
        this.barn = barn;
        this.filterType = type;
        this.user = user;
        this.hudRefreshCallback = hudRefreshCallback;
        refresh();
        setFeedback("", true);
        if (user != null) updateBarnStats(user.getUsername());
        updateUpgradeCost();
    }

    @FXML
    private void initialize() {
        nameCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getName()));
        levelCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleIntegerProperty(cell.getValue().getLevel()));
        amountCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleIntegerProperty(cell.getValue().getAmount()));
        setFeedback("", true);
        refresh();
        elementTable.setRowFactory(tv -> {
            TableRow<FarmElement> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    FarmElement selected = row.getItem();
                    openItemPopup(selected);
                }
            });
            return row;
        });
        if (user != null) updateBarnStats(user.getUsername());
        updateUpgradeCost();
    }

    public void refresh() {
        List<FarmElement> filtered = new ArrayList<>();
        if (barn == null) return;
        if (filterType == null) {
            filtered.addAll(barn.getAllElements());
        } else {
            for (FarmElement elem : barn.getAllElements()) {
                switch (filterType) {
                    case ANIMAL:
                        if (elem.getType() == FarmElement.Type.ANIMAL ||
                                (elem.getType() == FarmElement.Type.FEED && "animal".equalsIgnoreCase(elem.getSubType())))
                            filtered.add(elem);
                        break;
                    case FISH:
                        if (elem.getType() == FarmElement.Type.FISH ||
                                (elem.getType() == FarmElement.Type.FEED && "fish".equalsIgnoreCase(elem.getSubType())))
                            filtered.add(elem);
                        break;
                    case FRUIT:
                        if (elem.getType() == FarmElement.Type.FRUIT ||
                                (elem.getType() == FarmElement.Type.SAPLING && "fruit".equalsIgnoreCase(elem.getSubType())))
                            filtered.add(elem);
                        break;
                    case CROP:
                        if (elem.getType() == FarmElement.Type.CROP ||
                                (elem.getType() == FarmElement.Type.SEED && "crop".equalsIgnoreCase(elem.getSubType())) ||
                                (elem.getType() == FarmElement.Type.FERTILIZER && "crop".equalsIgnoreCase(elem.getSubType())))
                            filtered.add(elem);
                        break;
                    case FLOWER:
                        if (elem.getType() == FarmElement.Type.FLOWER ||
                                (elem.getType() == FarmElement.Type.SEED && "flower".equalsIgnoreCase(elem.getSubType())) ||
                                (elem.getType() == FarmElement.Type.FERTILIZER && "flower".equalsIgnoreCase(elem.getSubType())))
                            filtered.add(elem);
                        break;
                    default:
                        if (elem.getType() == filterType)
                            filtered.add(elem);
                }
            }
        }
        elementTable.setItems(FXCollections.observableArrayList(filtered));
        updateBarnLevelLabel();
        updateUpgradeCost();
    }

    private void openItemPopup(FarmElement element) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/item_detail_popup.fxml"));
            Parent root = loader.load();
            main.ui.ItemDetailPopupController ctrl = loader.getController();
            Stage stage = new Stage();
            ctrl.setData(user, element, stage, () -> {
                refresh();
                if (hudRefreshCallback != null) hudRefreshCallback.run();
            });
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.setTitle(element.getName() + " Details");
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- HANDLER METHODS for FXML buttons ---
    @FXML
    private void handleUpgrade() {
        if (user == null || barn == null) {
            return;
        }

        // This single method call handles coin checking, deduction, and leveling up.
        if (barn.upgradeBarn(user)) {
            BarnDAO.saveBarn(user);
            setFeedback("Barn upgraded! Level " + barn.getLevel() + ", capacity: " + barn.getMaxCapacity(), true);

            // Refresh all UI elements to show the new state
            refresh();
            updateBarnLevelLabel();
            updateUpgradeCost();
            if (hudRefreshCallback != null) {
                hudRefreshCallback.run();
            }
        } else {
            // This block executes if upgradeBarn(user) returns false (not enough coins).
            setFeedback("Not enough coins to upgrade (need " + barn.getUpgradeCost() + ")", false);
        }
    }

    @FXML
    private void handleRemove() {
        FarmElement selected = elementTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            setFeedback("Select an item to remove.", false);
            return;
        }
        barn.removeElement(selected.getTypeEnum(), selected.getName(), selected.getLevel(), selected.getAmount());
        refresh();
        BarnDAO.saveBarn(user);
        setFeedback(selected.getName() + " removed from barn.", true);
        if (hudRefreshCallback != null) hudRefreshCallback.run();
        updateUpgradeCost();
    }

    @FXML
    private void handleClose() {
        ((Stage) closeBtn.getScene().getWindow()).close();
        if (hudRefreshCallback != null) hudRefreshCallback.run();
        if (currentInstance == this) currentInstance = null;
    }

    // --- UI THEME/STATS helpers ---
    private void setFeedback(String msg, boolean success) {
        if (feedbackLabel != null) {
            feedbackLabel.setText(msg);
            feedbackLabel.setStyle("-fx-text-fill:" + (success ? "#3f8421" : "#bf2c2c") + ";");
        }
    }
    private void updateUpgradeCost() {
        if (upgradeCostLabel != null && barn != null)
            upgradeCostLabel.setText("Upgrade Cost: " + (150 * barn.getLevel()) + " coins");
    }
    private int getTotalByType(String username, String type) {
        try (Connection conn = DatabaseConnector.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT SUM(amount) FROM user_barn WHERE username = ? AND type = ?"
            );
            ps.setString(1, username);
            ps.setString(2, type);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    private int getTotalByProduct(String username, String productName) {
        try (Connection conn = DatabaseConnector.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT SUM(amount) FROM user_barn WHERE username = ? AND name = ?"
            );
            ps.setString(1, username);
            ps.setString(2, productName);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    public void updateBarnStats(String username) {
        if (cropsLabel != null)   cropsLabel.setText("Crops: "   + getTotalByType(username, "CROP"));
        if (animalsLabel != null) animalsLabel.setText("Animals: " + getTotalByType(username, "ANIMAL"));
        if (fruitsLabel != null)  fruitsLabel.setText("Fruits: "  + getTotalByType(username, "FRUIT"));
        if (flowersLabel != null) flowersLabel.setText("Flowers: " + getTotalByType(username, "FLOWER"));
        if (fishLabel != null)    fishLabel.setText("Fish: "      + getTotalByType(username, "FISH"));
        if (eggsLabel != null)    eggsLabel.setText("Eggs: "      + getTotalByProduct(username, "Egg"));
    }

    private void updateBarnLevelLabel() {
        if (barnLevelLabel != null && barn != null)
            barnLevelLabel.setText("Barn Level: " + barn.getLevel());
        if (barnCapLabel != null && barn != null)
            barnCapLabel.setText(barn.getUsedCapacity() + " / " + barn.getMaxCapacity());
    }
}