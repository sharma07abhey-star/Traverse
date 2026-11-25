package ticketing.concurrent;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Classic bounded buffer for the producer-consumer pattern (generic class).
 *
 * Monitor = the BoundedBuffer object itself.
 *  - put():  guarded block -> while the buffer is FULL, wait(); after adding, notifyAll().
 *  - take(): guarded block -> while the buffer is EMPTY, wait(); after removing, notifyAll().
 * The guard is always re-checked in a while loop (never "if") to survive spurious wake-ups.
 */
public class BoundedBuffer<T> {
    private final Queue<T> queue = new LinkedList<>();
    private final int capacity;

    public BoundedBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be > 0");
        }
        this.capacity = capacity;
    }

    public synchronized void put(T item) throws InterruptedException {
        while (queue.size() == capacity) {
            wait();                      // buffer full: producer waits, releasing the monitor
        }
        queue.add(item);
        notifyAll();                     // wake up consumers waiting for an item
    }

    public synchronized T take() throws InterruptedException {
        while (queue.isEmpty()) {
            wait();                      // buffer empty: consumer waits
        }
        T item = queue.poll();
        notifyAll();                     // wake up producers waiting for free space
        return item;
    }

    public synchronized int size() {
        return queue.size();
    }
}
