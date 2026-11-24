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

package com.kristasoft.extensions.essentials.collaboration.confluence.authentication;

import app.krista.sdk.atlassian.config.AtlassianProduct;
import app.krista.sdk.atlassian.registry.annotations.AtlassianExtension;
import app.krista.sdk.atlassian.registry.annotations.OAuthScope;
import com.kristasoft.extensions.essentials.collaboration.confluence.utils.ConfluenceConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for the ConfluenceExtensionConfig annotation-based config class.
 */
@DisplayName("ConfluenceExtensionConfig Tests")
class ConfluenceExtensionConfigTest {

    @Nested
    @DisplayName("@AtlassianExtension annotation tests")
    class AtlassianExtensionAnnotationTests {

        @Test
        @DisplayName("Class should be annotated with @AtlassianExtension")
        void classShouldBeAnnotatedWithAtlassianExtension() {
            assertTrue(ConfluenceExtensionConfig.class.isAnnotationPresent(AtlassianExtension.class));
        }

        @Test
        @DisplayName("extensionType should be 'confluence'")
        void extensionTypeShouldBeConfluence() {
            AtlassianExtension annotation = ConfluenceExtensionConfig.class.getAnnotation(AtlassianExtension.class);
            assertEquals("confluence", annotation.extensionType());
        }

        @Test
        @DisplayName("product should be AtlassianProduct.CONFLUENCE")
        void productShouldBeConfluence() {
            AtlassianExtension annotation = ConfluenceExtensionConfig.class.getAnnotation(AtlassianExtension.class);
            assertEquals(AtlassianProduct.CONFLUENCE, annotation.product());
        }

        @Test
        @DisplayName("jaxrsId should match ConfluenceConstants.JAXRS_ID")
        void jaxrsIdShouldMatchConfluenceConstant() {
            AtlassianExtension annotation = ConfluenceExtensionConfig.class.getAnnotation(AtlassianExtension.class);
            assertEquals(ConfluenceConstants.JAXRS_ID, annotation.jaxrsId());
            assertEquals("confluenceextn", annotation.jaxrsId());
        }

        @Test
        @DisplayName("displayName should be 'Confluence'")
        void displayNameShouldBeConfluence() {
            AtlassianExtension annotation = ConfluenceExtensionConfig.class.getAnnotation(AtlassianExtension.class);
            assertEquals("Confluence", annotation.displayName());
        }
    }

    @Nested
    @DisplayName("@OAuthScope annotation tests")
    class OAuthScopeAnnotationTests {

        private OAuthScope[] getOAuthScopes() {
            return ConfluenceExtensionConfig.class.getAnnotationsByType(OAuthScope.class);
        }

        @Test
        @DisplayName("Should have exactly 14 OAuthScope annotations")
        void shouldHaveExactly14OAuthScopes() {
            OAuthScope[] scopes = getOAuthScopes();
            assertEquals(14, scopes.length);
        }

        @Test
        @DisplayName("Should contain read:space:confluence scope")
        void shouldContainReadSpaceScope() {
            OAuthScope scope = findScopeByValue("read:space:confluence");
            assertNotNull(scope, "read:space:confluence scope should exist");
            assertEquals("Read Confluence spaces", scope.description());
            assertArrayEquals(
                    new String[]{"Get Space By Id", "Get Spaces", "Get Direct Child Pages of Space"},
                    scope.catalogRequests()
            );
        }

        @Test
        @DisplayName("Should contain write:space:confluence scope")
        void shouldContainWriteSpaceScope() {
            OAuthScope scope = findScopeByValue("write:space:confluence");
            assertNotNull(scope, "write:space:confluence scope should exist");
            assertEquals("Create and update Confluence spaces", scope.description());
            assertEquals(0, scope.catalogRequests().length);
        }

