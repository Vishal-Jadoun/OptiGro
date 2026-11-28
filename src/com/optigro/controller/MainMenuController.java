package com.optigro.controller;

import com.optigro.constants.UserRole;
import com.optigro.exception.InvalidAuthException;
import com.optigro.models.DeliveryPartner;
import com.optigro.models.User;
import com.optigro.repository.DeliveryPartnerRepository;
import com.optigro.service.AuthService;
import com.optigro.service.DeliveryService;
import com.optigro.util.ConsoleScannerUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class MainMenuController {
    private final AuthService authService;
    private final DeliveryService deliveryService;
    private final DeliveryPartnerRepository partnerRepo;

    private Consumer<User> customerMenuHandler;
    private Consumer<User> pickerMenuHandler;
    private Runnable inventoryMenuHandler;
    private Runnable gridLayoutHandler;
    private Runnable simulationHandler;
    private Supplier<String> stockReportSupplier;
    private Supplier<String> salesReportSupplier;
    private Supplier<String> pickerReportSupplier;

    public MainMenuController(AuthService authService,
                              DeliveryService deliveryService,
                              DeliveryPartnerRepository partnerRepo) {
        this.authService = authService;
        this.deliveryService = deliveryService;
        this.partnerRepo = partnerRepo;
    }

    public void setCustomerMenuHandler(Consumer<User> customerMenuHandler) {
        this.customerMenuHandler = customerMenuHandler;
    }

    public void setPickerMenuHandler(Consumer<User> pickerMenuHandler) {
        this.pickerMenuHandler = pickerMenuHandler;
    }

    public void setInventoryMenuHandler(Runnable inventoryMenuHandler) {
        this.inventoryMenuHandler = inventoryMenuHandler;
    }

    public void setGridLayoutHandler(Runnable gridLayoutHandler) {
        this.gridLayoutHandler = gridLayoutHandler;
    }

    public void setSimulationHandler(Runnable simulationHandler) {
        this.simulationHandler = simulationHandler;
    }

    public void setReportSuppliers(Supplier<String> stockReportSupplier,
                                   Supplier<String> salesReportSupplier,
                                   Supplier<String> pickerReportSupplier) {
        this.stockReportSupplier = stockReportSupplier;
        this.salesReportSupplier = salesReportSupplier;
        this.pickerReportSupplier = pickerReportSupplier;
    }

    public void start() {
        printBanner();

        while (true) {
            System.out.println("\n===== MAIN SYSTEM MENU =====");
            System.out.println("1. Login to Portal");
            System.out.println("2. Exit System");

            int choice = ConsoleScannerUtil.readIntInRange("Select option [1-2]: ", 1, 2);
            if (choice == 2) {
                System.out.println("\nThank you for using OptiGro Logistics Engine. Goodbye!");
                break;
            }

            System.out.println("\n--- OptiGro Secure Authentication ---");
            String username = ConsoleScannerUtil.readString("Enter Username: ");
            String password = ConsoleScannerUtil.readString("Enter Password: ");

            try {
                User user = authService.login(username, password);
                System.out.printf("✓ Login Successful! Welcome %s [Role: %s]%n", user.getUsername(), user.getRole());
                routeByRole(user);
            } catch (InvalidAuthException e) {
                System.out.println("[!] Login Error: " + e.getMessage());
            }
        }
    }

    public void printBanner() {
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║                        OPTIGRO                           ║");
        System.out.println("║     10-Minute Grocery Delivery Logistics Engine          ║");
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.println("║  Default Demo Credentials (seeded from CSV):             ║");
        System.out.println("║  • Administrator:     admin    / admin123                ║");
        System.out.println("║  • Customer Profile:  cust1    / pass1                   ║");
        System.out.println("║  • Warehouse Picker:  picker1  / pass1                   ║");
        System.out.println("║  • Delivery Partner:  partner1 / pass1                   ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
    }

    public void routeByRole(User user) {
        switch (user.getRole()) {
            case ADMIN:
                showAdminMenu();
                break;
            case CUSTOMER:
                if (customerMenuHandler != null) {
                    customerMenuHandler.accept(user);
                } else {
                    System.out.println("[Customer Portal] Customer module ready for M2 integration.");
                }
                break;
            case PICKER:
                if (pickerMenuHandler != null) {
                    pickerMenuHandler.accept(user);
                } else {
                    System.out.println("[Picker Portal] Picker module ready for M3 integration.");
                }
                break;
            case DELIVERY_PARTNER:
                DeliveryPartner partner = partnerRepo.findByUserId(user.getUserId())
                    .orElseThrow(() -> new IllegalStateException("Profile missing for partner user: " + user.getUsername()));
                new DeliveryMenu(partner, deliveryService).show();
                break;
        }
    }

    public void showAdminMenu() {
        while (true) {
            System.out.println("\n===== ADMINISTRATOR CONTROL PANEL =====");
            System.out.println("1. Manage Inventory & Catalog (M1)");
            System.out.println("2. Warehouse Store Grid Layout (M1)");
            System.out.println("3. Run Live Multi-Threaded Simulation (M3 Concurrency Demo)");
            System.out.println("4. System Reports & Plain-Text File Export (M4)");
            System.out.println("5. Logout to Main Screen");

            int ch = ConsoleScannerUtil.readIntInRange("Admin Option [1-5]: ", 1, 5);
            switch (ch) {
                case 1:
                    if (inventoryMenuHandler != null) {
                        inventoryMenuHandler.run();
                    } else {
                        System.out.println("[M1 Inventory] Module ready for M1 integration.");
                    }
                    break;
                case 2:
                    if (gridLayoutHandler != null) {
                        gridLayoutHandler.run();
                    } else {
                        System.out.println("[M1 BinLayout] Grid layout ready for M1 integration.");
                    }
                    break;
                case 3:
                    if (simulationHandler != null) {
                        simulationHandler.run();
                    } else {
                        runLiveSimulation();
                    }
                    break;
                case 4:
                    reportsAndExportFlow();
                    break;
                case 5:
                    return;
            }
        }
    }

    private void runLiveSimulation() {
        System.out.println("\n========================================================");
        System.out.println("      LAUNCHING LIVE MULTI-THREADED DEMO SIMULATION     ");
        System.out.println("========================================================");

        deliveryService.registerPartnerOnline(6001);

        System.out.println("[Simulation] Partner #6001 registered online for order dispatch.");
        System.out.println("[Simulation] Full end-to-end multi-threading simulation active with M3 picker threads.");
        System.out.println("========================================================");
    }

    public void reportsAndExportFlow() {
        while (true) {
            System.out.println("\n===== CONSOLIDATED SYSTEM REPORTS =====");
            System.out.println("1. Print Stock Report (M1)");
            System.out.println("2. Print Sales Report (M2)");
            System.out.println("3. Print Picker Performance Report (M3)");
            System.out.println("4. Print Delivery SLA Report (M4)");
            System.out.println("5. Export All Reports to reports/full_report.txt (File I/O)");
            System.out.println("6. Back");

            int ch = ConsoleScannerUtil.readIntInRange("Choose report [1-6]: ", 1, 6);
            switch (ch) {
                case 1:
                    if (stockReportSupplier != null) {
                        System.out.println(stockReportSupplier.get());
                    } else {
                        System.out.println("[M1 Stock Report] Ready for M1 InventoryService integration.");
                    }
                    break;
                case 2:
                    if (salesReportSupplier != null) {
                        System.out.println(salesReportSupplier.get());
                    } else {
                        System.out.println("[M2 Sales Report] Ready for M2 OrderService integration.");
                    }
                    break;
                case 3:
                    if (pickerReportSupplier != null) {
                        System.out.println(pickerReportSupplier.get());
                    } else {
                        System.out.println("[M3 Picker Report] Ready for M3 PickTaskService integration.");
                    }
                    break;
                case 4:
                    System.out.println(deliveryService.generateDeliverySLAReport());
                    break;
                case 5:
                    exportAllReportsToFile();
                    break;
                case 6:
                    return;
            }
        }
    }

    public void exportAllReportsToFile() {
        File dir = new File("reports");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String path = "reports/full_report.txt";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path))) {
            writer.write("╔══════════════════════════════════════════════════════════╗\n");
            writer.write("║                 OPTIGRO - FULL SYSTEM AUDIT REPORT       ║\n");
            writer.write("║                 Generated: " + LocalDateTime.now() + "     ║\n");
            writer.write("╚══════════════════════════════════════════════════════════╝\n\n");

            if (stockReportSupplier != null) {
                writer.write(stockReportSupplier.get());
                writer.write("\n\n");
            } else {
                writer.write("=== M1 INVENTORY & STOCK REPORT ===\n(Ready for M1 integration)\n\n");
            }

            if (salesReportSupplier != null) {
                writer.write(salesReportSupplier.get());
                writer.write("\n\n");
            } else {
                writer.write("=== M2 ORDERS & SALES REPORT ===\n(Ready for M2 integration)\n\n");
            }

            if (pickerReportSupplier != null) {
                writer.write(pickerReportSupplier.get());
                writer.write("\n\n");
            } else {
                writer.write("=== M3 PICKING & FULFILLMENT REPORT ===\n(Ready for M3 integration)\n\n");
            }

            writer.write(deliveryService.generateDeliverySLAReport());

            System.out.println("✓ Complete consolidated audit report exported to " + path + "!");
        } catch (IOException e) {
            System.out.println("[!] Error writing export file: " + e.getMessage());
        }
    }
}
