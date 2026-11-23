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
import ticketing.exception.SeatUnavailableException;
import ticketing.model.Baggage;
import ticketing.model.BaggageStatus;
import ticketing.model.Booking;
import ticketing.model.BookingStatus;
import ticketing.model.TravelClass;
import ticketing.repository.BookingRepository;
import ticketing.util.TagGenerator;

/**
 * Booking + baggage persistence. The important methods (saveWithBaggage, cancelBooking) run as
 * JDBC transactions: setAutoCommit(false) ... commit(), or rollback() on any failure.
 */
public class JdbcBookingRepository implements BookingRepository {
    private static final String BOOKING_COLUMNS =
            "id, passenger_id, trip_id, seat_no, travel_class, fare, baggage_fee, status, booked_at";
    private static final String BAGGAGE_COLUMNS = "id, booking_id, tag_no, weight_kg, fee, status";

    private final DbConnection db;

    public JdbcBookingRepository(DbConnection db) {
        this.db = db;
    }

    // ---------------------------------------------------------------- transactional operations

    @Override
    public Booking saveWithBaggage(Booking booking, List<Baggage> baggage) {
        synchronized (DbConnection.WRITE_LOCK) {
            try (Connection c = db.open()) {
                c.setAutoCommit(false);                     // begin transaction
                try {
                    insertBooking(c, booking);
                    int n = 1;
                    for (Baggage b : baggage) {
                        b.setBookingId(booking.getId());
                        b.setTagNo(TagGenerator.tag(booking.getId(), n++));
                        insertBaggage(c, b);
                    }
                    c.commit();                             // both booking and baggage saved
                    return booking;
                } catch (SQLException e) {
                    c.rollback();                           // nothing saved
                    // MySQL error 1062 = "Duplicate entry" for the unique (trip, seat) key
                    if (e.getErrorCode() == 1062 || "23000".equals(e.getSQLState())
                            && e.getMessage() != null && e.getMessage().contains("Duplicate")) {
                        throw new SeatUnavailableException("Seat " + booking.getSeatNo() + " is already booked");
                    }
                    throw new PersistenceException("Booking rolled back: " + e.getMessage(), e);
                }
            } catch (SQLException e) {
                throw new PersistenceException("Database error: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public boolean cancelBooking(int bookingId) {
        String cancelBooking = "UPDATE booking SET status = 'CANCELLED' WHERE id = ? AND status = 'CONFIRMED'";
        String cancelBags = "UPDATE baggage SET status = 'CANCELLED' WHERE booking_id = ?";
        synchronized (DbConnection.WRITE_LOCK) {
            try (Connection c = db.open()) {
                c.setAutoCommit(false);
                try (PreparedStatement p1 = c.prepareStatement(cancelBooking);
                     PreparedStatement p2 = c.prepareStatement(cancelBags)) {
                    p1.setInt(1, bookingId);
                    if (p1.executeUpdate() == 0) {
                        c.rollback();
                        return false;
                    }
                    p2.setInt(1, bookingId);
                    p2.executeUpdate();
                    c.commit();
                    return true;
                } catch (SQLException e) {
                    c.rollback();
                    throw new PersistenceException("Cancel rolled back: " + e.getMessage(), e);
                }
            } catch (SQLException e) {
                throw new PersistenceException("Database error: " + e.getMessage(), e);
            }
        }
    }

    private void insertBooking(Connection c, Booking b) throws SQLException {
        String sql = "INSERT INTO booking(passenger_id, trip_id, seat_no, travel_class, fare, baggage_fee,"
                + " status, booked_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, b.getPassengerId());
            ps.setInt(2, b.getTripId());
            ps.setInt(3, b.getSeatNo());
            ps.setString(4, b.getTravelClass().name());
            ps.setDouble(5, b.getFare());
            ps.setDouble(6, b.getBaggageFee());
            ps.setString(7, b.getStatus().name());
            ps.setTimestamp(8, Timestamp.valueOf(b.getBookedAt()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    b.setId(keys.getInt(1));
                }
            }
        }
    }

    private void insertBaggage(Connection c, Baggage g) throws SQLException {
        String sql = "INSERT INTO baggage(booking_id, tag_no, weight_kg, fee, status) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, g.getBookingId());
            ps.setString(2, g.getTagNo());
            ps.setDouble(3, g.getWeightKg());
            ps.setDouble(4, g.getFee());
            ps.setString(5, g.getStatus().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    g.setId(keys.getInt(1));
                }
            }
        }
    }

