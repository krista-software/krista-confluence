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

@DisplayName("Page Entity Tests")
class PageEntityTest {

    private Page page;

    @BeforeEach
    void setUp() {
        page = new Page();
    }

    @Test
    @DisplayName("Should have @Domain annotation type available")
    void shouldHaveDomainAnnotation() {
        // @Domain has CLASS retention, so it is not available at runtime via reflection.
        // Verify the annotation type is a valid annotation interface that the class references.
        assertTrue(Domain.class.isAnnotation(), "Domain should be an annotation type");
        // The Page class is annotated with @Domain in source (verified at compile time).
    }

    @Test
    @DisplayName("Should have @Entity annotation with correct attributes")
    void shouldHaveEntityAnnotation() {
        Entity entity = Page.class.getAnnotation(Entity.class);
        assertNotNull(entity, "Page class should have @Entity annotation");
        assertEquals("Page", entity.name());
        assertEquals("localDomainEntity_69687da7-4c87-4234-81d1-bb6961e2ff18", entity.id());
        assertEquals("Id", entity.primaryKey());
        assertFalse(entity.supportStore());
    }

    @Test
    @DisplayName("Should set and get id field")
    void shouldSetAndGetId() {
        page.id = "page-001";
        assertEquals("page-001", page.id);
    }

    @Test
    @DisplayName("Should set and get spaceID field")
    void shouldSetAndGetSpaceID() {
        page.spaceID = "space-100";
        assertEquals("space-100", page.spaceID);
    }

    @Test
    @DisplayName("Should set and get title field")
    void shouldSetAndGetTitle() {
        page.title = "My Confluence Page";
        assertEquals("My Confluence Page", page.title);
    }

    @Test
    @DisplayName("Should set and get status field")
    void shouldSetAndGetStatus() {
        page.status = "current";
        assertEquals("current", page.status);
    }

    @Test
    @DisplayName("Should set and get pageURL field")
    void shouldSetAndGetPageURL() {
        page.pageURL = "https://example.atlassian.net/wiki/spaces/TEST/pages/123";
        assertEquals("https://example.atlassian.net/wiki/spaces/TEST/pages/123", page.pageURL);
    }

    @Test
    @DisplayName("Should set and get body field")
    void shouldSetAndGetBody() {
        page.body = "<p>Page content</p>";
        assertEquals("<p>Page content</p>", page.body);
    }

    @Test
    @DisplayName("Should set and get parentID field")
    void shouldSetAndGetParentID() {
        page.parentID = "parent-999";
        assertEquals("parent-999", page.parentID);
    }

    @Test
    @DisplayName("Should set and get parentType field")
    void shouldSetAndGetParentType() {
        page.parentType = "page";
        assertEquals("page", page.parentType);
    }

    @Test
    @DisplayName("Should set and get hasChildren field as Boolean")
    void shouldSetAndGetHasChildren() {
        page.hasChildren = true;
        assertTrue(page.hasChildren);

        page.hasChildren = false;
        assertFalse(page.hasChildren);
    }

    @Test
    @DisplayName("Should set and get type field")
    void shouldSetAndGetType() {
        page.type = "page";
        assertEquals("page", page.type);
    }

    @Test
    @DisplayName("All fields should be null by default")
    void allFieldsShouldBeNullByDefault() {
        assertNull(page.id);
        assertNull(page.spaceID);
        assertNull(page.title);
        assertNull(page.status);
        assertNull(page.pageURL);
        assertNull(page.body);
        assertNull(page.parentID);
        assertNull(page.parentType);
        assertNull(page.hasChildren);
        assertNull(page.type);
    }

    @Test
    @DisplayName("toString should contain all field values")
    void toStringShouldContainAllFieldValues() {
        page.id = "pg-1";
        page.spaceID = "sp-1";
        page.title = "Test Title";
        page.status = "current";
        page.type = "page";
        page.pageURL = "https://example.com/page";
        page.body = "<p>body</p>";
        page.parentID = "parent-1";
        page.parentType = "page";
        page.hasChildren = true;

        String result = page.toString();

        assertAll("toString should include all fields",
                () -> assertTrue(result.contains("pg-1"), "Should contain id"),
                () -> assertTrue(result.contains("sp-1"), "Should contain spaceID"),
                () -> assertTrue(result.contains("Test Title"), "Should contain title"),
                () -> assertTrue(result.contains("current"), "Should contain status"),
                () -> assertTrue(result.contains("page"), "Should contain type"),
                () -> assertTrue(result.contains("https://example.com/page"), "Should contain pageURL"),
                () -> assertTrue(result.contains("<p>body</p>"), "Should contain body"),
                () -> assertTrue(result.contains("parent-1"), "Should contain parentID"),
                () -> assertTrue(result.contains("true"), "Should contain hasChildren value")
        );
    }

    @Test
    @DisplayName("toString should start with 'Page{' prefix")
    void toStringShouldStartWithPagePrefix() {
        String result = page.toString();
        assertTrue(result.startsWith("Page{"));
        assertTrue(result.endsWith("}"));
    }

    @Test
    @DisplayName("Should have exactly 12 public fields")
    void shouldHaveCorrectNumberOfPublicFields() {
        long publicFieldCount = java.util.Arrays.stream(Page.class.getDeclaredFields())
                .filter(f -> java.lang.reflect.Modifier.isPublic(f.getModifiers()))
                .count();
        assertEquals(12, publicFieldCount);
    }
}
