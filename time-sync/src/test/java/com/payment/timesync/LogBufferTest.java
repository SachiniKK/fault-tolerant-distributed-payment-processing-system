package com.payment.timesync;

import com.payment.consensus.model.LogEntry;
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
    void testPriorityQueueOrdering() {
        LogEntry e1 = new LogEntry(1, 1, "cmd1");
        e1.setCorrectedTimestamp(2000L);
        LogEntry e2 = new LogEntry(1, 2, "cmd2");
        e2.setCorrectedTimestamp(1000L); // Arrived out of order

        logBuffer.bufferEntry(e1);
        logBuffer.bufferEntry(e2);

        // Sleep to bypass window
        try { Thread.sleep(300); } catch (InterruptedException e) {}

        List<LogEntry> ready = logBuffer.drainIfReady();
        assertEquals(2, ready.size());
        assertEquals(1000L, ready.get(0).getCorrectedTimestamp());
        assertEquals(2000L, ready.get(1).getCorrectedTimestamp());
    }

    @Test
    void testWindowMechanism() {
        LogEntry e1 = new LogEntry(1, 1, "cmd1");
        e1.setCorrectedTimestamp(2000L);
        
        logBuffer.bufferEntry(e1);
        
        // Immediate drain should be empty (before 200ms)
        assertTrue(logBuffer.drainIfReady().isEmpty());
        
        // After 210ms should be ready
        try { Thread.sleep(210); } catch (InterruptedException e) {}
        assertFalse(logBuffer.drainIfReady().isEmpty());
    }

    @Test
    void testConcurrentInsertions() throws InterruptedException {
        int threads = 5;
        Thread[] devs = new Thread[threads];
        for (int i = 0; i < threads; i++) {
            final int id = i;
            devs[i] = new Thread(() -> {
                LogEntry e = new LogEntry(1, id, "cmd" + id);
                e.setCorrectedTimestamp(id * 100);
                logBuffer.bufferEntry(e);
            });
            devs[i].start();
        }
        for (Thread t : devs) t.join();
        
        assertEquals(threads, logBuffer.size());
    }
}
