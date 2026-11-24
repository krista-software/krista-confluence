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
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.errorhandlers.ErrorHandlingStateManager;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.errorhandlers.ExtensionResponseGenerator;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.validators.ValidationOrchestrator;
import com.kristasoft.extensions.essentials.collaboration.confluence.dto.Template;
import com.kristasoft.extensions.essentials.collaboration.confluence.service.ConfluenceService;
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
 * Comprehensive JUnit 5 tests for {@link TemplateAreaSubCatalogRequestHelper}.
 * Targets 100% code coverage of all public methods, including try/catch branches
 * and the private getValidationResults helper.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TemplateAreaSubCatalogRequestHelper")
class TemplateAreaSubCatalogRequestHelperTest {

    @Mock private ConfluenceService confluenceService;
    @Mock private ErrorHandlingStateManager internalStateManager;
    @Mock private ExtensionResponseGenerator responseGenerator;
    @Mock private ValidationOrchestrator validationOrchestrator;

    private TemplateAreaSubCatalogRequestHelper helper;

    private static final String STATE_ID_VALUE = "state-1";

    @BeforeEach
    void setUp() {
        helper = new TemplateAreaSubCatalogRequestHelper(
                confluenceService, internalStateManager, responseGenerator, validationOrchestrator);
    }

    // ── Shared helper methods ────────────────────────────────────────────────

    private static ValidationOrchestrator.ValidationResult validationResult() {
        return new ValidationOrchestrator.ValidationResult(
                "confirm step", "Template ID", "fetch step",
                "Template ID is not valid", "Identifier", Map.of());
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

    private static Template dtoTemplate(String id, String name) {
        return Template.builder().setTemplateId(id).setName(name).build();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  confirmReenterGetTemplateById
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("confirmReenterGetTemplateById")
    class ConfirmReenterGetTemplateById {

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
                    eq(Constants.HANDLE_REENTER_GET_TEMPLATE_BY_ID),
                    eq(inputMap)))
                    .thenReturn(mockResponse);

            ExtensionResponse result = helper.confirmReenterGetTemplateById(inputMap);

            assertSame(mockResponse, result);
            verify(responseGenerator).generateFetchDenyResponse(
                    eq(ExtensionResponse.Error.ExceptionType.INPUT_ERROR),
                    anyList(),
                    eq(Constants.HANDLE_REENTER_GET_TEMPLATE_BY_ID),
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
                    eq(Constants.HANDLE_REENTER_GET_TEMPLATE_BY_ID),
                    eq(inputMap)))
                    .thenReturn(mockResponse);

            ExtensionResponse result = helper.confirmReenterGetTemplateById(inputMap);

