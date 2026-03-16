package com.payment.consensus.service;

import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.payment.consensus.config.RaftConfig;
import com.payment.consensus.model.RaftState;

import jakarta.annotation.PostConstruct;

/**
 * Raft consensus node: leader election, log replication, heartbeats.
 */
@Service
public class RaftNode {
    private static final Logger log = LoggerFactory.getLogger(RaftNode.class);

    @Autowired
    private RaftConfig config;
    @Autowired
    private RaftLog raftLog;
    @Autowired
    private RestTemplate restTemplate;

    // Persistent state
    private volatile int currentTerm = 0;
    private volatile String votedFor = null;

    // Volatile state
    private volatile RaftState state = RaftState.FOLLOWER;
    private volatile long lastHeartbeatTime = System.currentTimeMillis();
    private volatile String currentLeaderId = null;

    // Leader-only state
    private final ConcurrentHashMap<String, Integer> nextIndex = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> matchIndex = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("[RAFT] Node {} starting as FOLLOWER in term {}", config.getNodeId(), currentTerm);
    }

    // GETTERS
    public RaftState getState() {
        return state;
    }

    public int getCurrentTerm() {
        return currentTerm;
    }

    public String getCurrentLeaderId() {
        return currentLeaderId;
    }

    public String getVotedFor() {
        return votedFor;
    }
}