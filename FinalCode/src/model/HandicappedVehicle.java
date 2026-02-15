package model;

public class HandicappedVehicle extends Vehicle {
    private boolean hasHandicappedCard;

    public HandicappedVehicle(String licensePlate, boolean hasHandicappedCard) {
        super(licensePlate, "HANDICAPPED");
        this.hasHandicappedCard = hasHandicappedCard;
    }

    @Override
    public boolean canParkIn(String spotType) {
        // Handicapped vehicles can park in ANY spot type
        return true;
    }

    public boolean hasHandicappedCard() {
        return hasHandicappedCard;
    }
}
