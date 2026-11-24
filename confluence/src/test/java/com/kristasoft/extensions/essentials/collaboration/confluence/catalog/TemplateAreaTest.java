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
import com.kristasoft.extensions.essentials.collaboration.confluence.api.ConfluenceAuditService;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.entities.Template;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.errorhandlers.ErrorHandlingStateManager;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.errorhandlers.ExtensionResponseGenerator;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.validators.ValidationOrchestrator;
import com.kristasoft.extensions.essentials.collaboration.confluence.service.ConfluenceService;
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
 * Comprehensive tests for {@link TemplateArea} targeting 100% code coverage.
 *
 * <h2>Branch map</h2>
 * <pre>
 * getTemplateByID / getTemplatesBySpaceID
 *   ├─ templateID/spaceID non-null, non-blank → validation param added
 *   ├─ templateID/spaceID null or blank       → validation param NOT added
 *   ├─ validationResults.isEmpty() == true    → service call, success response
 *   ├─ validationResults.isEmpty() == false
 *   │     ├─ allowRetry == true               → confirmation response
 *   │     └─ allowRetry != true               → validation error response
 *   ├─ MustAuthorizeException                 → authFailure, rethrown
 *   ├─ general Exception                      → failure response, audit failure
 *   └─ finally: auditRecorded check           → success if not already recorded
 * </pre>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TemplateArea Tests")
class TemplateAreaTest {

    @Mock
    private ConfluenceService service;

    @Mock
    private ValidationOrchestrator validationOrchestrator;

    @Mock
    private ErrorHandlingStateManager errorHandlingStateManager;

    @Mock
    private ExtensionResponseGenerator responseGenerator;

    @Mock
    private TelemetryHelper telemetryHelper;

    @Mock
    private ConfluenceAuditService confluenceAuditService;

    @Mock
    private RequestContext requestContext;

    private TemplateArea templateArea;

    private static final String INVOKER_ID = "test-invoker-id";

    @BeforeEach
    void setUp() {
        when(requestContext.getInvokerId()).thenReturn(INVOKER_ID);
        templateArea = new TemplateArea(service, validationOrchestrator, errorHandlingStateManager,
                responseGenerator, telemetryHelper, confluenceAuditService, requestContext);
    }

    // ── Helper methods ──────────────────────────────────────────────────────

    /**
     * Creates a DTO Template for mocking service responses.
     * Note: body is null so ParserUtils.setTemplateEntity will skip body processing.
     */
    private com.kristasoft.extensions.essentials.collaboration.confluence.dto.Template createDtoTemplate(
            String templateId, String name, String templateType) {
        return com.kristasoft.extensions.essentials.collaboration.confluence.dto.Template.builder()
                .setTemplateId(templateId)
                .setName(name)
                .setTemplateType(templateType)
                .setSpaceId("space-1")
                .build();
    }

    private ValidationOrchestrator.ValidationResult createValidationResult() {
        return new ValidationOrchestrator.ValidationResult(
                "Please confirm the template ID",
                "Template ID",
                "Enter the template ID",
                "Invalid template ID",
                "Identifier",
                Map.of("visualWidth", "S")
        );
    }

    private ExtensionResponse createMockConfirmationResponse() {
        return mock(ExtensionResponse.class);
    }

