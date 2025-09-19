package main.models;

/**
 * Represents Fruit Trees. They can be watered (treated as fertilizing) to improve yield.
 */
public class FruitTree extends main.models.FarmElement {
    private boolean wateredRecently = false;
    private long lastWateredTime = 0;
    private static final long WATER_COOLDOWN_MS = 60_000; // 1 minute cooldown for watering.

    public FruitTree(String name, int level, int amount) {
        super(Type.FRUIT, name, level, amount);
        setReadyToHarvest(true);
    }

    @Override
    public boolean feedOrWaterOrFertilize() {
        long now = System.currentTimeMillis();
        if (wateredRecently && now - lastWateredTime < WATER_COOLDOWN_MS) return false;

        // For simplicity, we use the 'fertilized' flag to represent being watered.
        fertilized = true;
        wateredRecently = true;
        lastWateredTime = now;
        markAction("water");
        return true;
    }

    @Override
    public int collect(main.models.User user, boolean batch) {
        if (!isReadyToHarvest() || amount <= 0) return 0;

        int yield = super.collect(user, batch);

        // Reset state for the next growth cycle.
        wateredRecently = false;
        setReadyToHarvest(false);
        setGrowEndTime(System.currentTimeMillis() + 100_000); // 100 seconds to grow fruit again.

        return yield;
    }

    /**
     * Gets the product from the tree.
     * E.g., An "Apple Tree" produces "Apple".
     * @return The name of the fruit.
     */
    @Override
    public String getProduct() {
        if (name != null && name.toLowerCase().endsWith(" tree")) {
            return name.substring(0, name.length() - 5).trim();
        }
        return name; // Fallback
    }
}
