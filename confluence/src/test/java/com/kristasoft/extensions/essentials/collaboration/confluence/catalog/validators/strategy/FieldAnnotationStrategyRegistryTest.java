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

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FieldAnnotationStrategyRegistry Tests")
class FieldAnnotationStrategyRegistryTest {

    private FieldAnnotationStrategyRegistry registry;

    /**
     * Helper class with annotated method parameters for reflection-based testing.
     * Each method has one parameter annotated with a different @Field.* annotation type.
     */
    @SuppressWarnings("unused")
    static class AnnotatedHelper {
        public void booleanField(@Field.Boolean(name = "Is Active") String isActive) {}
        public void dateField(@Field.Date(name = "Date From") String dateFrom) {}
        public void textField(@Field.Text(name = "Space Key") String spaceKey) {}
        public void descField(@Field.Desc(name = "Attendees", type = "UserList") String attendees) {}
        public void fileField(@Field.File(name = "Attachments", multipleFileUpload = true) String attachments) {}
        public void basicField(@Field(name = "Event ID", type = "Text") String eventId) {}
        public void identifierField(@Field(name = "Space ID", type = "Identifier") String spaceId) {}
        public void noAnnotation(String plain) {}
        public void textFieldWithAttributes(
                @Field.Text(name = "Custom Text", attributes = {
                        @Attribute(name = "maxLength", value = "100")
                }) String customText) {}
    }

