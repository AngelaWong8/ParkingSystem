package logic;

import java.time.Duration;
import java.time.LocalDateTime;

public class PaymentLogic {

    // Requirement: Ceiling rounding (e.g., 1.1 hours = 2 hours)
    public double calculateFee(LocalDateTime entry, double hourlyRate) {
        Duration duration = Duration.between(entry, LocalDateTime.now());
        long hours = (long) Math.ceil(duration.toMinutes() / 60.0);
        if (hours < 1) hours = 1; 
        return hours * hourlyRate;
    }

    // Requirement: Fine Schemes (A: Fixed, B: Progressive, C: Hourly)
    public double calculateFine(LocalDateTime entry, String scheme) {
        Duration duration = Duration.between(entry, LocalDateTime.now());
        long totalHours = (long) Math.ceil(duration.toMinutes() / 60.0);
        
        if (totalHours <= 24) return 0.0; // Fines only apply after 24 hours
        long overstay = totalHours - 24;

        switch (scheme.toUpperCase()) {
            case "A": return 50.0;
            case "B": // Progressive
                if (totalHours <= 48) return 50.0;
                if (totalHours <= 72) return 150.0; // 50 + 100
                return 350.0; // 50 + 100 + 200
            case "C": return overstay * 20.0;
            default: return 0.0;
        }
    }
}