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
 * Tests for {@link Variable} and its inner {@link Variable.PlaceHolderBuilder}.
 *
 * Covers: PlaceHolderBuilder (placeHolderKey, placeHolderValue, build()),
 *         Variable static builder(), getters.
 */
@DisplayName("Variable DTO")
class VariableTest {

    // ══════════════════════════════════════════════════════════════════════
    //  Static factory: builder()
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("builder() returns a non-null PlaceHolderBuilder")
    void builder_returnsNonNullBuilder() {
        assertNotNull(Variable.builder());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  PlaceHolderBuilder tests
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("PlaceHolderBuilder")
    class PlaceHolderBuilderTest {

        @Test
        @DisplayName("build with all fields populates Variable correctly")
        void build_withAllFields() {
            Variable variable = Variable.builder()
                    .placeHolderKey("{{name}}")
                    .placeHolderValue("John Doe")
                    .build();

            assertNotNull(variable);
            assertEquals("{{name}}", variable.getPlaceHolderKey());
            assertEquals("John Doe", variable.getPlaceHolderValue());
        }

        @Test
        @DisplayName("build with no fields leaves all values null")
        void build_withNoFields_allNull() {
            Variable variable = Variable.builder().build();

            assertNotNull(variable);
            assertNull(variable.getPlaceHolderKey());
            assertNull(variable.getPlaceHolderValue());
        }

        @Test
        @DisplayName("builder setters return the same builder instance (fluent API)")
        void builderSetters_returnSameInstance() {
            Variable.PlaceHolderBuilder builder = Variable.builder();

            assertSame(builder, builder.placeHolderKey("key"));
            assertSame(builder, builder.placeHolderValue("value"));
        }

        @Test
        @DisplayName("no-arg PlaceHolderBuilder constructor creates a usable builder")
        void noArgConstructor_createsUsableBuilder() {
            Variable.PlaceHolderBuilder builder = new Variable.PlaceHolderBuilder();
            Variable variable = builder.placeHolderKey("k").placeHolderValue("v").build();

            assertEquals("k", variable.getPlaceHolderKey());
            assertEquals("v", variable.getPlaceHolderValue());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Getter tests
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("getPlaceHolderKey returns the key set via builder")
    void getPlaceHolderKey_returnsKey() {
        Variable variable = Variable.builder()
                .placeHolderKey("{{title}}")
                .build();

        assertEquals("{{title}}", variable.getPlaceHolderKey());
    }

    @Test
    @DisplayName("getPlaceHolderValue returns the value set via builder")
    void getPlaceHolderValue_returnsValue() {
        Variable variable = Variable.builder()
                .placeHolderValue("My Page Title")
                .build();

        assertEquals("My Page Title", variable.getPlaceHolderValue());
    }

    @Test
    @DisplayName("getPlaceHolderKey returns null when not set")
    void getPlaceHolderKey_nullWhenNotSet() {
        Variable variable = Variable.builder().build();

        assertNull(variable.getPlaceHolderKey());
    }

    @Test
    @DisplayName("getPlaceHolderValue returns null when not set")
    void getPlaceHolderValue_nullWhenNotSet() {
        Variable variable = Variable.builder().build();

        assertNull(variable.getPlaceHolderValue());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Multiple builds from same builder
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("building multiple Variables from the same builder produces independent instances")
    void multipleBuild_producesIndependentInstances() {
        Variable.PlaceHolderBuilder builder = Variable.builder()
                .placeHolderKey("key1")
                .placeHolderValue("value1");

        Variable v1 = builder.build();

        builder.placeHolderKey("key2").placeHolderValue("value2");
        Variable v2 = builder.build();

        assertEquals("key1", v1.getPlaceHolderKey());
        assertEquals("value1", v1.getPlaceHolderValue());
        assertEquals("key2", v2.getPlaceHolderKey());
        assertEquals("value2", v2.getPlaceHolderValue());
    }
}
