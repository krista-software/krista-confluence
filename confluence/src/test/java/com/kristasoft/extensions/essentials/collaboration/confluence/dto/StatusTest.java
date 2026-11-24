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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link Status} enum.
 *
 * Covers: valueOf, toString(), values(), and enum constant verification.
 */
@DisplayName("Status Enum")
class StatusTest {

    // ── values() ──

    @Test
    @DisplayName("values() returns exactly two constants")
    void values_returnsTwoConstants() {
        Status[] values = Status.values();

        assertEquals(2, values.length, "Status enum must have exactly 2 constants");
    }

    @Test
    @DisplayName("values() contains draft and current")
    void values_containsDraftAndCurrent() {
        Status[] values = Status.values();

        assertEquals(Status.draft, values[0]);
        assertEquals(Status.current, values[1]);
    }

    // ── valueOf ──

    @Test
    @DisplayName("valueOf('draft') returns Status.draft")
    void valueOf_draft() {
        assertEquals(Status.draft, Status.valueOf("draft"));
    }

    @Test
    @DisplayName("valueOf('current') returns Status.current")
    void valueOf_current() {
        assertEquals(Status.current, Status.valueOf("current"));
    }

    @Test
    @DisplayName("valueOf with invalid name throws IllegalArgumentException")
    void valueOf_invalidName_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> Status.valueOf("archived"));
    }

    // ── toString() ──

    @Test
    @DisplayName("draft.toString() returns 'draft'")
    void draft_toString_returnsDraft() {
        assertEquals("draft", Status.draft.toString());
    }

    @Test
    @DisplayName("current.toString() returns 'current'")
    void current_toString_returnsCurrent() {
        assertEquals("current", Status.current.toString());
    }

    // ── Constructor lowercases the value ──

    @Test
    @DisplayName("toString returns lowercase value")
    void toString_returnsLowercaseValue() {
        // Both enum constants have lowercase string values
        for (Status status : Status.values()) {
            String result = status.toString();
            assertEquals(result.toLowerCase(), result,
                    "toString must return a lowercase string for " + status.name());
        }
    }
}
