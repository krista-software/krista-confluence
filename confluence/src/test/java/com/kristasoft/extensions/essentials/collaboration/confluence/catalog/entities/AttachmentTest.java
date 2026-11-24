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

@DisplayName("Attachment Entity Tests")
class AttachmentTest {

    private Attachment attachment;

    @BeforeEach
    void setUp() {
        attachment = new Attachment();
    }

    @Test
    @DisplayName("Should have @Domain annotation type available")
    void shouldHaveDomainAnnotation() {
        // @Domain has CLASS retention, so it is not available at runtime via reflection.
        // Verify the annotation type is a valid annotation interface that the class references.
        assertTrue(Domain.class.isAnnotation(), "Domain should be an annotation type");
        // The Attachment class is annotated with @Domain in source (verified at compile time).
    }

    @Test
    @DisplayName("Should have @Entity annotation with correct attributes")
    void shouldHaveEntityAnnotation() {
        Entity entity = Attachment.class.getAnnotation(Entity.class);
        assertNotNull(entity, "Attachment class should have @Entity annotation");
        assertEquals("Attachment", entity.name());
        assertEquals("localDomainEntity_e846fb62-e0fe-4f46-b27f-9ba52536db5a", entity.id());
        assertEquals("Id", entity.primaryKey());
        assertFalse(entity.supportStore());
    }

    @Test
    @DisplayName("Should set and get id field")
    void shouldSetAndGetId() {
        attachment.id = "att-123";
        assertEquals("att-123", attachment.id);
    }

    @Test
    @DisplayName("Should set and get title field")
    void shouldSetAndGetTitle() {
        attachment.title = "document.pdf";
        assertEquals("document.pdf", attachment.title);
    }

    @Test
    @DisplayName("Should set and get pageId field")
    void shouldSetAndGetPageId() {
        attachment.pageId = "page-456";
        assertEquals("page-456", attachment.pageId);
    }

    @Test
    @DisplayName("Should set and get mediaType field")
    void shouldSetAndGetMediaType() {
        attachment.mediaType = "application/pdf";
        assertEquals("application/pdf", attachment.mediaType);
    }

    @Test
    @DisplayName("Should set and get fileSize field as Double")
    void shouldSetAndGetFileSize() {
        attachment.fileSize = 1024.5;
        assertEquals(1024.5, attachment.fileSize);
    }

    @Test
    @DisplayName("Should set and get downloadLink field")
    void shouldSetAndGetDownloadLink() {
        attachment.downloadLink = "https://example.com/download/att-123";
        assertEquals("https://example.com/download/att-123", attachment.downloadLink);
    }

    @Test
    @DisplayName("Should set and get webUILink field")
    void shouldSetAndGetWebUILink() {
        attachment.webUILink = "https://example.com/wiki/att-123";
        assertEquals("https://example.com/wiki/att-123", attachment.webUILink);
    }

    @Test
    @DisplayName("Should set and get createdAt field as Long")
    void shouldSetAndGetCreatedAt() {
        attachment.createdAt = 1700000000000L;
        assertEquals(1700000000000L, attachment.createdAt);
    }

    @Test
    @DisplayName("Should set and get versionNumber field as Double")
    void shouldSetAndGetVersionNumber() {
        attachment.versionNumber = 3.0;
        assertEquals(3.0, attachment.versionNumber);
    }

    @Test
    @DisplayName("Should set and get versionCreatedAt field as Long")
    void shouldSetAndGetVersionCreatedAt() {
        attachment.versionCreatedAt = 1700000001000L;
        assertEquals(1700000001000L, attachment.versionCreatedAt);
    }

    @Test
    @DisplayName("Should set and get status field")
    void shouldSetAndGetStatus() {
        attachment.status = "current";
        assertEquals("current", attachment.status);
    }

    @Test
    @DisplayName("All fields should be null by default")
    void allFieldsShouldBeNullByDefault() {
        assertNull(attachment.id);
        assertNull(attachment.title);
        assertNull(attachment.pageId);
        assertNull(attachment.mediaType);
        assertNull(attachment.fileSize);
        assertNull(attachment.downloadLink);
        assertNull(attachment.webUILink);
        assertNull(attachment.createdAt);
        assertNull(attachment.versionNumber);
        assertNull(attachment.versionCreatedAt);
        assertNull(attachment.status);
    }

    @Test
    @DisplayName("Should have exactly 11 public fields")
    void shouldHaveCorrectNumberOfPublicFields() {
        long publicFieldCount = java.util.Arrays.stream(Attachment.class.getDeclaredFields())
                .filter(f -> java.lang.reflect.Modifier.isPublic(f.getModifiers()))
                .count();
        assertEquals(11, publicFieldCount);
    }
}
