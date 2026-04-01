package com.payment.timesync;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * ClockSkewSimulator intentionally shifts a node's clock to simulate drift.
 */
@Component
public class ClockSkewSimulator {
    private static final Logger log = LoggerFactory.getLogger(ClockSkewSimulator.class);

    @Value("${app.clock-skew-ms:0}")
    private long skewMillis;

    @PostConstruct
    public void logSkewConfiguration() {
        if (skewMillis != 0) {
            log.warn("[SKEW] Clock skew simulation ENABLED: {} ms", skewMillis);
        } else {
            log.info("[SKEW] Clock skew simulation DISABLED (real time)");
        }
    }

    /**
     * Applies the configured skew to a raw timestamp.
     */
    public long applySkew(long rawTimestamp) {
        return rawTimestamp + skewMillis;
    }

    public long getSkewMillis() {
        return skewMillis;
    }
}
