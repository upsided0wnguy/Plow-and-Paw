package models;

public class InventoryItem {
    private String name;
    private int quantity;
    private String type; // "crop", "animal_good", "fruit", "fish", "flower"

    public InventoryItem(String name, int quantity, String type) {
        this.name = name;
        this.quantity = quantity;
        this.type = type;
    }

    public InventoryItem add(int qty) {
        this.quantity += qty;
        return this;
    }

    public void subtract(int qty) {
        this.quantity -= qty;
    }

    public String getName() { return name; }
    public int getQuantity() { return quantity; }
    public String getType() { return type; }
}
