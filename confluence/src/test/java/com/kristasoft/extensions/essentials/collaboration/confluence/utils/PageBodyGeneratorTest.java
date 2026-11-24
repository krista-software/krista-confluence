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

package com.kristasoft.extensions.essentials.collaboration.confluence.utils;

import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.entities.Content;
import com.kristasoft.extensions.essentials.collaboration.confluence.constants.ConfluenceConstants;
import com.kristasoft.extensions.essentials.collaboration.confluence.dto.Body;
import com.kristasoft.extensions.essentials.collaboration.confluence.dto.Page;
import com.kristasoft.extensions.essentials.collaboration.confluence.dto.Storage;
import com.kristasoft.extensions.essentials.collaboration.confluence.dto.Template;
import com.kristasoft.extensions.essentials.collaboration.confluence.dto.Version;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PageBodyGenerator")
class PageBodyGeneratorTest {

    private PageBodyGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new PageBodyGenerator();
    }

    // ── helpers ──────────────────────────────────────────────────

    private Template buildTemplate(String htmlBody) {
        Storage storage = Storage.builder().setValue(htmlBody).setRepresentation("storage").build();
        Body body = Body.setBody(storage);
        return Template.builder()
                .setTemplateId("tpl-1")
                .setBody(body)
                .build();
    }

    private Page buildPage(String id, String title, String status, int versionNumber, String bodyHtml) {
        Page.PageBuilder builder = new Page.PageBuilder(id, title)
                .status(status)
                .version(Version.builder().withNumber(versionNumber).withMessage("v" + versionNumber).build());
        if (bodyHtml != null) {
            Storage storage = Storage.builder().setValue(bodyHtml).setRepresentation("storage").build();
            Body body = Body.setBody(storage);
            builder.body(body);
        }
        return builder.build();
    }

    private List<Content> buildContents(String... bodies) {
        return java.util.Arrays.stream(bodies).map(b -> {
            Content c = new Content();
            c.body = b;
            return c;
        }).toList();
    }

    // ──────────────────────────────────────────────────────────────
    //  generateBodyForPageRequest (template variant)
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("generateBodyForPageRequest with template")
    class PageRequestWithTemplate {

        @Test
        @DisplayName("should create body map with spaceId, status=draft, title, and body storage")
        void createsCorrectBody() {
            Template template = buildTemplate("<p>Hello <at:var at:name=\"name\"/>!</p>");
            List<Map<String, Object>> variables = List.of(
                    Map.of("Key", "name", "Value", "World")
            );

            Map<String, Object> result = generator.generateBodyForPageRequest("space-1", template, "Test Page", variables);

            assertNotNull(result);
            assertEquals("space-1", result.get("spaceId"));
            assertEquals("draft", result.get(ConfluenceConstants.STATUS));
            assertEquals("Test Page", result.get(ConfluenceConstants.TITLE));
            assertNotNull(result.get("body"), "body (Storage) should be present");
        }

        @Test
        @DisplayName("should replace at:var tags with variable values")
        void replacesVariables() {
            Template template = buildTemplate("<p><at:var at:name=\"greeting\"/></p>");
            List<Map<String, Object>> variables = List.of(
                    Map.of("Key", "greeting", "Value", "Hello!")
            );

            Map<String, Object> result = generator.generateBodyForPageRequest("space-1", template, "Page", variables);

            assertNotNull(result.get("body"));
        }

        @Test
        @DisplayName("should handle empty variables list")
        void emptyVariables() {
            Template template = buildTemplate("<p>No vars</p>");
            List<Map<String, Object>> variables = List.of();

            Map<String, Object> result = generator.generateBodyForPageRequest("space-1", template, "Page", variables);

            assertNotNull(result);
            assertEquals("draft", result.get(ConfluenceConstants.STATUS));
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  generateBodyForPageRequest (content variant)
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("generateBodyForPageRequest with content list")
    class PageRequestWithContent {

        @Test
        @DisplayName("should join content bodies with newline")
        void joinsContentBodies() {
            List<Content> contents = buildContents("<p>Part 1</p>", "<p>Part 2</p>");

            Map<String, Object> result = generator.generateBodyForPageRequest("space-2", "Content Page", contents);

            assertNotNull(result);
            assertEquals("space-2", result.get("spaceId"));
            assertEquals("draft", result.get(ConfluenceConstants.STATUS));
            assertEquals("Content Page", result.get(ConfluenceConstants.TITLE));
            assertNotNull(result.get("body"));
        }

        @Test
        @DisplayName("should handle single content entry")
        void singleContent() {
            List<Content> contents = buildContents("<p>Only</p>");

            Map<String, Object> result = generator.generateBodyForPageRequest("space-3", "Single", contents);

            assertNotNull(result);
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  generateBodyForPageUpdate (template variant)
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("generateBodyForPageUpdate with template")
    class PageUpdateWithTemplate {

        @Test
        @DisplayName("draft to current should set version number to 1")
        void draftToCurrentVersionOne() {
            Page page = buildPage("pg-1", "Old Title", "draft", 3, "<p>old</p>");
            Template template = buildTemplate("<p><at:var at:name=\"x\"/></p>");
            List<Map<String, Object>> variables = List.of(Map.of("Key", "x", "Value", "new"));

            Map<String, Object> result = generator.generateBodyForPageUpdate(page, template, "current", "New Title", "Publishing", variables);

            assertNotNull(result);
            assertEquals("pg-1", result.get("id"));
            assertEquals("current", result.get(ConfluenceConstants.STATUS));
            assertEquals("New Title", result.get(ConfluenceConstants.TITLE));

            Object versionObj = result.get("version");
            assertNotNull(versionObj, "version should be present for current status");
            assertInstanceOf(Version.class, versionObj);
            assertEquals(1, ((Version) versionObj).getNumber(),
                    "version number should be 1 when transitioning from draft to current");
        }

        @Test
        @DisplayName("current to current should increment version number")
        void currentToCurrentIncrements() {
            Page page = buildPage("pg-2", "Existing", "current", 5, "<p>body</p>");
            Template template = buildTemplate("<p>content</p>");

            Map<String, Object> result = generator.generateBodyForPageUpdate(page, template, "current", "Updated", "Update comment", List.of());

            assertEquals("current", result.get(ConfluenceConstants.STATUS));

            Version version = (Version) result.get("version");
            assertNotNull(version);
            assertEquals(6, version.getNumber(),
                    "version number should be page version + 1 for current-to-current");
            assertEquals("Update comment", version.getMessage());
        }

        @Test
        @DisplayName("null status should fall back to page status")
        void nullStatusFallsBack() {
            Page page = buildPage("pg-3", "FallBack", "draft", 2, null);
            Template template = buildTemplate("<p>tpl</p>");

            Map<String, Object> result = generator.generateBodyForPageUpdate(page, template, null, null, null, null);

            assertEquals("draft", result.get(ConfluenceConstants.STATUS));
            assertEquals("FallBack", result.get(ConfluenceConstants.TITLE),
                    "title should fall back to page title when null");
        }

        @Test
        @DisplayName("null variables should preserve existing page body")
        void nullVariablesPreservesBody() {
            Page page = buildPage("pg-4", "KeepBody", "current", 1, "<p>existing</p>");
            Template template = buildTemplate("<p>ignored</p>");

            Map<String, Object> result = generator.generateBodyForPageUpdate(page, template, "current", "KeepBody", "comment", null);

            assertNotNull(result.get("body"));
        }

        @Test
        @DisplayName("null title should fall back to page title")
        void nullTitleFallsBack() {
            Page page = buildPage("pg-5", "Original Title", "current", 1, "<p>body</p>");
            Template template = buildTemplate("<p>content</p>");

            Map<String, Object> result = generator.generateBodyForPageUpdate(page, template, "current", null, "comment", List.of());

            assertEquals("Original Title", result.get(ConfluenceConstants.TITLE));
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  generateBodyForPageUpdate (content variant)
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("generateBodyForPageUpdate with content list")
    class PageUpdateWithContent {

        @Test
        @DisplayName("draft-to-draft should keep draft status without version")
        void draftToDraftKeepsDraft() {
            Page page = buildPage("pg-10", "Draft Page", "draft", 1, "<p>old</p>");
            List<Content> contents = buildContents("<p>new</p>");

            Map<String, Object> result = generator.generateBodyForPageUpdate(page, "draft", "Updated Draft", "comment", contents);

            assertEquals("draft", result.get(ConfluenceConstants.STATUS));
            assertNull(result.get("version"), "version should not be set for draft-to-draft");
        }

        @Test
        @DisplayName("draft-to-current should set current status and increment version")
        void draftToCurrentSetsCurrentAndVersion() {
            Page page = buildPage("pg-11", "Going Live", "draft", 3, "<p>old</p>");
            List<Content> contents = buildContents("<p>live content</p>");

            Map<String, Object> result = generator.generateBodyForPageUpdate(page, "current", "Live Page", "Publishing", contents);

            assertEquals("current", result.get(ConfluenceConstants.STATUS));
            Version version = (Version) result.get("version");
            assertNotNull(version);
            assertEquals(4, version.getNumber(), "version should be page version + 1");
        }

        @Test
        @DisplayName("current-to-current should increment version")
        void currentToCurrentIncrements() {
            Page page = buildPage("pg-12", "Existing Page", "current", 7, "<p>old</p>");
            List<Content> contents = buildContents("<p>updated</p>");

            Map<String, Object> result = generator.generateBodyForPageUpdate(page, "current", "Updated", "Editing", contents);

            assertEquals("current", result.get(ConfluenceConstants.STATUS));
            Version version = (Version) result.get("version");
            assertNotNull(version);
            assertEquals(8, version.getNumber());
        }

        @Test
        @DisplayName("null status should fall back to page status")
        void nullStatusFallsBack() {
            Page page = buildPage("pg-13", "Fallback", "current", 2, "<p>body</p>");

            Map<String, Object> result = generator.generateBodyForPageUpdate(page, null, null, null, null);

            assertEquals("current", result.get(ConfluenceConstants.STATUS));
            assertEquals("Fallback", result.get(ConfluenceConstants.TITLE));
        }

        @Test
        @DisplayName("null contents should preserve existing page body")
        void nullContentsPreservesBody() {
            Page page = buildPage("pg-14", "Keep", "current", 1, "<p>keep this</p>");

            Map<String, Object> result = generator.generateBodyForPageUpdate(page, "current", "Keep", "comment", null);

            assertNotNull(result.get("body"));
        }

        @Test
        @DisplayName("null title should fall back to page title")
        void nullTitleFallsBack() {
            Page page = buildPage("pg-15", "Original", "draft", 1, "<p>body</p>");
            List<Content> contents = buildContents("<p>content</p>");

            Map<String, Object> result = generator.generateBodyForPageUpdate(page, "draft", null, null, contents);

            assertEquals("Original", result.get(ConfluenceConstants.TITLE));
        }
    }
}
