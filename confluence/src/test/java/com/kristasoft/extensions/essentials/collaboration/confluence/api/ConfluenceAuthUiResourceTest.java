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
import app.krista.ksdk.authentication.AuthorizationListener;
import app.krista.ksdk.context.RequestContext;
import app.krista.sdk.atlassian.audit.KeyValueAuditStore;
import com.google.gson.JsonObject;
import com.kristasoft.extensions.essentials.collaboration.confluence.ConfluenceAttributes;
import com.kristasoft.extensions.essentials.collaboration.confluence.authentication.ConfluenceOAuthService;
import com.kristasoft.extensions.essentials.collaboration.confluence.authentication.SessionManager;
import com.kristasoft.extensions.essentials.collaboration.confluence.utils.ConfluenceConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for {@link ConfluenceAuthUiResource}.
 *
 * <p>The class is {@code final}, so protected methods are tested via reflection.
 * The constructor calls {@code requestContext.getInvokerId()} and
 * {@code confluenceAuditService.getAuditStore()}, so those mocks must be
 * configured before construction.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ConfluenceAuthUiResource Tests")
class ConfluenceAuthUiResourceTest {

    @Mock
    private ConfluenceAttributes confluenceAttributes;

    @Mock
    private ConfluenceOAuthService confluenceOAuthService;

    @Mock
    private SessionManager sessionManager;

    @Mock
    private RequestContext requestContext;

    @Mock
    private KeyValueStore keyValueStore;

    @Mock
    private AuthorizationListener authorizationListener;

    @Mock
    private ConfluenceAuditService confluenceAuditService;

    @Mock
    private KeyValueAuditStore keyValueAuditStore;

    private ConfluenceAuthUiResource resource;

    @BeforeEach
    void setUp() {
        when(requestContext.getInvokerId()).thenReturn("test-invoker");
        when(confluenceAuditService.getAuditStore()).thenReturn(keyValueAuditStore);

        resource = new ConfluenceAuthUiResource(
                confluenceAttributes,
                confluenceOAuthService,
                sessionManager,
                requestContext,
                keyValueStore,
                authorizationListener,
                confluenceAuditService
        );
    }

    // ── Helper to invoke protected/private methods via reflection ──

    private Object invokeMethod(String methodName, Class<?>[] paramTypes, Object... args) throws Exception {
        Method method = ConfluenceAuthUiResource.class.getDeclaredMethod(methodName, paramTypes);
        method.setAccessible(true);
        return method.invoke(resource, args);
    }

    // ── Constructor tests ──

    @Nested
    @DisplayName("Constructor")
    class ConstructorTests {

        @Test
        @DisplayName("Should construct successfully with valid dependencies")
        void shouldConstructSuccessfully() {
            assertNotNull(resource);
        }

        @Test
        @DisplayName("Constructor should call requestContext.getInvokerId()")
        void shouldCallGetInvokerId() {
            verify(requestContext).getInvokerId();
        }

        @Test
        @DisplayName("Constructor should call confluenceAuditService.getAuditStore()")
        void shouldCallGetAuditStore() {
            verify(confluenceAuditService).getAuditStore();
        }
    }

    // ── loadAttributes tests ──

    @Nested
    @DisplayName("loadAttributes()")
    class LoadAttributesTests {

        @Test
        @DisplayName("Should return the injected confluenceAttributes regardless of key")
        void shouldReturnConfluenceAttributes() throws Exception {
            ConfluenceAttributes result = (ConfluenceAttributes) invokeMethod(
                    "loadAttributes",
                    new Class[]{String.class},
                    "any-key"
            );
            assertSame(confluenceAttributes, result);
        }

        @Test
        @DisplayName("Should return same attributes for different keys")
        void shouldReturnSameAttributesForDifferentKeys() throws Exception {
            ConfluenceAttributes result1 = (ConfluenceAttributes) invokeMethod(
                    "loadAttributes", new Class[]{String.class}, "key1");
            ConfluenceAttributes result2 = (ConfluenceAttributes) invokeMethod(
                    "loadAttributes", new Class[]{String.class}, "key2");
            assertSame(result1, result2);
        }

