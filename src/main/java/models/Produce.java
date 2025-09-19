package main.models;

public class Produce extends main.models.FarmElement {
    public Produce(String name, int level, int amount) {
        super(Type.PRODUCE, name, level, amount);
    }

    @Override
    public String getProduct() { return name; }

    @Override
    public boolean feedOrWaterOrFertilize() {
        // Produced goods cannot be fed or fertilized
        return false;
    }

    @Override
    public int collect(main.models.User user, boolean batch) {
        // Not harvestable—it’s the product, ready to sell
        return 0;
    }
}
