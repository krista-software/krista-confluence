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

package com.kristasoft.extensions.essentials.collaboration.confluence.catalog.errorhandlers;

import app.krista.extension.executor.ExtensionResponse;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.validators.ValidationOrchestrator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.jvnet.hk2.annotations.Service;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExtensionResponseGenerator Tests")
class ExtensionResponseGeneratorTest {

    private ExtensionResponseGenerator generator;

    private static final String SUB_CATALOG_REQUEST = "testSubCatalog";

    @BeforeEach
    void setUp() {
        generator = new ExtensionResponseGenerator();
    }

    // ──────────────────────────────────────────────────────────────
    // Annotation verification
    // ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Class should be annotated with @Service")
    void classShouldBeAnnotatedWithService() {
        assertTrue(
                ExtensionResponseGenerator.class.isAnnotationPresent(Service.class),
                "ExtensionResponseGenerator should have @Service annotation"
        );
    }

    // ──────────────────────────────────────────────────────────────
    // Helper to create ValidationResult
    // ──────────────────────────────────────────────────────────────

    private ValidationOrchestrator.ValidationResult createValidationResult(
            String fieldName, String errMessage) {
        return new ValidationOrchestrator.ValidationResult(
                "Please confirm the " + fieldName + " value.",
                fieldName,
                "Please enter a valid " + fieldName + ".",
                errMessage,
                "Text",
                Map.of()
        );
    }

    private ValidationOrchestrator.ValidationResult createValidationResultWithAttributes(
            String fieldName, String errMessage, String fieldType, Map<String, Object> attributes) {
        return new ValidationOrchestrator.ValidationResult(
                "Please confirm the " + fieldName + " value.",
                fieldName,
                "Please enter a valid " + fieldName + ".",
                errMessage,
                fieldType,
                attributes
        );
    }

    // ──────────────────────────────────────────────────────────────
    // generateConfirmationResponse()
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("generateConfirmationResponse")
    class GenerateConfirmationResponseTests {

        @Test
        @DisplayName("Should return non-null response with FAILURE result")
        void shouldReturnNonNullResponseWithFailureResult() {
            List<ValidationOrchestrator.ValidationResult> results = List.of(
                    createValidationResult("Space ID", "Space ID not found")
            );
            Map<String, Object> state = Map.of("step", "confirm");

            ExtensionResponse response = generator.generateConfirmationResponse(
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    results,
                    SUB_CATALOG_REQUEST,
                    state
            );

            assertNotNull(response, "Response should not be null");
            assertEquals(ExtensionResponse.Result.FAILURE, response.getResult());
        }

        @Test
        @DisplayName("Should include error in response")
        void shouldIncludeErrorInResponse() {
            List<ValidationOrchestrator.ValidationResult> results = List.of(
                    createValidationResult("Page ID", "Page not found")
            );

            ExtensionResponse response = generator.generateConfirmationResponse(
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    results,
                    SUB_CATALOG_REQUEST,
                    Map.of()
            );

            assertNotNull(response.getError(), "Error should not be null");
            assertTrue(response.getError().getErrorMessage().contains("Page not found"),
                    "Error message should contain the validation error");
        }

        @Test
        @DisplayName("Should include remediation actions")
        void shouldIncludeRemediationActions() {
            List<ValidationOrchestrator.ValidationResult> results = List.of(
                    createValidationResult("Space ID", "Space not found")
            );

            ExtensionResponse response = generator.generateConfirmationResponse(
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    results,
                    SUB_CATALOG_REQUEST,
                    Map.of()
            );

            assertNotNull(response.getRemediationActions(),
                    "Remediation actions should not be null");
        }

        @Test
        @DisplayName("Should include state in response")
        void shouldIncludeStateInResponse() {
            List<ValidationOrchestrator.ValidationResult> results = List.of(
                    createValidationResult("Field", "Error")
            );
            Map<String, Object> state = new HashMap<>();
            state.put("stateKey", "stateValue");

            ExtensionResponse response = generator.generateConfirmationResponse(
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    results,
                    SUB_CATALOG_REQUEST,
                    state
            );

            assertNotNull(response.getState(), "State should not be null");
            assertEquals("stateValue", response.getState().get("stateKey"));
        }

        @Test
        @DisplayName("Should handle multiple validation results")
        void shouldHandleMultipleValidationResults() {
            List<ValidationOrchestrator.ValidationResult> results = List.of(
                    createValidationResult("Space ID", "Space not found"),
                    createValidationResult("Page ID", "Page not found")
            );

            ExtensionResponse response = generator.generateConfirmationResponse(
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    results,
                    SUB_CATALOG_REQUEST,
                    Map.of()
            );

            assertNotNull(response);
            assertEquals(ExtensionResponse.Result.FAILURE, response.getResult());
            assertNotNull(response.getError());
            String errorMessage = response.getError().getErrorMessage();
            assertTrue(errorMessage.contains("Space not found"),
                    "Error should contain first validation error");
            assertTrue(errorMessage.contains("Page not found"),
                    "Error should contain second validation error");
        }
    }

