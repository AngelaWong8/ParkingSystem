package model;

public class ParkingSpot {
    private int dbId;
    private String spotId;
    private String type;
    private int floorNumber;
    private int rowNumber;
    private boolean isOccupied;
    private double hourlyRate;

    // Constructor for Person A (full details)
    public ParkingSpot(int dbId, String spotId, String type, int floorNumber, int rowNumber, double hourlyRate) {
        this.dbId = dbId;
        this.spotId = spotId;
        this.type = type;
        this.floorNumber = floorNumber;
        this.rowNumber = rowNumber;
        this.hourlyRate = hourlyRate;
        this.isOccupied = false;
    }

    // Constructor for Person B (simple version)
    public ParkingSpot(String spotId, String type, double hourlyRate) {
        this.spotId = spotId;
        this.type = type;
        this.hourlyRate = hourlyRate;
        this.isOccupied = false;
    }

    // Getters
    public int getDbId() { return dbId; }
    public String getSpotId() { return spotId; }
    public String getType() { return type; }
    public String getSpotType() { return type; } // Alias for Person B
    public int getFloorNumber() { return floorNumber; }
    public int getRowNumber() { return rowNumber; }
    public double getHourlyRate() { return hourlyRate; }
    public boolean isOccupied() { return isOccupied; }
    public boolean isAvailable() { return !isOccupied; }

    // Setters
    public void setOccupied(boolean occupied) { isOccupied = occupied; }
    public void setAvailable(boolean available) { isOccupied = !available; }

    @Override
    public String toString() {
        return spotId + " - " + type + " (RM" + hourlyRate + "/hr)";
    }
}
