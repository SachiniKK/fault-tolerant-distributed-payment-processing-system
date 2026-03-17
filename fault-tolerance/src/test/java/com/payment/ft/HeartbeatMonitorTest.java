package com.payment.ft;
 
import com.payment.ft.config.AppConfig;
import com.payment.ft.detection.HeartbeatMonitor;
import com.payment.ft.detection.NodeStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
 
import java.util.List;
 
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
 
class HeartbeatMonitorTest {
 
    @Mock private AppConfig config;
    @Mock private RestTemplate restTemplate;
    private HeartbeatMonitor monitor;
 
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(config.getPeerUrls()).thenReturn(List.of("http://localhost:8082"));
        when(config.getMissedThreshold()).thenReturn(3);
        monitor = new HeartbeatMonitor(config, restTemplate);
    }
 
    @Test
    void nodeStartsAsUnknown() {
        assertEquals(NodeStatus.UNKNOWN, monitor.getStatus("http://localhost:8082"));
    }
 
    @Test
    void nodeMarkedUpWhenReachable() {
        when(restTemplate.getForObject(anyString(), eq(String.class)))
            .thenReturn("OK");
        monitor.checkAllPeers();
        assertEquals(NodeStatus.UP, monitor.getStatus("http://localhost:8082"));
    }
 
    @Test
    void nodeNotDownUntilThresholdReached() {
        // Mock HTTP failure
        when(restTemplate.getForObject(anyString(), eq(String.class)))
            .thenThrow(new ResourceAccessException("Connection refused"));
 
        monitor.checkAllPeers(); // miss 1
        assertNotEquals(NodeStatus.DOWN, monitor.getStatus("http://localhost:8082"),
            "Should not be DOWN after only 1 missed beat");
 
        monitor.checkAllPeers(); // miss 2
        assertNotEquals(NodeStatus.DOWN, monitor.getStatus("http://localhost:8082"),
            "Should not be DOWN after only 2 missed beats");
 
        monitor.checkAllPeers(); // miss 3 — threshold reached
        assertEquals(NodeStatus.DOWN, monitor.getStatus("http://localhost:8082"),
            "Should be DOWN after 3 missed beats");
    }
 
    @Test
    void nodeRecoveredAfterComingBack() {
        // Fail 3 times to mark as DOWN
        when(restTemplate.getForObject(anyString(), eq(String.class)))
            .thenThrow(new ResourceAccessException("Connection refused"));
        monitor.checkAllPeers();
        monitor.checkAllPeers();
        monitor.checkAllPeers();
        assertEquals(NodeStatus.DOWN, monitor.getStatus("http://localhost:8082"));
 
        // Now the node comes back
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn("OK");
        monitor.checkAllPeers();
        assertEquals(NodeStatus.UP, monitor.getStatus("http://localhost:8082"),
            "Node should recover to UP when it starts responding again");
    }
 
    @Test
    void getHealthyNodesReturnsOnlyUpNodes() {
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn("OK");
        monitor.checkAllPeers();
        assertEquals(1, monitor.getHealthyNodes().size());
    }
}
