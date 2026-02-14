package model;

public class HandicappedVehicle extends Vehicle {
    private boolean hasHandicappedCard;

    public HandicappedVehicle(String licensePlate, boolean hasHandicappedCard) {
        super(licensePlate, "HANDICAPPED");
        this.hasHandicappedCard = hasHandicappedCard;
    }

    @Override
    public boolean canParkIn(String spotType) {
        return spotType.equalsIgnoreCase("Handicapped") ||
                spotType.equalsIgnoreCase("Regular");
    }

    public boolean hasHandicappedCard() {
        return hasHandicappedCard;
    }
}
