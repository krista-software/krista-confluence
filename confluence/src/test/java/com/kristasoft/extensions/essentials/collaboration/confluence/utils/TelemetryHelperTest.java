/*
 * Confluence Extension for Krista
 * Copyright (C) 2025 Krista Software
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.kristasoft.extensions.essentials.collaboration.confluence.utils;

import app.krista.ksdk.telemetry.TelemetryMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TelemetryHelper")
class TelemetryHelperTest {

    @Mock
    private TelemetryMetrics telemetry;

    private TelemetryHelper helper;

    @BeforeEach
    void setUp() {
        helper = new TelemetryHelper(telemetry);
    }

    // ──────────────────────────────────────────────────────────────
    //  recordSuccess
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("recordSuccess")
    class RecordSuccess {

        @Test
        @DisplayName("should increment success counter and record duration")
        void incrementsCounterAndRecordsDuration() {
            long startTime = System.currentTimeMillis() - 100;
            Map<String, String> tags = Map.of("action", "create");

            helper.recordSuccess("confluence.page", startTime, tags);

            verify(telemetry).incrementCounter(eq("confluence.page.success"), eq(1L));
            verify(telemetry).recordDuration(eq("confluence.page.duration"), anyLong(), argThat(map ->
                    "success".equals(map.get("status")) && "create".equals(map.get("action"))));
        }

        @Test
        @DisplayName("should handle null additional tags")
        void nullAdditionalTags() {
            long startTime = System.currentTimeMillis();

            assertDoesNotThrow(() -> helper.recordSuccess("metric", startTime, null));

            verify(telemetry).incrementCounter(eq("metric.success"), eq(1L));
            verify(telemetry).recordDuration(eq("metric.duration"), anyLong(), argThat(map ->
                    "success".equals(map.get("status"))));
        }

        @Test
        @DisplayName("should sanitize null values in tags to NA")
        void sanitizesNullTagValues() {
            long startTime = System.currentTimeMillis();
            Map<String, String> tags = new HashMap<>();
            tags.put("key", null);

            helper.recordSuccess("metric", startTime, tags);

            verify(telemetry).recordDuration(eq("metric.duration"), anyLong(), argThat(map ->
                    "NA".equals(map.get("key"))));
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  recordRetryPrompted
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("recordRetryPrompted")
    class RecordRetryPrompted {

        @Test
        @DisplayName("should increment retry_prompted counter and record duration")
        void incrementsCounterAndRecordsDuration() {
            long startTime = System.currentTimeMillis() - 50;
            Map<String, String> tags = Map.of("attempt", "2");

            helper.recordRetryPrompted("confluence.page", startTime, tags);

            verify(telemetry).incrementCounter(eq("confluence.page.retry_prompted"), eq(1L));
            verify(telemetry).recordDuration(eq("confluence.page.duration"), anyLong(), argThat(map ->
                    "retry_prompted".equals(map.get("status"))));
        }

        @Test
        @DisplayName("should handle null tags")
        void nullTags() {
            long startTime = System.currentTimeMillis();

            assertDoesNotThrow(() -> helper.recordRetryPrompted("metric", startTime, null));

            verify(telemetry).incrementCounter(eq("metric.retry_prompted"), eq(1L));
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  recordValidationError
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("recordValidationError")
    class RecordValidationError {

        @Test
        @DisplayName("should increment mustAuthorizeException counter and include error details")
        void recordsValidationError() {
            long startTime = System.currentTimeMillis();

            helper.recordValidationError("confluence.page", startTime, "Invalid space ID", Map.of("field", "spaceId"));

            verify(telemetry).incrementCounter(eq("confluence.page.mustAuthorizeException"), eq(1L));
            verify(telemetry).recordDuration(eq("confluence.page.duration"), anyLong(), argThat(map ->
                    "validation_error".equals(map.get("status"))
                            && "validation".equals(map.get("error_type"))
                            && "Invalid space ID".equals(map.get("error_message"))));
        }

        @Test
        @DisplayName("should use 'Unknown error' when errorMessage is null")
        void nullErrorMessage() {
            long startTime = System.currentTimeMillis();

            helper.recordValidationError("metric", startTime, null, Map.of());

            verify(telemetry).recordDuration(eq("metric.duration"), anyLong(), argThat(map ->
                    "Unknown error".equals(map.get("error_message"))));
        }

        @Test
        @DisplayName("should handle null tags")
        void nullTags() {
            long startTime = System.currentTimeMillis();

            assertDoesNotThrow(() -> helper.recordValidationError("metric", startTime, "err", null));
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  recordError
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("recordError")
    class RecordError {

        @Test
        @DisplayName("should increment error counter and include exception details")
        void recordsError() {
            long startTime = System.currentTimeMillis();
            RuntimeException ex = new RuntimeException("Something broke");

            helper.recordError("confluence.page", startTime, ex, Map.of("operation", "update"));

            verify(telemetry).incrementCounter(eq("confluence.page.error"), eq(1L));
            verify(telemetry).recordDuration(eq("confluence.page.duration"), anyLong(), argThat(map ->
                    "error".equals(map.get("status"))
                            && "general".equals(map.get("error_type"))
                            && "Something broke".equals(map.get("error_message"))
                            && "RuntimeException".equals(map.get("error_class"))));
        }

        @Test
        @DisplayName("should use 'Unknown error' when exception message is null")
        void nullExceptionMessage() {
            long startTime = System.currentTimeMillis();
            RuntimeException ex = new RuntimeException((String) null);

            helper.recordError("metric", startTime, ex, Map.of());

            verify(telemetry).recordDuration(eq("metric.duration"), anyLong(), argThat(map ->
                    "Unknown error".equals(map.get("error_message"))));
        }

        @Test
        @DisplayName("should handle null tags")
        void nullTags() {
            long startTime = System.currentTimeMillis();
            Exception ex = new Exception("fail");

            assertDoesNotThrow(() -> helper.recordError("metric", startTime, ex, null));
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  incrementCount
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("incrementCount")
    class IncrementCount {

        @Test
        @DisplayName("should increment counter with .count suffix")
        void incrementsWithCountSuffix() {
            helper.incrementCount("confluence.page");

            verify(telemetry).incrementCounter(eq("confluence.page.count"), eq(1L));
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  safeTagMap (static)
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("safeTagMap")
    class SafeTagMap {

        @Test
        @DisplayName("should create a map from key-value pairs")
        void createsMap() {
            Map<String, String> result = TelemetryHelper.safeTagMap("k1", "v1", "k2", "v2");

            assertEquals(2, result.size());
            assertEquals("v1", result.get("k1"));
            assertEquals("v2", result.get("k2"));
        }

        @Test
        @DisplayName("should replace null values with NA")
        void nullValuesBecomNA() {
            Map<String, String> result = TelemetryHelper.safeTagMap("key", null);

            assertEquals("NA", result.get("key"));
        }

        @Test
        @DisplayName("should return empty map for no arguments")
        void emptyArguments() {
            Map<String, String> result = TelemetryHelper.safeTagMap();

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should ignore a trailing key without a value")
        void oddNumberOfArgs() {
            Map<String, String> result = TelemetryHelper.safeTagMap("k1", "v1", "orphan");

            assertEquals(1, result.size());
            assertEquals("v1", result.get("k1"));
            assertNull(result.get("orphan"), "orphan key without value pair should not be included");
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  recordCounter
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("recordCounter")
    class RecordCounter {

        @Test
        @DisplayName("should delegate to telemetry.incrementCounter with value and tags")
        void delegatesToTelemetry() {
            Map<String, String> tags = Map.of("type", "page");

            helper.recordCounter("confluence.requests", 5L, tags);

            verify(telemetry).incrementCounter("confluence.requests", 5L, tags);
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  recordGauge
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("recordGauge")
    class RecordGauge {

        @Test
        @DisplayName("should delegate to telemetry.observeGauge with value")
        void delegatesToTelemetry() {
            helper.recordGauge("confluence.active_sessions", 42.0);

            verify(telemetry).observeGauge("confluence.active_sessions", 42.0);
        }
    }
}
