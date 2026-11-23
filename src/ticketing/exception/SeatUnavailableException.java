package ticketing.exception;

/** Thrown when a seat is already booked (prevents double-booking). */
public class SeatUnavailableException extends TicketingException {
    public SeatUnavailableException(String message) {
        super(message);
    }
}
