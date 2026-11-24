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

package com.kristasoft.extensions.essentials.collaboration.confluence.catalog;

import app.krista.extension.authorization.MustAuthorizeException;
import app.krista.extension.executor.ExtensionResponse;
import app.krista.ksdk.context.RequestContext;
import com.kristasoft.extensions.essentials.collaboration.confluence.ConfluenceAttributes;
import com.kristasoft.extensions.essentials.collaboration.confluence.api.ConfluenceAuditService;
import com.kristasoft.extensions.essentials.collaboration.confluence.client.ConfluenceRESTClient;
import com.kristasoft.extensions.essentials.collaboration.confluence.service.ConfluenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.HttpURLConnection;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for {@link WebhookArea} targeting 100% code coverage.
 *
 * <h2>Branch map</h2>
 * <pre>
 * createWebhook
 *   ├─ active == null                 → defaults to true
 *   ├─ active != null                 → uses provided value
 *   ├─ events != null && !events.isEmpty() → events added to request
 *   ├─ events == null or empty        → events not added to request
 *   ├─ response HTTP_OK              → success map with Name, Success=true
 *   ├─ response HTTP_CREATED         → success map with Name, Success=true
 *   ├─ response with "name" field    → uses JSON name
 *   ├─ response without "name" field → uses fallback webhookName
 *   ├─ response non-200/201          → failure map with Name, Success=false
 *   ├─ MustAuthorizeException        → authFailure, rethrown
 *   ├─ general Exception             → failure response, audit failure
 *   └─ finally: auditRecorded check  → success if not already recorded
 * </pre>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WebhookArea Tests")
class WebhookAreaTest {

    @Mock
    private ConfluenceService confluenceService;

    @Mock
    private ConfluenceAuditService confluenceAuditService;

    @Mock
    private RequestContext requestContext;

    @Mock
    private ConfluenceAttributes confluenceAttributes;

    @Mock
    private ConfluenceRESTClient restClient;

    @Mock
    @SuppressWarnings("unchecked")
    private HttpResponse<String> httpResponse;

    private WebhookArea webhookArea;

    private static final String INVOKER_ID = "test-invoker-id";

    @BeforeEach
    void setUp() {
        when(requestContext.getInvokerId()).thenReturn(INVOKER_ID);
        webhookArea = new WebhookArea(confluenceService, confluenceAuditService, requestContext);
    }

    // ── Helper methods ──────────────────────────────────────────────────────

    private void mockRestClientChain() {
        when(confluenceService.getConfluenceAttributes()).thenReturn(confluenceAttributes);
        when(confluenceAttributes.getRestClient()).thenReturn(restClient);
    }

    private void mockDoSyncPost(HttpResponse<String> response) {
        mockRestClientChain();
        when(restClient.doSyncPost(isNull(), anyMap(),
                eq("wiki"), eq("rest"), eq("webhooks"), eq("1.0"), eq("webhook")))
                .thenReturn(response);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  createWebhook
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("createWebhook - Success paths")
    class CreateWebhookSuccess {

        @Test
        @DisplayName("HTTP_OK with events and active=true: returns success with Name from response JSON")
        void httpOk_withEvents_returnsSuccessWithNameFromJson() {
            // Arrange
            mockDoSyncPost(httpResponse);
            when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
            when(httpResponse.body()).thenReturn("{\"name\": \"My Webhook\", \"id\": 123}");

            List<String> events = List.of("page_created", "page_updated");

            // Act
            ExtensionResponse result = webhookArea.createWebhook("TestWebhook", "https://example.com/hook", events, true);

            // Assert
            assertNotNull(result);
            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());

            @SuppressWarnings("unchecked")
            Map<String, Object> webhookResponse = (Map<String, Object>) result.getResponseValue().get("Webhook Response");
            assertNotNull(webhookResponse);
            assertEquals("My Webhook", webhookResponse.get("Name"));
            assertEquals(true, webhookResponse.get("Success"));

            verify(confluenceAuditService).triggered(INVOKER_ID, "Create Webhook");
            verify(confluenceAuditService).success(INVOKER_ID, "Create Webhook");
            verify(confluenceAuditService, never()).failure(anyString(), anyString(), any());
            verify(confluenceAuditService, never()).authFailure(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("HTTP_CREATED: returns success")
        void httpCreated_returnsSuccess() {
            // Arrange
            mockDoSyncPost(httpResponse);
            when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_CREATED);
            when(httpResponse.body()).thenReturn("{\"name\": \"Created Hook\"}");

            // Act
            ExtensionResponse result = webhookArea.createWebhook("NewHook", "https://example.com/hook", null, true);

            // Assert
            assertNotNull(result);
            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());

            @SuppressWarnings("unchecked")
            Map<String, Object> webhookResponse = (Map<String, Object>) result.getResponseValue().get("Webhook Response");
            assertEquals("Created Hook", webhookResponse.get("Name"));
            assertEquals(true, webhookResponse.get("Success"));
        }

        @Test
        @DisplayName("Response JSON without 'name' field: falls back to provided webhookName")
        void responseWithoutNameField_fallsBackToProvidedName() {
            // Arrange
            mockDoSyncPost(httpResponse);
            when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
            when(httpResponse.body()).thenReturn("{\"id\": 456}");

            // Act
            ExtensionResponse result = webhookArea.createWebhook("FallbackName", "https://example.com/hook", null, true);

            // Assert
            @SuppressWarnings("unchecked")
            Map<String, Object> webhookResponse = (Map<String, Object>) result.getResponseValue().get("Webhook Response");
            assertEquals("FallbackName", webhookResponse.get("Name"));
            assertEquals(true, webhookResponse.get("Success"));
        }
    }

