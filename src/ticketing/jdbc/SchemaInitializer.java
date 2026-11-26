package ticketing.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import ticketing.exception.PersistenceException;

/** Creates the MySQL tables on first run (CREATE TABLE IF NOT EXISTS). Needs MySQL 8.0 / 5.7+. */
public final class SchemaInitializer {
    private SchemaInitializer() {
    }

    private static final String[] DDL = {
            "CREATE TABLE IF NOT EXISTS passenger ("
                    + " id INT AUTO_INCREMENT PRIMARY KEY,"
                    + " name VARCHAR(100) NOT NULL,"
                    + " email VARCHAR(120) NOT NULL,"
                    + " phone VARCHAR(15) NOT NULL,"
                    + " passport_no VARCHAR(20) NOT NULL UNIQUE"
                    + ") ENGINE=InnoDB",

            "CREATE TABLE IF NOT EXISTS trip ("
                    + " id INT AUTO_INCREMENT PRIMARY KEY,"
                    + " flight_no VARCHAR(20) NOT NULL,"
                    + " origin VARCHAR(60) NOT NULL,"
                    + " destination VARCHAR(60) NOT NULL,"
                    + " departure DATETIME NOT NULL,"
                    + " total_seats INT NOT NULL CHECK (total_seats > 0),"
                    + " base_fare DOUBLE NOT NULL CHECK (base_fare > 0)"
                    + ") ENGINE=InnoDB",

            // active_key is 1 for CONFIRMED rows and NULL otherwise. MySQL treats NULLs as distinct in a
            // UNIQUE index, so the index below allows only ONE confirmed booking per (trip, seat) while
            // cancelled bookings never block the seat. This is the last line of defence against double booking.
            "CREATE TABLE IF NOT EXISTS booking ("
                    + " id INT AUTO_INCREMENT PRIMARY KEY,"
                    + " passenger_id INT NOT NULL,"
                    + " trip_id INT NOT NULL,"
                    + " seat_no INT NOT NULL,"
                    + " travel_class VARCHAR(10) NOT NULL,"
                    + " fare DOUBLE NOT NULL,"
                    + " baggage_fee DOUBLE NOT NULL,"
                    + " status VARCHAR(10) NOT NULL,"
                    + " booked_at DATETIME NOT NULL,"
                    + " active_key TINYINT GENERATED ALWAYS AS (IF(status = 'CONFIRMED', 1, NULL)) STORED,"
                    + " UNIQUE KEY ux_active_seat (trip_id, seat_no, active_key),"
                    + " FOREIGN KEY (passenger_id) REFERENCES passenger(id),"
                    + " FOREIGN KEY (trip_id) REFERENCES trip(id)"
                    + ") ENGINE=InnoDB",

            "CREATE TABLE IF NOT EXISTS baggage ("
                    + " id INT AUTO_INCREMENT PRIMARY KEY,"
                    + " booking_id INT NOT NULL,"
                    + " tag_no VARCHAR(20) NOT NULL,"
                    + " weight_kg DOUBLE NOT NULL CHECK (weight_kg > 0),"
                    + " fee DOUBLE NOT NULL,"
                    + " status VARCHAR(12) NOT NULL,"
                    + " FOREIGN KEY (booking_id) REFERENCES booking(id)"
                    + ") ENGINE=InnoDB"
    };

    public static void init(DbConnection db) {
        try (Connection c = db.open(); Statement st = c.createStatement()) {
            for (String sql : DDL) {
                st.executeUpdate(sql);
            }
        } catch (SQLException e) {
            throw new PersistenceException("Could not initialise database: " + e.getMessage(), e);
        }
    }
}
