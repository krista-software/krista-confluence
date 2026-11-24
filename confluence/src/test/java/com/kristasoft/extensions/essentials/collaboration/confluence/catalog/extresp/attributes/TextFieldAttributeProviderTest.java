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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TextFieldAttributeProvider Tests")
class TextFieldAttributeProviderTest {

    private TextFieldAttributeProvider provider;

    @BeforeEach
    void setUp() {
        provider = new TextFieldAttributeProvider();
    }

    @Test
    @DisplayName("getFieldType should return 'text'")
    void getFieldTypeShouldReturnText() {
        assertEquals("text", provider.getFieldType());
    }

    @Test
    @DisplayName("getDefaultAttributes should return map with visualWidth")
    void getDefaultAttributesShouldContainVisualWidth() {
        Map<String, Object> attrs = provider.getDefaultAttributes();
        assertNotNull(attrs);
        assertEquals("S", attrs.get("visualWidth"));
    }

    @Test
    @DisplayName("getDefaultAttributes should return map with trim set to true")
    void getDefaultAttributesShouldContainTrim() {
        Map<String, Object> attrs = provider.getDefaultAttributes();
        assertNotNull(attrs);
        assertEquals(true, attrs.get("trim"));
    }

    @Test
    @DisplayName("getDefaultAttributes should return exactly 2 entries")
    void getDefaultAttributesShouldHaveTwoEntries() {
        Map<String, Object> attrs = provider.getDefaultAttributes();
        assertEquals(2, attrs.size());
    }

    @Test
    @DisplayName("Should implement FieldAttributeProvider interface")
    void shouldImplementFieldAttributeProvider() {
        assertInstanceOf(FieldAttributeProvider.class, provider);
    }

    @Test
    @DisplayName("getDefaultAttributes should return a new map each time")
    void getDefaultAttributesShouldReturnNewMapEachTime() {
        Map<String, Object> attrs1 = provider.getDefaultAttributes();
        Map<String, Object> attrs2 = provider.getDefaultAttributes();
        assertNotSame(attrs1, attrs2, "Each call should return a new map instance");
        assertEquals(attrs1, attrs2, "Maps should have equal content");
    }
}
