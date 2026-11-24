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
import app.krista.ksdk.context.RequestContext;
import app.krista.ksdk.files.FileRepository;
import app.krista.model.base.File;
import com.kristasoft.extensions.essentials.collaboration.confluence.ConfluenceAttributes;
import com.kristasoft.extensions.essentials.collaboration.confluence.client.ConfluenceRESTClient;
import com.kristasoft.extensions.essentials.collaboration.confluence.api.ConfluenceAuditService;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.entities.Attachment;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.entities.Content;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.entities.Page;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.errorhandlers.ErrorHandlingStateManager;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.errorhandlers.ExtensionResponseGenerator;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.stores.Pair;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.validators.ValidationOrchestrator;
import com.kristasoft.extensions.essentials.collaboration.confluence.service.ConfluenceService;
import com.kristasoft.extensions.essentials.collaboration.confluence.service.PageDocxService;
import com.kristasoft.extensions.essentials.collaboration.confluence.service.PageMarkdownService;
import com.kristasoft.extensions.essentials.collaboration.confluence.utils.Constants;
import com.kristasoft.extensions.essentials.collaboration.confluence.utils.TelemetryHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for {@link PageArea} targeting 80%+ code coverage.
 *
 * <h2>Methods covered (14 total)</h2>
 * <pre>
 * 1. createPageFromContent         - validation + retry pattern
 * 2. createPageFromTemplate        - validation + retry pattern
 * 3. getPageByID                   - validation + retry pattern
 * 4. getPagesInSpace               - validation + retry pattern, optional title filter
 * 5. updatePageByContent           - validation + retry pattern
 * 6. updatePageFromTemplate        - validation + retry pattern
 * 7. getPageInSpace                - validation + retry pattern, downloads file
 * 8. getDirectChildPagesOfPage     - simple (no validation orchestrator)
 * 9. checkIfPageHasChildren        - simple (no validation orchestrator)
 * 10. getAllNestedDescendantsOfPage - simple (no validation orchestrator)
 * 11. getAncestorsOfPage           - simple (no validation orchestrator)
 * 12. getAttachmentsForPage        - validation + retry pattern
 * 13. getAttachmentByID            - validation + retry pattern, HTTP response
 * 14. downloadAttachment           - link validation, HTTP download
 * </pre>
 *
 * <p>Each method tests at minimum: happy path, MustAuthorizeException, general Exception.
 * Methods with validation+retry also test: validation fail with allowRetry=true, and
 * validation fail with allowRetry=false/null.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PageArea Tests")
class PageAreaTest {

    @Mock
    private ConfluenceService service;

    @Mock
    private ValidationOrchestrator validationOrchestrator;

    @Mock
    private ErrorHandlingStateManager errorHandlingStateManager;

    @Mock
    private ExtensionResponseGenerator responseGenerator;

    @Mock
    private PageDocxService pageDocxService;

    @Mock
    private TelemetryHelper telemetryHelper;

    @Mock
    private PageAreaAttachmentHelper attachmentHelper;

    @Mock
    private FileRepository fileRepository;

    @Mock
    private ConfluenceAuditService confluenceAuditService;

    @Mock
    private RequestContext requestContext;

    @Mock
    private PageMarkdownService pageMarkdownService;

    private PageArea pageArea;

    private static final String INVOKER_ID = "test-invoker-id";

    @BeforeEach
    void setUp() {
        // Lenient: used by nearly every request test, but not by the pure-logic
        // toDownloadPath tests, which would otherwise trip strict-stub checking.
        lenient().when(requestContext.getInvokerId()).thenReturn(INVOKER_ID);
        pageArea = new PageArea(service, validationOrchestrator, errorHandlingStateManager,
                responseGenerator, pageDocxService, pageMarkdownService, telemetryHelper,
                attachmentHelper, fileRepository, confluenceAuditService, requestContext);
    }

    // ── Helper methods ─────────────────────────────────────────────────────────

    private com.kristasoft.extensions.essentials.collaboration.confluence.dto.Page createDtoPage(
            String id, String title) {
        var page = new com.kristasoft.extensions.essentials.collaboration.confluence.dto.Page.PageBuilder(id, title)
                .status("current")
                .type("page")
                .spaceId("space-1")
                .build();
        page.setDomainUrl("https://test.atlassian.net/wiki");
        page.setLinks(Map.of("webui", "/spaces/space-1/pages/" + id));
        return page;
    }

    private ValidationOrchestrator.ValidationResult createValidationResult() {
        return new ValidationOrchestrator.ValidationResult(
                "Please confirm the value",
                "Field Name",
                "Enter the value",
                "Invalid value provided",
                "Identifier",
                Map.of("visualWidth", "S")
        );
    }

    // ════════════════════════════════════════════════════════════════════════════
    //  1. createPageFromContent
    // ════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("createPageFromContent")
    class CreatePageFromContent {

        @Test
        @DisplayName("Happy path: validation passes, creates page successfully")
        void happyPath_validationPasses_createsPage() {
            String spaceId = "space-1";
            String pageTitle = "New Page";
            List<Content> content = List.of();

            when(validationOrchestrator.validate(anyMap(), eq(PageArea.class), eq("createPageFromContent")))
                    .thenReturn(Collections.emptyList());
            when(service.createPageFromContent(spaceId, pageTitle, content))
                    .thenReturn(createDtoPage("pg-1", "New Page"));

            ExtensionResponse result = pageArea.createPageFromContent(pageTitle, spaceId, content, null);

            assertNotNull(result);
            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());
            Object pageObj = result.getResponseValue().get("Page");
            assertNotNull(pageObj);
            assertInstanceOf(Page.class, pageObj);
            assertEquals("pg-1", ((Page) pageObj).id);

            verify(confluenceAuditService).triggered(INVOKER_ID, "Create Page from Content");
            verify(confluenceAuditService).success(INVOKER_ID, "Create Page from Content");
            verify(telemetryHelper).incrementCount("confluence.createPageFromContent");
            verify(telemetryHelper).recordSuccess(eq("confluence.createPageFromContent"), anyLong(), anyMap());
        }

        @Test
        @DisplayName("Validation fails with allowRetry=true: returns confirmation response")
        void validationFails_allowRetryTrue_returnsConfirmation() {
            List<ValidationOrchestrator.ValidationResult> validationResults = List.of(createValidationResult());
            when(validationOrchestrator.validate(anyMap(), eq(PageArea.class), eq("createPageFromContent")))
                    .thenReturn(validationResults);

            ExtensionResponse mockResponse = mock(ExtensionResponse.class);
            when(responseGenerator.generateConfirmationResponse(
                    eq(ExtensionResponse.Error.ExceptionType.INPUT_ERROR),
                    eq(validationResults),
                    eq(Constants.CONFIRM_REENTER_CREATE_PAGE_FROM_CONTENT),
                    anyMap()
            )).thenReturn(mockResponse);

            ExtensionResponse result = pageArea.createPageFromContent("title", "bad-space", List.of(), true);

            assertSame(mockResponse, result);
            verify(errorHandlingStateManager).put(anyString(), anyMap());
            verify(telemetryHelper).recordRetryPrompted(eq("confluence.createPageFromContent"), anyLong(), anyMap());
        }

        @Test
        @DisplayName("Validation fails with allowRetry=false: returns validation error")
        void validationFails_allowRetryFalse_returnsValidationError() {
            List<ValidationOrchestrator.ValidationResult> validationResults = List.of(createValidationResult());
            when(validationOrchestrator.validate(anyMap(), eq(PageArea.class), eq("createPageFromContent")))
                    .thenReturn(validationResults);

            ExtensionResponse mockResponse = mock(ExtensionResponse.class);
            when(responseGenerator.generateValidationErrorResponse(validationResults)).thenReturn(mockResponse);

            ExtensionResponse result = pageArea.createPageFromContent("title", "bad-space", List.of(), false);

            assertSame(mockResponse, result);
            verify(responseGenerator).generateValidationErrorResponse(validationResults);
            verify(telemetryHelper).recordError(eq("confluence.createPageFromContent"), anyLong(), any(Exception.class), anyMap());
        }

        @Test
        @DisplayName("MustAuthorizeException: calls authFailure and rethrows")
        void mustAuthorizeException_rethrows() {
            MustAuthorizeException authEx = new MustAuthorizeException("Not authorized");
            when(validationOrchestrator.validate(anyMap(), eq(PageArea.class), eq("createPageFromContent")))
                    .thenThrow(authEx);

            MustAuthorizeException thrown = assertThrows(MustAuthorizeException.class,
                    () -> pageArea.createPageFromContent("title", "space-1", List.of(), null));

            assertSame(authEx, thrown);
            verify(confluenceAuditService).authFailure(INVOKER_ID, "Create Page from Content", authEx);
            verify(confluenceAuditService, never()).success(anyString(), anyString());
        }

        @Test
        @DisplayName("General Exception: returns failure response")
        void generalException_returnsFailure() {
            RuntimeException exception = new RuntimeException("Service error");
            when(validationOrchestrator.validate(anyMap(), eq(PageArea.class), eq("createPageFromContent")))
                    .thenReturn(Collections.emptyList());
            when(service.createPageFromContent(anyString(), anyString(), anyList()))
                    .thenThrow(exception);

            ExtensionResponse result = pageArea.createPageFromContent("title", "space-1", List.of(), null);

            assertNotNull(result);
            assertEquals(ExtensionResponse.Result.FAILURE, result.getResult());
            assertNotNull(result.getError());
            assertEquals(ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR, result.getError().getExceptionType());
            assertTrue(result.getError().getErrorMessage().contains("Service error"));

            verify(confluenceAuditService).failure(INVOKER_ID, "Create Page from Content", exception);
            verify(confluenceAuditService, never()).success(anyString(), anyString());
        }

