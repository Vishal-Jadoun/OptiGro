package com.optigro.repository;

import com.optigro.models.Order;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class OrderRepository implements BaseRepository<Order> {
    private final ConcurrentHashMap<Integer, Order> store = new ConcurrentHashMap<>();
    private final AtomicInteger counter = new AtomicInteger(5000);

    @Override
    public void save(Order order) {
        store.put(order.getOrderId(), order);
    }

    @Override
    public Optional<Order> findById(int id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Order> findAll() {
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

    public List<Order> findByCustomerId(int customerId) {
        return store.values().stream()
            .filter(o -> o.getCustomerId() == customerId)
            .collect(Collectors.toList());
    }

    
    public synchronized void appendOrderToCsv(Order order, String filePath) throws IOException {
        File file = new File(filePath);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        boolean writeHeader = !file.exists() || file.length() == 0;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
            if (writeHeader) {
                writer.write("# orderId,customerId,total,paymentMode,status,placedAt\n");
            }
            writer.write(String.format("%d,%d,%.2f,%s,%s,%s%n",
                order.getOrderId(), order.getCustomerId(), order.getTotal(),
                order.getPaymentMode().name(), order.getStatus().name(), order.getPlacedAt()));
        }
    }
}
