package com.payment.timesync.service;

import com.payment.timesync.config.TimeSyncConfig;
import com.payment.timesync.model.DualTimestamp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Creates dual timestamps for events by combining
 * NTP-corrected physical time with Lamport logical time.
 */
@Service
public class TimestampCorrector {

    @Autowired
    private NtpSyncService ntpSync;
    @Autowired
    private LamportClock lamportClock;
    @Autowired
    private TimeSyncConfig config;

    /**
     * Creates a DualTimestamp for a local event.
     * Ticks the Lamport clock (local event) and reads NTP-corrected time.
     */
    public DualTimestamp stampLocalEvent() {
        long lamport = lamportClock.tick();
        long ntpTime = ntpSync.getCorrectedTime();
        return new DualTimestamp(ntpTime, lamport, config.getNodeId());
    }

    /**
     * Creates a DualTimestamp on receiving a remote event.
     * Updates the Lamport clock with the remote timestamp.
     */
    public DualTimestamp stampRemoteEvent(long remoteLamport) {
        long lamport = lamportClock.update(remoteLamport);
        long ntpTime = ntpSync.getCorrectedTime();
        return new DualTimestamp(ntpTime, lamport, config.getNodeId());
    }
}
