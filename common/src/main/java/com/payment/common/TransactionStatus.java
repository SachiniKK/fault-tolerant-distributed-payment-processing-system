package com.payment.common;

/**
 * Shared transaction status enum used by all modules.
 */
public enum TransactionStatus {
    PENDING,      // Initial state
    PROCESSING,   // Being processed by a node
    SUCCESS,      // Successfully committed
    FAILED,       // Failed to process
    DUPLICATE     // Duplicate transaction detected
}
