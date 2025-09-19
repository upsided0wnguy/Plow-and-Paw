package main.models;

/**
 * Represents Fish in the farm. Fish can be fed to improve their yield.
 */
public class Fish extends main.models.FarmElement {
    private boolean fedRecently = false;
    private long lastFedTime = 0;
    private static final long FEED_COOLDOWN_MS = 60_000; // 1 minute cooldown for feeding.

    public Fish(String name, int level, int amount) {
        super(Type.FISH, name, level, amount);
        // Fish start as ready to harvest.
        setReadyToHarvest(true);
    }

    @Override
    public boolean feedOrWaterOrFertilize() {
        long now = System.currentTimeMillis();
        // Prevent feeding if it has been fed recently.
        if (fedRecently && now - lastFedTime < FEED_COOLDOWN_MS) return false;

        fed = true;
        fedRecently = true;
        lastFedTime = now;
        markAction("feed");
        return true;
    }

    @Override
    public int collect(main.models.User user, boolean batch) {
        if (!isReadyToHarvest() || amount <= 0) return 0;

        // Use the base class collect logic for yield, XP, and golden chance.
        int yield = super.collect(user, batch);

        // After collecting, reset the state and start the timer for the next harvest.
        fedRecently = false; // Cooldown is reset.
        setReadyToHarvest(false);
        setGrowEndTime(System.currentTimeMillis() + 70_000); // 70 seconds to be ready again.

        return yield;
    }

    @Override
    public String getProduct() {
        // The product of a fish pond is simply "Fish".
        return "Fish";
    }
}
