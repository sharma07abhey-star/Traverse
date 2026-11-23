package ticketing.exception;

/** Thrown when a passenger, trip or booking does not exist. */
public class NotFoundException extends TicketingException {
    public NotFoundException(String message) {
        super(message);
    }
}
