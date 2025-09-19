package main.models;

public class Fertilizer extends main.models.FarmElement {
    public Fertilizer(String name, int level, int amount) {
        super(Type.FERTILIZER, name, level, amount);
    }

    @Override
    public String getProduct() { return "Fertilizer"; }

    @Override
    public boolean feedOrWaterOrFertilize() {
        // Fertilizer is consumed by crops/flowers, not itself
        return false;
    }

    @Override
    public int collect(main.models.User user, boolean batch) {
        // Not a harvestable item
        return 0;
    }
}
