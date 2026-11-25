package ticketing.jdbc;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * JDBC architecture in one class: JDBC URL + user + password -> DriverManager -> Connection (MySQL).
 * Settings are read from db.properties (url, user, password); defaults point to a local MySQL server.
 * A new Connection is opened per operation (and closed by try-with-resources).
 */
public class DbConnection {
    public static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/ticketing_db?createDatabaseIfNotExist=true&useSSL=false"
                    + "&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    public static final String DEFAULT_USER = "root";
    public static final String DEFAULT_PASSWORD = "";

    /** Serialises writes inside this JVM; the UNIQUE key in MySQL protects against other clients too. */
    public static final Object WRITE_LOCK = new Object();

    private final String url;
    private final String user;
    private final String password;

    public DbConnection(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    /** Loads db.properties (file handling + Properties); missing file => defaults. */
    public static DbConnection fromProperties(String path) {
        Properties props = new Properties();
        try (InputStream in = new FileInputStream(path)) {
            props.load(in);
        } catch (IOException e) {
            System.out.println("[info] " + path + " not found, using default MySQL settings.");
        }
        return new DbConnection(props.getProperty("url", DEFAULT_URL).trim(),
                props.getProperty("user", DEFAULT_USER).trim(),
                props.getProperty("password", DEFAULT_PASSWORD));
    }

    public String getUrl() {
        return url;
    }

    public Connection open() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }
}
