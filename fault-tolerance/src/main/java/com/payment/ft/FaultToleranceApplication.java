package com.payment.ft;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main entry point for the Fault Tolerance module.
 * 
 * @EnableScheduling activates the @Scheduled heartbeat monitor.
 */
@SpringBootApplication
@EnableScheduling
public class FaultToleranceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FaultToleranceApplication.class, args);
    }
}
