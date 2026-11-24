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

package com.kristasoft.extensions.essentials.collaboration.confluence.client;

import com.kristasoft.extensions.essentials.collaboration.confluence.api.APIVersion;
import com.kristasoft.extensions.essentials.collaboration.confluence.exception.ConfluenceRESTException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConfluenceRESTClient")
class ConfluenceRESTClientTest {

    @Nested
    @DisplayName("Basic Auth Constructor")
    class BasicAuthConstructorTests {

        @Test
        @DisplayName("should create instance successfully")
        void testBasicAuthConstructor() {
            ConfluenceRESTClient client = new ConfluenceRESTClient("mydomain", "user@test.com", "apikey123");
            assertNotNull(client);
        }

        @Test
        @DisplayName("should set useOAuth to false")
        void testBasicAuth_UseOAuthIsFalse() throws Exception {
            ConfluenceRESTClient client = new ConfluenceRESTClient("domain", "user@test.com", "apikey");

            Field field = ConfluenceRESTClient.class.getDeclaredField("useOAuth");
            field.setAccessible(true);
            assertFalse((boolean) field.get(client));
        }

        @Test
        @DisplayName("should set cloudId to null")
        void testBasicAuth_CloudIdIsNull() throws Exception {
            ConfluenceRESTClient client = new ConfluenceRESTClient("domain", "user@test.com", "apikey");

            Field field = ConfluenceRESTClient.class.getDeclaredField("cloudId");
            field.setAccessible(true);
            assertNull(field.get(client));
        }

        @Test
        @DisplayName("should set oAuthTokenSupplier to null")
        void testBasicAuth_OAuthTokenSupplierIsNull() throws Exception {
            ConfluenceRESTClient client = new ConfluenceRESTClient("domain", "user@test.com", "apikey");

            Field field = ConfluenceRESTClient.class.getDeclaredField("oAuthTokenSupplier");
            field.setAccessible(true);
            assertNull(field.get(client));
        }

        @Test
        @DisplayName("should encode credentials correctly")
        void testBasicAuthEncoding_CorrectFormat() throws Exception {
            ConfluenceRESTClient client = new ConfluenceRESTClient("domain", "admin@company.com", "secret-key");

            Field field = ConfluenceRESTClient.class.getDeclaredField("basicAuthEncoded");
            field.setAccessible(true);
            String encoded = (String) field.get(client);

            String decoded = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
            assertEquals("admin@company.com:secret-key", decoded);
        }

        @Test
        @DisplayName("should encode special characters in credentials correctly")
        void testBasicAuthEncoding_SpecialCharacters() throws Exception {
            ConfluenceRESTClient client = new ConfluenceRESTClient("domain", "user+tag@test.com", "p@ss:w0rd!");

            Field field = ConfluenceRESTClient.class.getDeclaredField("basicAuthEncoded");
            field.setAccessible(true);
            String encoded = (String) field.get(client);

            String decoded = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
            assertEquals("user+tag@test.com:p@ss:w0rd!", decoded);
        }

        @Test
        @DisplayName("should store domainName")
        void testBasicAuth_DomainNameSet() throws Exception {
            ConfluenceRESTClient client = new ConfluenceRESTClient("mydomain", "user@test.com", "apikey");

            Field field = ConfluenceRESTClient.class.getDeclaredField("domainName");
            field.setAccessible(true);
            assertEquals("mydomain", field.get(client));
        }
    }

    @Nested
    @DisplayName("OAuth Constructor")
    class OAuthConstructorTests {

        @Test
        @DisplayName("should create instance successfully")
        void testOAuthConstructor() {
            Supplier<String> tokenSupplier = () -> "test-oauth-token";
            ConfluenceRESTClient client = new ConfluenceRESTClient("cloud-id-123", tokenSupplier);
            assertNotNull(client);
        }

        @Test
        @DisplayName("should set useOAuth to true")
        void testOAuth_UseOAuthIsTrue() throws Exception {
            ConfluenceRESTClient client = new ConfluenceRESTClient("cloud-id", () -> "token");

            Field field = ConfluenceRESTClient.class.getDeclaredField("useOAuth");
            field.setAccessible(true);
            assertTrue((boolean) field.get(client));
        }

