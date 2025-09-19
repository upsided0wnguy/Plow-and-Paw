package main.models;

public class Sapling extends main.models.FarmElement {
    public Sapling(String name, int level, int amount) {
        super(Type.SAPLING, name, level, amount);
    }

    @Override
    public String getProduct() { return "Young Tree"; }

    @Override
    public boolean feedOrWaterOrFertilize() {
        // Saplings cannot be fertilized directly
        return false;
    }

    @Override
    public int collect(main.models.User user, boolean batch) {
        // Not harvestable—grows into a fruit tree or similar
        return 0;
    }
}
