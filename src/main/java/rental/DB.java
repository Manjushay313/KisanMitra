package rental;

import java.sql.*;

public class DB {
    private static final String URL =
        "jdbc:mysql://localhost:3306/farm_rental?useSSL=false&serverTimezone=Asia/Kolkata";
    private static final String USER = "root";
    private static final String PASS = "your_password_here"; // Replace with password

    public static Connection get() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }
}