        @Test
        @DisplayName("Should contain read:page:confluence scope with all catalog requests")
        void shouldContainReadPageScope() {
            OAuthScope scope = findScopeByValue("read:page:confluence");
            assertNotNull(scope, "read:page:confluence scope should exist");
            assertEquals("Read Confluence pages", scope.description());
            String[] expectedRequests = {
                    "Get Page By ID", "Get Pages in Space", "Get Page in Space",
                    "Get Direct Child Pages of Page", "Check if Page Has Children",
                    "Get All Nested Descendants of Page", "Get Ancestors of Page",
                    "Get Direct Child Pages of Space"
            };
            assertArrayEquals(expectedRequests, scope.catalogRequests());
        }

        @Test
        @DisplayName("Should contain write:page:confluence scope")
        void shouldContainWritePageScope() {
            OAuthScope scope = findScopeByValue("write:page:confluence");
            assertNotNull(scope, "write:page:confluence scope should exist");
            assertEquals("Create and update Confluence pages", scope.description());
            assertArrayEquals(
                    new String[]{"Create Page from Content", "Create Page from Template",
                            "Update Page by Content", "Update Page from Template"},
                    scope.catalogRequests()
            );
        }

        @Test
        @DisplayName("Should contain read:content:confluence scope")
        void shouldContainReadContentScope() {
            OAuthScope scope = findScopeByValue("read:content:confluence");
            assertNotNull(scope, "read:content:confluence scope should exist");
            assertEquals("Read Confluence content", scope.description());
            assertArrayEquals(
                    new String[]{"Generate Content for Table", "Generate Heading Content", "Generate Anchor Tag"},
                    scope.catalogRequests()
            );
        }

        @Test
        @DisplayName("Should contain write:content:confluence scope")
        void shouldContainWriteContentScope() {
            OAuthScope scope = findScopeByValue("write:content:confluence");
            assertNotNull(scope, "write:content:confluence scope should exist");
            assertEquals("Create and update Confluence content", scope.description());
            assertArrayEquals(
                    new String[]{"Generate Content for Table", "Generate Heading Content", "Generate Anchor Tag"},
                    scope.catalogRequests()
            );
        }

        @Test
        @DisplayName("Should contain read:content-details:confluence scope")
        void shouldContainReadContentDetailsScope() {
            OAuthScope scope = findScopeByValue("read:content-details:confluence");
            assertNotNull(scope, "read:content-details:confluence scope should exist");
            assertEquals("Read Confluence content details", scope.description());
            assertArrayEquals(
                    new String[]{"Get Page By ID", "Get Page in Space"},
                    scope.catalogRequests()
            );
        }

        @Test
        @DisplayName("Should contain read:user:confluence scope")
        void shouldContainReadUserScope() {
            OAuthScope scope = findScopeByValue("read:user:confluence");
            assertNotNull(scope, "read:user:confluence scope should exist");
            assertEquals("Read Confluence user information", scope.description());
            assertArrayEquals(
                    new String[]{"Get User By Account ID"},
                    scope.catalogRequests()
            );
        }

        @Test
        @DisplayName("Should contain read:template:confluence scope")
        void shouldContainReadTemplateScope() {
            OAuthScope scope = findScopeByValue("read:template:confluence");
            assertNotNull(scope, "read:template:confluence scope should exist");
            assertEquals("Read Confluence templates", scope.description());
            assertArrayEquals(
                    new String[]{"Get Template by ID", "Get Templates By Space ID",
                            "Create Page from Template", "Update Page from Template"},
                    scope.catalogRequests()
            );
        }

        @Test
        @DisplayName("Should contain write:template:confluence scope")
        void shouldContainWriteTemplateScope() {
            OAuthScope scope = findScopeByValue("write:template:confluence");
            assertNotNull(scope, "write:template:confluence scope should exist");
            assertEquals("Create and update Confluence templates", scope.description());
            assertEquals(0, scope.catalogRequests().length);
        }

        @Test
        @DisplayName("Should contain read:attachment:confluence scope")
        void shouldContainReadAttachmentScope() {
            OAuthScope scope = findScopeByValue("read:attachment:confluence");
            assertNotNull(scope, "read:attachment:confluence scope should exist");
            assertEquals("Read Confluence attachments", scope.description());
            assertArrayEquals(
                    new String[]{"Get Attachments for Page", "Get Attachment by ID", "Download Attachment"},
                    scope.catalogRequests()
            );
        }

