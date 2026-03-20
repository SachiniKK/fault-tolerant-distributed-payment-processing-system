package com.payment.replication.ledger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages the local list of LedgerEntry objects.
 * Thread-safe via ConcurrentHashMap — handles concurrent
 * reads and writes from replication and HTTP threads.
 *
 * In production this would be backed by H2 or a file-based
 * log for persistence across node restarts.
 */
@Service
public class LedgerStore {
    private static final Logger log = LoggerFactory.getLogger(LedgerStore.class);

    // Key: transactionId -> LedgerEntry
    private final ConcurrentHashMap<String, LedgerEntry> store = new ConcurrentHashMap<>();

    public void save(LedgerEntry entry) {
        store.put(entry.getTransactionId(), entry);
        log.debug("[LEDGER] Saved entry: {} status={}", entry.getTransactionId(), entry.getStatus());
    }

    public LedgerEntry get(String transactionId) {
        return store.get(transactionId);
    }

    public boolean exists(String transactionId) {
        return store.containsKey(transactionId);
    }

    public Collection<LedgerEntry> getAll() {
        return Collections.unmodifiableCollection(store.values());
    }

    public int size() {
        return store.size();
    }

    /**
     * Returns all entries created after sinceTimestamp.
     * Used by GET /internal/ledger-sync for node recovery.
     */
    public List<LedgerEntry> getEntriesSince(long sinceTimestamp) {
        return store.values().stream()
                .filter(e -> e.getCreatedTimestamp() > sinceTimestamp)
                .sorted(Comparator.comparingLong(LedgerEntry::getCreatedTimestamp))
                .collect(Collectors.toList());
    }
}