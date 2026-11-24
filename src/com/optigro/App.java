package com.optigro;

import com.optigro.controller.MainMenuController;
import com.optigro.repository.DeliveryPartnerRepository;
import com.optigro.repository.UserRepository;
import com.optigro.service.AuthService;
import com.optigro.service.DeliveryService;

import java.io.IOException;

/**
 * OptiGro — 10-Minute Grocery Delivery Logistics Engine
 *
 * Master application bootstrap with Manual Constructor Injection.
 * This class wires all repositories, services, and controllers,
 * loads seed data from CSV files, and launches the main menu.
 *
 * Architecture: Controller → Service → Repository → Model
 * Design pattern: Constructor injection (no frameworks)
 *
 * M4 Integration Notes:
 * - M1 (Inventory), M2 (Orders), M3 (Picking) handlers are set via
 *   pluggable callbacks on MainMenuController, so the application
 *   launches cleanly even when those modules are not yet integrated.
 * - DeliveryService uses functional callbacks (setOrderLookup,
 *   setOrderStatusUpdater) to avoid a direct dependency on M2's
 *   OrderService. These will be wired when M2's services are ready.
 *
 * @author Aditya (M4 — Security, Delivery & Platform Integration)
 */
public class App {

    public static void main(String[] args) {
        System.out.println("Initializing OptiGro 10-Minute Grocery Logistics Engine...");

        // ==========================================
        // 1. REPOSITORIES  (Data Access Layer)
        // ==========================================
        UserRepository userRepo               = new UserRepository();
        DeliveryPartnerRepository partnerRepo  = new DeliveryPartnerRepository();

        // M1 repos: ProductRepository          — pending M1 implementation
        // M2 repos: CustomerRepository, OrderRepository — pending M2 implementation
        // M3 repos: PickerRepository, PickTaskRepository — pending M3 implementation

        // ==========================================
        // 2. LOAD SEED DATA FROM CSV  (File I/O)
        // ==========================================
        try {
            userRepo.loadFromCsv("data/users.csv");
            System.out.println("  ✓ Loaded users from data/users.csv");
        } catch (IOException e) {
            System.err.println("  [!] Warning: Could not load users.csv — " + e.getMessage());
        }

        try {
            partnerRepo.loadFromCsv("data/partners.csv");
            System.out.println("  ✓ Loaded delivery partners from data/partners.csv");
        } catch (IOException e) {
            System.err.println("  [!] Warning: Could not load partners.csv — " + e.getMessage());
        }

        // M1: DataInitializer.initialize(...) — pending M1/M3 implementation
        // M2: customerRepo.loadFromCsv("data/customers.csv") — pending M2
        // M1: productRepo.loadFromCsv("data/products.csv")   — pending M1

        // ==========================================
        // 3. SERVICES  (Constructor Injection)
        // ==========================================

        // M4 Layer 1: Authentication — depends only on UserRepository
        AuthService auth = new AuthService(userRepo);

        // M4 Layer 2: Delivery Logistics — depends on DeliveryPartnerRepository
        DeliveryService delivery = new DeliveryService(partnerRepo);

        // M1 services: InventoryService, BinLayoutService, ExpiryService — pending
        // M2 services: PricingService, CartService, OrderService          — pending
        // M3 services: PickTaskService, SubstitutionService, PickRoutePlanner — pending

        // ==========================================
        // 4. DECOUPLED CALLBACK WIRING
        // ==========================================
        // When M2 OrderService is available, wire:
        //   delivery.setOrderLookup(orders::getOrder);
        //   delivery.setOrderStatusUpdater(orders::updateStatus);
        //
        // When M2 orders + M3 picking are available, wire:
        //   orders.setOnOrderPlaced(picking::createTask);
        //   picking.setOnOrderPacked(delivery::dispatch);
        //
        // These callbacks are null-safe; DeliveryService and
        // MainMenuController handle missing wiring gracefully
        // by printing placeholder messages.

        // ==========================================
        // 5. CONTROLLER  (Presentation Layer)
        // ==========================================
        MainMenuController mainMenu = new MainMenuController(auth, delivery, partnerRepo);

        // M1/M2/M3 pluggable handlers — will be wired when available:
        // mainMenu.setCustomerMenuHandler(user -> customerMenu.show(user));
        // mainMenu.setPickerMenuHandler(user -> pickerMenu.show(user));
        // mainMenu.setInventoryMenuHandler(() -> inventoryMenu.show());
        // mainMenu.setGridLayoutHandler(() -> layout.printGrid());
        // mainMenu.setSimulationHandler(() -> simulation.run());
        // mainMenu.setReportSuppliers(
        //     inventory::generateStockReport,
        //     orders::generateSalesReport,
        //     picking::generatePickerReport
        // );

        // ==========================================
        // 6. LAUNCH
        // ==========================================
        System.out.println("  ✓ All M4 components initialized successfully.");
        System.out.println("  ℹ M1/M2/M3 modules will activate when teammates integrate.\n");
        mainMenu.start();
    }
}
