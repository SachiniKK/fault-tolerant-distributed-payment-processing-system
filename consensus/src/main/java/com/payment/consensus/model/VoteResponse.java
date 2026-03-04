package com.payment.consensus.model;

import java.io.Serializable;

public class VoteResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private int term;
    private boolean voteGranted;

    public VoteResponse() {
    }

    public VoteResponse(int term, boolean voteGranted) {
        this.term = term;
        this.voteGranted = voteGranted;
    }

    public int getTerm() {
        return term;
    }

    public boolean isVoteGranted() {
        return voteGranted;
    }

    public void setTerm(int term) {
        this.term = term;
    }

    public void setVoteGranted(boolean voteGranted) {
        this.voteGranted = voteGranted;
    }
}