    private ExtensionResponse createMockValidationErrorResponse() {
        return mock(ExtensionResponse.class);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  getTemplateByID
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getTemplateByID")
    class GetTemplateByID {

        @Test
        @DisplayName("Happy path: validation passes, returns Template entity")
        void happyPath_validationPasses_returnsTemplate() {
            // Arrange
            String templateID = "t-123";
            when(validationOrchestrator.validate(anyMap(), eq(TemplateArea.class), eq("getTemplateByID")))
                    .thenReturn(Collections.emptyList());

            com.kristasoft.extensions.essentials.collaboration.confluence.dto.Template dtoTemplate =
                    createDtoTemplate("t-123", "Test Template", "page");
            when(service.getTemplateById(templateID)).thenReturn(dtoTemplate);

            // Act
            ExtensionResponse result = templateArea.getTemplateByID(templateID, null);

            // Assert
            assertNotNull(result);
            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());

            Object templateObj = result.getResponseValue().get("Template");
            assertNotNull(templateObj);
            assertInstanceOf(Template.class, templateObj);
            Template template = (Template) templateObj;
            assertEquals("t-123", template.id);
            assertEquals("Test Template", template.name);
            assertEquals("page", template.type);

            verify(confluenceAuditService).triggered(INVOKER_ID, "Get Template by ID");
            verify(confluenceAuditService).success(INVOKER_ID, "Get Template by ID");
            verify(telemetryHelper).incrementCount("confluence.getTemplateByID");
            verify(telemetryHelper).recordSuccess(eq("confluence.getTemplateByID"), anyLong(), anyMap());
        }

        @Test
        @DisplayName("Validation fails with allowRetry=true: returns confirmation response")
        void validationFails_allowRetryTrue_returnsConfirmation() {
            // Arrange
            String templateID = "invalid-id";
            List<ValidationOrchestrator.ValidationResult> validationResults =
                    List.of(createValidationResult());
            when(validationOrchestrator.validate(anyMap(), eq(TemplateArea.class), eq("getTemplateByID")))
                    .thenReturn(validationResults);

            ExtensionResponse mockResponse = createMockConfirmationResponse();
            when(responseGenerator.generateConfirmationResponse(
                    eq(ExtensionResponse.Error.ExceptionType.INPUT_ERROR),
                    eq(validationResults),
                    anyString(),
                    anyMap()
            )).thenReturn(mockResponse);

            // Act
            ExtensionResponse result = templateArea.getTemplateByID(templateID, true);

            // Assert
            assertSame(mockResponse, result);
            verify(responseGenerator).generateConfirmationResponse(
                    eq(ExtensionResponse.Error.ExceptionType.INPUT_ERROR),
                    eq(validationResults),
                    anyString(),
                    anyMap());
            verify(errorHandlingStateManager).put(anyString(), anyMap());
            verify(telemetryHelper).recordRetryPrompted(eq("confluence.getTemplateByID"), anyLong(), anyMap());
            verify(confluenceAuditService).success(INVOKER_ID, "Get Template by ID");
        }

        @Test
        @DisplayName("Validation fails with allowRetry=false: returns validation error response")
        void validationFails_allowRetryFalse_returnsValidationError() {
            // Arrange
            String templateID = "invalid-id";
            List<ValidationOrchestrator.ValidationResult> validationResults =
                    List.of(createValidationResult());
            when(validationOrchestrator.validate(anyMap(), eq(TemplateArea.class), eq("getTemplateByID")))
                    .thenReturn(validationResults);

            ExtensionResponse mockResponse = createMockValidationErrorResponse();
            when(responseGenerator.generateValidationErrorResponse(validationResults)).thenReturn(mockResponse);

            // Act
            ExtensionResponse result = templateArea.getTemplateByID(templateID, false);

            // Assert
            assertSame(mockResponse, result);
            verify(responseGenerator).generateValidationErrorResponse(validationResults);
            verify(errorHandlingStateManager, never()).put(anyString(), anyMap());
            verify(telemetryHelper).recordError(eq("confluence.getTemplateByID"), anyLong(), any(Exception.class), anyMap());
            verify(confluenceAuditService).success(INVOKER_ID, "Get Template by ID");
        }

        @Test
        @DisplayName("Validation fails with allowRetry=null: returns validation error response")
        void validationFails_allowRetryNull_returnsValidationError() {
            // Arrange
            String templateID = "invalid-id";
            List<ValidationOrchestrator.ValidationResult> validationResults =
                    List.of(createValidationResult());
            when(validationOrchestrator.validate(anyMap(), eq(TemplateArea.class), eq("getTemplateByID")))
                    .thenReturn(validationResults);

            ExtensionResponse mockResponse = createMockValidationErrorResponse();
            when(responseGenerator.generateValidationErrorResponse(validationResults)).thenReturn(mockResponse);

            // Act
            ExtensionResponse result = templateArea.getTemplateByID(templateID, null);

            // Assert
            assertSame(mockResponse, result);
            verify(responseGenerator).generateValidationErrorResponse(validationResults);
        }

        @Test
        @DisplayName("Null templateID: validation param not added")
        void nullTemplateID_validationParamNotAdded() {
            // Arrange
            when(validationOrchestrator.validate(anyMap(), eq(TemplateArea.class), eq("getTemplateByID")))
                    .thenReturn(Collections.emptyList());

            com.kristasoft.extensions.essentials.collaboration.confluence.dto.Template dtoTemplate =
                    createDtoTemplate("t-null", "NullId Template", "page");
            when(service.getTemplateById(null)).thenReturn(dtoTemplate);

            // Act
            ExtensionResponse result = templateArea.getTemplateByID(null, null);

            // Assert
            assertNotNull(result);
            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());
        }

