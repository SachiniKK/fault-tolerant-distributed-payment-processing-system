package com.payment.replication.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

/**
 * Reads all configuration values from application.properties.
 * Provides RestTemplate bean with timeouts for peer HTTP calls.
 */
@Configuration
public class ReplicationConfig {

    @Value("${app.node.id}")
    private String nodeId;

    @Value("${server.port}")
    private int port;

    @Value("${app.peers:}")
    private String peersRaw;

    @Value("${app.replication.write-quorum:2}")
    private int writeQuorum;

    @Value("${app.idempotency.ttl-minutes:5}")
    private int idempotencyTtlMinutes;

    public String getNodeId() { return nodeId; }
    public int getPort() { return port; }
    public int getWriteQuorum() { return writeQuorum; }
    public int getIdempotencyTtlMinutes() { return idempotencyTtlMinutes; }

    public List<String> getPeerUrls() {
        if (peersRaw == null || peersRaw.isBlank()) return List.of();
        return Arrays.asList(peersRaw.split(","));
    }

    @Bean
    public RestTemplate restTemplate() {
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(3000);
        return new RestTemplate(factory);
    }
}