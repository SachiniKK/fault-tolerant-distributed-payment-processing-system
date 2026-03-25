package com.payment.common.interfaces;

import com.payment.common.Transaction;

import java.util.List;
import java.util.Optional;

/**
 * Interface for Data Replication module (Member 2).
 *
 * Responsibilities:
 * - Quorum-based replication (W=2, R=2, N=3)
 * - Deduplication using transaction IDs
 * - Transaction persistence and retrieval
 * - Sync for recovery
 */
public interface ReplicationService {

    /**
     * Process and replicate a transaction.
     * Saves locally and replicates to peers with quorum.
     *
     * @param transaction The transaction to process
     * @return The transaction with updated status
     */
    Transaction processTransaction(Transaction transaction);

    /**
     * Check if a transaction ID has already been processed.
     * Used for deduplication.
     */
    boolean isDuplicate(String transactionId);

    /**
     * Get a transaction by its ID.
     */
    Optional<Transaction> getTransaction(String transactionId);

    /**
     * Get all transactions for a specific user.
     */
    List<Transaction> getTransactionsByUser(String userId);

    /**
     * Get transactions created after the given timestamp.
     * Used by recovering nodes to catch up.
     */
    List<Transaction> getTransactionsSince(long sinceTimestamp);

    /**
     * Handle a replicated transaction from a peer.
     * Called when THIS node is receiving a replication request.
     */
    Transaction handleReplicatedTransaction(Transaction transaction);
}
