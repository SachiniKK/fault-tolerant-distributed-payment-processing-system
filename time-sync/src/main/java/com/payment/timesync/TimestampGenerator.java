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

    @Autowired
    private ClockSkewSimulator clockSkewSimulator;

    /**
     * Generates timestamps with intentional skew and NTP compensation.
     * Raw timestamp includes simulated drift; Corrected timestamp has NTP offset applied.
     */
    public TimestampPair generateTimestamps() {
        long raw = System.currentTimeMillis();
        long withSkew = clockSkewSimulator.applySkew(raw);  // Apply intentional drift
        long corrected = withSkew + ntpSyncService.getOffsetMillis(); // Apply NTP correction
        
        return new TimestampPair(withSkew, corrected);
    }

    public long getRawTimestamp() {
        return System.currentTimeMillis();
    }

    public long getCorrectedTimestamp(long raw) {
        return raw + ntpSyncService.getOffsetMillis();
    }
}
