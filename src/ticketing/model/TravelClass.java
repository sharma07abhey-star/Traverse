package ticketing.model;

import ticketing.exception.ValidationException;

/**
 * Travel class with its fare multiplier and baggage rules.
 * Extra baggage fee = (weight - freeAllowanceKg) * extraRatePerKg for each overweight bag.
 */
public enum TravelClass {
    //          fare x   free kg/bag  max bags  Rs per extra kg
    ECONOMY(1.0, 20, 2, 400),
    BUSINESS(1.8, 30, 3, 300),
    FIRST(2.5, 40, 4, 200);

    private final double fareMultiplier;
    private final double freeAllowanceKg;
    private final int maxBags;
    private final double extraRatePerKg;

    TravelClass(double fareMultiplier, double freeAllowanceKg, int maxBags, double extraRatePerKg) {
        this.fareMultiplier = fareMultiplier;
        this.freeAllowanceKg = freeAllowanceKg;
        this.maxBags = maxBags;
        this.extraRatePerKg = extraRatePerKg;
    }

    public double getFareMultiplier() {
        return fareMultiplier;
    }

    public double getFreeAllowanceKg() {
        return freeAllowanceKg;
    }

    public int getMaxBags() {
        return maxBags;
    }

    public double getExtraRatePerKg() {
        return extraRatePerKg;
    }

    public static TravelClass parse(String text) {
        try {
            return valueOf(text.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ValidationException("Unknown travel class: " + text + " (use ECONOMY, BUSINESS or FIRST)");
        }
    }
}
