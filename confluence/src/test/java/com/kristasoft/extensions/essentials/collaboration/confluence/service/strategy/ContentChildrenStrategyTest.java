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

package com.kristasoft.extensions.essentials.collaboration.confluence.service.strategy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for the {@link ContentChildrenStrategy} interface,
 * including the default {@code supports()} method.
 */
@DisplayName("ContentChildrenStrategy Interface Tests")
class ContentChildrenStrategyTest {

    /**
     * Creates an anonymous implementation of the strategy for the given ContentType.
     */
    private ContentChildrenStrategy createStrategy(ContentType contentType) {
        return new ContentChildrenStrategy() {
            @Override
            public String[] getChildrenPathComponents(String contentId) {
                return contentType.getChildrenPathComponents(contentId);
            }

            @Override
            public ContentType getContentType() {
                return contentType;
            }
        };
    }

    @Nested
    @DisplayName("Default supports() method")
    class SupportsTests {

        @Test
        @DisplayName("Should return true when type matches PAGE")
        void shouldReturnTrueForPageType() {
            ContentChildrenStrategy strategy = createStrategy(ContentType.PAGE);
            assertTrue(strategy.supports("page"));
        }

        @Test
        @DisplayName("Should return true for case-insensitive match with PAGE")
        void shouldReturnTrueForCaseInsensitivePageMatch() {
            ContentChildrenStrategy strategy = createStrategy(ContentType.PAGE);
            assertTrue(strategy.supports("PAGE"));
            assertTrue(strategy.supports("Page"));
            assertTrue(strategy.supports("pAgE"));
        }

        @Test
        @DisplayName("Should return false for non-matching type with PAGE strategy")
        void shouldReturnFalseForNonMatchingType() {
            ContentChildrenStrategy strategy = createStrategy(ContentType.PAGE);
            assertFalse(strategy.supports("folder"));
            assertFalse(strategy.supports("whiteboard"));
            assertFalse(strategy.supports("database"));
            assertFalse(strategy.supports("embed"));
        }

        @Test
        @DisplayName("Should return true when type matches FOLDER")
        void shouldReturnTrueForFolderType() {
            ContentChildrenStrategy strategy = createStrategy(ContentType.FOLDER);
            assertTrue(strategy.supports("folder"));
            assertTrue(strategy.supports("FOLDER"));
        }

        @Test
        @DisplayName("Should return false for FOLDER strategy with non-matching type")
        void shouldReturnFalseForFolderNonMatch() {
            ContentChildrenStrategy strategy = createStrategy(ContentType.FOLDER);
            assertFalse(strategy.supports("page"));
        }

        @Test
        @DisplayName("Should return true when type matches WHITEBOARD")
        void shouldReturnTrueForWhiteboardType() {
            ContentChildrenStrategy strategy = createStrategy(ContentType.WHITEBOARD);
            assertTrue(strategy.supports("whiteboard"));
            assertTrue(strategy.supports("WHITEBOARD"));
        }

        @Test
        @DisplayName("Should return true when type matches DATABASE")
        void shouldReturnTrueForDatabaseType() {
            ContentChildrenStrategy strategy = createStrategy(ContentType.DATABASE);
            assertTrue(strategy.supports("database"));
            assertTrue(strategy.supports("DATABASE"));
        }

        @Test
        @DisplayName("Should return true when type matches EMBED")
        void shouldReturnTrueForEmbedType() {
            ContentChildrenStrategy strategy = createStrategy(ContentType.EMBED);
            assertTrue(strategy.supports("embed"));
            assertTrue(strategy.supports("EMBED"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"unknown", "invalid", "", "null", "pages", "folders"})
        @DisplayName("Should return false for unknown type strings")
        void shouldReturnFalseForUnknownTypes(String type) {
            ContentChildrenStrategy strategy = createStrategy(ContentType.PAGE);
            if (!"page".equalsIgnoreCase(type)) {
                assertFalse(strategy.supports(type));
            }
        }
    }

    @Nested
    @DisplayName("Strategy interface contract")
    class ContractTests {

