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

@DisplayName("DateFieldAnnotationStrategy Tests")
class DateFieldAnnotationStrategyTest {

    private DateFieldAnnotationStrategy strategy;

    /**
     * Helper class with annotated method parameters for reflection-based testing.
     */
    @SuppressWarnings("unused")
    static class AnnotatedHelper {
        public void dateFromField(@Field.Date(name = "Date From") String dateFrom) {}
        public void fromField(@Field.Date(name = "From") String from) {}
        public void dateToField(@Field.Date(name = "Date To") String dateTo) {}
        public void dateToLowerField(@Field.Date(name = "Date to") String dateTo) {}
        public void toField(@Field.Date(name = "To") String to) {}
        public void genericDateField(@Field.Date(name = "Some Date") String someDate) {}
        public void dateWithAllAttributes(
                @Field.Date(
                        name = "Full Date",
                        includeTimeOfDay = true,
                        showHowManyDaysInViewer = 7,
                        allowPast = false,
                        allowToday = false,
                        allowFuture = false,
                        defaultTimeSpan = 30
                ) String fullDate) {}
        public void dateWithDefaults(@Field.Date(name = "Default Date") String defaultDate) {}
        public void dateWithCustomAttributes(
                @Field.Date(name = "Custom Date", attributes = {
                        @Attribute(name = "format", value = "yyyy-MM-dd"),
                        @Attribute(name = "timezone", value = "UTC")
                }) String customDate) {}
        public void noAnnotation(String plain) {}
        public void textField(@Field.Text(name = "Some Text") String text) {}
    }

