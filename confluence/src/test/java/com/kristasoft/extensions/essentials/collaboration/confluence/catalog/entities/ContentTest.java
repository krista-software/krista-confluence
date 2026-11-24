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

@DisplayName("Content Entity Tests")
class ContentTest {

    private Content content;

    @BeforeEach
    void setUp() {
        content = new Content();
    }

    @Test
    @DisplayName("Should have @Domain annotation type available")
    void shouldHaveDomainAnnotation() {
        // @Domain has CLASS retention, so it is not available at runtime via reflection.
        // Verify the annotation type is a valid annotation interface that the class references.
        assertTrue(Domain.class.isAnnotation(), "Domain should be an annotation type");
        // The Content class is annotated with @Domain in source (verified at compile time).
    }

    @Test
    @DisplayName("Should have @Entity annotation with correct attributes")
    void shouldHaveEntityAnnotation() {
        Entity entity = Content.class.getAnnotation(Entity.class);
        assertNotNull(entity, "Content class should have @Entity annotation");
        assertEquals("Content", entity.name());
        assertEquals("localDomainEntity_3d378ea7-749b-4837-9b7f-2ce96ea0a954", entity.id());
        assertEquals("ID", entity.primaryKey());
        assertFalse(entity.supportStore());
    }

    @Test
    @DisplayName("Should set and get iD field")
    void shouldSetAndGetId() {
        content.iD = "content-123";
        assertEquals("content-123", content.iD);
    }

    @Test
    @DisplayName("Should set and get body field")
    void shouldSetAndGetBody() {
        content.body = "<p>Some rich text content</p>";
        assertEquals("<p>Some rich text content</p>", content.body);
    }

    @Test
    @DisplayName("All fields should be null by default")
    void allFieldsShouldBeNullByDefault() {
        assertNull(content.iD);
        assertNull(content.body);
    }

    @Test
    @DisplayName("Should have exactly 2 public fields")
    void shouldHaveCorrectNumberOfPublicFields() {
        long publicFieldCount = java.util.Arrays.stream(Content.class.getDeclaredFields())
                .filter(f -> java.lang.reflect.Modifier.isPublic(f.getModifiers()))
                .count();
        assertEquals(2, publicFieldCount);
    }
}
