package com.payment.ft.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import java.util.Arrays;
import java.util.List;

/**
 * Reads configuration from application.properties and makes
 * values available as Spring beans throughout the application.
 */
@Configuration
public class AppConfig {

    @Value("${app.node.id}")
    private String nodeId;

    @Value("${app.node.host}")
    private String nodeHost;

    @Value("${server.port}")
    private int port;

    /**
     * Parses the comma-separated peer list from properties.
     * e.g., "http://localhost:8082,http://localhost:8083"
     */
    @Value("${app.peers}")
    private String peersRaw;

    @Value("${app.heartbeat.missed-threshold:3}")
    private int missedThreshold;

    public String getNodeId() {
        return nodeId;
    }

    public String getNodeHost() {
        return nodeHost;
    }

    public int getPort() {
        return port;
    }

    public int getMissedThreshold() {
        return missedThreshold;
    }

    public List<String> getPeerUrls() {
        return Arrays.asList(peersRaw.split(","));
    }

    /**
     * RestTemplate is the HTTP client used to call peer nodes.
     * We configure a 2-second connection timeout and 3-second read timeout
     * so that a slow/dead node doesn't block the heartbeat thread.
     */
    @Bean
    public RestTemplate restTemplate() {
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000); // 2 second connect timeout
        factory.setReadTimeout(3000); // 3 second read timeout
        return new RestTemplate(factory);
    }
}
