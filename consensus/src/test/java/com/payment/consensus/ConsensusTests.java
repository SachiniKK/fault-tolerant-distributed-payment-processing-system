package com.payment.consensus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConsensusTests {

    private RaftLog raftLog;

    @BeforeEach
    void setUp() {
        raftLog = new RaftLog();
    }

    // --- RaftLog Tests ---

    @Test
    void emptyLogHasZeroIndex() {
        assertEquals(0, raftLog.getLastIndex());
        assertEquals(0, raftLog.getLastTerm());
    }

    @Test
    void appendIncrementsIndex() {
        raftLog.append(1, "cmd1");
        assertEquals(1, raftLog.getLastIndex());

        raftLog.append(1, "cmd2");
        assertEquals(2, raftLog.getLastIndex());
    }

    @Test
    void getEntryReturnsCorrectEntry() {
        raftLog.append(1, "cmd1");
        raftLog.append(2, "cmd2");

        LogEntry entry = raftLog.getEntry(1);
        assertNotNull(entry);
        assertEquals(1, entry.getTerm());
        assertEquals("cmd1", entry.getCommand());

        LogEntry entry2 = raftLog.getEntry(2);
        assertEquals(2, entry2.getTerm());
        assertEquals("cmd2", entry2.getCommand());
    }

    @Test
    void getEntryReturnsNullForInvalidIndex() {
        assertNull(raftLog.getEntry(0));
        assertNull(raftLog.getEntry(1));
    }

    @Test
    void truncateRemovesEntriesFromIndex() {
        raftLog.append(1, "cmd1");
        raftLog.append(1, "cmd2");
        raftLog.append(2, "cmd3");

        raftLog.truncateFrom(2);
        assertEquals(1, raftLog.getLastIndex());
        assertEquals("cmd1", raftLog.getEntry(1).getCommand());
    }

    @Test
    void getTermAtReturnsCorrectTerm() {
        raftLog.append(1, "cmd1");
        raftLog.append(3, "cmd2");

        assertEquals(1, raftLog.getTermAt(1));
        assertEquals(3, raftLog.getTermAt(2));
        assertEquals(0, raftLog.getTermAt(3));
    }

    @Test
    void commitIndexTracksCorrectly() {
        assertEquals(0, raftLog.getCommitIndex());
        raftLog.setCommitIndex(5);
        assertEquals(5, raftLog.getCommitIndex());
    }

    @Test
    void getEntriesReturnsRange() {
        raftLog.append(1, "cmd1");
        raftLog.append(1, "cmd2");
        raftLog.append(2, "cmd3");

        List<LogEntry> entries = raftLog.getEntries(2, 3);
        assertEquals(2, entries.size());
        assertEquals("cmd2", entries.get(0).getCommand());
        assertEquals("cmd3", entries.get(1).getCommand());
    }

    @Test
    void appendAtHandlesConflict() {
        raftLog.append(1, "cmd1");
        raftLog.append(1, "cmd2");
        raftLog.append(1, "cmd3");

        LogEntry conflicting = new LogEntry(2, 2, "new-cmd2");
        raftLog.appendAt(conflicting);

        assertEquals(2, raftLog.getLastIndex());
        assertEquals(2, raftLog.getEntry(2).getTerm());
        assertEquals("new-cmd2", raftLog.getEntry(2).getCommand());
    }

    // --- Model Tests ---

    @Test
    void raftStateEnumValues() {
        assertEquals(3, RaftState.values().length);
        assertNotNull(RaftState.FOLLOWER);
        assertNotNull(RaftState.CANDIDATE);
        assertNotNull(RaftState.LEADER);
    }

    @Test
    void voteRequestFieldsSetCorrectly() {
        VoteRequest req = new VoteRequest(5, "node1", 10, 3);
        assertEquals(5, req.getTerm());
        assertEquals("node1", req.getCandidateId());
        assertEquals(10, req.getLastLogIndex());
        assertEquals(3, req.getLastLogTerm());
    }

    @Test
    void appendEntriesRequestFieldsSetCorrectly() {
        List<LogEntry> entries = List.of(new LogEntry(1, 1, "cmd"));
        AppendEntriesRequest req = new AppendEntriesRequest(2, "leader1", 0, 0, entries, 0);
        assertEquals(2, req.getTerm());
        assertEquals("leader1", req.getLeaderId());
        assertEquals(1, req.getEntries().size());
    }

    @Test
    void logEntryToString() {
        LogEntry entry = new LogEntry(1, 5, "pay-100");
        String s = entry.toString();
        assertTrue(s.contains("term=1"));
        assertTrue(s.contains("index=5"));
        assertTrue(s.contains("pay-100"));
    }
}
