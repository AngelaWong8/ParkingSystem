package model;

public class HandicappedVehicle extends Vehicle {
    private boolean hasHandicappedCard;
    
    public HandicappedVehicle(String licensePlate, boolean hasHandicappedCard) {
        super(licensePlate, "HANDICAPPED");
        this.hasHandicappedCard = hasHandicappedCard;
    }
    
    @Override
    public boolean canParkIn(String spotType) {
        // REAL-WORLD LOGIC:
        // 1. PREFER Handicapped spots (if available)
        // 2. CAN park in Regular spots (if no handicapped spots)
        // 3. CANNOT park in Compact or Motorcycle spots (too small!)
        // 4. CANNOT park in Reserved spots (unless VIP with reservation)
        
        return spotType.equalsIgnoreCase("HANDICAPPED") || 
               spotType.equalsIgnoreCase("REGULAR");
        // Compact? No! Motorcycle? No! Reserved? No (unless reservation system)
    }
    
    public boolean hasHandicappedCard() {
        return hasHandicappedCard;
    }
}