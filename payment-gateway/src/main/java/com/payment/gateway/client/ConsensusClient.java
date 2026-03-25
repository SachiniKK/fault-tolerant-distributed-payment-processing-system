package com.payment.gateway.client;

import com.payment.gateway.config.ModuleConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Client for Consensus module (Member 4).
 * Calls Raft nodes via REST for leader info and command submission.
 */
@Service
public class ConsensusClient {

    private static final Logger log = LoggerFactory.getLogger(ConsensusClient.class);

    @Autowired
    private ModuleConfig config;

    @Autowired
    private RestTemplate restTemplate;

    /**
     * Find the current Raft leader.
     * Queries all consensus nodes until one reports being the leader.
     */
    @SuppressWarnings("unchecked")
    public String findLeaderUrl() {
        for (String raftUrl : config.getConsensus().getUrls()) {
            try {
                ResponseEntity<Map> response = restTemplate.getForEntity(
                        raftUrl + "/raft/status", Map.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    Map<String, Object> body = response.getBody();
                    String state = (String) body.get("state");

                    if ("LEADER".equals(state)) {
                        log.info("[CONSENSUS-CLIENT] Found leader: {}", raftUrl);
                        return raftUrl;
                    }

                    // If this node knows who the leader is, return that
                    String leaderId = (String) body.get("leaderId");
                    if (leaderId != null && !leaderId.isEmpty()) {
                        // Try to find the leader URL by ID
                        for (String url : config.getConsensus().getUrls()) {
                            if (url.contains(leaderId.replace("node", ""))) {
                                log.info("[CONSENSUS-CLIENT] Leader reported by {}: {}", raftUrl, url);
                                return url;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("[CONSENSUS-CLIENT] Failed to contact Raft node {}: {}", raftUrl, e.getMessage());
            }
        }

        log.warn("[CONSENSUS-CLIENT] No leader found!");
        return null;
    }

    /**
     * Check if a specific node is the leader.
     */
    @SuppressWarnings("unchecked")
    public boolean isLeader(String raftUrl) {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                    raftUrl + "/raft/status", Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return "LEADER".equals(response.getBody().get("state"));
            }
        } catch (Exception e) {
            log.warn("[CONSENSUS-CLIENT] Failed to check leader status: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Submit a command to the Raft leader.
     *
     * @param command The command (payment JSON) to submit
     * @return Result containing success, index, term
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> submitCommand(String command) {
        String leaderUrl = findLeaderUrl();

        if (leaderUrl == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "No Raft leader available");
            return error;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> body = new HashMap<>();
            body.put("command", command);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    leaderUrl + "/raft/submit", request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("[CONSENSUS-CLIENT] Command submitted to leader. Response: {}", response.getBody());
                return response.getBody();
            }

        } catch (Exception e) {
            log.error("[CONSENSUS-CLIENT] Failed to submit command: {}", e.getMessage());
        }

        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("error", "Failed to submit command to leader");
        return error;
    }

    /**
     * Get current term from any available node.
     */
    @SuppressWarnings("unchecked")
    public int getCurrentTerm() {
        for (String raftUrl : config.getConsensus().getUrls()) {
            try {
                ResponseEntity<Map> response = restTemplate.getForEntity(
                        raftUrl + "/raft/status", Map.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    Object term = response.getBody().get("currentTerm");
                    if (term instanceof Number) {
                        return ((Number) term).intValue();
                    }
                }
            } catch (Exception e) {
                // Try next node
            }
        }
        return -1;
    }
}