        @ParameterizedTest
        @EnumSource(ContentType.class)
        @DisplayName("Anonymous implementation should return correct ContentType for each enum value")
        void shouldReturnCorrectContentType(ContentType contentType) {
            ContentChildrenStrategy strategy = createStrategy(contentType);
            assertEquals(contentType, strategy.getContentType());
        }

        @ParameterizedTest
        @EnumSource(ContentType.class)
        @DisplayName("Anonymous implementation should support its own type name")
        void shouldSupportOwnTypeName(ContentType contentType) {
            ContentChildrenStrategy strategy = createStrategy(contentType);
            assertTrue(strategy.supports(contentType.getTypeName()));
        }

        @Test
        @DisplayName("getChildrenPathComponents should return correct path for PAGE")
        void shouldReturnCorrectPathForPage() {
            ContentChildrenStrategy strategy = createStrategy(ContentType.PAGE);
            String[] components = strategy.getChildrenPathComponents("12345");
            assertArrayEquals(new String[]{"pages", "12345", "direct-children"}, components);
        }

        @Test
        @DisplayName("getChildrenPathComponents should return correct path for FOLDER")
        void shouldReturnCorrectPathForFolder() {
            ContentChildrenStrategy strategy = createStrategy(ContentType.FOLDER);
            String[] components = strategy.getChildrenPathComponents("67890");
            assertArrayEquals(new String[]{"folders", "67890", "direct-children"}, components);
        }

        @Test
        @DisplayName("getChildrenPathComponents should return correct path for WHITEBOARD")
        void shouldReturnCorrectPathForWhiteboard() {
            ContentChildrenStrategy strategy = createStrategy(ContentType.WHITEBOARD);
            String[] components = strategy.getChildrenPathComponents("111");
            assertArrayEquals(new String[]{"whiteboards", "111", "direct-children"}, components);
        }

        @Test
        @DisplayName("getChildrenPathComponents should return correct path for DATABASE")
        void shouldReturnCorrectPathForDatabase() {
            ContentChildrenStrategy strategy = createStrategy(ContentType.DATABASE);
            String[] components = strategy.getChildrenPathComponents("222");
            assertArrayEquals(new String[]{"databases", "222", "direct-children"}, components);
        }

        @Test
        @DisplayName("getChildrenPathComponents should return correct path for EMBED")
        void shouldReturnCorrectPathForEmbed() {
            ContentChildrenStrategy strategy = createStrategy(ContentType.EMBED);
            String[] components = strategy.getChildrenPathComponents("333");
            assertArrayEquals(new String[]{"embeds", "333", "direct-children"}, components);
        }
    }

    @Nested
    @DisplayName("supports() delegates to ContentType.matches()")
    class DelegationTests {

        @Test
        @DisplayName("Default supports method should delegate to ContentType.matches()")
        void defaultSupportsMethodShouldDelegateToMatches() {
            // Create a strategy and verify that supports() behavior matches ContentType.matches()
            for (ContentType type : ContentType.values()) {
                ContentChildrenStrategy strategy = createStrategy(type);
                // The supports() default method should match the result of ContentType.matches()
                assertEquals(type.matches("page"), strategy.supports("page"),
                        "supports('page') should match ContentType.matches('page') for " + type);
                assertEquals(type.matches("folder"), strategy.supports("folder"),
                        "supports('folder') should match ContentType.matches('folder') for " + type);
            }
        }

        @Test
        @DisplayName("Each strategy should only support its own type")
        void eachStrategyShouldOnlySupportItsOwnType() {
            for (ContentType type : ContentType.values()) {
                ContentChildrenStrategy strategy = createStrategy(type);
                for (ContentType otherType : ContentType.values()) {
                    if (type == otherType) {
                        assertTrue(strategy.supports(otherType.getTypeName()),
                                type + " strategy should support " + otherType.getTypeName());
                    } else {
                        assertFalse(strategy.supports(otherType.getTypeName()),
                                type + " strategy should not support " + otherType.getTypeName());
                    }
                }
            }
        }
    }
}
