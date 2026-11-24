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

import app.krista.extension.executor.Invoker;
import app.krista.sdk.atlassian.oauth.AccessibleResource;
import app.krista.sdk.atlassian.oauth.ClientCredentialsTokenProvider;
import app.krista.ksdk.context.AuthorizationContext;
import app.krista.ksdk.context.RequestContext;
import com.kristasoft.extensions.essentials.collaboration.confluence.authentication.ConfluenceOAuthService;
import com.kristasoft.extensions.essentials.collaboration.confluence.authentication.SessionManager;
import com.kristasoft.extensions.essentials.collaboration.confluence.client.ConfluenceRESTClient;
import com.kristasoft.extensions.essentials.collaboration.confluence.utils.ConfluenceConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConfluenceAttributes Unit Tests")
class ConfluenceAttributesTest {

    @Mock
    private Invoker invoker;

    @Mock
    private RequestContext requestContext;

    @Mock
    private SessionManager sessionManager;

    @Mock
    private AuthorizationContext authorizationContext;

    private ConfluenceAttributes confluenceAttributes;

    @BeforeEach
    void setUp() {
        when(invoker.getAttributes()).thenReturn(new HashMap<>());
        confluenceAttributes = new ConfluenceAttributes(invoker, requestContext, sessionManager, authorizationContext);
    }

    @Test
    @DisplayName("Should initialize correctly with invoker")
    void testConstructor_WithInvoker() {
        assertNotNull(confluenceAttributes);
        assertNotNull(confluenceAttributes.getAttributes());
    }

    @Test
    @DisplayName("Should initialize correctly with basic auth parameters")
    void testConstructor_WithBasicAuthParams() {
        ConfluenceAttributes basicAttrs = new ConfluenceAttributes("testDomain", "test@email.com", "apiKey123");
        assertEquals("testDomain", basicAttrs.getDomainName());
        assertEquals("test@email.com", basicAttrs.getAccountEmail());
        assertEquals("apiKey123", basicAttrs.getApiKey());
    }

    @Test
    @DisplayName("Should get domain name from attributes")
    void testGetDomainName_FromAttributes() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(ConfluenceConstants.DOMAIN_NAME, "myDomain");
        confluenceAttributes.setAttributes(attrs);