        @Test
        @DisplayName("Null parameters in confirmation: handles null pageTitle, spaceID, content")
        void nullParams_handledInConfirmation() {
            List<ValidationOrchestrator.ValidationResult> validationResults = List.of(createValidationResult());
            when(validationOrchestrator.validate(anyMap(), eq(PageArea.class), eq("createPageFromContent")))
                    .thenReturn(validationResults);

            ExtensionResponse mockResponse = mock(ExtensionResponse.class);
            when(responseGenerator.generateConfirmationResponse(any(), any(), anyString(), anyMap()))
                    .thenReturn(mockResponse);

            ExtensionResponse result = pageArea.createPageFromContent(null, null, null, true);

            assertSame(mockResponse, result);
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    //  2. createPageFromTemplate
    // ════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("createPageFromTemplate")
    class CreatePageFromTemplate {

        @Test
        @DisplayName("Happy path: validation passes, creates page from template")
        void happyPath_validationPasses_createsPage() {
            when(validationOrchestrator.validate(anyMap(), eq(PageArea.class), eq("createPageFromTemplate")))
                    .thenReturn(Collections.emptyList());
            when(service.createPageFromTemplate("space-1", "tmpl-1", "New Page", List.of()))
                    .thenReturn(createDtoPage("pg-1", "New Page"));

            ExtensionResponse result = pageArea.createPageFromTemplate("space-1", "tmpl-1", "New Page", List.of(), null);

            assertNotNull(result);
            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());
            Page page = (Page) result.getResponseValue().get("Page");
            assertEquals("pg-1", page.id);

            verify(confluenceAuditService).triggered(INVOKER_ID, "Create Page from Template");
            verify(confluenceAuditService).success(INVOKER_ID, "Create Page from Template");
            verify(telemetryHelper).recordSuccess(eq("confluence.createPageFromTemplate"), anyLong(), anyMap());
        }

        @Test
        @DisplayName("Validation fails with allowRetry=true: returns confirmation response")
        void validationFails_allowRetryTrue_returnsConfirmation() {
            List<ValidationOrchestrator.ValidationResult> validationResults = List.of(createValidationResult());
            when(validationOrchestrator.validate(anyMap(), eq(PageArea.class), eq("createPageFromTemplate")))
                    .thenReturn(validationResults);

            ExtensionResponse mockResponse = mock(ExtensionResponse.class);
            when(responseGenerator.generateConfirmationResponse(any(), any(),
                    eq(Constants.CONFIRM_REENTER_CREATE_PAGE_FROM_TEMPLATE), anyMap()))
                    .thenReturn(mockResponse);

            ExtensionResponse result = pageArea.createPageFromTemplate("space-1", "tmpl-1", "title", List.of(), true);

            assertSame(mockResponse, result);
            verify(errorHandlingStateManager).put(anyString(), anyMap());
        }

        @Test
        @DisplayName("Validation fails with allowRetry=false: returns validation error")
        void validationFails_allowRetryFalse_returnsValidationError() {
            List<ValidationOrchestrator.ValidationResult> validationResults = List.of(createValidationResult());
            when(validationOrchestrator.validate(anyMap(), eq(PageArea.class), eq("createPageFromTemplate")))
                    .thenReturn(validationResults);

            ExtensionResponse mockResponse = mock(ExtensionResponse.class);
            when(responseGenerator.generateValidationErrorResponse(validationResults)).thenReturn(mockResponse);

            ExtensionResponse result = pageArea.createPageFromTemplate("space-1", "tmpl-1", "title", List.of(), false);

            assertSame(mockResponse, result);
            verify(responseGenerator).generateValidationErrorResponse(validationResults);
        }

        @Test
        @DisplayName("MustAuthorizeException: calls authFailure and rethrows")
        void mustAuthorizeException_rethrows() {
            MustAuthorizeException authEx = new MustAuthorizeException("Auth needed");
            when(validationOrchestrator.validate(anyMap(), eq(PageArea.class), eq("createPageFromTemplate")))
                    .thenThrow(authEx);

            MustAuthorizeException thrown = assertThrows(MustAuthorizeException.class,
                    () -> pageArea.createPageFromTemplate("space-1", "tmpl-1", "title", List.of(), null));

            assertSame(authEx, thrown);
            verify(confluenceAuditService).authFailure(INVOKER_ID, "Create Page from Template", authEx);
            verify(confluenceAuditService, never()).success(anyString(), anyString());
        }

        @Test
        @DisplayName("General Exception: returns failure response")
        void generalException_returnsFailure() {
            RuntimeException exception = new RuntimeException("Template error");
            when(validationOrchestrator.validate(anyMap(), eq(PageArea.class), eq("createPageFromTemplate")))
                    .thenReturn(Collections.emptyList());
            when(service.createPageFromTemplate(anyString(), anyString(), anyString(), anyList()))
                    .thenThrow(exception);

            ExtensionResponse result = pageArea.createPageFromTemplate("space-1", "tmpl-1", "title", List.of(), null);

            assertNotNull(result);
            assertEquals(ExtensionResponse.Result.FAILURE, result.getResult());
            assertTrue(result.getError().getErrorMessage().contains("Template error"));
            verify(confluenceAuditService).failure(INVOKER_ID, "Create Page from Template", exception);
            verify(confluenceAuditService, never()).success(anyString(), anyString());
        }

        @Test
        @DisplayName("Null parameters in confirmation: handles null spaceID, templateID, pageTitle, variables")
        void nullParams_handledInConfirmation() {
            List<ValidationOrchestrator.ValidationResult> validationResults = List.of(createValidationResult());
            when(validationOrchestrator.validate(anyMap(), eq(PageArea.class), eq("createPageFromTemplate")))
                    .thenReturn(validationResults);

            ExtensionResponse mockResponse = mock(ExtensionResponse.class);
            when(responseGenerator.generateConfirmationResponse(any(), any(), anyString(), anyMap()))
                    .thenReturn(mockResponse);

            ExtensionResponse result = pageArea.createPageFromTemplate(null, null, null, null, true);

            assertSame(mockResponse, result);
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    //  3. getPageByID
    // ════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getPageByID")
    class GetPageByID {

        @Test
        @DisplayName("Happy path: validation passes, returns Page entity")
        void happyPath_validationPasses_returnsPage() {
            when(validationOrchestrator.validate(anyMap(), eq(PageArea.class), eq("getPageByID")))
                    .thenReturn(Collections.emptyList());
            when(service.getPageById("pg-1")).thenReturn(createDtoPage("pg-1", "Test Page"));

            ExtensionResponse result = pageArea.getPageByID("pg-1", null);

            assertNotNull(result);
            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());
            Page page = (Page) result.getResponseValue().get("Page");
            assertNotNull(page);
            assertEquals("pg-1", page.id);
            assertEquals("Test Page", page.title);

            verify(confluenceAuditService).triggered(INVOKER_ID, "Get Page By ID");
            verify(confluenceAuditService).success(INVOKER_ID, "Get Page By ID");
            verify(telemetryHelper).incrementCount("confluence.getPageByID");
            verify(telemetryHelper).recordSuccess(eq("confluence.getPageByID"), anyLong(), anyMap());
        }

        @Test
        @DisplayName("Validation fails with allowRetry=true: returns confirmation response")
        void validationFails_allowRetryTrue_returnsConfirmation() {
            List<ValidationOrchestrator.ValidationResult> validationResults = List.of(createValidationResult());
            when(validationOrchestrator.validate(anyMap(), eq(PageArea.class), eq("getPageByID")))
                    .thenReturn(validationResults);

            ExtensionResponse mockResponse = mock(ExtensionResponse.class);
            when(responseGenerator.generateConfirmationResponse(any(), any(),
                    eq(Constants.CONFIRM_REENTER_GET_PAGE_BY_ID), anyMap()))
                    .thenReturn(mockResponse);

            ExtensionResponse result = pageArea.getPageByID("invalid", true);

            assertSame(mockResponse, result);
            verify(errorHandlingStateManager).put(anyString(), anyMap());
            verify(telemetryHelper).recordRetryPrompted(eq("confluence.getPageByID"), anyLong(), anyMap());
        }

        @Test
        @DisplayName("Validation fails with allowRetry=false: returns validation error")
        void validationFails_allowRetryFalse_returnsValidationError() {
            List<ValidationOrchestrator.ValidationResult> validationResults = List.of(createValidationResult());
            when(validationOrchestrator.validate(anyMap(), eq(PageArea.class), eq("getPageByID")))
                    .thenReturn(validationResults);

            ExtensionResponse mockResponse = mock(ExtensionResponse.class);
            when(responseGenerator.generateValidationErrorResponse(validationResults)).thenReturn(mockResponse);

            ExtensionResponse result = pageArea.getPageByID("invalid", false);

            assertSame(mockResponse, result);
            verify(responseGenerator).generateValidationErrorResponse(validationResults);
        }

        @Test
        @DisplayName("Validation fails with allowRetry=null: returns validation error")
        void validationFails_allowRetryNull_returnsValidationError() {
            List<ValidationOrchestrator.ValidationResult> validationResults = List.of(createValidationResult());
            when(validationOrchestrator.validate(anyMap(), eq(PageArea.class), eq("getPageByID")))
                    .thenReturn(validationResults);

            ExtensionResponse mockResponse = mock(ExtensionResponse.class);
            when(responseGenerator.generateValidationErrorResponse(validationResults)).thenReturn(mockResponse);

            ExtensionResponse result = pageArea.getPageByID("invalid", null);

            assertSame(mockResponse, result);
        }

        @Test
        @DisplayName("Null pageID: validation param not added")
        void nullPageId_validationParamNotAdded() {
            when(validationOrchestrator.validate(anyMap(), eq(PageArea.class), eq("getPageByID")))
                    .thenReturn(Collections.emptyList());
            when(service.getPageById(null)).thenReturn(createDtoPage("null-id", "Null Page"));

            ExtensionResponse result = pageArea.getPageByID(null, null);

            assertNotNull(result);
            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());
        }

        @Test
        @DisplayName("MustAuthorizeException: calls authFailure and rethrows")
        void mustAuthorizeException_rethrows() {
            MustAuthorizeException authEx = new MustAuthorizeException("Not authorized");
            when(validationOrchestrator.validate(anyMap(), eq(PageArea.class), eq("getPageByID")))
                    .thenThrow(authEx);

            MustAuthorizeException thrown = assertThrows(MustAuthorizeException.class,
                    () -> pageArea.getPageByID("pg-1", null));

            assertSame(authEx, thrown);
            verify(confluenceAuditService).authFailure(INVOKER_ID, "Get Page By ID", authEx);
            verify(confluenceAuditService, never()).success(anyString(), anyString());
        }

        @Test
        @DisplayName("General Exception: returns failure response")
        void generalException_returnsFailure() {
            RuntimeException exception = new RuntimeException("Page not found");
            when(validationOrchestrator.validate(anyMap(), eq(PageArea.class), eq("getPageByID")))
                    .thenReturn(Collections.emptyList());
            when(service.getPageById("bad")).thenThrow(exception);

            ExtensionResponse result = pageArea.getPageByID("bad", null);

            assertNotNull(result);
            assertEquals(ExtensionResponse.Result.FAILURE, result.getResult());
            assertTrue(result.getError().getErrorMessage().contains("Page not found"));
            verify(confluenceAuditService).failure(INVOKER_ID, "Get Page By ID", exception);
            verify(confluenceAuditService, never()).success(anyString(), anyString());
            verify(telemetryHelper).recordError(eq("confluence.getPageByID"), anyLong(), eq(exception), anyMap());
        }

        @Test
        @DisplayName("Null pageID in confirmation params: uses empty string")
        void nullPageIdInConfirmation() {
            List<ValidationOrchestrator.ValidationResult> validationResults = List.of(createValidationResult());
            when(validationOrchestrator.validate(anyMap(), eq(PageArea.class), eq("getPageByID")))
                    .thenReturn(validationResults);

            ExtensionResponse mockResponse = mock(ExtensionResponse.class);
            when(responseGenerator.generateConfirmationResponse(any(), any(), anyString(), anyMap()))
                    .thenReturn(mockResponse);

            ExtensionResponse result = pageArea.getPageByID(null, true);

            assertSame(mockResponse, result);
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    //  4. getPagesInSpace
    // ════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getPagesInSpace")
    class GetPagesInSpace {

        @Test
        @DisplayName("Happy path without title filter: returns all pages")
        void happyPath_noTitleFilter_returnsAllPages() {
            when(validationOrchestrator.validate(anyMap(), eq(PageArea.class), eq("getPagesInSpace")))
                    .thenReturn(Collections.emptyList());
            com.kristasoft.extensions.essentials.collaboration.confluence.dto.Page[] dtoPages = {
                    createDtoPage("p-1", "Page One"),
                    createDtoPage("p-2", "Page Two")
            };
            when(service.getPagesBySpaceId("space-1")).thenReturn(dtoPages);

            ExtensionResponse result = pageArea.getPagesInSpace("space-1", null, null);

            assertNotNull(result);
            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());

            @SuppressWarnings("unchecked")
            List<Page> pages = (List<Page>) result.getResponseValue().get("Pages");
            assertEquals(2, pages.size());

            @SuppressWarnings("unchecked")
            List<String> identifiers = (List<String>) result.getResponseValue().get("Page Identifiers");
            assertEquals(2, identifiers.size());

            verify(confluenceAuditService).triggered(INVOKER_ID, "Get Pages in Space");
            verify(confluenceAuditService).success(INVOKER_ID, "Get Pages in Space");
        }

        @Test
        @DisplayName("Happy path with title filter: returns filtered pages")
        void happyPath_withTitleFilter_returnsFilteredPages() {
            when(validationOrchestrator.validate(anyMap(), eq(PageArea.class), eq("getPagesInSpace")))
                    .thenReturn(Collections.emptyList());
            com.kristasoft.extensions.essentials.collaboration.confluence.dto.Page[] dtoPages = {
                    createDtoPage("p-1", "Target Page"),
                    createDtoPage("p-2", "Other Page")
            };
            when(service.getPagesBySpaceId("space-1")).thenReturn(dtoPages);

            ExtensionResponse result = pageArea.getPagesInSpace("space-1", "Target Page", null);

            assertNotNull(result);
            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());

            @SuppressWarnings("unchecked")
            List<Page> pages = (List<Page>) result.getResponseValue().get("Pages");
            assertEquals(1, pages.size());
            assertEquals("Target Page", pages.get(0).title);
        }

        @Test
        @DisplayName("Happy path with blank title filter: returns all pages")
        void happyPath_blankTitleFilter_returnsAllPages() {
            when(validationOrchestrator.validate(anyMap(), eq(PageArea.class), eq("getPagesInSpace")))
                    .thenReturn(Collections.emptyList());
            com.kristasoft.extensions.essentials.collaboration.confluence.dto.Page[] dtoPages = {
                    createDtoPage("p-1", "Page One")
            };
            when(service.getPagesBySpaceId("space-1")).thenReturn(dtoPages);

            ExtensionResponse result = pageArea.getPagesInSpace("space-1", "   ", null);

            @SuppressWarnings("unchecked")
            List<Page> pages = (List<Page>) result.getResponseValue().get("Pages");
            assertEquals(1, pages.size());
        }

        @Test
        @DisplayName("Validation fails with allowRetry=true: returns confirmation response")
        void validationFails_allowRetryTrue_returnsConfirmation() {
            List<ValidationOrchestrator.ValidationResult> validationResults = List.of(createValidationResult());
            when(validationOrchestrator.validate(anyMap(), eq(PageArea.class), eq("getPagesInSpace")))
                    .thenReturn(validationResults);

            ExtensionResponse mockResponse = mock(ExtensionResponse.class);
            when(responseGenerator.generateConfirmationResponse(any(), any(),
                    eq(Constants.CONFIRM_REENTER_GET_PAGES_IN_SPACE), anyMap()))
                    .thenReturn(mockResponse);

            ExtensionResponse result = pageArea.getPagesInSpace("bad", "title", true);

            assertSame(mockResponse, result);
        }

        @Test
        @DisplayName("Validation fails with allowRetry=false: returns validation error")
        void validationFails_allowRetryFalse_returnsValidationError() {
            List<ValidationOrchestrator.ValidationResult> validationResults = List.of(createValidationResult());
            when(validationOrchestrator.validate(anyMap(), eq(PageArea.class), eq("getPagesInSpace")))
                    .thenReturn(validationResults);

            ExtensionResponse mockResponse = mock(ExtensionResponse.class);
            when(responseGenerator.generateValidationErrorResponse(validationResults)).thenReturn(mockResponse);

            ExtensionResponse result = pageArea.getPagesInSpace("bad", "title", false);

            assertSame(mockResponse, result);
        }

        @Test
        @DisplayName("MustAuthorizeException: calls authFailure and rethrows")
        void mustAuthorizeException_rethrows() {
            MustAuthorizeException authEx = new MustAuthorizeException("Auth required");
            when(validationOrchestrator.validate(anyMap(), eq(PageArea.class), eq("getPagesInSpace")))
                    .thenThrow(authEx);

            assertThrows(MustAuthorizeException.class,
                    () -> pageArea.getPagesInSpace("space-1", null, null));

            verify(confluenceAuditService).authFailure(INVOKER_ID, "Get Pages in Space", authEx);
            verify(confluenceAuditService, never()).success(anyString(), anyString());
        }

        @Test
        @DisplayName("General Exception: returns failure response")
        void generalException_returnsFailure() {
            RuntimeException exception = new RuntimeException("Space error");
            when(validationOrchestrator.validate(anyMap(), eq(PageArea.class), eq("getPagesInSpace")))
                    .thenReturn(Collections.emptyList());
            when(service.getPagesBySpaceId("bad")).thenThrow(exception);

            ExtensionResponse result = pageArea.getPagesInSpace("bad", null, null);

            assertEquals(ExtensionResponse.Result.FAILURE, result.getResult());
            assertTrue(result.getError().getErrorMessage().contains("Space error"));
            verify(confluenceAuditService).failure(INVOKER_ID, "Get Pages in Space", exception);
        }

        @Test
        @DisplayName("Null spaceId and title in confirmation params: uses empty strings")
        void nullParams_inConfirmation() {
            List<ValidationOrchestrator.ValidationResult> validationResults = List.of(createValidationResult());
            when(validationOrchestrator.validate(anyMap(), eq(PageArea.class), eq("getPagesInSpace")))
                    .thenReturn(validationResults);

            ExtensionResponse mockResponse = mock(ExtensionResponse.class);
            when(responseGenerator.generateConfirmationResponse(any(), any(), anyString(), anyMap()))
                    .thenReturn(mockResponse);

            ExtensionResponse result = pageArea.getPagesInSpace(null, null, true);

            assertSame(mockResponse, result);
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    //  5. updatePageByContent
    // ════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("updatePageByContent")
    class UpdatePageByContent {

        @Test
        @DisplayName("Happy path: validation passes, updates page successfully")
        void happyPath_validationPasses_updatesPage() {
            when(validationOrchestrator.validate(anyMap(), eq(PageArea.class), eq("updatePageByContent")))
                    .thenReturn(Collections.emptyList());
            when(service.updatePageFromContent("pg-1", "current", "Updated Title", "comment", List.of()))
                    .thenReturn(createDtoPage("pg-1", "Updated Title"));

            ExtensionResponse result = pageArea.updatePageByContent("pg-1", "Updated Title", "current", "comment", List.of(), null);

            assertNotNull(result);
            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());
            Page page = (Page) result.getResponseValue().get("Page");
            assertEquals("pg-1", page.id);

            verify(confluenceAuditService).triggered(INVOKER_ID, "Update Page by Content");
            verify(confluenceAuditService).success(INVOKER_ID, "Update Page by Content");
        }

        @Test
        @DisplayName("Validation fails with allowRetry=true: returns confirmation response")
        void validationFails_allowRetryTrue_returnsConfirmation() {
            List<ValidationOrchestrator.ValidationResult> validationResults = List.of(createValidationResult());
            when(validationOrchestrator.validate(anyMap(), eq(PageArea.class), eq("updatePageByContent")))
                    .thenReturn(validationResults);

            ExtensionResponse mockResponse = mock(ExtensionResponse.class);
            when(responseGenerator.generateConfirmationResponse(any(), any(),
                    eq(Constants.CONFIRM_REENTER_UPDATE_PAGE_BY_CONTENT), anyMap()))
                    .thenReturn(mockResponse);

            ExtensionResponse result = pageArea.updatePageByContent("pg-1", "title", "current", "comment", List.of(), true);

            assertSame(mockResponse, result);
        }

        @Test
        @DisplayName("Validation fails with allowRetry=false: returns validation error")
        void validationFails_allowRetryFalse_returnsValidationError() {
            List<ValidationOrchestrator.ValidationResult> validationResults = List.of(createValidationResult());
            when(validationOrchestrator.validate(anyMap(), eq(PageArea.class), eq("updatePageByContent")))
                    .thenReturn(validationResults);

            ExtensionResponse mockResponse = mock(ExtensionResponse.class);
            when(responseGenerator.generateValidationErrorResponse(validationResults)).thenReturn(mockResponse);

            ExtensionResponse result = pageArea.updatePageByContent("pg-1", "title", "current", null, null, false);

            assertSame(mockResponse, result);
        }

        @Test
        @DisplayName("MustAuthorizeException: calls authFailure and rethrows")
        void mustAuthorizeException_rethrows() {
            MustAuthorizeException authEx = new MustAuthorizeException("Auth required");
            when(validationOrchestrator.validate(anyMap(), eq(PageArea.class), eq("updatePageByContent")))
                    .thenThrow(authEx);

            assertThrows(MustAuthorizeException.class,
                    () -> pageArea.updatePageByContent("pg-1", "title", "current", null, null, null));

            verify(confluenceAuditService).authFailure(INVOKER_ID, "Update Page by Content", authEx);
            verify(confluenceAuditService, never()).success(anyString(), anyString());
        }

        @Test
        @DisplayName("General Exception: returns failure response")
        void generalException_returnsFailure() {
            RuntimeException exception = new RuntimeException("Update failed");
            when(validationOrchestrator.validate(anyMap(), eq(PageArea.class), eq("updatePageByContent")))
                    .thenReturn(Collections.emptyList());
            when(service.updatePageFromContent(anyString(), any(), any(), any(), any()))
                    .thenThrow(exception);

            ExtensionResponse result = pageArea.updatePageByContent("pg-1", "title", "current", null, null, null);

            assertEquals(ExtensionResponse.Result.FAILURE, result.getResult());
            assertTrue(result.getError().getErrorMessage().contains("Update failed"));
            verify(confluenceAuditService).failure(INVOKER_ID, "Update Page by Content", exception);
        }

        @Test
        @DisplayName("Null parameters in confirmation: handles all null optional fields")
        void nullParams_handledInConfirmation() {
            List<ValidationOrchestrator.ValidationResult> validationResults = List.of(createValidationResult());
            when(validationOrchestrator.validate(anyMap(), eq(PageArea.class), eq("updatePageByContent")))
                    .thenReturn(validationResults);

            ExtensionResponse mockResponse = mock(ExtensionResponse.class);
            when(responseGenerator.generateConfirmationResponse(any(), any(), anyString(), anyMap()))
                    .thenReturn(mockResponse);

            ExtensionResponse result = pageArea.updatePageByContent(null, null, null, null, null, true);

            assertSame(mockResponse, result);
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    //  6. updatePageFromTemplate
    // ════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("updatePageFromTemplate")
    class UpdatePageFromTemplate {

        @Test
        @DisplayName("Happy path: validation passes, updates page from template")
        void happyPath_validationPasses_updatesPage() {
            when(validationOrchestrator.validate(anyMap(), eq(PageArea.class), eq("updatePageFromTemplate")))
                    .thenReturn(Collections.emptyList());
            when(service.updatePageFromTemplate("pg-1", "tmpl-1", "current", "New Title", "comment", List.of()))
                    .thenReturn(createDtoPage("pg-1", "New Title"));

            ExtensionResponse result = pageArea.updatePageFromTemplate("pg-1", "tmpl-1", "current", "New Title", "comment", List.of(), null);

            assertNotNull(result);
            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());
            Page page = (Page) result.getResponseValue().get("Page");
            assertEquals("pg-1", page.id);

            verify(confluenceAuditService).triggered(INVOKER_ID, "Update Page from Template");
            verify(confluenceAuditService).success(INVOKER_ID, "Update Page from Template");
        }

        @Test
        @DisplayName("Validation fails with allowRetry=true: returns confirmation response")
        void validationFails_allowRetryTrue_returnsConfirmation() {
            List<ValidationOrchestrator.ValidationResult> validationResults = List.of(createValidationResult());
            when(validationOrchestrator.validate(anyMap(), eq(PageArea.class), eq("updatePageFromTemplate")))
                    .thenReturn(validationResults);

            ExtensionResponse mockResponse = mock(ExtensionResponse.class);
            when(responseGenerator.generateConfirmationResponse(any(), any(),
                    eq(Constants.CONFIRM_REENTER_UPDATE_PAGE_FROM_TEMPLATE), anyMap()))
                    .thenReturn(mockResponse);

            ExtensionResponse result = pageArea.updatePageFromTemplate("pg-1", "tmpl-1", "current", "title", "comment", List.of(), true);

            assertSame(mockResponse, result);
        }

        @Test
        @DisplayName("Validation fails with allowRetry=false: returns validation error")
        void validationFails_allowRetryFalse_returnsValidationError() {
            List<ValidationOrchestrator.ValidationResult> validationResults = List.of(createValidationResult());
            when(validationOrchestrator.validate(anyMap(), eq(PageArea.class), eq("updatePageFromTemplate")))
                    .thenReturn(validationResults);

            ExtensionResponse mockResponse = mock(ExtensionResponse.class);
            when(responseGenerator.generateValidationErrorResponse(validationResults)).thenReturn(mockResponse);

            ExtensionResponse result = pageArea.updatePageFromTemplate("pg-1", "tmpl-1", null, null, null, null, false);

            assertSame(mockResponse, result);
        }

        @Test
        @DisplayName("MustAuthorizeException: calls authFailure and rethrows")
        void mustAuthorizeException_rethrows() {
            MustAuthorizeException authEx = new MustAuthorizeException("Auth needed");
            when(validationOrchestrator.validate(anyMap(), eq(PageArea.class), eq("updatePageFromTemplate")))
                    .thenThrow(authEx);

            assertThrows(MustAuthorizeException.class,
                    () -> pageArea.updatePageFromTemplate("pg-1", "tmpl-1", null, null, null, null, null));

            verify(confluenceAuditService).authFailure(INVOKER_ID, "Update Page from Template", authEx);
            verify(confluenceAuditService, never()).success(anyString(), anyString());
        }

        @Test
        @DisplayName("General Exception: returns failure response")
        void generalException_returnsFailure() {
            RuntimeException exception = new RuntimeException("Template update error");
            when(validationOrchestrator.validate(anyMap(), eq(PageArea.class), eq("updatePageFromTemplate")))
                    .thenReturn(Collections.emptyList());
            when(service.updatePageFromTemplate(anyString(), anyString(), any(), any(), any(), any()))
                    .thenThrow(exception);

            ExtensionResponse result = pageArea.updatePageFromTemplate("pg-1", "tmpl-1", null, null, null, null, null);

            assertEquals(ExtensionResponse.Result.FAILURE, result.getResult());
            assertTrue(result.getError().getErrorMessage().contains("Template update error"));
            verify(confluenceAuditService).failure(INVOKER_ID, "Update Page from Template", exception);
        }

        @Test
        @DisplayName("Null parameters in confirmation: handles all null optional fields")
        void nullParams_handledInConfirmation() {
            List<ValidationOrchestrator.ValidationResult> validationResults = List.of(createValidationResult());
            when(validationOrchestrator.validate(anyMap(), eq(PageArea.class), eq("updatePageFromTemplate")))
                    .thenReturn(validationResults);

            ExtensionResponse mockResponse = mock(ExtensionResponse.class);
            when(responseGenerator.generateConfirmationResponse(any(), any(), anyString(), anyMap()))
                    .thenReturn(mockResponse);

            ExtensionResponse result = pageArea.updatePageFromTemplate(null, null, null, null, null, null, true);

            assertSame(mockResponse, result);
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    //  7. getPageInSpace
    // ════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getPageInSpace")
    class GetPageInSpace {

        @Test
        @DisplayName("Happy path: validation passes, downloads page as file")
        void happyPath_validationPasses_downloadsFile() {
            when(validationOrchestrator.validate(anyMap(), eq(PageArea.class), eq("getPageInSpace")))
                    .thenReturn(Collections.emptyList());

            File mockFile = mock(File.class);
            when(pageDocxService.downloadPageAsDocxFileWithStatus("pg-1", "docx"))
                    .thenReturn(Pair.of(mockFile, true));

            ExtensionResponse result = pageArea.getPageInSpace("pg-1", "docx", null);

            assertNotNull(result);
            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());
            assertSame(mockFile, result.getResponseValue().get("Downloaded File"));
            assertEquals(true, result.getResponseValue().get("Parsing Success"));

            verify(confluenceAuditService).triggered(INVOKER_ID, "Get Page in Space");
            verify(confluenceAuditService).success(INVOKER_ID, "Get Page in Space");
            verify(telemetryHelper).recordSuccess(eq("confluence.getPageInSpace"), anyLong(), anyMap());
        }

        @Test
        @DisplayName("Happy path: parsing success is false")
        void happyPath_parsingFalse() {
            when(validationOrchestrator.validate(anyMap(), eq(PageArea.class), eq("getPageInSpace")))
                    .thenReturn(Collections.emptyList());

            File mockFile = mock(File.class);
            when(pageDocxService.downloadPageAsDocxFileWithStatus("pg-1", "docx"))
                    .thenReturn(Pair.of(mockFile, false));

            ExtensionResponse result = pageArea.getPageInSpace("pg-1", "docx", null);

            assertEquals(false, result.getResponseValue().get("Parsing Success"));
        }

        @Test
        @DisplayName("Validation fails with allowRetry=true: returns confirmation response")
        void validationFails_allowRetryTrue_returnsConfirmation() {
            List<ValidationOrchestrator.ValidationResult> validationResults = List.of(createValidationResult());
            when(validationOrchestrator.validate(anyMap(), eq(PageArea.class), eq("getPageInSpace")))
                    .thenReturn(validationResults);

            ExtensionResponse mockResponse = mock(ExtensionResponse.class);
            when(responseGenerator.generateConfirmationResponse(any(), any(),
                    eq(Constants.CONFIRM_REENTER_GET_PAGE_IN_SPACE), anyMap()))
                    .thenReturn(mockResponse);

            ExtensionResponse result = pageArea.getPageInSpace("pg-1", "docx", true);

            assertSame(mockResponse, result);
        }

        @Test
        @DisplayName("Validation fails with allowRetry=false: returns validation error")
        void validationFails_allowRetryFalse_returnsValidationError() {
            List<ValidationOrchestrator.ValidationResult> validationResults = List.of(createValidationResult());
            when(validationOrchestrator.validate(anyMap(), eq(PageArea.class), eq("getPageInSpace")))
                    .thenReturn(validationResults);

            ExtensionResponse mockResponse = mock(ExtensionResponse.class);
            when(responseGenerator.generateValidationErrorResponse(validationResults)).thenReturn(mockResponse);

            ExtensionResponse result = pageArea.getPageInSpace("pg-1", "docx", false);

            assertSame(mockResponse, result);
        }

        @Test
        @DisplayName("MustAuthorizeException: calls authFailure and rethrows")
        void mustAuthorizeException_rethrows() {
            MustAuthorizeException authEx = new MustAuthorizeException("Auth needed");
            when(validationOrchestrator.validate(anyMap(), eq(PageArea.class), eq("getPageInSpace")))
                    .thenThrow(authEx);

            assertThrows(MustAuthorizeException.class,
                    () -> pageArea.getPageInSpace("pg-1", "docx", null));

            verify(confluenceAuditService).authFailure(INVOKER_ID, "Get Page in Space", authEx);
        }

        @Test
        @DisplayName("General Exception: returns failure response")
        void generalException_returnsFailure() {
            RuntimeException exception = new RuntimeException("Download error");
            when(validationOrchestrator.validate(anyMap(), eq(PageArea.class), eq("getPageInSpace")))
                    .thenReturn(Collections.emptyList());
            when(pageDocxService.downloadPageAsDocxFileWithStatus(anyString(), anyString()))
                    .thenThrow(exception);

            ExtensionResponse result = pageArea.getPageInSpace("pg-1", "docx", null);

            assertEquals(ExtensionResponse.Result.FAILURE, result.getResult());
            assertTrue(result.getError().getErrorMessage().contains("Download error"));
            verify(confluenceAuditService).failure(INVOKER_ID, "Get Page in Space", exception);
        }

        @Test
        @DisplayName("Null parameters in confirmation: handles null pageID and exportFormat")
        void nullParams_handledInConfirmation() {
            List<ValidationOrchestrator.ValidationResult> validationResults = List.of(createValidationResult());
            when(validationOrchestrator.validate(anyMap(), eq(PageArea.class), eq("getPageInSpace")))
                    .thenReturn(validationResults);

            ExtensionResponse mockResponse = mock(ExtensionResponse.class);
            when(responseGenerator.generateConfirmationResponse(any(), any(), anyString(), anyMap()))
                    .thenReturn(mockResponse);

            ExtensionResponse result = pageArea.getPageInSpace(null, null, true);

            assertSame(mockResponse, result);
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    //  8. getDirectChildPagesOfPage
    // ════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getDirectChildPagesOfPage")
    class GetDirectChildPagesOfPage {

        @Test
        @DisplayName("Happy path: returns child pages with Success=true")
        void happyPath_returnsChildPages() {
            com.kristasoft.extensions.essentials.collaboration.confluence.dto.Page[] dtoPages = {
                    createDtoPage("child-1", "Child One"),
                    createDtoPage("child-2", "Child Two")
            };
            when(service.getDirectChildPagesOfPage("pg-1")).thenReturn(dtoPages);

            ExtensionResponse result = pageArea.getDirectChildPagesOfPage("pg-1");

            assertNotNull(result);
            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());

            @SuppressWarnings("unchecked")
            List<Page> pages = (List<Page>) result.getResponseValue().get(Constants.PAGES);
            assertEquals(2, pages.size());
            assertEquals("child-1", pages.get(0).id);
            assertEquals("child-2", pages.get(1).id);

            @SuppressWarnings("unchecked")
            List<String> identifiers = (List<String>) result.getResponseValue().get(Constants.PAGE_IDENTIFIERS);
            assertEquals(2, identifiers.size());

            assertEquals(true, result.getResponseValue().get("Success"));

            verify(confluenceAuditService).triggered(INVOKER_ID, "Get Direct Child Pages of Page");
            verify(confluenceAuditService).success(INVOKER_ID, "Get Direct Child Pages of Page");
            verify(telemetryHelper).recordSuccess(eq("confluence.getDirectChildPagesOfPage"), anyLong(), anyMap());
        }

        @Test
        @DisplayName("Happy path with empty result")
        void happyPath_emptyResult() {
            when(service.getDirectChildPagesOfPage("pg-1"))
                    .thenReturn(new com.kristasoft.extensions.essentials.collaboration.confluence.dto.Page[0]);

            ExtensionResponse result = pageArea.getDirectChildPagesOfPage("pg-1");

            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());

            @SuppressWarnings("unchecked")
            List<Page> pages = (List<Page>) result.getResponseValue().get(Constants.PAGES);
            assertEquals(0, pages.size());
            assertEquals(true, result.getResponseValue().get("Success"));
        }

        @Test
        @DisplayName("MustAuthorizeException: calls authFailure and rethrows")
        void mustAuthorizeException_rethrows() {
            MustAuthorizeException authEx = new MustAuthorizeException("Auth required");
            when(service.getDirectChildPagesOfPage("pg-1")).thenThrow(authEx);

            assertThrows(MustAuthorizeException.class,
                    () -> pageArea.getDirectChildPagesOfPage("pg-1"));

            verify(confluenceAuditService).authFailure(INVOKER_ID, "Get Direct Child Pages of Page", authEx);
            verify(confluenceAuditService, never()).success(anyString(), anyString());
        }

        @Test
        @DisplayName("General Exception: returns graceful error with Success=false")
        void generalException_returnsGracefulError() {
            RuntimeException exception = new RuntimeException("Child pages error");
            when(service.getDirectChildPagesOfPage("pg-1")).thenThrow(exception);

            ExtensionResponse result = pageArea.getDirectChildPagesOfPage("pg-1");

            assertNotNull(result);
            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());
            assertEquals(false, result.getResponseValue().get("Success"));
            assertEquals("Child pages error", result.getResponseValue().get("Error"));

            @SuppressWarnings("unchecked")
            List<?> pages = (List<?>) result.getResponseValue().get(Constants.PAGES);
            assertTrue(pages.isEmpty());

            @SuppressWarnings("unchecked")
            List<?> identifiers = (List<?>) result.getResponseValue().get(Constants.PAGE_IDENTIFIERS);
            assertTrue(identifiers.isEmpty());

            verify(confluenceAuditService).failure(INVOKER_ID, "Get Direct Child Pages of Page", exception);
            verify(confluenceAuditService, never()).success(anyString(), anyString());
            verify(telemetryHelper).recordError(eq("confluence.getDirectChildPagesOfPage"), anyLong(), eq(exception), anyMap());
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    //  9. checkIfPageHasChildren
    // ════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("checkIfPageHasChildren")
    class CheckIfPageHasChildren {

        @Test
        @DisplayName("Happy path: page has children, returns true")
        void happyPath_hasChildren_returnsTrue() {
            when(service.hasChildPages("pg-1")).thenReturn(true);

            ExtensionResponse result = pageArea.checkIfPageHasChildren("pg-1");

            assertNotNull(result);
            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());
            assertEquals(true, result.getResponseValue().get("Has Children"));
            assertEquals(true, result.getResponseValue().get("Success"));

            verify(confluenceAuditService).triggered(INVOKER_ID, "Check if Page Has Children");
            verify(confluenceAuditService).success(INVOKER_ID, "Check if Page Has Children");
            verify(telemetryHelper).recordSuccess(eq("confluence.checkIfPageHasChildren"), anyLong(), anyMap());
        }

        @Test
        @DisplayName("Happy path: page has no children, returns false")
        void happyPath_noChildren_returnsFalse() {
            when(service.hasChildPages("pg-1")).thenReturn(false);

            ExtensionResponse result = pageArea.checkIfPageHasChildren("pg-1");

            assertEquals(false, result.getResponseValue().get("Has Children"));
            assertEquals(true, result.getResponseValue().get("Success"));
        }

        @Test
        @DisplayName("MustAuthorizeException: calls authFailure and rethrows")
        void mustAuthorizeException_rethrows() {
            MustAuthorizeException authEx = new MustAuthorizeException("Auth needed");
            when(service.hasChildPages("pg-1")).thenThrow(authEx);

            assertThrows(MustAuthorizeException.class,
                    () -> pageArea.checkIfPageHasChildren("pg-1"));

            verify(confluenceAuditService).authFailure(INVOKER_ID, "Check if Page Has Children", authEx);
            verify(confluenceAuditService, never()).success(anyString(), anyString());
        }

        @Test
        @DisplayName("General Exception: returns graceful error with Has Children=false")
        void generalException_returnsGracefulError() {
            RuntimeException exception = new RuntimeException("Check children error");
            when(service.hasChildPages("pg-1")).thenThrow(exception);

            ExtensionResponse result = pageArea.checkIfPageHasChildren("pg-1");

            assertNotNull(result);
            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());
            assertEquals(false, result.getResponseValue().get("Success"));
            assertEquals(false, result.getResponseValue().get("Has Children"));
            assertEquals("Check children error", result.getResponseValue().get("Error"));

            verify(confluenceAuditService).failure(INVOKER_ID, "Check if Page Has Children", exception);
            verify(confluenceAuditService, never()).success(anyString(), anyString());
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    //  10. getAllNestedDescendantsOfPage
    // ════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getAllNestedDescendantsOfPage")
    class GetAllNestedDescendantsOfPage {

        @Test
        @DisplayName("Happy path: returns all descendants with Total Count")
        void happyPath_returnsDescendants() {
            com.kristasoft.extensions.essentials.collaboration.confluence.dto.Page[] dtoPages = {
                    createDtoPage("d-1", "Descendant One"),
                    createDtoPage("d-2", "Descendant Two"),
                    createDtoPage("d-3", "Descendant Three")
            };
            when(service.getAllNestedDescendants("pg-1")).thenReturn(dtoPages);

            ExtensionResponse result = pageArea.getAllNestedDescendantsOfPage("pg-1");

            assertNotNull(result);
            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());

            @SuppressWarnings("unchecked")
            List<Page> pages = (List<Page>) result.getResponseValue().get("Pages");
            assertEquals(3, pages.size());
            assertEquals(true, result.getResponseValue().get("Success"));
            assertEquals(3, result.getResponseValue().get("Total Count"));

            verify(confluenceAuditService).triggered(INVOKER_ID, "Get All Nested Descendants of Page");
            verify(confluenceAuditService).success(INVOKER_ID, "Get All Nested Descendants of Page");
            verify(telemetryHelper).recordSuccess(eq("confluence.getAllNestedDescendantsOfPage"), anyLong(), anyMap());
        }

        @Test
        @DisplayName("Happy path: empty descendants")
        void happyPath_emptyDescendants() {
            when(service.getAllNestedDescendants("pg-1"))
                    .thenReturn(new com.kristasoft.extensions.essentials.collaboration.confluence.dto.Page[0]);

            ExtensionResponse result = pageArea.getAllNestedDescendantsOfPage("pg-1");

            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());
            assertEquals(0, result.getResponseValue().get("Total Count"));
        }

        @Test
        @DisplayName("MustAuthorizeException: calls authFailure and rethrows")
        void mustAuthorizeException_rethrows() {
            MustAuthorizeException authEx = new MustAuthorizeException("Auth needed");
            when(service.getAllNestedDescendants("pg-1")).thenThrow(authEx);

            assertThrows(MustAuthorizeException.class,
                    () -> pageArea.getAllNestedDescendantsOfPage("pg-1"));

            verify(confluenceAuditService).authFailure(INVOKER_ID, "Get All Nested Descendants of Page", authEx);
            verify(confluenceAuditService, never()).success(anyString(), anyString());
        }

        @Test
        @DisplayName("General Exception: returns graceful error with empty pages and Total Count=0")
        void generalException_returnsGracefulError() {
            RuntimeException exception = new RuntimeException("Descendants error");
            when(service.getAllNestedDescendants("pg-1")).thenThrow(exception);

            ExtensionResponse result = pageArea.getAllNestedDescendantsOfPage("pg-1");

            assertNotNull(result);
            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());
            assertEquals(false, result.getResponseValue().get("Success"));
            assertEquals(0, result.getResponseValue().get("Total Count"));

            @SuppressWarnings("unchecked")
            List<?> pages = (List<?>) result.getResponseValue().get("Pages");
            assertTrue(pages.isEmpty());

            verify(confluenceAuditService).failure(INVOKER_ID, "Get All Nested Descendants of Page", exception);
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    //  11. getAncestorsOfPage
    // ════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getAncestorsOfPage")
    class GetAncestorsOfPage {

        @Test
        @DisplayName("Happy path: returns ancestors with Total Count")
        void happyPath_returnsAncestors() {
            com.kristasoft.extensions.essentials.collaboration.confluence.dto.Page[] dtoPages = {
                    createDtoPage("a-1", "Parent Page"),
                    createDtoPage("a-2", "Grandparent Page")
            };
            when(service.getAncestorsOfPage("pg-1")).thenReturn(dtoPages);

            ExtensionResponse result = pageArea.getAncestorsOfPage("pg-1");

            assertNotNull(result);
            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());

            @SuppressWarnings("unchecked")
            List<Page> ancestors = (List<Page>) result.getResponseValue().get("Ancestors");
            assertEquals(2, ancestors.size());
            assertEquals(true, result.getResponseValue().get("Success"));
            assertEquals(2, result.getResponseValue().get("Total Count"));

            verify(confluenceAuditService).triggered(INVOKER_ID, "Get Ancestors of Page");
            verify(confluenceAuditService).success(INVOKER_ID, "Get Ancestors of Page");
            verify(telemetryHelper).incrementCount("confluence.getAncestorsOfPage");
            verify(telemetryHelper).recordSuccess(eq("confluence.getAncestorsOfPage"), anyLong(), anyMap());
        }

        @Test
        @DisplayName("Happy path: no ancestors (root page)")
        void happyPath_noAncestors() {
            when(service.getAncestorsOfPage("root-pg"))
                    .thenReturn(new com.kristasoft.extensions.essentials.collaboration.confluence.dto.Page[0]);

            ExtensionResponse result = pageArea.getAncestorsOfPage("root-pg");

            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());
            assertEquals(0, result.getResponseValue().get("Total Count"));
        }

        @Test
        @DisplayName("MustAuthorizeException: calls authFailure and rethrows")
        void mustAuthorizeException_rethrows() {
            MustAuthorizeException authEx = new MustAuthorizeException("Auth needed");
            when(service.getAncestorsOfPage("pg-1")).thenThrow(authEx);

            assertThrows(MustAuthorizeException.class,
                    () -> pageArea.getAncestorsOfPage("pg-1"));

            verify(confluenceAuditService).authFailure(INVOKER_ID, "Get Ancestors of Page", authEx);
            verify(confluenceAuditService, never()).success(anyString(), anyString());
        }

        @Test
        @DisplayName("General Exception: returns graceful error with empty ancestors")
        void generalException_returnsGracefulError() {
            RuntimeException exception = new RuntimeException("Ancestors error");
            when(service.getAncestorsOfPage("pg-1")).thenThrow(exception);

            ExtensionResponse result = pageArea.getAncestorsOfPage("pg-1");

            assertNotNull(result);
            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());
            assertEquals(false, result.getResponseValue().get("Success"));
            assertEquals(0, result.getResponseValue().get("Total Count"));

            @SuppressWarnings("unchecked")
            List<?> ancestors = (List<?>) result.getResponseValue().get("Ancestors");
            assertTrue(ancestors.isEmpty());

            verify(confluenceAuditService).failure(INVOKER_ID, "Get Ancestors of Page", exception);
            verify(confluenceAuditService, never()).success(anyString(), anyString());
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    //  12. getAttachmentsForPage
    // ════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getAttachmentsForPage")
    class GetAttachmentsForPage {

        @Test
        @DisplayName("Happy path: validation passes, returns attachments")
        void happyPath_returnsAttachments() {
            when(validationOrchestrator.validate(anyMap(), eq(PageArea.class), eq("getAttachmentsForPage")))
                    .thenReturn(Collections.emptyList());

            Attachment att1 = new Attachment();
            att1.id = "att-1";
            att1.title = "file1.pdf";
            Attachment att2 = new Attachment();
            att2.id = "att-2";
            att2.title = "file2.docx";
            when(attachmentHelper.retrieveAllAttachments("pg-1", "current", "application/pdf"))
                    .thenReturn(List.of(att1, att2));

            ExtensionResponse result = pageArea.getAttachmentsForPage("pg-1", "current", "application/pdf", null);

            assertNotNull(result);
            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());

            @SuppressWarnings("unchecked")
            List<Attachment> attachments = (List<Attachment>) result.getResponseValue().get("Attachments");
            assertEquals(2, attachments.size());
            assertEquals(2, result.getResponseValue().get("Total Count"));
            assertEquals(true, result.getResponseValue().get("Success"));

            verify(confluenceAuditService).triggered(INVOKER_ID, "Get Attachments for Page");
            verify(confluenceAuditService).success(INVOKER_ID, "Get Attachments for Page");
            verify(telemetryHelper).recordSuccess(eq("confluence.getAttachmentsForPage"), anyLong(), anyMap());
        }

        @Test
        @DisplayName("Validation fails with allowRetry=true: returns confirmation response")
        void validationFails_allowRetryTrue_returnsConfirmation() {
            List<ValidationOrchestrator.ValidationResult> validationResults = List.of(createValidationResult());
            when(validationOrchestrator.validate(anyMap(), eq(PageArea.class), eq("getAttachmentsForPage")))
                    .thenReturn(validationResults);

            ExtensionResponse mockResponse = mock(ExtensionResponse.class);
            when(responseGenerator.generateConfirmationResponse(any(), any(),
                    eq("confirmReenterGetAttachmentsForPage"), anyMap()))
                    .thenReturn(mockResponse);

            ExtensionResponse result = pageArea.getAttachmentsForPage("pg-1", "current", null, true);

            assertSame(mockResponse, result);
        }

        @Test
        @DisplayName("Validation fails with allowRetry=false: returns validation error")
        void validationFails_allowRetryFalse_returnsValidationError() {
            List<ValidationOrchestrator.ValidationResult> validationResults = List.of(createValidationResult());
            when(validationOrchestrator.validate(anyMap(), eq(PageArea.class), eq("getAttachmentsForPage")))
                    .thenReturn(validationResults);

            ExtensionResponse mockResponse = mock(ExtensionResponse.class);
            when(responseGenerator.generateValidationErrorResponse(validationResults)).thenReturn(mockResponse);

            ExtensionResponse result = pageArea.getAttachmentsForPage("pg-1", "current", null, false);

            assertSame(mockResponse, result);
        }

        @Test
        @DisplayName("MustAuthorizeException: calls authFailure and rethrows")
        void mustAuthorizeException_rethrows() {
            MustAuthorizeException authEx = new MustAuthorizeException("Auth required");
            when(validationOrchestrator.validate(anyMap(), eq(PageArea.class), eq("getAttachmentsForPage")))
                    .thenThrow(authEx);

            assertThrows(MustAuthorizeException.class,
                    () -> pageArea.getAttachmentsForPage("pg-1", null, null, null));

            verify(confluenceAuditService).authFailure(INVOKER_ID, "Get Attachments for Page", authEx);
        }

        @Test
        @DisplayName("General Exception: returns failure response")
        void generalException_returnsFailure() {
            RuntimeException exception = new RuntimeException("Attachments error");
            when(validationOrchestrator.validate(anyMap(), eq(PageArea.class), eq("getAttachmentsForPage")))
                    .thenReturn(Collections.emptyList());
            when(attachmentHelper.retrieveAllAttachments(anyString(), any(), any()))
                    .thenThrow(exception);

            ExtensionResponse result = pageArea.getAttachmentsForPage("pg-1", null, null, null);

            assertEquals(ExtensionResponse.Result.FAILURE, result.getResult());
            assertTrue(result.getError().getErrorMessage().contains("Attachments error"));
            verify(confluenceAuditService).failure(INVOKER_ID, "Get Attachments for Page", exception);
        }

        @Test
        @DisplayName("Null parameters in confirmation: handles null status and mediaType")
        void nullParams_handledInConfirmation() {
            List<ValidationOrchestrator.ValidationResult> validationResults = List.of(createValidationResult());
            when(validationOrchestrator.validate(anyMap(), eq(PageArea.class), eq("getAttachmentsForPage")))
                    .thenReturn(validationResults);

            ExtensionResponse mockResponse = mock(ExtensionResponse.class);
            when(responseGenerator.generateConfirmationResponse(any(), any(), anyString(), anyMap()))
                    .thenReturn(mockResponse);

            ExtensionResponse result = pageArea.getAttachmentsForPage(null, null, null, true);

            assertSame(mockResponse, result);
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    //  13. getAttachmentByID
    // ════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getAttachmentByID")
    class GetAttachmentByID {

        @Test
        @DisplayName("Happy path: validation passes, returns attachment (HTTP 200)")
        void happyPath_returnsAttachment() {
            when(validationOrchestrator.validate(anyMap(), eq(PageArea.class), eq("getAttachmentById")))
                    .thenReturn(Collections.emptyList());

            // Mock the REST client chain for getAttachmentByID
            var confluenceAttributes = mock(ConfluenceAttributes.class);
            var restClient = mock(ConfluenceRESTClient.class);
            @SuppressWarnings("unchecked")
            java.net.http.HttpResponse<String> httpResponse = mock(java.net.http.HttpResponse.class);

            when(service.getConfluenceAttributes()).thenReturn(confluenceAttributes);
            when(confluenceAttributes.getRestClient()).thenReturn(restClient);
            when(restClient.doSyncGet(any(), any(), eq("attachments"), eq("att-1"))).thenReturn(httpResponse);
            when(httpResponse.statusCode()).thenReturn(200);
            when(httpResponse.body()).thenReturn("{\"id\":\"att-1\",\"title\":\"file.pdf\",\"pageId\":\"pg-1\",\"mediaType\":\"application/pdf\",\"fileSize\":1024}");

            Attachment mockAttachment = new Attachment();
            mockAttachment.id = "att-1";
            mockAttachment.title = "file.pdf";
            when(attachmentHelper.parseAttachment(any(), isNull())).thenReturn(mockAttachment);

            ExtensionResponse result = pageArea.getAttachmentByID("att-1", null);

            assertNotNull(result);
            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());
            Attachment attachment = (Attachment) result.getResponseValue().get("Attachment");
            assertEquals("att-1", attachment.id);
            assertEquals(true, result.getResponseValue().get("Success"));

            verify(confluenceAuditService).triggered(INVOKER_ID, "Get Attachment by ID");
            verify(confluenceAuditService).success(INVOKER_ID, "Get Attachment by ID");
        }

        @Test
        @DisplayName("HTTP non-200 response: throws RuntimeException leading to failure")
        void httpError_throwsRuntimeException() {
            when(validationOrchestrator.validate(anyMap(), eq(PageArea.class), eq("getAttachmentById")))
                    .thenReturn(Collections.emptyList());

            var confluenceAttributes = mock(ConfluenceAttributes.class);
            var restClient = mock(ConfluenceRESTClient.class);
            @SuppressWarnings("unchecked")
            java.net.http.HttpResponse<String> httpResponse = mock(java.net.http.HttpResponse.class);

            when(service.getConfluenceAttributes()).thenReturn(confluenceAttributes);
            when(confluenceAttributes.getRestClient()).thenReturn(restClient);
            when(restClient.doSyncGet(any(), any(), eq("attachments"), eq("att-bad"))).thenReturn(httpResponse);
            when(httpResponse.statusCode()).thenReturn(404);
            when(httpResponse.body()).thenReturn("Not found");

            ExtensionResponse result = pageArea.getAttachmentByID("att-bad", null);

            assertEquals(ExtensionResponse.Result.FAILURE, result.getResult());
            assertTrue(result.getError().getErrorMessage().contains("Failed to retrieve attachment"));
            verify(confluenceAuditService).failure(eq(INVOKER_ID), eq("Get Attachment by ID"), any(RuntimeException.class));
        }

        @Test
        @DisplayName("Validation fails with allowRetry=true: returns confirmation response")
        void validationFails_allowRetryTrue_returnsConfirmation() {
            List<ValidationOrchestrator.ValidationResult> validationResults = List.of(createValidationResult());
            when(validationOrchestrator.validate(anyMap(), eq(PageArea.class), eq("getAttachmentById")))
                    .thenReturn(validationResults);

            ExtensionResponse mockResponse = mock(ExtensionResponse.class);
            when(responseGenerator.generateConfirmationResponse(any(), any(),
                    eq("confirmReenterGetAttachmentById"), anyMap()))
                    .thenReturn(mockResponse);

            ExtensionResponse result = pageArea.getAttachmentByID("att-1", true);

            assertSame(mockResponse, result);
        }

        @Test
        @DisplayName("Validation fails with allowRetry=false: returns validation error")
        void validationFails_allowRetryFalse_returnsValidationError() {
            List<ValidationOrchestrator.ValidationResult> validationResults = List.of(createValidationResult());
            when(validationOrchestrator.validate(anyMap(), eq(PageArea.class), eq("getAttachmentById")))
                    .thenReturn(validationResults);

            ExtensionResponse mockResponse = mock(ExtensionResponse.class);
            when(responseGenerator.generateValidationErrorResponse(validationResults)).thenReturn(mockResponse);

            ExtensionResponse result = pageArea.getAttachmentByID("att-1", false);

            assertSame(mockResponse, result);
        }

        @Test
        @DisplayName("MustAuthorizeException: calls authFailure and rethrows")
        void mustAuthorizeException_rethrows() {
            MustAuthorizeException authEx = new MustAuthorizeException("Auth needed");
            when(validationOrchestrator.validate(anyMap(), eq(PageArea.class), eq("getAttachmentById")))
                    .thenThrow(authEx);

            assertThrows(MustAuthorizeException.class,
                    () -> pageArea.getAttachmentByID("att-1", null));

            verify(confluenceAuditService).authFailure(INVOKER_ID, "Get Attachment by ID", authEx);
        }

        @Test
        @DisplayName("General Exception: returns failure response")
        void generalException_returnsFailure() {
            RuntimeException exception = new RuntimeException("Attachment fetch error");
            when(validationOrchestrator.validate(anyMap(), eq(PageArea.class), eq("getAttachmentById")))
                    .thenReturn(Collections.emptyList());
            when(service.getConfluenceAttributes()).thenThrow(exception);

            ExtensionResponse result = pageArea.getAttachmentByID("att-1", null);

            assertEquals(ExtensionResponse.Result.FAILURE, result.getResult());
            assertTrue(result.getError().getErrorMessage().contains("Attachment fetch error"));
            verify(confluenceAuditService).failure(INVOKER_ID, "Get Attachment by ID", exception);
        }

        @Test
        @DisplayName("Null attachmentID in confirmation params: uses empty string")
        void nullAttachmentId_handledInConfirmation() {
            List<ValidationOrchestrator.ValidationResult> validationResults = List.of(createValidationResult());
            when(validationOrchestrator.validate(anyMap(), eq(PageArea.class), eq("getAttachmentById")))
                    .thenReturn(validationResults);

            ExtensionResponse mockResponse = mock(ExtensionResponse.class);
            when(responseGenerator.generateConfirmationResponse(any(), any(), anyString(), anyMap()))
                    .thenReturn(mockResponse);

            ExtensionResponse result = pageArea.getAttachmentByID(null, true);

            assertSame(mockResponse, result);
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    //  14. downloadAttachment
    // ════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("downloadAttachment")
    class DownloadAttachment {

        @Test
        @DisplayName("MustAuthorizeException: calls authFailure and rethrows")
        void mustAuthorizeException_rethrows() {
            MustAuthorizeException authEx = new MustAuthorizeException("Auth needed");
            var confluenceAttributes = mock(ConfluenceAttributes.class);
            var restClient = mock(ConfluenceRESTClient.class);
            when(service.getConfluenceAttributes()).thenReturn(confluenceAttributes);
            when(confluenceAttributes.getRestClient()).thenReturn(restClient);
            when(restClient.doSyncGetBinary("/wiki/download/attachments/123/file.pdf")).thenThrow(authEx);

            assertThrows(MustAuthorizeException.class,
                    () -> pageArea.downloadAttachment("/wiki/download/attachments/123/file.pdf", "att-1"));

            verify(confluenceAuditService).authFailure(INVOKER_ID, "Download Attachment", authEx);
            verify(confluenceAuditService, never()).success(anyString(), anyString());
        }

        @Test
        @DisplayName("General Exception: returns graceful error with Success=false")
        void generalException_returnsGracefulError() {
            RuntimeException exception = new RuntimeException("Download failed");
            var confluenceAttributes = mock(ConfluenceAttributes.class);
            var restClient = mock(ConfluenceRESTClient.class);
            when(service.getConfluenceAttributes()).thenReturn(confluenceAttributes);
            when(confluenceAttributes.getRestClient()).thenReturn(restClient);
            when(restClient.doSyncGetBinary("/wiki/download/attachments/123/file.pdf")).thenThrow(exception);

            ExtensionResponse result = pageArea.downloadAttachment("/wiki/download/attachments/123/file.pdf", "att-1");

            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());
            assertEquals(false, result.getResponseValue().get("Success"));
            assertEquals("Download failed", result.getResponseValue().get("Error"));
            verify(confluenceAuditService).failure(INVOKER_ID, "Download Attachment", exception);
            verify(confluenceAuditService, never()).success(anyString(), anyString());
        }

        @Test
        @DisplayName("Null download link: throws IllegalArgumentException, returns graceful error")
        void nullDownloadLink_returnsGracefulError() {
            ExtensionResponse result = pageArea.downloadAttachment(null, "att-1");

            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());
            assertEquals(false, result.getResponseValue().get("Success"));
            assertTrue(((String) result.getResponseValue().get("Error")).contains("Download link is required"));
            verify(confluenceAuditService).failure(eq(INVOKER_ID), eq("Download Attachment"), any(IllegalArgumentException.class));
        }

        @Test
        @DisplayName("Empty download link: throws IllegalArgumentException, returns graceful error")
        void emptyDownloadLink_returnsGracefulError() {
            ExtensionResponse result = pageArea.downloadAttachment("   ", "att-1");

            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());
            assertEquals(false, result.getResponseValue().get("Success"));
            assertTrue(((String) result.getResponseValue().get("Error")).contains("Download link is required"));
        }

        @Test
        @DisplayName("Invalid download link format: throws SecurityException, returns graceful error")
        void invalidDownloadLinkFormat_returnsGracefulError() {
            ExtensionResponse result = pageArea.downloadAttachment("https://evil.com/file.pdf", "att-1");

            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());
            assertEquals(false, result.getResponseValue().get("Success"));
            assertTrue(((String) result.getResponseValue().get("Error")).contains("Invalid download link format"));
            verify(confluenceAuditService).failure(eq(INVOKER_ID), eq("Download Attachment"), any(SecurityException.class));
        }

        @Test
        @DisplayName("Download link starting with /download/attachments/ is corrected to /wiki prefix")
        void downloadLinkCorrected_addsWikiPrefix() {
            // The download link starting with /download/attachments/ should be corrected to /wiki/download/attachments/
            var confluenceAttributes = mock(ConfluenceAttributes.class);
            var restClient = mock(ConfluenceRESTClient.class);
            when(service.getConfluenceAttributes()).thenReturn(confluenceAttributes);
            when(confluenceAttributes.getRestClient()).thenReturn(restClient);

            RuntimeException exception = new RuntimeException("Network error");
            when(restClient.doSyncGetBinary("/wiki/download/attachments/123/file.pdf")).thenThrow(exception);

            ExtensionResponse result = pageArea.downloadAttachment("/download/attachments/123/file.pdf", "att-1");

            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());
            assertEquals(false, result.getResponseValue().get("Success"));
            // Verify the corrected link was used
            verify(restClient).doSyncGetBinary("/wiki/download/attachments/123/file.pdf");
        }

