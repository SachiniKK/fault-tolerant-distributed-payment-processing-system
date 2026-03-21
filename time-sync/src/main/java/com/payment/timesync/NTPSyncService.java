package com.payment.timesync;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.net.InetAddress;

/**
 * NTP Synchronization Service — connects to pool.ntp.org to calculate
 * the offset between the local system clock and authoritative NTP time.
 *
 * PDF reference (Step 7A, Page 9):
 * "volatile long offsetMillis; // Must use volatile keyword"
 * "NTP accounts for network delay:
 *  offsetMillis = (server_time - local_time) - (network_round_trip / 2)"
 *
 * Architecture notes:
 * - Uses volatile for thread-safe offset reads (multiple threads may read concurrently)
 * - @Scheduled runs sync every 60 seconds on a Spring-managed thread
 * - Graceful degradation: if NTP is unreachable, keeps last known offset
 * - Uses Apache Commons Net NTPUDPClient for standard NTP protocol support
 */
@Service
public class NTPSyncService {

    private static final Logger log = LoggerFactory.getLogger(NTPSyncService.class);

    /**
     * NTP server pool to query. Uses the general pool which auto-selects
     * the closest geographically-available server.
     */
    private static final String NTP_SERVER = "pool.ntp.org";

    /**
     * Timeout for NTP UDP requests in milliseconds.
     * Prevents hanging if server is unreachable.
     */
    private static final int NTP_TIMEOUT_MS = 5000;

    /**
     * The calculated offset between local clock and NTP authoritative time.
     * MUST be volatile — multiple threads read this value:
     * - The @Scheduled sync thread writes it
     * - Request handler threads read it via TimestampGenerator
     *
     * PDF reference: "volatile long offsetMillis"
     */
    private volatile long offsetMillis = 0;

    /**
     * Tracks whether at least one successful sync has occurred.
     * Used for logging and diagnostics.
     */
    private volatile boolean synced = false;

    /**
     * Runs at application startup to perform the first NTP sync immediately.
     * Follows the same @PostConstruct pattern used by RaftNode.init().
     */
    @PostConstruct
    public void init() {
        log.info("[NTP] NTPSyncService starting. Initial sync with {}...", NTP_SERVER);
        performSync();
        log.info("[NTP] NTP sync scheduled every 60 seconds");
    }

    /**
     * Periodic sync task — runs every 60,000ms (60 seconds).
     * Uses fixedDelay to ensure the next execution starts 60s AFTER
     * the previous one finishes (same pattern as HeartbeatMonitor).
     */
    @Scheduled(fixedDelay = 60000)
    public void startPeriodicSync() {
        performSync();
    }

    /**
     * Performs a single NTP synchronization:
     * 1. Opens a UDP connection to the NTP server
     * 2. Sends a time request and receives server timestamp
     * 3. Calculates offset accounting for network round-trip delay
     * 4. Updates the volatile offsetMillis field
     *
     * If the NTP server is unreachable, logs a warning and keeps
     * the last known offset (graceful degradation).
     */
    void performSync() {
        try {
            long newOffset = calculateOffsetMillis();
            updateOffset(newOffset);
            synced = true;
            log.info("[NTP] Offset updated: {} ms (local clock is {} ms {} NTP time)",
                    newOffset,
                    Math.abs(newOffset),
                    newOffset >= 0 ? "behind" : "ahead of");
        } catch (Exception e) {
            log.warn("[NTP] Failed to sync with {} — keeping last offset ({} ms). Reason: {}",
                    NTP_SERVER, offsetMillis, e.getMessage());
        }
    }

    /**
     * Queries the NTP server and calculates the offset between local
     * and server time, accounting for network round-trip delay.
     *
     * Formula from PDF (Page 9):
     *   offsetMillis = (server_time - local_time) - (network_round_trip / 2)
     *
     * Apache Commons Net handles this calculation internally via
     * TimeInfo.computeDetails(), which computes the offset using
     * the standard NTP algorithm:
     *   offset = ((T2 - T1) + (T3 - T4)) / 2
     * where:
     *   T1 = client send time
     *   T2 = server receive time
     *   T3 = server transmit time
     *   T4 = client receive time
     *
     * @return offset in milliseconds (positive = local clock is behind NTP)
     * @throws Exception if the NTP server is unreachable
     */
    long calculateOffsetMillis() throws Exception {
        NTPUDPClient client = new NTPUDPClient();
        client.setDefaultTimeout(NTP_TIMEOUT_MS);
        try {
            client.open();
            InetAddress serverAddress = InetAddress.getByName(NTP_SERVER);
            TimeInfo timeInfo = client.getTime(serverAddress);

            // computeDetails() calculates offset and delay using
            // standard NTP stratum algorithm
            timeInfo.computeDetails();

            Long offset = timeInfo.getOffset();
            if (offset == null) {
                throw new RuntimeException("NTP response did not contain offset");
            }

            log.debug("[NTP] Raw NTP response — offset: {} ms, delay: {} ms",
                    offset, timeInfo.getDelay());

            return offset;
        } finally {
            client.close();
        }
    }

    /**
     * Atomically updates the volatile offset field.
     * Called after a successful NTP query.
     *
     * @param newOffset the new offset value in milliseconds
     */
    void updateOffset(long newOffset) {
        long previousOffset = this.offsetMillis;
        this.offsetMillis = newOffset;

        if (previousOffset != newOffset) {
            log.debug("[NTP] Offset changed from {} ms to {} ms (delta: {} ms)",
                    previousOffset, newOffset, newOffset - previousOffset);
        }
    }

    /**
     * Returns the current NTP offset in milliseconds.
     * Thread-safe: reads the volatile field directly.
     * Positive value means local clock is behind NTP authoritative time.
     *
     * @return current offset in milliseconds
     */
    public long getOffsetMillis() {
        return offsetMillis;
    }

    /**
     * Returns whether at least one successful NTP sync has occurred.
     *
     * @return true if synced at least once
     */
    public boolean isSynced() {
        return synced;
    }
}
