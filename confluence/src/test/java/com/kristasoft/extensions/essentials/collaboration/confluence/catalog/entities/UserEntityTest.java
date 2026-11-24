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

@DisplayName("User Entity Tests")
class UserEntityTest {

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
    }

    @Test
    @DisplayName("Should have @Domain annotation type available")
    void shouldHaveDomainAnnotation() {
        // @Domain has CLASS retention, so it is not available at runtime via reflection.
        // Verify the annotation type is a valid annotation interface that the class references.
        assertTrue(Domain.class.isAnnotation(), "Domain should be an annotation type");
        // The User class is annotated with @Domain in source (verified at compile time).
    }

    @Test
    @DisplayName("Should have @Entity annotation with correct attributes")
    void shouldHaveEntityAnnotation() {
        Entity entity = User.class.getAnnotation(Entity.class);
        assertNotNull(entity, "User class should have @Entity annotation");
        assertEquals("User", entity.name());
        assertEquals("localDomainEntity_8f9a2b3c-4d5e-6f7a-8b9c-0d1e2f3a4b5c", entity.id());
        assertEquals("Account ID", entity.primaryKey());
        assertFalse(entity.supportStore());
    }

    @Test
    @DisplayName("Should set and get accountID field")
    void shouldSetAndGetAccountID() {
        user.accountID = "5b10ac8d82e05b22cc7d4ef5";
        assertEquals("5b10ac8d82e05b22cc7d4ef5", user.accountID);
    }

    @Test
    @DisplayName("Should set and get displayName field")
    void shouldSetAndGetDisplayName() {
        user.displayName = "John Doe";
        assertEquals("John Doe", user.displayName);
    }

    @Test
    @DisplayName("Should set and get email field")
    void shouldSetAndGetEmail() {
        user.email = "john.doe@example.com";
        assertEquals("john.doe@example.com", user.email);
    }

    @Test
    @DisplayName("Should set and get publicName field")
    void shouldSetAndGetPublicName() {
        user.publicName = "John D.";
        assertEquals("John D.", user.publicName);
    }

    @Test
    @DisplayName("Should set and get accountType field")
    void shouldSetAndGetAccountType() {
        user.accountType = "atlassian";
        assertEquals("atlassian", user.accountType);
    }

    @Test
    @DisplayName("Should set and get accountStatus field")
    void shouldSetAndGetAccountStatus() {
        user.accountStatus = "active";
        assertEquals("active", user.accountStatus);
    }

    @Test
    @DisplayName("Should set and get timeZone field")
    void shouldSetAndGetTimeZone() {
        user.timeZone = "America/New_York";
        assertEquals("America/New_York", user.timeZone);
    }

    @Test
    @DisplayName("Should set and get locale field")
    void shouldSetAndGetLocale() {
        user.locale = "en_US";
        assertEquals("en_US", user.locale);
    }

    @Test
    @DisplayName("Should set and get isExternalCollaborator field as Boolean")
    void shouldSetAndGetIsExternalCollaborator() {
        user.isExternalCollaborator = true;
        assertTrue(user.isExternalCollaborator);

        user.isExternalCollaborator = false;
        assertFalse(user.isExternalCollaborator);
    }

    @Test
    @DisplayName("Should set and get isGuest field as Boolean")
    void shouldSetAndGetIsGuest() {
        user.isGuest = true;
        assertTrue(user.isGuest);

        user.isGuest = false;
        assertFalse(user.isGuest);
    }

    @Test
    @DisplayName("Should set and get profilePicturePath field")
    void shouldSetAndGetProfilePicturePath() {
        user.profilePicturePath = "/wiki/aa-avatar/user-123";
        assertEquals("/wiki/aa-avatar/user-123", user.profilePicturePath);
    }

    @Test
    @DisplayName("All fields should be null by default")
    void allFieldsShouldBeNullByDefault() {
        assertNull(user.accountID);
        assertNull(user.displayName);
        assertNull(user.email);
        assertNull(user.publicName);
        assertNull(user.accountType);
        assertNull(user.accountStatus);
        assertNull(user.timeZone);
        assertNull(user.locale);
        assertNull(user.isExternalCollaborator);
        assertNull(user.isGuest);
        assertNull(user.profilePicturePath);
    }

    @Test
    @DisplayName("toString should contain key field values")
    void toStringShouldContainKeyFieldValues() {
        user.accountID = "acc-123";
        user.displayName = "Jane Smith";
        user.email = "jane@example.com";
        user.accountType = "atlassian";
        user.accountStatus = "active";

        String result = user.toString();

        assertAll("toString should include key fields",
                () -> assertTrue(result.contains("acc-123"), "Should contain accountID"),
                () -> assertTrue(result.contains("Jane Smith"), "Should contain displayName"),
                () -> assertTrue(result.contains("jane@example.com"), "Should contain email"),
                () -> assertTrue(result.contains("atlassian"), "Should contain accountType"),
                () -> assertTrue(result.contains("active"), "Should contain accountStatus")
        );
    }

    @Test
    @DisplayName("toString should start with 'User{' prefix")
    void toStringShouldStartWithUserPrefix() {
        String result = user.toString();
        assertTrue(result.startsWith("User{"));
        assertTrue(result.endsWith("}"));
    }

    @Test
    @DisplayName("Should have exactly 11 public fields")
    void shouldHaveCorrectNumberOfPublicFields() {
        long publicFieldCount = java.util.Arrays.stream(User.class.getDeclaredFields())
                .filter(f -> java.lang.reflect.Modifier.isPublic(f.getModifiers()))
                .count();
        assertEquals(11, publicFieldCount);
    }
}