        @Test
        @DisplayName("should set domainName to null")
        void testOAuth_DomainNameIsNull() throws Exception {
            ConfluenceRESTClient client = new ConfluenceRESTClient("cloud-id", () -> "token");

            Field field = ConfluenceRESTClient.class.getDeclaredField("domainName");
            field.setAccessible(true);
            assertNull(field.get(client));
        }

        @Test
        @DisplayName("should set basicAuthEncoded to null")
        void testOAuth_BasicAuthEncodedIsNull() throws Exception {
            ConfluenceRESTClient client = new ConfluenceRESTClient("cloud-id", () -> "token");

            Field field = ConfluenceRESTClient.class.getDeclaredField("basicAuthEncoded");
            field.setAccessible(true);
            assertNull(field.get(client));
        }

        @Test
        @DisplayName("should store cloudId")
        void testOAuth_CloudIdSet() throws Exception {
            ConfluenceRESTClient client = new ConfluenceRESTClient("my-cloud-id", () -> "token");

            Field field = ConfluenceRESTClient.class.getDeclaredField("cloudId");
            field.setAccessible(true);
            assertEquals("my-cloud-id", field.get(client));
        }
    }

    @Nested
    @DisplayName("client()")
    class HttpClientTests {

        @Test
        @DisplayName("should return a non-null HttpClient")
        void testClient_ReturnsNonNull() {
            ConfluenceRESTClient client = new ConfluenceRESTClient("domain", "user@test.com", "apikey");
            assertNotNull(client.client());
        }

        @Test
        @DisplayName("should return the same instance on repeated calls (singleton)")
        void testClient_ReturnsSameInstance() {
            ConfluenceRESTClient client = new ConfluenceRESTClient("domain", "user@test.com", "apikey");
            assertSame(client.client(), client.client(), "Should return the same HttpClient instance");
        }
    }

    @Nested
    @DisplayName("getAuthorizationHeader() via reflection")
    class GetAuthorizationHeaderTests {

        @Test
        @DisplayName("should return Basic auth header for basic auth client")
        void testGetAuthorizationHeader_BasicAuth() throws Exception {
            ConfluenceRESTClient client = new ConfluenceRESTClient("domain", "user@test.com", "apikey");

            Method method = ConfluenceRESTClient.class.getDeclaredMethod("getAuthorizationHeader");
            method.setAccessible(true);
            String header = (String) method.invoke(client);

            assertTrue(header.startsWith("Basic "));
            String expectedEncoded = Base64.getEncoder().encodeToString("user@test.com:apikey".getBytes(StandardCharsets.UTF_8));
            assertEquals("Basic " + expectedEncoded, header);
        }

        @Test
        @DisplayName("should return Bearer token for OAuth client")
        void testGetAuthorizationHeader_OAuth() throws Exception {
            Supplier<String> tokenSupplier = () -> "my-access-token-123";
            ConfluenceRESTClient client = new ConfluenceRESTClient("cloud-id", tokenSupplier);

            Method method = ConfluenceRESTClient.class.getDeclaredMethod("getAuthorizationHeader");
            method.setAccessible(true);
            String header = (String) method.invoke(client);

            assertEquals("Bearer my-access-token-123", header);
        }

        @Test
        @DisplayName("should call supplier each time for dynamic token rotation")
        void testGetAuthorizationHeader_OAuth_DynamicToken() throws Exception {
            int[] callCount = {0};
            Supplier<String> tokenSupplier = () -> "token-" + (++callCount[0]);
            ConfluenceRESTClient client = new ConfluenceRESTClient("cloud-id", tokenSupplier);

            Method method = ConfluenceRESTClient.class.getDeclaredMethod("getAuthorizationHeader");
            method.setAccessible(true);

            String first = (String) method.invoke(client);
            String second = (String) method.invoke(client);

            assertEquals("Bearer token-1", first);
            assertEquals("Bearer token-2", second);
        }
    }

    @Nested
    @DisplayName("getBaseUrl() via reflection")
    class GetBaseUrlTests {

        @Test
        @DisplayName("should return Atlassian cloud base URL for OAuth mode")
        void testGetBaseUrl_OAuth() throws Exception {
            ConfluenceRESTClient client = new ConfluenceRESTClient("cloud-id-abc", () -> "token");

            Method method = ConfluenceRESTClient.class.getDeclaredMethod("getBaseUrl");
            method.setAccessible(true);
            String baseUrl = (String) method.invoke(client);

            assertEquals("https://api.atlassian.com/ex/confluence/cloud-id-abc", baseUrl);
        }

