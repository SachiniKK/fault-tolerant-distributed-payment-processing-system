package com.payment.consensus.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.payment.consensus.model.LogEntry;

@Service
public class RaftLog {
    private static final Logger log = LoggerFactory.getLogger(RaftLog.class);

    private final List<LogEntry> entries = new ArrayList<>();
    private int commitIndex = 0;

    public synchronized LogEntry append(int term, String command) {
        int index = entries.size() + 1;
        LogEntry entry = new LogEntry(term, index, command);
        entries.add(entry);
        log.debug("[RAFT-LOG] Appended: {}", entry);
        return entry;
    }

    public synchronized LogEntry getEntry(int index) {
        if (index < 1 || index > entries.size())
            return null;
        return entries.get(index - 1);
    }

    public synchronized int getLastIndex() {
        return entries.size();
    }

    public synchronized int getLastTerm() {
        if (entries.isEmpty())
            return 0;
        return entries.get(entries.size() - 1).getTerm();
    }

    public synchronized int getTermAt(int index) {
        if (index < 1 || index > entries.size())
            return 0;
        return entries.get(index - 1).getTerm();
    }

    public synchronized void truncateFrom(int fromIndex) {
        if (fromIndex < 1 || fromIndex > entries.size())
            return;
        log.warn("[RAFT-LOG] Truncating from index {}", fromIndex);
        entries.subList(fromIndex - 1, entries.size()).clear();
    }

    public synchronized void appendAt(LogEntry entry) {
        int index = entry.getIndex();
        if (index == entries.size() + 1) {
            entries.add(entry);
        } else if (index <= entries.size()) {
            if (entries.get(index - 1).getTerm() != entry.getTerm()) {
                truncateFrom(index);
                entries.add(entry);
            }
        }
    }

    public synchronized int getCommitIndex() {
        return commitIndex;
    }

    public synchronized void setCommitIndex(int commitIndex) {
        this.commitIndex = commitIndex;
    }

    public synchronized List<LogEntry> getEntries(int startIndex, int endIndex) {
        if (startIndex < 1 || startIndex > entries.size())
            return Collections.emptyList();
        int end = Math.min(endIndex, entries.size());
        return new ArrayList<>(entries.subList(startIndex - 1, end));
    }

    public synchronized int size() {
        return entries.size();
    }
}
