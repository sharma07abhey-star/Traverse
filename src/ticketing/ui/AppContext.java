package ticketing.ui;

import ticketing.concurrent.BaggageProcessor;
import ticketing.jdbc.DbConnection;
import ticketing.jdbc.JdbcBookingRepository;
import ticketing.jdbc.JdbcPassengerRepository;
import ticketing.jdbc.JdbcTripRepository;
import ticketing.jdbc.SchemaInitializer;
import ticketing.model.Passenger;
import ticketing.model.Trip;
import ticketing.repository.BookingRepository;
import ticketing.repository.InMemoryBookingRepository;
import ticketing.repository.InMemoryRepository;
import ticketing.repository.Repository;
import ticketing.service.BookingService;
import ticketing.service.PassengerService;
import ticketing.service.ReportService;
import ticketing.service.TripService;

/**
 * Wires everything together (repositories -> services -> baggage processor).
 * Tries JDBC/MySQL first; if the driver jar or server is missing it falls back to the collection-based store.
 */
public class AppContext {
    public static final String DB_CONFIG = "db.properties";

    public final String mode;
    public final boolean usingJdbc;
    public final PassengerService passengerService;
    public final TripService tripService;
    public final BookingService bookingService;
    public final ReportService reportService;
    public final BaggageProcessor baggageProcessor;

    private AppContext(String mode, boolean usingJdbc, Repository<Passenger> passengers, Repository<Trip> trips,
                       BookingRepository bookings, long loadDelayMillis) {
        this.mode = mode;
        this.usingJdbc = usingJdbc;
        this.baggageProcessor = new BaggageProcessor(bookings, 20, loadDelayMillis, false);
        this.passengerService = new PassengerService(passengers, bookings);
        this.tripService = new TripService(trips, bookings);
        this.bookingService = new BookingService(passengers, trips, bookings, baggageProcessor);
        this.reportService = new ReportService(passengers, trips, bookings);
    }

    public static AppContext create(boolean forceMemory) {
        long delay = 150;       // milliseconds a bag "takes" to be loaded, so the queue is visible
        if (!forceMemory) {
            try {
                DbConnection db = DbConnection.fromProperties(DB_CONFIG);
                SchemaInitializer.init(db);                    // fails here if no driver / MySQL not running
                return new AppContext("JDBC / MySQL (" + db.getUrl().split("\\?")[0] + ")", true,
                        new JdbcPassengerRepository(db), new JdbcTripRepository(db),
                        new JdbcBookingRepository(db), delay);
            } catch (Exception e) {
                System.out.println("[info] JDBC database not available (" + firstLine(e.getMessage()) + ").");
                System.out.println("[info] Start MySQL, check db.properties and put the MySQL connector jar in lib/.");
                System.out.println("[info] Falling back to in-memory mode for now.");
            }
        }
        InMemoryBookingRepository bookings = new InMemoryBookingRepository();
        return new AppContext("IN-MEMORY collections (data is lost on exit)", false,
                new InMemoryRepository<Passenger>(), new InMemoryRepository<Trip>(), bookings, delay);
    }

    private static String firstLine(String s) {
        if (s == null) {
            return "unknown error";
        }
        int i = s.indexOf('\n');
        return i < 0 ? s : s.substring(0, i);
    }

    public void shutdown() {
        baggageProcessor.awaitIdle();
        baggageProcessor.shutdown();
    }
}
