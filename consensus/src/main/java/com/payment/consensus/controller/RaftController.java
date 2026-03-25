package com.payment.consensus.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

import com.payment.consensus.config.RaftConfig;
import com.payment.consensus.model.AppendEntriesRequest;
import com.payment.consensus.model.AppendEntriesResponse;
import com.payment.consensus.model.LogEntry;
import com.payment.consensus.model.VoteRequest;
import com.payment.consensus.model.VoteResponse;
import com.payment.consensus.service.RaftLog;
import com.payment.consensus.service.RaftNode;

@RestController
@CrossOrigin(origins = "*")
public class RaftController {
    private static final Logger log = LoggerFactory.getLogger(RaftController.class);

    @Autowired
    private RaftConfig config;
    @Autowired
    private RaftNode raftNode;
    @Autowired
    private RaftLog raftLog;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "nodeId", config.getNodeId(),
                "status", "UP"));
    }

    @PostMapping("/raft/vote")
    public ResponseEntity<VoteResponse> requestVote(@RequestBody VoteRequest request) {
        VoteResponse response = raftNode.handleVoteRequest(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/raft/append")
    public ResponseEntity<AppendEntriesResponse> appendEntries(@RequestBody AppendEntriesRequest request) {
        AppendEntriesResponse response = raftNode.handleAppendEntries(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/raft/submit")
    public ResponseEntity<Map<String, Object>> submit(@RequestBody Map<String, String> body) {
        String command = body.get("command");
        if (command == null || command.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing command"));
        }

        Map<String, Object> result = raftNode.submitCommand(command);

        if ((boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(307).body(result);
        }
    }

    @GetMapping("/raft/status")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(Map.of(
                "nodeId", config.getNodeId(),
                "state", raftNode.getState().name(),
                "currentTerm", raftNode.getCurrentTerm(),
                "votedFor", raftNode.getVotedFor() != null ? raftNode.getVotedFor() : "none",
                "currentLeader", raftNode.getCurrentLeaderId() != null ? raftNode.getCurrentLeaderId() : "unknown",
                "logSize", raftLog.size(),
                "commitIndex", raftLog.getCommitIndex()));
    }

    @GetMapping("/raft/log")
    public ResponseEntity<List<Map<String, Object>>> getLog() {
        List<LogEntry> entries = raftLog.getEntries(1, raftLog.size());
        int commitIndex = raftLog.getCommitIndex();

        List<Map<String, Object>> result = new ArrayList<>();
        for (LogEntry entry : entries) {
            Map<String, Object> entryMap = new HashMap<>();
            entryMap.put("index", entry.getIndex());
            entryMap.put("term", entry.getTerm());
            entryMap.put("command", entry.getCommand());
            entryMap.put("committed", entry.getIndex() <= commitIndex);
            result.add(entryMap);
        }
        return ResponseEntity.ok(result);
    }
}
