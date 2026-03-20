package com.payment.ft;
 
import com.payment.ft.config.AppConfig;
import com.payment.ft.detection.HeartbeatMonitor;
import com.payment.ft.failover.FailoverRouter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
 
import java.util.List;
 
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
 
class FailoverRouterTest {
 
    @Mock private AppConfig config;
    @Mock private HeartbeatMonitor heartbeatMonitor;
    @Mock private RestTemplate restTemplate;
 
    @InjectMocks
    private FailoverRouter router;
 
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }
 
    @Test
    void returnsServiceUnavailableWhenNoHealthyNodes() {
        // Mock scenario: zero nodes are UP
        when(heartbeatMonitor.getHealthyNodes()).thenReturn(List.of());
        
        ResponseEntity<String> response = router.route("/payment", "{}");
        
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
    }
 
    @Test
    void routesToHealthyNodeSuccessfully() {
        // Mock scenario: node1 is healthy
        when(heartbeatMonitor.getHealthyNodes()).thenReturn(List.of("http://node1"));
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
            .thenReturn(ResponseEntity.ok("Success"));
 
        ResponseEntity<String> response = router.route("/payment", "{}");
 
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(restTemplate).exchange(contains("http://node1"), any(), any(), eq(String.class));
    }
 
    @Test
    void failsOverWhenFirstNodeThrowsException() {
        // Mock scenario: node1 and node2 are healthy, but node1 fails during the call
        when(heartbeatMonitor.getHealthyNodes()).thenReturn(List.of("http://node1", "http://node2"));
        
        // Force failure on node1
        when(restTemplate.exchange(contains("node1"), any(), any(), eq(String.class)))
            .thenThrow(new RuntimeException("Connection timed out"));
        
        // Success on node2
        when(restTemplate.exchange(contains("node2"), any(), any(), eq(String.class)))
            .thenReturn(ResponseEntity.ok("Success from node 2"));
 
        ResponseEntity<String> response = router.route("/payment", "{}");
 
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Success from node 2", response.getBody());
        
        // Verify both were attempted
        verify(restTemplate, times(2)).exchange(anyString(), any(), any(), eq(String.class));
    }
}
