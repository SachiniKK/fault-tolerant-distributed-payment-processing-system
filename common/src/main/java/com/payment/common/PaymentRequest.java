package com.payment.common;

import java.math.BigDecimal;

/**
 * Request DTO for submitting a new payment.
 * Used by the Gateway to receive client requests.
 */
public class PaymentRequest {

    private String userId;
    private BigDecimal amount;
    private String currency;
    private String idempotencyKey;  // Optional: client-provided key for deduplication

    public PaymentRequest() {}

    public PaymentRequest(String userId, BigDecimal amount, String currency) {
        this.userId = userId;
        this.amount = amount;
        this.currency = currency;
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

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }
}
