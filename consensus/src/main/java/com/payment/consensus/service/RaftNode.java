package com.payment.consensus.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.payment.consensus.config.RaftConfig;
import com.payment.consensus.model.AppendEntriesRequest;
import com.payment.consensus.model.AppendEntriesResponse;
import com.payment.consensus.model.LogEntry;
import com.payment.consensus.model.RaftState;
import com.payment.consensus.model.VoteRequest;
import com.payment.consensus.model.VoteResponse;

import jakarta.annotation.PostConstruct;

// Raft consensus node: leader election, log replication, heartbeats.
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

    // ELECTION TIMER
    @Scheduled(fixedRate = 100)
    public void electionTimerCheck() {
        if (state == RaftState.LEADER)
            return;

        long elapsed = System.currentTimeMillis() - lastHeartbeatTime;
        int timeout = ThreadLocalRandom.current().nextInt(
                config.getElectionTimeoutMinMs(), config.getElectionTimeoutMaxMs());

        if (elapsed > timeout) {
            log.info("[RAFT] Election timeout ({} ms). Starting election.", elapsed);
            startElection();
        }
    }

    // LEADER HEARTBEAT
    @Scheduled(fixedDelayString = "${app.raft.heartbeat-interval-ms:500}")
    public void leaderHeartbeat() {
        if (state != RaftState.LEADER)
            return;

        for (String peerUrl : config.getPeerUrls()) {
            sendAppendEntries(peerUrl);
        }
    }

    // LEADER ELECTION
    private synchronized void startElection() {
        currentTerm++;
        state = RaftState.CANDIDATE;
        votedFor = config.getNodeId();
        lastHeartbeatTime = System.currentTimeMillis();

        log.info("[RAFT] Node {} starting election for term {}", config.getNodeId(), currentTerm);

        AtomicInteger voteCount = new AtomicInteger(1);
        int neededVotes = config.getMajority();

        VoteRequest request = new VoteRequest(
                currentTerm, config.getNodeId(),
                raftLog.getLastIndex(), raftLog.getLastTerm());

        for (String peerUrl : config.getPeerUrls()) {
            try {
                String url = peerUrl + "/raft/vote";
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<VoteRequest> entity = new HttpEntity<>(request, headers);

                ResponseEntity<VoteResponse> response = restTemplate.exchange(
                        url, HttpMethod.POST, entity, VoteResponse.class);

                VoteResponse voteResponse = response.getBody();
                if (voteResponse != null) {
                    if (voteResponse.getTerm() > currentTerm) {
                        stepDown(voteResponse.getTerm());
                        return;
                    }
                    if (voteResponse.isVoteGranted()) {
                        int votes = voteCount.incrementAndGet();
                        log.info("[RAFT] Received vote from peer. Total: {}/{}", votes, neededVotes);
                    }
                }
            } catch (Exception e) {
                log.warn("[RAFT] Vote request to {} failed: {}", peerUrl, e.getMessage());
            }
        }

        if (state == RaftState.CANDIDATE && voteCount.get() >= neededVotes) {
            becomeLeader();
        }
    }

    private synchronized void becomeLeader() {
        state = RaftState.LEADER;
        currentLeaderId = config.getNodeId();
        log.info("[RAFT] *** Node {} became LEADER for term {} ***", config.getNodeId(), currentTerm);

        int lastIndex = raftLog.getLastIndex();
        for (String peer : config.getPeerUrls()) {
            nextIndex.put(peer, lastIndex + 1);
            matchIndex.put(peer, 0);
        }

        leaderHeartbeat();
    }

    private synchronized void stepDown(int newTerm) {
        log.info("[RAFT] Stepping down. Old term: {}, new term: {}", currentTerm, newTerm);
        currentTerm = newTerm;
        state = RaftState.FOLLOWER;
        votedFor = null;
        lastHeartbeatTime = System.currentTimeMillis();
    }

    // HANDLE VOTE REQUEST
    public synchronized VoteResponse handleVoteRequest(VoteRequest request) {
        if (request.getTerm() > currentTerm) {
            stepDown(request.getTerm());
        }

        if (request.getTerm() < currentTerm) {
            return new VoteResponse(currentTerm, false);
        }

        boolean canVote = (votedFor == null || votedFor.equals(request.getCandidateId()));
        boolean logOk = isLogUpToDate(request.getLastLogTerm(), request.getLastLogIndex());

        if (canVote && logOk) {
            votedFor = request.getCandidateId();
            lastHeartbeatTime = System.currentTimeMillis();
            log.info("[RAFT] Voted for {} in term {}", request.getCandidateId(), currentTerm);
            return new VoteResponse(currentTerm, true);
        }

        return new VoteResponse(currentTerm, false);
    }

    private boolean isLogUpToDate(int candidateLastTerm, int candidateLastIndex) {
        int myLastTerm = raftLog.getLastTerm();
        int myLastIndex = raftLog.getLastIndex();

        if (candidateLastTerm != myLastTerm) {
            return candidateLastTerm > myLastTerm;
        }
        return candidateLastIndex >= myLastIndex;
    }

    // HANDLE APPEND ENTRIES
    public synchronized AppendEntriesResponse handleAppendEntries(AppendEntriesRequest request) {
        if (request.getTerm() < currentTerm) {
            return new AppendEntriesResponse(currentTerm, false);
        }

        if (request.getTerm() >= currentTerm) {
            if (request.getTerm() > currentTerm || state != RaftState.FOLLOWER) {
                stepDown(request.getTerm());
            }
            currentLeaderId = request.getLeaderId();
            lastHeartbeatTime = System.currentTimeMillis();
        }

        // Log consistency check
        if (request.getPrevLogIndex() > 0) {
            int termAtPrev = raftLog.getTermAt(request.getPrevLogIndex());
            if (termAtPrev == 0 || termAtPrev != request.getPrevLogTerm()) {
                return new AppendEntriesResponse(currentTerm, false);
            }
        }

        // Append new entries
        if (request.getEntries() != null) {
            for (LogEntry entry : request.getEntries()) {
                raftLog.appendAt(entry);
            }
        }

        // Update commit index
        if (request.getLeaderCommit() > raftLog.getCommitIndex()) {
            int lastNewIndex = request.getEntries() != null && !request.getEntries().isEmpty()
                    ? request.getEntries().get(request.getEntries().size() - 1).getIndex()
                    : raftLog.getLastIndex();
            raftLog.setCommitIndex(Math.min(request.getLeaderCommit(), lastNewIndex));
        }

        return new AppendEntriesResponse(currentTerm, true);
    }

    // LOG REPLICATION
    private void sendAppendEntries(String peerUrl) {
        try {
            int peerNextIndex = nextIndex.getOrDefault(peerUrl, raftLog.getLastIndex() + 1);
            int prevIndex = peerNextIndex - 1;
            int prevTerm = raftLog.getTermAt(prevIndex);

            List<LogEntry> entries = Collections.emptyList();
            if (peerNextIndex <= raftLog.getLastIndex()) {
                entries = raftLog.getEntries(peerNextIndex, raftLog.getLastIndex());
            }

            AppendEntriesRequest request = new AppendEntriesRequest(
                    currentTerm, config.getNodeId(), prevIndex, prevTerm, entries, raftLog.getCommitIndex());

            String url = peerUrl + "/raft/append";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<AppendEntriesRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<AppendEntriesResponse> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, AppendEntriesResponse.class);

            AppendEntriesResponse appendResponse = response.getBody();
            if (appendResponse != null) {
                if (appendResponse.getTerm() > currentTerm) {
                    stepDown(appendResponse.getTerm());
                    return;
                }

                if (appendResponse.isSuccess()) {
                    if (!entries.isEmpty()) {
                        int lastSentIndex = entries.get(entries.size() - 1).getIndex();
                        nextIndex.put(peerUrl, lastSentIndex + 1);
                        matchIndex.put(peerUrl, lastSentIndex);
                        updateCommitIndex();
                    }
                } else {
                    int newNextIndex = Math.max(1, peerNextIndex - 1);
                    nextIndex.put(peerUrl, newNextIndex);
                }
            }
        } catch (Exception e) {
            log.warn("[RAFT] AppendEntries to {} failed: {}", peerUrl, e.getMessage());
        }
    }

    private synchronized void updateCommitIndex() {
        int lastIndex = raftLog.getLastIndex();
        for (int n = lastIndex; n > raftLog.getCommitIndex(); n--) {
            if (raftLog.getTermAt(n) != currentTerm)
                continue;

            int replicatedCount = 1;
            for (String peer : config.getPeerUrls()) {
                if (matchIndex.getOrDefault(peer, 0) >= n) {
                    replicatedCount++;
                }
            }

            if (replicatedCount >= config.getMajority()) {
                raftLog.setCommitIndex(n);
                log.info("[RAFT] Committed up to index {}", n);
                break;
            }
        }
    }

    // CLIENT COMMAND SUBMISSION 
    public Map<String, Object> submitCommand(String command) {
        if (state != RaftState.LEADER) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", "Not the leader");
            result.put("leaderId", currentLeaderId);
            return result;
        }

        LogEntry entry = raftLog.append(currentTerm, command);
        log.info("[RAFT] Leader appended command at index {}", entry.getIndex());

        for (String peerUrl : config.getPeerUrls()) {
            sendAppendEntries(peerUrl);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("index", entry.getIndex());
        result.put("term", entry.getTerm());
        result.put("leaderId", config.getNodeId());
        return result;
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