package main.models;

/**
 * Represents an Animal on the farm (e.g., Cow, Chicken).
 * Animals can be fed to increase their product yield.
 */
public class Animal extends main.models.FarmElement {
    private boolean fedRecently = false;
    private long lastFedTime = 0;
    private static final long FEED_COOLDOWN_MS = 60_000; // 1 minute cooldown for feeding.

    public Animal(String name, int level, int amount) {
        super(Type.ANIMAL, name, level, amount);
        setReadyToHarvest(true); // Animals are ready to be collected from by default.
    }

    @Override
    public boolean feedOrWaterOrFertilize() {
        long now = System.currentTimeMillis();
        // Prevent feeding if it has been done within the cooldown period.
        if (fedRecently && now - lastFedTime < FEED_COOLDOWN_MS) return false;

        fed = true;
        fedRecently = true;
        lastFedTime = now;
        markAction("feed");
        return true;
    }

    /**
     * Collects products (like Milk or Eggs) from the animal.
     * KEY CHANGE: This no longer consumes the animal. It just starts a cooldown timer.
     * @param user The user performing the action.
     * @param batch Whether this is part of a mass-collection action.
     * @return The amount of product yielded.
     */
    @Override
    public int collect(main.models.User user, boolean batch) {
        if (!isReadyToHarvest()) return 0;

        // Use the base class logic to calculate yield, XP, and golden chance.
        int yield = super.collect(user, batch);

        // After collecting, reset state and start the timer for the next harvest.
        fed = false;
        fedRecently = false;
        setReadyToHarvest(false);
        setGrowEndTime(System.currentTimeMillis() + 45_000); // 45-second cooldown.

        return yield;
    }

    /**
     * Determines the product based on the animal's name and level.
     * For example, a high-level Cow might produce Cheese.
     * @return The name of the product as a String.
     */
    @Override
    public String getProduct() {
        return switch (name.toLowerCase()) {
            case "cow", "goat" -> (level >= 3) ? "Cheese" : "Milk";
            case "chicken", "turkey" -> "Egg";
            case "sheep" -> "Wool";
            default -> "Animal Product";
        };
    }
}
