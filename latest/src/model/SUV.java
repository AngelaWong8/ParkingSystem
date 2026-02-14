package model;

public class SUV extends Vehicle {
    public SUV(String licensePlate) {
        super(licensePlate, "SUV");
    }

    @Override
    public boolean canParkIn(String spotType) {
        return spotType.equalsIgnoreCase("Regular");
    }
}
