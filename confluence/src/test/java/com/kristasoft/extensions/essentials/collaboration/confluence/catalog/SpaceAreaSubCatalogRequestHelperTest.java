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
import com.kristasoft.extensions.essentials.collaboration.confluence.dto.Page;
import com.kristasoft.extensions.essentials.collaboration.confluence.dto.Space;
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
 * Comprehensive JUnit 5 tests for {@link SpaceAreaSubCatalogRequestHelper}.
 * Targets 100% code coverage of all public methods and the private getValidationResults helper.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SpaceAreaSubCatalogRequestHelper")
class SpaceAreaSubCatalogRequestHelperTest {

    @Mock private ConfluenceService confluenceService;
    @Mock private ErrorHandlingStateManager internalStateManager;
    @Mock private ExtensionResponseGenerator responseGenerator;

    private SpaceAreaSubCatalogRequestHelper helper;

    private static final String STATE_ID_VALUE = "state-1";

    @BeforeEach
    void setUp() {
        helper = new SpaceAreaSubCatalogRequestHelper(confluenceService, internalStateManager, responseGenerator);
    }

    // ── Shared helper methods ────────────────────────────────────────────────

    private static ValidationOrchestrator.ValidationResult validationResult() {
        return new ValidationOrchestrator.ValidationResult(
                "confirm step", "Space ID", "fetch step",
                "Space ID is not valid", "Identifier", Map.of());
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

    private static Space dtoSpace(String id, String name) {
        return Space.builder().setId(id).setName(name).build();
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
    //  confirmReenterGetSpaceById
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("confirmReenterGetSpaceById")
    class ConfirmReenterGetSpaceById {

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
                    anyList(), isNull(), eq(Map.of())))
                    .thenReturn(mockResponse);

            ExtensionResponse result = helper.confirmReenterGetSpaceById(inputMap);

            assertSame(mockResponse, result);
            verify(responseGenerator).generateFetchDenyResponse(
                    eq(ExtensionResponse.Error.ExceptionType.INPUT_ERROR),
                    anyList(), isNull(), eq(Map.of()));
            verify(responseGenerator, never()).generateFetchResponse(any(), anyList(), anyString(), anyMap());
        }

        @Test
        @DisplayName("reEnter=true returns fetch response")
        void reEnterTrue_returnsFetchResponse() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put(Constants.RE_ENTER, true);
            inputMap.put(Constants.STATE_ID, STATE_ID_VALUE);

            Map<String, Object> state = stateWithEmptyValidationResults();
            when(internalStateManager.get(STATE_ID_VALUE)).thenReturn(state);
            ExtensionResponse mockResponse = mock(ExtensionResponse.class);
            when(responseGenerator.generateFetchResponse(
                    eq(ExtensionResponse.Error.ExceptionType.INPUT_ERROR),
                    anyList(),
                    eq(Constants.HANDLE_REENTER_GET_SPACE_BY_ID),
                    eq(state)))
                    .thenReturn(mockResponse);

            ExtensionResponse result = helper.confirmReenterGetSpaceById(inputMap);

