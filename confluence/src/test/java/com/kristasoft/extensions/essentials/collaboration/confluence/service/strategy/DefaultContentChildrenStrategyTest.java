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
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the DefaultContentChildrenStrategy class.
 */
@DisplayName("DefaultContentChildrenStrategy Tests")
class DefaultContentChildrenStrategyTest {

    @ParameterizedTest
    @EnumSource(ContentType.class)
    @DisplayName("Should create strategy for each content type")
    void shouldCreateStrategyForEachContentType(ContentType contentType) {
        DefaultContentChildrenStrategy strategy = new DefaultContentChildrenStrategy(contentType);
        
        assertNotNull(strategy);
        assertEquals(contentType, strategy.getContentType());
    }

    @Test
    @DisplayName("getChildrenPathComponents should delegate to ContentType")
    void getChildrenPathComponentsShouldDelegateToContentType() {
        String contentId = "12345";
        
        DefaultContentChildrenStrategy pageStrategy = new DefaultContentChildrenStrategy(ContentType.PAGE);
        assertArrayEquals(new String[]{"pages", "12345", "direct-children"}, 
                         pageStrategy.getChildrenPathComponents(contentId));
        
        DefaultContentChildrenStrategy folderStrategy = new DefaultContentChildrenStrategy(ContentType.FOLDER);
        assertArrayEquals(new String[]{"folders", "12345", "direct-children"}, 
                         folderStrategy.getChildrenPathComponents(contentId));
    }

    @Test
    @DisplayName("supports should return true for matching type")
    void supportsShouldReturnTrueForMatchingType() {
        DefaultContentChildrenStrategy strategy = new DefaultContentChildrenStrategy(ContentType.PAGE);
        
        assertTrue(strategy.supports("page"));
        assertTrue(strategy.supports("PAGE"));
        assertTrue(strategy.supports("Page"));
    }

    @Test
    @DisplayName("supports should return false for non-matching type")
    void supportsShouldReturnFalseForNonMatchingType() {
        DefaultContentChildrenStrategy strategy = new DefaultContentChildrenStrategy(ContentType.PAGE);
        
        assertFalse(strategy.supports("folder"));
        assertFalse(strategy.supports("whiteboard"));
        assertFalse(strategy.supports("database"));
    }

    @Test
    @DisplayName("toString should contain content type information")
    void toStringShouldContainContentTypeInfo() {
        DefaultContentChildrenStrategy strategy = new DefaultContentChildrenStrategy(ContentType.FOLDER);
        
        String result = strategy.toString();
        
        assertTrue(result.contains("DefaultContentChildrenStrategy"));
        assertTrue(result.contains("FOLDER"));
    }

    @ParameterizedTest
    @EnumSource(ContentType.class)
    @DisplayName("getContentType should return the correct type for all content types")
    void getContentTypeShouldReturnCorrectType(ContentType contentType) {
        DefaultContentChildrenStrategy strategy = new DefaultContentChildrenStrategy(contentType);
        
        assertEquals(contentType, strategy.getContentType());
    }

    @Test
    @DisplayName("Should correctly handle whiteboard content type")
    void shouldCorrectlyHandleWhiteboardContentType() {
        DefaultContentChildrenStrategy strategy = new DefaultContentChildrenStrategy(ContentType.WHITEBOARD);
        
        assertEquals(ContentType.WHITEBOARD, strategy.getContentType());
        assertTrue(strategy.supports("whiteboard"));
        assertArrayEquals(new String[]{"whiteboards", "999", "direct-children"}, 
                         strategy.getChildrenPathComponents("999"));
    }

    @Test
    @DisplayName("Should correctly handle database content type")
    void shouldCorrectlyHandleDatabaseContentType() {
        DefaultContentChildrenStrategy strategy = new DefaultContentChildrenStrategy(ContentType.DATABASE);
        
        assertEquals(ContentType.DATABASE, strategy.getContentType());
        assertTrue(strategy.supports("database"));
        assertArrayEquals(new String[]{"databases", "888", "direct-children"}, 
                         strategy.getChildrenPathComponents("888"));
    }

    @Test
    @DisplayName("Should correctly handle embed content type")
    void shouldCorrectlyHandleEmbedContentType() {
        DefaultContentChildrenStrategy strategy = new DefaultContentChildrenStrategy(ContentType.EMBED);
        
        assertEquals(ContentType.EMBED, strategy.getContentType());
        assertTrue(strategy.supports("embed"));
        assertArrayEquals(new String[]{"embeds", "777", "direct-children"}, 
                         strategy.getChildrenPathComponents("777"));
    }
}

