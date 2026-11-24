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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FieldTypes Tests")
class FieldTypesTest {

    // --- Constant value tests ---

    @Nested
    @DisplayName("Constant Value Tests")
    class ConstantValueTests {

        @Test
        @DisplayName("TEXT_FIELD_LOGICAL_NAME should be 'com.krista.fields.Text'")
        void textFieldLogicalName() {
            assertEquals("com.krista.fields.Text", FieldTypes.TEXT_FIELD_LOGICAL_NAME);
        }

        @Test
        @DisplayName("NUMBER_FIELD_LOGICAL_NAME should be 'com.krista.fields.Number'")
        void numberFieldLogicalName() {
            assertEquals("com.krista.fields.Number", FieldTypes.NUMBER_FIELD_LOGICAL_NAME);
        }

        @Test
        @DisplayName("SWITCH_FIELD_LOGICAL_NAME should be 'com.krista.fields.Switch'")
        void switchFieldLogicalName() {
            assertEquals("com.krista.fields.Switch", FieldTypes.SWITCH_FIELD_LOGICAL_NAME);
        }

        @Test
        @DisplayName("BOOLEAN_FIELD_LOGICAL_NAME should be 'com.krista.fields.Boolean'")
        void booleanFieldLogicalName() {
            assertEquals("com.krista.fields.Boolean", FieldTypes.BOOLEAN_FIELD_LOGICAL_NAME);
        }

        @Test
        @DisplayName("DATE_FIELD_LOGICAL_NAME should be 'com.krista.fields.Date'")
        void dateFieldLogicalName() {
            assertEquals("com.krista.fields.Date", FieldTypes.DATE_FIELD_LOGICAL_NAME);
        }

        @Test
        @DisplayName("RICH_TEXT_FIELD_LOGICAL_NAME should be 'com.krista.fields.RichText'")
        void richTextFieldLogicalName() {
            assertEquals("com.krista.fields.RichText", FieldTypes.RICH_TEXT_FIELD_LOGICAL_NAME);
        }

        @Test
        @DisplayName("FILE_FIELD_LOGICAL_NAME should be 'com.krista.fields.File'")
        void fileFieldLogicalName() {
            assertEquals("com.krista.fields.File", FieldTypes.FILE_FIELD_LOGICAL_NAME);
        }

        @Test
        @DisplayName("PICK_ONE_LOGICAL_NAME should be 'com.krista.fields.PickOne'")
        void pickOneLogicalName() {
            assertEquals("com.krista.fields.PickOne", FieldTypes.PICK_ONE_LOGICAL_NAME);
        }

        @Test
        @DisplayName("TABLE_FIELD_LOGICAL_NAME should be 'com.krista.fields.Table'")
        void tableFieldLogicalName() {
            assertEquals("com.krista.fields.Table", FieldTypes.TABLE_FIELD_LOGICAL_NAME);
        }

        @Test
        @DisplayName("ENTITY_FIELD_LOGICAL_NAME should be 'com.krista.fields.EntityReference'")
        void entityFieldLogicalName() {
            assertEquals("com.krista.fields.EntityReference", FieldTypes.ENTITY_FIELD_LOGICAL_NAME);
        }

        @Test
        @DisplayName("SELECTION_FIELD_LOGICAL_NAME should be 'com.krista.fields.Entity'")
        void selectionFieldLogicalName() {
            assertEquals("com.krista.fields.Entity", FieldTypes.SELECTION_FIELD_LOGICAL_NAME);
        }

        @Test
        @DisplayName("SCHEDULE_FIELD_LOGICAL_NAME should be 'com.krista.fields.Schedule'")
        void scheduleFieldLogicalName() {
            assertEquals("com.krista.fields.Schedule", FieldTypes.SCHEDULE_FIELD_LOGICAL_NAME);
        }

        @Test
        @DisplayName("LABEL_FIELD_LOGICAL_NAME should be 'com.krista.fields.Label'")
        void labelFieldLogicalName() {
            assertEquals("com.krista.fields.Label", FieldTypes.LABEL_FIELD_LOGICAL_NAME);
        }