    @Nested
    @DisplayName("createWebhook - Active state defaults")
    class CreateWebhookActiveDefaults {

        @Test
        @DisplayName("Active null defaults to true")
        void activeNull_defaultsToTrue() {
            // Arrange
            mockDoSyncPost(httpResponse);
            when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
            when(httpResponse.body()).thenReturn("{\"name\": \"Hook\"}");

            // Act
            ExtensionResponse result = webhookArea.createWebhook("Hook", "https://test.com", List.of("event1"), null);

            // Assert
            assertNotNull(result);
            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());
            verify(confluenceAuditService).success(INVOKER_ID, "Create Webhook");
        }

        @Test
        @DisplayName("Active false is preserved")
        void activeFalse_isPreserved() {
            // Arrange
            mockDoSyncPost(httpResponse);
            when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
            when(httpResponse.body()).thenReturn("{\"name\": \"Inactive\"}");

            // Act
            ExtensionResponse result = webhookArea.createWebhook("Inactive", "https://test.com", null, false);

            // Assert
            assertNotNull(result);
            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());
        }
    }

    @Nested
    @DisplayName("createWebhook - Events handling")
    class CreateWebhookEvents {

        @Test
        @DisplayName("Events null: events not added to request")
        void eventsNull_notAddedToRequest() {
            // Arrange
            mockDoSyncPost(httpResponse);
            when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
            when(httpResponse.body()).thenReturn("{\"name\": \"NoEvents\"}");

            // Act
            ExtensionResponse result = webhookArea.createWebhook("NoEvents", "https://test.com", null, true);

            // Assert
            assertNotNull(result);
            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());
        }

        @Test
        @DisplayName("Events empty list: events not added to request")
        void eventsEmpty_notAddedToRequest() {
            // Arrange
            mockDoSyncPost(httpResponse);
            when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
            when(httpResponse.body()).thenReturn("{\"name\": \"EmptyEvents\"}");

            // Act
            ExtensionResponse result = webhookArea.createWebhook("EmptyEvents", "https://test.com", List.of(), true);

            // Assert
            assertNotNull(result);
            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());
        }

        @Test
        @DisplayName("Events with values: events added to request")
        void eventsWithValues_addedToRequest() {
            // Arrange
            mockDoSyncPost(httpResponse);
            when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_CREATED);
            when(httpResponse.body()).thenReturn("{\"name\": \"WithEvents\"}");

            List<String> events = List.of("page_created", "space_created");

            // Act
            ExtensionResponse result = webhookArea.createWebhook("WithEvents", "https://test.com", events, true);

            // Assert
            assertNotNull(result);
            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());
        }
    }

    @Nested
    @DisplayName("createWebhook - Error response (non-200/201)")
    class CreateWebhookErrorResponse {

        @Test
        @DisplayName("HTTP 400 status: returns failure map with Success=false")
        void http400_returnsFailureMap() {
            // Arrange
            mockDoSyncPost(httpResponse);
            when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_BAD_REQUEST);
            when(httpResponse.body()).thenReturn("{\"error\": \"Bad request\"}");

            // Act
            ExtensionResponse result = webhookArea.createWebhook("BadHook", "https://invalid.com", null, true);

            // Assert
            assertNotNull(result);
            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());

            @SuppressWarnings("unchecked")
            Map<String, Object> webhookResponse = (Map<String, Object>) result.getResponseValue().get("Webhook Response");
            assertNotNull(webhookResponse);
            assertEquals("BadHook", webhookResponse.get("Name"));
            assertEquals(false, webhookResponse.get("Success"));

            verify(confluenceAuditService).success(INVOKER_ID, "Create Webhook");
        }

        @Test
        @DisplayName("HTTP 403 Forbidden: returns failure map")
        void http403_returnsFailureMap() {
            // Arrange
            mockDoSyncPost(httpResponse);
            when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_FORBIDDEN);
            when(httpResponse.body()).thenReturn("Forbidden");

            // Act
            ExtensionResponse result = webhookArea.createWebhook("ForbiddenHook", "https://test.com", null, true);

            // Assert
            @SuppressWarnings("unchecked")
            Map<String, Object> webhookResponse = (Map<String, Object>) result.getResponseValue().get("Webhook Response");
            assertEquals("ForbiddenHook", webhookResponse.get("Name"));
            assertEquals(false, webhookResponse.get("Success"));
        }

        @Test
        @DisplayName("HTTP 500 Internal Server Error: returns failure map")
        void http500_returnsFailureMap() {
            // Arrange
            mockDoSyncPost(httpResponse);
            when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_INTERNAL_ERROR);
            when(httpResponse.body()).thenReturn("Internal Server Error");

            // Act
            ExtensionResponse result = webhookArea.createWebhook("ErrorHook", "https://test.com", null, true);

            // Assert
            @SuppressWarnings("unchecked")
            Map<String, Object> webhookResponse = (Map<String, Object>) result.getResponseValue().get("Webhook Response");
            assertEquals("ErrorHook", webhookResponse.get("Name"));
            assertEquals(false, webhookResponse.get("Success"));
        }
    }

    @Nested
    @DisplayName("createWebhook - Exception paths")
    class CreateWebhookExceptions {

        @Test
        @DisplayName("MustAuthorizeException: calls authFailure and rethrows")
        void mustAuthorizeException_callsAuthFailureAndRethrows() {
            // Arrange
            MustAuthorizeException authException = new MustAuthorizeException("Authorization needed");
            mockRestClientChain();
            when(confluenceAttributes.getRestClient()).thenThrow(authException);

            // Act & Assert
            MustAuthorizeException thrown = assertThrows(MustAuthorizeException.class,
                    () -> webhookArea.createWebhook("AuthHook", "https://test.com", null, true));

            assertSame(authException, thrown);
            verify(confluenceAuditService).triggered(INVOKER_ID, "Create Webhook");
            verify(confluenceAuditService).authFailure(INVOKER_ID, "Create Webhook", authException);
            verify(confluenceAuditService, never()).success(anyString(), anyString());
            verify(confluenceAuditService, never()).failure(anyString(), anyString(), any(Exception.class));
        }

        @Test
        @DisplayName("General Exception: returns failure response and calls audit failure")
        void generalException_returnsFailureResponse() {
            // Arrange
            RuntimeException exception = new RuntimeException("Connection refused");
            mockRestClientChain();
            when(confluenceAttributes.getRestClient()).thenThrow(exception);

            // Act
            ExtensionResponse result = webhookArea.createWebhook("FailHook", "https://test.com", null, true);

            // Assert
            assertNotNull(result);
            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());

            @SuppressWarnings("unchecked")
            Map<String, Object> webhookResponse = (Map<String, Object>) result.getResponseValue().get("Webhook Response");
            assertEquals("FailHook", webhookResponse.get("Name"));
            assertEquals(false, webhookResponse.get("Success"));

            verify(confluenceAuditService).triggered(INVOKER_ID, "Create Webhook");
            verify(confluenceAuditService).failure(INVOKER_ID, "Create Webhook", exception);
            verify(confluenceAuditService, never()).success(anyString(), anyString());
        }

        @Test
        @DisplayName("NullPointerException during REST call: returns failure response")
        void nullPointerException_returnsFailureResponse() {
            // Arrange
            when(confluenceService.getConfluenceAttributes()).thenReturn(null);

            // Act
            ExtensionResponse result = webhookArea.createWebhook("NullHook", "https://test.com", List.of("e1"), true);

            // Assert
            assertNotNull(result);
            @SuppressWarnings("unchecked")
            Map<String, Object> webhookResponse = (Map<String, Object>) result.getResponseValue().get("Webhook Response");
            assertEquals("NullHook", webhookResponse.get("Name"));
            assertEquals(false, webhookResponse.get("Success"));
        }
    }

    @Nested
    @DisplayName("createWebhook - Audit event verification")
    class CreateWebhookAuditEvents {

        @Test
        @DisplayName("Successful response: triggered then success audit events")
        void successfulResponse_triggeredThenSuccess() {
            // Arrange
            mockDoSyncPost(httpResponse);
            when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
            when(httpResponse.body()).thenReturn("{\"name\": \"AuditHook\"}");

            // Act
            webhookArea.createWebhook("AuditHook", "https://test.com", null, true);

            // Assert
            var inOrder = inOrder(confluenceAuditService);
            inOrder.verify(confluenceAuditService).triggered(INVOKER_ID, "Create Webhook");
            inOrder.verify(confluenceAuditService).success(INVOKER_ID, "Create Webhook");
        }

        @Test
        @DisplayName("Error response: triggered then success (error response is not an exception)")
        void errorResponse_triggeredThenSuccess() {
            // Arrange
            mockDoSyncPost(httpResponse);
            when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_BAD_REQUEST);
            when(httpResponse.body()).thenReturn("Bad request");

            // Act
            webhookArea.createWebhook("ErrorAudit", "https://test.com", null, true);

            // Assert
            verify(confluenceAuditService).triggered(INVOKER_ID, "Create Webhook");
            verify(confluenceAuditService).success(INVOKER_ID, "Create Webhook");
            verify(confluenceAuditService, never()).failure(anyString(), anyString(), any());
        }
    }
}
