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

import app.krista.extension.impl.anno.Field;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.validators.Validator.ValidationResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BooleanFieldAnnotationStrategy Tests")
class BooleanFieldAnnotationStrategyTest {

    private BooleanFieldAnnotationStrategy strategy;

    /**
     * Helper class with annotated method parameters for reflection-based testing.
     */
    @SuppressWarnings("unused")
    static class AnnotatedHelper {
        public void booleanField(@Field.Boolean(name = "Is Active") String isActive) {}
        public void booleanFieldWithAttributes(
                @Field.Boolean(name = "Enabled", attributes = {
                        @app.krista.extension.impl.anno.Attribute(name = "defaultValue", value = "true"),
                        @app.krista.extension.impl.anno.Attribute(name = "displayAs", value = "toggle")
                }) String enabled) {}
        public void noAnnotation(String plain) {}
        public void textField(@Field.Text(name = "Some Text") String text) {}
    }

    @BeforeEach
    void setUp() {
        strategy = new BooleanFieldAnnotationStrategy();
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
        @DisplayName("Should return true when parameter has @Field.Boolean with matching name")
        void shouldReturnTrueForMatchingBooleanAnnotation() throws Exception {
            Parameter param = getParameter("booleanField");
            assertTrue(strategy.canHandle(param, "Is Active"));
        }

        @Test
        @DisplayName("Should return false when parameter has @Field.Boolean with non-matching name")
        void shouldReturnFalseForNonMatchingName() throws Exception {
            Parameter param = getParameter("booleanField");
            assertFalse(strategy.canHandle(param, "Wrong Name"));
        }

        @Test
        @DisplayName("Should return false when parameter has no annotation")
        void shouldReturnFalseForNoAnnotation() throws Exception {
            Parameter param = getParameter("noAnnotation");
            assertFalse(strategy.canHandle(param, "Is Active"));
        }

        @Test
        @DisplayName("Should return false when parameter has a different annotation type")
        void shouldReturnFalseForDifferentAnnotationType() throws Exception {
            Parameter param = getParameter("textField");
            assertFalse(strategy.canHandle(param, "Some Text"));
        }
    }

    @Nested
    @DisplayName("extractAttributes()")
    class ExtractAttributesTests {

        @Test
        @DisplayName("Should return empty map when no custom attributes are defined")
        void shouldReturnEmptyMapForNoAttributes() throws Exception {
            Parameter param = getParameter("booleanField");
            Map<String, Object> attributes = strategy.extractAttributes(param, "Is Active");
            assertNotNull(attributes);
            assertTrue(attributes.isEmpty());
        }

        @Test
        @DisplayName("Should extract custom attributes from @Field.Boolean annotation")
        void shouldExtractCustomAttributes() throws Exception {
            Parameter param = getParameter("booleanFieldWithAttributes");
            Map<String, Object> attributes = strategy.extractAttributes(param, "Enabled");
            assertNotNull(attributes);
            assertEquals(2, attributes.size());
            assertEquals("true", attributes.get("defaultValue"));
            assertEquals("toggle", attributes.get("displayAs"));
        }

        @Test
        @DisplayName("Should return empty map when field name does not match")
        void shouldReturnEmptyMapForNonMatchingName() throws Exception {
            Parameter param = getParameter("booleanField");
            Map<String, Object> attributes = strategy.extractAttributes(param, "Wrong Name");
            assertNotNull(attributes);
            assertTrue(attributes.isEmpty());
        }

        @Test
        @DisplayName("Should return empty map when parameter has no @Field.Boolean annotation")
        void shouldReturnEmptyMapForNoAnnotation() throws Exception {
            Parameter param = getParameter("noAnnotation");
            Map<String, Object> attributes = strategy.extractAttributes(param, "Is Active");
            assertNotNull(attributes);
            assertTrue(attributes.isEmpty());
        }
    }

    @Nested
    @DisplayName("extractFieldType()")
    class ExtractFieldTypeTests {

        @Test
        @DisplayName("Should return 'Boolean' for matching @Field.Boolean annotation")
        void shouldReturnBooleanType() throws Exception {
            Parameter param = getParameter("booleanField");
            assertEquals("Boolean", strategy.extractFieldType(param, "Is Active"));
        }

        @Test
        @DisplayName("Should return null when field name does not match")
        void shouldReturnNullForNonMatchingName() throws Exception {
            Parameter param = getParameter("booleanField");
            assertNull(strategy.extractFieldType(param, "Wrong Name"));
        }

        @Test
        @DisplayName("Should return null when parameter has no @Field.Boolean annotation")
        void shouldReturnNullForNoAnnotation() throws Exception {
            Parameter param = getParameter("noAnnotation");
            assertNull(strategy.extractFieldType(param, "Is Active"));
        }
    }

    @Nested
    @DisplayName("mapToValidationResource()")
    class MapToValidationResourceTests {

        @Test
        @DisplayName("Should return null for boolean fields (no specific mapping exists)")
        void shouldReturnNullForBooleanField() throws Exception {
            Parameter param = getParameter("booleanField");
            assertNull(strategy.mapToValidationResource(param, "Is Active"));
        }

        @Test
        @DisplayName("Should return null when field name does not match")
        void shouldReturnNullForNonMatchingName() throws Exception {
            Parameter param = getParameter("booleanField");
            assertNull(strategy.mapToValidationResource(param, "Wrong Name"));
        }

        @Test
        @DisplayName("Should return null when parameter has no @Field.Boolean annotation")
        void shouldReturnNullForNoAnnotation() throws Exception {
            Parameter param = getParameter("noAnnotation");
            assertNull(strategy.mapToValidationResource(param, "Is Active"));
        }
    }

    @Nested
    @DisplayName("getAnnotationType()")
    class GetAnnotationTypeTests {

        @Test
        @DisplayName("Should return Field.Boolean.class")
        void shouldReturnFieldBooleanClass() {
            assertEquals(Field.Boolean.class, strategy.getAnnotationType());
        }
    }

    @Nested
    @DisplayName("getStrategyName()")
    class GetStrategyNameTests {

        @Test
        @DisplayName("Should return 'BooleanField'")
        void shouldReturnBooleanField() {
            assertEquals("BooleanField", strategy.getStrategyName());
        }
    }
}
