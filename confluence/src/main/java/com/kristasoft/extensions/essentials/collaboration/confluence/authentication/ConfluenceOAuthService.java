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

import app.krista.extension.executor.Invoker;
import app.krista.ksdk.context.AuthorizationContext;
import app.krista.ksdk.context.RequestContext;
import app.krista.sdk.atlassian.oauth.AbstractAtlassianOAuthService;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.kristasoft.extensions.essentials.collaboration.confluence.ConfluenceAttributes;
import com.kristasoft.extensions.essentials.collaboration.confluence.utils.ConfluenceConstants;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.util.List;

/**
 * Confluence OAuth 2.0 (3LO) service built on the shared Atlassian SDK.
 *
 * <p>The token lifecycle (ScribeJava service construction, refresh-token
 * lookup/rotation, accessible-resources resolution) now lives in
 * {@link AbstractAtlassianOAuthService}; this class contributes only the
 * Confluence scopes, JAX-RS id and authorization prompt, and keeps the
 * legacy {@code getConfluence*} method names as delegates for backward
 * compatibility.</p>
 */
@Service
public final class ConfluenceOAuthService extends AbstractAtlassianOAuthService {

    public static final List<String> SCOPES = List.of(
            "read:space:confluence",
            "write:space:confluence",
            "read:page:confluence",
            "write:page:confluence",
            "read:content:confluence",
            "write:content:confluence",
            "read:content-details:confluence",
            "read:user:confluence",
            "read:template:confluence",
            "write:template:confluence",
            "read:attachment:confluence",
            "write:attachment:confluence",
            "read:configuration:confluence",
            "write:configuration:confluence"
    );

    @Inject
    public ConfluenceOAuthService(RequestContext requestContext, SessionManager sessionManager, Invoker invoker,
                                  AuthorizationContext authorizationContext, ConfluenceAttributes confluenceAttributes) {
        super(requestContext, sessionManager, invoker, authorizationContext, confluenceAttributes);
    }

    @Override
    protected List<String> getScopes() {
        return SCOPES;
    }

    @Override
    protected String getJaxrsId() {
        return ConfluenceConstants.JAXRS_ID;
    }

    @Override
    protected String getAuthorizationPrompt() {
        return ConfluenceConstants.AUTHORIZATION_PROMPT;
    }

    /**
     * Legacy Confluence behaviour: use the first accessible resource instead
     * of requiring every requested scope on it.
     */
    @Override
    protected boolean requireAllScopesOnResource() {
        return false;
    }

    // ── Backward-compatible delegates (legacy public API) ──

    public OAuth2AccessToken getConfluenceAccessToken(String accountId, String invokerId) {
        return getAccessToken(accountId, invokerId);
    }

    public OAuth2AccessToken getConfluenceAccessToken() {
        return getAccessToken();
    }

    public String getConfluenceToken() {
        return getToken();
    }
}
