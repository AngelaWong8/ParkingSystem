package model;

public class Motorcycle extends Vehicle {
    public Motorcycle(String licensePlate) {
        super(licensePlate, "MOTORCYCLE");
    }

    @Override
    public boolean canParkIn(String spotType) {
        return spotType.equalsIgnoreCase("Compact");
    }
}