        @Test
        @DisplayName("HTTP non-200 response: throws RuntimeException, returns graceful error")
        void httpError_returnsGracefulError() {
            var confluenceAttributes = mock(ConfluenceAttributes.class);
            var restClient = mock(ConfluenceRESTClient.class);
            @SuppressWarnings("unchecked")
            java.net.http.HttpResponse<byte[]> httpResponse = mock(java.net.http.HttpResponse.class);

            when(service.getConfluenceAttributes()).thenReturn(confluenceAttributes);
            when(confluenceAttributes.getRestClient()).thenReturn(restClient);
            when(restClient.doSyncGetBinary("/wiki/download/attachments/123/file.pdf")).thenReturn(httpResponse);
            when(httpResponse.statusCode()).thenReturn(403);

            ExtensionResponse result = pageArea.downloadAttachment("/wiki/download/attachments/123/file.pdf", "att-1");

            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());
            assertEquals(false, result.getResponseValue().get("Success"));
            assertTrue(((String) result.getResponseValue().get("Error")).contains("status code: 403"));
            verify(confluenceAuditService).failure(eq(INVOKER_ID), eq("Download Attachment"), any(RuntimeException.class));
        }

        @Test
        @DisplayName("Null attachmentID in error telemetry: uses 'unknown'")
        void nullAttachmentId_usesUnknown() {
            ExtensionResponse result = pageArea.downloadAttachment(null, null);

            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());
            assertEquals(false, result.getResponseValue().get("Success"));
            verify(confluenceAuditService).failure(eq(INVOKER_ID), eq("Download Attachment"), any(Exception.class));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  toDownloadPath — 100% branch coverage
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("toDownloadPath")
    class ToDownloadPath {

        /** Invokes the private {@code PageArea.toDownloadPath(String)} via reflection. */
        private String invoke(String downloadLink) throws Exception {
            java.lang.reflect.Method method =
                    PageArea.class.getDeclaredMethod("toDownloadPath", String.class);
            method.setAccessible(true);
            return (String) method.invoke(pageArea, downloadLink);
        }

        @Test
        @DisplayName("http:// URL without query → returns the path only")
        void httpUrlNoQuery_returnsPath() throws Exception {
            assertEquals("/wiki/download/attachments/1/file.doc",
                    invoke("http://acme.atlassian.net/wiki/download/attachments/1/file.doc"));
        }

        @Test
        @DisplayName("https:// URL with query → returns path plus raw query")
        void httpsUrlWithQuery_returnsPathAndQuery() throws Exception {
            assertEquals("/wiki/download/attachments/1/file.doc?version=1&cacheVersion=1",
                    invoke("https://acme.atlassian.net/wiki/download/attachments/1/file.doc?version=1&cacheVersion=1"));
        }

        @Test
        @DisplayName("relative path (no scheme) → returned unchanged")
        void relativePath_returnedUnchanged() throws Exception {
            assertEquals("/download/attachments/1/file.doc",
                    invoke("/download/attachments/1/file.doc"));
        }

        @Test
        @DisplayName("surrounding whitespace is trimmed for a relative path")
        void trimsWhitespace() throws Exception {
            assertEquals("/wiki/download/attachments/1/file.doc",
                    invoke("   /wiki/download/attachments/1/file.doc   "));
        }

        @Test
        @DisplayName("malformed http(s) URL → IllegalArgumentException caught, returns trimmed input")
        void malformedUrl_returnsTrimmedInput() throws Exception {
            // The space is an illegal URI character, so URI.create throws
            // IllegalArgumentException and the method falls back to the raw value.
            assertEquals("https://acme.atlassian.net/bad path",
                    invoke("  https://acme.atlassian.net/bad path  "));
        }
    }
}
