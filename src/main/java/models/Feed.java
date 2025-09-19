package main.models;

public class Feed extends main.models.FarmElement {
    public Feed(String name, int level, int amount) {
        super(Type.FEED, name, level, amount);
    }

    @Override
    public String getProduct() { return "Feed"; }

    @Override
    public boolean feedOrWaterOrFertilize() {
        // Feed itself is not consumed directly—handled in animal/fish logic
        return false;
    }

    @Override
    public int collect(main.models.User user, boolean batch) {
        // Not harvestable—just a consumable item
        return 0;
    }
}
