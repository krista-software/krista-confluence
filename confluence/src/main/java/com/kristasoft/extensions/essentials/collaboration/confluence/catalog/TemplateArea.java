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

import app.krista.extension.executor.ExtensionResponse;
import app.krista.extension.executor.ExtensionResponseBuilder;
import app.krista.extension.impl.anno.Attribute;
import app.krista.extension.impl.anno.CatalogRequest;
import app.krista.extension.impl.anno.Domain;
import app.krista.extension.impl.anno.Field;
import app.krista.ksdk.context.RequestContext;
import com.kristasoft.extensions.essentials.collaboration.confluence.api.ConfluenceAuditService;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.entities.Template;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.errorhandlers.ErrorHandlingStateManager;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.errorhandlers.ExtensionResponseGenerator;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.validators.ValidationOrchestrator;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.validators.Validator;
import com.kristasoft.extensions.essentials.collaboration.confluence.utils.ConfluenceConstants;
import com.kristasoft.extensions.essentials.collaboration.confluence.service.ConfluenceService;
import com.kristasoft.extensions.essentials.collaboration.confluence.utils.Constants;
import com.kristasoft.extensions.essentials.collaboration.confluence.utils.ParserUtils;
import com.kristasoft.extensions.essentials.collaboration.confluence.utils.TelemetryHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

import static com.kristasoft.extensions.essentials.collaboration.confluence.utils.TelemetryHelper.safeTagMap;

