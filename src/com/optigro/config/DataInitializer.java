package com.optigro.config;

import com.optigro.repository.*;
import com.optigro.service.BinLayoutService;
import com.optigro.service.OrderService;

import java.io.File;
import java.io.IOException;

public class DataInitializer {

    public static void initialize(UserRepository userRepo,
                                  ProductRepository productRepo,
                                  BinLayoutService layout,
                                  CustomerRepository customerRepo,
                                  PickerRepository pickerRepo,
                                  DeliveryPartnerRepository partnerRepo,
                                  OrderService orderService) {

        System.out.println("\n╔══════════════════════════════════════════════════════════╗");
        System.out.println("║         OPTIGRO SYSTEM BOOT & PERSISTENCE INITIALIZER    ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");

        new File("data").mkdirs();
        new File("reports").mkdirs();

        try {
            
            userRepo.loadFromCsv("data/users.csv");
            partnerRepo.loadFromCsv("data/partners.csv");

            
            productRepo.loadFromCsv("data/products.csv");
            layout.autoAssignBins();

            
            customerRepo.loadFromCsv("data/customers.csv");

            
            pickerRepo.loadFromCsv("data/pickers.csv");

            System.out.println("✓ Persistence Boot Complete! System loaded from data/*.csv files.");
            System.out.printf("  Users: %-4d | Products: %-4d | Customers: %-4d | Pickers: %-4d | Partners: %-4d%n",
                userRepo.findAll().size(), productRepo.findAll().size(), customerRepo.findAll().size(),
                pickerRepo.findAll().size(), partnerRepo.findAll().size());

        } catch (IOException e) {
            System.out.println("[!] Warning during CSV load: " + e.getMessage() + ". Check file permissions.");
        }
    }
}
