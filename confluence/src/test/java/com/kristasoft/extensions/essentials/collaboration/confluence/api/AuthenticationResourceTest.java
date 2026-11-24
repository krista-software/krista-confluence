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

import com.kristasoft.extensions.essentials.collaboration.confluence.ConfluenceAttributes;
import com.kristasoft.extensions.essentials.collaboration.confluence.authentication.ConfluenceOAuthService;
import com.kristasoft.extensions.essentials.collaboration.confluence.authentication.SessionManager;
import com.kristasoft.extensions.essentials.collaboration.confluence.utils.ConfluenceConstants;
import app.krista.ksdk.authentication.AuthorizationListener;
import app.krista.ksdk.context.AuthorizationContext;
import app.krista.ksdk.context.RequestContext;
import app.krista.sdk.atlassian.extension.authentication.AbstractAtlassianAuthResource;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.oauth.OAuth20Service;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AuthenticationResource} targeting branch and instruction
 * coverage in both {@code getCallBack()} and the private
 * {@code ensureCredentialsLoaded()} method.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationResource Unit Tests")
class AuthenticationResourceTest {

    @Mock
    private SessionManager sessionManager;

    @Mock
    private ConfluenceOAuthService confluenceOAuthService;

    @Mock
    private RequestContext requestContext;

    @Mock
    private AuthorizationContext authorizationContext;

    @Mock
    private AuthorizationListener authorizationListener;

    @Mock
    private ConfluenceAttributes confluenceAttributes;

    @Mock
    private OAuth20Service oAuth20Service;

    private AuthenticationResource resource;

    @BeforeEach
    void setUp() {
        resource = new AuthenticationResource(
                sessionManager, confluenceOAuthService, requestContext,
                authorizationContext, authorizationListener, confluenceAttributes);
    }

    // ── Helper: sets up a successful token exchange scenario ─────────────────

