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
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.entities.Page;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.entities.Space;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.errorhandlers.ErrorHandlingStateManager;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.errorhandlers.ExtensionResponseGenerator;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.validators.ValidationOrchestrator;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.validators.ValidationOrchestrator.ValidationResult;
import com.kristasoft.extensions.essentials.collaboration.confluence.service.ConfluenceService;
import com.kristasoft.extensions.essentials.collaboration.confluence.utils.Constants;
import com.kristasoft.extensions.essentials.collaboration.confluence.utils.ParserUtils;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Helper class for handling sub-catalog requests related to space operations and validation flows
 * in the Confluence integration.
 * <p>
 * Provides endpoints for confirming re-entry of details, handling user prompts, and retrieving spaces
 * for both authenticated and specified users. This class is typically used by the Krista platform's
 * catalog infrastructure to support remediation and error handling flows.
 * </p>
 */
@Service
public class SpaceAreaSubCatalogRequestHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpaceAreaSubCatalogRequestHelper.class);

    private final ConfluenceService confluenceService;
    private final ErrorHandlingStateManager internalStateManager;
    private final ExtensionResponseGenerator responseGenerator;

    /**
     * Constructs a new SpaceAreaSubCatalogRequestHelper with required dependencies.
     *
     * @param confluenceService the confluence service for space operations
     * @param internalStateManager the error handling state manager
     * @param responseGenerator the extension response generator
     */
    @Inject
    public SpaceAreaSubCatalogRequestHelper(ConfluenceService confluenceService, 
                                          ErrorHandlingStateManager internalStateManager, 
                                          ExtensionResponseGenerator responseGenerator) {
        LOGGER.info("Initializing SpaceAreaSubCatalogRequestHelper with dependencies");
        this.confluenceService = confluenceService;
        this.internalStateManager = internalStateManager;
        this.responseGenerator = responseGenerator;
        LOGGER.debug("SpaceAreaSubCatalogRequestHelper initialized successfully");
    }

    /**
     * Handles confirmation of whether the user wants to re-enter details for getting a space by ID.
     * If re-entry is declined, returns a failure response; otherwise, prompts for re-entry.
     *
     * @param map input map containing reEnter flag, stateId, and spaceId
     * @return an extension response prompting for re-entry or indicating failure
     */
    @SubCatalogRequest(
            name = Constants.CONFIRM_REENTER_GET_SPACE_BY_ID,
            description = "Confirm re-entering space ID for getting space by ID",
            type = CatalogRequest.Type.QUERY_SYSTEM)
    @Field(name = Constants.RE_ENTER, type = "Switch", required = true)
    @SuppressWarnings("unchecked")
    public ExtensionResponse confirmReenterGetSpaceById(@Field.Desc(name = "inputMap",
            type = "{ re-enter: Boolean, stateId: Text, Space Id: Text }", required = true) Map<String, Object> map) {
        LOGGER.info("→ confirmReenterGetSpaceById: Starting confirmation for re-entering space ID");
        LOGGER.debug("→ confirmReenterGetSpaceById: Input map: {}", map);

        Boolean reEnter = (Boolean) map.get(Constants.RE_ENTER);
        String stateId = (String) map.get(Constants.STATE_ID);
        LOGGER.debug("→ confirmReenterGetSpaceById: Extracted values - reEnter: {}, stateId: {}", reEnter, stateId);

        Map<String, Object> state = internalStateManager.get(stateId);
        LOGGER.debug("→ confirmReenterGetSpaceById: Retrieved state: {}", state);

        List<ValidationOrchestrator.ValidationResult> validationResults = getValidationResults(state);
        LOGGER.debug("→ confirmReenterGetSpaceById: Validation results: {}", validationResults);

        if (Boolean.FALSE.equals(reEnter)) {
            LOGGER.debug("→ confirmReenterGetSpaceById: User declined to reenter, generating deny response");
            ExtensionResponse response = responseGenerator.generateFetchDenyResponse(ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    validationResults, null, Map.of());
            LOGGER.info("→ confirmReenterGetSpaceById: Deny response generated");
            return response;
        }

        LOGGER.debug("→ confirmReenterGetSpaceById: User confirmed reenter, generating fetch response");
        ExtensionResponse response = responseGenerator.generateFetchResponse(ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                validationResults, Constants.HANDLE_REENTER_GET_SPACE_BY_ID, state);
        LOGGER.info("→ confirmReenterGetSpaceById: Fetch response generated successfully");
        return response;
    }

    /**
     * Handles the actual get space by ID operation after the user has re-entered the space ID.
     *
     * @param map input map containing stateId and spaceId
     * @return an extension response containing the space details or error information
     */
    @SubCatalogRequest(
            name = Constants.HANDLE_REENTER_GET_SPACE_BY_ID,
            description = "Handle re-entered space ID for getting space by ID",
            type = CatalogRequest.Type.QUERY_SYSTEM)
    @Field.Desc(name = "Space", type = "Entity(Space)", required = false)
    public ExtensionResponse handleReenterGetSpaceById(
            @Field.Desc(name = "inputMap", type = "{ stateId: Text, Space Id: Text }") Map<String, Object> map) {
        LOGGER.info("→ handleReenterGetSpaceById: Starting re-entry handling for get space by ID");
        LOGGER.debug("→ handleReenterGetSpaceById: Input map: {}", map);

        try {
            String stateId = (String) map.get(Constants.STATE_ID);
            String spaceId = (String) map.get(Constants.SPACE_ID);
            LOGGER.debug("→ handleReenterGetSpaceById: Extracted values - stateId: {}, spaceId: {}", stateId, spaceId);

            // Get space by ID
            Space space = ParserUtils.setSpaceEntity(confluenceService.getSpaceById(spaceId));
            LOGGER.debug("→ handleReenterGetSpaceById: Successfully retrieved space: {}", space.id);

            ExtensionResponse response = new ExtensionResponseBuilder().success(Map.of("Space", space)).build();
            LOGGER.info("→ handleReenterGetSpaceById: Successfully retrieved space");
            return response;
        } catch (Exception cause) {
            LOGGER.error("→ handleReenterGetSpaceById: Exception occurred - {}", cause.getMessage(), cause);
            ExtensionResponse.Error error = new ExtensionResponse.Error(Constants.FAILED_TO_GET_SPACE_BY_ID + cause.getMessage(), 
                    System.currentTimeMillis(), ExtensionResponse.Error.ExceptionType.INPUT_ERROR, " SpaceArea -> getSpaceById ");
            return new ExtensionResponseBuilder().failure(error).build();
        }
    }

    /**
     * Processes the provided state map and performs validations, returning a list of validation results.
     *
     * @param state a map representing the current state or context for validation, where keys are state names and values are their corresponding objects
     * @return a list of {@link ValidationOrchestrator.ValidationResult} objects representing the outcome of each validation performed on the state
     */

    private static List<ValidationOrchestrator.ValidationResult> getValidationResults(Map<String, Object> state) {
        LOGGER.debug("→ GET VALIDATION RESULTS START");
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
        LOGGER.debug("→ GET VALIDATION RESULTS END");
        return validationResults;
    }

    /**
     * Handles confirmation of whether the user wants to re-enter details for getting all spaces.
     * If re-entry is declined, returns a failure response; otherwise, prompts for re-entry.
     *
     * @param map input map containing reEnter flag and stateId
     * @return an extension response prompting for re-entry or indicating failure
     */
    @SubCatalogRequest(
            name = Constants.CONFIRM_REENTER_GET_SPACES,
            description = "Confirm re-entering details for getting all spaces",
            type = CatalogRequest.Type.QUERY_SYSTEM)
    @Field(name = Constants.RE_ENTER, type = "Switch", required = true)
    @SuppressWarnings("unchecked")
    public ExtensionResponse confirmReenterGetSpaces(@Field.Desc(name = "inputMap",
            type = "{ re-enter: Boolean, stateId: Text }", required = true) Map<String, Object> map) {
        LOGGER.info("→ confirmReenterGetSpaces: Starting confirmation for re-entering get spaces");
        LOGGER.debug("→ confirmReenterGetSpaces: Input map: {}", map);

        Boolean reEnter = (Boolean) map.get(Constants.RE_ENTER);
        String stateId = (String) map.get(Constants.STATE_ID);
        LOGGER.debug("→ confirmReenterGetSpaces: Extracted values - reEnter: {}, stateId: {}", reEnter, stateId);

        Map<String, Object> state = internalStateManager.get(stateId);
        LOGGER.debug("→ confirmReenterGetSpaces: Retrieved state: {}", state);

        List<ValidationOrchestrator.ValidationResult> validationResults = getValidationResults(state);
        LOGGER.debug("→ confirmReenterGetSpaces: Validation results: {}", validationResults);

        if (Boolean.FALSE.equals(reEnter)) {
            LOGGER.debug("→ confirmReenterGetSpaces: User declined to reenter, generating deny response");
            ExtensionResponse response = responseGenerator.generateFetchDenyResponse(ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    validationResults, null, Map.of());
            LOGGER.info("→ confirmReenterGetSpaces: Deny response generated");
            return response;
        }

        LOGGER.debug("→ confirmReenterGetSpaces: User confirmed reenter, generating fetch response");
        ExtensionResponse response = responseGenerator.generateFetchResponse(ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                validationResults, Constants.HANDLE_REENTER_GET_SPACES, state);
        LOGGER.info("→ confirmReenterGetSpaces: Fetch response generated successfully");
        return response;
    }

    /**
     * Handles the actual get all spaces operation after the user has confirmed re-entry.
     *
     * @param map input map containing stateId
     * @return an extension response containing the spaces list or error information
     */
    @SubCatalogRequest(
            name = Constants.HANDLE_REENTER_GET_SPACES,
            description = "Handle re-entered details for getting all spaces",
            type = CatalogRequest.Type.QUERY_SYSTEM)
    @Field.Desc(name = Constants.SPACES, type = "[ Entity(Space) ]", required = false)
    @Field.Desc(name = Constants.SPACE_IDENTIFIERS, type = "[ Identifier ]", required = false)
    public ExtensionResponse handleReenterGetSpaces(
            @Field.Desc(name = "inputMap", type = "{ stateId: Text }") Map<String, Object> map) {
        LOGGER.info("→ handleReenterGetSpaces: Starting re-entry handling for get all spaces");
        LOGGER.debug("→ handleReenterGetSpaces: Input map: {}", map);

        try {
            String stateId = (String) map.get(Constants.STATE_ID);
            LOGGER.debug("→ handleReenterGetSpaces: Extracted stateId: {}", stateId);

            // Get all spaces
            List<Space> spaces = Arrays.stream(confluenceService.getSpaces())
                    .map(ParserUtils::setSpaceEntity)
                    .collect(Collectors.toList());
            LOGGER.debug("→ handleReenterGetSpaces: Successfully retrieved {} spaces", spaces.size());

            Map<String, Object> result = Map.of(
                    Constants.SPACES, spaces,
                    Constants.SPACE_IDENTIFIERS, spaces.stream().map(space -> space.id).collect(Collectors.toList())
            );

            ExtensionResponse response = new ExtensionResponseBuilder().success(result).build();
            LOGGER.info("→ handleReenterGetSpaces: Successfully retrieved spaces");
            return response;
        } catch (Exception cause) {
            LOGGER.error("→ handleReenterGetSpaces: Exception occurred - {}", cause.getMessage(), cause);
            ExtensionResponse.Error error = new ExtensionResponse.Error(Constants.FAILED_TO_GET_SPACES + cause.getMessage(),
                    System.currentTimeMillis(), ExtensionResponse.Error.ExceptionType.INPUT_ERROR, " SpaceArea -> getSpaces ");
            return new ExtensionResponseBuilder().failure(error).build();
        }
    }

    /**
     * Handles confirmation of whether the user wants to re-enter details for getting direct child pages of space.
     * If re-entry is declined, returns a failure response; otherwise, prompts for re-entry.
     *
     * @param map input map containing reEnter flag, stateId, and spaceId
     * @return an extension response prompting for re-entry or indicating failure
     */
    @SubCatalogRequest(
            name = Constants.CONFIRM_REENTER_GET_DIRECT_CHILD_PAGES_OF_SPACE,
            description = "Confirm re-entering space ID for getting direct child pages of space",
            type = CatalogRequest.Type.QUERY_SYSTEM)
    @Field(name = Constants.RE_ENTER, type = "Switch", required = true)
    @SuppressWarnings("unchecked")
    public ExtensionResponse confirmReenterGetDirectChildPagesOfSpace(@Field.Desc(name = "inputMap",
            type = "{ re-enter: Boolean, stateId: Text, Space ID: Text }", required = true) Map<String, Object> map) {
        LOGGER.info("→ confirmReenterGetDirectChildPagesOfSpace: Starting confirmation for re-entering space ID");
        LOGGER.debug("→ confirmReenterGetDirectChildPagesOfSpace: Input map: {}", map);

        Boolean reEnter = (Boolean) map.get(Constants.RE_ENTER);
        String stateId = (String) map.get(Constants.STATE_ID);
        LOGGER.debug("→ confirmReenterGetDirectChildPagesOfSpace: Extracted values - reEnter: {}, stateId: {}", reEnter, stateId);

        Map<String, Object> state = internalStateManager.get(stateId);
        LOGGER.debug("→ confirmReenterGetDirectChildPagesOfSpace: Retrieved state: {}", state);

        if (Boolean.FALSE.equals(reEnter)) {
            LOGGER.debug("→ confirmReenterGetDirectChildPagesOfSpace: User declined to reenter, generating deny response");
            List<ValidationResult> validationResults = (List<ValidationResult>) state.get(Constants.VALIDATION_RESULTS);
            ExtensionResponse response = responseGenerator.generateFetchDenyResponse(ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    validationResults, null, Map.of());
            LOGGER.info("→ confirmReenterGetDirectChildPagesOfSpace: Deny response generated");
            return response;
        }

        LOGGER.debug("→ confirmReenterGetDirectChildPagesOfSpace: User confirmed reenter, generating fetch response");
        List<ValidationResult> validationResults = (List<ValidationResult>) state.get(Constants.VALIDATION_RESULTS);
        ExtensionResponse response = responseGenerator.generateFetchResponse(ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                validationResults, Constants.HANDLE_REENTER_GET_DIRECT_CHILD_PAGES_OF_SPACE, state);
        LOGGER.info("→ confirmReenterGetDirectChildPagesOfSpace: Fetch response generated successfully");
        return response;
    }

    /**
     * Handles the actual get direct child pages of space operation after the user has re-entered the space ID.
     *
     * @param map input map containing stateId and spaceId
     * @return an extension response containing the direct child pages details or error information
     */
    @SubCatalogRequest(
            name = Constants.HANDLE_REENTER_GET_DIRECT_CHILD_PAGES_OF_SPACE,
            description = "Handle re-entered space ID for getting direct child pages of space",
            type = CatalogRequest.Type.QUERY_SYSTEM)
    @Field.Desc(name = Constants.PAGES, type = "[ Entity(Page) ]", required = false)
    @Field.Desc(name = Constants.PAGE_IDENTIFIERS, type = "[ Identifier ]", required = false)
    public ExtensionResponse handleReenterGetDirectChildPagesOfSpace(
            @Field.Desc(name = "inputMap", type = "{ stateId: Text, Space ID: Text }") Map<String, Object> map) {
        LOGGER.info("→ handleReenterGetDirectChildPagesOfSpace: Starting re-entry handling for get direct child pages of space");
        LOGGER.debug("→ handleReenterGetDirectChildPagesOfSpace: Input map: {}", map);

        try {
            String stateId = (String) map.get(Constants.STATE_ID);
            String spaceId = (String) map.get(Constants.SPACE_ID);
            LOGGER.debug("→ handleReenterGetDirectChildPagesOfSpace: Extracted values - stateId: {}, spaceId: {}", stateId, spaceId);

            // Get direct child pages of space
            List<Page> pages = Arrays.stream(confluenceService.getDirectChildPagesOfSpace(spaceId))
                    .map(ParserUtils::setPageEntity)
                    .collect(Collectors.toList());


            LOGGER.debug("→ handleReenterGetDirectChildPagesOfSpace: Successfully retrieved {} direct child pages", pages.size());

            Map<String, Object> result = Map.of(
                    Constants.PAGES, pages,
                    Constants.PAGE_IDENTIFIERS, pages.stream().map(page -> page.id).collect(Collectors.toList())
            );

            ExtensionResponse response = new ExtensionResponseBuilder().success(result).build();
            LOGGER.info("→ handleReenterGetDirectChildPagesOfSpace: Successfully retrieved direct child pages of space");
            return response;
        } catch (Exception cause) {
            LOGGER.error("→ handleReenterGetDirectChildPagesOfSpace: Exception occurred - {}", cause.getMessage(), cause);
            ExtensionResponse.Error error = new ExtensionResponse.Error(Constants.FAILED_TO_GET_PAGES_IN_SPACE + cause.getMessage(),
                    System.currentTimeMillis(), ExtensionResponse.Error.ExceptionType.INPUT_ERROR, " SpaceArea -> getDirectChildPagesOfSpace ");
            return new ExtensionResponseBuilder().failure(error).build();
        }
    }
}
