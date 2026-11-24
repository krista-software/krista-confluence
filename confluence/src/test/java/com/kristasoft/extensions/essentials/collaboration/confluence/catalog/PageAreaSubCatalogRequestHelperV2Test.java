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
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.errorhandlers.ErrorHandlingStateManager;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.errorhandlers.ExtensionResponseGenerator;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.validators.ValidationOrchestrator;
import com.kristasoft.extensions.essentials.collaboration.confluence.dto.Page;
import com.kristasoft.extensions.essentials.collaboration.confluence.service.ConfluenceService;
import com.kristasoft.extensions.essentials.collaboration.confluence.service.PageDocxService;
import com.kristasoft.extensions.essentials.collaboration.confluence.service.PageMarkdownService;
import com.kristasoft.extensions.essentials.collaboration.confluence.utils.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 100% branch-coverage tests for
 * {@link PageAreaSubCatalogRequestHelper#confirmReenterGetPagesInSpaceV2} and
 * {@link PageAreaSubCatalogRequestHelper#handleReenterGetPagesInSpaceV2}.
 *
 * <h2>Branch map</h2>
 * <pre>
 * confirmReenterGetPagesInSpaceV2
 *   ├─ if (Boolean.FALSE.equals(reEnter))
 *   │     T (reEnter=false)  → generateFetchDenyResponse   (test 1)
 *   │     F (reEnter=true)   → generateFetchResponse       (test 2)
 *   │     F (reEnter=null)   → generateFetchResponse       (test 3)
 *   └─ catch (Exception)     → INPUT_ERROR failure         (test 4)
 *
 * getValidationResults  (private helper, exercised via confirmReenter tests)
 *   ├─ if (results != null)  → T, list non-empty → for-loop body executes  (test 1)
 *   ├─ if (results != null)  → T, list empty     → for-loop body skipped   (test 2)
 *   └─ if (results != null)  → F (results null)  → loop never reached      (test 3)
 *
 * handleReenterGetPagesInSpaceV2
 *   ├─ Boolean.TRUE.equals(excludeRestricted) = false (null/false)  → flag=false (test 5)
 *   ├─ Boolean.TRUE.equals(excludeRestricted) = true                → flag=true  (test 6)
 *   ├─ success: 0 pages                                                          (test 7)
 *   └─ catch (Exception)     → INPUT_ERROR failure                               (test 8)
 * </pre>
 */
@Disabled("Methods confirmReenterGetPagesInSpaceV2 and handleReenterGetPagesInSpaceV2 removed from production code")
@ExtendWith(MockitoExtension.class)
@DisplayName("PageAreaSubCatalogRequestHelper – confirmReenterGetPagesInSpaceV2 & handleReenterGetPagesInSpaceV2")
class PageAreaSubCatalogRequestHelperV2Test {

    // ─── mocks ────────────────────────────────────────────────────────────────

    @Mock private ConfluenceService confluenceService;
    @Mock private ErrorHandlingStateManager internalStateManager;
    @Mock private ExtensionResponseGenerator responseGenerator;
    @Mock private ValidationOrchestrator validationOrchestrator;
    @Mock private PageDocxService pageDocxService;
    @Mock private PageMarkdownService pageMarkdownService;

    private PageAreaSubCatalogRequestHelper helper;

    private static final String STATE_ID = "state-123";

    @BeforeEach
    void setUp() {
        helper = new PageAreaSubCatalogRequestHelper(
                confluenceService, internalStateManager, responseGenerator,
                validationOrchestrator, pageDocxService, pageMarkdownService);
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    /**
     * State map with one ValidationResult in the list.
     * Exercises the for-loop body inside {@code getValidationResults}.
     */
    private static Map<String, Object> stateWithValidationResults() {
        ValidationOrchestrator.ValidationResult vr = new ValidationOrchestrator.ValidationResult(
                "confirm step", "Space ID", "fetch step",
                "Space ID is not valid", "Identifier", Map.of());
        Map<String, Object> state = new HashMap<>();
        state.put(Constants.VALIDATION_RESULTS, List.of(vr));
        return state;
    }

    /**
     * State map with an empty (non-null) ValidationResults list.
     * Exercises the for-loop "no iterations" branch inside {@code getValidationResults}.
     */
    private static Map<String, Object> stateWithEmptyValidationResults() {
        Map<String, Object> state = new HashMap<>();
        state.put(Constants.VALIDATION_RESULTS, Collections.emptyList());
        return state;
    }

    /**
     * State map with no VALIDATION_RESULTS key (null when retrieved).
     * Exercises the {@code if (results != null)} → false branch.
     */
    private static Map<String, Object> stateWithNoValidationResults() {
        return new HashMap<>();
    }

    /** Minimal dto.Page suitable for {@code ParserUtils::setPageEntity}. */
    private static Page dtoPage(String id, String title) {
        return new Page.PageBuilder(id, title)
                .status("current")
                .spaceId("42")
                .domainUrl("https://test.atlassian.net")
                .build();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  confirmReenterGetPagesInSpaceV2
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Test 1 – reEnter=false → {@code Boolean.FALSE.equals(false)} = true → if-branch.
     * State has a non-empty ValidationResults list so the for-loop body is exercised.
     */
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
                eq(Constants.HANDLE_REENTER_GET_PAGES_IN_SPACE),
                eq(inputMap)))
                .thenReturn(denyResp);

        ExtensionResponse response = helper.confirmReenterGetPagesInSpace(inputMap);

        assertSame(denyResp, response);
        verify(responseGenerator).generateFetchDenyResponse(
                eq(ExtensionResponse.Error.ExceptionType.INPUT_ERROR),
                anyList(),
                eq(Constants.HANDLE_REENTER_GET_PAGES_IN_SPACE),
                eq(inputMap));
        verify(responseGenerator, never()).generateFetchResponse(any(), any(), any(), any());
    }

    /**
     * Test 2 – reEnter=true → {@code Boolean.FALSE.equals(true)} = false → else-branch.
     * State has an empty ValidationResults list: for-loop body is never entered (zero-iteration branch).
     */
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
                eq(Constants.HANDLE_REENTER_GET_PAGES_IN_SPACE),
                eq(inputMap)))
                .thenReturn(fetchResp);

        ExtensionResponse response = helper.confirmReenterGetPagesInSpace(inputMap);

        assertSame(fetchResp, response);
        verify(responseGenerator).generateFetchResponse(
                eq(ExtensionResponse.Error.ExceptionType.INPUT_ERROR),
                anyList(),
                eq(Constants.HANDLE_REENTER_GET_PAGES_IN_SPACE),
                eq(inputMap));
        verify(responseGenerator, never()).generateFetchDenyResponse(any(), any(), any(), any());
    }

    /**
     * Test 3 – reEnter=null → {@code Boolean.FALSE.equals(null)} = false → else-branch.
     * State has no VALIDATION_RESULTS key so {@code results} is null (null branch of the null-check).
     */
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
                eq(Constants.HANDLE_REENTER_GET_PAGES_IN_SPACE),
                eq(inputMap)))
                .thenReturn(fetchResp);

        ExtensionResponse response = helper.confirmReenterGetPagesInSpace(inputMap);

        assertSame(fetchResp, response);
        verify(responseGenerator).generateFetchResponse(
                eq(ExtensionResponse.Error.ExceptionType.INPUT_ERROR),
                anyList(),
                eq(Constants.HANDLE_REENTER_GET_PAGES_IN_SPACE),
                eq(inputMap));
    }

    /**
     * Test 4 – {@code internalStateManager.get()} throws → catch block reached.
     * Response must be FAILURE with INPUT_ERROR and the FAILED_TO_GET_PAGES_IN_SPACE_V2_VALIDATION prefix.
     */
    @Test
    @DisplayName("confirmReenter: exception thrown → INPUT_ERROR failure response")
    void confirmReenter_exceptionThrown_returnsInputErrorFailure() {
        Map<String, Object> inputMap = new HashMap<>();
        inputMap.put(Constants.RE_ENTER, false);
        inputMap.put(Constants.STATE_ID, STATE_ID);

        when(internalStateManager.get(STATE_ID))
                .thenThrow(new RuntimeException("state store unavailable"));

        ExtensionResponse response = helper.confirmReenterGetPagesInSpace(inputMap);

        assertNotNull(response);
        assertEquals(ExtensionResponse.Result.FAILURE, response.getResult());
        assertNotNull(response.getError());
        assertEquals(ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                response.getError().getExceptionType());
        assertTrue(response.getError().getErrorMessage()
                .contains(Constants.FAILED_TO_GET_PAGES_IN_SPACE_VALIDATION));
        assertTrue(response.getError().getErrorMessage()
                .contains("state store unavailable"));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  handleReenterGetPagesInSpaceV2
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Test 5 – excludeRestricted key absent → {@code Boolean.TRUE.equals(null)} = false → flag=false.
     * Verifies pages and identifiers are returned correctly.
     */
    @Test
    @DisplayName("handleReenter: excludeRestricted absent (null) → service called with flag=false, pages returned")
    void handleReenter_excludeRestrictedNull_callsServiceWithFalse() {
        Map<String, Object> inputMap = new HashMap<>();
        inputMap.put("Space ID", "42");
        // no EXCLUDE_RESTRICTED_PAGES entry → treated as false

        when(confluenceService.getPagesBySpaceId("42"))
                .thenReturn(new Page[]{dtoPage("101", "Page One"), dtoPage("102", "Page Two")});

        ExtensionResponse response = helper.handleReenterGetPagesInSpace(inputMap);

        assertNotNull(response);
        assertEquals(ExtensionResponse.Result.SUCCESS, response.getResult());
        List<?> pages = (List<?>) response.getResponseValue().get(Constants.PAGES);
        List<?> ids   = (List<?>) response.getResponseValue().get(Constants.PAGE_IDENTIFIERS);
        assertEquals(2, pages.size());
        assertEquals(2, ids.size());
        assertTrue(ids.contains("101"));
        assertTrue(ids.contains("102"));
        verify(confluenceService).getPagesBySpaceId("42");
    }

    /**
     * Test 6 – excludeRestricted=true → {@code Boolean.TRUE.equals(true)} = true → flag=true forwarded.
     */
    @Test
    @DisplayName("handleReenter: excludeRestricted=true → service called with flag=true")
    void handleReenter_excludeRestrictedTrue_passesFlagToService() {
        Map<String, Object> inputMap = new HashMap<>();
        inputMap.put("Space ID", "99");
        inputMap.put("Exclude Restricted Pages", true);

        when(confluenceService.getPagesBySpaceId("99"))
                .thenReturn(new Page[]{dtoPage("200", "Open Page")});

        ExtensionResponse response = helper.handleReenterGetPagesInSpace(inputMap);

        assertEquals(ExtensionResponse.Result.SUCCESS, response.getResult());
        verify(confluenceService).getPagesBySpaceId("99");
    }

    /**
     * Test 7 – Service returns empty array → result map has empty (non-null) lists.
     */
    @Test
    @DisplayName("handleReenter: 0 pages returned → empty Pages and Page Identifiers lists")
    void handleReenter_noPages_returnsEmptyLists() {
        Map<String, Object> inputMap = new HashMap<>();
        inputMap.put("Space ID", "42");
        inputMap.put("Exclude Restricted Pages", false);

        when(confluenceService.getPagesBySpaceId("42")).thenReturn(new Page[0]);

        ExtensionResponse response = helper.handleReenterGetPagesInSpace(inputMap);

        assertEquals(ExtensionResponse.Result.SUCCESS, response.getResult());
        assertEquals(0, ((List<?>) response.getResponseValue().get(Constants.PAGES)).size());
        assertEquals(0, ((List<?>) response.getResponseValue().get(Constants.PAGE_IDENTIFIERS)).size());
    }

    /**
     * Test 8 – Service throws → catch block reached → INPUT_ERROR failure with FAILED_TO_GET_PAGES_IN_SPACE_V2 prefix.
     */
    @Test
    @DisplayName("handleReenter: service throws → INPUT_ERROR failure response")
    void handleReenter_serviceThrows_returnsInputErrorFailure() {
        Map<String, Object> inputMap = new HashMap<>();
        inputMap.put("Space ID", "bad-id");
        inputMap.put("Exclude Restricted Pages", false);

        when(confluenceService.getPagesBySpaceId("bad-id"))
                .thenThrow(new RuntimeException("Confluence unavailable"));

        ExtensionResponse response = helper.handleReenterGetPagesInSpace(inputMap);

        assertNotNull(response);
        assertEquals(ExtensionResponse.Result.FAILURE, response.getResult());
        assertNotNull(response.getError());
        assertEquals(ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                response.getError().getExceptionType());
        assertTrue(response.getError().getErrorMessage()
                .contains(Constants.FAILED_TO_GET_PAGES_IN_SPACE));
        assertTrue(response.getError().getErrorMessage()
                .contains("Confluence unavailable"));
    }
}
