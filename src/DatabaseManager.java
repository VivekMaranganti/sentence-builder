import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {
    private static final String URL = "jdbc:mysql://localhost:3306/sentence_builder";
    private static final String USER = "root";
    public static Connection getConnection() throws SQLException {
        String password = System.getenv("DB_PASSWORD");
        if (password == null || password.isBlank()) {
            throw new SQLException("Set the DB_PASSWORD environment variable before connecting.");
        }
        return DriverManager.getConnection(URL, USER, password);
    }

    public static void main(String[] args) {
        try (Connection conn = getConnection()) {
            if (conn != null) {
                System.out.println("Successfully connected to sentence_builder database!");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}