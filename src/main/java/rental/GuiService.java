package rental;

import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class GuiService {

    private final RentalService base = new RentalService();

    public int registerFarmer(String name, String phone, String village) throws SQLException {
        return base.registerFarmer(name, phone, village);
    }

    public List<Object[]> equipmentRows() throws SQLException {
        List<Object[]> rows = new ArrayList<>();
        try (Connection c = DB.get();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(
                 "SELECT id, name, category, price_per_day FROM equipment")) {
            while (rs.next()) {
                rows.add(new Object[]{
                    rs.getInt(1), rs.getString(2), rs.getString(3),
                    String.format("Rs.%.2f", rs.getDouble(4))});
            }
        }
        return rows;
    }

    public String rent(int farmerId, int equipId, LocalDate from, LocalDate to) throws SQLException {
        if (from.isBefore(LocalDate.now())) return "Start date cannot be in the past.";
        if (to.isBefore(from)) return "End date cannot be before start date.";

        try (Connection c = DB.get()) {
            PreparedStatement p = c.prepareStatement(
                "SELECT price_per_day FROM equipment WHERE id=?");
            p.setInt(1, equipId);
            ResultSet rs = p.executeQuery();
            if (!rs.next()) return "Equipment not found.";
            double price = rs.getDouble(1);

            if (!base.isAvailable(c, equipId, from, to))
                return "Sorry, this equipment is already booked for those dates.";

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
            return String.format("Booked! %d day(s), total = Rs.%.2f", days, total);
        }
    }

    public List<Object[]> rentalRows(int farmerId) throws SQLException {
        List<Object[]> rows = new ArrayList<>();
        try (Connection c = DB.get()) {
            PreparedStatement ps = c.prepareStatement(
                "SELECT r.id, e.name, r.start_date, r.end_date, r.total_cost, r.status " +
                "FROM rentals r JOIN equipment e ON e.id = r.equipment_id " +
                "WHERE r.farmer_id=? ORDER BY r.start_date DESC");
            ps.setInt(1, farmerId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                rows.add(new Object[]{
                    rs.getInt(1), rs.getString(2),
                    rs.getDate(3).toString(), rs.getDate(4).toString(),
                    String.format("%.2f", rs.getDouble(5)), rs.getString(6)});
            }
        }
        return rows;
    }

    // newStatus = "RETURNED" or "CANCELLED"
    public String closeRental(int farmerId, int rentalId, String newStatus) throws SQLException {
        try (Connection c = DB.get()) {
            PreparedStatement ps = c.prepareStatement(
                "UPDATE rentals SET status=? WHERE id=? AND farmer_id=? AND status='ACTIVE'");
            ps.setString(1, newStatus);
            ps.setInt(2, rentalId);
            ps.setInt(3, farmerId);
            int n = ps.executeUpdate();
            return n > 0 ? "Rental marked " + newStatus + "."
                         : "No active rental found with that ID.";
        }
    }

    public String addEquipment(String name, String category, double price) throws SQLException {
        try (Connection c = DB.get()) {
            PreparedStatement ps = c.prepareStatement(
                "INSERT INTO equipment(name, category, price_per_day) VALUES(?,?,?)");
            ps.setString(1, name);
            ps.setString(2, category);
            ps.setDouble(3, price);
            ps.executeUpdate();
            return "Equipment added.";
        }
    }
}