    /**
     * Configures mocks so that calling getCallBack(code, state) proceeds past
     * ensureCredentialsLoaded (clientId already present) and through the token
     * exchange, returning the given refreshToken.
     */
    private OAuth2AccessToken stubSuccessfulTokenExchange(String code, String refreshToken) throws Exception {
        when(confluenceAttributes.getClientId()).thenReturn("existing-client-id");
        when(requestContext.getInvokerId()).thenReturn("inv-1");
        when(confluenceOAuthService.getOauthService()).thenReturn(oAuth20Service);
        when(oAuth20Service.getApiKey()).thenReturn("ck");
        when(oAuth20Service.getApiSecret()).thenReturn("cs");

        OAuth2AccessToken token = mock(OAuth2AccessToken.class);
        when(token.getRefreshToken()).thenReturn(refreshToken);
        when(oAuth20Service.getAccessToken(code)).thenReturn(token);
        return token;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  ensureCredentialsLoaded — exercised indirectly through getCallBack
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("ensureCredentialsLoaded paths")
    class EnsureCredentialsLoaded {

        @Test
        @DisplayName("Early return when clientId is already present and non-blank")
        void clientIdAlreadyPresent_skipsSnapshotLoading() throws Exception {
            // clientId is non-blank → ensureCredentialsLoaded returns immediately
            stubSuccessfulTokenExchange("code1", "rt-1");

            String result = resource.getCallBack("code1", null);

            assertEquals(AbstractAtlassianAuthResource.CALLBACK_SUCCESS_HTML, result);
            // No snapshot lookup should have happened
            verify(sessionManager, never()).get(anyString());
        }

        @Test
        @DisplayName("Early return when invokerId is null")
        void invokerIdNull_skipsSnapshotLoading() throws Exception {
            // clientId is null → falls through to invokerId check
            when(confluenceAttributes.getClientId()).thenReturn(null);
            // invokerId is null → early return from ensureCredentialsLoaded
            when(requestContext.getInvokerId()).thenReturn(null);
            when(confluenceOAuthService.getOauthService()).thenReturn(oAuth20Service);
            when(oAuth20Service.getApiKey()).thenReturn("ck");
            when(oAuth20Service.getApiSecret()).thenReturn("cs");

            OAuth2AccessToken token = mock(OAuth2AccessToken.class);
            when(token.getRefreshToken()).thenReturn("rt-2");
            when(oAuth20Service.getAccessToken("code2")).thenReturn(token);

            String result = resource.getCallBack("code2", null);

            assertEquals(AbstractAtlassianAuthResource.CALLBACK_SUCCESS_HTML, result);
            // sessionManager.get should NOT have been called for snapshot keys
            verify(sessionManager, never()).get(anyString());
        }

        @Test
        @DisplayName("Early return when clientId is blank (empty string)")
        void clientIdBlank_fallsThroughToSnapshotCheck() throws Exception {
            // clientId is blank → does NOT early-return, continues to invokerId check
            when(confluenceAttributes.getClientId()).thenReturn("   ");
            when(requestContext.getInvokerId()).thenReturn("inv-snap");
            // Snapshot keys return null → no loading
            when(sessionManager.get("cred.inv-snap.clientId")).thenReturn(null);
            when(confluenceOAuthService.getOauthService()).thenReturn(oAuth20Service);
            when(oAuth20Service.getApiKey()).thenReturn("ck");
            when(oAuth20Service.getApiSecret()).thenReturn("cs");

            OAuth2AccessToken token = mock(OAuth2AccessToken.class);
            when(token.getRefreshToken()).thenReturn("rt-3");
            when(oAuth20Service.getAccessToken("code3")).thenReturn(token);

            String result = resource.getCallBack("code3", null);

            assertEquals(AbstractAtlassianAuthResource.CALLBACK_SUCCESS_HTML, result);
            // Snapshot lookup was attempted for clientId
            verify(sessionManager).get("cred.inv-snap.clientId");
            // But setAttributes was never called because snapClientId was null
            verify(confluenceAttributes, never()).setAttributes(anyMap());
        }

        @Test
        @DisplayName("Snapshot loading: both snapClientId and snapClientSecret present, existing attributes non-null, mode present")
        void snapshotLoading_withExistingAttributes_andMode() throws Exception {
            when(confluenceAttributes.getClientId()).thenReturn(null);
            when(requestContext.getInvokerId()).thenReturn("inv-full");
            when(sessionManager.get("cred.inv-full.clientId")).thenReturn("snap-cid");
            when(sessionManager.get("cred.inv-full.clientSecret")).thenReturn("snap-cs");
            when(sessionManager.get("cred.inv-full.credentialsMode")).thenReturn("managed");

            Map<String, Object> existing = new HashMap<>();
            existing.put("SomeKey", "SomeValue");
            when(confluenceAttributes.getAttributes()).thenReturn(existing);

            when(confluenceOAuthService.getOauthService()).thenReturn(oAuth20Service);
            when(oAuth20Service.getApiKey()).thenReturn("ck");
            when(oAuth20Service.getApiSecret()).thenReturn("cs");
            OAuth2AccessToken token = mock(OAuth2AccessToken.class);
            when(token.getRefreshToken()).thenReturn("rt-snap");
            when(oAuth20Service.getAccessToken("code-snap")).thenReturn(token);

            String result = resource.getCallBack("code-snap", null);

            assertEquals(AbstractAtlassianAuthResource.CALLBACK_SUCCESS_HTML, result);
            // Verify setAttributes was called with a map containing merged data
            @SuppressWarnings("unchecked")
            var captor = org.mockito.ArgumentCaptor.forClass(Map.class);
            verify(confluenceAttributes).setAttributes(captor.capture());
            Map<String, Object> captured = captor.getValue();
            assertEquals("snap-cid", captured.get(ConfluenceConstants.CLIENT_ID));
            assertEquals("snap-cs", captured.get(ConfluenceConstants.CLIENT_SECRET));
            assertEquals("managed", captured.get(ConfluenceConstants.CREDENTIALS_MODE));
            assertEquals("SomeValue", captured.get("SomeKey"));
        }

        @Test
        @DisplayName("Snapshot loading: existing attributes null, mode null")
        void snapshotLoading_existingAttributesNull_modeNull() throws Exception {
            when(confluenceAttributes.getClientId()).thenReturn("");
            when(requestContext.getInvokerId()).thenReturn("inv-null-attr");
            when(sessionManager.get("cred.inv-null-attr.clientId")).thenReturn("snap-cid2");
            when(sessionManager.get("cred.inv-null-attr.clientSecret")).thenReturn("snap-cs2");
            when(sessionManager.get("cred.inv-null-attr.credentialsMode")).thenReturn(null);

            // existing attributes returns null
            when(confluenceAttributes.getAttributes()).thenReturn(null);

            when(confluenceOAuthService.getOauthService()).thenReturn(oAuth20Service);
            when(oAuth20Service.getApiKey()).thenReturn("ck");
            when(oAuth20Service.getApiSecret()).thenReturn("cs");
            OAuth2AccessToken token = mock(OAuth2AccessToken.class);
            when(token.getRefreshToken()).thenReturn("rt-nullattr");
            when(oAuth20Service.getAccessToken("code-nullattr")).thenReturn(token);

            String result = resource.getCallBack("code-nullattr", null);

            assertEquals(AbstractAtlassianAuthResource.CALLBACK_SUCCESS_HTML, result);
            @SuppressWarnings("unchecked")
            var captor = org.mockito.ArgumentCaptor.forClass(Map.class);
            verify(confluenceAttributes).setAttributes(captor.capture());
            Map<String, Object> captured = captor.getValue();
            assertEquals("snap-cid2", captured.get(ConfluenceConstants.CLIENT_ID));
            assertEquals("snap-cs2", captured.get(ConfluenceConstants.CLIENT_SECRET));
            // mode was null, so CREDENTIALS_MODE should NOT be in the map
            assertFalse(captured.containsKey(ConfluenceConstants.CREDENTIALS_MODE));
        }

        @Test
        @DisplayName("Snapshot loading skipped: snapClientId present but snapClientSecret null")
        void snapshotLoading_snapClientSecretNull_skips() throws Exception {
            when(confluenceAttributes.getClientId()).thenReturn(null);
            when(requestContext.getInvokerId()).thenReturn("inv-no-secret");
            when(sessionManager.get("cred.inv-no-secret.clientId")).thenReturn("snap-cid");
            when(sessionManager.get("cred.inv-no-secret.clientSecret")).thenReturn(null);

            when(confluenceOAuthService.getOauthService()).thenReturn(oAuth20Service);
            when(oAuth20Service.getApiKey()).thenReturn("ck");
            when(oAuth20Service.getApiSecret()).thenReturn("cs");
            OAuth2AccessToken token = mock(OAuth2AccessToken.class);
            when(token.getRefreshToken()).thenReturn("rt-nosec");
            when(oAuth20Service.getAccessToken("code-nosec")).thenReturn(token);

            String result = resource.getCallBack("code-nosec", null);

            assertEquals(AbstractAtlassianAuthResource.CALLBACK_SUCCESS_HTML, result);
            verify(confluenceAttributes, never()).setAttributes(anyMap());
        }

        @Test
        @DisplayName("Snapshot loading skipped: snapClientId null")
        void snapshotLoading_snapClientIdNull_skips() throws Exception {
            when(confluenceAttributes.getClientId()).thenReturn(null);
            when(requestContext.getInvokerId()).thenReturn("inv-no-cid");
            when(sessionManager.get("cred.inv-no-cid.clientId")).thenReturn(null);

            when(confluenceOAuthService.getOauthService()).thenReturn(oAuth20Service);
            when(oAuth20Service.getApiKey()).thenReturn("ck");
            when(oAuth20Service.getApiSecret()).thenReturn("cs");
            OAuth2AccessToken token = mock(OAuth2AccessToken.class);
            when(token.getRefreshToken()).thenReturn("rt-nocid");
            when(oAuth20Service.getAccessToken("code-nocid")).thenReturn(token);

            String result = resource.getCallBack("code-nocid", null);

            assertEquals(AbstractAtlassianAuthResource.CALLBACK_SUCCESS_HTML, result);
            verify(confluenceAttributes, never()).setAttributes(anyMap());
        }

        @Test
        @DisplayName("Snapshot loading skipped: snapClientId is blank")
        void snapshotLoading_snapClientIdBlank_skips() throws Exception {
            when(confluenceAttributes.getClientId()).thenReturn(null);
            when(requestContext.getInvokerId()).thenReturn("inv-blank-cid");
            when(sessionManager.get("cred.inv-blank-cid.clientId")).thenReturn("   ");

            when(confluenceOAuthService.getOauthService()).thenReturn(oAuth20Service);
            when(oAuth20Service.getApiKey()).thenReturn("ck");
            when(oAuth20Service.getApiSecret()).thenReturn("cs");
            OAuth2AccessToken token = mock(OAuth2AccessToken.class);
            when(token.getRefreshToken()).thenReturn("rt-blcid");
            when(oAuth20Service.getAccessToken("code-blcid")).thenReturn(token);

            String result = resource.getCallBack("code-blcid", null);

            assertEquals(AbstractAtlassianAuthResource.CALLBACK_SUCCESS_HTML, result);
            verify(confluenceAttributes, never()).setAttributes(anyMap());
        }

        @Test
        @DisplayName("Snapshot loading skipped: snapClientSecret is blank")
        void snapshotLoading_snapClientSecretBlank_skips() throws Exception {
            when(confluenceAttributes.getClientId()).thenReturn(null);
            when(requestContext.getInvokerId()).thenReturn("inv-blank-sec");
            when(sessionManager.get("cred.inv-blank-sec.clientId")).thenReturn("snap-cid");
            when(sessionManager.get("cred.inv-blank-sec.clientSecret")).thenReturn("");

            when(confluenceOAuthService.getOauthService()).thenReturn(oAuth20Service);
            when(oAuth20Service.getApiKey()).thenReturn("ck");
            when(oAuth20Service.getApiSecret()).thenReturn("cs");
            OAuth2AccessToken token = mock(OAuth2AccessToken.class);
            when(token.getRefreshToken()).thenReturn("rt-blsec");
            when(oAuth20Service.getAccessToken("code-blsec")).thenReturn(token);

            String result = resource.getCallBack("code-blsec", null);

            assertEquals(AbstractAtlassianAuthResource.CALLBACK_SUCCESS_HTML, result);
            verify(confluenceAttributes, never()).setAttributes(anyMap());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  getCallBack — CSRF state validation via OAuthStateManager
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getCallBack CSRF state validation")
    class CsrfStateValidation {

        @Test
        @DisplayName("State with valid CSRF nonce: stateManager validates and returns accountId")
        void validCsrfNonce_accountIdExtracted() throws Exception {
            // State format: "myAccountId.abc123nonce"
            // OAuthStateManager will call store.get("oauth-state#abc123nonce")
            // and if it returns a future timestamp, it validates successfully,
            // calls store.remove("oauth-state#abc123nonce"), and returns "myAccountId".
            String nonce = "abc123nonce";
            String state = "myAccountId." + nonce;
            String futureExpiry = String.valueOf(System.currentTimeMillis() + 600_000);

            when(confluenceAttributes.getClientId()).thenReturn("cid");
            when(requestContext.getInvokerId()).thenReturn("inv-csrf");
            when(sessionManager.get("oauth-state#" + nonce)).thenReturn(futureExpiry);
            when(confluenceOAuthService.getOauthService()).thenReturn(oAuth20Service);
            when(oAuth20Service.getApiKey()).thenReturn("ck");
            when(oAuth20Service.getApiSecret()).thenReturn("cs");

            OAuth2AccessToken token = mock(OAuth2AccessToken.class);
            when(token.getRefreshToken()).thenReturn("rt-csrf");
            when(oAuth20Service.getAccessToken("code-csrf")).thenReturn(token);

            String result = resource.getCallBack("code-csrf", state);

            assertEquals(AbstractAtlassianAuthResource.CALLBACK_SUCCESS_HTML, result);
            // The validated accountId is "myAccountId", so user key = "myAccountId#inv-csrf"
            verify(sessionManager).storeRefToken(eq("myAccountId#inv-csrf"), eq("rt-csrf"));
            // The nonce should have been consumed (removed)
            verify(sessionManager).remove("oauth-state#" + nonce);
        }

        @Test
        @DisplayName("State without valid CSRF nonce: validateAndConsume returns null, raw state used as accountId")
        void invalidCsrfNonce_rawStateUsedAsAccountId() throws Exception {
            // State "some-account-id" has no "." separator for nonce,
            // so OAuthStateManager.validateAndConsume returns null,
            // and the code falls back to using raw state as accountId.
            String state = "some-account-id";

            when(confluenceAttributes.getClientId()).thenReturn("cid");
            when(requestContext.getInvokerId()).thenReturn("inv-raw");
            when(confluenceOAuthService.getOauthService()).thenReturn(oAuth20Service);
            when(oAuth20Service.getApiKey()).thenReturn("ck");
            when(oAuth20Service.getApiSecret()).thenReturn("cs");

            OAuth2AccessToken token = mock(OAuth2AccessToken.class);
            when(token.getRefreshToken()).thenReturn("rt-raw");
            when(oAuth20Service.getAccessToken("code-raw")).thenReturn(token);

            String result = resource.getCallBack("code-raw", state);

            assertEquals(AbstractAtlassianAuthResource.CALLBACK_SUCCESS_HTML, result);
            // Raw state used as accountId: key = "some-account-id#inv-raw"
            verify(sessionManager).storeRefToken(eq("some-account-id#inv-raw"), eq("rt-raw"));
        }

        @Test
        @DisplayName("State with nonce that has no stored expiry: validateAndConsume returns null, raw state used")
        void csrfNonceNotInStore_rawStateUsed() throws Exception {
            // State format has a dot but nonce is unknown in store
            String state = "acct.unknownnonce";

            when(confluenceAttributes.getClientId()).thenReturn("cid");
            when(requestContext.getInvokerId()).thenReturn("inv-unknown");
            // The nonce lookup returns null (not stored)
            when(sessionManager.get("oauth-state#unknownnonce")).thenReturn(null);
            when(confluenceOAuthService.getOauthService()).thenReturn(oAuth20Service);
            when(oAuth20Service.getApiKey()).thenReturn("ck");
            when(oAuth20Service.getApiSecret()).thenReturn("cs");

            OAuth2AccessToken token = mock(OAuth2AccessToken.class);
            when(token.getRefreshToken()).thenReturn("rt-unk");
            when(oAuth20Service.getAccessToken("code-unk")).thenReturn(token);

            String result = resource.getCallBack("code-unk", state);

            assertEquals(AbstractAtlassianAuthResource.CALLBACK_SUCCESS_HTML, result);
            // Fallback: raw state "acct.unknownnonce" is used as accountId
            verify(sessionManager).storeRefToken(eq("acct.unknownnonce#inv-unknown"), eq("rt-unk"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  getCallBack — exception paths
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getCallBack exception handling")
    class ExceptionHandling {

        @Test
        @DisplayName("Exception with a non-null cause appends cause message to error HTML")
        void exceptionWithCause_includesCauseMessage() throws Exception {
            when(confluenceAttributes.getClientId()).thenReturn("cid");
            when(confluenceOAuthService.getOauthService()).thenReturn(oAuth20Service);

            RuntimeException rootCause = new RuntimeException("root problem");
            RuntimeException wrapper = new RuntimeException("outer error", rootCause);
            when(oAuth20Service.getAccessToken("code-exc")).thenThrow(wrapper);

            String result = resource.getCallBack("code-exc", "state");

            assertTrue(result.contains("Authentication Failed"), "Should contain failure heading");
            assertTrue(result.contains("outer error"), "Should contain outer error message");
            assertTrue(result.contains("root problem"), "Should contain root cause message");
        }

        @Test
        @DisplayName("Exception without a cause does not append extra message")
        void exceptionWithoutCause_noExtraMessage() throws Exception {
            when(confluenceAttributes.getClientId()).thenReturn("cid");
            when(confluenceOAuthService.getOauthService()).thenReturn(oAuth20Service);

            when(oAuth20Service.getAccessToken("code-norc")).thenThrow(new RuntimeException("simple failure"));

            String result = resource.getCallBack("code-norc", "state");

            assertTrue(result.contains("Authentication Failed"));
            assertTrue(result.contains("simple failure"));
            // Should NOT contain a "| " separator since there is no cause
            assertFalse(result.contains("| "), "Should not contain cause separator when cause is null");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  getCallBack — refreshToken edge cases
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getCallBack refresh token edge cases")
    class RefreshTokenEdgeCases {

        @Test
        @DisplayName("Empty string refreshToken results in error HTML")
        void emptyRefreshToken_returnsErrorHtml() throws Exception {
            stubSuccessfulTokenExchange("code-empty-rt", "");

            String result = resource.getCallBack("code-empty-rt", "acct-id");

            assertTrue(result.contains("Authentication Failed"));
            assertTrue(result.contains("no refresh token"));
            verify(authorizationListener, never()).authorized();
        }

        @Test
        @DisplayName("Blank refreshToken (whitespace only) results in error HTML")
        void blankRefreshToken_returnsErrorHtml() throws Exception {
            stubSuccessfulTokenExchange("code-blank-rt", "   ");

            String result = resource.getCallBack("code-blank-rt", "acct-id");

            assertTrue(result.contains("Authentication Failed"));
            assertTrue(result.contains("no refresh token"));
            verify(authorizationListener, never()).authorized();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  getCallBack — code parameter edge cases
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getCallBack code parameter validation")
    class CodeParameterValidation {

        @Test
        @DisplayName("Null code returns error HTML without invoking OAuth service")
        void nullCode_returnsError() {
            String result = resource.getCallBack(null, "state");

            assertTrue(result.contains("Authentication Failed"));
            assertTrue(result.contains("No authorization code"));
            verifyNoInteractions(confluenceOAuthService);
        }

        @Test
        @DisplayName("Blank code returns error HTML without invoking OAuth service")
        void blankCode_returnsError() {
            String result = resource.getCallBack("", "state");

            assertTrue(result.contains("Authentication Failed"));
            assertTrue(result.contains("No authorization code"));
            verifyNoInteractions(confluenceOAuthService);
        }

        @Test
        @DisplayName("Whitespace-only code returns error HTML")
        void whitespaceCode_returnsError() {
            String result = resource.getCallBack("  \t  ", null);

            assertTrue(result.contains("Authentication Failed"));
            assertTrue(result.contains("No authorization code"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  getCallBack — OAuth service null
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getCallBack OAuth service availability")
    class OAuthServiceAvailability {

        @Test
        @DisplayName("Null OAuth service returns error HTML about configuration")
        void nullOAuthService_returnsConfigError() {
            when(confluenceAttributes.getClientId()).thenReturn("cid");
            when(confluenceOAuthService.getOauthService()).thenReturn(null);

            String result = resource.getCallBack("valid-code", "state");

            assertTrue(result.contains("Authentication Failed"));
            assertTrue(result.contains("OAuth service is not configured"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  getCallBack — token storage paths
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getCallBack token storage")
    class TokenStorage {

        @Test
        @DisplayName("Non-blank state stores refresh token under user-specific key")
        void nonBlankState_storesUnderUserKey() throws Exception {
            stubSuccessfulTokenExchange("code-user", "rt-user");

            resource.getCallBack("code-user", "user123");

            verify(sessionManager).storeRefToken(eq("user123#inv-1"), eq("rt-user"));
            verify(sessionManager, times(1)).storeRefToken(anyString(), anyString());
        }

        @Test
        @DisplayName("Null state stores refresh token under app-wide key")
        void nullState_storesUnderAppKey() throws Exception {
            stubSuccessfulTokenExchange("code-app", "rt-app");

            resource.getCallBack("code-app", null);

            verify(sessionManager).storeRefToken(eq("ck#cs#inv-1"), eq("rt-app"));
            verify(sessionManager, times(1)).storeRefToken(anyString(), anyString());
        }

        @Test
        @DisplayName("Blank state stores refresh token under app-wide key")
        void blankState_storesUnderAppKey() throws Exception {
            stubSuccessfulTokenExchange("code-bl", "rt-bl");

            resource.getCallBack("code-bl", "   ");

            verify(sessionManager).storeRefToken(eq("ck#cs#inv-1"), eq("rt-bl"));
        }

        @Test
        @DisplayName("Empty string state stores refresh token under app-wide key")
        void emptyState_storesUnderAppKey() throws Exception {
            stubSuccessfulTokenExchange("code-es", "rt-es");

            resource.getCallBack("code-es", "");

            verify(sessionManager).storeRefToken(eq("ck#cs#inv-1"), eq("rt-es"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  getCallBack — authorization listener
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getCallBack authorization listener")
    class AuthorizationListenerBehavior {

        @Test
        @DisplayName("Listener.authorized() is called on successful authentication with valid refreshToken")
        void successfulAuth_callsListener() throws Exception {
            stubSuccessfulTokenExchange("code-lis", "rt-lis");

            String result = resource.getCallBack("code-lis", null);

            assertEquals(AbstractAtlassianAuthResource.CALLBACK_SUCCESS_HTML, result);
            verify(authorizationListener).authorized();
        }

        @Test
        @DisplayName("Listener failure is swallowed and success HTML is still returned")
        void listenerFailure_stillReturnsSuccess() throws Exception {
            stubSuccessfulTokenExchange("code-lf", "rt-lf");
            doThrow(new RuntimeException("listener boom")).when(authorizationListener).authorized();

            String result = resource.getCallBack("code-lf", null);

            assertEquals(AbstractAtlassianAuthResource.CALLBACK_SUCCESS_HTML, result);
        }

        @Test
        @DisplayName("Listener is NOT called when refreshToken is null")
        void nullRefreshToken_listenerNotCalled() throws Exception {
            stubSuccessfulTokenExchange("code-nrt", null);

            resource.getCallBack("code-nrt", "acct");

            verify(authorizationListener, never()).authorized();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  ensureCredentialsLoaded + getCallBack combined: snapshot feeds OAuth
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("ensureCredentialsLoaded integration with getCallBack")
    class EnsureCredentialsIntegration {

        @Test
        @DisplayName("Snapshot loading with existing attributes containing pre-existing entries preserves them")
        void snapshotMergesWithExistingAttributes() throws Exception {
            when(confluenceAttributes.getClientId()).thenReturn(null);
            when(requestContext.getInvokerId()).thenReturn("inv-merge");
            when(sessionManager.get("cred.inv-merge.clientId")).thenReturn("s-cid");
            when(sessionManager.get("cred.inv-merge.clientSecret")).thenReturn("s-cs");
            when(sessionManager.get("cred.inv-merge.credentialsMode")).thenReturn("own");

            Map<String, Object> existing = new HashMap<>();
            existing.put("Domain Name", "my-domain");
            existing.put("ExtraKey", 42);
            when(confluenceAttributes.getAttributes()).thenReturn(existing);

            when(confluenceOAuthService.getOauthService()).thenReturn(oAuth20Service);
            when(oAuth20Service.getApiKey()).thenReturn("ck");
            when(oAuth20Service.getApiSecret()).thenReturn("cs");
            OAuth2AccessToken token = mock(OAuth2AccessToken.class);
            when(token.getRefreshToken()).thenReturn("rt-merge");
            when(oAuth20Service.getAccessToken("code-merge")).thenReturn(token);

            resource.getCallBack("code-merge", null);

            @SuppressWarnings("unchecked")
            var captor = org.mockito.ArgumentCaptor.forClass(Map.class);
            verify(confluenceAttributes).setAttributes(captor.capture());
            Map<String, Object> captured = captor.getValue();
            // Original entries preserved
            assertEquals("my-domain", captured.get("Domain Name"));
            assertEquals(42, captured.get("ExtraKey"));
            // Snapshot entries added
            assertEquals("s-cid", captured.get(ConfluenceConstants.CLIENT_ID));
            assertEquals("s-cs", captured.get(ConfluenceConstants.CLIENT_SECRET));
            assertEquals("own", captured.get(ConfluenceConstants.CREDENTIALS_MODE));
        }

        @Test
        @DisplayName("ensureCredentialsLoaded exception inside getCallBack is caught by the outer catch block")
        void ensureCredentialsLoaded_exceptionInOAuthService_caughtByCatchBlock() throws Exception {
            // clientId null → ensureCredentialsLoaded proceeds
            when(confluenceAttributes.getClientId()).thenReturn(null);
            when(requestContext.getInvokerId()).thenReturn("inv-err");
            // Snapshot returns valid data
            when(sessionManager.get("cred.inv-err.clientId")).thenReturn("cid-err");
            when(sessionManager.get("cred.inv-err.clientSecret")).thenReturn("cs-err");
            when(sessionManager.get("cred.inv-err.credentialsMode")).thenReturn(null);
            when(confluenceAttributes.getAttributes()).thenReturn(null);
            // OAuth service throws
            when(confluenceOAuthService.getOauthService()).thenThrow(
                    new RuntimeException("service init failed"));

            String result = resource.getCallBack("code-err", "state");

            assertTrue(result.contains("Authentication Failed"));
            assertTrue(result.contains("service init failed"));
        }
    }
}
