package facade;

import dao.*;
import model.*;
import database.DatabaseConnection;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

public class ParkingSystemFacade {

    private ParkingSpotDAO parkingSpotDAO;
    private VehicleDAO vehicleDAO;
    private TicketDAO ticketDAO;
    private ReportDAO reportDAO;

    // Person C DAOs - commented until they deliver
    // private PaymentDAO paymentDAO;
    // private FineDAO fineDAO;

    public ParkingSystemFacade() {
        this.parkingSpotDAO = new ParkingSpotDAO();
        this.vehicleDAO = new VehicleDAO();
        this.ticketDAO = new TicketDAO();
        this.reportDAO = new ReportDAO();

        // this.paymentDAO = new PaymentDAO();
        // this.fineDAO = new FineDAO();
    }

    // ============ PERSON A's METHODS ============

    public List<ParkingSpot> getAllSpots() {
        return parkingSpotDAO.getAllSpots();
    }

    public ParkingSpot getSpotBySpotId(String spotId) {
        return parkingSpotDAO.getSpotBySpotId(spotId);
    }

    public void updateSpotStatus(int spotDbId, boolean occupied) {
        parkingSpotDAO.updateSpotStatus(spotDbId, occupied);
    }

    // ============ PERSON B's METHODS ============

    public void parkVehicle(String licensePlate, String vehicleType, boolean hasCard, String spotId)
            throws SQLException {

        Vehicle vehicle;
        switch (vehicleType) {
            case "CAR": vehicle = new Car(licensePlate); break;
            case "MOTORCYCLE": vehicle = new Motorcycle(licensePlate); break;
            case "SUV": vehicle = new SUV(licensePlate); break;
            case "HANDICAPPED":
                if (!hasCard) {
                    throw new IllegalArgumentException("Handicapped vehicles must have a card");
                }
                vehicle = new HandicappedVehicle(licensePlate, true);
                break;
            default: throw new IllegalArgumentException("Invalid vehicle type");
        }

        vehicle.setEntryTime(LocalDateTime.now());
        vehicleDAO.saveVehicle(vehicle);

        Ticket ticket = new Ticket(licensePlate, spotId);
        ticketDAO.saveTicket(ticket);

        ParkingSpot spot = parkingSpotDAO.getSpotBySpotId(spotId);
        if (spot != null) {
            parkingSpotDAO.updateSpotStatus(spot.getDbId(), true);
        }
    }

    public Vehicle findVehicle(String licensePlate) throws SQLException {
        return vehicleDAO.findVehicle(licensePlate);
    }

    public Ticket findActiveTicket(String licensePlate) throws SQLException {
        return ticketDAO.findActiveTicket(licensePlate);
    }

    public List<ParkingSpot> getAvailableSpotsForVehicle(Vehicle vehicle) {
        List<ParkingSpot> allSpots = parkingSpotDAO.getAllSpots();
        return allSpots.stream()
                .filter(spot -> spot.isAvailable() && vehicle.canParkIn(spot.getType()))
                .collect(Collectors.toList());
    }

    // ============ PERSON C's METHODS (STUBBED - WAITING) ============

    /*
    public Payment processExit(String licensePlate, String paymentMethod) throws SQLException {
        // Will be implemented when Person C delivers
        return null;
    }

    public Fine calculateFine(Ticket ticket, long overstayMinutes) {
        return null;
    }
    */

    // ============ PERSON D's METHODS (REPORTS & ADMIN) ============

    public double getTotalRevenueToday() throws SQLException {
        return reportDAO.getTotalRevenueToday();
    }

    public double getTotalFinesToday() throws SQLException {
        return reportDAO.getTotalFinesToday();
    }

    public int getCurrentOccupancy() {
        List<ParkingSpot> spots = parkingSpotDAO.getAllSpots();
        return (int) spots.stream().filter(ParkingSpot::isOccupied).count();
    }

    public int getTotalSpots() {
        return parkingSpotDAO.getAllSpots().size();
    }

    public double getOccupancyRate() {
        int total = getTotalSpots();
        if (total == 0) return 0;
        return (getCurrentOccupancy() * 100.0) / total;
    }

    public List<Object[]> getHourlyOccupancy() throws SQLException {
        return reportDAO.getHourlyOccupancy();
    }
}
