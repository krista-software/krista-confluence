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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for the ConfluenceConstants class in the utils package.
 */
@DisplayName("ConfluenceConstants (utils package) Tests")
class ConfluenceConstantsUtilsTest {

    @Nested
    @DisplayName("Basic Auth attribute constants")
    class BasicAuthAttributeConstants {

        @Test
        @DisplayName("AUTHORIZATION_EMAIL should have correct value")
        void authorizationEmailShouldHaveCorrectValue() {
            assertEquals("Confluence Account Email", ConfluenceConstants.AUTHORIZATION_EMAIL);
        }

        @Test
        @DisplayName("API_KEY should have correct value")
        void apiKeyShouldHaveCorrectValue() {
            assertEquals("API Key", ConfluenceConstants.API_KEY);
        }

        @Test
        @DisplayName("DOMAIN_NAME should have correct value")
        void domainNameShouldHaveCorrectValue() {
            assertEquals("Domain Name", ConfluenceConstants.DOMAIN_NAME);
        }
    }

    @Nested
    @DisplayName("OAuth 2.0 attribute constants")
    class OAuthAttributeConstants {

        @Test
        @DisplayName("CLIENT_ID should have correct value")
        void clientIdShouldHaveCorrectValue() {
            assertEquals("Client Id", ConfluenceConstants.CLIENT_ID);
        }

        @Test
        @DisplayName("CLIENT_SECRET should have correct value")
        void clientSecretShouldHaveCorrectValue() {
            assertEquals("Client Secret", ConfluenceConstants.CLIENT_SECRET);
        }

        @Test
        @DisplayName("CREDENTIALS_MODE should have correct value")
        void credentialsModeShouldHaveCorrectValue() {
            assertEquals("Credentials Mode", ConfluenceConstants.CREDENTIALS_MODE);
        }

        @Test
        @DisplayName("SERVICE_ACCOUNT should have correct value")
        void serviceAccountShouldHaveCorrectValue() {
            assertEquals("Service Account", ConfluenceConstants.SERVICE_ACCOUNT);
        }
    }

    @Nested
    @DisplayName("OAuth 2.0 protocol constants")
    class OAuthProtocolConstants {

        @Test
        @DisplayName("BEARER should have correct value with trailing space")
        void bearerShouldHaveCorrectValue() {
            assertEquals("Bearer ", ConfluenceConstants.BEARER);
        }

        @Test
        @DisplayName("BEARER should end with a space")
        void bearerShouldEndWithSpace() {
            assertTrue(ConfluenceConstants.BEARER.endsWith(" "),
                    "BEARER constant should end with a trailing space for header concatenation");
        }

        @Test
        @DisplayName("REFRESH_TOKEN should have correct value")
        void refreshTokenShouldHaveCorrectValue() {
            assertEquals("refresh_token", ConfluenceConstants.REFRESH_TOKEN);
        }

        @Test
        @DisplayName("CODE should have correct value")
        void codeShouldHaveCorrectValue() {
            assertEquals("code", ConfluenceConstants.CODE);
        }

        @Test
        @DisplayName("STATE should have correct value")
        void stateShouldHaveCorrectValue() {
            assertEquals("state", ConfluenceConstants.STATE);
        }

        @Test
        @DisplayName("JAXRS_ID should have correct value")
        void jaxrsIdShouldHaveCorrectValue() {
            assertEquals("confluenceextn", ConfluenceConstants.JAXRS_ID);
        }

        @Test
        @DisplayName("AUTHORIZATION_PROMPT should have correct value")
        void authorizationPromptShouldHaveCorrectValue() {
            assertEquals("Authorize yourself and click Validate Attributes before saving changes to proceed further",
                    ConfluenceConstants.AUTHORIZATION_PROMPT);
        }
    }

    @Nested
    @DisplayName("Catalog constants")
    class CatalogConstants {

