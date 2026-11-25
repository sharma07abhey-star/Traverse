package ticketing.repository;

import java.util.List;
import java.util.Optional;

import ticketing.model.Identifiable;

/**
 * Generic CRUD contract. {@code T extends Identifiable} is a bounded type parameter.
 * Implemented once with collections (in-memory) and once with JDBC.
 */
public interface Repository<T extends Identifiable> {
    T save(T entity);                    // Create (assigns id)

    Optional<T> findById(int id);        // Read one

    List<T> findAll();                   // Read all

    boolean update(T entity);            // Update

    boolean delete(int id);              // Delete
}
