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

import app.krista.sdk.atlassian.constants.AtlassianConstants;
import com.kristasoft.extensions.essentials.collaboration.confluence.api.APIVersion;
import com.kristasoft.extensions.essentials.collaboration.confluence.exception.ConfluenceRESTException;
import com.kristasoft.extensions.essentials.collaboration.confluence.utils.GsonFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ConfluenceRESTClient {
    private static final Logger log = LoggerFactory.getLogger(ConfluenceRESTClient.class);
    private static HttpClient httpClient;
    private final String domainName;
    private final String basicAuthEncoded;
    private static final String BASIC_AUTH_PREFIX = "Basic ";
    private static final String BEARER_PREFIX = AtlassianConstants.BEARER;
    private static final String ATLASSIAN_CLOUD_BASE_URL = AtlassianConstants.API_GATEWAY_BASE_URL + "confluence/";

    private final boolean useOAuth;
    private final Supplier<String> oAuthTokenSupplier;
    private final String cloudId;

    public HttpClient client() {
        if (httpClient == null) {
            httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMinutes(1))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
        }
        return httpClient;
    }

    /**
     * Constructor for Basic Auth mode (existing behavior).
     */
    public ConfluenceRESTClient(String domainName, String accountEmail, String apiKey) {
        log.info("ConfluenceRESTClient constructor called (Basic Auth mode)");
        log.info("Constructor parameters - domainName: '{}', accountEmail: '{}', apiKey length: {}",
                domainName, accountEmail, apiKey != null ? apiKey.length() : 0);
        this.domainName = domainName;
        this.basicAuthEncoded = Base64.getEncoder().encodeToString((accountEmail + ":" + apiKey).getBytes(StandardCharsets.UTF_8));
        this.useOAuth = false;
        this.oAuthTokenSupplier = null;
        this.cloudId = null;
        log.info("ConfluenceRESTClient constructor completed (Basic Auth) - domainName: '{}'", this.domainName);
    }

    /**
     * Constructor for OAuth 2.0 mode.
     *
     * @param cloudId           the Atlassian cloud ID for the site
     * @param oAuthTokenSupplier supplier that provides the current OAuth access token
     */
    public ConfluenceRESTClient(String cloudId, Supplier<String> oAuthTokenSupplier) {
        log.info("ConfluenceRESTClient constructor called (OAuth 2.0 mode)");
        log.info("Constructor parameters - cloudId: '{}'", cloudId);
        this.domainName = null;
        this.basicAuthEncoded = null;
        this.useOAuth = true;
        this.oAuthTokenSupplier = oAuthTokenSupplier;
        this.cloudId = cloudId;
        log.info("ConfluenceRESTClient constructor completed (OAuth 2.0) - cloudId: '{}'", this.cloudId);
    }

    private String getAuthorizationHeader() {
        if (useOAuth) {
            return BEARER_PREFIX + oAuthTokenSupplier.get();
        }
        return BASIC_AUTH_PREFIX + basicAuthEncoded;
    }

    public HttpResponse<String> doSyncGet(APIVersion version, Map<String, String> queryParams, String... endpoint) {
        log.info("ConfluenceRESTClient.doSyncGet() called");
        log.info("doSyncGet parameters - version: {}, queryParams: {}, endpoint: {}", version, queryParams, java.util.Arrays.toString(endpoint));
        try {
            log.info("Step 1: Building URI");
            URI uri = queryParams == null ? getRequestURI(version, endpoint) : URI.create(Objects.requireNonNull(getRequestURI(version, endpoint)) + "?" +
                    queryParams.entrySet().stream().map(entry -> entry.getKey() + "=" + entry.getValue()).collect(Collectors.joining("&")));
            log.info("Step 1: URI built successfully: {}", uri);
            log.info("GET Request to: {}", uri);
            long startTime = System.currentTimeMillis();
            HttpRequest getRequest = HttpRequest.newBuilder()
                    .uri(uri)
                    .GET()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, getAuthorizationHeader())
                    .build();
            HttpResponse<String> response = client().send(getRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            long endTime = System.currentTimeMillis();
            log.info(String.format("GET Response [status code: %s, time taken: %.2f ms]", response.statusCode(), (double) (endTime - startTime)));
            log.info("GET Response Body: {}", response.body());
            return response;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ConfluenceRESTException("Request was interrupted. Please try again. Details: " + e.getMessage());
        } catch (IOException e) {
            throw new ConfluenceRESTException("Unable to connect to Confluence. Please verify your network connection and Confluence URL, then try again. Details: " + e.getMessage());
        }
    }

    public CompletableFuture<HttpResponse<String>> doAsyncGet(APIVersion version, Map<String, String> queryParams, String... endpoint) {
        URI uri = queryParams == null ? getRequestURI(version, endpoint) : URI.create(Objects.requireNonNull(getRequestURI(version, endpoint)) + "?" +
                queryParams.entrySet().stream().map(entry -> entry.getKey() + "=" + entry.getValue()).collect(Collectors.joining("&")));
        log.info("GET Request to: {}", uri);
        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(uri)
                .GET()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getAuthorizationHeader())
                .build();
        return client().sendAsync(getRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    public HttpResponse<String> doSyncPost(APIVersion version, Object body, String... endpoint) {
        try {
            URI uri = getRequestURI(version, endpoint);
            String json = GsonFactory.getInstance().toJson(body);
            log.info("POST Request: {}", uri);
            log.info("POST Body: {}", json);
            long startTime = System.currentTimeMillis();
            HttpRequest postRequest = HttpRequest.newBuilder()
                    .uri(uri)
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, getAuthorizationHeader())
                    .build();
            long endTime = System.currentTimeMillis();
            HttpResponse<String> response = client().send(postRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            log.info(String.format("POST Response [status code: %s, time taken: %.2f ms]", response.statusCode(), (double) (endTime - startTime)));
            return response;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            String errorMessage = String.format("""
                    Your request was interrupted.
                    What to do next:
                    1. Try your request again
                    2. If the problem persists, check your network connection
                    3. Contact your administrator if issues continue

                    Technical details: %s""", e.getMessage());
            throw new ConfluenceRESTException(errorMessage);
        } catch (IOException e) {
            String errorMessage = String.format("""
                    We're having trouble connecting to Confluence.
                    This could mean:
                    • Your network connection is unstable
                    • The Confluence URL is incorrect
                    • Confluence is temporarily unavailable

                    What to do next:
                    1. Check your internet connection
                    2. Verify the Confluence URL in your settings
                    3. Try again in a moment
                    4. Contact your Confluence administrator if the issue persists

                    Technical details: %s""", e.getMessage());
            throw new ConfluenceRESTException(errorMessage);
        }
    }

    public HttpResponse<String> doSyncPut(APIVersion version, Object body, String... endpoint) {
        try {
            URI uri = getRequestURI(version, endpoint);
            String json = GsonFactory.getInstance().toJson(body);
            log.info("PUT Request: {}", uri);
            log.info("PUT Body: {}", json);
            long startTime = System.currentTimeMillis();
            HttpRequest postRequest = HttpRequest.newBuilder()
                    .uri(uri)
                    .PUT(HttpRequest.BodyPublishers.ofString(json))
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, getAuthorizationHeader())
                    .build();
            long endTime = System.currentTimeMillis();
            HttpResponse<String> response = client().send(postRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            log.info(String.format("PUT Response [status code: %s, time taken: %.2f ms]", response.statusCode(), (double) (endTime - startTime)));
            return response;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            String errorMessage = String.format("""
                    Your request was interrupted.
                    What to do next:
                    1. Try your request again
                    2. If the problem persists, check your network connection
                    3. Contact your administrator if issues continue

                    Technical details: %s""", e.getMessage());
            throw new ConfluenceRESTException(errorMessage);
        } catch (IOException e) {
            String errorMessage = String.format("""
                    We're having trouble connecting to Confluence.
                    This could mean:
                    • Your network connection is unstable
                    • The Confluence URL is incorrect
                    • Confluence is temporarily unavailable

                    What to do next:
                    1. Check your internet connection
                    2. Verify the Confluence URL in your settings
                    3. Try again in a moment
                    4. Contact your Confluence administrator if the issue persists

                    Technical details: %s""", e.getMessage());
            throw new ConfluenceRESTException(errorMessage);
        }
    }

    /**
     * Downloads binary content from Confluence (e.g., attachments).
     * Uses a direct URL path without API version prefix.
     *
     * @param downloadPath The download path (e.g., "/wiki/download/attachments/123/file.pdf")
     * @return HttpResponse containing byte array of the file content
     */
    public HttpResponse<byte[]> doSyncGetBinary(String downloadPath) {
        log.info("ConfluenceRESTClient.doSyncGetBinary() called");
        log.info("Download path: {}", downloadPath);
        try {
            String baseUrl = getBaseUrl();

            // Ensure download path starts with /
            String path = downloadPath.startsWith("/") ? downloadPath : "/" + downloadPath;
            URI uri = URI.create(baseUrl + path);

            log.info("Binary GET Request to: {}", uri);
            long startTime = System.currentTimeMillis();

            HttpRequest getRequest = HttpRequest.newBuilder()
                    .uri(uri)
                    .GET()
                    .header(HttpHeaders.AUTHORIZATION, getAuthorizationHeader())
                    .build();

            HttpResponse<byte[]> response = client().send(getRequest, HttpResponse.BodyHandlers.ofByteArray());
            long endTime = System.currentTimeMillis();

            log.info(String.format("Binary GET Response [status code: %s, content length: %d bytes, time taken: %.2f ms]",
                    response.statusCode(), response.body().length, (double) (endTime - startTime)));

            return response;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ConfluenceRESTException("Binary download was interrupted. Please try again. Details: " + e.getMessage());
        } catch (IOException e) {
            throw new ConfluenceRESTException("Unable to download binary content from Confluence. Please verify your network connection and try again. Details: " + e.getMessage());
        }
    }

    private String getBaseUrl() {
        if (useOAuth) {
            return ATLASSIAN_CLOUD_BASE_URL + cloudId;
        }
        if (domainName != null) {
            if (domainName.contains("http://") || domainName.contains("https://")) {
                String baseUrl = domainName;
                if (baseUrl.endsWith("/")) {
                    baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
                }
                return baseUrl;
            } else {
                return "https://" + domainName + ".atlassian.net";
            }
        }
        throw new RuntimeException("Cannot determine base URL. Neither domain name nor cloud ID is configured.");
    }

    private URI getRequestURI(APIVersion version, String... endpoint) {
        log.info("ConfluenceRESTClient.getRequestURI() called");
        log.info("getRequestURI parameters - version: {}, endpoint: {}", version, java.util.Arrays.toString(endpoint));

        String baseUrl = getBaseUrl();
        log.info("Base URL: '{}'", baseUrl);

        // APIVersion.toString() already includes trailing slash, so don't add extra slash
        String versionPath = version != null ? version.toString() : "";
        String endpointPath = String.join("/", endpoint);
        log.info("Version path: '{}'", versionPath);
        log.info("Endpoint path: '{}'", endpointPath);

        String finalURI = baseUrl + "/" + versionPath + endpointPath;
        log.info("Final URI string before URI.create(): '{}'", finalURI);

        URI result = URI.create(finalURI);
        log.info("URI.create() successful, returning: '{}'", result);
        return result;
    }
}
