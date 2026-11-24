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
import app.krista.model.base.File;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.errorhandlers.ErrorHandlingStateManager;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.errorhandlers.ExtensionResponseGenerator;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.validators.ValidationOrchestrator;
import com.kristasoft.extensions.essentials.collaboration.confluence.service.ConfluenceService;
import com.kristasoft.extensions.essentials.collaboration.confluence.service.PageDocxService;
import com.kristasoft.extensions.essentials.collaboration.confluence.service.PageMarkdownService;
import com.kristasoft.extensions.essentials.collaboration.confluence.utils.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 100% branch-coverage tests for
 * {@link PageAreaSubCatalogRequestHelper#confirmReenterGetPageContentAsFile} and
 * {@link PageAreaSubCatalogRequestHelper#handleReenterGetPageContentAsFile}.
 *
 * <h2>Branch map</h2>
 * <pre>
 * confirmReenterGetPageContentAsFile
 *   ├─ if (Boolean.FALSE.equals(reEnter))
 *   │     T (reEnter=false)  → generateFetchDenyResponse   (test 1)
 *   │     F (reEnter=true)   → generateFetchResponse       (test 2)
 *   │     F (reEnter=null)   → generateFetchResponse       (test 3)
 *   └─ catch (Exception)     → INPUT_ERROR failure          (test 4)
 *
 * getValidationResults  (private helper, exercised via confirmReenter tests)
 *   ├─ results != null, list non-empty → for-loop body executes  (test 1)
 *   ├─ results != null, list empty     → for-loop body skipped   (test 2)
 *   └─ results == null                 → loop never reached       (test 3)
 *
 * handleReenterGetPageContentAsFile
 *   ├─ success: file returned                                     (test 5)
 *   └─ catch (Exception) → INPUT_ERROR failure                    (test 6)
 * </pre>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PageAreaSubCatalogRequestHelper – confirmReenter & handleReenter GetPageContentAsFile")
class PageAreaSubCatalogRequestHelperContentAsFileTest {

    // ─── mocks ────────────────────────────────────────────────────────────────

    @Mock private ConfluenceService confluenceService;
    @Mock private ErrorHandlingStateManager internalStateManager;
    @Mock private ExtensionResponseGenerator responseGenerator;
    @Mock private ValidationOrchestrator validationOrchestrator;
    @Mock private PageDocxService pageDocxService;
    @Mock private PageMarkdownService pageMarkdownService;

    private PageAreaSubCatalogRequestHelper helper;

    private static final String STATE_ID = "state-abc";

    @BeforeEach
    void setUp() {
        helper = new PageAreaSubCatalogRequestHelper(
                confluenceService, internalStateManager, responseGenerator,
                validationOrchestrator, pageDocxService, pageMarkdownService);
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    /** State map with one ValidationResult — exercises the for-loop body inside getValidationResults. */
    private static Map<String, Object> stateWithValidationResults() {
        ValidationOrchestrator.ValidationResult vr = new ValidationOrchestrator.ValidationResult(
                "confirm step", "Page ID", "fetch step",
                "Page ID is not valid", "Identifier", Map.of());
        Map<String, Object> state = new HashMap<>();
        state.put(Constants.VALIDATION_RESULTS, List.of(vr));
        return state;
    }

    /** State map with an empty (non-null) list — for-loop "zero iterations" branch. */
    private static Map<String, Object> stateWithEmptyValidationResults() {
        Map<String, Object> state = new HashMap<>();
        state.put(Constants.VALIDATION_RESULTS, Collections.emptyList());
        return state;
    }

    /** State map with no VALIDATION_RESULTS key — the {@code results == null} branch. */
    private static Map<String, Object> stateWithNoValidationResults() {
        return new HashMap<>();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  confirmReenterGetPageContentAsFile
    // ══════════════════════════════════════════════════════════════════════════

    /** Test 1 – reEnter=false → {@code Boolean.FALSE.equals(false)}=true → deny; non-empty validation list. */
    @Test
    @DisplayName("confirmReenter: reEnter=false → generateFetchDenyResponse called")
    void confirmReenter_reEnterFalse_generatesFetchDenyResponse() {
        Map<String, Object> inputMap = new HashMap<>();
        inputMap.put(Constants.RE_ENTER, false);
        inputMap.put(Constants.STATE_ID, STATE_ID);

        when(internalStateManager.get(STATE_ID)).thenReturn(stateWithValidationResults());
        ExtensionResponse denyResp = mock(ExtensionResponse.class);
        when(responseGenerator.generateFetchDenyResponse(
                eq(ExtensionResponse.Error.ExceptionType.INPUT_ERROR),
                anyList(),
                eq(Constants.HANDLE_REENTER_GET_PAGE_CONTENT_AS_FILE),
                eq(inputMap)))
                .thenReturn(denyResp);

        ExtensionResponse response = helper.confirmReenterGetPageContentAsFile(inputMap);

        assertSame(denyResp, response);
        verify(responseGenerator).generateFetchDenyResponse(
                eq(ExtensionResponse.Error.ExceptionType.INPUT_ERROR),
                anyList(),
                eq(Constants.HANDLE_REENTER_GET_PAGE_CONTENT_AS_FILE),
                eq(inputMap));
        verify(responseGenerator, never()).generateFetchResponse(any(), any(), any(), any());
    }

    /** Test 2 – reEnter=true → else-branch → fetch; empty validation list (for-loop skipped). */
    @Test
    @DisplayName("confirmReenter: reEnter=true → generateFetchResponse called (empty validation list)")
    void confirmReenter_reEnterTrue_generatesFetchResponse() {
        Map<String, Object> inputMap = new HashMap<>();
        inputMap.put(Constants.RE_ENTER, true);
        inputMap.put(Constants.STATE_ID, STATE_ID);

        when(internalStateManager.get(STATE_ID)).thenReturn(stateWithEmptyValidationResults());
        ExtensionResponse fetchResp = mock(ExtensionResponse.class);
        when(responseGenerator.generateFetchResponse(
                eq(ExtensionResponse.Error.ExceptionType.INPUT_ERROR),
                anyList(),
                eq(Constants.HANDLE_REENTER_GET_PAGE_CONTENT_AS_FILE),
                eq(inputMap)))
                .thenReturn(fetchResp);

        ExtensionResponse response = helper.confirmReenterGetPageContentAsFile(inputMap);

        assertSame(fetchResp, response);
        verify(responseGenerator).generateFetchResponse(
                eq(ExtensionResponse.Error.ExceptionType.INPUT_ERROR),
                anyList(),
                eq(Constants.HANDLE_REENTER_GET_PAGE_CONTENT_AS_FILE),
                eq(inputMap));
        verify(responseGenerator, never()).generateFetchDenyResponse(any(), any(), any(), any());
    }

    /** Test 3 – reEnter=null → {@code Boolean.FALSE.equals(null)}=false → else-branch; results null. */
    @Test
    @DisplayName("confirmReenter: reEnter=null treated as accept → generateFetchResponse called")
    void confirmReenter_reEnterNull_treatedAsAccept() {
        Map<String, Object> inputMap = new HashMap<>();
        inputMap.put(Constants.RE_ENTER, null);
        inputMap.put(Constants.STATE_ID, STATE_ID);

        when(internalStateManager.get(STATE_ID)).thenReturn(stateWithNoValidationResults());
        ExtensionResponse fetchResp = mock(ExtensionResponse.class);
        when(responseGenerator.generateFetchResponse(
                eq(ExtensionResponse.Error.ExceptionType.INPUT_ERROR),
                anyList(),
                eq(Constants.HANDLE_REENTER_GET_PAGE_CONTENT_AS_FILE),
                eq(inputMap)))
                .thenReturn(fetchResp);

        ExtensionResponse response = helper.confirmReenterGetPageContentAsFile(inputMap);

        assertSame(fetchResp, response);
        verify(responseGenerator).generateFetchResponse(
                eq(ExtensionResponse.Error.ExceptionType.INPUT_ERROR),
                anyList(),
                eq(Constants.HANDLE_REENTER_GET_PAGE_CONTENT_AS_FILE),
                eq(inputMap));
    }

    /** Test 4 – state store throws → catch → INPUT_ERROR failure with the validation-error prefix. */
    @Test
    @DisplayName("confirmReenter: exception thrown → INPUT_ERROR failure response")
    void confirmReenter_exceptionThrown_returnsInputErrorFailure() {
        Map<String, Object> inputMap = new HashMap<>();
        inputMap.put(Constants.RE_ENTER, false);
        inputMap.put(Constants.STATE_ID, STATE_ID);

        when(internalStateManager.get(STATE_ID))
                .thenThrow(new RuntimeException("state store unavailable"));

        ExtensionResponse response = helper.confirmReenterGetPageContentAsFile(inputMap);

        assertNotNull(response);
        assertEquals(ExtensionResponse.Result.FAILURE, response.getResult());
        assertNotNull(response.getError());
        assertEquals(ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                response.getError().getExceptionType());
        assertTrue(response.getError().getErrorMessage()
                .contains(Constants.FAILED_TO_GET_PAGE_CONTENT_AS_FILE_VALIDATION));
        assertTrue(response.getError().getErrorMessage()
                .contains("state store unavailable"));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  handleReenterGetPageContentAsFile
    // ══════════════════════════════════════════════════════════════════════════

    /** Test 5 – Happy path: page ID provided, service returns file → SUCCESS with "Downloaded File". */
    @Test
    @DisplayName("handleReenter: success → returns Downloaded File")
    void handleReenter_success_returnsDownloadedFile() {
        Map<String, Object> inputMap = new HashMap<>();
        inputMap.put("Page ID", "123");

        File mockFile = mock(File.class);
        when(pageMarkdownService.downloadPageAsMarkdownFile("123")).thenReturn(mockFile);

        ExtensionResponse response = helper.handleReenterGetPageContentAsFile(inputMap);

        assertNotNull(response);
        assertEquals(ExtensionResponse.Result.SUCCESS, response.getResult());
        assertSame(mockFile, response.getResponseValue().get("Downloaded File"));
        verify(pageMarkdownService).downloadPageAsMarkdownFile("123");
    }

    /** Test 6 – Service throws → catch → INPUT_ERROR failure with the download-failure prefix. */
    @Test
    @DisplayName("handleReenter: service throws → INPUT_ERROR failure response")
    void handleReenter_serviceThrows_returnsInputErrorFailure() {
        Map<String, Object> inputMap = new HashMap<>();
        inputMap.put("Page ID", "bad-id");

        when(pageMarkdownService.downloadPageAsMarkdownFile("bad-id"))
                .thenThrow(new RuntimeException("Markdown generation failed"));

        ExtensionResponse response = helper.handleReenterGetPageContentAsFile(inputMap);

        assertNotNull(response);
        assertEquals(ExtensionResponse.Result.FAILURE, response.getResult());
        assertNotNull(response.getError());
        assertEquals(ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                response.getError().getExceptionType());
        assertTrue(response.getError().getErrorMessage()
                .contains(Constants.FAILED_TO_GET_PAGE_CONTENT_AS_FILE));
        assertTrue(response.getError().getErrorMessage()
                .contains("Markdown generation failed"));
    }
}
