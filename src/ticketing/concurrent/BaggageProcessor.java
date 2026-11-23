package ticketing.concurrent;

import java.util.concurrent.atomic.AtomicInteger;

import ticketing.model.Baggage;
import ticketing.repository.BookingRepository;

/**
 * Producer-consumer between CHECK-IN and LOADING.
 *
 *   producers : booking threads call submit(bag) after a booking is saved (bag status CHECKED_IN)
 *   queue     : BoundedBuffer<Baggage>
 *   consumer  : one "baggage-loader" thread takes bags and marks them LOADED in the repository
 *
 * awaitIdle() is another guarded block: callers wait() until every submitted bag has been processed.
 */
public class BaggageProcessor {
    private static final Baggage POISON_PILL = new Baggage();   // tells the consumer to stop

    private final BoundedBuffer<Baggage> buffer;
    private final BookingRepository repository;
    private final long delayMillis;
    private final boolean verbose;
    private final Thread consumer;

    private final Object idleLock = new Object();
    private int pending = 0;                                      // guarded by idleLock
    private final AtomicInteger loadedCount = new AtomicInteger();
    private final AtomicInteger skippedCount = new AtomicInteger();

    public BaggageProcessor(BookingRepository repository, int queueCapacity, long delayMillis, boolean verbose) {
        this.repository = repository;
        this.buffer = new BoundedBuffer<>(queueCapacity);
        this.delayMillis = delayMillis;
        this.verbose = verbose;
        this.consumer = new Thread(this::consumeLoop, "baggage-loader");
        this.consumer.start();
    }

    /** Producer side. Blocks (inside BoundedBuffer.put) if the queue is full. */
    public void submit(Baggage bag) {
        synchronized (idleLock) {
            pending++;
        }
        try {
            buffer.put(bag);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            finished();
        }
    }

    private void consumeLoop() {
        try {
            while (true) {
                Baggage bag = buffer.take();
                if (bag == POISON_PILL) {
                    break;
                }
                try {
                    if (delayMillis > 0) {
                        Thread.sleep(delayMillis);               // simulate physical loading time
                    }
                    if (repository.markLoaded(bag.getId())) {
                        loadedCount.incrementAndGet();
                        if (verbose) {
                            System.out.println("   [loader] tag " + bag.getTagNo() + " loaded onto aircraft");
                        }
                    } else {
                        skippedCount.incrementAndGet();          // cancelled before it was loaded
                    }
                } catch (RuntimeException e) {
                    System.err.println("[loader] failed for bag " + bag.getId() + ": " + e.getMessage());
                } finally {
                    finished();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void finished() {
        synchronized (idleLock) {
            pending--;
            idleLock.notifyAll();
        }
    }

    /** Blocks until all submitted bags have been processed. */
    public void awaitIdle() {
        synchronized (idleLock) {
            while (pending > 0) {
                try {
                    idleLock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    public int pendingCount() {
        synchronized (idleLock) {
            return pending;
        }
    }

    public int queueSize() {
        return buffer.size();
    }

    public int getLoadedCount() {
        return loadedCount.get();
    }

    public int getSkippedCount() {
        return skippedCount.get();
    }

    /** Lets the consumer finish queued work, then stops it. */
    public void shutdown() {
        try {
            buffer.put(POISON_PILL);
            consumer.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
