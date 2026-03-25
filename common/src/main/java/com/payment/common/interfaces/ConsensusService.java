package com.payment.common.interfaces;

import java.util.Map;

/**
 * Interface for Consensus module (Member 4).
 *
 * Responsibilities:
 * - Raft leader election
 * - Log replication across nodes
 * - Command submission and commit
 */
public interface ConsensusService {

    /**
     * Check if this node is the current Raft leader.
     */
    boolean isLeader();

    /**
     * Get the current leader's node ID.
     * Returns null if no leader elected yet.
     */
    String getLeaderId();

    /**
     * Get the current Raft term.
     */
    int getCurrentTerm();

    /**
     * Submit a command (payment) to the Raft log.
     * Only succeeds if this node is the leader.
     *
     * @param command The command to append to the log
     * @return Result containing success, index, term, and any error
     */
    Map<String, Object> submitCommand(String command);

    /**
     * Check if a log entry at the given index has been committed.
     */
    boolean isCommitted(int logIndex);
}
