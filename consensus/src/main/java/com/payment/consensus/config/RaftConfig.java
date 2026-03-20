package com.payment.consensus.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

/**
 * Raft consensus configuration.
 * Election timeout is randomized between min and max to prevent split votes.
 */
@Configuration
public class RaftConfig {
    @Value("${app.node.id}")
    private String nodeId;

    @Value("${app.raft.election-timeout-min-ms:1500}")
    private int electionTimeoutMinMs;

    @Value("${app.raft.election-timeout-max-ms:3000}")
    private int electionTimeoutMaxMs;

    @Value("${app.raft.heartbeat-interval-ms:500}")
    private int heartbeatIntervalMs;

    @Value("${app.peers:}")
    private String peersRaw;

    public String getNodeId() {
        return nodeId;
    }

    public int getElectionTimeoutMinMs() {
        return electionTimeoutMinMs;
    }

    public int getElectionTimeoutMaxMs() {
        return electionTimeoutMaxMs;
    }

    public int getHeartbeatIntervalMs() {
        return heartbeatIntervalMs;
    }

    public List<String> getPeerUrls() {
        if (peersRaw == null || peersRaw.isBlank())
            return List.of();
        return Arrays.asList(peersRaw.split(","));
    }

    public int getClusterSize() {
        return getPeerUrls().size() + 1;
    }

    public int getMajority() {
        return getClusterSize() / 2 + 1;
    }

    @Bean
    public RestTemplate restTemplate() {
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(3000);
        return new RestTemplate(factory);
    }
}
