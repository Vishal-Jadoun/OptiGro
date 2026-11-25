package com.optigro.models;

import com.optigro.constants.TaskStatus;

import java.time.LocalDateTime;
import java.util.*;

public class PickTask implements Comparable<PickTask>{
    private final int taskId;
    private final int orderId;
    private int pickerId;
    private TaskStatus status ;
    private final LocalDateTime createdAt;
    private final int priority; 
    private final List<Substitution> substitutions;
    private final Set<Integer> pickedProductIds;

    public PickTask(int taskId, int orderId, int priority) {
        this.taskId = taskId;
        this.orderId = orderId;
        this.pickerId = -1; // Unassigned initially
        this.status = TaskStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.priority = priority;
        this.substitutions = new ArrayList<>();
        this.pickedProductIds = new HashSet<>();
    }

    public int getTaskId() { return taskId; }
    public int getOrderId() { return orderId; }
    public int getPickerId() { return pickerId; }
    public void setPickerId(int pickerId) { this.pickerId = pickerId;}
    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public int getPriority() { return priority; }
    public List<Substitution> getSubstitutions() { return Collections.unmodifiableList(substitutions); }

    public void addSubstitution(Substitution sub) {
        substitutions.add(sub);
    }

    public synchronized boolean markItemPicked(int productId) {
        return pickedProductIds.add(productId); 
    }
    public synchronized boolean isItemPicked(int productId) {
        return pickedProductIds.contains(productId);
    }
    public Set<Integer> getPickedProductIds() {
        return Collections.unmodifiableSet(pickedProductIds);
    }

    @Override
    public int compareTo(PickTask other) {
        int pCompare = Integer.compare(this.priority, other.priority);
        if (pCompare != 0) return pCompare; 
        return this.createdAt.compareTo(other.createdAt); 
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PickTask)) return false;
        PickTask pickTask = (PickTask) o;
        return taskId == pickTask.taskId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskId);
    }

    @Override
    public String toString() {
        return String.format("Task #%d | Order #%d | Picker: %d | Status: %s | Priority: %d | Picked Items: %d",
            taskId, orderId, pickerId, status, priority, pickedProductIds.size());
    }
}