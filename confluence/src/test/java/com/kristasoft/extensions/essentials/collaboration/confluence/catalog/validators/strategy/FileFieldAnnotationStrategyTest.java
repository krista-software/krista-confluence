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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FileFieldAnnotationStrategy Tests")
class FileFieldAnnotationStrategyTest {

    private FileFieldAnnotationStrategy strategy;

    /**
     * Helper class with annotated method parameters for reflection-based testing.
     */
    @SuppressWarnings("unused")
    static class AnnotatedHelper {
        public void attachmentsField(@Field.File(name = "Attachments") String attachments) {}
        public void multipleUploadField(@Field.File(name = "Documents", multipleFileUpload = true) String documents) {}
        public void singleUploadField(@Field.File(name = "SingleDoc", multipleFileUpload = false) String singleDoc) {}
        public void fileWithCustomAttributes(
                @Field.File(name = "Files", multipleFileUpload = true, attributes = {
                        @Attribute(name = "maxSize", value = "10MB"),
                        @Attribute(name = "allowedTypes", value = "pdf,doc,docx")
                }) String files) {}
        public void genericFileField(@Field.File(name = "Upload") String upload) {}
        public void noAnnotation(String plain) {}
        public void textField(@Field.Text(name = "Some Text") String text) {}
    }

    @BeforeEach
    void setUp() {
        strategy = new FileFieldAnnotationStrategy();
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
        @DisplayName("Should return true when parameter has @Field.File with matching name")
        void shouldReturnTrueForMatchingFileAnnotation() throws Exception {
            Parameter param = getParameter("attachmentsField");
            assertTrue(strategy.canHandle(param, "Attachments"));
        }

        @Test
        @DisplayName("Should return false when parameter has @Field.File with non-matching name")
        void shouldReturnFalseForNonMatchingName() throws Exception {
            Parameter param = getParameter("attachmentsField");
            assertFalse(strategy.canHandle(param, "Wrong Name"));
        }

        @Test
        @DisplayName("Should return false when parameter has no annotation")
        void shouldReturnFalseForNoAnnotation() throws Exception {
            Parameter param = getParameter("noAnnotation");
            assertFalse(strategy.canHandle(param, "Attachments"));
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
        @DisplayName("Should extract multipleFileUpload=true when enabled")
        void shouldExtractMultipleFileUploadTrue() throws Exception {
            Parameter param = getParameter("multipleUploadField");
            Map<String, Object> attributes = strategy.extractAttributes(param, "Documents");

            assertNotNull(attributes);
            assertEquals("true", attributes.get("multipleFileUpload"));
        }

        @Test
        @DisplayName("Should extract multipleFileUpload=false when disabled")
        void shouldExtractMultipleFileUploadFalse() throws Exception {
            Parameter param = getParameter("singleUploadField");
            Map<String, Object> attributes = strategy.extractAttributes(param, "SingleDoc");

            assertNotNull(attributes);
            assertEquals("false", attributes.get("multipleFileUpload"));
        }

        @Test
        @DisplayName("Should extract both multipleFileUpload and custom attributes")
        void shouldExtractAllAttributes() throws Exception {
            Parameter param = getParameter("fileWithCustomAttributes");
            Map<String, Object> attributes = strategy.extractAttributes(param, "Files");

            assertNotNull(attributes);
            assertEquals("true", attributes.get("multipleFileUpload"));
            assertEquals("10MB", attributes.get("maxSize"));
            assertEquals("pdf,doc,docx", attributes.get("allowedTypes"));
        }

        @Test
        @DisplayName("Should include multipleFileUpload=false for default file annotation")
        void shouldIncludeMultipleFileUploadForDefaultAnnotation() throws Exception {
            Parameter param = getParameter("attachmentsField");
            Map<String, Object> attributes = strategy.extractAttributes(param, "Attachments");

            assertNotNull(attributes);
            assertTrue(attributes.containsKey("multipleFileUpload"));
        }

        @Test
        @DisplayName("Should return empty map when field name does not match")
        void shouldReturnEmptyMapForNonMatchingName() throws Exception {
            Parameter param = getParameter("attachmentsField");
            Map<String, Object> attributes = strategy.extractAttributes(param, "Wrong Name");
            assertNotNull(attributes);
            assertTrue(attributes.isEmpty());
        }

        @Test
        @DisplayName("Should return empty map when parameter has no @Field.File annotation")
        void shouldReturnEmptyMapForNoAnnotation() throws Exception {
            Parameter param = getParameter("noAnnotation");
            Map<String, Object> attributes = strategy.extractAttributes(param, "Attachments");
            assertNotNull(attributes);
            assertTrue(attributes.isEmpty());
        }
    }

    @Nested
    @DisplayName("extractFieldType()")
    class ExtractFieldTypeTests {

        @Test
        @DisplayName("Should return 'File' for matching @Field.File annotation")
        void shouldReturnFileType() throws Exception {
            Parameter param = getParameter("attachmentsField");
            assertEquals("File", strategy.extractFieldType(param, "Attachments"));
        }

        @Test
        @DisplayName("Should return null when field name does not match")
        void shouldReturnNullForNonMatchingName() throws Exception {
            Parameter param = getParameter("attachmentsField");
            assertNull(strategy.extractFieldType(param, "Wrong Name"));
        }

        @Test
        @DisplayName("Should return null when parameter has no @Field.File annotation")
        void shouldReturnNullForNoAnnotation() throws Exception {
            Parameter param = getParameter("noAnnotation");
            assertNull(strategy.extractFieldType(param, "Attachments"));
        }
    }

    @Nested
    @DisplayName("mapToValidationResource()")
    class MapToValidationResourceTests {

        @Test
        @DisplayName("Should map 'Attachments' to ATTACHMENTS")
        void shouldMapAttachmentsToAttachments() throws Exception {
            Parameter param = getParameter("attachmentsField");
            assertEquals(ValidationResource.ATTACHMENTS, strategy.mapToValidationResource(param, "Attachments"));
        }

        @Test
        @DisplayName("Should return null for unrecognized file field names")
        void shouldReturnNullForUnrecognizedFieldName() throws Exception {
            Parameter param = getParameter("genericFileField");
            assertNull(strategy.mapToValidationResource(param, "Upload"));
        }

        @Test
        @DisplayName("Should return null when field name does not match annotation")
        void shouldReturnNullForNonMatchingName() throws Exception {
            Parameter param = getParameter("attachmentsField");
            assertNull(strategy.mapToValidationResource(param, "Wrong Name"));
        }

        @Test
        @DisplayName("Should return null when parameter has no @Field.File annotation")
        void shouldReturnNullForNoAnnotation() throws Exception {
            Parameter param = getParameter("noAnnotation");
            assertNull(strategy.mapToValidationResource(param, "Attachments"));
        }
    }

    @Nested
    @DisplayName("getAnnotationType()")
    class GetAnnotationTypeTests {

        @Test
        @DisplayName("Should return Field.File.class")
        void shouldReturnFieldFileClass() {
            assertEquals(Field.File.class, strategy.getAnnotationType());
        }
    }

    @Nested
    @DisplayName("getStrategyName()")
    class GetStrategyNameTests {

        @Test
        @DisplayName("Should return 'FileField'")
        void shouldReturnFileField() {
            assertEquals("FileField", strategy.getStrategyName());
        }
    }
}
