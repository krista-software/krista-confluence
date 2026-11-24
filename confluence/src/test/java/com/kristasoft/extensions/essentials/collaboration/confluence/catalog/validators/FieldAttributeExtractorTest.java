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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FieldAttributeExtractor")
class FieldAttributeExtractorTest {

    // ──────────────────────────────────────────────────────────────────────────
    // Helper class with annotated parameters for testing reflection-based extraction
    // ──────────────────────────────────────────────────────────────────────────

    @SuppressWarnings("unused")
    static class AnnotatedHelper {

        public void methodWithBasicField(
                @Field(name = "Space ID", type = "Identifier", required = true,
                        attributes = {@Attribute(name = "visualWidth", value = "S")},
                        options = {}) String spaceId) {
        }

        public void methodWithTextField(
                @Field.Text(name = "Space Name", required = true,
                        attributes = {@Attribute(name = "visualWidth", value = "M")},
                        options = {}) String spaceName) {
        }

        public void methodWithMultipleFields(
                @Field(name = "Space ID", type = "Identifier", required = true,
                        attributes = {@Attribute(name = "visualWidth", value = "S")},
                        options = {}) String spaceId,
                @Field.Text(name = "Page Title", required = true,
                        attributes = {@Attribute(name = "visualWidth", value = "L")},
                        options = {}) String pageTitle) {
        }

        public void methodWithNoAnnotations(String plainParam) {
        }

