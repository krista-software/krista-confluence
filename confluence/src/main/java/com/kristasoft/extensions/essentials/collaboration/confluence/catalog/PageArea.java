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
import app.krista.ksdk.files.FileHandle;
import app.krista.ksdk.files.FileRepository;
import app.krista.model.base.File;
import com.google.gson.JsonObject;
import com.kristasoft.extensions.essentials.collaboration.confluence.api.APIVersion;
import com.kristasoft.extensions.essentials.collaboration.confluence.api.ConfluenceAuditService;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.entities.Attachment;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.entities.Content;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.entities.Page;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.errorhandlers.ErrorHandlingStateManager;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.errorhandlers.ExtensionResponseGenerator;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.stores.Pair;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.validators.ValidationOrchestrator;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.validators.Validator;
import com.kristasoft.extensions.essentials.collaboration.confluence.service.ConfluenceService;
import com.kristasoft.extensions.essentials.collaboration.confluence.service.PageDocxService;
import com.kristasoft.extensions.essentials.collaboration.confluence.service.PageMarkdownService;
import com.kristasoft.extensions.essentials.collaboration.confluence.utils.ConfluenceConstants;
import com.kristasoft.extensions.essentials.collaboration.confluence.utils.Constants;
import com.kristasoft.extensions.essentials.collaboration.confluence.utils.GsonFactory;
import com.kristasoft.extensions.essentials.collaboration.confluence.utils.ParserUtils;
import com.kristasoft.extensions.essentials.collaboration.confluence.utils.TelemetryHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URLDecoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.kristasoft.extensions.essentials.collaboration.confluence.utils.ParserUtils.setPageEntity;
import static com.kristasoft.extensions.essentials.collaboration.confluence.utils.TelemetryHelper.safeTagMap;

