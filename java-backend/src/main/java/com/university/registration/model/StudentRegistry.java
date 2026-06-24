package com.university.registration.model;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * ADT: StudentRegistry&lt;T&gt;
 *
 * A generic registry that can store any type with a String ID.
 * Used as StudentRegistry&lt;Student&gt; in this system, but the type
 * parameter makes it reusable for any identifiable entity.
 *
 * Demonstrates:
 *   - Generics with a bounded type parameter (Registrable interface)
 *   - Higher-order methods using Predicate&lt;T&gt; (functional interface)
 *   - Encapsulation of an internal Map
 *   - Reusable library component
 *
 * @param <T> any type that implements Registrable (has a String ID)
 */
public class StudentRegistry<T extends StudentRegistry.Registrable> {

    /**
     * Functional interface — any stored type must be able to return its ID.
     * This is the bounded type constraint on T.
     */
    public interface Registrable {
        String getId();
    }

    //  Internal store 
    private final Map<String, T> store = new LinkedHashMap<>();
    private final String registryName;

    public StudentRegistry(String registryName) {
        this.registryName = Objects.requireNonNull(registryName);
    }

    // CRUD

    /**
     * Registers an entity. Throws if the ID is already taken.
     */
    public void register(T entity) {
        Objects.requireNonNull(entity, "entity must not be null");
        String id = entity.getId();
        if (store.containsKey(id))
            throw new IllegalArgumentException("ID already registered: " + id);
        store.put(id, entity);
    }

    /**
     * Retrieves an entity by ID, wrapped in an Optional.
     */
    public Optional<T> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    /**
     * Removes an entity by ID.
     * @return true if removed, false if ID was not found.
     */
    public boolean remove(String id) {
        return store.remove(id) != null;
    }

    public boolean contains(String id) {
        return store.containsKey(id);
    }

    public int size() { return store.size(); }

    //  Higher-order query methods (functional interfaces)

    /**
     * Returns all entities matching the given predicate.
     * Example: registry.findWhere(s -> s.getLevel() == SENIOR)
     *
     * @param predicate a Predicate&lt;T&gt; — a functional interface
     */
    public List<T> findWhere(Predicate<T> predicate) {
        return store.values().stream()
            .filter(predicate)
            .collect(Collectors.toList());
    }

    /**
     * Returns the count of entities matching the predicate.
     */
    public long countWhere(Predicate<T> predicate) {
        return store.values().stream().filter(predicate).count();
    }

    /**
     * Returns all entities as an unmodifiable list.
     */
    public List<T> getAll() {
        return Collections.unmodifiableList(new ArrayList<>(store.values()));
    }

    // Object contract

    @Override
    public String toString() {
        return String.format("StudentRegistry['%s', size=%d]", registryName, store.size());
    }
}
