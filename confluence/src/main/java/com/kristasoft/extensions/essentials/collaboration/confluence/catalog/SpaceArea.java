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

import app.krista.extension.authorization.MustAuthorizeException;
import app.krista.extension.executor.ExtensionResponse;
import app.krista.extension.executor.ExtensionResponseBuilder;
import app.krista.extension.impl.anno.Attribute;
import app.krista.extension.impl.anno.CatalogRequest;
import app.krista.extension.impl.anno.Domain;
import app.krista.extension.impl.anno.Field;
import app.krista.ksdk.context.RequestContext;
import app.krista.model.base.FreeForm;
import com.kristasoft.extensions.essentials.collaboration.confluence.api.ConfluenceAuditService;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.entities.Page;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.entities.Space;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.errorhandlers.ErrorHandlingStateManager;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.errorhandlers.ExtensionResponseGenerator;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.validators.ValidationOrchestrator;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.validators.Validator;
import com.kristasoft.extensions.essentials.collaboration.confluence.service.PageDocxService;
import com.kristasoft.extensions.essentials.collaboration.confluence.utils.*;
import com.kristasoft.extensions.essentials.collaboration.confluence.service.ConfluenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

import static com.kristasoft.extensions.essentials.collaboration.confluence.utils.Constants.CONFLUENCE_DATA;
import static com.kristasoft.extensions.essentials.collaboration.confluence.utils.Constants.CONFLUENCE_UPDATES;
import static com.kristasoft.extensions.essentials.collaboration.confluence.utils.ParserUtils.setSpaceEntity;
import static com.kristasoft.extensions.essentials.collaboration.confluence.utils.TelemetryHelper.safeTagMap;