        @Test
        @DisplayName("should return atlassian.net URL for simple domain name")
        void testGetBaseUrl_BasicAuth_SimpleDomain() throws Exception {
            ConfluenceRESTClient client = new ConfluenceRESTClient("mycompany", "user@test.com", "apikey");

            Method method = ConfluenceRESTClient.class.getDeclaredMethod("getBaseUrl");
            method.setAccessible(true);
            String baseUrl = (String) method.invoke(client);

            assertEquals("https://mycompany.atlassian.net", baseUrl);
        }

        @Test
        @DisplayName("should return full HTTPS URL as-is without trailing slash")
        void testGetBaseUrl_BasicAuth_FullHttpsUrl() throws Exception {
            ConfluenceRESTClient client = new ConfluenceRESTClient("https://custom-confluence.example.com", "user@test.com", "apikey");

            Method method = ConfluenceRESTClient.class.getDeclaredMethod("getBaseUrl");
            method.setAccessible(true);
            String baseUrl = (String) method.invoke(client);

            assertEquals("https://custom-confluence.example.com", baseUrl);
        }

        @Test
        @DisplayName("should return HTTP URL as-is")
        void testGetBaseUrl_BasicAuth_HttpUrl() throws Exception {
            ConfluenceRESTClient client = new ConfluenceRESTClient("http://local-confluence.com", "user@test.com", "apikey");

            Method method = ConfluenceRESTClient.class.getDeclaredMethod("getBaseUrl");
            method.setAccessible(true);
            String baseUrl = (String) method.invoke(client);

            assertEquals("http://local-confluence.com", baseUrl);
        }

        @Test
        @DisplayName("should strip trailing slash from URL")
        void testGetBaseUrl_BasicAuth_UrlWithTrailingSlash() throws Exception {
            ConfluenceRESTClient client = new ConfluenceRESTClient("https://confluence.example.com/", "user@test.com", "apikey");

            Method method = ConfluenceRESTClient.class.getDeclaredMethod("getBaseUrl");
            method.setAccessible(true);
            String baseUrl = (String) method.invoke(client);

            assertEquals("https://confluence.example.com", baseUrl);
        }

        @Test
        @DisplayName("should strip trailing slash from HTTP URL")
        void testGetBaseUrl_BasicAuth_HttpUrlWithTrailingSlash() throws Exception {
            ConfluenceRESTClient client = new ConfluenceRESTClient("http://local.server.com/", "user@test.com", "apikey");

            Method method = ConfluenceRESTClient.class.getDeclaredMethod("getBaseUrl");
            method.setAccessible(true);
            String baseUrl = (String) method.invoke(client);

            assertEquals("http://local.server.com", baseUrl);
        }

        @Test
        @DisplayName("should throw RuntimeException when both domainName and cloudId are null")
        void testGetBaseUrl_NullDomain_ThrowsRuntimeException() throws Exception {
            ConfluenceRESTClient client = new ConfluenceRESTClient("temp", "user@test.com", "apikey");

            Field domainField = ConfluenceRESTClient.class.getDeclaredField("domainName");
            domainField.setAccessible(true);
            domainField.set(client, null);

            Field useOAuthField = ConfluenceRESTClient.class.getDeclaredField("useOAuth");
            useOAuthField.setAccessible(true);
            useOAuthField.set(client, false);

            Method method = ConfluenceRESTClient.class.getDeclaredMethod("getBaseUrl");
            method.setAccessible(true);

            assertThrows(RuntimeException.class, () -> {
                try {
                    method.invoke(client);
                } catch (java.lang.reflect.InvocationTargetException e) {
                    throw e.getCause();
                }
            });
        }
    }

    @Nested
    @DisplayName("getRequestURI() via reflection")
    class GetRequestURITests {

        @Test
        @DisplayName("should build correct URI for basic auth with V2 API")
        void testGetRequestURI_BasicAuth_V2() throws Exception {
            ConfluenceRESTClient client = new ConfluenceRESTClient("mycompany", "user@test.com", "apikey");

            Method method = ConfluenceRESTClient.class.getDeclaredMethod("getRequestURI", APIVersion.class, String[].class);
            method.setAccessible(true);
            URI uri = (URI) method.invoke(client, APIVersion.V2, new String[]{"spaces"});

            assertEquals("https://mycompany.atlassian.net/wiki/api/v2/spaces", uri.toString());
        }

