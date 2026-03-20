package com.payment.consensus;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Basic smoke tests for Consensus module.
 * Verifies that Spring context loads correctly.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "app.node.id=test-node",
    "server.port=0",
    "app.peers=",
    "app.raft.election-timeout-min-ms=10000",
    "app.raft.election-timeout-max-ms=20000"
})
@DisplayName("Consensus Module Smoke Tests")
class ConsensusTests {

    @Test
    @DisplayName("Application context should load successfully")
    void contextLoads() {
        // If we get here, Spring context loaded successfully
        assertTrue(true);
    }
}
