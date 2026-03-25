package com.payment.timesync.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

/**
 * Configuration for the Time Synchronization module.
 */
@Configuration
public class TimeSyncConfig {
    @Value("${app.node.id}")
    private String nodeId;

    @Value("${app.ntp.server:pool.ntp.org}")
    private String ntpServer;

    @Value("${app.ntp.sync-interval-ms:60000}")
    private long ntpSyncIntervalMs;

    @Value("${app.peers:}")
    private String peersRaw;

    public String getNodeId() {
        return nodeId;
    }

    public String getNtpServer() {
        return ntpServer;
    }

    public long getNtpSyncIntervalMs() {
        return ntpSyncIntervalMs;
    }

    public List<String> getPeerUrls() {
        if (peersRaw == null || peersRaw.isBlank())
            return List.of();
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
