package com.payment.replication.ledger;

/**
 * The data object stored in the local ledger.
 * Per the PDF spec: transactionId, idempotencyKey, status,
 * amount, sourceAccount, destinationAccount.
 *
 * Status lifecycle: PENDING -> COMMITTED (success) or FAILED
 */
public class LedgerEntry {

    public enum Status {
        PENDING,
        COMMITTED,
        FAILED
    }

    private String transactionId;       // UUID — unique per payment attempt
    private String idempotencyKey;      // Client-provided key to prevent double-processing
    private Status status;
    private double amount;
    private String sourceAccount;
    private String destinationAccount;
    private String processedByNode;
    private long createdTimestamp;
    private long lastUpdatedTimestamp;

    public LedgerEntry() {
        this.createdTimestamp = System.currentTimeMillis();
        this.lastUpdatedTimestamp = System.currentTimeMillis();
        this.status = Status.PENDING;
    }

    public LedgerEntry(String transactionId, String idempotencyKey,
                       double amount, String sourceAccount, String destinationAccount) {
        this();
        this.transactionId = transactionId;
        this.idempotencyKey = idempotencyKey;
        this.amount = amount;
        this.sourceAccount = sourceAccount;
        this.destinationAccount = destinationAccount;
    }

    // Getters
    public String getTransactionId() { return transactionId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public Status getStatus() { return status; }
    public double getAmount() { return amount; }
    public String getSourceAccount() { return sourceAccount; }
    public String getDestinationAccount() { return destinationAccount; }
    public String getProcessedByNode() { return processedByNode; }
    public long getCreatedTimestamp() { return createdTimestamp; }
    public long getLastUpdatedTimestamp() { return lastUpdatedTimestamp; }

    // Setters
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public void setStatus(Status status) {
        this.status = status;
        this.lastUpdatedTimestamp = System.currentTimeMillis();
    }
    public void setAmount(double amount) { this.amount = amount; }
    public void setSourceAccount(String sourceAccount) { this.sourceAccount = sourceAccount; }
    public void setDestinationAccount(String destinationAccount) { this.destinationAccount = destinationAccount; }
    public void setProcessedByNode(String processedByNode) { this.processedByNode = processedByNode; }
    public void setCreatedTimestamp(long createdTimestamp) { this.createdTimestamp = createdTimestamp; }
    public void setLastUpdatedTimestamp(long lastUpdatedTimestamp) { this.lastUpdatedTimestamp = lastUpdatedTimestamp; }

    @Override
    public String toString() {
        return "LedgerEntry{id='" + transactionId + "', key='" + idempotencyKey +
                "', status=" + status + ", amount=" + amount + "}";
    }
}