package com.optigro.repository;

import com.optigro.constants.TaskStatus;
import com.optigro.models.PickTask;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;     

public class PickTaskRepository implements BaseRepository<PickTask> {
    private final ConcurrentHashMap<Integer, PickTask> store = new ConcurrentHashMap<>();
    private final AtomicInteger counter = new AtomicInteger(4000);

    @Override
    public void save(PickTask task) {
        store.put(task.getTaskId(), task);
    }
    @Override
    public Optional<PickTask> findById(int id) {
        return Optional.ofNullable(store.get(id));
    }
    @Override
    public List<PickTask> findAll() {
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

    public List<PickTask> findByPickerId(int pickerId) {
        return store.values().stream()
            .filter(t -> t.getPickerId() == pickerId)
            .collect(Collectors.toList());
    }
    public List<PickTask> findByStatus(TaskStatus status) {
        return store.values().stream()
            .filter(t -> t.getStatus() == status)
            .collect(Collectors.toList());
    }

    public synchronized void appendTaskHistoryToCsv(PickTask task, String filePath) throws IOException {
        File file = new File(filePath);
        boolean writeHeader = !file.exists() || file.length() == 0;
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
            if (writeHeader) {
                writer.write("# taskId,orderId,pickerId,status,priority,substitutionsCount,createdAt\n");
            }
            writer.write(String.format("%d,%d,%d,%s,%d,%d,%s%n",
                task.getTaskId(), task.getOrderId(), task.getPickerId(),
                task.getStatus().name(), task.getPriority(),
                task.getSubstitutions().size(), task.getCreatedAt()));
        }
    }
}