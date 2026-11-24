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

package com.kristasoft.extensions.essentials.collaboration.confluence.mcp;

import app.krista.extension.executor.Invoker;
import app.krista.sdk.atlassian.mcp.AbstractMcpQueryService;
import app.krista.sdk.atlassian.mcp.DefaultMcpAuthBridge;
import app.krista.sdk.atlassian.mcp.McpProductConfig;
import com.kristasoft.extensions.essentials.collaboration.confluence.ConfluenceAttributes;
import com.kristasoft.extensions.essentials.collaboration.confluence.authentication.ConfluenceOAuthService;
import com.kristasoft.extensions.essentials.collaboration.confluence.authentication.SessionManager;
import com.kristasoft.extensions.essentials.collaboration.confluence.utils.ConfluenceConstants;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;

/**
 * Confluence MCP query service — thin subclass of the SDK's
 * {@link AbstractMcpQueryService} that plugs in Confluence product config
 * and bridges to the Authentication tab's credentials.
 *
 * <p>Used by MCPArea to execute natural language CQL queries via the
 * "Ask Agent" catalog request.</p>
 */
@Service
public class ConfluenceMcpQueryService extends AbstractMcpQueryService {

    @Inject
    public ConfluenceMcpQueryService(SessionManager sessionManager, Invoker invoker,
                                     ConfluenceAttributes confluenceAttributes,
                                     ConfluenceOAuthService confluenceOAuthService) {
        super(sessionManager, invoker,
              new DefaultMcpAuthBridge(confluenceAttributes, confluenceOAuthService,
                                      sessionManager, invoker.getInvokerId()));
    }

    @Override
    protected McpProductConfig getProductConfig() {
        return new ConfluenceMcpProductConfig();
    }

    @Override
    protected String getJaxrsId() {
        return ConfluenceConstants.JAXRS_ID;
    }
}
