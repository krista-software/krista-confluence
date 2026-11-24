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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.Map;

/**
 * Factory class for creating ContentChildrenStrategy instances based on content type.
 * 
 * <p>This factory uses a cache of strategy instances to avoid creating new objects
 * for each request. Since strategies are stateless, they can be safely reused.</p>
 * 
 * <p>Usage example:</p>
 * <pre>{@code
 * ContentChildrenStrategyFactory factory = ContentChildrenStrategyFactory.getInstance();
 * ContentChildrenStrategy strategy = factory.getStrategy("folder");
 * String[] pathComponents = strategy.getChildrenPathComponents("123456");
 * // pathComponents = ["folders", "123456", "direct-children"]
 * }</pre>
 * 
 * @see ContentChildrenStrategy
 * @see ContentType
 */
public class ContentChildrenStrategyFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContentChildrenStrategyFactory.class);

    // Singleton instance
    private static final ContentChildrenStrategyFactory INSTANCE = new ContentChildrenStrategyFactory();

    // Cache of strategy instances, one per content type
    private final Map<ContentType, ContentChildrenStrategy> strategyCache;

    // Default strategy for unknown types
    private final ContentChildrenStrategy defaultStrategy;

    /**
     * Private constructor for singleton pattern.
     * Initializes the strategy cache with all known content types.
     */
    private ContentChildrenStrategyFactory() {
        this.strategyCache = new EnumMap<>(ContentType.class);
        
        // Pre-populate cache with strategies for all content types
        for (ContentType type : ContentType.values()) {
            strategyCache.put(type, new DefaultContentChildrenStrategy(type));
        }
        
        // Default strategy uses PAGE type
        this.defaultStrategy = strategyCache.get(ContentType.PAGE);
        
        LOGGER.debug("ContentChildrenStrategyFactory initialized with {} strategies", strategyCache.size());
    }

    /**
     * Gets the singleton instance of the factory.
     * 
     * @return the factory instance
     */
    public static ContentChildrenStrategyFactory getInstance() {
        return INSTANCE;
    }

    /**
     * Gets the appropriate strategy for the given content type string.
     * 
     * @param contentType the content type string (e.g., "page", "folder", "whiteboard")
     * @return the strategy for the content type, or the default (PAGE) strategy if unknown
     */
    public ContentChildrenStrategy getStrategy(String contentType) {
        ContentType type = ContentType.fromString(contentType);
        ContentChildrenStrategy strategy = strategyCache.get(type);
        
        if (strategy == null) {
            LOGGER.warn("No strategy found for content type '{}', using default PAGE strategy", contentType);
            return defaultStrategy;
        }
        
        LOGGER.debug("Using {} strategy for content type '{}'", type, contentType);
        return strategy;
    }

    /**
     * Gets the strategy for the given ContentType enum value.
     * 
     * @param contentType the ContentType enum value
     * @return the strategy for the content type
     */
    public ContentChildrenStrategy getStrategy(ContentType contentType) {
        if (contentType == null) {
            LOGGER.warn("Null content type provided, using default PAGE strategy");
            return defaultStrategy;
        }
        return strategyCache.getOrDefault(contentType, defaultStrategy);
    }

    /**
     * Gets the path components for retrieving children of the specified content.
     * This is a convenience method that combines getting the strategy and calling it.
     * 
     * @param contentId the ID of the content item
     * @param contentType the type of content (e.g., "page", "folder")
     * @return array of path components for the API call
     */
    public String[] getChildrenPathComponents(String contentId, String contentType) {
        return getStrategy(contentType).getChildrenPathComponents(contentId);
    }

    /**
     * Checks if the given content type is supported.
     * 
     * @param contentType the content type string to check
     * @return true if the content type is recognized and supported
     */
    public boolean isSupported(String contentType) {
        return ContentType.isKnownType(contentType);
    }

    /**
     * Gets the default strategy (PAGE).
     * 
     * @return the default strategy
     */
    public ContentChildrenStrategy getDefaultStrategy() {
        return defaultStrategy;
    }
}

