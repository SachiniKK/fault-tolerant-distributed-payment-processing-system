package com.payment.consensus.model;

import java.io.Serializable;
import java.util.List;

public class AppendEntriesRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private int term;
    private String leaderId;
    private int prevLogIndex;
    private int prevLogTerm;
    private List<LogEntry> entries;
    private int leaderCommit;

    public AppendEntriesRequest() {
    }

    public AppendEntriesRequest(int term, String leaderId, int prevLogIndex, int prevLogTerm, List<LogEntry> entries, int leaderCommit) {
        this.term = term;
        this.leaderId = leaderId;
        this.prevLogIndex = prevLogIndex;
        this.prevLogTerm = prevLogTerm;
        this.entries = entries;
        this.leaderCommit = leaderCommit;
    }

    public int getTerm() {
        return term;
    }

    public String getLeaderId() {
        return leaderId;
    }

    public int getPrevLogIndex() {
        return prevLogIndex;
    }

    public int getPrevLogTerm() {
        return prevLogTerm;
    }

    public List<LogEntry> getEntries() {
        return entries;
    }

    public int getLeaderCommit() {
        return leaderCommit;
    }

    public void setTerm(int term) {
        this.term = term;
    }

    public void setLeaderId(String leaderId) {
        this.leaderId = leaderId;
    }

    public void setPrevLogIndex(int prevLogIndex) {
        this.prevLogIndex = prevLogIndex;
    }

    public void setPrevLogTerm(int prevLogTerm) {
        this.prevLogTerm = prevLogTerm;
    }

    public void setEntries(List<LogEntry> entries) {
        this.entries = entries;
    }

    public void setLeaderCommit(int leaderCommit) {
        this.leaderCommit = leaderCommit;
    }
}
