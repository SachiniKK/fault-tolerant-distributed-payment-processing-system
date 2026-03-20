package com.payment.consensus.service;

import com.payment.consensus.config.RaftConfig;
import com.payment.consensus.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RaftNode service.
 * Tests leader election, vote handling, and log replication.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RaftNode Tests")
class RaftNodeTest {

    @Mock
    private RaftConfig config;

    @Mock
    private RestTemplate restTemplate;

    private RaftLog raftLog;
    private RaftNode raftNode;

    @BeforeEach
    void setUp() {
        raftLog = new RaftLog();
        raftNode = new RaftNode();

        // Inject dependencies using reflection
        ReflectionTestUtils.setField(raftNode, "config", config);
        ReflectionTestUtils.setField(raftNode, "raftLog", raftLog);
        ReflectionTestUtils.setField(raftNode, "restTemplate", restTemplate);

        // Default config behavior
        when(config.getNodeId()).thenReturn("node1");
        when(config.getElectionTimeoutMinMs()).thenReturn(1500);
        when(config.getElectionTimeoutMaxMs()).thenReturn(3000);
        when(config.getPeerUrls()).thenReturn(Arrays.asList("http://localhost:8072", "http://localhost:8073"));
        when(config.getMajority()).thenReturn(2);
    }

    @Nested
    @DisplayName("Initial State")
    class InitialState {

        @Test
        @DisplayName("Should start as FOLLOWER")
        void shouldStartAsFollower() {
            assertEquals(RaftState.FOLLOWER, raftNode.getState());
        }

        @Test
        @DisplayName("Should start with term 0")
        void shouldStartWithTermZero() {
            assertEquals(0, raftNode.getCurrentTerm());
        }

        @Test
        @DisplayName("Should have no leader initially")
        void shouldHaveNoLeaderInitially() {
            assertNull(raftNode.getCurrentLeaderId());
        }

        @Test
        @DisplayName("Should not have voted for anyone initially")
        void shouldNotHaveVotedInitially() {
            assertNull(raftNode.getVotedFor());
        }
    }

    @Nested
    @DisplayName("Vote Request Handling")
    class VoteRequestHandling {

        @Test
        @DisplayName("Should grant vote to candidate with higher term")
        void shouldGrantVoteToHigherTerm() {
            VoteRequest request = new VoteRequest(1, "node2", 0, 0);

            VoteResponse response = raftNode.handleVoteRequest(request);

            assertTrue(response.isVoteGranted());
            assertEquals(1, response.getTerm());
            assertEquals("node2", raftNode.getVotedFor());
        }

        @Test
        @DisplayName("Should reject vote request with lower term")
        void shouldRejectLowerTerm() {
            // Set node to term 2
            ReflectionTestUtils.setField(raftNode, "currentTerm", 2);

            VoteRequest request = new VoteRequest(1, "node2", 0, 0);

            VoteResponse response = raftNode.handleVoteRequest(request);

            assertFalse(response.isVoteGranted());
            assertEquals(2, response.getTerm());
        }

        @Test
        @DisplayName("Should not vote twice in same term")
        void shouldNotVoteTwiceInSameTerm() {
            // First vote
            VoteRequest request1 = new VoteRequest(1, "node2", 0, 0);
            VoteResponse response1 = raftNode.handleVoteRequest(request1);
            assertTrue(response1.isVoteGranted());

            // Second vote request from different candidate
            VoteRequest request2 = new VoteRequest(1, "node3", 0, 0);
            VoteResponse response2 = raftNode.handleVoteRequest(request2);

            assertFalse(response2.isVoteGranted());
            assertEquals("node2", raftNode.getVotedFor()); // Still voted for node2
        }

        @Test
        @DisplayName("Should grant vote to same candidate again (idempotent)")
        void shouldGrantVoteToSameCandidateAgain() {
            VoteRequest request = new VoteRequest(1, "node2", 0, 0);

            VoteResponse response1 = raftNode.handleVoteRequest(request);
            VoteResponse response2 = raftNode.handleVoteRequest(request);

            assertTrue(response1.isVoteGranted());
            assertTrue(response2.isVoteGranted());
        }

