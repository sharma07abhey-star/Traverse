package ticketing.exception;

/** Thrown when user input or imported data fails validation. */
public class ValidationException extends TicketingException {
    public ValidationException(String message) {
        super(message);
    }
}