        @Test
        @DisplayName("Should contain write:attachment:confluence scope")
        void shouldContainWriteAttachmentScope() {
            OAuthScope scope = findScopeByValue("write:attachment:confluence");
            assertNotNull(scope, "write:attachment:confluence scope should exist");
            assertEquals("Upload Confluence attachments", scope.description());
            assertEquals(0, scope.catalogRequests().length);
        }

        @Test
        @DisplayName("Should contain read:configuration:confluence scope")
        void shouldContainReadConfigurationScope() {
            OAuthScope scope = findScopeByValue("read:configuration:confluence");
            assertNotNull(scope, "read:configuration:confluence scope should exist");
            assertEquals("Read Confluence configuration and webhooks", scope.description());
            assertArrayEquals(
                    new String[]{"Get Notification"},
                    scope.catalogRequests()
            );
        }

        @Test
        @DisplayName("Should contain write:configuration:confluence scope")
        void shouldContainWriteConfigurationScope() {
            OAuthScope scope = findScopeByValue("write:configuration:confluence");
            assertNotNull(scope, "write:configuration:confluence scope should exist");
            assertEquals("Create and update Confluence configuration", scope.description());
            assertArrayEquals(
                    new String[]{"Create Webhook"},
                    scope.catalogRequests()
            );
        }

        @Test
        @DisplayName("All scope values should follow pattern 'action:resource:confluence'")
        void allScopeValuesShouldFollowPattern() {
            OAuthScope[] scopes = getOAuthScopes();
            for (OAuthScope scope : scopes) {
                assertTrue(scope.value().endsWith(":confluence"),
                        "Scope '" + scope.value() + "' should end with ':confluence'");
                assertTrue(scope.value().startsWith("read:") || scope.value().startsWith("write:"),
                        "Scope '" + scope.value() + "' should start with 'read:' or 'write:'");
            }
        }

        @Test
        @DisplayName("All scopes should have non-empty descriptions")
        void allScopesShouldHaveNonEmptyDescriptions() {
            OAuthScope[] scopes = getOAuthScopes();
            for (OAuthScope scope : scopes) {
                assertNotNull(scope.description());
                assertFalse(scope.description().isEmpty(),
                        "Description for scope '" + scope.value() + "' should not be empty");
            }
        }

        @Test
        @DisplayName("All scope values should be unique")
        void allScopeValuesShouldBeUnique() {
            OAuthScope[] scopes = getOAuthScopes();
            List<String> scopeValues = Arrays.stream(scopes)
                    .map(OAuthScope::value)
                    .collect(Collectors.toList());
            long uniqueCount = scopeValues.stream().distinct().count();
            assertEquals(scopeValues.size(), uniqueCount,
                    "All scope values should be unique");
        }

        private OAuthScope findScopeByValue(String value) {
            OAuthScope[] scopes = getOAuthScopes();
            for (OAuthScope scope : scopes) {
                if (scope.value().equals(value)) {
                    return scope;
                }
            }
            return null;
        }
    }

    @Test
    @DisplayName("Constructor should be private")
    void constructorShouldBePrivate() throws NoSuchMethodException {
        Constructor<ConfluenceExtensionConfig> constructor =
                ConfluenceExtensionConfig.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()),
                "Constructor should be private");
    }

    @Test
    @DisplayName("Private constructor should be invocable via reflection")
    void privateConstructorShouldBeInvocableViaReflection() throws Exception {
        Constructor<ConfluenceExtensionConfig> constructor =
                ConfluenceExtensionConfig.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        ConfluenceExtensionConfig instance = constructor.newInstance();
        assertNotNull(instance);
    }

    @Test
    @DisplayName("Class should be declared final")
    void classShouldBeFinal() {
        assertTrue(Modifier.isFinal(ConfluenceExtensionConfig.class.getModifiers()),
                "ConfluenceExtensionConfig class should be declared final");
    }
}
