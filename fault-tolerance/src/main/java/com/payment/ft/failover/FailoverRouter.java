package com.payment.ft.failover;

import com.payment.ft.config.AppConfig;
import com.payment.ft.detection.HeartbeatMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * FailoverRouter forwards payment requests to healthy peer nodes.
 * Uses round-robin among UP nodes.
 * If the target node fails mid-request, it tries the next UP node.
 *
 * Lecture 4 reference: Timeout-Based Detection + Failover Mechanisms.
 */
@Service
public class FailoverRouter {

    private static final Logger log = LoggerFactory.getLogger(FailoverRouter.class);

    @Autowired
    private AppConfig config;
    @Autowired
    private HeartbeatMonitor heartbeatMonitor;
    @Autowired
    private RestTemplate restTemplate;

    // Round-robin counter (thread-safe)
    private final AtomicInteger roundRobinIndex = new AtomicInteger(0);

    /**
     * Routes a payment request to a healthy node.
     *
     * @param path    The API path to call on the target node (e.g., "/payment")
     * @param payload The JSON payload to forward
     * @return ResponseEntity from the target node, or 503 if all nodes are DOWN
     */
    public ResponseEntity<String> route(String path, String payload) {
        List<String> healthyNodes = heartbeatMonitor.getHealthyNodes();

        if (healthyNodes.isEmpty()) {
            log.error("[FAILOVER] All nodes are DOWN. Cannot route request.");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("{\"error\": \"All payment nodes are unavailable\"}");
        }

        // Pick a node using round-robin among healthy nodes only
        int attempts = 0;
        int size = healthyNodes.size();
        while (attempts < size) {
            int idx = (roundRobinIndex.getAndIncrement() & Integer.MAX_VALUE) % size;
            String targetNode = healthyNodes.get(idx);
            String fullUrl = targetNode + path;

            try {
                log.info("[FAILOVER] Routing to: {}", fullUrl);
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<String> request = new HttpEntity<>(payload, headers);

                ResponseEntity<String> response = restTemplate.exchange(
                        fullUrl, HttpMethod.POST, request, String.class);

                log.info("[FAILOVER] Success from node: {}", targetNode);
                return response;

            } catch (Exception e) {
                log.warn("[FAILOVER] Node {} failed during routing: {}. Trying next node.",
                        targetNode, e.getMessage());
                attempts++;
            }
        }

        log.error("[FAILOVER] All healthy nodes failed during routing.");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("{\"error\": \"Payment routing failed after trying all nodes\"}");
    }

    /**
     * Returns the URL of a single healthy node (for redirects).
     * Member 4's Raft module uses this for leader discovery.
     */
    public String getHealthyNodeUrl() {
        List<String> nodes = heartbeatMonitor.getHealthyNodes();
        if (nodes.isEmpty())
            return null;
        int idx = (roundRobinIndex.getAndIncrement() & Integer.MAX_VALUE) % nodes.size();
        return nodes.get(idx);
    }
}
