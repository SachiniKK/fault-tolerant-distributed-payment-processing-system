package com.payment.timesync.service;

import com.payment.timesync.config.TimeSyncConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.net.*;

/**
 * NTP synchronization service using raw UDP packets.
 *
 * From lectures: NTP (Network Time Protocol) synchronizes clocks
 * across machines by measuring round-trip delay and calculating
 * the offset between local and server clocks.
 *
 * Offset = ((T2 - T1) + (T3 - T4)) / 2
 * where T1 = client send, T2 = server receive, T3 = server send, T4 = client
 * receive
 */
@Service
public class NtpSyncService {
    private static final Logger log = LoggerFactory.getLogger(NtpSyncService.class);

    @Autowired
    private TimeSyncConfig config;

    private volatile long offsetMs = 0;
    private volatile long lastSyncTime = 0;
    private volatile boolean syncSuccessful = false;

    @PostConstruct
    public void init() {
        syncWithNtp();
    }

    /**
     * Periodically sync with NTP server.
     */
    @Scheduled(fixedDelayString = "${app.ntp.sync-interval-ms:60000}")
    public void scheduledSync() {
        syncWithNtp();
    }

    /**
     * Performs NTP synchronization using raw UDP.
     * NTP uses 48-byte packets on port 123.
     */
    public void syncWithNtp() {
        try {
            InetAddress address = InetAddress.getByName(config.getNtpServer());

            // NTP packet: 48 bytes, first byte = 0x1B (LI=0, VN=3, Mode=3=client)
            byte[] buffer = new byte[48];
            buffer[0] = 0x1B;

            DatagramSocket socket = new DatagramSocket();
            socket.setSoTimeout(5000);

            long t1 = System.currentTimeMillis();

            DatagramPacket request = new DatagramPacket(buffer, buffer.length, address, 123);
            socket.send(request);

            DatagramPacket response = new DatagramPacket(buffer, buffer.length);
            socket.receive(response);

            long t4 = System.currentTimeMillis();
            socket.close();

            // Extract server transmit timestamp from bytes 40-47
            // NTP timestamps are seconds since 1900-01-01
            long serverTime = extractNtpTimestamp(buffer, 40);

            // T2 and T3 are approximated as serverTime (for basic NTP)
            long t2 = serverTime;
            long t3 = serverTime;

            // Offset = ((T2 - T1) + (T3 - T4)) / 2
            this.offsetMs = ((t2 - t1) + (t3 - t4)) / 2;
            this.lastSyncTime = System.currentTimeMillis();
            this.syncSuccessful = true;

            log.info("[NTP] Synced with {}. Offset: {} ms", config.getNtpServer(), offsetMs);

        } catch (Exception e) {
            log.warn("[NTP] Sync failed: {}", e.getMessage());
            this.syncSuccessful = false;
        }
    }

    /**
     * Extract NTP timestamp (seconds since 1900) from packet bytes
     * and convert to Java epoch milliseconds (since 1970).
     */
    private long extractNtpTimestamp(byte[] buffer, int offset) {
        // Read 4 bytes as unsigned integer (seconds since 1900)
        long seconds = 0;
        for (int i = 0; i < 4; i++) {
            seconds = (seconds << 8) | (buffer[offset + i] & 0xFF);
        }
        // Read 4 bytes for fractional seconds
        long fraction = 0;
        for (int i = 4; i < 8; i++) {
            fraction = (fraction << 8) | (buffer[offset + i] & 0xFF);
        }

        // Convert NTP epoch (1900) to Java epoch (1970)
        // Difference: 70 years = 2208988800 seconds
        long NTP_EPOCH_OFFSET = 2208988800L;
        long epochSeconds = seconds - NTP_EPOCH_OFFSET;
        long millis = (fraction * 1000L) / 0x100000000L;

        return epochSeconds * 1000 + millis;
    }

    /** Returns the current time corrected by NTP offset. */
    public long getCorrectedTime() {
        return System.currentTimeMillis() + offsetMs;
    }

    public long getOffsetMs() {
        return offsetMs;
    }

    public long getLastSyncTime() {
        return lastSyncTime;
    }

    public boolean isSyncSuccessful() {
        return syncSuccessful;
    }
}
