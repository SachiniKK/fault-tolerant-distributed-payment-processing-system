package com.payment.timesync;

import com.payment.consensus.model.LogEntry;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

/**
 * LogBuffer handles out-of-order event reordering before committing to Raft.
 * 
 * From PDF (Page 10):
 * "LogBuffer is like a waiting room... hold it for a short window (200ms)...
 * sort everything by correctedTimestamp and commit them in the right order."
 */
@Component
public class LogBuffer {

    private final PriorityQueue<LogEntry> buffer = new PriorityQueue<>((e1, e2) -> 
        Long.compare(e1.getCorrectedTimestamp(), e2.getCorrectedTimestamp())
    );

    private volatile long windowStartMs = 0;
    private static final long WINDOW_SIZE_MS = 200;

    /**
     * Adds an entry to the buffer and starts the 200ms timer if it's the first.
     */
    public synchronized void bufferEntry(LogEntry entry) {
        if (buffer.isEmpty()) {
            windowStartMs = System.currentTimeMillis();
        }
        buffer.offer(entry);
    }

    /**
     * Drains the buffer if the 200ms window has expired.
     * Returns a sorted list of entries chronologically.
     */
    public synchronized List<LogEntry> drainIfReady() {
        if (buffer.isEmpty()) return Collections.emptyList();

        long elapsedMs = System.currentTimeMillis() - windowStartMs;
        if (elapsedMs >= WINDOW_SIZE_MS) {
            List<LogEntry> drained = new ArrayList<>();
            while (!buffer.isEmpty()) {
                drained.add(buffer.poll()); // Already sorted by PriorityQueue
            }
            return drained;
        }
        return Collections.emptyList();
    }

    public synchronized int size() {
        return buffer.size();
    }
}
