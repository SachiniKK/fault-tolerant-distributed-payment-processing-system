package com.payment.ft;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main entry point for the Fault Tolerance module.
 *
 * This module provides:
 * - Heartbeat-based failure detection
 * - ZooKeeper-based node registration and monitoring
 * - Automatic failover routing
 * - Node recovery after crash
 *
 * Requires ZooKeeper running on port 2181.
 */
@SpringBootApplication
@EnableScheduling
public class FaultToleranceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FaultToleranceApplication.class, args);
    }
}
