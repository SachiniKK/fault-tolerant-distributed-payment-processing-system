package com.payment.common.interfaces;

import java.util.List;

/**
 * Interface for Fault Tolerance module (Member 1).
 *
 * Responsibilities:
 * - Health monitoring via heartbeats
 * - Node registration with ZooKeeper
 * - Failover routing to healthy nodes
 * - Node recovery handling
 */
public interface FaultToleranceService {

    /**
     * Check if a specific node is healthy.
     */
    boolean isNodeHealthy(String nodeId);

    /**
     * Get list of all healthy node URLs.
     */
    List<String> getHealthyNodes();

    /**
     * Route a request to a healthy node using round-robin.
     * Returns the selected node URL.
     */
    String selectHealthyNode();

    /**
     * Mark a node as down (called when ZooKeeper detects failure).
     */
    void markNodeDown(String nodeId);

    /**
     * Reset a node status when it recovers.
     */
    void resetNode(String nodeId);
}
