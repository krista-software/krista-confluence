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

/**
 * Strategy interface for handling children operations for different Confluence content types.
 * Each content type (page, folder, whiteboard, database, embed) has its own API endpoint
 * for retrieving children, and this strategy pattern allows for clean separation of concerns.
 * 
 * <p>Confluence V2 API endpoints for children:</p>
 * <ul>
 *   <li>Pages: GET /wiki/api/v2/pages/{id}/direct-children</li>
 *   <li>Folders: GET /wiki/api/v2/folders/{id}/direct-children</li>
 *   <li>Whiteboards: GET /wiki/api/v2/whiteboards/{id}/children</li>
 *   <li>Databases: GET /wiki/api/v2/databases/{id}/children</li>
 *   <li>Custom Content (Embeds): GET /wiki/api/v2/custom-content/{id}/children</li>
 * </ul>
 * 
 * @see ContentType
 * @see ContentChildrenStrategyFactory
 */
public interface ContentChildrenStrategy {

    /**
     * Gets the API path components for retrieving children of this content type.
     * The path components are used to build the full API URL.
     * 
     * @param contentId the ID of the content item
     * @return array of path components (e.g., ["pages", "123", "direct-children"])
     */
    String[] getChildrenPathComponents(String contentId);

    /**
     * Gets the content type this strategy handles.
     * 
     * @return the ContentType enum value
     */
    ContentType getContentType();

    /**
     * Checks if this strategy supports the given content type string.
     * 
     * @param type the content type string (case-insensitive)
     * @return true if this strategy handles the given type
     */
    default boolean supports(String type) {
        return getContentType().matches(type);
    }
}

