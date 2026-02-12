package model;

public class Motorcycle extends Vehicle {
    public Motorcycle(String licensePlate) {
        super(licensePlate, "MOTORCYCLE");
    }
    
    @Override
    public boolean canParkIn(String spotType) {
        // Motorcycle can only park in Compact spots
        return spotType.equalsIgnoreCase("COMPACT");
    }
}
