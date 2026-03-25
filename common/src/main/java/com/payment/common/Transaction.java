package com.payment.common;

import java.math.BigDecimal;

/**
 * Shared transaction model used by all modules.
 *
 * Flow:
 * 1. FaultTolerance: Routes to healthy node
 * 2. Consensus: Leader proposes, replicates to followers
 * 3. TimeSync: Adds synchronized timestamp + Lamport clock
 * 4. Replication: Persists to storage with quorum
 */
public class Transaction {

    private String transactionId;
    private String userId;
    private BigDecimal amount;
    private String currency;
    private TransactionStatus status;

    // Time Sync fields (Member 3)
    private long createdTimestamp;    // NTP-corrected timestamp
    private long lamportClock;        // Lamport logical clock value

    // Processing metadata
    private String processedByNode;   // Which node processed this
    private int raftTerm;             // Raft term when committed (Member 4)
    private int raftIndex;            // Raft log index (Member 4)

    public Transaction() {}

    public Transaction(String transactionId, String userId, BigDecimal amount, String currency) {
        this.transactionId = transactionId;
        this.userId = userId;
        this.amount = amount;
        this.currency = currency;
        this.status = TransactionStatus.PENDING;
        this.createdTimestamp = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public long getCreatedTimestamp() {
        return createdTimestamp;
    }

    public void setCreatedTimestamp(long createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }

    public long getLamportClock() {
        return lamportClock;
    }

    public void setLamportClock(long lamportClock) {
        this.lamportClock = lamportClock;
    }

    public String getProcessedByNode() {
        return processedByNode;
    }

    public void setProcessedByNode(String processedByNode) {
        this.processedByNode = processedByNode;
    }

    public int getRaftTerm() {
        return raftTerm;
    }

    public void setRaftTerm(int raftTerm) {
        this.raftTerm = raftTerm;
    }

    public int getRaftIndex() {
        return raftIndex;
    }

    public void setRaftIndex(int raftIndex) {
        this.raftIndex = raftIndex;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "id='" + transactionId + '\'' +
                ", userId='" + userId + '\'' +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", status=" + status +
                ", lamportClock=" + lamportClock +
                '}';
    }
}
