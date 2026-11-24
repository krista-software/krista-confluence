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
import app.krista.ksdk.context.RequestContext;
import app.krista.ksdk.files.FileRepository;
import com.kristasoft.extensions.essentials.collaboration.confluence.api.ConfluenceAuditService;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.errorhandlers.ErrorHandlingStateManager;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.errorhandlers.ExtensionResponseGenerator;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.validators.ValidationOrchestrator;
import com.kristasoft.extensions.essentials.collaboration.confluence.dto.Page;
import com.kristasoft.extensions.essentials.collaboration.confluence.exception.ConfluenceRESTException;
import com.kristasoft.extensions.essentials.collaboration.confluence.service.ConfluenceService;
import com.kristasoft.extensions.essentials.collaboration.confluence.service.PageDocxService;
import com.kristasoft.extensions.essentials.collaboration.confluence.service.PageMarkdownService;
import com.kristasoft.extensions.essentials.collaboration.confluence.utils.Constants;
import com.kristasoft.extensions.essentials.collaboration.confluence.utils.TelemetryHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
 * 100% branch-coverage tests for {@link PageArea#getPagesInSpaceV2}.
 *
 * <h2>Branch map</h2>
 * <pre>
 * getPagesInSpaceV2
 *   ├─ excludeRestrictedFlag = Boolean.TRUE.equals(excludeRestricted)
 *   │     T → test 2          F (null/false) → tests 1, 3
 *   ├─ excludeContentFlag = !Boolean.TRUE.equals(includeContent)
 *   │     includeContent T → excludeContent F → test 2
 *   │     includeContent null/F → excludeContent T → tests 1, 3
 *   ├─ if (validationResults.isEmpty())
 *   │     T → success path (tests 1, 2, 3)
 *   │     F → handleValidationFailure (tests 4, 5, 6)
 *   ├─ pageStatus != null ? pageStatus : "any"   (ternary, success + failure)
 *   │     T → tests 2, 4          F → tests 1, 3, 6
 *   ├─ spaceID != null ? spaceID : ""            (ternary, failure path)
 *   │     T → tests 4, 5          F → test 6
 *   ├─ catch (OutOfMemoryError)  → test 7
 *   └─ catch (Exception)         → tests 8, 9
 *
 * handleValidationFailure
 *   ├─ Boolean.TRUE.equals(allowRetry)
 *   │     T (retry)    → test 5
 *   │     F (no retry) → tests 4, 6
 *
 * addValidationParamIfNotEmpty (value != null && !value.trim().isEmpty())
 *   ├─ T (add)   → tests 1, 2, 4, 5, 7, 8, 9
 *   ├─ F (null)  → test 6
 *   └─ F (blank) → test 3
 * </pre>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PageArea.getPagesInSpaceV2 – 100% branch coverage")
class PageAreaGetPagesInSpaceV2Test {

    // ─── mocks ────────────────────────────────────────────────────────────────

    @Mock private ConfluenceService service;
    @Mock private ValidationOrchestrator validationOrchestrator;
    @Mock private ErrorHandlingStateManager errorHandlingStateManager;
    @Mock private ExtensionResponseGenerator responseGenerator;
    @Mock private PageDocxService pageDocxService;
    @Mock private PageMarkdownService pageMarkdownService;
    @Mock private TelemetryHelper telemetryHelper;
    @Mock private PageAreaAttachmentHelper attachmentHelper;
    @Mock private FileRepository fileRepository;
    @Mock private ConfluenceAuditService confluenceAuditService;
    @Mock private RequestContext requestContext;

    private PageArea pageArea;

    @BeforeEach
    void setUp() {
        pageArea = new PageArea(service, validationOrchestrator, errorHandlingStateManager,
                responseGenerator, pageDocxService, pageMarkdownService, telemetryHelper, attachmentHelper,
                fileRepository, confluenceAuditService, requestContext);
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    /** Creates a minimal {@code dto.Page} that {@code ParserUtils.setPageEntity} can process. */
    private static Page dtoPage(String id, String title) {
        return new Page.PageBuilder(id, title)
                .status("current")
                .spaceId("42")
                .domainUrl("https://test.atlassian.net")
                .build();
    }

    /** Creates a stub {@code ValidationResult} for validation-failure tests. */
    private static ValidationOrchestrator.ValidationResult validationResult(String msg) {
        return new ValidationOrchestrator.ValidationResult(
                "confirm step", "Space ID", "fetch step", msg, "Identifier", Map.of());
    }

    private void stubValidationPasses() {
        when(validationOrchestrator.validate(anyMap(), eq(PageArea.class), eq("getPagesInSpaceV2")))
                .thenReturn(Collections.emptyList());
    }

    private void stubValidationFails(String msg) {
        when(validationOrchestrator.validate(anyMap(), eq(PageArea.class), eq("getPagesInSpaceV2")))
                .thenReturn(List.of(validationResult(msg)));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  SUCCESS PATH  (validationResults.isEmpty() = true)
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Test 1 – Happy path with all optional flags null:
     * excludeRestricted=false, excludeContent=true (default), pageStatus=null → "any".
     */
    @Test
    @DisplayName("Success: all flags null → excludeRestricted=false, excludeContent=true, status=any")
    void validationPasses_defaults_returnsPages() {
        Page[] dtoPages = {dtoPage("100", "Page A"), dtoPage("101", "Page B")};
        stubValidationPasses();
        when(service.getPagesInSpaceV2("42", false, true, null)).thenReturn(dtoPages);

        ExtensionResponse response = pageArea.getPagesInSpaceV2("42", null, null, null, null);

        assertNotNull(response);
        assertEquals(ExtensionResponse.Result.SUCCESS, response.getResult());
        List<?> pages = (List<?>) response.getResponseValue().get("Pages");
        List<?> ids   = (List<?>) response.getResponseValue().get("Page Identifiers");
        assertEquals(2, pages.size());
        assertTrue(ids.contains("100"));
        assertTrue(ids.contains("101"));
        verify(service).getPagesInSpaceV2("42", false, true, null);
        verify(telemetryHelper).incrementCount("confluence.getPagesInSpaceV2");
        verify(telemetryHelper).recordSuccess(eq("confluence.getPagesInSpaceV2"), anyLong(), anyMap());
    }

    /**
     * Test 2 – All flags flipped: excludeRestricted=true, includeContent=true (excludeContent=false),
     * pageStatus="current" → ternary true. Verifies flags forwarded correctly to the service.
     */
    @Test
    @DisplayName("Success: excludeRestricted=true, includeContent=true, status=current → flags forwarded")
    void validationPasses_flagsFlipped_forwardedToService() {
        Page[] dtoPages = {dtoPage("200", "Open Page")};
        stubValidationPasses();
        when(service.getPagesInSpaceV2("42", true, false, "current")).thenReturn(dtoPages);

        ExtensionResponse response = pageArea.getPagesInSpaceV2("42", true, "current", true, null);

        assertEquals(ExtensionResponse.Result.SUCCESS, response.getResult());
        assertEquals(1, ((List<?>) response.getResponseValue().get("Pages")).size());
        verify(service).getPagesInSpaceV2("42", true, false, "current");
    }

    /**
     * Test 3 – Blank spaceID: {@code addValidationParamIfNotEmpty} blank-skip branch; 0 pages
     * returned → empty lists (covers the stream over an empty array).
     */
    @Test
    @DisplayName("Success: blank spaceID → param skipped; 0 pages → empty lists")
    void validationPasses_blankSpaceId_returnsEmptyLists() {
        stubValidationPasses();
        when(service.getPagesInSpaceV2("", false, true, null)).thenReturn(new Page[0]);

        ExtensionResponse response = pageArea.getPagesInSpaceV2("   ", false, null, false, null);

        assertEquals(ExtensionResponse.Result.SUCCESS, response.getResult());
        assertEquals(0, ((List<?>) response.getResponseValue().get("Pages")).size());
        assertEquals(0, ((List<?>) response.getResponseValue().get("Page Identifiers")).size());
        verify(service).getPagesInSpaceV2("", false, true, null);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  VALIDATION-FAILURE PATH  (validationResults.isEmpty() = false)
    // ══════════════════════════════════════════════════════════════════════════

    /** Test 4 – Validation fails, allowRetry=false → direct validation error response. */
    @Test
    @DisplayName("Validation failure: allowRetry=false → direct validation error response")
    void validationFailsAllowRetryFalse_returnsDirectError() {
        stubValidationFails("Space ID is not valid");
        ExtensionResponse errorResp = mock(ExtensionResponse.class);
        when(responseGenerator.generateValidationErrorResponse(anyList())).thenReturn(errorResp);

        ExtensionResponse response = pageArea.getPagesInSpaceV2("bad-id", true, "archived", true, false);

        assertSame(errorResp, response);
        verify(responseGenerator).generateValidationErrorResponse(anyList());
        verify(responseGenerator, never()).generateConfirmationResponse(any(), any(), any(), any());
        verify(service, never()).getPagesInSpaceV2(anyString(), anyBoolean(), anyBoolean(), any());
        verify(telemetryHelper).recordError(eq("confluence.getPagesInSpaceV2"), anyLong(), any(), anyMap());
    }

    /** Test 5 – Validation fails, allowRetry=true → confirmation/retry response. */
    @Test
    @DisplayName("Validation failure: allowRetry=true → confirmation/retry response")
    void validationFailsAllowRetryTrue_returnsConfirmationResponse() {
        stubValidationFails("Space ID is not valid");
        ExtensionResponse confirmResp = mock(ExtensionResponse.class);
        when(responseGenerator.generateConfirmationResponse(any(), anyList(), anyString(), anyMap()))
                .thenReturn(confirmResp);

        ExtensionResponse response = pageArea.getPagesInSpaceV2("42", null, "current", null, true);

        assertSame(confirmResp, response);
        verify(responseGenerator).generateConfirmationResponse(
                eq(ExtensionResponse.Error.ExceptionType.INPUT_ERROR),
                anyList(),
                eq(Constants.CONFIRM_REENTER_GET_PAGES_IN_SPACE_V2),
                anyMap());
        verify(errorHandlingStateManager).put(anyString(), anyMap());
        verify(telemetryHelper).recordRetryPrompted(eq("confluence.getPagesInSpaceV2"), anyLong(), anyMap());
        verify(service, never()).getPagesInSpaceV2(anyString(), anyBoolean(), anyBoolean(), any());
    }

    /**
     * Test 6 – Validation fails with spaceID=null and pageStatus=null → both ternaries take the
     * fallback branch ("" for spaceID, "any" for status). allowRetry=null treated as false.
     */
    @Test
    @DisplayName("Validation failure: spaceID=null & status=null → ternary fallbacks, allowRetry=null")
    void validationFailsNullSpaceIdAndStatus_ternaryFallbacks() {
        stubValidationFails("Space ID required");
        ExtensionResponse errorResp = mock(ExtensionResponse.class);
        when(responseGenerator.generateValidationErrorResponse(anyList())).thenReturn(errorResp);

        ExtensionResponse response = pageArea.getPagesInSpaceV2(null, null, null, null, null);

        assertSame(errorResp, response);
        verify(responseGenerator).generateValidationErrorResponse(anyList());
        verify(responseGenerator, never()).generateConfirmationResponse(any(), any(), any(), any());
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  ERROR / EXCEPTION PATHS
    // ══════════════════════════════════════════════════════════════════════════

    /** Test 7 – Service throws {@code OutOfMemoryError} → dedicated OOM catch → OOM message. */
    @Test
    @DisplayName("OutOfMemoryError: service OOMs → SYSTEM_ERROR with OOM guidance message")
    void serviceThrowsOom_returnsOomFailure() {
        stubValidationPasses();
        when(service.getPagesInSpaceV2("42", false, false, null))
                .thenThrow(new OutOfMemoryError("Java heap space"));

        ExtensionResponse response = pageArea.getPagesInSpaceV2("42", null, null, true, null);

        assertNotNull(response);
        assertEquals(ExtensionResponse.Result.FAILURE, response.getResult());
        assertEquals(ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR,
                response.getError().getExceptionType());
        assertTrue(response.getError().getErrorMessage().contains("space '42'"));
        assertTrue(response.getError().getErrorMessage().contains("too large to fit in memory"));
        verify(telemetryHelper).recordError(eq("confluence.getPagesInSpaceV2"), anyLong(), any(), anyMap());
    }

    /**
     * Test 7b – OOM with the opposite flag values (excludeRestricted=true, includeContent=null →
     * excludeContent=true). Exercises the OOM catch's log statement with the inverse boxed
     * booleans, completing branch coverage of that line.
     */
    @Test
    @DisplayName("OutOfMemoryError: inverse flags → OOM failure (covers log autoboxing branch)")
    void serviceThrowsOom_inverseFlags_returnsOomFailure() {
        stubValidationPasses();
        when(service.getPagesInSpaceV2("42", true, true, null))
                .thenThrow(new OutOfMemoryError("Java heap space"));

        ExtensionResponse response = pageArea.getPagesInSpaceV2("42", true, null, null, null);

        assertEquals(ExtensionResponse.Result.FAILURE, response.getResult());
        assertEquals(ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR,
                response.getError().getExceptionType());
        assertTrue(response.getError().getErrorMessage().contains("space '42'"));
    }

    /** Test 8 – Service throws {@code RuntimeException} → generic catch → cause message surfaced. */
    @Test
    @DisplayName("Exception: service throws RuntimeException → SYSTEM_ERROR with cause message")
    void serviceThrowsRuntimeException_returnsSystemErrorFailure() {
        stubValidationPasses();
        when(service.getPagesInSpaceV2("42", false, true, null))
                .thenThrow(new RuntimeException("Confluence unreachable"));

        ExtensionResponse response = pageArea.getPagesInSpaceV2("42", null, null, null, null);

        assertEquals(ExtensionResponse.Result.FAILURE, response.getResult());
        assertEquals(ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR,
                response.getError().getExceptionType());
        assertEquals("Confluence unreachable", response.getError().getErrorMessage());
        verify(telemetryHelper).recordError(eq("confluence.getPagesInSpaceV2"), anyLong(), any(), anyMap());
    }

    /** Test 9 – Service throws {@code ConfluenceRESTException} (checked subtype) → generic catch. */
    @Test
    @DisplayName("Exception: ConfluenceRESTException from service → SYSTEM_ERROR failure")
    void serviceThrowsConfluenceRestException_returnsSystemErrorFailure() {
        stubValidationPasses();
        when(service.getPagesInSpaceV2("42", false, true, null))
                .thenThrow(new ConfluenceRESTException("Space not found"));

        ExtensionResponse response = pageArea.getPagesInSpaceV2("42", null, null, null, null);

        assertEquals(ExtensionResponse.Result.FAILURE, response.getResult());
        assertEquals("Space not found", response.getError().getErrorMessage());
    }
}
