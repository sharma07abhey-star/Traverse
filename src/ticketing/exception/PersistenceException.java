package ticketing.exception;

/** Wraps SQLException / IOException coming from the persistence layer. */
public class PersistenceException extends TicketingException {
    public PersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
