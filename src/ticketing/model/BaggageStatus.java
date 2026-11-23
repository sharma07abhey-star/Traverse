package ticketing.model;

/** CHECKED_IN (produced at check-in) -> LOADED (consumed by the loader thread) or CANCELLED. */
public enum BaggageStatus {
    CHECKED_IN, LOADED, CANCELLED
}
