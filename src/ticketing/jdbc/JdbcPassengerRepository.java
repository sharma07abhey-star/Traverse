package ticketing.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ticketing.exception.PersistenceException;
import ticketing.model.Passenger;
import ticketing.repository.Repository;

/** CRUD for passengers using PreparedStatement (parameter binding => no SQL injection). */
public class JdbcPassengerRepository implements Repository<Passenger> {
    private final DbConnection db;

    public JdbcPassengerRepository(DbConnection db) {
        this.db = db;
    }

    @Override
    public Passenger save(Passenger p) {
        String sql = "INSERT INTO passenger(name, email, phone, passport_no) VALUES (?, ?, ?, ?)";
        synchronized (DbConnection.WRITE_LOCK) {
            try (Connection c = db.open();
                 PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, p.getName());
                ps.setString(2, p.getEmail());
                ps.setString(3, p.getPhone());
                ps.setString(4, p.getPassportNo());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        p.setId(keys.getInt(1));
                    }
                }
                return p;
            } catch (SQLException e) {
                throw new PersistenceException("Could not save passenger: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public Optional<Passenger> findById(int id) {
        String sql = "SELECT id, name, email, phone, passport_no FROM passenger WHERE id = ?";
        try (Connection c = db.open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenceException("Could not read passenger: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Passenger> findAll() {
        String sql = "SELECT id, name, email, phone, passport_no FROM passenger ORDER BY id";
        List<Passenger> list = new ArrayList<>();
        try (Connection c = db.open();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {            // cursor iteration
                list.add(map(rs));         // row -> Java object mapping
            }
            return list;
        } catch (SQLException e) {
            throw new PersistenceException("Could not list passengers: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean update(Passenger p) {
        String sql = "UPDATE passenger SET name = ?, email = ?, phone = ?, passport_no = ? WHERE id = ?";
        synchronized (DbConnection.WRITE_LOCK) {
            try (Connection c = db.open(); PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, p.getName());
                ps.setString(2, p.getEmail());
                ps.setString(3, p.getPhone());
                ps.setString(4, p.getPassportNo());
                ps.setInt(5, p.getId());
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                throw new PersistenceException("Could not update passenger: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM passenger WHERE id = ?";
        synchronized (DbConnection.WRITE_LOCK) {
            try (Connection c = db.open(); PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, id);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                throw new PersistenceException("Could not delete passenger: " + e.getMessage(), e);
            }
        }
    }

    private Passenger map(ResultSet rs) throws SQLException {
        return new Passenger(rs.getInt("id"), rs.getString("name"), rs.getString("email"),
                rs.getString("phone"), rs.getString("passport_no"));
    }
}
