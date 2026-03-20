package com.payment.consensus.service;

import com.payment.consensus.model.LogEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RaftLog service.
 * Tests log operations: append, truncate, commit, and consistency checks.
 */
@DisplayName("RaftLog Tests")
class RaftLogTest {

    private RaftLog raftLog;

    @BeforeEach
    void setUp() {
        raftLog = new RaftLog();
    }

    @Nested
    @DisplayName("Append Operations")
    class AppendOperations {

        @Test
        @DisplayName("Should append entry with correct index starting from 1")
        void shouldAppendWithCorrectIndex() {
            LogEntry entry1 = raftLog.append(1, "PAYMENT:user1:user2:100:USD");
            LogEntry entry2 = raftLog.append(1, "PAYMENT:user3:user4:200:USD");

            assertEquals(1, entry1.getIndex());
            assertEquals(2, entry2.getIndex());
            assertEquals(2, raftLog.size());
        }

        @Test
        @DisplayName("Should store term correctly in appended entries")
        void shouldStoreTermCorrectly() {
            raftLog.append(1, "cmd1");
            raftLog.append(2, "cmd2");
            raftLog.append(2, "cmd3");

            assertEquals(1, raftLog.getTermAt(1));
            assertEquals(2, raftLog.getTermAt(2));
            assertEquals(2, raftLog.getTermAt(3));
        }

        @Test
        @DisplayName("Should return correct last index and term")
        void shouldReturnCorrectLastIndexAndTerm() {
            assertEquals(0, raftLog.getLastIndex());
            assertEquals(0, raftLog.getLastTerm());

            raftLog.append(1, "cmd1");
            assertEquals(1, raftLog.getLastIndex());
            assertEquals(1, raftLog.getLastTerm());

            raftLog.append(3, "cmd2");
            assertEquals(2, raftLog.getLastIndex());
            assertEquals(3, raftLog.getLastTerm());
        }
    }

    @Nested
    @DisplayName("Get Entry Operations")
    class GetEntryOperations {

        @Test
        @DisplayName("Should retrieve entry by valid index")
        void shouldRetrieveEntryByIndex() {
            raftLog.append(1, "payment1");
            raftLog.append(2, "payment2");

            LogEntry entry = raftLog.getEntry(1);
            assertNotNull(entry);
            assertEquals("payment1", entry.getCommand());
            assertEquals(1, entry.getTerm());
        }

        @Test
        @DisplayName("Should return null for invalid index")
        void shouldReturnNullForInvalidIndex() {
            raftLog.append(1, "cmd1");

            assertNull(raftLog.getEntry(0));  // Index 0 is invalid
            assertNull(raftLog.getEntry(-1)); // Negative index
            assertNull(raftLog.getEntry(2));  // Beyond log size
        }

        @Test
        @DisplayName("Should return 0 for term at invalid index")
        void shouldReturnZeroForTermAtInvalidIndex() {
            raftLog.append(1, "cmd1");

            assertEquals(0, raftLog.getTermAt(0));
            assertEquals(0, raftLog.getTermAt(-1));
            assertEquals(0, raftLog.getTermAt(100));
        }
    }

    @Nested
    @DisplayName("Truncate Operations")
    class TruncateOperations {

        @Test
        @DisplayName("Should truncate log from specified index")
        void shouldTruncateFromIndex() {
            raftLog.append(1, "cmd1");
            raftLog.append(1, "cmd2");
            raftLog.append(2, "cmd3");
            raftLog.append(2, "cmd4");

            raftLog.truncateFrom(3);

            assertEquals(2, raftLog.size());
            assertNotNull(raftLog.getEntry(2));
            assertNull(raftLog.getEntry(3));
        }

        @Test
        @DisplayName("Should handle truncate at beginning")
        void shouldTruncateFromBeginning() {
            raftLog.append(1, "cmd1");
            raftLog.append(1, "cmd2");

            raftLog.truncateFrom(1);

            assertEquals(0, raftLog.size());
        }

        @Test
        @DisplayName("Should ignore truncate with invalid index")
        void shouldIgnoreInvalidTruncate() {
            raftLog.append(1, "cmd1");
            raftLog.append(1, "cmd2");

            raftLog.truncateFrom(0);
            assertEquals(2, raftLog.size());

            raftLog.truncateFrom(10);
            assertEquals(2, raftLog.size());
        }
    }

    @Nested
    @DisplayName("AppendAt Operations (Leader Replication)")
    class AppendAtOperations {

        @Test
        @DisplayName("Should append entry at next expected index")
        void shouldAppendAtNextIndex() {
            raftLog.append(1, "cmd1");

            LogEntry newEntry = new LogEntry(1, 2, "cmd2");
            raftLog.appendAt(newEntry);

            assertEquals(2, raftLog.size());
            assertEquals("cmd2", raftLog.getEntry(2).getCommand());
        }

