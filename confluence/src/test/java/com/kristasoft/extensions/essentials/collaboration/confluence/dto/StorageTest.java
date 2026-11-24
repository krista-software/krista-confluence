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

package com.kristasoft.extensions.essentials.collaboration.confluence.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link Storage} and its inner {@link Storage.StorageBuilder}.
 *
 * Covers: StorageBuilder (setValue, setRepresentation, build), static builder(),
 *         getters/setters on Storage, toString().
 */
@DisplayName("Storage DTO")
class StorageTest {

    // ══════════════════════════════════════════════════════════════════════
    //  Static factory: builder()
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("builder() returns a non-null StorageBuilder")
    void builder_returnsNonNullBuilder() {
        assertNotNull(Storage.builder());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  StorageBuilder tests
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("StorageBuilder")
    class StorageBuilderTest {

        @Test
        @DisplayName("build with all fields populates Storage correctly")
        void build_withAllFields() {
            Storage storage = Storage.builder()
                    .setValue("<p>Hello World</p>")
                    .setRepresentation("storage")
                    .build();

            assertNotNull(storage);
            assertEquals("<p>Hello World</p>", storage.getValue());
            assertEquals("storage", storage.getRepresentation());
        }

        @Test
        @DisplayName("build with no fields leaves all values null")
        void build_withNoFields_allNull() {
            Storage storage = Storage.builder().build();

            assertNotNull(storage);
            assertNull(storage.getValue());
            assertNull(storage.getRepresentation());
        }

        @Test
        @DisplayName("builder setters return the same builder instance (fluent API)")
        void builderSetters_returnSameInstance() {
            Storage.StorageBuilder builder = Storage.builder();

            assertSame(builder, builder.setValue("v"));
            assertSame(builder, builder.setRepresentation("r"));
        }

        @Test
        @DisplayName("no-arg StorageBuilder constructor creates a usable builder")
        void noArgConstructor_createsUsableBuilder() {
            Storage.StorageBuilder builder = new Storage.StorageBuilder();
            Storage storage = builder.setValue("test").setRepresentation("wiki").build();

            assertEquals("test", storage.getValue());
            assertEquals("wiki", storage.getRepresentation());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Setter tests on Storage
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("setValue updates the value field")
    void setValue_updatesValue() {
        Storage storage = Storage.builder().setValue("old").build();
        storage.setValue("new");

        assertEquals("new", storage.getValue());
    }

    @Test
    @DisplayName("setRepresentation updates the representation field")
    void setRepresentation_updatesRepresentation() {
        Storage storage = Storage.builder().setRepresentation("old").build();
        storage.setRepresentation("atlas_doc_format");

        assertEquals("atlas_doc_format", storage.getRepresentation());
    }

    @Test
    @DisplayName("setValue to null clears the value")
    void setValue_null_clearsValue() {
        Storage storage = Storage.builder().setValue("content").build();
        storage.setValue(null);

        assertNull(storage.getValue());
    }

    @Test
    @DisplayName("setRepresentation to null clears the representation")
    void setRepresentation_null_clearsRepresentation() {
        Storage storage = Storage.builder().setRepresentation("storage").build();
        storage.setRepresentation(null);

        assertNull(storage.getRepresentation());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  toString
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("toString with populated fields contains value and representation")
    void toString_withPopulatedFields() {
        Storage storage = Storage.builder()
                .setValue("<p>content</p>")
                .setRepresentation("storage")
                .build();

        String result = storage.toString();

        assertNotNull(result);
        assertTrue(result.startsWith("Storage{"));
        assertTrue(result.contains("value='<p>content</p>'"));
        assertTrue(result.contains("representation='storage'"));
        assertTrue(result.endsWith("}"));
    }

    @Test
    @DisplayName("toString with null fields contains null literals")
    void toString_withNullFields() {
        Storage storage = Storage.builder().build();

        String result = storage.toString();

        assertNotNull(result);
        assertTrue(result.startsWith("Storage{"));
        assertTrue(result.contains("null"));
        assertTrue(result.endsWith("}"));
    }
}
