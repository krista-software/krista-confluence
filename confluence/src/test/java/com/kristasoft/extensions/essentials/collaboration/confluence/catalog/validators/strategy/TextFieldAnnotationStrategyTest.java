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
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.validators.Validator.ValidationResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TextFieldAnnotationStrategy Tests")
class TextFieldAnnotationStrategyTest {

    private TextFieldAnnotationStrategy strategy;

    /**
     * Helper class with annotated method parameters for reflection-based testing.
     */
    @SuppressWarnings("unused")
    static class AnnotatedHelper {
        public void spaceKeyField(@Field.Text(name = "Space Key") String spaceKey) {}
        public void spaceNameField(@Field.Text(name = "Space Name") String spaceName) {}
        public void spaceIdField(@Field.Text(name = "Space Id") String spaceId) {}
        public void spaceIdUpperField(@Field.Text(name = "Space ID") String spaceId) {}
        public void pageIdField(@Field.Text(name = "Page ID") String pageId) {}
        public void pageIdCamelField(@Field.Text(name = "PageId") String pageId) {}
        public void pageIdMixedField(@Field.Text(name = "Page Id") String pageId) {}
        public void templateIdField(@Field.Text(name = "Template ID") String templateId) {}
        public void templateIdMixedField(@Field.Text(name = "Template Id") String templateId) {}
        public void templateIdCamelField(@Field.Text(name = "TemplateId") String templateId) {}
        public void genericTextField(@Field.Text(name = "Some Text") String someText) {}
        public void textWithCustomAttributes(
                @Field.Text(name = "Custom Text", attributes = {
                        @Attribute(name = "maxLength", value = "100"),
                        @Attribute(name = "placeholder", value = "Enter text")
                }) String customText) {}
        public void noAnnotation(String plain) {}
        public void dateField(@Field.Date(name = "Some Date") String date) {}
    }

    @BeforeEach
    void setUp() {
        strategy = new TextFieldAnnotationStrategy();
    }

