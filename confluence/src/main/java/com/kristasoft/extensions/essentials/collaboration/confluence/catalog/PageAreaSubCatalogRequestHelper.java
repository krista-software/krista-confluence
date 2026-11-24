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
import app.krista.model.base.File;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.entities.Content;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.entities.Page;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.errorhandlers.ErrorHandlingStateManager;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.errorhandlers.ExtensionResponseGenerator;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.stores.Pair;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.validators.ValidationOrchestrator;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.validators.ValidationOrchestrator.ValidationResult;
import com.kristasoft.extensions.essentials.collaboration.confluence.service.ConfluenceService;
import com.kristasoft.extensions.essentials.collaboration.confluence.service.PageDocxService;
import com.kristasoft.extensions.essentials.collaboration.confluence.service.PageMarkdownService;
import com.kristasoft.extensions.essentials.collaboration.confluence.utils.Constants;
import com.kristasoft.extensions.essentials.collaboration.confluence.utils.ParserUtils;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Helper class for handling sub-catalog requests related to page operations and validation flows
 * in the Confluence integration.
 * <p>
 * Provides endpoints for confirming re-entry of details, handling user prompts, and retrieving pages
 * for both authenticated and specified users. This class is typically used by the Krista platform's
 * catalog infrastructure to support remediation and error handling flows.
 * </p>
 */
