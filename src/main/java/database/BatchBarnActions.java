package database;

import main.models.User;
import main.models.Animal;
import main.models.Barn;
import main.models.FarmElement;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class BatchBarnActions {

    public static void collectFromAll(User user) {
        Barn barn = user.getBarn();
        int totalCollected;

        for (FarmElement element : barn.getElements(FarmElement.Type.ANIMAL)) {
            if (element instanceof Animal) {
                Animal animal = (Animal) element;
                totalCollected = animal.collect(user, true); // batchCollect = true

                updateDatabase(user.getUsername(), animal.getName(), totalCollected);
            }
        }
    }

    private static void updateDatabase(String username, String animalName, int amount) {
        String sql = "UPDATE user_barn SET amount = amount + ? WHERE username = ? AND name = ?";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, amount);
            stmt.setString(2, username);
            stmt.setString(3, animalName);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}