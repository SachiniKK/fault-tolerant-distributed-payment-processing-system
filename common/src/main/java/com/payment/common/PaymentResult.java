package com.payment.common;

/**
 * Response DTO for payment operations.
 * Includes status and any error messages.
 */
public class PaymentResult {

    private boolean success;
    private String transactionId;
    private TransactionStatus status;
    private String message;
    private String processedBy;
    private long timestamp;

    public PaymentResult() {}

    public static PaymentResult success(Transaction tx) {
        PaymentResult result = new PaymentResult();
        result.success = true;
        result.transactionId = tx.getTransactionId();
        result.status = tx.getStatus();
        result.message = "Payment processed successfully";
        result.processedBy = tx.getProcessedByNode();
        result.timestamp = tx.getCreatedTimestamp();
        return result;
    }

    public static PaymentResult failed(String message) {
        PaymentResult result = new PaymentResult();
        result.success = false;
        result.status = TransactionStatus.FAILED;
        result.message = message;
        return result;
    }

    public static PaymentResult duplicate(String transactionId) {
        PaymentResult result = new PaymentResult();
        result.success = false;
        result.transactionId = transactionId;
        result.status = TransactionStatus.DUPLICATE;
        result.message = "Duplicate transaction detected";
        return result;
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getProcessedBy() {
        return processedBy;
    }

    public void setProcessedBy(String processedBy) {
        this.processedBy = processedBy;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
