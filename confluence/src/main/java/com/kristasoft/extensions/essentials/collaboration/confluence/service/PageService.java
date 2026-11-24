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

package com.kristasoft.extensions.essentials.collaboration.confluence.service;

import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.entities.Content;
import com.kristasoft.extensions.essentials.collaboration.confluence.dto.Page;
import com.kristasoft.extensions.essentials.collaboration.confluence.dto.Variable;

import java.util.List;
import java.util.Map;

public interface PageService {

    /**
     * V2 implementation of "get pages in a space".
     *
     * <p>Backed by {@code GET /wiki/api/v2/spaces/{spaceId}/pages} — uses the numeric
     * Space Id directly (no Space Key lookup), supports cursor-based pagination, and
     * surfaces non-200 responses as {@link com.kristasoft.extensions.essentials.collaboration.confluence.exception.ConfluenceRESTException}
     * (no silent empty array).
     *
     * <p>When {@code excludeRestricted} is {@code true}, each fetched page is checked
     * against {@code GET /wiki/api/v2/pages/{id}/operations} and any page where the
     * {@code read} operation reports a content/space restriction is filtered out.
     *
     * <p>When {@code excludeContent} is {@code true} (the default in the catalog UI),
     * the request omits the {@code body-format} query parameter so Confluence returns
     * each page without its storage-format body. This keeps response payloads small
     * for spaces with many or large pages.
     *
     * <p>When {@code status} is non-null, non-empty and not {@code "any"}, it is passed to the
     * v2 pages endpoint as the {@code status} query parameter so the server returns only pages of
     * that status (e.g. {@code current}, {@code archived}, {@code trashed}). Non-matching pages are
     * never fetched and therefore never incur a restriction call. {@code "any"}/null/empty applies
     * no status filter (the endpoint's default set).
     */
    Page[] getPagesInSpaceV2(String spaceId, boolean excludeRestricted, boolean excludeContent,
                             String status);

    Page[] getPagesBySpaceId(String spaceId);

    Page getPageById(String id);

    // New methods for hierarchical page navigation
    Page[] getDirectChildPagesOfSpace(String spaceId);
    Page[] getDirectChildPagesOfPage(String pageId);
    boolean hasChildPages(String pageId);

    /**
     * Check if a content item has children, using the appropriate endpoint based on type.
     * @param contentId the ID of the content item (page, folder, etc.)
     * @param contentType the type of content ("page", "folder", "whiteboard", "database", "embed")
     * @return true if the content has children, false otherwise
     */
    boolean hasChildren(String contentId, String contentType);

    /**
     * Get direct children of a content item using type-aware API endpoint.
     * Uses Strategy pattern to determine the correct V2 API endpoint.
     * Supports all content types: page, folder, whiteboard, database, embed.
     *
     * @param contentId the content ID to get children from
     * @param contentType the type of content ("page", "folder", "whiteboard", "database", "embed")
     * @return Array of direct children
     */
    Page[] getDirectChildrenOfContent(String contentId, String contentType);

    /**
     * Get all ancestors of a content item using type-aware V2 API endpoint.
     * Supports all content types: page, folder, whiteboard, database, embed.
     * Ancestors are returned in top-to-bottom order (highest ancestor first).
     *
     * @param contentId the content ID to get ancestors from
     * @param contentType the type of content ("page", "folder", "whiteboard", "database", "embed")
     * @return Array of all ancestor content items
     */
    Page[] getAncestorsOfContent(String contentId, String contentType);

    Page createPageFromTemplate(String spaceId, String templateId, String title, List<Map<String, Object>> variables);
    Page createPageFromContent(String spaceId, String title, List<Content> contents);
    Page updatePageFromTemplate(String id, String templateId, String status, String title, String comment, List<Map<String, Object>> variables);
    Page updatePageFromContent(String id, String status, String title, String comment, List<Content> contents);
}
