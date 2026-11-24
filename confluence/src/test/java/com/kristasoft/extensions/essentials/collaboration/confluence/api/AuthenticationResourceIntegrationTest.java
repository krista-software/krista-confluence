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
import app.krista.ksdk.authentication.AuthorizationListener;
import app.krista.ksdk.context.AuthorizationContext;
import app.krista.ksdk.context.RequestContext;
import app.krista.sdk.atlassian.extension.authentication.AbstractAtlassianAuthResource;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.oauth.OAuth20Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationResource Integration Tests")
class AuthenticationResourceIntegrationTest {

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

    private AuthenticationResource authenticationResource;

    @BeforeEach
    void setUp() {
        authenticationResource = new AuthenticationResource(
                sessionManager, confluenceOAuthService, requestContext,
                authorizationContext, authorizationListener, confluenceAttributes);
    }

    @Test
    @DisplayName("Should successfully handle OAuth callback with state (per-user token storage)")
    void testGetCallBack_Success_WithState() throws Exception {
        String code = "auth-code-123";
        String state = "test-account-id";
        String invokerId = "invoker-123";
        String clientId = "client-id";
        String clientSecret = "client-secret";
        String refreshToken = "refresh-token-abc";

        when(requestContext.getInvokerId()).thenReturn(invokerId);
        when(confluenceOAuthService.getOauthService()).thenReturn(oAuth20Service);
        when(oAuth20Service.getApiKey()).thenReturn(clientId);
        when(oAuth20Service.getApiSecret()).thenReturn(clientSecret);

        OAuth2AccessToken accessToken = mock(OAuth2AccessToken.class);
        when(accessToken.getRefreshToken()).thenReturn(refreshToken);
        when(oAuth20Service.getAccessToken(code)).thenReturn(accessToken);

        String result = authenticationResource.getCallBack(code, state);

        // Should return auto-close HTML, not plain text
        assertEquals(AbstractAtlassianAuthResource.CALLBACK_SUCCESS_HTML, result);

        // Per-user flow: only user-specific key is stored (not app-wide)
        String userKey = state + "#" + invokerId;
        verify(sessionManager).storeRefToken(userKey, refreshToken);
        verify(sessionManager, times(1)).storeRefToken(anyString(), anyString());

        verify(authorizationListener).authorized();
    }

    @Test
    @DisplayName("Should return error HTML when refresh token is null")
    void testGetCallBack_NullRefreshToken() throws Exception {
        String code = "auth-code-456";
        String state = "test-account-id";
        String invokerId = "invoker-456";

        when(requestContext.getInvokerId()).thenReturn(invokerId);
        when(confluenceOAuthService.getOauthService()).thenReturn(oAuth20Service);
        when(oAuth20Service.getApiKey()).thenReturn("client-id");
        when(oAuth20Service.getApiSecret()).thenReturn("client-secret");

        OAuth2AccessToken accessToken = mock(OAuth2AccessToken.class);
        when(accessToken.getRefreshToken()).thenReturn(null);
        when(oAuth20Service.getAccessToken(code)).thenReturn(accessToken);

        String result = authenticationResource.getCallBack(code, state);

        assertTrue(result.contains("Authentication Failed"));
        assertTrue(result.contains("no refresh token"));
        verify(authorizationListener, never()).authorized();
    }

    @Test
    @DisplayName("Should store under app-wide key when state is null")
    void testGetCallBack_NullState_StoresAppWideKey() throws Exception {
        String code = "auth-code-789";
        String invokerId = "invoker-789";
        String clientId = "client-id";
        String clientSecret = "client-secret";

        when(requestContext.getInvokerId()).thenReturn(invokerId);
        when(confluenceOAuthService.getOauthService()).thenReturn(oAuth20Service);
        when(oAuth20Service.getApiKey()).thenReturn(clientId);
        when(oAuth20Service.getApiSecret()).thenReturn(clientSecret);

        OAuth2AccessToken accessToken = mock(OAuth2AccessToken.class);
        when(accessToken.getRefreshToken()).thenReturn("refresh-token");
        when(oAuth20Service.getAccessToken(code)).thenReturn(accessToken);

        authenticationResource.getCallBack(code, null);

        // Null state → app-wide key storage
        String appKey = clientId + "#" + clientSecret + "#" + invokerId;
        verify(sessionManager).storeRefToken(appKey, "refresh-token");
        verify(sessionManager, times(1)).storeRefToken(anyString(), anyString());
    }

