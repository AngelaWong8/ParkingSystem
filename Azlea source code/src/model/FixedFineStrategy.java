package model;
public class FixedFineStrategy implements FineStrategy {
    @Override
    public double calculateFine(long overstayMinutes) {
        return overstayMinutes > 0 ? 50.0 : 0.0;
    }
    @Override
    public String getSchemeName() { return "Fixed (RM 50)"; }
}