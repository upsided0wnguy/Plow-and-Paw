package database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import main.models.User;
import utils.DBUtil; // Utility class for DB connection

public class SaveLoadDAO {
    // Load user state from DB by username (or ID)
    public static void loadUserState(User user) {
        try (Connection conn = DBUtil.getConnection()) {
            String sql = "SELECT full_name, username, level FROM users WHERE username = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, user.getUsername());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                // Populate user state (add more fields as needed)
                String fullName = rs.getString("full_name");
                int level = rs.getInt("level");
                // No setters in your User class, so you may need to create a new User instance or update the reference
                // If you make User mutable, set the fields here. Else, update reference in SessionManager.
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Save user state to DB
    public static void saveUserState(User user) {
        try (Connection conn = DBUtil.getConnection()) {
            String sql = "UPDATE users SET full_name=?, level=? WHERE username=?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, user.getFullName());
            stmt.setInt(2, user.getLevel());
            stmt.setString(3, user.getUsername());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
