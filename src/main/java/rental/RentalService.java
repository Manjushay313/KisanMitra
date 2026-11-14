package rental;

import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class RentalService {

    // Register a farmer, or return the existing id if the phone is already registered
    public int registerFarmer(String name, String phone, String village) throws SQLException {
        try (Connection c = DB.get()) {
            PreparedStatement find = c.prepareStatement("SELECT id FROM farmers WHERE phone=?");
            find.setString(1, phone);
            ResultSet rs = find.executeQuery();
            if (rs.next()) return rs.getInt(1);

            PreparedStatement ins = c.prepareStatement(
                "INSERT INTO farmers(name, phone, village) VALUES(?,?,?)",
                Statement.RETURN_GENERATED_KEYS);
            ins.setString(1, name);
            ins.setString(2, phone);
            ins.setString(3, village);
            ins.executeUpdate();
            ResultSet keys = ins.getGeneratedKeys();
            keys.next();
            return keys.getInt(1);
        }
    }

    public void listEquipment() throws SQLException {
        try (Connection c = DB.get();
             ResultSet rs = c.createStatement().executeQuery(
                 "SELECT id, name, category, price_per_day FROM equipment")) {
            System.out.printf("%-4s %-25s %-12s %s%n", "ID", "Name", "Category", "Price/Day");
            while (rs.next()) {
                System.out.printf("%-4d %-25s %-12s Rs.%.2f%n",
                    rs.getInt(1), rs.getString(2), rs.getString(3), rs.getDouble(4));
            }
        }
    }

    // Available = no ACTIVE rental overlapping the requested dates
    public boolean isAvailable(Connection c, int equipId, LocalDate from, LocalDate to)
            throws SQLException {
        PreparedStatement ps = c.prepareStatement(
            "SELECT COUNT(*) FROM rentals WHERE equipment_id=? AND status='ACTIVE' " +
            "AND start_date <= ? AND end_date >= ?");
        ps.setInt(1, equipId);
        ps.setDate(2, Date.valueOf(to));
        ps.setDate(3, Date.valueOf(from));
        ResultSet rs = ps.executeQuery();
        rs.next();
        return rs.getInt(1) == 0;
    }

    public void rent(int farmerId, int equipId, LocalDate from, LocalDate to) throws SQLException {
        if (from.isBefore(LocalDate.now())) {
            System.out.println("Start date cannot be in the past.");
            return;
        }
        if (to.isBefore(from)) {
            System.out.println("End date cannot be before start date.");
            return;
        }
        try (Connection c = DB.get()) {
            PreparedStatement p = c.prepareStatement(
                "SELECT price_per_day FROM equipment WHERE id=?");
            p.setInt(1, equipId);
            ResultSet rs = p.executeQuery();
            if (!rs.next()) {
                System.out.println("Equipment not found.");
                return;
            }
            double price = rs.getDouble(1);

            if (!isAvailable(c, equipId, from, to)) {
                System.out.println("Sorry, this equipment is already booked for those dates.");
                return;
            }
            long days = ChronoUnit.DAYS.between(from, to) + 1;
            double total = days * price;

            PreparedStatement ins = c.prepareStatement(
                "INSERT INTO rentals(farmer_id, equipment_id, start_date, end_date, total_cost) " +
                "VALUES(?,?,?,?,?)");
            ins.setInt(1, farmerId);
            ins.setInt(2, equipId);
            ins.setDate(3, Date.valueOf(from));
            ins.setDate(4, Date.valueOf(to));
            ins.setDouble(5, total);
            ins.executeUpdate();
            System.out.printf("Booked! %d day(s), total = Rs.%.2f%n", days, total);
        }
    }

    public void myRentals(int farmerId) throws SQLException {
        try (Connection c = DB.get()) {
            PreparedStatement ps = c.prepareStatement(
                "SELECT r.id, e.name, r.start_date, r.end_date, r.total_cost, r.status " +
                "FROM rentals r JOIN equipment e ON e.id = r.equipment_id " +
                "WHERE r.farmer_id=? ORDER BY r.start_date DESC");
            ps.setInt(1, farmerId);
            ResultSet rs = ps.executeQuery();
            System.out.printf("%-5s %-25s %-12s %-12s %-10s %s%n",
                "ID", "Equipment", "From", "To", "Cost", "Status");
            boolean any = false;
            while (rs.next()) {
                any = true;
                System.out.printf("%-5d %-25s %-12s %-12s %-10.2f %s%n",
                    rs.getInt(1), rs.getString(2), rs.getDate(3), rs.getDate(4),
                    rs.getDouble(5), rs.getString(6));
            }
            if (!any) System.out.println("No rentals yet.");
        }
    }

    // newStatus = "RETURNED" or "CANCELLED"
    public void closeRental(int farmerId, int rentalId, String newStatus) throws SQLException {
        try (Connection c = DB.get()) {
            PreparedStatement ps = c.prepareStatement(
                "UPDATE rentals SET status=? WHERE id=? AND farmer_id=? AND status='ACTIVE'");
            ps.setString(1, newStatus);
            ps.setInt(2, rentalId);
            ps.setInt(3, farmerId);
            int n = ps.executeUpdate();
            System.out.println(n > 0 ? "Rental marked " + newStatus + "." :
                                       "No active rental found with that ID.");
        }
    }

    public void addEquipment(String name, String category, double price) throws SQLException {
        try (Connection c = DB.get()) {
            PreparedStatement ps = c.prepareStatement(
                "INSERT INTO equipment(name, category, price_per_day) VALUES(?,?,?)");
            ps.setString(1, name);
            ps.setString(2, category);
            ps.setDouble(3, price);
            ps.executeUpdate();
            System.out.println("Equipment added.");
        }
    }
}