        @Test
        @DisplayName("CALCULATION_FIELD_LOGICAL_NAME should be 'com.krista.fields.Calculation'")
        void calculationFieldLogicalName() {
            assertEquals("com.krista.fields.Calculation", FieldTypes.CALCULATION_FIELD_LOGICAL_NAME);
        }

        @Test
        @DisplayName("CAMERA_FIELD_LOGICAL_NAME should be 'com.krista.fields.Camera'")
        void cameraFieldLogicalName() {
            assertEquals("com.krista.fields.Camera", FieldTypes.CAMERA_FIELD_LOGICAL_NAME);
        }

        @Test
        @DisplayName("COMPOSITE_FIELD_LOGICAL_NAME should be 'com.krista.fields.Composite'")
        void compositeFieldLogicalName() {
            assertEquals("com.krista.fields.Composite", FieldTypes.COMPOSITE_FIELD_LOGICAL_NAME);
        }

        @Test
        @DisplayName("LIST_FIELD_LOGICAL_NAME should be 'com.krista.fields.List'")
        void listFieldLogicalName() {
            assertEquals("com.krista.fields.List", FieldTypes.LIST_FIELD_LOGICAL_NAME);
        }

        @Test
        @DisplayName("MULTI_FIELD_LOGICAL_NAME should be 'com.krista.fields.MultiField'")
        void multiFieldLogicalName() {
            assertEquals("com.krista.fields.MultiField", FieldTypes.MULTI_FIELD_LOGICAL_NAME);
        }

        @Test
        @DisplayName("PARAGRAPH_FIELD_LOGICAL_NAME should be 'com.krista.fields.Paragraph'")
        void paragraphFieldLogicalName() {
            assertEquals("com.krista.fields.Paragraph", FieldTypes.PARAGRAPH_FIELD_LOGICAL_NAME);
        }

        @Test
        @DisplayName("CURRENCY_FIELD_LOGICAL_NAME should be 'com.krista.fields.Currency'")
        void currencyFieldLogicalName() {
            assertEquals("com.krista.fields.Currency", FieldTypes.CURRENCY_FIELD_LOGICAL_NAME);
        }

        @Test
        @DisplayName("PERCENTAGE_FIELD_LOGICAL_NAME should be 'com.krista.fields.Percentage'")
        void percentageFieldLogicalName() {
            assertEquals("com.krista.fields.Percentage", FieldTypes.PERCENTAGE_FIELD_LOGICAL_NAME);
        }

        @Test
        @DisplayName("UNIT_FIELD_LOGICAL_NAME should be 'com.krista.fields.Unit'")
        void unitFieldLogicalName() {
            assertEquals("com.krista.fields.Unit", FieldTypes.UNIT_FIELD_LOGICAL_NAME);
        }

        @Test
        @DisplayName("PICK_MANY_LOGICAL_NAME should be 'com.krista.fields.Checkboxes'")
        void pickManyLogicalName() {
            assertEquals("com.krista.fields.Checkboxes", FieldTypes.PICK_MANY_LOGICAL_NAME);
        }

        @Test
        @DisplayName("ADDRESS_FIELD_LOGICAL_NAME should be 'com.krista.fields.Address'")
        void addressFieldLogicalName() {
            assertEquals("com.krista.fields.Address", FieldTypes.ADDRESS_FIELD_LOGICAL_NAME);
        }

        @Test
        @DisplayName("DATE_RANGE_LOGICAL_NAME should be 'com.krista.fields.DateRange'")
        void dateRangeLogicalName() {
            assertEquals("com.krista.fields.DateRange", FieldTypes.DATE_RANGE_LOGICAL_NAME);
        }

        @Test
        @DisplayName("SCHEDULE_LOGICAL_NAME should be 'com.krista.fields.Schedule'")
        void scheduleLogicalName() {
            assertEquals("com.krista.fields.Schedule", FieldTypes.SCHEDULE_LOGICAL_NAME);
        }

        @Test
        @DisplayName("DATE_TIME_RANGE_LOGICAL_NAME should be 'com.krista.fields.DateTimeRange'")
        void dateTimeRangeLogicalName() {
            assertEquals("com.krista.fields.DateTimeRange", FieldTypes.DATE_TIME_RANGE_LOGICAL_NAME);
        }