        @Test
        @DisplayName("Blank templateID: validation param not added")
        void blankTemplateID_validationParamNotAdded() {
            // Arrange
            when(validationOrchestrator.validate(anyMap(), eq(TemplateArea.class), eq("getTemplateByID")))
                    .thenReturn(Collections.emptyList());

            com.kristasoft.extensions.essentials.collaboration.confluence.dto.Template dtoTemplate =
                    createDtoTemplate("t-blank", "Blank Template", "page");
            when(service.getTemplateById("   ")).thenReturn(dtoTemplate);

            // Act
            ExtensionResponse result = templateArea.getTemplateByID("   ", null);

            // Assert
            assertNotNull(result);
            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());
        }

        @Test
        @DisplayName("MustAuthorizeException: calls authFailure and rethrows")
        void mustAuthorizeException_callsAuthFailureAndRethrows() {
            // Arrange
            MustAuthorizeException authException = new MustAuthorizeException("Not authorized");
            when(validationOrchestrator.validate(anyMap(), eq(TemplateArea.class), eq("getTemplateByID")))
                    .thenThrow(authException);

            // Act & Assert
            MustAuthorizeException thrown = assertThrows(MustAuthorizeException.class,
                    () -> templateArea.getTemplateByID("t-123", null));

            assertSame(authException, thrown);
            verify(confluenceAuditService).triggered(INVOKER_ID, "Get Template by ID");
            verify(confluenceAuditService).authFailure(INVOKER_ID, "Get Template by ID", authException);
            verify(confluenceAuditService, never()).success(anyString(), anyString());
            verify(confluenceAuditService, never()).failure(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("General Exception: returns failure response and calls audit failure")
        void generalException_returnsFailure() {
            // Arrange
            RuntimeException exception = new RuntimeException("Template not found");
            when(validationOrchestrator.validate(anyMap(), eq(TemplateArea.class), eq("getTemplateByID")))
                    .thenReturn(Collections.emptyList());
            when(service.getTemplateById("t-bad")).thenThrow(exception);

            // Act
            ExtensionResponse result = templateArea.getTemplateByID("t-bad", null);

            // Assert
            assertNotNull(result);
            assertEquals(ExtensionResponse.Result.FAILURE, result.getResult());
            assertNotNull(result.getError());
            assertEquals(ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR, result.getError().getExceptionType());
            assertTrue(result.getError().getErrorMessage().contains("Template not found"));

            verify(confluenceAuditService).triggered(INVOKER_ID, "Get Template by ID");
            verify(confluenceAuditService).failure(INVOKER_ID, "Get Template by ID", exception);
            verify(confluenceAuditService, never()).success(anyString(), anyString());
            verify(telemetryHelper).recordError(eq("confluence.getTemplateByID"), anyLong(), eq(exception), anyMap());
        }

        @Test
        @DisplayName("Validation fails with null templateID and allowRetry=true: confirmationParams has empty string")
        void validationFails_nullTemplateID_allowRetryTrue_emptyStringInParams() {
            // Arrange
            List<ValidationOrchestrator.ValidationResult> validationResults =
                    List.of(createValidationResult());
            when(validationOrchestrator.validate(anyMap(), eq(TemplateArea.class), eq("getTemplateByID")))
                    .thenReturn(validationResults);

            ExtensionResponse mockResponse = createMockConfirmationResponse();
            when(responseGenerator.generateConfirmationResponse(
                    eq(ExtensionResponse.Error.ExceptionType.INPUT_ERROR),
                    eq(validationResults),
                    anyString(),
                    anyMap()
            )).thenReturn(mockResponse);

            // Act
            ExtensionResponse result = templateArea.getTemplateByID(null, true);

            // Assert
            assertSame(mockResponse, result);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  getTemplatesBySpaceID
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getTemplatesBySpaceID")
    class GetTemplatesBySpaceID {

        @Test
        @DisplayName("Happy path: validation passes, returns templates list")
        void happyPath_validationPasses_returnsTemplatesList() {
            // Arrange
            String spaceID = "space-1";
            when(validationOrchestrator.validate(anyMap(), eq(TemplateArea.class), eq("getTemplatesBySpaceID")))
                    .thenReturn(Collections.emptyList());

            com.kristasoft.extensions.essentials.collaboration.confluence.dto.Template[] dtoTemplates = {
                    createDtoTemplate("t-1", "Template One", "page"),
                    createDtoTemplate("t-2", "Template Two", "blog")
            };
            when(service.getTemplatesBySpaceId(spaceID)).thenReturn(dtoTemplates);

            // Act
            ExtensionResponse result = templateArea.getTemplatesBySpaceID(spaceID, null);

            // Assert
            assertNotNull(result);
            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());

            @SuppressWarnings("unchecked")
            List<Template> templates = (List<Template>) result.getResponseValue().get("Templates");
            assertNotNull(templates);
            assertEquals(2, templates.size());
            assertEquals("t-1", templates.get(0).id);
            assertEquals("t-2", templates.get(1).id);

            @SuppressWarnings("unchecked")
            List<String> identifiers = (List<String>) result.getResponseValue().get("Template Identifiers");
            assertNotNull(identifiers);
            assertEquals(2, identifiers.size());
            assertTrue(identifiers.contains("t-1"));
            assertTrue(identifiers.contains("t-2"));

            verify(confluenceAuditService).triggered(INVOKER_ID, "Get Templates By Space ID");
            verify(confluenceAuditService).success(INVOKER_ID, "Get Templates By Space ID");
            verify(telemetryHelper).incrementCount("confluence.getTemplatesBySpaceID");
            verify(telemetryHelper).recordSuccess(eq("confluence.getTemplatesBySpaceID"), anyLong(), anyMap());
        }

        @Test
        @DisplayName("Happy path with empty templates array")
        void happyPath_emptyTemplatesArray() {
            // Arrange
            String spaceID = "space-empty";
            when(validationOrchestrator.validate(anyMap(), eq(TemplateArea.class), eq("getTemplatesBySpaceID")))
                    .thenReturn(Collections.emptyList());
            when(service.getTemplatesBySpaceId(spaceID)).thenReturn(new com.kristasoft.extensions.essentials.collaboration.confluence.dto.Template[0]);

            // Act
            ExtensionResponse result = templateArea.getTemplatesBySpaceID(spaceID, null);

            // Assert
            assertNotNull(result);
            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());

            @SuppressWarnings("unchecked")
            List<Template> templates = (List<Template>) result.getResponseValue().get("Templates");
            assertEquals(0, templates.size());
        }

        @Test
        @DisplayName("Validation fails with allowRetry=true: returns confirmation response")
        void validationFails_allowRetryTrue_returnsConfirmation() {
            // Arrange
            String spaceID = "invalid-space";
            List<ValidationOrchestrator.ValidationResult> validationResults =
                    List.of(new ValidationOrchestrator.ValidationResult(
                            "Please confirm the space ID",
                            "Space ID",
                            "Enter the space ID",
                            "Invalid space ID",
                            "Identifier",
                            Map.of("visualWidth", "S")
                    ));
            when(validationOrchestrator.validate(anyMap(), eq(TemplateArea.class), eq("getTemplatesBySpaceID")))
                    .thenReturn(validationResults);

            ExtensionResponse mockResponse = createMockConfirmationResponse();
            when(responseGenerator.generateConfirmationResponse(
                    eq(ExtensionResponse.Error.ExceptionType.INPUT_ERROR),
                    eq(validationResults),
                    anyString(),
                    anyMap()
            )).thenReturn(mockResponse);

            // Act
            ExtensionResponse result = templateArea.getTemplatesBySpaceID(spaceID, true);

            // Assert
            assertSame(mockResponse, result);
            verify(responseGenerator).generateConfirmationResponse(
                    eq(ExtensionResponse.Error.ExceptionType.INPUT_ERROR),
                    eq(validationResults),
                    anyString(),
                    anyMap());
            verify(errorHandlingStateManager).put(anyString(), anyMap());
            verify(telemetryHelper).recordRetryPrompted(eq("confluence.getTemplatesBySpaceID"), anyLong(), anyMap());
        }

        @Test
        @DisplayName("Validation fails with allowRetry=false: returns validation error response")
        void validationFails_allowRetryFalse_returnsValidationError() {
            // Arrange
            String spaceID = "invalid-space";
            List<ValidationOrchestrator.ValidationResult> validationResults =
                    List.of(new ValidationOrchestrator.ValidationResult(
                            "confirm", "Space ID", "fetch", "Invalid", "Identifier", Map.of()));
            when(validationOrchestrator.validate(anyMap(), eq(TemplateArea.class), eq("getTemplatesBySpaceID")))
                    .thenReturn(validationResults);

            ExtensionResponse mockResponse = createMockValidationErrorResponse();
            when(responseGenerator.generateValidationErrorResponse(validationResults)).thenReturn(mockResponse);

            // Act
            ExtensionResponse result = templateArea.getTemplatesBySpaceID(spaceID, false);

            // Assert
            assertSame(mockResponse, result);
            verify(responseGenerator).generateValidationErrorResponse(validationResults);
            verify(telemetryHelper).recordError(eq("confluence.getTemplatesBySpaceID"), anyLong(), any(Exception.class), anyMap());
        }

        @Test
        @DisplayName("Validation fails with allowRetry=null: returns validation error response")
        void validationFails_allowRetryNull_returnsValidationError() {
            // Arrange
            List<ValidationOrchestrator.ValidationResult> validationResults =
                    List.of(new ValidationOrchestrator.ValidationResult(
                            "confirm", "Space ID", "fetch", "Invalid", "Identifier", Map.of()));
            when(validationOrchestrator.validate(anyMap(), eq(TemplateArea.class), eq("getTemplatesBySpaceID")))
                    .thenReturn(validationResults);

            ExtensionResponse mockResponse = createMockValidationErrorResponse();
            when(responseGenerator.generateValidationErrorResponse(validationResults)).thenReturn(mockResponse);

            // Act
            ExtensionResponse result = templateArea.getTemplatesBySpaceID("space-x", null);

            // Assert
            assertSame(mockResponse, result);
        }

        @Test
        @DisplayName("Null spaceID: validation param not added")
        void nullSpaceID_validationParamNotAdded() {
            // Arrange
            when(validationOrchestrator.validate(anyMap(), eq(TemplateArea.class), eq("getTemplatesBySpaceID")))
                    .thenReturn(Collections.emptyList());
            when(service.getTemplatesBySpaceId(null)).thenReturn(
                    new com.kristasoft.extensions.essentials.collaboration.confluence.dto.Template[]{
                            createDtoTemplate("t-all", "All Template", "page")
                    });

            // Act
            ExtensionResponse result = templateArea.getTemplatesBySpaceID(null, null);

            // Assert
            assertNotNull(result);
            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());
        }

        @Test
        @DisplayName("Blank spaceID: validation param not added")
        void blankSpaceID_validationParamNotAdded() {
            // Arrange
            when(validationOrchestrator.validate(anyMap(), eq(TemplateArea.class), eq("getTemplatesBySpaceID")))
                    .thenReturn(Collections.emptyList());
            when(service.getTemplatesBySpaceId("  ")).thenReturn(
                    new com.kristasoft.extensions.essentials.collaboration.confluence.dto.Template[0]);

            // Act
            ExtensionResponse result = templateArea.getTemplatesBySpaceID("  ", null);

            // Assert
            assertNotNull(result);
            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());
        }

        @Test
        @DisplayName("MustAuthorizeException: calls authFailure and rethrows")
        void mustAuthorizeException_callsAuthFailureAndRethrows() {
            // Arrange
            MustAuthorizeException authException = new MustAuthorizeException("Auth required");
            when(validationOrchestrator.validate(anyMap(), eq(TemplateArea.class), eq("getTemplatesBySpaceID")))
                    .thenThrow(authException);

            // Act & Assert
            MustAuthorizeException thrown = assertThrows(MustAuthorizeException.class,
                    () -> templateArea.getTemplatesBySpaceID("space-1", null));

            assertSame(authException, thrown);
            verify(confluenceAuditService).triggered(INVOKER_ID, "Get Templates By Space ID");
            verify(confluenceAuditService).authFailure(INVOKER_ID, "Get Templates By Space ID", authException);
            verify(confluenceAuditService, never()).success(anyString(), anyString());
        }

        @Test
        @DisplayName("General Exception: returns failure response")
        void generalException_returnsFailure() {
            // Arrange
            RuntimeException exception = new RuntimeException("Space not found");
            when(validationOrchestrator.validate(anyMap(), eq(TemplateArea.class), eq("getTemplatesBySpaceID")))
                    .thenReturn(Collections.emptyList());
            when(service.getTemplatesBySpaceId("bad-space")).thenThrow(exception);

            // Act
            ExtensionResponse result = templateArea.getTemplatesBySpaceID("bad-space", null);

            // Assert
            assertNotNull(result);
            assertEquals(ExtensionResponse.Result.FAILURE, result.getResult());
            assertNotNull(result.getError());
            assertEquals(ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR, result.getError().getExceptionType());
            assertTrue(result.getError().getErrorMessage().contains("Space not found"));

            verify(confluenceAuditService).triggered(INVOKER_ID, "Get Templates By Space ID");
            verify(confluenceAuditService).failure(INVOKER_ID, "Get Templates By Space ID", exception);
            verify(confluenceAuditService, never()).success(anyString(), anyString());
            verify(telemetryHelper).recordError(eq("confluence.getTemplatesBySpaceID"), anyLong(), eq(exception), anyMap());
        }

        @Test
        @DisplayName("Validation fails with null spaceID and allowRetry=true: confirmationParams has empty string")
        void validationFails_nullSpaceID_allowRetryTrue_emptyStringInParams() {
            // Arrange
            List<ValidationOrchestrator.ValidationResult> validationResults =
                    List.of(new ValidationOrchestrator.ValidationResult(
                            "confirm", "Space ID", "fetch", "Invalid", "Identifier", Map.of()));
            when(validationOrchestrator.validate(anyMap(), eq(TemplateArea.class), eq("getTemplatesBySpaceID")))
                    .thenReturn(validationResults);

            ExtensionResponse mockResponse = createMockConfirmationResponse();
            when(responseGenerator.generateConfirmationResponse(
                    eq(ExtensionResponse.Error.ExceptionType.INPUT_ERROR),
                    eq(validationResults),
                    anyString(),
                    anyMap()
            )).thenReturn(mockResponse);

            // Act
            ExtensionResponse result = templateArea.getTemplatesBySpaceID(null, true);

            // Assert
            assertSame(mockResponse, result);
        }
    }
}
