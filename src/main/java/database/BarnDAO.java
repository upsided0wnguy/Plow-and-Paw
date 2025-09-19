package database;

import main.models.Barn;
import main.models.FarmElement;
import main.models.User;
import java.sql.*;

public class BarnDAO {

    /**
     * Saves the entire game state for a given user in a single transaction.
     * This includes user progress (coins, xp, level) and all barn inventory.
     * @param user The user object containing all data to be saved.
     */
    public static void saveGame(User user) {
        if (user == null || user.getBarn() == null) return;
        String username = user.getUsername().toLowerCase();

        String progressSql = "INSERT INTO user_progress (username, coins, gems, xp, level, barnLevel) VALUES (?,?,?,?,?,?) " +
                "ON DUPLICATE KEY UPDATE coins=VALUES(coins), gems=VALUES(gems), xp=VALUES(xp), level=VALUES(level), barnLevel=VALUES(barnLevel)";

        String deleteInventorySql = "DELETE FROM user_inventory WHERE username = ?";

        String insertInventorySql = "INSERT INTO user_inventory (username, type, name, amount, level, subType, growEndTime) VALUES (?,?,?,?,?,?,?)";

        Connection conn = null;
        try {
            conn = DatabaseConnector.getConnection();
            conn.setAutoCommit(false); // Start transaction

            // 1. Save User Progress
            try (PreparedStatement ps = conn.prepareStatement(progressSql)) {
                ps.setString(1, username);
                ps.setInt(2, user.getCoins());
                ps.setInt(3, user.getGems());
                ps.setInt(4, user.getXp());
                ps.setInt(5, user.getLevel());
                ps.setInt(6, user.getBarn().getLevel());
                ps.executeUpdate();
            }

            // 2. Clear old inventory
            try (PreparedStatement ps = conn.prepareStatement(deleteInventorySql)) {
                ps.setString(1, username);
                ps.executeUpdate();
            }

            // 3. Insert all current inventory items
            try (PreparedStatement ps = conn.prepareStatement(insertInventorySql)) {
                for (FarmElement elem : user.getBarn().getAllElements()) {
                    if (elem.getAmount() <= 0) continue; // Do not save empty stacks
                    ps.setString(1, username);
                    ps.setString(2, elem.getType().name());
                    ps.setString(3, elem.getName());
                    ps.setInt(4, elem.getAmount());
                    ps.setInt(5, elem.getLevel());
                    ps.setString(6, elem.getSubType());
                    ps.setLong(7, elem.getGrowEndTime());
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            conn.commit(); // Finalize all changes

        } catch (Exception e) {
            e.printStackTrace();
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Loads the entire game state for a user from the database.
     * @param user The user object to populate with loaded data.
     */
    public static void loadGame(User user) {
        if (user == null) return;
        String username = user.getUsername().toLowerCase();

        String progressSql = "SELECT * FROM user_progress WHERE username = ?";
        String inventorySql = "SELECT * FROM user_inventory WHERE username = ?";

        try (Connection conn = DatabaseConnector.getConnection()) {

            // 1. Load User Progress and initialize Barn
            try (PreparedStatement ps = conn.prepareStatement(progressSql)) {
                ps.setString(1, username);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    user.setCoins(rs.getInt("coins"));
                    user.setGems(rs.getInt("gems"));
                    user.setXp(rs.getInt("xp"));
                    user.setLevel(rs.getInt("level"));
                    user.setBarn(new Barn(rs.getInt("barnLevel")));
                } else {
                    // If no progress, it's a new player. Create default barn.
                    user.setBarn(new Barn(1));
                }
            }

            // 2. Load Barn Inventory
            Barn barn = user.getBarn();
            barn.clearAll();

            try (PreparedStatement ps = conn.prepareStatement(inventorySql)) {
                ps.setString(1, username);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    FarmElement.Type type = FarmElement.Type.valueOf(rs.getString("type"));
                    String name = rs.getString("name");
                    int amount = rs.getInt("amount");
                    int level = rs.getInt("level");
                    long growEndTime = rs.getLong("growEndTime");

                    // The Barn's addElement method handles creating the correct subclass.
                    barn.addElement(type, name, amount, level);

                    // Restore the timer state for the newly added element.
                    barn.getAllElements().stream()
                            .filter(e -> e.getName().equalsIgnoreCase(name) && e.getType() == type && e.getLevel() == level)
                            .findFirst()
                            .ifPresent(e -> {
                                e.setGrowEndTime(growEndTime);
                                // If timer has passed while offline, mark as ready.
                                if (growEndTime > 0 && System.currentTimeMillis() > growEndTime) {
                                    e.setReadyToHarvest(true);
                                } else if(growEndTime == 0) {
                                    e.setReadyToHarvest(true); // Default to ready if no timer set.
                                } else {
                                    e.setReadyToHarvest(false);
                                }
                            });
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void loadBarn(main.models.User user) {
    }

    public static void saveBarn(main.models.User user) {
    }
}