        @Test
        @DisplayName("PHONE_FIELD_LOGICAL_NAME should be 'com.krista.fields.Phone'")
        void phoneFieldLogicalName() {
            assertEquals("com.krista.fields.Phone", FieldTypes.PHONE_FIELD_LOGICAL_NAME);
        }

        @Test
        @DisplayName("LOCATION_FIELD_LOGICAL_NAME should be 'com.krista.fields.Location'")
        void locationFieldLogicalName() {
            assertEquals("com.krista.fields.Location", FieldTypes.LOCATION_FIELD_LOGICAL_NAME);
        }

        @Test
        @DisplayName("TIME_FIELD_LOGICAL_NAME should be 'com.krista.fields.Time'")
        void timeFieldLogicalName() {
            assertEquals("com.krista.fields.Time", FieldTypes.TIME_FIELD_LOGICAL_NAME);
        }

        @Test
        @DisplayName("EMAIL_FIELD_LOGICAL_NAME should be 'com.krista.fields.Email'")
        void emailFieldLogicalName() {
            assertEquals("com.krista.fields.Email", FieldTypes.EMAIL_FIELD_LOGICAL_NAME);
        }

        @Test
        @DisplayName("STREAM_FIELD_LOGICAL_NAME should be 'com.krista.fields.Stream'")
        void streamFieldLogicalName() {
            assertEquals("com.krista.fields.Stream", FieldTypes.STREAM_FIELD_LOGICAL_NAME);
        }
    }

    @Nested
    @DisplayName("Legacy Constant Alias Tests")
    class LegacyConstantAliasTests {

        @Test
        @DisplayName("TEXT_FIELD should equal TEXT_FIELD_LOGICAL_NAME")
        void textFieldAlias() {
            assertSame(FieldTypes.TEXT_FIELD_LOGICAL_NAME, FieldTypes.TEXT_FIELD);
        }

        @Test
        @DisplayName("NUMBER_FIELD should equal NUMBER_FIELD_LOGICAL_NAME")
        void numberFieldAlias() {
            assertSame(FieldTypes.NUMBER_FIELD_LOGICAL_NAME, FieldTypes.NUMBER_FIELD);
        }

        @Test
        @DisplayName("SWITCH_FIELD should equal SWITCH_FIELD_LOGICAL_NAME")
        void switchFieldAlias() {
            assertSame(FieldTypes.SWITCH_FIELD_LOGICAL_NAME, FieldTypes.SWITCH_FIELD);
        }

        @Test
        @DisplayName("BOOLEAN_FIELD should equal BOOLEAN_FIELD_LOGICAL_NAME")
        void booleanFieldAlias() {
            assertSame(FieldTypes.BOOLEAN_FIELD_LOGICAL_NAME, FieldTypes.BOOLEAN_FIELD);
        }

        @Test
        @DisplayName("DATE_FIELD should equal DATE_FIELD_LOGICAL_NAME")
        void dateFieldAlias() {
            assertSame(FieldTypes.DATE_FIELD_LOGICAL_NAME, FieldTypes.DATE_FIELD);
        }

        @Test
        @DisplayName("RICH_TEXT_FIELD should equal RICH_TEXT_FIELD_LOGICAL_NAME")
        void richTextFieldAlias() {
            assertSame(FieldTypes.RICH_TEXT_FIELD_LOGICAL_NAME, FieldTypes.RICH_TEXT_FIELD);
        }

        @Test
        @DisplayName("FILE_FIELD should equal FILE_FIELD_LOGICAL_NAME")
        void fileFieldAlias() {
            assertSame(FieldTypes.FILE_FIELD_LOGICAL_NAME, FieldTypes.FILE_FIELD);
        }

        @Test
        @DisplayName("PICK_ONE_FIELD should equal PICK_ONE_LOGICAL_NAME")
        void pickOneFieldAlias() {
            assertSame(FieldTypes.PICK_ONE_LOGICAL_NAME, FieldTypes.PICK_ONE_FIELD);
        }
    }

    // --- Utility method tests ---

    @Nested
    @DisplayName("isCompositeField Tests")
    class IsCompositeFieldTests {

