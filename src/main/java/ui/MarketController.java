package main.ui;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.collections.*;
import javafx.application.Platform;
import main.models.User;
import main.models.FarmElement;
import main.ui.FarmGameController;
import java.net.URL;
import java.util.*;

public class MarketController implements Initializable {

    // ---- BUY TAB ----
    @FXML private ComboBox<FarmElement.Type> categoryCombo;
    @FXML private ComboBox<String> itemCombo;
    @FXML private Spinner<Integer> qtySpinner;
    @FXML private Button confirmBuyBtn;
    @FXML private Label buyFeedbackLabel;
    @FXML private TableView<ShopItem> marketTable;
    @FXML private TableColumn<ShopItem, String> itemCol;
    @FXML private TableColumn<ShopItem, Integer> priceCol;
    @FXML private TableColumn<ShopItem, Integer> stockCol;
    @FXML private Button buyGemsButton;
    @FXML private Label buyGemFeedbackLabel;

    // ---- SELL TAB ----
    @FXML private ComboBox<FarmElement.Type> sellCategoryCombo;
    @FXML private ComboBox<String> sellItemCombo;
    @FXML private Spinner<Integer> sellQtySpinner;
    @FXML private Button confirmSellBtn;
    @FXML private Label sellFeedbackLabel;
    @FXML private ListView<String> marketSellList;
    @FXML private TableColumn<ShopItem, String> descCol;
    @FXML private TableColumn<ShopItem, String> typeCol;
    @FXML private Button closeBtn;


    private User user;
    private Runnable updateHudCallback;
    private FarmGameController farmGameController;

    private final ObservableList<ShopItem> shopItems = FXCollections.observableArrayList();

    // Simple in-game "shop" items for demonstration
    private static final Map<FarmElement.Type, List<ShopItem>> SHOP_ITEMS = Map.of(
            FarmElement.Type.ANIMAL, Arrays.asList(
                    new ShopItem("Cow", 100, 10, FarmElement.Type.ANIMAL, "Produces milk every day. Essential for dairy farming."),
                    new ShopItem("Chicken", 40, 20, FarmElement.Type.ANIMAL, "Lays eggs daily. Fast and easy to raise."),
                    new ShopItem("Turkey", 80, 8, FarmElement.Type.ANIMAL, "Large bird that lays big eggs."),
                    new ShopItem("Animal Feed", 8, 999, FarmElement.Type.FEED, "Feed for cows, chickens, turkeys. Increases product yield.")
            ),
            FarmElement.Type.FISH, Arrays.asList(
                    new ShopItem("Fish", 20, 30, FarmElement.Type.FISH, "Raise in ponds and harvest for coins."),
                    new ShopItem("Fish Feed", 6, 999, FarmElement.Type.FEED, "Feed for all pond fish. Required for growth.")
            ),
            FarmElement.Type.FRUIT, Arrays.asList(
                    new ShopItem("Apple", 10, 100, FarmElement.Type.FRUIT, "Juicy apple fruit. Can be eaten or sold."),
                    new ShopItem("Fruit Sapling", 22, 30, FarmElement.Type.SAPLING, "Plant this to grow an apple tree.")
            ),
            FarmElement.Type.FLOWER, Arrays.asList(
                    new ShopItem("Flower", 12, 50, FarmElement.Type.FLOWER, "Harvest for coins or use for bouquets."),
                    new ShopItem("Flower Seed", 5, 100, FarmElement.Type.SEED, "Plant to grow flowers."),
                    new ShopItem("Flower Fertilizer", 16, 60, FarmElement.Type.FERTILIZER, "Speeds up flower growth.")
            ),
            FarmElement.Type.CROP, Arrays.asList(
                    new ShopItem("Wheat", 8, 50, FarmElement.Type.CROP, "Basic crop. Used for flour and feed."),
                    new ShopItem("Wheat Seed", 2, 150, FarmElement.Type.SEED, "Plant to grow wheat fields."),
                    new ShopItem("Corn", 15, 40, FarmElement.Type.CROP, "Popular crop for food and feed."),
                    new ShopItem("Corn Seed", 3, 100, FarmElement.Type.SEED, "Plant to grow corn fields."),
                    new ShopItem("Crop Fertilizer", 15, 80, FarmElement.Type.FERTILIZER, "Speeds up crop growth and increases yield.")
            ),
            // ---- SUPPORTING CATEGORIES ----
            FarmElement.Type.FEED, Arrays.asList(
                    new ShopItem("Animal Feed", 8, 999, FarmElement.Type.FEED, "Feed for cows, chickens, turkeys."),
                    new ShopItem("Fish Feed", 6, 999, FarmElement.Type.FEED, "Feed for all types of fish.")
            ),
            FarmElement.Type.SEED, Arrays.asList(
                    new ShopItem("Wheat Seed", 2, 150, FarmElement.Type.SEED, "Plant to grow wheat."),
                    new ShopItem("Corn Seed", 3, 100, FarmElement.Type.SEED, "Plant to grow corn."),
                    new ShopItem("Flower Seed", 5, 100, FarmElement.Type.SEED, "Plant to grow flowers.")
            ),
            FarmElement.Type.FERTILIZER, Arrays.asList(
                    new ShopItem("Crop Fertilizer", 15, 80, FarmElement.Type.FERTILIZER, "Speeds up crop growth and increases yield."),
                    new ShopItem("Flower Fertilizer", 16, 60, FarmElement.Type.FERTILIZER, "Speeds up flower growth.")
            ),
            FarmElement.Type.SAPLING, Arrays.asList(
                    new ShopItem("Fruit Sapling", 22, 30, FarmElement.Type.SAPLING, "Plant this to grow a fruit tree.")
            ),
            FarmElement.Type.PRODUCE, Arrays.asList(
                    new ShopItem("Egg", 12, 80, FarmElement.Type.PRODUCE, "Collected from chickens and turkeys."),
                    new ShopItem("Milk", 20, 50, FarmElement.Type.PRODUCE, "Collected from cows and goats."),
                    new ShopItem("Wool", 18, 40, FarmElement.Type.PRODUCE, "Collected from sheep.")
                    // Add more produce as your game expands
            )
    );