    @BeforeEach
    void setUp() {
        strategy = new DateFieldAnnotationStrategy();
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
        @DisplayName("Should return true when parameter has @Field.Date with matching name")
        void shouldReturnTrueForMatchingDateAnnotation() throws Exception {
            Parameter param = getParameter("dateFromField");
            assertTrue(strategy.canHandle(param, "Date From"));
        }

        @Test
        @DisplayName("Should return false when parameter has @Field.Date with non-matching name")
        void shouldReturnFalseForNonMatchingName() throws Exception {
            Parameter param = getParameter("dateFromField");
            assertFalse(strategy.canHandle(param, "Wrong Name"));
        }

        @Test
        @DisplayName("Should return false when parameter has no annotation")
        void shouldReturnFalseForNoAnnotation() throws Exception {
            Parameter param = getParameter("noAnnotation");
            assertFalse(strategy.canHandle(param, "Date From"));
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
        @DisplayName("Should extract all date-specific attributes when they differ from defaults")
        void shouldExtractAllDateSpecificAttributes() throws Exception {
            Parameter param = getParameter("dateWithAllAttributes");
            Map<String, Object> attributes = strategy.extractAttributes(param, "Full Date");

            assertNotNull(attributes);
            assertEquals("true", attributes.get("includeTimeOfDay"));
            assertEquals("7", attributes.get("showHowManyDaysInViewer"));
            assertEquals("false", attributes.get("allowPast"));
            assertEquals("false", attributes.get("allowToday"));
            assertEquals("false", attributes.get("allowFuture"));
            assertEquals("30", attributes.get("defaultTimeSpan"));
        }

        @Test
        @DisplayName("Should include date attributes that differ from thresholds for default annotation values")
        void shouldIncludeAttributesThatDifferFromThresholds() throws Exception {
            Parameter param = getParameter("dateWithDefaults");
            Map<String, Object> attributes = strategy.extractAttributes(param, "Default Date");
            assertNotNull(attributes);
            // Annotation defaults: includeTimeOfDay=true, showHowManyDaysInViewer=1,
            // allowPast=true, allowToday=true, allowFuture=true, defaultTimeSpan=1
            // includeTimeOfDay=true triggers: attributes.put("includeTimeOfDay", "true")
            // showHowManyDaysInViewer=1 (>0) triggers: attributes.put("showHowManyDaysInViewer", "1")
            // allowPast=true (!true=false) -> NOT added
            // allowToday=true (!true=false) -> NOT added
            // allowFuture=true (!true=false) -> NOT added
            // defaultTimeSpan=1 (>0) triggers: attributes.put("defaultTimeSpan", "1")
            assertEquals("true", attributes.get("includeTimeOfDay"));
            assertEquals("1", attributes.get("showHowManyDaysInViewer"));
            assertEquals("1", attributes.get("defaultTimeSpan"));
            assertFalse(attributes.containsKey("allowPast"));
            assertFalse(attributes.containsKey("allowToday"));
            assertFalse(attributes.containsKey("allowFuture"));
        }

        @Test
        @DisplayName("Should extract custom attributes from the attributes array")
        void shouldExtractCustomAttributes() throws Exception {
            Parameter param = getParameter("dateWithCustomAttributes");
            Map<String, Object> attributes = strategy.extractAttributes(param, "Custom Date");

            assertNotNull(attributes);
            assertEquals("yyyy-MM-dd", attributes.get("format"));
            assertEquals("UTC", attributes.get("timezone"));
        }

        @Test
        @DisplayName("Should return empty map when field name does not match")
        void shouldReturnEmptyMapForNonMatchingName() throws Exception {
            Parameter param = getParameter("dateFromField");
            Map<String, Object> attributes = strategy.extractAttributes(param, "Wrong Name");
            assertNotNull(attributes);
            assertTrue(attributes.isEmpty());
        }

        @Test
        @DisplayName("Should return empty map when parameter has no @Field.Date annotation")
        void shouldReturnEmptyMapForNoAnnotation() throws Exception {
            Parameter param = getParameter("noAnnotation");
            Map<String, Object> attributes = strategy.extractAttributes(param, "Date From");
            assertNotNull(attributes);
            assertTrue(attributes.isEmpty());
        }
    }

    @Nested
    @DisplayName("extractFieldType()")
    class ExtractFieldTypeTests {

        @Test
        @DisplayName("Should return 'Date' for matching @Field.Date annotation")
        void shouldReturnDateType() throws Exception {
            Parameter param = getParameter("dateFromField");
            assertEquals("Date", strategy.extractFieldType(param, "Date From"));
        }

        @Test
        @DisplayName("Should return null when field name does not match")
        void shouldReturnNullForNonMatchingName() throws Exception {
            Parameter param = getParameter("dateFromField");
            assertNull(strategy.extractFieldType(param, "Wrong Name"));
        }

        @Test
        @DisplayName("Should return null when parameter has no @Field.Date annotation")
        void shouldReturnNullForNoAnnotation() throws Exception {
            Parameter param = getParameter("noAnnotation");
            assertNull(strategy.extractFieldType(param, "Date From"));
        }
    }

    @Nested
    @DisplayName("mapToValidationResource()")
    class MapToValidationResourceTests {

        @ParameterizedTest
        @MethodSource("com.kristasoft.extensions.essentials.collaboration.confluence.catalog.validators.strategy.DateFieldAnnotationStrategyTest#provideFromDateMappings")
        @DisplayName("Should map FROM_DATE field names correctly")
        void shouldMapToFromDate(String methodName, String fieldName) throws Exception {
            Parameter param = getParameter(methodName);
            assertEquals(ValidationResource.FROM_DATE, strategy.mapToValidationResource(param, fieldName));
        }

        @ParameterizedTest
        @MethodSource("com.kristasoft.extensions.essentials.collaboration.confluence.catalog.validators.strategy.DateFieldAnnotationStrategyTest#provideToDateMappings")
        @DisplayName("Should map TO_DATE field names correctly")
        void shouldMapToToDate(String methodName, String fieldName) throws Exception {
            Parameter param = getParameter(methodName);
            assertEquals(ValidationResource.TO_DATE, strategy.mapToValidationResource(param, fieldName));
        }

        @Test
        @DisplayName("Should return null for unrecognized date field names")
        void shouldReturnNullForUnrecognizedFieldName() throws Exception {
            Parameter param = getParameter("genericDateField");
            assertNull(strategy.mapToValidationResource(param, "Some Date"));
        }

        @Test
        @DisplayName("Should return null when field name does not match annotation")
        void shouldReturnNullForNonMatchingName() throws Exception {
            Parameter param = getParameter("dateFromField");
            assertNull(strategy.mapToValidationResource(param, "Wrong Name"));
        }

        @Test
        @DisplayName("Should return null when parameter has no @Field.Date annotation")
        void shouldReturnNullForNoAnnotation() throws Exception {
            Parameter param = getParameter("noAnnotation");
            assertNull(strategy.mapToValidationResource(param, "Date From"));
        }
    }

    static Stream<Arguments> provideFromDateMappings() {
        return Stream.of(
                Arguments.of("dateFromField", "Date From"),
                Arguments.of("fromField", "From")
        );
    }

    static Stream<Arguments> provideToDateMappings() {
        return Stream.of(
                Arguments.of("dateToField", "Date To"),
                Arguments.of("dateToLowerField", "Date to"),
                Arguments.of("toField", "To")
        );
    }

    @Nested
    @DisplayName("getAnnotationType()")
    class GetAnnotationTypeTests {

        @Test
        @DisplayName("Should return Field.Date.class")
        void shouldReturnFieldDateClass() {
            assertEquals(Field.Date.class, strategy.getAnnotationType());
        }
    }

    @Nested
    @DisplayName("getStrategyName()")
    class GetStrategyNameTests {

        @Test
        @DisplayName("Should return 'DateField'")
        void shouldReturnDateField() {
            assertEquals("DateField", strategy.getStrategyName());
        }
    }
}
