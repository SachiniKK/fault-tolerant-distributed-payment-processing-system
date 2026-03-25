package com.payment.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration for all module endpoints.
 */
@Configuration
@ConfigurationProperties(prefix = "modules")
public class ModuleConfig {

    private ModuleEndpoints faultTolerance;
    private ModuleEndpoints consensus;
    private ModuleEndpoints timeSync;
    private ModuleEndpoints replication;

    public static class ModuleEndpoints {
        private List<String> urls;

        public List<String> getUrls() {
            return urls;
        }

        public void setUrls(List<String> urls) {
            this.urls = urls;
        }
    }

    public ModuleEndpoints getFaultTolerance() {
        return faultTolerance;
    }

    public void setFaultTolerance(ModuleEndpoints faultTolerance) {
        this.faultTolerance = faultTolerance;
    }

    public ModuleEndpoints getConsensus() {
        return consensus;
    }

    public void setConsensus(ModuleEndpoints consensus) {
        this.consensus = consensus;
    }

    public ModuleEndpoints getTimeSync() {
        return timeSync;
    }

    public void setTimeSync(ModuleEndpoints timeSync) {
        this.timeSync = timeSync;
    }

    public ModuleEndpoints getReplication() {
        return replication;
    }

    public void setReplication(ModuleEndpoints replication) {
        this.replication = replication;
    }
}
