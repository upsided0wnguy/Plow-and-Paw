package main.models;

/**
 * Represents a player in the game.
 * This class holds all the core information about a user, including their
 * identity, progress (level, XP, currency), and their barn inventory.
 */
public class User {
    private String fullName;
    private String username;
    private int level;
    private int coins;
    private int gems;
    private int xp;
    private main.models.Barn barn;

    /**
     * Main constructor for creating a user instance.
     * @param fullName The user's full name.
     * @param username The user's unique username (will be converted to lowercase).
     * @param level The user's current level.
     * @param coins The amount of coins the user has.
     * @param gems The amount of gems the user has.
     * @param xp The user's current experience points.
     * @param barn The user's barn object. If null, a new default barn is created.
     */
    public User(String fullName, String username, int level, int coins, int gems, int xp, main.models.Barn barn) {
        this.fullName = fullName;
        this.setUsername(username); // Use setter for lowercase enforcement
        this.level = Math.max(1, level);
        this.coins = coins;
        this.gems = gems;
        this.xp = xp;
        this.barn = barn != null ? barn : new main.models.Barn(1);
    }

    /**
     * Simplified constructor for creating a new user with default values.
     */
    public User(String fullName, String username, int level) {
        this(fullName, username, level, 100, 5, 0, new main.models.Barn(1)); // Start with 100 coins, 5 gems.
    }

    // --- Core Gameplay Logic ---

    /**
     * Adds experience points to the user and handles leveling up.
     * @param amount The amount of XP to add.
     */
    public void addXp(int amount) {
        if (amount <= 0) return;
        this.xp += amount;

        // Check for level up. The required XP for the next level could be a formula.
        // Example formula: 100 * level^1.5
        int xpForNextLevel = (int) (100 * Math.pow(level, 1.5));

        while (this.xp >= xpForNextLevel) {
            this.level++;
            this.xp -= xpForNextLevel; // Subtract the XP needed for that level.

            // Give a reward for leveling up.
            this.coins += 50 * this.level;
            this.gems += 5;

            // Recalculate XP needed for the new current level.
            xpForNextLevel = (int) (100 * Math.pow(level, 1.5));
        }
    }

    // --- Getters ---
    public String getFullName() { return fullName; }
    public String getUsername() { return username; }
    public int getLevel() { return level; }
    public int getCoins() { return coins; }
    public int getGems() { return gems; }
    public int getXp() { return xp; }
    public main.models.Barn getBarn() { return barn; }

    // --- Setters ---
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setUsername(String username) {
        this.username = (username != null) ? username.toLowerCase() : null;
    }
    public void setLevel(int level) { this.level = level; }
    public void setCoins(int coins) { this.coins = coins; }
    public void setGems(int gems) { this.gems = gems; }
    public void setXp(int xp) { this.xp = xp; }
    public void setBarn(main.models.Barn barn) { this.barn = barn; }
}
