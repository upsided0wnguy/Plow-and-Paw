package main.ui;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.collections.*;
import javafx.beans.property.SimpleStringProperty;
import main.models.User;
import main.models.FarmElement;
import database.BarnDAO;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;

import java.util.Comparator;

public class InventoryController {
    @FXML private Label barnUsageLabel;
    @FXML private TableView<FarmElement> inventoryTable;
    @FXML private TableColumn<FarmElement, String> typeCol, nameCol, levelCol, amountCol, statusCol;
    @FXML private Button closeBtn, upgradeBarnBtn;

    private User user;

    public void setUser(User user) {
        this.user = user;
        updateUI();
    }

    @FXML
    public void initialize() {
        // Cell value factories to populate the table columns from the FarmElement object.
        typeCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getType().name()));
        nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName()));
        levelCol.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getLevel())));
        amountCol.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getAmount())));
        statusCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus()));

        // Custom cell factory for the 'Status' column to add color-coded badges.
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null); setStyle("");
                } else {
                    setText(status);
                    String style = "-fx-font-size:13px; -fx-font-weight:700; -fx-background-radius:12; -fx-alignment:center; -fx-padding: 3 8 3 8;";
                    switch (status) {
                        case "Ready":    style += "-fx-background-color:#d3f9d8;-fx-text-fill:#2f9e44;"; break;
                        case "Golden!":  style += "-fx-background-color:#fff3bf;-fx-text-fill:#f59f0b;"; break;
                        case "Fed":
                        case "Fertilized": style += "-fx-background-color:#e7f5ff;-fx-text-fill:#1c7ed6;"; break;
                        case "Growing":  style += "-fx-background-color:#fff9db;-fx-text-fill:#f08c00;"; break;
                        default:         style += "-fx-background-color:#f1f3f5;-fx-text-fill:#495057;"; break;
                    }
                    setStyle(style);
                }
            }
        });

        // Row factory to handle double-click events for opening the detail popup.
        inventoryTable.setRowFactory(tv -> {
            TableRow<FarmElement> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    openItemPopup(row.getItem());
                }
            });
            return row;
        });

        closeBtn.setOnAction(e -> handleClose());
        upgradeBarnBtn.setOnAction(e -> handleUpgradeBarn());
    }

    public void updateUI() {
        if (user == null || user.getBarn() == null) return;

        ObservableList<FarmElement> entries = FXCollections.observableArrayList();
        // This will correctly fetch ALL items, including producers and products.
        entries.addAll(user.getBarn().getAllElements());

        // Sort the list for better readability: by Type, then by Name.
        entries.sort(Comparator.comparing((FarmElement e) -> e.getType().toString()).thenComparing(FarmElement::getName));

        inventoryTable.setItems(entries);
        barnUsageLabel.setText("Barn Usage: " + user.getBarn().getUsedCapacity() + " / " + user.getBarn().getMaxCapacity());
    }

    private void openItemPopup(FarmElement element) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/item_detail_popup.fxml"));
            Parent root = loader.load();
            main.ui.ItemDetailPopupController ctrl = loader.getController();
            Stage stage = new Stage();

            // Pass the data and a callback to this updateUI method.
            // When something changes in the popup, the inventory screen will refresh automatically.
            ctrl.setData(user, element, stage, this::updateUI);

            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.setTitle(element.getName() + " Details");
            stage.showAndWait(); // Wait for the popup to close before continuing.
            updateUI(); // Final refresh after popup is closed.
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleUpgradeBarn() {
        if (user == null || user.getBarn() == null) return;
        int cost = user.getBarn().getUpgradeCost();

        if (user.getCoins() < cost) {
            showAlert(Alert.AlertType.ERROR, "Not enough coins to upgrade barn! Need " + cost);
            return;
        }

        if (user.getBarn().upgradeBarn(user)) { // This method handles coin deduction
            BarnDAO.saveBarn(user);
            showAlert(Alert.AlertType.INFORMATION, "Barn upgraded to level " + user.getBarn().getLevel() + "!\nNew capacity: " + user.getBarn().getMaxCapacity());
            updateUI();
        } else {
            showAlert(Alert.AlertType.ERROR, "Upgrade failed. Please try again.");
        }
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) closeBtn.getScene().getWindow();
        stage.close();
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}