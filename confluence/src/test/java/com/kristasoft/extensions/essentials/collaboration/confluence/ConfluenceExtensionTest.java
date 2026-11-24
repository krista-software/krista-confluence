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

package com.kristasoft.extensions.essentials.collaboration.confluence;

import app.krista.extension.authorization.MustAuthorizeException;
import app.krista.extension.authorization.RequestAuthenticator;
import com.kristasoft.extensions.essentials.collaboration.confluence.api.APIVersion;
import com.kristasoft.extensions.essentials.collaboration.confluence.authentication.ConfluenceRequestAuthenticator;
import com.kristasoft.extensions.essentials.collaboration.confluence.client.ConfluenceRESTClient;
import com.kristasoft.extensions.essentials.collaboration.confluence.utils.ConfluenceConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.HttpURLConnection;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class ConfluenceExtensionTest {

    @Mock
    private ConfluenceAttributes confluenceAttributes;

    @Mock
    private ConfluenceRequestAuthenticator confluenceRequestAuthenticator;

    @Mock
    private ConfluenceRESTClient restClient;

    @Mock
    private HttpResponse<String> httpResponse;

    private ConfluenceExtension confluenceExtension;

    @BeforeEach
    void setUp() {
        confluenceExtension = new ConfluenceExtension(confluenceAttributes, confluenceRequestAuthenticator);
    }

    @Test
    void testConstructor() {
        assertNotNull(confluenceExtension);
    }

    // --- customTabs Tests ---

    @Test
    void testCustomTabs_ReturnsDocumentationTab() {
        Map<String, String> tabs = confluenceExtension.customTabs();

        assertNotNull(tabs);
        assertEquals(4, tabs.size());
        assertTrue(tabs.containsKey("Documentation"));
        assertEquals("static/docs", tabs.get("Documentation"));
        assertTrue(tabs.containsKey("Authentication"));
        assertEquals("confluenceextn/sdk/auth/", tabs.get("Authentication"));
        assertTrue(tabs.containsKey("Confluence Audit"));
        assertEquals("confluenceextn/sdk/audit/", tabs.get("Confluence Audit"));
        assertTrue(tabs.containsKey("AI Assistant"));
        assertEquals("confluenceextn/mcp-setup", tabs.get("AI Assistant"));
    }

    // --- validateAttributes Tests ---

    @Test
    void testValidateAttributes_Success_BasicAuth() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(ConfluenceConstants.DOMAIN_NAME, "myDomain");
        attributes.put(ConfluenceConstants.AUTHORIZATION_EMAIL, "user@example.com");
        attributes.put(ConfluenceConstants.API_KEY, "apiKey123");

        when(confluenceAttributes.getRestClient()).thenReturn(restClient);
        when(restClient.doSyncGet(eq(APIVersion.V2), isNull(), eq("spaces"))).thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);

        assertDoesNotThrow(() -> confluenceExtension.validateAttributes(attributes));

        verify(confluenceAttributes).setAttributes(attributes);
        verify(confluenceAttributes).validateAttributes(attributes);
        verify(confluenceAttributes).getRestClient();
    }

    @Test
    void testValidateAttributes_FailedHttpResponse() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(ConfluenceConstants.DOMAIN_NAME, "myDomain");
        attributes.put(ConfluenceConstants.AUTHORIZATION_EMAIL, "user@example.com");
        attributes.put(ConfluenceConstants.API_KEY, "apiKey123");

        when(confluenceAttributes.getRestClient()).thenReturn(restClient);
        when(restClient.doSyncGet(eq(APIVersion.V2), isNull(), eq("spaces"))).thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_UNAUTHORIZED);
        when(httpResponse.body()).thenReturn("Unauthorized access");

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> confluenceExtension.validateAttributes(attributes));
        assertEquals("Unauthorized access", exception.getMessage());
    }

    @Test
    void testValidateAttributes_NullResponse() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(ConfluenceConstants.DOMAIN_NAME, "myDomain");
        attributes.put(ConfluenceConstants.AUTHORIZATION_EMAIL, "user@example.com");
        attributes.put(ConfluenceConstants.API_KEY, "apiKey123");

        when(confluenceAttributes.getRestClient()).thenReturn(restClient);
        when(restClient.doSyncGet(eq(APIVersion.V2), isNull(), eq("spaces"))).thenReturn(null);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> confluenceExtension.validateAttributes(attributes));
        assertEquals("Failed to get api response", exception.getMessage());
    }

    @Test
    void testValidateAttributes_MustAuthorizeException_Rethrown() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(ConfluenceConstants.CLIENT_ID, "client-id");
        attributes.put(ConfluenceConstants.CLIENT_SECRET, "client-secret");

        doThrow(new MustAuthorizeException("Authorization needed"))
                .when(confluenceAttributes).validateAttributes(attributes);

        assertThrows(MustAuthorizeException.class,
                () -> confluenceExtension.validateAttributes(attributes));
    }

    @Test
    void testValidateAttributes_IllegalArgumentException_Rethrown() {
        Map<String, Object> attributes = new HashMap<>();

        doThrow(new IllegalArgumentException("Invalid credentials"))
                .when(confluenceAttributes).validateAttributes(attributes);

        assertThrows(IllegalArgumentException.class,
                () -> confluenceExtension.validateAttributes(attributes));
    }

    @Test
    void testValidateAttributes_SetsAttributesBeforeValidation() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(ConfluenceConstants.DOMAIN_NAME, "domain");
        attributes.put(ConfluenceConstants.AUTHORIZATION_EMAIL, "user@test.com");
        attributes.put(ConfluenceConstants.API_KEY, "key");

        when(confluenceAttributes.getRestClient()).thenReturn(restClient);
        when(restClient.doSyncGet(eq(APIVersion.V2), isNull(), eq("spaces"))).thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);

        confluenceExtension.validateAttributes(attributes);

        // Verify order: setAttributes is called before validateAttributes
        var inOrder = inOrder(confluenceAttributes);
        inOrder.verify(confluenceAttributes).setAttributes(attributes);
        inOrder.verify(confluenceAttributes).validateAttributes(attributes);
        inOrder.verify(confluenceAttributes).getRestClient();
    }

    // --- testConnection Tests ---

    @Test
    void testTestConnection_Success() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(ConfluenceConstants.DOMAIN_NAME, "domain");
        attributes.put(ConfluenceConstants.AUTHORIZATION_EMAIL, "user@test.com");
        attributes.put(ConfluenceConstants.API_KEY, "key");

        when(confluenceAttributes.getAttributes()).thenReturn(attributes);
        when(confluenceAttributes.getRestClient()).thenReturn(restClient);
        when(restClient.doSyncGet(eq(APIVersion.V2), isNull(), eq("spaces"))).thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);

        assertDoesNotThrow(() -> confluenceExtension.testConnection());

        verify(confluenceAttributes).getAttributes();
    }

    @Test
    void testTestConnection_Failure() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(ConfluenceConstants.DOMAIN_NAME, "domain");

        when(confluenceAttributes.getAttributes()).thenReturn(attributes);
        doThrow(new IllegalArgumentException("Invalid")).when(confluenceAttributes).validateAttributes(attributes);

        assertThrows(IllegalArgumentException.class, () -> confluenceExtension.testConnection());
    }

    // --- getRequestAuthenticator Tests ---

    @Test
    void testGetRequestAuthenticator_ReturnsAuthenticator() {
        RequestAuthenticator authenticator = confluenceExtension.getRequestAuthenticator();

        assertNotNull(authenticator);
        assertSame(confluenceRequestAuthenticator, authenticator);
    }

    @Test
    void testGetRequestAuthenticator_ReturnsConfluenceRequestAuthenticator() {
        RequestAuthenticator authenticator = confluenceExtension.getRequestAuthenticator();

        assertInstanceOf(ConfluenceRequestAuthenticator.class, authenticator);
    }

    // --- OAuth validateAttributes flow ---

    @Test
    void testValidateAttributes_OAuthFlow_MustAuthorizeFromGetRestClient() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(ConfluenceConstants.CLIENT_ID, "valid-client-id");
        attributes.put(ConfluenceConstants.CLIENT_SECRET, "valid-client-secret-long-enough-32chars");

        when(confluenceAttributes.getRestClient())
                .thenThrow(new MustAuthorizeException("Authorize yourself"));

        assertThrows(MustAuthorizeException.class,
                () -> confluenceExtension.validateAttributes(attributes));
    }

    @Test
    void testValidateAttributes_OAuthFlow_Success() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(ConfluenceConstants.CLIENT_ID, "valid-client-id");
        attributes.put(ConfluenceConstants.CLIENT_SECRET, "valid-client-secret-long-enough-32chars");

        when(confluenceAttributes.getRestClient()).thenReturn(restClient);
        when(restClient.doSyncGet(eq(APIVersion.V2), isNull(), eq("spaces"))).thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);

        assertDoesNotThrow(() -> confluenceExtension.validateAttributes(attributes));
    }

    @Test
    void testValidateAttributes_HttpBadRequest() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(ConfluenceConstants.DOMAIN_NAME, "domain");
        attributes.put(ConfluenceConstants.AUTHORIZATION_EMAIL, "user@test.com");
        attributes.put(ConfluenceConstants.API_KEY, "key");

        when(confluenceAttributes.getRestClient()).thenReturn(restClient);
        when(restClient.doSyncGet(eq(APIVersion.V2), isNull(), eq("spaces"))).thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_BAD_REQUEST);
        when(httpResponse.body()).thenReturn("Bad Request");

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> confluenceExtension.validateAttributes(attributes));
        assertEquals("Bad Request", exception.getMessage());
    }

    @Test
    void testValidateAttributes_HttpForbidden() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(ConfluenceConstants.DOMAIN_NAME, "domain");
        attributes.put(ConfluenceConstants.AUTHORIZATION_EMAIL, "user@test.com");
        attributes.put(ConfluenceConstants.API_KEY, "key");

        when(confluenceAttributes.getRestClient()).thenReturn(restClient);
        when(restClient.doSyncGet(eq(APIVersion.V2), isNull(), eq("spaces"))).thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_FORBIDDEN);
        when(httpResponse.body()).thenReturn("Forbidden");

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> confluenceExtension.validateAttributes(attributes));
        assertEquals("Forbidden", exception.getMessage());
    }
}
