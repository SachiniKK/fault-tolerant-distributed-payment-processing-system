package com.payment.timesync.controller;

import com.payment.timesync.config.TimeSyncConfig;
import com.payment.timesync.model.DualTimestamp;
import com.payment.timesync.service.LamportClock;
import com.payment.timesync.service.NtpSyncService;
import com.payment.timesync.service.TimestampCorrector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.Map;

/**
 * REST endpoints for the Time Synchronization module.
 *
 * Endpoints:
 * - GET /health — Health check
 * - GET /time/status — Full time sync status
 * - GET /time/ntp-offset — Current NTP offset
 * - POST /time/event — Record a remote event and return dual timestamp
 * - GET /time/now — Get current dual timestamp
 */
@RestController
@CrossOrigin(origins = "*")
public class TimeSyncController {
    private static final Logger log = LoggerFactory.getLogger(TimeSyncController.class);

    @Autowired
    private TimeSyncConfig config;
    @Autowired
    private NtpSyncService ntpSync;
    @Autowired
    private LamportClock lamportClock;
    @Autowired
    private TimestampCorrector timestampCorrector;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "nodeId", config.getNodeId(),
                "status", "UP"));
    }

    @GetMapping("/time/status")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(Map.of(
                "nodeId", config.getNodeId(),
                "ntpOffset", ntpSync.getOffsetMs(),
                "ntpSyncSuccessful", ntpSync.isSyncSuccessful(),
                "lastNtpSync", ntpSync.getLastSyncTime(),
                "lamportClock", lamportClock.getTime(),
                "correctedTime", ntpSync.getCorrectedTime()));
    }

    @GetMapping("/time/ntp-offset")
    public ResponseEntity<Map<String, Object>> ntpOffset() {
        return ResponseEntity.ok(Map.of(
                "offsetMs", ntpSync.getOffsetMs(),
                "syncSuccessful", ntpSync.isSyncSuccessful()));
    }

    /**
     * Called when a remote event occurs (e.g., receiving a replicated transaction).
     * Updates the Lamport clock and returns a DualTimestamp for ordering.
     *
     * Body: {"remoteLamport": 42}
     */
    @PostMapping("/time/event")
    public ResponseEntity<DualTimestamp> recordEvent(@RequestBody Map<String, Long> body) {
        long remoteLamport = body.getOrDefault("remoteLamport", 0L);
        DualTimestamp ts = timestampCorrector.stampRemoteEvent(remoteLamport);
        log.info("[TIME] Recorded remote event: {}", ts);
        return ResponseEntity.ok(ts);
    }

    /**
     * Returns the current DualTimestamp (for local event stamping).
     */
    @GetMapping("/time/now")
    public ResponseEntity<DualTimestamp> now() {
        DualTimestamp ts = timestampCorrector.stampLocalEvent();
        return ResponseEntity.ok(ts);
    }
}