        assertEquals("myDomain", confluenceAttributes.getDomainName());
    }

    @Test
    @DisplayName("Should default domain name to empty string")
    void testGetDomainName_DefaultsToEmptyString() {
        Map<String, Object> attrs = new HashMap<>();
        confluenceAttributes.setAttributes(attrs);

        assertEquals("", confluenceAttributes.getDomainName());
    }

    @Test
    @DisplayName("Should get account email from attributes")
    void testGetAccountEmail_FromAttributes() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(ConfluenceConstants.AUTHORIZATION_EMAIL, "user@test.com");
        confluenceAttributes.setAttributes(attrs);

        assertEquals("user@test.com", confluenceAttributes.getAccountEmail());
    }

    @Test
    @DisplayName("Should default account email to empty string")
    void testGetAccountEmail_DefaultsToEmptyString() {
        Map<String, Object> attrs = new HashMap<>();
        confluenceAttributes.setAttributes(attrs);

        assertEquals("", confluenceAttributes.getAccountEmail());
    }

    @Test
    @DisplayName("Should get API key from attributes")
    void testGetApiKey_FromAttributes() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(ConfluenceConstants.API_KEY, "secret-api-key");
        confluenceAttributes.setAttributes(attrs);

        assertEquals("secret-api-key", confluenceAttributes.getApiKey());
    }

    @Test
    @DisplayName("Should default API key to empty string")
    void testGetApiKey_DefaultsToEmptyString() {
        Map<String, Object> attrs = new HashMap<>();
        confluenceAttributes.setAttributes(attrs);

        assertEquals("", confluenceAttributes.getApiKey());
    }

    @Test
    @DisplayName("Should get client ID from attributes")
    void testGetClientId_FromAttributes() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(ConfluenceConstants.CLIENT_ID, "my-client-id");
        confluenceAttributes.setAttributes(attrs);

        assertEquals("my-client-id", confluenceAttributes.getClientId());
    }

    @Test
    @DisplayName("Should fall back to invoker attributes for client ID when snapshot absent")
    void testGetClientId_FallsBackToInvokerAttributes() {
        Map<String, Object> invokerAttrs = new HashMap<>();
        invokerAttrs.put(ConfluenceConstants.CLIENT_ID, "invoker-client-id");
        // Resetting confluenceAttributes to use the updated invoker mock
        when(invoker.getAttributes()).thenReturn(invokerAttrs);
        confluenceAttributes = new ConfluenceAttributes(invoker, requestContext, sessionManager, authorizationContext);

        assertEquals("invoker-client-id", confluenceAttributes.getClientId());
    }

    @Test
    @DisplayName("Should prefer credential snapshot over invoker attributes for client ID")
    void testGetClientId_PrefersSnapshot_OverInvokerAttribute() {
        Map<String, Object> invokerAttrs = new HashMap<>();
        invokerAttrs.put(ConfluenceConstants.CLIENT_ID, "stale-invoker-id");
        when(invoker.getAttributes()).thenReturn(invokerAttrs);
        when(requestContext.getInvokerId()).thenReturn("inv-1");
        when(sessionManager.get("cred.inv-1.clientId")).thenReturn("fresh-snap-id");
        confluenceAttributes = new ConfluenceAttributes(invoker, requestContext, sessionManager, authorizationContext);

        assertEquals("fresh-snap-id", confluenceAttributes.getClientId());
    }

    @Test
    @DisplayName("Should return null for client ID when not available")
    void testGetClientId_ReturnsNull_WhenNotAvailable() {
        assertNull(confluenceAttributes.getClientId());
    }

    @Test
    @DisplayName("Should get client secret from attributes")
    void testGetClientSecret_FromAttributes() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(ConfluenceConstants.CLIENT_SECRET, "REDACTED_SECRET");
        confluenceAttributes.setAttributes(attrs);

        assertEquals("my-client-secret-long-enough-value-32", confluenceAttributes.getClientSecret());
    }

    @Test
    @DisplayName("Should fall back to invoker attributes for client secret when snapshot absent")
    void testGetClientSecret_FallsBackToInvokerAttributes() {
        Map<String, Object> invokerAttrs = new HashMap<>();
        invokerAttrs.put(ConfluenceConstants.CLIENT_SECRET, "invoker-client-secret-long-enough");
        when(invoker.getAttributes()).thenReturn(invokerAttrs);
        confluenceAttributes = new ConfluenceAttributes(invoker, requestContext, sessionManager, authorizationContext);

        assertEquals("invoker-client-secret-long-enough", confluenceAttributes.getClientSecret());
    }

    @Test
    @DisplayName("Should prefer credential snapshot over invoker attributes for client secret")
    void testGetClientSecret_PrefersSnapshot_OverInvokerAttribute() {
        Map<String, Object> invokerAttrs = new HashMap<>();
        invokerAttrs.put(ConfluenceConstants.CLIENT_SECRET, "stale-invoker-secret");
        when(invoker.getAttributes()).thenReturn(invokerAttrs);
        when(requestContext.getInvokerId()).thenReturn("inv-1");
        when(sessionManager.get("cred.inv-1.clientSecret")).thenReturn("fresh-snap-secret");
        confluenceAttributes = new ConfluenceAttributes(invoker, requestContext, sessionManager, authorizationContext);

        assertEquals("fresh-snap-secret", confluenceAttributes.getClientSecret());
    }

    @Test
    @DisplayName("Should return null for client secret when not available")
    void testGetClientSecret_ReturnsNull_WhenNotAvailable() {
        assertNull(confluenceAttributes.getClientSecret());
    }

    @Test
    @DisplayName("Should reset REST client when attributes are updated")
    void testSetAttributes_ResetsRestClient() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(ConfluenceConstants.CLIENT_ID, "new-client-id");
        attrs.put(ConfluenceConstants.CLIENT_SECRET, "new-client-secret");

        confluenceAttributes.setAttributes(attrs);

        assertEquals("new-client-id", confluenceAttributes.getClientId());
        assertEquals("new-client-secret", confluenceAttributes.getClientSecret());
    }

    @Test
    @DisplayName("Should persist OAuth credentials to snapshot when setAttributes is called")
    void testSetAttributes_PersistsOAuthToSnapshot() {
        when(requestContext.getInvokerId()).thenReturn("inv-42");

        Map<String, Object> attrs = new HashMap<>();
        attrs.put(ConfluenceConstants.CLIENT_ID, "snap-client-id");
        attrs.put(ConfluenceConstants.CLIENT_SECRET, "snap-client-secret");
        attrs.put(ConfluenceConstants.CREDENTIALS_MODE, "own");

        confluenceAttributes.setAttributes(attrs);

        verify(sessionManager).store("cred.inv-42.clientId", "snap-client-id");
        verify(sessionManager).store("cred.inv-42.clientSecret", "snap-client-secret");
        verify(sessionManager).store("cred.inv-42.credentialsMode", "own");
    }

    @Test
    @DisplayName("Should not persist to snapshot when OAuth credentials are absent")
    void testSetAttributes_DoesNotPersistWhenNoOAuth() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(ConfluenceConstants.DOMAIN_NAME, "myDomain");
        attrs.put(ConfluenceConstants.AUTHORIZATION_EMAIL, "user@test.com");
        attrs.put(ConfluenceConstants.API_KEY, "apikey");

        confluenceAttributes.setAttributes(attrs);

        verify(sessionManager, never()).store(anyString(), anyString());
    }

    @Test
    @DisplayName("Should generate correct domain URL")
    void testGetDomainURL() {
        ConfluenceAttributes basicAttrs = new ConfluenceAttributes("mycompany", "test@email.com", "apiKey");
        assertEquals("https://mycompany.atlassian.net/wiki", basicAttrs.getDomainURL());
    }

    @Test
    @DisplayName("Should detect when basic credentials are present")
    void testAreBasicCredentialsPresent() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(ConfluenceConstants.DOMAIN_NAME, "domain");
        attrs.put(ConfluenceConstants.AUTHORIZATION_EMAIL, "email@test.com");
        attrs.put(ConfluenceConstants.API_KEY, "apikey");
        confluenceAttributes.setAttributes(attrs);

        assertTrue(confluenceAttributes.areBasicCredentialsPresent());

        attrs.remove(ConfluenceConstants.API_KEY);
        confluenceAttributes.setAttributes(attrs);
        assertFalse(confluenceAttributes.areBasicCredentialsPresent());
    }

    @Test
    @DisplayName("Should detect when OAuth credentials are present")
    void testAreOAuthCredentialsPresent() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(ConfluenceConstants.CREDENTIALS_MODE, "own");
        attrs.put(ConfluenceConstants.CLIENT_ID, "client-id");
        attrs.put(ConfluenceConstants.CLIENT_SECRET, "client-secret");
        confluenceAttributes.setAttributes(attrs);

        assertTrue(confluenceAttributes.areOAuthCredentialsPresent());

        attrs.remove(ConfluenceConstants.CLIENT_SECRET);
        confluenceAttributes.setAttributes(attrs);
        assertFalse(confluenceAttributes.areOAuthCredentialsPresent());
    }

    @Test
    @DisplayName("Should validate successful basic auth attributes")
    void testValidateAttributes_Success_Basic() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(ConfluenceConstants.DOMAIN_NAME, "myDomain");
        attrs.put(ConfluenceConstants.AUTHORIZATION_EMAIL, "user@example.com");
        attrs.put(ConfluenceConstants.API_KEY, "my-api-key");

        assertDoesNotThrow(() -> confluenceAttributes.validateAttributes(attrs));
    }

    @Test
    @DisplayName("Should validate successful OAuth attributes")
    void testValidateAttributes_Success_OAuth() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(ConfluenceConstants.CLIENT_ID, "valid-client-id");
        attrs.put(ConfluenceConstants.CLIENT_SECRET, "REDACTED_SECRET");

        assertDoesNotThrow(() -> confluenceAttributes.validateAttributes(attrs));
    }

    @Test
    @DisplayName("Should throw exception for mixed credentials")
    void testValidateAttributes_MixedCredentials_ThrowsException() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(ConfluenceConstants.CREDENTIALS_MODE, "own");
        attrs.put(ConfluenceConstants.DOMAIN_NAME, "myDomain");
        attrs.put(ConfluenceConstants.CLIENT_ID, "client-id");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> confluenceAttributes.validateAttributes(attrs));
        assertTrue(exception.getMessage().contains("Please choose ONE authentication method"));
    }

    @Test
    @DisplayName("Should throw exception when no credentials are provided")
    void testValidateAttributes_NoCredentials_ThrowsException() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(ConfluenceConstants.CREDENTIALS_MODE, "own");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> confluenceAttributes.validateAttributes(attrs));
        assertTrue(exception.getMessage().contains("Please choose ONE authentication method"));
    }

    @Test
    @DisplayName("Should throw exception for partial OAuth credentials")
    void testValidateAttributes_PartialOAuth_ThrowsException() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(ConfluenceConstants.CREDENTIALS_MODE, "own");
        attrs.put(ConfluenceConstants.CLIENT_ID, "client-id-only");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> confluenceAttributes.validateAttributes(attrs));
        assertTrue(exception.getMessage().contains("Please choose ONE authentication method"));
    }

    @Test
    @DisplayName("Should throw exception for partial basic credentials")
    void testValidateAttributes_PartialBasic_ThrowsException() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(ConfluenceConstants.CREDENTIALS_MODE, "own");
        attrs.put(ConfluenceConstants.DOMAIN_NAME, "domain-only");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> confluenceAttributes.validateAttributes(attrs));
        assertTrue(exception.getMessage().contains("Please choose ONE authentication method"));
    }

    @Test
    @DisplayName("Should throw exception for invalid email format")
    void testValidateAttributes_InvalidEmail_ThrowsException() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(ConfluenceConstants.DOMAIN_NAME, "domain");
        attrs.put(ConfluenceConstants.AUTHORIZATION_EMAIL, "invalid-email");
        attrs.put(ConfluenceConstants.API_KEY, "apikey");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> confluenceAttributes.validateAttributes(attrs));
        assertTrue(exception.getMessage().contains("Account Email is not a valid email address"));
    }

    @Test
    @DisplayName("Should throw exception for invalid client ID format")
    void testValidateAttributes_InvalidClientId_ThrowsException() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(ConfluenceConstants.CLIENT_ID, "invalid client id!");
        attrs.put(ConfluenceConstants.CLIENT_SECRET, "REDACTED_SECRET");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> confluenceAttributes.validateAttributes(attrs));
        assertTrue(exception.getMessage().contains("Client ID format is invalid"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "user@example.com", "admin@company.org", "test.user@domain.co.uk" })
    @DisplayName("Should accept valid email formats")
    void testValidateAttributes_ValidEmails(String email) {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(ConfluenceConstants.DOMAIN_NAME, "domain");
        attrs.put(ConfluenceConstants.AUTHORIZATION_EMAIL, email);
        attrs.put(ConfluenceConstants.API_KEY, "apikey");

        assertDoesNotThrow(() -> confluenceAttributes.validateAttributes(attrs));
    }

    @Test
    @DisplayName("Should create Basic Auth REST client")
    void testGetRestClient_Basic() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(ConfluenceConstants.DOMAIN_NAME, "testdomain");
        attrs.put(ConfluenceConstants.AUTHORIZATION_EMAIL, "user@test.com");
        attrs.put(ConfluenceConstants.API_KEY, "apikey123");
        confluenceAttributes.setAttributes(attrs);

        ConfluenceRESTClient client = confluenceAttributes.getRestClient();
        assertNotNull(client);
    }

    @Test
    @DisplayName("Should create OAuth REST client")
    void testGetRestClient_OAuth() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(ConfluenceConstants.CLIENT_ID, "client-id");
        attrs.put(ConfluenceConstants.CLIENT_SECRET, "client-secret");
        confluenceAttributes.setAttributes(attrs);

        try (MockedConstruction<ConfluenceOAuthService> mockedOAuthService = mockConstruction(
                ConfluenceOAuthService.class,
                (mock, context) -> {
                    AccessibleResource mockResources = mock(
                            AccessibleResource.class);
                    when(mockResources.getId()).thenReturn("cloud-id-123");
                    when(mock.getAccessibleResources()).thenReturn(mockResources);
                })) {

            try (MockedConstruction<ConfluenceRESTClient> mockedRestClient = mockConstruction(
                    ConfluenceRESTClient.class)) {
                ConfluenceRESTClient client = confluenceAttributes.getRestClient();
                assertNotNull(client);

                ConfluenceOAuthService oAuthServiceMock = mockedOAuthService.constructed().get(0);
                verify(oAuthServiceMock).setCalledFromSetup(true);
                verify(oAuthServiceMock).getAccessibleResources();
            }
        }
    }

    @Test
    @DisplayName("Should create 2LO (client-credentials) REST client in service-account mode")
    void testGetRestClient_ServiceAccount2LO() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(ConfluenceConstants.CLIENT_ID, "client-id");
        attrs.put(ConfluenceConstants.CLIENT_SECRET, "client-secret");
        attrs.put(ConfluenceConstants.SERVICE_ACCOUNT, true);
        // isServiceAccountModeEnabled() reads the "Service Account" flag straight
        // from the invoker attributes, so make the invoker expose the same map.
        when(invoker.getAttributes()).thenReturn(attrs);
        confluenceAttributes.setAttributes(attrs);

        try (MockedConstruction<ClientCredentialsTokenProvider> mockedProvider = mockConstruction(
                ClientCredentialsTokenProvider.class,
                (mock, context) -> {
                    AccessibleResource resources = mock(AccessibleResource.class);
                    when(resources.getId()).thenReturn("cloud-id-2lo");
                    when(resources.getUrl()).thenReturn("https://sa.atlassian.net");
                    when(mock.getAccessibleResources()).thenReturn(resources);
                });
             MockedConstruction<ConfluenceRESTClient> mockedRestClient = mockConstruction(ConfluenceRESTClient.class)) {

            ConfluenceRESTClient client = confluenceAttributes.getRestClient();

            assertNotNull(client);
            verify(mockedProvider.constructed().get(0)).getAccessibleResources();
            // siteBaseUrl is captured from the 2LO accessible resource.
            assertEquals("https://sa.atlassian.net/wiki", confluenceAttributes.getDomainURL());
        }
    }

    @Test
    @DisplayName("Should return the cached REST client on subsequent calls without rebuilding")
    void testGetRestClient_ReturnsCachedInstance() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(ConfluenceConstants.DOMAIN_NAME, "testdomain");
        attrs.put(ConfluenceConstants.AUTHORIZATION_EMAIL, "user@test.com");
        attrs.put(ConfluenceConstants.API_KEY, "apikey123");
        confluenceAttributes.setAttributes(attrs);

        ConfluenceRESTClient first = confluenceAttributes.getRestClient();
        // Second call hits the restClient != null branch and returns the same instance.
        ConfluenceRESTClient second = confluenceAttributes.getRestClient();

        assertNotNull(first);
        assertSame(first, second);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  getDomainURL — 100% branch coverage (incl. stripTrailingSlash / withWikiSuffix)
    // ════════════════════════════════════════════════════════════════════════

    /** Sets the private {@code siteBaseUrl} field via reflection to seed cached/blank states. */
    private void setSiteBaseUrl(ConfluenceAttributes attrs, String value) throws Exception {
        java.lang.reflect.Field field = ConfluenceAttributes.class.getDeclaredField("siteBaseUrl");
        field.setAccessible(true);
        field.set(attrs, value);
    }

    private void setOAuthCredentials() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(ConfluenceConstants.CLIENT_ID, "client-id");
        attrs.put(ConfluenceConstants.CLIENT_SECRET, "client-secret");
        confluenceAttributes.setAttributes(attrs);
    }

    @Test
    @DisplayName("getDomainURL: OAuth with cached site URL skips REST client and appends /wiki")
    void getDomainURL_oauthCachedSiteUrl_skipsRestClient() throws Exception {
        setOAuthCredentials();
        // Non-blank cache → first if is false (REST client not built).
        setSiteBaseUrl(confluenceAttributes, "https://cached.atlassian.net");

        assertEquals("https://cached.atlassian.net/wiki", confluenceAttributes.getDomainURL());
    }

    @Test
    @DisplayName("getDomainURL: OAuth with blank cache resolves via REST client, strips trailing slash, keeps /wiki")
    void getDomainURL_oauthBlankCache_resolvesTrailingSlashWithWiki() throws Exception {
        setOAuthCredentials();
        // Blank (non-null) cache → first if true via isBlank(), so REST client is built.
        setSiteBaseUrl(confluenceAttributes, "");

        try (MockedConstruction<ConfluenceOAuthService> ignoredOAuth = mockConstruction(
                ConfluenceOAuthService.class,
                (mock, context) -> {
                    AccessibleResource resources = mock(AccessibleResource.class);
                    when(resources.getId()).thenReturn("cloud-id");
                    when(resources.getUrl()).thenReturn("https://acme.atlassian.net/wiki/");
                    when(mock.getAccessibleResources()).thenReturn(resources);
                });
             MockedConstruction<ConfluenceRESTClient> ignoredRest = mockConstruction(ConfluenceRESTClient.class)) {

            assertEquals("https://acme.atlassian.net/wiki", confluenceAttributes.getDomainURL());
        }
    }

    @Test
    @DisplayName("getDomainURL: OAuth with null cache resolves plain site URL and appends /wiki")
    void getDomainURL_oauthNullCache_resolvesPlainUrl() {
        setOAuthCredentials(); // setAttributes resets siteBaseUrl to null

        try (MockedConstruction<ConfluenceOAuthService> ignoredOAuth = mockConstruction(
                ConfluenceOAuthService.class,
                (mock, context) -> {
                    AccessibleResource resources = mock(AccessibleResource.class);
                    when(resources.getId()).thenReturn("cloud-id");
                    when(resources.getUrl()).thenReturn("https://acme.atlassian.net");
                    when(mock.getAccessibleResources()).thenReturn(resources);
                });
             MockedConstruction<ConfluenceRESTClient> ignoredRest = mockConstruction(ConfluenceRESTClient.class)) {

            assertEquals("https://acme.atlassian.net/wiki", confluenceAttributes.getDomainURL());
        }
    }

    @Test
    @DisplayName("getDomainURL: OAuth resolving a blank site URL falls back to the domain-name form")
    void getDomainURL_oauthResolvesBlankUrl_fallsBackToDomainFormat() {
        setOAuthCredentials(); // no DOMAIN_NAME → getDomainName() returns ""

        try (MockedConstruction<ConfluenceOAuthService> ignoredOAuth = mockConstruction(
                ConfluenceOAuthService.class,
                (mock, context) -> {
                    AccessibleResource resources = mock(AccessibleResource.class);
                    when(resources.getId()).thenReturn("cloud-id");
                    // Resolved to a non-null but blank URL → second guard's !isBlank() is false.
                    when(resources.getUrl()).thenReturn("");
                    when(mock.getAccessibleResources()).thenReturn(resources);
                });
             MockedConstruction<ConfluenceRESTClient> ignoredRest = mockConstruction(ConfluenceRESTClient.class)) {

            assertEquals("https://.atlassian.net/wiki", confluenceAttributes.getDomainURL());
        }
    }

    @Test
    @DisplayName("getDomainURL: OAuth REST-client failure is caught and falls back to the domain-name form")
    void getDomainURL_oauthRestClientThrows_fallsBackToDomainFormat() {
        setOAuthCredentials(); // no DOMAIN_NAME → getDomainName() returns ""

        try (MockedConstruction<ConfluenceOAuthService> ignoredOAuth = mockConstruction(
                ConfluenceOAuthService.class,
                (mock, context) ->
                        when(mock.getAccessibleResources()).thenThrow(new RuntimeException("boom")))) {

            // Resolution fails → siteBaseUrl stays null → fall back to the domain-name format.
            assertEquals("https://.atlassian.net/wiki", confluenceAttributes.getDomainURL());
        }
    }

    @Test
    @DisplayName("getDomainURL: Basic Auth with an http:// domain strips the trailing slash and appends /wiki")
    void getDomainURL_basicAuthHttpScheme() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(ConfluenceConstants.DOMAIN_NAME, "http://onprem.example.com/");
        attrs.put(ConfluenceConstants.AUTHORIZATION_EMAIL, "user@test.com");
        attrs.put(ConfluenceConstants.API_KEY, "apikey");
        confluenceAttributes.setAttributes(attrs);

        assertEquals("http://onprem.example.com/wiki", confluenceAttributes.getDomainURL());
    }

    @Test
    @DisplayName("getDomainURL: Basic Auth with an https:// domain that already ends in /wiki is returned unchanged")
    void getDomainURL_basicAuthHttpsSchemeAlreadyWiki() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(ConfluenceConstants.DOMAIN_NAME, "https://onprem.example.com/wiki");
        attrs.put(ConfluenceConstants.AUTHORIZATION_EMAIL, "user@test.com");
        attrs.put(ConfluenceConstants.API_KEY, "apikey");
        confluenceAttributes.setAttributes(attrs);

        assertEquals("https://onprem.example.com/wiki", confluenceAttributes.getDomainURL());
    }

    @Test
    @DisplayName("getDomainURL: null domain name yields the null-subdomain fallback URL")
    void getDomainURL_nullDomain_usesNullSubdomainFormat() {
        // 3-arg constructor leaves requestContext/sessionManager null, so credSnap
        // returns null and getDomainName() returns null.
        ConfluenceAttributes attrs = new ConfluenceAttributes(null, "user@test.com", "apikey");

        assertEquals("https://null.atlassian.net/wiki", attrs.getDomainURL());
    }
}
