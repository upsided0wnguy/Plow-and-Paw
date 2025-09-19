package main.models;

/**
 * Represents a Crop on the farm (e.g., Wheat Field, Tomato Plant).
 * Crops can be fertilized for a better harvest.
 */
public class Crop extends main.models.FarmElement {
    private boolean fertilizedRecently = false;
    private long lastFertilizedTime = 0;
    private static final long FERT_COOLDOWN_MS = 60_000; // 1 minute cooldown for fertilizing.

    public Crop(String name, int level, int amount) {
        super(Type.CROP, name, level, amount);
        setReadyToHarvest(true); // Crops start ready for their first harvest.
    }

    @Override
    public boolean feedOrWaterOrFertilize() {
        long now = System.currentTimeMillis();
        if (fertilizedRecently && now - lastFertilizedTime < FERT_COOLDOWN_MS) return false;

        fertilized = true;
        fertilizedRecently = true;
        lastFertilizedTime = now;
        markAction("fertilize");
        return true;
    }

    /**
     * Harvests the crop.
     * KEY CHANGE: This no longer consumes the crop field. It puts it on a regrowth timer.
     * @param user The user performing the action.
     * @param batch Whether this is a mass-harvest action.
     * @return The amount of product yielded.
     */
    @Override
    public int collect(main.models.User user, boolean batch) {
        if (!isReadyToHarvest()) return 0;

        int yield = super.collect(user, batch);

        // Reset state for the next growth cycle.
        fertilized = false;
        fertilizedRecently = false;
        setReadyToHarvest(false);
        setGrowEndTime(System.currentTimeMillis() + 75_000); // 75-second regrowth time.

        return yield;
    }

    /**
     * Gets the product from the crop field.
     * E.g., A "Wheat Field" produces "Wheat".
     * @return The name of the harvested crop.
     */
    @Override
    public String getProduct() {
        if (name != null) {
            return name.replace(" Field", "").replace(" Plant", "").trim();
        }
        return "Crop Product"; // Fallback
    }
}
