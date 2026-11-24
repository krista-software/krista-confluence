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

@DisplayName("DateFieldAttributeProvider Tests")
class DateFieldAttributeProviderTest {

    private DateFieldAttributeProvider provider;

    @BeforeEach
    void setUp() {
        provider = new DateFieldAttributeProvider();
    }

    @Test
    @DisplayName("getFieldType should return 'date'")
    void getFieldTypeShouldReturnDate() {
        assertEquals("date", provider.getFieldType());
    }

    @Test
    @DisplayName("getDefaultAttributes should return map with visualWidth")
    void getDefaultAttributesShouldContainVisualWidth() {
        Map<String, Object> attrs = provider.getDefaultAttributes();
        assertNotNull(attrs);
        assertEquals("S", attrs.get("visualWidth"));
    }

    @Test
    @DisplayName("getDefaultAttributes should return map with dateFormat")
    void getDefaultAttributesShouldContainDateFormat() {
        Map<String, Object> attrs = provider.getDefaultAttributes();
        assertEquals("MM/dd/yyyy", attrs.get("dateFormat"));
    }

    @Test
    @DisplayName("getDefaultAttributes should return map with showCalendar set to true")
    void getDefaultAttributesShouldContainShowCalendar() {
        Map<String, Object> attrs = provider.getDefaultAttributes();
        assertEquals(true, attrs.get("showCalendar"));
    }

    @Test
    @DisplayName("getDefaultAttributes should return map with allowPastDates set to true")
    void getDefaultAttributesShouldContainAllowPastDates() {
        Map<String, Object> attrs = provider.getDefaultAttributes();
        assertEquals(true, attrs.get("allowPastDates"));
    }

    @Test
    @DisplayName("getDefaultAttributes should return map with allowFutureDates set to true")
    void getDefaultAttributesShouldContainAllowFutureDates() {
        Map<String, Object> attrs = provider.getDefaultAttributes();
        assertEquals(true, attrs.get("allowFutureDates"));
    }

    @Test
    @DisplayName("getDefaultAttributes should return exactly 5 entries")
    void getDefaultAttributesShouldHaveFiveEntries() {
        Map<String, Object> attrs = provider.getDefaultAttributes();
        assertEquals(5, attrs.size());
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
