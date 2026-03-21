package com.payment.timesync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Generates timestamps with NTP offset applied for every payment event.
 *
 * Always returns BOTH raw and NTP-corrected timestamps via TimestampPair.
 * This dual-timestamp approach is required by the project specification
 * so that Member 3 can demonstrate the difference between raw (possibly skewed)
 * and NTP-corrected times.
 *
 * PDF reference (Page 7):
 * "timestamp and correctedTimestamp — Both stored so Member 3 can demonstrate
 *  the difference between raw (possibly skewed) and NTP-corrected times"
 *
 * Integration points:
 * - Injected into PaymentService when saving transactions
 * - Called every time a Payment is saved or a Raft log entry is created
 * - Thread-safe: reads volatile offsetMillis from NTPSyncService
 */
@Component
public class TimestampGenerator {

    private static final Logger log = LoggerFactory.getLogger(TimestampGenerator.class);

    private final NTPSyncService ntpSyncService;

    @Autowired
    public TimestampGenerator(NTPSyncService ntpSyncService) {
        this.ntpSyncService = ntpSyncService;
        log.info("[TIMESTAMP] TimestampGenerator initialized with NTPSyncService");
    }

    /**
     * Generates a TimestampPair containing both the raw system time
     * and the NTP-corrected time.
     *
     * Flow:
     * 1. Get raw system time: System.currentTimeMillis()
     * 2. Read NTP offset (volatile, thread-safe)
     * 3. Compute corrected = raw + offset
     * 4. Return both in a TimestampPair
     *
     * @return TimestampPair with (rawTimestamp, correctedTimestamp)
     */
    public TimestampPair generateTimestamps() {
        long raw = getRawTimestamp();
        long corrected = getCorrectedTimestamp(raw);

        log.debug("[TIMESTAMP] Generated — raw: {}, corrected: {}, offset: {} ms",
                raw, corrected, ntpSyncService.getOffsetMillis());

        return new TimestampPair(raw, corrected);
    }

    /**
     * Returns the current system time in milliseconds.
     * This is the raw, un-corrected time that may be affected by clock drift.
     *
     * @return current system time in millis
     */
    public long getRawTimestamp() {
        return System.currentTimeMillis();
    }

    /**
     * Applies the NTP offset to a raw timestamp to produce a corrected timestamp.
     * The offset is read from NTPSyncService's volatile field — thread-safe.
     *
     * Formula: corrected = raw + offsetMillis
     * - If offset is positive (local clock behind NTP): corrected > raw
     * - If offset is negative (local clock ahead of NTP): corrected < raw
     *
     * @param rawTimestamp the raw system timestamp
     * @return NTP-corrected timestamp
     */
    public long getCorrectedTimestamp(long rawTimestamp) {
        return rawTimestamp + ntpSyncService.getOffsetMillis();
    }

    /**
     * Convenience method: returns a corrected timestamp using the current time.
     *
     * @return NTP-corrected current timestamp
     */
    public long getCorrectedTimestamp() {
        return getRawTimestamp() + ntpSyncService.getOffsetMillis();
    }

    /**
     * Returns the current NTP offset being applied.
     * Useful for diagnostics and the TimeSkewAnalyzer (Part 3).
     *
     * @return current NTP offset in milliseconds
     */
    public long getCurrentOffset() {
        return ntpSyncService.getOffsetMillis();
    }
}
