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
 * Tests for {@link Version} and its inner {@link Version.VersionBuilder}.
 *
 * Covers: VersionBuilder (no-arg constructor, (int,String) constructor,
 *         withNumber, withMessage, build()), static builder(),
 *         getters/setters on Version.
 */
@DisplayName("Version DTO")
class VersionTest {

    // ══════════════════════════════════════════════════════════════════════
    //  Static factory: builder()
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("builder() returns a non-null VersionBuilder")
    void builder_returnsNonNullBuilder() {
        assertNotNull(Version.builder());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  VersionBuilder tests
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("VersionBuilder")
    class VersionBuilderTest {

        @Test
        @DisplayName("no-arg constructor creates builder with defaults (number=0, message=null)")
        void noArgConstructor_defaults() {
            Version version = new Version.VersionBuilder().build();

            assertNotNull(version);
            assertEquals(0, version.getNumber());
            assertNull(version.getMessage());
        }

        @Test
        @DisplayName("(int, String) constructor creates builder with given values")
        void parameterizedConstructor_setsValues() {
            Version version = new Version.VersionBuilder(3, "third version").build();

            assertEquals(3, version.getNumber());
            assertEquals("third version", version.getMessage());
        }

        @Test
        @DisplayName("withNumber sets the version number")
        void withNumber_setsNumber() {
            Version version = Version.builder().withNumber(5).build();

            assertEquals(5, version.getNumber());
        }

        @Test
        @DisplayName("withMessage sets the version message")
        void withMessage_setsMessage() {
            Version version = Version.builder().withMessage("update content").build();

            assertEquals("update content", version.getMessage());
        }

        @Test
        @DisplayName("build with all fields populates Version correctly")
        void build_withAllFields() {
            Version version = Version.builder()
                    .withNumber(2)
                    .withMessage("second revision")
                    .build();

            assertEquals(2, version.getNumber());
            assertEquals("second revision", version.getMessage());
        }

        @Test
        @DisplayName("builder setters return the same builder instance (fluent API)")
        void builderSetters_returnSameInstance() {
            Version.VersionBuilder builder = Version.builder();

            assertSame(builder, builder.withNumber(1));
            assertSame(builder, builder.withMessage("msg"));
        }

        @Test
        @DisplayName("(int, String) constructor values can be overridden by fluent methods")
        void parameterizedConstructor_overriddenByFluentMethods() {
            Version version = new Version.VersionBuilder(1, "original")
                    .withNumber(10)
                    .withMessage("overridden")
                    .build();

            assertEquals(10, version.getNumber());
            assertEquals("overridden", version.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Setter tests on Version
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("setNumber updates the number field")
    void setNumber_updatesNumber() {
        Version version = Version.builder().withNumber(1).build();
        version.setNumber(99);

        assertEquals(99, version.getNumber());
    }

    @Test
    @DisplayName("setMessage updates the message field")
    void setMessage_updatesMessage() {
        Version version = Version.builder().withMessage("old").build();
        version.setMessage("new message");

        assertEquals("new message", version.getMessage());
    }

    @Test
    @DisplayName("setMessage to null clears the message")
    void setMessage_null_clearsMessage() {
        Version version = Version.builder().withMessage("something").build();
        version.setMessage(null);

        assertNull(version.getMessage());
    }

    @Test
    @DisplayName("setNumber to zero resets number")
    void setNumber_toZero() {
        Version version = Version.builder().withNumber(5).build();
        version.setNumber(0);

        assertEquals(0, version.getNumber());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Getter round-trip
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("getNumber returns the value set via builder")
    void getNumber_returnsBuilderValue() {
        Version version = Version.builder().withNumber(7).build();

        assertEquals(7, version.getNumber());
    }

    @Test
    @DisplayName("getMessage returns the value set via builder")
    void getMessage_returnsBuilderValue() {
        Version version = Version.builder().withMessage("init").build();

        assertEquals("init", version.getMessage());
    }
}
