package model;

public class SUV extends Vehicle {
    public SUV(String licensePlate) {
        super(licensePlate, "SUV");
    }
    
    @Override
    public boolean canParkIn(String spotType) {
        // SUV/Truck can park in Regular spots only
        return spotType.equalsIgnoreCase("REGULAR");
    }
}