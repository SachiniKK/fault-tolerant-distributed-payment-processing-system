package com.payment.replication.service;

import com.payment.common.Transaction;
import com.payment.replication.config.ReplicationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Automatic recovery service that runs when a node starts up.
 *
 * On startup, this node's in-memory ledger is empty (all data was lost
 * when the JVM died). This service contacts each peer via the
 * /internal/ledger-sync endpoint and replays all missed transactions
 * into the local TransactionLedger.
 *
 * Uses @EventListener(ApplicationReadyEvent) so it runs AFTER Spring
 * has fully initialized all beans, the web server is listening, and
 * RestTemplate is available.
 */
@Service
public class RecoveryService {
    private static final Logger log = LoggerFactory.getLogger(RecoveryService.class);

    @Autowired private ReplicationConfig config;
    @Autowired private TransactionLedger ledger;
    @Autowired private RestTemplate restTemplate;

    /**
     * Runs automatically after the application is fully started.
     * Contacts each peer to fetch all transactions this node missed.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void recoverFromPeers() {
        List<String> peers = config.getPeerUrls();
        if (peers.isEmpty()) {
            log.info("[RECOVERY] No peers configured — skipping recovery sync");
            return;
        }

        log.info("[RECOVERY] Node {} starting recovery sync from {} peer(s)...",
                config.getNodeId(), peers.size());

        for (String peerUrl : peers) {
            try {
                String url = peerUrl + "/internal/ledger-sync?since=0";
                ResponseEntity<List<Transaction>> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<List<Transaction>>() {}
                );

                List<Transaction> missed = response.getBody();
                if (missed != null && !missed.isEmpty()) {
                    int synced = 0;
                    for (Transaction tx : missed) {
                        if (!ledger.exists(tx.getTransactionId())) {
                            ledger.save(tx);
                            synced++;
                        }
                    }
                    log.info("[RECOVERY] Synced {} transactions from {}", synced, peerUrl);
                    // Successfully recovered from one peer — no need to fetch from others
                    // since all peers should have the same committed data (quorum guarantee)
                    break;
                } else {
                    log.info("[RECOVERY] Peer {} returned 0 entries", peerUrl);
                }
            } catch (Exception e) {
                log.warn("[RECOVERY] Could not reach peer {} — {}", peerUrl, e.getMessage());
            }
        }

        log.info("[RECOVERY] Recovery complete. Ledger now has {} entries.", ledger.size());
    }
}