/**
 * Catalog area for Confluence page operations (create, read, update, delete, attachments).
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
public class PageArea {

    private static final Logger LOGGER = LoggerFactory.getLogger(PageArea.class);
    private final ConfluenceService service;
    private final ValidationOrchestrator validationOrchestrator;
    private final ErrorHandlingStateManager errorHandlingStateManager;
    private final ExtensionResponseGenerator responseGenerator;
    private final PageDocxService pageDocxService;
    private final PageMarkdownService pageMarkdownService;
    private final TelemetryHelper telemetryHelper;
    private final PageAreaAttachmentHelper attachmentHelper;
    private final FileRepository fileRepository;
    private final ConfluenceAuditService confluenceAuditService;
    private final app.krista.ksdk.context.RequestContext requestContext;


    @Inject
    public PageArea(ConfluenceService service,
                    ValidationOrchestrator validationOrchestrator,
                    ErrorHandlingStateManager errorHandlingStateManager,
                    ExtensionResponseGenerator responseGenerator,
                    PageDocxService pageDocxService,
                    PageMarkdownService pageMarkdownService,
                    TelemetryHelper telemetryHelper,
                    PageAreaAttachmentHelper attachmentHelper,
                    FileRepository fileRepository,
                    ConfluenceAuditService confluenceAuditService,
                    app.krista.ksdk.context.RequestContext requestContext) {
        this.service = service;
        this.validationOrchestrator = validationOrchestrator;
        this.errorHandlingStateManager = errorHandlingStateManager;
        this.responseGenerator = responseGenerator;
        this.pageDocxService = pageDocxService;
        this.pageMarkdownService = pageMarkdownService;
        this.telemetryHelper = telemetryHelper;
        this.attachmentHelper = attachmentHelper;
        this.fileRepository = fileRepository;
        this.confluenceAuditService = confluenceAuditService;
        this.requestContext = requestContext;
    }


    @CatalogRequest(
            id = "localDomainRequest_1fde626a-3392-48d1-b0eb-66233ae14cb7",
            name = "Create Page from Content",
            description = "Create a new page given the content in order.",
            area = "Page",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.Desc(name = "Page", type = "Entity(Page)", required = false)
    public ExtensionResponse createPageFromContent(
            @Field.Text(name = "Page Title", attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "toolTip", value = "'Enter a descriptive title for the new page'")}) String pageTitle,
            @Field.Text(name = "Space ID", attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "toolTip", value = "'Enter the unique identifier of the Confluence space where the page will be created'")}) String spaceID,
            @Field.Desc(name = "Content", type = "[ Entity(Content) ]") List<Content> content,
            @Field.Boolean(name = Constants.ENABLE_ERROR_HANDLING, required = false,
                    attributes = {@Attribute(name = "toolTip", value = "'Enable to allow re-entry of values if validation fails'")}) Boolean allowRetry) {

        String invokerId = requestContext.getInvokerId();
        confluenceAuditService.triggered(invokerId, "Create Page from Content");
        boolean auditRecorded = false;

        long startTime = System.currentTimeMillis();
        String methodName = "createPageFromContent";
        String metricName = "confluence.createPageFromContent";
        LOGGER.info("→ {}: Starting operation with pageTitle: {}, spaceID: {}, allowRetry: {}", methodName, pageTitle, spaceID, allowRetry);

        try {
            telemetryHelper.incrementCount(metricName);
            Map<Validator.ValidationResource, String> validationParams = new HashMap<>();
            addValidationParamIfNotEmpty(validationParams, Validator.ValidationResource.SPACE_ID, spaceID, methodName);

            List<ValidationOrchestrator.ValidationResult> validationResults =
                    validationOrchestrator.validate(validationParams, PageArea.class, methodName);

            if (validationResults.isEmpty()) {
                return executeCreatePageFromContent(spaceID, pageTitle, content, metricName, startTime);
            } else {
                Map<String, Object> confirmationParams = Map.of(
                        "Page Title", pageTitle != null ? pageTitle : "",
                        "Space ID", spaceID != null ? spaceID : "",
                        "Content", content != null ? content : List.of());
                Map<String, String> telemetryTags = safeTagMap("space_id", spaceID, "page_title", pageTitle, "validation_count", String.valueOf(validationResults.size()));
                return handleValidationFailure(validationResults, allowRetry, metricName, startTime,
                        Constants.CONFIRM_REENTER_CREATE_PAGE_FROM_CONTENT, confirmationParams, telemetryTags, methodName);
            }
        } catch (MustAuthorizeException cause) {
            auditRecorded = true;
            confluenceAuditService.authFailure(invokerId, "Create Page from Content", cause);
            throw cause;
        } catch (Exception cause) {
            auditRecorded = true;
            confluenceAuditService.failure(invokerId, "Create Page from Content", cause);
            String errorMessage = String.format("""
                    We couldn't create the page in your Confluence space.
                    This could mean:
                    • The space ID '%s' doesn't exist or you don't have access to it
                    • The page title '%s' might conflict with an existing page
                    • Your Confluence connection might be experiencing issues

                    What to do next:
                    1. Verify the space ID is correct using 'Get Spaces'
                    2. Check if a page with this title already exists in the space
                    3. Ensure you have permission to create pages in this space

                    Technical details: %s
                    """, spaceID, pageTitle, cause.getMessage());
            return buildSystemErrorResponse(cause, errorMessage, metricName, startTime,
                    safeTagMap("space_id", spaceID, "page_title", pageTitle), methodName);
        } finally {
            if (!auditRecorded) {
                confluenceAuditService.success(invokerId, "Create Page from Content");
            }
        }
    }

    private ExtensionResponse executeCreatePageFromContent(String spaceID, String pageTitle, List<Content> content,
                                                           String metricName, long startTime) {
        LOGGER.debug("→ createPageFromContent: Validation passed, creating page from content");
        Page page = setPageEntity(service.createPageFromContent(spaceID, pageTitle, content));
        LOGGER.info("→ createPageFromContent: Successfully created page: {}", page.id);
        telemetryHelper.recordSuccess(metricName, startTime,
                safeTagMap("space_id", spaceID, "page_title", pageTitle, "page_id", page.id));
        return new ExtensionResponseBuilder().success(Map.of("Page", page)).build();
    }


    @CatalogRequest(
            id = "localDomainRequest_fe01abd4-a8e9-4c1d-9c63-7a289927af7d",
            name = "Create Page from Template",
            description = "Create a new page given the template identifier and the placeholders of the template",
            area = "Page",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.Desc(name = "Page", type = "Entity(Page)", required = false)
    public ExtensionResponse createPageFromTemplate(
            @Field.Text(name = "Space ID", attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "toolTip", value = "'Enter the unique identifier of the Confluence space where the page will be created'")}) String spaceID,
            @Field.Text(name = "Template ID", attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "toolTip", value = "'Enter the unique identifier of the template to use for creating the page'")}) String templateID,
            @Field(name = "Page Title", type = "Identifier", attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "toolTip", value = "'Enter a descriptive title for the new page'")}) String pageTitle,
            @Field.Desc(name = "Variables", type = "[ { Key: Text, Value: Text } ]") List<Map<String, Object>> variables,
            @Field.Boolean(name = Constants.ENABLE_ERROR_HANDLING, required = false,
                    attributes = {@Attribute(name = "toolTip", value = "'Enable to allow re-entry of values if validation fails'")}) Boolean allowRetry) {

        String invokerId = requestContext.getInvokerId();
        confluenceAuditService.triggered(invokerId, "Create Page from Template");
        boolean auditRecorded = false;

        long startTime = System.currentTimeMillis();
        String methodName = "createPageFromTemplate";
        String metricName = "confluence.createPageFromTemplate";
        LOGGER.info("→ {}: Starting operation with spaceID: {}, templateID: {}, pageTitle: {}, allowRetry: {}", methodName, spaceID, templateID, pageTitle, allowRetry);

        try {
            telemetryHelper.incrementCount(metricName);
            Map<Validator.ValidationResource, String> validationParams = new HashMap<>();
            addValidationParamIfNotEmpty(validationParams, Validator.ValidationResource.SPACE_ID, spaceID, methodName);
            addValidationParamIfNotEmpty(validationParams, Validator.ValidationResource.TEMPLATE_ID, templateID, methodName);

            List<ValidationOrchestrator.ValidationResult> validationResults =
                    validationOrchestrator.validate(validationParams, PageArea.class, methodName);

            if (validationResults.isEmpty()) {
                return executeCreatePageFromTemplate(spaceID, templateID, pageTitle, variables, metricName, startTime);
            } else {
                Map<String, Object> confirmationParams = Map.of(
                        "Space ID", spaceID != null ? spaceID : "",
                        "Template ID", templateID != null ? templateID : "",
                        "Page Title", pageTitle != null ? pageTitle : "",
                        "Variables", variables != null ? variables : List.of());
                Map<String, String> telemetryTags = safeTagMap("space_id", spaceID, "template_id", templateID, "page_title", pageTitle, "validation_count", String.valueOf(validationResults.size()));
                return handleValidationFailure(validationResults, allowRetry, metricName, startTime,
                        Constants.CONFIRM_REENTER_CREATE_PAGE_FROM_TEMPLATE, confirmationParams, telemetryTags, methodName);
            }
        } catch (MustAuthorizeException cause) {
            auditRecorded = true;
            confluenceAuditService.authFailure(invokerId, "Create Page from Template", cause);
            throw cause;
        } catch (Exception cause) {
            auditRecorded = true;
            confluenceAuditService.failure(invokerId, "Create Page from Template", cause);
            String errorMessage = String.format("""
                    We couldn't create the page from the template.
                    This could mean:
                    • The template ID '%s' doesn't exist or isn't accessible
                    • The space ID '%s' doesn't exist or you don't have access to it
                    • The page title '%s' might conflict with an existing page
                    • Template variables might be incorrectly formatted

                    What to do next:
                    1. Verify the template ID using 'Get Templates By Space ID'
                    2. Confirm the space ID is correct using 'Get Spaces'
                    3. Check if a page with this title already exists
                    4. Review your template variables for correct formatting

                    Technical details: %s
                    """, templateID, spaceID, pageTitle, cause.getMessage());
            return buildSystemErrorResponse(cause, errorMessage, metricName, startTime,
                    safeTagMap("space_id", spaceID, "template_id", templateID, "page_title", pageTitle), methodName);
        } finally {
            if (!auditRecorded) {
                confluenceAuditService.success(invokerId, "Create Page from Template");
            }
        }
    }

    private ExtensionResponse executeCreatePageFromTemplate(String spaceID, String templateID, String pageTitle,
                                                            List<Map<String, Object>> variables, String metricName, long startTime) {
        LOGGER.debug("→ createPageFromTemplate: Validation passed, creating page from template");
        Page page = setPageEntity(service.createPageFromTemplate(spaceID, templateID, pageTitle, variables));
        LOGGER.info("→ createPageFromTemplate: Successfully created page: {}", page.id);
        telemetryHelper.recordSuccess(metricName, startTime,
                safeTagMap("space_id", spaceID, "template_id", templateID, "page_title", pageTitle, "page_id", page.id));
        return new ExtensionResponseBuilder().success(Map.of("Page", page)).build();
    }

    @CatalogRequest(
            id = "localDomainRequest_8c2c2d03-23ba-4925-bdb9-176c22f19b43",
            name = "Get Page By ID",
            description = "Get the page with the given ID",
            area = "Page",
            type = CatalogRequest.Type.QUERY_SYSTEM, tool = true)
    @Field.Desc(name = "Page", type = "Entity(Page)", required = false)
    public ExtensionResponse getPageByID(
            @Field.Text(name = "Page ID", attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "toolTip", value = "'Enter the unique identifier of the page you want to retrieve'")}) String pageID,
            @Field.Boolean(name = Constants.ENABLE_ERROR_HANDLING, required = false,
                    attributes = {
                            @Attribute(name = "toolTip", value = "'Enable to allow re-entry of values if validation fails'")
                    }) Boolean allowRetry) {

        String invokerId = requestContext.getInvokerId();
        confluenceAuditService.triggered(invokerId, "Get Page By ID");
        boolean auditRecorded = false;

        long startTime = System.currentTimeMillis();
        String methodName = "getPageByID";
        String metricName = "confluence.getPageByID";
        LOGGER.info("→ {}: Starting operation with pageID: {}, allowRetry: {}", methodName, pageID, allowRetry);

        try {
            telemetryHelper.incrementCount(metricName);
            Map<Validator.ValidationResource, String> validationParams = new HashMap<>();
            addValidationParamIfNotEmpty(validationParams, Validator.ValidationResource.PAGE_ID, pageID, methodName);

            List<ValidationOrchestrator.ValidationResult> validationResults =
                    validationOrchestrator.validate(validationParams, PageArea.class, methodName);

            if (validationResults.isEmpty()) {
                return executeGetPageByID(pageID, metricName, startTime);
            } else {
                Map<String, Object> confirmationParams = Map.of("Page ID", pageID != null ? pageID : "");
                Map<String, String> telemetryTags = safeTagMap("page_id", pageID, "validation_count", String.valueOf(validationResults.size()));
                return handleValidationFailure(validationResults, allowRetry, metricName, startTime,
                        Constants.CONFIRM_REENTER_GET_PAGE_BY_ID, confirmationParams, telemetryTags, methodName);
            }
        } catch (MustAuthorizeException cause) {
            auditRecorded = true;
            confluenceAuditService.authFailure(invokerId, "Get Page By ID", cause);
            throw cause;
        } catch (Exception cause) {
            auditRecorded = true;
            confluenceAuditService.failure(invokerId, "Get Page By ID", cause);
            String errorMessage = String.format("""
                    We couldn't retrieve the page information.
                    This could mean:
                    • The page ID '%s' doesn't exist or has been deleted
                    • You don't have permission to view this page
                    • The page is in a restricted space

                    What to do next:
                    1. Verify the page ID is correct using 'Get Pages in Space'
                    2. Check if you have access to the page's space
                    3. Confirm the page hasn't been archived or deleted

                    Technical details: %s
                    """, pageID, cause.getMessage());
            return buildSystemErrorResponse(cause, errorMessage, metricName, startTime,
                    safeTagMap("page_id", pageID), methodName);
        } finally {
            if (!auditRecorded) {
                confluenceAuditService.success(invokerId, "Get Page By ID");
            }
        }
    }

    private ExtensionResponse executeGetPageByID(String pageID, String metricName, long startTime) {
        LOGGER.debug("→ getPageByID: Validation passed, retrieving page");
        Page page = setPageEntity(service.getPageById(pageID));
        LOGGER.info("→ getPageByID: Successfully retrieved page: {}", page.id);
        telemetryHelper.recordSuccess(metricName, startTime, safeTagMap("page_id", pageID));
        return new ExtensionResponseBuilder().success(Map.of("Page", page)).build();
    }

    @CatalogRequest(
            id = "localDomainRequest_a9708abf-c565-4d0e-912a-c5f3dc1b6b01",
            name = "Get Pages in Space (Deprecated)",
            description = "DEPRECATED — backed by the v1 /wiki/rest/api/content endpoint that Atlassian removed from Confluence Cloud (HTTP 410 Gone). New integrations should use 'Get Pages in Space (V2)' instead, which uses the v2 API, accepts the numeric Space Id directly, supports cursor-based pagination, and offers an Exclude Restricted Pages filter. This request is preserved for backward compatibility only and will not function against current Confluence Cloud.",
            area = "Page",
            type = CatalogRequest.Type.QUERY_SYSTEM, tool = true)
    @Field.Desc(name = "Pages", type = "[ Entity(Page) ]", required = false)
    @Field.Desc(name = "Page Identifiers", type = "[ Identifier ]", required = false)
    public ExtensionResponse getPagesInSpace(
            @Field.Text(name = "Space ID", attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "toolTip", value = "'Enter the unique identifier of the Confluence space to search for pages'")}) String spaceID,
            @Field.Text(name = "Title", required = false, attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "toolTip", value = "'Optional: Enter a specific page title to filter results'")}) String title,
            @Field.Boolean(name = Constants.ENABLE_ERROR_HANDLING, required = false,
                    attributes = {@Attribute(name = "toolTip", value = "'Enable to allow re-entry of values if validation fails'")}) Boolean allowRetry) {

        String invokerId = requestContext.getInvokerId();
        confluenceAuditService.triggered(invokerId, "Get Pages in Space");
        boolean auditRecorded = false;

        long startTime = System.currentTimeMillis();
        String methodName = "getPagesInSpace";
        String metricName = "confluence.getPagesInSpace";
        LOGGER.info("→ {}: Starting operation with spaceID: {}, title: {}, allowRetry: {}", methodName, spaceID, title, allowRetry);

        try {
            telemetryHelper.incrementCount(metricName);
            Map<Validator.ValidationResource, String> validationParams = new HashMap<>();
            addValidationParamIfNotEmpty(validationParams, Validator.ValidationResource.SPACE_ID, spaceID, methodName);

            List<ValidationOrchestrator.ValidationResult> validationResults =
                    validationOrchestrator.validate(validationParams, PageArea.class, methodName);

            if (validationResults.isEmpty()) {
                return executeGetPagesInSpace(spaceID, title, metricName, startTime);
            } else {
                Map<String, Object> confirmationParams = Map.of(
                        "Space ID", spaceID != null ? spaceID : "",
                        "Title", title != null ? title : "");
                Map<String, String> telemetryTags = safeTagMap("space_id", spaceID, "title", title, "validation_count", String.valueOf(validationResults.size()));
                return handleValidationFailure(validationResults, allowRetry, metricName, startTime,
                        Constants.CONFIRM_REENTER_GET_PAGES_IN_SPACE, confirmationParams, telemetryTags, methodName);
            }
        } catch (MustAuthorizeException cause) {
            auditRecorded = true;
            confluenceAuditService.authFailure(invokerId, "Get Pages in Space", cause);
            throw cause;
        } catch (Exception cause) {
            auditRecorded = true;
            confluenceAuditService.failure(invokerId, "Get Pages in Space", cause);
            String errorMessage = String.format("""
                    We couldn't retrieve pages from the space.
                    This could mean:
                    • The space ID '%s' doesn't exist or you don't have access to it
                    • The space is empty or has no pages matching your criteria
                    • Your Confluence connection might be experiencing issues

                    What to do next:
                    1. Verify the space ID is correct using 'Get Spaces'
                    2. Check if you have permission to view pages in this space
                    3. Try without a title filter to see all pages

                    Technical details: %s
                    """, spaceID, cause.getMessage());
            return buildSystemErrorResponse(cause, errorMessage,
                    metricName, startTime, safeTagMap("space_id", spaceID, "title", title), methodName);
        } finally {
            if (!auditRecorded) {
                confluenceAuditService.success(invokerId, "Get Pages in Space");
            }
        }
    }

    private ExtensionResponse executeGetPagesInSpace(String spaceID, String title, String metricName, long startTime) {
        LOGGER.debug("→ getPagesInSpace: Validation passed, retrieving pages");
        List<Page> pages = retrievePagesFromSpace(spaceID, title);

        Map<String, Object> result = Map.of(
                "Pages", pages,
                "Page Identifiers", pages.stream().map(page -> page.id).collect(Collectors.toList())
        );
        telemetryHelper.recordSuccess(metricName, startTime,
                safeTagMap("space_id", spaceID, "title", title, "page_count", String.valueOf(pages.size())));
        return new ExtensionResponseBuilder().success(result).build();
    }

    private List<Page> retrievePagesFromSpace(String spaceID, String title) {
        List<Page> pages;
        if (title != null && !title.trim().isEmpty()) {
            pages = Arrays.stream(service.getPagesBySpaceId(spaceID)).map(ParserUtils::setPageEntity)
                    .filter(page -> page.title.equalsIgnoreCase(title.trim()))
                    .collect(Collectors.toList());
            LOGGER.info("→ getPagesInSpace: Successfully retrieved {} pages with title filter: {}", pages.size(), title);
        } else {
            pages = Arrays.stream(service.getPagesBySpaceId(spaceID)).map(ParserUtils::setPageEntity)
                    .collect(Collectors.toList());
            LOGGER.info("→ getPagesInSpace: Successfully retrieved {} pages", pages.size());
        }
        return pages;
    }

    @CatalogRequest(
            id = "localDomainRequest_b9d7e2f4-1c5a-4f8b-93de-2a1c8f3e7d04",
            name = "Get Pages in Space (V2)",
            description = "Get pages in a Confluence space using the v2 REST API. Accepts the numeric Space Id directly (no Space Key lookup), supports an Exclude Restricted Pages flag to return only pages open to all users, a Status filter that returns only pages of the chosen status (current/archived/trashed; 'any' for no filter), and an Exclude Page Content flag (on by default) that omits page body to keep responses small for large spaces.",
            area = "Page",
            type = CatalogRequest.Type.QUERY_SYSTEM)
    @Field.Desc(name = "Pages", type = "[ Entity(Page) ]", required = false)
    @Field.Desc(name = "Page Identifiers", type = "[ Identifier ]", required = false)
    public ExtensionResponse getPagesInSpaceV2(
            @Field.Text(name = "Space ID", attributes = {@Attribute(name = "visualWidth", value = "S"),
                    @Attribute(name = "toolTip", value = "'Enter the numeric Confluence Space Id (use Get Spaces or Get Space Id by Alias to discover it).'")}) String spaceID,
            @Field.Boolean(name = Constants.EXCLUDE_RESTRICTED_PAGES, required = false,
                    attributes = {@Attribute(name = "toolTip", value = "'When enabled, only returns pages open to all users — pages with read restrictions (page or inherited) are excluded. Implemented via v2 /pages/{id}/restrictions check per page.'")}) Boolean excludeRestricted,
            @Field.PickOne(name = Constants.PAGE_STATUS, values = {"current", "archived", "draft", "any", "trashed"}, required = false,
                    attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "toolTip", value = "'Return only pages of the selected status. Choose any to apply no status filter. The filter is passed to the v2 API, so non-matching pages are never fetched or restriction-checked.'")}) String pageStatus,
            @Field.Boolean(name = Constants.INCLUDE_PAGE_CONTENT, required = false,
                    attributes = {@Attribute(name = "toolTip", value = "'Page bodies are omitted by default. Tick to include full storage-format content on every page — WARNING: spaces with many or large pages can exhaust memory and fail with an out-of-memory error.'")}) Boolean includeContent,
            @Field.Boolean(name = Constants.ENABLE_ERROR_HANDLING, required = false,
                    attributes = {@Attribute(name = "toolTip", value = "'Enable to allow re-entry of values if validation fails'")}) Boolean allowRetry) {

        long startTime = System.currentTimeMillis();
        String methodName = "getPagesInSpaceV2";
        String metricName = "confluence.getPagesInSpaceV2";
        boolean excludeRestrictedFlag = Boolean.TRUE.equals(excludeRestricted);
        // Server follows the UI exactly: unticked/null = exclude (safe default); ticked = include.
        boolean excludeContentFlag = !Boolean.TRUE.equals(includeContent);
        LOGGER.info("→ {}: Starting operation with spaceID: {}, excludeRestricted: {}, status: {}, includeContent: {}, allowRetry: {}",
                methodName, spaceID, excludeRestrictedFlag, pageStatus, !excludeContentFlag, allowRetry);

        try {
            telemetryHelper.incrementCount(metricName);

            Map<Validator.ValidationResource, String> validationParams = new HashMap<>();
            addValidationParamIfNotEmpty(validationParams, Validator.ValidationResource.SPACE_ID, spaceID, methodName);

            List<ValidationOrchestrator.ValidationResult> validationResults =
                    validationOrchestrator.validate(validationParams, PageArea.class, methodName);

            if (validationResults.isEmpty()) {
                List<Page> pages = Arrays.stream(service.getPagesInSpaceV2(spaceID.trim(), excludeRestrictedFlag, excludeContentFlag, pageStatus))
                        .map(ParserUtils::setPageEntity)
                        .collect(Collectors.toList());

                Map<String, Object> result = Map.of(
                        "Pages", pages,
                        "Page Identifiers", pages.stream().map(page -> page.id).collect(Collectors.toList())
                );
                telemetryHelper.recordSuccess(metricName, startTime,
                        safeTagMap("space_id", spaceID,
                                "exclude_restricted", String.valueOf(excludeRestrictedFlag),
                                "status", pageStatus != null ? pageStatus : "any",
                                "exclude_content", String.valueOf(excludeContentFlag),
                                "page_count", String.valueOf(pages.size())));
                LOGGER.info("→ {}: Returning {} pages", methodName, pages.size());
                return new ExtensionResponseBuilder().success(result).build();
            } else {
                Map<String, Object> confirmationParams = Map.of(
                        "Space ID", spaceID != null ? spaceID : "",
                        Constants.EXCLUDE_RESTRICTED_PAGES, excludeRestrictedFlag,
                        Constants.PAGE_STATUS, pageStatus != null ? pageStatus : "any",
                        Constants.INCLUDE_PAGE_CONTENT, !excludeContentFlag);
                Map<String, String> telemetryTags = safeTagMap("space_id", spaceID,
                        "exclude_restricted", String.valueOf(excludeRestrictedFlag),
                        "status", pageStatus != null ? pageStatus : "any",
                        "exclude_content", String.valueOf(excludeContentFlag),
                        "validation_count", String.valueOf(validationResults.size()));
                return handleValidationFailure(validationResults, allowRetry, metricName, startTime,
                        Constants.CONFIRM_REENTER_GET_PAGES_IN_SPACE_V2, confirmationParams, telemetryTags, methodName);
            }
        } catch (OutOfMemoryError oom) {
            // OOM is an Error, not an Exception, so the generic catch below would miss it.
            // Surface a concrete, actionable error naming the flag the user needs to toggle.
            LOGGER.error("→ {}: OutOfMemoryError for space {} (includeContent={}, excludeRestricted={}). " +
                            "Re-run with Include Page Content unticked to drop page bodies from the response.",
                    methodName, spaceID, !excludeContentFlag, excludeRestrictedFlag);
            telemetryHelper.recordError(metricName, startTime, new RuntimeException("out_of_memory"),
                    safeTagMap("space_id", spaceID,
                            "exclude_restricted", String.valueOf(excludeRestrictedFlag),
                            "exclude_content", String.valueOf(excludeContentFlag),
                            "error_type", "out_of_memory"));
            ExtensionResponse.Error error = new ExtensionResponse.Error(
                    String.format(Constants.PAGES_IN_SPACE_V2_OOM_MESSAGE, spaceID),
                    System.currentTimeMillis(),
                    ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR,
                    "");
            return new ExtensionResponseBuilder().failure(error).build();
        } catch (Exception cause) {
            LOGGER.error("→ {}: Exception occurred — {}", methodName, cause.getMessage(), cause);
            telemetryHelper.recordError(metricName, startTime, cause,
                    safeTagMap("space_id", spaceID,
                            "exclude_restricted", String.valueOf(excludeRestrictedFlag),
                            "exclude_content", String.valueOf(excludeContentFlag)));
            ExtensionResponse.Error error = new ExtensionResponse.Error(
                    cause.getMessage(),
                    System.currentTimeMillis(),
                    ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR,
                    "");
            return new ExtensionResponseBuilder().failure(error).build();
        }
    }

    @CatalogRequest(
            id = "localDomainRequest_dc9804e0-ea2f-46a7-9a22-05e12ca35a3c",
            name = "Update Page by Content",
            description = "Update a page given the ID. Set the status and title. Provide the dynamic content to use for the update",
            area = "Page",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.Desc(name = "Page", type = "Entity(Page)", required = false)
    public ExtensionResponse updatePageByContent(
            @Field.Text(name = "Page ID", attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "toolTip", value = "'Enter the unique identifier of the page you want to update'")}) String pageID,
            @Field.Text(name = "Title", required = false, attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "toolTip", value = "'Optional: Enter a new title for the page'")}) String title,
            @Field.PickOne(name = "Status", values = {"Draft", "Current"}, required = false, attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "toolTip", value = "'Select the publication status for the page'")}) String status,
            @Field.Text(name = "Comments", required = false, attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "toolTip", value = "'Optional: Add comments about the changes made to the page'")}) String comments,
            @Field.Desc(name = "Content", type = "[ Entity(Content) ]", required = false) List<Content> content,
            @Field.Boolean(name = Constants.ENABLE_ERROR_HANDLING, required = false,
                    attributes = {@Attribute(name = "toolTip", value = "'Enable to allow re-entry of values if validation fails'")}) Boolean allowRetry) {

        String invokerId = requestContext.getInvokerId();
        confluenceAuditService.triggered(invokerId, "Update Page by Content");
        boolean auditRecorded = false;

        long startTime = System.currentTimeMillis();
        String methodName = "updatePageByContent";
        String metricName = "confluence.updatePageByContent";
        LOGGER.info("→ {}: Starting operation with pageID: {}, title: {}, status: {}, allowRetry: {}", methodName, pageID, title, status, allowRetry);

        try {
            telemetryHelper.incrementCount(metricName);
            Map<Validator.ValidationResource, String> validationParams = new HashMap<>();
            addValidationParamIfNotEmpty(validationParams, Validator.ValidationResource.PAGE_ID, pageID, methodName);

            List<ValidationOrchestrator.ValidationResult> validationResults =
                    validationOrchestrator.validate(validationParams, PageArea.class, methodName);

            if (validationResults.isEmpty()) {
                return executeUpdatePageByContent(pageID, status, title, comments, content, metricName, startTime);
            } else {
                Map<String, Object> confirmationParams = Map.of(
                        "Page ID", pageID != null ? pageID : "",
                        "Title", title != null ? title : "",
                        "Status", status != null ? status : "",
                        "Comments", comments != null ? comments : "",
                        "Content", content != null ? content : List.of());
                Map<String, String> telemetryTags = safeTagMap("page_id", pageID, "title", title, "status", status, "validation_count", String.valueOf(validationResults.size()));
                return handleValidationFailure(validationResults, allowRetry, metricName, startTime,
                        Constants.CONFIRM_REENTER_UPDATE_PAGE_BY_CONTENT, confirmationParams, telemetryTags, methodName);
            }
        } catch (MustAuthorizeException cause) {
            auditRecorded = true;
            confluenceAuditService.authFailure(invokerId, "Update Page by Content", cause);
            throw cause;
        } catch (Exception cause) {
            auditRecorded = true;
            confluenceAuditService.failure(invokerId, "Update Page by Content", cause);
            String errorMessage = String.format("""
                    We couldn't update the page with the new content.
                    This could mean:
                    • The page ID '%s' doesn't exist or has been deleted
                    • You don't have permission to edit this page
                    • The page is locked by another user
                    • The content format might be invalid

                    What to do next:
                    1. Verify the page ID is correct using 'Get Page By ID'
                    2. Check if you have edit permissions for this page
                    3. Ensure the page isn't currently being edited by someone else
                    4. Review your content structure for any formatting issues

                    Technical details: %s
                    """, pageID, cause.getMessage());
            return buildSystemErrorResponse(cause, errorMessage, metricName, startTime,
                    safeTagMap("page_id", pageID, "title", title, "status", status), methodName);
        } finally {
            if (!auditRecorded) {
                confluenceAuditService.success(invokerId, "Update Page by Content");
            }
        }
    }

    private ExtensionResponse executeUpdatePageByContent(String pageID, String status, String title, String comments,
                                                         List<Content> content, String metricName, long startTime) {
        LOGGER.debug("→ updatePageByContent: Validation passed, updating page by content");
        Page page = setPageEntity(service.updatePageFromContent(pageID, status, title, comments, content));
        LOGGER.info("→ updatePageByContent: Successfully updated page: {}", page.id);
        telemetryHelper.recordSuccess(metricName, startTime,
                safeTagMap("page_id", pageID, "title", title, "status", status));
        return new ExtensionResponseBuilder().success(Map.of("Page", page)).build();
    }

    @CatalogRequest(
            id = "localDomainRequest_9cd3f8ab-f842-44c5-9649-24936d2081a5",
            name = "Update Page from Template",
            description = "Update a page given the ID. Set the status and title. If a valid template ID is provided then variables are updated with the values of the placeholders.",
            area = "Page",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.Desc(name = "Page", type = "Entity(Page)", required = false)
    public ExtensionResponse updatePageFromTemplate(
            @Field.Text(name = "Page ID", attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "toolTip", value = "'Enter the unique identifier of the page you want to update'")}) String pageID,
            @Field.Text(name = "Template Id", attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "toolTip", value = "'Enter the unique identifier of the template to use for updating the page'")}) String templateId,
            @Field.PickOne(name = "Status", values = {"Current", "Draft"}, required = false, attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "toolTip", value = "'Select the publication status for the updated page'")}) String status,
            @Field.Text(name = "Title", required = false, attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "toolTip", value = "'Optional: Enter a new title for the page'")}) String title,
            @Field.Text(name = "Comment", required = false, attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "toolTip", value = "'Optional: Add comments about the changes made to the page'")}) String comment,
            @Field.Desc(name = "Variables", type = "[ { Key: Identifier, Value: Text } ]", required = false) List<Map<String, Object>> variables,
            @Field.Boolean(name = Constants.ENABLE_ERROR_HANDLING, required = false,
                    attributes = {@Attribute(name = "toolTip", value = "'Enable to allow re-entry of values if validation fails'")}) Boolean allowRetry) {

        String invokerId = requestContext.getInvokerId();
        confluenceAuditService.triggered(invokerId, "Update Page from Template");
        boolean auditRecorded = false;

        long startTime = System.currentTimeMillis();
        String methodName = "updatePageFromTemplate";
        String metricName = "confluence.updatePageFromTemplate";
        LOGGER.info("→ {}: Starting operation with pageID: {}, templateId: {}, status: {}, title: {}, allowRetry: {}", methodName, pageID, templateId, status, title, allowRetry);

        try {
            telemetryHelper.incrementCount(metricName);
            Map<Validator.ValidationResource, String> validationParams = new HashMap<>();
            addValidationParamIfNotEmpty(validationParams, Validator.ValidationResource.PAGE_ID, pageID, methodName);
            addValidationParamIfNotEmpty(validationParams, Validator.ValidationResource.TEMPLATE_ID, templateId, methodName);

            List<ValidationOrchestrator.ValidationResult> validationResults =
                    validationOrchestrator.validate(validationParams, PageArea.class, methodName);

            if (validationResults.isEmpty()) {
                return executeUpdatePageFromTemplate(pageID, templateId, status, title, comment, variables, metricName, startTime);
            } else {
                Map<String, Object> confirmationParams = Map.of(
                        "Page ID", pageID != null ? pageID : "",
                        "Template Id", templateId != null ? templateId : "",
                        "Status", status != null ? status : "",
                        "Title", title != null ? title : "",
                        "Comment", comment != null ? comment : "",
                        "Variables", variables != null ? variables : List.of());
                Map<String, String> telemetryTags = safeTagMap("page_id", pageID, "template_id", templateId, "status", status, "title", title, "validation_count", String.valueOf(validationResults.size()));
                return handleValidationFailure(validationResults, allowRetry, metricName, startTime,
                        Constants.CONFIRM_REENTER_UPDATE_PAGE_FROM_TEMPLATE, confirmationParams, telemetryTags, methodName);
            }
        } catch (MustAuthorizeException cause) {
            auditRecorded = true;
            confluenceAuditService.authFailure(invokerId, "Update Page from Template", cause);
            throw cause;
        } catch (Exception cause) {
            auditRecorded = true;
            confluenceAuditService.failure(invokerId, "Update Page from Template", cause);
            String errorMessage = String.format("""
                    We couldn't update the page using the template.
                    This could mean:
                    • The page ID '%s' doesn't exist or has been deleted
                    • The template ID '%s' doesn't exist or isn't accessible
                    • You don't have permission to edit this page
                    • Template variables might be incorrectly formatted

                    What to do next:
                    1. Verify the page ID is correct using 'Get Page By ID'
                    2. Confirm the template ID using 'Get Templates By Space ID'
                    3. Check if you have edit permissions for this page
                    4. Review your template variables for correct formatting

                    Technical details: %s
                    """, pageID, templateId, cause.getMessage());
            return buildSystemErrorResponse(cause, errorMessage, metricName, startTime,
                    safeTagMap("page_id", pageID, "template_id", templateId, "status", status, "title", title), methodName);
        } finally {
            if (!auditRecorded) {
                confluenceAuditService.success(invokerId, "Update Page from Template");
            }
        }
    }

    private ExtensionResponse executeUpdatePageFromTemplate(String pageID, String templateId, String status, String title,
                                                            String comment, List<Map<String, Object>> variables, String metricName, long startTime) {
        LOGGER.debug("→ updatePageFromTemplate: Validation passed, updating page from template");
        Page page = setPageEntity(service.updatePageFromTemplate(pageID, templateId, status, title, comment, variables));
        LOGGER.info("→ updatePageFromTemplate: Successfully updated page: {}", page.id);
        telemetryHelper.recordSuccess(metricName, startTime,
                safeTagMap("page_id", pageID, "template_id", templateId, "status", status, "title", title));
        return new ExtensionResponseBuilder().success(Map.of("Page", page)).build();
    }

    @CatalogRequest(
            id = "localDomainRequest_a1b2c3d4-e5f6-7890-abcd-ef1234567890",
            name = "Get Page in Space",
            description = "Given a page's unique ID, download the full page including content and formatting as a Word (DOCX) file",
            area = "Page",
            type = CatalogRequest.Type.QUERY_SYSTEM, tool = true)
    @Field.File(name = "Downloaded File", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")})
    @Field.Boolean(name = "Parsing Success", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")})
    public ExtensionResponse getPageInSpace(
            @Field.Text(name = "Page ID", attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "toolTip", value = "'Enter the unique identifier of the page you want to download'")}) String pageID,
            @Field.PickOne(name = "Export Format", values = {"docx"}, attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "toolTip", value = "'Select the format for downloading the page (currently supports DOCX)'")}) String exportFormat,
            @Field.Boolean(name = Constants.ENABLE_ERROR_HANDLING, required = false,
                    attributes = {@Attribute(name = "toolTip", value = "'Enable to allow re-entry of values if validation fails'")}) Boolean allowRetry) {

        String invokerId = requestContext.getInvokerId();
        confluenceAuditService.triggered(invokerId, "Get Page in Space");
        boolean auditRecorded = false;

        long startTime = System.currentTimeMillis();
        String methodName = "getPageInSpace";
        String metricName = "confluence.getPageInSpace";
        LOGGER.info("→ {}: Starting operation with pageID: {}, exportFormat: {}, allowRetry: {}", methodName, pageID, exportFormat, allowRetry);

        try {
            telemetryHelper.incrementCount(metricName);
            Map<Validator.ValidationResource, String> validationParams = new HashMap<>();
            addValidationParamIfNotEmpty(validationParams, Validator.ValidationResource.PAGE_ID, pageID, methodName);

            List<ValidationOrchestrator.ValidationResult> validationResults =
                    validationOrchestrator.validate(validationParams, PageArea.class, methodName);

            if (validationResults.isEmpty()) {
                return executeGetPageInSpace(pageID, exportFormat, metricName, startTime);
            } else {
                Map<String, Object> confirmationParams = Map.of(
                        "Page ID", pageID != null ? pageID : "",
                        "Export Format", exportFormat != null ? exportFormat : "");
                Map<String, String> telemetryTags = safeTagMap("page_id", pageID, "export_format", exportFormat, "validation_count", String.valueOf(validationResults.size()));
                return handleValidationFailure(validationResults, allowRetry, metricName, startTime,
                        Constants.CONFIRM_REENTER_GET_PAGE_IN_SPACE, confirmationParams, telemetryTags, methodName);
            }
        } catch (MustAuthorizeException cause) {
            auditRecorded = true;
            confluenceAuditService.authFailure(invokerId, "Get Page in Space", cause);
            throw cause;
        } catch (Exception cause) {
            auditRecorded = true;
            confluenceAuditService.failure(invokerId, "Get Page in Space", cause);
            String errorMessage = String.format("""
                    We couldn't download the page in the requested format.
                    This could mean:
                    • The page ID '%s' doesn't exist or has been deleted
                    • You don't have permission to view or export this page
                    • The export format '%s' might not be supported
                    • The page content might be too large or complex to export

                    What to do next:
                    1. Verify the page ID is correct using 'Get Page By ID'
                    2. Check if you have permission to view this page
                    3. Ensure the export format is supported (currently: docx)
                    4. Try exporting a simpler page to test the connection

                    Technical details: %s
                    """, pageID, exportFormat, cause.getMessage());
            return buildSystemErrorResponse(cause, errorMessage, metricName, startTime,
                    safeTagMap("page_id", pageID, "export_format", exportFormat), methodName);
        } finally {
            if (!auditRecorded) {
                confluenceAuditService.success(invokerId, "Get Page in Space");
            }
        }
    }

    private ExtensionResponse executeGetPageInSpace(String pageID, String exportFormat, String metricName, long startTime) {
        LOGGER.debug("→ getPageInSpace: Validation passed, proceeding with page download");
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageID, exportFormat);
        File downloadedFile = result.getKey();
        Boolean parsingSuccess = result.getValue();
        LOGGER.info("→ getPageInSpace: Successfully downloaded page {} as {}, parsing success: {}", pageID, exportFormat, parsingSuccess);
        telemetryHelper.recordSuccess(metricName, startTime,
                safeTagMap("page_id", pageID, "export_format", exportFormat, "parsing_success", String.valueOf(parsingSuccess)));
        return new ExtensionResponseBuilder().success(Map.of(
                "Downloaded File", downloadedFile,
                "Parsing Success", parsingSuccess
        )).build();
    }

    @CatalogRequest(
            id = "localDomainRequest_135f6920-3f12-40ca-a94d-abaf890e4c04",
            name = "Get Page Content as a File",
            description = "Download a single page's content as a Markdown (.md) file. Storage-format XHTML and Confluence macros (code, panels, links, images, emoticons) are converted to Markdown; the file is named after the page title.",
            area = "Page",
            type = CatalogRequest.Type.QUERY_SYSTEM)
    @Field.File(name = "Downloaded File", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")})
    public ExtensionResponse getPageContentAsFile(
            @Field.Text(name = "Page ID",
                    attributes = {@Attribute(name = "visualWidth", value = "S"),
                            @Attribute(name = "toolTip", value = "'Enter the unique identifier of the page whose content you want downloaded as a Markdown file'")}) String pageID,
            @Field.Boolean(name = Constants.ENABLE_ERROR_HANDLING, required = false,
                    attributes = {@Attribute(name = "toolTip", value = "'Enable to allow re-entry of values if validation fails'")}) Boolean allowRetry) {

        long startTime = System.currentTimeMillis();
        String methodName = "getPageContentAsFile";
        String metricName = "confluence.getPageContentAsFile";
        LOGGER.info("→ {}: Starting operation with pageID: {}, allowRetry: {}", methodName, pageID, allowRetry);

        try {
            telemetryHelper.incrementCount(metricName);
            Map<Validator.ValidationResource, String> validationParams = new HashMap<>();
            addValidationParamIfNotEmpty(validationParams, Validator.ValidationResource.PAGE_ID, pageID, methodName);

            List<ValidationOrchestrator.ValidationResult> validationResults =
                    validationOrchestrator.validate(validationParams, PageArea.class, methodName);

            if (validationResults.isEmpty()) {
                return executeGetPageContentAsFile(pageID, metricName, startTime);
            } else {
                Map<String, Object> confirmationParams = Map.of("Page ID", pageID != null ? pageID : "");
                Map<String, String> telemetryTags = safeTagMap("page_id", pageID, "validation_count", String.valueOf(validationResults.size()));
                return handleValidationFailure(validationResults, allowRetry, metricName, startTime,
                        Constants.CONFIRM_REENTER_GET_PAGE_CONTENT_AS_FILE, confirmationParams, telemetryTags, methodName);
            }
        } catch (Exception cause) {
            String errorMessage = String.format("""
                    We couldn't download the page content as a Markdown file.
                    This could mean:
                    • The page ID '%s' doesn't exist or has been deleted
                    • You don't have permission to view this page
                    • The page is in a restricted space
                    • The page body contains content that couldn't be converted

                    What to do next:
                    1. Verify the page ID is correct using 'Get Pages in Space (V2)'
                    2. Check that you can view the page in Confluence
                    3. Confirm the page hasn't been archived or deleted

                    Technical details: %s
                    """, pageID, cause.getMessage());
            return buildSystemErrorResponse(cause, errorMessage, metricName, startTime,
                    safeTagMap("page_id", pageID), methodName);
        }
    }

    private ExtensionResponse executeGetPageContentAsFile(String pageID, String metricName, long startTime) {
        LOGGER.debug("→ getPageContentAsFile: Validation passed, generating Markdown file");
        File markdownFile = pageMarkdownService.downloadPageAsMarkdownFile(pageID);
        LOGGER.info("→ getPageContentAsFile: Successfully generated Markdown file for page {}", pageID);
        telemetryHelper.recordSuccess(metricName, startTime, safeTagMap("page_id", pageID));
        return new ExtensionResponseBuilder().success(Map.of("Downloaded File", markdownFile)).build();
    }


    @CatalogRequest(
            id = "localDomainRequest_a50361a7-6e81-426c-9ce9-71a7e43b91d6",
            name = "Get Direct Child Pages of Page",
            description = "Get the direct child pages of a specific page for hierarchical navigation",
            area = "Page",
            type = CatalogRequest.Type.QUERY_SYSTEM, tool = true)
    @Field.Desc(name = "Pages", type = "[ Entity(Page) ]", required = false)
    @Field.Desc(name = "Success", type = "Boolean", required = false)
    public ExtensionResponse getDirectChildPagesOfPage(
            @Field.Text(name = "Page ID", attributes = {@Attribute(name = "visualWidth", value = "S")}) String pageID) {

        String invokerId = requestContext.getInvokerId();
        confluenceAuditService.triggered(invokerId, "Get Direct Child Pages of Page");
        boolean auditRecorded = false;

        long startTime = System.currentTimeMillis();
        String methodName = "getDirectChildPagesOfPage";
        String metricName = "confluence.getDirectChildPagesOfPage";
        LOGGER.info("→ {}: Starting operation with pageID: {}", methodName, pageID);

        try {
            List<Page> pages = Arrays.stream(service.getDirectChildPagesOfPage(pageID))
                    .map(ParserUtils::setPageEntity)
                    .collect(Collectors.toList());

            Map<String, Object> result = Map.of(
                    Constants.PAGES, pages,
                    Constants.PAGE_IDENTIFIERS, pages.stream().map(page -> page.id).collect(Collectors.toList()),
                    "Success", true
            );
            telemetryHelper.recordSuccess(metricName, startTime,
                    safeTagMap("page_id", pageID, "pages_count", String.valueOf(pages.size())));
            return new ExtensionResponseBuilder().success(result).build();
        } catch (MustAuthorizeException cause) {
            auditRecorded = true;
            confluenceAuditService.authFailure(invokerId, "Get Direct Child Pages of Page", cause);
            throw cause;
        } catch (Exception e) {
            auditRecorded = true;
            confluenceAuditService.failure(invokerId, "Get Direct Child Pages of Page", e);
            return buildGracefulErrorResponse(e, metricName, startTime, safeTagMap("page_id", pageID), methodName,
                    Map.of(Constants.PAGES, Collections.emptyList(), Constants.PAGE_IDENTIFIERS, Collections.emptyList()));
        } finally {
            if (!auditRecorded) {
                confluenceAuditService.success(invokerId, "Get Direct Child Pages of Page");
            }
        }
    }


    @CatalogRequest(
            id = "localDomainRequest_e900655e-4336-4f77-b2bc-ea80aec15deb",
            name = "Check if Page Has Children",
            description = "Check whether a specific page has child pages for hierarchical navigation purposes",
            area = "Page",
            type = CatalogRequest.Type.QUERY_SYSTEM, tool = true)
    @Field.Boolean(name = "Has Children", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")})
    @Field.Desc(name = "Success", type = "Boolean", required = false)
    public ExtensionResponse checkIfPageHasChildren(
            @Field.Text(name = "Page ID", attributes = {@Attribute(name = "visualWidth", value = "S")}) String pageID) {

        String invokerId = requestContext.getInvokerId();
        confluenceAuditService.triggered(invokerId, "Check if Page Has Children");
        boolean auditRecorded = false;

        long startTime = System.currentTimeMillis();
        String methodName = "checkIfPageHasChildren";
        String metricName = "confluence.checkIfPageHasChildren";
        LOGGER.info("→ {}: Starting operation with pageID: {}", methodName, pageID);

        try {
            boolean hasChildren = service.hasChildPages(pageID);
            Map<String, Object> result = Map.of("Has Children", hasChildren, "Success", true);
            telemetryHelper.recordSuccess(metricName, startTime,
                    safeTagMap("page_id", pageID, "has_children", String.valueOf(hasChildren)));
            return new ExtensionResponseBuilder().success(result).build();
        } catch (MustAuthorizeException cause) {
            auditRecorded = true;
            confluenceAuditService.authFailure(invokerId, "Check if Page Has Children", cause);
            throw cause;
        } catch (Exception e) {
            auditRecorded = true;
            confluenceAuditService.failure(invokerId, "Check if Page Has Children", e);
            return buildGracefulErrorResponse(e, metricName, startTime, safeTagMap("page_id", pageID), methodName,
                    Map.of("Has Children", false));
        } finally {
            if (!auditRecorded) {
                confluenceAuditService.success(invokerId, "Check if Page Has Children");
            }
        }
    }


    @CatalogRequest(
            id = "localDomainRequest_2a1a0602-73bc-44a5-941f-39e9818f24d3",
            name = "Get All Nested Descendants of Page",
            description = "Get all nested descendants (children, grandchildren, etc.) of a specific page recursively for complete hierarchical navigation",
            area = "Page",
            type = CatalogRequest.Type.QUERY_SYSTEM, tool = true)
    @Field.Desc(name = "Pages", type = "[ Entity(Page) ]", required = false)
    @Field.Boolean(name = "Success", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")})
    @Field(name = "Total Count", type = "Number", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")})
    public ExtensionResponse getAllNestedDescendantsOfPage(
            @Field.Text(name = "Page ID", attributes = {@Attribute(name = "visualWidth", value = "S")}) String pageID) {

        String invokerId = requestContext.getInvokerId();
        confluenceAuditService.triggered(invokerId, "Get All Nested Descendants of Page");
        boolean auditRecorded = false;

        long startTime = System.currentTimeMillis();
        String methodName = "getAllNestedDescendantsOfPage";
        String metricName = "confluence.getAllNestedDescendantsOfPage";
        LOGGER.info("→ {}: Starting operation with pageID: {}", methodName, pageID);

        try {
            List<Page> pages = Arrays.stream(service.getAllNestedDescendants(pageID))
                    .map(ParserUtils::setPageEntity)
                    .toList();

            Map<String, Object> result = Map.of("Pages", pages, "Success", true, "Total Count", pages.size());
            LOGGER.info("→ {}: Successfully retrieved {} nested descendants", methodName, pages.size());
            telemetryHelper.recordSuccess(metricName, startTime,
                    safeTagMap("page_id", pageID, "descendants_count", String.valueOf(pages.size())));
            return new ExtensionResponseBuilder().success(result).build();
        } catch (MustAuthorizeException cause) {
            auditRecorded = true;
            confluenceAuditService.authFailure(invokerId, "Get All Nested Descendants of Page", cause);
            throw cause;
        } catch (Exception e) {
            auditRecorded = true;
            confluenceAuditService.failure(invokerId, "Get All Nested Descendants of Page", e);
            return buildGracefulErrorResponse(e, metricName, startTime, safeTagMap("page_id", pageID), methodName,
                    Map.of("Pages", Collections.emptyList(), "Total Count", 0));
        } finally {
            if (!auditRecorded) {
                confluenceAuditService.success(invokerId, "Get All Nested Descendants of Page");
            }
        }
    }


    @CatalogRequest(
            id = "localDomainRequest_337b5c91-5415-4432-ae8b-a895fc3e31cd",
            name = "Get Ancestors of Page",
            description = "Get all ancestors (parent pages) of a specific page for hierarchical navigation and breadcrumb display",
            area = "Page",
            type = CatalogRequest.Type.QUERY_SYSTEM, tool = true)
    @Field.Desc(name = "Ancestors", type = "[ Entity(Page) ]", required = false)
    @Field.Boolean(name = "Success", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")})
    @Field(name = "Total Count", type = "Number", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")})
    public ExtensionResponse getAncestorsOfPage(
            @Field.Text(name = "Page ID", attributes = {@Attribute(name = "visualWidth", value = "S")}) String pageID) {

        String invokerId = requestContext.getInvokerId();
        confluenceAuditService.triggered(invokerId, "Get Ancestors of Page");
        boolean auditRecorded = false;

        long startTime = System.currentTimeMillis();
        String methodName = "getAncestorsOfPage";
        String metricName = "confluence.getAncestorsOfPage";
        LOGGER.info("→ {}: Starting operation with pageID: {}", methodName, pageID);

        try {
            telemetryHelper.incrementCount(metricName);
            List<Page> ancestors = Arrays.stream(service.getAncestorsOfPage(pageID))
                    .map(ParserUtils::setPageEntity)
                    .toList();

            Map<String, Object> result = Map.of("Ancestors", ancestors, "Success", true, "Total Count", ancestors.size());
            LOGGER.info("→ {}: Successfully retrieved {} ancestors", methodName, ancestors.size());
            telemetryHelper.recordSuccess(metricName, startTime,
                    safeTagMap("page_id", pageID, "ancestors_count", String.valueOf(ancestors.size())));
            return new ExtensionResponseBuilder().success(result).build();
        } catch (MustAuthorizeException cause) {
            auditRecorded = true;
            confluenceAuditService.authFailure(invokerId, "Get Ancestors of Page", cause);
            throw cause;
        } catch (Exception e) {
            auditRecorded = true;
            confluenceAuditService.failure(invokerId, "Get Ancestors of Page", e);
            return buildGracefulErrorResponse(e, metricName, startTime, safeTagMap("page_id", pageID), methodName,
                    Map.of("Ancestors", Collections.emptyList(), "Ancestor Identifiers", Collections.emptyList(), "Total Count", 0));
        } finally {
            if (!auditRecorded) {
                confluenceAuditService.success(invokerId, "Get Ancestors of Page");
            }
        }
    }

    @CatalogRequest(
            id = "localDomainRequest_9df76fb2-bc25-4b7f-aabb-e4f26ae8d3a6",
            name = "Get Attachments for Page",
            description = "Retrieve all attachments for a specific page with optional filtering by status and media type. Supports pagination to handle pages with many attachments",
            area = "Page",
            type = CatalogRequest.Type.QUERY_SYSTEM, tool = true)
    @Field.Desc(name = "Attachments", type = "[ Entity(Attachment) ]",required = false)
    @Field(name = "Total Count", type = "Number", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field.Boolean(name = "Success", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public ExtensionResponse getAttachmentsForPage(
            @Field.Text(name = "Page ID", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String pageID,
            @Field.PickOne(name = "Status", values = {"current","archived","any","trashed"}, required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String status,
            @Field.Text(name = "Media Type", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String mediaType,
            @Field.Boolean(name = "Allow Retry", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) Boolean allowRetry) {

        String invokerId = requestContext.getInvokerId();
        confluenceAuditService.triggered(invokerId, "Get Attachments for Page");
        boolean auditRecorded = false;

        long startTime = System.currentTimeMillis();
        String methodName = "getAttachmentsForPage";
        String metricName = "confluence.getAttachmentsForPage";
        LOGGER.info("→ {}: Starting operation with pageID: {}, status: {}, mediaType: {}, allowRetry: {}",
                methodName, pageID, status, mediaType, allowRetry);

        try {
            telemetryHelper.incrementCount(metricName);
            Map<Validator.ValidationResource, String> validationParams = new HashMap<>();
            addValidationParamIfNotEmpty(validationParams, Validator.ValidationResource.PAGE_ID, pageID, methodName);

            List<ValidationOrchestrator.ValidationResult> validationResults =
                    validationOrchestrator.validate(validationParams, PageArea.class, methodName);

            if (validationResults.isEmpty()) {
                return executeGetAttachmentsForPage(pageID, status, mediaType, metricName, startTime);
            } else {
                Map<String, Object> confirmationParams = Map.of(
                        "Page ID", pageID != null ? pageID : "",
                        "Status", status != null ? status : "current",
                        "Media Type", mediaType != null ? mediaType : "");
                Map<String, String> telemetryTags = safeTagMap("page_id", pageID, "status", status, "media_type", mediaType, "validation_count", String.valueOf(validationResults.size()));
                return handleValidationFailure(validationResults, allowRetry, metricName, startTime,
                        "confirmReenterGetAttachmentsForPage", confirmationParams, telemetryTags, methodName);
            }
        } catch (MustAuthorizeException cause) {
            auditRecorded = true;
            confluenceAuditService.authFailure(invokerId, "Get Attachments for Page", cause);
            throw cause;
        } catch (Exception cause) {
            auditRecorded = true;
            confluenceAuditService.failure(invokerId, "Get Attachments for Page", cause);
            String errorMessage = String.format("""
                    We couldn't retrieve attachments for the page.
                    This could mean:
                    • The page ID '%s' doesn't exist or has been deleted
                    • You don't have permission to view attachments on this page
                    • The page has no attachments matching your criteria
                    • Your Confluence connection might be experiencing issues

                    What to do next:
                    1. Verify the page ID is correct using 'Get Page By ID'
                    2. Check if you have permission to view this page
                    3. Try without status or media type filters to see all attachments
                    4. Confirm the page actually has attachments

                    Technical details: %s
                    """, pageID, cause.getMessage());
            return buildSystemErrorResponse(cause, errorMessage, metricName, startTime,
                    safeTagMap("page_id", pageID, "status", status, "media_type", mediaType), methodName);
        } finally {
            if (!auditRecorded) {
                confluenceAuditService.success(invokerId, "Get Attachments for Page");
            }
        }
    }

    private ExtensionResponse executeGetAttachmentsForPage(String pageID, String status, String mediaType,
                                                           String metricName, long startTime) {
        LOGGER.debug("→ getAttachmentsForPage: Validation passed, retrieving attachments");
        List<Attachment> allAttachments = attachmentHelper.retrieveAllAttachments(pageID, status, mediaType);
        LOGGER.info("→ getAttachmentsForPage: Successfully retrieved {} attachments", allAttachments.size());

        Map<String, Object> result = Map.of(
                "Attachments", allAttachments,
                "Total Count", allAttachments.size(),
                "Success", true
        );
        telemetryHelper.recordSuccess(metricName, startTime,
                safeTagMap("page_id", pageID, "status", status, "media_type", mediaType, "attachment_count", String.valueOf(allAttachments.size())));
        return new ExtensionResponseBuilder().success(result).build();
    }


    @CatalogRequest(
            id = "localDomainRequest_97cc63ec-d8ce-4e05-8629-0094ebdf63af",
            name = "Get Attachment by ID",
            description = "Retrieve detailed metadata for a specific attachment including version history for staleness detection",
            area = "Page",
            type = CatalogRequest.Type.QUERY_SYSTEM, tool = true)
    @Field.Desc(name = "Attachment", type = "Entity(Attachment)",required = false)
    @Field.Boolean(name = "Success", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public ExtensionResponse getAttachmentByID(
            @Field.Text(name = "Attachment ID", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String attachmentID,
            @Field.Boolean(name = "Allow Retry", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) Boolean allowRetry) {

        String invokerId = requestContext.getInvokerId();
        confluenceAuditService.triggered(invokerId, "Get Attachment by ID");
        boolean auditRecorded = false;

        long startTime = System.currentTimeMillis();
        String methodName = "getAttachmentById";
        String metricName = "confluence.getAttachmentById";
        LOGGER.info("→ {}: Starting operation with attachmentID: {}, allowRetry: {}", methodName, attachmentID, allowRetry);

        try {
            telemetryHelper.incrementCount(metricName);
            Map<Validator.ValidationResource, String> validationParams = new HashMap<>();
            addValidationParamIfNotEmpty(validationParams, Validator.ValidationResource.ATTACHMENT_ID, attachmentID, methodName);

            List<ValidationOrchestrator.ValidationResult> validationResults =
                    validationOrchestrator.validate(validationParams, PageArea.class, methodName);

            if (validationResults.isEmpty()) {
                return executeGetAttachmentByID(attachmentID, metricName, startTime);
            } else {
                Map<String, Object> confirmationParams = Map.of("Attachment ID", attachmentID != null ? attachmentID : "");
                Map<String, String> telemetryTags = safeTagMap("attachment_id", attachmentID, "validation_count", String.valueOf(validationResults.size()));
                return handleValidationFailure(validationResults, allowRetry, metricName, startTime,
                        "confirmReenterGetAttachmentById", confirmationParams, telemetryTags, methodName);
            }
        } catch (MustAuthorizeException cause) {
            auditRecorded = true;
            confluenceAuditService.authFailure(invokerId, "Get Attachment by ID", cause);
            throw cause;
        } catch (Exception cause) {
            auditRecorded = true;
            confluenceAuditService.failure(invokerId, "Get Attachment by ID", cause);
            String errorMessage = String.format("""
                    We couldn't retrieve the attachment information.
                    This could mean:
                    • The attachment ID '%s' doesn't exist or has been deleted
                    • You don't have permission to view this attachment
                    • The attachment's parent page might have been deleted

                    What to do next:
                    1. Verify the attachment ID is correct using 'Get Attachments For Page'
                    2. Check if you have permission to view the attachment's page
                    3. Confirm the attachment hasn't been removed

                    Technical details: %s
                    """, attachmentID, cause.getMessage());
            return buildSystemErrorResponse(cause, errorMessage, metricName, startTime,
                    safeTagMap("attachment_id", attachmentID), methodName);
        } finally {
            if (!auditRecorded) {
                confluenceAuditService.success(invokerId, "Get Attachment by ID");
            }
        }
    }

    private ExtensionResponse executeGetAttachmentByID(String attachmentID, String metricName, long startTime) {
        LOGGER.debug("→ getAttachmentById: Validation passed, retrieving attachment");
        HttpResponse<String> response = service.getConfluenceAttributes().getRestClient()
                .doSyncGet(APIVersion.V2, null, "attachments", attachmentID);

        if (response.statusCode() == HttpURLConnection.HTTP_OK) {
            JsonObject responseBody = GsonFactory.getInstance().fromJson(response.body(), JsonObject.class);
            Attachment attachment = attachmentHelper.parseAttachment(responseBody, null);
            LOGGER.info("→ getAttachmentById: Successfully retrieved attachment: {}", attachmentID);

            Map<String, Object> result = Map.of("Attachment", attachment, "Success", true);
            telemetryHelper.recordSuccess(metricName, startTime, safeTagMap("attachment_id", attachmentID));
            return new ExtensionResponseBuilder().success(result).build();
        } else {
            LOGGER.error("→ getAttachmentById: Failed to retrieve attachment. Status: {}, Response: {}",
                    response.statusCode(), response.body());
            throw new RuntimeException("Failed to retrieve attachment: " + attachmentID +
                    ". Status: " + response.statusCode() + ", Response: " + response.body());
        }
    }


    @CatalogRequest(
            id = "localDomainRequest_43799e65-bc3f-4ec6-871e-5708ddd061b2",
            name = "Download Attachment",
            description = "Request Description",
            area = "Page",
            type = CatalogRequest.Type.CHANGE_SYSTEM, tool = true)
    @Field.File(name = "File" , multipleFileUpload = false, required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field.Text(name = "File Name", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field(name = "File Size", type = "Number", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field.Text(name = "Content Type", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field.Boolean(name = "Success", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public ExtensionResponse downloadAttachment(
            @Field(name = "Download Link", type = "Paragraph", required = true, attributes = {@Attribute(name = "visualWidth", value = "L")}, options = {}) String downloadLink,
            @Field.Text(name = "Attachment ID", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String attachmentID) {

        String invokerId = requestContext.getInvokerId();
        confluenceAuditService.triggered(invokerId, "Download Attachment");
        boolean auditRecorded = false;

        long startTime = System.currentTimeMillis();
        String methodName = "downloadAttachment";
        String metricName = "confluence.downloadAttachment";
        LOGGER.info("→ {}: Starting operation with downloadLink: {}, attachmentID: {}", methodName, downloadLink, attachmentID);

        try {
            telemetryHelper.incrementCount(metricName);
            validateDownloadLink(downloadLink);
            String correctedDownloadLink = correctDownloadLink(downloadLink);

            HttpResponse<byte[]> response = service.getConfluenceAttributes().getRestClient()
                    .doSyncGetBinary(correctedDownloadLink);

            if (response.statusCode() == HttpURLConnection.HTTP_OK) {
                return processSuccessfulDownload(response, downloadLink, attachmentID, metricName, startTime);
            } else {
                throw new RuntimeException(String.format("""
                        The Confluence server returned an error (status code: %d). \
                        This usually means the attachment is not accessible or the download link is invalid.
                        """, response.statusCode()));
            }
        } catch (MustAuthorizeException cause) {
            auditRecorded = true;
            confluenceAuditService.authFailure(invokerId, "Download Attachment", cause);
            throw cause;
        } catch (Exception cause) {
            auditRecorded = true;
            confluenceAuditService.failure(invokerId, "Download Attachment", cause);
            return buildGracefulErrorResponse(cause, metricName, startTime,
                    safeTagMap("attachment_id", attachmentID != null ? attachmentID : "unknown"), methodName,
                    Map.of("File Name", "", "File Size", 0.0, "Content Type", ""));
        }
        finally {
            if (!auditRecorded) {
                confluenceAuditService.success(invokerId, "Download Attachment");
            }
        }
    }

    @CatalogRequest(
            id = "localDomainRequest_fb407a56-0c2a-4135-b106-e0e1499a646e",
            name = "Download Attachment (v2)",
            description = "Download an attachment's binary content by its ID using the Confluence v2 API. The attachment metadata is fetched first to resolve a fresh download link, then the file is retrieved for ingestion into Document Sets.",
            area = "Page",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.File(name = "File", multipleFileUpload = false, required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field.Text(name = "File Name", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field(name = "File Size", type = "Number", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field.Text(name = "Content Type", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field.Boolean(name = "Success", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public ExtensionResponse downloadAttachmentV2(
            @Field.Text(name = "Attachment ID", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String attachmentID) {
        return attachmentHelper.downloadAttachmentV2(attachmentID);
    }

    // Matches the v1 REST API child-attachment download endpoint (with or without the
    // '/wiki' context prefix) — the endpoint that actually accepts API-token/OAuth
    // Basic Auth, unlike the legacy '/wiki/download/...' path. See downloadBinaryContent()
    // in PageAreaAttachmentHelper for why this specific path is the one that works.
    private static final Pattern CHILD_ATTACHMENT_DOWNLOAD_PATTERN =
            Pattern.compile("^(/wiki)?/rest/api/content/[^/]+/child/attachment/[^/]+/download(?:\\?.*)?$");

    private void validateDownloadLink(String downloadLink) {
        if (downloadLink == null || downloadLink.trim().isEmpty()) {
            LOGGER.error("→ downloadAttachment: Download link is required");
            throw new IllegalArgumentException("""
                    Download link is required to download the attachment. \
                    Please provide a valid download link from the attachment information.
                    """);
        }
        // 'Get Attachments For Page' now returns fully-qualified download URLs
        // (e.g. https://acme.atlassian.net/wiki/download/...), so reduce to the
        // /wiki-relative path before validating the Confluence download prefix.
        String path = toDownloadPath(downloadLink);
        boolean isLegacyDownloadPath = path.startsWith("/wiki/download/") || path.startsWith("/download/attachments/");
        boolean isChildAttachmentDownloadPath = CHILD_ATTACHMENT_DOWNLOAD_PATTERN.matcher(path).matches();
        if (!isLegacyDownloadPath && !isChildAttachmentDownloadPath) {
            LOGGER.error("→ downloadAttachment: Invalid download link format: {}", downloadLink);
            throw new SecurityException(String.format("""
                    Invalid download link format: '%s'. \
                    The download link must be a valid Confluence download path starting with '/wiki/download/' or '/download/attachments/', \
                    or a v1 child-attachment download path ('/wiki/rest/api/content/{pageId}/child/attachment/{attachmentId}/download'). \
                    Please use the download link from 'Get Attachment By ID' or 'Get Attachments For Page'.
                    """, downloadLink));
        }
    }

    private String correctDownloadLink(String downloadLink) {
        String path = toDownloadPath(downloadLink);
        if (path.startsWith("/download/attachments/")) {
            String corrected = "/wiki" + path;
            LOGGER.debug("→ downloadAttachment: Corrected download link from {} to {}", downloadLink, corrected);
            return corrected;
        }
        if (CHILD_ATTACHMENT_DOWNLOAD_PATTERN.matcher(path).matches() && !path.startsWith("/wiki/")) {
            String corrected = "/wiki" + path;
            LOGGER.debug("→ downloadAttachment: Corrected download link from {} to {}", downloadLink, corrected);
            return corrected;
        }
        return path;
    }

    /**
     * Reduces a download link to its site-relative path so the binary download
     * (which prepends the API base URL itself) works for both relative paths and
     * fully-qualified URLs. Absolute URLs are reduced to path + query; relative
     * paths are returned unchanged.
     */
    private String toDownloadPath(String downloadLink) {
        String trimmed = downloadLink.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            try {
                // A hierarchical http(s) URI always has a defined (possibly empty) path,
                // so getRawPath() is never null here.
                java.net.URI uri = java.net.URI.create(trimmed);
                String path = uri.getRawPath();
                return uri.getRawQuery() != null ? path + "?" + uri.getRawQuery() : path;
            } catch (IllegalArgumentException cause) {
                LOGGER.warn("→ downloadAttachment: Could not parse download URL '{}': {}", trimmed, cause.getMessage());
                return trimmed;
            }
        }
        return trimmed;
    }

    private ExtensionResponse processSuccessfulDownload(HttpResponse<byte[]> response, String downloadLink,
                                                        String attachmentID, String metricName, long startTime) throws IOException {
        String fileName = extractFileName(downloadLink, response.headers().map());
        return writeDownloadResponse(response, fileName, attachmentID, metricName, startTime);
    }

    private ExtensionResponse writeDownloadResponse(HttpResponse<byte[]> response, String fileName,
                                                    String attachmentID, String metricName, long startTime) throws IOException {
        byte[] content = response.body();
        LOGGER.info("→ writeDownloadResponse: Successfully downloaded {} bytes", content.length);

        String contentType = response.headers().firstValue("Content-Type").orElse("application/octet-stream");

        // The Krista MediaStore turns the filename into a temp Path; non-ASCII chars (en-dash,
        // smart quotes, accented letters) crash UnixPath.encode on containers whose
        // sun.jnu.encoding isn't UTF-8. Shared with v2 — see PageAreaAttachmentHelper.
        String safeFileName = PageAreaAttachmentHelper.sanitizeFileNameForFilesystem(fileName);

        java.io.File tempFile = java.io.File.createTempFile("confluence_attachment_", getFileExtension(safeFileName));
        tempFile.deleteOnExit();

        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(content);
        }

        File kristaFile;
        try (final FileHandle fileHandle = fileRepository.createNewFileByName(safeFileName)) {
            fileHandle.setContent(new java.io.FileInputStream(tempFile));
            kristaFile = fileHandle.getFile();
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }

        Map<String, Object> result = Map.of(
                "File", kristaFile,
                "File Name", safeFileName,
                "File Size", (double) content.length,
                "Content Type", contentType,
                "Success", true
        );

        telemetryHelper.recordSuccess(metricName, startTime,
                safeTagMap("attachment_id", attachmentID != null ? attachmentID : "unknown",
                        "file_size", String.valueOf(content.length),
                        "content_type", contentType));

        return new ExtensionResponseBuilder().success(result).build();
    }





    /**
     * Extracts filename from download link or Content-Disposition header.
     *
     * @param downloadLink The download URL
     * @param headers      HTTP response headers
     * @return The extracted filename
     */
    private String extractFileName(String downloadLink, Map<String, List<String>> headers) {
        // Try Content-Disposition header first
        List<String> contentDisposition = headers.get("Content-Disposition");
        if (contentDisposition != null && !contentDisposition.isEmpty()) {
            String disposition = contentDisposition.get(0);
            if (disposition.contains("filename=")) {
                String fileName = disposition
                        .substring(disposition.indexOf("filename=") + 9)
                        .replace("\"", "")
                        .trim();
                if (!fileName.isEmpty()) {
                    LOGGER.debug("→ extractFileName: Extracted from Content-Disposition: {}", fileName);
                    return fileName;
                }
            }
        }

        // Fall back to extracting from URL
        int lastSlash = downloadLink.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < downloadLink.length() - 1) {
            String fileName = URLDecoder.decode(
                    downloadLink.substring(lastSlash + 1),
                    StandardCharsets.UTF_8);
            LOGGER.debug("→ extractFileName: Extracted from URL: {}", fileName);
            return fileName;
        }

        LOGGER.debug("→ extractFileName: Using default filename");
        return "attachment";
    }

    /**
     * Gets file extension from filename.
     *
     * @param fileName The filename
     * @return The file extension including the dot (e.g., ".pdf")
     */
    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) {
            return fileName.substring(lastDot);
        }
        return ".bin";
    }

    // ==================== Helper Methods for Refactoring ====================

    /**
     * Adds a validation parameter if the value is not null or empty.
     *
     * @param params      The validation parameters map
     * @param resource    The validation resource type
     * @param value       The value to add
     * @param methodName  The method name for logging
     */
    private void addValidationParamIfNotEmpty(
            Map<Validator.ValidationResource, String> params,
            Validator.ValidationResource resource,
            String value,
            String methodName) {
        if (value != null && !value.trim().isEmpty()) {
            params.put(resource, value);
            LOGGER.debug("→ {}: Added {} validation parameter: {}", methodName, resource.name(), value);
        }
    }

    /**
     * Handles validation failure by either generating a confirmation response (if retry allowed)
     * or returning a direct validation error response.
     *
     * @param validationResults      The validation results
     * @param allowRetry             Whether retry is allowed
     * @param metricName             The telemetry metric name
     * @param startTime              The operation start time
     * @param confirmationRequestName The confirmation request name
     * @param confirmationParams     The confirmation parameters
     * @param telemetryTags          The telemetry tags
     * @param methodName             The method name for logging
     * @return The appropriate ExtensionResponse
     */
    private ExtensionResponse handleValidationFailure(
            List<ValidationOrchestrator.ValidationResult> validationResults,
            Boolean allowRetry,
            String metricName,
            long startTime,
            String confirmationRequestName,
            Map<String, Object> confirmationParams,
            Map<String, String> telemetryTags,
            String methodName) {

        LOGGER.warn("→ {}: Validation failed with {} errors", methodName, validationResults.size());

        if (Boolean.TRUE.equals(allowRetry)) {
            LOGGER.debug("→ {}: Allow Retry is enabled, generating confirmation response", methodName);
            String stateId = UUID.randomUUID().toString();
            errorHandlingStateManager.put(stateId, Map.of(Constants.VALIDATION_RESULTS, validationResults));

            Map<String, Object> paramsWithStateId = new HashMap<>(confirmationParams);
            paramsWithStateId.put(Constants.STATE_ID, stateId);

            telemetryHelper.recordRetryPrompted(metricName, startTime, telemetryTags);

            return responseGenerator.generateConfirmationResponse(
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    validationResults,
                    confirmationRequestName,
                    paramsWithStateId);
        } else {
            LOGGER.debug("→ {}: Allow Retry is disabled, returning direct validation error", methodName);
            telemetryHelper.recordError(metricName, startTime, new Exception("Validation failed"), telemetryTags);
            return responseGenerator.generateValidationErrorResponse(validationResults);
        }
    }

    /**
     * Builds a system error response from an exception.
     *
     * @param cause              The exception that caused the error
     * @param errorMessagePrefix The error message prefix
     * @param metricName         The telemetry metric name
     * @param startTime          The operation start time
     * @param telemetryTags      The telemetry tags
     * @param methodName         The method name for logging
     * @return The failure ExtensionResponse
     */
    private ExtensionResponse buildSystemErrorResponse(
            Exception cause,
            String errorMessagePrefix,
            String metricName,
            long startTime,
            Map<String, String> telemetryTags,
            String methodName) {

        LOGGER.error("→ {}: Exception occurred - {}", methodName, cause.getMessage(), cause);
        telemetryHelper.recordError(metricName, startTime, cause, telemetryTags);

        ExtensionResponse.Error error = new ExtensionResponse.Error(
                errorMessagePrefix + cause.getMessage(),
                System.currentTimeMillis(),
                ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR,
                ""
        );

        LOGGER.debug("→ {}: Created error response for exception", methodName);
        return new ExtensionResponseBuilder().failure(error).build();
    }

    /**
     * Builds a graceful error response that returns success with error information.
     * Used for operations that should not fail hard but instead return empty results with error details.
     *
     * @param cause           The exception that caused the error
     * @param metricName      The telemetry metric name
     * @param startTime       The operation start time
     * @param telemetryTags   The telemetry tags
     * @param methodName      The method name for logging
     * @param emptyResultData The empty result data to include (e.g., empty lists, zero counts)
     * @return The success ExtensionResponse with error information
     */
    private ExtensionResponse buildGracefulErrorResponse(
            Exception cause,
            String metricName,
            long startTime,
            Map<String, String> telemetryTags,
            String methodName,
            Map<String, Object> emptyResultData) {

        LOGGER.error("→ {}: Error occurred - {}", methodName, cause.getMessage(), cause);
        telemetryHelper.recordError(metricName, startTime, cause, telemetryTags);

        Map<String, Object> errorResult = new HashMap<>(emptyResultData);
        errorResult.put("Success", false);
        errorResult.put("Error", cause.getMessage());

        return new ExtensionResponseBuilder().success(errorResult).build();
    }

}