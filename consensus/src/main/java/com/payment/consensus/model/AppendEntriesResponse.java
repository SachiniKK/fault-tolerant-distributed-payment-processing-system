package com.payment.consensus.model;

import java.io.Serializable;

public class AppendEntriesResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private int term;
    private boolean success;

    public AppendEntriesResponse() {
    }

    public AppendEntriesResponse(int term, boolean success) {
        this.term = term;
        this.success = success;
    }

    public int getTerm() {
        return term;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setTerm(int term) {
        this.term = term;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}