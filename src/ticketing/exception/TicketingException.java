package ticketing.exception;

/** Base class for all application-specific (unchecked) exceptions. */
public class TicketingException extends RuntimeException {
    public TicketingException(String message) {
        super(message);
    }

    public TicketingException(String message, Throwable cause) {
        super(message, cause);
    }
}