    @BeforeEach
    void setUp() {
        registry = new FieldAnnotationStrategyRegistry();
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
    @DisplayName("Constructor")
    class ConstructorTests {

        @Test
        @DisplayName("Should create registry with exactly 7 strategies")
        void shouldCreateRegistryWith7Strategies() {
            assertEquals(7, registry.getStrategyCount());
        }

        @Test
        @DisplayName("Should create non-null list of strategies")
        void shouldCreateNonNullStrategies() {
            List<FieldAnnotationStrategy> strategies = registry.getAllStrategies();
            assertNotNull(strategies);
            assertFalse(strategies.isEmpty());
        }

        @Test
        @DisplayName("Should contain all expected strategy types")
        void shouldContainAllExpectedStrategyTypes() {
            List<FieldAnnotationStrategy> strategies = registry.getAllStrategies();
            List<String> strategyNames = strategies.stream()
                    .map(FieldAnnotationStrategy::getStrategyName)
                    .toList();

            assertTrue(strategyNames.contains("BasicField"));
            assertTrue(strategyNames.contains("DateField"));
            assertTrue(strategyNames.contains("TextField"));
            assertTrue(strategyNames.contains("BooleanField"));
            assertTrue(strategyNames.contains("DescField"));
            assertTrue(strategyNames.contains("FileField"));
            assertTrue(strategyNames.contains("IdentifierField"));
        }
    }

    @Nested
    @DisplayName("findStrategy()")
    class FindStrategyTests {

        @Test
        @DisplayName("Should find BooleanFieldAnnotationStrategy for @Field.Boolean parameter")
        void shouldFindBooleanStrategy() throws Exception {
            Parameter param = getParameter("booleanField");
            Optional<FieldAnnotationStrategy> result = registry.findStrategy(param, "Is Active");

            assertTrue(result.isPresent());
            assertEquals("BooleanField", result.get().getStrategyName());
        }

        @Test
        @DisplayName("Should find DateFieldAnnotationStrategy for @Field.Date parameter")
        void shouldFindDateStrategy() throws Exception {
            Parameter param = getParameter("dateField");
            Optional<FieldAnnotationStrategy> result = registry.findStrategy(param, "Date From");

            assertTrue(result.isPresent());
            assertEquals("DateField", result.get().getStrategyName());
        }

        @Test
        @DisplayName("Should find TextFieldAnnotationStrategy for @Field.Text parameter")
        void shouldFindTextStrategy() throws Exception {
            Parameter param = getParameter("textField");
            Optional<FieldAnnotationStrategy> result = registry.findStrategy(param, "Space Key");

            assertTrue(result.isPresent());
            assertEquals("TextField", result.get().getStrategyName());
        }

        @Test
        @DisplayName("Should find DescFieldAnnotationStrategy for @Field.Desc parameter")
        void shouldFindDescStrategy() throws Exception {
            Parameter param = getParameter("descField");
            Optional<FieldAnnotationStrategy> result = registry.findStrategy(param, "Attendees");

            assertTrue(result.isPresent());
            assertEquals("DescField", result.get().getStrategyName());
        }

        @Test
        @DisplayName("Should find FileFieldAnnotationStrategy for @Field.File parameter")
        void shouldFindFileStrategy() throws Exception {
            Parameter param = getParameter("fileField");
            Optional<FieldAnnotationStrategy> result = registry.findStrategy(param, "Attachments");

            assertTrue(result.isPresent());
            assertEquals("FileField", result.get().getStrategyName());
        }

        @Test
        @DisplayName("Should find a matching strategy for @Field with type='Identifier'")
        void shouldFindStrategyForIdentifierField() throws Exception {
            Parameter param = getParameter("identifierField");
            Optional<FieldAnnotationStrategy> result = registry.findStrategy(param, "Space ID");

            assertTrue(result.isPresent());
            // BasicFieldAnnotationStrategy is registered before IdentifierFieldAnnotationStrategy
            // and it matches any @Field annotation regardless of type, so it wins
            assertEquals("BasicField", result.get().getStrategyName());
        }

        @Test
        @DisplayName("Should find BasicFieldAnnotationStrategy for plain @Field parameter")
        void shouldFindBasicStrategy() throws Exception {
            Parameter param = getParameter("basicField");
            Optional<FieldAnnotationStrategy> result = registry.findStrategy(param, "Event ID");

            assertTrue(result.isPresent());
            // BasicField or IdentifierField may match depending on type;
            // BasicField checks @Field with any type, IdentifierField requires type="Identifier"
            // basicField has type="Text", so BasicField should match
            assertEquals("BasicField", result.get().getStrategyName());
        }

        @Test
        @DisplayName("Should return empty Optional when no strategy matches")
        void shouldReturnEmptyForNoMatch() throws Exception {
            Parameter param = getParameter("noAnnotation");
            Optional<FieldAnnotationStrategy> result = registry.findStrategy(param, "Any Name");

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should return empty Optional when field name does not match any annotation")
        void shouldReturnEmptyForNonMatchingFieldName() throws Exception {
            Parameter param = getParameter("textField");
            Optional<FieldAnnotationStrategy> result = registry.findStrategy(param, "Non Existent Field");

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("extractAttributes()")
    class ExtractAttributesTests {

        @Test
        @DisplayName("Should delegate to the matching strategy for attribute extraction")
        void shouldDelegateToMatchingStrategy() throws Exception {
            Parameter param = getParameter("textFieldWithAttributes");
            Map<String, Object> attributes = registry.extractAttributes(param, "Custom Text");

            assertNotNull(attributes);
            assertEquals("100", attributes.get("maxLength"));
        }

        @Test
        @DisplayName("Should delegate to FileFieldAnnotationStrategy for file attributes")
        void shouldDelegateToFileStrategy() throws Exception {
            Parameter param = getParameter("fileField");
            Map<String, Object> attributes = registry.extractAttributes(param, "Attachments");

            assertNotNull(attributes);
            assertEquals("true", attributes.get("multipleFileUpload"));
        }

        @Test
        @DisplayName("Should return empty map when no strategy matches")
        void shouldReturnEmptyMapForNoMatch() throws Exception {
            Parameter param = getParameter("noAnnotation");
            Map<String, Object> attributes = registry.extractAttributes(param, "Any Name");

            assertNotNull(attributes);
            assertTrue(attributes.isEmpty());
        }

        @Test
        @DisplayName("Should return empty map when field name does not match")
        void shouldReturnEmptyMapForNonMatchingField() throws Exception {
            Parameter param = getParameter("textField");
            Map<String, Object> attributes = registry.extractAttributes(param, "Wrong Name");

            assertNotNull(attributes);
            assertTrue(attributes.isEmpty());
        }
    }

    @Nested
    @DisplayName("extractFieldType()")
    class ExtractFieldTypeTests {

        @Test
        @DisplayName("Should delegate to TextFieldAnnotationStrategy for field type extraction")
        void shouldDelegateToTextStrategy() throws Exception {
            Parameter param = getParameter("textField");
            String fieldType = registry.extractFieldType(param, "Space Key");

            assertEquals("Text", fieldType);
        }

        @Test
        @DisplayName("Should delegate to DateFieldAnnotationStrategy for field type extraction")
        void shouldDelegateToDateStrategy() throws Exception {
            Parameter param = getParameter("dateField");
            String fieldType = registry.extractFieldType(param, "Date From");

            assertEquals("Date", fieldType);
        }

        @Test
        @DisplayName("Should delegate to BooleanFieldAnnotationStrategy for field type extraction")
        void shouldDelegateToBooleanStrategy() throws Exception {
            Parameter param = getParameter("booleanField");
            String fieldType = registry.extractFieldType(param, "Is Active");

            assertEquals("Boolean", fieldType);
        }

        @Test
        @DisplayName("Should delegate to FileFieldAnnotationStrategy for field type extraction")
        void shouldDelegateToFileStrategy() throws Exception {
            Parameter param = getParameter("fileField");
            String fieldType = registry.extractFieldType(param, "Attachments");

            assertEquals("File", fieldType);
        }

        @Test
        @DisplayName("Should return null when no strategy matches")
        void shouldReturnNullForNoMatch() throws Exception {
            Parameter param = getParameter("noAnnotation");
            String fieldType = registry.extractFieldType(param, "Any Name");

            assertNull(fieldType);
        }
    }

    @Nested
    @DisplayName("mapToValidationResource()")
    class MapToValidationResourceTests {

        @Test
        @DisplayName("Should delegate to TextFieldAnnotationStrategy for SPACE_KEY mapping")
        void shouldDelegateForSpaceKeyMapping() throws Exception {
            Parameter param = getParameter("textField");
            ValidationResource resource = registry.mapToValidationResource(param, "Space Key");

            assertEquals(ValidationResource.SPACE_KEY, resource);
        }

        @Test
        @DisplayName("Should delegate to DateFieldAnnotationStrategy for FROM_DATE mapping")
        void shouldDelegateForFromDateMapping() throws Exception {
            Parameter param = getParameter("dateField");
            ValidationResource resource = registry.mapToValidationResource(param, "Date From");

            assertEquals(ValidationResource.FROM_DATE, resource);
        }

        @Test
        @DisplayName("Should delegate to DescFieldAnnotationStrategy for ATTENDEES mapping")
        void shouldDelegateForAttendeesMapping() throws Exception {
            Parameter param = getParameter("descField");
            ValidationResource resource = registry.mapToValidationResource(param, "Attendees");

            assertEquals(ValidationResource.ATTENDEES, resource);
        }

        @Test
        @DisplayName("Should delegate to FileFieldAnnotationStrategy for ATTACHMENTS mapping")
        void shouldDelegateForAttachmentsMapping() throws Exception {
            Parameter param = getParameter("fileField");
            ValidationResource resource = registry.mapToValidationResource(param, "Attachments");

            assertEquals(ValidationResource.ATTACHMENTS, resource);
        }

        @Test
        @DisplayName("Should return null when no strategy matches")
        void shouldReturnNullForNoMatch() throws Exception {
            Parameter param = getParameter("noAnnotation");
            ValidationResource resource = registry.mapToValidationResource(param, "Any Name");

            assertNull(resource);
        }

        @Test
        @DisplayName("Should return null for boolean fields (no validation resource mapping)")
        void shouldReturnNullForBooleanField() throws Exception {
            Parameter param = getParameter("booleanField");
            ValidationResource resource = registry.mapToValidationResource(param, "Is Active");

            assertNull(resource);
        }
    }

    @Nested
    @DisplayName("getAllStrategies()")
    class GetAllStrategiesTests {

        @Test
        @DisplayName("Should return a list of all strategies")
        void shouldReturnAllStrategies() {
            List<FieldAnnotationStrategy> strategies = registry.getAllStrategies();

            assertNotNull(strategies);
            assertEquals(7, strategies.size());
        }

        @Test
        @DisplayName("Should return an immutable list")
        void shouldReturnImmutableList() {
            List<FieldAnnotationStrategy> strategies = registry.getAllStrategies();

            assertThrows(UnsupportedOperationException.class, () ->
                    strategies.add(new BooleanFieldAnnotationStrategy()));
        }
    }

    @Nested
    @DisplayName("getStrategyCount()")
    class GetStrategyCountTests {

        @Test
        @DisplayName("Should return 7")
        void shouldReturn7() {
            assertEquals(7, registry.getStrategyCount());
        }
    }
}
