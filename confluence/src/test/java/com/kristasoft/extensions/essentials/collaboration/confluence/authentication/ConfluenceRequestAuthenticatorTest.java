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

import app.krista.extension.authorization.MustAuthenticateException;
import app.krista.extension.authorization.MustAuthorizeException;
import app.krista.extension.authorization.RequestAuthenticator;
import app.krista.extension.request.ProtoRequest;
import app.krista.extension.request.protos.http.HttpRequest;
import app.krista.ksdk.accounts.Account;
import app.krista.ksdk.context.AuthorizationContext;
import com.github.scribejava.core.oauth.OAuth20Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfluenceRequestAuthenticatorTest {

    @Mock
    private ConfluenceOAuthService confluenceOAuthService;

    @Mock
    private AuthorizationContext authorizationContext;

    @Mock
    private Account authorizedAccount;

    @Mock
    private OAuth20Service oAuth20Service;

    private ConfluenceRequestAuthenticator authenticator;

    @BeforeEach
    void setUp() {
        authenticator = new ConfluenceRequestAuthenticator(confluenceOAuthService, authorizationContext);
    }

    @Test
    void testGetScheme_ReturnsNull() {
        assertNull(authenticator.getScheme());
    }

    @Test
    void testGetSupportedProtocols_ReturnsEmptySet() {
        assertTrue(authenticator.getSupportedProtocols().isEmpty());
    }

    @Test
    void testGetAuthenticatedAccountId_WithCallbackPath_ReturnsState() throws IOException {
        HttpRequest mockRequest = mock(HttpRequest.class);
        URI mockUri = mock(URI.class);
        when(mockUri.getPath()).thenReturn("/callback");
        when(mockRequest.getUri()).thenReturn(mockUri);

        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.put("state", List.of("test-account-id"));
        when(mockRequest.getQueryParameters()).thenReturn(queryParams);

        String result = authenticator.getAuthenticatedAccountId(mockRequest);

        assertEquals("test-account-id", result);
        verify(mockRequest).bufferBody();
    }

    @Test
    void testGetAuthenticatedAccountId_WithNonCallbackPath_ReturnsNull() {
        HttpRequest mockRequest = mock(HttpRequest.class);
        URI mockUri = mock(URI.class);
        when(mockUri.getPath()).thenReturn("/other-path");
        when(mockRequest.getUri()).thenReturn(mockUri);

        String result = authenticator.getAuthenticatedAccountId(mockRequest);

        assertNull(result);
    }

    @Test
    void testGetAuthenticatedAccountId_WithNullQueryParameters_ReturnsNull() throws IOException {
        HttpRequest mockRequest = mock(HttpRequest.class);
        URI mockUri = mock(URI.class);
        when(mockUri.getPath()).thenReturn("/callback");
        when(mockRequest.getUri()).thenReturn(mockUri);
        when(mockRequest.getQueryParameters()).thenReturn(null);

        String result = authenticator.getAuthenticatedAccountId(mockRequest);

        assertNull(result);
    }

    @Test
    void testGetAuthenticatedAccountId_WithNullStateParam_ReturnsNull() throws IOException {
        HttpRequest mockRequest = mock(HttpRequest.class);
        URI mockUri = mock(URI.class);
        when(mockUri.getPath()).thenReturn("/callback");
        when(mockRequest.getUri()).thenReturn(mockUri);

        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        when(mockRequest.getQueryParameters()).thenReturn(queryParams);

        String result = authenticator.getAuthenticatedAccountId(mockRequest);

        assertNull(result);
    }

    @Test
    void testGetAuthenticatedAccountId_WithNullStateValue_ReturnsNull() throws IOException {
        HttpRequest mockRequest = mock(HttpRequest.class);
        URI mockUri = mock(URI.class);
        when(mockUri.getPath()).thenReturn("/callback");
        when(mockRequest.getUri()).thenReturn(mockUri);

        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.put("state", Collections.singletonList(null));
        when(mockRequest.getQueryParameters()).thenReturn(queryParams);

        String result = authenticator.getAuthenticatedAccountId(mockRequest);

        assertNull(result);
    }

    @Test
    void testGetAuthenticatedAccountId_RuntimeException_ReturnsNull() {
        HttpRequest mockRequest = mock(HttpRequest.class);
        when(mockRequest.getUri()).thenThrow(new RuntimeException("test error"));

        String result = authenticator.getAuthenticatedAccountId(mockRequest);

        assertNull(result);
    }

    @Test
    void testGetAuthenticatedAccountId_IOException_ReturnsNull() throws IOException {
        HttpRequest mockRequest = mock(HttpRequest.class);
        URI mockUri = mock(URI.class);
        when(mockUri.getPath()).thenReturn("/callback");
        when(mockRequest.getUri()).thenReturn(mockUri);
        doThrow(new IOException("IO error")).when(mockRequest).bufferBody();

        String result = authenticator.getAuthenticatedAccountId(mockRequest);

        assertNull(result);
    }

    @Test
    void testSetServiceAuthorization_ReturnsFalse() {
        assertFalse(authenticator.setServiceAuthorization("any-string"));
    }

    @Test
    void testGetAttributeFields_ReturnsEmptyMap() {
        assertTrue(authenticator.getAttributeFields().isEmpty());
    }

    @Test
    void testGetMustAuthenticateResponse_WithProtoRequest_ReturnsNull() {
        MustAuthenticateException cause = new MustAuthenticateException("test");
        ProtoRequest protoRequest = mock(ProtoRequest.class);

        assertNull(authenticator.getMustAuthenticateResponse(cause, protoRequest));
    }

    @Test
    void testGetMustAuthenticateResponse_WithoutProtoRequest_ReturnsNull() {
        MustAuthenticateException cause = new MustAuthenticateException("test");

        assertNull(authenticator.getMustAuthenticateResponse(cause));
    }

    @Test
    void testGetMustAuthorizeResponse_WithProtoRequest_ReturnsNull() {
        MustAuthorizeException cause = new MustAuthorizeException("test");
        ProtoRequest protoRequest = mock(ProtoRequest.class);

        assertNull(authenticator.getMustAuthorizeResponse(cause, protoRequest));
    }

    @Test
    void testGetMustAuthorizeResponse_ReturnsAuthorizationResponse() {
        MustAuthorizeException cause = new MustAuthorizeException("test");
        when(authorizationContext.getAuthorizedAccount()).thenReturn(authorizedAccount);
        when(authorizedAccount.getAccountId()).thenReturn("test-account-123");
        when(confluenceOAuthService.buildSecuredAuthorizationUrl("test-account-123"))
                .thenReturn("https://auth.atlassian.com/authorize?client_id=xyz&state=test-account-123");

        RequestAuthenticator.AuthorizationResponse response = authenticator.getMustAuthorizeResponse(cause);

        assertNotNull(response);
        String location = response.getLocation();
        assertTrue(location.contains("https://auth.atlassian.com/authorize"));
        assertTrue(location.contains("audience=api.atlassian.com"));
        assertTrue(location.contains("prompt=consent"));
    }

    @Test
    void testGetMustAuthorizeResponse_LocationContainsAudienceAndPrompt() {
        MustAuthorizeException cause = new MustAuthorizeException("authorization needed");
        when(authorizationContext.getAuthorizedAccount()).thenReturn(authorizedAccount);
        when(authorizedAccount.getAccountId()).thenReturn("acc-456");
        when(confluenceOAuthService.buildSecuredAuthorizationUrl("acc-456"))
                .thenReturn("https://auth.atlassian.com/authorize?client_id=abc");

        RequestAuthenticator.AuthorizationResponse response = authenticator.getMustAuthorizeResponse(cause);

        assertNotNull(response);
        String expectedSuffix = "&audience=api.atlassian.com&prompt=consent";
        assertTrue(response.getLocation().endsWith(expectedSuffix),
                "Location should end with audience and prompt parameters");
    }
}
