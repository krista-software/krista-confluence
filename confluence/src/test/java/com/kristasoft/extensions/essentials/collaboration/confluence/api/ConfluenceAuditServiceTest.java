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

package com.kristasoft.extensions.essentials.collaboration.confluence.api;

import app.krista.extensions.util.KeyValueStore;
import app.krista.sdk.atlassian.audit.KeyValueAuditStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ConfluenceAuditService.
 *
 * <p>Key assertions:
 * <ul>
 *   <li>{@link ConfluenceAuditService#getAuditStore()} returns the same singleton instance.</li>
 *   <li>Each convenience method ({@code triggered}, {@code success}, {@code failure},
 *       {@code authFailure}) delegates to the underlying {@link KeyValueAuditStore} with
 *       the correct event type string and cause details.</li>
 *   <li>No audit method ever propagates exceptions — audit must never affect catalog
 *       execution paths.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ConfluenceAuditService Tests")
class ConfluenceAuditServiceTest {

    @Mock
    private KeyValueStore keyValueStore;

    private ConfluenceAuditService confluenceAuditService;

    @BeforeEach
    void setUp() {
        confluenceAuditService = new ConfluenceAuditService(keyValueStore);
    }

    // ── Singleton store ────────────────────────────────────────────────────

    @Test
    @DisplayName("getAuditStore returns the same non-null instance on every call")
    void testGetAuditStore_ReturnsSingletonInstance() {
        KeyValueAuditStore first = confluenceAuditService.getAuditStore();
        KeyValueAuditStore second = confluenceAuditService.getAuditStore();

        assertNotNull(first);
        assertSame(first, second);
    }

    // ── triggered() ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("triggered() Tests")
    class TriggeredTests {

        @Test
        @DisplayName("triggered() calls auditStore.record with CATALOG_REQUEST_TRIGGERED event type")
        void testTriggered_DelegatesToAuditStore() {
            try (MockedConstruction<KeyValueAuditStore> mocked = mockConstruction(KeyValueAuditStore.class)) {
                ConfluenceAuditService service = new ConfluenceAuditService(keyValueStore);

                service.triggered("invoker-1", "GetPages");

                KeyValueAuditStore store = mocked.constructed().get(0);
                verify(store).record("invoker-1", "CATALOG_REQUEST_TRIGGERED", "triggered",
                        "GetPages", null, null, null);
            }
        }

        @Test
        @DisplayName("triggered() never propagates exceptions from the audit store")
        void testTriggered_NeverThrows_WhenStoreThrows() {
            try (MockedConstruction<KeyValueAuditStore> mocked = mockConstruction(KeyValueAuditStore.class,
                    (mock, ctx) -> doThrow(new RuntimeException("store failure"))
                            .when(mock).record(any(), any(), any(), any(), any(), any(), any()))) {
                ConfluenceAuditService service = new ConfluenceAuditService(keyValueStore);

                assertDoesNotThrow(() -> service.triggered("invoker-1", "GetPages"));
            }
        }
    }

    // ── success() ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("success() Tests")
    class SuccessTests {

        @Test
        @DisplayName("success() calls auditStore.record with CATALOG_REQUEST_SUCCESS event type")
        void testSuccess_DelegatesToAuditStore() {
            try (MockedConstruction<KeyValueAuditStore> mocked = mockConstruction(KeyValueAuditStore.class)) {
                ConfluenceAuditService service = new ConfluenceAuditService(keyValueStore);

                service.success("invoker-1", "GetPages");

                KeyValueAuditStore store = mocked.constructed().get(0);
                verify(store).record("invoker-1", "CATALOG_REQUEST_SUCCESS", "success",
                        "GetPages", null, null, null);
            }
        }

        @Test
        @DisplayName("success() never propagates exceptions from the audit store")
        void testSuccess_NeverThrows_WhenStoreThrows() {
            try (MockedConstruction<KeyValueAuditStore> mocked = mockConstruction(KeyValueAuditStore.class,
                    (mock, ctx) -> doThrow(new RuntimeException("store failure"))
                            .when(mock).record(any(), any(), any(), any(), any(), any(), any()))) {
                ConfluenceAuditService service = new ConfluenceAuditService(keyValueStore);

                assertDoesNotThrow(() -> service.success("invoker-1", "GetPages"));
            }
        }
    }

    // ── failure() ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("failure() Tests")
    class FailureTests {

        @Test
        @DisplayName("failure() passes cause message and class name to auditStore.record")
        void testFailure_PassesCauseMessageAndClass() {
            RuntimeException cause = new RuntimeException("connection timeout");

            try (MockedConstruction<KeyValueAuditStore> mocked = mockConstruction(KeyValueAuditStore.class)) {
                ConfluenceAuditService service = new ConfluenceAuditService(keyValueStore);

                service.failure("invoker-1", "GetPages", cause);

                KeyValueAuditStore store = mocked.constructed().get(0);
                verify(store).record("invoker-1", "CATALOG_REQUEST_FAILURE", "failure",
                        "GetPages", "connection timeout", "RuntimeException", null);
            }
        }

        @Test
        @DisplayName("failure() handles null cause — passes null message and null class")
        void testFailure_NullCause_PassesNulls() {
            try (MockedConstruction<KeyValueAuditStore> mocked = mockConstruction(KeyValueAuditStore.class)) {
                ConfluenceAuditService service = new ConfluenceAuditService(keyValueStore);

                service.failure("invoker-1", "GetPages", null);

                KeyValueAuditStore store = mocked.constructed().get(0);
                verify(store).record("invoker-1", "CATALOG_REQUEST_FAILURE", "failure",
                        "GetPages", null, null, null);
            }
        }

        @Test
        @DisplayName("failure() never propagates exceptions from the audit store")
        void testFailure_NeverThrows_WhenStoreThrows() {
            try (MockedConstruction<KeyValueAuditStore> mocked = mockConstruction(KeyValueAuditStore.class,
                    (mock, ctx) -> doThrow(new RuntimeException("store failure"))
                            .when(mock).record(any(), any(), any(), any(), any(), any(), any()))) {
                ConfluenceAuditService service = new ConfluenceAuditService(keyValueStore);

                assertDoesNotThrow(() -> service.failure("invoker-1", "GetPages",
                        new RuntimeException("original cause")));
            }
        }
    }

    // ── authFailure() ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("authFailure() Tests")
    class AuthFailureTests {

        @Test
        @DisplayName("authFailure() delegates to auditStore.recordAuthError with invokerId, catalogName, cause")
        void testAuthFailure_DelegatesToRecordAuthError() {
            RuntimeException cause = new RuntimeException("401 Unauthorized");

            try (MockedConstruction<KeyValueAuditStore> mocked = mockConstruction(KeyValueAuditStore.class)) {
                ConfluenceAuditService service = new ConfluenceAuditService(keyValueStore);

                service.authFailure("invoker-1", "GetPages", cause);

                KeyValueAuditStore store = mocked.constructed().get(0);
                verify(store).recordAuthError("invoker-1", "GetPages", cause);
            }
        }

        @Test
        @DisplayName("authFailure() never propagates exceptions from the audit store")
        void testAuthFailure_NeverThrows_WhenStoreThrows() {
            try (MockedConstruction<KeyValueAuditStore> mocked = mockConstruction(KeyValueAuditStore.class,
                    (mock, ctx) -> doThrow(new RuntimeException("store failure"))
                            .when(mock).recordAuthError(any(), any(), any()))) {
                ConfluenceAuditService service = new ConfluenceAuditService(keyValueStore);

                assertDoesNotThrow(() -> service.authFailure("invoker-1", "GetPages",
                        new RuntimeException("auth cause")));
            }
        }
    }
}
