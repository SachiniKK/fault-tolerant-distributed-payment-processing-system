package com.payment.timesync.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Lamport Logical Clock implementation.
 *
 * From lectures: Lamport clocks establish a happened-before relation
 * between events in a distributed system without relying on physical clocks.
 *
 * Rules:
 * 1. Before each local event: clock = clock + 1
 * 2. When sending a message: attach current clock value
 * 3. When receiving a message with timestamp T: clock = max(clock, T) + 1
 *
 * Property: if event A happened-before event B, then L(A) < L(B)
 * Note: the converse is NOT true (L(A) < L(B) does NOT imply A -> B)
 */
@Service
public class LamportClock {
    private static final Logger log = LoggerFactory.getLogger(LamportClock.class);

    private final AtomicLong counter = new AtomicLong(0);

    /**
     * Increment for a local event.
     * 
     * @return the new clock value after increment
     */
    public long tick() {
        long newValue = counter.incrementAndGet();
        log.debug("[LAMPORT] tick -> {}", newValue);
        return newValue;
    }

    /**
     * Update on receiving a message with remote timestamp.
     * clock = max(local, received) + 1
     * 
     * @param receivedTimestamp the Lamport timestamp from the remote message
     * @return the new clock value after update
     */
    public long update(long receivedTimestamp) {
        long newValue = counter.updateAndGet(current -> Math.max(current, receivedTimestamp) + 1);
        log.debug("[LAMPORT] update(received={}) -> {}", receivedTimestamp, newValue);
        return newValue;
    }

    /**
     * Returns current clock value without incrementing.
     */
    public long getTime() {
        return counter.get();
    }

    /**
     * Resets the clock to zero. Used in tests.
     */
    public void reset() {
        counter.set(0);
    }
}