/**
 * Catalog area for Confluence space operations (list, watch, and space-level queries).
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
public class SpaceArea {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpaceArea.class);
    private final ConfluenceService service;
    private final ValidationOrchestrator validationOrchestrator;
    private final ErrorHandlingStateManager errorHandlingStateManager;
    private final ExtensionResponseGenerator responseGenerator;
    private final TelemetryHelper telemetryHelper;
    private final ConfluenceAuditService confluenceAuditService;
    private final app.krista.ksdk.context.RequestContext requestContext;


    @Inject
    public SpaceArea(ConfluenceService service,
                     ValidationOrchestrator validationOrchestrator,
                     ErrorHandlingStateManager errorHandlingStateManager,
                     ExtensionResponseGenerator responseGenerator, PageDocxService pageDocxService, TelemetryHelper telemetryHelper,
                     ConfluenceAuditService confluenceAuditService,
                     app.krista.ksdk.context.RequestContext requestContext) {
        this.service = service;
        this.validationOrchestrator = validationOrchestrator;
        this.errorHandlingStateManager = errorHandlingStateManager;
        this.responseGenerator = responseGenerator;
        this.telemetryHelper = telemetryHelper;
        this.confluenceAuditService = confluenceAuditService;
        this.requestContext = requestContext;
    }

    @CatalogRequest(
            id = "localDomainRequest_2e636f7f-b826-4912-8603-fb03b06ae644",
            name = "Get Space By Id",
            description = "Get a single space with the associate ID",
            area = "Space",
            type = CatalogRequest.Type.QUERY_SYSTEM, tool = true)
    @Field.Desc(name = "Space", type = "Entity(Space)", required = false)
    public ExtensionResponse getSpaceById(
            @Field.Text(name = "Space Id or Key", required = true, attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "toolTip", value = "'Enter the Space Id (numeric, e.g. 98304) or the Space Key (e.g. LEG). Either is accepted; the Space Key is the editable identifier shown in Confluence under Space settings.'")}, options = {}) String spaceId,
            @Field.Boolean(name = Constants.ENABLE_ERROR_HANDLING, required = false,
                    attributes = {@Attribute(name = "toolTip", value = "'Enable to allow re-entry of values if validation fails'")}) Boolean allowRetry) {

        String invokerId = requestContext.getInvokerId();
        confluenceAuditService.triggered(invokerId, "Get Space By Id");
        boolean auditRecorded = false;

        long startTime = System.currentTimeMillis();
        LOGGER.info("→ getSpaceById: Starting operation with spaceId: {}, allowRetry: {}", spaceId, allowRetry);

        try {
            telemetryHelper.incrementCount("confluence.getSpaceById");

            // Validate input parameters
            Map<Validator.ValidationResource, String> validationParams = new HashMap<>();

            if (spaceId != null && !spaceId.trim().isEmpty()) {
                validationParams.put(Validator.ValidationResource.SPACE_ID, spaceId);
                LOGGER.debug("→ getSpaceById: Added space ID validation parameter: {}", spaceId);
            }

            // Validate input parameters using dynamic field attribute extraction
            LOGGER.debug("→ getSpaceById: Starting validation with {} parameters", validationParams.size());
            List<ValidationOrchestrator.ValidationResult> validationResults =
                    validationOrchestrator.validate(validationParams, SpaceArea.class, "getSpaceById");
            LOGGER.debug("→ getSpaceById: Validation completed with {} results", validationResults.size());

            if (validationResults.isEmpty()) {
                // Validation passed, perform the get space operation
                LOGGER.debug("→ getSpaceById: Validation passed, retrieving space");
                Space space = setSpaceEntity(service.getSpaceById(spaceId));
                LOGGER.info("→ getSpaceById: Successfully retrieved space: {}", space.id);
                telemetryHelper.recordSuccess("confluence.getSpaceById", startTime,
                        safeTagMap("space_id", spaceId, "space_name", space.name, "space_key", space.id));
                return new ExtensionResponseBuilder().success(Map.of("Space", space)).build();
            } else {
                LOGGER.warn("→ getSpaceById: Validation failed with {} errors", validationResults.size());

                if (Boolean.TRUE.equals(allowRetry)) {
                    LOGGER.debug("→ getSpaceById: Allow Retry is enabled, generating confirmation response");
                    String stateId = UUID.randomUUID().toString();
                    errorHandlingStateManager.put(stateId, Map.of(Constants.VALIDATION_RESULTS, validationResults));
                    Map<String, Object> confirmationParams = Map.of(
                            Constants.STATE_ID, stateId,
                            Constants.SPACE_ID, spaceId != null ? spaceId : "");
                    telemetryHelper.recordRetryPrompted("confluence.getSpaceById", startTime,
                            safeTagMap("space_id", spaceId, "validation_count", String.valueOf(validationResults.size())));
                    ExtensionResponse response = responseGenerator.generateConfirmationResponse(
                            ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                            validationResults,
                            Constants.CONFIRM_REENTER_GET_SPACE_BY_ID,
                            confirmationParams);
                    telemetryHelper.recordSuccess("confluence.getSpaceById", startTime,
                            safeTagMap("space_id", spaceId, "validation_count", String.valueOf(validationResults.size())));
                    return response;
                } else {
                    LOGGER.debug("→ getSpaceById: Allow Retry is disabled, returning direct validation error");
                    telemetryHelper.recordError("confluence.getSpaceById", startTime, new Exception("Validation failed"),
                            safeTagMap("space_id", spaceId, "validation_count", String.valueOf(validationResults.size())));
                    return responseGenerator.generateValidationErrorResponse(validationResults);
                }
            }
        } catch (MustAuthorizeException cause) {
            auditRecorded = true;
            confluenceAuditService.authFailure(invokerId, "Get Space By Id", cause);
            throw cause;
        } catch (Exception cause) {
            auditRecorded = true;
            confluenceAuditService.failure(invokerId, "Get Space By Id", cause);
            LOGGER.error("→ getSpaceById: Exception occurred - {}", cause.getMessage(), cause);

            telemetryHelper.recordError("confluence.getSpaceById", startTime, cause,
                    safeTagMap("space_id", spaceId));
            String errorMessage = String.format("""
                    We couldn't retrieve the space information.
                    This could mean:
                    • The space doesn't exist or was deleted
                    • You don't have permission to view this space
                    • Your Confluence connection needs to be refreshed

                    What to do next:
                    1. Use 'Get Spaces' to see available spaces
                    2. Verify your space permissions
                    3. Check your Confluence connection settings
                    4. Try again in a moment

                    Technical details: %s""", cause.getMessage());
            ExtensionResponse.Error error = new ExtensionResponse.Error(
                    errorMessage,
                    System.currentTimeMillis(),
                    ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR,
                    ""
            );
            LOGGER.debug("→ getSpaceById: Created error response for exception");
            return new ExtensionResponseBuilder().failure(error).build();
        }
        finally {
            if (!auditRecorded) {
                confluenceAuditService.success(invokerId, "Get Space By Id");
            }
        }
    }

    @CatalogRequest(
            id = "localDomainRequest_3a2c1b08-5e9d-4a62-9e2f-1c4e6d8b9f01",
            name = "Get Space Id by Alias",
            description = "Resolve a Space Alias (the value shown in Confluence URLs, e.g. 'johndoe', '~johndoe', 'MFS') to its numeric Space ID. Useful when the numeric Space ID is not visible in the Confluence UI — chain the result into 'Get Space By Id'.",
            area = "Space",
            type = CatalogRequest.Type.QUERY_SYSTEM)
    @Field.Desc(name = "Space", type = "Entity(Space)", required = false)
    @Field.Desc(name = "Space Id", type = "Identifier", required = false)
    public ExtensionResponse getSpaceIdByAlias(
            @Field.Text(name = Constants.SPACE_ALIAS, required = true,
                    attributes = {@Attribute(name = "visualWidth", value = "S"),
                            @Attribute(name = "toolTip", value = "'Enter the Space Alias as shown in Confluence URLs. For personal spaces this is typically a name like ~johndoe (or just johndoe); for global spaces it usually matches the Space Key (e.g. MFS). Use Get Spaces to discover each space currentActiveAlias.'")},
                    options = {}) String spaceAlias) {

        long startTime = System.currentTimeMillis();
        LOGGER.info("→ getSpaceIdByAlias: Starting operation with spaceAlias: {}", spaceAlias);

        try {
            telemetryHelper.incrementCount("confluence.getSpaceIdByAlias");

            if (spaceAlias == null || spaceAlias.trim().isEmpty()) {
                LOGGER.warn("→ getSpaceIdByAlias: spaceAlias is null or empty");
                ExtensionResponse.Error error = new ExtensionResponse.Error(
                        "Space Alias is required. Provide the alias shown in Confluence URLs (e.g. 'johndoe', '~johndoe', 'MFS').",
                        System.currentTimeMillis(),
                        ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                        ""
                );
                telemetryHelper.recordError("confluence.getSpaceIdByAlias", startTime, new Exception("Empty alias"),
                        safeTagMap("space_alias", spaceAlias));
                return new ExtensionResponseBuilder().failure(error).build();
            }

            Space space = setSpaceEntity(service.getSpaceIdByAlias(spaceAlias.trim()));
            LOGGER.info("→ getSpaceIdByAlias: Resolved alias '{}' → Space ID '{}'", spaceAlias, space.id);
            telemetryHelper.recordSuccess("confluence.getSpaceIdByAlias", startTime,
                    safeTagMap("space_alias", spaceAlias, "space_id", space.id, "space_name", space.name));

            return new ExtensionResponseBuilder()
                    .success(Map.of(
                            "Space", space,
                            "Space Id", space.id))
                    .build();
        } catch (Exception cause) {
            LOGGER.error("→ getSpaceIdByAlias: Exception occurred - {}", cause.getMessage(), cause);
            telemetryHelper.recordError("confluence.getSpaceIdByAlias", startTime, cause,
                    safeTagMap("space_alias", spaceAlias));
            ExtensionResponse.Error error = new ExtensionResponse.Error(
                    cause.getMessage(),
                    System.currentTimeMillis(),
                    ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR,
                    ""
            );
            return new ExtensionResponseBuilder().failure(error).build();
        }
    }

    @CatalogRequest(
            id = "localDomainRequest_d9433f59-a182-4209-a092-bb388c1dcb52",
            name = "Get Spaces",
            description = "Get all spaces available in the Confluence Environment",
            area = "Space",
            type = CatalogRequest.Type.QUERY_SYSTEM, tool = true)
    @Field.Desc(name = "Spaces", type = "[ Entity(Space) ]", required = false)
    @Field.Desc(name = "Space Identifiers", type = "[ Identifier ]", required = false)
    public ExtensionResponse getSpaces() {
        String invokerId = requestContext.getInvokerId();
        confluenceAuditService.triggered(invokerId, "Get Spaces");
        boolean auditRecorded = false;

        // Validation passed, perform the get spaces operation
        long startTime = System.currentTimeMillis();
        try {
            telemetryHelper.incrementCount("confluence.getSpaces");

            // Validation passed, perform the get spaces operation
            LOGGER.debug("→ getSpaces: Validation passed, retrieving all spaces");
            List<Space> spaces = Arrays.stream(service.getSpaces())
                    .map(ParserUtils::setSpaceEntity)
                    .collect(Collectors.toList());

            Map<String, Object> result = Map.of(
                    Constants.SPACES, spaces,
                    Constants.SPACE_IDENTIFIERS, spaces.stream().map(space -> space.id).collect(Collectors.toList())
            );

            LOGGER.info("→ getSpaces: Successfully retrieved {} spaces", spaces.size());

            telemetryHelper.recordSuccess("confluence.getSpaces", startTime,
                    safeTagMap("space_count", String.valueOf(spaces.size())));

            return new ExtensionResponseBuilder().success(result).build();
        } catch (MustAuthorizeException cause) {
            auditRecorded = true;
            confluenceAuditService.authFailure(invokerId, "Get Spaces", cause);
            throw cause;
        } catch (Exception cause) {
            auditRecorded = true;
            confluenceAuditService.failure(invokerId, "Get Spaces", cause);
            telemetryHelper.recordError("confluence.getSpaces", startTime, cause, Map.of());
            throw cause;
        } finally {
            if (!auditRecorded) {
                confluenceAuditService.success(invokerId, "Get Spaces");
            }
        }
    }


    @CatalogRequest(
            id = "localDomainRequest_4882f698-c33e-4af1-aea8-30cb1bc17c32",
            name = "Get Direct Child Pages of Space",
            description = "Get only the direct child pages of a space (pages with no parent page, i.e., root pages of the space)",
            area = "Space",
            type = CatalogRequest.Type.QUERY_SYSTEM, tool = true)
    @Field.Desc(name = "Pages", type = "[ Entity(Page) ]", required = false)
    @Field.Desc(name = "Success", type = "Boolean", required = false)
    public ExtensionResponse getDirectChildPagesOfSpace(
            @Field.Text(name = "Space ID", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String spaceID) {

        String invokerId = requestContext.getInvokerId();
        confluenceAuditService.triggered(invokerId, "Get Direct Child Pages of Space");
        boolean auditRecorded = false;

        long startTime = System.currentTimeMillis();
        LOGGER.info("→ getDirectChildPagesOfSpace: Starting operation with spaceID: {}", spaceID);

        try {
            LOGGER.debug("→ getDirectChildPagesOfSpace: Retrieving direct child pages");
            List<Page> pages = Arrays.stream(service.getDirectChildPagesOfSpace(spaceID))
                    .map(ParserUtils::setPageEntity)
                    .collect(Collectors.toList());

            LOGGER.debug("→ getDirectChildPagesOfSpace: Successfully retrieved {} direct child pages", pages.size());


            Map<String, Object> result = Map.of(
                    Constants.PAGES, pages,
                    Constants.PAGE_IDENTIFIERS, pages.stream().map(page -> page.id).collect(Collectors.toList()),
                    "Success", true
            );

            telemetryHelper.recordSuccess("confluence.getDirectChildPagesOfSpace", startTime,
                    safeTagMap("space_id", spaceID, "pages_count", String.valueOf(pages.size())));

            return new ExtensionResponseBuilder().success(result).build();
        } catch (MustAuthorizeException cause) {
            auditRecorded = true;
            confluenceAuditService.authFailure(invokerId, "Get Direct Child Pages of Space", cause);
            throw cause;
        } catch (Exception e) {
            auditRecorded = true;
            confluenceAuditService.failure(invokerId, "Get Direct Child Pages of Space", e);
            LOGGER.error("→ getDirectChildPagesOfSpace: Error occurred while retrieving child pages", e);
            telemetryHelper.recordError("confluence.getDirectChildPagesOfSpace", startTime, e,
                    safeTagMap("space_id", spaceID));

            Map<String, Object> errorResult = Map.of(
                    Constants.PAGES, Collections.emptyList(),
                    Constants.PAGE_IDENTIFIERS, Collections.emptyList(),
                    "Success", false,
                    "Error", e.getMessage()
            );

            return new ExtensionResponseBuilder().success(errorResult).build();
        } finally {
            if (!auditRecorded) {
                confluenceAuditService.success(invokerId, "Get Direct Child Pages of Space");
            }
        }
    }


    @CatalogRequest(
            id = "localDomainRequest_4447b3b9-b2e8-4e5f-b1b3-4aa01dffad32",
            name = "Get Notification",
            description = "Receive webhook-based notifications from Confluence with real-time updates about Space changes, Page updates, and other events.",
            area = "Space",
            type = CatalogRequest.Type.WAIT_FOR_EVENT)
    @Field(name = "Notification Data", type = "FreeForm", required = false, attributes = {}, options = {})
    public Map<String, Object> getNotification(
            @Field(name = "eventName", type = "Text", attributes = {@Attribute(name = "toolTip", value = "'The name of the webhook event received from Confluence'")}) String eventName,
            @Field(name = "eventData", type = "FreeForm") FreeForm eventData) {

        String invokerId = requestContext.getInvokerId();
        confluenceAuditService.triggered(invokerId, "Get Notification");
        boolean auditRecorded = false;

        LOGGER.info("Received event: {}", eventName);
        try {
            if (eventName.equals(CONFLUENCE_UPDATES)) {

                // Get the JSON string from eventData
                String jsonString = (String) eventData.get(CONFLUENCE_DATA);

                // Convert JSON string to FreeForm object
                return JsonParser.convertJsonToFreeForm(jsonString);
            }

            return null;
        } catch (MustAuthorizeException cause) {
            auditRecorded = true;
            confluenceAuditService.authFailure(invokerId, "Get Notification", cause);
            throw cause;
        } catch (Exception e) {
            auditRecorded = true;
            confluenceAuditService.failure(invokerId, "Get Notification", e);
            throw e;
        } finally {
            if (!auditRecorded) {
                confluenceAuditService.success(invokerId, "Get Notification");
            }
        }
    }

}