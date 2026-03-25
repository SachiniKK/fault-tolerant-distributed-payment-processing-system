package com.payment.replication.service;

import com.payment.common.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory transaction storage using ConcurrentHashMap.
 * Thread-safe: supports concurrent reads and writes from
 * replication threads and HTTP request threads simultaneously.
 */
@Service
public class TransactionLedger {
    private static final Logger log = LoggerFactory.getLogger(TransactionLedger.class);

    private final ConcurrentHashMap<String, Transaction> ledger = new ConcurrentHashMap<>();

    public void save(Transaction tx) {
        ledger.put(tx.getTransactionId(), tx);
        log.debug("[LEDGER] Saved: {}", tx.getTransactionId());
    }

    public Transaction get(String transactionId) {
        return ledger.get(transactionId);
    }

    public boolean exists(String transactionId) {
        return ledger.containsKey(transactionId);
    }

    public Collection<Transaction> getAll() {
        return Collections.unmodifiableCollection(ledger.values());
    }

    public int size() {
        return ledger.size();
    }

    /**
     * Returns all transactions created after the given timestamp.
     * Used by recovery/sync endpoint so recovering nodes can
     * catch up on missed transactions.
     */
    public List<Transaction> getTransactionsSince(long sinceTimestamp) {
        return ledger.values().stream()
                .filter(tx -> tx.getCreatedTimestamp() > sinceTimestamp)
                .sorted(Comparator.comparingLong(Transaction::getCreatedTimestamp))
                .collect(Collectors.toList());
    }
}
