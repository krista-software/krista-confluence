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
 * Tests for {@link Body}.
 *
 * Covers: static factory setBody(Storage), getStorage(), setStorage(), toString().
 */
@DisplayName("Body DTO")
class BodyTest {

    // ── static factory: setBody(Storage) ──

    @Test
    @DisplayName("setBody creates Body with the given Storage")
    void setBody_createsBodyWithGivenStorage() {
        Storage storage = Storage.builder()
                .setValue("<p>Hello</p>")
                .setRepresentation("storage")
                .build();

        Body body = Body.setBody(storage);

        assertNotNull(body, "Body instance must not be null");
        assertSame(storage, body.getStorage(), "getStorage must return the Storage passed to setBody");
    }

    @Test
    @DisplayName("setBody with null Storage creates Body with null storage")
    void setBody_withNull_createsBodyWithNullStorage() {
        Body body = Body.setBody(null);

        assertNotNull(body, "Body instance must not be null even when storage is null");
        assertNull(body.getStorage(), "getStorage must return null when constructed with null");
    }

    // ── getStorage / setStorage ──

    @Test
    @DisplayName("setStorage replaces the current storage and returns this")
    void setStorage_replacesStorageAndReturnsThis() {
        Storage original = Storage.builder().setValue("original").build();
        Storage replacement = Storage.builder().setValue("replacement").build();

        Body body = Body.setBody(original);
        Body returned = body.setStorage(replacement);

        assertSame(body, returned, "setStorage must return the same Body instance (fluent API)");
        assertSame(replacement, body.getStorage(), "getStorage must return the replacement storage");
    }

    @Test
    @DisplayName("setStorage to null clears the storage")
    void setStorage_toNull_clearsStorage() {
        Storage original = Storage.builder().setValue("content").build();
        Body body = Body.setBody(original);

        body.setStorage(null);

        assertNull(body.getStorage(), "getStorage must return null after setStorage(null)");
    }

    // ── toString ──

    @Test
    @DisplayName("toString includes storage details when storage is set")
    void toString_withStorage_containsStorageInfo() {
        Storage storage = Storage.builder()
                .setValue("<p>test</p>")
                .setRepresentation("storage")
                .build();
        Body body = Body.setBody(storage);

        String result = body.toString();

        assertNotNull(result);
        assertTrue(result.startsWith("Body{"), "must start with 'Body{'");
        assertTrue(result.contains("storage="), "must contain 'storage='");
        assertTrue(result.endsWith("}"), "must end with '}'");
    }

    @Test
    @DisplayName("toString with null storage contains null literal")
    void toString_withNullStorage_containsNull() {
        Body body = Body.setBody(null);

        String result = body.toString();

        assertNotNull(result);
        assertTrue(result.startsWith("Body{"), "must start with 'Body{'");
        assertTrue(result.contains("null"), "must contain 'null' when storage is null");
        assertTrue(result.endsWith("}"), "must end with '}'");
    }
}