    // ──────────────────────────────────────────────────────────────
    // generateFetchResponse()
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("generateFetchResponse")
    class GenerateFetchResponseTests {

        @Test
        @DisplayName("Should return non-null response with FAILURE result")
        void shouldReturnNonNullResponseWithFailureResult() {
            List<ValidationOrchestrator.ValidationResult> results = List.of(
                    createValidationResult("Template ID", "Template not found")
            );

            ExtensionResponse response = generator.generateFetchResponse(
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    results,
                    SUB_CATALOG_REQUEST,
                    Map.of()
            );

            assertNotNull(response, "Response should not be null");
            assertEquals(ExtensionResponse.Result.FAILURE, response.getResult());
        }

        @Test
        @DisplayName("Should include error message from validation results")
        void shouldIncludeErrorMessageFromValidationResults() {
            List<ValidationOrchestrator.ValidationResult> results = List.of(
                    createValidationResult("Space Key", "Invalid space key")
            );

            ExtensionResponse response = generator.generateFetchResponse(
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    results,
                    SUB_CATALOG_REQUEST,
                    Map.of()
            );

            assertNotNull(response.getError());
            assertTrue(response.getError().getErrorMessage().contains("Invalid space key"));
        }

        @Test
        @DisplayName("Should include remediation actions with input fields")
        void shouldIncludeRemediationActionsWithInputFields() {
            List<ValidationOrchestrator.ValidationResult> results = List.of(
                    createValidationResultWithAttributes(
                            "Page ID", "Page not found", "Identifier", Map.of("visualWidth", "S"))
            );

            ExtensionResponse response = generator.generateFetchResponse(
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    results,
                    SUB_CATALOG_REQUEST,
                    Map.of()
            );

            assertNotNull(response.getRemediationActions(),
                    "Remediation actions should not be null for fetch response");
        }

        @Test
        @DisplayName("Should preserve state in response")
        void shouldPreserveStateInResponse() {
            List<ValidationOrchestrator.ValidationResult> results = List.of(
                    createValidationResult("Field", "Error")
            );
            Map<String, Object> state = new HashMap<>();
            state.put("retryCount", 2);

            ExtensionResponse response = generator.generateFetchResponse(
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    results,
                    SUB_CATALOG_REQUEST,
                    state
            );

            assertNotNull(response.getState());
            assertEquals(2, response.getState().get("retryCount"));
        }

        @Test
        @DisplayName("Should handle multiple validation results with fields")
        void shouldHandleMultipleValidationResultsWithFields() {
            List<ValidationOrchestrator.ValidationResult> results = List.of(
                    createValidationResult("Space ID", "Space not found"),
                    createValidationResult("Page ID", "Page not found")
            );

            ExtensionResponse response = generator.generateFetchResponse(
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    results,
                    SUB_CATALOG_REQUEST,
                    Map.of()
            );

            assertNotNull(response);
            assertEquals(ExtensionResponse.Result.FAILURE, response.getResult());
        }
    }

    // ──────────────────────────────────────────────────────────────
    // generateFetchDenyResponse()
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("generateFetchDenyResponse")
    class GenerateFetchDenyResponseTests {

        @Test
        @DisplayName("Should return non-null response with FAILURE result")
        void shouldReturnNonNullResponseWithFailureResult() {
            List<ValidationOrchestrator.ValidationResult> results = List.of(
                    createValidationResult("Space ID", "Not provided")
            );

            ExtensionResponse response = generator.generateFetchDenyResponse(
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    results,
                    SUB_CATALOG_REQUEST,
                    Map.of()
            );

            assertNotNull(response, "Response should not be null");
            assertEquals(ExtensionResponse.Result.FAILURE, response.getResult());
        }

        @Test
        @DisplayName("Should include error message from validation results")
        void shouldIncludeErrorMessageFromResults() {
            List<ValidationOrchestrator.ValidationResult> results = List.of(
                    createValidationResult("Template ID", "Template missing")
            );

            ExtensionResponse response = generator.generateFetchDenyResponse(
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    results,
                    SUB_CATALOG_REQUEST,
                    Map.of()
            );

            assertNotNull(response.getError());
            assertTrue(response.getError().getErrorMessage().contains("Template missing"));
        }

        @Test
        @DisplayName("Should include remediation actions informing all participants")
        void shouldIncludeRemediationActionsInformingAllParticipants() {
            List<ValidationOrchestrator.ValidationResult> results = List.of(
                    createValidationResult("Space Name", "Missing")
            );

            ExtensionResponse response = generator.generateFetchDenyResponse(
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    results,
                    SUB_CATALOG_REQUEST,
                    Map.of()
            );

            assertNotNull(response.getRemediationActions(),
                    "Remediation actions should be present for deny response");
        }

