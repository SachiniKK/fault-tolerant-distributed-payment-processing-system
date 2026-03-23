package com.payment.replication.replication;

import com.payment.replication.config.ReplicationConfig;
import com.payment.replication.ledger.LedgerEntry;
import com.payment.replication.ledger.LedgerStore;
import com.payment.replication.service.IdempotencyService;
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
    @Autowired private LedgerStore ledgerStore;
    @Autowired private IdempotencyService idempotencyService;
    @Autowired private RestTemplate restTemplate;

    /**
     * Main entry point. Processes a payment with idempotency check
     * and quorum-based replication.
     */
    public LedgerEntry processPayment(LedgerEntry entry) {

        // IDEMPOTENCY CHECK — return cached result if key was seen before
        LedgerEntry cached = idempotencyService.getIfDuplicate(entry.getIdempotencyKey());
        if (cached != null) {
            log.info("[REPL] Duplicate idempotencyKey={}, returning cached result", entry.getIdempotencyKey());
            return cached;
        }

        // STEP 2: PRE-LOG — save as PENDING locally before replicating
        entry.setStatus(LedgerEntry.Status.PENDING);
        entry.setProcessedByNode(config.getNodeId());
        ledgerStore.save(entry);
        log.info("[REPL] Pre-logged PENDING: {}", entry.getTransactionId());

        // STEP 3 + 4: REPLICATE IN PARALLEL and wait for quorum ACKs
        List<String> peers = config.getPeerUrls();
        List<CompletableFuture<Boolean>> futures = peers.stream()
                .map(peerUrl -> CompletableFuture.supplyAsync(() -> sendToPeer(peerUrl, entry)))
                .collect(Collectors.toList());

        // Count acks: self = 1, add each successful peer response
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

        // STEP 5: COMMIT if quorum reached, else FAIL
        if (acks >= config.getWriteQuorum()) {
            entry.setStatus(LedgerEntry.Status.COMMITTED);
            ledgerStore.save(entry); // update local status to COMMITTED
            notifyBackupsToCommit(entry); // tell backups to mark COMMITTED too
            idempotencyService.record(entry.getIdempotencyKey(), entry); // cache result
            log.info("[REPL] COMMITTED {} with {}/{} acks", entry.getTransactionId(),
                    acks, peers.size() + 1);
        } else {
            entry.setStatus(LedgerEntry.Status.FAILED);
            ledgerStore.save(entry);
            log.warn("[REPL] FAILED {} — only {}/{} acks, need quorum={}",
                    entry.getTransactionId(), acks, peers.size() + 1, config.getWriteQuorum());
        }

        return entry;
    }

    /**
     * Called when THIS node is a backup receiving a pre-log from the primary.
     * Saves as PENDING (not yet committed — waiting for commit signal).
     */
    public LedgerEntry receivePendingFromPrimary(LedgerEntry entry) {
        entry.setStatus(LedgerEntry.Status.PENDING);
        ledgerStore.save(entry);
        log.debug("[REPL] Stored PENDING from primary: {}", entry.getTransactionId());
        return entry;
    }

    /**
     * Called when THIS node is a backup receiving a commit signal from primary.
     * Updates the existing PENDING entry to COMMITTED.
     */
    public void receiveCommitFromPrimary(String transactionId) {
        LedgerEntry entry = ledgerStore.get(transactionId);
        if (entry != null) {
            entry.setStatus(LedgerEntry.Status.COMMITTED);
            ledgerStore.save(entry);
            log.debug("[REPL] Committed on backup: {}", transactionId);
        } else {
            log.warn("[REPL] Commit received for unknown transactionId: {}", transactionId);
        }
    }

    /**
     * Sends a LedgerEntry to a single peer for pre-logging.
     * Returns true if peer responds with 2xx.
     */
    private boolean sendToPeer(String peerUrl, LedgerEntry entry) {
        try {
            String url = peerUrl + "/internal/replicate";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<LedgerEntry> request = new HttpEntity<>(entry, headers);
            ResponseEntity<LedgerEntry> response = restTemplate.exchange(
                    url, HttpMethod.POST, request, LedgerEntry.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.warn("[REPL] Failed to replicate to {}: {}", peerUrl, e.getMessage());
            return false;
        }
    }

    /**
     * Notifies all backup peers to mark the transaction as COMMITTED.
     * Fire-and-forget — if a peer is down it will catch up via ledger-sync.
     */
    private void notifyBackupsToCommit(LedgerEntry entry) {
        for (String peerUrl : config.getPeerUrls()) {
            CompletableFuture.runAsync(() -> {
                try {
                    String url = peerUrl + "/internal/commit/" + entry.getTransactionId();
                    restTemplate.put(url, null);
                } catch (Exception e) {
                    log.warn("[REPL] Commit notification to {} failed: {}", peerUrl, e.getMessage());
                }
            });
        }
    }
}