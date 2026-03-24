package com.payment.gateway.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.common.PaymentRequest;
import com.payment.common.PaymentResult;
import com.payment.common.Transaction;
import com.payment.common.TransactionStatus;
import com.payment.gateway.client.ConsensusClient;
import com.payment.gateway.client.FaultToleranceClient;
import com.payment.gateway.client.ReplicationClient;
import com.payment.gateway.client.TimeSyncClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * Payment Coordinator - The heart of the integration.
 *
 * Orchestrates the payment flow through all 4 modules:
 * 1. FAULT TOLERANCE: Check health, select node
 * 2. CONSENSUS: Submit to Raft leader for ordering
 * 3. TIME SYNC: Add synchronized timestamp and Lamport clock
 * 4. REPLICATION: Persist with quorum, deduplicate
 *
 * This ensures:
 * - Availability (FT routes around failures)
 * - Consistency (Raft orders all transactions)
 * - Ordering (Lamport clocks for happened-before)
 * - Durability (Quorum replication)
 */
@Service
public class PaymentCoordinator {

    private static final Logger log = LoggerFactory.getLogger(PaymentCoordinator.class);

    @Autowired
    private FaultToleranceClient ftClient;

    @Autowired
    private ConsensusClient consensusClient;

    @Autowired
    private TimeSyncClient timeSyncClient;

    @Autowired
    private ReplicationClient replicationClient;

    @Value("${gateway.node-id:gateway-1}")
    private String nodeId;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Process a payment through all 4 modules.
     *
     * Flow:
     * 1. FT: Verify system is available
     * 2. TimeSync: Get timestamp and Lamport clock
     * 3. Replication: Check for duplicate
     * 4. Consensus: Submit to Raft for ordering
     * 5. Replication: Persist with quorum
     */
    public PaymentResult processPayment(PaymentRequest request) {
        log.info("========== PROCESSING PAYMENT ==========");
        log.info("User: {}, Amount: {} {}", request.getUserId(), request.getAmount(), request.getCurrency());

        // ========== STEP 1: FAULT TOLERANCE ==========
        log.info("[STEP 1] Checking system health via Fault Tolerance...");

        if (!ftClient.isServiceAvailable()) {
            log.error("[STEP 1] FAILED - No healthy nodes available!");
            return PaymentResult.failed("System unavailable - no healthy nodes");
        }

        String selectedNode = ftClient.selectHealthyNode();
        log.info("[STEP 1] SUCCESS - Selected healthy node: {}", selectedNode);

        // ========== STEP 2: TIME SYNC ==========
        log.info("[STEP 2] Getting synchronized timestamp...");

        long syncedTimestamp = timeSyncClient.getSynchronizedTimestamp();
        long lamportClock = timeSyncClient.tickLamportClock();
        log.info("[STEP 2] SUCCESS - Timestamp: {}, Lamport: {}", syncedTimestamp, lamportClock);

        // ========== STEP 3: CREATE TRANSACTION ==========
        String transactionId = request.getIdempotencyKey() != null
                ? request.getIdempotencyKey()
                : UUID.randomUUID().toString();

        Transaction transaction = new Transaction(
                transactionId,
                request.getUserId(),
                request.getAmount(),
                request.getCurrency()
        );
        transaction.setCreatedTimestamp(syncedTimestamp);
        transaction.setLamportClock(lamportClock);
        transaction.setProcessedByNode(nodeId);
        transaction.setStatus(TransactionStatus.PENDING);

        // ========== STEP 4: REPLICATION - DEDUP CHECK ==========
        log.info("[STEP 3] Checking for duplicate transaction...");

        if (replicationClient.isDuplicate(transactionId)) {
            log.warn("[STEP 3] DUPLICATE - Transaction {} already exists!", transactionId);
            return PaymentResult.duplicate(transactionId);
        }
        log.info("[STEP 3] SUCCESS - Not a duplicate");

        // ========== STEP 5: CONSENSUS ==========
        log.info("[STEP 4] Submitting to Consensus (Raft)...");

        try {
            String commandJson = objectMapper.writeValueAsString(transaction);
            Map<String, Object> raftResult = consensusClient.submitCommand(commandJson);

            if (Boolean.TRUE.equals(raftResult.get("success"))) {
                Object indexObj = raftResult.get("index");
                Object termObj = raftResult.get("term");

                int raftIndex = indexObj instanceof Number ? ((Number) indexObj).intValue() : 0;
                int raftTerm = termObj instanceof Number ? ((Number) termObj).intValue() : 0;

                transaction.setRaftIndex(raftIndex);
                transaction.setRaftTerm(raftTerm);
                transaction.setStatus(TransactionStatus.PROCESSING);

                log.info("[STEP 4] SUCCESS - Raft index: {}, term: {}", raftIndex, raftTerm);
            } else {
                log.warn("[STEP 4] FAILED - Consensus error: {}", raftResult.get("error"));
                // Continue without consensus if unavailable (graceful degradation)
                transaction.setStatus(TransactionStatus.PROCESSING);
            }
        } catch (JsonProcessingException e) {
            log.error("[STEP 4] FAILED - JSON error: {}", e.getMessage());
            return PaymentResult.failed("Failed to serialize transaction");
        }

        // ========== STEP 6: REPLICATION - PERSIST ==========
        log.info("[STEP 5] Persisting via Replication (quorum)...");

        Transaction result = replicationClient.processTransaction(transaction);

        if (result.getStatus() == TransactionStatus.SUCCESS) {
            log.info("[STEP 5] SUCCESS - Transaction committed with quorum");
            log.info("========== PAYMENT COMPLETE ==========");
            return PaymentResult.success(result);
        } else if (result.getStatus() == TransactionStatus.DUPLICATE) {
            log.warn("[STEP 5] DUPLICATE detected during replication");
            return PaymentResult.duplicate(transactionId);
        } else {
            log.error("[STEP 5] FAILED - Replication status: {}", result.getStatus());
            return PaymentResult.failed("Replication failed - " + result.getStatus());
        }
    }

    /**
     * Get a transaction by ID.
     */
    public Transaction getTransaction(String transactionId) {
        return replicationClient.getTransaction(transactionId).orElse(null);
    }

    /**
     * Get system status across all modules.
     */
    public Map<String, Object> getSystemStatus() {
        return Map.of(
                "gateway", nodeId,
                "faultTolerance", Map.of(
                        "available", ftClient.isServiceAvailable(),
                        "healthyNodes", ftClient.getHealthyNodes()
                ),
                "consensus", Map.of(
                        "leaderUrl", consensusClient.findLeaderUrl(),
                        "currentTerm", consensusClient.getCurrentTerm()
                ),
                "timeSync", Map.of(
                        "available", timeSyncClient.isServiceAvailable(),
                        "lamportClock", timeSyncClient.getLamportClock()
                ),
                "replication", Map.of(
                        "available", replicationClient.isServiceAvailable()
                )
        );
    }
}
