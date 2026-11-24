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
 * Tests for {@link ProfilePicture}.
 *
 * Covers: no-arg constructor, getters/setters for path, width, height, isDefault, toString().
 */
@DisplayName("ProfilePicture DTO")
class ProfilePictureTest {

    // ── Constructor ──

    @Test
    @DisplayName("no-arg constructor creates instance with all null fields")
    void noArgConstructor_allFieldsNull() {
        ProfilePicture pp = new ProfilePicture();

        assertNotNull(pp);
        assertNull(pp.getPath());
        assertNull(pp.getWidth());
        assertNull(pp.getHeight());
        assertNull(pp.getIsDefault());
    }

    // ── path ──

    @Test
    @DisplayName("setPath / getPath round-trip")
    void setPath_getPath_roundTrip() {
        ProfilePicture pp = new ProfilePicture();
        pp.setPath("/wiki/aa-avatar/default.png");

        assertEquals("/wiki/aa-avatar/default.png", pp.getPath());
    }

    @Test
    @DisplayName("setPath with null clears path")
    void setPath_null_clearsPath() {
        ProfilePicture pp = new ProfilePicture();
        pp.setPath("/some/path");
        pp.setPath(null);

        assertNull(pp.getPath());
    }

    // ── width ──

    @Test
    @DisplayName("setWidth / getWidth round-trip")
    void setWidth_getWidth_roundTrip() {
        ProfilePicture pp = new ProfilePicture();
        pp.setWidth(48);

        assertEquals(48, pp.getWidth());
    }

    @Test
    @DisplayName("setWidth with null clears width")
    void setWidth_null_clearsWidth() {
        ProfilePicture pp = new ProfilePicture();
        pp.setWidth(100);
        pp.setWidth(null);

        assertNull(pp.getWidth());
    }

    // ── height ──

    @Test
    @DisplayName("setHeight / getHeight round-trip")
    void setHeight_getHeight_roundTrip() {
        ProfilePicture pp = new ProfilePicture();
        pp.setHeight(48);

        assertEquals(48, pp.getHeight());
    }

    @Test
    @DisplayName("setHeight with null clears height")
    void setHeight_null_clearsHeight() {
        ProfilePicture pp = new ProfilePicture();
        pp.setHeight(200);
        pp.setHeight(null);

        assertNull(pp.getHeight());
    }

    // ── isDefault ──

    @Test
    @DisplayName("setIsDefault / getIsDefault round-trip with true")
    void setIsDefault_true_roundTrip() {
        ProfilePicture pp = new ProfilePicture();
        pp.setIsDefault(true);

        assertTrue(pp.getIsDefault());
    }

    @Test
    @DisplayName("setIsDefault / getIsDefault round-trip with false")
    void setIsDefault_false_roundTrip() {
        ProfilePicture pp = new ProfilePicture();
        pp.setIsDefault(false);

        assertFalse(pp.getIsDefault());
    }

    @Test
    @DisplayName("setIsDefault with null clears isDefault")
    void setIsDefault_null_clearsIsDefault() {
        ProfilePicture pp = new ProfilePicture();
        pp.setIsDefault(true);
        pp.setIsDefault(null);

        assertNull(pp.getIsDefault());
    }

    // ── toString ──

    @Test
    @DisplayName("toString with all fields set contains every value")
    void toString_withAllFieldsSet() {
        ProfilePicture pp = new ProfilePicture();
        pp.setPath("/avatar.png");
        pp.setWidth(48);
        pp.setHeight(48);
        pp.setIsDefault(false);

        String result = pp.toString();

        assertNotNull(result);
        assertTrue(result.startsWith("ProfilePicture{"));
        assertTrue(result.contains("path='/avatar.png'"));
        assertTrue(result.contains("width=48"));
        assertTrue(result.contains("height=48"));
        assertTrue(result.contains("isDefault=false"));
        assertTrue(result.endsWith("}"));
    }

    @Test
    @DisplayName("toString with all null fields contains null literals")
    void toString_withAllNullFields() {
        ProfilePicture pp = new ProfilePicture();

        String result = pp.toString();

        assertNotNull(result);
        assertTrue(result.startsWith("ProfilePicture{"));
        assertTrue(result.contains("null"));
        assertTrue(result.endsWith("}"));
    }
}
