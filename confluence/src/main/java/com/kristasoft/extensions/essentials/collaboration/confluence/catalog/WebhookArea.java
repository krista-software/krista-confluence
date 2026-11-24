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

import app.krista.extension.executor.ExtensionResponse;
import app.krista.extension.executor.ExtensionResponseBuilder;
import app.krista.extension.impl.anno.Attribute;
import app.krista.extension.impl.anno.CatalogRequest;
import app.krista.extension.impl.anno.Domain;
import app.krista.extension.impl.anno.Field;
import app.krista.ksdk.context.RequestContext;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kristasoft.extensions.essentials.collaboration.confluence.api.ConfluenceAuditService;
import com.kristasoft.extensions.essentials.collaboration.confluence.service.ConfluenceService;
import com.kristasoft.extensions.essentials.collaboration.confluence.utils.ConfluenceConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.net.HttpURLConnection;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Catalog area for Confluence webhook registration and management.
 *
 * <p>All {@link app.krista.extension.impl.anno.CatalogRequest} methods are annotated with
 * {@code tool = true}, exposing them as MCP (Model Context Protocol) tools for AI agent
 * consumption. Each operation records TRIGGERED / SUCCESS / FAILURE audit events via
 * {@link com.kristasoft.extensions.essentials.collaboration.confluence.api.ConfluenceAuditService},
 * which are visible on the extension's audit dashboard.</p>
 */
@Domain(id = ConfluenceConstants.DOMAIN_ID,
        name = ConfluenceConstants.KRISTA_DOMAIN_NAME,
        ecosystemId = ConfluenceConstants.ECOSYSTEM_ID,
        ecosystemName = ConfluenceConstants.ECOSYSTEM_NAME,
        ecosystemVersion = ConfluenceConstants.ECOSYSTEM_VERSION)
