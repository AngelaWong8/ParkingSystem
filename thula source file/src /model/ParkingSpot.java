package model;

public class ParkingSpot {
    private String spotId;
    private String spotType;
    private boolean isAvailable;
    private double hourlyRate;
    
    public ParkingSpot(String spotId, String spotType, double hourlyRate) {
        this.spotId = spotId;
        this.spotType = spotType;
        this.isAvailable = true;
        this.hourlyRate = hourlyRate;
    }
    
    // Getters and setters
    public String getSpotId() { return spotId; }
    public String getSpotType() { return spotType; }
    public boolean isAvailable() { return isAvailable; }
    public void setAvailable(boolean available) { isAvailable = available; }
    public double getHourlyRate() { return hourlyRate; }
    
    @Override
    public String toString() {
        return spotId + " - " + spotType + " (RM" + hourlyRate + "/hr)";
    }
}
