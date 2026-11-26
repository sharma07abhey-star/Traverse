package ticketing.concurrent;

/**
 * One-shot starting gate: many threads call await() and block until the main thread calls open().
 * Used by the simulation so all "passengers" start booking at the same instant.
 */
public class StartGate {
    private boolean open = false;

    public synchronized void await() throws InterruptedException {
        while (!open) {
            wait();
        }
    }

    public synchronized void open() {
        open = true;
        notifyAll();
    }
}