/**
 * Catalog area for Confluence template operations (list and create-from-template).
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
public class TemplateArea {

    private static final Logger LOGGER = LoggerFactory.getLogger(TemplateArea.class);

    private final ConfluenceService service;
    private final ValidationOrchestrator validationOrchestrator;
    private final ErrorHandlingStateManager errorHandlingStateManager;
    private final ExtensionResponseGenerator responseGenerator;
    private final TelemetryHelper telemetryHelper;
    private final ConfluenceAuditService confluenceAuditService;
    private final RequestContext requestContext;


    @Inject
    public TemplateArea(ConfluenceService service,
                        ValidationOrchestrator validationOrchestrator,
                        ErrorHandlingStateManager errorHandlingStateManager,
                        ExtensionResponseGenerator responseGenerator, TelemetryHelper telemetryHelper,
                        ConfluenceAuditService confluenceAuditService, RequestContext requestContext) {
        this.service = service;
        this.validationOrchestrator = validationOrchestrator;
        this.errorHandlingStateManager = errorHandlingStateManager;
        this.responseGenerator = responseGenerator;
        this.telemetryHelper = telemetryHelper;
        this.confluenceAuditService = confluenceAuditService;
        this.requestContext = requestContext;
    }

    @CatalogRequest(
            id = "localDomainRequest_b9a052cc-98b1-46c3-b1dd-0890f6940e7c",
            name = "Get Template by ID",
            description = "Get a template associated with the given ID",
            area = "Template",
            type = CatalogRequest.Type.QUERY_SYSTEM, tool = true)
    @Field.Desc(name = "Template", type = "Entity(Template)", required = false)
    public ExtensionResponse getTemplateByID(
            @Field.Text(name = "Template ID",  required = true, attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "toolTip", value = "'Enter the unique identifier of the template you want to retrieve'")}, options = {}) String templateID,
            @Field.Boolean(name = Constants.ENABLE_ERROR_HANDLING, required = false,
                    attributes = {@Attribute(name = "toolTip", value = "'Enable to allow re-entry of values if validation fails'")}) Boolean allowRetry) {

        String invokerId = requestContext.getInvokerId();
        confluenceAuditService.triggered(invokerId, "Get Template by ID");
        boolean auditRecorded = false;
        long startTime = System.currentTimeMillis();
        LOGGER.info("→ getTemplateByID: Starting operation with templateID: {}, allowRetry: {}", templateID, allowRetry);

        try {
            telemetryHelper.incrementCount("confluence.getTemplateByID");

            Map<Validator.ValidationResource, String> validationParams = new HashMap<>();

            if (templateID != null && !templateID.trim().isEmpty()) {
                validationParams.put(Validator.ValidationResource.TEMPLATE_ID, templateID);
                LOGGER.debug("→ getTemplateByID: Added template ID validation parameter: {}", templateID);
            }

            LOGGER.debug("→ getTemplateByID: Starting validation with {} parameters", validationParams.size());
            List<ValidationOrchestrator.ValidationResult> validationResults =
                    validationOrchestrator.validate(validationParams, TemplateArea.class, "getTemplateByID");
            LOGGER.debug("→ getTemplateByID: Validation completed with {} results", validationResults.size());

            if (validationResults.isEmpty()) {
                LOGGER.debug("→ getTemplateByID: Validation passed, retrieving template");
                Template template = ParserUtils.setTemplateEntity(service.getTemplateById(templateID));
                LOGGER.info("→ getTemplateByID: Successfully retrieved template: {}", template.id);
                telemetryHelper.recordSuccess("confluence.getTemplateByID", startTime,
                        safeTagMap("template_id", templateID, "template_name", template.name, "template_type", template.type));

                return new ExtensionResponseBuilder().success(Map.of("Template", template)).build();
            } else {
                LOGGER.warn("→ getTemplateByID: Validation failed with {} errors", validationResults.size());

                if (Boolean.TRUE.equals(allowRetry)) {
                    LOGGER.debug("→ getTemplateByID: Allow Retry is enabled, generating confirmation response");
                    String stateId = UUID.randomUUID().toString();
                    errorHandlingStateManager.put(stateId, Map.of(Constants.VALIDATION_RESULTS, validationResults));
                    Map<String, Object> confirmationParams = Map.of(
                            Constants.STATE_ID, stateId,
                            "Template ID", templateID != null ? templateID : "");
                    telemetryHelper.recordRetryPrompted("confluence.getTemplateByID", startTime,
                            safeTagMap("template_id", templateID, "validation_count", String.valueOf(validationResults.size())));
                    ExtensionResponse response = responseGenerator.generateConfirmationResponse(
                            ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                            validationResults,
                            Constants.CONFIRM_REENTER_GET_TEMPLATE_BY_ID,
                            confirmationParams);
                    telemetryHelper.recordSuccess("confluence.getTemplateByID", startTime,
                            safeTagMap("template_id", templateID, "validation_count", String.valueOf(validationResults.size())));
                    return response;
                } else {
                    LOGGER.debug("→ getTemplateByID: Allow Retry is disabled, returning direct validation error");
                    telemetryHelper.recordError("confluence.getTemplateByID", startTime, new Exception("Validation failed"),
                            safeTagMap("template_id", templateID, "validation_count", String.valueOf(validationResults.size())));
                    return responseGenerator.generateValidationErrorResponse(validationResults);
                }
            }
        } catch (app.krista.extension.authorization.MustAuthorizeException cause) {
            auditRecorded = true;
            confluenceAuditService.authFailure(invokerId, "Get Template by ID", cause);
            throw cause;
        } catch (Exception cause) {
            auditRecorded = true;
            confluenceAuditService.failure(invokerId, "Get Template by ID", cause);
            LOGGER.error("→ getTemplateByID: Exception occurred - {}", cause.getMessage(), cause);

            telemetryHelper.recordError("confluence.getTemplateByID", startTime, cause,
                    safeTagMap("template_id", templateID));
            String errorMessage = String.format("""
                    We couldn't retrieve the template.
                    This could mean:
                    • The Template ID doesn't exist
                    • The template was deleted
                    • You don't have permission to access this template

                    What to do next:
                    1. Use 'Get Templates By Space ID' to see available templates
                    2. Verify the Template ID is correct
                    3. Check your template permissions

                    Technical details: %s""", cause.getMessage());
            ExtensionResponse.Error error = new ExtensionResponse.Error(
                    errorMessage,
                    System.currentTimeMillis(),
                    ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR,
                    ""
            );
            LOGGER.debug("→ getTemplateByID: Created error response for exception");
            return new ExtensionResponseBuilder().failure(error).build();
        } finally {
            if (!auditRecorded) {
                confluenceAuditService.success(invokerId, "Get Template by ID");
            }
        }
    }

    @CatalogRequest(
            id = "localDomainRequest_a3bfb369-4bde-4ed5-af1b-121ae6757bb1",
            name = "Get Templates By Space ID",
            description = "Get the templates inside the space identified by the ID",
            area = "Template",
            type = CatalogRequest.Type.QUERY_SYSTEM, tool = true)
    @Field.Desc(name = "Templates", type = "[ Entity(Template) ]", required = false)
    @Field.Desc(name = "Template Identifiers", type = "[ Identifier ]", required = false)
    public ExtensionResponse getTemplatesBySpaceID(
            @Field.Text(name = "Space ID", required = true, attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "toolTip", value = "'Enter the unique identifier of the Confluence space to retrieve templates from'")}, options = {}) String spaceID,
            @Field.Boolean(name = Constants.ENABLE_ERROR_HANDLING, required = false,
                    attributes = {@Attribute(name = "toolTip", value = "'Enable to allow re-entry of values if validation fails'")}) Boolean allowRetry) {

        String invokerId = requestContext.getInvokerId();
        confluenceAuditService.triggered(invokerId, "Get Templates By Space ID");
        boolean auditRecorded = false;
        long startTime = System.currentTimeMillis();
        LOGGER.info("→ getTemplatesBySpaceID: Starting operation with spaceID: {}, allowRetry: {}", spaceID, allowRetry);

        try {
            telemetryHelper.incrementCount("confluence.getTemplatesBySpaceID");

            Map<Validator.ValidationResource, String> validationParams = new HashMap<>();

            if (spaceID != null && !spaceID.trim().isEmpty()) {
                validationParams.put(Validator.ValidationResource.SPACE_ID, spaceID);
                LOGGER.debug("→ getTemplatesBySpaceID: Added space ID validation parameter: {}", spaceID);
            }

            LOGGER.debug("→ getTemplatesBySpaceID: Starting validation with {} parameters", validationParams.size());
            List<ValidationOrchestrator.ValidationResult> validationResults =
                    validationOrchestrator.validate(validationParams, TemplateArea.class, "getTemplatesBySpaceID");
            LOGGER.debug("→ getTemplatesBySpaceID: Validation completed with {} results", validationResults.size());

            if (validationResults.isEmpty()) {
                LOGGER.debug("→ getTemplatesBySpaceID: Validation passed, retrieving templates");
                List<Template> templates = Arrays.stream(service.getTemplatesBySpaceId(spaceID))
                        .map(ParserUtils::setTemplateEntity)
                        .collect(Collectors.toList());
                LOGGER.info("→ getTemplatesBySpaceID: Successfully retrieved {} templates", templates.size());

                Map<String, Object> result = Map.of(
                        "Templates", templates,
                        "Template Identifiers", templates.stream().map(template -> template.id).collect(Collectors.toList())
                );

                telemetryHelper.recordSuccess("confluence.getTemplatesBySpaceID", startTime,
                        safeTagMap("space_id", spaceID, "template_count", String.valueOf(templates.size())));
                return new ExtensionResponseBuilder().success(result).build();
            } else {
                LOGGER.warn("→ getTemplatesBySpaceID: Validation failed with {} errors", validationResults.size());

                if (Boolean.TRUE.equals(allowRetry)) {
                    LOGGER.debug("→ getTemplatesBySpaceID: Allow Retry is enabled, generating confirmation response");
                    String stateId = UUID.randomUUID().toString();
                    errorHandlingStateManager.put(stateId, Map.of(Constants.VALIDATION_RESULTS, validationResults));
                    Map<String, Object> confirmationParams = Map.of(
                            Constants.STATE_ID, stateId,
                            "Space ID", spaceID != null ? spaceID : "");
                    telemetryHelper.recordRetryPrompted("confluence.getTemplatesBySpaceID", startTime,
                            safeTagMap("space_id", spaceID, "validation_count", String.valueOf(validationResults.size())));
                    ExtensionResponse response = responseGenerator.generateConfirmationResponse(
                            ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                            validationResults,
                            Constants.CONFIRM_REENTER_GET_TEMPLATES_BY_SPACE_ID,
                            confirmationParams);
                    telemetryHelper.recordSuccess("confluence.getTemplatesBySpaceID", startTime,
                            safeTagMap("space_id", spaceID, "validation_count", String.valueOf(validationResults.size())));
                    return response;
                } else {
                    LOGGER.debug("→ getTemplatesBySpaceID: Allow Retry is disabled, returning direct validation error");
                    telemetryHelper.recordError("confluence.getTemplatesBySpaceID", startTime, new Exception("Validation failed"),
                            safeTagMap("space_id", spaceID, "validation_count", String.valueOf(validationResults.size())));
                    return responseGenerator.generateValidationErrorResponse(validationResults);
                }
            }
        } catch (app.krista.extension.authorization.MustAuthorizeException cause) {
            auditRecorded = true;
            confluenceAuditService.authFailure(invokerId, "Get Templates By Space ID", cause);
            throw cause;
        } catch (Exception cause) {
            auditRecorded = true;
            confluenceAuditService.failure(invokerId, "Get Templates By Space ID", cause);
            LOGGER.error("→ getTemplatesBySpaceID: Exception occurred - {}", cause.getMessage(), cause);

            telemetryHelper.recordError("confluence.getTemplatesBySpaceID", startTime, cause,
                    safeTagMap("space_id", spaceID));
            String errorMessage = String.format("""
                    We couldn't retrieve templates from this space.
                    This could mean:
                    • The space doesn't exist or has no templates
                    • You don't have permission to view templates in this space
                    • There's a temporary connection issue

                    What to do next:
                    1. Use 'Get Spaces' to verify the space exists
                    2. Check your space and template permissions
                    3. Try again in a moment

                    Technical details: %s""", cause.getMessage());
            ExtensionResponse.Error error = new ExtensionResponse.Error(
                    errorMessage,
                    System.currentTimeMillis(),
                    ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR,
                    ""
            );
            LOGGER.debug("→ getTemplatesBySpaceID: Created error response for exception");
            return new ExtensionResponseBuilder().failure(error).build();
        } finally {
            if (!auditRecorded) {
                confluenceAuditService.success(invokerId, "Get Templates By Space ID");
            }
        }
    }
}