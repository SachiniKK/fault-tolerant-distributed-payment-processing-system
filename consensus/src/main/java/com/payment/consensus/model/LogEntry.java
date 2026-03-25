package com.payment.consensus.model;

import java.io.Serializable;

public class LogEntry implements Serializable {
    private static final long serialVersionUID = 1L;

    private int term;
    private int index;
    private String command;

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

    @Override
    public String toString() {
        return "LogEntry{term=" + term + ", index=" + index + ", cmd=" + command + "}";
    }
}
