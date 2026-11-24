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

package com.kristasoft.extensions.essentials.collaboration.confluence.catalog.extresp;

import app.krista.model.field.NamedField;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("NamedFieldFactory")
class NamedFieldFactoryTest {

    // ──────────────────────────────────────────────────────────────
    //  Simple factory methods
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Simple factory methods")
    class SimpleFactoryMethods {

        @Test
        @DisplayName("createTextField should return a non-null NamedField")
        void createTextField() {
            NamedField field = NamedFieldFactory.createTextField("Name");

            assertNotNull(field);
            assertEquals("Name", field.getName());
        }

        @Test
        @DisplayName("createSwitchField should return a non-null NamedField")
        void createSwitchField() {
            NamedField field = NamedFieldFactory.createSwitchField("Toggle");

            assertNotNull(field);
            assertEquals("Toggle", field.getName());
        }

        @Test
        @DisplayName("createNumberField should return a non-null NamedField with Number type")
        void createNumberField() {
            NamedField field = NamedFieldFactory.createNumberField("Count");

            assertNotNull(field);
            assertEquals("Count", field.getName());
            assertEquals("com.krista.fields.Number", field.getFieldType());
        }

        @Test
        @DisplayName("createBooleanField should return a non-null NamedField with Boolean type")
        void createBooleanField() {
            NamedField field = NamedFieldFactory.createBooleanField("Active");

            assertNotNull(field);
            assertEquals("Active", field.getName());
            assertEquals("com.krista.fields.Boolean", field.getFieldType());
        }

        @Test
        @DisplayName("createDateField should return a non-null NamedField with Date type")
        void createDateField() {
            NamedField field = NamedFieldFactory.createDateField("Birthday");

            assertNotNull(field);
            assertEquals("Birthday", field.getName());
            assertEquals("com.krista.fields.Date", field.getFieldType());
        }

        @Test
        @DisplayName("createRichTextField should return a non-null NamedField with RichText type")
        void createRichTextField() {
            NamedField field = NamedFieldFactory.createRichTextField("Description");

            assertNotNull(field);
            assertEquals("Description", field.getName());
            assertEquals("com.krista.fields.RichText", field.getFieldType());
        }

        @Test
        @DisplayName("createFileField should return a non-null NamedField with File type")
        void createFileField() {
            NamedField field = NamedFieldFactory.createFileField("Attachment");

            assertNotNull(field);
            assertEquals("Attachment", field.getName());
            assertEquals("com.krista.fields.File", field.getFieldType());
        }

        @Test
        @DisplayName("createPickOneField should return a non-null NamedField with PickOne type")
        void createPickOneField() {
            NamedField field = NamedFieldFactory.createPickOneField("Category");

            assertNotNull(field);
            assertEquals("Category", field.getName());
            assertEquals("com.krista.fields.PickOne", field.getFieldType());
        }

        @Test
        @DisplayName("createEmailField should return a non-null NamedField with Email type")
        void createEmailField() {
            NamedField field = NamedFieldFactory.createEmailField("ContactEmail");

            assertNotNull(field);
            assertEquals("ContactEmail", field.getName());
            assertEquals("com.krista.fields.Email", field.getFieldType());
        }

