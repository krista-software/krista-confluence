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

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link User} DTO.
 *
 * Covers: no-arg constructor, getters/setters for type, accountId, accountType,
 *         email, publicName, timeZone, profilePicture, displayName,
 *         isExternalCollaborator, isGuest, locale, accountStatus,
 *         expandable, links, toString().
 */
@DisplayName("User DTO")
class UserTest {

    // ── Constructor ──

    @Test
    @DisplayName("no-arg constructor creates instance with all null fields")
    void noArgConstructor_allFieldsNull() {
        User user = new User();

        assertNotNull(user);
        assertNull(user.getType());
        assertNull(user.getAccountId());
        assertNull(user.getAccountType());
        assertNull(user.getEmail());
        assertNull(user.getPublicName());
        assertNull(user.getTimeZone());
        assertNull(user.getProfilePicture());
        assertNull(user.getDisplayName());
        assertNull(user.getIsExternalCollaborator());
        assertNull(user.getIsGuest());
        assertNull(user.getLocale());
        assertNull(user.getAccountStatus());
        assertNull(user.getExpandable());
        assertNull(user.getLinks());
    }

    // ── type ──

    @Test
    @DisplayName("setType / getType round-trip")
    void setType_getType_roundTrip() {
        User user = new User();
        user.setType("known");

        assertEquals("known", user.getType());
    }

    // ── accountId ──

    @Test
    @DisplayName("setAccountId / getAccountId round-trip")
    void setAccountId_getAccountId_roundTrip() {
        User user = new User();
        user.setAccountId("5b10ac8d82e05b22cc7d4ef5");

        assertEquals("5b10ac8d82e05b22cc7d4ef5", user.getAccountId());
    }

    // ── accountType ──

    @Test
    @DisplayName("setAccountType / getAccountType round-trip")
    void setAccountType_getAccountType_roundTrip() {
        User user = new User();
        user.setAccountType("atlassian");

        assertEquals("atlassian", user.getAccountType());
    }

    // ── email ──

    @Test
    @DisplayName("setEmail / getEmail round-trip")
    void setEmail_getEmail_roundTrip() {
        User user = new User();
        user.setEmail("user@example.com");

        assertEquals("user@example.com", user.getEmail());
    }

    // ── publicName ──

    @Test
    @DisplayName("setPublicName / getPublicName round-trip")
    void setPublicName_getPublicName_roundTrip() {
        User user = new User();
        user.setPublicName("John Doe");

        assertEquals("John Doe", user.getPublicName());
    }

    // ── timeZone ──

    @Test
    @DisplayName("setTimeZone / getTimeZone round-trip")
    void setTimeZone_getTimeZone_roundTrip() {
        User user = new User();
        user.setTimeZone("America/New_York");

        assertEquals("America/New_York", user.getTimeZone());
    }

    // ── profilePicture ──

    @Test
    @DisplayName("setProfilePicture / getProfilePicture round-trip")
    void setProfilePicture_getProfilePicture_roundTrip() {
        User user = new User();
        ProfilePicture pp = new ProfilePicture();
        pp.setPath("/avatar.png");
        pp.setWidth(48);
        pp.setHeight(48);
        pp.setIsDefault(false);

        user.setProfilePicture(pp);

        assertSame(pp, user.getProfilePicture());
        assertEquals("/avatar.png", user.getProfilePicture().getPath());
    }

    // ── displayName ──

    @Test
    @DisplayName("setDisplayName / getDisplayName round-trip")
    void setDisplayName_getDisplayName_roundTrip() {
        User user = new User();
        user.setDisplayName("Jane Smith");

        assertEquals("Jane Smith", user.getDisplayName());
    }

    // ── isExternalCollaborator ──

    @Test
    @DisplayName("setIsExternalCollaborator / getIsExternalCollaborator round-trip with true")
    void setIsExternalCollaborator_true_roundTrip() {
        User user = new User();
        user.setIsExternalCollaborator(true);

        assertTrue(user.getIsExternalCollaborator());
    }

