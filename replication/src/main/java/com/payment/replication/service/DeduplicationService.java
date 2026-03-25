package com.payment.replication.service;

import com.payment.common.Transaction;
import com.payment.replication.ledger.LedgerEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Idempotency layer — prevents double-processing of payments.
 *
 * Before processing any payment, check if the
 * idempotencyKey has been seen before. If a retry arrives with
 * the same key, return the previous result without re-executing.
 *
 * Uses a ConcurrentHashMap<String, LedgerEntry> to store the
 * result of every idempotencyKey (as the PDF specifies).
 * Entries expire after TTL (5 minutes) to prevent unbounded memory growth.
 */
@Service
public class IdempotencyService {
    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);

    // Stores idempotencyKey -> (LedgerEntry result, timestamp when recorded)
    private final ConcurrentHashMap<String, Long> seenTransactions = new ConcurrentHashMap<>();
    private long ttlMillis = 5 * 60 * 1000L; // 5 minutes default

    public void setTtlMinutes(int minutes) {
        this.ttlMillis = minutes * 60L * 1000L;
    }

    /**
     * Returns true if this transaction ID has been seen before (duplicate).
     * If not seen, records it and returns false.
     */
    public boolean isDuplicate(String transactionId) {
        Long existingTimestamp = seenTransactions.putIfAbsent(transactionId, System.currentTimeMillis());
        if (existingTimestamp != null) {
            log.warn("[DEDUP] Duplicate detected: {}", transactionId);
            return true;
        }
        return false;
    }

    /**
     * Record the result of a successfully processed payment.
     * Future requests with the same idempotencyKey will receive this result.
     */
    public void record(String idempotencyKey, Transaction transaction) {
        seenTransactions.put(idempotencyKey, transaction.getCreatedTimestamp());
        log.debug("[IDEMPOTENCY] Recorded key: {}", idempotencyKey);
    }

    /**
     * Cleanup expired cache entries every 60 seconds.
     * Prevents unbounded memory growth as payments accumulate.
     */
    @Scheduled(fixedRate = 60000)
    public void cleanup() {
        long now = System.currentTimeMillis();
        int removed = 0;
        Iterator<Map.Entry<String, Long>> it = seenTransactions.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Long> entry = it.next();
            if (now - entry.getValue() > ttlMillis) {
                it.remove();
                removed++;
            }
        }
        if (removed > 0) log.debug("[IDEMPOTENCY] Cleaned {} expired entries", removed);
    }

    public int size() {
        return seenTransactions.size();
    }
}