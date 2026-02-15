package facade;

import dao.*;
import model.*;
import database.DatabaseConnection;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ParkingSystemFacade {

    private ParkingSpotDAO parkingSpotDAO;
    private VehicleDAO vehicleDAO;
    private TicketDAO ticketDAO;
    private ReportDAO reportDAO;
    private PaymentDAO paymentDAO;
    private ReservationDAO reservationDAO;

    // Refresh listeners
    private List<Runnable> refreshListeners = new ArrayList<>();

    public ParkingSystemFacade() {
        this.parkingSpotDAO = new ParkingSpotDAO();
        this.vehicleDAO = new VehicleDAO();
        this.ticketDAO = new TicketDAO();
        this.reportDAO = new ReportDAO();
        this.paymentDAO = new PaymentDAO();
        this.reservationDAO = new ReservationDAO();
    }

    public void addRefreshListener(Runnable listener) {
        refreshListeners.add(listener);
    }

    public void notifyRefresh() {
        for (Runnable listener : refreshListeners) {
            listener.run();
        }
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
        notifyRefresh();
    }

    // ============ PERSON B's METHODS ============

    public void parkVehicle(String licensePlate, String vehicleType, boolean hasCard, String spotId)
            throws SQLException {

        System.out.println("\n=== PARK VEHICLE DEBUG ===");
        System.out.println("License Plate: " + licensePlate);
        System.out.println("Vehicle Type: " + vehicleType);
        System.out.println("Has Card: " + hasCard);
        System.out.println("Spot ID received: '" + spotId + "'");
        System.out.println("Spot ID length: " + spotId.length());

        Vehicle vehicle;
        switch (vehicleType) {
            case "CAR":
                vehicle = new Car(licensePlate);
                System.out.println("Created CAR vehicle");
                break;
            case "MOTORCYCLE":
                vehicle = new Motorcycle(licensePlate);
                System.out.println("Created MOTORCYCLE vehicle");
                break;
            case "SUV":
                vehicle = new SUV(licensePlate);
                System.out.println("Created SUV vehicle");
                break;
            case "HANDICAPPED":
                vehicle = new HandicappedVehicle(licensePlate, hasCard);
                System.out.println("Created HANDICAPPED vehicle (Card: " + hasCard + ")");
                break;
            default:
                throw new IllegalArgumentException("Invalid vehicle type");
        }

        vehicle.setEntryTime(LocalDateTime.now());
        vehicleDAO.saveVehicle(vehicle);
        System.out.println("✓ Vehicle saved to DB");

        Ticket ticket = new Ticket(licensePlate, spotId);
        ticketDAO.saveTicket(ticket);
        System.out.println("✓ Ticket saved to DB");

        System.out.println("\n--- Looking up spot in database ---");
        ParkingSpot spot = parkingSpotDAO.getSpotBySpotId(spotId);

        if (spot != null) {
            System.out.println("✓ Spot found in database!");
            System.out.println("  Spot DB ID: " + spot.getDbId());
            System.out.println("  Spot ID string: " + spot.getSpotId());
            System.out.println("  Spot type: " + spot.getType());
            System.out.println("  Current occupied status: " + spot.isOccupied());

            System.out.println("\n--- Updating spot status to OCCUPIED ---");
            parkingSpotDAO.updateSpotStatus(spot.getDbId(), true);

            // Verify the update
            ParkingSpot verifySpot = parkingSpotDAO.getSpotBySpotId(spotId);
            System.out.println("\n--- Verification after update ---");
            System.out.println("Spot occupied now: " + verifySpot.isOccupied());

            if (verifySpot.isOccupied()) {
                System.out.println("✓ SUCCESS: Spot successfully marked as occupied");
            } else {
                System.out.println("❌ FAILED: Spot still shows as available");
            }
        } else {
            System.out.println("❌ SPOT NOT FOUND IN DATABASE!");
            System.out.println("The spot ID '" + spotId + "' does not match any spot in the database");
        }

        System.out.println("=== END PARK VEHICLE DEBUG ===\n");

        notifyRefresh();
    }

    public Vehicle findVehicle(String licensePlate) {
        try {
            return vehicleDAO.findVehicle(licensePlate);
        } catch (SQLException e) {
            return null;
        }
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

    // ============ RESERVATION METHODS ============

    public boolean hasActiveReservation(String licensePlate, String spotId) {
        return reservationDAO.hasActiveReservation(licensePlate, spotId);
    }

    public void createReservation(String licensePlate, String spotId, LocalDateTime start, LocalDateTime end) throws SQLException {
        reservationDAO.createReservation(licensePlate, spotId, start, end);
    }

    public List<String> getAvailableReservationSpots(LocalDateTime start, LocalDateTime end) {
        List<ParkingSpot> allSpots = parkingSpotDAO.getAllSpots();
        List<String> reservedSpotIds = reservationDAO.getReservedSpotsInTimeRange(start, end);

        return allSpots.stream()
                .filter(spot -> spot.getType().equalsIgnoreCase("Reserved"))
                .filter(spot -> !reservedSpotIds.contains(spot.getSpotId()))
                .map(spot -> spot.getSpotId() + " - " + spot.getType() + " (RM " + spot.getHourlyRate() + "/hr)")
                .collect(Collectors.toList());
    }

    // ============ PERSON C's METHODS ============

    public void processExit(String licensePlate, String paymentMethod, double amount, double fines) throws SQLException {
        Ticket ticket = findActiveTicket(licensePlate);
        if (ticket != null) {
            // Save payment
            paymentDAO.savePayment(ticket.getTicketId(), licensePlate, amount, paymentMethod);

            // Mark spot as available
            ParkingSpot spot = parkingSpotDAO.getSpotBySpotId(ticket.getSpotId());
            if (spot != null) {
                parkingSpotDAO.updateSpotStatus(spot.getDbId(), false);
            }

            // Remove ticket
            ticketDAO.removeTicket(licensePlate);

            notifyRefresh();
        }
    }

    // ============ PERSON D's METHODS (REPORTS & ADMIN) ============

    public double getTotalRevenueToday() throws SQLException {
        return reportDAO.getTotalRevenueToday();
    }

    public double getTotalFinesToday() throws SQLException {
        return reportDAO.getTotalFinesToday();
    }

    public int getCurrentOccupancy() {
        List<ParkingSpot> spots = parkingSpotDAO.getAllSpots();
        int occupied = (int) spots.stream().filter(ParkingSpot::isOccupied).count();
        System.out.println("Current occupancy count: " + occupied + "/" + spots.size());
        return occupied;
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
