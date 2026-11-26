package ticketing.model;

public class Baggage implements Identifiable {
    private int id;
    private int bookingId;
    private String tagNo;
    private double weightKg;
    private double fee;
    private BaggageStatus status;

    /** Used as the "poison pill" that tells the consumer thread to stop. */
    public Baggage() {
        this(0, 0, null, 0, 0, BaggageStatus.CHECKED_IN);
    }

    public Baggage(double weightKg, double fee) {
        this(0, 0, null, weightKg, fee, BaggageStatus.CHECKED_IN);
    }

    public Baggage(int id, int bookingId, String tagNo, double weightKg, double fee, BaggageStatus status) {
        this.id = id;
        this.bookingId = bookingId;
        this.tagNo = tagNo;
        this.weightKg = weightKg;
        this.fee = fee;
        this.status = status;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    public int getBookingId() {
        return bookingId;
    }

    public void setBookingId(int bookingId) {
        this.bookingId = bookingId;
    }

    public String getTagNo() {
        return tagNo;
    }

    public void setTagNo(String tagNo) {
        this.tagNo = tagNo;
    }

    public double getWeightKg() {
        return weightKg;
    }

    public double getFee() {
        return fee;
    }

    public BaggageStatus getStatus() {
        return status;
    }

    public void setStatus(BaggageStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return String.format("#%-3d tag:%-10s booking:%-3d %6.1f kg  fee: %8.2f  %s",
                id, tagNo, bookingId, weightKg, fee, status);
    }
}
