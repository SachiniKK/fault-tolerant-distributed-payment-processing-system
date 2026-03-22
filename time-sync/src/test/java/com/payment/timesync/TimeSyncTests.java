package com.payment.timesync;

import com.payment.timesync.model.DualTimestamp;
import com.payment.timesync.service.LamportClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TimeSyncTests {

    private LamportClock clock;

    @BeforeEach
    void setUp() {
        clock = new LamportClock();
    }

    // --- LamportClock Tests ---

    @Test
    void clockStartsAtZero() {
        assertEquals(0, clock.getTime());
    }

    @Test
    void tickIncrementsClockByOne() {
        assertEquals(1, clock.tick());
        assertEquals(2, clock.tick());
        assertEquals(3, clock.tick());
    }

    @Test
    void updateTakesMaxOfLocalAndRemotePlusOne() {
        clock.tick(); // local = 1
        clock.tick(); // local = 2

        // Remote timestamp = 10 -> new value = max(2, 10) + 1 = 11
        long result = clock.update(10);
        assertEquals(11, result);
        assertEquals(11, clock.getTime());
    }

    @Test
    void updateWhenLocalIsHigher() {
        for (int i = 0; i < 20; i++)
            clock.tick(); // local = 20

        // Remote timestamp = 5 -> new value = max(20, 5) + 1 = 21
        long result = clock.update(5);
        assertEquals(21, result);
    }

    @Test
    void tickIsMonotonicallyIncreasing() {
        long prev = 0;
        for (int i = 0; i < 100; i++) {
            long current = clock.tick();
            assertTrue(current > prev);
            prev = current;
        }
    }

    @Test
    void resetSetsClockToZero() {
        clock.tick();
        clock.tick();
        clock.reset();
        assertEquals(0, clock.getTime());
    }

    @Test
    void concurrentTicksAreThreadSafe() throws InterruptedException {
        int threads = 10;
        int ticksPerThread = 1000;
        Thread[] threadArray = new Thread[threads];

        for (int i = 0; i < threads; i++) {
            threadArray[i] = new Thread(() -> {
                for (int j = 0; j < ticksPerThread; j++) {
                    clock.tick();
                }
            });
        }

        for (Thread t : threadArray)
            t.start();
        for (Thread t : threadArray)
            t.join();

        assertEquals(threads * ticksPerThread, clock.getTime());
    }

    // --- DualTimestamp Tests ---

    @Test
    void dualTimestampOrdersByLamportFirst() {
        DualTimestamp ts1 = new DualTimestamp(5000, 1, "node1");
        DualTimestamp ts2 = new DualTimestamp(4000, 2, "node2");

        // ts1 has lower Lamport (1 < 2), so ts1 < ts2
        assertTrue(ts1.compareTo(ts2) < 0);
    }

    @Test
    void dualTimestampUsesNtpAsTiebreaker() {
        DualTimestamp ts1 = new DualTimestamp(5000, 10, "node1");
        DualTimestamp ts2 = new DualTimestamp(6000, 10, "node2");

        // Same Lamport, ts1 has lower NTP, so ts1 < ts2
        assertTrue(ts1.compareTo(ts2) < 0);
    }

    @Test
    void dualTimestampUsesNodeIdAsFinalTiebreaker() {
        DualTimestamp ts1 = new DualTimestamp(5000, 10, "node1");
        DualTimestamp ts2 = new DualTimestamp(5000, 10, "node2");

        // Same Lamport and NTP, node1 < node2 lexicographically
        assertTrue(ts1.compareTo(ts2) < 0);
    }
}