        @Test
        @DisplayName("Should return true for composite field logical name")
        void shouldReturnTrueForComposite() {
            assertTrue(FieldTypes.isCompositeField(FieldTypes.COMPOSITE_FIELD_LOGICAL_NAME));
        }

        @Test
        @DisplayName("Should return false for non-composite field")
        void shouldReturnFalseForNonComposite() {
            assertFalse(FieldTypes.isCompositeField(FieldTypes.TEXT_FIELD_LOGICAL_NAME));
        }
    }

    @Nested
    @DisplayName("isTableField Tests")
    class IsTableFieldTests {

        @Test
        @DisplayName("Should return true for table field logical name")
        void shouldReturnTrueForTable() {
            assertTrue(FieldTypes.isTableField(FieldTypes.TABLE_FIELD_LOGICAL_NAME));
        }

        @Test
        @DisplayName("Should return false for non-table field")
        void shouldReturnFalseForNonTable() {
            assertFalse(FieldTypes.isTableField(FieldTypes.TEXT_FIELD_LOGICAL_NAME));
        }
    }

    @Nested
    @DisplayName("isEntityField Tests")
    class IsEntityFieldTests {

        @Test
        @DisplayName("Should return true for entity field logical name")
        void shouldReturnTrueForEntity() {
            assertTrue(FieldTypes.isEntityField(FieldTypes.ENTITY_FIELD_LOGICAL_NAME));
        }

        @Test
        @DisplayName("Should return false for non-entity field")
        void shouldReturnFalseForNonEntity() {
            assertFalse(FieldTypes.isEntityField(FieldTypes.TEXT_FIELD_LOGICAL_NAME));
        }
    }

    @Nested
    @DisplayName("isSelectionField Tests")
    class IsSelectionFieldTests {

        @Test
        @DisplayName("Should return true for selection field logical name")
        void shouldReturnTrueForSelection() {
            assertTrue(FieldTypes.isSelectionField(FieldTypes.SELECTION_FIELD_LOGICAL_NAME));
        }

        @Test
        @DisplayName("Should return false for non-selection field")
        void shouldReturnFalseForNonSelection() {
            assertFalse(FieldTypes.isSelectionField(FieldTypes.TEXT_FIELD_LOGICAL_NAME));
        }
    }

    @Nested
    @DisplayName("isListField Tests")
    class IsListFieldTests {

        @Test
        @DisplayName("Should return true for list field logical name")
        void shouldReturnTrueForList() {
            assertTrue(FieldTypes.isListField(FieldTypes.LIST_FIELD_LOGICAL_NAME));
        }

        @Test
        @DisplayName("Should return false for non-list field")
        void shouldReturnFalseForNonList() {
            assertFalse(FieldTypes.isListField(FieldTypes.TEXT_FIELD_LOGICAL_NAME));
        }
    }

    @Nested
    @DisplayName("isMultiField Tests")
    class IsMultiFieldTests {

        @Test
        @DisplayName("Should return true for multi field logical name")
        void shouldReturnTrueForMulti() {
            assertTrue(FieldTypes.isMultiField(FieldTypes.MULTI_FIELD_LOGICAL_NAME));
        }

        @Test
        @DisplayName("Should return false for non-multi field")
        void shouldReturnFalseForNonMulti() {
            assertFalse(FieldTypes.isMultiField(FieldTypes.TEXT_FIELD_LOGICAL_NAME));
        }
    }

    @Nested
    @DisplayName("isNumberField Tests")
    class IsNumberFieldTests {

        @Test
        @DisplayName("Should return true for number field logical name")
        void shouldReturnTrueForNumber() {
            assertTrue(FieldTypes.isNumberField(FieldTypes.NUMBER_FIELD_LOGICAL_NAME));
        }

        @Test
        @DisplayName("Should return false for non-number field")
        void shouldReturnFalseForNonNumber() {
            assertFalse(FieldTypes.isNumberField(FieldTypes.TEXT_FIELD_LOGICAL_NAME));
        }
    }

    @Nested
    @DisplayName("isFileField Tests")
    class IsFileFieldTests {

