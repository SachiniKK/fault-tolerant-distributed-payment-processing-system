package com.payment.ft.detection;

/**
 * Represents the current health status of a payment server node.
 * Used by HeartbeatMonitor and FailoverRouter.
 */
public enum NodeStatus {
    UP, // Node is responding normally
    DOWN, // Node has missed heartbeat threshold
    UNKNOWN // Initial state before first heartbeat check
}
