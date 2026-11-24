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

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.kristasoft.extensions.essentials.collaboration.confluence.ConfluenceAttributes;
import com.kristasoft.extensions.essentials.collaboration.confluence.api.APIVersion;
import com.kristasoft.extensions.essentials.collaboration.confluence.constants.ConfluenceConstants;
import com.kristasoft.extensions.essentials.collaboration.confluence.dto.Page;
import com.kristasoft.extensions.essentials.collaboration.confluence.dto.v2.V2Ancestor;
import com.kristasoft.extensions.essentials.collaboration.confluence.dto.v2.V2AncestorsResponse;
import com.kristasoft.extensions.essentials.collaboration.confluence.dto.v2.V2ChildPage;
import com.kristasoft.extensions.essentials.collaboration.confluence.dto.v2.V2ChildrenResponse;
import com.kristasoft.extensions.essentials.collaboration.confluence.exception.ConfluenceRESTException;
import com.kristasoft.extensions.essentials.collaboration.confluence.service.strategy.ContentChildrenStrategyFactory;
import com.kristasoft.extensions.essentials.collaboration.confluence.service.strategy.ContentType;
import com.kristasoft.extensions.essentials.collaboration.confluence.utils.GsonFactory;
import com.kristasoft.extensions.essentials.collaboration.confluence.utils.PageConverter;
import com.kristasoft.extensions.essentials.collaboration.confluence.utils.UrlParser;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.net.HttpURLConnection;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Service for managing page hierarchy operations.
 * Handles children, ancestors, and descendant navigation.
 */
