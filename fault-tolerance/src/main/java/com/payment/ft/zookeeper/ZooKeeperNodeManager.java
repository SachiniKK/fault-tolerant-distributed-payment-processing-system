package com.payment.ft.zookeeper;

import com.payment.ft.config.AppConfig;
import com.payment.ft.config.ZooKeeperConfig;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.zookeeper.CreateMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Manages this node's registration in ZooKeeper and monitors peer nodes.
 *
 * On startup: creates an ephemeral node at /payment-nodes/{nodeId}
 * When this node crashes: ZooKeeper auto-deletes the ephemeral node
 * When a peer crashes: ZooKeeper notifies NodeWatcher via PathChildrenCache
 */
@Component
public class ZooKeeperNodeManager {

    private static final Logger log = LoggerFactory.getLogger(ZooKeeperNodeManager.class);

    @Autowired
    private CuratorFramework curator;
    @Autowired
    private AppConfig config;
    @Autowired
    private NodeWatcher nodeWatcher;

    private PathChildrenCache nodeCache;

    /**
     * Called automatically after Spring finishes injecting dependencies.
     * Registers this node and starts watching for peer changes.
     */
    @PostConstruct
    public void initialize() {
        try {
            ensureRootPathExists();
            registerThisNode();
            startWatchingNodes();
            log.info("[ZOOKEEPER] Node {} registered and watching peers", config.getNodeId());
        } catch (Exception e) {
            log.error("[ZOOKEEPER] Failed to initialize: {}", e.getMessage(), e);
            throw new RuntimeException("ZooKeeper initialization failed", e);
        }
    }

    /** Creates /payment-nodes if it doesn't exist yet */
    private void ensureRootPathExists() throws Exception {
        if (curator.checkExists().forPath(ZooKeeperConfig.NODES_ROOT) == null) {
            curator.create().creatingParentsIfNeeded()
                    .forPath(ZooKeeperConfig.NODES_ROOT);
        }
    }

    /**
     * Creates an EPHEMERAL node for this server.
     * Ephemeral = auto-deleted when this process dies.
     * Stores host:port as the node data so peers know how to reach us.
     */
    private void registerThisNode() throws Exception {
        String nodePath = ZooKeeperConfig.NODES_ROOT + "/" + config.getNodeId();
        String nodeData = config.getNodeHost() + ":" + config.getPort();

        // If a stale entry exists (e.g., from a crash without clean shutdown), delete
        // it
        if (curator.checkExists().forPath(nodePath) != null) {
            curator.delete().forPath(nodePath);
        }

        curator.create()
                .withMode(CreateMode.EPHEMERAL) // KEY: auto-deleted on disconnect
                .forPath(nodePath, nodeData.getBytes(StandardCharsets.UTF_8));

        log.info("[ZOOKEEPER] Registered at path: {} with data: {}", nodePath, nodeData);
    }

    /**
     * Uses PathChildrenCache to watch /payment-nodes.
     * When a child node is added or removed, NodeWatcher is notified.
     */
    private void startWatchingNodes() throws Exception {
        nodeCache = new PathChildrenCache(curator, ZooKeeperConfig.NODES_ROOT, true);

        nodeCache.getListenable().addListener((curatorClient, event) -> {
            String childPath = event.getData() != null ? event.getData().getPath() : "unknown";
            String nodeId = childPath.replace(ZooKeeperConfig.NODES_ROOT + "/", "");

            switch (event.getType()) {
                case CHILD_ADDED:
                    log.info("[ZOOKEEPER] Node joined cluster: {}", nodeId);
                    nodeWatcher.onNodeJoined(nodeId);
                    break;
                case CHILD_REMOVED:
                    log.warn("[ZOOKEEPER] Node left/crashed: {}", nodeId);
                    nodeWatcher.onNodeFailed(nodeId);
                    break;
                default:
                    break;
            }
        });

        nodeCache.start();
    }

    /** Returns all currently registered node IDs */
    public List<String> getRegisteredNodeIds() throws Exception {
        return curator.getChildren().forPath(ZooKeeperConfig.NODES_ROOT);
    }

    @PreDestroy
    public void shutdown() {
        try {
            if (nodeCache != null)
                nodeCache.close();
        } catch (Exception e) {
            log.warn("[ZOOKEEPER] Error during shutdown: {}", e.getMessage());
        }
    }
}
