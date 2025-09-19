package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * A utility class for creating a connection to the MySQL database.
 */
public class DatabaseConnector {

    // Database connection details.
    private static final String URL = "jdbc:mysql://localhost:3306/plow_and_paw";
    private static final String USER = "root";
    private static final String PASSWORD = ""; // Default for XAMPP

    /**
     * Establishes and returns a connection to the database.
     * @return A Connection object.
     * @throws SQLException if a database access error occurs.
     * @throws ClassNotFoundException if the MySQL JDBC driver is not found.
     */
    public static Connection getConnection() throws SQLException, ClassNotFoundException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
