package com.payment.timesync.model;

import java.io.Serializable;

/**
 * Dual timestamp combining physical (NTP-corrected) and logical (Lamport) time.
 *
 * From lectures: neither physical nor logical clocks alone are sufficient
 * for ordering events in a distributed system. Physical clocks can drift;
 * logical clocks don't capture real time. By combining both:
 * - NTP-corrected wall clock gives approximate real-world ordering
 * - Lamport clock gives causal ordering guarantee
 *
 * Ordering: first compare Lamport values (causal order),
 * then use NTP time as tiebreaker (physical order).
 */
public class DualTimestamp implements Serializable, Comparable<DualTimestamp> {
    private static final long serialVersionUID = 1L;

    private long ntpCorrectedTimeMs;
    private long lamportTimestamp;
    private String nodeId;

    public DualTimestamp() {
    }

    public DualTimestamp(long ntpCorrectedTimeMs, long lamportTimestamp, String nodeId) {
        this.ntpCorrectedTimeMs = ntpCorrectedTimeMs;
        this.lamportTimestamp = lamportTimestamp;
        this.nodeId = nodeId;
    }

    public long getNtpCorrectedTimeMs() {
        return ntpCorrectedTimeMs;
    }

    public void setNtpCorrectedTimeMs(long ntpCorrectedTimeMs) {
        this.ntpCorrectedTimeMs = ntpCorrectedTimeMs;
    }

    public long getLamportTimestamp() {
        return lamportTimestamp;
    }

    public void setLamportTimestamp(long lamportTimestamp) {
        this.lamportTimestamp = lamportTimestamp;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    /**
     * Compare by Lamport (causal) first, then NTP (physical), then nodeId
     * (tiebreak).
     */
    @Override
    public int compareTo(DualTimestamp other) {
        int cmp = Long.compare(this.lamportTimestamp, other.lamportTimestamp);
        if (cmp != 0)
            return cmp;
        cmp = Long.compare(this.ntpCorrectedTimeMs, other.ntpCorrectedTimeMs);
        if (cmp != 0)
            return cmp;
        return this.nodeId.compareTo(other.nodeId);
    }

    @Override
    public String toString() {
        return "DualTimestamp{lamport=" + lamportTimestamp +
                ", ntp=" + ntpCorrectedTimeMs + ", node=" + nodeId + "}";
    }
}
