package ticketing.util;

/** Builds baggage tag numbers, e.g. booking 7, second bag -> BG00007-2. Unique because booking ids are unique. */
public final class TagGenerator {
    private TagGenerator() {
    }

    public static String tag(int bookingId, int bagNumber) {
        return String.format("BG%05d-%d", bookingId, bagNumber);
    }
}
