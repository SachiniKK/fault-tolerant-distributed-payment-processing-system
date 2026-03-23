package com.payment.replication.replication;

import com.payment.replication.config.ReplicationConfig;
import com.payment.replication.ledger.LedgerEntry;
import com.payment.replication.ledger.LedgerStore;
import com.payment.replication.service.IdempotencyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * REST endpoints for the replication module.
 *
 * External endpoints (client-facing):
 *   POST /payment              — submit a new payment
 *   GET  /health               — heartbeat for fault-tolerance module
 *   GET  /ledger               — list all entries on this node
 *   GET  /replication/status   — quorum config and node stats
 *
 * Internal endpoints (node-to-node only):
 *   POST /internal/replicate           — primary sends PENDING entry to backup
 *   PUT  /internal/commit/{txId}       — primary signals backup to COMMIT
 *   GET  /internal/ledger-sync?since=  — recovery sync for crashed nodes
 */
@RestController
public class ReplicationController {
    private static final Logger log = LoggerFactory.getLogger(ReplicationController.class);

    @Autowired private ReplicationConfig config;
    @Autowired private ReplicationManager replicationManager;
    @Autowired private LedgerStore ledgerStore;
    @Autowired private IdempotencyService idempotencyService;

    // ─────────────────────────────────────────────
    // EXTERNAL ENDPOINTS
    // ─────────────────────────────────────────────

    /**
     * Primary entry point for clients submitting payments.
     * Expects Idempotency-Key header to enable safe retries.
     */
    @PostMapping("/payment")
    public ResponseEntity<LedgerEntry> submitPayment(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody LedgerEntry entry) {

        // If client didn't provide idempotency key, use transactionId as fallback
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            idempotencyKey = entry.getTransactionId();
        }
        entry.setIdempotencyKey(idempotencyKey);

        log.info("[API] Payment received: txId={} key={}", entry.getTransactionId(), idempotencyKey);
        LedgerEntry result = replicationManager.processPayment(entry);
        return ResponseEntity.ok(result);
    }

    /**
     * Health check — called by fault-tolerance module's HeartbeatMonitor every 2 seconds.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "nodeId", config.getNodeId(),
                "status", "UP",
                "totalEntries", ledgerStore.size()));
    }

    /**
     * Lists all ledger entries on this node.
     * Used for verification — all nodes should return the same entries after replication.
     */
    @GetMapping("/ledger")
    public ResponseEntity<Collection<LedgerEntry>> getLedger() {
        return ResponseEntity.ok(ledgerStore.getAll());
    }

    /**
     * Status dashboard — shows quorum config and node stats.
     */
    @GetMapping("/replication/status")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(Map.of(
                "nodeId", config.getNodeId(),
                "totalEntries", ledgerStore.size(),
                "writeQuorum", config.getWriteQuorum(),
                "peerCount", config.getPeerUrls().size(),
                "idempotencyCacheSize", idempotencyService.size()));
    }

    // ─────────────────────────────────────────────
    // INTERNAL ENDPOINTS (node-to-node)
    // ─────────────────────────────────────────────

    /**
     * Primary calls this to pre-log a PENDING entry on this backup node.
     * Part of Step 3 in the PDF replication protocol.
     */
    @PostMapping("/internal/replicate")
    public ResponseEntity<LedgerEntry> receiveReplicated(@RequestBody LedgerEntry entry) {
        log.debug("[API] Received PENDING from primary: {}", entry.getTransactionId());
        LedgerEntry result = replicationManager.receivePendingFromPrimary(entry);
        return ResponseEntity.ok(result);
    }

    /**
     * Primary calls this after quorum is reached to signal COMMIT on this backup.
     * Part of Step 5 in the PDF replication protocol.
     */
    @PutMapping("/internal/commit/{transactionId}")
    public ResponseEntity<Void> receiveCommit(@PathVariable("transactionId") String transactionId) {
        log.debug("[API] Received COMMIT signal for: {}", transactionId);
        replicationManager.receiveCommitFromPrimary(transactionId);
        return ResponseEntity.ok().build();
    }

    /**
     * Recovery sync endpoint.
     * Crashed node calls GET /internal/ledger-sync?since=<lastId>
     * to get all entries it missed. The 'since' parameter is a timestamp here.
     */
    @GetMapping("/internal/ledger-sync")
    public ResponseEntity<List<LedgerEntry>> ledgerSync(
            @RequestParam(value = "since", defaultValue = "0") long since) {
        List<LedgerEntry> missed = ledgerStore.getEntriesSince(since);
        log.info("[API] Ledger sync request: returning {} entries since {}", missed.size(), since);
        return ResponseEntity.ok(missed);
    }
}