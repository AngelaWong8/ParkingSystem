package model;

public class Car extends Vehicle {
    public Car(String licensePlate) {
        super(licensePlate, "CAR");
    }
    
    @Override
    public boolean canParkIn(String spotType) {
        // Car can park in Compact or Regular spots
        return spotType.equalsIgnoreCase("COMPACT") || 
               spotType.equalsIgnoreCase("REGULAR");
    }
}
