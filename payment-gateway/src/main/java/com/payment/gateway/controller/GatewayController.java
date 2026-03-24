package com.payment.gateway.controller;

import com.payment.common.PaymentRequest;
import com.payment.common.PaymentResult;
import com.payment.common.Transaction;
import com.payment.gateway.service.PaymentCoordinator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Gateway REST Controller.
 *
 * This is the single entry point for all payment operations.
 * It coordinates all 4 modules (FT, Consensus, TimeSync, Replication).
 */
@RestController
@RequestMapping("/api")
public class GatewayController {

    private static final Logger log = LoggerFactory.getLogger(GatewayController.class);

    @Autowired
    private PaymentCoordinator coordinator;

    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "payment-gateway"
        ));
    }

    /**
     * Submit a new payment.
     *
     * This goes through:
     * 1. FaultTolerance - route to healthy node
     * 2. Consensus - order via Raft
     * 3. TimeSync - add synchronized timestamp
     * 4. Replication - persist with quorum
     */
    @PostMapping("/payment")
    public ResponseEntity<PaymentResult> submitPayment(@RequestBody PaymentRequest request) {
        log.info("Received payment request: userId={}, amount={} {}",
                request.getUserId(), request.getAmount(), request.getCurrency());

        PaymentResult result = coordinator.processPayment(request);

        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * Get a specific transaction by ID.
     */
    @GetMapping("/payment/{transactionId}")
    public ResponseEntity<?> getPayment(@PathVariable String transactionId) {
        Transaction tx = coordinator.getTransaction(transactionId);

        if (tx != null) {
            return ResponseEntity.ok(tx);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get overall system status across all modules.
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(coordinator.getSystemStatus());
    }

    /**
     * Demo endpoint to show integration flow.
     */
    @GetMapping("/demo/flow")
    public ResponseEntity<Map<String, Object>> demoFlow() {
        return ResponseEntity.ok(Map.of(
                "title", "Distributed Payment Processing System",
                "flow", new String[]{
                        "1. CLIENT sends payment request to GATEWAY",
                        "2. GATEWAY checks FAULT TOLERANCE for healthy nodes",
                        "3. GATEWAY submits to CONSENSUS (Raft) for ordering",
                        "4. GATEWAY gets synchronized timestamp from TIME SYNC",
                        "5. GATEWAY persists via REPLICATION with quorum",
                        "6. GATEWAY returns result to CLIENT"
                },
                "modules", Map.of(
                        "faultTolerance", "Member 1 - Health, Failover, Recovery",
                        "replication", "Member 2 - Quorum, Deduplication, Consistency",
                        "timeSync", "Member 3 - NTP, Lamport Clocks, Ordering",
                        "consensus", "Member 4 - Raft, Leader Election, Log Replication"
                )
        ));
    }
}