        @Test
        @DisplayName("Should return true for file field logical name")
        void shouldReturnTrueForFile() {
            assertTrue(FieldTypes.isFileField(FieldTypes.FILE_FIELD_LOGICAL_NAME));
        }

        @Test
        @DisplayName("Should return false for non-file field")
        void shouldReturnFalseForNonFile() {
            assertFalse(FieldTypes.isFileField(FieldTypes.TEXT_FIELD_LOGICAL_NAME));
        }
    }

    @Nested
    @DisplayName("isPickOneField Tests")
    class IsPickOneFieldTests {

        @Test
        @DisplayName("Should return true for pick-one field logical name")
        void shouldReturnTrueForPickOne() {
            assertTrue(FieldTypes.isPickOneField(FieldTypes.PICK_ONE_LOGICAL_NAME));
        }

        @Test
        @DisplayName("Should return false for non-pick-one field")
        void shouldReturnFalseForNonPickOne() {
            assertFalse(FieldTypes.isPickOneField(FieldTypes.TEXT_FIELD_LOGICAL_NAME));
        }
    }

    @Nested
    @DisplayName("isPickManyField Tests")
    class IsPickManyFieldTests {

        @Test
        @DisplayName("Should return true for pick-many field logical name")
        void shouldReturnTrueForPickMany() {
            assertTrue(FieldTypes.isPickManyField(FieldTypes.PICK_MANY_LOGICAL_NAME));
        }

        @Test
        @DisplayName("Should return false for non-pick-many field")
        void shouldReturnFalseForNonPickMany() {
            assertFalse(FieldTypes.isPickManyField(FieldTypes.TEXT_FIELD_LOGICAL_NAME));
        }
    }

    @Nested
    @DisplayName("isStreamField Tests")
    class IsStreamFieldTests {

        @Test
        @DisplayName("Should return true for stream field logical name")
        void shouldReturnTrueForStream() {
            assertTrue(FieldTypes.isStreamField(FieldTypes.STREAM_FIELD_LOGICAL_NAME));
        }

        @Test
        @DisplayName("Should return false for non-stream field")
        void shouldReturnFalseForNonStream() {
            assertFalse(FieldTypes.isStreamField(FieldTypes.TEXT_FIELD_LOGICAL_NAME));
        }
    }

    @Nested
    @DisplayName("isTextField Tests")
    class IsTextFieldTests {

        @Test
        @DisplayName("Should return true for text field logical name")
        void shouldReturnTrueForText() {
            assertTrue(FieldTypes.isTextField(FieldTypes.TEXT_FIELD_LOGICAL_NAME));
        }

        @Test
        @DisplayName("Should return false for non-text field")
        void shouldReturnFalseForNonText() {
            assertFalse(FieldTypes.isTextField(FieldTypes.NUMBER_FIELD_LOGICAL_NAME));
        }
    }

    @Nested
    @DisplayName("isDateField Tests")
    class IsDateFieldTests {

        @Test
        @DisplayName("Should return true for date field logical name")
        void shouldReturnTrueForDate() {
            assertTrue(FieldTypes.isDateField(FieldTypes.DATE_FIELD_LOGICAL_NAME));
        }

        @Test
        @DisplayName("Should return false for non-date field")
        void shouldReturnFalseForNonDate() {
            assertFalse(FieldTypes.isDateField(FieldTypes.TEXT_FIELD_LOGICAL_NAME));
        }
    }

    @Nested
    @DisplayName("isBooleanField Tests")
    class IsBooleanFieldTests {

        @Test
        @DisplayName("Should return true for boolean field logical name")
        void shouldReturnTrueForBoolean() {
            assertTrue(FieldTypes.isBooleanField(FieldTypes.BOOLEAN_FIELD_LOGICAL_NAME));
        }

        @Test
        @DisplayName("Should return false for non-boolean field")
        void shouldReturnFalseForNonBoolean() {
            assertFalse(FieldTypes.isBooleanField(FieldTypes.TEXT_FIELD_LOGICAL_NAME));
        }
    }

    @Nested
    @DisplayName("isSwitchField Tests")
    class IsSwitchFieldTests {

