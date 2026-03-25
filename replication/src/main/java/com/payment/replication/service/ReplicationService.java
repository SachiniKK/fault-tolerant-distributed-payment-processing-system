package com.payment.replication.replication;

import com.payment.common.Transaction;
import com.payment.common.TransactionStatus;
import com.payment.replication.config.ReplicationConfig;
import com.payment.replication.service.DeduplicationService;
import com.payment.replication.service.TransactionLedger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * ReplicationManager — Primary node logic for replicating to backups.
 *
 * Implements the PDF's 6-step replication protocol:
 * 1. Receive  — POST /payment arrives
 * 2. Pre-log  — save as PENDING locally
 * 3. Replicate — send to all backups in PARALLEL via CompletableFuture
 * 4. Wait     — wait for quorum (majority) of ACKs
 * 5. Commit   — mark COMMITTED locally and tell backups to commit
 * 6. Reply    — respond to client
 *
 * Also handles:
 * - Idempotency check (return cached result for duplicate keys)
 * - Recovery sync via getEntriesSince()
 */
@Service
public class ReplicationManager {
    private static final Logger log = LoggerFactory.getLogger(ReplicationManager.class);

    @Autowired private ReplicationConfig config;
    @Autowired private TransactionLedger ledger;
    @Autowired private DeduplicationService dedup;
    @Autowired private RestTemplate restTemplate;

    /**
     * Main entry point. Processes a payment with idempotency check
     * and quorum-based replication.
     */
    public Transaction processTransaction(Transaction transaction) {

        // Deduplication check
        if (dedup.isDuplicate(transaction.getTransactionId())) {
            transaction.setStatus(TransactionStatus.DUPLICATE);
            return transaction;
        }

        // STEP 2: PRE-LOG — save as PENDING locally before replicating
        transaction.setStatus(TransactionStatus.PROCESSING);
        transaction.setProcessedByNode(config.getNodeId());
        ledger.save(transaction);
        log.info("[REPL] Pre-logged PENDING: {}", transaction.getTransactionId());

        // Step 3: Replicate to all peers IN PARALLEL using CompletableFuture
        List<String> peers = config.getPeerUrls();
        List<CompletableFuture<Boolean>> futures = peers.stream()
                .map(peerUrl -> CompletableFuture.supplyAsync(() -> replicateToPeer(peerUrl, transaction)))
                .collect(Collectors.toList());

        // Step 4: Wait for responses and count acks
        int acks = 1;
        for (CompletableFuture<Boolean> future : futures) {
            try {
                if (future.get(5, TimeUnit.SECONDS)) {
                    acks++;
                }
            } catch (Exception e) {
                log.warn("[REPL] Peer replication timed out or failed: {}", e.getMessage());
            }
        }

        // STEP 5: COMMIT
        if (acks >= config.getWriteQuorum()) {
            // Quorum reached — mark SUCCESS locally
            transaction.setStatus(TransactionStatus.SUCCESS);
            ledger.save(transaction);

            // Broadcast commit signal to all backup peers (fire-and-forget)
            notifyPeersToCommit(transaction.getTransactionId());

            log.info("[REPL] Phase 2: COMMITTED {} with {}/{} acks (quorum={})",
                    transaction.getTransactionId(), acks, peers.size() + 1, config.getWriteQuorum());
        } else {
            transaction.setStatus(TransactionStatus.FAILED);
            ledger.save(transaction);
            log.warn("[REPL] Phase 2: FAILED {} — only {}/{} acks (need quorum={})",
                    transaction.getTransactionId(), acks, peers.size() + 1, config.getWriteQuorum());
        }

        return transaction;
    }

    /**
     * Handles an incoming replicated transaction from a peer (Phase 1).
     * Saves as PROCESSING — will be upgraded to SUCCESS when commit signal arrives.
     */
    public Transaction handleReplicatedTransaction(Transaction tx) {
        if (dedup.isDuplicate(tx.getTransactionId())) {
            tx.setStatus(TransactionStatus.DUPLICATE);
            return tx;
        }
        tx.setStatus(TransactionStatus.PROCESSING);
        ledger.save(tx);
        log.debug("[REPL] Phase 1: Stored PROCESSING from primary: {}", tx.getTransactionId());
        return tx;
    }

    /**
     * Handles a commit signal from the primary (Phase 2).
     * Upgrades the PROCESSING entry to SUCCESS on this backup node.
     */
    public void commitTransaction(String transactionId) {
        Transaction tx = ledger.get(transactionId);
        if (tx != null) {
            tx.setStatus(TransactionStatus.SUCCESS);
            ledger.save(tx);
            log.debug("[REPL] Phase 2: Committed on backup: {}", transactionId);
        } else {
            log.warn("[REPL] Commit received for unknown transaction: {}", transactionId);
        }
    }

    /**
     * Sends a transaction to a single peer for replication (Phase 1).
     *
     * @return true if the peer acknowledged successfully
     */
    private boolean replicateToPeer(String peerUrl, Transaction tx) {
        try {
            String url = peerUrl + "/internal/replicate";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Transaction> entity = new HttpEntity<>(tx, headers);

            ResponseEntity<Transaction> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, Transaction.class);

            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.warn("[REPL] Replication to {} failed: {}", peerUrl, e.getMessage());
            return false;
        }
    }

    /**
     * Notifies all backup peers to mark a transaction as SUCCESS (Phase 2).
     * Fire-and-forget — if a peer is down, it will catch up via recovery sync.
     */
    private void notifyPeersToCommit(String transactionId) {
        for (String peerUrl : config.getPeerUrls()) {
            CompletableFuture.runAsync(() -> {
                try {
                    String url = peerUrl + "/internal/commit/" + transactionId;
                    restTemplate.put(url, null);
                } catch (Exception e) {
                    log.warn("[REPL] Commit notification to {} failed: {}", peerUrl, e.getMessage());
                }
            });
        }
    }
}