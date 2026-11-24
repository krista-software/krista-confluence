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

import app.krista.sdk.atlassian.config.AtlassianProduct;
import app.krista.sdk.atlassian.registry.annotations.AtlassianExtension;
import app.krista.sdk.atlassian.registry.annotations.OAuthScope;
import com.kristasoft.extensions.essentials.collaboration.confluence.utils.ConfluenceConstants;

/**
 * Atlassian SDK registry marker for the Confluence extension. Scanned via
 * {@link ConfluenceExtensionDescriptor} and the
 * {@code META-INF/services/app.krista.sdk.atlassian.registry.AtlassianExtensionDescriptor}
 * service registration.
 *
 * <p>The declared scopes mirror
 * {@link com.kristasoft.extensions.essentials.collaboration.confluence.authentication.ConfluenceOAuthService#SCOPES}.</p>
 */
@AtlassianExtension(extensionType = "confluence", product = AtlassianProduct.CONFLUENCE,
        jaxrsId = ConfluenceConstants.JAXRS_ID, displayName = "Confluence")
@OAuthScope(value = "read:space:confluence", description = "Read Confluence spaces",
        catalogRequests = {"Get Space By Id", "Get Spaces", "Get Direct Child Pages of Space"})
@OAuthScope(value = "write:space:confluence", description = "Create and update Confluence spaces",
        catalogRequests = {})
@OAuthScope(value = "read:page:confluence", description = "Read Confluence pages",
        catalogRequests = {"Get Page By ID", "Get Pages in Space", "Get Page in Space",
                "Get Direct Child Pages of Page", "Check if Page Has Children",
                "Get All Nested Descendants of Page", "Get Ancestors of Page",
                "Get Direct Child Pages of Space"})
@OAuthScope(value = "write:page:confluence", description = "Create and update Confluence pages",
        catalogRequests = {"Create Page from Content", "Create Page from Template",
                "Update Page by Content", "Update Page from Template"})
@OAuthScope(value = "read:content:confluence", description = "Read Confluence content",
        catalogRequests = {"Generate Content for Table", "Generate Heading Content", "Generate Anchor Tag"})
@OAuthScope(value = "write:content:confluence", description = "Create and update Confluence content",
        catalogRequests = {"Generate Content for Table", "Generate Heading Content", "Generate Anchor Tag"})
@OAuthScope(value = "read:content-details:confluence", description = "Read Confluence content details",
        catalogRequests = {"Get Page By ID", "Get Page in Space"})
@OAuthScope(value = "read:user:confluence", description = "Read Confluence user information",
        catalogRequests = {"Get User By Account ID"})
@OAuthScope(value = "read:template:confluence", description = "Read Confluence templates",
        catalogRequests = {"Get Template by ID", "Get Templates By Space ID",
                "Create Page from Template", "Update Page from Template"})
@OAuthScope(value = "write:template:confluence", description = "Create and update Confluence templates",
        catalogRequests = {})
@OAuthScope(value = "read:attachment:confluence", description = "Read Confluence attachments",
        catalogRequests = {"Get Attachments for Page", "Get Attachment by ID", "Download Attachment"})
@OAuthScope(value = "write:attachment:confluence", description = "Upload Confluence attachments",
        catalogRequests = {})
@OAuthScope(value = "read:configuration:confluence", description = "Read Confluence configuration and webhooks",
        catalogRequests = {"Get Notification"})
@OAuthScope(value = "write:configuration:confluence", description = "Create and update Confluence configuration",
        catalogRequests = {"Create Webhook"})
public final class ConfluenceExtensionConfig {

    private ConfluenceExtensionConfig() {
    }
}
