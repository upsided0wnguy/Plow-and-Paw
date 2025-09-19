package main.models;

/**
 * Represents Flowers in the farm. Flowers can be fertilized to improve yield.
 */
public class Flower extends main.models.FarmElement {
    private boolean fertilizedRecently = false;
    private long lastFertilizedTime = 0;
    private static final long FERT_COOLDOWN_MS = 60_000; // 1 minute cooldown for fertilizing.

    public Flower(String name, int level, int amount) {
        super(Type.FLOWER, name, level, amount);
        setReadyToHarvest(true);
    }

    @Override
    public boolean feedOrWaterOrFertilize() {
        long now = System.currentTimeMillis();
        // Prevent fertilizing if it has been done recently.
        if (fertilizedRecently && now - lastFertilizedTime < FERT_COOLDOWN_MS) return false;

        fertilized = true;
        fertilizedRecently = true;
        lastFertilizedTime = now;
        markAction("fertilize");
        return true;
    }

    @Override
    public int collect(main.models.User user, boolean batch) {
        if (!isReadyToHarvest() || amount <= 0) return 0;

        // Use the base class collect logic.
        int yield = super.collect(user, batch);

        // Reset state for the next growth cycle.
        fertilizedRecently = false;
        setReadyToHarvest(false);
        setGrowEndTime(System.currentTimeMillis() + 90_000); // 90 seconds to grow again.

        return yield;
    }

    @Override
    public String getProduct() {
        // The product of a flower is the flower itself (e.g., "Rose", "Tulip").
        return name;
    }
}
