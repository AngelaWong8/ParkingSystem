package model;

import java.time.LocalDateTime;

public abstract class Vehicle {
    private String licensePlate;
    private String vehicleType;
    private LocalDateTime entryTime;
    private LocalDateTime exitTime;

    public Vehicle(String licensePlate, String vehicleType) {
        this.licensePlate = licensePlate.toUpperCase(); // Store as uppercase
        this.vehicleType = vehicleType;
        this.entryTime = null;
        this.exitTime = null;
    }

    public String getLicensePlate() { return licensePlate; }
    public void setLicensePlate(String licensePlate) { this.licensePlate = licensePlate.toUpperCase(); }
    public String getVehicleType() { return vehicleType; }
    public LocalDateTime getEntryTime() { return entryTime; }
    public void setEntryTime(LocalDateTime entryTime) { this.entryTime = entryTime; }
    public LocalDateTime getExitTime() { return exitTime; }
    public void setExitTime(LocalDateTime exitTime) { this.exitTime = exitTime; }

    public abstract boolean canParkIn(String spotType);

    @Override
    public String toString() {
        return licensePlate + " (" + vehicleType + ")";
    }
}