        @Test
        @DisplayName("Should replace conflicting entry with different term")
        void shouldReplaceConflictingEntry() {
            raftLog.append(1, "cmd1");
            raftLog.append(1, "old_cmd2"); // term 1
            raftLog.append(1, "old_cmd3"); // will be truncated

            // Leader sends entry at index 2 with term 2 (conflict!)
            LogEntry newEntry = new LogEntry(2, 2, "new_cmd2");
            raftLog.appendAt(newEntry);

            assertEquals(2, raftLog.size()); // old_cmd3 truncated
            assertEquals(2, raftLog.getEntry(2).getTerm()); // new term
            assertEquals("new_cmd2", raftLog.getEntry(2).getCommand());
        }

        @Test
        @DisplayName("Should not replace entry with same term (idempotent)")
        void shouldBeIdempotent() {
            raftLog.append(1, "cmd1");
            raftLog.append(2, "cmd2");

            // Same term at same index - no change
            LogEntry sameEntry = new LogEntry(2, 2, "cmd2");
            raftLog.appendAt(sameEntry);

            assertEquals(2, raftLog.size());
        }
    }

    @Nested
    @DisplayName("Get Entries Range")
    class GetEntriesRange {

        @Test
        @DisplayName("Should return entries in range")
        void shouldReturnEntriesInRange() {
            raftLog.append(1, "cmd1");
            raftLog.append(1, "cmd2");
            raftLog.append(2, "cmd3");
            raftLog.append(2, "cmd4");

            List<LogEntry> entries = raftLog.getEntries(2, 4);

            assertEquals(3, entries.size());
            assertEquals("cmd2", entries.get(0).getCommand());
            assertEquals("cmd4", entries.get(2).getCommand());
        }

        @Test
        @DisplayName("Should return empty list for invalid start index")
        void shouldReturnEmptyForInvalidStart() {
            raftLog.append(1, "cmd1");

            assertTrue(raftLog.getEntries(0, 1).isEmpty());
            assertTrue(raftLog.getEntries(-1, 1).isEmpty());
            assertTrue(raftLog.getEntries(10, 15).isEmpty());
        }

        @Test
        @DisplayName("Should clamp end index to log size")
        void shouldClampEndIndex() {
            raftLog.append(1, "cmd1");
            raftLog.append(1, "cmd2");

            List<LogEntry> entries = raftLog.getEntries(1, 100);

            assertEquals(2, entries.size());
        }
    }

    @Nested
    @DisplayName("Commit Index Operations")
    class CommitIndexOperations {

        @Test
        @DisplayName("Should track commit index correctly")
        void shouldTrackCommitIndex() {
            assertEquals(0, raftLog.getCommitIndex());

            raftLog.setCommitIndex(5);
            assertEquals(5, raftLog.getCommitIndex());
        }

        @Test
        @DisplayName("Commit index should be independent of log size")
        void commitIndexIndependentOfLogSize() {
            raftLog.append(1, "cmd1");
            raftLog.setCommitIndex(1);

            assertEquals(1, raftLog.getCommitIndex());
            assertEquals(1, raftLog.size());
        }
    }

    @Nested
    @DisplayName("Thread Safety")
    class ThreadSafety {

        @Test
        @DisplayName("Should handle concurrent appends")
        void shouldHandleConcurrentAppends() throws InterruptedException {
            int threadCount = 10;
            int appendsPerThread = 100;
            Thread[] threads = new Thread[threadCount];

            for (int i = 0; i < threadCount; i++) {
                final int threadId = i;
                threads[i] = new Thread(() -> {
                    for (int j = 0; j < appendsPerThread; j++) {
                        raftLog.append(1, "thread" + threadId + "_cmd" + j);
                    }
                });
            }

            for (Thread t : threads) t.start();
            for (Thread t : threads) t.join();

            assertEquals(threadCount * appendsPerThread, raftLog.size());
        }
    }

    @Nested
    @DisplayName("Payment Command Scenarios")
    class PaymentScenarios {

        @Test
        @DisplayName("Should store payment commands correctly")
        void shouldStorePaymentCommands() {
            String payment1 = "PAYMENT:user123:merchant456:99.99:USD";
            String payment2 = "PAYMENT:user789:merchant012:50.00:EUR";

            raftLog.append(1, payment1);
            raftLog.append(1, payment2);

            assertEquals(payment1, raftLog.getEntry(1).getCommand());
            assertEquals(payment2, raftLog.getEntry(2).getCommand());
        }

        @Test
        @DisplayName("Should maintain payment order after truncation")
        void shouldMaintainOrderAfterTruncation() {
            raftLog.append(1, "PAYMENT:A:B:100:USD");
            raftLog.append(1, "PAYMENT:C:D:200:USD"); // will conflict
            raftLog.append(1, "PAYMENT:E:F:300:USD"); // will be truncated

            // Simulate leader overwriting with different term
            LogEntry leaderEntry = new LogEntry(2, 2, "PAYMENT:X:Y:500:USD");
            raftLog.appendAt(leaderEntry);

            assertEquals(2, raftLog.size());
            assertEquals("PAYMENT:A:B:100:USD", raftLog.getEntry(1).getCommand());
            assertEquals("PAYMENT:X:Y:500:USD", raftLog.getEntry(2).getCommand());
        }
    }
}
