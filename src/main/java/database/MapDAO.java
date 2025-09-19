package database;

import main.models.TileMap;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class MapDAO {
    // Save the current map to player_maps table for the specific user
    public static void saveMapToDB(TileMap tileMap, String username) {
        try (Connection conn = DatabaseConnector.getConnection()) {
            // Remove previous map for this user (safe for small maps)
            String deleteSQL = "DELETE FROM player_maps WHERE username = ?";
            try (PreparedStatement del = conn.prepareStatement(deleteSQL)) {
                del.setString(1, username);
                del.executeUpdate();
            }
            // Insert all tiles (row, col, code)
            String insertSQL = "INSERT INTO player_maps (username, row, col, tile_code) VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insertSQL)) {
                for (int r = 0; r < tileMap.getRows(); r++) {
                    for (int c = 0; c < tileMap.getCols(); c++) {
                        ps.setString(1, username);
                        ps.setInt(2, r);
                        ps.setInt(3, c);
                        ps.setString(4, tileMap.getTileCode(r, c));
                        ps.addBatch();
                    }
                }
                ps.executeBatch();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Load the player's map from DB (player_maps table)
    public static void loadMapFromDB(main.models.TileMap tileMap, String username) {
        try (Connection conn = DatabaseConnector.getConnection()) {
            String sql = "SELECT row, col, tile_code FROM player_maps WHERE username = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int row = rs.getInt("row");
                        int col = rs.getInt("col");
                        String code = rs.getString("tile_code");
                        tileMap.replaceTile(row, col, code);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean hasMapInDB(String username) {
        try (Connection conn = DatabaseConnector.getConnection()) {
            String sql = "SELECT 1 FROM player_maps WHERE username = ? LIMIT 1";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}