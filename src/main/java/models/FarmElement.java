package main.models;

import java.util.Random;

/**
 * Represents the base class for any item that can be stored in the Barn,
 * including animals, crops, products, seeds, etc.
 */
public class FarmElement {
    // Added PRODUCE type for collected items like "Wheat", "Milk", "Egg".
    public enum Type { ANIMAL, CROP, FRUIT, FLOWER, FISH, FEED, SEED, FERTILIZER, SAPLING, PRODUCE }

    protected Type type;
    protected String name;
    protected int level;
    protected int amount;
    protected String subType;

    // Gameplay state variables
    protected boolean readyToHarvest = true;
    protected boolean fertilized = false;
    protected boolean fed = false;
    protected boolean goldenReady = false;
    protected long growEndTime = 0;
    private long lastActionMillis = 0;

    public FarmElement(Type type, String name, int level, int amount) {
        this.type = type;
        this.name = name;
        this.level = Math.max(1, level);
        this.amount = Math.max(0, amount);
        this.subType = subType != null ? subType : type.name().toLowerCase();
    }

    // --- Main Gameplay Actions ---

    /**
     * Collects products from this element.
     * CRITICAL CHANGE: This method no longer decreases the amount of the producer.
     * It now returns the quantity of the product yielded.
     *
     * @param user The user performing the action, for XP and coin updates.
     * @param batch Whether this is part of a batch action (for bonus rewards).
     * @return The integer amount of the product collected.
     */
    public int collect(main.models.User user, boolean batch) {
        if (!isReadyToHarvest() || amount <= 0) return 0;

        int yield = Math.max(1, level); // Base yield is based on level.
        if (fertilized || fed) yield *= 2; // Double yield if fed/fertilized.

        // Chance for a golden product, which gives bonus coins.
        if ((fertilized || fed) && new Random().nextInt(5) == 0) { // 20% chance
            goldenReady = true;
            if (user != null) user.setCoins(user.getCoins() + 20 * level);
        }

        if (batch && user != null) user.setCoins(user.getCoins() + level); // Small coin bonus for batch actions.
        if (user != null) user.setXp(user.getXp() + 2 * level + (fertilized || fed ? 5 : 0)); // XP gain.

        // Reset state for the next growth cycle.
        readyToHarvest = false;
        fertilized = false;
        fed = false;
        // The specific grow time is set in the subclasses (e.g., Fish, Crop).
        markAction("collect");

        return yield;
    }

    /**
     * Feeds, waters, or fertilizes the element.
     * @return true if the action was successful, false otherwise (e.g., on cooldown).
     */
    public boolean feedOrWaterOrFertilize() {
        // This base method can be overridden by subclasses with specific cooldowns.
        if (type == Type.ANIMAL || type == Type.FISH) {
            fed = true;
        } else if (type == Type.CROP || type == Type.FLOWER || type == Type.FRUIT) {
            fertilized = true;
        } else {
            return false; // Not a feedable/fertilizable type.
        }
        markAction("feed/fertilize");
        return true;
    }

    /**
     * Plants a seed, starting its growth timer.
     * @param growMillis The time in milliseconds it takes for the seed to grow.
     * @return true if planting was successful.
     */
    public boolean plant(long growMillis) {
        if (type != Type.SEED) return false;
        readyToHarvest = false;
        growEndTime = System.currentTimeMillis() + growMillis;
        markAction("plant");
        return true;
    }

    public void upgrade() {
        setLevel(level + 1);
        markAction("upgrade");
    }

    // --- Getters and Setters ---

    public String getProduct() {
        // Default implementation. Subclasses provide more specific product names.
        return this.name;
    }

    public Type getType() { return type; }
    public String getName() { return name; }
    public int getLevel() { return level; }
    public int getAmount() { return amount; }
    public void setLevel(int lvl) { this.level = Math.max(1, lvl); }
    public void setAmount(int amt) { this.amount = Math.max(0, amt); }
    public String getSubType() { return subType; }
    public long getGrowEndTime() { return growEndTime; }
    public void setGrowEndTime(long time) { this.growEndTime = time; }
    public void setReadyToHarvest(boolean ready) { this.readyToHarvest = ready; }
    public Type getTypeEnum() { return type; }
    public int getUpgradeCost() { return 100 * getLevel(); }
    public int getSeedCost() { return 5 * level; }


    // --- Status and UI Methods ---
    public void markAction(String actionType) {
        this.lastActionMillis = System.currentTimeMillis();
    }
    public boolean isRecentlyActed() {
        return System.currentTimeMillis() - lastActionMillis < 2000; // Highlight for 2 seconds.
    }
    public boolean isReadyToHarvest() {
        // Products and materials are always "ready".
        if (type == Type.PRODUCE || type == Type.FEED || type == Type.FERTILIZER) return true;
        // For growing things, check the timer.
        if (!readyToHarvest && System.currentTimeMillis() > growEndTime) {
            readyToHarvest = true;
        }
        return readyToHarvest;
    }
    public boolean isGoldenReady() { return goldenReady; }
    public void clearGolden() { goldenReady = false; }
    public String getStatus() {
        if (isGoldenReady()) return "Golden!";
        if (isReadyToHarvest()) {
            return "Ready";
        }
        if (growEndTime > System.currentTimeMillis()) return "Growing";
        if (fed) return "Fed";
        if (fertilized) return "Fertilized";
        return "Idle";
    }
}
