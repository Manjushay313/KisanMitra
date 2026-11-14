package rental;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        RentalService svc = new RentalService();

        try (Scanner sc = new Scanner(System.in)) {
            System.out.println("=== Farm Equipment Rental ===");
            System.out.print("Your name: ");
            String name = sc.nextLine().trim();
            System.out.print("Phone number: ");
            String phone = sc.nextLine().trim();
            System.out.print("Village: ");
            String village = sc.nextLine().trim();

            int farmerId = svc.registerFarmer(name, phone, village);
            System.out.println("Welcome, " + name + "!");

            while (true) {
                System.out.println("\n1. View equipment");
                System.out.println("2. Rent equipment");
                System.out.println("3. My rentals");
                System.out.println("4. Return equipment");
                System.out.println("5. Cancel booking");
                System.out.println("6. Add equipment (owner)");
                System.out.println("0. Exit");
                System.out.print("Choose: ");
                String ch = sc.nextLine().trim();

                try {
                    switch (ch) {
                        case "1" -> svc.listEquipment();

                        case "2" -> {
                            svc.listEquipment();
                            System.out.print("Equipment ID: ");
                            int eid = Integer.parseInt(sc.nextLine().trim());
                            System.out.print("Start date (YYYY-MM-DD): ");
                            LocalDate from = LocalDate.parse(sc.nextLine().trim());
                            System.out.print("End date (YYYY-MM-DD): ");
                            LocalDate to = LocalDate.parse(sc.nextLine().trim());
                            svc.rent(farmerId, eid, from, to);
                        }

                        case "3" -> svc.myRentals(farmerId);

                        case "4" -> {
                            System.out.print("Rental ID: ");
                            int rid = Integer.parseInt(sc.nextLine().trim());
                            svc.closeRental(farmerId, rid, "RETURNED");
                        }

                        case "5" -> {
                            System.out.print("Rental ID: ");
                            int rid = Integer.parseInt(sc.nextLine().trim());
                            svc.closeRental(farmerId, rid, "CANCELLED");
                        }

                        case "6" -> {
                            System.out.print("Equipment name: ");
                            String n = sc.nextLine().trim();
                            System.out.print("Category: ");
                            String cat = sc.nextLine().trim();
                            System.out.print("Price per day: ");
                            double p = Double.parseDouble(sc.nextLine().trim());
                            svc.addEquipment(n, cat, p);
                        }

                        case "0" -> {
                            System.out.println("Goodbye!");
                            return;
                        }

                        default -> System.out.println("Invalid choice. Enter a number from the menu.");
                    }
                } catch (NumberFormatException | DateTimeParseException e) {
                    System.out.println("Invalid input, please try again.");
                } catch (java.sql.SQLException e) {
                    System.out.println("Database error: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}