        @Test
        @DisplayName("Should return confluenceAttributes for null key")
        void shouldReturnConfluenceAttributesForNullKey() throws Exception {
            ConfluenceAttributes result = (ConfluenceAttributes) invokeMethod(
                    "loadAttributes", new Class[]{String.class}, (String) null);
            assertSame(confluenceAttributes, result);
        }
    }

    // ── saveAttributes tests ──

    @Nested
    @DisplayName("saveAttributes()")
    class SaveAttributesTests {

        @Test
        @DisplayName("Should always return true")
        void shouldAlwaysReturnTrue() throws Exception {
            boolean result = (boolean) invokeMethod(
                    "saveAttributes",
                    new Class[]{ConfluenceAttributes.class, String.class},
                    confluenceAttributes, "any-key"
            );
            assertTrue(result);
        }

        @Test
        @DisplayName("Should return true regardless of which attributes instance is passed")
        void shouldReturnTrueForAnyAttributes() throws Exception {
            ConfluenceAttributes otherAttrs = mock(ConfluenceAttributes.class);
            boolean result = (boolean) invokeMethod(
                    "saveAttributes",
                    new Class[]{ConfluenceAttributes.class, String.class},
                    otherAttrs, "different-key"
            );
            assertTrue(result);
        }
    }

    // ── getBasicAuthFieldLabels tests ──

    @Nested
    @DisplayName("getBasicAuthFieldLabels()")
    class GetBasicAuthFieldLabelsTests {

        private JsonObject getLabels() throws Exception {
            return (JsonObject) invokeMethod("getBasicAuthFieldLabels", new Class[]{});
        }

        @Test
        @DisplayName("Should return non-null JsonObject")
        void shouldReturnNonNullJsonObject() throws Exception {
            JsonObject labels = getLabels();
            assertNotNull(labels);
        }

        @Test
        @DisplayName("Should contain hostUrl with label 'Domain'")
        void shouldContainHostUrlWithDomainLabel() throws Exception {
            JsonObject labels = getLabels();
            assertTrue(labels.has("hostUrl"));
            JsonObject hostUrl = labels.getAsJsonObject("hostUrl");
            assertEquals("Domain", hostUrl.get("label").getAsString());
        }

        @Test
        @DisplayName("Should contain hostUrl with correct hint")
        void shouldContainHostUrlWithCorrectHint() throws Exception {
            JsonObject labels = getLabels();
            JsonObject hostUrl = labels.getAsJsonObject("hostUrl");
            assertEquals("Your Atlassian site domain name, e.g. acme",
                    hostUrl.get("hint").getAsString());
        }

        @Test
        @DisplayName("Should contain hostUrl with correct placeholder")
        void shouldContainHostUrlWithCorrectPlaceholder() throws Exception {
            JsonObject labels = getLabels();
            JsonObject hostUrl = labels.getAsJsonObject("hostUrl");
            assertEquals("your-site", hostUrl.get("placeholder").getAsString());
        }

        @Test
        @DisplayName("Should contain userName with label 'Account Email'")
        void shouldContainUserNameWithAccountEmailLabel() throws Exception {
            JsonObject labels = getLabels();
            assertTrue(labels.has("userName"));
            JsonObject userName = labels.getAsJsonObject("userName");
            assertEquals("Account Email", userName.get("label").getAsString());
        }

        @Test
        @DisplayName("Should contain userName with correct hint and placeholder")
        void shouldContainUserNameWithCorrectHintAndPlaceholder() throws Exception {
            JsonObject labels = getLabels();
            JsonObject userName = labels.getAsJsonObject("userName");
            assertNotNull(userName.get("hint").getAsString());
            assertEquals("you@company.com", userName.get("placeholder").getAsString());
        }

        @Test
        @DisplayName("Should contain authToken with label 'API Key'")
        void shouldContainAuthTokenWithApiKeyLabel() throws Exception {
            JsonObject labels = getLabels();
            assertTrue(labels.has("authToken"));
            JsonObject authToken = labels.getAsJsonObject("authToken");
            assertEquals("API Key", authToken.get("label").getAsString());
        }

        @Test
        @DisplayName("Should contain authToken with correct hint and placeholder")
        void shouldContainAuthTokenWithCorrectHintAndPlaceholder() throws Exception {
            JsonObject labels = getLabels();
            JsonObject authToken = labels.getAsJsonObject("authToken");
            assertNotNull(authToken.get("hint").getAsString());
            assertEquals("Atlassian API token", authToken.get("placeholder").getAsString());
        }