        @Test
        @DisplayName("should build correct URI for OAuth with V2 API")
        void testGetRequestURI_OAuth_V2() throws Exception {
            ConfluenceRESTClient client = new ConfluenceRESTClient("cloud-123", () -> "token");

            Method method = ConfluenceRESTClient.class.getDeclaredMethod("getRequestURI", APIVersion.class, String[].class);
            method.setAccessible(true);
            URI uri = (URI) method.invoke(client, APIVersion.V2, new String[]{"spaces"});

            assertEquals("https://api.atlassian.com/ex/confluence/cloud-123/wiki/api/v2/spaces", uri.toString());
        }

        @Test
        @DisplayName("should build correct URI for basic auth with V1 API and multiple endpoint segments")
        void testGetRequestURI_BasicAuth_V1_MultipleEndpoints() throws Exception {
            ConfluenceRESTClient client = new ConfluenceRESTClient("mycompany", "user@test.com", "apikey");

            Method method = ConfluenceRESTClient.class.getDeclaredMethod("getRequestURI", APIVersion.class, String[].class);
            method.setAccessible(true);
            URI uri = (URI) method.invoke(client, APIVersion.V1, new String[]{"template", "page"});

            assertEquals("https://mycompany.atlassian.net/wiki/rest/api/template/page", uri.toString());
        }

        @Test
        @DisplayName("should build correct URI for OAuth with V1 API and multiple endpoint segments")
        void testGetRequestURI_OAuth_V1_MultipleEndpoints() throws Exception {
            ConfluenceRESTClient client = new ConfluenceRESTClient("cloud-456", () -> "token");

            Method method = ConfluenceRESTClient.class.getDeclaredMethod("getRequestURI", APIVersion.class, String[].class);
            method.setAccessible(true);
            URI uri = (URI) method.invoke(client, APIVersion.V1, new String[]{"content", "123"});

            assertEquals("https://api.atlassian.com/ex/confluence/cloud-456/wiki/rest/api/content/123", uri.toString());
        }

        @Test
        @DisplayName("should build correct URI when version is null")
        void testGetRequestURI_NullVersion() throws Exception {
            ConfluenceRESTClient client = new ConfluenceRESTClient("mycompany", "user@test.com", "apikey");

            Method method = ConfluenceRESTClient.class.getDeclaredMethod("getRequestURI", APIVersion.class, String[].class);
            method.setAccessible(true);
            URI uri = (URI) method.invoke(client, new Object[]{null, new String[]{"some-path"}});

            assertEquals("https://mycompany.atlassian.net/some-path", uri.toString());
        }
    }

    @Nested
    @DisplayName("doSyncGet()")
    @ExtendWith(MockitoExtension.class)
    class DoSyncGetTests {

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("should return HttpResponse for successful GET request without query params")
        void doSyncGet_successWithoutQueryParams() throws Exception {
            HttpClient mockHttpClient = mock(HttpClient.class);
            HttpResponse<String> mockResponse = mock(HttpResponse.class);
            when(mockResponse.statusCode()).thenReturn(200);
            when(mockResponse.body()).thenReturn("{\"results\":[]}");
            when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);

            ConfluenceRESTClient client = new ConfluenceRESTClient("mycompany", "user@test.com", "apikey");
            setHttpClient(client, mockHttpClient);

            HttpResponse<String> response = client.doSyncGet(APIVersion.V2, null, "spaces");

            assertNotNull(response);
            assertEquals(200, response.statusCode());
            assertEquals("{\"results\":[]}", response.body());
        }

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("should return HttpResponse for successful GET request with query params")
        void doSyncGet_successWithQueryParams() throws Exception {
            HttpClient mockHttpClient = mock(HttpClient.class);
            HttpResponse<String> mockResponse = mock(HttpResponse.class);
            when(mockResponse.statusCode()).thenReturn(200);
            when(mockResponse.body()).thenReturn("{\"id\":\"123\"}");
            when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);

            ConfluenceRESTClient client = new ConfluenceRESTClient("mycompany", "user@test.com", "apikey");
            setHttpClient(client, mockHttpClient);

            Map<String, String> queryParams = Map.of("status", "current", "limit", "25");
            HttpResponse<String> response = client.doSyncGet(APIVersion.V2, queryParams, "pages");

