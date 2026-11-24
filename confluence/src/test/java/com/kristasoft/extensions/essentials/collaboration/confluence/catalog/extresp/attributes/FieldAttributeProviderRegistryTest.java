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

package com.kristasoft.extensions.essentials.collaboration.confluence.catalog.extresp.attributes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FieldAttributeProviderRegistry Tests")
class FieldAttributeProviderRegistryTest {

    @Test
    @DisplayName("Should return Text-specific attributes for 'Text' field type")
    void shouldReturnTextAttributes() {
        Map<String, Object> attrs = FieldAttributeProviderRegistry.getDefaultAttributes("Text");
        assertNotNull(attrs);
        assertEquals("S", attrs.get("visualWidth"));
        assertEquals(true, attrs.get("trim"));
    }

    @Test
    @DisplayName("Should return Text-specific attributes for 'text' (lowercase) field type")
    void shouldReturnTextAttributesCaseInsensitive() {
        Map<String, Object> attrs = FieldAttributeProviderRegistry.getDefaultAttributes("text");
        assertNotNull(attrs);
        assertEquals("S", attrs.get("visualWidth"));
        assertEquals(true, attrs.get("trim"));
    }

    @Test
    @DisplayName("Should return File-specific attributes for 'File' field type")
    void shouldReturnFileAttributes() {
        Map<String, Object> attrs = FieldAttributeProviderRegistry.getDefaultAttributes("File");
        assertNotNull(attrs);
        assertEquals("S", attrs.get("visualWidth"));
        assertEquals(false, attrs.get("multipleFileUpload"));
        assertEquals("KB", attrs.get("minFileSizeUnit"));
        assertEquals("MB", attrs.get("maxFileSizeUnit"));
        assertNotNull(attrs.get("fileTypes"));
    }

    @Test
    @DisplayName("Should return Date-specific attributes for 'Date' field type")
    void shouldReturnDateAttributes() {
        Map<String, Object> attrs = FieldAttributeProviderRegistry.getDefaultAttributes("Date");
        assertNotNull(attrs);
        assertEquals("S", attrs.get("visualWidth"));
        assertEquals("MM/dd/yyyy", attrs.get("dateFormat"));
        assertEquals(true, attrs.get("showCalendar"));
        assertEquals(true, attrs.get("allowPastDates"));
        assertEquals(true, attrs.get("allowFutureDates"));
    }

    @Test
    @DisplayName("Should return default attributes for unknown field type")
    void shouldReturnDefaultAttributesForUnknownType() {
        Map<String, Object> attrs = FieldAttributeProviderRegistry.getDefaultAttributes("UnknownType");
        assertNotNull(attrs);
        assertEquals("S", attrs.get("visualWidth"));
        assertEquals(1, attrs.size(), "Default attributes should only contain visualWidth");
    }

    @Test
    @DisplayName("hasProvider should return true for registered types")
    void hasProviderShouldReturnTrueForRegisteredTypes() {
        assertTrue(FieldAttributeProviderRegistry.hasProvider("text"));
        assertTrue(FieldAttributeProviderRegistry.hasProvider("file"));
        assertTrue(FieldAttributeProviderRegistry.hasProvider("date"));
    }

    @Test
    @DisplayName("hasProvider should return true for registered types (case insensitive)")
    void hasProviderShouldBeCaseInsensitive() {
        assertTrue(FieldAttributeProviderRegistry.hasProvider("Text"));
        assertTrue(FieldAttributeProviderRegistry.hasProvider("FILE"));
        assertTrue(FieldAttributeProviderRegistry.hasProvider("DATE"));
    }

    @Test
    @DisplayName("hasProvider should return false for unregistered types")
    void hasProviderShouldReturnFalseForUnregisteredTypes() {
        assertFalse(FieldAttributeProviderRegistry.hasProvider("UnknownField"));
        assertFalse(FieldAttributeProviderRegistry.hasProvider("Boolean"));
        assertFalse(FieldAttributeProviderRegistry.hasProvider("Number"));
    }

    @Test
    @DisplayName("getRegisteredFieldTypes should return all registered type keys")
    void getRegisteredFieldTypesShouldReturnAllKeys() {
        Set<String> registeredTypes = FieldAttributeProviderRegistry.getRegisteredFieldTypes();
        assertNotNull(registeredTypes);
        assertFalse(registeredTypes.isEmpty());
        assertTrue(registeredTypes.contains("text"));
        assertTrue(registeredTypes.contains("file"));
        assertTrue(registeredTypes.contains("date"));
    }

    @Test
    @DisplayName("getRegisteredFieldTypes should have exactly 3 registered providers")
    void getRegisteredFieldTypesShouldHaveThreeProviders() {
        Set<String> registeredTypes = FieldAttributeProviderRegistry.getRegisteredFieldTypes();
        assertEquals(3, registeredTypes.size());
    }

    @Test
    @DisplayName("Default attributes for unknown type should not include type-specific keys")
    void defaultAttributesShouldNotIncludeTypeSpecificKeys() {
        Map<String, Object> attrs = FieldAttributeProviderRegistry.getDefaultAttributes("SomethingRandom");
        assertNull(attrs.get("trim"));
        assertNull(attrs.get("dateFormat"));
        assertNull(attrs.get("multipleFileUpload"));
        assertNull(attrs.get("fileTypes"));
    }
}