            assertSame(mockResponse, result);
            verify(responseGenerator).generateFetchResponse(
                    eq(ExtensionResponse.Error.ExceptionType.INPUT_ERROR),
                    anyList(),
                    eq(Constants.HANDLE_REENTER_GET_SPACE_BY_ID),
                    eq(state));
            verify(responseGenerator, never()).generateFetchDenyResponse(any(), anyList(), any(), anyMap());
        }

        @Test
        @DisplayName("reEnter=null treated as accept, returns fetch response")
        void reEnterNull_treatedAsAccept() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put(Constants.RE_ENTER, null);
            inputMap.put(Constants.STATE_ID, STATE_ID_VALUE);

            Map<String, Object> state = stateWithNullValidationResults();
            when(internalStateManager.get(STATE_ID_VALUE)).thenReturn(state);
            ExtensionResponse mockResponse = mock(ExtensionResponse.class);
            when(responseGenerator.generateFetchResponse(
                    eq(ExtensionResponse.Error.ExceptionType.INPUT_ERROR),
                    anyList(),
                    eq(Constants.HANDLE_REENTER_GET_SPACE_BY_ID),
                    eq(state)))
                    .thenReturn(mockResponse);

            ExtensionResponse result = helper.confirmReenterGetSpaceById(inputMap);

            assertSame(mockResponse, result);
            verify(responseGenerator).generateFetchResponse(any(), anyList(), anyString(), anyMap());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  handleReenterGetSpaceById
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("handleReenterGetSpaceById")
    class HandleReenterGetSpaceById {

        @Test
        @DisplayName("success: retrieves space and returns success response")
        void success_returnsSpaceEntity() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put(Constants.STATE_ID, STATE_ID_VALUE);
            inputMap.put(Constants.SPACE_ID, "sp-1");

            when(confluenceService.getSpaceById("sp-1")).thenReturn(dtoSpace("sp-1", "My Space"));

            ExtensionResponse result = helper.handleReenterGetSpaceById(inputMap);

            assertNotNull(result);
            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());
            assertNotNull(result.getResponseValue().get("Space"));
            verify(confluenceService).getSpaceById("sp-1");
        }

        @Test
        @DisplayName("exception: returns failure response with error message")
        void exception_returnsFailure() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put(Constants.STATE_ID, STATE_ID_VALUE);
            inputMap.put(Constants.SPACE_ID, "bad-id");

            when(confluenceService.getSpaceById("bad-id"))
                    .thenThrow(new RuntimeException("Space not found"));

            ExtensionResponse result = helper.handleReenterGetSpaceById(inputMap);

            assertNotNull(result);
            assertEquals(ExtensionResponse.Result.FAILURE, result.getResult());
            assertNotNull(result.getError());
            assertEquals(ExtensionResponse.Error.ExceptionType.INPUT_ERROR, result.getError().getExceptionType());
            assertTrue(result.getError().getErrorMessage().contains(Constants.FAILED_TO_GET_SPACE_BY_ID));
            assertTrue(result.getError().getErrorMessage().contains("Space not found"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  confirmReenterGetSpaces
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("confirmReenterGetSpaces")
    class ConfirmReenterGetSpaces {

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
                    anyList(), isNull(), eq(Map.of())))
                    .thenReturn(mockResponse);

            ExtensionResponse result = helper.confirmReenterGetSpaces(inputMap);

            assertSame(mockResponse, result);
            verify(responseGenerator).generateFetchDenyResponse(
                    eq(ExtensionResponse.Error.ExceptionType.INPUT_ERROR),
                    anyList(), isNull(), eq(Map.of()));
            verify(responseGenerator, never()).generateFetchResponse(any(), anyList(), anyString(), anyMap());
        }

        @Test
        @DisplayName("reEnter=true returns fetch response")
        void reEnterTrue_returnsFetchResponse() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put(Constants.RE_ENTER, true);
            inputMap.put(Constants.STATE_ID, STATE_ID_VALUE);

            Map<String, Object> state = stateWithValidationResults();
            when(internalStateManager.get(STATE_ID_VALUE)).thenReturn(state);
            ExtensionResponse mockResponse = mock(ExtensionResponse.class);
            when(responseGenerator.generateFetchResponse(
                    eq(ExtensionResponse.Error.ExceptionType.INPUT_ERROR),
                    anyList(),
                    eq(Constants.HANDLE_REENTER_GET_SPACES),
                    eq(state)))
                    .thenReturn(mockResponse);

            ExtensionResponse result = helper.confirmReenterGetSpaces(inputMap);

            assertSame(mockResponse, result);
            verify(responseGenerator).generateFetchResponse(
                    eq(ExtensionResponse.Error.ExceptionType.INPUT_ERROR),
                    anyList(),
                    eq(Constants.HANDLE_REENTER_GET_SPACES),
                    eq(state));
        }

        @Test
        @DisplayName("reEnter=null treated as accept, returns fetch response")
        void reEnterNull_treatedAsAccept() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put(Constants.RE_ENTER, null);
            inputMap.put(Constants.STATE_ID, STATE_ID_VALUE);

            Map<String, Object> state = stateWithEmptyValidationResults();
            when(internalStateManager.get(STATE_ID_VALUE)).thenReturn(state);
            ExtensionResponse mockResponse = mock(ExtensionResponse.class);
            when(responseGenerator.generateFetchResponse(
                    eq(ExtensionResponse.Error.ExceptionType.INPUT_ERROR),
                    anyList(),
                    eq(Constants.HANDLE_REENTER_GET_SPACES),
                    eq(state)))
                    .thenReturn(mockResponse);

            ExtensionResponse result = helper.confirmReenterGetSpaces(inputMap);

            assertSame(mockResponse, result);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  handleReenterGetSpaces
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("handleReenterGetSpaces")
    class HandleReenterGetSpaces {

        @Test
        @DisplayName("success: retrieves spaces and returns success response with identifiers")
        void success_returnsSpacesAndIdentifiers() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put(Constants.STATE_ID, STATE_ID_VALUE);

            Space dto1 = dtoSpace("sp-1", "Space One");
            Space dto2 = dtoSpace("sp-2", "Space Two");
            when(confluenceService.getSpaces()).thenReturn(new Space[]{dto1, dto2});

            ExtensionResponse result = helper.handleReenterGetSpaces(inputMap);

            assertNotNull(result);
            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());
            @SuppressWarnings("unchecked")
            List<?> spaces = (List<?>) result.getResponseValue().get(Constants.SPACES);
            @SuppressWarnings("unchecked")
            List<?> identifiers = (List<?>) result.getResponseValue().get(Constants.SPACE_IDENTIFIERS);
            assertEquals(2, spaces.size());
            assertEquals(2, identifiers.size());
            assertTrue(identifiers.contains("sp-1"));
            assertTrue(identifiers.contains("sp-2"));
            verify(confluenceService).getSpaces();
        }

        @Test
        @DisplayName("success: returns empty lists when no spaces")
        void success_emptySpaces() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put(Constants.STATE_ID, STATE_ID_VALUE);

            when(confluenceService.getSpaces()).thenReturn(new Space[0]);

            ExtensionResponse result = helper.handleReenterGetSpaces(inputMap);

            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());
            assertEquals(0, ((List<?>) result.getResponseValue().get(Constants.SPACES)).size());
            assertEquals(0, ((List<?>) result.getResponseValue().get(Constants.SPACE_IDENTIFIERS)).size());
        }

        @Test
        @DisplayName("exception: returns failure response with error message")
        void exception_returnsFailure() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put(Constants.STATE_ID, STATE_ID_VALUE);

            when(confluenceService.getSpaces()).thenThrow(new RuntimeException("API unavailable"));

            ExtensionResponse result = helper.handleReenterGetSpaces(inputMap);

            assertNotNull(result);
            assertEquals(ExtensionResponse.Result.FAILURE, result.getResult());
            assertNotNull(result.getError());
            assertEquals(ExtensionResponse.Error.ExceptionType.INPUT_ERROR, result.getError().getExceptionType());
            assertTrue(result.getError().getErrorMessage().contains(Constants.FAILED_TO_GET_SPACES));
            assertTrue(result.getError().getErrorMessage().contains("API unavailable"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  confirmReenterGetDirectChildPagesOfSpace
    //  Note: This method uses direct cast (List<ValidationResult>) state.get(...)
    //  instead of getValidationResults()
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("confirmReenterGetDirectChildPagesOfSpace")
    class ConfirmReenterGetDirectChildPagesOfSpace {

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

            ExtensionResponse result = helper.confirmReenterGetDirectChildPagesOfSpace(inputMap);

            assertSame(mockResponse, result);
            verify(responseGenerator).generateFetchDenyResponse(
                    eq(ExtensionResponse.Error.ExceptionType.INPUT_ERROR),
                    anyList(), isNull(), eq(Map.of()));
            verify(responseGenerator, never()).generateFetchResponse(any(), anyList(), anyString(), anyMap());
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
                    eq(Constants.HANDLE_REENTER_GET_DIRECT_CHILD_PAGES_OF_SPACE),
                    eq(state)))
                    .thenReturn(mockResponse);

            ExtensionResponse result = helper.confirmReenterGetDirectChildPagesOfSpace(inputMap);

            assertSame(mockResponse, result);
            verify(responseGenerator).generateFetchResponse(
                    eq(ExtensionResponse.Error.ExceptionType.INPUT_ERROR),
                    anyList(),
                    eq(Constants.HANDLE_REENTER_GET_DIRECT_CHILD_PAGES_OF_SPACE),
                    eq(state));
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
                    eq(Constants.HANDLE_REENTER_GET_DIRECT_CHILD_PAGES_OF_SPACE),
                    eq(state)))
                    .thenReturn(mockResponse);

            ExtensionResponse result = helper.confirmReenterGetDirectChildPagesOfSpace(inputMap);

            assertSame(mockResponse, result);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  handleReenterGetDirectChildPagesOfSpace
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("handleReenterGetDirectChildPagesOfSpace")
    class HandleReenterGetDirectChildPagesOfSpace {

        @Test
        @DisplayName("success: retrieves child pages and returns success response with identifiers")
        void success_returnsPagesAndIdentifiers() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put(Constants.STATE_ID, STATE_ID_VALUE);
            inputMap.put(Constants.SPACE_ID, "sp-1");

            Page dto1 = dtoPage("pg-1", "Child Page 1");
            Page dto2 = dtoPage("pg-2", "Child Page 2");
            when(confluenceService.getDirectChildPagesOfSpace("sp-1"))
                    .thenReturn(new Page[]{dto1, dto2});

            ExtensionResponse result = helper.handleReenterGetDirectChildPagesOfSpace(inputMap);

            assertNotNull(result);
            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());
            @SuppressWarnings("unchecked")
            List<?> pages = (List<?>) result.getResponseValue().get(Constants.PAGES);
            @SuppressWarnings("unchecked")
            List<?> identifiers = (List<?>) result.getResponseValue().get(Constants.PAGE_IDENTIFIERS);
            assertEquals(2, pages.size());
            assertEquals(2, identifiers.size());
            assertTrue(identifiers.contains("pg-1"));
            assertTrue(identifiers.contains("pg-2"));
            verify(confluenceService).getDirectChildPagesOfSpace("sp-1");
        }

        @Test
        @DisplayName("success: returns empty lists when no child pages")
        void success_emptyPages() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put(Constants.STATE_ID, STATE_ID_VALUE);
            inputMap.put(Constants.SPACE_ID, "sp-1");

            when(confluenceService.getDirectChildPagesOfSpace("sp-1")).thenReturn(new Page[0]);

            ExtensionResponse result = helper.handleReenterGetDirectChildPagesOfSpace(inputMap);

            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());
            assertEquals(0, ((List<?>) result.getResponseValue().get(Constants.PAGES)).size());
            assertEquals(0, ((List<?>) result.getResponseValue().get(Constants.PAGE_IDENTIFIERS)).size());
        }

        @Test
        @DisplayName("exception: returns failure response with error message")
        void exception_returnsFailure() {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put(Constants.STATE_ID, STATE_ID_VALUE);
            inputMap.put(Constants.SPACE_ID, "bad-space");

            when(confluenceService.getDirectChildPagesOfSpace("bad-space"))
                    .thenThrow(new RuntimeException("Space not accessible"));

            ExtensionResponse result = helper.handleReenterGetDirectChildPagesOfSpace(inputMap);

            assertNotNull(result);
            assertEquals(ExtensionResponse.Result.FAILURE, result.getResult());
            assertNotNull(result.getError());
            assertEquals(ExtensionResponse.Error.ExceptionType.INPUT_ERROR, result.getError().getExceptionType());
            assertTrue(result.getError().getErrorMessage().contains(Constants.FAILED_TO_GET_PAGES_IN_SPACE));
            assertTrue(result.getError().getErrorMessage().contains("Space not accessible"));
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

            ExtensionResponse result = helper.confirmReenterGetSpaceById(inputMap);

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

            ExtensionResponse result = helper.confirmReenterGetSpaceById(inputMap);

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

            ExtensionResponse result = helper.confirmReenterGetSpaces(inputMap);

            assertSame(mockResponse, result);
        }
    }
}