    @Test
    @DisplayName("setIsExternalCollaborator / getIsExternalCollaborator round-trip with false")
    void setIsExternalCollaborator_false_roundTrip() {
        User user = new User();
        user.setIsExternalCollaborator(false);

        assertFalse(user.getIsExternalCollaborator());
    }

    // ── isGuest ──

    @Test
    @DisplayName("setIsGuest / getIsGuest round-trip with true")
    void setIsGuest_true_roundTrip() {
        User user = new User();
        user.setIsGuest(true);

        assertTrue(user.getIsGuest());
    }

    @Test
    @DisplayName("setIsGuest / getIsGuest round-trip with false")
    void setIsGuest_false_roundTrip() {
        User user = new User();
        user.setIsGuest(false);

        assertFalse(user.getIsGuest());
    }

    // ── locale ──

    @Test
    @DisplayName("setLocale / getLocale round-trip")
    void setLocale_getLocale_roundTrip() {
        User user = new User();
        user.setLocale("en-US");

        assertEquals("en-US", user.getLocale());
    }

    // ── accountStatus ──

    @Test
    @DisplayName("setAccountStatus / getAccountStatus round-trip")
    void setAccountStatus_getAccountStatus_roundTrip() {
        User user = new User();
        user.setAccountStatus("active");

        assertEquals("active", user.getAccountStatus());
    }

    // ── expandable ──

    @Test
    @DisplayName("setExpandable / getExpandable round-trip")
    void setExpandable_getExpandable_roundTrip() {
        User user = new User();
        Map<String, String> expandable = new HashMap<>();
        expandable.put("operations", "");
        expandable.put("personalSpace", "");

        user.setExpandable(expandable);

        assertSame(expandable, user.getExpandable());
        assertEquals(2, user.getExpandable().size());
    }

    // ── links ──

    @Test
    @DisplayName("setLinks / getLinks round-trip")
    void setLinks_getLinks_roundTrip() {
        User user = new User();
        Map<String, String> links = new HashMap<>();
        links.put("self", "https://example.atlassian.net/wiki/rest/api/user?accountId=abc");

        user.setLinks(links);

        assertSame(links, user.getLinks());
        assertEquals("https://example.atlassian.net/wiki/rest/api/user?accountId=abc",
                user.getLinks().get("self"));
    }

    // ── toString ──

    @Test
    @DisplayName("toString with populated fields contains key values")
    void toString_withPopulatedFields() {
        User user = new User();
        user.setType("known");
        user.setAccountId("acc-123");
        user.setAccountType("atlassian");
        user.setEmail("test@example.com");
        user.setPublicName("Public");
        user.setDisplayName("Display");
        user.setAccountStatus("active");

        String result = user.toString();

        assertNotNull(result);
        assertTrue(result.startsWith("User{"));
        assertTrue(result.contains("type='known'"));
        assertTrue(result.contains("accountId='acc-123'"));
        assertTrue(result.contains("accountType='atlassian'"));
        assertTrue(result.contains("email='test@example.com'"));
        assertTrue(result.contains("publicName='Public'"));
        assertTrue(result.contains("displayName='Display'"));
        assertTrue(result.contains("accountStatus='active'"));
        assertTrue(result.endsWith("}"));
    }

    @Test
    @DisplayName("toString with all null fields contains null literals")
    void toString_withAllNullFields() {
        User user = new User();

        String result = user.toString();

        assertNotNull(result);
        assertTrue(result.startsWith("User{"));
        assertTrue(result.contains("null"));
        assertTrue(result.endsWith("}"));
    }

    // ── Setter overwrites builder values ──

    @Test
    @DisplayName("setters overwrite previously set values")
    void setters_overwritePreviousValues() {
        User user = new User();
        user.setType("old-type");
        user.setAccountId("old-id");
        user.setEmail("old@example.com");

        user.setType("new-type");
        user.setAccountId("new-id");
        user.setEmail("new@example.com");

        assertEquals("new-type", user.getType());
        assertEquals("new-id", user.getAccountId());
        assertEquals("new@example.com", user.getEmail());
    }
}
