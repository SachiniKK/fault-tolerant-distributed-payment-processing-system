package com.payment.gateway.client;

import com.payment.gateway.config.ModuleConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Client for Time Synchronization module (Member 3).
 * Calls TimeSync nodes to get synchronized timestamps and Lamport clocks.
 */
@Service
public class TimeSyncClient {

    private static final Logger log = LoggerFactory.getLogger(TimeSyncClient.class);

    @Autowired
    private ModuleConfig config;

    @Autowired
    private RestTemplate restTemplate;

    /**
     * Get synchronized timestamp from TimeSync module.
     * Falls back to System.currentTimeMillis() if unavailable.
     */
    @SuppressWarnings("unchecked")
    public long getSynchronizedTimestamp() {
        for (String syncUrl : config.getTimeSync().getUrls()) {
            try {
                ResponseEntity<Map> response = restTemplate.getForEntity(
                        syncUrl + "/sync/time", Map.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    Object timestamp = response.getBody().get("correctedTime");
                    if (timestamp instanceof Number) {
                        log.debug("[TIMESYNC-CLIENT] Got synchronized time from {}: {}", syncUrl, timestamp);
                        return ((Number) timestamp).longValue();
                    }
                }
            } catch (Exception e) {
                log.warn("[TIMESYNC-CLIENT] Failed to contact TimeSync node {}: {}", syncUrl, e.getMessage());
            }
        }

        // Fallback to local time
        log.warn("[TIMESYNC-CLIENT] No TimeSync nodes available, using local time");
        return System.currentTimeMillis();
    }

    /**
     * Get and increment Lamport clock from TimeSync module.
     */
    @SuppressWarnings("unchecked")
    public long tickLamportClock() {
        for (String syncUrl : config.getTimeSync().getUrls()) {
            try {
                ResponseEntity<Map> response = restTemplate.postForEntity(
                        syncUrl + "/sync/tick", null, Map.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    Object lamport = response.getBody().get("lamportClock");
                    if (lamport instanceof Number) {
                        log.debug("[TIMESYNC-CLIENT] Lamport clock tick: {}", lamport);
                        return ((Number) lamport).longValue();
                    }
                }
            } catch (Exception e) {
                log.warn("[TIMESYNC-CLIENT] Failed to tick Lamport clock: {}", e.getMessage());
            }
        }

        log.warn("[TIMESYNC-CLIENT] No TimeSync nodes available for Lamport clock");
        return System.nanoTime(); // Fallback
    }

    /**
     * Get current Lamport clock value without incrementing.
     */
    @SuppressWarnings("unchecked")
    public long getLamportClock() {
        for (String syncUrl : config.getTimeSync().getUrls()) {
            try {
                ResponseEntity<Map> response = restTemplate.getForEntity(
                        syncUrl + "/sync/lamport", Map.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    Object lamport = response.getBody().get("lamportClock");
                    if (lamport instanceof Number) {
                        return ((Number) lamport).longValue();
                    }
                }
            } catch (Exception e) {
                // Try next node
            }
        }
        return 0;
    }

    /**
     * Update Lamport clock with received timestamp.
     */
    @SuppressWarnings("unchecked")
    public long updateLamportClock(long receivedTimestamp) {
        for (String syncUrl : config.getTimeSync().getUrls()) {
            try {
                ResponseEntity<Map> response = restTemplate.postForEntity(
                        syncUrl + "/sync/update?received=" + receivedTimestamp, null, Map.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    Object lamport = response.getBody().get("lamportClock");
                    if (lamport instanceof Number) {
                        return ((Number) lamport).longValue();
                    }
                }
            } catch (Exception e) {
                log.warn("[TIMESYNC-CLIENT] Failed to update Lamport clock: {}", e.getMessage());
            }
        }
        return receivedTimestamp + 1;
    }

    /**
     * Check if TimeSync service is available.
     */
    public boolean isServiceAvailable() {
        for (String syncUrl : config.getTimeSync().getUrls()) {
            try {
                restTemplate.getForEntity(syncUrl + "/sync/status", Map.class);
                return true;
            } catch (Exception e) {
                // Try next node
            }
        }
        return false;
    }
}
