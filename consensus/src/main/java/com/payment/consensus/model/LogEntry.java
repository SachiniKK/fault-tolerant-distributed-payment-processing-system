package com.payment.consensus.model;

import java.io.Serializable;

public class LogEntry implements Serializable {
    private static final long serialVersionUID = 1L;

    private int term;
    private int index;
    private String command;
    private long timestamp;           // Raw system time (Member 3)
    private long correctedTimestamp;  // NTP-corrected time (Member 3)

    public LogEntry() {
    }

    public LogEntry(int term, int index, String command) {
        this.term = term;
        this.index = index;
        this.command = command;
    }

    public int getTerm() {
        return term;
    }

    public int getIndex() {
        return index;
    }

    public String getCommand() {
        return command;
    }

    public void setTerm(int term) {
        this.term = term;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getCorrectedTimestamp() {
        return correctedTimestamp;
    }

    public void setCorrectedTimestamp(long correctedTimestamp) {
        this.correctedTimestamp = correctedTimestamp;
    }

    @Override
    public String toString() {
        return "LogEntry{term=" + term + ", index=" + index + ", cmd=" + command + "}";
    }
}
