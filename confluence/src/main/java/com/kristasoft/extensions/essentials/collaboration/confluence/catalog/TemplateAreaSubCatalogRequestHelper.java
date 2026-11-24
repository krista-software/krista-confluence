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
import app.krista.extension.impl.anno.CatalogRequest;
import app.krista.extension.impl.anno.Field;
import app.krista.extension.impl.anno.SubCatalogRequest;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.entities.Template;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.errorhandlers.ErrorHandlingStateManager;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.errorhandlers.ExtensionResponseGenerator;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.validators.ValidationOrchestrator;
import com.kristasoft.extensions.essentials.collaboration.confluence.service.ConfluenceService;
import com.kristasoft.extensions.essentials.collaboration.confluence.utils.Constants;
import com.kristasoft.extensions.essentials.collaboration.confluence.utils.ParserUtils;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Helper class for handling SubCatalogRequest operations related to Template area error handling.
 * <p>
 * This class provides confirmation and re-entry handling for template operations that encounter
 * validation errors. It follows the same pattern as other area SubCatalogRequest helpers.
 * </p>
 */
@Service
public class TemplateAreaSubCatalogRequestHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(TemplateAreaSubCatalogRequestHelper.class);

    private final ConfluenceService confluenceService;
    private final ErrorHandlingStateManager internalStateManager;
    private final ExtensionResponseGenerator responseGenerator;
    private final ValidationOrchestrator validationOrchestrator;

    @Inject
    public TemplateAreaSubCatalogRequestHelper(ConfluenceService confluenceService,
                                              ErrorHandlingStateManager internalStateManager,
                                              ExtensionResponseGenerator responseGenerator,
                                              ValidationOrchestrator validationOrchestrator) {
        this.confluenceService = confluenceService;
        this.internalStateManager = internalStateManager;
        this.responseGenerator = responseGenerator;
        this.validationOrchestrator = validationOrchestrator;
    }

    /**
     * Handles confirmation of whether the user wants to re-enter details for getting template by ID.
     * If re-entry is declined, returns a failure response; otherwise, prompts for re-entry.
     *
     * @param map input map containing reEnter flag, stateId, and templateId
     * @return an extension response prompting for re-entry or indicating failure
     */
    @SubCatalogRequest(
            name = Constants.CONFIRM_REENTER_GET_TEMPLATE_BY_ID,
            description = "Confirm re-entering template ID for getting template by ID",
            type = CatalogRequest.Type.QUERY_SYSTEM)
    @Field(name = Constants.RE_ENTER, type = "Switch", required = true)
    @SuppressWarnings("unchecked")
    public ExtensionResponse confirmReenterGetTemplateById(@Field.Desc(name = "inputMap",
            type = "{ stateId: Text, Template ID: Text, re-enter: Switch }") Map<String, Object> map) {
        try {
            Boolean reEnter = (Boolean) map.get(Constants.RE_ENTER);
            String stateId = (String) map.get(Constants.STATE_ID);
            LOGGER.debug("→ confirmReenterGetTemplateById: Re-enter: {}, State ID: {}", reEnter, stateId);

            Map<String, Object> storedState = internalStateManager.get(stateId);
            List<ValidationOrchestrator.ValidationResult> validationResults = getValidationResults(storedState);

            if (Boolean.FALSE.equals(reEnter)) {
                LOGGER.info("→ confirmReenterGetTemplateById: User declined re-entry, returning failure response");
                return responseGenerator.generateFetchDenyResponse(
                        ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                        validationResults,
                        Constants.HANDLE_REENTER_GET_TEMPLATE_BY_ID,
                        map);
            } else {
                LOGGER.info("→ confirmReenterGetTemplateById: User accepted re-entry, generating fetch response");
                return responseGenerator.generateFetchResponse(
                        ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                        validationResults,
                        Constants.HANDLE_REENTER_GET_TEMPLATE_BY_ID,
                        map);
            }
        } catch (Exception cause) {
            LOGGER.error("→ confirmReenterGetTemplateById: Exception occurred - {}", cause.getMessage(), cause);
            ExtensionResponse.Error error = new ExtensionResponse.Error(Constants.FAILED_TO_GET_TEMPLATE_BY_ID_VALIDATION + cause.getMessage(), 
                    System.currentTimeMillis(), ExtensionResponse.Error.ExceptionType.INPUT_ERROR, " TemplateArea -> getTemplateByID ");
            return new ExtensionResponseBuilder().failure(error).build();
        }
    }

    /**
     * Handles the actual get template by ID operation after the user has re-entered the template ID.
     *
     * @param map input map containing stateId and templateId
     * @return an extension response containing the template details or error information
     */
    @SubCatalogRequest(
            name = Constants.HANDLE_REENTER_GET_TEMPLATE_BY_ID,
            description = "Handle re-entered template ID for getting template by ID",
            type = CatalogRequest.Type.QUERY_SYSTEM)
    @Field.Desc(name = "Template", type = "Entity(Template)", required = false)
    public ExtensionResponse handleReenterGetTemplateById(
            @Field.Desc(name = "inputMap", type = "{ stateId: Text, Template ID: Text }") Map<String, Object> map) {
        LOGGER.info("→ handleReenterGetTemplateById: Starting re-entry handling for get template by ID");
        LOGGER.debug("→ handleReenterGetTemplateById: Input map: {}", map);

        try {
            String templateId = (String) map.get("Template ID");
            LOGGER.info("→ handleReenterGetTemplateById: Template ID: {}", templateId);

            Template template = ParserUtils.setTemplateEntity(confluenceService.getTemplateById(templateId));
            LOGGER.info("→ handleReenterGetTemplateById: Successfully retrieved template: {}", template.id);

            Map<String, Object> result = Map.of("Template", template);

            ExtensionResponse response = new ExtensionResponseBuilder().success(result).build();
            LOGGER.info("→ handleReenterGetTemplateById: Successfully retrieved template");
            return response;
        } catch (Exception cause) {
            LOGGER.error("→ handleReenterGetTemplateById: Exception occurred - {}", cause.getMessage(), cause);
            ExtensionResponse.Error error = new ExtensionResponse.Error(Constants.FAILED_TO_GET_TEMPLATE_BY_ID + cause.getMessage(),
                    System.currentTimeMillis(), ExtensionResponse.Error.ExceptionType.INPUT_ERROR, " TemplateArea -> getTemplateByID ");
            return new ExtensionResponseBuilder().failure(error).build();
        }
    }

    /**
     * Handles confirmation of whether the user wants to re-enter details for getting templates by space ID.
     * If re-entry is declined, returns a failure response; otherwise, prompts for re-entry.
     *
     * @param map input map containing reEnter flag, stateId, and spaceId
     * @return an extension response prompting for re-entry or indicating failure
     */
    @SubCatalogRequest(
            name = Constants.CONFIRM_REENTER_GET_TEMPLATES_BY_SPACE_ID,
            description = "Confirm re-entering space ID for getting templates by space ID",
            type = CatalogRequest.Type.QUERY_SYSTEM)
    @Field(name = Constants.RE_ENTER, type = "Switch", required = true)
    @SuppressWarnings("unchecked")
    public ExtensionResponse confirmReenterGetTemplatesBySpaceId(@Field.Desc(name = "inputMap",
            type = "{ stateId: Text, Space ID: Text, re-enter: Switch }") Map<String, Object> map) {
        try {
            Boolean reEnter = (Boolean) map.get(Constants.RE_ENTER);
            String stateId = (String) map.get(Constants.STATE_ID);
            LOGGER.debug("→ confirmReenterGetTemplatesBySpaceId: Re-enter: {}, State ID: {}", reEnter, stateId);

            Map<String, Object> storedState = internalStateManager.get(stateId);
            List<ValidationOrchestrator.ValidationResult> validationResults = getValidationResults(storedState);

            if (Boolean.FALSE.equals(reEnter)) {
                LOGGER.info("→ confirmReenterGetTemplatesBySpaceId: User declined re-entry, returning failure response");
                return responseGenerator.generateFetchDenyResponse(
                        ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                        validationResults,
                        Constants.HANDLE_REENTER_GET_TEMPLATES_BY_SPACE_ID,
                        map);
            } else {
                LOGGER.info("→ confirmReenterGetTemplatesBySpaceId: User accepted re-entry, generating fetch response");
                return responseGenerator.generateFetchResponse(
                        ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                        validationResults,
                        Constants.HANDLE_REENTER_GET_TEMPLATES_BY_SPACE_ID,
                        map);
            }
        } catch (Exception cause) {
            LOGGER.error("→ confirmReenterGetTemplatesBySpaceId: Exception occurred - {}", cause.getMessage(), cause);
            ExtensionResponse.Error error = new ExtensionResponse.Error(Constants.FAILED_TO_GET_TEMPLATES_BY_SPACE_ID_VALIDATION + cause.getMessage(),
                    System.currentTimeMillis(), ExtensionResponse.Error.ExceptionType.INPUT_ERROR, " TemplateArea -> getTemplatesBySpaceID ");
            return new ExtensionResponseBuilder().failure(error).build();
        }
    }

    /**
     * Handles the actual get templates by space ID operation after the user has re-entered the space ID.
     *
     * @param map input map containing stateId and spaceId
     * @return an extension response containing the templates and template identifiers or error information
     */
    @SubCatalogRequest(
            name = Constants.HANDLE_REENTER_GET_TEMPLATES_BY_SPACE_ID,
            description = "Handle re-entered space ID for getting templates by space ID",
            type = CatalogRequest.Type.QUERY_SYSTEM)
    @Field.Desc(name = "Templates", type = "[ Entity(Template) ]", required = false)
    @Field.Desc(name = "Template Identifiers", type = "[ Identifier ]", required = false)
    public ExtensionResponse handleReenterGetTemplatesBySpaceId(
            @Field.Desc(name = "inputMap", type = "{ stateId: Text, Space ID: Text }") Map<String, Object> map) {
        LOGGER.info("→ handleReenterGetTemplatesBySpaceId: Starting re-entry handling for get templates by space ID");
        LOGGER.debug("→ handleReenterGetTemplatesBySpaceId: Input map: {}", map);

        try {
            String spaceId = (String) map.get("Space ID");
            LOGGER.info("→ handleReenterGetTemplatesBySpaceId: Space ID: {}", spaceId);

            List<Template> templates = Arrays.stream(confluenceService.getTemplatesBySpaceId(spaceId))
                    .map(ParserUtils::setTemplateEntity)
                    .collect(Collectors.toList());
            LOGGER.info("→ handleReenterGetTemplatesBySpaceId: Successfully retrieved {} templates", templates.size());

            Map<String, Object> result = Map.of(
                    "Templates", templates,
                    "Template Identifiers", templates.stream().map(template -> template.id).collect(Collectors.toList())
            );

            ExtensionResponse response = new ExtensionResponseBuilder().success(result).build();
            LOGGER.info("→ handleReenterGetTemplatesBySpaceId: Successfully retrieved templates by space ID");
            return response;
        } catch (Exception cause) {
            LOGGER.error("→ handleReenterGetTemplatesBySpaceId: Exception occurred - {}", cause.getMessage(), cause);
            ExtensionResponse.Error error = new ExtensionResponse.Error(Constants.FAILED_TO_GET_TEMPLATES_BY_SPACE_ID + cause.getMessage(),
                    System.currentTimeMillis(), ExtensionResponse.Error.ExceptionType.INPUT_ERROR, " TemplateArea -> getTemplatesBySpaceID ");
            return new ExtensionResponseBuilder().failure(error).build();
        }
    }

    /**
     * Helper method to properly deserialize validation results from stored state.
     * This handles the Gson deserialization issue where ValidationResult records
     * are deserialized as LinkedTreeMap objects.
     *
     * @param state the state map containing validation results
     * @return properly deserialized list of ValidationResult objects
     */
    private static List<ValidationOrchestrator.ValidationResult> getValidationResults(Map<String, Object> state) {
        LOGGER.debug("→ getValidationResults: Deserializing validation results from stored state");
        LOGGER.debug("→ State: {}", state);

        List<ValidationOrchestrator.ValidationResult> validationResults = new ArrayList<>();
        List<?> results = (List<?>) state.get(Constants.VALIDATION_RESULTS);
        LOGGER.debug("→ Raw validation results from state: {}", results);

        if (results != null) {
            for (Object item : results) {
                LOGGER.debug("→ Processing validation result item: {} (type: {})", item, (item != null ? item.getClass().getSimpleName() : "null"));
                ValidationOrchestrator.ValidationResult validationResult = Constants.GSON.fromJson(Constants.GSON.toJson(item), ValidationOrchestrator.ValidationResult.class);
                LOGGER.debug("→ Converted validation result: {}", validationResult);
                validationResults.add(validationResult);
            }
        } else {
            LOGGER.debug("→ No validation results found in state");
        }

        LOGGER.debug("→ Final validation results: {}", validationResults);
        return validationResults;
    }
}
