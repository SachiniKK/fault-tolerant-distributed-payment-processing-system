package com.payment.gateway.client;

import com.payment.gateway.config.ModuleConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Client for Fault Tolerance module (Member 1).
 * Calls FT nodes via REST to check health and route payments.
 */
@Service
public class FaultToleranceClient {

    private static final Logger log = LoggerFactory.getLogger(FaultToleranceClient.class);

    @Autowired
    private ModuleConfig config;

    @Autowired
    private RestTemplate restTemplate;

    private final AtomicInteger roundRobin = new AtomicInteger(0);

    /**
     * Get list of healthy nodes from FT module.
     */
    @SuppressWarnings("unchecked")
    public List<String> getHealthyNodes() {
        List<String> healthyNodes = new ArrayList<>();

        for (String ftUrl : config.getFaultTolerance().getUrls()) {
            try {
                ResponseEntity<Map> response = restTemplate.getForEntity(
                        ftUrl + "/fault/status", Map.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    Map<String, Object> body = response.getBody();
                    Map<String, String> peerStatuses = (Map<String, String>) body.get("peerStatuses");

                    if (peerStatuses != null) {
                        peerStatuses.forEach((peer, status) -> {
                            if ("UP".equals(status) && !healthyNodes.contains(peer)) {
                                healthyNodes.add(peer);
                            }
                        });
                    }

                    // The node itself is healthy if it responded
                    if (!healthyNodes.contains(ftUrl)) {
                        healthyNodes.add(ftUrl);
                    }
                }
            } catch (Exception e) {
                log.warn("[FT-CLIENT] Failed to contact FT node {}: {}", ftUrl, e.getMessage());
            }
        }

        log.debug("[FT-CLIENT] Healthy nodes: {}", healthyNodes);
        return healthyNodes;
    }

    /**
     * Select a healthy node using round-robin.
     */
    public String selectHealthyNode() {
        List<String> healthy = getHealthyNodes();
        if (healthy.isEmpty()) {
            return null;
        }
        int idx = (roundRobin.getAndIncrement() & Integer.MAX_VALUE) % healthy.size();
        return healthy.get(idx);
    }

    /**
     * Check if any FT node is reachable.
     */
    public boolean isServiceAvailable() {
        for (String ftUrl : config.getFaultTolerance().getUrls()) {
            try {
                restTemplate.getForEntity(ftUrl + "/health", String.class);
                return true;
            } catch (Exception e) {
                // Try next node
            }
        }
        return false;
    }
}