        @Test
        @DisplayName("createPasswordField should return a text field with password attribute")
        void createPasswordField() {
            NamedField field = NamedFieldFactory.createPasswordField("Secret");

            assertNotNull(field);
            assertEquals("Secret", field.getName());
            assertEquals("com.krista.fields.Text", field.getFieldType());
            assertTrue((Boolean) field.getAttributeValues().get("password"),
                    "password attribute should be true");
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  createListField
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createListField")
    class CreateListField {

        @Test
        @DisplayName("should create a list field with inner type")
        void listFieldWithInnerType() {
            NamedField field = NamedFieldFactory.createListField("Emails", "Email");

            assertNotNull(field);
            assertEquals("Emails", field.getName());
            assertEquals("com.krista.fields.List", field.getFieldType());
            assertNotNull(field.getAttributeValues().get("field"),
                    "list field should have a 'field' attribute defining the inner type");
        }

        @Test
        @DisplayName("should create a list field with inner type and attributes")
        void listFieldWithInnerTypeAndAttributes() {
            Map<String, Object> attrs = new HashMap<>();
            attrs.put("maxLength", 100);

            NamedField field = NamedFieldFactory.createListField("Items", "Text", attrs);

            assertNotNull(field);
            assertEquals("Items", field.getName());
            assertEquals("com.krista.fields.List", field.getFieldType());
        }

        @Test
        @DisplayName("should handle null inner attributes")
        void listFieldNullAttributes() {
            NamedField field = NamedFieldFactory.createListField("Items", "Text", null);

            assertNotNull(field);
            assertEquals("com.krista.fields.List", field.getFieldType());
        }

        @Test
        @DisplayName("should handle empty inner attributes")
        void listFieldEmptyAttributes() {
            NamedField field = NamedFieldFactory.createListField("Items", "Date", Map.of());

            assertNotNull(field);
            assertEquals("com.krista.fields.List", field.getFieldType());
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  createFieldWithAttributes
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createFieldWithAttributes")
    class CreateFieldWithAttributes {

        @Test
        @DisplayName("should handle bracket list format [ Email ]")
        void bracketListFormatEmail() {
            NamedField field = NamedFieldFactory.createFieldWithAttributes("Emails", "[ Email ]", Map.of());

            assertNotNull(field);
            assertEquals("com.krista.fields.List", field.getFieldType());
        }

        @Test
        @DisplayName("should handle angle bracket list format List<Email>")
        void angleBracketListFormat() {
            NamedField field = NamedFieldFactory.createFieldWithAttributes("Emails", "List<Email>", Map.of());

            assertNotNull(field);
            assertEquals("com.krista.fields.List", field.getFieldType());
        }

        @Test
        @DisplayName("should handle square bracket list format List[Date]")
        void squareBracketListFormat() {
            NamedField field = NamedFieldFactory.createFieldWithAttributes("Dates", "List[Date]", Map.of());

            assertNotNull(field);
            assertEquals("com.krista.fields.List", field.getFieldType());
        }

        @Test
        @DisplayName("should handle plain 'List' type defaulting to Text list")
        void plainListDefaultsToText() {
            NamedField field = NamedFieldFactory.createFieldWithAttributes("Items", "List", Map.of());

            assertNotNull(field);
            assertEquals("com.krista.fields.List", field.getFieldType());
        }

        @Test
        @DisplayName("should handle password special case for text field named Password")
        void passwordSpecialCase() {
            Map<String, Object> attrs = new HashMap<>();
            attrs.put("visualWidth", "M");

            NamedField field = NamedFieldFactory.createFieldWithAttributes("Password", "Text", attrs);

            assertNotNull(field);
            assertEquals("com.krista.fields.Text", field.getFieldType());
            assertTrue((Boolean) field.getAttributeValues().get("password"),
                    "password attribute should be set for field named Password");
        }

        @Test
        @DisplayName("should delegate standard types to createFieldByTypeWithAttributes")
        void standardTypesDelegated() {
            NamedField field = NamedFieldFactory.createFieldWithAttributes("MyDate", "Date", Map.of());

            assertNotNull(field);
            assertEquals("com.krista.fields.Date", field.getFieldType());
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  createFieldByTypeWithAttributes
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createFieldByTypeWithAttributes")
    class CreateFieldByTypeWithAttributes {

        @ParameterizedTest
        @ValueSource(strings = {"date", "email", "number", "boolean", "file", "switch", "richtext", "pickone", "text"})
        @DisplayName("should create field for each recognized type")
        void recognizedTypes(String type) {
            Map<String, Object> attrs = new HashMap<>();
            attrs.put("visualWidth", "S");

            NamedField field = NamedFieldFactory.createFieldByTypeWithAttributes("TestField", type, attrs);

            assertNotNull(field, "Field should not be null for type: " + type);
            assertEquals("TestField", field.getName());
        }

        @Test
        @DisplayName("should handle password type with password attribute")
        void passwordType() {
            Map<String, Object> attrs = new HashMap<>();
            attrs.put("visualWidth", "M");

            NamedField field = NamedFieldFactory.createFieldByTypeWithAttributes("Secret", "password", attrs);

            assertNotNull(field);
            assertEquals("com.krista.fields.Text", field.getFieldType());
            assertTrue((Boolean) field.getAttributeValues().get("password"));
        }

        @Test
        @DisplayName("should handle list type defaulting to Text list")
        void listType() {
            NamedField field = NamedFieldFactory.createFieldByTypeWithAttributes("Items", "list", Map.of());

            assertNotNull(field);
            assertEquals("com.krista.fields.List", field.getFieldType());
        }

        @Test
        @DisplayName("should default to text field for unknown type")
        void unknownTypeDefaultsToText() {
            NamedField field = NamedFieldFactory.createFieldByTypeWithAttributes("Unknown", "foobar", Map.of());

            assertNotNull(field);
            assertEquals("com.krista.fields.Text", field.getFieldType());
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  createFieldByType
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createFieldByType")
    class CreateFieldByType {

        @ParameterizedTest
        @ValueSource(strings = {"date", "email", "number", "boolean", "file", "switch", "richtext", "pickone", "text", "password", "list"})
        @DisplayName("should create field for each recognized type")
        void recognizedTypes(String type) {
            NamedField field = NamedFieldFactory.createFieldByType("TestField", type);

            assertNotNull(field, "Field should not be null for type: " + type);
            assertEquals("TestField", field.getName());
        }

        @Test
        @DisplayName("should default to text field for unknown type")
        void unknownTypeDefaultsToText() {
            NamedField field = NamedFieldFactory.createFieldByType("Fallback", "unknown_type");

            assertNotNull(field);
            assertEquals("com.krista.fields.Text", field.getFieldType());
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  WithAttributes methods
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("WithAttributes factory methods")
    class WithAttributesMethods {

        @Test
        @DisplayName("createTextFieldWithAttributes should merge default and custom attributes")
        void textFieldWithAttributes() {
            Map<String, Object> attrs = new HashMap<>();
            attrs.put("customKey", "customValue");

            NamedField field = NamedFieldFactory.createTextFieldWithAttributes("Name", attrs);

            assertNotNull(field);
            assertEquals("com.krista.fields.Text", field.getFieldType());
            assertEquals("customValue", field.getAttributeValues().get("customKey"));
        }

        @Test
        @DisplayName("createTextFieldWithAttributes should handle null attributes")
        void textFieldWithNullAttributes() {
            NamedField field = NamedFieldFactory.createTextFieldWithAttributes("Name", null);

            assertNotNull(field);
            assertEquals("com.krista.fields.Text", field.getFieldType());
        }

        @Test
        @DisplayName("createSwitchFieldWithAttributes should use provided attributes")
        void switchFieldWithAttributes() {
            Map<String, Object> attrs = Map.of("defaultValue", true);

            NamedField field = NamedFieldFactory.createSwitchFieldWithAttributes("Toggle", attrs);

            assertNotNull(field);
            assertEquals("com.krista.fields.Switch", field.getFieldType());
        }

        @Test
        @DisplayName("createNumberFieldWithAttributes should use provided attributes")
        void numberFieldWithAttributes() {
            Map<String, Object> attrs = Map.of("min", 0);

            NamedField field = NamedFieldFactory.createNumberFieldWithAttributes("Count", attrs);

            assertNotNull(field);
            assertEquals("com.krista.fields.Number", field.getFieldType());
        }

        @Test
        @DisplayName("createBooleanFieldWithAttributes should use provided attributes")
        void booleanFieldWithAttributes() {
            NamedField field = NamedFieldFactory.createBooleanFieldWithAttributes("Flag", Map.of());

            assertNotNull(field);
            assertEquals("com.krista.fields.Boolean", field.getFieldType());
        }

        @Test
        @DisplayName("createDateFieldWithAttributes should use provided attributes")
        void dateFieldWithAttributes() {
            NamedField field = NamedFieldFactory.createDateFieldWithAttributes("Due", Map.of());

            assertNotNull(field);
            assertEquals("com.krista.fields.Date", field.getFieldType());
        }

        @Test
        @DisplayName("createRichTextFieldWithAttributes should use provided attributes")
        void richTextFieldWithAttributes() {
            NamedField field = NamedFieldFactory.createRichTextFieldWithAttributes("Content", Map.of());

            assertNotNull(field);
            assertEquals("com.krista.fields.RichText", field.getFieldType());
        }

        @Test
        @DisplayName("createFileFieldWithAttributes should include default file upload config")
        void fileFieldWithAttributes() {
            Map<String, Object> attrs = new HashMap<>();
            attrs.put("multipleFileUpload", true);

            NamedField field = NamedFieldFactory.createFileFieldWithAttributes("Upload", attrs);

            assertNotNull(field);
            assertEquals("com.krista.fields.File", field.getFieldType());
            assertEquals("S", field.getAttributeValues().get("visualWidth"));
            assertEquals(true, field.getAttributeValues().get("multipleFileUpload"));
        }

        @Test
        @DisplayName("createFileFieldWithAttributes should default multipleFileUpload to false")
        void fileFieldDefaultMultipleUpload() {
            NamedField field = NamedFieldFactory.createFileFieldWithAttributes("Upload", Map.of());

            assertNotNull(field);
            assertEquals(false, field.getAttributeValues().get("multipleFileUpload"));
        }

        @Test
        @DisplayName("createPickOneFieldWithAttributes should use provided attributes")
        void pickOneFieldWithAttributes() {
            NamedField field = NamedFieldFactory.createPickOneFieldWithAttributes("Choice", Map.of());

            assertNotNull(field);
            assertEquals("com.krista.fields.PickOne", field.getFieldType());
        }

        @Test
        @DisplayName("createEmailFieldWithAttributes should use provided attributes")
        void emailFieldWithAttributes() {
            NamedField field = NamedFieldFactory.createEmailFieldWithAttributes("Contact", Map.of());

            assertNotNull(field);
            assertEquals("com.krista.fields.Email", field.getFieldType());
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  getDefaultAttributesForFieldType
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getDefaultAttributesForFieldType")
    class GetDefaultAttributes {

        @Test
        @DisplayName("should return non-null map for Text type")
        void textTypeAttributes() {
            Map<String, Object> attrs = NamedFieldFactory.getDefaultAttributesForFieldType("Text");

            assertNotNull(attrs);
        }

        @Test
        @DisplayName("should return non-null map for File type")
        void fileTypeAttributes() {
            Map<String, Object> attrs = NamedFieldFactory.getDefaultAttributesForFieldType("File");

            assertNotNull(attrs);
        }

        @Test
        @DisplayName("should return non-null map for Date type")
        void dateTypeAttributes() {
            Map<String, Object> attrs = NamedFieldFactory.getDefaultAttributesForFieldType("Date");

            assertNotNull(attrs);
        }

        @Test
        @DisplayName("should return non-null map for unknown type (default provider)")
        void unknownTypeAttributes() {
            Map<String, Object> attrs = NamedFieldFactory.getDefaultAttributesForFieldType("UnknownType");

            assertNotNull(attrs);
        }
    }
}
