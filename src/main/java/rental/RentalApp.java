package rental;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

public class RentalApp {

    private static final GuiService svc = new GuiService();

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) { }
        SwingUtilities.invokeLater(RentalApp::showLogin);
    }

    // ================= LOGIN WINDOW =================
    private static void showLogin() { 
        JFrame f = new JFrame("Farm Equipment Rental");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JTextField name = new JTextField(20);
        JTextField phone = new JTextField(20);
        JTextField village = new JTextField(20);
        JButton go = new JButton("Continue");

        JPanel form = new JPanel(new GridLayout(0, 2, 10, 10));
        form.add(new JLabel("Your name"));    form.add(name);
        form.add(new JLabel("Phone number")); form.add(phone);
        form.add(new JLabel("Village"));      form.add(village);

        JLabel title = new JLabel("Farm Equipment Rental", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));

        JPanel root = new JPanel(new BorderLayout(10, 15));
        root.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
        root.add(title, BorderLayout.NORTH);
        root.add(form, BorderLayout.CENTER);
        root.add(go, BorderLayout.SOUTH);

        go.addActionListener(e -> {
            if (name.getText().isBlank() || phone.getText().isBlank()) {
                JOptionPane.showMessageDialog(f, "Name and phone number are required.");
                return;
            }
            try {
                int id = svc.registerFarmer(name.getText().trim(),
                        phone.getText().trim(), village.getText().trim());
                f.dispose();
                showMain(id, name.getText().trim());
            } catch (SQLException ex) {
                error(f, ex);
            }
        });

        f.getRootPane().setDefaultButton(go);
        f.setContentPane(root);
        f.pack();
        f.setLocationRelativeTo(null);
        f.setVisible(true);
    }

    // ================= MAIN WINDOW =================
    private static void showMain(int farmerId, String farmerName) {
        JFrame frame = new JFrame("Farm Equipment Rental - Welcome, " + farmerName);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        DefaultTableModel eqModel = model("ID", "Name", "Category", "Price/Day");
        DefaultTableModel rentModel = model("ID", "Equipment", "From", "To", "Cost", "Status");
        JComboBox<String> combo = new JComboBox<>();

        // reloads equipment table, rent dropdown and my-rentals table
        Runnable refresh = () -> {
            try {
                List<Object[]> eq = svc.equipmentRows();
                fill(eqModel, eq);
                combo.removeAllItems();
                for (Object[] r : eq) {
                    combo.addItem(r[0] + " - " + r[1] + " (" + r[3] + ")");
                }
                fill(rentModel, svc.rentalRows(farmerId));
            } catch (SQLException ex) {
                error(frame, ex);
            }
        };

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Equipment", equipmentTab(eqModel, refresh));
        tabs.addTab("Rent", rentTab(frame, farmerId, combo, refresh));
        tabs.addTab("My Rentals", rentalsTab(frame, farmerId, rentModel, refresh));
        tabs.addTab("Add Equipment", addTab(frame, refresh));
        tabs.addChangeListener(e -> refresh.run());

        frame.add(tabs);
        frame.setSize(820, 500);
        frame.setLocationRelativeTo(null);
        refresh.run();
        frame.setVisible(true);
    }

    // ---------- Tab 1: View equipment ----------
    private static JPanel equipmentTab(DefaultTableModel eqModel, Runnable refresh) {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        p.add(new JScrollPane(new JTable(eqModel)), BorderLayout.CENTER);
        JButton ref = new JButton("Refresh");
        ref.addActionListener(e -> refresh.run());
        p.add(ref, BorderLayout.SOUTH);
        return p;
    }

    // ---------- Tab 2: Rent equipment ----------
    private static JPanel rentTab(JFrame frame, int farmerId,
                                  JComboBox<String> combo, Runnable refresh) {
        JTextField start = new JTextField(LocalDate.now().toString());
        JTextField end = new JTextField(LocalDate.now().plusDays(1).toString());
        JButton rentBtn = new JButton("Rent Now");

        JPanel form = new JPanel(new GridLayout(0, 2, 10, 12));
        form.add(new JLabel("Equipment"));              form.add(combo);
        form.add(new JLabel("Start date (YYYY-MM-DD)")); form.add(start);
        form.add(new JLabel("End date (YYYY-MM-DD)"));   form.add(end);
        form.add(new JLabel(""));                        form.add(rentBtn);

        rentBtn.addActionListener(e -> {
            if (combo.getSelectedItem() == null) {
                JOptionPane.showMessageDialog(frame, "No equipment available.");
                return;
            }
            try {
                int eid = Integer.parseInt(combo.getSelectedItem().toString().split(" - ")[0]);
                LocalDate from = LocalDate.parse(start.getText().trim());
                LocalDate to = LocalDate.parse(end.getText().trim());
                JOptionPane.showMessageDialog(frame, svc.rent(farmerId, eid, from, to));
                refresh.run();
            } catch (DateTimeParseException ex) {
                JOptionPane.showMessageDialog(frame, "Enter dates in the format YYYY-MM-DD.");
            } catch (SQLException ex) {
                error(frame, ex);
            }
        });
        return wrap(form);
    }

    // ---------- Tab 3: My rentals (return / cancel) ----------
    private static JPanel rentalsTab(JFrame frame, int farmerId,
                                     DefaultTableModel rentModel, Runnable refresh) {
        JTable table = new JTable(rentModel);
        JButton ret = new JButton("Mark Returned");
        JButton cancel = new JButton("Cancel Booking");
        JButton ref = new JButton("Refresh");

        ret.addActionListener(e ->
            closeSelected(frame, farmerId, table, rentModel, "RETURNED", refresh));
        cancel.addActionListener(e ->
            closeSelected(frame, farmerId, table, rentModel, "CANCELLED", refresh));
        ref.addActionListener(e -> refresh.run());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        buttons.add(ret);
        buttons.add(cancel);
        buttons.add(ref);

        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        p.add(new JScrollPane(table), BorderLayout.CENTER);
        p.add(buttons, BorderLayout.SOUTH);
        return p;
    }

    private static void closeSelected(JFrame frame, int farmerId, JTable table,
                                      DefaultTableModel model, String status, Runnable refresh) {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(frame, "Select a rental from the table first.");
            return;
        }
        int rentalId = (int) model.getValueAt(table.convertRowIndexToModel(row), 0);
        try {
            JOptionPane.showMessageDialog(frame, svc.closeRental(farmerId, rentalId, status));
            refresh.run();
        } catch (SQLException ex) {
            error(frame, ex);
        }
    }

    // ---------- Tab 4: Add equipment (owner) ----------
    private static JPanel addTab(JFrame frame, Runnable refresh) {
        JTextField name = new JTextField();
        JTextField category = new JTextField();
        JTextField price = new JTextField();
        JButton add = new JButton("Add Equipment");

        JPanel form = new JPanel(new GridLayout(0, 2, 10, 12));
        form.add(new JLabel("Equipment name")); form.add(name);
        form.add(new JLabel("Category"));       form.add(category);
        form.add(new JLabel("Price per day"));  form.add(price);
        form.add(new JLabel(""));               form.add(add);

        add.addActionListener(e -> {
            if (name.getText().isBlank()) {
                JOptionPane.showMessageDialog(frame, "Equipment name is required.");
                return;
            }
            try {
                double p = Double.parseDouble(price.getText().trim());
                JOptionPane.showMessageDialog(frame,
                    svc.addEquipment(name.getText().trim(), category.getText().trim(), p));
                name.setText(""); category.setText(""); price.setText("");
                refresh.run();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(frame, "Enter a valid number for the price.");
            } catch (SQLException ex) {
                error(frame, ex);
            }
        });
        return wrap(form);
    }

    // ================= HELPERS =================
    private static DefaultTableModel model(String... cols) {
        return new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    private static void fill(DefaultTableModel m, List<Object[]> rows) {
        m.setRowCount(0);
        for (Object[] r : rows) m.addRow(r);
    }

    private static JPanel wrap(JPanel form) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createEmptyBorder(25, 40, 25, 40));
        p.add(form, BorderLayout.NORTH);
        return p;
    }

    private static void error(Component parent, Exception ex) {
        JOptionPane.showMessageDialog(parent, "Database error: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
    }
}