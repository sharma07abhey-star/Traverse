package ticketing.service;

import java.util.List;

import ticketing.exception.ValidationException;
import ticketing.model.TravelClass;

/** Baggage pricing rules: fee depends on weight AND travel class. */
public final class BaggageFeeCalculator {
    public static final double MAX_BAG_WEIGHT_KG = 50.0;

    private BaggageFeeCalculator() {
    }

    /** Fee for a single bag: only the kilos above the class allowance are charged. */
    public static double feeFor(TravelClass cls, double weightKg) {
        double extraKg = Math.max(0, weightKg - cls.getFreeAllowanceKg());
        return extraKg * cls.getExtraRatePerKg();
    }

    public static double totalFee(TravelClass cls, List<Double> weights) {
        return weights.stream().mapToDouble(w -> feeFor(cls, w)).sum();
    }

    public static void validate(TravelClass cls, List<Double> weights) {
        if (weights.size() > cls.getMaxBags()) {
            throw new ValidationException(cls + " class allows at most " + cls.getMaxBags() + " bags");
        }
        for (double w : weights) {
            if (w <= 0 || w > MAX_BAG_WEIGHT_KG) {
                throw new ValidationException("Each bag must weigh between 0 and " + MAX_BAG_WEIGHT_KG
                        + " kg (got " + w + ")");
            }
        }
    }
}
