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

import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.entities.Page;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.entities.Space;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.entities.Template;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.entities.User;
import com.kristasoft.extensions.essentials.collaboration.confluence.dto.Body;
import com.kristasoft.extensions.essentials.collaboration.confluence.dto.ProfilePicture;
import com.kristasoft.extensions.essentials.collaboration.confluence.dto.Storage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ParserUtils")
class ParserUtilsTest {

    // ──────────────────────────────────────────────────────────────
    //  Private constructor guard
    // ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("should have a private constructor to prevent instantiation")
    void privateConstructor() throws Exception {
        Constructor<ParserUtils> ctor = ParserUtils.class.getDeclaredConstructor();
        assertFalse(ctor.canAccess(null), "Constructor should be private");

        ctor.setAccessible(true);
        assertNotNull(ctor.newInstance());
    }

    // ──────────────────────────────────────────────────────────────
    //  setTemplateEntity
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("setTemplateEntity")
    class SetTemplateEntity {

        @Test
        @DisplayName("should map all DTO fields to entity fields")
        void mapsAllFields() {
            String htmlBody = "<p>Hello <at:var at:name=\"userName\"/>!</p>";
            Storage storage = Storage.builder().setValue(htmlBody).setRepresentation("storage").build();
            Body body = Body.setBody(storage);

            com.kristasoft.extensions.essentials.collaboration.confluence.dto.Template dto =
                    com.kristasoft.extensions.essentials.collaboration.confluence.dto.Template.builder()
                            .setTemplateId("t-100")
                            .setSpaceId("sp-200")
                            .setName("Meeting Notes")
                            .setTemplateType("page")
                            .setBody(body)
                            .build();

            Template entity = ParserUtils.setTemplateEntity(dto);

            assertNotNull(entity);
            assertEquals("t-100", entity.id);
            assertEquals("sp-200", entity.spaceId);
            assertEquals("Meeting Notes", entity.name);
            assertEquals("page", entity.type);
            assertNotNull(entity.body);
            assertNotNull(entity.variables, "variables should be extracted from HTML");
            assertFalse(entity.variables.isEmpty(), "at:var tag should produce at least one variable");
            assertEquals("userName", entity.variables.get(0).get("Key"));
        }

        @Test
        @DisplayName("should handle template with no body")
        void noBody() {
            com.kristasoft.extensions.essentials.collaboration.confluence.dto.Template dto =
                    com.kristasoft.extensions.essentials.collaboration.confluence.dto.Template.builder()
                            .setTemplateId("t-101")
                            .setName("Empty Template")
                            .build();

            Template entity = ParserUtils.setTemplateEntity(dto);

            assertNotNull(entity);
            assertEquals("t-101", entity.id);
            assertEquals("Empty Template", entity.name);
            assertNull(entity.body);
            assertNull(entity.variables);
        }

        @Test
        @DisplayName("should extract multiple variables from template body")
        void multipleVariables() {
            String htmlBody = "<p><at:var at:name=\"first\"/> and <at:var at:name=\"second\"/></p>";
            Storage storage = Storage.builder().setValue(htmlBody).setRepresentation("storage").build();
            Body body = Body.setBody(storage);

            com.kristasoft.extensions.essentials.collaboration.confluence.dto.Template dto =
                    com.kristasoft.extensions.essentials.collaboration.confluence.dto.Template.builder()
                            .setTemplateId("t-102")
                            .setBody(body)
                            .build();

            Template entity = ParserUtils.setTemplateEntity(dto);

            assertNotNull(entity.variables);
            assertEquals(2, entity.variables.size());
        }

