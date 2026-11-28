package com.optigro.service;

import com.optigro.constants.PartnerStatus;
import com.optigro.constants.OrderStatus;
import com.optigro.models.Delivery;
import com.optigro.models.DeliveryPartner;
import com.optigro.models.Order;
import com.optigro.repository.DeliveryPartnerRepository;

import java.io.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class DeliveryService {
    private final DeliveryPartnerRepository partnerRepo;
    private final Queue<Integer> availablePartners = new ArrayDeque<>();
    private final Queue<Order> pendingOrders = new ArrayDeque<>();
    private final ConcurrentHashMap<Integer, Delivery> deliveries = new ConcurrentHashMap<>();
    private final AtomicInteger deliveryCounter = new AtomicInteger(7000);
    private final Set<Integer> flaggedLowRatedPartners = new HashSet<>();

    private java.util.function.Function<Integer, Optional<Order>> orderLookup;
    private java.util.function.BiConsumer<Integer, OrderStatus> orderStatusUpdater;

    public DeliveryService(DeliveryPartnerRepository partnerRepo) {
        this.partnerRepo = partnerRepo;
    }

    public void setOrderLookup(java.util.function.Function<Integer, Optional<Order>> orderLookup) {
        this.orderLookup = orderLookup;
    }

    public void setOrderStatusUpdater(java.util.function.BiConsumer<Integer, OrderStatus> orderStatusUpdater) {
        this.orderStatusUpdater = orderStatusUpdater;
    }

    public synchronized void registerPartnerOnline(int partnerId) {
        partnerRepo.findById(partnerId).ifPresent(p -> {
            p.setStatus(PartnerStatus.ONLINE);
            if (!availablePartners.contains(partnerId)) {
                availablePartners.offer(partnerId);
            }
            System.out.printf("[DeliveryHub] Partner %s is now ONLINE (Queue Position: %d)%n",
                p.getName(), availablePartners.size());

            if (!pendingOrders.isEmpty()) {
                Order waiting = pendingOrders.poll();
                System.out.printf("[DeliveryHub] Auto-dispatching pending Order #%d to newly online partner %s%n",
                    waiting.getOrderId(), p.getName());
                dispatch(waiting);
            }
        });
    }

    public synchronized void registerPartnerOffline(int partnerId) {
        partnerRepo.findById(partnerId).ifPresent(p -> {
            p.setStatus(PartnerStatus.OFFLINE);
            availablePartners.remove(partnerId);
            System.out.printf("[DeliveryHub] Partner %s is now OFFLINE.%n", p.getName());
        });
    }

    public synchronized Delivery dispatch(Order order) {
        if (availablePartners.isEmpty()) {
            pendingOrders.offer(order);
            System.out.printf("[DeliveryHub] No delivery partners online! Order #%d queued in pending queue.%n",
                order.getOrderId());
            return null;
        }

        int partnerId = availablePartners.poll();
        DeliveryPartner partner = partnerRepo.findById(partnerId).orElseThrow();
        partner.setStatus(PartnerStatus.ON_DELIVERY);

        int deliveryId = deliveryCounter.incrementAndGet();
        Delivery delivery = new Delivery(deliveryId, order.getOrderId(), partnerId);
        deliveries.put(deliveryId, delivery);

        if (orderStatusUpdater != null) {
            orderStatusUpdater.accept(order.getOrderId(), OrderStatus.OUT_FOR_DELIVERY);
        }

        System.out.printf("[DeliveryHub] Order #%d dispatched to Partner %s (Delivery #%d)!%n",
            order.getOrderId(), partner.getName(), deliveryId);

        return delivery;
    }

    public synchronized void markDelivered(int deliveryId) {
        Delivery delivery = deliveries.get(deliveryId);
        if (delivery == null) {
            throw new IllegalArgumentException("Delivery record not found: " + deliveryId);
        }

        LocalDateTime now = LocalDateTime.now();
        delivery.setDeliveredAt(now);

        long slaMinutes = -1;
        boolean met = false;
        if (orderLookup != null) {
            Optional<Order> orderOpt = orderLookup.apply(delivery.getOrderId());
            if (orderOpt.isPresent()) {
                Duration totalDuration = Duration.between(orderOpt.get().getPlacedAt(), now);
                slaMinutes = totalDuration.toMinutes();
                met = slaMinutes <= 10;
            }
        }
        delivery.setMetSla(met);

        if (orderStatusUpdater != null) {
            orderStatusUpdater.accept(delivery.getOrderId(), OrderStatus.DELIVERED);
        }

        DeliveryPartner partner = partnerRepo.findById(delivery.getPartnerId()).orElseThrow();
        partner.setStatus(PartnerStatus.ONLINE);
        availablePartners.offer(partner.getPartnerId());

        System.out.println("\n╔══════════════════════════════════════════════════════════╗");
        System.out.printf("║ DELIVERED: Order #%-6d fulfilled by %-18s ║%n",
            delivery.getOrderId(), partner.getName());
        if (slaMinutes >= 0) {
            System.out.printf("║ Total Fulfillment Time: %-2d minutes                       ║%n", slaMinutes);
            System.out.printf("║ 10-Minute SLA Verdict: [%-28s] ║%n",
                (met ? "MET ✓ (Within 10 Mins)" : "BREACHED ✗ (> 10 Mins)"));
        }
        System.out.println("╚══════════════════════════════════════════════════════════╝");

        try {
            appendDeliveryToCsv(delivery, "data/deliveries.csv");
        } catch (IOException e) {
            System.err.println("[!] Notice: Delivery marked in memory, failed to log to disk: " + e.getMessage());
        }

        if (!pendingOrders.isEmpty()) {
            Order next = pendingOrders.poll();
            dispatch(next);
        }
    }

    public synchronized void appendDeliveryToCsv(Delivery d, String filePath) throws IOException {
        File file = new File(filePath);
        boolean writeHeader = !file.exists() || file.length() == 0;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
            if (writeHeader) {
                writer.write("# deliveryId,orderId,partnerId,dispatchedAt,deliveredAt,metSla,rating\n");
            }
            writer.write(String.format("%d,%d,%d,%s,%s,%b,%d%n",
                d.getDeliveryId(), d.getOrderId(), d.getPartnerId(),
                d.getDispatchedAt(), d.getDeliveredAt(), d.isMetSla(), d.getRating()));
        }
    }

    public void rateDelivery(int deliveryId, int stars) {
        Delivery d = deliveries.get(deliveryId);
        if (d != null) {
            d.setRating(stars);
            DeliveryPartner p = partnerRepo.findById(d.getPartnerId()).orElseThrow();
            p.addRating(stars);

            if (p.isLowRated()) {
                flaggedLowRatedPartners.add(p.getPartnerId());
            }
        }
    }

    public Optional<Delivery> findActiveDeliveryByPartner(int partnerId) {
        return deliveries.values().stream()
            .filter(d -> d.getPartnerId() == partnerId && d.getDeliveredAt() == null)
            .findFirst();
    }

    public Set<Integer> getFlaggedLowRatedPartners() {
        return Collections.unmodifiableSet(flaggedLowRatedPartners);
    }

    public List<Delivery> getAllDeliveries() {
        return new ArrayList<>(deliveries.values());
    }

    public String generateDeliverySLAReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("===== OPTIGRO 10-MINUTE SLA & DELIVERY REPORT =====\n");
        sb.append(String.format("%-8s %-8s %-16s %-14s %-10s %-8s%n",
            "DelivID", "OrderID", "Partner", "Time (Mins)", "SLA", "Rating"));
        sb.append("-".repeat(70)).append("\n");

        int total = 0;
        int metCount = 0;

        for (Delivery d : deliveries.values()) {
            total++;
            if (d.isMetSla()) metCount++;

            String timeStr = "In Transit";
            if (d.getDeliveredAt() != null && orderLookup != null) {
                Optional<Order> orderOpt = orderLookup.apply(d.getOrderId());
                if (orderOpt.isPresent()) {
                    long mins = Duration.between(orderOpt.get().getPlacedAt(), d.getDeliveredAt()).toMinutes();
                    timeStr = mins + " mins";
                }
            }

            DeliveryPartner p = partnerRepo.findById(d.getPartnerId()).orElse(null);
            String partnerName = p != null ? p.getName() : "Unknown";

            sb.append(String.format("%-8d %-8d %-16s %-14s %-10s %-8s%n",
                d.getDeliveryId(), d.getOrderId(), partnerName, timeStr,
                d.isMetSla() ? "MET ✓" : (d.getDeliveredAt() == null ? "PENDING" : "BREACH ✗"),
                d.getRating() > 0 ? d.getRating() + "★" : "-"));
        }

        sb.append(String.format("%nTotal Completed Deliveries: %d%n", total));
        if (total > 0) {
            sb.append(String.format("10-Minute SLA Adherence: %d/%d (%.1f%%)%n",
                metCount, total, (metCount * 100.0 / total)));
        }

        if (!flaggedLowRatedPartners.isEmpty()) {
            sb.append("\nFlagged Low-Rated Partners (< 3.0 Stars):\n");
            for (int pid : flaggedLowRatedPartners) {
                partnerRepo.findById(pid).ifPresent(p ->
                    sb.append(String.format(" - Partner #%d: %s (Avg: %.1f stars)%n",
                        pid, p.getName(), p.getAverageRating())));
            }
        }
        return sb.toString();
    }
}
