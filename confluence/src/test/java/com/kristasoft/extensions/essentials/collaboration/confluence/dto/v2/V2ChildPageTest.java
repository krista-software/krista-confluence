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

package com.kristasoft.extensions.essentials.collaboration.confluence.dto.v2;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link V2ChildPage}.
 *
 * Covers: no-arg constructor, getters/setters for id, status, title, type,
 *         spaceId, childPosition, toString().
 */
@DisplayName("V2ChildPage DTO")
class V2ChildPageTest {

    // ── Constructor ──

    @Test
    @DisplayName("no-arg constructor creates instance with all null/default fields")
    void noArgConstructor_allFieldsNullOrDefault() {
        V2ChildPage child = new V2ChildPage();

        assertNotNull(child);
        assertNull(child.getId());
        assertNull(child.getStatus());
        assertNull(child.getTitle());
        assertNull(child.getType());
        assertNull(child.getSpaceId());
        assertNull(child.getChildPosition());
    }

    // ── id ──

    @Test
    @DisplayName("setId / getId round-trip")
    void setId_getId_roundTrip() {
        V2ChildPage child = new V2ChildPage();
        child.setId("child-123");

        assertEquals("child-123", child.getId());
    }

    @Test
    @DisplayName("setId to null clears id")
    void setId_null_clearsId() {
        V2ChildPage child = new V2ChildPage();
        child.setId("some-id");
        child.setId(null);

        assertNull(child.getId());
    }

    // ── status ──

    @Test
    @DisplayName("setStatus / getStatus round-trip")
    void setStatus_getStatus_roundTrip() {
        V2ChildPage child = new V2ChildPage();
        child.setStatus("current");

        assertEquals("current", child.getStatus());
    }

    // ── title ──

    @Test
    @DisplayName("setTitle / getTitle round-trip")
    void setTitle_getTitle_roundTrip() {
        V2ChildPage child = new V2ChildPage();
        child.setTitle("Child Page Title");

        assertEquals("Child Page Title", child.getTitle());
    }

    // ── type ──

    @Test
    @DisplayName("setType / getType round-trip")
    void setType_getType_roundTrip() {
        V2ChildPage child = new V2ChildPage();
        child.setType("page");

        assertEquals("page", child.getType());
    }

    @Test
    @DisplayName("setType with folder value")
    void setType_folder() {
        V2ChildPage child = new V2ChildPage();
        child.setType("folder");

        assertEquals("folder", child.getType());
    }

    // ── spaceId ──

    @Test
    @DisplayName("setSpaceId / getSpaceId round-trip")
    void setSpaceId_getSpaceId_roundTrip() {
        V2ChildPage child = new V2ChildPage();
        child.setSpaceId("space-456");

        assertEquals("space-456", child.getSpaceId());
    }

    // ── childPosition ──

    @Test
    @DisplayName("setChildPosition / getChildPosition round-trip")
    void setChildPosition_getChildPosition_roundTrip() {
        V2ChildPage child = new V2ChildPage();
        child.setChildPosition(238);

        assertEquals(238, child.getChildPosition());
    }

    @Test
    @DisplayName("setChildPosition with null clears childPosition")
    void setChildPosition_null_clearsChildPosition() {
        V2ChildPage child = new V2ChildPage();
        child.setChildPosition(100);
        child.setChildPosition(null);

        assertNull(child.getChildPosition());
    }

    @Test
    @DisplayName("setChildPosition with zero")
    void setChildPosition_zero() {
        V2ChildPage child = new V2ChildPage();
        child.setChildPosition(0);

        assertEquals(0, child.getChildPosition());
    }

    // ── toString ──

    @Test
    @DisplayName("toString with all fields populated contains all values")
    void toString_withAllFieldsPopulated() {
        V2ChildPage child = new V2ChildPage();
        child.setId("789");
        child.setStatus("current");
        child.setTitle("Test Child");
        child.setType("page");
        child.setSpaceId("space-10");
        child.setChildPosition(42);

        String result = child.toString();

        assertNotNull(result);
        assertTrue(result.startsWith("V2ChildPage{"));
        assertTrue(result.contains("id='789'"));
        assertTrue(result.contains("status='current'"));
        assertTrue(result.contains("title='Test Child'"));
        assertTrue(result.contains("type='page'"));
        assertTrue(result.contains("spaceId='space-10'"));
        assertTrue(result.contains("childPosition=42"));
        assertTrue(result.endsWith("}"));
    }

    @Test
    @DisplayName("toString with null fields contains null literals")
    void toString_withNullFields() {
        V2ChildPage child = new V2ChildPage();

        String result = child.toString();

        assertNotNull(result);
        assertTrue(result.startsWith("V2ChildPage{"));
        assertTrue(result.contains("null"));
        assertTrue(result.endsWith("}"));
    }

    // ── Setter overwrites ──

    @Test
    @DisplayName("setters overwrite previously set values")
    void setters_overwritePreviousValues() {
        V2ChildPage child = new V2ChildPage();
        child.setId("old-id");
        child.setTitle("Old Title");
        child.setChildPosition(1);

        child.setId("new-id");
        child.setTitle("New Title");
        child.setChildPosition(99);

        assertEquals("new-id", child.getId());
        assertEquals("New Title", child.getTitle());
        assertEquals(99, child.getChildPosition());
    }
}
