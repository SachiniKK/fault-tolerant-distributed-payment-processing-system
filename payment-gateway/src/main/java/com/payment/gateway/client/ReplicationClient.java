package com.payment.gateway.client;

import com.payment.common.Transaction;
import com.payment.gateway.config.ModuleConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Client for Data Replication module (Member 2).
 * Calls Replication nodes for persistence and deduplication.
 */
@Service
public class ReplicationClient {

    private static final Logger log = LoggerFactory.getLogger(ReplicationClient.class);

    @Autowired
    private ModuleConfig config;

    @Autowired
    private RestTemplate restTemplate;

    /**
     * Process a transaction through the replication service.
     * Handles quorum-based persistence.
     */
    public Transaction processTransaction(Transaction transaction) {
        for (String replUrl : config.getReplication().getUrls()) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Transaction> request = new HttpEntity<>(transaction, headers);

                ResponseEntity<Transaction> response = restTemplate.postForEntity(
                        replUrl + "/payment", request, Transaction.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    log.info("[REPL-CLIENT] Transaction processed by {}: {}",
                            replUrl, response.getBody().getStatus());
                    return response.getBody();
                }
            } catch (Exception e) {
                log.warn("[REPL-CLIENT] Failed to contact Replication node {}: {}", replUrl, e.getMessage());
            }
        }

        log.error("[REPL-CLIENT] All replication nodes failed!");
        return transaction; // Return original with unchanged status
    }

    /**
     * Check if a transaction ID is a duplicate.
     */
    @SuppressWarnings("unchecked")
    public boolean isDuplicate(String transactionId) {
        for (String replUrl : config.getReplication().getUrls()) {
            try {
                ResponseEntity<Map> response = restTemplate.getForEntity(
                        replUrl + "/replication/check/" + transactionId, Map.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    Object isDup = response.getBody().get("isDuplicate");
                    if (isDup instanceof Boolean) {
                        return (Boolean) isDup;
                    }
                }
            } catch (Exception e) {
                // Try next node
            }
        }
        return false;
    }

    /**
     * Get a transaction by ID from any available node.
     */
    public Optional<Transaction> getTransaction(String transactionId) {
        for (String replUrl : config.getReplication().getUrls()) {
            try {
                ResponseEntity<Transaction> response = restTemplate.getForEntity(
                        replUrl + "/replication/transaction/" + transactionId, Transaction.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    return Optional.of(response.getBody());
                }
            } catch (Exception e) {
                // Try next node
            }
        }
        return Optional.empty();
    }

    /**
     * Get all transactions for a user.
     */
    @SuppressWarnings("unchecked")
    public List<Transaction> getTransactionsByUser(String userId) {
        for (String replUrl : config.getReplication().getUrls()) {
            try {
                ResponseEntity<List> response = restTemplate.getForEntity(
                        replUrl + "/replication/transactions/user/" + userId, List.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    return response.getBody();
                }
            } catch (Exception e) {
                // Try next node
            }
        }
        return List.of();
    }

    /**
     * Check if Replication service is available.
     */
    public boolean isServiceAvailable() {
        for (String replUrl : config.getReplication().getUrls()) {
            try {
                restTemplate.getForEntity(replUrl + "/replication/status", Map.class);
                return true;
            } catch (Exception e) {
                // Try next node
            }
        }
        return false;
    }
}