        @Test
        @DisplayName("Should return true for switch field logical name")
        void shouldReturnTrueForSwitch() {
            assertTrue(FieldTypes.isSwitchField(FieldTypes.SWITCH_FIELD_LOGICAL_NAME));
        }

        @Test
        @DisplayName("Should return false for non-switch field")
        void shouldReturnFalseForNonSwitch() {
            assertFalse(FieldTypes.isSwitchField(FieldTypes.TEXT_FIELD_LOGICAL_NAME));
        }
    }

    @Nested
    @DisplayName("isRichTextField Tests")
    class IsRichTextFieldTests {

        @Test
        @DisplayName("Should return true for rich text field logical name")
        void shouldReturnTrueForRichText() {
            assertTrue(FieldTypes.isRichTextField(FieldTypes.RICH_TEXT_FIELD_LOGICAL_NAME));
        }

        @Test
        @DisplayName("Should return false for non-rich-text field")
        void shouldReturnFalseForNonRichText() {
            assertFalse(FieldTypes.isRichTextField(FieldTypes.TEXT_FIELD_LOGICAL_NAME));
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("All type check methods should return false for empty string")
        void shouldReturnFalseForEmptyString() {
            assertAll(
                    () -> assertFalse(FieldTypes.isCompositeField("")),
                    () -> assertFalse(FieldTypes.isTableField("")),
                    () -> assertFalse(FieldTypes.isEntityField("")),
                    () -> assertFalse(FieldTypes.isSelectionField("")),
                    () -> assertFalse(FieldTypes.isListField("")),
                    () -> assertFalse(FieldTypes.isMultiField("")),
                    () -> assertFalse(FieldTypes.isNumberField("")),
                    () -> assertFalse(FieldTypes.isFileField("")),
                    () -> assertFalse(FieldTypes.isPickOneField("")),
                    () -> assertFalse(FieldTypes.isPickManyField("")),
                    () -> assertFalse(FieldTypes.isStreamField("")),
                    () -> assertFalse(FieldTypes.isTextField("")),
                    () -> assertFalse(FieldTypes.isDateField("")),
                    () -> assertFalse(FieldTypes.isBooleanField("")),
                    () -> assertFalse(FieldTypes.isSwitchField("")),
                    () -> assertFalse(FieldTypes.isRichTextField(""))
            );
        }

        @Test
        @DisplayName("All type check methods should return false for arbitrary string")
        void shouldReturnFalseForArbitraryString() {
            String arbitrary = "com.krista.fields.Unknown";
            assertAll(
                    () -> assertFalse(FieldTypes.isCompositeField(arbitrary)),
                    () -> assertFalse(FieldTypes.isTableField(arbitrary)),
                    () -> assertFalse(FieldTypes.isEntityField(arbitrary)),
                    () -> assertFalse(FieldTypes.isSelectionField(arbitrary)),
                    () -> assertFalse(FieldTypes.isListField(arbitrary)),
                    () -> assertFalse(FieldTypes.isMultiField(arbitrary)),
                    () -> assertFalse(FieldTypes.isNumberField(arbitrary)),
                    () -> assertFalse(FieldTypes.isFileField(arbitrary)),
                    () -> assertFalse(FieldTypes.isPickOneField(arbitrary)),
                    () -> assertFalse(FieldTypes.isPickManyField(arbitrary)),
                    () -> assertFalse(FieldTypes.isStreamField(arbitrary)),
                    () -> assertFalse(FieldTypes.isTextField(arbitrary)),
                    () -> assertFalse(FieldTypes.isDateField(arbitrary)),
                    () -> assertFalse(FieldTypes.isBooleanField(arbitrary)),
                    () -> assertFalse(FieldTypes.isSwitchField(arbitrary)),
                    () -> assertFalse(FieldTypes.isRichTextField(arbitrary))
            );
        }
    }

    @Test
    @DisplayName("Constructor should be private")
    void constructorShouldBePrivate() throws NoSuchMethodException {
        Constructor<FieldTypes> constructor = FieldTypes.class.getDeclaredConstructor();
        assertFalse(constructor.canAccess(null), "Constructor should not be accessible");
        constructor.setAccessible(true);
        assertDoesNotThrow(() -> constructor.newInstance());
    }
}