        @Test
        @DisplayName("Should reject vote if candidate log is behind")
        void shouldRejectIfCandidateLogBehind() {
            // Our log has entry at term 2
            raftLog.append(2, "cmd1");

            // Candidate only has term 1 in log
            VoteRequest request = new VoteRequest(3, "node2", 1, 1);

            VoteResponse response = raftNode.handleVoteRequest(request);

            assertFalse(response.isVoteGranted());
        }

        @Test
        @DisplayName("Should grant vote if candidate log is more up-to-date")
        void shouldGrantVoteIfCandidateLogMoreUpToDate() {
            // Our log has entry at term 1
            raftLog.append(1, "cmd1");

            // Candidate has term 2 in log (more up-to-date)
            VoteRequest request = new VoteRequest(3, "node2", 1, 2);

            VoteResponse response = raftNode.handleVoteRequest(request);

            assertTrue(response.isVoteGranted());
        }

        @Test
        @DisplayName("Should step down on higher term in vote request")
        void shouldStepDownOnHigherTerm() {
            // Set as CANDIDATE in term 1
            ReflectionTestUtils.setField(raftNode, "currentTerm", 1);
            ReflectionTestUtils.setField(raftNode, "state", RaftState.CANDIDATE);
            ReflectionTestUtils.setField(raftNode, "votedFor", "node1");

            // Receive vote request with higher term
            VoteRequest request = new VoteRequest(5, "node2", 0, 0);
            raftNode.handleVoteRequest(request);

            assertEquals(5, raftNode.getCurrentTerm());
            assertEquals(RaftState.FOLLOWER, raftNode.getState());
        }
    }

    @Nested
    @DisplayName("AppendEntries Handling")
    class AppendEntriesHandling {

        @Test
        @DisplayName("Should reject AppendEntries with lower term")
        void shouldRejectLowerTerm() {
            ReflectionTestUtils.setField(raftNode, "currentTerm", 2);

            AppendEntriesRequest request = new AppendEntriesRequest(
                    1, "node2", 0, 0, Collections.emptyList(), 0);

            AppendEntriesResponse response = raftNode.handleAppendEntries(request);

            assertFalse(response.isSuccess());
            assertEquals(2, response.getTerm());
        }

        @Test
        @DisplayName("Should accept heartbeat (empty AppendEntries)")
        void shouldAcceptHeartbeat() {
            AppendEntriesRequest request = new AppendEntriesRequest(
                    1, "node2", 0, 0, Collections.emptyList(), 0);

            AppendEntriesResponse response = raftNode.handleAppendEntries(request);

            assertTrue(response.isSuccess());
            assertEquals("node2", raftNode.getCurrentLeaderId());
        }

        @Test
        @DisplayName("Should append entries from leader")
        void shouldAppendEntriesFromLeader() {
            List<LogEntry> entries = Arrays.asList(
                    new LogEntry(1, 1, "PAYMENT:A:B:100:USD"),
                    new LogEntry(1, 2, "PAYMENT:C:D:200:USD")
            );

            AppendEntriesRequest request = new AppendEntriesRequest(
                    1, "leader1", 0, 0, entries, 0);

            AppendEntriesResponse response = raftNode.handleAppendEntries(request);

            assertTrue(response.isSuccess());
            assertEquals(2, raftLog.size());
            assertEquals("PAYMENT:A:B:100:USD", raftLog.getEntry(1).getCommand());
        }

        @Test
        @DisplayName("Should fail consistency check if prevLogIndex entry missing")
        void shouldFailConsistencyCheckIfNoEntry() {
            // No entries in log, but leader thinks we have entry at index 1
            AppendEntriesRequest request = new AppendEntriesRequest(
                    1, "leader1", 1, 1, Collections.emptyList(), 0);

            AppendEntriesResponse response = raftNode.handleAppendEntries(request);

            assertFalse(response.isSuccess());
        }

