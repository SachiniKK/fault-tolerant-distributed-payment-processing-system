package com.payment.timesync;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class NTPSyncServiceTest {

    private NTPSyncService ntpSyncService;

    @BeforeEach
    void setUp() {
        ntpSyncService = new NTPSyncService();
    }

    @Test
    void testOffsetCalculationWithKnownValues() throws Exception {
        // Since NTPSyncService uses pool.ntp.org, we simulate offset updates
        long manualOffset = 450;
        ntpSyncService.updateOffset(manualOffset);
        assertEquals(450, ntpSyncService.getOffsetMillis());
    }

    @Test
    void testVolatileVisibility() throws InterruptedException {
        // Test that offset updates are visible across threads
        Thread t1 = new Thread(() -> {
            ntpSyncService.updateOffset(100);
        });
        t1.start();
        t1.join();
        assertEquals(100, ntpSyncService.getOffsetMillis());
    }
}
