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
 * NTPSyncService fetches authoritative time from pool.ntp.org every 60 seconds.
 * Calculates clock offset accounting for network round-trip delay.
 * 
 * From PDF (Page 9):
 * "volatile long offsetMillis; // Must use volatile keyword"
 * "offsetMillis = (server_time - local_time) - (network_round_trip / 2)"
 */
@Service
public class NTPSyncService {
    private static final Logger log = LoggerFactory.getLogger(NTPSyncService.class);
    private static final String NTP_SERVER = "pool.ntp.org";
    private static final int TIMEOUT_MS = 5000;

    // Must use volatile keyword for thread-safe updates visible across threads
    private volatile long offsetMillis = 0;

    @PostConstruct
    public void init() {
        log.info("[NTP] Initializing NTPSyncService. Initial sync with {}...", NTP_SERVER);
        performSync();
    }

    /**
     * Periodic sync task — runs every 60 seconds.
     */
    @Scheduled(fixedDelay = 60000)
    public void startPeriodicSync() {
        performSync();
    }

    /**
     * Performs a single NTP synchronization and updates the offset.
     */
    private void performSync() {
        try {
            long newOffset = calculateOffsetMillis();
            updateOffset(newOffset);
            log.info("[NTP] Sync successful. Offset: {} ms", newOffset);
        } catch (Exception e) {
            log.warn("[NTP] Sync failed, maintaining last known offset ({} ms). Reason: {}", 
                offsetMillis, e.getMessage());
        }
    }

    /**
     * Queries NTP server and calculates offset.
     * Formula: offsetMillis = ((T2 - T1) + (T3 - T4)) / 2
     * which simplifies to (server_time - local_time) - (round_trip / 2)
     */
    public long calculateOffsetMillis() throws Exception {
        NTPUDPClient client = new NTPUDPClient();
        client.setDefaultTimeout(TIMEOUT_MS);
        try {
            client.open();
            InetAddress address = InetAddress.getByName(NTP_SERVER);
            TimeInfo info = client.getTime(address);
            info.computeDetails();
            
            Long offset = info.getOffset();
            if (offset == null) {
                throw new RuntimeException("No offset returned from NTP server");
            }
            return offset;
        } finally {
            client.close();
        }
    }

    /**
     * Atomically updates volatile field.
     */
    public void updateOffset(long newOffset) {
        this.offsetMillis = newOffset;
    }

    public long getOffsetMillis() {
        return offsetMillis;
    }
}
