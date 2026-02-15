package model;

public class HourlyFineStrategy implements FineStrategy {
    @Override
    public double calculateFine(long overstayMinutes) {
        if (overstayMinutes <= 0) return 0.0;
        // RM 20 per hour, prorated by minute
        double hours = overstayMinutes / 60.0;
        return Math.ceil(hours) * 20.0;
    }

    @Override
    public String getSchemeName() {
        return "Hourly (RM 20/hr)";
    }
}