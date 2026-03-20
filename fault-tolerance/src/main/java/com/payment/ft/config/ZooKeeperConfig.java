package com.payment.ft.config;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the Apache Curator Framework ZooKeeper client.
 * Curator is a higher-level wrapper around the raw ZooKeeper client
 * that handles connection management, retries, and error handling.
 */
@Configuration
public class ZooKeeperConfig {

    @Value("${app.zookeeper.connect-string:127.0.0.1:2181}")
    private String connectString;

    @Value("${app.zookeeper.session-timeout-ms:5000}")
    private int sessionTimeout;

    @Value("${app.zookeeper.connection-timeout-ms:3000}")
    private int connectionTimeout;

    /**
     * Creates and starts the Curator client.
     * ExponentialBackoffRetry: if ZooKeeper is temporarily unreachable,
     * retry with increasing delays (1s, 2s, 4s, ...) up to 3 times.
     */
    @Bean(destroyMethod = "close")
    public CuratorFramework curatorFramework() {
        CuratorFramework client = CuratorFrameworkFactory.builder()
                .connectString(connectString)
                .sessionTimeoutMs(sessionTimeout)
                .connectionTimeoutMs(connectionTimeout)
                .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                .build();

        client.start();
        return client;
    }

    // ZooKeeper path constants — all nodes in the cluster use these paths
    public static final String NODES_ROOT = "/payment-nodes";
    public static final String LEADER_PATH = "/payment-leader";
}
