package com.payment.ft.controller;

import com.payment.ft.config.AppConfig;
import com.payment.ft.detection.HeartbeatMonitor;
import com.payment.ft.detection.NodeStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.payment.ft.failover.FailoverRouter;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.util.Map;

/**
 * REST endpoints exposed by the Fault Tolerance module.
 * /health — peer nodes call this to check if this node is alive
 * /fault/status — shows the health of all known peers (useful for demo)
 */
@RestController
public class FaultToleranceController {

    @Autowired
    private AppConfig config;
    @Autowired
    private HeartbeatMonitor heartbeatMonitor;
    @Autowired
    private FailoverRouter failoverRouter;

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
     * Client-facing payment endpoint.
     * Receives a payment request and routes it to a healthy node.
     * If the targeted node goes down, routes to the next healthy one.
     */
    @PostMapping(value = "/fault/payment", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> routePayment(@RequestBody String payload) {
        return failoverRouter.route("/payment", payload);
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
}