        @Test
        @DisplayName("Should fail consistency check if prevLogTerm mismatch")
        void shouldFailConsistencyCheckIfTermMismatch() {
            // Our log has entry at index 1 with term 1
            raftLog.append(1, "cmd1");

            // Leader thinks entry at index 1 has term 2
            AppendEntriesRequest request = new AppendEntriesRequest(
                    2, "leader1", 1, 2, Collections.emptyList(), 0);

            AppendEntriesResponse response = raftNode.handleAppendEntries(request);

            assertFalse(response.isSuccess());
        }

        @Test
        @DisplayName("Should update commit index from leader")
        void shouldUpdateCommitIndex() {
            // Append some entries
            raftLog.append(1, "cmd1");
            raftLog.append(1, "cmd2");

            // Leader says commit up to index 2
            AppendEntriesRequest request = new AppendEntriesRequest(
                    1, "leader1", 2, 1, Collections.emptyList(), 2);

            raftNode.handleAppendEntries(request);

            assertEquals(2, raftLog.getCommitIndex());
        }

        @Test
        @DisplayName("Should step down from CANDIDATE on valid AppendEntries")
        void shouldStepDownFromCandidate() {
            ReflectionTestUtils.setField(raftNode, "state", RaftState.CANDIDATE);
            ReflectionTestUtils.setField(raftNode, "currentTerm", 1);

            AppendEntriesRequest request = new AppendEntriesRequest(
                    1, "leader1", 0, 0, Collections.emptyList(), 0);

            raftNode.handleAppendEntries(request);

            assertEquals(RaftState.FOLLOWER, raftNode.getState());
            assertEquals("leader1", raftNode.getCurrentLeaderId());
        }

        @Test
        @DisplayName("Should step down from LEADER on higher term")
        void shouldStepDownFromLeaderOnHigherTerm() {
            ReflectionTestUtils.setField(raftNode, "state", RaftState.LEADER);
            ReflectionTestUtils.setField(raftNode, "currentTerm", 1);

            AppendEntriesRequest request = new AppendEntriesRequest(
                    2, "newLeader", 0, 0, Collections.emptyList(), 0);

            raftNode.handleAppendEntries(request);

            assertEquals(RaftState.FOLLOWER, raftNode.getState());
            assertEquals(2, raftNode.getCurrentTerm());
        }
    }

    @Nested
    @DisplayName("Command Submission")
    class CommandSubmission {

        @Test
        @DisplayName("Should reject command if not leader")
        void shouldRejectIfNotLeader() {
            Map<String, Object> result = raftNode.submitCommand("PAYMENT:A:B:100:USD");

            assertFalse((Boolean) result.get("success"));
            assertEquals("Not the leader", result.get("error"));
        }

        @Test
        @DisplayName("Should accept command if leader")
        void shouldAcceptIfLeader() {
            // Make this node the leader
            ReflectionTestUtils.setField(raftNode, "state", RaftState.LEADER);
            ReflectionTestUtils.setField(raftNode, "currentTerm", 1);

            Map<String, Object> result = raftNode.submitCommand("PAYMENT:A:B:100:USD");

            assertTrue((Boolean) result.get("success"));
            assertEquals(1, result.get("index"));
            assertEquals(1, result.get("term"));
        }

        @Test
        @DisplayName("Should return leader ID when not leader")
        void shouldReturnLeaderIdWhenNotLeader() {
            ReflectionTestUtils.setField(raftNode, "currentLeaderId", "node2");

            Map<String, Object> result = raftNode.submitCommand("PAYMENT:A:B:100:USD");

            assertEquals("node2", result.get("leaderId"));
        }

        @Test
        @DisplayName("Should append command to log when leader")
        void shouldAppendToLogWhenLeader() {
            ReflectionTestUtils.setField(raftNode, "state", RaftState.LEADER);
            ReflectionTestUtils.setField(raftNode, "currentTerm", 3);

            raftNode.submitCommand("PAYMENT:X:Y:500:USD");

            assertEquals(1, raftLog.size());
            assertEquals(3, raftLog.getEntry(1).getTerm());
            assertEquals("PAYMENT:X:Y:500:USD", raftLog.getEntry(1).getCommand());
        }
    }

