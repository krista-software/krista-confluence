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

import java.util.Map;
import app.krista.extension.executor.*;
import app.krista.extension.impl.anno.CatalogRequest;
import app.krista.extension.impl.anno.*;
import app.krista.ksdk.context.RequestContext;
import com.kristasoft.extensions.essentials.collaboration.confluence.api.ConfluenceAuditService;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.entities.User;
import com.kristasoft.extensions.essentials.collaboration.confluence.service.ConfluenceService;
import com.kristasoft.extensions.essentials.collaboration.confluence.utils.ParserUtils;
import com.kristasoft.extensions.essentials.collaboration.confluence.utils.TelemetryHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

import static com.kristasoft.extensions.essentials.collaboration.confluence.utils.TelemetryHelper.safeTagMap;

/**
 * Catalog area for Confluence user lookup operations.
 *
 * <p>All {@link app.krista.extension.impl.anno.CatalogRequest} methods are annotated with
 * {@code tool = true}, exposing them as MCP (Model Context Protocol) tools for AI agent
 * consumption. Each operation records TRIGGERED / SUCCESS / FAILURE audit events via
 * {@link com.kristasoft.extensions.essentials.collaboration.confluence.api.ConfluenceAuditService},
 * which are visible on the extension's audit dashboard.</p>
 */
@Domain(id = "catEntryDomain_1a86a3cc-0f2a-45fa-b9c0-b32487c830f8",
        name = "Confluence",
        ecosystemId = "catEntryEcosystem_0aeef50c-bc48-4c2c-844a-948193e53e82",
        ecosystemName = "Collaboration",
        ecosystemVersion = "68a92f71-9001-48d3-9e99-86b99ad82fde")
public class UserArea {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserArea.class);
    private static final String TAG_ACCOUNT_ID = "account_id";
    private static final String METRIC_GET_USER_BY_ACCOUNT_ID = "confluence.getUserByAccountID";

    private final ConfluenceService service;
    private final TelemetryHelper telemetryHelper;
    private final ConfluenceAuditService confluenceAuditService;
    private final RequestContext requestContext;

    @Inject
    public UserArea(ConfluenceService service, TelemetryHelper telemetryHelper,
                    ConfluenceAuditService confluenceAuditService, RequestContext requestContext) {
        this.service = service;
        this.telemetryHelper = telemetryHelper;
        this.confluenceAuditService = confluenceAuditService;
        this.requestContext = requestContext;
    }

    /**
     * Retrieves detailed user information by Atlassian Account ID.
     * <p>
     * This method queries the Confluence REST API to fetch comprehensive user details including
     * display name, email, account type, status, timezone, locale, and profile picture information.
     * </p>
     *
     * @param accountID the Atlassian account ID of the user (required, cannot be null or empty)
     * @return ExtensionResponse containing:
     *         <ul>
     *           <li>On success: User entity with all available user information</li>
     *           <li>On validation error: INPUT_ERROR with descriptive message</li>
     *           <li>On API error: SYSTEM_ERROR with user-friendly error message</li>
     *         </ul>
     * @throws IllegalArgumentException if accountID is null or empty (handled internally, returned as ExtensionResponse.Error)
     */
    @CatalogRequest(
            id = "localDomainRequest_c916375b-fc95-47e9-b8f0-0f31d571eb6c",
            name = "Get User By Account ID",
            description = "Retrieve Confluence user details by Atlassian Account ID",
            area = "User",
            type = CatalogRequest.Type.QUERY_SYSTEM, tool = true)
    @Field.Desc(name = "User", type = "Entity(User)", required = false)
    public ExtensionResponse getUserByAccountID(
            @Field.Text(name = "Account ID", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String accountID) {

        String invokerId = requestContext.getInvokerId();
        confluenceAuditService.triggered(invokerId, "Get User By Account ID");
        boolean auditRecorded = false;
        long startTime = System.currentTimeMillis();
        LOGGER.info("→ getUserByAccountID: Starting operation with accountID: {}", accountID);

        try {
            telemetryHelper.incrementCount(METRIC_GET_USER_BY_ACCOUNT_ID);

            // Validate input
            if (accountID == null || accountID.trim().isEmpty()) {
                LOGGER.warn("→ getUserByAccountID: Account ID is null or empty");
                telemetryHelper.recordError(METRIC_GET_USER_BY_ACCOUNT_ID, startTime,
                        new IllegalArgumentException("Account ID is required"),
                        safeTagMap(TAG_ACCOUNT_ID, "null_or_empty"));
                ExtensionResponse.Error error = new ExtensionResponse.Error(
                        "Account ID is required and cannot be empty.",
                        System.currentTimeMillis(),
                        ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                        ""
                );
                return new ExtensionResponseBuilder().failure(error).build();
            }

            // Retrieve user from service
            LOGGER.debug("→ getUserByAccountID: Retrieving user from Confluence API");
            com.kristasoft.extensions.essentials.collaboration.confluence.dto.User userDto =
                    service.getUserByAccountId(accountID);

            // Convert DTO to entity
            User user = ParserUtils.setUserEntity(userDto);

            LOGGER.info("→ getUserByAccountID: Successfully retrieved user: {} ({})",
                    user.displayName, user.accountID);

            telemetryHelper.recordSuccess(METRIC_GET_USER_BY_ACCOUNT_ID, startTime,
                    safeTagMap(TAG_ACCOUNT_ID, accountID, "display_name", user.displayName,
                            "account_type", user.accountType));

            return new ExtensionResponseBuilder().success(Map.of("User", user)).build();

        } catch (app.krista.extension.authorization.MustAuthorizeException cause) {
            auditRecorded = true;
            confluenceAuditService.authFailure(invokerId, "Get User By Account ID", cause);
            throw cause;
        } catch (Exception cause) {
            auditRecorded = true;
            confluenceAuditService.failure(invokerId, "Get User By Account ID", cause);
            LOGGER.error("→ getUserByAccountID: Exception occurred - {}", cause.getMessage(), cause);

            telemetryHelper.recordError(METRIC_GET_USER_BY_ACCOUNT_ID, startTime, cause,
                    safeTagMap(TAG_ACCOUNT_ID, accountID));

            String errorMessage = String.format("""
                    We couldn't retrieve the user information.
                    This could mean:
                    • The Account ID is incorrect or doesn't exist
                    • You don't have permission to view user details
                    • There's a temporary connection issue

                    What to do next:
                    1. Double-check the Account ID
                    2. Verify your permissions with your Confluence administrator
                    3. Try again in a moment

                    Technical details: %s""", cause.getMessage());

            ExtensionResponse.Error error = new ExtensionResponse.Error(
                    errorMessage,
                    System.currentTimeMillis(),
                    ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR,
                    ""
            );
            LOGGER.debug("→ getUserByAccountID: Created error response for exception");
            return new ExtensionResponseBuilder().failure(error).build();
        } finally {
            if (!auditRecorded) {
                confluenceAuditService.success(invokerId, "Get User By Account ID");
            }
        }
    }

}

