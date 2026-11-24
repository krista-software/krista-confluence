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
import app.krista.sdk.atlassian.extension.authentication.AbstractAtlassianAuthResource;
import com.google.gson.JsonObject;
import com.kristasoft.extensions.essentials.collaboration.confluence.ConfluenceAttributes;
import com.kristasoft.extensions.essentials.collaboration.confluence.authentication.ConfluenceOAuthService;
import com.kristasoft.extensions.essentials.collaboration.confluence.authentication.SessionManager;
import com.kristasoft.extensions.essentials.collaboration.confluence.utils.ConfluenceConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.Path;

/**
 * SDK-driven authentication tab for the Confluence extension.
 *
 * <p>All endpoint logic (serving the auth HTML, get/save/test credentials,
 * 3LO authorize URL + callback, token removal) lives in
 * {@link AbstractAtlassianAuthResource}; this class wires Confluence's
 * attribute service and {@link SessionManager} (refresh-token store) into the
 * hooks. Mounted under {@code /sdk} so the legacy {@code /callback} endpoint
 * in {@link AuthenticationResource} keeps working unchanged.</p>
 *
 * <p><strong>Audit store:</strong> the {@link app.krista.sdk.atlassian.audit.KeyValueAuditStore}
 * is obtained from {@link ConfluenceAuditService#getAuditStore()} — the shared singleton —
 * so audit records written by this resource and by catalog areas all land in the same
 * underlying store and are visible together on the audit dashboard.</p>
 */
@Path("/sdk")
public final class ConfluenceAuthUiResource extends AbstractAtlassianAuthResource<ConfluenceAttributes> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfluenceAuthUiResource.class);

    private final ConfluenceAttributes confluenceAttributes;
    private final KeyValueStore keyValueStore;
    private final AuthorizationListener authorizationListener;

    @Inject
    public ConfluenceAuthUiResource(ConfluenceAttributes confluenceAttributes,
                                    ConfluenceOAuthService confluenceOAuthService,
                                    SessionManager sessionManager, RequestContext requestContext,
                                    KeyValueStore keyValueStore,
                                    AuthorizationListener authorizationListener,
                                    ConfluenceAuditService confluenceAuditService) {
        super("confluence", requestContext.getInvokerId(), sessionManager, confluenceOAuthService,
                confluenceAuditService.getAuditStore());
        this.confluenceAttributes = confluenceAttributes;
        this.keyValueStore = keyValueStore;
        this.authorizationListener = authorizationListener;
    }

    @Override
    protected ConfluenceAttributes loadAttributes(String key) {
        return confluenceAttributes;
    }

    @Override
    protected boolean saveAttributes(ConfluenceAttributes attributes, String key) {
        // The validated candidate has already been applied in-memory via
        // BaseAtlassianAttributes#update; the Krista attribute screen remains
        // the system of record for invoker attributes.
        return true;
    }

    @Override
    protected JsonObject getBasicAuthFieldLabels() {
        JsonObject labels = new JsonObject();
        JsonObject hostUrl = new JsonObject();
        hostUrl.addProperty("label", "Domain");
        hostUrl.addProperty("hint", "Your Atlassian site domain name, e.g. acme");
        hostUrl.addProperty("placeholder", "your-site");
        labels.add("hostUrl", hostUrl);
        JsonObject userName = new JsonObject();
        userName.addProperty("label", "Account Email");
        userName.addProperty("hint", "The Atlassian account email the API token belongs to.");
        userName.addProperty("placeholder", "you@company.com");
        labels.add("userName", userName);
        JsonObject authToken = new JsonObject();
        authToken.addProperty("label", "API Key");
        authToken.addProperty("hint", "An API token created at id.atlassian.com \u2192 Security \u2192 API tokens.");
        authToken.addProperty("placeholder", "Atlassian API token");
        labels.add("authToken", authToken);
        labels.addProperty("sidebarDesc", "Domain, account email and API key.");
        return labels;
    }

    /**
     * Returns the Confluence service-account (2LO) field name so the auth
     * tab's {@code toAttributeMap} persists the toggle into the candidate
     * attribute map — matching
     * {@link ConfluenceAttributes#getServiceAccountFieldName()}.
     */
    @Override
    protected String getServiceAccountFieldName() {
        return ConfluenceConstants.SERVICE_ACCOUNT;
    }

    @Override
    protected void onOAuthCallbackSuccess(String refreshTokenKey, String state) {
        LOGGER.info("OAuth callback success — notifying AuthorizationListener (key='{}')", refreshTokenKey);
        authorizationListener.authorized();
    }

    @Override
    protected void removeTokens(ConfluenceAttributes attributes) {
        restoreCredentialsModeFromSnapshot(attributes);
        if (attributes.areOAuthCredentialsPresent() || attributes.isManagedCredentialsEnabled()) {
            keyValueStore.remove(appWideTokenKey(attributes));
        }
    }
}
