package ticketing.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ticketing.exception.PersistenceException;
import ticketing.model.Trip;
import ticketing.repository.Repository;

public class JdbcTripRepository implements Repository<Trip> {
    private static final String COLUMNS = "id, flight_no, origin, destination, departure, total_seats, base_fare";
    private final DbConnection db;

    public JdbcTripRepository(DbConnection db) {
        this.db = db;
    }

    @Override
    public Trip save(Trip t) {
        String sql = "INSERT INTO trip(flight_no, origin, destination, departure, total_seats, base_fare)"
                + " VALUES (?, ?, ?, ?, ?, ?)";
        synchronized (DbConnection.WRITE_LOCK) {
            try (Connection c = db.open();
                 PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, t.getFlightNo());
                ps.setString(2, t.getOrigin());
                ps.setString(3, t.getDestination());
                ps.setTimestamp(4, Timestamp.valueOf(t.getDeparture()));
                ps.setInt(5, t.getTotalSeats());
                ps.setDouble(6, t.getBaseFare());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        t.setId(keys.getInt(1));
                    }
                }
                return t;
            } catch (SQLException e) {
                throw new PersistenceException("Could not save trip: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public Optional<Trip> findById(int id) {
        String sql = "SELECT " + COLUMNS + " FROM trip WHERE id = ?";
        try (Connection c = db.open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenceException("Could not read trip: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Trip> findAll() {
        String sql = "SELECT " + COLUMNS + " FROM trip ORDER BY id";
        List<Trip> list = new ArrayList<>();
        try (Connection c = db.open();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(map(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new PersistenceException("Could not list trips: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean update(Trip t) {
        String sql = "UPDATE trip SET flight_no = ?, origin = ?, destination = ?, departure = ?,"
                + " total_seats = ?, base_fare = ? WHERE id = ?";
        synchronized (DbConnection.WRITE_LOCK) {
            try (Connection c = db.open(); PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, t.getFlightNo());
                ps.setString(2, t.getOrigin());
                ps.setString(3, t.getDestination());
                ps.setTimestamp(4, Timestamp.valueOf(t.getDeparture()));
                ps.setInt(5, t.getTotalSeats());
                ps.setDouble(6, t.getBaseFare());
                ps.setInt(7, t.getId());
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                throw new PersistenceException("Could not update trip: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM trip WHERE id = ?";
        synchronized (DbConnection.WRITE_LOCK) {
            try (Connection c = db.open(); PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, id);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                throw new PersistenceException("Could not delete trip: " + e.getMessage(), e);
            }
        }
    }

    private Trip map(ResultSet rs) throws SQLException {
        return new Trip(rs.getInt("id"), rs.getString("flight_no"), rs.getString("origin"),
                rs.getString("destination"), rs.getTimestamp("departure").toLocalDateTime(),
                rs.getInt("total_seats"), rs.getDouble("base_fare"));
    }
}
