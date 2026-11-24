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

import app.krista.extension.executor.ExtensionResponse;
import app.krista.extension.impl.anno.Attribute;
import app.krista.extension.impl.anno.CatalogRequest;
import app.krista.extension.impl.anno.Domain;
import app.krista.extension.impl.anno.Field;
import app.krista.sdk.atlassian.mcp.AbstractMcpArea;
import com.kristasoft.extensions.essentials.collaboration.confluence.utils.ConfluenceConstants;

import javax.inject.Inject;

/**
 * Confluence catalog entry point for the MCP "Ask Agent" natural language query.
 */
@Domain(id = ConfluenceConstants.DOMAIN_ID,
        name = ConfluenceConstants.KRISTA_DOMAIN_NAME,
        ecosystemId = ConfluenceConstants.ECOSYSTEM_ID,
        ecosystemName = ConfluenceConstants.ECOSYSTEM_NAME,
        ecosystemVersion = "4a6f7346-f826-4bc6-aff6-99374798fdd9")
public class MCPArea extends AbstractMcpArea {

    @Inject
    public MCPArea(ConfluenceMcpQueryService mcpQueryService) {
        super(mcpQueryService);
    }

    @Override
    @CatalogRequest(
            id = CATALOG_REQUEST_ID,
            name = CATALOG_REQUEST_NAME,
            description = CATALOG_REQUEST_DESCRIPTION,
            area = CATALOG_REQUEST_AREA,
            type = CatalogRequest.Type.QUERY_SYSTEM)
    @Field.Desc(name = "Response", type = "[ FreeForm ]", required = false,
            description = "Results matching the query")
    public ExtensionResponse askAgent(
            @Field(name = "Query", type = "Paragraph", required = true, description = "",
                    attributes = {@Attribute(name = "visualWidth", value = "L")},
                    options = {}) String query) {
        return super.askAgent(query);
    }
}
