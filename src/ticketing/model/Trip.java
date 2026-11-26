package ticketing.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Trip implements Identifiable {
    public static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private int id;
    private String flightNo;
    private String origin;
    private String destination;
    private LocalDateTime departure;
    private int totalSeats;
    private double baseFare;

    public Trip(String flightNo, String origin, String destination,
                LocalDateTime departure, int totalSeats, double baseFare) {
        this(0, flightNo, origin, destination, departure, totalSeats, baseFare);
    }

    public Trip(int id, String flightNo, String origin, String destination,
                LocalDateTime departure, int totalSeats, double baseFare) {
        this.id = id;
        this.flightNo = flightNo;
        this.origin = origin;
        this.destination = destination;
        this.departure = departure;
        this.totalSeats = totalSeats;
        this.baseFare = baseFare;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    public String getFlightNo() {
        return flightNo;
    }

    public String getOrigin() {
        return origin;
    }

    public String getDestination() {
        return destination;
    }

    public LocalDateTime getDeparture() {
        return departure;
    }

    public void setDeparture(LocalDateTime departure) {
        this.departure = departure;
    }

    public int getTotalSeats() {
        return totalSeats;
    }

    public void setTotalSeats(int totalSeats) {
        this.totalSeats = totalSeats;
    }

    public double getBaseFare() {
        return baseFare;
    }

    public void setBaseFare(double baseFare) {
        this.baseFare = baseFare;
    }

    @Override
    public String toString() {
        return String.format("#%-3d %-8s %-12s -> %-12s %s  seats:%-4d base fare: Rs %.2f",
                id, flightNo, origin, destination, departure.format(DISPLAY), totalSeats, baseFare);
    }
}