    private Parameter getParameter(String methodName) throws Exception {
        for (Method method : AnnotatedHelper.class.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                return method.getParameters()[0];
            }
        }
        throw new IllegalArgumentException("Method not found: " + methodName);
    }

    @Nested
    @DisplayName("canHandle()")
    class CanHandleTests {

        @Test
        @DisplayName("Should return true when parameter has @Field.Text with matching name")
        void shouldReturnTrueForMatchingTextAnnotation() throws Exception {
            Parameter param = getParameter("spaceKeyField");
            assertTrue(strategy.canHandle(param, "Space Key"));
        }

        @Test
        @DisplayName("Should return false when parameter has @Field.Text with non-matching name")
        void shouldReturnFalseForNonMatchingName() throws Exception {
            Parameter param = getParameter("spaceKeyField");
            assertFalse(strategy.canHandle(param, "Wrong Name"));
        }

        @Test
        @DisplayName("Should return false when parameter has no annotation")
        void shouldReturnFalseForNoAnnotation() throws Exception {
            Parameter param = getParameter("noAnnotation");
            assertFalse(strategy.canHandle(param, "Space Key"));
        }

        @Test
        @DisplayName("Should return false when parameter has a different annotation type")
        void shouldReturnFalseForDifferentAnnotationType() throws Exception {
            Parameter param = getParameter("dateField");
            assertFalse(strategy.canHandle(param, "Some Date"));
        }
    }

    @Nested
    @DisplayName("extractAttributes()")
    class ExtractAttributesTests {

        @Test
        @DisplayName("Should return empty map for text field with no custom attributes")
        void shouldReturnEmptyMapForNoAttributes() throws Exception {
            Parameter param = getParameter("spaceKeyField");
            Map<String, Object> attributes = strategy.extractAttributes(param, "Space Key");
            assertNotNull(attributes);
            assertTrue(attributes.isEmpty());
        }

        @Test
        @DisplayName("Should extract custom attributes from @Field.Text annotation")
        void shouldExtractCustomAttributes() throws Exception {
            Parameter param = getParameter("textWithCustomAttributes");
            Map<String, Object> attributes = strategy.extractAttributes(param, "Custom Text");

            assertNotNull(attributes);
            assertEquals(2, attributes.size());
            assertEquals("100", attributes.get("maxLength"));
            assertEquals("Enter text", attributes.get("placeholder"));
        }

        @Test
        @DisplayName("Should return empty map when field name does not match")
        void shouldReturnEmptyMapForNonMatchingName() throws Exception {
            Parameter param = getParameter("spaceKeyField");
            Map<String, Object> attributes = strategy.extractAttributes(param, "Wrong Name");
            assertNotNull(attributes);
            assertTrue(attributes.isEmpty());
        }

        @Test
        @DisplayName("Should return empty map when parameter has no @Field.Text annotation")
        void shouldReturnEmptyMapForNoAnnotation() throws Exception {
            Parameter param = getParameter("noAnnotation");
            Map<String, Object> attributes = strategy.extractAttributes(param, "Space Key");
            assertNotNull(attributes);
            assertTrue(attributes.isEmpty());
        }
    }

    @Nested
    @DisplayName("extractFieldType()")
    class ExtractFieldTypeTests {

        @Test
        @DisplayName("Should return 'Text' for matching @Field.Text annotation")
        void shouldReturnTextType() throws Exception {
            Parameter param = getParameter("spaceKeyField");
            assertEquals("Text", strategy.extractFieldType(param, "Space Key"));
        }

        @Test
        @DisplayName("Should return null when field name does not match")
        void shouldReturnNullForNonMatchingName() throws Exception {
            Parameter param = getParameter("spaceKeyField");
            assertNull(strategy.extractFieldType(param, "Wrong Name"));
        }

        @Test
        @DisplayName("Should return null when parameter has no @Field.Text annotation")
        void shouldReturnNullForNoAnnotation() throws Exception {
            Parameter param = getParameter("noAnnotation");
            assertNull(strategy.extractFieldType(param, "Space Key"));
        }
    }

    @Nested
    @DisplayName("mapToValidationResource()")
    class MapToValidationResourceTests {

        @Test
        @DisplayName("Should map 'Space Key' to SPACE_KEY")
        void shouldMapSpaceKeyToSpaceKey() throws Exception {
            Parameter param = getParameter("spaceKeyField");
            assertEquals(ValidationResource.SPACE_KEY, strategy.mapToValidationResource(param, "Space Key"));
        }

        @Test
        @DisplayName("Should map 'Space Name' to SPACE_NAME")
        void shouldMapSpaceNameToSpaceName() throws Exception {
            Parameter param = getParameter("spaceNameField");
            assertEquals(ValidationResource.SPACE_NAME, strategy.mapToValidationResource(param, "Space Name"));
        }

        @ParameterizedTest
        @MethodSource("com.kristasoft.extensions.essentials.collaboration.confluence.catalog.validators.strategy.TextFieldAnnotationStrategyTest#provideSpaceIdMappings")
        @DisplayName("Should map Space Id variants to SPACE_ID")
        void shouldMapSpaceIdVariantsToSpaceId(String methodName, String fieldName) throws Exception {
            Parameter param = getParameter(methodName);
            assertEquals(ValidationResource.SPACE_ID, strategy.mapToValidationResource(param, fieldName));
        }

        @ParameterizedTest
        @MethodSource("com.kristasoft.extensions.essentials.collaboration.confluence.catalog.validators.strategy.TextFieldAnnotationStrategyTest#providePageIdMappings")
        @DisplayName("Should map Page ID variants to PAGE_ID")
        void shouldMapPageIdVariantsToPageId(String methodName, String fieldName) throws Exception {
            Parameter param = getParameter(methodName);
            assertEquals(ValidationResource.PAGE_ID, strategy.mapToValidationResource(param, fieldName));
        }

        @ParameterizedTest
        @MethodSource("com.kristasoft.extensions.essentials.collaboration.confluence.catalog.validators.strategy.TextFieldAnnotationStrategyTest#provideTemplateIdMappings")
        @DisplayName("Should map Template ID variants to TEMPLATE_ID")
        void shouldMapTemplateIdVariantsToTemplateId(String methodName, String fieldName) throws Exception {
            Parameter param = getParameter(methodName);
            assertEquals(ValidationResource.TEMPLATE_ID, strategy.mapToValidationResource(param, fieldName));
        }

        @Test
        @DisplayName("Should return null for unrecognized text field names")
        void shouldReturnNullForUnrecognizedFieldName() throws Exception {
            Parameter param = getParameter("genericTextField");
            assertNull(strategy.mapToValidationResource(param, "Some Text"));
        }

        @Test
        @DisplayName("Should return null when field name does not match annotation")
        void shouldReturnNullForNonMatchingName() throws Exception {
            Parameter param = getParameter("spaceKeyField");
            assertNull(strategy.mapToValidationResource(param, "Wrong Name"));
        }

        @Test
        @DisplayName("Should return null when parameter has no @Field.Text annotation")
        void shouldReturnNullForNoAnnotation() throws Exception {
            Parameter param = getParameter("noAnnotation");
            assertNull(strategy.mapToValidationResource(param, "Space Key"));
        }
    }

    static Stream<Arguments> provideSpaceIdMappings() {
        return Stream.of(
                Arguments.of("spaceIdField", "Space Id"),
                Arguments.of("spaceIdUpperField", "Space ID")
        );
    }

    static Stream<Arguments> providePageIdMappings() {
        return Stream.of(
                Arguments.of("pageIdField", "Page ID"),
                Arguments.of("pageIdCamelField", "PageId"),
                Arguments.of("pageIdMixedField", "Page Id")
        );
    }

    static Stream<Arguments> provideTemplateIdMappings() {
        return Stream.of(
                Arguments.of("templateIdField", "Template ID"),
                Arguments.of("templateIdMixedField", "Template Id"),
                Arguments.of("templateIdCamelField", "TemplateId")
        );
    }

    @Nested
    @DisplayName("getAnnotationType()")
    class GetAnnotationTypeTests {

        @Test
        @DisplayName("Should return Field.Text.class")
        void shouldReturnFieldTextClass() {
            assertEquals(Field.Text.class, strategy.getAnnotationType());
        }
    }

    @Nested
    @DisplayName("getStrategyName()")
    class GetStrategyNameTests {

        @Test
        @DisplayName("Should return 'TextField'")
        void shouldReturnTextField() {
            assertEquals("TextField", strategy.getStrategyName());
        }
    }
}
