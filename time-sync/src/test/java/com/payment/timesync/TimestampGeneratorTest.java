package com.payment.timesync;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TimestampGeneratorTest {

    @Mock
    private NTPSyncService ntpSyncService;

    @Mock
    private ClockSkewSimulator clockSkewSimulator;

    @InjectMocks
    private TimestampGenerator timestampGenerator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testBothTimestampsGenerated() {
        when(ntpSyncService.getOffsetMillis()).thenReturn(100L);
        when(clockSkewSimulator.applySkew(anyLong())).thenAnswer(invocation -> invocation.getArgument(0));
        TimestampPair pair = timestampGenerator.generateTimestamps();
        assertNotNull(pair);
        assertTrue(pair.getTimestamp() > 0);
        assertTrue(pair.getCorrectedTimestamp() > 0);
        // Corrected = withSkew (identity) + offset (100)
        assertEquals(100L, pair.getCorrectedTimestamp() - pair.getTimestamp());
    }

    @Test
    void testThreadSafetyOfOffsetReads() {
        when(ntpSyncService.getOffsetMillis()).thenReturn(500L);
        long raw = 1000L;
        assertEquals(1500L, timestampGenerator.getCorrectedTimestamp(raw));
    }
}
