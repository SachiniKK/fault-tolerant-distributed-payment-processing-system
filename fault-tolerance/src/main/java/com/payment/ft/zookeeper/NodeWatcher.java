package com.payment.ft.zookeeper;

import com.payment.ft.config.AppConfig;
import com.payment.ft.detection.HeartbeatMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Handles ZooKeeper events for node joins and departures.
 * Bridges ZooKeeper events to the HeartbeatMonitor's status map.
 */
@Component
public class NodeWatcher {

    private static final Logger log = LoggerFactory.getLogger(NodeWatcher.class);

    @Autowired
    private AppConfig config;
    @Lazy
    @Autowired
    private HeartbeatMonitor heartbeatMonitor;

    /**
     * Called when a new node appears in ZooKeeper (startup or rejoin).
     * Resets the node's heartbeat counter so it gets a fresh start.
     */
    public void onNodeJoined(String nodeId, String nodeUrl) {
        if (nodeId.equals(config.getNodeId())) {
            return; // Don't watch ourselves
        }
        log.info("[WATCHER] Node {} joined at {} — resetting heartbeat state", nodeId, nodeUrl);
        heartbeatMonitor.resetNode(nodeUrl);
    }

    /**
     * Called when a node disappears from ZooKeeper (crash or clean shutdown).
     * ZooKeeper auto-detects this via the ephemeral node expiring.
     */
    public void onNodeFailed(String nodeId, String nodeUrl) {
        if (nodeId.equals(config.getNodeId())) {
            return; // Don't process events about ourselves
        }
        log.error("[WATCHER] Node {} detected as FAILED via ZooKeeper!", nodeId);
        // Immediately mark the node as DOWN for faster failover
        heartbeatMonitor.forceDown(nodeUrl);
    }
}