    // ---------------------------------------------------------------- simple CRUD

    /** Bookings are only created through saveWithBaggage, so plain save() delegates with no baggage. */
    @Override
    public Booking save(Booking booking) {
        return saveWithBaggage(booking, new ArrayList<>());
    }

    @Override
    public Optional<Booking> findById(int id) {
        String sql = "SELECT " + BOOKING_COLUMNS + " FROM booking WHERE id = ?";
        try (Connection c = db.open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapBooking(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenceException("Could not read booking: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Booking> findAll() {
        String sql = "SELECT " + BOOKING_COLUMNS + " FROM booking ORDER BY id";
        List<Booking> list = new ArrayList<>();
        try (Connection c = db.open();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapBooking(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new PersistenceException("Could not list bookings: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean update(Booking b) {
        String sql = "UPDATE booking SET fare = ?, baggage_fee = ?, status = ? WHERE id = ?";
        synchronized (DbConnection.WRITE_LOCK) {
            try (Connection c = db.open(); PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setDouble(1, b.getFare());
                ps.setDouble(2, b.getBaggageFee());
                ps.setString(3, b.getStatus().name());
                ps.setInt(4, b.getId());
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                throw new PersistenceException("Could not update booking: " + e.getMessage(), e);
            }
        }
    }

    /** Bookings are cancelled, never physically deleted (they are needed for revenue history). */
    @Override
    public boolean delete(int id) {
        return cancelBooking(id);
    }

    @Override
    public boolean isSeatTaken(int tripId, int seatNo) {
        String sql = "SELECT COUNT(*) FROM booking WHERE trip_id = ? AND seat_no = ? AND status = 'CONFIRMED'";
        try (Connection c = db.open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, tripId);
            ps.setInt(2, seatNo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new PersistenceException("Could not check seat: " + e.getMessage(), e);
        }
    }

    // ---------------------------------------------------------------- baggage

    @Override
    public List<Baggage> findBaggageByBooking(int bookingId) {
        String sql = "SELECT " + BAGGAGE_COLUMNS + " FROM baggage WHERE booking_id = ? ORDER BY id";
        List<Baggage> list = new ArrayList<>();
        try (Connection c = db.open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapBaggage(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new PersistenceException("Could not read baggage: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Baggage> findAllBaggage() {
        String sql = "SELECT " + BAGGAGE_COLUMNS + " FROM baggage ORDER BY id";
        List<Baggage> list = new ArrayList<>();
        try (Connection c = db.open();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapBaggage(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new PersistenceException("Could not list baggage: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean markLoaded(int baggageId) {
        String sql = "UPDATE baggage SET status = 'LOADED' WHERE id = ? AND status = 'CHECKED_IN'";
        synchronized (DbConnection.WRITE_LOCK) {
            try (Connection c = db.open(); PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, baggageId);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                throw new PersistenceException("Could not update baggage: " + e.getMessage(), e);
            }
        }
    }

    // ---------------------------------------------------------------- ResultSet -> object mapping

    private Booking mapBooking(ResultSet rs) throws SQLException {
        return new Booking(rs.getInt("id"), rs.getInt("passenger_id"), rs.getInt("trip_id"),
                rs.getInt("seat_no"), TravelClass.valueOf(rs.getString("travel_class")),
                rs.getDouble("fare"), rs.getDouble("baggage_fee"),
                BookingStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("booked_at").toLocalDateTime());
    }

    private Baggage mapBaggage(ResultSet rs) throws SQLException {
        return new Baggage(rs.getInt("id"), rs.getInt("booking_id"), rs.getString("tag_no"),
                rs.getDouble("weight_kg"), rs.getDouble("fee"),
                BaggageStatus.valueOf(rs.getString("status")));
    }
}