        @Test
        @DisplayName("Should handle multiple missing fields in deny message")
        void shouldHandleMultipleMissingFields() {
            List<ValidationOrchestrator.ValidationResult> results = List.of(
                    createValidationResult("Space ID", "Space ID not provided"),
                    createValidationResult("Page ID", "Page ID not provided")
            );

            ExtensionResponse response = generator.generateFetchDenyResponse(
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    results,
                    SUB_CATALOG_REQUEST,
                    Map.of()
            );

            assertNotNull(response);
            assertEquals(ExtensionResponse.Result.FAILURE, response.getResult());
            assertNotNull(response.getError());
            // The error message should contain both error messages
            String errMsg = response.getError().getErrorMessage();
            assertTrue(errMsg.contains("Space ID not provided"));
            assertTrue(errMsg.contains("Page ID not provided"));
        }

        @Test
        @DisplayName("Should preserve state in deny response")
        void shouldPreserveState() {
            List<ValidationOrchestrator.ValidationResult> results = List.of(
                    createValidationResult("Field", "Missing")
            );
            Map<String, Object> state = new HashMap<>();
            state.put("key", "value");

            ExtensionResponse response = generator.generateFetchDenyResponse(
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    results,
                    SUB_CATALOG_REQUEST,
                    state
            );

            assertNotNull(response.getState());
            assertEquals("value", response.getState().get("key"));
        }
    }

    // ──────────────────────────────────────────────────────────────
    // generateValidationErrorResponse()
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("generateValidationErrorResponse")
    class GenerateValidationErrorResponseTests {

        @Test
        @DisplayName("Should return non-null response with FAILURE result")
        void shouldReturnNonNullResponseWithFailureResult() {
            List<ValidationOrchestrator.ValidationResult> results = List.of(
                    createValidationResult("Space ID", "Invalid space")
            );

            ExtensionResponse response = generator.generateValidationErrorResponse(results);

            assertNotNull(response, "Response should not be null");
            assertEquals(ExtensionResponse.Result.FAILURE, response.getResult());
        }

        @Test
        @DisplayName("Should have INPUT_ERROR exception type")
        void shouldHaveInputErrorExceptionType() {
            List<ValidationOrchestrator.ValidationResult> results = List.of(
                    createValidationResult("Field", "Error")
            );

            ExtensionResponse response = generator.generateValidationErrorResponse(results);

            assertNotNull(response.getError());
            assertEquals(ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    response.getError().getExceptionType());
        }

        @Test
        @DisplayName("Should include error message from validation results")
        void shouldIncludeErrorMessage() {
            List<ValidationOrchestrator.ValidationResult> results = List.of(
                    createValidationResult("Page ID", "Page ID is invalid")
            );

            ExtensionResponse response = generator.generateValidationErrorResponse(results);

            assertNotNull(response.getError());
            assertTrue(response.getError().getErrorMessage().contains("Page ID is invalid"));
        }

        @Test
        @DisplayName("Should not include remediation actions")
        void shouldNotIncludeRemediationActions() {
            List<ValidationOrchestrator.ValidationResult> results = List.of(
                    createValidationResult("Field", "Error")
            );

            ExtensionResponse response = generator.generateValidationErrorResponse(results);

            assertNull(response.getRemediationActions(),
                    "Validation error response should not have remediation actions");
        }

        @Test
        @DisplayName("Should concatenate error messages from multiple results")
        void shouldConcatenateMultipleErrorMessages() {
            List<ValidationOrchestrator.ValidationResult> results = List.of(
                    createValidationResult("Space ID", "Space invalid"),
                    createValidationResult("Template ID", "Template not found")
            );

            ExtensionResponse response = generator.generateValidationErrorResponse(results);

            assertNotNull(response.getError());
            String errMsg = response.getError().getErrorMessage();
            assertTrue(errMsg.contains("Space invalid"),
                    "Should contain first error message");
            assertTrue(errMsg.contains("Template not found"),
                    "Should contain second error message");
        }

        @Test
        @DisplayName("Should not include state")
        void shouldNotIncludeState() {
            List<ValidationOrchestrator.ValidationResult> results = List.of(
                    createValidationResult("Field", "Error")
            );

            ExtensionResponse response = generator.generateValidationErrorResponse(results);

            assertNull(response.getState(),
                    "Validation error response should not have state");
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Constants verification
    // ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("ERROR_MESSAGE constant should be 'ErrorMessage'")
    void errorMessageConstantShouldBeCorrect() {
        assertEquals("ErrorMessage", ExtensionResponseGenerator.ERROR_MESSAGE);
    }

    @Test
    @DisplayName("STEP_MESSAGE constant should be 'StepMessage'")
    void stepMessageConstantShouldBeCorrect() {
        assertEquals("StepMessage", ExtensionResponseGenerator.STEP_MESSAGE);
    }

    @Test
    @DisplayName("FIELD constant should be 'Field'")
    void fieldConstantShouldBeCorrect() {
        assertEquals("Field", ExtensionResponseGenerator.FIELD);
    }
}
