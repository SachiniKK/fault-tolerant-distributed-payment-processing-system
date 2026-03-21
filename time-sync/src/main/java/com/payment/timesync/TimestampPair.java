package com.payment.timesync;

import java.io.Serializable;

/**
 * Holds both raw and NTP-corrected timestamps for each payment event.
 *
 * PDF reference (Page 7):
 * "timestamp and correctedTimestamp — Both stored so Member 3 can demonstrate the
 *  difference between raw (possibly skewed) and NTP-corrected times; also needed
 *  for the sync-on-rejoin mechanism ('give me transactions after timestamp X')"
 *
 * The raw timestamp may be affected by clock drift on the originating node.
 * The correctedTimestamp applies the NTP offset to give a globally consistent time.
 */
public class TimestampPair implements Serializable {

    private static final long serialVersionUID = 1L;

    private long timestamp;            // Raw, possibly drifted system time
    private long correctedTimestamp;   // After NTP offset applied

    public TimestampPair() {
    }

    public TimestampPair(long timestamp, long correctedTimestamp) {
        this.timestamp = timestamp;
        this.correctedTimestamp = correctedTimestamp;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getCorrectedTimestamp() {
        return correctedTimestamp;
    }

    public void setCorrectedTimestamp(long correctedTimestamp) {
        this.correctedTimestamp = correctedTimestamp;
    }

    /**
     * Returns the difference between corrected and raw timestamps.
     * Positive value means NTP time is ahead of local time.
     */
    public long getDifferenceMillis() {
        return correctedTimestamp - timestamp;
    }

    @Override
    public String toString() {
        return "TimestampPair{" +
                "timestamp=" + timestamp +
                ", correctedTimestamp=" + correctedTimestamp +
                ", diff=" + getDifferenceMillis() + "ms}";
    }
}
