package com.optigro;

import com.optigro.controller.MainMenuController;
import com.optigro.repository.DeliveryPartnerRepository;
import com.optigro.repository.UserRepository;
import com.optigro.service.AuthService;
import com.optigro.service.DeliveryService;

import java.io.IOException;

public class App {

    public static void main(String[] args) {
        System.out.println("Initializing OptiGro 10-Minute Grocery Logistics Engine...");

        UserRepository userRepo               = new UserRepository();
        DeliveryPartnerRepository partnerRepo  = new DeliveryPartnerRepository();

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

        AuthService auth = new AuthService(userRepo);
        DeliveryService delivery = new DeliveryService(partnerRepo);

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

        System.out.println("  ✓ All M4 components initialized successfully.");
        System.out.println("  ℹ M1/M2/M3 modules will activate when teammates integrate.\n");
        mainMenu.start();
    }
}
