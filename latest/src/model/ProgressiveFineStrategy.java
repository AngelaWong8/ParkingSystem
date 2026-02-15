package model;

public class ProgressiveFineStrategy implements FineStrategy {
    @Override
    public double calculateFine(long overstayMinutes) {
        if (overstayMinutes <= 0) return 0;
        if (overstayMinutes <= 60) return 20.0;
        return 20.0 + (Math.ceil((overstayMinutes - 60) / 60.0) * 10.0);
    }
    @Override
    public String getSchemeName() { return "Progressive Scheme"; }
}