    // --- Setters ---
    public void setUser(User user) { this.user = user; }
    public void setUpdateHudCallback(Runnable cb) { this.updateHudCallback = cb; }
    public void setFarmGameController(FarmGameController controller) { this.farmGameController = controller; }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // ---- BUY TAB ----
        if (typeCol != null)
            typeCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getType().name()));
        if (descCol != null)
            descCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getDescription()));

        if (categoryCombo != null) {
            categoryCombo.setItems(FXCollections.observableArrayList(FarmElement.Type.values()));
            categoryCombo.setOnAction(e -> refreshBuyItemCombo());
        }
        if (itemCombo != null) {
            itemCombo.setOnAction(e -> refreshBuyTable());
        }
        if (qtySpinner != null) {
            qtySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 999, 1));
        }
        if (itemCol != null) itemCol.setCellValueFactory(cell -> cell.getValue().nameProperty());
        if (priceCol != null) priceCol.setCellValueFactory(cell -> cell.getValue().priceProperty().asObject());
        if (stockCol != null) stockCol.setCellValueFactory(cell -> cell.getValue().stockProperty().asObject());
        if (marketTable != null) {
            marketTable.setItems(shopItems);
            marketTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        }
        if (confirmBuyBtn != null) {
            confirmBuyBtn.setOnAction(e -> handleBuy());
        }
        if (buyGemsButton != null) {
            buyGemsButton.setOnAction(e -> setBuyGemFeedback("Buying gems coming soon!"));
        }

        // ---- SELL TAB ----
        if (sellCategoryCombo != null) {
            sellCategoryCombo.setItems(FXCollections.observableArrayList(FarmElement.Type.values()));
            sellCategoryCombo.setOnAction(e -> refreshSellItemCombo());
        }
        if (sellItemCombo != null) {
            sellItemCombo.setOnAction(e -> refreshSellList());
        }
        if (sellQtySpinner != null) {
            sellQtySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 999, 1));
        }
        if (marketSellList != null) {
            marketSellList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        }
        if (confirmSellBtn != null) {
            confirmSellBtn.setOnAction(e -> handleSell());
        }

        if (closeBtn != null) {
            closeBtn.setOnAction(e -> {
                ((Stage) closeBtn.getScene().getWindow()).close();
                if (updateHudCallback != null) updateHudCallback.run();
                if (farmGameController != null) farmGameController.requestViewportFocus();
            });
        }

        Platform.runLater(this::refreshAllUI);
    }

    // ---- BUY TAB LOGIC ----
    private void refreshBuyItemCombo() {
        if (categoryCombo == null || itemCombo == null) return;
        FarmElement.Type selectedType = categoryCombo.getValue();
        List<String> itemNames = new ArrayList<>();
        if (selectedType != null && SHOP_ITEMS.containsKey(selectedType)) {
            for (ShopItem si : SHOP_ITEMS.get(selectedType)) itemNames.add(si.getName());
        }
        itemCombo.setItems(FXCollections.observableArrayList(itemNames));
        if (!itemNames.isEmpty()) itemCombo.getSelectionModel().selectFirst();
        refreshBuyTable();
    }

    private void refreshBuyTable() {
        shopItems.clear();
        if (categoryCombo == null || itemCombo == null) return;
        FarmElement.Type selectedType = categoryCombo.getValue();
        String selectedName = itemCombo.getValue();
        if (selectedType != null && SHOP_ITEMS.containsKey(selectedType)) {
            for (ShopItem si : SHOP_ITEMS.get(selectedType)) {
                if (selectedName == null || selectedName.equals(si.getName()))
                    shopItems.add(si);
            }
        }
        if (!shopItems.isEmpty()) marketTable.getSelectionModel().selectFirst();
    }

    private void handleBuy() {
        if (user == null || categoryCombo == null || itemCombo == null || qtySpinner == null) return;
        FarmElement.Type selectedType = categoryCombo.getValue();
        String selectedName = itemCombo.getValue();
        int qty = qtySpinner.getValue();
        if (selectedType == null || selectedName == null || qty <= 0) {
            setBuyFeedback("Select item and quantity.", false);
            return;
        }
        ShopItem toBuy = null;
        for (ShopItem si : shopItems) {
            if (si.getName().equals(selectedName)) {
                toBuy = si; break;
            }
        }
        if (toBuy == null) {
            setBuyFeedback("Item not found.", false);
            return;
        }
        int totalCost = toBuy.getPrice() * qty;
        if (user.getCoins() < totalCost) {
            setBuyFeedback("Not enough coins!", false);
            return;
        }
        if (qty > toBuy.getStock()) {
            setBuyFeedback("Not enough stock!", false);
            return;
        }
        user.setCoins(user.getCoins() - totalCost);
        toBuy.setStock(toBuy.getStock() - qty);
        // Add to barn
        user.getBarn().addElement(selectedType, selectedName, qty, 1);
        setBuyFeedback("Bought " + qty + "x " + selectedName + "!", true);
        refreshBuyTable();
        if (updateHudCallback != null) updateHudCallback.run();
    }

    private void setBuyFeedback(String msg, boolean good) {
        if (buyFeedbackLabel != null) {
            buyFeedbackLabel.setText(msg);
            buyFeedbackLabel.setStyle("-fx-text-fill:" + (good ? "#268d44" : "#b63123") + ";");
        }
    }
    private void setBuyGemFeedback(String msg) {
        if (buyGemFeedbackLabel != null) {
            buyGemFeedbackLabel.setText(msg);
        }
    }

    // ---- SELL TAB LOGIC ----
    private void refreshSellItemCombo() {
        if (sellCategoryCombo == null || sellItemCombo == null) return;
        FarmElement.Type selectedType = sellCategoryCombo.getValue();
        List<String> itemNames = new ArrayList<>();

        if (user != null && selectedType != null) {
            for (FarmElement elem : user.getBarn().getAllElements()) {
                if (elem.getAmount() > 0) {
                    // --- Main categories ---
                    if (selectedType == FarmElement.Type.ANIMAL) {
                        if (elem.getType() == FarmElement.Type.ANIMAL || elem.getType() == FarmElement.Type.PRODUCE
                                || (elem.getType() == FarmElement.Type.FEED && "animal".equalsIgnoreCase(elem.getSubType()))) {
                            String label = elem.getName() + " (Lv" + elem.getLevel() + ")";
                            if (!itemNames.contains(label))
                                itemNames.add(label);
                        }
                    } else if (selectedType == FarmElement.Type.FISH) {
                        if (elem.getType() == FarmElement.Type.FISH ||
                                (elem.getType() == FarmElement.Type.FEED && "fish".equalsIgnoreCase(elem.getSubType())) ||
                                (elem.getType() == FarmElement.Type.PRODUCE && "fish".equalsIgnoreCase(elem.getSubType()))) {
                            String label = elem.getName() + " (Lv" + elem.getLevel() + ")";
                            if (!itemNames.contains(label))
                                itemNames.add(label);
                        }
                    } else if (selectedType == FarmElement.Type.FRUIT) {
                        if (elem.getType() == FarmElement.Type.FRUIT ||
                                (elem.getType() == FarmElement.Type.SAPLING && "fruit".equalsIgnoreCase(elem.getSubType()))) {
                            String label = elem.getName() + " (Lv" + elem.getLevel() + ")";
                            if (!itemNames.contains(label))
                                itemNames.add(label);
                        }
                    } else if (selectedType == FarmElement.Type.CROP) {
                        if (elem.getType() == FarmElement.Type.CROP ||
                                (elem.getType() == FarmElement.Type.SEED && "crop".equalsIgnoreCase(elem.getSubType())) ||
                                (elem.getType() == FarmElement.Type.FERTILIZER && "crop".equalsIgnoreCase(elem.getSubType()))) {
                            String label = elem.getName() + " (Lv" + elem.getLevel() + ")";
                            if (!itemNames.contains(label))
                                itemNames.add(label);
                        }
                    } else if (selectedType == FarmElement.Type.FLOWER) {
                        if (elem.getType() == FarmElement.Type.FLOWER ||
                                (elem.getType() == FarmElement.Type.SEED && "flower".equalsIgnoreCase(elem.getSubType())) ||
                                (elem.getType() == FarmElement.Type.FERTILIZER && "flower".equalsIgnoreCase(elem.getSubType()))) {
                            String label = elem.getName() + " (Lv" + elem.getLevel() + ")";
                            if (!itemNames.contains(label))
                                itemNames.add(label);
                        }
                    }
                    // --- Minor/supporting categories: FEED, SEED, FERTILIZER, SAPLING, PRODUCE ---
                    else if (selectedType == FarmElement.Type.FEED) {
                        if (elem.getType() == FarmElement.Type.FEED) {
                            String label = elem.getName() + " (Lv" + elem.getLevel() + ")";
                            if (!itemNames.contains(label))
                                itemNames.add(label);
                        }
                    } else if (selectedType == FarmElement.Type.SEED) {
                        if (elem.getType() == FarmElement.Type.SEED) {
                            String label = elem.getName() + " (Lv" + elem.getLevel() + ")";
                            if (!itemNames.contains(label))
                                itemNames.add(label);
                        }
                    } else if (selectedType == FarmElement.Type.FERTILIZER) {
                        if (elem.getType() == FarmElement.Type.FERTILIZER) {
                            String label = elem.getName() + " (Lv" + elem.getLevel() + ")";
                            if (!itemNames.contains(label))
                                itemNames.add(label);
                        }
                    } else if (selectedType == FarmElement.Type.SAPLING) {
                        if (elem.getType() == FarmElement.Type.SAPLING) {
                            String label = elem.getName() + " (Lv" + elem.getLevel() + ")";
                            if (!itemNames.contains(label))
                                itemNames.add(label);
                        }
                    } else if (selectedType == FarmElement.Type.PRODUCE) {
                        if (elem.getType() == FarmElement.Type.PRODUCE) {
                            String label = elem.getName() + " (Lv" + elem.getLevel() + ")";
                            if (!itemNames.contains(label))
                                itemNames.add(label);
                        }
                    }
                }
            }
        }
        sellItemCombo.setItems(FXCollections.observableArrayList(itemNames));
        if (!itemNames.isEmpty()) sellItemCombo.getSelectionModel().selectFirst();
        refreshSellList();
    }
    private void refreshSellList() {
        if (marketSellList == null || user == null || sellCategoryCombo == null || sellItemCombo == null) return;
        FarmElement.Type selectedType = sellCategoryCombo.getValue();
        String selectedLabel = sellItemCombo.getValue();
        ObservableList<String> list = FXCollections.observableArrayList();

        if (selectedType != null && selectedLabel != null) {
            for (FarmElement elem : user.getBarn().getAllElements()) {
                String label = elem.getName() + " (Lv" + elem.getLevel() + ")";
                if (label.equals(selectedLabel) && elem.getAmount() > 0) {
                    String entry = elem.getName() + "  |  Lv " + elem.getLevel() + "  |  Qty: " + elem.getAmount();
                    list.add(entry);
                }
            }
        }
        marketSellList.setItems(list);
        if (!list.isEmpty()) marketSellList.getSelectionModel().selectFirst();
    }

    private void handleSell() {
        if (user == null || sellCategoryCombo == null || sellItemCombo == null || sellQtySpinner == null || marketSellList == null)
            return;

        FarmElement.Type selectedType = sellCategoryCombo.getValue();
        String selectedLabel = sellItemCombo.getValue();
        int qty = sellQtySpinner.getValue();

        if (selectedType == null || selectedLabel == null || qty <= 0) {
            setSellStatus("Select item and quantity.", false);
            return;
        }

        // Parse name and level from label
        String name;
        int level = 1;
        try {
            int idx = selectedLabel.lastIndexOf(" (Lv");
            name = selectedLabel.substring(0, idx);
            String lvlStr = selectedLabel.substring(idx + 4, selectedLabel.length() - 1);
            level = Integer.parseInt(lvlStr);
        } catch (Exception ex) {
            setSellStatus("Invalid item selection.", false);
            return;
        }

        FarmElement target = null;
        for (FarmElement elem : user.getBarn().getAllElements()) {
            if (elem.getName().equalsIgnoreCase(name) && elem.getLevel() == level && elem.getAmount() > 0) {
                target = elem;
                break;
            }
        }

        if (target == null) {
            setSellStatus("No items to sell.", false);
            return;
        }

        if (qty > target.getAmount()) {
            setSellStatus("Not enough items.", false);
            return;
        }

        int sellPrice = 20 * target.getLevel() * qty;
        user.setCoins(user.getCoins() + sellPrice);
        user.getBarn().removeElement(target.getTypeEnum(), target.getName(), target.getLevel(), qty);

        setSellStatus("Sold " + qty + "x " + target.getName() + " (L" + target.getLevel() + ") for " + sellPrice + " coins.", true);
        refreshSellList();
        if (updateHudCallback != null) updateHudCallback.run();
    }

    private void setSellStatus(String msg, boolean good) {
        if (sellFeedbackLabel != null) {
            sellFeedbackLabel.setText(msg);
            sellFeedbackLabel.setStyle("-fx-text-fill:" + (good ? "#3f8421" : "#bf2c2c") + ";");
        }
    }
    // --- Called after user is set ---
    public void refreshAllUI() {
        // Buy
        if (categoryCombo != null) {
            categoryCombo.getSelectionModel().clearSelection();
            categoryCombo.setItems(FXCollections.observableArrayList(FarmElement.Type.values()));
            if (!categoryCombo.getItems().isEmpty()) categoryCombo.getSelectionModel().selectFirst();
            refreshBuyItemCombo();
        }
        // Sell
        if (sellCategoryCombo != null) {
            sellCategoryCombo.getSelectionModel().clearSelection();
            sellCategoryCombo.setItems(FXCollections.observableArrayList(FarmElement.Type.values()));
            if (!sellCategoryCombo.getItems().isEmpty()) sellCategoryCombo.getSelectionModel().selectFirst();
            refreshSellItemCombo();
        }
        refreshSellList();
    }

    // --- Helper Class for Shop Items ---
    public static class ShopItem {
        private final javafx.beans.property.SimpleStringProperty name;
        private final javafx.beans.property.SimpleIntegerProperty price;
        private final javafx.beans.property.SimpleIntegerProperty stock;
        private final FarmElement.Type type;
        private final String description;

        public ShopItem(String name, int price, int stock, FarmElement.Type type, String description) {
            this.name = new javafx.beans.property.SimpleStringProperty(name);
            this.price = new javafx.beans.property.SimpleIntegerProperty(price);
            this.stock = new javafx.beans.property.SimpleIntegerProperty(stock);
            this.type = type;
            this.description = description;
        }

        // Overload for backward compatibility (in case some existing code calls it without a description)
        public ShopItem(String name, int price, int stock, FarmElement.Type type) {
            this(name, price, stock, type, "");
        }

        public String getName() { return name.get(); }
        public int getPrice() { return price.get(); }
        public int getStock() { return stock.get(); }
        public void setStock(int s) { stock.set(s); }
        public javafx.beans.property.SimpleStringProperty nameProperty() { return name; }
        public javafx.beans.property.SimpleIntegerProperty priceProperty() { return price; }
        public javafx.beans.property.SimpleIntegerProperty stockProperty() { return stock; }
        public FarmElement.Type getType() { return type; }
        public String getDescription() { return description; }
    }
}