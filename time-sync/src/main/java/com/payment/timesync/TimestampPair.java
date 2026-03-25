package com.payment.timesync;

/**
 * POJO for dual timestamp pattern.
 */
public class TimestampPair {
    private final long timestamp;           // Raw, possibly drifted
    private final long correctedTimestamp;  // After NTP offset applied

    public TimestampPair(long timestamp, long correctedTimestamp) {
        this.timestamp = timestamp;
        this.correctedTimestamp = correctedTimestamp;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public long getCorrectedTimestamp() {
        return correctedTimestamp;
    }
}