    @Nested
    @DisplayName("State Transitions")
    class StateTransitions {

        @Test
        @DisplayName("FOLLOWER should remain FOLLOWER on valid heartbeat")
        void followerRemainsFollowerOnHeartbeat() {
            assertEquals(RaftState.FOLLOWER, raftNode.getState());

            AppendEntriesRequest heartbeat = new AppendEntriesRequest(
                    1, "leader1", 0, 0, Collections.emptyList(), 0);
            raftNode.handleAppendEntries(heartbeat);

            assertEquals(RaftState.FOLLOWER, raftNode.getState());
        }

        @Test
        @DisplayName("CANDIDATE should become FOLLOWER on higher term")
        void candidateBecomesFollowerOnHigherTerm() {
            ReflectionTestUtils.setField(raftNode, "state", RaftState.CANDIDATE);
            ReflectionTestUtils.setField(raftNode, "currentTerm", 1);

            VoteRequest request = new VoteRequest(5, "node3", 0, 0);
            raftNode.handleVoteRequest(request);

            assertEquals(RaftState.FOLLOWER, raftNode.getState());
            assertEquals(5, raftNode.getCurrentTerm());
        }
    }

    @Nested
    @DisplayName("Term Management")
    class TermManagement {

        @Test
        @DisplayName("Should update term when seeing higher term in vote request")
        void shouldUpdateTermFromVoteRequest() {
            assertEquals(0, raftNode.getCurrentTerm());

            VoteRequest request = new VoteRequest(5, "node2", 0, 0);
            raftNode.handleVoteRequest(request);

            assertEquals(5, raftNode.getCurrentTerm());
        }

        @Test
        @DisplayName("Should update term when seeing higher term in AppendEntries")
        void shouldUpdateTermFromAppendEntries() {
            assertEquals(0, raftNode.getCurrentTerm());

            AppendEntriesRequest request = new AppendEntriesRequest(
                    3, "leader1", 0, 0, Collections.emptyList(), 0);
            raftNode.handleAppendEntries(request);

            assertEquals(3, raftNode.getCurrentTerm());
        }

        @Test
        @DisplayName("Should clear votedFor when stepping down to new term")
        void shouldClearVotedForOnNewTerm() {
            // Vote in term 1
            VoteRequest vote1 = new VoteRequest(1, "node2", 0, 0);
            raftNode.handleVoteRequest(vote1);
            assertEquals("node2", raftNode.getVotedFor());

            // New higher term arrives
            AppendEntriesRequest ae = new AppendEntriesRequest(
                    3, "leader1", 0, 0, Collections.emptyList(), 0);
            raftNode.handleAppendEntries(ae);

            assertNull(raftNode.getVotedFor());
        }
    }

    @Nested
    @DisplayName("Log Consistency Checks")
    class LogConsistencyChecks {

        @Test
        @DisplayName("Should pass consistency check when log matches")
        void shouldPassWhenLogMatches() {
            raftLog.append(1, "cmd1");
            raftLog.append(1, "cmd2");

            AppendEntriesRequest request = new AppendEntriesRequest(
                    1, "leader1", 2, 1,
                    Arrays.asList(new LogEntry(1, 3, "cmd3")), 0);

            AppendEntriesResponse response = raftNode.handleAppendEntries(request);

            assertTrue(response.isSuccess());
            assertEquals(3, raftLog.size());
        }

        @Test
        @DisplayName("Should accept entry at index 1 with prevLogIndex 0")
        void shouldAcceptFirstEntry() {
            AppendEntriesRequest request = new AppendEntriesRequest(
                    1, "leader1", 0, 0,
                    Arrays.asList(new LogEntry(1, 1, "first_cmd")), 0);

            AppendEntriesResponse response = raftNode.handleAppendEntries(request);

            assertTrue(response.isSuccess());
            assertEquals(1, raftLog.size());
            assertEquals("first_cmd", raftLog.getEntry(1).getCommand());
        }
    }
}
