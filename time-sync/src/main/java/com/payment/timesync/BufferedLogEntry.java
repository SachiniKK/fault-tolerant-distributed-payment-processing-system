package com.payment.timesync;

/**
 * Wrapper for log entries in the LogBuffer.
 * Holds both raw and NTP-corrected timestamps for reordering.
 *
 * This is used by LogBuffer to sort entries by correctedTimestamp
 * before committing them in the correct chronological order.
 *
 * Integration point: When integrating with Raft (Member 4),
 * this wraps a Raft LogEntry with time-sync metadata.
 */
public class BufferedLogEntry {
    private final String command;
    private final long timestamp;           // Raw system time (possibly skewed)
    private final long correctedTimestamp;   // NTP-corrected time

    public BufferedLogEntry(String command, long timestamp, long correctedTimestamp) {
        this.command = command;
        this.timestamp = timestamp;
        this.correctedTimestamp = correctedTimestamp;
    }

    public String getCommand() {
        return command;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public long getCorrectedTimestamp() {
        return correctedTimestamp;
    }

    @Override
    public String toString() {
        return "BufferedLogEntry{cmd=" + command +
                ", raw=" + timestamp +
                ", corrected=" + correctedTimestamp + "}";
    }
}
