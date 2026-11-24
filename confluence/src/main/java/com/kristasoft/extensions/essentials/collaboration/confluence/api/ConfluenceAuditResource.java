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

import app.krista.ksdk.context.RequestContext;
import app.krista.sdk.atlassian.audit.AbstractAuditResource;
import app.krista.sdk.atlassian.audit.AuditData;
import app.krista.sdk.atlassian.audit.KeyValueAuditStore;
import app.krista.sdk.atlassian.config.AuthMethod;
import com.kristasoft.extensions.essentials.collaboration.confluence.ConfluenceAttributes;
import com.kristasoft.extensions.essentials.collaboration.confluence.authentication.SessionManager;

import javax.inject.Inject;
import javax.ws.rs.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * SDK-driven audit dashboard for the Confluence extension.
 *
 * <p>The dashboard UI and JSON endpoints live in
 * {@link AbstractAuditResource}; persistence is the SDK's
 * {@link KeyValueAuditStore} (bounded, KV-backed), so this class only
 * contributes Confluence's credential health summary.</p>
 */
@Path("/sdk/audit")
public final class ConfluenceAuditResource extends AbstractAuditResource {

    private final KeyValueAuditStore auditStore;
    private final ConfluenceAttributes confluenceAttributes;
    private final SessionManager sessionManager;

    @Inject
    public ConfluenceAuditResource(ConfluenceAuditService confluenceAuditService, RequestContext requestContext,
                                   ConfluenceAttributes confluenceAttributes, SessionManager sessionManager) {
        super(requestContext.getInvokerId());
        this.auditStore = confluenceAuditService.getAuditStore();
        this.confluenceAttributes = confluenceAttributes;
        this.sessionManager = sessionManager;
    }

    @Override
    protected AuditData getAuditData(String invokerId, Long from, Long to, String type, String status) {
        return auditStore.getData(invokerId, from, to, type, status);
    }

    @Override
    protected void clearAuditRecords(String invokerId, Long from, Long to) {
        auditStore.clearRecords(invokerId, from, to);
    }

    @Override
    protected Map<String, Object> getGlobalCounter() {
        return auditStore.getGlobalCounter();
    }

    @Override
    protected Map<String, Object> buildHealthResponse() {
        Map<String, Object> health = new HashMap<>();
        try {
            if (confluenceAttributes.areBasicCredentialsPresent()) {
                health.put("authMethod", AuthMethod.BASIC.name());
                health.put("hostUrl", confluenceAttributes.getHostUrl());
                health.put("userName", confluenceAttributes.getUserName());
                health.put("hasRefreshToken", "N/A"); // basic auth uses no tokens
            } else if (confluenceAttributes.areOAuthCredentialsPresent()) {
                boolean serviceAccount = confluenceAttributes.isServiceAccountModeEnabled();
                health.put("authMethod", serviceAccount
                        ? AuthMethod.OAUTH2_CLIENT_CREDENTIALS.name()
                        : AuthMethod.OAUTH2.name());
                health.put("clientId", mask(confluenceAttributes.getClientId()));
                if (serviceAccount) {
                    health.put("hasRefreshToken", "N/A"); // 2LO uses no refresh tokens
                } else {
                    String appKey = confluenceAttributes.getClientId() + "#"
                            + confluenceAttributes.getClientSecret() + "#" + getInvokerId();
                    String refreshToken = sessionManager.get(appKey);
                    health.put("hasRefreshToken", refreshToken != null && !refreshToken.isEmpty());
                }
            } else {
                health.put("authMethod", "not configured");
                health.put("hasRefreshToken", false);
            }
        } catch (Exception e) {
            health.put("authMethod", "error");
            health.put("hasRefreshToken", false);
        }
        return health;
    }
}
