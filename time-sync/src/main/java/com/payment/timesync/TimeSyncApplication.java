package com.payment.timesync;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Time Synchronization module entry point.
 * Enables @Scheduled for periodic NTP sync tasks.
 */
@SpringBootApplication
@EnableScheduling
public class TimeSyncApplication {
    public static void main(String[] args) {
        SpringApplication.run(TimeSyncApplication.class, args);
    }
}
