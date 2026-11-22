package com.optigro.repository;

import java.util.List;
import java.util.Optional;

/**
 * Generic repository interface for all data access objects.
 * Each repository implementation stores entities in a ConcurrentHashMap.
 */
public interface BaseRepository<T> {
    void save(T entity);
    Optional<T> findById(int id);
    List<T> findAll();
    boolean deleteById(int id);
    int nextId();
}
