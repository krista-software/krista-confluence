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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the ContentChildrenStrategyFactory class.
 */
@DisplayName("ContentChildrenStrategyFactory Tests")
class ContentChildrenStrategyFactoryTest {

    private ContentChildrenStrategyFactory factory;

    @BeforeEach
    void setUp() {
        factory = ContentChildrenStrategyFactory.getInstance();
    }

    @Test
    @DisplayName("getInstance should return singleton instance")
    void getInstanceShouldReturnSingletonInstance() {
        ContentChildrenStrategyFactory instance1 = ContentChildrenStrategyFactory.getInstance();
        ContentChildrenStrategyFactory instance2 = ContentChildrenStrategyFactory.getInstance();
        
        assertSame(instance1, instance2);
    }

    @ParameterizedTest
    @EnumSource(ContentType.class)
    @DisplayName("getStrategy with ContentType should return correct strategy")
    void getStrategyWithContentTypeShouldReturnCorrectStrategy(ContentType contentType) {
        ContentChildrenStrategy strategy = factory.getStrategy(contentType);
        
        assertNotNull(strategy);
        assertEquals(contentType, strategy.getContentType());
    }

    @Test
    @DisplayName("getStrategy with null ContentType should return default strategy")
    void getStrategyWithNullContentTypeShouldReturnDefaultStrategy() {
        ContentChildrenStrategy strategy = factory.getStrategy((ContentType) null);
        
        assertNotNull(strategy);
        assertEquals(ContentType.PAGE, strategy.getContentType());
    }

    @ParameterizedTest
    @ValueSource(strings = {"page", "PAGE", "Page"})
    @DisplayName("getStrategy with string should return correct strategy for page")
    void getStrategyWithStringShouldReturnCorrectStrategyForPage(String type) {
        ContentChildrenStrategy strategy = factory.getStrategy(type);
        
        assertNotNull(strategy);
        assertEquals(ContentType.PAGE, strategy.getContentType());
    }

    @ParameterizedTest
    @ValueSource(strings = {"folder", "FOLDER", "Folder"})
    @DisplayName("getStrategy with string should return correct strategy for folder")
    void getStrategyWithStringShouldReturnCorrectStrategyForFolder(String type) {
        ContentChildrenStrategy strategy = factory.getStrategy(type);
        
        assertNotNull(strategy);
        assertEquals(ContentType.FOLDER, strategy.getContentType());
    }

    @ParameterizedTest
    @ValueSource(strings = {"unknown", "invalid", "random"})
    @DisplayName("getStrategy with unknown type should return default PAGE strategy")
    void getStrategyWithUnknownTypeShouldReturnDefaultStrategy(String type) {
        ContentChildrenStrategy strategy = factory.getStrategy(type);
        
        assertNotNull(strategy);
        assertEquals(ContentType.PAGE, strategy.getContentType());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("getStrategy with null or empty string should return default strategy")
    void getStrategyWithNullOrEmptyStringShouldReturnDefaultStrategy(String type) {
        ContentChildrenStrategy strategy = factory.getStrategy(type);
        
        assertNotNull(strategy);
        assertEquals(ContentType.PAGE, strategy.getContentType());
    }

    @Test
    @DisplayName("getChildrenPathComponents should return correct path")
    void getChildrenPathComponentsShouldReturnCorrectPath() {
        String contentId = "12345";
        
        assertArrayEquals(new String[]{"pages", "12345", "direct-children"}, 
                         factory.getChildrenPathComponents(contentId, "page"));
        assertArrayEquals(new String[]{"folders", "12345", "direct-children"}, 
                         factory.getChildrenPathComponents(contentId, "folder"));
        assertArrayEquals(new String[]{"whiteboards", "12345", "direct-children"}, 
                         factory.getChildrenPathComponents(contentId, "whiteboard"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"page", "folder", "whiteboard", "database", "embed"})
    @DisplayName("isSupported should return true for known types")
    void isSupportedShouldReturnTrueForKnownTypes(String type) {
        assertTrue(factory.isSupported(type));
    }

    @ParameterizedTest
    @ValueSource(strings = {"unknown", "invalid", "random"})
    @DisplayName("isSupported should return false for unknown types")
    void isSupportedShouldReturnFalseForUnknownTypes(String type) {
        assertFalse(factory.isSupported(type));
    }

    @Test
    @DisplayName("getDefaultStrategy should return PAGE strategy")
    void getDefaultStrategyShouldReturnPageStrategy() {
        ContentChildrenStrategy defaultStrategy = factory.getDefaultStrategy();
        
        assertNotNull(defaultStrategy);
        assertEquals(ContentType.PAGE, defaultStrategy.getContentType());
    }

    @Test
    @DisplayName("Strategies should be cached and reused")
    void strategiesShouldBeCachedAndReused() {
        ContentChildrenStrategy strategy1 = factory.getStrategy("page");
        ContentChildrenStrategy strategy2 = factory.getStrategy("page");
        
        assertSame(strategy1, strategy2);
    }

    @Test
    @DisplayName("Different content types should return different strategies")
    void differentContentTypesShouldReturnDifferentStrategies() {
        ContentChildrenStrategy pageStrategy = factory.getStrategy("page");
        ContentChildrenStrategy folderStrategy = factory.getStrategy("folder");
        
        assertNotSame(pageStrategy, folderStrategy);
        assertNotEquals(pageStrategy.getContentType(), folderStrategy.getContentType());
    }
}