    @Test
    @DisplayName("Should store under app-wide key when state is blank")
    void testGetCallBack_BlankState_StoresAppWideKey() throws Exception {
        String code = "auth-code-blank";
        String invokerId = "invoker-blank";
        String clientId = "client-id";
        String clientSecret = "client-secret";

        when(requestContext.getInvokerId()).thenReturn(invokerId);
        when(confluenceOAuthService.getOauthService()).thenReturn(oAuth20Service);
        when(oAuth20Service.getApiKey()).thenReturn(clientId);
        when(oAuth20Service.getApiSecret()).thenReturn(clientSecret);

        OAuth2AccessToken accessToken = mock(OAuth2AccessToken.class);
        when(accessToken.getRefreshToken()).thenReturn("refresh-token");
        when(oAuth20Service.getAccessToken(code)).thenReturn(accessToken);

        authenticationResource.getCallBack(code, "   ");

        // Blank state → app-wide key storage
        String appKey = clientId + "#" + clientSecret + "#" + invokerId;
        verify(sessionManager).storeRefToken(appKey, "refresh-token");
        verify(sessionManager, times(1)).storeRefToken(anyString(), anyString());
    }

    @Test
    @DisplayName("Should return error HTML when code is null")
    void testGetCallBack_NullCode() {
        String result = authenticationResource.getCallBack(null, "state");
        assertTrue(result.contains("Authentication Failed"));
        assertTrue(result.contains("No authorization code"));
    }

    @Test
    @DisplayName("Should return error HTML when code is blank")
    void testGetCallBack_BlankCode() {
        String result = authenticationResource.getCallBack("   ", "state");
        assertTrue(result.contains("Authentication Failed"));
        assertTrue(result.contains("No authorization code"));
    }

    @Test
    @DisplayName("Should return error HTML when OAuth service is null")
    void testGetCallBack_NullOAuthService() {
        when(confluenceOAuthService.getOauthService()).thenReturn(null);

        String result = authenticationResource.getCallBack("code", "state");

        assertTrue(result.contains("Authentication Failed"));
        assertTrue(result.contains("OAuth service is not configured"));
    }

    @Test
    @DisplayName("Should return error HTML when token exchange throws exception")
    void testGetCallBack_TokenExchangeException() throws Exception {
        when(confluenceOAuthService.getOauthService()).thenReturn(oAuth20Service);
        when(oAuth20Service.getAccessToken("code")).thenThrow(new RuntimeException("Token exchange failed"));

        String result = authenticationResource.getCallBack("code", "state");

        assertTrue(result.contains("Authentication Failed"));
        assertTrue(result.contains("Token exchange failed"));
    }

    @Test
    @DisplayName("Should succeed even if authorizationListener throws")
    void testGetCallBack_ListenerFailure_StillSucceeds() throws Exception {
        String code = "auth-code-listener";
        String invokerId = "invoker-listener";
        String clientId = "client-id";
        String clientSecret = "client-secret";

        when(requestContext.getInvokerId()).thenReturn(invokerId);
        when(confluenceOAuthService.getOauthService()).thenReturn(oAuth20Service);
        when(oAuth20Service.getApiKey()).thenReturn(clientId);
        when(oAuth20Service.getApiSecret()).thenReturn(clientSecret);

        OAuth2AccessToken accessToken = mock(OAuth2AccessToken.class);
        when(accessToken.getRefreshToken()).thenReturn("refresh-token");
        when(oAuth20Service.getAccessToken(code)).thenReturn(accessToken);
        doThrow(new RuntimeException("listener failed")).when(authorizationListener).authorized();

        // Should still return success HTML even if listener throws
        String result = authenticationResource.getCallBack(code, null);

        assertEquals(AbstractAtlassianAuthResource.CALLBACK_SUCCESS_HTML, result);
    }
}
