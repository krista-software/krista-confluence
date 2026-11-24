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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DescFieldAnnotationStrategy Tests")
class DescFieldAnnotationStrategyTest {

    private DescFieldAnnotationStrategy strategy;

    /**
     * Helper class with annotated method parameters for reflection-based testing.
     */
    @SuppressWarnings("unused")
    static class AnnotatedHelper {
        public void attendeesField(@Field.Desc(name = "Attendees", type = "UserList") String attendees) {}
        public void spaceField(@Field.Desc(name = "Space", type = "SpaceType") String space) {}
        public void spacesField(@Field.Desc(name = "Spaces", type = "SpaceList") String spaces) {}
        public void spaceIdentifiersField(@Field.Desc(name = "Space Identifiers", type = "SpaceIds") String spaceIds) {}
        public void genericDescField(@Field.Desc(name = "Description", type = "TextArea") String description) {}
        public void descWithType(@Field.Desc(name = "Typed Desc", type = "CustomType") String typedDesc) {}
        public void noAnnotation(String plain) {}
        public void textField(@Field.Text(name = "Some Text") String text) {}
    }

    @BeforeEach
    void setUp() {
        strategy = new DescFieldAnnotationStrategy();
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
        @DisplayName("Should return true when parameter has @Field.Desc with matching name")
        void shouldReturnTrueForMatchingDescAnnotation() throws Exception {
            Parameter param = getParameter("attendeesField");
            assertTrue(strategy.canHandle(param, "Attendees"));
        }

        @Test
        @DisplayName("Should return false when parameter has @Field.Desc with non-matching name")
        void shouldReturnFalseForNonMatchingName() throws Exception {
            Parameter param = getParameter("attendeesField");
            assertFalse(strategy.canHandle(param, "Wrong Name"));
        }

        @Test
        @DisplayName("Should return false when parameter has no annotation")
        void shouldReturnFalseForNoAnnotation() throws Exception {
            Parameter param = getParameter("noAnnotation");
            assertFalse(strategy.canHandle(param, "Attendees"));
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
        @DisplayName("Should always return an empty map for @Field.Desc annotations")
        void shouldReturnEmptyMapForDescAnnotation() throws Exception {
            Parameter param = getParameter("attendeesField");
            Map<String, Object> attributes = strategy.extractAttributes(param, "Attendees");
            assertNotNull(attributes);
            assertTrue(attributes.isEmpty());
        }

        @Test
        @DisplayName("Should return empty map when field name does not match")
        void shouldReturnEmptyMapForNonMatchingName() throws Exception {
            Parameter param = getParameter("attendeesField");
            Map<String, Object> attributes = strategy.extractAttributes(param, "Wrong Name");
            assertNotNull(attributes);
            assertTrue(attributes.isEmpty());
        }

        @Test
        @DisplayName("Should return empty map when parameter has no @Field.Desc annotation")
        void shouldReturnEmptyMapForNoAnnotation() throws Exception {
            Parameter param = getParameter("noAnnotation");
            Map<String, Object> attributes = strategy.extractAttributes(param, "Attendees");
            assertNotNull(attributes);
            assertTrue(attributes.isEmpty());
        }

        @Test
        @DisplayName("Should return empty map even for Desc fields with a type value")
        void shouldReturnEmptyMapEvenWithType() throws Exception {
            Parameter param = getParameter("descWithType");
            Map<String, Object> attributes = strategy.extractAttributes(param, "Typed Desc");
            assertNotNull(attributes);
            assertTrue(attributes.isEmpty());
        }
    }

    @Nested
    @DisplayName("extractFieldType()")
    class ExtractFieldTypeTests {

        @Test
        @DisplayName("Should return the annotation's type() value")
        void shouldReturnAnnotationType() throws Exception {
            Parameter param = getParameter("attendeesField");
            assertEquals("UserList", strategy.extractFieldType(param, "Attendees"));
        }

        @Test
        @DisplayName("Should return the correct type for different Desc annotations")
        void shouldReturnCorrectTypeForDifferentAnnotations() throws Exception {
            Parameter param = getParameter("descWithType");
            assertEquals("CustomType", strategy.extractFieldType(param, "Typed Desc"));
        }

        @Test
        @DisplayName("Should return null when field name does not match")
        void shouldReturnNullForNonMatchingName() throws Exception {
            Parameter param = getParameter("attendeesField");
            assertNull(strategy.extractFieldType(param, "Wrong Name"));
        }

        @Test
        @DisplayName("Should return null when parameter has no @Field.Desc annotation")
        void shouldReturnNullForNoAnnotation() throws Exception {
            Parameter param = getParameter("noAnnotation");
            assertNull(strategy.extractFieldType(param, "Attendees"));
        }
    }

    @Nested
    @DisplayName("mapToValidationResource()")
    class MapToValidationResourceTests {

        @Test
        @DisplayName("Should map 'Attendees' to ATTENDEES")
        void shouldMapAttendeesToAttendees() throws Exception {
            Parameter param = getParameter("attendeesField");
            assertEquals(ValidationResource.ATTENDEES, strategy.mapToValidationResource(param, "Attendees"));
        }

        @ParameterizedTest
        @MethodSource("com.kristasoft.extensions.essentials.collaboration.confluence.catalog.validators.strategy.DescFieldAnnotationStrategyTest#provideSpaceIdMappings")
        @DisplayName("Should map space-related field names to SPACE_ID")
        void shouldMapSpaceFieldNamesToSpaceId(String methodName, String fieldName) throws Exception {
            Parameter param = getParameter(methodName);
            assertEquals(ValidationResource.SPACE_ID, strategy.mapToValidationResource(param, fieldName));
        }

        @Test
        @DisplayName("Should return null for unrecognized desc field names")
        void shouldReturnNullForUnrecognizedFieldName() throws Exception {
            Parameter param = getParameter("genericDescField");
            assertNull(strategy.mapToValidationResource(param, "Description"));
        }

        @Test
        @DisplayName("Should return null when field name does not match annotation")
        void shouldReturnNullForNonMatchingName() throws Exception {
            Parameter param = getParameter("attendeesField");
            assertNull(strategy.mapToValidationResource(param, "Wrong Name"));
        }

        @Test
        @DisplayName("Should return null when parameter has no @Field.Desc annotation")
        void shouldReturnNullForNoAnnotation() throws Exception {
            Parameter param = getParameter("noAnnotation");
            assertNull(strategy.mapToValidationResource(param, "Attendees"));
        }
    }

    static Stream<Arguments> provideSpaceIdMappings() {
        return Stream.of(
                Arguments.of("spaceField", "Space"),
                Arguments.of("spacesField", "Spaces"),
                Arguments.of("spaceIdentifiersField", "Space Identifiers")
        );
    }

    @Nested
    @DisplayName("getAnnotationType()")
    class GetAnnotationTypeTests {

        @Test
        @DisplayName("Should return Field.Desc.class")
        void shouldReturnFieldDescClass() {
            assertEquals(Field.Desc.class, strategy.getAnnotationType());
        }
    }

    @Nested
    @DisplayName("getStrategyName()")
    class GetStrategyNameTests {

        @Test
        @DisplayName("Should return 'DescField'")
        void shouldReturnDescField() {
            assertEquals("DescField", strategy.getStrategyName());
        }
    }
}
