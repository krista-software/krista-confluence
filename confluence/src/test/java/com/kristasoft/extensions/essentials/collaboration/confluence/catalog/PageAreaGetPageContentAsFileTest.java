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
import app.krista.model.base.File;
import com.kristasoft.extensions.essentials.collaboration.confluence.api.ConfluenceAuditService;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.errorhandlers.ErrorHandlingStateManager;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.errorhandlers.ExtensionResponseGenerator;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.validators.ValidationOrchestrator;
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
 * 100% branch-coverage tests for {@link PageArea#getPageContentAsFile} and its private
 * helper {@code executeGetPageContentAsFile}.
 *
 * <h2>Branch map</h2>
 * <pre>
 * getPageContentAsFile
 *   ├─ if (validationResults.isEmpty())
 *   │     T → executeGetPageContentAsFile → success           (tests 1, 2)
 *   │     F → handleValidationFailure                          (tests 3, 4, 5, 6)
 *   ├─ pageID != null ? pageID : ""   (ternary in failure path)
 *   │     T (pageID != null) → tests 3, 4, 5
 *   │     F (pageID == null) → test 6
 *   └─ catch (Exception cause) → buildSystemErrorResponse      (tests 7, 8)
 *
 * handleValidationFailure
 *   ├─ Boolean.TRUE.equals(allowRetry)
 *   │     T (retry)    → test 5
 *   │     F (no retry) → tests 3 (false), 4 (null), 6 (false)
 *
 * addValidationParamIfNotEmpty (value != null && !value.trim().isEmpty())
 *   ├─ T (add)   → tests 1, 3, 4, 5, 7, 8
 *   ├─ F (null)  → test 6
 *   └─ F (blank) → test 2
 * </pre>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PageArea.getPageContentAsFile – 100% branch coverage")
class PageAreaGetPageContentAsFileTest {

    // ─── mocks ────────────────────────────────────────────────────────────────

    @Mock private ConfluenceService service;
    @Mock private ValidationOrchestrator validationOrchestrator;
    @Mock private ErrorHandlingStateManager errorHandlingStateManager;
    @Mock private ExtensionResponseGenerator responseGenerator;
    @Mock private PageDocxService pageDocxService;
    @Mock private TelemetryHelper telemetryHelper;
    @Mock private PageAreaAttachmentHelper attachmentHelper;
    @Mock private FileRepository fileRepository;
    @Mock private ConfluenceAuditService confluenceAuditService;
    @Mock private RequestContext requestContext;
    @Mock private PageMarkdownService pageMarkdownService;

    private PageArea pageArea;

    @BeforeEach
    void setUp() {
        pageArea = new PageArea(service, validationOrchestrator, errorHandlingStateManager,
                responseGenerator, pageDocxService, pageMarkdownService, telemetryHelper, attachmentHelper,
                fileRepository, confluenceAuditService, requestContext);
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    /** Creates a stub {@code ValidationResult} for validation-failure tests. */
    private static ValidationOrchestrator.ValidationResult validationResult(String msg) {
        return new ValidationOrchestrator.ValidationResult(
                "confirm step", "Page ID", "fetch step", msg, "Identifier", Map.of());
    }

    private void stubValidationPasses() {
        when(validationOrchestrator.validate(anyMap(), eq(PageArea.class), eq("getPageContentAsFile")))
                .thenReturn(Collections.emptyList());
    }

    private void stubValidationFails(String msg) {
        when(validationOrchestrator.validate(anyMap(), eq(PageArea.class), eq("getPageContentAsFile")))
                .thenReturn(List.of(validationResult(msg)));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  SUCCESS PATH  (validationResults.isEmpty() = true)
    // ══════════════════════════════════════════════════════════════════════════

    /** Test 1 – Happy path: validation passes → Markdown file generated and returned. */
    @Test
    @DisplayName("Success: validation passes → returns SUCCESS with Downloaded File")
    void validationPasses_returnsSuccessWithFile() {
        File mockFile = mock(File.class);
        stubValidationPasses();
        when(pageMarkdownService.downloadPageAsMarkdownFile("123")).thenReturn(mockFile);

        ExtensionResponse response = pageArea.getPageContentAsFile("123", null);

        assertNotNull(response);
        assertEquals(ExtensionResponse.Result.SUCCESS, response.getResult());
        assertSame(mockFile, response.getResponseValue().get("Downloaded File"));
        verify(pageMarkdownService).downloadPageAsMarkdownFile("123");
        verify(telemetryHelper).incrementCount("confluence.getPageContentAsFile");
        verify(telemetryHelper).recordSuccess(eq("confluence.getPageContentAsFile"), anyLong(), anyMap());
    }

    /**
     * Test 2 – Blank pageID: {@code addValidationParamIfNotEmpty} skips the blank value,
     * validation is empty, and the file is still generated (blank-skip branch).
     */
    @Test
    @DisplayName("Success: blank pageID → validation param skipped, still returns file")
    void blankPageId_validationParamSkipped_returnsSuccess() {
        File mockFile = mock(File.class);
        stubValidationPasses();
        when(pageMarkdownService.downloadPageAsMarkdownFile("   ")).thenReturn(mockFile);

        ExtensionResponse response = pageArea.getPageContentAsFile("   ", null);

        assertEquals(ExtensionResponse.Result.SUCCESS, response.getResult());
        assertSame(mockFile, response.getResponseValue().get("Downloaded File"));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  VALIDATION-FAILURE PATH  (validationResults.isEmpty() = false)
    // ══════════════════════════════════════════════════════════════════════════

    /** Test 3 – Validation fails, allowRetry=false → direct validation error response. */
    @Test
    @DisplayName("Validation failure: allowRetry=false → direct validation error response")
    void validationFailsAllowRetryFalse_returnsDirectError() {
        stubValidationFails("Page ID is not valid");
        ExtensionResponse errorResp = mock(ExtensionResponse.class);
        when(responseGenerator.generateValidationErrorResponse(anyList())).thenReturn(errorResp);

        ExtensionResponse response = pageArea.getPageContentAsFile("bad-id", false);

        assertSame(errorResp, response);
        verify(responseGenerator).generateValidationErrorResponse(anyList());
        verify(responseGenerator, never()).generateConfirmationResponse(any(), any(), any(), any());
        verify(pageMarkdownService, never()).downloadPageAsMarkdownFile(anyString());
        verify(telemetryHelper).recordError(eq("confluence.getPageContentAsFile"), anyLong(), any(), anyMap());
    }

    /** Test 4 – Validation fails, allowRetry=null → treated as false → direct error. */
    @Test
    @DisplayName("Validation failure: allowRetry=null treated as false → direct error")
    void validationFailsAllowRetryNull_treatedAsFalse() {
        stubValidationFails("Page ID required");
        ExtensionResponse errorResp = mock(ExtensionResponse.class);
        when(responseGenerator.generateValidationErrorResponse(anyList())).thenReturn(errorResp);

        ExtensionResponse response = pageArea.getPageContentAsFile("42", null);

        assertSame(errorResp, response);
        verify(responseGenerator).generateValidationErrorResponse(anyList());
        verify(responseGenerator, never()).generateConfirmationResponse(any(), any(), any(), any());
    }

    /** Test 5 – Validation fails, allowRetry=true → confirmation/retry response. */
    @Test
    @DisplayName("Validation failure: allowRetry=true → confirmation/retry response")
    void validationFailsAllowRetryTrue_returnsConfirmationResponse() {
        stubValidationFails("Page ID is not valid");
        ExtensionResponse confirmResp = mock(ExtensionResponse.class);
        when(responseGenerator.generateConfirmationResponse(any(), anyList(), anyString(), anyMap()))
                .thenReturn(confirmResp);

        ExtensionResponse response = pageArea.getPageContentAsFile("bad-id", true);

        assertSame(confirmResp, response);
        verify(responseGenerator).generateConfirmationResponse(
                eq(ExtensionResponse.Error.ExceptionType.INPUT_ERROR),
                anyList(),
                eq(Constants.CONFIRM_REENTER_GET_PAGE_CONTENT_AS_FILE),
                anyMap());
        verify(errorHandlingStateManager).put(anyString(), anyMap());
        verify(telemetryHelper).recordRetryPrompted(eq("confluence.getPageContentAsFile"), anyLong(), anyMap());
        verify(pageMarkdownService, never()).downloadPageAsMarkdownFile(anyString());
    }

    /**
     * Test 6 – Validation fails with pageID=null → the {@code pageID != null ? pageID : ""}
     * ternary falls back to the empty string (ternary-false + null-skip branches).
     */
    @Test
    @DisplayName("Validation failure: pageID=null → ternary falls back to empty string")
    void validationFailsPageIdNull_ternaryFallsToEmptyString() {
        stubValidationFails("Page ID required");
        ExtensionResponse errorResp = mock(ExtensionResponse.class);
        when(responseGenerator.generateValidationErrorResponse(anyList())).thenReturn(errorResp);

        ExtensionResponse response = pageArea.getPageContentAsFile(null, false);

        assertSame(errorResp, response);
        verify(responseGenerator).generateValidationErrorResponse(anyList());
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  CATCH BLOCK  (catch (Exception cause) → buildSystemErrorResponse)
    // ══════════════════════════════════════════════════════════════════════════

    /** Test 7 – Markdown service throws → catch block → SYSTEM_ERROR failure response. */
    @Test
    @DisplayName("Exception: markdown service throws → SYSTEM_ERROR failure response")
    void markdownServiceThrows_returnsSystemErrorFailure() {
        stubValidationPasses();
        when(pageMarkdownService.downloadPageAsMarkdownFile("123"))
                .thenThrow(new RuntimeException("page not found"));

        ExtensionResponse response = pageArea.getPageContentAsFile("123", null);

        assertNotNull(response);
        assertEquals(ExtensionResponse.Result.FAILURE, response.getResult());
        assertNotNull(response.getError());
        assertEquals(ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR,
                response.getError().getExceptionType());
        assertTrue(response.getError().getErrorMessage().contains("123"));
        assertTrue(response.getError().getErrorMessage().contains("page not found"));
        verify(telemetryHelper).recordError(eq("confluence.getPageContentAsFile"), anyLong(), any(), anyMap());
    }

    /** Test 8 – Validation orchestrator itself throws → catch block still handles it. */
    @Test
    @DisplayName("Exception: validation orchestrator throws → SYSTEM_ERROR failure response")
    void validationOrchestratorThrows_returnsSystemErrorFailure() {
        when(validationOrchestrator.validate(anyMap(), eq(PageArea.class), eq("getPageContentAsFile")))
                .thenThrow(new RuntimeException("validator exploded"));

        ExtensionResponse response = pageArea.getPageContentAsFile("123", null);

        assertEquals(ExtensionResponse.Result.FAILURE, response.getResult());
        assertEquals(ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR,
                response.getError().getExceptionType());
        verify(pageMarkdownService, never()).downloadPageAsMarkdownFile(anyString());
    }
}