        @Test
        @DisplayName("DOMAIN_ID should have correct value")
        void domainIdShouldHaveCorrectValue() {
            assertEquals("catEntryDomain_1a86a3cc-0f2a-45fa-b9c0-b32487c830f8", ConfluenceConstants.DOMAIN_ID);
        }

        @Test
        @DisplayName("DOMAIN_ID should start with 'catEntryDomain_'")
        void domainIdShouldStartWithPrefix() {
            assertTrue(ConfluenceConstants.DOMAIN_ID.startsWith("catEntryDomain_"));
        }

        @Test
        @DisplayName("KRISTA_DOMAIN_NAME should have correct value")
        void kristaDomainNameShouldHaveCorrectValue() {
            assertEquals("Confluence", ConfluenceConstants.KRISTA_DOMAIN_NAME);
        }

        @Test
        @DisplayName("ECOSYSTEM_ID should have correct value")
        void ecosystemIdShouldHaveCorrectValue() {
            assertEquals("catEntryEcosystem_0aeef50c-bc48-4c2c-844a-948193e53e82", ConfluenceConstants.ECOSYSTEM_ID);
        }

        @Test
        @DisplayName("ECOSYSTEM_ID should start with 'catEntryEcosystem_'")
        void ecosystemIdShouldStartWithPrefix() {
            assertTrue(ConfluenceConstants.ECOSYSTEM_ID.startsWith("catEntryEcosystem_"));
        }

        @Test
        @DisplayName("ECOSYSTEM_NAME should have correct value")
        void ecosystemNameShouldHaveCorrectValue() {
            assertEquals("Collaboration", ConfluenceConstants.ECOSYSTEM_NAME);
        }

        @Test
        @DisplayName("ECOSYSTEM_VERSION should have correct value")
        void ecosystemVersionShouldHaveCorrectValue() {
            assertEquals("77af6758-3926-41c4-9145-061944792713", ConfluenceConstants.ECOSYSTEM_VERSION);
        }
    }

    @Test
    @DisplayName("Constructor should be private")
    void constructorShouldBePrivate() throws NoSuchMethodException {
        Constructor<ConfluenceConstants> constructor = ConfluenceConstants.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()),
                "Constructor should be private");
    }

    @Test
    @DisplayName("Private constructor should be invocable via reflection")
    void privateConstructorShouldBeInvocableViaReflection() throws Exception {
        Constructor<ConfluenceConstants> constructor = ConfluenceConstants.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        ConfluenceConstants instance = constructor.newInstance();
        assertNotNull(instance);
    }

    @Test
    @DisplayName("All constants should be non-null")
    void allConstantsShouldBeNonNull() {
        assertNotNull(ConfluenceConstants.AUTHORIZATION_EMAIL);
        assertNotNull(ConfluenceConstants.API_KEY);
        assertNotNull(ConfluenceConstants.DOMAIN_NAME);
        assertNotNull(ConfluenceConstants.CLIENT_ID);
        assertNotNull(ConfluenceConstants.CLIENT_SECRET);
        assertNotNull(ConfluenceConstants.CREDENTIALS_MODE);
        assertNotNull(ConfluenceConstants.SERVICE_ACCOUNT);
        assertNotNull(ConfluenceConstants.BEARER);
        assertNotNull(ConfluenceConstants.REFRESH_TOKEN);
        assertNotNull(ConfluenceConstants.CODE);
        assertNotNull(ConfluenceConstants.STATE);
        assertNotNull(ConfluenceConstants.JAXRS_ID);
        assertNotNull(ConfluenceConstants.AUTHORIZATION_PROMPT);
        assertNotNull(ConfluenceConstants.DOMAIN_ID);
        assertNotNull(ConfluenceConstants.KRISTA_DOMAIN_NAME);
        assertNotNull(ConfluenceConstants.ECOSYSTEM_ID);
        assertNotNull(ConfluenceConstants.ECOSYSTEM_NAME);
        assertNotNull(ConfluenceConstants.ECOSYSTEM_VERSION);
    }
}