            assertNotNull(response);
            assertEquals(200, response.statusCode());
        }

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("should throw ConfluenceRESTException wrapping IOException")
        void doSyncGet_throwsOnIOException() throws Exception {
            HttpClient mockHttpClient = mock(HttpClient.class);
            when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenThrow(new IOException("Connection refused"));

            ConfluenceRESTClient client = new ConfluenceRESTClient("mycompany", "user@test.com", "apikey");
            setHttpClient(client, mockHttpClient);

            ConfluenceRESTException ex = assertThrows(ConfluenceRESTException.class,
                    () -> client.doSyncGet(APIVersion.V2, null, "spaces"));
            assertTrue(ex.getMessage().contains("Unable to connect to Confluence"));
            assertTrue(ex.getMessage().contains("Connection refused"));
        }

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("should throw ConfluenceRESTException and interrupt thread on InterruptedException")
        void doSyncGet_throwsOnInterruptedException() throws Exception {
            HttpClient mockHttpClient = mock(HttpClient.class);
            when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenThrow(new InterruptedException("Thread interrupted"));

            ConfluenceRESTClient client = new ConfluenceRESTClient("mycompany", "user@test.com", "apikey");
            setHttpClient(client, mockHttpClient);

            ConfluenceRESTException ex = assertThrows(ConfluenceRESTException.class,
                    () -> client.doSyncGet(APIVersion.V2, null, "spaces"));
            assertTrue(ex.getMessage().contains("Request was interrupted"));
            assertTrue(ex.getMessage().contains("Thread interrupted"));
            // Clear the interrupt flag set by the handler
            Thread.interrupted();
        }
    }

    @Nested
    @DisplayName("doAsyncGet()")
    @ExtendWith(MockitoExtension.class)
    class DoAsyncGetTests {

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("should return CompletableFuture for async GET without query params")
        void doAsyncGet_successWithoutQueryParams() {
            HttpClient mockHttpClient = mock(HttpClient.class);
            HttpResponse<String> mockResponse = mock(HttpResponse.class);
            when(mockHttpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(CompletableFuture.completedFuture(mockResponse));

            ConfluenceRESTClient client = new ConfluenceRESTClient("mycompany", "user@test.com", "apikey");
            setHttpClient(client, mockHttpClient);

            CompletableFuture<HttpResponse<String>> future = client.doAsyncGet(APIVersion.V2, null, "spaces");

            assertNotNull(future);
            assertNotNull(future.join());
        }

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("should return CompletableFuture for async GET with query params")
        void doAsyncGet_successWithQueryParams() {
            HttpClient mockHttpClient = mock(HttpClient.class);
            HttpResponse<String> mockResponse = mock(HttpResponse.class);
            when(mockHttpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(CompletableFuture.completedFuture(mockResponse));

            ConfluenceRESTClient client = new ConfluenceRESTClient("mycompany", "user@test.com", "apikey");
            setHttpClient(client, mockHttpClient);

            Map<String, String> queryParams = Map.of("cursor", "abc123");
            CompletableFuture<HttpResponse<String>> future = client.doAsyncGet(APIVersion.V2, queryParams, "pages");

            assertNotNull(future);
            assertNotNull(future.join());
        }
    }

    @Nested
    @DisplayName("doSyncPost()")
    @ExtendWith(MockitoExtension.class)
    class DoSyncPostTests {

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("should return HttpResponse for successful POST request")
        void doSyncPost_success() throws Exception {
            HttpClient mockHttpClient = mock(HttpClient.class);
            HttpResponse<String> mockResponse = mock(HttpResponse.class);
            when(mockResponse.statusCode()).thenReturn(200);
            when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);

            ConfluenceRESTClient client = new ConfluenceRESTClient("mycompany", "user@test.com", "apikey");
            setHttpClient(client, mockHttpClient);

            Map<String, String> body = Map.of("title", "New Page", "spaceId", "123");
            HttpResponse<String> response = client.doSyncPost(APIVersion.V2, body, "pages");

            assertNotNull(response);
            assertEquals(200, response.statusCode());
        }

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("should throw ConfluenceRESTException wrapping IOException")
        void doSyncPost_throwsOnIOException() throws Exception {
            HttpClient mockHttpClient = mock(HttpClient.class);
            when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenThrow(new IOException("Network error"));

            ConfluenceRESTClient client = new ConfluenceRESTClient("mycompany", "user@test.com", "apikey");
            setHttpClient(client, mockHttpClient);

            Map<String, String> body = Map.of("title", "Test");
            ConfluenceRESTException ex = assertThrows(ConfluenceRESTException.class,
                    () -> client.doSyncPost(APIVersion.V2, body, "pages"));
            assertTrue(ex.getMessage().contains("trouble connecting to Confluence"));
            assertTrue(ex.getMessage().contains("Network error"));
        }

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("should throw ConfluenceRESTException and interrupt thread on InterruptedException")
        void doSyncPost_throwsOnInterruptedException() throws Exception {
            HttpClient mockHttpClient = mock(HttpClient.class);
            when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenThrow(new InterruptedException("Interrupted"));

            ConfluenceRESTClient client = new ConfluenceRESTClient("mycompany", "user@test.com", "apikey");
            setHttpClient(client, mockHttpClient);

            Map<String, String> body = Map.of("title", "Test");
            ConfluenceRESTException ex = assertThrows(ConfluenceRESTException.class,
                    () -> client.doSyncPost(APIVersion.V2, body, "pages"));
            assertTrue(ex.getMessage().contains("request was interrupted"));
            Thread.interrupted();
        }
    }

    @Nested
    @DisplayName("doSyncPut()")
    @ExtendWith(MockitoExtension.class)
    class DoSyncPutTests {

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("should return HttpResponse for successful PUT request")
        void doSyncPut_success() throws Exception {
            HttpClient mockHttpClient = mock(HttpClient.class);
            HttpResponse<String> mockResponse = mock(HttpResponse.class);
            when(mockResponse.statusCode()).thenReturn(200);
            when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);

            ConfluenceRESTClient client = new ConfluenceRESTClient("mycompany", "user@test.com", "apikey");
            setHttpClient(client, mockHttpClient);

            Map<String, String> body = Map.of("title", "Updated Page");
            HttpResponse<String> response = client.doSyncPut(APIVersion.V2, body, "pages", "123");

            assertNotNull(response);
            assertEquals(200, response.statusCode());
        }

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("should throw ConfluenceRESTException wrapping IOException")
        void doSyncPut_throwsOnIOException() throws Exception {
            HttpClient mockHttpClient = mock(HttpClient.class);
            when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenThrow(new IOException("Timeout"));

            ConfluenceRESTClient client = new ConfluenceRESTClient("mycompany", "user@test.com", "apikey");
            setHttpClient(client, mockHttpClient);

            Map<String, String> body = Map.of("title", "Test");
            ConfluenceRESTException ex = assertThrows(ConfluenceRESTException.class,
                    () -> client.doSyncPut(APIVersion.V2, body, "pages", "123"));
            assertTrue(ex.getMessage().contains("trouble connecting to Confluence"));
            assertTrue(ex.getMessage().contains("Timeout"));
        }

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("should throw ConfluenceRESTException and interrupt thread on InterruptedException")
        void doSyncPut_throwsOnInterruptedException() throws Exception {
            HttpClient mockHttpClient = mock(HttpClient.class);
            when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenThrow(new InterruptedException("Interrupted"));

            ConfluenceRESTClient client = new ConfluenceRESTClient("mycompany", "user@test.com", "apikey");
            setHttpClient(client, mockHttpClient);

            Map<String, String> body = Map.of("title", "Test");
            ConfluenceRESTException ex = assertThrows(ConfluenceRESTException.class,
                    () -> client.doSyncPut(APIVersion.V2, body, "pages", "123"));
            assertTrue(ex.getMessage().contains("request was interrupted"));
            Thread.interrupted();
        }
    }

    @Nested
    @DisplayName("doSyncGetBinary()")
    @ExtendWith(MockitoExtension.class)
    class DoSyncGetBinaryTests {

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("should return byte array HttpResponse for successful binary GET")
        void doSyncGetBinary_success() throws Exception {
            HttpClient mockHttpClient = mock(HttpClient.class);
            HttpResponse<byte[]> mockResponse = mock(HttpResponse.class);
            byte[] binaryContent = "PDF content".getBytes(StandardCharsets.UTF_8);
            when(mockResponse.statusCode()).thenReturn(200);
            when(mockResponse.body()).thenReturn(binaryContent);
            when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);

            ConfluenceRESTClient client = new ConfluenceRESTClient("mycompany", "user@test.com", "apikey");
            setHttpClient(client, mockHttpClient);

            HttpResponse<byte[]> response = client.doSyncGetBinary("/wiki/download/attachments/123/file.pdf");

            assertNotNull(response);
            assertEquals(200, response.statusCode());
            assertArrayEquals(binaryContent, response.body());
        }

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("should prepend slash to download path if missing")
        void doSyncGetBinary_prependsSlash() throws Exception {
            HttpClient mockHttpClient = mock(HttpClient.class);
            HttpResponse<byte[]> mockResponse = mock(HttpResponse.class);
            when(mockResponse.statusCode()).thenReturn(200);
            when(mockResponse.body()).thenReturn(new byte[]{1, 2, 3});
            when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);

            ConfluenceRESTClient client = new ConfluenceRESTClient("mycompany", "user@test.com", "apikey");
            setHttpClient(client, mockHttpClient);

            HttpResponse<byte[]> response = client.doSyncGetBinary("wiki/download/attachments/123/file.pdf");

            assertNotNull(response);
            assertEquals(200, response.statusCode());
        }

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("should throw ConfluenceRESTException wrapping IOException")
        void doSyncGetBinary_throwsOnIOException() throws Exception {
            HttpClient mockHttpClient = mock(HttpClient.class);
            when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenThrow(new IOException("Download failed"));

            ConfluenceRESTClient client = new ConfluenceRESTClient("mycompany", "user@test.com", "apikey");
            setHttpClient(client, mockHttpClient);

            ConfluenceRESTException ex = assertThrows(ConfluenceRESTException.class,
                    () -> client.doSyncGetBinary("/wiki/download/file.pdf"));
            assertTrue(ex.getMessage().contains("Unable to download binary content"));
            assertTrue(ex.getMessage().contains("Download failed"));
        }

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("should throw ConfluenceRESTException and interrupt thread on InterruptedException")
        void doSyncGetBinary_throwsOnInterruptedException() throws Exception {
            HttpClient mockHttpClient = mock(HttpClient.class);
            when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenThrow(new InterruptedException("Interrupted"));

            ConfluenceRESTClient client = new ConfluenceRESTClient("mycompany", "user@test.com", "apikey");
            setHttpClient(client, mockHttpClient);

            ConfluenceRESTException ex = assertThrows(ConfluenceRESTException.class,
                    () -> client.doSyncGetBinary("/wiki/download/file.pdf"));
            assertTrue(ex.getMessage().contains("Binary download was interrupted"));
            Thread.interrupted();
        }

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("should use OAuth authorization for binary GET")
        void doSyncGetBinary_oAuthMode() throws Exception {
            HttpClient mockHttpClient = mock(HttpClient.class);
            HttpResponse<byte[]> mockResponse = mock(HttpResponse.class);
            when(mockResponse.statusCode()).thenReturn(200);
            when(mockResponse.body()).thenReturn(new byte[]{0x50, 0x44, 0x46});
            when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);

            ConfluenceRESTClient client = new ConfluenceRESTClient("cloud-id-xyz", () -> "oauth-token-123");
            setHttpClient(client, mockHttpClient);

            HttpResponse<byte[]> response = client.doSyncGetBinary("/wiki/download/file.pdf");

            assertNotNull(response);
            assertEquals(200, response.statusCode());
        }
    }

    @Nested
    @DisplayName("HTTP methods with OAuth client")
    @ExtendWith(MockitoExtension.class)
    class OAuthHttpMethodTests {

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("doSyncGet should work with OAuth client")
        void doSyncGet_oAuth() throws Exception {
            HttpClient mockHttpClient = mock(HttpClient.class);
            HttpResponse<String> mockResponse = mock(HttpResponse.class);
            when(mockResponse.statusCode()).thenReturn(200);
            when(mockResponse.body()).thenReturn("{\"results\":[]}");
            when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);

            ConfluenceRESTClient client = new ConfluenceRESTClient("cloud-id", () -> "oauth-token");
            setHttpClient(client, mockHttpClient);

            HttpResponse<String> response = client.doSyncGet(APIVersion.V2, null, "spaces");

            assertNotNull(response);
            assertEquals(200, response.statusCode());
        }

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("doSyncPost should work with OAuth client")
        void doSyncPost_oAuth() throws Exception {
            HttpClient mockHttpClient = mock(HttpClient.class);
            HttpResponse<String> mockResponse = mock(HttpResponse.class);
            when(mockResponse.statusCode()).thenReturn(200);
            when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);

            ConfluenceRESTClient client = new ConfluenceRESTClient("cloud-id", () -> "oauth-token");
            setHttpClient(client, mockHttpClient);

            HttpResponse<String> response = client.doSyncPost(APIVersion.V2, Map.of("title", "Page"), "pages");

            assertNotNull(response);
            assertEquals(200, response.statusCode());
        }

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("doSyncPut should work with OAuth client")
        void doSyncPut_oAuth() throws Exception {
            HttpClient mockHttpClient = mock(HttpClient.class);
            HttpResponse<String> mockResponse = mock(HttpResponse.class);
            when(mockResponse.statusCode()).thenReturn(200);
            when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);

            ConfluenceRESTClient client = new ConfluenceRESTClient("cloud-id", () -> "oauth-token");
            setHttpClient(client, mockHttpClient);

            HttpResponse<String> response = client.doSyncPut(APIVersion.V2, Map.of("title", "Updated"), "pages", "123");

            assertNotNull(response);
            assertEquals(200, response.statusCode());
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCaseTests {

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("doSyncGet should handle multiple endpoint segments")
        void doSyncGet_multipleEndpoints() throws Exception {
            HttpClient mockHttpClient = mock(HttpClient.class);
            HttpResponse<String> mockResponse = mock(HttpResponse.class);
            when(mockResponse.statusCode()).thenReturn(200);
            when(mockResponse.body()).thenReturn("{}");
            when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);

            ConfluenceRESTClient client = new ConfluenceRESTClient("mycompany", "user@test.com", "apikey");
            setHttpClient(client, mockHttpClient);

            HttpResponse<String> response = client.doSyncGet(APIVersion.V1, null, "content", "123", "child", "page");

            assertNotNull(response);
            assertEquals(200, response.statusCode());
        }

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("doSyncPost with V1 API version should build correct URI")
        void doSyncPost_v1Api() throws Exception {
            HttpClient mockHttpClient = mock(HttpClient.class);
            HttpResponse<String> mockResponse = mock(HttpResponse.class);
            when(mockResponse.statusCode()).thenReturn(200);
            when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);

            ConfluenceRESTClient client = new ConfluenceRESTClient("mycompany", "user@test.com", "apikey");
            setHttpClient(client, mockHttpClient);

            HttpResponse<String> response = client.doSyncPost(APIVersion.V1, Map.of("data", "test"), "template", "page");

            assertNotNull(response);
            assertEquals(200, response.statusCode());
        }

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("doSyncGet with non-200 status code should still return response")
        void doSyncGet_nonSuccessStatusCode() throws Exception {
            HttpClient mockHttpClient = mock(HttpClient.class);
            HttpResponse<String> mockResponse = mock(HttpResponse.class);
            when(mockResponse.statusCode()).thenReturn(404);
            when(mockResponse.body()).thenReturn("Not found");
            when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);

            ConfluenceRESTClient client = new ConfluenceRESTClient("mycompany", "user@test.com", "apikey");
            setHttpClient(client, mockHttpClient);

            HttpResponse<String> response = client.doSyncGet(APIVersion.V2, null, "pages", "nonexistent");

            assertNotNull(response);
            assertEquals(404, response.statusCode());
        }

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("doSyncGet with query params and OAuth")
        void doSyncGet_queryParamsOAuth() throws Exception {
            HttpClient mockHttpClient = mock(HttpClient.class);
            HttpResponse<String> mockResponse = mock(HttpResponse.class);
            when(mockResponse.statusCode()).thenReturn(200);
            when(mockResponse.body()).thenReturn("{\"results\":[]}");
            when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);

            ConfluenceRESTClient client = new ConfluenceRESTClient("cloud-id", () -> "token");
            setHttpClient(client, mockHttpClient);

            Map<String, String> queryParams = Map.of("depth", "root");
            HttpResponse<String> response = client.doSyncGet(APIVersion.V2, queryParams, "spaces", "123", "pages");

            assertNotNull(response);
            assertEquals(200, response.statusCode());
        }
    }

    /**
     * Helper method to inject a mock HttpClient into the static field of ConfluenceRESTClient.
     */
    private static void setHttpClient(ConfluenceRESTClient client, HttpClient mockHttpClient) {
        try {
            Field httpClientField = ConfluenceRESTClient.class.getDeclaredField("httpClient");
            httpClientField.setAccessible(true);
            httpClientField.set(null, mockHttpClient);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set httpClient field", e);
        }
    }
}