@Service
public class PageAreaSubCatalogRequestHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(PageAreaSubCatalogRequestHelper.class);

    private final ConfluenceService confluenceService;
    private final ErrorHandlingStateManager internalStateManager;
    private final ExtensionResponseGenerator responseGenerator;
    private final ValidationOrchestrator validationOrchestrator;
    private final PageDocxService pageDocxService;
    private final PageMarkdownService pageMarkdownService;


    @Inject
    public PageAreaSubCatalogRequestHelper(ConfluenceService confluenceService,
                                           ErrorHandlingStateManager internalStateManager,
                                           ExtensionResponseGenerator responseGenerator,
                                           ValidationOrchestrator validationOrchestrator,
                                           PageDocxService pageDocxService, PageMarkdownService pageMarkdownService) {
        this.confluenceService = confluenceService;
        this.internalStateManager = internalStateManager;
        this.responseGenerator = responseGenerator;
        this.validationOrchestrator = validationOrchestrator;
        this.pageDocxService = pageDocxService;
        this.pageMarkdownService = pageMarkdownService;
    }

    /**
     * Handles confirmation of whether the user wants to re-enter details for getting pages in space.
     * If re-entry is declined, returns a failure response; otherwise, prompts for re-entry.
     *
     * @param map input map containing reEnter flag, stateId, spaceId, and title
     * @return an extension response prompting for re-entry or indicating failure
     */
    @SubCatalogRequest(
            name = Constants.CONFIRM_REENTER_GET_PAGES_IN_SPACE,
            description = "Confirm re-entering space ID for getting pages in space",
            type = CatalogRequest.Type.QUERY_SYSTEM)
    @Field(name = Constants.RE_ENTER, type = "Switch", required = true)
    @SuppressWarnings("unchecked")
    public ExtensionResponse confirmReenterGetPagesInSpace(@Field.Desc(name = "inputMap",
            type = "{ stateId: Text, Space Id: Text, Title: Text, re-enter: Switch }") Map<String, Object> map) {
        try {
            Boolean reEnter = (Boolean) map.get(Constants.RE_ENTER);
            String stateId = (String) map.get(Constants.STATE_ID);
            LOGGER.debug("→ confirmReenterGetPagesInSpace: Re-enter: {}, State ID: {}", reEnter, stateId);

            Map<String, Object> storedState = internalStateManager.get(stateId);
            List<ValidationOrchestrator.ValidationResult> validationResults = getValidationResults(storedState);

            if (Boolean.FALSE.equals(reEnter)) {
                LOGGER.info("→ confirmReenterGetPagesInSpace: User declined re-entry, returning failure response");
                return responseGenerator.generateFetchDenyResponse(
                        ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                        validationResults,
                        Constants.HANDLE_REENTER_GET_PAGES_IN_SPACE,
                        map);
            } else {
                LOGGER.info("→ confirmReenterGetPagesInSpace: User accepted re-entry, generating fetch response");
                return responseGenerator.generateFetchResponse(
                        ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                        validationResults,
                        Constants.HANDLE_REENTER_GET_PAGES_IN_SPACE,
                        map);
            }
        } catch (Exception cause) {
            LOGGER.error("→ confirmReenterGetPagesInSpace: Exception occurred - {}", cause.getMessage(), cause);
            ExtensionResponse.Error error = new ExtensionResponse.Error(Constants.FAILED_TO_GET_PAGES_IN_SPACE_VALIDATION + cause.getMessage(), 
                    System.currentTimeMillis(), ExtensionResponse.Error.ExceptionType.INPUT_ERROR, " PageArea -> getPagesInSpace ");
            return new ExtensionResponseBuilder().failure(error).build();
        }
    }

    /**
     * Handles the actual get pages in space operation after the user has re-entered the space ID.
     *
     * @param map input map containing stateId, spaceId, and title
     * @return an extension response containing the pages details or error information
     */
    @SubCatalogRequest(
            name = Constants.HANDLE_REENTER_GET_PAGES_IN_SPACE,
            description = "Handle re-entered space ID for getting pages in space",
            type = CatalogRequest.Type.QUERY_SYSTEM)
    @Field.Desc(name = Constants.PAGES, type = "[ Entity(Page) ]", required = false)
    @Field.Desc(name = Constants.PAGE_IDENTIFIERS, type = "[ Identifier ]", required = false)
    public ExtensionResponse handleReenterGetPagesInSpace(
            @Field.Desc(name = "inputMap", type = "{ stateId: Text, Space ID: Text, Title: Text }") Map<String, Object> map) {
        LOGGER.info("→ handleReenterGetPagesInSpace: Starting re-entry handling for get pages in space");
        LOGGER.debug("→ handleReenterGetPagesInSpace: Input map: {}", map);

        try {
            String spaceId = (String) map.get("Space ID");
            String title = (String) map.get("Title");
            LOGGER.info("→ handleReenterGetPagesInSpace: Space ID: {}, Title: {}", spaceId, title);

            List<Page> pages;
            if (title != null && !title.trim().isEmpty()) {
                pages = Arrays.stream(confluenceService.getPagesBySpaceId(spaceId)).map(ParserUtils::setPageEntity)
                        .filter(page -> page.title.equalsIgnoreCase(title.trim()))
                        .collect(Collectors.toList());
                LOGGER.info("→ handleReenterGetPagesInSpace: Successfully retrieved {} pages with title filter: {}", pages.size(), title);
            } else {
                pages = Arrays.stream(confluenceService.getPagesBySpaceId(spaceId)).map(ParserUtils::setPageEntity)
                        .collect(Collectors.toList());
                LOGGER.info("→ handleReenterGetPagesInSpace: Successfully retrieved {} pages", pages.size());
            }

            Map<String, Object> result = Map.of(
                    Constants.PAGES, pages,
                    Constants.PAGE_IDENTIFIERS, pages.stream().map(page -> page.id).collect(Collectors.toList())
            );

            ExtensionResponse response = new ExtensionResponseBuilder().success(result).build();
            LOGGER.info("→ handleReenterGetPagesInSpace: Successfully retrieved pages");
            return response;
        } catch (Exception cause) {
            LOGGER.error("→ handleReenterGetPagesInSpace: Exception occurred - {}", cause.getMessage(), cause);
            ExtensionResponse.Error error = new ExtensionResponse.Error(Constants.FAILED_TO_GET_PAGES_IN_SPACE + cause.getMessage(), 
                    System.currentTimeMillis(), ExtensionResponse.Error.ExceptionType.INPUT_ERROR, " PageArea -> getPagesInSpace ");
            return new ExtensionResponseBuilder().failure(error).build();
        }
    }

    /**
     * Handles confirmation of whether the user wants to re-enter details for creating page from content.
     * If re-entry is declined, returns a failure response; otherwise, prompts for re-entry.
     *
     * @param map input map containing reEnter flag, stateId, pageTitle, spaceId, and content
     * @return an extension response prompting for re-entry or indicating failure
     */
    @SubCatalogRequest(
            name = Constants.CONFIRM_REENTER_CREATE_PAGE_FROM_CONTENT,
            description = "Confirm re-entering details for creating page from content",
            type = CatalogRequest.Type.QUERY_SYSTEM)
    @Field(name = Constants.RE_ENTER, type = "Switch", required = true)
    @SuppressWarnings("unchecked")
    public ExtensionResponse confirmReenterCreatePageFromContent(@Field.Desc(name = "inputMap",
            type = "{ stateId: Text, Page Title: Text, Space ID: Text, Content: [ Entity(Content) ], re-enter: Switch }") Map<String, Object> map) {
        try {
            LOGGER.info("→ confirmReenterCreatePageFromContent: Starting confirmation for re-entering create page from content");
            Boolean reEnter = (Boolean) map.get(Constants.RE_ENTER);
            String stateId = (String) map.get(Constants.STATE_ID);
            LOGGER.debug("→ confirmReenterCreatePageFromContent: Re-enter: {}, State ID: {}", reEnter, stateId);

            Map<String, Object> storedState = internalStateManager.get(stateId);
            List<ValidationOrchestrator.ValidationResult> validationResults = getValidationResults(storedState);

            if (Boolean.FALSE.equals(reEnter)) {
                LOGGER.info("→ confirmReenterCreatePageFromContent: User declined re-entry, returning failure response");
                return responseGenerator.generateFetchDenyResponse(
                        ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                        validationResults,
                        Constants.HANDLE_REENTER_CREATE_PAGE_FROM_CONTENT,
                        map);
            } else {
                LOGGER.info("→ confirmReenterCreatePageFromContent: User accepted re-entry, generating fetch response");
                return responseGenerator.generateFetchResponse(
                        ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                        validationResults,
                        Constants.HANDLE_REENTER_CREATE_PAGE_FROM_CONTENT,
                        map);
            }
        } catch (Exception cause) {
            LOGGER.error("→ confirmReenterCreatePageFromContent: Exception occurred - {}", cause.getMessage(), cause);
            ExtensionResponse.Error error = new ExtensionResponse.Error(Constants.FAILED_TO_CREATE_PAGE_FROM_CONTENT_VALIDATION + cause.getMessage(),
                    System.currentTimeMillis(), ExtensionResponse.Error.ExceptionType.INPUT_ERROR, " PageArea -> createPageFromContent ");
            return new ExtensionResponseBuilder().failure(error).build();
        }
    }

    /**
     * Handles the actual create page from content operation after the user has re-entered the details.
     *
     * @param map input map containing stateId, pageTitle, spaceId, and content
     * @return an extension response containing the created page details or error information
     */
    @SubCatalogRequest(
            name = Constants.HANDLE_REENTER_CREATE_PAGE_FROM_CONTENT,
            description = "Handle re-entered details for creating page from content",
            type = CatalogRequest.Type.QUERY_SYSTEM)
    @Field.Desc(name = "Page", type = "Entity(Page)", required = false)
    public ExtensionResponse handleReenterCreatePageFromContent(
            @Field.Desc(name = "inputMap", type = "{ stateId: Text, Page Title: Text, Space ID: Text, Content: [ Entity(Content) ] }") Map<String, Object> map) {
        LOGGER.info("→ handleReenterCreatePageFromContent: Starting re-entry handling for create page from content");
        LOGGER.debug("→ handleReenterCreatePageFromContent: Input map: {}", map);

        try {
            String pageTitle = (String) map.get("Page Title");
            String spaceId = (String) map.get("Space ID");
            @SuppressWarnings("unchecked")
            List<Content> content = (List<Content>) map.get("Content");
            LOGGER.info("→ handleReenterCreatePageFromContent: Page Title: {}, Space ID: {}", pageTitle, spaceId);

            Page page = ParserUtils.setPageEntity(confluenceService.createPageFromContent(spaceId, pageTitle, content));
            LOGGER.info("→ handleReenterCreatePageFromContent: Successfully created page: {}", page.id);

            Map<String, Object> result = Map.of("Page", page);

            ExtensionResponse response = new ExtensionResponseBuilder().success(result).build();
            LOGGER.info("→ handleReenterCreatePageFromContent: Successfully created page from content");
            return response;
        } catch (Exception cause) {
            LOGGER.error("→ handleReenterCreatePageFromContent: Exception occurred - {}", cause.getMessage(), cause);
            ExtensionResponse.Error error = new ExtensionResponse.Error(Constants.FAILED_TO_CREATE_PAGE_FROM_CONTENT + cause.getMessage(),
                    System.currentTimeMillis(), ExtensionResponse.Error.ExceptionType.INPUT_ERROR, " PageArea -> createPageFromContent ");
            return new ExtensionResponseBuilder().failure(error).build();
        }
    }

    /**
     * Handles confirmation of whether the user wants to re-enter details for creating page from template.
     * If re-entry is declined, returns a failure response; otherwise, prompts for re-entry.
     *
     * @param map input map containing reEnter flag, stateId, spaceId, templateId, pageTitle, and variables
     * @return an extension response prompting for re-entry or indicating failure
     */
    @SubCatalogRequest(
            name = Constants.CONFIRM_REENTER_CREATE_PAGE_FROM_TEMPLATE,
            description = "Confirm re-entering details for creating page from template",
            type = CatalogRequest.Type.QUERY_SYSTEM)
    @Field(name = Constants.RE_ENTER, type = "Switch", required = true)
    @SuppressWarnings("unchecked")
    public ExtensionResponse confirmReenterCreatePageFromTemplate(@Field.Desc(name = "inputMap",
            type = "{ stateId: Text, Space ID: Text, Template ID: Text, Page Title: Text, Variables: [ { Key: Text, Value: Text } ], re-enter: Switch }") Map<String, Object> map) {
        try {
            Boolean reEnter = (Boolean) map.get(Constants.RE_ENTER);
            String stateId = (String) map.get(Constants.STATE_ID);
            LOGGER.debug("→ confirmReenterCreatePageFromTemplate: Re-enter: {}, State ID: {}", reEnter, stateId);

            Map<String, Object> storedState = internalStateManager.get(stateId);
            List<ValidationOrchestrator.ValidationResult> validationResults = getValidationResults(storedState);

            if (Boolean.FALSE.equals(reEnter)) {
                LOGGER.info("→ confirmReenterCreatePageFromTemplate: User declined re-entry, returning failure response");
                return responseGenerator.generateFetchDenyResponse(
                        ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                        validationResults,
                        Constants.HANDLE_REENTER_CREATE_PAGE_FROM_TEMPLATE,
                        map);
            } else {
                LOGGER.info("→ confirmReenterCreatePageFromTemplate: User accepted re-entry, generating fetch response");
                return responseGenerator.generateFetchResponse(
                        ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                        validationResults,
                        Constants.HANDLE_REENTER_CREATE_PAGE_FROM_TEMPLATE,
                        map);
            }
        } catch (Exception cause) {
            LOGGER.error("→ confirmReenterCreatePageFromTemplate: Exception occurred - {}", cause.getMessage(), cause);
            ExtensionResponse.Error error = new ExtensionResponse.Error(Constants.FAILED_TO_CREATE_PAGE_FROM_TEMPLATE_VALIDATION + cause.getMessage(),
                    System.currentTimeMillis(), ExtensionResponse.Error.ExceptionType.INPUT_ERROR, " PageArea -> createPageFromTemplate ");
            return new ExtensionResponseBuilder().failure(error).build();
        }
    }

    /**
     * Handles the actual create page from template operation after the user has re-entered the details.
     *
     * @param map input map containing stateId, spaceId, templateId, pageTitle, and variables
     * @return an extension response containing the created page details or error information
     */
    @SubCatalogRequest(
            name = Constants.HANDLE_REENTER_CREATE_PAGE_FROM_TEMPLATE,
            description = "Handle re-entered details for creating page from template",
            type = CatalogRequest.Type.QUERY_SYSTEM)
    @Field.Desc(name = "Page", type = "Entity(Page)", required = false)
    public ExtensionResponse handleReenterCreatePageFromTemplate(
            @Field.Desc(name = "inputMap", type = "{ stateId: Text, Space ID: Text, Template ID: Text, Page Title: Text, Variables: [ { Key: Text, Value: Text } ] }") Map<String, Object> map) {
        LOGGER.info("→ handleReenterCreatePageFromTemplate: Starting re-entry handling for create page from template");
        LOGGER.debug("→ handleReenterCreatePageFromTemplate: Input map: {}", map);

        try {
            String spaceId = (String) map.get("Space ID");
            String templateId = (String) map.get("Template ID");
            String pageTitle = (String) map.get("Page Title");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> variables = (List<Map<String, Object>>) map.get("Variables");
            LOGGER.info("→ handleReenterCreatePageFromTemplate: Space ID: {}, Template ID: {}, Page Title: {}", spaceId, templateId, pageTitle);

            Page page = ParserUtils.setPageEntity(confluenceService.createPageFromTemplate(spaceId, templateId, pageTitle, variables));
            LOGGER.info("→ handleReenterCreatePageFromTemplate: Successfully created page: {}", page.id);

            Map<String, Object> result = Map.of("Page", page);

            ExtensionResponse response = new ExtensionResponseBuilder().success(result).build();
            LOGGER.info("→ handleReenterCreatePageFromTemplate: Successfully created page from template");
            return response;
        } catch (Exception cause) {
            LOGGER.error("→ handleReenterCreatePageFromTemplate: Exception occurred - {}", cause.getMessage(), cause);
            ExtensionResponse.Error error = new ExtensionResponse.Error(Constants.FAILED_TO_CREATE_PAGE_FROM_TEMPLATE + cause.getMessage(),
                    System.currentTimeMillis(), ExtensionResponse.Error.ExceptionType.INPUT_ERROR, " PageArea -> createPageFromTemplate ");
            return new ExtensionResponseBuilder().failure(error).build();
        }
    }

    /**
     * Handles confirmation of whether the user wants to re-enter details for getting page by ID.
     * If re-entry is declined, returns a failure response; otherwise, prompts for re-entry.
     *
     * @param map input map containing reEnter flag, stateId, and pageId
     * @return an extension response prompting for re-entry or indicating failure
     */
    @SubCatalogRequest(
            name = Constants.CONFIRM_REENTER_GET_PAGE_BY_ID,
            description = "Confirm re-entering page ID for getting page by ID",
            type = CatalogRequest.Type.QUERY_SYSTEM)
    @Field(name = Constants.RE_ENTER, type = "Switch", required = true)
    @SuppressWarnings("unchecked")
    public ExtensionResponse confirmReenterGetPageById(@Field.Desc(name = "inputMap",
            type = "{ stateId: Text, Page ID: Text, re-enter: Switch }") Map<String, Object> map) {
        try {
            Boolean reEnter = (Boolean) map.get(Constants.RE_ENTER);
            String stateId = (String) map.get(Constants.STATE_ID);
            LOGGER.debug("→ confirmReenterGetPageById: Re-enter: {}, State ID: {}", reEnter, stateId);

            Map<String, Object> storedState = internalStateManager.get(stateId);
            List<ValidationOrchestrator.ValidationResult> validationResults = getValidationResults(storedState);

            if (Boolean.FALSE.equals(reEnter)) {
                LOGGER.info("→ confirmReenterGetPageById: User declined re-entry, returning failure response");
                return responseGenerator.generateFetchDenyResponse(
                        ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                        validationResults,
                        Constants.HANDLE_REENTER_GET_PAGE_BY_ID,
                        map);
            } else {
                LOGGER.info("→ confirmReenterGetPageById: User accepted re-entry, generating fetch response");
                return responseGenerator.generateFetchResponse(
                        ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                        validationResults,
                        Constants.HANDLE_REENTER_GET_PAGE_BY_ID,
                        map);
            }
        } catch (Exception cause) {
            LOGGER.error("→ confirmReenterGetPageById: Exception occurred - {}", cause.getMessage(), cause);
            ExtensionResponse.Error error = new ExtensionResponse.Error(Constants.FAILED_TO_GET_PAGE_BY_ID_VALIDATION + cause.getMessage(),
                    System.currentTimeMillis(), ExtensionResponse.Error.ExceptionType.INPUT_ERROR, " PageArea -> getPageByID ");
            return new ExtensionResponseBuilder().failure(error).build();
        }
    }

    /**
     * Handles the actual get page by ID operation after the user has re-entered the page ID.
     *
     * @param map input map containing stateId and pageId
     * @return an extension response containing the page details or error information
     */
    @SubCatalogRequest(
            name = Constants.HANDLE_REENTER_GET_PAGE_BY_ID,
            description = "Handle re-entered page ID for getting page by ID",
            type = CatalogRequest.Type.QUERY_SYSTEM)
    @Field.Desc(name = "Page", type = "Entity(Page)", required = false)
    public ExtensionResponse handleReenterGetPageById(
            @Field.Desc(name = "inputMap", type = "{ stateId: Text, Page ID: Text }") Map<String, Object> map) {
        LOGGER.info("→ handleReenterGetPageById: Starting re-entry handling for get page by ID");
        LOGGER.debug("→ handleReenterGetPageById: Input map: {}", map);

        try {
            String pageId = (String) map.get("Page ID");
            LOGGER.info("→ handleReenterGetPageById: Page ID: {}", pageId);

            Page page = ParserUtils.setPageEntity(confluenceService.getPageById(pageId));
            LOGGER.info("→ handleReenterGetPageById: Successfully retrieved page: {}", page.id);

            Map<String, Object> result = Map.of("Page", page);

            ExtensionResponse response = new ExtensionResponseBuilder().success(result).build();
            LOGGER.info("→ handleReenterGetPageById: Successfully retrieved page");
            return response;
        } catch (Exception cause) {
            LOGGER.error("→ handleReenterGetPageById: Exception occurred - {}", cause.getMessage(), cause);
            ExtensionResponse.Error error = new ExtensionResponse.Error(Constants.FAILED_TO_GET_PAGE_BY_ID + cause.getMessage(),
                    System.currentTimeMillis(), ExtensionResponse.Error.ExceptionType.INPUT_ERROR, " PageArea -> getPageByID ");
            return new ExtensionResponseBuilder().failure(error).build();
        }
    }

    /**
     * Handles confirmation of whether the user wants to re-enter details for updating page by content.
     * If re-entry is declined, returns a failure response; otherwise, prompts for re-entry.
     *
     * @param map input map containing reEnter flag, stateId, pageId, title, status, comments, and content
     * @return an extension response prompting for re-entry or indicating failure
     */
    @SubCatalogRequest(
            name = Constants.CONFIRM_REENTER_UPDATE_PAGE_BY_CONTENT,
            description = "Confirm re-entering details for updating page by content",
            type = CatalogRequest.Type.QUERY_SYSTEM)
    @Field(name = Constants.RE_ENTER, type = "Switch", required = true)
    @SuppressWarnings("unchecked")
    public ExtensionResponse confirmReenterUpdatePageByContent(@Field.Desc(name = "inputMap",
            type = "{ stateId: Text, Page ID: Text, Title: Text, Status: Text, Comments: Text, Content: [ Entity(Content) ], re-enter: Switch }") Map<String, Object> map) {
        try {
            Boolean reEnter = (Boolean) map.get(Constants.RE_ENTER);
            String stateId = (String) map.get(Constants.STATE_ID);
            LOGGER.debug("→ confirmReenterUpdatePageByContent: Re-enter: {}, State ID: {}", reEnter, stateId);

            Map<String, Object> storedState = internalStateManager.get(stateId);
            List<ValidationOrchestrator.ValidationResult> validationResults = getValidationResults(storedState);

            if (Boolean.FALSE.equals(reEnter)) {
                LOGGER.info("→ confirmReenterUpdatePageByContent: User declined re-entry, returning failure response");
                return responseGenerator.generateFetchDenyResponse(
                        ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                        validationResults,
                        Constants.HANDLE_REENTER_UPDATE_PAGE_BY_CONTENT,
                        map);
            } else {
                LOGGER.info("→ confirmReenterUpdatePageByContent: User accepted re-entry, generating fetch response");
                return responseGenerator.generateFetchResponse(
                        ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                        validationResults,
                        Constants.HANDLE_REENTER_UPDATE_PAGE_BY_CONTENT,
                        map);
            }
        } catch (Exception cause) {
            LOGGER.error("→ confirmReenterUpdatePageByContent: Exception occurred - {}", cause.getMessage(), cause);
            ExtensionResponse.Error error = new ExtensionResponse.Error(Constants.FAILED_TO_UPDATE_PAGE_BY_CONTENT_VALIDATION + cause.getMessage(),
                    System.currentTimeMillis(), ExtensionResponse.Error.ExceptionType.INPUT_ERROR, " PageArea -> updatePageByContent ");
            return new ExtensionResponseBuilder().failure(error).build();
        }
    }

    /**
     * Handles the actual update page by content operation after the user has re-entered the details.
     *
     * @param map input map containing stateId, pageId, title, status, comments, and content
     * @return an extension response containing the updated page details or error information
     */
    @SubCatalogRequest(
            name = Constants.HANDLE_REENTER_UPDATE_PAGE_BY_CONTENT,
            description = "Handle re-entered details for updating page by content",
            type = CatalogRequest.Type.QUERY_SYSTEM)
    @Field.Desc(name = "Page", type = "Entity(Page)", required = false)
    public ExtensionResponse handleReenterUpdatePageByContent(
            @Field.Desc(name = "inputMap", type = "{ stateId: Text, Page ID: Text, Title: Text, Status: Text, Comments: Text, Content: [ Entity(Content) ] }") Map<String, Object> map) {
        LOGGER.info("→ handleReenterUpdatePageByContent: Starting re-entry handling for update page by content");
        LOGGER.debug("→ handleReenterUpdatePageByContent: Input map: {}", map);

        try {
            String pageId = (String) map.get("Page ID");
            String title = (String) map.get("Title");
            String status = (String) map.get("Status");
            String comments = (String) map.get("Comments");
            @SuppressWarnings("unchecked")
            List<Content> content = (List<Content>) map.get("Content");
            LOGGER.info("→ handleReenterUpdatePageByContent: Page ID: {}, Title: {}, Status: {}", pageId, title, status);

            Page page = ParserUtils.setPageEntity(confluenceService.updatePageFromContent(pageId, status, title, comments, content));
            LOGGER.info("→ handleReenterUpdatePageByContent: Successfully updated page: {}", page.id);

            Map<String, Object> result = Map.of("Page", page);

            ExtensionResponse response = new ExtensionResponseBuilder().success(result).build();
            LOGGER.info("→ handleReenterUpdatePageByContent: Successfully updated page by content");
            return response;
        } catch (Exception cause) {
            LOGGER.error("→ handleReenterUpdatePageByContent: Exception occurred - {}", cause.getMessage(), cause);
            ExtensionResponse.Error error = new ExtensionResponse.Error(Constants.FAILED_TO_UPDATE_PAGE_BY_CONTENT + cause.getMessage(),
                    System.currentTimeMillis(), ExtensionResponse.Error.ExceptionType.INPUT_ERROR, " PageArea -> updatePageByContent ");
            return new ExtensionResponseBuilder().failure(error).build();
        }
    }

    /**
     * Handles confirmation of whether the user wants to re-enter details for updating page from template.
     * If re-entry is declined, returns a failure response; otherwise, prompts for re-entry.
     *
     * @param map input map containing reEnter flag, stateId, pageId, templateId, status, title, comment, and variables
     * @return an extension response prompting for re-entry or indicating failure
     */
    @SubCatalogRequest(
            name = Constants.CONFIRM_REENTER_UPDATE_PAGE_FROM_TEMPLATE,
            description = "Confirm re-entering details for updating page from template",
            type = CatalogRequest.Type.QUERY_SYSTEM)
    @Field(name = Constants.RE_ENTER, type = "Switch", required = true)
    @SuppressWarnings("unchecked")
    public ExtensionResponse confirmReenterUpdatePageFromTemplate(@Field.Desc(name = "inputMap",
            type = "{ stateId: Text, Page ID: Text, Template Id: Text, Status: Text, Title: Text, Comment: Text, Variables: [ { Key: Text, Value: Text } ], re-enter: Switch }") Map<String, Object> map) {
        try {
            Boolean reEnter = (Boolean) map.get(Constants.RE_ENTER);
            String stateId = (String) map.get(Constants.STATE_ID);
            LOGGER.debug("→ confirmReenterUpdatePageFromTemplate: Re-enter: {}, State ID: {}", reEnter, stateId);

            Map<String, Object> storedState = internalStateManager.get(stateId);
            List<ValidationOrchestrator.ValidationResult> validationResults = getValidationResults(storedState);

            if (Boolean.FALSE.equals(reEnter)) {
                LOGGER.info("→ confirmReenterUpdatePageFromTemplate: User declined re-entry, returning failure response");
                return responseGenerator.generateFetchDenyResponse(
                        ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                        validationResults,
                        Constants.HANDLE_REENTER_UPDATE_PAGE_FROM_TEMPLATE,
                        map);
            } else {
                LOGGER.info("→ confirmReenterUpdatePageFromTemplate: User accepted re-entry, generating fetch response");
                return responseGenerator.generateFetchResponse(
                        ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                        validationResults,
                        Constants.HANDLE_REENTER_UPDATE_PAGE_FROM_TEMPLATE,
                        map);
            }
        } catch (Exception cause) {
            LOGGER.error("→ confirmReenterUpdatePageFromTemplate: Exception occurred - {}", cause.getMessage(), cause);
            ExtensionResponse.Error error = new ExtensionResponse.Error(Constants.FAILED_TO_UPDATE_PAGE_FROM_TEMPLATE_VALIDATION + cause.getMessage(),
                    System.currentTimeMillis(), ExtensionResponse.Error.ExceptionType.INPUT_ERROR, " PageArea -> updatePageFromTemplate ");
            return new ExtensionResponseBuilder().failure(error).build();
        }
    }

    /**
     * Handles the actual update page from template operation after the user has re-entered the details.
     *
     * @param map input map containing stateId, pageId, templateId, status, title, comment, and variables
     * @return an extension response containing the updated page details or error information
     */
    @SubCatalogRequest(
            name = Constants.HANDLE_REENTER_UPDATE_PAGE_FROM_TEMPLATE,
            description = "Handle re-entered details for updating page from template",
            type = CatalogRequest.Type.QUERY_SYSTEM)
    @Field.Desc(name = "Page", type = "Entity(Page)", required = false)
    public ExtensionResponse handleReenterUpdatePageFromTemplate(
            @Field.Desc(name = "inputMap", type = "{ stateId: Text, Page ID: Text, Template Id: Text, Status: Text, Title: Text, Comment: Text, Variables: [ { Key: Text, Value: Text } ] }") Map<String, Object> map) {
        LOGGER.info("→ handleReenterUpdatePageFromTemplate: Starting re-entry handling for update page from template");
        LOGGER.debug("→ handleReenterUpdatePageFromTemplate: Input map: {}", map);

        try {
            String pageId = (String) map.get("Page ID");
            String templateId = (String) map.get("Template Id");
            String status = (String) map.get("Status");
            String title = (String) map.get("Title");
            String comment = (String) map.get("Comment");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> variables = (List<Map<String, Object>>) map.get("Variables");
            LOGGER.info("→ handleReenterUpdatePageFromTemplate: Page ID: {}, Template Id: {}, Status: {}, Title: {}", pageId, templateId, status, title);

            Page page = ParserUtils.setPageEntity(confluenceService.updatePageFromTemplate(pageId, templateId, status, title, comment, variables));
            LOGGER.info("→ handleReenterUpdatePageFromTemplate: Successfully updated page: {}", page.id);

            Map<String, Object> result = Map.of("Page", page);

            ExtensionResponse response = new ExtensionResponseBuilder().success(result).build();
            LOGGER.info("→ handleReenterUpdatePageFromTemplate: Successfully updated page from template");
            return response;
        } catch (Exception cause) {
            LOGGER.error("→ handleReenterUpdatePageFromTemplate: Exception occurred - {}", cause.getMessage(), cause);
            ExtensionResponse.Error error = new ExtensionResponse.Error(Constants.FAILED_TO_UPDATE_PAGE_FROM_TEMPLATE + cause.getMessage(),
                    System.currentTimeMillis(), ExtensionResponse.Error.ExceptionType.INPUT_ERROR, " PageArea -> updatePageFromTemplate ");
            return new ExtensionResponseBuilder().failure(error).build();
        }
    }

    /**
     * Handles confirmation of whether the user wants to re-enter details for getting page in space.
     * If re-entry is declined, returns a failure response; otherwise, prompts for re-entry.
     *
     * @param map input map containing reEnter flag, stateId, pageId, and exportFormat
     * @return an extension response prompting for re-entry or indicating failure
     */
    @SubCatalogRequest(
            name = Constants.CONFIRM_REENTER_GET_PAGE_IN_SPACE,
            description = "Confirm re-entering page ID for getting page in space",
            type = CatalogRequest.Type.QUERY_SYSTEM)
    @Field(name = Constants.RE_ENTER, type = "Switch", required = true)
    @SuppressWarnings("unchecked")
    public ExtensionResponse confirmReenterGetPageInSpace(@Field.Desc(name = "inputMap",
            type = "{ stateId: Text, Page ID: Text, Export Format: Text, re-enter: Switch }") Map<String, Object> map) {
        try {
            Boolean reEnter = (Boolean) map.get(Constants.RE_ENTER);
            String stateId = (String) map.get(Constants.STATE_ID);
            LOGGER.debug("→ confirmReenterGetPageInSpace: Re-enter: {}, State ID: {}", reEnter, stateId);

            Map<String, Object> storedState = internalStateManager.get(stateId);
            List<ValidationOrchestrator.ValidationResult> validationResults = getValidationResults(storedState);

            if (Boolean.FALSE.equals(reEnter)) {
                LOGGER.info("→ confirmReenterGetPageInSpace: User declined re-entry, returning failure response");
                return responseGenerator.generateFetchDenyResponse(
                        ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                        validationResults,
                        Constants.HANDLE_REENTER_GET_PAGE_IN_SPACE,
                        map);
            } else {
                LOGGER.info("→ confirmReenterGetPageInSpace: User accepted re-entry, generating fetch response");
                return responseGenerator.generateFetchResponse(
                        ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                        validationResults,
                        Constants.HANDLE_REENTER_GET_PAGE_IN_SPACE,
                        map);
            }
        } catch (Exception cause) {
            LOGGER.error("→ confirmReenterGetPageInSpace: Exception occurred - {}", cause.getMessage(), cause);
            ExtensionResponse.Error error = new ExtensionResponse.Error(Constants.FAILED_TO_GET_PAGE_IN_SPACE_VALIDATION + cause.getMessage(),
                    System.currentTimeMillis(), ExtensionResponse.Error.ExceptionType.INPUT_ERROR, " PageArea -> getPageInSpace ");
            return new ExtensionResponseBuilder().failure(error).build();
        }
    }

    /**
     * Handles the actual get page in space operation after the user has re-entered the page ID.
     *
     * @param map input map containing stateId, pageId, and exportFormat
     * @return an extension response containing the downloaded file and parsing success status or error information
     */
    @SubCatalogRequest(
            name = Constants.HANDLE_REENTER_GET_PAGE_IN_SPACE,
            description = "Handle re-entered page ID for getting page in space",
            type = CatalogRequest.Type.QUERY_SYSTEM)
    @Field.File(name = "Downloaded File", multipleFileUpload = false, required = false)
    @Field.Boolean(name = "Parsing Success", required = false)
    public ExtensionResponse handleReenterGetPageInSpace(
            @Field.Desc(name = "inputMap", type = "{ stateId: Text, Page ID: Text, Export Format: Text }") Map<String, Object> map) {
        LOGGER.info("→ handleReenterGetPageInSpace: Starting re-entry handling for get page in space");
        LOGGER.debug("→ handleReenterGetPageInSpace: Input map: {}", map);

        try {
            String pageId = (String) map.get("Page ID");
            String exportFormat = (String) map.get("Export Format");
            LOGGER.info("→ handleReenterGetPageInSpace: Page ID: {}, Export Format: {}", pageId, exportFormat);

            Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);
            File downloadedFile = result.getKey();
            Boolean parsingSuccess = result.getValue();
            LOGGER.debug("→ handleReenterGetPageInSpace: Page downloaded via service, parsing success: {}", parsingSuccess);
            LOGGER.info("→ handleReenterGetPageInSpace: Successfully downloaded page {} as {}, parsing success: {}", pageId, exportFormat, parsingSuccess);

            Map<String, Object> resultMap = Map.of(
                    "Downloaded File", downloadedFile,
                    "Parsing Success", parsingSuccess
            );

            ExtensionResponse response = new ExtensionResponseBuilder().success(resultMap).build();
            LOGGER.info("→ handleReenterGetPageInSpace: Successfully downloaded page");
            return response;
        } catch (Exception cause) {
            LOGGER.error("→ handleReenterGetPageInSpace: Exception occurred - {}", cause.getMessage(), cause);
            ExtensionResponse.Error error = new ExtensionResponse.Error(Constants.FAILED_TO_GET_PAGE_IN_SPACE + cause.getMessage(),
                    System.currentTimeMillis(), ExtensionResponse.Error.ExceptionType.INPUT_ERROR, " PageArea -> getPageInSpace ");
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

    /**
     * Handles confirmation of whether the user wants to re-enter details for getting direct child pages of page.
     * If re-entry is declined, returns a failure response; otherwise, prompts for re-entry.
     *
     * @param map input map containing reEnter flag, stateId, and pageId
     * @return an extension response prompting for re-entry or indicating failure
     */
    @SubCatalogRequest(
            name = Constants.CONFIRM_REENTER_GET_DIRECT_CHILD_PAGES_OF_PAGE,
            description = "Confirm re-entering page ID for getting direct child pages of page",
            type = CatalogRequest.Type.QUERY_SYSTEM)
    @Field(name = Constants.RE_ENTER, type = "Switch", required = true)
    @SuppressWarnings("unchecked")
    public ExtensionResponse confirmReenterGetDirectChildPagesOfPage(@Field.Desc(name = "inputMap",
            type = "{ re-enter: Boolean, stateId: Text, Page ID: Text }", required = true) Map<String, Object> map) {
        LOGGER.info("→ confirmReenterGetDirectChildPagesOfPage: Starting confirmation for re-entering page ID");
        LOGGER.debug("→ confirmReenterGetDirectChildPagesOfPage: Input map: {}", map);

        Boolean reEnter = (Boolean) map.get(Constants.RE_ENTER);
        String stateId = (String) map.get(Constants.STATE_ID);
        LOGGER.debug("→ confirmReenterGetDirectChildPagesOfPage: Extracted values - reEnter: {}, stateId: {}", reEnter, stateId);

        Map<String, Object> state = internalStateManager.get(stateId);
        LOGGER.debug("→ confirmReenterGetDirectChildPagesOfPage: Retrieved state: {}", state);

        if (Boolean.FALSE.equals(reEnter)) {
            LOGGER.debug("→ confirmReenterGetDirectChildPagesOfPage: User declined to reenter, generating deny response");
            List<ValidationResult> validationResults = (List<ValidationResult>) state.get(Constants.VALIDATION_RESULTS);
            ExtensionResponse response = responseGenerator.generateFetchDenyResponse(ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    validationResults, null, Map.of());
            LOGGER.info("→ confirmReenterGetDirectChildPagesOfPage: Deny response generated");
            return response;
        }

        LOGGER.debug("→ confirmReenterGetDirectChildPagesOfPage: User confirmed reenter, generating fetch response");
        List<ValidationResult> validationResults = (List<ValidationResult>) state.get(Constants.VALIDATION_RESULTS);
        ExtensionResponse response = responseGenerator.generateFetchResponse(ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                validationResults, Constants.HANDLE_REENTER_GET_DIRECT_CHILD_PAGES_OF_PAGE, state);
        LOGGER.info("→ confirmReenterGetDirectChildPagesOfPage: Fetch response generated successfully");
        return response;
    }

    /**
     * Handles the actual get direct child pages of page operation after the user has re-entered the page ID.
     *
     * @param map input map containing stateId and pageId
     * @return an extension response containing the direct child pages details or error information
     */
    @SubCatalogRequest(
            name = Constants.HANDLE_REENTER_GET_DIRECT_CHILD_PAGES_OF_PAGE,
            description = "Handle re-entered page ID for getting direct child pages of page",
            type = CatalogRequest.Type.QUERY_SYSTEM)
    @Field.Desc(name = Constants.PAGES, type = "[ Entity(Page) ]", required = false)
    @Field.Desc(name = Constants.PAGE_IDENTIFIERS, type = "[ Identifier ]", required = false)
    public ExtensionResponse handleReenterGetDirectChildPagesOfPage(
            @Field.Desc(name = "inputMap", type = "{ stateId: Text, Page ID: Text }") Map<String, Object> map) {
        LOGGER.info("→ handleReenterGetDirectChildPagesOfPage: Starting re-entry handling for get direct child pages of page");
        LOGGER.debug("→ handleReenterGetDirectChildPagesOfPage: Input map: {}", map);

        try {
            String stateId = (String) map.get(Constants.STATE_ID);
            String pageId = (String) map.get(Constants.PAGE_ID);
            LOGGER.debug("→ handleReenterGetDirectChildPagesOfPage: Extracted values - stateId: {}, pageId: {}", stateId, pageId);

            // Get direct child pages of page
            List<Page> pages = Arrays.stream(confluenceService.getDirectChildPagesOfPage(pageId))
                    .map(ParserUtils::setPageEntity)
                    .collect(Collectors.toList());


            LOGGER.debug("→ handleReenterGetDirectChildPagesOfPage: Successfully retrieved {} direct child pages", pages.size());

            Map<String, Object> result = Map.of(
                    Constants.PAGES, pages,
                    Constants.PAGE_IDENTIFIERS, pages.stream().map(page -> page.id).collect(Collectors.toList())
            );

            ExtensionResponse response = new ExtensionResponseBuilder().success(result).build();
            LOGGER.info("→ handleReenterGetDirectChildPagesOfPage: Successfully retrieved direct child pages of page");
            return response;
        } catch (Exception cause) {
            LOGGER.error("→ handleReenterGetDirectChildPagesOfPage: Exception occurred - {}", cause.getMessage(), cause);
            ExtensionResponse.Error error = new ExtensionResponse.Error(Constants.FAILED_TO_GET_PAGES_IN_SPACE + cause.getMessage(),
                    System.currentTimeMillis(), ExtensionResponse.Error.ExceptionType.INPUT_ERROR, " PageArea -> getDirectChildPagesOfPage ");
            return new ExtensionResponseBuilder().failure(error).build();
        }
    }

    /**
     * Handles confirmation of whether the user wants to re-enter details for checking if page has children.
     * If re-entry is declined, returns a failure response; otherwise, prompts for re-entry.
     *
     * @param map input map containing reEnter flag, stateId, and pageId
     * @return an extension response prompting for re-entry or indicating failure
     */
    @SubCatalogRequest(
            name = Constants.CONFIRM_REENTER_CHECK_PAGE_HAS_CHILDREN,
            description = "Confirm re-entering page ID for checking if page has children",
            type = CatalogRequest.Type.QUERY_SYSTEM)
    @Field(name = Constants.RE_ENTER, type = "Switch", required = true)
    @SuppressWarnings("unchecked")
    public ExtensionResponse confirmReenterCheckPageHasChildren(@Field.Desc(name = "inputMap",
            type = "{ re-enter: Boolean, stateId: Text, Page ID: Text }", required = true) Map<String, Object> map) {
        LOGGER.info("→ confirmReenterCheckPageHasChildren: Starting confirmation for re-entering page ID");
        LOGGER.debug("→ confirmReenterCheckPageHasChildren: Input map: {}", map);

        Boolean reEnter = (Boolean) map.get(Constants.RE_ENTER);
        String stateId = (String) map.get(Constants.STATE_ID);
        LOGGER.debug("→ confirmReenterCheckPageHasChildren: Extracted values - reEnter: {}, stateId: {}", reEnter, stateId);

        Map<String, Object> state = internalStateManager.get(stateId);
        LOGGER.debug("→ confirmReenterCheckPageHasChildren: Retrieved state: {}", state);

        if (Boolean.FALSE.equals(reEnter)) {
            LOGGER.debug("→ confirmReenterCheckPageHasChildren: User declined to reenter, generating deny response");
            List<ValidationResult> validationResults = (List<ValidationResult>) state.get(Constants.VALIDATION_RESULTS);
            ExtensionResponse response = responseGenerator.generateFetchDenyResponse(ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    validationResults, null, Map.of());
            LOGGER.info("→ confirmReenterCheckPageHasChildren: Deny response generated");
            return response;
        }

        LOGGER.debug("→ confirmReenterCheckPageHasChildren: User confirmed reenter, generating fetch response");
        List<ValidationResult> validationResults = (List<ValidationResult>) state.get(Constants.VALIDATION_RESULTS);
        ExtensionResponse response = responseGenerator.generateFetchResponse(ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                validationResults, Constants.HANDLE_REENTER_CHECK_PAGE_HAS_CHILDREN, state);
        LOGGER.info("→ confirmReenterCheckPageHasChildren: Fetch response generated successfully");
        return response;
    }

    /**
     * Handles the actual check page has children operation after the user has re-entered the page ID.
     *
     * @param map input map containing stateId and pageId
     * @return an extension response containing the has children result or error information
     */
    @SubCatalogRequest(
            name = Constants.HANDLE_REENTER_CHECK_PAGE_HAS_CHILDREN,
            description = "Handle re-entered page ID for checking if page has children",
            type = CatalogRequest.Type.QUERY_SYSTEM)
    @Field.Boolean(name = "Has Children", required = false)
    public ExtensionResponse handleReenterCheckPageHasChildren(
            @Field.Desc(name = "inputMap", type = "{ stateId: Text, Page ID: Text }") Map<String, Object> map) {
        LOGGER.info("→ handleReenterCheckPageHasChildren: Starting re-entry handling for check page has children");
        LOGGER.debug("→ handleReenterCheckPageHasChildren: Input map: {}", map);

        try {
            String stateId = (String) map.get(Constants.STATE_ID);
            String pageId = (String) map.get(Constants.PAGE_ID);
            LOGGER.debug("→ handleReenterCheckPageHasChildren: Extracted values - stateId: {}, pageId: {}", stateId, pageId);

            // Check if page has children
            boolean hasChildren = confluenceService.hasChildPages(pageId);
            LOGGER.debug("→ handleReenterCheckPageHasChildren: Page {} has children: {}", pageId, hasChildren);

            Map<String, Object> result = Map.of("Has Children", hasChildren);

            ExtensionResponse response = new ExtensionResponseBuilder().success(result).build();
            LOGGER.info("→ handleReenterCheckPageHasChildren: Successfully checked if page has children");
            return response;
        } catch (Exception cause) {
            LOGGER.error("→ handleReenterCheckPageHasChildren: Exception occurred - {}", cause.getMessage(), cause);
            ExtensionResponse.Error error = new ExtensionResponse.Error(Constants.FAILED_TO_GET_PAGE_BY_ID + cause.getMessage(),
                    System.currentTimeMillis(), ExtensionResponse.Error.ExceptionType.INPUT_ERROR, " PageArea -> checkPageHasChildren ");
            return new ExtensionResponseBuilder().failure(error).build();
        }
    }

    /**
     * Handles confirmation of whether the user wants to re-enter the Page ID for downloading
     * the page content as a Markdown file. If re-entry is declined, returns a failure response.
     */
    @SubCatalogRequest(
            name = Constants.CONFIRM_REENTER_GET_PAGE_CONTENT_AS_FILE,
            description = "Confirm re-entering page ID for downloading page content as a Markdown file",
            type = CatalogRequest.Type.QUERY_SYSTEM)
    @Field(name = Constants.RE_ENTER, type = "Switch", required = true)
    @SuppressWarnings("unchecked")
    public ExtensionResponse confirmReenterGetPageContentAsFile(@Field.Desc(name = "inputMap",
            type = "{ stateId: Text, Page ID: Text, re-enter: Switch }") Map<String, Object> map) {
        try {
            Boolean reEnter = (Boolean) map.get(Constants.RE_ENTER);
            String stateId = (String) map.get(Constants.STATE_ID);
            LOGGER.debug("→ confirmReenterGetPageContentAsFile: Re-enter: {}, State ID: {}", reEnter, stateId);

            Map<String, Object> storedState = internalStateManager.get(stateId);
            List<ValidationOrchestrator.ValidationResult> validationResults = getValidationResults(storedState);

            if (Boolean.FALSE.equals(reEnter)) {
                LOGGER.info("→ confirmReenterGetPageContentAsFile: User declined re-entry, returning failure response");
                return responseGenerator.generateFetchDenyResponse(
                        ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                        validationResults,
                        Constants.HANDLE_REENTER_GET_PAGE_CONTENT_AS_FILE,
                        map);
            } else {
                LOGGER.info("→ confirmReenterGetPageContentAsFile: User accepted re-entry, generating fetch response");
                return responseGenerator.generateFetchResponse(
                        ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                        validationResults,
                        Constants.HANDLE_REENTER_GET_PAGE_CONTENT_AS_FILE,
                        map);
            }
        } catch (Exception cause) {
            LOGGER.error("→ confirmReenterGetPageContentAsFile: Exception occurred - {}", cause.getMessage(), cause);
            ExtensionResponse.Error error = new ExtensionResponse.Error(Constants.FAILED_TO_GET_PAGE_CONTENT_AS_FILE_VALIDATION + cause.getMessage(),
                    System.currentTimeMillis(), ExtensionResponse.Error.ExceptionType.INPUT_ERROR, " PageArea -> getPageContentAsFile ");
            return new ExtensionResponseBuilder().failure(error).build();
        }
    }

    /**
     * Handles the actual download of page content as a Markdown file after the user has re-entered the Page ID.
     */
    @SubCatalogRequest(
            name = Constants.HANDLE_REENTER_GET_PAGE_CONTENT_AS_FILE,
            description = "Handle re-entered page ID for downloading page content as a Markdown file",
            type = CatalogRequest.Type.QUERY_SYSTEM)
    @Field.File(name = "Downloaded File", multipleFileUpload = false, required = false)
    public ExtensionResponse handleReenterGetPageContentAsFile(
            @Field.Desc(name = "inputMap", type = "{ stateId: Text, Page ID: Text }") Map<String, Object> map) {
        LOGGER.info("→ handleReenterGetPageContentAsFile: Starting re-entry handling");
        LOGGER.debug("→ handleReenterGetPageContentAsFile: Input map: {}", map);

        try {
            String pageId = (String) map.get("Page ID");
            LOGGER.info("→ handleReenterGetPageContentAsFile: Page ID: {}", pageId);

            File downloadedFile = pageMarkdownService.downloadPageAsMarkdownFile(pageId);
            LOGGER.info("→ handleReenterGetPageContentAsFile: Successfully generated Markdown file for page {}", pageId);

            return new ExtensionResponseBuilder().success(Map.of("Downloaded File", downloadedFile)).build();
        } catch (Exception cause) {
            LOGGER.error("→ handleReenterGetPageContentAsFile: Exception occurred - {}", cause.getMessage(), cause);
            ExtensionResponse.Error error = new ExtensionResponse.Error(Constants.FAILED_TO_GET_PAGE_CONTENT_AS_FILE + cause.getMessage(),
                    System.currentTimeMillis(), ExtensionResponse.Error.ExceptionType.INPUT_ERROR, " PageArea -> getPageContentAsFile ");
            return new ExtensionResponseBuilder().failure(error).build();
        }
    }
}
