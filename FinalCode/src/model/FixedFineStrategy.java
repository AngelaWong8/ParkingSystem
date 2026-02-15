package model;

public class FixedFineStrategy implements FineStrategy {
    @Override
    public double calculateFine(long overstayMinutes) {
        // Only charge fine if there is actually an overstay (beyond 24 hours)
        // overstayMinutes parameter should be the minutes beyond the first 24 hours
        if (overstayMinutes <= 0) {
            return 0.0;
        }
        return 50.0;
    }

    @Override
    public String getSchemeName() {
        return "Fixed (RM 50)";
    }
}
