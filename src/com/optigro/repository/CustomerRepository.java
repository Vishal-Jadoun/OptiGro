package com.optigro.repository;

import com.optigro.models.Customer;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;


public class CustomerRepository implements BaseRepository<Customer> {
    private final ConcurrentHashMap<Integer, Customer> store = new ConcurrentHashMap<>();
    private final AtomicInteger counter = new AtomicInteger(2000);

    @Override
    public void save(Customer customer) {
        store.put(customer.getCustomerId(), customer);
    }

    @Override
    public Optional<Customer> findById(int id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Customer> findAll() {
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

    public Optional<Customer> findByUserId(int userId) {
        return store.values().stream()
            .filter(c -> c.getUserId() == userId)
            .findFirst();
    }

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
                String phone = parts[3].trim();
                String address = parts[4].trim();
                double balance = Double.parseDouble(parts[5].trim());

                save(new Customer(id, userId, name, phone, address, balance));
                if (id >= counter.get()) {
                    counter.set(id);
                }
            }
        }
    }
}
