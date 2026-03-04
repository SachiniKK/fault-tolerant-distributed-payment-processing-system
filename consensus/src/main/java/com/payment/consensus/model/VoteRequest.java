package com.payment.consensus.model;

import java.io.Serializable;

public class VoteRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private int term;
    private String candidateId;
    private int lastLogIndex;
    private int lastLogTerm;

    public VoteRequest() {
    }

    public VoteRequest(int term, String candidateId, int lastLogIndex, int lastLogTerm) {
        this.term = term;
        this.candidateId = candidateId;
        this.lastLogIndex = lastLogIndex;
        this.lastLogTerm = lastLogTerm;
    }

    public int getTerm() {
        return term;
    }

    public String getCandidateId() {
        return candidateId;
    }

    public int getLastLogIndex() {
        return lastLogIndex;
    }

    public int getLastLogTerm() {
        return lastLogTerm;
    }

    public void setTerm(int term) {
        this.term = term;
    }

    public void setCandidateId(String candidateId) {
        this.candidateId = candidateId;
    }

    public void setLastLogIndex(int lastLogIndex) {
        this.lastLogIndex = lastLogIndex;
    }

    public void setLastLogTerm(int lastLogTerm) {
        this.lastLogTerm = lastLogTerm;
    }
}
