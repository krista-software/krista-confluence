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

package com.kristasoft.extensions.essentials.collaboration.confluence.catalog.validators;

import app.krista.extension.impl.anno.Attribute;
import app.krista.extension.impl.anno.Field;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.validators.Validator.ValidationResource;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.validators.ValidationOrchestrator.ValidationResult;
import com.kristasoft.extensions.essentials.collaboration.confluence.service.ConfluenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ValidationOrchestrator")
class ValidationOrchestratorTest {

    @Mock
    private ConfluenceService confluenceService;

    private ValidationOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        orchestrator = new ValidationOrchestrator(confluenceService);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helper class with annotated parameters for dynamic extraction tests
    // ──────────────────────────────────────────────────────────────────────────

    @SuppressWarnings("unused")
    static class CatalogHelper {

        public void createPage(
                @Field(name = "Space ID", type = "Identifier", required = true,
                        attributes = {@Attribute(name = "visualWidth", value = "S")},
                        options = {}) String spaceId,
                @Field.Text(name = "Page Title", required = true,
                        attributes = {@Attribute(name = "visualWidth", value = "L")},
                        options = {}) String pageTitle) {
        }

        public void createFromTemplate(
                @Field(name = "Space ID", type = "Identifier", required = true,
                        attributes = {@Attribute(name = "visualWidth", value = "S")},
                        options = {}) String spaceId,
                @Field(name = "Template ID", type = "Identifier", required = true,
                        attributes = {@Attribute(name = "visualWidth", value = "S")},
                        options = {}) String templateId) {
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Tests for ValidationResult record
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("ValidationResult record")
    class ValidationResultTests {

        @Test
        @DisplayName("should create a ValidationResult with all fields")
        void shouldCreateValidationResultWithAllFields() {
            Map<String, Object> attributes = Map.of("visualWidth", "S");
            ValidationResult result = new ValidationResult(
                    "confirm msg",
                    "Space Id",
                    "fetch msg",
                    "error msg",
                    "Identifier",
                    attributes
            );

            assertEquals("confirm msg", result.confirmStepMessage());
            assertEquals("Space Id", result.fetchFieldName());
            assertEquals("fetch msg", result.fetchStepMessage());
            assertEquals("error msg", result.errMessage());
            assertEquals("Identifier", result.fieldType());
            assertEquals(attributes, result.fieldAttributes());
        }

        @Test
        @DisplayName("should support null values in ValidationResult fields")
        void shouldSupportNullValues() {
            ValidationResult result = new ValidationResult(null, null, null, null, null, null);

            assertNull(result.confirmStepMessage());
            assertNull(result.fetchFieldName());
            assertNull(result.fetchStepMessage());
            assertNull(result.errMessage());
            assertNull(result.fieldType());
            assertNull(result.fieldAttributes());
        }

        @Test
        @DisplayName("should have correct equality semantics for records")
        void shouldHaveCorrectEquality() {
            Map<String, Object> attributes = Map.of("key", "value");
            ValidationResult result1 = new ValidationResult("c", "f", "s", "e", "t", attributes);
            ValidationResult result2 = new ValidationResult("c", "f", "s", "e", "t", attributes);

            assertEquals(result1, result2);
            assertEquals(result1.hashCode(), result2.hashCode());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Tests for validate (simple overload, no source class/method)
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("validate with no source class")
    class ValidateSimpleTests {

        @Test
        @DisplayName("should return empty list when all validations pass")
        void shouldReturnEmptyListWhenAllValidationsPass() {
            // Space ID validation calls getSpaceById - mock it to succeed
            when(confluenceService.getSpaceById("12345")).thenReturn(
                    com.kristasoft.extensions.essentials.collaboration.confluence.dto.Space.builder().setId("12345").build()
            );

            Map<ValidationResource, String> resources = new HashMap<>();
            resources.put(ValidationResource.SPACE_ID, "12345");

            List<ValidationResult> results = orchestrator.validate(resources);

            assertNotNull(results);
            assertTrue(results.isEmpty(), "All validations passed so results should be empty");
        }

        @Test
        @DisplayName("should return ValidationResult when SPACE_ID validation fails")
        void shouldReturnResultWhenSpaceIdValidationFails() {
            // Mock getSpaceById to throw an exception (space not found)
            when(confluenceService.getSpaceById("invalid_id"))
                    .thenThrow(new RuntimeException("Space not found"));

            Map<ValidationResource, String> resources = new HashMap<>();
            resources.put(ValidationResource.SPACE_ID, "invalid_id");

            List<ValidationResult> results = orchestrator.validate(resources);

            assertNotNull(results);
            assertFalse(results.isEmpty(), "Validation should fail for invalid space ID");
            assertEquals(1, results.size());

            ValidationResult result = results.get(0);
            assertNotNull(result.fetchFieldName());
            assertNotNull(result.fetchStepMessage());
            assertNotNull(result.errMessage());
            assertNotNull(result.confirmStepMessage());
        }

        @Test
        @DisplayName("should return ValidationResult when SPACE_NAME is null")
        void shouldReturnResultWhenSpaceNameIsNull() {
            Map<ValidationResource, String> resources = new HashMap<>();
            resources.put(ValidationResource.SPACE_NAME, null);

            List<ValidationResult> results = orchestrator.validate(resources);

            assertNotNull(results);
            assertFalse(results.isEmpty(), "Validation should fail for null space name");
            assertEquals(1, results.size());
        }

        @Test
        @DisplayName("should return ValidationResult when SPACE_NAME is empty")
        void shouldReturnResultWhenSpaceNameIsEmpty() {
            Map<ValidationResource, String> resources = new HashMap<>();
            resources.put(ValidationResource.SPACE_NAME, "   ");

            List<ValidationResult> results = orchestrator.validate(resources);

            assertNotNull(results);
            assertFalse(results.isEmpty(), "Validation should fail for empty space name");
        }

        @Test
        @DisplayName("should return ValidationResult when SPACE_KEY is null")
        void shouldReturnResultWhenSpaceKeyIsNull() {
            Map<ValidationResource, String> resources = new HashMap<>();
            resources.put(ValidationResource.SPACE_KEY, null);

            List<ValidationResult> results = orchestrator.validate(resources);

            assertNotNull(results);
            assertFalse(results.isEmpty(), "Validation should fail for null space key");
        }

        @Test
        @DisplayName("should return ValidationResult when PAGE_ID is null")
        void shouldReturnResultWhenPageIdIsNull() {
            Map<ValidationResource, String> resources = new HashMap<>();
            resources.put(ValidationResource.PAGE_ID, null);

            List<ValidationResult> results = orchestrator.validate(resources);

            assertNotNull(results);
            assertFalse(results.isEmpty(), "Validation should fail for null page ID");
        }

        @Test
        @DisplayName("should return ValidationResult when TEMPLATE_ID is null")
        void shouldReturnResultWhenTemplateIdIsNull() {
            Map<ValidationResource, String> resources = new HashMap<>();
            resources.put(ValidationResource.TEMPLATE_ID, null);

            List<ValidationResult> results = orchestrator.validate(resources);

            assertNotNull(results);
            assertFalse(results.isEmpty(), "Validation should fail for null template ID");
        }

        @Test
        @DisplayName("should return null for resource with no registered validator")
        void shouldNotFailForUnregisteredResource() {
            Map<ValidationResource, String> resources = new HashMap<>();
            resources.put(ValidationResource.EMAIL, "test@example.com");

            List<ValidationResult> results = orchestrator.validate(resources);

            assertNotNull(results);
            // EMAIL has no registered validator in the orchestrator, so it should pass (no result)
            assertTrue(results.isEmpty(),
                    "Unregistered resources should not produce validation results");
        }

        @Test
        @DisplayName("should return empty list when resources map is empty")
        void shouldReturnEmptyListForEmptyResources() {
            Map<ValidationResource, String> resources = new HashMap<>();

            List<ValidationResult> results = orchestrator.validate(resources);

            assertNotNull(results);
            assertTrue(results.isEmpty());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Tests for validate with source class and method name (dynamic extraction)
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("validate with source class and method name")
    class ValidateWithDynamicExtractionTests {

        @Test
        @DisplayName("should use dynamic field attributes when source class and method are provided")
        void shouldUseDynamicFieldAttributes() {
            // Space ID validation will fail because getSpaceById throws
            when(confluenceService.getSpaceById("bad-id"))
                    .thenThrow(new RuntimeException("Space not found"));

            Map<ValidationResource, String> resources = new HashMap<>();
            resources.put(ValidationResource.SPACE_ID, "bad-id");

            List<ValidationResult> results = orchestrator.validate(
                    resources, CatalogHelper.class, "createPage");

            assertNotNull(results);
            assertFalse(results.isEmpty());

            ValidationResult result = results.get(0);
            assertNotNull(result.fieldType());
            assertNotNull(result.fieldAttributes());
            // The dynamically extracted attributes should include visualWidth="S"
            // from the @Field annotation on the helper class
            assertEquals("S", result.fieldAttributes().get("visualWidth"));
            assertEquals("Identifier", result.fieldType());
        }

        @Test
        @DisplayName("should fall back to hardcoded attributes when method is not found")
        void shouldFallBackWhenMethodNotFound() {
            when(confluenceService.getSpaceById("bad-id"))
                    .thenThrow(new RuntimeException("Space not found"));

            Map<ValidationResource, String> resources = new HashMap<>();
            resources.put(ValidationResource.SPACE_ID, "bad-id");

            List<ValidationResult> results = orchestrator.validate(
                    resources, CatalogHelper.class, "nonExistentMethod");

            assertNotNull(results);
            assertFalse(results.isEmpty());

            ValidationResult result = results.get(0);
            // Should fall back to hardcoded field type
            assertNotNull(result.fieldType());
        }

        @Test
        @DisplayName("should fall back when source class is null")
        void shouldFallBackWhenSourceClassIsNull() {
            when(confluenceService.getSpaceById("bad-id"))
                    .thenThrow(new RuntimeException("Space not found"));

            Map<ValidationResource, String> resources = new HashMap<>();
            resources.put(ValidationResource.SPACE_ID, "bad-id");

            List<ValidationResult> results = orchestrator.validate(resources, null, null);

            assertNotNull(results);
            assertFalse(results.isEmpty());

            ValidationResult result = results.get(0);
            // Should use hardcoded fallback values
            assertNotNull(result.fieldType());
            assertEquals("Identifier", result.fieldType());
        }

        @Test
        @DisplayName("should handle multiple failed validations with dynamic extraction")
        void shouldHandleMultipleFailedValidations() {
            when(confluenceService.getSpaceById("bad-space"))
                    .thenThrow(new RuntimeException("Space not found"));
            when(confluenceService.getTemplateById("bad-template"))
                    .thenThrow(new RuntimeException("Template not found"));

            Map<ValidationResource, String> resources = new HashMap<>();
            resources.put(ValidationResource.SPACE_ID, "bad-space");
            resources.put(ValidationResource.TEMPLATE_ID, "bad-template");

            List<ValidationResult> results = orchestrator.validate(
                    resources, CatalogHelper.class, "createFromTemplate");

            assertNotNull(results);
            assertEquals(2, results.size(), "Both validations should fail");
        }

        @Test
        @DisplayName("should return empty list when all dynamic validations pass")
        void shouldReturnEmptyListWhenAllDynamicValidationsPass() {
            when(confluenceService.getSpaceById("valid-space")).thenReturn(
                    com.kristasoft.extensions.essentials.collaboration.confluence.dto.Space.builder().setId("valid-space").build()
            );
            when(confluenceService.getTemplateById("valid-template")).thenReturn(
                    com.kristasoft.extensions.essentials.collaboration.confluence.dto.Template.builder().setTemplateId("valid-template").build()
            );

            Map<ValidationResource, String> resources = new HashMap<>();
            resources.put(ValidationResource.SPACE_ID, "valid-space");
            resources.put(ValidationResource.TEMPLATE_ID, "valid-template");

            List<ValidationResult> results = orchestrator.validate(
                    resources, CatalogHelper.class, "createFromTemplate");

            assertNotNull(results);
            assertTrue(results.isEmpty(), "All validations should pass");
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Tests for fallback field type resolution
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("fallback field type and attribute resolution")
    class FallbackResolutionTests {

        @Test
        @DisplayName("should use fallback field type 'Identifier' for SPACE_ID")
        void shouldUseFallbackTypeForSpaceId() {
            when(confluenceService.getSpaceById("bad-id"))
                    .thenThrow(new RuntimeException("Not found"));

            Map<ValidationResource, String> resources = new HashMap<>();
            resources.put(ValidationResource.SPACE_ID, "bad-id");

            List<ValidationResult> results = orchestrator.validate(resources);

            assertFalse(results.isEmpty());
            assertEquals("Identifier", results.get(0).fieldType());
        }

        @Test
        @DisplayName("should use fallback field type 'Text' for SPACE_NAME")
        void shouldUseFallbackTypeForSpaceName() {
            Map<ValidationResource, String> resources = new HashMap<>();
            resources.put(ValidationResource.SPACE_NAME, "");

            List<ValidationResult> results = orchestrator.validate(resources);

            assertFalse(results.isEmpty());
            assertEquals("Text", results.get(0).fieldType());
        }

        @Test
        @DisplayName("should use fallback field type 'Text' for SPACE_KEY")
        void shouldUseFallbackTypeForSpaceKey() {
            Map<ValidationResource, String> resources = new HashMap<>();
            resources.put(ValidationResource.SPACE_KEY, null);

            List<ValidationResult> results = orchestrator.validate(resources);

            assertFalse(results.isEmpty());
            assertEquals("Text", results.get(0).fieldType());
        }

        @Test
        @DisplayName("should use fallback field type 'Identifier' for PAGE_ID")
        void shouldUseFallbackTypeForPageId() {
            Map<ValidationResource, String> resources = new HashMap<>();
            resources.put(ValidationResource.PAGE_ID, null);

            List<ValidationResult> results = orchestrator.validate(resources);

            assertFalse(results.isEmpty());
            assertEquals("Identifier", results.get(0).fieldType());
        }

        @Test
        @DisplayName("should use fallback field type 'Identifier' for TEMPLATE_ID")
        void shouldUseFallbackTypeForTemplateId() {
            Map<ValidationResource, String> resources = new HashMap<>();
            resources.put(ValidationResource.TEMPLATE_ID, null);

            List<ValidationResult> results = orchestrator.validate(resources);

            assertFalse(results.isEmpty());
            assertEquals("Identifier", results.get(0).fieldType());
        }

        @Test
        @DisplayName("should include fetch field name from validator in fallback")
        void shouldIncludeFetchFieldNameFromValidator() {
            when(confluenceService.getSpaceById("bad"))
                    .thenThrow(new RuntimeException("Not found"));

            Map<ValidationResource, String> resources = new HashMap<>();
            resources.put(ValidationResource.SPACE_ID, "bad");

            List<ValidationResult> results = orchestrator.validate(resources);

            assertFalse(results.isEmpty());
            assertEquals("Space Id", results.get(0).fetchFieldName());
        }

        @Test
        @DisplayName("should use fallback attributes with visualWidth for SPACE_ID")
        void shouldUseFallbackAttributesForSpaceId() {
            when(confluenceService.getSpaceById("bad"))
                    .thenThrow(new RuntimeException("Not found"));

            Map<ValidationResource, String> resources = new HashMap<>();
            resources.put(ValidationResource.SPACE_ID, "bad");

            List<ValidationResult> results = orchestrator.validate(resources);

            assertFalse(results.isEmpty());
            Map<String, Object> attrs = results.get(0).fieldAttributes();
            assertNotNull(attrs);
            assertEquals("S", attrs.get("visualWidth"));
        }

        @Test
        @DisplayName("should use empty fallback attributes for SPACE_NAME")
        void shouldUseEmptyFallbackAttributesForSpaceName() {
            Map<ValidationResource, String> resources = new HashMap<>();
            resources.put(ValidationResource.SPACE_NAME, "");

            List<ValidationResult> results = orchestrator.validate(resources);

            assertFalse(results.isEmpty());
            Map<String, Object> attrs = results.get(0).fieldAttributes();
            assertNotNull(attrs);
            assertTrue(attrs.isEmpty(), "SPACE_NAME fallback attributes should be empty");
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Tests for validation error messages
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("validation error messages")
    class ValidationErrorMessageTests {

        @Test
        @DisplayName("should include resource value in error message for SPACE_ID")
        void shouldIncludeResourceValueInSpaceIdError() {
            when(confluenceService.getSpaceById("bad-space"))
                    .thenThrow(new RuntimeException("Not found"));

            Map<ValidationResource, String> resources = new HashMap<>();
            resources.put(ValidationResource.SPACE_ID, "bad-space");

            List<ValidationResult> results = orchestrator.validate(resources);

            assertFalse(results.isEmpty());
            assertTrue(results.get(0).errMessage().contains("bad-space"),
                    "Error message should contain the invalid space ID");
        }

        @Test
        @DisplayName("should include confirmation step message for failed validation")
        void shouldIncludeConfirmationMessage() {
            when(confluenceService.getSpaceById("bad-space"))
                    .thenThrow(new RuntimeException("Not found"));

            Map<ValidationResource, String> resources = new HashMap<>();
            resources.put(ValidationResource.SPACE_ID, "bad-space");

            List<ValidationResult> results = orchestrator.validate(resources);

            assertFalse(results.isEmpty());
            assertNotNull(results.get(0).confirmStepMessage());
            assertTrue(results.get(0).confirmStepMessage().contains("bad-space"),
                    "Confirmation message should contain the resource value");
        }

        @Test
        @DisplayName("should include fetch step message for failed validation")
        void shouldIncludeFetchStepMessage() {
            Map<ValidationResource, String> resources = new HashMap<>();
            resources.put(ValidationResource.SPACE_KEY, null);

            List<ValidationResult> results = orchestrator.validate(resources);

            assertFalse(results.isEmpty());
            assertNotNull(results.get(0).fetchStepMessage());
            assertFalse(results.get(0).fetchStepMessage().isEmpty(),
                    "Fetch step message should not be empty");
        }
    }
}