        @Test
        @DisplayName("should return empty variables list for body with no at:var tags")
        void noVariables() {
            String htmlBody = "<p>No variables here</p>";
            Storage storage = Storage.builder().setValue(htmlBody).setRepresentation("storage").build();
            Body body = Body.setBody(storage);

            com.kristasoft.extensions.essentials.collaboration.confluence.dto.Template dto =
                    com.kristasoft.extensions.essentials.collaboration.confluence.dto.Template.builder()
                            .setTemplateId("t-103")
                            .setBody(body)
                            .build();

            Template entity = ParserUtils.setTemplateEntity(dto);

            assertNotNull(entity.variables);
            assertTrue(entity.variables.isEmpty());
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  setSpaceEntity
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("setSpaceEntity")
    class SetSpaceEntity {

        @Test
        @DisplayName("should map all DTO fields including createdAt timestamp")
        void mapsAllFields() {
            Date now = new Date();
            com.kristasoft.extensions.essentials.collaboration.confluence.dto.Space dto =
                    com.kristasoft.extensions.essentials.collaboration.confluence.dto.Space.builder()
                            .setId("sp-1")
                            .setName("Engineering")
                            .setType("global")
                            .setStatus("current")
                            .setDescription("Engineering space")
                            .setCreatedAt(now)
                            .build();

            Space entity = ParserUtils.setSpaceEntity(dto);

            assertNotNull(entity);
            assertEquals("sp-1", entity.id);
            assertEquals("Engineering", entity.name);
            assertEquals("global", entity.type);
            assertEquals("current", entity.status);
            assertEquals("Engineering space", entity.description);
            assertEquals(now.getTime(), entity.createdAt, "createdAt should be the epoch millis from getTime()");
        }

        @Test
        @DisplayName("should handle null createdAt gracefully")
        void nullCreatedAt() {
            com.kristasoft.extensions.essentials.collaboration.confluence.dto.Space dto =
                    com.kristasoft.extensions.essentials.collaboration.confluence.dto.Space.builder()
                            .setId("sp-2")
                            .setName("Marketing")
                            .build();

            Space entity = ParserUtils.setSpaceEntity(dto);

            assertNotNull(entity);
            assertEquals("sp-2", entity.id);
            assertNull(entity.createdAt, "createdAt should be null when DTO has null date");
        }

        @Test
        @DisplayName("should handle all-null optional fields")
        void allNullOptionalFields() {
            com.kristasoft.extensions.essentials.collaboration.confluence.dto.Space dto =
                    com.kristasoft.extensions.essentials.collaboration.confluence.dto.Space.builder()
                            .setId("sp-3")
                            .build();

            Space entity = ParserUtils.setSpaceEntity(dto);

            assertNotNull(entity);
            assertEquals("sp-3", entity.id);
            assertNull(entity.name);
            assertNull(entity.type);
            assertNull(entity.status);
            assertNull(entity.description);
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  setPageEntity
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("setPageEntity")
    class SetPageEntity {

        @Test
        @DisplayName("should build pageURL from domainUrl and webui link")
        void buildsPageUrl() {
            com.kristasoft.extensions.essentials.collaboration.confluence.dto.Page dtoPage =
                    new com.kristasoft.extensions.essentials.collaboration.confluence.dto.Page.PageBuilder("pg-1", "My Page")
                            .status("current")
                            .type("page")
                            .spaceId("sp-1")
                            .parentId("pg-0")
                            .parentType("page")
                            .build();
            dtoPage.setDomainUrl("https://example.atlassian.net/wiki");
            dtoPage.setLinks(Map.of("webui", "/spaces/ENG/pages/12345/My+Page"));

            Page entity = ParserUtils.setPageEntity(dtoPage);

            assertNotNull(entity);
            assertEquals("pg-1", entity.id);
            assertEquals("My Page", entity.title);
            assertEquals("current", entity.status);
            assertEquals("page", entity.type);
            assertEquals("sp-1", entity.spaceID);
            assertEquals("pg-0", entity.parentID);
            assertEquals("page", entity.parentType);
            assertFalse(entity.hasChildren);
            assertEquals("https://example.atlassian.net/wiki/spaces/ENG/pages/12345/My+Page", entity.pageURL,
                    "pageURL should be a plain URL, not an <a href> wrapper");
            assertFalse(entity.pageURL.contains("<a"), "pageURL should not contain an href anchor tag");
        }

        @Test
        @DisplayName("should handle null links gracefully")
        void nullLinks() {
            com.kristasoft.extensions.essentials.collaboration.confluence.dto.Page dtoPage =
                    new com.kristasoft.extensions.essentials.collaboration.confluence.dto.Page.PageBuilder("pg-2", "No Links Page")
                            .status("draft")
                            .build();
            dtoPage.setDomainUrl("https://example.atlassian.net");

            Page entity = ParserUtils.setPageEntity(dtoPage);

            assertNotNull(entity);
            assertEquals("pg-2", entity.id);
            assertNotNull(entity.pageURL, "pageURL should still be created when links are null");
        }

        @Test
        @DisplayName("should default type to page when DTO type is null")
        void defaultTypeToPage() {
            com.kristasoft.extensions.essentials.collaboration.confluence.dto.Page dtoPage =
                    new com.kristasoft.extensions.essentials.collaboration.confluence.dto.Page.PageBuilder("pg-3", "Typed Page")
                            .build();
            dtoPage.setDomainUrl("");

            Page entity = ParserUtils.setPageEntity(dtoPage);

            assertEquals("page", entity.type, "type should default to 'page' if not specified");
        }

        @Test
        @DisplayName("should extract body from storage when present")
        void extractsBody() {
            Storage storage = Storage.builder().setValue("  <p>Content</p>  ").setRepresentation("storage").build();
            Body body = Body.setBody(storage);

            com.kristasoft.extensions.essentials.collaboration.confluence.dto.Page dtoPage =
                    new com.kristasoft.extensions.essentials.collaboration.confluence.dto.Page.PageBuilder("pg-4", "With Body")
                            .body(body)
                            .build();
            dtoPage.setDomainUrl("");

            Page entity = ParserUtils.setPageEntity(dtoPage);

            assertEquals("<p>Content</p>", entity.body, "body should be trimmed");
        }

        @Test
        @DisplayName("should handle null body gracefully")
        void nullBody() {
            com.kristasoft.extensions.essentials.collaboration.confluence.dto.Page dtoPage =
                    new com.kristasoft.extensions.essentials.collaboration.confluence.dto.Page.PageBuilder("pg-5", "No Body")
                            .build();
            dtoPage.setDomainUrl("");

            Page entity = ParserUtils.setPageEntity(dtoPage);

            assertNull(entity.body);
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  setUserEntity
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("setUserEntity")
    class SetUserEntity {

        @Test
        @DisplayName("should map all DTO fields to entity including profilePicture path")
        void mapsAllFields() {
            com.kristasoft.extensions.essentials.collaboration.confluence.dto.User dto =
                    new com.kristasoft.extensions.essentials.collaboration.confluence.dto.User();
            dto.setAccountId("acc-123");
            dto.setDisplayName("Jane Doe");
            dto.setEmail("jane@example.com");
            dto.setPublicName("Jane");
            dto.setAccountType("atlassian");
            dto.setAccountStatus("active");
            dto.setTimeZone("America/New_York");
            dto.setLocale("en_US");
            dto.setIsExternalCollaborator(false);
            dto.setIsGuest(false);

            ProfilePicture pp = new ProfilePicture();
            pp.setPath("/wiki/aa-avatar/acc-123");
            dto.setProfilePicture(pp);

            User entity = ParserUtils.setUserEntity(dto);

            assertNotNull(entity);
            assertEquals("acc-123", entity.accountID);
            assertEquals("Jane Doe", entity.displayName);
            assertEquals("jane@example.com", entity.email);
            assertEquals("Jane", entity.publicName);
            assertEquals("atlassian", entity.accountType);
            assertEquals("active", entity.accountStatus);
            assertEquals("America/New_York", entity.timeZone);
            assertEquals("en_US", entity.locale);
            assertFalse(entity.isExternalCollaborator);
            assertFalse(entity.isGuest);
            assertEquals("/wiki/aa-avatar/acc-123", entity.profilePicturePath);
        }

        @Test
        @DisplayName("should handle null profilePicture without NPE")
        void nullProfilePicture() {
            com.kristasoft.extensions.essentials.collaboration.confluence.dto.User dto =
                    new com.kristasoft.extensions.essentials.collaboration.confluence.dto.User();
            dto.setAccountId("acc-456");

            User entity = ParserUtils.setUserEntity(dto);

            assertNotNull(entity);
            assertEquals("acc-456", entity.accountID);
            assertNull(entity.profilePicturePath,
                    "profilePicturePath should be null when profilePicture is null");
        }

        @Test
        @DisplayName("should accept null optional fields gracefully")
        void nullOptionalFields() {
            com.kristasoft.extensions.essentials.collaboration.confluence.dto.User dto =
                    new com.kristasoft.extensions.essentials.collaboration.confluence.dto.User();
            dto.setAccountId("acc-789");

            User entity = ParserUtils.setUserEntity(dto);

            assertNotNull(entity);
            assertEquals("acc-789", entity.accountID);
            assertNull(entity.displayName);
            assertNull(entity.email);
            assertNull(entity.publicName);
            assertNull(entity.accountType);
            assertNull(entity.accountStatus);
            assertNull(entity.timeZone);
            assertNull(entity.locale);
            assertNull(entity.isExternalCollaborator);
            assertNull(entity.isGuest);
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  parseIsoToEpochMillis (private static — invoked via reflection)
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("parseIsoToEpochMillis")
    class ParseIsoToEpochMillis {

        /** Invokes the private static {@code parseIsoToEpochMillis(String)} reflectively. */
        private Long invoke(String iso) throws Exception {
            Method m = ParserUtils.class.getDeclaredMethod("parseIsoToEpochMillis", String.class);
            m.setAccessible(true);
            return (Long) m.invoke(null, iso);
        }

        @Test
        @DisplayName("null input → returns null (first operand of guard)")
        void nullInput_returnsNull() throws Exception {
            assertNull(invoke(null));
        }

        @Test
        @DisplayName("empty input → returns null (second operand of guard)")
        void emptyInput_returnsNull() throws Exception {
            assertNull(invoke(""));
        }

        @Test
        @DisplayName("valid ISO-8601 instant → returns epoch millis")
        void validIso_returnsEpochMillis() throws Exception {
            Long result = invoke("2024-01-01T00:00:00Z");
            assertNotNull(result);
            assertEquals(Instant.parse("2024-01-01T00:00:00Z").toEpochMilli(), result);
            assertEquals(1704067200000L, result);
        }

        @Test
        @DisplayName("unparseable input → DateTimeParseException caught → returns null")
        void invalidIso_returnsNull() throws Exception {
            assertNull(invoke("not-a-real-timestamp"));
        }
    }
}
