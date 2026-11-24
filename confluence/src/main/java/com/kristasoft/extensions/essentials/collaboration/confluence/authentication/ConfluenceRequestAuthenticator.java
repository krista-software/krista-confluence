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

import app.krista.ksdk.context.AuthorizationContext;
import app.krista.sdk.atlassian.authn.AbstractAtlassianRequestAuthenticator;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;

/**
 * Confluence {@link app.krista.extension.authorization.RequestAuthenticator}
 * built on the shared Atlassian SDK. The OAuth callback/state handling and
 * the must-authorize redirect (including the
 * {@code &audience=api.atlassian.com&prompt=consent} suffix the legacy
 * implementation appended) live in
 * {@link AbstractAtlassianRequestAuthenticator}.
 */
@Service
public final class ConfluenceRequestAuthenticator extends AbstractAtlassianRequestAuthenticator {

    private final ConfluenceOAuthService confluenceOAuthService;

    @Inject
    public ConfluenceRequestAuthenticator(ConfluenceOAuthService confluenceOAuthService,
                                          AuthorizationContext authorizationContext) {
        super(authorizationContext);
        this.confluenceOAuthService = confluenceOAuthService;
    }

    @Override
    protected String buildAuthorizationUrl(String accountId) {
        return confluenceOAuthService.buildSecuredAuthorizationUrl(accountId);
    }
}
