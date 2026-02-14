package model;

public interface FineStrategy {
    double calculateFine(long overstayMinutes);
    String getSchemeName();
}