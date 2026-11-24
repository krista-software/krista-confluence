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

package com.kristasoft.extensions.essentials.collaboration.confluence.authentication;

import app.krista.extension.authorization.MustAuthorizeException;
import app.krista.sdk.atlassian.constants.AtlassianConstants;
import app.krista.sdk.atlassian.oauth.AccessibleResource;
import app.krista.extension.executor.Invoker;
import app.krista.extension.request.RoutingInfo;
import app.krista.extension.request.protos.http.HttpProtocol;
import app.krista.ksdk.accounts.Account;
import app.krista.ksdk.context.AuthorizationContext;
import app.krista.ksdk.context.RequestContext;
import com.kristasoft.extensions.essentials.collaboration.confluence.ConfluenceAttributes;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.oauth.OAuth20Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfluenceOAuthServiceTest {

    @Mock
    private RequestContext requestContext;

    @Mock
    private SessionManager sessionManager;

    @Mock
    private Invoker invoker;

    @Mock
    private AuthorizationContext authorizationContext;

    @Mock
    private ConfluenceAttributes confluenceAttributes;

    @Mock
    private RoutingInfo routingInfo;

    @Mock
    private Account authorizedAccount;

    private ConfluenceOAuthService oAuthService;

    @BeforeEach
    void setUp() {
        lenient().when(invoker.getRoutingInfo()).thenReturn(routingInfo);
        lenient().when(routingInfo.getRoutingURL(eq(HttpProtocol.PROTOCOL_NAME), eq(RoutingInfo.Type.APPLIANCE)))
                .thenReturn("https://example.com/api");
        lenient().when(confluenceAttributes.areOAuthCredentialsPresent()).thenReturn(true);
        lenient().when(confluenceAttributes.getEffectiveClientId()).thenReturn("test-client-id");
        lenient().when(confluenceAttributes.getEffectiveClientSecret()).thenReturn("test-client-secret-that-is-long-enough-32chars");

        oAuthService = new ConfluenceOAuthService(requestContext, sessionManager, invoker,
                authorizationContext, confluenceAttributes);
    }

    // --- Constructor ---

    @Test
    void testConstructor() {
        assertNotNull(oAuthService);
    }

    // --- getOauthService ---

    @Test
    void testGetOauthService_ReturnsNonNull() {
        OAuth20Service service = oAuthService.getOauthService();
        assertNotNull(service);
    }

    @Test
    void testGetOauthService_ReturnsSameInstance() {
        OAuth20Service first = oAuthService.getOauthService();
        OAuth20Service second = oAuthService.getOauthService();
        assertSame(first, second, "Should return the same OAuth20Service instance");
    }

    @Test
    void testGetOauthService_RebuildsWhenCredentialsChange() {
        OAuth20Service firstService = oAuthService.getOauthService();
        assertNotNull(firstService);

        // Change credentials so they no longer match
        when(confluenceAttributes.getEffectiveClientId()).thenReturn("new-client-id");
        when(confluenceAttributes.getEffectiveClientSecret()).thenReturn("new-client-secret-that-is-long-enough-32ch");

        OAuth20Service secondService = oAuthService.getOauthService();
        assertNotNull(secondService);
        assertNotSame(firstService, secondService, "Should rebuild service when credentials change");
    }

    @Test
    void testGetOauthService_Rebuilds_WhenOnlyClientIdChanges() {
        OAuth20Service firstService = oAuthService.getOauthService();
        assertNotNull(firstService);

        // Change only clientId — any credential change should trigger a rebuild
        when(confluenceAttributes.getEffectiveClientId()).thenReturn("new-client-id");

        OAuth20Service secondService = oAuthService.getOauthService();
        assertNotSame(firstService, secondService,
                "Should rebuild when clientId changes");
    }

    @Test
    void testGetOauthService_Rebuilds_WhenOnlyClientSecretChanges() {
        OAuth20Service firstService = oAuthService.getOauthService();
        assertNotNull(firstService);

        // Change only clientSecret — the || fix means any single credential change triggers a rebuild
        when(confluenceAttributes.getEffectiveClientSecret())
                .thenReturn("brand-new-secret-that-is-long-enough-32ch");

        OAuth20Service secondService = oAuthService.getOauthService();
        assertNotSame(firstService, secondService,
                "Should rebuild when clientSecret changes");
    }

    @Test
    void testGetOauthService_DoesNotRebuild_WhenNothingChanges() {
        OAuth20Service firstService = oAuthService.getOauthService();
        assertNotNull(firstService);

        // Credentials remain unchanged — service must be reused
        OAuth20Service secondService = oAuthService.getOauthService();
        assertSame(firstService, secondService,
                "Should reuse the same service when credentials have not changed");
    }

    @Test
    void testGetOauthService_WhenNoOAuthCredentials_ReturnsNull() {
        when(confluenceAttributes.areOAuthCredentialsPresent()).thenReturn(false);

        ConfluenceOAuthService serviceNoOAuth = new ConfluenceOAuthService(requestContext, sessionManager, invoker,
                authorizationContext, confluenceAttributes);

        assertNull(serviceNoOAuth.getOauthService());
    }

    // --- SCOPES ---

    @Test
    void testScopesContains14Scopes() {
        assertEquals(14, ConfluenceOAuthService.SCOPES.size());
    }

    @Test
    void testScopesContainsRequiredPermissions() {
        assertTrue(ConfluenceOAuthService.SCOPES.contains("read:space:confluence"));
        assertTrue(ConfluenceOAuthService.SCOPES.contains("write:space:confluence"));
        assertTrue(ConfluenceOAuthService.SCOPES.contains("read:page:confluence"));
        assertTrue(ConfluenceOAuthService.SCOPES.contains("write:page:confluence"));
        assertTrue(ConfluenceOAuthService.SCOPES.contains("read:content:confluence"));
        assertTrue(ConfluenceOAuthService.SCOPES.contains("write:content:confluence"));
        assertTrue(ConfluenceOAuthService.SCOPES.contains("read:content-details:confluence"));
        assertTrue(ConfluenceOAuthService.SCOPES.contains("read:user:confluence"));
        assertTrue(ConfluenceOAuthService.SCOPES.contains("read:template:confluence"));
        assertTrue(ConfluenceOAuthService.SCOPES.contains("write:template:confluence"));
        assertTrue(ConfluenceOAuthService.SCOPES.contains("read:attachment:confluence"));
        assertTrue(ConfluenceOAuthService.SCOPES.contains("write:attachment:confluence"));
        assertTrue(ConfluenceOAuthService.SCOPES.contains("read:configuration:confluence"));
        assertTrue(ConfluenceOAuthService.SCOPES.contains("write:configuration:confluence"));
    }

    // --- accessible-resources URL (now AtlassianConstants.ACCESSIBLE_RESOURCES_URL) ---

    @Test
    void testResourceUrl() {
        assertEquals("https://api.atlassian.com/oauth/token/accessible-resources",
                AtlassianConstants.ACCESSIBLE_RESOURCES_URL);
    }

    // --- setCalledFromSetup ---

    @Test
    void testSetCalledFromSetup() {
        assertDoesNotThrow(() -> oAuthService.setCalledFromSetup(true));
        assertDoesNotThrow(() -> oAuthService.setCalledFromSetup(false));
    }

    // --- getConfluenceAccessToken(String, String) ---

    @Test
    void testGetConfluenceAccessToken_WhenRefreshTokenIsNull_ThrowsMustAuthorize() {
        when(requestContext.invokeAsUser()).thenReturn(false);
        when(sessionManager.get(anyString())).thenReturn(null);

        assertThrows(MustAuthorizeException.class,
                () -> oAuthService.getConfluenceAccessToken("accountId", "invokerId"));
    }

    @Test
    void testGetConfluenceAccessToken_InvokeAsUser_FallsBackToAccountKey() {
        oAuthService.setCalledFromSetup(false);
        when(requestContext.invokeAsUser()).thenReturn(true);
        when(sessionManager.get(anyString())).thenReturn(null);

        assertThrows(MustAuthorizeException.class,
                () -> oAuthService.getConfluenceAccessToken("accountId", "invokerId"));

        // Verify it tried the fallback key (accountId#invokerId)
        verify(sessionManager, atLeast(1)).get("accountId#invokerId");
    }

    @Test
    void testGetConfluenceAccessToken_WithValidRefreshToken() throws Exception {
        String refreshToken = "valid-refresh-token";
        when(requestContext.invokeAsUser()).thenReturn(false);
        String expectedKey = "test-client-id#test-client-secret-that-is-long-enough-32chars#invokerId";
        when(sessionManager.get(expectedKey)).thenReturn(refreshToken);

        OAuth20Service mockOAuthService = mock(OAuth20Service.class);
        OAuth2AccessToken mockToken = mock(OAuth2AccessToken.class);
        when(mockToken.getRefreshToken()).thenReturn("new-refresh-token");
        when(mockOAuthService.refreshAccessToken(refreshToken)).thenReturn(mockToken);

        ConfluenceOAuthService spiedService = spy(oAuthService);
        doReturn(mockOAuthService).when(spiedService).getOauthService();

        OAuth2AccessToken result = spiedService.getConfluenceAccessToken("accountId", "invokerId");

        assertNotNull(result);
        verify(sessionManager).store(expectedKey, "new-refresh-token");
    }

    @Test
    void testGetConfluenceAccessToken_CalledFromSetup_UsesClientCredentialsKey() {
        oAuthService.setCalledFromSetup(true);
        when(requestContext.invokeAsUser()).thenReturn(true);
        when(sessionManager.get(anyString())).thenReturn(null);

        assertThrows(MustAuthorizeException.class,
                () -> oAuthService.getConfluenceAccessToken("accountId", "invokerId"));
    }

    @Test
    void testGetConfluenceAccessToken_ExecutionException_WithNullCredentials_ThrowsIllegalCaller() {
        when(requestContext.invokeAsUser()).thenReturn(false);
        String expectedKey = "test-client-id#test-client-secret-that-is-long-enough-32chars#invokerId";
        when(sessionManager.get(expectedKey)).thenReturn("token");

        when(confluenceAttributes.getEffectiveClientId()).thenReturn(null);
        when(confluenceAttributes.getEffectiveClientSecret()).thenReturn(null);

        ConfluenceOAuthService spiedService = spy(oAuthService);
        OAuth20Service mockOAuthService = mock(OAuth20Service.class);
        doReturn(mockOAuthService).when(spiedService).getOauthService();

        try {
            when(mockOAuthService.refreshAccessToken("token"))
                    .thenThrow(new ExecutionException("test", new RuntimeException()));
        } catch (Exception ignored) {
        }

        assertThrows(IllegalCallerException.class,
                () -> spiedService.getConfluenceAccessToken("accountId", "invokerId"));
    }

    @Test
    void testGetConfluenceAccessToken_ExecutionException_WithNullClientSecretOnly_ThrowsIllegalCaller() {
        when(requestContext.invokeAsUser()).thenReturn(false);
        String expectedKey = "test-client-id#test-client-secret-that-is-long-enough-32chars#invokerId";
        when(sessionManager.get(expectedKey)).thenReturn("token");

        // clientId is non-null but clientSecret is null
        when(confluenceAttributes.getEffectiveClientSecret()).thenReturn(null);

        ConfluenceOAuthService spiedService = spy(oAuthService);
        OAuth20Service mockOAuthService = mock(OAuth20Service.class);
        doReturn(mockOAuthService).when(spiedService).getOauthService();

        try {
            when(mockOAuthService.refreshAccessToken("token"))
                    .thenThrow(new ExecutionException("test", new RuntimeException()));
        } catch (Exception ignored) {
        }

        assertThrows(IllegalCallerException.class,
                () -> spiedService.getConfluenceAccessToken("accountId", "invokerId"));
    }

    @Test
    void testGetConfluenceAccessToken_ExecutionException_WithValidCredentials_ThrowsMustAuthorize() {
        when(requestContext.invokeAsUser()).thenReturn(false);
        String expectedKey = "test-client-id#test-client-secret-that-is-long-enough-32chars#invokerId";
        when(sessionManager.get(expectedKey)).thenReturn("token");

        ConfluenceOAuthService spiedService = spy(oAuthService);
        OAuth20Service mockOAuthService = mock(OAuth20Service.class);
        doReturn(mockOAuthService).when(spiedService).getOauthService();

        try {
            when(mockOAuthService.refreshAccessToken("token"))
                    .thenThrow(new ExecutionException("test", new RuntimeException()));
        } catch (Exception ignored) {
        }

        assertThrows(MustAuthorizeException.class,
                () -> spiedService.getConfluenceAccessToken("accountId", "invokerId"));
    }

    @Test
    void testGetConfluenceAccessToken_RuntimeException_ThrowsMustAuthorize() {
        when(requestContext.invokeAsUser()).thenReturn(false);
        String expectedKey = "test-client-id#test-client-secret-that-is-long-enough-32chars#invokerId";
        when(sessionManager.get(expectedKey)).thenReturn("token");

        ConfluenceOAuthService spiedService = spy(oAuthService);
        OAuth20Service mockOAuthService = mock(OAuth20Service.class);
        doReturn(mockOAuthService).when(spiedService).getOauthService();

        try {
            when(mockOAuthService.refreshAccessToken("token")).thenThrow(new RuntimeException("runtime error"));
        } catch (Exception ignored) {
        }

        assertThrows(MustAuthorizeException.class,
                () -> spiedService.getConfluenceAccessToken("accountId", "invokerId"));
    }

    @Test
    void testGetConfluenceAccessToken_IOException_ThrowsRuntimeException() {
        when(requestContext.invokeAsUser()).thenReturn(false);
        String expectedKey = "test-client-id#test-client-secret-that-is-long-enough-32chars#invokerId";
        when(sessionManager.get(expectedKey)).thenReturn("token");

        ConfluenceOAuthService spiedService = spy(oAuthService);
        OAuth20Service mockOAuthService = mock(OAuth20Service.class);
        doReturn(mockOAuthService).when(spiedService).getOauthService();

        try {
            when(mockOAuthService.refreshAccessToken("token")).thenThrow(new IOException("IO error"));
        } catch (Exception ignored) {
        }

        assertThrows(RuntimeException.class,
                () -> spiedService.getConfluenceAccessToken("accountId", "invokerId"));
    }

    @Test
    void testGetConfluenceAccessToken_InterruptedException_ThrowsRuntimeException() {
        when(requestContext.invokeAsUser()).thenReturn(false);
        String expectedKey = "test-client-id#test-client-secret-that-is-long-enough-32chars#invokerId";
        when(sessionManager.get(expectedKey)).thenReturn("token");

        ConfluenceOAuthService spiedService = spy(oAuthService);
        OAuth20Service mockOAuthService = mock(OAuth20Service.class);
        doReturn(mockOAuthService).when(spiedService).getOauthService();

        try {
            when(mockOAuthService.refreshAccessToken("token")).thenThrow(new InterruptedException("interrupted"));
        } catch (Exception ignored) {
        }

        assertThrows(RuntimeException.class,
                () -> spiedService.getConfluenceAccessToken("accountId", "invokerId"));
    }

    @Test
    void testGetConfluenceAccessToken_MustAuthorizeException_IsRethrown() {
        when(requestContext.invokeAsUser()).thenReturn(false);
        when(sessionManager.get(anyString())).thenReturn(null);

        MustAuthorizeException thrown = assertThrows(MustAuthorizeException.class,
                () -> oAuthService.getConfluenceAccessToken("accountId", "invokerId"));
        assertNotNull(thrown.getMessage());
    }

    // --- getConfluenceAccessToken() (no-args) ---

    @Test
    void testGetConfluenceAccessToken_NoArgs_UsesContextValues() {
        when(authorizationContext.getAuthorizedAccount()).thenReturn(authorizedAccount);
        when(authorizedAccount.getAccountId()).thenReturn("test-account-id");
        when(requestContext.getInvokerId()).thenReturn("test-invoker-id");
        when(requestContext.invokeAsUser()).thenReturn(false);
        when(sessionManager.get(anyString())).thenReturn(null);

        assertThrows(MustAuthorizeException.class,
                () -> oAuthService.getConfluenceAccessToken());
    }

    // --- getConfluenceToken ---

    @Test
    void testGetConfluenceToken_WhenAccessTokenAvailable() throws Exception {
        when(authorizationContext.getAuthorizedAccount()).thenReturn(authorizedAccount);
        when(authorizedAccount.getAccountId()).thenReturn("account-id");
        when(requestContext.getInvokerId()).thenReturn("invoker-id");
        when(requestContext.invokeAsUser()).thenReturn(false);

        String expectedKey = "test-client-id#test-client-secret-that-is-long-enough-32chars#invoker-id";
        when(sessionManager.get(expectedKey)).thenReturn("refresh-token");

        ConfluenceOAuthService spiedService = spy(oAuthService);
        OAuth20Service mockOAuthService = mock(OAuth20Service.class);
        doReturn(mockOAuthService).when(spiedService).getOauthService();

        OAuth2AccessToken mockToken = mock(OAuth2AccessToken.class);
        when(mockToken.getAccessToken()).thenReturn("access-token-123");
        when(mockToken.getRefreshToken()).thenReturn("new-refresh-token");
        when(mockOAuthService.refreshAccessToken("refresh-token")).thenReturn(mockToken);

        String token = spiedService.getConfluenceToken();
        assertEquals("access-token-123", token);
    }

    // --- getAccessibleResources ---

    @Test
    void testGetAccessibleResources_BadRequest_ThrowsIllegalState() throws Exception {
        when(authorizationContext.getAuthorizedAccount()).thenReturn(authorizedAccount);
        when(authorizedAccount.getAccountId()).thenReturn("account-id");
        when(requestContext.getInvokerId()).thenReturn("invoker-id");
        when(requestContext.invokeAsUser()).thenReturn(false);

        String expectedKey = "test-client-id#test-client-secret-that-is-long-enough-32chars#invoker-id";
        when(sessionManager.get(expectedKey)).thenReturn("refresh-token");

        ConfluenceOAuthService spiedService = spy(oAuthService);
        OAuth20Service mockOAuthService = mock(OAuth20Service.class);
        doReturn(mockOAuthService).when(spiedService).getOauthService();

        OAuth2AccessToken mockToken = mock(OAuth2AccessToken.class);
        when(mockToken.getAccessToken()).thenReturn("access-token");
        when(mockToken.getRefreshToken()).thenReturn("new-refresh");
        when(mockOAuthService.refreshAccessToken("refresh-token")).thenReturn(mockToken);

        Response mockResponse = mock(Response.class);
        when(mockResponse.getCode()).thenReturn(400);
        when(mockResponse.getBody()).thenReturn("Bad Request");
        when(mockOAuthService.execute(any())).thenReturn(mockResponse);

        assertThrows(IllegalStateException.class, () -> spiedService.getAccessibleResources());
    }

    @Test
    void testGetAccessibleResources_EmptyArray_ThrowsIllegalState() throws Exception {
        when(authorizationContext.getAuthorizedAccount()).thenReturn(authorizedAccount);
        when(authorizedAccount.getAccountId()).thenReturn("account-id");
        when(requestContext.getInvokerId()).thenReturn("invoker-id");
        when(requestContext.invokeAsUser()).thenReturn(false);

        String expectedKey = "test-client-id#test-client-secret-that-is-long-enough-32chars#invoker-id";
        when(sessionManager.get(expectedKey)).thenReturn("refresh-token");

        ConfluenceOAuthService spiedService = spy(oAuthService);
        OAuth20Service mockOAuthService = mock(OAuth20Service.class);
        doReturn(mockOAuthService).when(spiedService).getOauthService();

        OAuth2AccessToken mockToken = mock(OAuth2AccessToken.class);
        when(mockToken.getAccessToken()).thenReturn("access-token");
        when(mockToken.getRefreshToken()).thenReturn("new-refresh");
        when(mockOAuthService.refreshAccessToken("refresh-token")).thenReturn(mockToken);

        Response mockResponse = mock(Response.class);
        when(mockResponse.getCode()).thenReturn(200);
        when(mockResponse.getBody()).thenReturn("[]");
        when(mockOAuthService.execute(any())).thenReturn(mockResponse);

        assertThrows(IllegalStateException.class, () -> spiedService.getAccessibleResources());
    }

    @Test
    void testGetAccessibleResources_Success() throws Exception {
        when(authorizationContext.getAuthorizedAccount()).thenReturn(authorizedAccount);
        when(authorizedAccount.getAccountId()).thenReturn("account-id");
        when(requestContext.getInvokerId()).thenReturn("invoker-id");
        when(requestContext.invokeAsUser()).thenReturn(false);

        String expectedKey = "test-client-id#test-client-secret-that-is-long-enough-32chars#invoker-id";
        when(sessionManager.get(expectedKey)).thenReturn("refresh-token");

        ConfluenceOAuthService spiedService = spy(oAuthService);
        OAuth20Service mockOAuthService = mock(OAuth20Service.class);
        doReturn(mockOAuthService).when(spiedService).getOauthService();

        OAuth2AccessToken mockToken = mock(OAuth2AccessToken.class);
        when(mockToken.getAccessToken()).thenReturn("access-token");
        when(mockToken.getRefreshToken()).thenReturn("new-refresh");
        when(mockOAuthService.refreshAccessToken("refresh-token")).thenReturn(mockToken);

        Response mockResponse = mock(Response.class);
        when(mockResponse.getCode()).thenReturn(200);
        when(mockResponse.getBody()).thenReturn("[{\"id\":\"cloud-id-123\",\"scopes\":[\"read:space:confluence\"]}]");
        when(mockOAuthService.execute(any())).thenReturn(mockResponse);

        AccessibleResource resources = spiedService.getAccessibleResources();

        assertNotNull(resources);
        assertEquals("cloud-id-123", resources.getId());
    }

    @Test
    void testGetAccessibleResources_MustAuthorizeFromAccessToken_PropagatesDirectly() {
        when(authorizationContext.getAuthorizedAccount()).thenReturn(authorizedAccount);
        when(authorizedAccount.getAccountId()).thenReturn("account-id");
        when(requestContext.getInvokerId()).thenReturn("invoker-id");
        when(requestContext.invokeAsUser()).thenReturn(false);
        when(sessionManager.get(anyString())).thenReturn(null);

        // getConfluenceAccessToken throws MustAuthorizeException which propagates directly
        // (it's thrown before the try-with-resources block in getAccessibleResources)
        assertThrows(MustAuthorizeException.class,
                () -> oAuthService.getAccessibleResources());
    }

    @Test
    void testGetAccessibleResources_MustAuthorizeFromExecute_IsWrapped() throws Exception {
        when(authorizationContext.getAuthorizedAccount()).thenReturn(authorizedAccount);
        when(authorizedAccount.getAccountId()).thenReturn("account-id");
        when(requestContext.getInvokerId()).thenReturn("invoker-id");
        when(requestContext.invokeAsUser()).thenReturn(false);

        String expectedKey = "test-client-id#test-client-secret-that-is-long-enough-32chars#invoker-id";
        when(sessionManager.get(expectedKey)).thenReturn("refresh-token");

        ConfluenceOAuthService spiedService = spy(oAuthService);
        OAuth20Service mockOAuthService = mock(OAuth20Service.class);
        doReturn(mockOAuthService).when(spiedService).getOauthService();

        OAuth2AccessToken mockToken = mock(OAuth2AccessToken.class);
        when(mockToken.getAccessToken()).thenReturn("access-token");
        when(mockToken.getRefreshToken()).thenReturn("new-refresh");
        when(mockOAuthService.refreshAccessToken("refresh-token")).thenReturn(mockToken);

        when(mockOAuthService.execute(any())).thenThrow(new MustAuthorizeException("re-authorize"));

        MustAuthorizeException thrown = assertThrows(MustAuthorizeException.class,
                () -> spiedService.getAccessibleResources());
        assertTrue(thrown.getMessage().contains("Exception"));
    }

    // --- SDK AccessibleResource ---

    @Test
    void testAccessibleResources_Constructor_AndGetId() {
        AccessibleResource resources =
                new AccessibleResource("test-cloud-id", List.of("scope1", "scope2"));

        assertEquals("test-cloud-id", resources.getId());
    }

    @Test
    void testAccessibleResources_NullId() {
        AccessibleResource resources =
                new AccessibleResource(null, List.of());

        assertNull(resources.getId());
    }
}