@Service
public class PageHierarchyService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PageHierarchyService.class);

    /**
     * Default pagination limit for API requests.
     */
    private static final int DEFAULT_PAGINATION_LIMIT = 50;

    private final ConfluenceAttributes confluenceAttributes;
    private final PageConverter pageConverter;
    private final String domainUrl;

    @Inject
    public PageHierarchyService(ConfluenceAttributes confluenceAttributes) {
        this.confluenceAttributes = confluenceAttributes;
        this.domainUrl = confluenceAttributes.getDomainURL();
        this.pageConverter = new PageConverter(confluenceAttributes, domainUrl);
    }

    /**
     * Get direct child pages of a space (root-level pages).
     *
     * @param spaceId the space ID
     * @return array of direct child pages
     */
    public Page[] getDirectChildPagesOfSpace(String spaceId) {
        List<Page> rootPages = getRootPagesInSpace(spaceId);

        return rootPages.stream()
                .peek(page -> page.setDomainUrl(domainUrl))
                .toArray(Page[]::new);
    }

    /**
     * Get direct child pages of a page.
     *
     * @param pageId the page ID
     * @return array of direct child pages
     */
    public Page[] getDirectChildPagesOfPage(String pageId) {
        // Use V2 API dedicated endpoint for getting direct child pages with pagination support
        // V2 API endpoint: GET /pages/{id}/direct-children
        List<Page> allChildPages = getAllChildrenOfContent(pageId, "page");

        // Set domain URL and return as array
        return allChildPages.stream()
                .peek(page -> page.setDomainUrl(domainUrl))
                .toArray(Page[]::new);
    }

    /**
     * Check if a page has child pages.
     *
     * @param pageId the page ID
     * @return true if the page has children, false otherwise
     */
    public boolean hasChildPages(String pageId) {
        // Delegate to the type-aware method with default type "page"
        return hasChildren(pageId, "page");
    }

    /**
     * Check if a content item has children, using the appropriate endpoint based on type.
     *
     * @param contentId the ID of the content item (page, folder, etc.)
     * @param contentType the type of content ("page", "folder", "whiteboard", "database", "embed")
     * @return true if the content has children, false otherwise
     */
    public boolean hasChildren(String contentId, String contentType) {
        // Use Strategy pattern to get the correct API endpoint for the content type
        // Supported types: page, folder, whiteboard, database, embed
        // Each type has its own V2 API endpoint for retrieving children
        ContentChildrenStrategyFactory strategyFactory = ContentChildrenStrategyFactory.getInstance();
        String[] pathComponents = strategyFactory.getChildrenPathComponents(contentId, contentType);

        // Make a request with limit=1 to just check if any children exist
        HttpResponse<String> response = confluenceAttributes.getRestClient().doSyncGet(
                APIVersion.V2,
                Map.of("limit", "1"),
                pathComponents);

        if (response.statusCode() == HttpURLConnection.HTTP_OK) {
            V2ChildrenResponse v2Response = GsonFactory.getInstance().fromJson(response.body(), V2ChildrenResponse.class);
            return v2Response.getResults() != null && !v2Response.getResults().isEmpty();
        } else {
            throw new ConfluenceRESTException("Failed to check if " + contentType + " has children. Status: " + response.statusCode() + ", Response: " + response.body());
        }
    }

    /**
     * Get all nested descendants of a content item recursively.
     * This method traverses the entire content hierarchy starting from the given content ID.
     * Supports all content types: page, folder, whiteboard, database, embed.
     *
     * @param contentId the content ID to get all descendants from
     * @param contentType the type of content ("page", "folder", "whiteboard", "database", "embed")
     * @return Array of all descendant content items (children, grandchildren, etc.)
     */
    public Page[] getAllNestedDescendants(String contentId, String contentType) {
        List<Page> allDescendants = new ArrayList<>();
        collectAllDescendants(contentId, contentType, allDescendants);

        return allDescendants.stream()
                .peek(page -> page.setDomainUrl(domainUrl))
                .toArray(Page[]::new);
    }

    /**
     * Get all nested descendants of a page recursively.
     * This is a convenience method that defaults to "page" content type.
     * For type-aware retrieval, use {@link #getAllNestedDescendants(String, String)}.
     *
     * @param pageId the page ID to get all descendants from
     * @return Array of all descendant pages (children, grandchildren, etc.)
     */
    public Page[] getAllNestedDescendants(String pageId) {
        return getAllNestedDescendants(pageId, "page");
    }

    /**
     * Get direct children of a content item using type-aware API endpoint.
     * Uses Strategy pattern to determine the correct V2 API endpoint.
     *
     * @param contentId the content ID to get children from
     * @param contentType the type of content ("page", "folder", "whiteboard", "database", "embed")
     * @return Array of direct children
     */
    public Page[] getDirectChildrenOfContent(String contentId, String contentType) {
        List<Page> allChildren = getAllChildrenOfContent(contentId, contentType);

        // Set domain URL and return as array
        return allChildren.stream()
                .peek(page -> page.setDomainUrl(domainUrl))
                .toArray(Page[]::new);
    }

    /**
     * Get all ancestors of a page.
     * This is a convenience method that defaults to "page" content type.
     * For type-aware retrieval, use {@link #getAncestorsOfContent(String, String)}.
     *
     * @param pageId the page ID to get ancestors from
     * @return Array of all ancestor pages
     */
    public Page[] getAncestorsOfPage(String pageId) {
        return getAncestorsOfContent(pageId, "page");
    }

    /**
     * Get all ancestors of a content item using type-aware V2 API endpoint.
     * Supports all content types: page, folder, whiteboard, database, embed.
     * Ancestors are returned in top-to-bottom order (highest ancestor first).
     *
     * @param contentId the content ID to get ancestors from
     * @param contentType the type of content ("page", "folder", "whiteboard", "database", "embed")
     * @return Array of all ancestor content items
     */
    public Page[] getAncestorsOfContent(String contentId, String contentType) {
        List<Page> allAncestors = getAllAncestorsOfContent(contentId, contentType);

        return allAncestors.stream()
                .peek(page -> page.setDomainUrl(domainUrl))
                .toArray(Page[]::new);
    }

    /**
     * Recursively collect all descendants of a content item.
     * This is a helper method for getAllNestedDescendants.
     * Uses the content type to determine the correct API endpoint for each level.
     * Only recurses into content with status "current" - non-current content (archived, trashed, etc.)
     * is included in results but not traversed for children.
     *
     * @param contentId the content ID to collect descendants from
     * @param contentType the type of content ("page", "folder", "whiteboard", "database", "embed")
     * @param allDescendants the list to accumulate all descendants
     */
    private void collectAllDescendants(String contentId, String contentType, List<Page> allDescendants) {
        // Get direct children of current content using type-aware method
        Page[] directChildren = getDirectChildrenOfContent(contentId, contentType);

        // Add direct children to the list
        allDescendants.addAll(Arrays.asList(directChildren));

        // Recursively get descendants of each child, using their own type
        // Only recurse into children with status "current" - the Confluence API returns 404
        // when trying to get children of non-current pages (archived, trashed, deleted, etc.)
        for (Page child : directChildren) {
            String childStatus = child.getStatus();
            // Only recurse if the child has status "current"
            if (ConfluenceConstants.CURRENT.equals(childStatus)) {
                // Use the child's type for the recursive call
                String childType = child.getType() != null ? child.getType() : "page";
                collectAllDescendants(child.getId(), childType, allDescendants);
            }
        }
    }

    /**
     * Helper method to get all ancestors of a content item with pagination support.
     * Uses V2 API dedicated endpoint for ancestors with cursor-based pagination.
     * Uses ContentType enum to determine the correct API endpoint based on content type.
     *
     * @param contentId the content ID to get ancestors from
     * @param contentType the type of content ("page", "folder", "whiteboard", "database", "embed")
     * @return List of all ancestor content items
     */
    private List<Page> getAllAncestorsOfContent(String contentId, String contentType) {
        List<Page> allAncestors = new ArrayList<>();
        String nextUrl = null;

        // Use ContentType enum to get the correct API path components
        ContentType type = ContentType.fromString(contentType);
        String[] pathComponents = type.getAncestorsPathComponents(contentId);

        do {
            HttpResponse<String> response;
            if (nextUrl == null) {
                // First request - use the type-determined endpoint with limit
                response = confluenceAttributes.getRestClient().doSyncGet(
                        APIVersion.V2,
                        Map.of("limit", String.valueOf(DEFAULT_PAGINATION_LIMIT)),
                        pathComponents);
            } else {
                // Subsequent requests - use the next URL from _links (cursor-based pagination)
                UrlParser.ParsedUrl parsedUrl = UrlParser.parseNextUrl(nextUrl);
                response = confluenceAttributes.getRestClient().doSyncGet(APIVersion.V2, parsedUrl.getQueryParams(), parsedUrl.getPathComponents());
            }

            if (response.statusCode() == HttpURLConnection.HTTP_OK) {
                // Use V2 response POJO deserialization
                V2AncestorsResponse v2Response = GsonFactory.getInstance().fromJson(response.body(), V2AncestorsResponse.class);

                if (v2Response.getResults() != null) {
                    // Convert V2 ancestor items to Page DTOs
                    for (V2Ancestor v2Ancestor : v2Response.getResults()) {
                        Page ancestor = pageConverter.convertV2AncestorToPage(v2Ancestor);
                        allAncestors.add(ancestor);
                    }
                }

                // Check for next page using V2 API cursor-based pagination
                nextUrl = v2Response.hasNext() ? v2Response.getNextUrl() : null;

            } else {
                throw new ConfluenceRESTException("Failed to retrieve ancestors for " + contentType + " ID: " + contentId + ". Status: " + response.statusCode() + ", Response: " + response.body());
            }
        } while (nextUrl != null);

        return allAncestors;
    }

    /**
     * Helper method to get all child content of a specific content item with pagination support.
     * Uses V2 API dedicated endpoint for direct children with cursor-based pagination.
     * Uses Strategy pattern to determine the correct API endpoint based on content type.
     *
     * @param contentId the content ID to get children from
     * @param contentType the type of content ("page", "folder", "whiteboard", "database", "embed")
     * @return List of all child content items
     */
    private List<Page> getAllChildrenOfContent(String contentId, String contentType) {
        LOGGER.debug("Getting all children of {} of type {}", contentId, contentType);
        List<Page> allChildren = new ArrayList<>();
        String nextUrl = null;

        // Get the parent's spaceId first - all children are in the same space
        // The V2 direct-children endpoint may not return spaceId in the response
        String parentSpaceId = getContentSpaceId(contentId, contentType);

        // Use Strategy pattern to get the correct API path components
        ContentChildrenStrategyFactory strategyFactory = ContentChildrenStrategyFactory.getInstance();
        String[] pathComponents = strategyFactory.getChildrenPathComponents(contentId, contentType);

        do {
            HttpResponse<String> response;
            if (nextUrl == null) {
                // First request - use the strategy-determined endpoint with limit
                response = confluenceAttributes.getRestClient().doSyncGet(
                        APIVersion.V2,
                        Map.of("limit", String.valueOf(DEFAULT_PAGINATION_LIMIT)),
                        pathComponents);
            } else {
                // Subsequent requests - use the next URL from _links (cursor-based pagination)
                UrlParser.ParsedUrl parsedUrl = UrlParser.parseNextUrl(nextUrl);
                response = confluenceAttributes.getRestClient().doSyncGet(APIVersion.V2, parsedUrl.getQueryParams(), parsedUrl.getPathComponents());
            }

            if (response.statusCode() == HttpURLConnection.HTTP_OK) {
                // Use V2 response POJO deserialization
                V2ChildrenResponse v2Response = GsonFactory.getInstance().fromJson(response.body(), V2ChildrenResponse.class);

                if (v2Response.getResults() != null) {
                    // Convert V2 child items to Page DTOs (includes all content types)
                    for (V2ChildPage v2ChildPage : v2Response.getResults()) {
                        // Include all content types - type field distinguishes them
                        Page page = pageConverter.convertV2ChildPageToPage(v2ChildPage, contentId, parentSpaceId);
                        allChildren.add(page);
                    }
                }

                // Check for next page using V2 API cursor-based pagination
                nextUrl = v2Response.hasNext() ? v2Response.getNextUrl() : null;

            } else {
                throw new ConfluenceRESTException("Failed to retrieve children for " + contentType + ". Status: " + response.statusCode() + ", Response: " + response.body());
            }
        } while (nextUrl != null);

        return allChildren;
    }

    /**
     * Gets the spaceId of a content item using type-aware API endpoint.
     * Used to populate spaceId for child content since the direct-children endpoint
     * may not return spaceId in the response.
     *
     * @param contentId the content ID
     * @param contentType the type of content ("page", "folder", "whiteboard", "database", "embed")
     * @return the spaceId of the content item
     */
    private String getContentSpaceId(String contentId, String contentType) {
        try {
            // Use the appropriate endpoint based on content type
            ContentType type = ContentType.fromString(contentType);
            String apiPathSegment = type.getApiPathSegment();

            HttpResponse<String> response = confluenceAttributes.getRestClient().doSyncGet(
                    APIVersion.V2,
                    Map.of(),
                    apiPathSegment, contentId);

            if (response.statusCode() == HttpURLConnection.HTTP_OK) {
                // Parse the response to extract spaceId
                JsonObject jsonObject = GsonFactory.getInstance().fromJson(response.body(), JsonObject.class);
                if (jsonObject.has("spaceId")) {
                    return jsonObject.get("spaceId").getAsString();
                }
            }
            return "";
        } catch (Exception e) {
            // If we can't get the content, return empty string
            return "";
        }
    }

    /**
     * Helper method to get only root-level pages in a space (direct children of the space).
     * Uses the V2 API depth=root parameter to filter at the API level.
     * See: https://developer.atlassian.com/cloud/confluence/rest/v2/api-group-page/#api-spaces-id-pages-get
     *
     * @param spaceId the space ID to get root pages from
     * @return List of root-level pages in the space
     */
    private List<Page> getRootPagesInSpace(String spaceId) {
        return getPagesInSpace(spaceId, "root");
    }

    /**
     * Helper method to get pages in a space with pagination support and configurable depth.
     * Follows the _links.next to retrieve all pages across multiple API calls.
     *
     * @param spaceId the space ID to get pages from
     * @param depth the depth filter: "all" for all pages, "root" for only root-level pages
     * @return List of pages in the space
     */
    private List<Page> getPagesInSpace(String spaceId, String depth) {
        List<Page> allPages = new ArrayList<>();
        String nextUrl = null;

        do {
            HttpResponse<String> response;
            if (nextUrl == null) {
                // First request - use the standard endpoint with depth parameter
                response = confluenceAttributes.getRestClient().doSyncGet(APIVersion.V2, Map.of("depth", depth), "spaces", spaceId, "pages");
            } else {
                // Subsequent requests - use the next URL from _links
                // Parse the next URL to extract path components and query parameters
                UrlParser.ParsedUrl parsedUrl = UrlParser.parseNextUrl(nextUrl);
                response = confluenceAttributes.getRestClient().doSyncGet(APIVersion.V2, parsedUrl.getQueryParams(), parsedUrl.getPathComponents());
            }

            if (response.statusCode() == HttpURLConnection.HTTP_OK) {
                JsonObject resultObject = GsonFactory.getInstance().fromJson(response.body(), JsonObject.class);

                if (resultObject.has(ConfluenceConstants.RESULTS)) {
                    Page[] pagesBatch = GsonFactory.getInstance().fromJson(
                            GsonFactory.getInstance().toJson(resultObject.get(ConfluenceConstants.RESULTS)),
                            TypeToken.getArray(Page.class).getType());

                    // Add this batch to our collection
                    allPages.addAll(Arrays.asList(pagesBatch));
                }

                // Check for next page
                nextUrl = null;
                if (resultObject.has("_links")) {
                    JsonObject links = resultObject.getAsJsonObject("_links");
                    if (links.has("next")) {
                        nextUrl = links.get("next").getAsString();
                    }
                }
            } else {
                throw new ConfluenceRESTException("Failed to retrieve pages from space. Status: " + response.statusCode() + ", Response: " + response.body());
            }
        } while (nextUrl != null);

        return allPages;
    }
}

