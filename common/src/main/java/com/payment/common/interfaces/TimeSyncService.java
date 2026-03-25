package com.payment.common.interfaces;

/**
 * Interface for Time Synchronization module (Member 3).
 *
 * Responsibilities:
 * - NTP-based clock synchronization
 * - Lamport logical clocks for event ordering
 * - Timestamp correction for consistency
 */
public interface TimeSyncService {

    /**
     * Get NTP-corrected current timestamp.
     * This timestamp is synchronized across all nodes.
     */
    long getSynchronizedTimestamp();

    /**
     * Get current Lamport clock value without incrementing.
     */
    long getLamportClock();

    /**
     * Increment and return new Lamport clock value.
     * Call this for each local event (e.g., processing a payment).
     */
    long tickLamportClock();

    /**
     * Update Lamport clock when receiving a message with remote timestamp.
     * Uses: clock = max(local, received) + 1
     *
     * @param receivedTimestamp The Lamport timestamp from the remote message
     * @return The new clock value after update
     */
    long updateLamportClock(long receivedTimestamp);

    /**
     * Get the NTP offset in milliseconds.
     * Positive means local clock is behind server.
     */
    long getNtpOffsetMs();

    /**
     * Check if NTP sync is working.
     */
    boolean isSyncHealthy();
}
