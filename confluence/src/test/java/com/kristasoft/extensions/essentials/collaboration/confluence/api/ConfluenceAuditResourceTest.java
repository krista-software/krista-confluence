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

import app.krista.ksdk.context.RequestContext;
import app.krista.sdk.atlassian.audit.AuditData;
import app.krista.sdk.atlassian.audit.KeyValueAuditStore;
import app.krista.sdk.atlassian.config.AuthMethod;
import com.kristasoft.extensions.essentials.collaboration.confluence.ConfluenceAttributes;
import com.kristasoft.extensions.essentials.collaboration.confluence.authentication.SessionManager;
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
import static org.mockito.Mockito.*;

/**
 * Unit tests for ConfluenceAuditResource.
 *
 * <p>Key assertions:
 * <ul>
 *   <li>Constructor uses the shared {@link KeyValueAuditStore} from
 *       {@link ConfluenceAuditService#getAuditStore()} — not a newly created instance.</li>
 *   <li>Delegation to the store is correct for all audit operations.</li>
 *   <li>{@code buildHealthResponse()} reports the correct auth method for every
 *       credential configuration (BASIC, OAUTH2 3LO, OAUTH2_CLIENT_CREDENTIALS,
 *       not-configured, and exception path).</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ConfluenceAuditResource Tests")
class ConfluenceAuditResourceTest {

    @Mock
    private ConfluenceAuditService confluenceAuditService;

    @Mock
    private RequestContext requestContext;

    @Mock
    private ConfluenceAttributes confluenceAttributes;

    @Mock
    private SessionManager sessionManager;

    @Mock
    private KeyValueAuditStore auditStore;

    private ConfluenceAuditResource confluenceAuditResource;

    @BeforeEach
    void setUp() {
        when(requestContext.getInvokerId()).thenReturn("test-invoker-id");
        when(confluenceAuditService.getAuditStore()).thenReturn(auditStore);

        confluenceAuditResource = new ConfluenceAuditResource(confluenceAuditService, requestContext,
                confluenceAttributes, sessionManager);
    }

    // ── Shared store wiring ────────────────────────────────────────────────

    @Nested
    @DisplayName("Shared Audit Store Tests")
    class SharedAuditStoreTests {

        @Test
        @DisplayName("Constructor calls confluenceAuditService.getAuditStore() to obtain the shared store")
        void testConstructor_CallsGetAuditStore() {
            verify(confluenceAuditService).getAuditStore();
        }

        @Test
        @DisplayName("getAuditData delegates to the shared store, not a new instance")
        void testGetAuditData_DelegatesToSharedStore() {
            AuditData mockData = mock(AuditData.class);
            when(auditStore.getData("test-invoker-id", 0L, Long.MAX_VALUE, null, null))
                    .thenReturn(mockData);

            AuditData result = confluenceAuditResource.getAuditData(
                    "test-invoker-id", 0L, Long.MAX_VALUE, null, null);

            assertSame(mockData, result);
            verify(auditStore).getData("test-invoker-id", 0L, Long.MAX_VALUE, null, null);
        }

        @Test
        @DisplayName("clearAuditRecords delegates to the shared store")
        void testClearAuditRecords_DelegatesToSharedStore() {
            confluenceAuditResource.clearAuditRecords("test-invoker-id", 0L, Long.MAX_VALUE);

            verify(auditStore).clearRecords("test-invoker-id", 0L, Long.MAX_VALUE);
        }

        @Test
        @DisplayName("getGlobalCounter delegates to the shared store")
        void testGetGlobalCounter_DelegatesToSharedStore() {
            Map<String, Object> mockCounter = new HashMap<>();
            mockCounter.put("total", 42L);
            when(auditStore.getGlobalCounter()).thenReturn(mockCounter);

            Map<String, Object> result = confluenceAuditResource.getGlobalCounter();

            assertSame(mockCounter, result);
            verify(auditStore).getGlobalCounter();
        }

        @Test
        @DisplayName("Two ConfluenceAuditResource instances using the same service share one store")
        void testTwoInstances_ShareSameStoreInstance() {
            // Simulate a second injection — getAuditStore() always returns the same instance
            ConfluenceAuditResource second = new ConfluenceAuditResource(confluenceAuditService, requestContext,
                    confluenceAttributes, sessionManager);

            // Both were constructed from the same service; the store reference is the same object
            second.getAuditData("test-invoker-id", 0L, Long.MAX_VALUE, null, null);
            confluenceAuditResource.getAuditData("test-invoker-id", 0L, Long.MAX_VALUE, null, null);

            // Both calls route to the same mock store
            verify(auditStore, times(2))
                    .getData("test-invoker-id", 0L, Long.MAX_VALUE, null, null);
        }
    }

    // ── buildHealthResponse ────────────────────────────────────────────────

    @Nested
    @DisplayName("buildHealthResponse Tests")
    class BuildHealthResponseTests {

        @Test
        @DisplayName("Reports BASIC auth method when basic credentials present")
        void testBuildHealthResponse_BasicAuth() {
            when(confluenceAttributes.areBasicCredentialsPresent()).thenReturn(true);
            when(confluenceAttributes.getHostUrl()).thenReturn("mycompany.atlassian.net");
            when(confluenceAttributes.getUserName()).thenReturn("admin@example.com");

            Map<String, Object> health = confluenceAuditResource.buildHealthResponse();

            assertEquals(AuthMethod.BASIC.name(), health.get("authMethod"));
            assertEquals("mycompany.atlassian.net", health.get("hostUrl"));
            assertEquals("admin@example.com", health.get("userName"));
            assertEquals("N/A", health.get("hasRefreshToken"));
        }

        @Test
        @DisplayName("Reports OAUTH2 auth method for 3LO OAuth credentials without refresh token")
        void testBuildHealthResponse_OAuth2_ThreeLO_NoRefreshToken() {
            when(confluenceAttributes.areBasicCredentialsPresent()).thenReturn(false);
            when(confluenceAttributes.areOAuthCredentialsPresent()).thenReturn(true);
            when(confluenceAttributes.isServiceAccountModeEnabled()).thenReturn(false);
            when(confluenceAttributes.getClientId()).thenReturn("test-client-id");
            when(confluenceAttributes.getClientSecret()).thenReturn("test-client-secret");
            // invokerId from requestContext.getInvokerId() set in setUp → "test-invoker-id"

            String appKey = "test-client-id#test-client-secret#test-invoker-id";
            when(sessionManager.get(appKey)).thenReturn(null);

            Map<String, Object> health = confluenceAuditResource.buildHealthResponse();

            assertEquals(AuthMethod.OAUTH2.name(), health.get("authMethod"));
            assertFalse((Boolean) health.get("hasRefreshToken"));
        }

        @Test
        @DisplayName("Reports OAUTH2 with hasRefreshToken=true when refresh token exists in session")
        void testBuildHealthResponse_OAuth2_ThreeLO_WithRefreshToken() {
            when(confluenceAttributes.areBasicCredentialsPresent()).thenReturn(false);
            when(confluenceAttributes.areOAuthCredentialsPresent()).thenReturn(true);
            when(confluenceAttributes.isServiceAccountModeEnabled()).thenReturn(false);
            when(confluenceAttributes.getClientId()).thenReturn("cid");
            when(confluenceAttributes.getClientSecret()).thenReturn("csec");
            // invokerId from requestContext.getInvokerId() set in setUp → "test-invoker-id"

            when(sessionManager.get("cid#csec#test-invoker-id")).thenReturn("some-refresh-token");

            Map<String, Object> health = confluenceAuditResource.buildHealthResponse();

            assertEquals(AuthMethod.OAUTH2.name(), health.get("authMethod"));
            assertTrue((Boolean) health.get("hasRefreshToken"));
        }

        @Test
        @DisplayName("Reports OAUTH2_CLIENT_CREDENTIALS for service-account (2LO) mode")
        void testBuildHealthResponse_OAuth2_ServiceAccount() {
            when(confluenceAttributes.areBasicCredentialsPresent()).thenReturn(false);
            when(confluenceAttributes.areOAuthCredentialsPresent()).thenReturn(true);
            when(confluenceAttributes.isServiceAccountModeEnabled()).thenReturn(true);
            when(confluenceAttributes.getClientId()).thenReturn("sa-client-id");

            Map<String, Object> health = confluenceAuditResource.buildHealthResponse();

            assertEquals(AuthMethod.OAUTH2_CLIENT_CREDENTIALS.name(), health.get("authMethod"));
            assertEquals("N/A", health.get("hasRefreshToken"));
        }

        @Test
        @DisplayName("Reports 'not configured' when neither basic nor OAuth credentials present")
        void testBuildHealthResponse_NotConfigured() {
            when(confluenceAttributes.areBasicCredentialsPresent()).thenReturn(false);
            when(confluenceAttributes.areOAuthCredentialsPresent()).thenReturn(false);

            Map<String, Object> health = confluenceAuditResource.buildHealthResponse();

            assertEquals("not configured", health.get("authMethod"));
            assertFalse((Boolean) health.get("hasRefreshToken"));
        }

        @Test
        @DisplayName("Returns error health map when confluenceAttributes throws an exception")
        void testBuildHealthResponse_HandlesException() {
            when(confluenceAttributes.areBasicCredentialsPresent())
                    .thenThrow(new RuntimeException("simulated credential lookup failure"));

            Map<String, Object> health = confluenceAuditResource.buildHealthResponse();

            assertEquals("error", health.get("authMethod"));
            assertFalse((Boolean) health.get("hasRefreshToken"));
        }
    }
}
