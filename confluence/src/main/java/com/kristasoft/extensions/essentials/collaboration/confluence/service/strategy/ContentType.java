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
 * Enum representing the different content types in Confluence that can have children and ancestors.
 * Each content type has its own API endpoint pattern for retrieving children and ancestors.
 *
 * <p>Content types returned by V2 API:</p>
 * <ul>
 *   <li>PAGE - A regular Confluence page with content</li>
 *   <li>FOLDER - An organizational container for pages and other content</li>
 *   <li>WHITEBOARD - A collaborative whiteboard</li>
 *   <li>DATABASE - A Confluence database</li>
 *   <li>EMBED - An embedded content item (uses custom-content API)</li>
 * </ul>
 *
 * <p>V2 API Endpoints:</p>
 * <ul>
 *   <li>Children: GET /wiki/api/v2/{apiPathSegment}/{id}/{childrenEndpoint}</li>
 *   <li>Ancestors: GET /wiki/api/v2/{apiPathSegment}/{id}/ancestors</li>
 * </ul>
 */
public enum ContentType {

    /**
     * Regular Confluence page.
     * Children API: GET /wiki/api/v2/pages/{id}/direct-children
     * Ancestors API: GET /wiki/api/v2/pages/{id}/ancestors
     */
    PAGE("page", "pages", "direct-children", "ancestors"),

    /**
     * Folder - organizational container.
     * Children API: GET /wiki/api/v2/folders/{id}/direct-children
     * Ancestors API: GET /wiki/api/v2/folders/{id}/ancestors
     */
    FOLDER("folder", "folders", "direct-children", "ancestors"),

    /**
     * Collaborative whiteboard.
     * Children API: GET /wiki/api/v2/whiteboards/{id}/direct-children
     * Ancestors API: GET /wiki/api/v2/whiteboards/{id}/ancestors
     */
    WHITEBOARD("whiteboard", "whiteboards", "direct-children", "ancestors"),

    /**
     * Confluence database.
     * Children API: GET /wiki/api/v2/databases/{id}/direct-children
     * Ancestors API: GET /wiki/api/v2/databases/{id}/ancestors
     */
    DATABASE("database", "databases", "direct-children", "ancestors"),

    /**
     * Embedded content item.
     * Children API: GET /wiki/api/v2/embeds/{id}/direct-children
     * Ancestors API: GET /wiki/api/v2/embeds/{id}/ancestors
     */
    EMBED("embed", "embeds", "direct-children", "ancestors");

    private final String typeName;
    private final String apiPathSegment;
    private final String childrenEndpoint;
    private final String ancestorsEndpoint;

    ContentType(String typeName, String apiPathSegment, String childrenEndpoint, String ancestorsEndpoint) {
        this.typeName = typeName;
        this.apiPathSegment = apiPathSegment;
        this.childrenEndpoint = childrenEndpoint;
        this.ancestorsEndpoint = ancestorsEndpoint;
    }

    /**
     * Gets the type name as returned by the Confluence API.
     * 
     * @return the type name (e.g., "page", "folder")
     */
    public String getTypeName() {
        return typeName;
    }

    /**
     * Gets the API path segment for this content type.
     * 
     * @return the path segment (e.g., "pages", "folders")
     */
    public String getApiPathSegment() {
        return apiPathSegment;
    }

    /**
     * Gets the children endpoint suffix for this content type.
     *
     * @return the endpoint suffix (e.g., "direct-children", "children")
     */
    public String getChildrenEndpoint() {
        return childrenEndpoint;
    }

    /**
     * Gets the ancestors endpoint suffix for this content type.
     *
     * @return the endpoint suffix (always "ancestors" for V2 API)
     */
    public String getAncestorsEndpoint() {
        return ancestorsEndpoint;
    }

    /**
     * Checks if this content type matches the given type string (case-insensitive).
     *
     * @param type the type string to check
     * @return true if this content type matches
     */
    public boolean matches(String type) {
        return typeName.equalsIgnoreCase(type);
    }

    /**
     * Gets the path components for retrieving children of this content type.
     *
     * @param contentId the ID of the content item
     * @return array of path components
     */
    public String[] getChildrenPathComponents(String contentId) {
        return new String[]{apiPathSegment, contentId, childrenEndpoint};
    }

    /**
     * Gets the path components for retrieving ancestors of this content type.
     *
     * @param contentId the ID of the content item
     * @return array of path components
     */
    public String[] getAncestorsPathComponents(String contentId) {
        return new String[]{apiPathSegment, contentId, ancestorsEndpoint};
    }

    /**
     * Finds the ContentType enum value for the given type string.
     * Returns PAGE as the default if no match is found.
     * 
     * @param type the type string (case-insensitive)
     * @return the matching ContentType, or PAGE if not found
     */
    public static ContentType fromString(String type) {
        if (type == null || type.isEmpty()) {
            return PAGE;
        }
        for (ContentType contentType : values()) {
            if (contentType.matches(type)) {
                return contentType;
            }
        }
        // Default to PAGE for unknown types
        return PAGE;
    }

    /**
     * Checks if the given type string is a known content type.
     * 
     * @param type the type string to check
     * @return true if the type is recognized
     */
    public static boolean isKnownType(String type) {
        if (type == null || type.isEmpty()) {
            return false;
        }
        for (ContentType contentType : values()) {
            if (contentType.matches(type)) {
                return true;
            }
        }
        return false;
    }
}

