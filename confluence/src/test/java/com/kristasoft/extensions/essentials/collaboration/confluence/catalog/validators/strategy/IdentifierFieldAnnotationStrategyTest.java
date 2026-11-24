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

package com.kristasoft.extensions.essentials.collaboration.confluence.catalog.validators.strategy;

import app.krista.extension.impl.anno.Attribute;
import app.krista.extension.impl.anno.Field;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.validators.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for {@link IdentifierFieldAnnotationStrategy}.
 * Uses reflection to obtain Parameter objects from annotated helper methods.
 */
@DisplayName("IdentifierFieldAnnotationStrategy Tests")
class IdentifierFieldAnnotationStrategyTest {

    private IdentifierFieldAnnotationStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new IdentifierFieldAnnotationStrategy();
    }

    // ── Helper class with annotated methods for reflection-based testing ──

    @SuppressWarnings("unused")
    static class AnnotatedHelper {
        void identifierParam(
                @Field(name = "Space ID", type = "Identifier",
                        attributes = {@Attribute(name = "toolTip", value = "test")})
                String spaceId) {}

        void noAnnotationParam(String raw) {}

        void pageIdParam(
                @Field(name = "Page ID", type = "Identifier", attributes = {})
                String pageId) {}

        void pageIdAltParam(
                @Field(name = "Page Id", type = "Identifier", attributes = {})
                String pageId) {}

        void pageIdCamelParam(
                @Field(name = "PageId", type = "Identifier", attributes = {})
                String pageId) {}

        void templateIdParam(
                @Field(name = "Template ID", type = "Identifier", attributes = {})
                String templateId) {}

        void templateIdAltParam(
                @Field(name = "Template Id", type = "Identifier", attributes = {})
                String templateId) {}

        void templateIdCamelParam(
                @Field(name = "TemplateId", type = "Identifier", attributes = {})
                String templateId) {}

        void spaceKeyParam(
                @Field(name = "Space Key", type = "Identifier", attributes = {})
                String spaceKey) {}

        void spaceNameParam(
                @Field(name = "Space Name", type = "Identifier", attributes = {})
                String spaceName) {}

        void spaceIdAltParam(
                @Field(name = "Space Id", type = "Identifier", attributes = {})
                String spaceId) {}

        void spaceIdOrKeyParam(
                @Field(name = "Space Id or Key", type = "Identifier", attributes = {})
                String spaceIdOrKey) {}

        void unknownIdentifier(
                @Field(name = "Unknown Field", type = "Identifier", attributes = {})
                String unknown) {}

        void wrongTypeParam(
                @Field(name = "Space ID", type = "Text", attributes = {})
                String spaceId) {}

        void multipleAttributesParam(
                @Field(name = "Space ID", type = "Identifier",
                        attributes = {
                                @Attribute(name = "toolTip", value = "tip"),
                                @Attribute(name = "visualWidth", value = "S")
                        })
                String spaceId) {}
    }

    /**
     * Utility to get the first Parameter from a named method in AnnotatedHelper.
     */
    private Parameter getParameter(String methodName) {
        try {
            for (Method m : AnnotatedHelper.class.getDeclaredMethods()) {
                if (m.getName().equals(methodName)) {
                    return m.getParameters()[0];
                }
            }
            throw new IllegalArgumentException("Method not found: " + methodName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ── canHandle tests ──

    @Nested
    @DisplayName("canHandle()")
    class CanHandleTests {

        @Test
        @DisplayName("Should return true for matching @Field with type=Identifier and matching name")
        void shouldReturnTrueForMatchingFieldAnnotation() {
            Parameter param = getParameter("identifierParam");
            assertTrue(strategy.canHandle(param, "Space ID"));
        }

        @Test
        @DisplayName("Should return false when parameter has no annotation")
        void shouldReturnFalseForNoAnnotation() {
            Parameter param = getParameter("noAnnotationParam");
            assertFalse(strategy.canHandle(param, "Space ID"));
        }

        @Test
        @DisplayName("Should return false when field name does not match")
        void shouldReturnFalseForWrongName() {
            Parameter param = getParameter("identifierParam");
            assertFalse(strategy.canHandle(param, "Wrong Name"));
        }

        @Test
        @DisplayName("Should return false when type is not Identifier")
        void shouldReturnFalseForWrongType() {
            Parameter param = getParameter("wrongTypeParam");
            assertFalse(strategy.canHandle(param, "Space ID"));
        }

        @Test
        @DisplayName("Should return true for Page ID identifier")
        void shouldReturnTrueForPageIdIdentifier() {
            Parameter param = getParameter("pageIdParam");
            assertTrue(strategy.canHandle(param, "Page ID"));
        }

        @Test
        @DisplayName("Should return true for Template ID identifier")
        void shouldReturnTrueForTemplateIdIdentifier() {
            Parameter param = getParameter("templateIdParam");
            assertTrue(strategy.canHandle(param, "Template ID"));
        }

        @Test
        @DisplayName("Should return true for Space Key identifier")
        void shouldReturnTrueForSpaceKeyIdentifier() {
            Parameter param = getParameter("spaceKeyParam");
            assertTrue(strategy.canHandle(param, "Space Key"));
        }

        @Test
        @DisplayName("Should return true for Space Name identifier")
        void shouldReturnTrueForSpaceNameIdentifier() {
            Parameter param = getParameter("spaceNameParam");
            assertTrue(strategy.canHandle(param, "Space Name"));
        }
    }

    // ── extractAttributes tests ──

    @Nested
    @DisplayName("extractAttributes()")
    class ExtractAttributesTests {

        @Test
        @DisplayName("Should return attributes from @Field annotation")
        void shouldReturnAttributesFromFieldAnnotation() {
            Parameter param = getParameter("identifierParam");
            Map<String, Object> attrs = strategy.extractAttributes(param, "Space ID");

            assertNotNull(attrs);
            assertFalse(attrs.isEmpty());
            assertEquals("test", attrs.get("toolTip"));
        }

        @Test
        @DisplayName("Should return multiple attributes when present")
        void shouldReturnMultipleAttributes() {
            Parameter param = getParameter("multipleAttributesParam");
            Map<String, Object> attrs = strategy.extractAttributes(param, "Space ID");

            assertNotNull(attrs);
            assertEquals(2, attrs.size());
            assertEquals("tip", attrs.get("toolTip"));
            assertEquals("S", attrs.get("visualWidth"));
        }

        @Test
        @DisplayName("Should return empty map when no annotation present")
        void shouldReturnEmptyMapForNoAnnotation() {
            Parameter param = getParameter("noAnnotationParam");
            Map<String, Object> attrs = strategy.extractAttributes(param, "Space ID");

            assertNotNull(attrs);
            assertTrue(attrs.isEmpty());
        }

        @Test
        @DisplayName("Should return empty map when field name does not match")
        void shouldReturnEmptyMapForWrongName() {
            Parameter param = getParameter("identifierParam");
            Map<String, Object> attrs = strategy.extractAttributes(param, "Wrong Name");

            assertNotNull(attrs);
            assertTrue(attrs.isEmpty());
        }

        @Test
        @DisplayName("Should return empty map when type is not Identifier")
        void shouldReturnEmptyMapForWrongType() {
            Parameter param = getParameter("wrongTypeParam");
            Map<String, Object> attrs = strategy.extractAttributes(param, "Space ID");

            assertNotNull(attrs);
            assertTrue(attrs.isEmpty());
        }

        @Test
        @DisplayName("Should return empty map for empty attributes array")
        void shouldReturnEmptyMapForEmptyAttributesArray() {
            Parameter param = getParameter("pageIdParam");
            Map<String, Object> attrs = strategy.extractAttributes(param, "Page ID");

            assertNotNull(attrs);
            assertTrue(attrs.isEmpty());
        }
    }

    // ── extractFieldType tests ──

    @Nested
    @DisplayName("extractFieldType()")
    class ExtractFieldTypeTests {

        @Test
        @DisplayName("Should return 'Identifier' when annotation matches")
        void shouldReturnIdentifierWhenMatches() {
            Parameter param = getParameter("identifierParam");
            assertEquals("Identifier", strategy.extractFieldType(param, "Space ID"));
        }

        @Test
        @DisplayName("Should return null when no annotation present")
        void shouldReturnNullForNoAnnotation() {
            Parameter param = getParameter("noAnnotationParam");
            assertNull(strategy.extractFieldType(param, "Space ID"));
        }

        @Test
        @DisplayName("Should return null when field name does not match")
        void shouldReturnNullForWrongName() {
            Parameter param = getParameter("identifierParam");
            assertNull(strategy.extractFieldType(param, "Wrong Name"));
        }

        @Test
        @DisplayName("Should return null when type is not Identifier")
        void shouldReturnNullForWrongType() {
            Parameter param = getParameter("wrongTypeParam");
            assertNull(strategy.extractFieldType(param, "Space ID"));
        }

        @Test
        @DisplayName("Should return 'Identifier' for Page ID field")
        void shouldReturnIdentifierForPageId() {
            Parameter param = getParameter("pageIdParam");
            assertEquals("Identifier", strategy.extractFieldType(param, "Page ID"));
        }
    }

    // ── mapToValidationResource tests ──

    @Nested
    @DisplayName("mapToValidationResource()")
    class MapToValidationResourceTests {

        @Test
        @DisplayName("Should return SPACE_ID for 'Space ID'")
        void shouldReturnSpaceIdForSpaceID() {
            Parameter param = getParameter("identifierParam");
            assertEquals(Validator.ValidationResource.SPACE_ID,
                    strategy.mapToValidationResource(param, "Space ID"));
        }

        @Test
        @DisplayName("Should return SPACE_ID for 'Space Id'")
        void shouldReturnSpaceIdForSpaceIdAlt() {
            Parameter param = getParameter("spaceIdAltParam");
            assertEquals(Validator.ValidationResource.SPACE_ID,
                    strategy.mapToValidationResource(param, "Space Id"));
        }

        @Test
        @DisplayName("Should return SPACE_ID for 'Space Id or Key'")
        void shouldReturnSpaceIdForSpaceIdOrKey() {
            Parameter param = getParameter("spaceIdOrKeyParam");
            assertEquals(Validator.ValidationResource.SPACE_ID,
                    strategy.mapToValidationResource(param, "Space Id or Key"));
        }

        @Test
        @DisplayName("Should return SPACE_KEY for 'Space Key'")
        void shouldReturnSpaceKeyForSpaceKey() {
            Parameter param = getParameter("spaceKeyParam");
            assertEquals(Validator.ValidationResource.SPACE_KEY,
                    strategy.mapToValidationResource(param, "Space Key"));
        }

        @Test
        @DisplayName("Should return SPACE_NAME for 'Space Name'")
        void shouldReturnSpaceNameForSpaceName() {
            Parameter param = getParameter("spaceNameParam");
            assertEquals(Validator.ValidationResource.SPACE_NAME,
                    strategy.mapToValidationResource(param, "Space Name"));
        }

        @Test
        @DisplayName("Should return PAGE_ID for 'Page ID'")
        void shouldReturnPageIdForPageID() {
            Parameter param = getParameter("pageIdParam");
            assertEquals(Validator.ValidationResource.PAGE_ID,
                    strategy.mapToValidationResource(param, "Page ID"));
        }

        @Test
        @DisplayName("Should return PAGE_ID for 'Page Id'")
        void shouldReturnPageIdForPageIdAlt() {
            Parameter param = getParameter("pageIdAltParam");
            assertEquals(Validator.ValidationResource.PAGE_ID,
                    strategy.mapToValidationResource(param, "Page Id"));
        }

        @Test
        @DisplayName("Should return PAGE_ID for 'PageId'")
        void shouldReturnPageIdForPageIdCamel() {
            Parameter param = getParameter("pageIdCamelParam");
            assertEquals(Validator.ValidationResource.PAGE_ID,
                    strategy.mapToValidationResource(param, "PageId"));
        }

        @Test
        @DisplayName("Should return TEMPLATE_ID for 'Template ID'")
        void shouldReturnTemplateIdForTemplateID() {
            Parameter param = getParameter("templateIdParam");
            assertEquals(Validator.ValidationResource.TEMPLATE_ID,
                    strategy.mapToValidationResource(param, "Template ID"));
        }

        @Test
        @DisplayName("Should return TEMPLATE_ID for 'Template Id'")
        void shouldReturnTemplateIdForTemplateIdAlt() {
            Parameter param = getParameter("templateIdAltParam");
            assertEquals(Validator.ValidationResource.TEMPLATE_ID,
                    strategy.mapToValidationResource(param, "Template Id"));
        }

        @Test
        @DisplayName("Should return TEMPLATE_ID for 'TemplateId'")
        void shouldReturnTemplateIdForTemplateIdCamel() {
            Parameter param = getParameter("templateIdCamelParam");
            assertEquals(Validator.ValidationResource.TEMPLATE_ID,
                    strategy.mapToValidationResource(param, "TemplateId"));
        }

        @Test
        @DisplayName("Should return null for unknown field name")
        void shouldReturnNullForUnknownFieldName() {
            Parameter param = getParameter("unknownIdentifier");
            assertNull(strategy.mapToValidationResource(param, "Unknown Field"));
        }

        @Test
        @DisplayName("Should return null when no annotation present")
        void shouldReturnNullForNoAnnotation() {
            Parameter param = getParameter("noAnnotationParam");
            assertNull(strategy.mapToValidationResource(param, "Space ID"));
        }

        @Test
        @DisplayName("Should return null when field name does not match")
        void shouldReturnNullForWrongName() {
            Parameter param = getParameter("identifierParam");
            assertNull(strategy.mapToValidationResource(param, "Wrong Name"));
        }

        @Test
        @DisplayName("Should return null when type is not Identifier")
        void shouldReturnNullForWrongType() {
            Parameter param = getParameter("wrongTypeParam");
            assertNull(strategy.mapToValidationResource(param, "Space ID"));
        }
    }

    // ── getAnnotationType and getStrategyName tests ──

    @Nested
    @DisplayName("Metadata methods")
    class MetadataTests {

        @Test
        @DisplayName("getAnnotationType should return Field.class")
        void getAnnotationTypeShouldReturnFieldClass() {
            Class<? extends Annotation> annotationType = strategy.getAnnotationType();
            assertEquals(Field.class, annotationType);
        }

        @Test
        @DisplayName("getStrategyName should return 'IdentifierField'")
        void getStrategyNameShouldReturnIdentifierField() {
            assertEquals("IdentifierField", strategy.getStrategyName());
        }
    }
}
