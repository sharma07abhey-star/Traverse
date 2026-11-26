package ticketing.repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import ticketing.model.Identifiable;

/** Collection-based repository (HashMap-backed). Thread-safe because it uses ConcurrentHashMap. */
public class InMemoryRepository<T extends Identifiable> implements Repository<T> {
    protected final Map<Integer, T> store = new ConcurrentHashMap<>();
    private final AtomicInteger sequence = new AtomicInteger();

    @Override
    public T save(T entity) {
        entity.setId(sequence.incrementAndGet());
        store.put(entity.getId(), entity);
        return entity;
    }

    @Override
    public Optional<T> findById(int id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<T> findAll() {
        List<T> all = new ArrayList<>(store.values());
        all.sort(Comparator.comparingInt(Identifiable::getId));
        return all;
    }

    @Override
    public boolean update(T entity) {
        if (!store.containsKey(entity.getId())) {
            return false;
        }
        store.put(entity.getId(), entity);
        return true;
    }

    @Override
    public boolean delete(int id) {
        return store.remove(id) != null;
    }
}
