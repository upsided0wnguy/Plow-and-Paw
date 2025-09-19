package database;

import main.models.MapSource;
import java.sql.*;
import java.util.*;

public class MapSourceDAO {
    public static List<MapSource> loadSources(String username) {
        List<MapSource> sources = new ArrayList<>();
        try (Connection conn = DatabaseConnector.getConnection()) {
            String sql = "SELECT * FROM user_map_sources WHERE username = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                sources.add(new MapSource(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getInt("row"),
                        rs.getInt("col"),
                        rs.getString("code"),
                        rs.getString("type"),
                        rs.getString("name"),
                        rs.getInt("level")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sources;
    }

    public static void saveOrUpdate(MapSource source) {
        try (Connection conn = DatabaseConnector.getConnection()) {
            String sql = "INSERT INTO user_map_sources (username, row, col, code, type, name, level) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE level = VALUES(level)";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, source.getUsername());
            ps.setInt(2, source.getRow());
            ps.setInt(3, source.getCol());
            ps.setString(4, source.getCode());
            ps.setString(5, source.getType());
            ps.setString(6, source.getName());
            ps.setInt(7, source.getLevel());
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
