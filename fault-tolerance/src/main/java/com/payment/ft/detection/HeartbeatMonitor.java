package com.payment.ft.detection;

import com.payment.ft.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HeartbeatMonitor runs a background check every 2 seconds.
 * It sends an HTTP GET to /health on each peer node.
 * If a node misses 3 consecutive beats, it is marked DOWN.
 *
 * Lecture 4 reference: Heartbeat Mechanism — simplest fault detection method.
 */
@Component
public class HeartbeatMonitor {

    private static final Logger log = LoggerFactory.getLogger(HeartbeatMonitor.class);

    private final AppConfig config;
    private final RestTemplate restTemplate;

    // Thread-safe map: peerUrl -> current NodeStatus
    // ConcurrentHashMap because @Scheduled runs on a different thread
    // from HTTP request handler threads
    private final Map<String, NodeStatus> statusMap = new ConcurrentHashMap<>();

    // Tracks consecutive missed heartbeats per peer
    private final Map<String, Integer> missedCounts = new ConcurrentHashMap<>();

    @Autowired
    public HeartbeatMonitor(AppConfig config, RestTemplate restTemplate) {
        this.config = config;
        this.restTemplate = restTemplate;

        // Initialize all peers as UNKNOWN before first check
        config.getPeerUrls().forEach(url -> {
            statusMap.put(url, NodeStatus.UNKNOWN);
            missedCounts.put(url, 0);
        });

        log.info("HeartbeatMonitor initialized. Watching {} peers: {}",
                config.getPeerUrls().size(), config.getPeerUrls());
    }

    /**
     * Runs every 2000ms (2 seconds) on a Spring-managed thread pool.
     * Checks all configured peer nodes.
     * fixedDelay means the next execution starts 2s AFTER the previous one
     * finishes.
     */
    @Scheduled(fixedDelayString = "${app.heartbeat.interval-ms:2000}")
    public void checkAllPeers() {
        List<String> peers = config.getPeerUrls();
        for (String peerUrl : peers) {
            checkPeer(peerUrl);
        }
    }

    /**
     * Sends a single heartbeat to one peer.
     * Updates status based on response or exception.
     */
    private void checkPeer(String peerUrl) {
        String healthUrl = peerUrl + "/health";
        try {
            // This will throw an exception if the node is unreachable
            restTemplate.getForObject(healthUrl, String.class);

            // Success: reset miss count, mark as UP
            missedCounts.put(peerUrl, 0);
            NodeStatus previous = statusMap.put(peerUrl, NodeStatus.UP);

            // Only log the status change, not every successful ping
            if (previous != NodeStatus.UP) {
                log.info("[HEARTBEAT] Node {} is now UP", peerUrl);
            }

        } catch (Exception e) {
            // Failed: increment miss counter
            int missed = missedCounts.merge(peerUrl, 1, (a, b) -> a + b);
            log.warn("[HEARTBEAT] Node {} missed beat #{} — reason: {}",
                    peerUrl, missed, e.getMessage());

            // Only mark DOWN after threshold consecutive misses
            if (missed >= config.getMissedThreshold()) {
                NodeStatus previous = statusMap.put(peerUrl, NodeStatus.DOWN);
                if (previous != NodeStatus.DOWN) {
                    log.error("[HEARTBEAT] Node {} marked DOWN after {} missed beats!",
                            peerUrl, missed);
                }
            }
        }
    }

    /**
     * Returns the current status of a peer node.
     * Used by FailoverRouter to decide where to send payments.
     */
    public NodeStatus getStatus(String peerUrl) {
        return statusMap.getOrDefault(peerUrl, NodeStatus.UNKNOWN);
    }

    /**
     * Returns all UP nodes — used by FailoverRouter to pick a target.
     */
    public List<String> getHealthyNodes() {
        return statusMap.entrySet().stream()
                .filter(e -> e.getValue() == NodeStatus.UP)
                .map(Map.Entry::getKey)
                .toList();
    }

    /**
     * Returns ALL node statuses — used by the /fault/status endpoint.
     */
    public Map<String, NodeStatus> getAllStatuses() {
        return Map.copyOf(statusMap);
    }

    /**
     * Manually forces a node back to UNKNOWN.
     * Called by NodeRecoveryService when a node rejoins.
     */
    public void resetNode(String peerUrl) {
        missedCounts.put(peerUrl, 0);
        statusMap.put(peerUrl, NodeStatus.UNKNOWN);
        log.info("[HEARTBEAT] Node {} reset to UNKNOWN (rejoining)", peerUrl);
    }
}
