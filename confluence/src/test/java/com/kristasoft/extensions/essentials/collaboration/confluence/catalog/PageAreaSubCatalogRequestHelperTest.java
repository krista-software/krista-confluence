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
import app.krista.model.base.File;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.entities.Content;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.errorhandlers.ErrorHandlingStateManager;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.errorhandlers.ExtensionResponseGenerator;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.stores.Pair;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.validators.ValidationOrchestrator;
import com.kristasoft.extensions.essentials.collaboration.confluence.dto.Page;
import com.kristasoft.extensions.essentials.collaboration.confluence.service.ConfluenceService;
import com.kristasoft.extensions.essentials.collaboration.confluence.service.PageDocxService;
import com.kristasoft.extensions.essentials.collaboration.confluence.service.PageMarkdownService;
import com.kristasoft.extensions.essentials.collaboration.confluence.utils.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive JUnit 5 tests for {@link PageAreaSubCatalogRequestHelper}.
 * Targets 100% code coverage of all public methods, try/catch branches,
 * title filter logic, and the private getValidationResults helper.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PageAreaSubCatalogRequestHelper")
class PageAreaSubCatalogRequestHelperTest {

    @Mock private ConfluenceService confluenceService;
    @Mock private ErrorHandlingStateManager internalStateManager;
    @Mock private ExtensionResponseGenerator responseGenerator;
    @Mock private ValidationOrchestrator validationOrchestrator;
    @Mock private PageDocxService pageDocxService;
    @Mock private PageMarkdownService pageMarkdownService;

    private PageAreaSubCatalogRequestHelper helper;

    private static final String STATE_ID_VALUE = "state-1";

    @BeforeEach
    void setUp() {
        helper = new PageAreaSubCatalogRequestHelper(
                confluenceService, internalStateManager, responseGenerator,
                validationOrchestrator, pageDocxService, pageMarkdownService);
    }

    // ── Shared helper methods ────────────────────────────────────────────────

    private static ValidationOrchestrator.ValidationResult validationResult() {
        return new ValidationOrchestrator.ValidationResult(
                "confirm step", "Page ID", "fetch step",
                "Page ID is not valid", "Identifier", Map.of());
    }

    private static Map<String, Object> stateWithValidationResults() {
        Map<String, Object> state = new HashMap<>();
        state.put(Constants.VALIDATION_RESULTS, List.of(validationResult()));
        return state;
    }

    private static Map<String, Object> stateWithEmptyValidationResults() {
        Map<String, Object> state = new HashMap<>();
        state.put(Constants.VALIDATION_RESULTS, Collections.emptyList());
        return state;
    }

    private static Map<String, Object> stateWithNullValidationResults() {
        return new HashMap<>();
    }

    private static Page dtoPage(String id, String title) {
        Page page = new Page.PageBuilder(id, title)
                .status("current")
                .spaceId("sp-1")
                .build();
        page.setDomainUrl("");
        return page;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  confirmReenterGetPagesInSpace
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("confirmReenterGetPagesInSpace")
    class ConfirmReenterGetPagesInSpace {

        @Test
        @DisplayName("reEnter=false returns deny response")
        void reEnterFalse_returnsDenyResponse() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put(Constants.RE_ENTER, false);
            inputMap.put(Constants.STATE_ID, STATE_ID_VALUE);

            when(internalStateManager.get(STATE_ID_VALUE)).thenReturn(stateWithValidationResults());
            ExtensionResponse mockResponse = mock(ExtensionResponse.class);
            when(responseGenerator.generateFetchDenyResponse(
                    eq(ExtensionResponse.Error.ExceptionType.INPUT_ERROR),
                    anyList(),
                    eq(Constants.HANDLE_REENTER_GET_PAGES_IN_SPACE),
                    eq(inputMap)))
                    .thenReturn(mockResponse);

            ExtensionResponse result = helper.confirmReenterGetPagesInSpace(inputMap);

            assertSame(mockResponse, result);
            verify(responseGenerator).generateFetchDenyResponse(
                    eq(ExtensionResponse.Error.ExceptionType.INPUT_ERROR),
                    anyList(),
                    eq(Constants.HANDLE_REENTER_GET_PAGES_IN_SPACE),
                    eq(inputMap));
            verify(responseGenerator, never()).generateFetchResponse(any(), anyList(), anyString(), anyMap());
        }

        @Test
        @DisplayName("reEnter=true returns fetch response")
        void reEnterTrue_returnsFetchResponse() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put(Constants.RE_ENTER, true);
            inputMap.put(Constants.STATE_ID, STATE_ID_VALUE);

            when(internalStateManager.get(STATE_ID_VALUE)).thenReturn(stateWithEmptyValidationResults());
            ExtensionResponse mockResponse = mock(ExtensionResponse.class);
            when(responseGenerator.generateFetchResponse(
                    eq(ExtensionResponse.Error.ExceptionType.INPUT_ERROR),
                    anyList(),
                    eq(Constants.HANDLE_REENTER_GET_PAGES_IN_SPACE),
                    eq(inputMap)))
                    .thenReturn(mockResponse);

            ExtensionResponse result = helper.confirmReenterGetPagesInSpace(inputMap);

            assertSame(mockResponse, result);
            verify(responseGenerator).generateFetchResponse(
                    eq(ExtensionResponse.Error.ExceptionType.INPUT_ERROR),
                    anyList(),
                    eq(Constants.HANDLE_REENTER_GET_PAGES_IN_SPACE),
                    eq(inputMap));
        }

        @Test
        @DisplayName("reEnter=null treated as accept, returns fetch response")
        void reEnterNull_treatedAsAccept() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put(Constants.RE_ENTER, null);
            inputMap.put(Constants.STATE_ID, STATE_ID_VALUE);

            when(internalStateManager.get(STATE_ID_VALUE)).thenReturn(stateWithNullValidationResults());
            ExtensionResponse mockResponse = mock(ExtensionResponse.class);
            when(responseGenerator.generateFetchResponse(
                    eq(ExtensionResponse.Error.ExceptionType.INPUT_ERROR),
                    anyList(),
                    eq(Constants.HANDLE_REENTER_GET_PAGES_IN_SPACE),
                    eq(inputMap)))
                    .thenReturn(mockResponse);

            ExtensionResponse result = helper.confirmReenterGetPagesInSpace(inputMap);

