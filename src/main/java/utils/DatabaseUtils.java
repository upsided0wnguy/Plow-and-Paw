package utils;

import database.DatabaseConnector;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import main.models.Barn;
import main.models.User;

import static database.DatabaseConnector.getConnection;

public class DatabaseUtils {
    public static boolean validateLogin(String username, String password) {
        try (Connection conn = getConnection()) {
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM users WHERE username = ? AND password = ?");
            ps.setString(1, username);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();
            return rs.next(); // true if match found
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // You might need to adapt table/column names to match your schema!
    public static User loadUser(String username) {
        try (Connection conn = getConnection()) {
            // If you store user progress (coins, gems, xp, level) in user_progress:
            String sql = "SELECT full_name, username, level, coins, gems, xp FROM user_progress WHERE username=?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String fullName = rs.getString("full_name"); // If your schema has full_name
                int level = rs.getInt("level");
                int coins = rs.getInt("coins");
                int gems = rs.getInt("gems");
                int xp = rs.getInt("xp");
                // Use new Barn(1) for now; real barn is loaded by BarnDAO.loadBarn()
                return new User(fullName, username, level, coins, gems, xp, new Barn(1));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
