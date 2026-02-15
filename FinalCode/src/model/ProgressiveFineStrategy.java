package model;

public class ProgressiveFineStrategy implements FineStrategy {
    @Override
    public double calculateFine(long overstayMinutes) {
        if (overstayMinutes <= 0) return 0.0;

        // Convert to hours, rounding up
        long overstayHours = (long) Math.ceil(overstayMinutes / 60.0);

        // Required scheme:
        // First 24 hours: RM 50
        // Hours 24-48: Additional RM 100 (total RM 150)
        // Hours 48-72: Additional RM 150 (total RM 300)
        // Above 72 hours: Additional RM 200 (total RM 500)

        if (overstayHours <= 24) {
            return 50.0;
        } else if (overstayHours <= 48) {
            return 150.0; // 50 + 100
        } else if (overstayHours <= 72) {
            return 300.0; // 150 + 150
        } else {
            return 500.0; // 300 + 200
        }
    }

    @Override
    public String getSchemeName() {
        return "Progressive (RM50 first 24h, +RM100 next 24h, +RM150 next 24h, +RM200 beyond)";
    }
}