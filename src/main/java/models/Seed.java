package main.models;

public class Seed extends main.models.FarmElement {
    public Seed(String name, int level, int amount) {
        super(Type.SEED, name, level, amount);
    }

    @Override
    public String getProduct() { return "Seedling"; }

    @Override
    public boolean feedOrWaterOrFertilize() {
        // Seeds cannot be fertilized directly
        return false;
    }

    @Override
    public int collect(main.models.User user, boolean batch) {
        // Not harvestable—planted to grow crops/flowers
        return 0;
    }

    // Planting logic is handled via the .plant() method in FarmElement
}