            assertSame(mockResponse, result);
        }

        @Test
        @DisplayName("exception thrown returns failure response")
        void exceptionThrown_returnsFailure() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put(Constants.RE_ENTER, true);
            inputMap.put(Constants.STATE_ID, STATE_ID_VALUE);

            when(internalStateManager.get(STATE_ID_VALUE))
                    .thenThrow(new RuntimeException("state error"));

            ExtensionResponse result = helper.confirmReenterGetPagesInSpace(inputMap);

            assertNotNull(result);
            assertEquals(ExtensionResponse.Result.FAILURE, result.getResult());
            assertTrue(result.getError().getErrorMessage().contains(Constants.FAILED_TO_GET_PAGES_IN_SPACE_VALIDATION));
            assertTrue(result.getError().getErrorMessage().contains("state error"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  handleReenterGetPagesInSpace
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("handleReenterGetPagesInSpace")
    class HandleReenterGetPagesInSpace {

        @Test
        @DisplayName("success without title filter: returns all pages")
        void success_noTitleFilter_returnsAllPages() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put("Space ID", "sp-1");
            // No Title key

            Page dto1 = dtoPage("pg-1", "Meeting Notes");
            Page dto2 = dtoPage("pg-2", "Other Page");
            when(confluenceService.getPagesBySpaceId("sp-1"))
                    .thenReturn(new Page[]{dto1, dto2});

            ExtensionResponse result = helper.handleReenterGetPagesInSpace(inputMap);

            assertNotNull(result);
            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());
            List<?> pages = (List<?>) result.getResponseValue().get(Constants.PAGES);
            List<?> ids = (List<?>) result.getResponseValue().get(Constants.PAGE_IDENTIFIERS);
            assertEquals(2, pages.size());
            assertEquals(2, ids.size());
            verify(confluenceService).getPagesBySpaceId("sp-1");
        }

        @Test
        @DisplayName("success with empty title: returns all pages (no filter)")
        void success_emptyTitle_returnsAllPages() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put("Space ID", "sp-1");
            inputMap.put("Title", "   ");

            Page dto1 = dtoPage("pg-1", "Meeting Notes");
            when(confluenceService.getPagesBySpaceId("sp-1"))
                    .thenReturn(new Page[]{dto1});

            ExtensionResponse result = helper.handleReenterGetPagesInSpace(inputMap);

            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());
            List<?> pages = (List<?>) result.getResponseValue().get(Constants.PAGES);
            assertEquals(1, pages.size());
        }

        @Test
        @DisplayName("success with title filter: filters pages by equalsIgnoreCase")
        void success_withTitleFilter_filtersPages() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put("Space ID", "sp-1");
            inputMap.put("Title", "Meeting Notes");

            Page dto1 = dtoPage("pg-1", "Meeting Notes");
            Page dto2 = dtoPage("pg-2", "Other Page");
            when(confluenceService.getPagesBySpaceId("sp-1"))
                    .thenReturn(new Page[]{dto1, dto2});

            ExtensionResponse result = helper.handleReenterGetPagesInSpace(inputMap);

            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());
            List<?> pages = (List<?>) result.getResponseValue().get(Constants.PAGES);
            List<?> ids = (List<?>) result.getResponseValue().get(Constants.PAGE_IDENTIFIERS);
            assertEquals(1, pages.size());
            assertEquals(1, ids.size());
            assertTrue(ids.contains("pg-1"));
        }

        @Test
        @DisplayName("success with title filter: case-insensitive match")
        void success_caseInsensitiveFilter() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put("Space ID", "sp-1");
            inputMap.put("Title", "meeting notes");

            Page dto1 = dtoPage("pg-1", "Meeting Notes");
            Page dto2 = dtoPage("pg-2", "Other Page");
            when(confluenceService.getPagesBySpaceId("sp-1"))
                    .thenReturn(new Page[]{dto1, dto2});

            ExtensionResponse result = helper.handleReenterGetPagesInSpace(inputMap);

            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());
            List<?> pages = (List<?>) result.getResponseValue().get(Constants.PAGES);
            assertEquals(1, pages.size());
        }

        @Test
        @DisplayName("success with title filter that matches no pages")
        void success_titleFilterNoMatch() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put("Space ID", "sp-1");
            inputMap.put("Title", "Nonexistent Page");

            Page dto1 = dtoPage("pg-1", "Meeting Notes");
            when(confluenceService.getPagesBySpaceId("sp-1"))
                    .thenReturn(new Page[]{dto1});

            ExtensionResponse result = helper.handleReenterGetPagesInSpace(inputMap);

            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());
            assertEquals(0, ((List<?>) result.getResponseValue().get(Constants.PAGES)).size());
        }

        @Test
        @DisplayName("success with null title: returns all pages")
        void success_nullTitle_returnsAllPages() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put("Space ID", "sp-1");
            inputMap.put("Title", null);

            Page dto1 = dtoPage("pg-1", "Page One");
            when(confluenceService.getPagesBySpaceId("sp-1"))
                    .thenReturn(new Page[]{dto1});

            ExtensionResponse result = helper.handleReenterGetPagesInSpace(inputMap);

            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());
            assertEquals(1, ((List<?>) result.getResponseValue().get(Constants.PAGES)).size());
        }

        @Test
        @DisplayName("exception: returns failure response")
        void exception_returnsFailure() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put("Space ID", "bad-space");

            when(confluenceService.getPagesBySpaceId("bad-space"))
                    .thenThrow(new RuntimeException("Confluence unavailable"));

            ExtensionResponse result = helper.handleReenterGetPagesInSpace(inputMap);

            assertEquals(ExtensionResponse.Result.FAILURE, result.getResult());
            assertTrue(result.getError().getErrorMessage().contains(Constants.FAILED_TO_GET_PAGES_IN_SPACE));
            assertTrue(result.getError().getErrorMessage().contains("Confluence unavailable"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  confirmReenterCreatePageFromContent
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("confirmReenterCreatePageFromContent")
    class ConfirmReenterCreatePageFromContent {

        @Test
        @DisplayName("reEnter=false returns deny response")
        void reEnterFalse_returnsDenyResponse() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put(Constants.RE_ENTER, false);
            inputMap.put(Constants.STATE_ID, STATE_ID_VALUE);

            when(internalStateManager.get(STATE_ID_VALUE)).thenReturn(stateWithValidationResults());
            ExtensionResponse mockResponse = mock(ExtensionResponse.class);
            when(responseGenerator.generateFetchDenyResponse(
                    eq(ExtensionResponse.Error.ExceptionType.INPUT_ERROR),
                    anyList(),
                    eq(Constants.HANDLE_REENTER_CREATE_PAGE_FROM_CONTENT),
                    eq(inputMap)))
                    .thenReturn(mockResponse);

            ExtensionResponse result = helper.confirmReenterCreatePageFromContent(inputMap);

            assertSame(mockResponse, result);
            verify(responseGenerator, never()).generateFetchResponse(any(), anyList(), anyString(), anyMap());
        }

        @Test
        @DisplayName("reEnter=true returns fetch response")
        void reEnterTrue_returnsFetchResponse() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put(Constants.RE_ENTER, true);
            inputMap.put(Constants.STATE_ID, STATE_ID_VALUE);

            when(internalStateManager.get(STATE_ID_VALUE)).thenReturn(stateWithValidationResults());
            ExtensionResponse mockResponse = mock(ExtensionResponse.class);
            when(responseGenerator.generateFetchResponse(
                    eq(ExtensionResponse.Error.ExceptionType.INPUT_ERROR),
                    anyList(),
                    eq(Constants.HANDLE_REENTER_CREATE_PAGE_FROM_CONTENT),
                    eq(inputMap)))
                    .thenReturn(mockResponse);

            ExtensionResponse result = helper.confirmReenterCreatePageFromContent(inputMap);

            assertSame(mockResponse, result);
        }

        @Test
        @DisplayName("exception thrown returns failure response")
        void exceptionThrown_returnsFailure() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put(Constants.RE_ENTER, true);
            inputMap.put(Constants.STATE_ID, STATE_ID_VALUE);

            when(internalStateManager.get(STATE_ID_VALUE))
                    .thenThrow(new RuntimeException("state error"));

            ExtensionResponse result = helper.confirmReenterCreatePageFromContent(inputMap);

            assertEquals(ExtensionResponse.Result.FAILURE, result.getResult());
            assertTrue(result.getError().getErrorMessage().contains(Constants.FAILED_TO_CREATE_PAGE_FROM_CONTENT_VALIDATION));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  handleReenterCreatePageFromContent
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("handleReenterCreatePageFromContent")
    class HandleReenterCreatePageFromContent {

        @Test
        @DisplayName("success: creates page and returns success response")
        void success_returnsPageEntity() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put("Page Title", "New Page");
            inputMap.put("Space ID", "sp-1");
            List<Content> content = List.of(new Content());
            inputMap.put("Content", content);

            Page dtoResult = dtoPage("pg-new", "New Page");
            when(confluenceService.createPageFromContent("sp-1", "New Page", content))
                    .thenReturn(dtoResult);

            ExtensionResponse result = helper.handleReenterCreatePageFromContent(inputMap);

            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());
            assertNotNull(result.getResponseValue().get("Page"));
            verify(confluenceService).createPageFromContent("sp-1", "New Page", content);
        }

        @Test
        @DisplayName("exception: returns failure response")
        void exception_returnsFailure() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put("Page Title", "New Page");
            inputMap.put("Space ID", "sp-1");
            inputMap.put("Content", List.of(new Content()));

            when(confluenceService.createPageFromContent(anyString(), anyString(), anyList()))
                    .thenThrow(new RuntimeException("Creation failed"));

            ExtensionResponse result = helper.handleReenterCreatePageFromContent(inputMap);

            assertEquals(ExtensionResponse.Result.FAILURE, result.getResult());
            assertTrue(result.getError().getErrorMessage().contains(Constants.FAILED_TO_CREATE_PAGE_FROM_CONTENT));
            assertTrue(result.getError().getErrorMessage().contains("Creation failed"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  confirmReenterCreatePageFromTemplate
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("confirmReenterCreatePageFromTemplate")
    class ConfirmReenterCreatePageFromTemplate {

        @Test
        @DisplayName("reEnter=false returns deny response")
        void reEnterFalse_returnsDenyResponse() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put(Constants.RE_ENTER, false);
            inputMap.put(Constants.STATE_ID, STATE_ID_VALUE);

            when(internalStateManager.get(STATE_ID_VALUE)).thenReturn(stateWithValidationResults());
            ExtensionResponse mockResponse = mock(ExtensionResponse.class);
            when(responseGenerator.generateFetchDenyResponse(
                    eq(ExtensionResponse.Error.ExceptionType.INPUT_ERROR),
                    anyList(),
                    eq(Constants.HANDLE_REENTER_CREATE_PAGE_FROM_TEMPLATE),
                    eq(inputMap)))
                    .thenReturn(mockResponse);

            ExtensionResponse result = helper.confirmReenterCreatePageFromTemplate(inputMap);

            assertSame(mockResponse, result);
        }

        @Test
        @DisplayName("reEnter=true returns fetch response")
        void reEnterTrue_returnsFetchResponse() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put(Constants.RE_ENTER, true);
            inputMap.put(Constants.STATE_ID, STATE_ID_VALUE);

            when(internalStateManager.get(STATE_ID_VALUE)).thenReturn(stateWithValidationResults());
            ExtensionResponse mockResponse = mock(ExtensionResponse.class);
            when(responseGenerator.generateFetchResponse(
                    eq(ExtensionResponse.Error.ExceptionType.INPUT_ERROR),
                    anyList(),
                    eq(Constants.HANDLE_REENTER_CREATE_PAGE_FROM_TEMPLATE),
                    eq(inputMap)))
                    .thenReturn(mockResponse);

            ExtensionResponse result = helper.confirmReenterCreatePageFromTemplate(inputMap);

            assertSame(mockResponse, result);
        }

        @Test
        @DisplayName("exception thrown returns failure response")
        void exceptionThrown_returnsFailure() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put(Constants.RE_ENTER, false);
            inputMap.put(Constants.STATE_ID, STATE_ID_VALUE);

            when(internalStateManager.get(STATE_ID_VALUE))
                    .thenThrow(new RuntimeException("state crash"));

            ExtensionResponse result = helper.confirmReenterCreatePageFromTemplate(inputMap);

            assertEquals(ExtensionResponse.Result.FAILURE, result.getResult());
            assertTrue(result.getError().getErrorMessage().contains(Constants.FAILED_TO_CREATE_PAGE_FROM_TEMPLATE_VALIDATION));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  handleReenterCreatePageFromTemplate
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("handleReenterCreatePageFromTemplate")
    class HandleReenterCreatePageFromTemplate {

        @Test
        @DisplayName("success: creates page from template and returns success")
        void success_returnsPageEntity() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put("Space ID", "sp-1");
            inputMap.put("Template ID", "tmpl-1");
            inputMap.put("Page Title", "Template Page");
            List<Map<String, Object>> variables = List.of(Map.of("Key", "var1", "Value", "val1"));
            inputMap.put("Variables", variables);

            Page dtoResult = dtoPage("pg-tmpl", "Template Page");
            when(confluenceService.createPageFromTemplate("sp-1", "tmpl-1", "Template Page", variables))
                    .thenReturn(dtoResult);

            ExtensionResponse result = helper.handleReenterCreatePageFromTemplate(inputMap);

            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());
            assertNotNull(result.getResponseValue().get("Page"));
            verify(confluenceService).createPageFromTemplate("sp-1", "tmpl-1", "Template Page", variables);
        }

        @Test
        @DisplayName("exception: returns failure response")
        void exception_returnsFailure() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put("Space ID", "sp-1");
            inputMap.put("Template ID", "tmpl-1");
            inputMap.put("Page Title", "Page");
            inputMap.put("Variables", List.of());

            when(confluenceService.createPageFromTemplate(anyString(), anyString(), anyString(), anyList()))
                    .thenThrow(new RuntimeException("Template error"));

            ExtensionResponse result = helper.handleReenterCreatePageFromTemplate(inputMap);

            assertEquals(ExtensionResponse.Result.FAILURE, result.getResult());
            assertTrue(result.getError().getErrorMessage().contains(Constants.FAILED_TO_CREATE_PAGE_FROM_TEMPLATE));
            assertTrue(result.getError().getErrorMessage().contains("Template error"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  confirmReenterGetPageById
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("confirmReenterGetPageById")
    class ConfirmReenterGetPageById {

        @Test
        @DisplayName("reEnter=false returns deny response")
        void reEnterFalse_returnsDenyResponse() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put(Constants.RE_ENTER, false);
            inputMap.put(Constants.STATE_ID, STATE_ID_VALUE);

            when(internalStateManager.get(STATE_ID_VALUE)).thenReturn(stateWithValidationResults());
            ExtensionResponse mockResponse = mock(ExtensionResponse.class);
            when(responseGenerator.generateFetchDenyResponse(
                    eq(ExtensionResponse.Error.ExceptionType.INPUT_ERROR),
                    anyList(),
                    eq(Constants.HANDLE_REENTER_GET_PAGE_BY_ID),
                    eq(inputMap)))
                    .thenReturn(mockResponse);

            ExtensionResponse result = helper.confirmReenterGetPageById(inputMap);

            assertSame(mockResponse, result);
        }

        @Test
        @DisplayName("reEnter=true returns fetch response")
        void reEnterTrue_returnsFetchResponse() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put(Constants.RE_ENTER, true);
            inputMap.put(Constants.STATE_ID, STATE_ID_VALUE);

            when(internalStateManager.get(STATE_ID_VALUE)).thenReturn(stateWithValidationResults());
            ExtensionResponse mockResponse = mock(ExtensionResponse.class);
            when(responseGenerator.generateFetchResponse(
                    eq(ExtensionResponse.Error.ExceptionType.INPUT_ERROR),
                    anyList(),
                    eq(Constants.HANDLE_REENTER_GET_PAGE_BY_ID),
                    eq(inputMap)))
                    .thenReturn(mockResponse);

            ExtensionResponse result = helper.confirmReenterGetPageById(inputMap);

            assertSame(mockResponse, result);
        }

        @Test
        @DisplayName("exception thrown returns failure response")
        void exceptionThrown_returnsFailure() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put(Constants.RE_ENTER, true);
            inputMap.put(Constants.STATE_ID, STATE_ID_VALUE);

            when(internalStateManager.get(STATE_ID_VALUE))
                    .thenThrow(new RuntimeException("state error"));

            ExtensionResponse result = helper.confirmReenterGetPageById(inputMap);

            assertEquals(ExtensionResponse.Result.FAILURE, result.getResult());
            assertTrue(result.getError().getErrorMessage().contains(Constants.FAILED_TO_GET_PAGE_BY_ID_VALIDATION));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  handleReenterGetPageById
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("handleReenterGetPageById")
    class HandleReenterGetPageById {

        @Test
        @DisplayName("success: retrieves page and returns success response")
        void success_returnsPageEntity() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put(Constants.STATE_ID, STATE_ID_VALUE);
            inputMap.put("Page ID", "pg-1");

            when(confluenceService.getPageById("pg-1")).thenReturn(dtoPage("pg-1", "My Page"));

            ExtensionResponse result = helper.handleReenterGetPageById(inputMap);

            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());
            assertNotNull(result.getResponseValue().get("Page"));
            verify(confluenceService).getPageById("pg-1");
        }

        @Test
        @DisplayName("exception: returns failure response")
        void exception_returnsFailure() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put(Constants.STATE_ID, STATE_ID_VALUE);
            inputMap.put("Page ID", "bad-id");

            when(confluenceService.getPageById("bad-id"))
                    .thenThrow(new RuntimeException("Page not found"));

            ExtensionResponse result = helper.handleReenterGetPageById(inputMap);

            assertEquals(ExtensionResponse.Result.FAILURE, result.getResult());
            assertTrue(result.getError().getErrorMessage().contains(Constants.FAILED_TO_GET_PAGE_BY_ID));
            assertTrue(result.getError().getErrorMessage().contains("Page not found"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  confirmReenterUpdatePageByContent
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("confirmReenterUpdatePageByContent")
    class ConfirmReenterUpdatePageByContent {

        @Test
        @DisplayName("reEnter=false returns deny response")
        void reEnterFalse_returnsDenyResponse() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put(Constants.RE_ENTER, false);
            inputMap.put(Constants.STATE_ID, STATE_ID_VALUE);

            when(internalStateManager.get(STATE_ID_VALUE)).thenReturn(stateWithValidationResults());
            ExtensionResponse mockResponse = mock(ExtensionResponse.class);
            when(responseGenerator.generateFetchDenyResponse(
                    eq(ExtensionResponse.Error.ExceptionType.INPUT_ERROR),
                    anyList(),
                    eq(Constants.HANDLE_REENTER_UPDATE_PAGE_BY_CONTENT),
                    eq(inputMap)))
                    .thenReturn(mockResponse);

            ExtensionResponse result = helper.confirmReenterUpdatePageByContent(inputMap);

            assertSame(mockResponse, result);
        }

        @Test
        @DisplayName("reEnter=true returns fetch response")
        void reEnterTrue_returnsFetchResponse() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put(Constants.RE_ENTER, true);
            inputMap.put(Constants.STATE_ID, STATE_ID_VALUE);

            when(internalStateManager.get(STATE_ID_VALUE)).thenReturn(stateWithValidationResults());
            ExtensionResponse mockResponse = mock(ExtensionResponse.class);
            when(responseGenerator.generateFetchResponse(
                    eq(ExtensionResponse.Error.ExceptionType.INPUT_ERROR),
                    anyList(),
                    eq(Constants.HANDLE_REENTER_UPDATE_PAGE_BY_CONTENT),
                    eq(inputMap)))
                    .thenReturn(mockResponse);

            ExtensionResponse result = helper.confirmReenterUpdatePageByContent(inputMap);

            assertSame(mockResponse, result);
        }

        @Test
        @DisplayName("exception thrown returns failure response")
        void exceptionThrown_returnsFailure() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put(Constants.RE_ENTER, true);
            inputMap.put(Constants.STATE_ID, STATE_ID_VALUE);

            when(internalStateManager.get(STATE_ID_VALUE))
                    .thenThrow(new RuntimeException("state error"));

            ExtensionResponse result = helper.confirmReenterUpdatePageByContent(inputMap);

            assertEquals(ExtensionResponse.Result.FAILURE, result.getResult());
            assertTrue(result.getError().getErrorMessage().contains(Constants.FAILED_TO_UPDATE_PAGE_BY_CONTENT_VALIDATION));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  handleReenterUpdatePageByContent
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("handleReenterUpdatePageByContent")
    class HandleReenterUpdatePageByContent {

        @Test
        @DisplayName("success: updates page and returns success response")
        void success_returnsUpdatedPage() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put("Page ID", "pg-1");
            inputMap.put("Title", "Updated Title");
            inputMap.put("Status", "current");
            inputMap.put("Comments", "Some comment");
            List<Content> content = List.of(new Content());
            inputMap.put("Content", content);

            Page dtoResult = dtoPage("pg-1", "Updated Title");
            when(confluenceService.updatePageFromContent("pg-1", "current", "Updated Title", "Some comment", content))
                    .thenReturn(dtoResult);

            ExtensionResponse result = helper.handleReenterUpdatePageByContent(inputMap);

            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());
            assertNotNull(result.getResponseValue().get("Page"));
            verify(confluenceService).updatePageFromContent("pg-1", "current", "Updated Title", "Some comment", content);
        }

        @Test
        @DisplayName("exception: returns failure response")
        void exception_returnsFailure() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put("Page ID", "pg-1");
            inputMap.put("Title", "Title");
            inputMap.put("Status", "current");
            inputMap.put("Comments", "comment");
            inputMap.put("Content", List.of(new Content()));

            when(confluenceService.updatePageFromContent(anyString(), anyString(), anyString(), anyString(), anyList()))
                    .thenThrow(new RuntimeException("Update failed"));

            ExtensionResponse result = helper.handleReenterUpdatePageByContent(inputMap);

            assertEquals(ExtensionResponse.Result.FAILURE, result.getResult());
            assertTrue(result.getError().getErrorMessage().contains(Constants.FAILED_TO_UPDATE_PAGE_BY_CONTENT));
            assertTrue(result.getError().getErrorMessage().contains("Update failed"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  confirmReenterUpdatePageFromTemplate
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("confirmReenterUpdatePageFromTemplate")
    class ConfirmReenterUpdatePageFromTemplate {

        @Test
        @DisplayName("reEnter=false returns deny response")
        void reEnterFalse_returnsDenyResponse() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put(Constants.RE_ENTER, false);
            inputMap.put(Constants.STATE_ID, STATE_ID_VALUE);

            when(internalStateManager.get(STATE_ID_VALUE)).thenReturn(stateWithValidationResults());
            ExtensionResponse mockResponse = mock(ExtensionResponse.class);
            when(responseGenerator.generateFetchDenyResponse(
                    eq(ExtensionResponse.Error.ExceptionType.INPUT_ERROR),
                    anyList(),
                    eq(Constants.HANDLE_REENTER_UPDATE_PAGE_FROM_TEMPLATE),
                    eq(inputMap)))
                    .thenReturn(mockResponse);

            ExtensionResponse result = helper.confirmReenterUpdatePageFromTemplate(inputMap);

            assertSame(mockResponse, result);
        }

        @Test
        @DisplayName("reEnter=true returns fetch response")
        void reEnterTrue_returnsFetchResponse() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put(Constants.RE_ENTER, true);
            inputMap.put(Constants.STATE_ID, STATE_ID_VALUE);

            when(internalStateManager.get(STATE_ID_VALUE)).thenReturn(stateWithValidationResults());
            ExtensionResponse mockResponse = mock(ExtensionResponse.class);
            when(responseGenerator.generateFetchResponse(
                    eq(ExtensionResponse.Error.ExceptionType.INPUT_ERROR),
                    anyList(),
                    eq(Constants.HANDLE_REENTER_UPDATE_PAGE_FROM_TEMPLATE),
                    eq(inputMap)))
                    .thenReturn(mockResponse);

            ExtensionResponse result = helper.confirmReenterUpdatePageFromTemplate(inputMap);

            assertSame(mockResponse, result);
        }

        @Test
        @DisplayName("exception thrown returns failure response")
        void exceptionThrown_returnsFailure() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put(Constants.RE_ENTER, false);
            inputMap.put(Constants.STATE_ID, STATE_ID_VALUE);

            when(internalStateManager.get(STATE_ID_VALUE))
                    .thenThrow(new RuntimeException("state crash"));

            ExtensionResponse result = helper.confirmReenterUpdatePageFromTemplate(inputMap);

            assertEquals(ExtensionResponse.Result.FAILURE, result.getResult());
            assertTrue(result.getError().getErrorMessage().contains(Constants.FAILED_TO_UPDATE_PAGE_FROM_TEMPLATE_VALIDATION));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  handleReenterUpdatePageFromTemplate
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("handleReenterUpdatePageFromTemplate")
    class HandleReenterUpdatePageFromTemplate {

        @Test
        @DisplayName("success: updates page from template and returns success")
        void success_returnsUpdatedPage() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put("Page ID", "pg-1");
            inputMap.put("Template Id", "tmpl-1");
            inputMap.put("Status", "current");
            inputMap.put("Title", "Updated From Template");
            inputMap.put("Comment", "Template update");
            List<Map<String, Object>> variables = List.of(Map.of("Key", "var1", "Value", "val1"));
            inputMap.put("Variables", variables);

            Page dtoResult = dtoPage("pg-1", "Updated From Template");
            when(confluenceService.updatePageFromTemplate("pg-1", "tmpl-1", "current", "Updated From Template", "Template update", variables))
                    .thenReturn(dtoResult);

            ExtensionResponse result = helper.handleReenterUpdatePageFromTemplate(inputMap);

            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());
            assertNotNull(result.getResponseValue().get("Page"));
            verify(confluenceService).updatePageFromTemplate("pg-1", "tmpl-1", "current", "Updated From Template", "Template update", variables);
        }

        @Test
        @DisplayName("exception: returns failure response")
        void exception_returnsFailure() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put("Page ID", "pg-1");
            inputMap.put("Template Id", "tmpl-1");
            inputMap.put("Status", "current");
            inputMap.put("Title", "Title");
            inputMap.put("Comment", "comment");
            inputMap.put("Variables", List.of());

            when(confluenceService.updatePageFromTemplate(anyString(), anyString(), anyString(), anyString(), anyString(), anyList()))
                    .thenThrow(new RuntimeException("Template update error"));

            ExtensionResponse result = helper.handleReenterUpdatePageFromTemplate(inputMap);

            assertEquals(ExtensionResponse.Result.FAILURE, result.getResult());
            assertTrue(result.getError().getErrorMessage().contains(Constants.FAILED_TO_UPDATE_PAGE_FROM_TEMPLATE));
            assertTrue(result.getError().getErrorMessage().contains("Template update error"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  confirmReenterGetPageInSpace
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("confirmReenterGetPageInSpace")
    class ConfirmReenterGetPageInSpace {

        @Test
        @DisplayName("reEnter=false returns deny response")
        void reEnterFalse_returnsDenyResponse() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put(Constants.RE_ENTER, false);
            inputMap.put(Constants.STATE_ID, STATE_ID_VALUE);

            when(internalStateManager.get(STATE_ID_VALUE)).thenReturn(stateWithValidationResults());
            ExtensionResponse mockResponse = mock(ExtensionResponse.class);
            when(responseGenerator.generateFetchDenyResponse(
                    eq(ExtensionResponse.Error.ExceptionType.INPUT_ERROR),
                    anyList(),
                    eq(Constants.HANDLE_REENTER_GET_PAGE_IN_SPACE),
                    eq(inputMap)))
                    .thenReturn(mockResponse);

            ExtensionResponse result = helper.confirmReenterGetPageInSpace(inputMap);

            assertSame(mockResponse, result);
        }

        @Test
        @DisplayName("reEnter=true returns fetch response")
        void reEnterTrue_returnsFetchResponse() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put(Constants.RE_ENTER, true);
            inputMap.put(Constants.STATE_ID, STATE_ID_VALUE);

            when(internalStateManager.get(STATE_ID_VALUE)).thenReturn(stateWithValidationResults());
            ExtensionResponse mockResponse = mock(ExtensionResponse.class);
            when(responseGenerator.generateFetchResponse(
                    eq(ExtensionResponse.Error.ExceptionType.INPUT_ERROR),
                    anyList(),
                    eq(Constants.HANDLE_REENTER_GET_PAGE_IN_SPACE),
                    eq(inputMap)))
                    .thenReturn(mockResponse);

            ExtensionResponse result = helper.confirmReenterGetPageInSpace(inputMap);

            assertSame(mockResponse, result);
        }

        @Test
        @DisplayName("exception thrown returns failure response")
        void exceptionThrown_returnsFailure() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put(Constants.RE_ENTER, true);
            inputMap.put(Constants.STATE_ID, STATE_ID_VALUE);

            when(internalStateManager.get(STATE_ID_VALUE))
                    .thenThrow(new RuntimeException("state error"));

            ExtensionResponse result = helper.confirmReenterGetPageInSpace(inputMap);

            assertEquals(ExtensionResponse.Result.FAILURE, result.getResult());
            assertTrue(result.getError().getErrorMessage().contains(Constants.FAILED_TO_GET_PAGE_IN_SPACE_VALIDATION));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  handleReenterGetPageInSpace
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("handleReenterGetPageInSpace")
    class HandleReenterGetPageInSpace {

        @Test
        @DisplayName("success: downloads page as docx and returns file with parsing status")
        void success_returnsFileAndParsingSuccess() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put("Page ID", "pg-1");
            inputMap.put("Export Format", "docx");

            File mockFile = mock(File.class);
            when(pageDocxService.downloadPageAsDocxFileWithStatus("pg-1", "docx"))
                    .thenReturn(Pair.of(mockFile, true));

            ExtensionResponse result = helper.handleReenterGetPageInSpace(inputMap);

            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());
            assertSame(mockFile, result.getResponseValue().get("Downloaded File"));
            assertEquals(true, result.getResponseValue().get("Parsing Success"));
            verify(pageDocxService).downloadPageAsDocxFileWithStatus("pg-1", "docx");
        }

        @Test
        @DisplayName("success: parsing failure returns false for Parsing Success")
        void success_parsingFailure() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put("Page ID", "pg-2");
            inputMap.put("Export Format", "docx");

            File mockFile = mock(File.class);
            when(pageDocxService.downloadPageAsDocxFileWithStatus("pg-2", "docx"))
                    .thenReturn(Pair.of(mockFile, false));

            ExtensionResponse result = helper.handleReenterGetPageInSpace(inputMap);

            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());
            assertEquals(false, result.getResponseValue().get("Parsing Success"));
        }

        @Test
        @DisplayName("exception: returns failure response")
        void exception_returnsFailure() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put("Page ID", "bad-pg");
            inputMap.put("Export Format", "docx");

            when(pageDocxService.downloadPageAsDocxFileWithStatus("bad-pg", "docx"))
                    .thenThrow(new RuntimeException("Download failed"));

            ExtensionResponse result = helper.handleReenterGetPageInSpace(inputMap);

            assertEquals(ExtensionResponse.Result.FAILURE, result.getResult());
            assertTrue(result.getError().getErrorMessage().contains(Constants.FAILED_TO_GET_PAGE_IN_SPACE));
            assertTrue(result.getError().getErrorMessage().contains("Download failed"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  confirmReenterGetDirectChildPagesOfPage
    //  Note: This method uses direct cast (List<ValidationResult>) state.get(...)
    //  instead of getValidationResults()
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("confirmReenterGetDirectChildPagesOfPage")
    class ConfirmReenterGetDirectChildPagesOfPage {

        @Test
        @DisplayName("reEnter=false returns deny response via direct cast")
        void reEnterFalse_returnsDenyResponse() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put(Constants.RE_ENTER, false);
            inputMap.put(Constants.STATE_ID, STATE_ID_VALUE);

            Map<String, Object> state = new HashMap<>();
            state.put(Constants.VALIDATION_RESULTS, List.of(validationResult()));
            when(internalStateManager.get(STATE_ID_VALUE)).thenReturn(state);

            ExtensionResponse mockResponse = mock(ExtensionResponse.class);
            when(responseGenerator.generateFetchDenyResponse(
                    eq(ExtensionResponse.Error.ExceptionType.INPUT_ERROR),
                    anyList(), isNull(), eq(Map.of())))
                    .thenReturn(mockResponse);

            ExtensionResponse result = helper.confirmReenterGetDirectChildPagesOfPage(inputMap);

            assertSame(mockResponse, result);
            verify(responseGenerator).generateFetchDenyResponse(
                    eq(ExtensionResponse.Error.ExceptionType.INPUT_ERROR),
                    anyList(), isNull(), eq(Map.of()));
        }

        @Test
        @DisplayName("reEnter=true returns fetch response via direct cast")
        void reEnterTrue_returnsFetchResponse() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put(Constants.RE_ENTER, true);
            inputMap.put(Constants.STATE_ID, STATE_ID_VALUE);

            Map<String, Object> state = new HashMap<>();
            state.put(Constants.VALIDATION_RESULTS, List.of(validationResult()));
            when(internalStateManager.get(STATE_ID_VALUE)).thenReturn(state);

            ExtensionResponse mockResponse = mock(ExtensionResponse.class);
            when(responseGenerator.generateFetchResponse(
                    eq(ExtensionResponse.Error.ExceptionType.INPUT_ERROR),
                    anyList(),
                    eq(Constants.HANDLE_REENTER_GET_DIRECT_CHILD_PAGES_OF_PAGE),
                    eq(state)))
                    .thenReturn(mockResponse);

            ExtensionResponse result = helper.confirmReenterGetDirectChildPagesOfPage(inputMap);

            assertSame(mockResponse, result);
        }

        @Test
        @DisplayName("reEnter=null treated as accept, returns fetch response")
        void reEnterNull_treatedAsAccept() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put(Constants.RE_ENTER, null);
            inputMap.put(Constants.STATE_ID, STATE_ID_VALUE);

            Map<String, Object> state = new HashMap<>();
            state.put(Constants.VALIDATION_RESULTS, List.of(validationResult()));
            when(internalStateManager.get(STATE_ID_VALUE)).thenReturn(state);

            ExtensionResponse mockResponse = mock(ExtensionResponse.class);
            when(responseGenerator.generateFetchResponse(
                    eq(ExtensionResponse.Error.ExceptionType.INPUT_ERROR),
                    anyList(),
                    eq(Constants.HANDLE_REENTER_GET_DIRECT_CHILD_PAGES_OF_PAGE),
                    eq(state)))
                    .thenReturn(mockResponse);

            ExtensionResponse result = helper.confirmReenterGetDirectChildPagesOfPage(inputMap);

            assertSame(mockResponse, result);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  handleReenterGetDirectChildPagesOfPage
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("handleReenterGetDirectChildPagesOfPage")
    class HandleReenterGetDirectChildPagesOfPage {

        @Test
        @DisplayName("success: retrieves child pages and returns success with identifiers")
        void success_returnsPagesAndIdentifiers() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put(Constants.STATE_ID, STATE_ID_VALUE);
            inputMap.put(Constants.PAGE_ID, "pg-1");

            Page dto1 = dtoPage("child-1", "Child Page 1");
            Page dto2 = dtoPage("child-2", "Child Page 2");
            when(confluenceService.getDirectChildPagesOfPage("pg-1"))
                    .thenReturn(new Page[]{dto1, dto2});

            ExtensionResponse result = helper.handleReenterGetDirectChildPagesOfPage(inputMap);

            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());
            List<?> pages = (List<?>) result.getResponseValue().get(Constants.PAGES);
            List<?> ids = (List<?>) result.getResponseValue().get(Constants.PAGE_IDENTIFIERS);
            assertEquals(2, pages.size());
            assertEquals(2, ids.size());
            assertTrue(ids.contains("child-1"));
            assertTrue(ids.contains("child-2"));
            verify(confluenceService).getDirectChildPagesOfPage("pg-1");
        }

        @Test
        @DisplayName("success: returns empty lists when no child pages")
        void success_emptyChildPages() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put(Constants.STATE_ID, STATE_ID_VALUE);
            inputMap.put(Constants.PAGE_ID, "pg-1");

            when(confluenceService.getDirectChildPagesOfPage("pg-1")).thenReturn(new Page[0]);

            ExtensionResponse result = helper.handleReenterGetDirectChildPagesOfPage(inputMap);

            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());
            assertEquals(0, ((List<?>) result.getResponseValue().get(Constants.PAGES)).size());
        }

        @Test
        @DisplayName("exception: returns failure response")
        void exception_returnsFailure() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put(Constants.STATE_ID, STATE_ID_VALUE);
            inputMap.put(Constants.PAGE_ID, "bad-pg");

            when(confluenceService.getDirectChildPagesOfPage("bad-pg"))
                    .thenThrow(new RuntimeException("Page not accessible"));

            ExtensionResponse result = helper.handleReenterGetDirectChildPagesOfPage(inputMap);

            assertEquals(ExtensionResponse.Result.FAILURE, result.getResult());
            assertTrue(result.getError().getErrorMessage().contains(Constants.FAILED_TO_GET_PAGES_IN_SPACE));
            assertTrue(result.getError().getErrorMessage().contains("Page not accessible"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  confirmReenterCheckPageHasChildren
    //  Note: This method uses direct cast (List<ValidationResult>) state.get(...)
    //  instead of getValidationResults()
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("confirmReenterCheckPageHasChildren")
    class ConfirmReenterCheckPageHasChildren {

        @Test
        @DisplayName("reEnter=false returns deny response via direct cast")
        void reEnterFalse_returnsDenyResponse() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put(Constants.RE_ENTER, false);
            inputMap.put(Constants.STATE_ID, STATE_ID_VALUE);

            Map<String, Object> state = new HashMap<>();
            state.put(Constants.VALIDATION_RESULTS, List.of(validationResult()));
            when(internalStateManager.get(STATE_ID_VALUE)).thenReturn(state);

            ExtensionResponse mockResponse = mock(ExtensionResponse.class);
            when(responseGenerator.generateFetchDenyResponse(
                    eq(ExtensionResponse.Error.ExceptionType.INPUT_ERROR),
                    anyList(), isNull(), eq(Map.of())))
                    .thenReturn(mockResponse);

            ExtensionResponse result = helper.confirmReenterCheckPageHasChildren(inputMap);

            assertSame(mockResponse, result);
            verify(responseGenerator).generateFetchDenyResponse(
                    eq(ExtensionResponse.Error.ExceptionType.INPUT_ERROR),
                    anyList(), isNull(), eq(Map.of()));
        }

        @Test
        @DisplayName("reEnter=true returns fetch response via direct cast")
        void reEnterTrue_returnsFetchResponse() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put(Constants.RE_ENTER, true);
            inputMap.put(Constants.STATE_ID, STATE_ID_VALUE);

            Map<String, Object> state = new HashMap<>();
            state.put(Constants.VALIDATION_RESULTS, List.of(validationResult()));
            when(internalStateManager.get(STATE_ID_VALUE)).thenReturn(state);

            ExtensionResponse mockResponse = mock(ExtensionResponse.class);
            when(responseGenerator.generateFetchResponse(
                    eq(ExtensionResponse.Error.ExceptionType.INPUT_ERROR),
                    anyList(),
                    eq(Constants.HANDLE_REENTER_CHECK_PAGE_HAS_CHILDREN),
                    eq(state)))
                    .thenReturn(mockResponse);

            ExtensionResponse result = helper.confirmReenterCheckPageHasChildren(inputMap);

            assertSame(mockResponse, result);
        }

        @Test
        @DisplayName("reEnter=null treated as accept, returns fetch response")
        void reEnterNull_treatedAsAccept() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put(Constants.RE_ENTER, null);
            inputMap.put(Constants.STATE_ID, STATE_ID_VALUE);

            Map<String, Object> state = new HashMap<>();
            state.put(Constants.VALIDATION_RESULTS, List.of(validationResult()));
            when(internalStateManager.get(STATE_ID_VALUE)).thenReturn(state);

            ExtensionResponse mockResponse = mock(ExtensionResponse.class);
            when(responseGenerator.generateFetchResponse(
                    eq(ExtensionResponse.Error.ExceptionType.INPUT_ERROR),
                    anyList(),
                    eq(Constants.HANDLE_REENTER_CHECK_PAGE_HAS_CHILDREN),
                    eq(state)))
                    .thenReturn(mockResponse);

            ExtensionResponse result = helper.confirmReenterCheckPageHasChildren(inputMap);

            assertSame(mockResponse, result);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  handleReenterCheckPageHasChildren
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("handleReenterCheckPageHasChildren")
    class HandleReenterCheckPageHasChildren {

        @Test
        @DisplayName("success: page has children returns true")
        void success_hasChildren_returnsTrue() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put(Constants.STATE_ID, STATE_ID_VALUE);
            inputMap.put(Constants.PAGE_ID, "pg-1");

            when(confluenceService.hasChildPages("pg-1")).thenReturn(true);

            ExtensionResponse result = helper.handleReenterCheckPageHasChildren(inputMap);

            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());
            assertEquals(true, result.getResponseValue().get("Has Children"));
            verify(confluenceService).hasChildPages("pg-1");
        }

        @Test
        @DisplayName("success: page has no children returns false")
        void success_noChildren_returnsFalse() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put(Constants.STATE_ID, STATE_ID_VALUE);
            inputMap.put(Constants.PAGE_ID, "pg-2");

            when(confluenceService.hasChildPages("pg-2")).thenReturn(false);

            ExtensionResponse result = helper.handleReenterCheckPageHasChildren(inputMap);

            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());
            assertEquals(false, result.getResponseValue().get("Has Children"));
        }

        @Test
        @DisplayName("exception: returns failure response")
        void exception_returnsFailure() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put(Constants.STATE_ID, STATE_ID_VALUE);
            inputMap.put(Constants.PAGE_ID, "bad-pg");

            when(confluenceService.hasChildPages("bad-pg"))
                    .thenThrow(new RuntimeException("API error"));

            ExtensionResponse result = helper.handleReenterCheckPageHasChildren(inputMap);

            assertEquals(ExtensionResponse.Result.FAILURE, result.getResult());
            assertTrue(result.getError().getErrorMessage().contains(Constants.FAILED_TO_GET_PAGE_BY_ID));
            assertTrue(result.getError().getErrorMessage().contains("API error"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  getValidationResults (private) - coverage via confirmReenter methods
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getValidationResults private method coverage")
    class GetValidationResultsCoverage {

        @Test
        @DisplayName("non-empty validation results list exercises for-loop body")
        void nonEmptyValidationResults_forLoopExecutes() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put(Constants.RE_ENTER, true);
            inputMap.put(Constants.STATE_ID, STATE_ID_VALUE);

            when(internalStateManager.get(STATE_ID_VALUE)).thenReturn(stateWithValidationResults());
            ExtensionResponse mockResponse = mock(ExtensionResponse.class);
            when(responseGenerator.generateFetchResponse(any(), anyList(), anyString(), anyMap()))
                    .thenReturn(mockResponse);

            ExtensionResponse result = helper.confirmReenterGetPagesInSpace(inputMap);

            assertSame(mockResponse, result);
        }

        @Test
        @DisplayName("empty validation results list skips for-loop body")
        void emptyValidationResults_forLoopSkipped() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put(Constants.RE_ENTER, true);
            inputMap.put(Constants.STATE_ID, STATE_ID_VALUE);

            when(internalStateManager.get(STATE_ID_VALUE)).thenReturn(stateWithEmptyValidationResults());
            ExtensionResponse mockResponse = mock(ExtensionResponse.class);
            when(responseGenerator.generateFetchResponse(any(), anyList(), anyString(), anyMap()))
                    .thenReturn(mockResponse);

            ExtensionResponse result = helper.confirmReenterGetPageById(inputMap);

            assertSame(mockResponse, result);
        }

        @Test
        @DisplayName("null validation results key exercises null branch")
        void nullValidationResults_nullBranch() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put(Constants.RE_ENTER, true);
            inputMap.put(Constants.STATE_ID, STATE_ID_VALUE);

            when(internalStateManager.get(STATE_ID_VALUE)).thenReturn(stateWithNullValidationResults());
            ExtensionResponse mockResponse = mock(ExtensionResponse.class);
            when(responseGenerator.generateFetchResponse(any(), anyList(), anyString(), anyMap()))
                    .thenReturn(mockResponse);

            ExtensionResponse result = helper.confirmReenterUpdatePageByContent(inputMap);

            assertSame(mockResponse, result);
        }
    }
}
