package model;

public class Car extends Vehicle {
    public Car(String licensePlate) {
        super(licensePlate, "CAR");
    }

    @Override
    public boolean canParkIn(String spotType) {
        return spotType.equalsIgnoreCase("Compact") ||
                spotType.equalsIgnoreCase("Regular");
    }
}
