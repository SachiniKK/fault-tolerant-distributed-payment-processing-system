package com.payment.consensus.controller;

import com.payment.consensus.config.RaftConfig;
import com.payment.consensus.model.*;
import com.payment.consensus.service.RaftLog;
import com.payment.consensus.service.RaftNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
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
}
