import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Same as TestConnection but authenticates as 'sa' — pure diagnostic to
 * determine whether the login failure is specific to GameNestAppLogin or
 * affects every SQL-authenticated login from Java/JDBC.
 *
 * Compile:  javac TestConnectionSa.java
 * Run:      java -cp ".;<path-to-mssql-jdbc-jar>" TestConnectionSa <sa-password>
 */
public class TestConnectionSa {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java TestConnectionSa <sa-password>");
            return;
        }
        String url = "jdbc:sqlserver://localhost:50308;encrypt=true;trustServerCertificate=true";
        String username = "sa";
        String password = args[0];

        System.out.println("Attempting connection as 'sa'...");

        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            System.out.println("SUCCESS! Connected to: " + conn.getCatalog());
        } catch (SQLException e) {
            System.out.println("FAILED: " + e.getMessage());
        }
    }
}
