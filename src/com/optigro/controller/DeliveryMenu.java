package com.optigro.controller;

import com.optigro.constants.PartnerStatus;
import com.optigro.models.Delivery;
import com.optigro.models.DeliveryPartner;
import com.optigro.service.DeliveryService;
import com.optigro.util.ConsoleScannerUtil;

import java.util.Optional;

public class DeliveryMenu {
    private final DeliveryPartner partner;
    private final DeliveryService deliveryService;

    public DeliveryMenu(DeliveryPartner partner, DeliveryService deliveryService) {
        this.partner = partner;
        this.deliveryService = deliveryService;
    }

    public void show() {
        while (true) {
            System.out.printf("%n===== DELIVERY PARTNER PORTAL | %s (Status: %s) =====%n",
                partner.getName(), partner.getStatus());
            System.out.println("1. Toggle Status (Go Online / Offline)");
            System.out.println("2. View Assigned Active Order");
            System.out.println("3. Mark Order as Delivered (Calculates 10-Min SLA)");
            System.out.println("4. My Performance & Customer Ratings");
            System.out.println("5. Logout to Main Menu");

            int ch = ConsoleScannerUtil.readIntInRange("Choose [1-5]: ", 1, 5);
            switch (ch) {
                case 1:
                    if (partner.getStatus() == PartnerStatus.ONLINE) {
                        deliveryService.registerPartnerOffline(partner.getPartnerId());
                    } else if (partner.getStatus() == PartnerStatus.OFFLINE) {
                        deliveryService.registerPartnerOnline(partner.getPartnerId());
                    } else {
                        System.out.println("[!] Cannot go offline while actively delivering an order.");
                    }
                    break;

                case 2:
                    Optional<Delivery> active = deliveryService.findActiveDeliveryByPartner(partner.getPartnerId());
                    if (active.isEmpty()) {
                        System.out.println("No active order currently assigned. Status: " + partner.getStatus());
                    } else {
                        System.out.println("\n--- Assigned Active Delivery ---");
                        System.out.println(active.get());
                    }
                    break;

                case 3:
                    Optional<Delivery> deliveryToComplete = deliveryService.findActiveDeliveryByPartner(partner.getPartnerId());
                    if (deliveryToComplete.isEmpty()) {
                        System.out.println("[!] No active order assigned to mark delivered.");
                    } else {
                        deliveryService.markDelivered(deliveryToComplete.get().getDeliveryId());
                        System.out.println("✓ Order marked as DELIVERED!");
                    }
                    break;

                case 4:
                    System.out.println("\n--- My Performance Dashboard ---");
                    System.out.printf("Deliveries Completed: %d%n", partner.getDeliveriesDone());
                    System.out.printf("Average Customer Rating: %.2f / 5.0★%n", partner.getAverageRating());
                    if (partner.isLowRated()) {
                        System.out.println("⚠ Notice: Your average rating is below 3.0 stars. Quality review required.");
                    }
                    break;

                case 5:
                    return;
            }
        }
    }
}
