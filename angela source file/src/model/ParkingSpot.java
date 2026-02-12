package model;

public class ParkingSpot {
    // Internal Database ID (1, 2, 3...)
    private int dbId;

    // The "Client Requirement" ID (e.g., "F1-R1-S05")
    private String spotId;

    private String type;
    private int floorNumber;
    private int rowNumber;
    private boolean isOccupied;
    private double hourlyRate;

    public ParkingSpot(int dbId, String spotId, String type, int floorNumber, int rowNumber, double hourlyRate) {
        this.dbId = dbId;
        this.spotId = spotId;
        this.type = type;
        this.floorNumber = floorNumber;
        this.rowNumber = rowNumber;
        this.hourlyRate = hourlyRate;
        this.isOccupied = false;
    }

    // Getters and Setters
    public int getDbId() { return dbId; }
    public String getSpotId() { return spotId; }
    public String getType() { return type; }
    public int getFloorNumber() { return floorNumber; }
    public int getRowNumber() { return rowNumber; }
    public double getHourlyRate() { return hourlyRate; }

    public boolean isOccupied() { return isOccupied; }
    public void setOccupied(boolean occupied) { isOccupied = occupied; }
}
