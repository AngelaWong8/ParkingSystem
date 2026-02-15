package model;

public class HourlyFineStrategy implements FineStrategy {
    @Override
    public double calculateFine(long overstayMinutes) {
        if (overstayMinutes <= 0) return 0.0;

        // Convert to hours, rounding up
        double hours = Math.ceil(overstayMinutes / 60.0);

        // Only charge for hours beyond the first 24 hours
        // If hours <= 24, then overstayHours = 0 (no fine)
        // If hours > 24, then overstayHours = hours - 24
        double overstayHours = Math.max(0, hours - 24);

        // RM 20 per hour for overstay hours only
        return overstayHours * 20.0;
    }

    @Override
    public String getSchemeName() {
        return "Hourly (RM 20/hr after 24hrs)";
    }
}
