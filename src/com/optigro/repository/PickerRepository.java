package com.optigro.repository;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class PickerRepository implements BaseRepository<Picker> {
    private final ConcurrentHashMap<Integer, Picker> store = new ConcurrentHashMap<>();
    private final AtomicInteger counter = new AtomicInteger(3000);

    @Override
    public void save(Picker picker) {
        store.put(picker.getPickerId(), picker);
    }

    @Override
    public Optional<Picker> findById(int id) {
        return Optional.ofNullable(store.get(id));
    }
    @Override
    public List<Picker> findAll() {
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

    public List<Picker> findAvailable() {
        return store.values().stream()
            .filter(Picker::isAvailable)
            .collect(Collectors.toList());
    }
    public Optional<Picker> findByUserId(int userId) {
        return store.values().stream()
            .filter(p -> p.getUserId() == userId)
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
                if (parts.length < 5) continue;
                int id = Integer.parseInt(parts[0].trim());
                int userId = Integer.parseInt(parts[1].trim());
                String name = parts[2].trim();
                boolean available = Boolean.parseBoolean(parts[3].trim());
                int tasks = Integer.parseInt(parts[4].trim());
                Picker picker = new Picker(id, userId, name);
                picker.setAvailable(available);
                for (int i = 0; i < tasks; i++) picker.incrementTasksCompleted();
                save(picker);
                if (id >= counter.get()) {
                    counter.set(id);
                }
            }
        }
    }
}