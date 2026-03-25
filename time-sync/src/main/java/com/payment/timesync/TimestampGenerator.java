package com.payment.timesync;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * TimestampGenerator utility to apply NTP offset to raw system times.
 */
@Component
public class TimestampGenerator {

    @Autowired
    private NTPSyncService ntpSyncService;

    /**
     * Generates timestamps with NTP offset applied.
     * Both raw and corrected timestamps returned.
     */
    public TimestampPair generateTimestamps() {
        long raw = getRawTimestamp();
        long corrected = getCorrectedTimestamp(raw);
        return new TimestampPair(raw, corrected);
    }

    public long getRawTimestamp() {
        return System.currentTimeMillis();
    }

    public long getCorrectedTimestamp(long raw) {
        return raw + ntpSyncService.getOffsetMillis();
    }
}
