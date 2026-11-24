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

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link Page} and its inner {@link Page.PageBuilder}.
 *
 * Covers: builder pattern (all builder methods), build(), all getters/setters, toString().
 */
@DisplayName("Page DTO")
class PageTest {

    // ── Helper ──

    private Storage buildStorage(String value) {
        return Storage.builder().setValue(value).setRepresentation("storage").build();
    }

    private Version buildVersion(int number, String message) {
        return Version.builder().withNumber(number).withMessage(message).build();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  PageBuilder tests
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("PageBuilder")
    class PageBuilderTest {

        @Test
        @DisplayName("build with required fields only sets id and title")
        void build_withRequiredFieldsOnly() {
            Page page = new Page.PageBuilder("page-1", "My Title").build();

            assertNotNull(page);
            assertEquals("page-1", page.getId());
            assertEquals("My Title", page.getTitle());
            assertNull(page.getParentType());
            assertNull(page.getAuthorId());
            assertEquals(0, page.getPosition());
            assertNull(page.getStatus());
            assertNull(page.getType());
            assertNull(page.getBody());
            assertNull(page.getParentId());
            assertNull(page.getSpaceId());
            assertNull(page.getVersion());
        }

        @Test
        @DisplayName("build with all fields populates every property")
        void build_withAllFields_populatesAll() {
            Body body = Body.setBody(buildStorage("<p>content</p>"));
            Version version = buildVersion(1, "initial");

            Page page = new Page.PageBuilder("id-42", "Full Page")
                    .parentType("page")
                    .authorId("author-99")
                    .position(5)
                    .status("current")
                    .type("page")
                    .body(body)
                    .parentId("parent-10")
                    .spaceId("space-7")
                    .version(version)
                    .domainUrl("https://example.atlassian.net")
                    .build();

            assertEquals("id-42", page.getId());
            assertEquals("Full Page", page.getTitle());
            assertEquals("page", page.getParentType());
            assertEquals("author-99", page.getAuthorId());
            assertEquals(5, page.getPosition());
            assertEquals("current", page.getStatus());
            assertEquals("page", page.getType());
            assertSame(body, page.getBody());
            assertEquals("parent-10", page.getParentId());
            assertEquals("space-7", page.getSpaceId());
            assertSame(version, page.getVersion());
        }

        @Test
        @DisplayName("builder methods return the same builder instance (fluent API)")
        void builderMethods_returnSameInstance() {
            Body body = Body.setBody(null);
            Version version = buildVersion(1, "msg");
            Page.PageBuilder builder = new Page.PageBuilder("id", "title");

            assertSame(builder, builder.parentType("pt"));
            assertSame(builder, builder.authorId("aid"));
            assertSame(builder, builder.position(1));
            assertSame(builder, builder.status("s"));
            assertSame(builder, builder.type("t"));
            assertSame(builder, builder.body(body));
            assertSame(builder, builder.parentId("pid"));
            assertSame(builder, builder.spaceId("sid"));
            assertSame(builder, builder.version(version));
            assertSame(builder, builder.domainUrl("url"));
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Setter tests
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Setters")
    class SetterTests {

        @Test
        @DisplayName("setParentType updates and returns this")
        void setParentType_updatesAndReturnsThis() {
            Page page = new Page.PageBuilder("1", "T").build();
            Page returned = page.setParentType("folder");

            assertSame(page, returned);
            assertEquals("folder", page.getParentType());
        }

        @Test
        @DisplayName("setAuthorId updates and returns this")
        void setAuthorId_updatesAndReturnsThis() {
            Page page = new Page.PageBuilder("1", "T").build();
            Page returned = page.setAuthorId("new-author");

            assertSame(page, returned);
            assertEquals("new-author", page.getAuthorId());
        }

        @Test
        @DisplayName("setId updates and returns this")
        void setId_updatesAndReturnsThis() {
            Page page = new Page.PageBuilder("1", "T").build();
            Page returned = page.setId("new-id");

            assertSame(page, returned);
            assertEquals("new-id", page.getId());
        }

        @Test
        @DisplayName("setPosition updates and returns this")
        void setPosition_updatesAndReturnsThis() {
            Page page = new Page.PageBuilder("1", "T").build();
            Page returned = page.setPosition(42);

            assertSame(page, returned);
            assertEquals(42, page.getPosition());
        }

        @Test
        @DisplayName("setTitle updates and returns this")
        void setTitle_updatesAndReturnsThis() {
            Page page = new Page.PageBuilder("1", "T").build();
            Page returned = page.setTitle("New Title");

            assertSame(page, returned);
            assertEquals("New Title", page.getTitle());
        }

        @Test
        @DisplayName("setStatus updates and returns this")
        void setStatus_updatesAndReturnsThis() {
            Page page = new Page.PageBuilder("1", "T").build();
            Page returned = page.setStatus("draft");

            assertSame(page, returned);
            assertEquals("draft", page.getStatus());
        }

        @Test
        @DisplayName("setType updates and returns this")
        void setType_updatesAndReturnsThis() {
            Page page = new Page.PageBuilder("1", "T").build();
            Page returned = page.setType("whiteboard");

            assertSame(page, returned);
            assertEquals("whiteboard", page.getType());
        }

        @Test
        @DisplayName("setBody updates and returns this")
        void setBody_updatesAndReturnsThis() {
            Page page = new Page.PageBuilder("1", "T").build();
            Body body = Body.setBody(null);
            Page returned = page.setBody(body);

            assertSame(page, returned);
            assertSame(body, page.getBody());
        }

        @Test
        @DisplayName("setParentId updates and returns this")
        void setParentId_updatesAndReturnsThis() {
            Page page = new Page.PageBuilder("1", "T").build();
            Page returned = page.setParentId("parent-2");

            assertSame(page, returned);
            assertEquals("parent-2", page.getParentId());
        }

        @Test
        @DisplayName("setSpaceId updates and returns this")
        void setSpaceId_updatesAndReturnsThis() {
            Page page = new Page.PageBuilder("1", "T").build();
            Page returned = page.setSpaceId("space-2");

            assertSame(page, returned);
            assertEquals("space-2", page.getSpaceId());
        }

        @Test
        @DisplayName("setVersion updates and returns this")
        void setVersion_updatesAndReturnsThis() {
            Page page = new Page.PageBuilder("1", "T").build();
            Version version = Version.builder().withNumber(2).withMessage("v2").build();
            Page returned = page.setVersion(version);

            assertSame(page, returned);
            assertSame(version, page.getVersion());
        }


        @Test
        @DisplayName("setLinks updates the links map")
        void setLinks_updatesLinksMap() {
            Page page = new Page.PageBuilder("1", "T").build();
            Map<String, String> links = new HashMap<>();
            links.put("webui", "/pages/1");

            page.setLinks(links);

            assertSame(links, page.getLinks());
            assertEquals("/pages/1", page.getLinks().get("webui"));
        }

        @Test
        @DisplayName("setDomainUrl updates the domain URL")
        void setDomainUrl_updatesDomainUrl() {
            Page page = new Page.PageBuilder("1", "T").build();
            page.setDomainUrl("https://mysite.atlassian.net");

            assertEquals("https://mysite.atlassian.net", page.getDomainUrl());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Getters – default values
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("getLinks returns null by default (not set via builder)")
    void getLinks_defaultIsNull() {
        Page page = new Page.PageBuilder("1", "T").build();
        assertNull(page.getLinks());
    }

    @Test
    @DisplayName("getDomainUrl returns null by default (not set via builder setter)")
    void getDomainUrl_defaultIsNull() {
        Page page = new Page.PageBuilder("1", "T").build();
        assertNull(page.getDomainUrl());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  toString
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("toString with populated fields contains all field values")
    void toString_withPopulatedFields_containsAllValues() {
        Page page = new Page.PageBuilder("id-1", "Title")
                .parentType("page")
                .authorId("author-1")
                .position(3)
                .status("current")
                .type("page")
                .parentId("pid-1")
                .spaceId("sid-1")
                .build();

        String result = page.toString();

        assertNotNull(result);
        assertTrue(result.startsWith("Page{"));
        assertTrue(result.contains("parentType='page'"));
        assertTrue(result.contains("authorId='author-1'"));
        assertTrue(result.contains("id='id-1'"));
        assertTrue(result.contains("position=3"));
        assertTrue(result.contains("title='Title'"));
        assertTrue(result.contains("status='current'"));
        assertTrue(result.contains("type='page'"));
        assertTrue(result.contains("parentId='pid-1'"));
        assertTrue(result.contains("spaceId='sid-1'"));
        assertTrue(result.endsWith("}"));
    }

    @Test
    @DisplayName("toString with null fields contains null literals")
    void toString_withNullFields_containsNullLiterals() {
        Page page = new Page.PageBuilder("1", "T").build();

        String result = page.toString();

        assertNotNull(result);
        assertTrue(result.startsWith("Page{"));
        assertTrue(result.contains("null"));
        assertTrue(result.endsWith("}"));
    }
}