            assertSame(mockResponse, result);
            verify(responseGenerator).generateFetchResponse(
                    eq(ExtensionResponse.Error.ExceptionType.INPUT_ERROR),
                    anyList(),
                    eq(Constants.HANDLE_REENTER_GET_TEMPLATE_BY_ID),
                    eq(inputMap));
            verify(responseGenerator, never()).generateFetchDenyResponse(any(), anyList(), anyString(), anyMap());
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
                    eq(Constants.HANDLE_REENTER_GET_TEMPLATE_BY_ID),
                    eq(inputMap)))
                    .thenReturn(mockResponse);

            ExtensionResponse result = helper.confirmReenterGetTemplateById(inputMap);

            assertSame(mockResponse, result);
        }

        @Test
        @DisplayName("exception thrown in try block returns failure response")
        void exceptionThrown_returnsFailure() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put(Constants.RE_ENTER, true);
            inputMap.put(Constants.STATE_ID, STATE_ID_VALUE);

            when(internalStateManager.get(STATE_ID_VALUE))
                    .thenThrow(new RuntimeException("state store unavailable"));

            ExtensionResponse result = helper.confirmReenterGetTemplateById(inputMap);

            assertNotNull(result);
            assertEquals(ExtensionResponse.Result.FAILURE, result.getResult());
            assertNotNull(result.getError());
            assertEquals(ExtensionResponse.Error.ExceptionType.INPUT_ERROR, result.getError().getExceptionType());
            assertTrue(result.getError().getErrorMessage().contains(Constants.FAILED_TO_GET_TEMPLATE_BY_ID_VALIDATION));
            assertTrue(result.getError().getErrorMessage().contains("state store unavailable"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  handleReenterGetTemplateById
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("handleReenterGetTemplateById")
    class HandleReenterGetTemplateById {

        @Test
        @DisplayName("success: retrieves template and returns success response")
        void success_returnsTemplateEntity() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put(Constants.STATE_ID, STATE_ID_VALUE);
            inputMap.put("Template ID", "tmpl-1");

            when(confluenceService.getTemplateById("tmpl-1")).thenReturn(dtoTemplate("tmpl-1", "My Template"));

            ExtensionResponse result = helper.handleReenterGetTemplateById(inputMap);

            assertNotNull(result);
            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());
            assertNotNull(result.getResponseValue().get("Template"));
            verify(confluenceService).getTemplateById("tmpl-1");
        }

        @Test
        @DisplayName("exception: returns failure response with error message")
        void exception_returnsFailure() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put(Constants.STATE_ID, STATE_ID_VALUE);
            inputMap.put("Template ID", "bad-id");

            when(confluenceService.getTemplateById("bad-id"))
                    .thenThrow(new RuntimeException("Template not found"));

            ExtensionResponse result = helper.handleReenterGetTemplateById(inputMap);

            assertNotNull(result);
            assertEquals(ExtensionResponse.Result.FAILURE, result.getResult());
            assertNotNull(result.getError());
            assertEquals(ExtensionResponse.Error.ExceptionType.INPUT_ERROR, result.getError().getExceptionType());
            assertTrue(result.getError().getErrorMessage().contains(Constants.FAILED_TO_GET_TEMPLATE_BY_ID));
            assertTrue(result.getError().getErrorMessage().contains("Template not found"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  confirmReenterGetTemplatesBySpaceId
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("confirmReenterGetTemplatesBySpaceId")
    class ConfirmReenterGetTemplatesBySpaceId {

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
                    eq(Constants.HANDLE_REENTER_GET_TEMPLATES_BY_SPACE_ID),
                    eq(inputMap)))
                    .thenReturn(mockResponse);

            ExtensionResponse result = helper.confirmReenterGetTemplatesBySpaceId(inputMap);

            assertSame(mockResponse, result);
            verify(responseGenerator).generateFetchDenyResponse(
                    eq(ExtensionResponse.Error.ExceptionType.INPUT_ERROR),
                    anyList(),
                    eq(Constants.HANDLE_REENTER_GET_TEMPLATES_BY_SPACE_ID),
                    eq(inputMap));
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
                    eq(Constants.HANDLE_REENTER_GET_TEMPLATES_BY_SPACE_ID),
                    eq(inputMap)))
                    .thenReturn(mockResponse);

            ExtensionResponse result = helper.confirmReenterGetTemplatesBySpaceId(inputMap);

            assertSame(mockResponse, result);
            verify(responseGenerator).generateFetchResponse(
                    eq(ExtensionResponse.Error.ExceptionType.INPUT_ERROR),
                    anyList(),
                    eq(Constants.HANDLE_REENTER_GET_TEMPLATES_BY_SPACE_ID),
                    eq(inputMap));
        }

        @Test
        @DisplayName("reEnter=null treated as accept, returns fetch response")
        void reEnterNull_treatedAsAccept() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put(Constants.RE_ENTER, null);
            inputMap.put(Constants.STATE_ID, STATE_ID_VALUE);

            when(internalStateManager.get(STATE_ID_VALUE)).thenReturn(stateWithEmptyValidationResults());
            ExtensionResponse mockResponse = mock(ExtensionResponse.class);
            when(responseGenerator.generateFetchResponse(
                    eq(ExtensionResponse.Error.ExceptionType.INPUT_ERROR),
                    anyList(),
                    eq(Constants.HANDLE_REENTER_GET_TEMPLATES_BY_SPACE_ID),
                    eq(inputMap)))
                    .thenReturn(mockResponse);

            ExtensionResponse result = helper.confirmReenterGetTemplatesBySpaceId(inputMap);

            assertSame(mockResponse, result);
        }

        @Test
        @DisplayName("exception thrown in try block returns failure response")
        void exceptionThrown_returnsFailure() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put(Constants.RE_ENTER, false);
            inputMap.put(Constants.STATE_ID, STATE_ID_VALUE);

            when(internalStateManager.get(STATE_ID_VALUE))
                    .thenThrow(new RuntimeException("state store crash"));

            ExtensionResponse result = helper.confirmReenterGetTemplatesBySpaceId(inputMap);

            assertNotNull(result);
            assertEquals(ExtensionResponse.Result.FAILURE, result.getResult());
            assertNotNull(result.getError());
            assertEquals(ExtensionResponse.Error.ExceptionType.INPUT_ERROR, result.getError().getExceptionType());
            assertTrue(result.getError().getErrorMessage().contains(Constants.FAILED_TO_GET_TEMPLATES_BY_SPACE_ID_VALIDATION));
            assertTrue(result.getError().getErrorMessage().contains("state store crash"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  handleReenterGetTemplatesBySpaceId
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("handleReenterGetTemplatesBySpaceId")
    class HandleReenterGetTemplatesBySpaceId {

        @Test
        @DisplayName("success: retrieves templates and returns success response with identifiers")
        void success_returnsTemplatesAndIdentifiers() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put(Constants.STATE_ID, STATE_ID_VALUE);
            inputMap.put("Space ID", "sp-1");

            Template dto1 = dtoTemplate("tmpl-1", "Template One");
            Template dto2 = dtoTemplate("tmpl-2", "Template Two");
            when(confluenceService.getTemplatesBySpaceId("sp-1"))
                    .thenReturn(new Template[]{dto1, dto2});

            ExtensionResponse result = helper.handleReenterGetTemplatesBySpaceId(inputMap);

            assertNotNull(result);
            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());
            @SuppressWarnings("unchecked")
            List<?> templates = (List<?>) result.getResponseValue().get("Templates");
            @SuppressWarnings("unchecked")
            List<?> identifiers = (List<?>) result.getResponseValue().get("Template Identifiers");
            assertEquals(2, templates.size());
            assertEquals(2, identifiers.size());
            assertTrue(identifiers.contains("tmpl-1"));
            assertTrue(identifiers.contains("tmpl-2"));
            verify(confluenceService).getTemplatesBySpaceId("sp-1");
        }

        @Test
        @DisplayName("success: returns empty lists when no templates")
        void success_emptyTemplates() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put(Constants.STATE_ID, STATE_ID_VALUE);
            inputMap.put("Space ID", "sp-empty");

            when(confluenceService.getTemplatesBySpaceId("sp-empty")).thenReturn(new Template[0]);

            ExtensionResponse result = helper.handleReenterGetTemplatesBySpaceId(inputMap);

            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());
            assertEquals(0, ((List<?>) result.getResponseValue().get("Templates")).size());
            assertEquals(0, ((List<?>) result.getResponseValue().get("Template Identifiers")).size());
        }

        @Test
        @DisplayName("exception: returns failure response with error message")
        void exception_returnsFailure() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put(Constants.STATE_ID, STATE_ID_VALUE);
            inputMap.put("Space ID", "bad-space");

            when(confluenceService.getTemplatesBySpaceId("bad-space"))
                    .thenThrow(new RuntimeException("API error"));

            ExtensionResponse result = helper.handleReenterGetTemplatesBySpaceId(inputMap);

            assertNotNull(result);
            assertEquals(ExtensionResponse.Result.FAILURE, result.getResult());
            assertNotNull(result.getError());
            assertEquals(ExtensionResponse.Error.ExceptionType.INPUT_ERROR, result.getError().getExceptionType());
            assertTrue(result.getError().getErrorMessage().contains(Constants.FAILED_TO_GET_TEMPLATES_BY_SPACE_ID));
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

            ExtensionResponse result = helper.confirmReenterGetTemplateById(inputMap);

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

            ExtensionResponse result = helper.confirmReenterGetTemplateById(inputMap);

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

            ExtensionResponse result = helper.confirmReenterGetTemplatesBySpaceId(inputMap);

            assertSame(mockResponse, result);
        }
    }
}
