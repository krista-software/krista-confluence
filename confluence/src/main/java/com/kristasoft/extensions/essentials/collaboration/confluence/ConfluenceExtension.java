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

package com.kristasoft.extensions.essentials.collaboration.confluence;

import app.krista.extension.authorization.RequestAuthenticator;
import app.krista.extension.impl.anno.*;
import com.kristasoft.extensions.essentials.collaboration.confluence.api.APIVersion;
import com.kristasoft.extensions.essentials.collaboration.confluence.authentication.ConfluenceRequestAuthenticator;
import com.kristasoft.extensions.essentials.collaboration.confluence.client.ConfluenceRESTClient;
import com.kristasoft.extensions.essentials.collaboration.confluence.utils.ConfluenceConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.net.HttpURLConnection;
import java.net.http.HttpResponse;
import java.util.Map;

@Extension(name = "Confluence", version = "4.0.4", description = "Integration with Confluence to create and update templates", jaxrsId = "confluenceextn",  implementationModel = Extension.ImplementationModel.Krista_4_O )
@Containerize(baseImageVersion = "3.6.2.sp3")
@Field.Text(name = ConfluenceConstants.DOMAIN_NAME, required = false)
@Field.Text(name = ConfluenceConstants.AUTHORIZATION_EMAIL, required = false)
@Field.Text(name = ConfluenceConstants.API_KEY, isSecured = true, required = false)
@Field.Text(name = ConfluenceConstants.CLIENT_ID, required = false)
@Field.Text(name = ConfluenceConstants.CLIENT_SECRET, isSecured = true, required = false)
@StaticResource(file = "documentation", path = "docs")
@ChangeLog(file = "confluence/src/main/resources/documentation/releaseNotes.md")
@Java(version = Java.Version.JAVA_21)
public class ConfluenceExtension {
    private static final Logger logger = LoggerFactory.getLogger(ConfluenceExtension.class);
    private final ConfluenceAttributes confluenceAttributes;
    private final ConfluenceRequestAuthenticator confluenceRequestAuthenticator;

    @Inject
    public ConfluenceExtension(ConfluenceAttributes confluenceAttributes,
                               ConfluenceRequestAuthenticator confluenceRequestAuthenticator) {
        this.confluenceAttributes = confluenceAttributes;
        this.confluenceRequestAuthenticator = confluenceRequestAuthenticator;
    }

    @InvokerRequest(InvokerRequest.Type.CUSTOM_TABS)
    public Map<String, String> customTabs() {
        return Map.of(
                "Documentation", "static/docs",
                "Authentication", "confluenceextn/sdk/auth/",
                "Confluence Audit", "confluenceextn/sdk/audit/",
                "AI Assistant", "confluenceextn/mcp-setup");
    }

    @InvokerRequest(InvokerRequest.Type.VALIDATE_ATTRIBUTES)
    public void validateAttributes(Map<String, Object> attributes) {
        logger.info("=== VALIDATE_ATTRIBUTES START ===");
        logger.info("Input attributes: {}", attributes);

        try {
            logger.info("Step 1: Setting attributes on confluenceAttributes");
            confluenceAttributes.setAttributes(attributes);
            logger.info("Step 1: Attributes set successfully");

            logger.info("Step 2: Validating attribute credentials");
            confluenceAttributes.validateAttributes(attributes);
            logger.info("Step 2: Credential validation passed");

            logger.info("Step 3: Getting REST client from confluenceAttributes");
            ConfluenceRESTClient client = confluenceAttributes.getRestClient();
            logger.info("Step 3: REST client obtained: {}", client != null ? "SUCCESS" : "NULL");

            logger.info("Step 4: Making doSyncGet call to API");
            logger.info("Step 4: API Version: {}, Endpoint: spaces", APIVersion.V2);
            HttpResponse<String> response = client.doSyncGet(APIVersion.V2, null, "spaces");
            logger.info("Step 4: doSyncGet call completed");

            logger.info("Step 5: Processing response");
            logger.info("Response status code: {}", response != null ? response.statusCode() : "NULL_RESPONSE");
            logger.info("Response body: {}", response != null ? response.body() : "NULL_RESPONSE");

            if (response != null && response.statusCode() == HttpURLConnection.HTTP_OK) {
                logger.info("Step 6: Validation SUCCESS - HTTP 200 received");
                logger.info("Attributes validated successfully");
            } else {
                logger.error("Step 6: Validation FAILED - Non-200 status or null response");
                String errorMessage = response != null ? response.body() : "Failed to get api response";
                logger.error("Error message: {}", errorMessage);
                throw new RuntimeException(errorMessage);
            }
        } catch (app.krista.extension.authorization.MustAuthorizeException e) {
            logger.info("OAuth authorization required - redirecting to Atlassian login...");
            throw e;
        } catch (Exception e) {
            logger.error("Exception in validateAttributes: {}", e.getMessage(), e);
            throw e;
        } finally {
            logger.info("=== VALIDATE_ATTRIBUTES END ===");
        }
    }

    @InvokerRequest(InvokerRequest.Type.TEST_CONNECTION)
    public void testConnection() {
        logger.info("=== TEST_CONNECTION START ===");
        logger.info("Getting attributes from confluenceAttributes");
        Map<String, Object> attrs = confluenceAttributes.getAttributes();
        logger.info("Retrieved attributes: {}", attrs);
        logger.info("Calling validateAttributes with retrieved attributes");
        validateAttributes(attrs);
        logger.info("=== TEST_CONNECTION END ===");
    }

    @InvokerRequest(InvokerRequest.Type.AUTHENTICATOR)
    public RequestAuthenticator getRequestAuthenticator() {
        return confluenceRequestAuthenticator;
    }
}
