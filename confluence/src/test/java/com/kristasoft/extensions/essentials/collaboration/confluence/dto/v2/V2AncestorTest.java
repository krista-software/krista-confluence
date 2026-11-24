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
 * Tests for {@link V2Ancestor}.
 *
 * Covers: no-arg constructor, (id, type) constructor, getters/setters for id and type, toString().
 */
@DisplayName("V2Ancestor DTO")
class V2AncestorTest {

    // ── Constructors ──

    @Test
    @DisplayName("no-arg constructor creates instance with all null fields")
    void noArgConstructor_allFieldsNull() {
        V2Ancestor ancestor = new V2Ancestor();

        assertNotNull(ancestor);
        assertNull(ancestor.getId());
        assertNull(ancestor.getType());
    }

    @Test
    @DisplayName("(id, type) constructor sets both fields")
    void parameterizedConstructor_setsBothFields() {
        V2Ancestor ancestor = new V2Ancestor("123456", "page");

        assertEquals("123456", ancestor.getId());
        assertEquals("page", ancestor.getType());
    }

    @Test
    @DisplayName("(id, type) constructor with null values")
    void parameterizedConstructor_withNullValues() {
        V2Ancestor ancestor = new V2Ancestor(null, null);

        assertNull(ancestor.getId());
        assertNull(ancestor.getType());
    }

    // ── Getters/Setters ──

    @Test
    @DisplayName("setId / getId round-trip")
    void setId_getId_roundTrip() {
        V2Ancestor ancestor = new V2Ancestor();
        ancestor.setId("page-42");

        assertEquals("page-42", ancestor.getId());
    }

    @Test
    @DisplayName("setType / getType round-trip")
    void setType_getType_roundTrip() {
        V2Ancestor ancestor = new V2Ancestor();
        ancestor.setType("folder");

        assertEquals("folder", ancestor.getType());
    }

    @Test
    @DisplayName("setId overwrites constructor value")
    void setId_overwritesConstructorValue() {
        V2Ancestor ancestor = new V2Ancestor("old-id", "page");
        ancestor.setId("new-id");

        assertEquals("new-id", ancestor.getId());
    }

    @Test
    @DisplayName("setType overwrites constructor value")
    void setType_overwritesConstructorValue() {
        V2Ancestor ancestor = new V2Ancestor("123", "page");
        ancestor.setType("whiteboard");

        assertEquals("whiteboard", ancestor.getType());
    }

    @Test
    @DisplayName("setId to null clears id")
    void setId_null_clearsId() {
        V2Ancestor ancestor = new V2Ancestor("123", "page");
        ancestor.setId(null);

        assertNull(ancestor.getId());
    }

    @Test
    @DisplayName("setType to null clears type")
    void setType_null_clearsType() {
        V2Ancestor ancestor = new V2Ancestor("123", "page");
        ancestor.setType(null);

        assertNull(ancestor.getType());
    }

    // ── toString ──

    @Test
    @DisplayName("toString with populated fields contains id and type")
    void toString_withPopulatedFields() {
        V2Ancestor ancestor = new V2Ancestor("789", "page");

        String result = ancestor.toString();

        assertNotNull(result);
        assertTrue(result.startsWith("V2Ancestor{"));
        assertTrue(result.contains("id='789'"));
        assertTrue(result.contains("type='page'"));
        assertTrue(result.endsWith("}"));
    }

    @Test
    @DisplayName("toString with null fields contains null literals")
    void toString_withNullFields() {
        V2Ancestor ancestor = new V2Ancestor();

        String result = ancestor.toString();

        assertNotNull(result);
        assertTrue(result.startsWith("V2Ancestor{"));
        assertTrue(result.contains("null"));
        assertTrue(result.endsWith("}"));
    }

    @Test
    @DisplayName("toString from parameterized constructor matches expected format")
    void toString_matchesExpectedFormat() {
        V2Ancestor ancestor = new V2Ancestor("100", "database");

        String result = ancestor.toString();

        assertEquals("V2Ancestor{id='100', type='database'}", result);
    }
}
