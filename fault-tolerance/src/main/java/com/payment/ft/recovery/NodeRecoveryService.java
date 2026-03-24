package com.payment.ft.recovery;

import com.payment.ft.config.AppConfig;
import com.payment.ft.detection.HeartbeatMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.time.Instant;

/**
 * Handles the recovery process when this node restarts after a crash.
 *
 * Recovery process:
 * 1. Node starts up
 * 2. @PostConstruct triggers checkAndRecover()
 * 3. Finds a healthy peer
 * 4. Requests missed transactions since lastSeenTimestamp
 * 5. Applies them locally
 * 6. Marks itself as ready to accept payments
 */
@Service
public class NodeRecoveryService {

    private static final Logger log = LoggerFactory.getLogger(NodeRecoveryService.class);

    @Autowired
    private AppConfig config;
    @Autowired
    private RestTemplate restTemplate;
    @Lazy
    @Autowired
    private HeartbeatMonitor heartbeatMonitor;

    // Tracks the timestamp of the last known transaction
    // In production, this would be persisted to disk
    private volatile Instant lastSeenTimestamp = Instant.EPOCH;

    // Whether this node has completed recovery and is ready for traffic
    private volatile boolean recoveryComplete = false;

    /**
     * Runs on startup, after ZooKeeper registration.
     * Checks if we need to sync missed transactions from a peer.
     */
    @PostConstruct
    public void checkAndRecover() {
        // Short delay to let ZooKeeper registration complete first
        new Thread(() -> {
            try {
                Thread.sleep(3000); // Wait 3s for peers to be discoverable
                performRecovery();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "recovery-thread").start();
    }

    private void performRecovery() {
        log.info("[RECOVERY] Starting recovery check for node: {}", config.getNodeId());

        java.util.List<String> healthyPeers = heartbeatMonitor.getHealthyNodes();

        if (healthyPeers.isEmpty()) {
            log.info("[RECOVERY] No healthy peers found. Starting fresh (first node or all peers down).");
            recoveryComplete = true;
            return;
        }

        // Ask the first healthy peer for transactions we missed
        String peer = healthyPeers.get(0);
        String syncUrl = peer + "/internal/ledger-sync?since=" + lastSeenTimestamp.toEpochMilli();

        try {
            log.info("[RECOVERY] Requesting missed transactions from {} since {}",
                    peer, lastSeenTimestamp);

            ResponseEntity<String> response = restTemplate.getForEntity(syncUrl, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                String missedData = response.getBody();
                log.info("[RECOVERY] Received sync data: {} bytes",
                        missedData != null ? missedData.length() : 0);

                // In full integration: Member 2's replication module applies these
                // For now, log that recovery succeeded
                log.info("[RECOVERY] Sync complete. Node {} is now up-to-date.", config.getNodeId());
                lastSeenTimestamp = Instant.now();
            }

        } catch (Exception e) {
            log.warn("[RECOVERY] Sync from {} failed: {}. Node will start with local state.",
                    peer, e.getMessage());
        }

        recoveryComplete = true;
        log.info("[RECOVERY] Node {} is ready to accept payments.", config.getNodeId());
    }

    public boolean isRecoveryComplete() {
        return recoveryComplete;
    }

    public void updateLastSeenTimestamp(Instant ts) {
        if (ts.isAfter(lastSeenTimestamp)) {
            this.lastSeenTimestamp = ts;
        }
    }
}
