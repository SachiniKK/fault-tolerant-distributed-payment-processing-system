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
    public void onNodeJoined(String nodeId) {
        if (nodeId.equals(config.getNodeId())) {
            return; // Don't watch ourselves
        }
        log.info("[WATCHER] Node {} joined — resetting heartbeat state", nodeId);

        // Find the URL for this nodeId from the peers list
        String peerUrl = findUrlForNodeId(nodeId);
        if (peerUrl != null) {
            heartbeatMonitor.resetNode(peerUrl);
        }
    }

    /**
     * Called when a node disappears from ZooKeeper (crash or clean shutdown).
     * ZooKeeper auto-detects this via the ephemeral node expiring.
     */
    public void onNodeFailed(String nodeId) {
        if (nodeId.equals(config.getNodeId())) {
            return; // Don't process events about ourselves
        }
        log.error("[WATCHER] Node {} detected as FAILED via ZooKeeper!", nodeId);
        // HeartbeatMonitor will mark it DOWN within its next cycle
        // ZooKeeper gives us early warning before the 3-beat threshold
    }

    /**
     * Derives peer URL from nodeId using the convention node1->8081, node2->8082
     */
    private String findUrlForNodeId(String nodeId) {
        for (String url : config.getPeerUrls()) {
            if (url.contains("808")) {
                // Simple mapping: node1->8081, node2->8082, node3->8083
                String num = nodeId.replace("node", "");
                String port = "808" + num;
                if (url.contains(port))
                    return url;
            }
        }
        return null;
    }
}
