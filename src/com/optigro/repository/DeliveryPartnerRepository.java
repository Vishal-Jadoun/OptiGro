package com.optigro.repository;

import com.optigro.constants.PartnerStatus;
import com.optigro.models.DeliveryPartner;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class DeliveryPartnerRepository implements BaseRepository<DeliveryPartner> {
    private final ConcurrentHashMap<Integer, DeliveryPartner> store = new ConcurrentHashMap<>();
    private final AtomicInteger counter = new AtomicInteger(6000);

    @Override
    public void save(DeliveryPartner partner) {
        store.put(partner.getPartnerId(), partner);
    }

    @Override
    public Optional<DeliveryPartner> findById(int id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<DeliveryPartner> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public boolean deleteById(int id) {
        return store.remove(id) != null;
    }

    @Override
    public int nextId() {
        return counter.incrementAndGet();
    }

    public Optional<DeliveryPartner> findByUserId(int userId) {
        return store.values().stream()
            .filter(p -> p.getUserId() == userId)
            .findFirst();
    }

    public List<DeliveryPartner> findOnline() {
        return store.values().stream()
            .filter(p -> p.getStatus() == PartnerStatus.ONLINE)
            .collect(Collectors.toList());
    }

    // ==========================================
    // File Persistence: Read data/partners.csv
    // ==========================================
    public void loadFromCsv(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] parts = line.split(",");
                if (parts.length < 6) continue;

                int id = Integer.parseInt(parts[0].trim());
                int userId = Integer.parseInt(parts[1].trim());
                String name = parts[2].trim();
                PartnerStatus status = PartnerStatus.valueOf(parts[3].trim());
                double rating = Double.parseDouble(parts[4].trim());
                int deliveries = Integer.parseInt(parts[5].trim());

                save(new DeliveryPartner(id, userId, name, status, rating, deliveries));
                if (id >= counter.get()) {
                    counter.set(id);
                }
            }
        }
    }
}
