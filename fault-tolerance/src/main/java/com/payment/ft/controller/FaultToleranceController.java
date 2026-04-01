package com.payment.ft.controller;

import com.payment.ft.config.AppConfig;
import com.payment.ft.detection.HeartbeatMonitor;
import com.payment.ft.detection.NodeStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.payment.ft.failover.FailoverRouter;
import com.payment.ft.recovery.NodeRecoveryService;
import org.springframework.http.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;

/**
 * REST endpoints exposed by the Fault Tolerance module.
 * /health — peer nodes call this to check if this node is alive
 * /fault/status — shows the health of all known peers (useful for demo)
 */
@RestController
@CrossOrigin
public class FaultToleranceController {

    @Autowired
    private AppConfig config;
    @Autowired
    private HeartbeatMonitor heartbeatMonitor;
    @Autowired
    private FailoverRouter failoverRouter;
    @Autowired
    private NodeRecoveryService recoveryService;

    private static final Logger log = LoggerFactory.getLogger(FaultToleranceController.class);

    /**
     * Health check endpoint.
     * Every peer calls this on a 2-second interval.
     * Returns 200 OK with node ID and current timestamp.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "nodeId", config.getNodeId(),
                "status", "UP",
                "timestamp", Instant.now().toString()));
    }

    /**
     * Dashboard endpoint showing all peer node statuses.
     * Useful for the demo video — shows which nodes are UP/DOWN.
     */
    @GetMapping("/fault/status")
    public ResponseEntity<Map<String, Object>> status() {
        Map<String, NodeStatus> statuses = heartbeatMonitor.getAllStatuses();
        return ResponseEntity.ok(Map.of(
                "thisNode", config.getNodeId(),
                "peers", statuses,
                "healthyCount", heartbeatMonitor.getHealthyNodes().size()));
    }

    /**
     * Dashboard endpoint showing all peer node statuses.
     * Useful for the demo video — shows which nodes are UP/DOWN.
     */
    @PostMapping("/fault/process-payment")
    public ResponseEntity<String> processPayment(@RequestBody String body) {
        // FOR MOCKING: Successfully process the payment
        log.info("[CONTROLLER] Successfully processed payment locally: {}", body);
        return ResponseEntity.ok("Payment processed successfully by node: " + config.getNodeId());
    }

    /**
     * Client-facing payment endpoint.
     * Receives a payment request and routes it to a healthy node.
     * If the targeted node goes down, routes to the next healthy one.
     */
    @PostMapping(value = "/fault/payment", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> routePayment(@RequestBody String payload) {
        return failoverRouter.route("/fault/process-payment", payload);
    }

    /**
     * Returns list of currently healthy nodes.
     * Used by other modules and for testing.
     */
    @GetMapping("/fault/healthy-nodes")
    public ResponseEntity<Object> healthyNodes() {
        return ResponseEntity.ok(Map.of(
                "healthyNodes", heartbeatMonitor.getHealthyNodes(),
                "count", heartbeatMonitor.getHealthyNodes().size()));
    }

    /**
     * Called by recovering nodes to get transactions they missed.
     * 'since' is a Unix epoch milliseconds timestamp.
     */
    @GetMapping("/recovery/sync")
    public ResponseEntity<Map<String, Object>> syncData(
            @RequestParam long since) {
        // In full integration, this calls Member 2's replication service
        // to get all transactions after 'since'
        log.info("[RECOVERY] Peer requested sync since epoch: {}", since);
        return ResponseEntity.ok(Map.of(
                "requestedSince", since,
                "message", "Sync data will be provided by replication module",
                "thisNode", config.getNodeId()));
    }

    /**
     * Shows whether this node has completed its recovery.
     * Used by other nodes before sending payment requests here.
     */
    @GetMapping("/fault/recovery-status")
    public ResponseEntity<Map<String, Object>> recoveryStatus() {
        return ResponseEntity.ok(Map.of(
                "nodeId", config.getNodeId(),
                "recoveryComplete", recoveryService.isRecoveryComplete(),
                "timestamp", Instant.now().toString()));
    }
}
