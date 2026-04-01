package com.payment.timesync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 *
 * Integration point: When Raft AppendEntries RPC arrives with new entries,
 * they are placed into this buffer. After the 200ms window expires,
 * entries are drained in chronological (correctedTimestamp) order.
 */
@Component
public class LogBuffer {

    private static final Logger log = LoggerFactory.getLogger(LogBuffer.class);

    private final PriorityQueue<BufferedLogEntry> buffer =
            new PriorityQueue<>((e1, e2) ->
                    Long.compare(e1.getCorrectedTimestamp(), e2.getCorrectedTimestamp())
            );

    private volatile long windowStartMs = 0;
    private static final long WINDOW_SIZE_MS = 200;

    /**
     * Adds an entry to the buffer and starts the 200ms timer if it's the first.
     */
    public synchronized void bufferEntry(BufferedLogEntry entry) {
        if (buffer.isEmpty()) {
            windowStartMs = System.currentTimeMillis();
        }
        buffer.offer(entry);
        log.debug("[LOG-BUFFER] Buffered entry: {}", entry);
    }

    /**
     * Drains the buffer if the 200ms window has expired.
     * Returns a sorted list of entries chronologically by correctedTimestamp.
     */
    public synchronized List<BufferedLogEntry> drainIfReady() {
        if (buffer.isEmpty()) return Collections.emptyList();

        long elapsedMs = System.currentTimeMillis() - windowStartMs;
        if (elapsedMs >= WINDOW_SIZE_MS) {
            List<BufferedLogEntry> drained = new ArrayList<>();
            while (!buffer.isEmpty()) {
                drained.add(buffer.poll()); // Already sorted by PriorityQueue
            }
            log.info("[LOG-BUFFER] Drained {} entries after {}ms window", drained.size(), elapsedMs);
            return drained;
        }
        return Collections.emptyList();
    }

    /**
     * Returns the current number of entries in the buffer.
     */
    public synchronized int size() {
        return buffer.size();
    }
}
