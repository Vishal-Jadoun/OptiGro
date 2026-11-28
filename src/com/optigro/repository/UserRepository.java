package com.optigro.repository;

import com.optigro.constants.UserRole;
import com.optigro.models.User;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class UserRepository implements BaseRepository<User> {
    private final ConcurrentHashMap<Integer, User> store = new ConcurrentHashMap<>();
    private final AtomicInteger counter = new AtomicInteger(100);

    @Override
    public void save(User user) {
        store.put(user.getUserId(), user);
    }

    @Override
    public Optional<User> findById(int id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<User> findAll() {
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

    public Optional<User> findByUsername(String username) {
        if (username == null) return Optional.empty();
        return store.values().stream()
            .filter(u -> u.getUsername().equalsIgnoreCase(username.trim()))
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
                if (parts.length < 4) continue;

                int id = Integer.parseInt(parts[0].trim());
                String username = parts[1].trim();
                String password = parts[2].trim();
                UserRole role = UserRole.valueOf(parts[3].trim());

                save(new User(id, username, password, role));
                if (id >= counter.get()) {
                    counter.set(id);
                }
            }
        }
    }
}
