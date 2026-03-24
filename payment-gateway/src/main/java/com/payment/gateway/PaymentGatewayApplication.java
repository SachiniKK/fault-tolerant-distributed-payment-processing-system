package com.payment.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Payment Gateway Application.
 *
 * This is the main entry point that integrates all 4 modules:
 * 1. Fault Tolerance (Member 1) - Health, Failover, Recovery
 * 2. Consensus (Member 4) - Raft leader election, log replication
 * 3. Time Sync (Member 3) - NTP synchronization, Lamport clocks
 * 4. Replication (Member 2) - Quorum-based persistence, deduplication
 *
 * Payment Flow:
 * Client -> Gateway -> FT (route) -> Consensus (order) -> TimeSync (timestamp) -> Replication (persist)
 */
@SpringBootApplication
@EnableScheduling
public class PaymentGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentGatewayApplication.class, args);
    }
}