        @Test
        @DisplayName("Should contain sidebarDesc with correct text")
        void shouldContainSidebarDescWithCorrectText() throws Exception {
            JsonObject labels = getLabels();
            assertTrue(labels.has("sidebarDesc"));
            assertEquals("Domain, account email and API key.",
                    labels.get("sidebarDesc").getAsString());
        }
    }

    // ── getServiceAccountFieldName tests ──

    @Nested
    @DisplayName("getServiceAccountFieldName()")
    class GetServiceAccountFieldNameTests {

        @Test
        @DisplayName("Should return ConfluenceConstants.SERVICE_ACCOUNT")
        void shouldReturnServiceAccountConstant() throws Exception {
            String result = (String) invokeMethod(
                    "getServiceAccountFieldName", new Class[]{});
            assertEquals(ConfluenceConstants.SERVICE_ACCOUNT, result);
            assertEquals("Service Account", result);
        }
    }

    // ── onOAuthCallbackSuccess tests ──

    @Nested
    @DisplayName("onOAuthCallbackSuccess()")
    class OnOAuthCallbackSuccessTests {

        @Test
        @DisplayName("Should call authorizationListener.authorized()")
        void shouldCallAuthorizationListenerAuthorized() throws Exception {
            invokeMethod(
                    "onOAuthCallbackSuccess",
                    new Class[]{String.class, String.class},
                    "test-refresh-token-key", "test-state"
            );
            verify(authorizationListener).authorized();
        }

        @Test
        @DisplayName("Should call authorized with null parameters")
        void shouldCallAuthorizedWithNullParams() throws Exception {
            invokeMethod(
                    "onOAuthCallbackSuccess",
                    new Class[]{String.class, String.class},
                    null, null
            );
            verify(authorizationListener).authorized();
        }

        @Test
        @DisplayName("Should call authorized exactly once per invocation")
        void shouldCallAuthorizedExactlyOnce() throws Exception {
            invokeMethod(
                    "onOAuthCallbackSuccess",
                    new Class[]{String.class, String.class},
                    "key", "state"
            );
            verify(authorizationListener, times(1)).authorized();
        }
    }

    // ── removeTokens tests ──

    @Nested
    @DisplayName("removeTokens()")
    class RemoveTokensTests {

        @Test
        @DisplayName("Should call keyValueStore.remove when OAuth credentials are present")
        void shouldCallRemoveWhenOAuthCredentialsPresent() throws Exception {
            when(confluenceAttributes.areOAuthCredentialsPresent()).thenReturn(true);
            when(confluenceAttributes.getEffectiveClientId()).thenReturn("client-id");
            when(confluenceAttributes.getEffectiveClientSecret()).thenReturn("client-secret");

            invokeMethod(
                    "removeTokens",
                    new Class[]{ConfluenceAttributes.class},
                    confluenceAttributes
            );

            verify(keyValueStore).remove(anyString());
        }

        @Test
        @DisplayName("Should not call keyValueStore.remove when OAuth credentials are absent")
        void shouldNotCallRemoveWhenNoOAuthCredentials() throws Exception {
            when(confluenceAttributes.areOAuthCredentialsPresent()).thenReturn(false);

            invokeMethod(
                    "removeTokens",
                    new Class[]{ConfluenceAttributes.class},
                    confluenceAttributes
            );

            verify(keyValueStore, never()).remove(anyString());
        }

        @Test
        @DisplayName("Should compute correct token key using effective client ID and secret")
        void shouldComputeCorrectTokenKey() throws Exception {
            when(confluenceAttributes.areOAuthCredentialsPresent()).thenReturn(true);
            when(confluenceAttributes.getEffectiveClientId()).thenReturn("my-client-id");
            when(confluenceAttributes.getEffectiveClientSecret()).thenReturn("my-client-secret");

            invokeMethod(
                    "removeTokens",
                    new Class[]{ConfluenceAttributes.class},
                    confluenceAttributes
            );

            // The appWideTokenKey format is: effectiveClientId#effectiveClientSecret#invokerId
            verify(keyValueStore).remove("my-client-id#my-client-secret#test-invoker");
        }
    }
}