public class WebhookArea {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebhookArea.class);

    private final ConfluenceService confluenceService;
    private final ConfluenceAuditService confluenceAuditService;
    private final RequestContext requestContext;

    @Inject
    public WebhookArea(ConfluenceService confluenceService,
                       ConfluenceAuditService confluenceAuditService, RequestContext requestContext) {
        this.confluenceService = confluenceService;
        this.confluenceAuditService = confluenceAuditService;
        this.requestContext = requestContext;
    }

    @CatalogRequest(
            id = "localDomainRequest_c687b547-39a9-46c5-8c3c-58576a350c14",
            name = "Create Webhook",
            description = "Register a new webhook in Confluence to receive real-time notifications for specific events",
            area = "Webhook",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.Desc(name = "Webhook Response", type = "{ Name: Text, Success: Boolean }", required = false)
    public ExtensionResponse createWebhook(
            @Field.Text(name = "Webhook Name", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String webhookName,
            @Field.Text(name = "Webhook URL", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String webhookURL,
            @Field.Desc(name = "Events", type = "[ Text ]", required = false) List<String> events,
            @Field.Boolean(name = "Active", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) Boolean active) {

        String invokerId = requestContext.getInvokerId();
        confluenceAuditService.triggered(invokerId, "Create Webhook");
        boolean auditRecorded = false;
        logWebhookCreationStart(webhookName, webhookURL, events);

        try {
            Boolean activeState = setDefaultActiveState(active);
            Map<String, Object> webhookRequest = buildWebhookRequest(webhookName, webhookURL, events, activeState);
            HttpResponse<String> response = callWebhookAPI(webhookRequest);

            return processWebhookResponse(response, webhookName);

        } catch (app.krista.extension.authorization.MustAuthorizeException cause) {
            auditRecorded = true;
            confluenceAuditService.authFailure(invokerId, "Create Webhook", cause);
            throw cause;
        } catch (Exception e) {
            auditRecorded = true;
            confluenceAuditService.failure(invokerId, "Create Webhook", e);
            return handleWebhookException(e, webhookName);
        } finally {
            if (!auditRecorded) {
                confluenceAuditService.success(invokerId, "Create Webhook");
            }
        }
    }

    private void logWebhookCreationStart(String webhookName, String webhookURL, List<String> events) {
        LOGGER.info("→ createWebhook: Starting webhook creation with name: {}, url: {}, events: {}",
                   webhookName, webhookURL, events);
    }

    private Boolean setDefaultActiveState(Boolean active) {
        return active != null ? active : true;
    }

    private Map<String, Object> buildWebhookRequest(String webhookName, String webhookURL, List<String> events, Boolean active) {
        Map<String, Object> webhookRequest = new HashMap<>();
        webhookRequest.put("name", webhookName);
        webhookRequest.put("url", webhookURL);
        addEventsIfPresent(webhookRequest, events);
        webhookRequest.put("active", active);

        LOGGER.debug("→ createWebhook: Webhook request body: {}", webhookRequest);
        return webhookRequest;
    }

    private void addEventsIfPresent(Map<String, Object> webhookRequest, List<String> events) {
        if (events != null && !events.isEmpty()) {
            webhookRequest.put("events", events);
        }
    }

    private HttpResponse<String> callWebhookAPI(Map<String, Object> webhookRequest) {
        HttpResponse<String> response = confluenceService.getConfluenceAttributes()
                .getRestClient()
                .doSyncPost(null, webhookRequest, "wiki", "rest", "webhooks", "1.0", "webhook");

        logAPIResponse(response);
        return response;
    }

    private void logAPIResponse(HttpResponse<String> response) {
        LOGGER.debug("→ createWebhook: API response status: {}", response.statusCode());
        LOGGER.debug("→ createWebhook: API response body: {}", response.body());
    }

    private ExtensionResponse processWebhookResponse(HttpResponse<String> response, String webhookName) {
        if (isSuccessResponse(response)) {
            return handleSuccessResponse(response, webhookName);
        } else {
            return handleErrorResponse(response, webhookName);
        }
    }

    private boolean isSuccessResponse(HttpResponse<String> response) {
        return response.statusCode() == HttpURLConnection.HTTP_OK ||
               response.statusCode() == HttpURLConnection.HTTP_CREATED;
    }

    private ExtensionResponse handleSuccessResponse(HttpResponse<String> response, String webhookName) {
        JsonObject responseJson = parseResponseJson(response);
        Map<String, Object> webhookResponse = createSuccessResponse(responseJson, webhookName);

        LOGGER.info("→ createWebhook: Successfully created webhook: {}", webhookResponse.get("Name"));
        return new ExtensionResponseBuilder().success(Map.of("Webhook Response", webhookResponse)).build();
    }

    private JsonObject parseResponseJson(HttpResponse<String> response) {
        Gson gson = new Gson();
        return gson.fromJson(response.body(), JsonObject.class);
    }

    private Map<String, Object> createSuccessResponse(JsonObject responseJson, String webhookName) {
        Map<String, Object> webhookResponse = new HashMap<>();
        webhookResponse.put("Name", extractWebhookName(responseJson, webhookName));
        webhookResponse.put("Success", true);
        return webhookResponse;
    }

    private String extractWebhookName(JsonObject responseJson, String fallbackName) {
        return responseJson.has("name") ? responseJson.get("name").getAsString() : fallbackName;
    }

    private ExtensionResponse handleErrorResponse(HttpResponse<String> response, String webhookName) {
        String errorMessage = buildErrorMessage(response);
        LOGGER.error("→ createWebhook: {}", errorMessage);

        Map<String, Object> webhookResponse = createFailureResponse(webhookName);
        return new ExtensionResponseBuilder().success(Map.of("Webhook Response", webhookResponse)).build();
    }

    private String buildErrorMessage(HttpResponse<String> response) {
        return String.format("""
                We couldn't create the webhook.
                This could mean:
                • The webhook URL is invalid or not accessible
                • The Confluence server rejected the request (status code: %d)
                • You don't have permission to create webhooks
                • The webhook configuration might be incorrect

                What to do next:
                1. Verify the webhook URL is valid and accessible
                2. Check if you have admin permissions to create webhooks
                3. Review the webhook configuration for any errors
                4. Ensure your Confluence instance supports webhooks

                Server response: %s
                """, response.statusCode(), response.body());
    }

    private ExtensionResponse handleWebhookException(Exception e, String webhookName) {
        String errorMessage = String.format("""
                We couldn't create the webhook due to an unexpected error.
                This could mean:
                • Your Confluence connection is not working properly
                • The webhook configuration contains invalid data
                • Network connectivity issues are preventing the request
                • The Confluence server is temporarily unavailable

                What to do next:
                1. Verify your Confluence connection is active
                2. Check your webhook configuration (name, URL, events)
                3. Ensure your network connection is stable
                4. Try again in a few moments if the server is busy

                Technical details: %s
                """, e.getMessage());
        LOGGER.error("→ createWebhook: Exception occurred", e);

        Map<String, Object> webhookResponse = createFailureResponse(webhookName);
        return new ExtensionResponseBuilder().success(Map.of("Webhook Response", webhookResponse)).build();
    }

    private Map<String, Object> createFailureResponse(String webhookName) {
        Map<String, Object> webhookResponse = new HashMap<>();
        webhookResponse.put("Name", webhookName);
        webhookResponse.put("Success", false);
        return webhookResponse;
    }
}