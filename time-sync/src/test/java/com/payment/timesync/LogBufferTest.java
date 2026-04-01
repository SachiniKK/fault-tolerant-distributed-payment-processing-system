package com.payment.timesync;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class LogBufferTest {

    private LogBuffer logBuffer;

    @BeforeEach
    void setUp() {
        logBuffer = new LogBuffer();
    }

    @Test
    void testPriorityQueueOrdering() throws InterruptedException {
        // Entry 1 has later correctedTimestamp (2000) than Entry 2 (1000)
        BufferedLogEntry e1 = new BufferedLogEntry("cmd1", 1900, 2000);
        BufferedLogEntry e2 = new BufferedLogEntry("cmd2", 900, 1000); // Arrived out of order

        logBuffer.bufferEntry(e1);
        logBuffer.bufferEntry(e2);

        // Wait for 200ms window to expire
        Thread.sleep(250);

        List<BufferedLogEntry> ready = logBuffer.drainIfReady();
        assertEquals(2, ready.size());
        assertEquals(1000, ready.get(0).getCorrectedTimestamp()); // Smaller first
        assertEquals(2000, ready.get(1).getCorrectedTimestamp()); // Larger second
    }

    @Test
    void testWindowMechanism() throws InterruptedException {
        BufferedLogEntry e1 = new BufferedLogEntry("cmd1", 1900, 2000);

        logBuffer.bufferEntry(e1);

        // Immediate drain should return empty (before 200ms)
        assertTrue(logBuffer.drainIfReady().isEmpty());

        // After 210ms the window should be expired
        Thread.sleep(210);
        assertFalse(logBuffer.drainIfReady().isEmpty());
    }

    @Test
    void testDrainReturnsChronologicalOrder() throws InterruptedException {
        // Add 5 entries in reverse order
        for (int i = 5; i >= 1; i--) {
            logBuffer.bufferEntry(new BufferedLogEntry("cmd" + i, i * 200, i * 100));
        }

        Thread.sleep(250);

        List<BufferedLogEntry> ready = logBuffer.drainIfReady();
        assertEquals(5, ready.size());
        for (int i = 0; i < ready.size() - 1; i++) {
            assertTrue(ready.get(i).getCorrectedTimestamp() <= ready.get(i + 1).getCorrectedTimestamp(),
                    "Entries should be in ascending correctedTimestamp order");
        }
    }

    @Test
    void testConcurrentInsertions() throws InterruptedException {
        int threads = 5;
        Thread[] devs = new Thread[threads];
        for (int i = 0; i < threads; i++) {
            final int id = i;
            devs[i] = new Thread(() -> {
                logBuffer.bufferEntry(new BufferedLogEntry("cmd" + id, id * 200, id * 100));
            });
            devs[i].start();
        }
        for (Thread t : devs) t.join();

        assertEquals(threads, logBuffer.size());
    }

    @Test
    void testEmptyBufferDrain() {
        List<BufferedLogEntry> ready = logBuffer.drainIfReady();
        assertTrue(ready.isEmpty());
    }
}
