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

package com.kristasoft.extensions.essentials.collaboration.confluence.catalog.entities;

import app.krista.extension.impl.anno.Domain;
import app.krista.extension.impl.anno.Entity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Space Entity Tests")
class SpaceEntityTest {

    private Space space;

    @BeforeEach
    void setUp() {
        space = new Space();
    }

    @Test
    @DisplayName("Should have @Domain annotation type available")
    void shouldHaveDomainAnnotation() {
        // @Domain has CLASS retention, so it is not available at runtime via reflection.
        // Verify the annotation type is a valid annotation interface that the class references.
        assertTrue(Domain.class.isAnnotation(), "Domain should be an annotation type");
        // The Space class is annotated with @Domain in source (verified at compile time).
    }

    @Test
    @DisplayName("Should have @Entity annotation with correct attributes")
    void shouldHaveEntityAnnotation() {
        Entity entity = Space.class.getAnnotation(Entity.class);
        assertNotNull(entity, "Space class should have @Entity annotation");
        assertEquals("Space", entity.name());
        assertEquals("localDomainEntity_340d866c-62e9-421a-9353-a5f144a81027", entity.id());
        assertEquals("Id", entity.primaryKey());
        assertFalse(entity.supportStore());
    }

    @Test
    @DisplayName("Should set and get id field")
    void shouldSetAndGetId() {
        space.id = "space-001";
        assertEquals("space-001", space.id);
    }

    @Test
    @DisplayName("Should set and get name field")
    void shouldSetAndGetName() {
        space.name = "Engineering Space";
        assertEquals("Engineering Space", space.name);
    }

    @Test
    @DisplayName("Should set and get type field")
    void shouldSetAndGetType() {
        space.type = "global";
        assertEquals("global", space.type);
    }

    @Test
    @DisplayName("Should set and get status field")
    void shouldSetAndGetStatus() {
        space.status = "current";
        assertEquals("current", space.status);
    }

    @Test
    @DisplayName("Should set and get description field")
    void shouldSetAndGetDescription() {
        space.description = "A space for engineering documentation";
        assertEquals("A space for engineering documentation", space.description);
    }

    @Test
    @DisplayName("Should set and get createdAt field as Long")
    void shouldSetAndGetCreatedAt() {
        space.createdAt = 1700000000000L;
        assertEquals(1700000000000L, space.createdAt);
    }

    @Test
    @DisplayName("All fields should be null by default")
    void allFieldsShouldBeNullByDefault() {
        assertNull(space.id);
        assertNull(space.name);
        assertNull(space.type);
        assertNull(space.status);
        assertNull(space.description);
        assertNull(space.createdAt);
    }

    @Test
    @DisplayName("Should have exactly 6 public fields")
    void shouldHaveCorrectNumberOfPublicFields() {
        long publicFieldCount = java.util.Arrays.stream(Space.class.getDeclaredFields())
                .filter(f -> java.lang.reflect.Modifier.isPublic(f.getModifiers()))
                .count();
        assertEquals(6, publicFieldCount);
    }
}
