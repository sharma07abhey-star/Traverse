package ticketing.model;

import java.time.LocalDateTime;

public class Booking implements Identifiable {
    private int id;
    private int passengerId;
    private int tripId;
    private int seatNo;
    private TravelClass travelClass;
    private double fare;
    private double baggageFee;
    private BookingStatus status;
    private LocalDateTime bookedAt;

    public Booking(int passengerId, int tripId, int seatNo, TravelClass travelClass,
                   double fare, double baggageFee) {
        this(0, passengerId, tripId, seatNo, travelClass, fare, baggageFee,
                BookingStatus.CONFIRMED, LocalDateTime.now());
    }

    public Booking(int id, int passengerId, int tripId, int seatNo, TravelClass travelClass,
                   double fare, double baggageFee, BookingStatus status, LocalDateTime bookedAt) {
        this.id = id;
        this.passengerId = passengerId;
        this.tripId = tripId;
        this.seatNo = seatNo;
        this.travelClass = travelClass;
        this.fare = fare;
        this.baggageFee = baggageFee;
        this.status = status;
        this.bookedAt = bookedAt;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    public int getPassengerId() {
        return passengerId;
    }

    public int getTripId() {
        return tripId;
    }

    public int getSeatNo() {
        return seatNo;
    }

    public TravelClass getTravelClass() {
        return travelClass;
    }

    public double getFare() {
        return fare;
    }

    public double getBaggageFee() {
        return baggageFee;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public LocalDateTime getBookedAt() {
        return bookedAt;
    }

    public double getTotal() {
        return fare + baggageFee;
    }

    public boolean isConfirmed() {
        return status == BookingStatus.CONFIRMED;
    }

    @Override
    public String toString() {
        return String.format("#%-3d passenger:%-3d trip:%-3d seat:%-4d %-9s fare: %9.2f  bag fee: %8.2f  total: %9.2f  %s",
                id, passengerId, tripId, seatNo, travelClass, fare, baggageFee, getTotal(), status);
    }
}
