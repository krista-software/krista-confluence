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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for the ContentType enum.
 */
@DisplayName("ContentType Enum Tests")
class ContentTypeTest {

    @Test
    @DisplayName("Should have exactly 5 content types")
    void shouldHaveExactlyFiveContentTypes() {
        assertEquals(5, ContentType.values().length);
    }

    @ParameterizedTest
    @MethodSource("provideContentTypeProperties")
    @DisplayName("Should have correct properties for each content type")
    void shouldHaveCorrectProperties(ContentType type, String expectedTypeName, 
                                      String expectedApiPath, String expectedChildrenEndpoint,
                                      String expectedAncestorsEndpoint) {
        assertEquals(expectedTypeName, type.getTypeName());
        assertEquals(expectedApiPath, type.getApiPathSegment());
        assertEquals(expectedChildrenEndpoint, type.getChildrenEndpoint());
        assertEquals(expectedAncestorsEndpoint, type.getAncestorsEndpoint());
    }

    private static Stream<Arguments> provideContentTypeProperties() {
        return Stream.of(
            Arguments.of(ContentType.PAGE, "page", "pages", "direct-children", "ancestors"),
            Arguments.of(ContentType.FOLDER, "folder", "folders", "direct-children", "ancestors"),
            Arguments.of(ContentType.WHITEBOARD, "whiteboard", "whiteboards", "direct-children", "ancestors"),
            Arguments.of(ContentType.DATABASE, "database", "databases", "direct-children", "ancestors"),
            Arguments.of(ContentType.EMBED, "embed", "embeds", "direct-children", "ancestors")
        );
    }

    @ParameterizedTest
    @MethodSource("provideMatchingTypes")
    @DisplayName("Should match type names case-insensitively")
    void shouldMatchTypeNamesCaseInsensitively(ContentType type, String input) {
        assertTrue(type.matches(input));
    }

    private static Stream<Arguments> provideMatchingTypes() {
        return Stream.of(
            Arguments.of(ContentType.PAGE, "page"),
            Arguments.of(ContentType.PAGE, "PAGE"),
            Arguments.of(ContentType.PAGE, "Page"),
            Arguments.of(ContentType.FOLDER, "folder"),
            Arguments.of(ContentType.FOLDER, "FOLDER"),
            Arguments.of(ContentType.WHITEBOARD, "whiteboard"),
            Arguments.of(ContentType.DATABASE, "database"),
            Arguments.of(ContentType.EMBED, "embed")
        );
    }

    @Test
    @DisplayName("Should not match incorrect type names")
    void shouldNotMatchIncorrectTypeNames() {
        assertFalse(ContentType.PAGE.matches("folder"));
        assertFalse(ContentType.FOLDER.matches("page"));
        assertFalse(ContentType.WHITEBOARD.matches("database"));
    }

    @ParameterizedTest
    @MethodSource("provideFromStringTestCases")
    @DisplayName("fromString should return correct ContentType")
    void fromStringShouldReturnCorrectContentType(String input, ContentType expected) {
        assertEquals(expected, ContentType.fromString(input));
    }

    private static Stream<Arguments> provideFromStringTestCases() {
        return Stream.of(
            Arguments.of("page", ContentType.PAGE),
            Arguments.of("PAGE", ContentType.PAGE),
            Arguments.of("folder", ContentType.FOLDER),
            Arguments.of("FOLDER", ContentType.FOLDER),
            Arguments.of("whiteboard", ContentType.WHITEBOARD),
            Arguments.of("database", ContentType.DATABASE),
            Arguments.of("embed", ContentType.EMBED),
            Arguments.of("unknown", ContentType.PAGE),  // Default to PAGE
            Arguments.of("invalid", ContentType.PAGE)   // Default to PAGE
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("fromString should return PAGE for null or empty input")
    void fromStringShouldReturnPageForNullOrEmpty(String input) {
        assertEquals(ContentType.PAGE, ContentType.fromString(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {"page", "folder", "whiteboard", "database", "embed"})
    @DisplayName("isKnownType should return true for known types")
    void isKnownTypeShouldReturnTrueForKnownTypes(String type) {
        assertTrue(ContentType.isKnownType(type));
    }

    @ParameterizedTest
    @ValueSource(strings = {"unknown", "invalid", "random", "test"})
    @DisplayName("isKnownType should return false for unknown types")
    void isKnownTypeShouldReturnFalseForUnknownTypes(String type) {
        assertFalse(ContentType.isKnownType(type));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("isKnownType should return false for null or empty input")
    void isKnownTypeShouldReturnFalseForNullOrEmpty(String input) {
        assertFalse(ContentType.isKnownType(input));
    }

    @Test
    @DisplayName("getChildrenPathComponents should return correct path array")
    void getChildrenPathComponentsShouldReturnCorrectPath() {
        String contentId = "123456";
        
        assertArrayEquals(new String[]{"pages", "123456", "direct-children"}, 
                         ContentType.PAGE.getChildrenPathComponents(contentId));
        assertArrayEquals(new String[]{"folders", "123456", "direct-children"}, 
                         ContentType.FOLDER.getChildrenPathComponents(contentId));
        assertArrayEquals(new String[]{"whiteboards", "123456", "direct-children"}, 
                         ContentType.WHITEBOARD.getChildrenPathComponents(contentId));
        assertArrayEquals(new String[]{"databases", "123456", "direct-children"}, 
                         ContentType.DATABASE.getChildrenPathComponents(contentId));
        assertArrayEquals(new String[]{"embeds", "123456", "direct-children"}, 
                         ContentType.EMBED.getChildrenPathComponents(contentId));
    }

    @Test
    @DisplayName("getAncestorsPathComponents should return correct path array")
    void getAncestorsPathComponentsShouldReturnCorrectPath() {
        String contentId = "789012";
        
        assertArrayEquals(new String[]{"pages", "789012", "ancestors"}, 
                         ContentType.PAGE.getAncestorsPathComponents(contentId));
        assertArrayEquals(new String[]{"folders", "789012", "ancestors"}, 
                         ContentType.FOLDER.getAncestorsPathComponents(contentId));
        assertArrayEquals(new String[]{"whiteboards", "789012", "ancestors"}, 
                         ContentType.WHITEBOARD.getAncestorsPathComponents(contentId));
        assertArrayEquals(new String[]{"databases", "789012", "ancestors"}, 
                         ContentType.DATABASE.getAncestorsPathComponents(contentId));
        assertArrayEquals(new String[]{"embeds", "789012", "ancestors"}, 
                         ContentType.EMBED.getAncestorsPathComponents(contentId));
    }
}