        public void methodWithMultipleAttributes(
                @Field(name = "Template ID", type = "Identifier", required = true,
                        attributes = {
                                @Attribute(name = "visualWidth", value = "S"),
                                @Attribute(name = "placeholder", value = "Enter ID")
                        },
                        options = {}) String templateId) {
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Tests for extractFieldAttributes
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("extractFieldAttributes")
    class ExtractFieldAttributesTests {

        @Test
        @DisplayName("should return attributes for matching @Field parameter")
        void shouldReturnAttributesForMatchingBasicField() {
            Method method = FieldAttributeExtractor.getMethodByName(AnnotatedHelper.class, "methodWithBasicField");
            assertNotNull(method);

            Map<String, Object> attributes = FieldAttributeExtractor.extractFieldAttributes(method, "Space ID");

            assertFalse(attributes.isEmpty(), "Attributes should not be empty for a matching field");
            assertEquals("S", attributes.get("visualWidth"));
        }

        @Test
        @DisplayName("should return attributes for matching @Field.Text parameter")
        void shouldReturnAttributesForMatchingTextField() {
            Method method = FieldAttributeExtractor.getMethodByName(AnnotatedHelper.class, "methodWithTextField");
            assertNotNull(method);

            Map<String, Object> attributes = FieldAttributeExtractor.extractFieldAttributes(method, "Space Name");

            assertFalse(attributes.isEmpty(), "Attributes should not be empty for a matching text field");
            assertEquals("M", attributes.get("visualWidth"));
        }

        @Test
        @DisplayName("should return empty map for non-matching field name")
        void shouldReturnEmptyMapForNonMatchingFieldName() {
            Method method = FieldAttributeExtractor.getMethodByName(AnnotatedHelper.class, "methodWithBasicField");
            assertNotNull(method);

            Map<String, Object> attributes = FieldAttributeExtractor.extractFieldAttributes(method, "Non Existent Field");

            assertTrue(attributes.isEmpty(), "Attributes should be empty for a non-matching field name");
        }

        @Test
        @DisplayName("should return correct attributes when multiple parameters exist")
        void shouldReturnCorrectAttributesFromMultipleParams() {
            Method method = FieldAttributeExtractor.getMethodByName(AnnotatedHelper.class, "methodWithMultipleFields");
            assertNotNull(method);

            Map<String, Object> spaceIdAttributes = FieldAttributeExtractor.extractFieldAttributes(method, "Space ID");
            assertFalse(spaceIdAttributes.isEmpty());
            assertEquals("S", spaceIdAttributes.get("visualWidth"));

            Map<String, Object> pageTitleAttributes = FieldAttributeExtractor.extractFieldAttributes(method, "Page Title");
            assertFalse(pageTitleAttributes.isEmpty());
            assertEquals("L", pageTitleAttributes.get("visualWidth"));
        }

        @Test
        @DisplayName("should return empty map when method has no annotated parameters")
        void shouldReturnEmptyMapForUnannotatedParameters() {
            Method method = FieldAttributeExtractor.getMethodByName(AnnotatedHelper.class, "methodWithNoAnnotations");
            assertNotNull(method);

            Map<String, Object> attributes = FieldAttributeExtractor.extractFieldAttributes(method, "plainParam");

            assertTrue(attributes.isEmpty());
        }

        @Test
        @DisplayName("should return empty map when method is null")
        void shouldReturnEmptyMapWhenMethodIsNull() {
            Map<String, Object> attributes = FieldAttributeExtractor.extractFieldAttributes(null, "Space ID");

            assertTrue(attributes.isEmpty());
        }

        @Test
        @DisplayName("should return empty map when field name is null")
        void shouldReturnEmptyMapWhenFieldNameIsNull() {
            Method method = FieldAttributeExtractor.getMethodByName(AnnotatedHelper.class, "methodWithBasicField");

            Map<String, Object> attributes = FieldAttributeExtractor.extractFieldAttributes(method, null);

            assertTrue(attributes.isEmpty());
        }

        @Test
        @DisplayName("should return all attributes when parameter has multiple attributes")
        void shouldReturnMultipleAttributes() {
            Method method = FieldAttributeExtractor.getMethodByName(AnnotatedHelper.class, "methodWithMultipleAttributes");
            assertNotNull(method);

            Map<String, Object> attributes = FieldAttributeExtractor.extractFieldAttributes(method, "Template ID");

            assertFalse(attributes.isEmpty());
            assertEquals("S", attributes.get("visualWidth"));
            assertEquals("Enter ID", attributes.get("placeholder"));
            assertEquals(2, attributes.size());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Tests for extractFieldType
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("extractFieldType")
    class ExtractFieldTypeTests {

        @Test
        @DisplayName("should return field type for matching @Field parameter")
        void shouldReturnFieldTypeForMatchingBasicField() {
            Method method = FieldAttributeExtractor.getMethodByName(AnnotatedHelper.class, "methodWithBasicField");
            assertNotNull(method);

            String fieldType = FieldAttributeExtractor.extractFieldType(method, "Space ID");

            assertNotNull(fieldType);
            assertEquals("Identifier", fieldType);
        }

        @Test
        @DisplayName("should return field type for matching @Field.Text parameter")
        void shouldReturnFieldTypeForMatchingTextField() {
            Method method = FieldAttributeExtractor.getMethodByName(AnnotatedHelper.class, "methodWithTextField");
            assertNotNull(method);

            String fieldType = FieldAttributeExtractor.extractFieldType(method, "Space Name");

            assertNotNull(fieldType);
            assertEquals("Text", fieldType);
        }

        @Test
        @DisplayName("should return null for non-matching field name")
        void shouldReturnNullForNonMatchingFieldName() {
            Method method = FieldAttributeExtractor.getMethodByName(AnnotatedHelper.class, "methodWithBasicField");
            assertNotNull(method);

            String fieldType = FieldAttributeExtractor.extractFieldType(method, "Does Not Exist");

            assertNull(fieldType);
        }

        @Test
        @DisplayName("should return correct field type from multiple parameters")
        void shouldReturnCorrectFieldTypeFromMultipleParams() {
            Method method = FieldAttributeExtractor.getMethodByName(AnnotatedHelper.class, "methodWithMultipleFields");
            assertNotNull(method);

            String spaceIdType = FieldAttributeExtractor.extractFieldType(method, "Space ID");
            assertEquals("Identifier", spaceIdType);

            String pageTitleType = FieldAttributeExtractor.extractFieldType(method, "Page Title");
            assertEquals("Text", pageTitleType);
        }

        @Test
        @DisplayName("should return null when method is null")
        void shouldReturnNullWhenMethodIsNull() {
            String fieldType = FieldAttributeExtractor.extractFieldType(null, "Space ID");

            assertNull(fieldType);
        }

        @Test
        @DisplayName("should return null when field name is null")
        void shouldReturnNullWhenFieldNameIsNull() {
            Method method = FieldAttributeExtractor.getMethodByName(AnnotatedHelper.class, "methodWithBasicField");

            String fieldType = FieldAttributeExtractor.extractFieldType(method, null);

            assertNull(fieldType);
        }

        @Test
        @DisplayName("should return null when method has no annotated parameters")
        void shouldReturnNullForUnannotatedParameters() {
            Method method = FieldAttributeExtractor.getMethodByName(AnnotatedHelper.class, "methodWithNoAnnotations");
            assertNotNull(method);

            String fieldType = FieldAttributeExtractor.extractFieldType(method, "plainParam");

            assertNull(fieldType);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Tests for getMethodByName
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getMethodByName")
    class GetMethodByNameTests {

        @Test
        @DisplayName("should return method when it exists in the class")
        void shouldReturnMethodWhenExists() {
            Method method = FieldAttributeExtractor.getMethodByName(AnnotatedHelper.class, "methodWithBasicField");

            assertNotNull(method);
            assertEquals("methodWithBasicField", method.getName());
        }

        @Test
        @DisplayName("should return null when method does not exist")
        void shouldReturnNullForNonExistentMethod() {
            Method method = FieldAttributeExtractor.getMethodByName(AnnotatedHelper.class, "nonExistentMethod");

            assertNull(method);
        }

        @Test
        @DisplayName("should return null when class is null")
        void shouldReturnNullWhenClassIsNull() {
            Method method = FieldAttributeExtractor.getMethodByName(null, "methodWithBasicField");

            assertNull(method);
        }

        @Test
        @DisplayName("should return null when method name is null")
        void shouldReturnNullWhenMethodNameIsNull() {
            Method method = FieldAttributeExtractor.getMethodByName(AnnotatedHelper.class, null);

            assertNull(method);
        }

        @Test
        @DisplayName("should find method with multiple parameters")
        void shouldFindMethodWithMultipleParameters() {
            Method method = FieldAttributeExtractor.getMethodByName(AnnotatedHelper.class, "methodWithMultipleFields");

            assertNotNull(method);
            assertEquals("methodWithMultipleFields", method.getName());
            assertEquals(2, method.getParameterCount());
        }

        @Test
        @DisplayName("should find method with no annotations")
        void shouldFindMethodWithNoAnnotations() {
            Method method = FieldAttributeExtractor.getMethodByName(AnnotatedHelper.class, "methodWithNoAnnotations");

            assertNotNull(method);
            assertEquals("methodWithNoAnnotations", method.getName());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Tests for createFieldNameToResourceMapping
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createFieldNameToResourceMapping")
    class CreateFieldNameToResourceMappingTests {

        @Test
        @DisplayName("should map @Field with type=Identifier and name 'Space ID' to SPACE_ID")
        void shouldMapIdentifierSpaceIdToResource() {
            Method method = FieldAttributeExtractor.getMethodByName(AnnotatedHelper.class, "methodWithBasicField");
            assertNotNull(method);

            Map<String, Validator.ValidationResource> mapping =
                    FieldAttributeExtractor.createFieldNameToResourceMapping(method);

            assertNotNull(mapping);
            assertEquals(Validator.ValidationResource.SPACE_ID, mapping.get("Space ID"));
        }

        @Test
        @DisplayName("should map @Field.Text with name 'Space Name' to SPACE_NAME")
        void shouldMapTextSpaceNameToResource() {
            Method method = FieldAttributeExtractor.getMethodByName(AnnotatedHelper.class, "methodWithTextField");
            assertNotNull(method);

            Map<String, Validator.ValidationResource> mapping =
                    FieldAttributeExtractor.createFieldNameToResourceMapping(method);

            assertNotNull(mapping);
            assertEquals(Validator.ValidationResource.SPACE_NAME, mapping.get("Space Name"));
        }

        @Test
        @DisplayName("should return empty mapping when method is null")
        void shouldReturnEmptyMappingWhenMethodIsNull() {
            Map<String, Validator.ValidationResource> mapping =
                    FieldAttributeExtractor.createFieldNameToResourceMapping(null);

            assertNotNull(mapping);
            assertTrue(mapping.isEmpty());
        }

        @Test
        @DisplayName("should handle method with no annotated parameters")
        void shouldReturnEmptyMappingForUnannotatedMethod() {
            Method method = FieldAttributeExtractor.getMethodByName(AnnotatedHelper.class, "methodWithNoAnnotations");
            assertNotNull(method);

            Map<String, Validator.ValidationResource> mapping =
                    FieldAttributeExtractor.createFieldNameToResourceMapping(method);

            assertNotNull(mapping);
            assertTrue(mapping.isEmpty());
        }

        @Test
        @DisplayName("should create mapping for multiple annotated parameters")
        void shouldCreateMappingForMultipleParams() {
            Method method = FieldAttributeExtractor.getMethodByName(AnnotatedHelper.class, "methodWithMultipleFields");
            assertNotNull(method);

            Map<String, Validator.ValidationResource> mapping =
                    FieldAttributeExtractor.createFieldNameToResourceMapping(method);

            assertNotNull(mapping);
            // "Space ID" with type=Identifier should map to SPACE_ID
            assertEquals(Validator.ValidationResource.SPACE_ID, mapping.get("Space ID"));
            // "Page Title" is a @Field.Text; it does not have a specific resource mapping
            // for that name, so it will not appear in the mapping
        }

        @Test
        @DisplayName("should not map @Field(type=Identifier) 'Template ID' when BasicField strategy intercepts first")
        void shouldNotMapTemplateIdWhenBasicStrategyInterceptsFirst() {
            // The BasicFieldAnnotationStrategy handles @Field annotations first in the registry
            // and its mapFieldTypeToValidationResource does not map "Template ID".
            // Since the loop breaks after the first matching strategy, the IdentifierFieldAnnotationStrategy
            // never gets a chance to handle it.
            Method method = FieldAttributeExtractor.getMethodByName(AnnotatedHelper.class, "methodWithMultipleAttributes");
            assertNotNull(method);

            Map<String, Validator.ValidationResource> mapping =
                    FieldAttributeExtractor.createFieldNameToResourceMapping(method);

            assertNotNull(mapping);
            // BasicFieldAnnotationStrategy intercepts first but doesn't map "Template ID" to a resource
            assertFalse(mapping.containsKey("Template ID"),
                    "Template ID should not be mapped because BasicFieldAnnotationStrategy intercepts first "
                    + "and does not map it to a ValidationResource");
        }
    }
}
