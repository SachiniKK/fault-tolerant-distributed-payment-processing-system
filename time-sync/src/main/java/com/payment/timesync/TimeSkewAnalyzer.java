package com.payment.timesync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * TimeSkewAnalyzer provides logging and metrics for demonstrating 
 * the impact of clock skew and the effectiveness of NTP correction.
 */
@Component
public class TimeSkewAnalyzer {
    private static final Logger log = LoggerFactory.getLogger(TimeSkewAnalyzer.class);

    /**
     * Logs the raw vs corrected timestamps for a payment.
     * In a real integration, this would take a Payment entity.
     */
    public void logTimestampComparison(long raw, long corrected) {
        long delta = corrected - raw;
        log.info("[ANALYZER] Payment Timing - Raw: {}, Corrected: {}, Drift: {} ms",
                raw, corrected, delta);
        
        if (Math.abs(delta) > 10) {
            log.warn("[ANALYZER] Significant drift detected ({} ms)! NTP correction is active.", delta);
        }
    }

    /**
     * Placeholder for reordering analysis.
     */
    public void analyzeOrderingCorrectness(List<BufferedLogEntry> entries) {
        log.info("[ANALYZER] Analyzing {} entries in LogBuffer...", entries.size());
        // In a demo, we would compare if raw-time ordering differs from corrected-time ordering
    }
}
