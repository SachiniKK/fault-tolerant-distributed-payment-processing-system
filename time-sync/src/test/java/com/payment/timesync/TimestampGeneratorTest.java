package com.payment.timesync;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TimestampGenerator.
 * Tests dual timestamp generation, offset application, and thread safety.
 *
 * Uses real NTPSyncService instances with updateOffset() to set known offsets
 * instead of Mockito mocks, avoiding ByteBuddy/Java 24 compatibility issues.
 * Follows the same @Nested and @DisplayName style as RaftLogTest.
 */
@DisplayName("TimestampGenerator Tests")
class TimestampGeneratorTest {

    private NTPSyncService ntpSyncService;
    private TimestampGenerator timestampGenerator;

    @BeforeEach
    void setUp() {
        // Use real NTPSyncService — set known offset directly via updateOffset()
        ntpSyncService = new NTPSyncService();
        timestampGenerator = new TimestampGenerator(ntpSyncService);
    }

    @Nested
    @DisplayName("Dual Timestamp Generation")
    class DualTimestampGeneration {

        @Test
        @DisplayName("Should generate both raw and corrected timestamps")
        void shouldGenerateBothTimestamps() {
            ntpSyncService.updateOffset(50);

            TimestampPair pair = timestampGenerator.generateTimestamps();

            assertNotNull(pair, "TimestampPair should not be null");
            assertTrue(pair.getTimestamp() > 0, "Raw timestamp should be positive");
            assertTrue(pair.getCorrectedTimestamp() > 0, "Corrected timestamp should be positive");
        }

        @Test
        @DisplayName("Should have corrected timestamp greater than raw when offset is positive")
        void shouldHaveCorrectedGreaterThanRawWhenOffsetPositive() {
            // Positive offset means local clock is behind NTP time
            ntpSyncService.updateOffset(100);

            TimestampPair pair = timestampGenerator.generateTimestamps();

            assertTrue(pair.getCorrectedTimestamp() > pair.getTimestamp(),
                    "Corrected should be > raw when offset is positive (local clock behind)");
            assertEquals(100, pair.getCorrectedTimestamp() - pair.getTimestamp(),
                    "Difference should equal the offset");
        }

        @Test
        @DisplayName("Should have corrected timestamp less than raw when offset is negative")
        void shouldHaveCorrectedLessThanRawWhenOffsetNegative() {
            // Negative offset means local clock is ahead of NTP time
            ntpSyncService.updateOffset(-50);

            TimestampPair pair = timestampGenerator.generateTimestamps();

            assertTrue(pair.getCorrectedTimestamp() < pair.getTimestamp(),
                    "Corrected should be < raw when offset is negative (local clock ahead)");
            assertEquals(-50, pair.getCorrectedTimestamp() - pair.getTimestamp(),
                    "Difference should equal the negative offset");
        }

        @Test
        @DisplayName("Should have equal timestamps when offset is zero")
        void shouldHaveEqualTimestampsWhenOffsetZero() {
            ntpSyncService.updateOffset(0);

            TimestampPair pair = timestampGenerator.generateTimestamps();

            assertEquals(pair.getTimestamp(), pair.getCorrectedTimestamp(),
                    "Raw and corrected should be equal when offset is 0");
        }
    }

    @Nested
    @DisplayName("Offset Application")
    class OffsetApplication {

        @Test
        @DisplayName("Should apply positive offset correctly to specific timestamp")
        void shouldApplyPositiveOffsetCorrectly() {
            ntpSyncService.updateOffset(200);

            long raw = 1000000L;
            long corrected = timestampGenerator.getCorrectedTimestamp(raw);

            assertEquals(1000200L, corrected,
                    "Corrected = raw + offset (1000000 + 200 = 1000200)");
        }

        @Test
        @DisplayName("Should apply negative offset correctly to specific timestamp")
        void shouldApplyNegativeOffsetCorrectly() {
            ntpSyncService.updateOffset(-300);

            long raw = 1000000L;
            long corrected = timestampGenerator.getCorrectedTimestamp(raw);

            assertEquals(999700L, corrected,
                    "Corrected = raw + offset (1000000 + (-300) = 999700)");
        }

        @Test
        @DisplayName("Should apply zero offset (no change)")
        void shouldApplyZeroOffset() {
            ntpSyncService.updateOffset(0);

            long raw = 1000000L;
            long corrected = timestampGenerator.getCorrectedTimestamp(raw);

            assertEquals(raw, corrected,
                    "With zero offset, corrected should equal raw");
        }

        @Test
        @DisplayName("Should apply large offset values")
        void shouldApplyLargeOffset() {
            long largeOffset = 86400000L; // 24 hours
            ntpSyncService.updateOffset(largeOffset);

            long raw = 1000000L;
            long corrected = timestampGenerator.getCorrectedTimestamp(raw);

            assertEquals(raw + largeOffset, corrected,
                    "Should correctly handle large offset values");
        }
    }

    @Nested
    @DisplayName("Raw Timestamp")
    class RawTimestamp {

        @Test
        @DisplayName("Should return current system time for raw timestamp")
        void shouldReturnCurrentSystemTime() {
            long before = System.currentTimeMillis();
            long raw = timestampGenerator.getRawTimestamp();
            long after = System.currentTimeMillis();

            assertTrue(raw >= before && raw <= after,
                    "Raw timestamp should be between before and after System.currentTimeMillis()");
        }

        @Test
        @DisplayName("Should return increasing raw timestamps on successive calls")
        void shouldReturnIncreasingRawTimestamps() {
            long first = timestampGenerator.getRawTimestamp();
            long second = timestampGenerator.getRawTimestamp();

            assertTrue(second >= first,
                    "Raw timestamps should be monotonically non-decreasing");
        }
    }

    @Nested
    @DisplayName("TimestampPair Properties")
    class TimestampPairProperties {

        @Test
        @DisplayName("Should calculate difference correctly in TimestampPair")
        void shouldCalculateDifferenceCorrectly() {
            TimestampPair pair = new TimestampPair(1000, 1050);

            assertEquals(50, pair.getDifferenceMillis(),
                    "Difference should be corrected - raw = 50");
        }

        @Test
        @DisplayName("Should handle negative difference in TimestampPair")
        void shouldHandleNegativeDifference() {
            TimestampPair pair = new TimestampPair(1050, 1000);

            assertEquals(-50, pair.getDifferenceMillis(),
                    "Negative difference when corrected < raw");
        }

        @Test
        @DisplayName("Should have sensible toString output")
        void shouldHaveSensibleToString() {
            TimestampPair pair = new TimestampPair(1000, 1050);
            String str = pair.toString();

            assertTrue(str.contains("1000"), "toString should contain raw timestamp");
            assertTrue(str.contains("1050"), "toString should contain corrected timestamp");
            assertTrue(str.contains("50"), "toString should contain difference");
        }

        @Test
        @DisplayName("Should support setter methods")
        void shouldSupportSetters() {
            TimestampPair pair = new TimestampPair();
            pair.setTimestamp(2000);
            pair.setCorrectedTimestamp(2100);

            assertEquals(2000, pair.getTimestamp());
            assertEquals(2100, pair.getCorrectedTimestamp());
            assertEquals(100, pair.getDifferenceMillis());
        }
    }

    @Nested
    @DisplayName("Thread Safety of Offset Reads")
    class ThreadSafetyOfOffsetReads {

        @Test
        @DisplayName("Should read offset safely from multiple threads")
        void shouldReadOffsetSafelyFromMultipleThreads() throws InterruptedException {
            ntpSyncService.updateOffset(42);

            int threadCount = 20;
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicBoolean allCorrect = new AtomicBoolean(true);

            for (int i = 0; i < threadCount; i++) {
                new Thread(() -> {
                    try {
                        TimestampPair pair = timestampGenerator.generateTimestamps();
                        long diff = pair.getCorrectedTimestamp() - pair.getTimestamp();
                        if (diff != 42) {
                            allCorrect.set(false);
                        }
                    } finally {
                        latch.countDown();
                    }
                }).start();
            }

            latch.await();

            assertTrue(allCorrect.get(),
                    "All threads should read the same offset value (42ms)");
        }

        @Test
        @DisplayName("Should handle offset changing between reads")
        void shouldHandleOffsetChangingBetweenReads() {
            ntpSyncService.updateOffset(100);
            TimestampPair pair1 = timestampGenerator.generateTimestamps();

            ntpSyncService.updateOffset(200);
            TimestampPair pair2 = timestampGenerator.generateTimestamps();

            assertEquals(100, pair1.getCorrectedTimestamp() - pair1.getTimestamp(),
                    "First pair should use offset 100");
            assertEquals(200, pair2.getCorrectedTimestamp() - pair2.getTimestamp(),
                    "Second pair should use updated offset 200");
        }
    }

    @Nested
    @DisplayName("Integration Readiness")
    class IntegrationReadiness {

        @Test
        @DisplayName("Should provide getCurrentOffset for diagnostics")
        void shouldProvideCurrentOffset() {
            ntpSyncService.updateOffset(77);

            assertEquals(77, timestampGenerator.getCurrentOffset(),
                    "getCurrentOffset should return the NTP offset");
        }

        @Test
        @DisplayName("Should generate timestamps suitable for Payment entity")
        void shouldGenerateTimestampsSuitableForPayment() {
            ntpSyncService.updateOffset(50);

            TimestampPair pair = timestampGenerator.generateTimestamps();

            // Verify timestamps are realistic (near current time)
            long now = System.currentTimeMillis();
            assertTrue(Math.abs(pair.getTimestamp() - now) < 1000,
                    "Raw timestamp should be within 1s of current time");
            assertTrue(Math.abs(pair.getCorrectedTimestamp() - (now + 50)) < 1000,
                    "Corrected timestamp should be within 1s of current time + offset");
        }
    }
}
