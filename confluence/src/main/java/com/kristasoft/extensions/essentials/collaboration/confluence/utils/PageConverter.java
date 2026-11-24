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

package com.kristasoft.extensions.essentials.collaboration.confluence.utils;

import com.google.gson.JsonObject;
import com.kristasoft.extensions.essentials.collaboration.confluence.ConfluenceAttributes;
import com.kristasoft.extensions.essentials.collaboration.confluence.api.APIVersion;
import com.kristasoft.extensions.essentials.collaboration.confluence.dto.Page;
import com.kristasoft.extensions.essentials.collaboration.confluence.dto.v1.V1Page;
import com.kristasoft.extensions.essentials.collaboration.confluence.dto.v2.V2Ancestor;
import com.kristasoft.extensions.essentials.collaboration.confluence.dto.v2.V2ChildPage;
import com.kristasoft.extensions.essentials.collaboration.confluence.service.strategy.ContentType;

import java.net.HttpURLConnection;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for converting V1 and V2 API responses to Page DTOs.
 */
public class PageConverter {

    private final ConfluenceAttributes confluenceAttributes;
    private final String domainUrl;

    public PageConverter(ConfluenceAttributes confluenceAttributes, String domainUrl) {
        this.confluenceAttributes = confluenceAttributes;
        this.domainUrl = domainUrl;
    }

    /**
     * Converts V2 API child page POJO to Page DTO.
     * V2 direct-children endpoint returns lightweight response without body content.
     * Supports all content types returned by the API:
     * - page: A regular Confluence page with content
     * - folder: An organizational container for pages and other content
     * - whiteboard: A collaborative whiteboard
     * - database: A Confluence database
     * - embed: An embedded content item
     *
     * @param v2ChildPage the V2ChildPage POJO from deserialization
     * @param parentPageId the parent page ID
     * @param parentSpaceId the parent page's spaceId (used as fallback if not in response)
     * @return Page DTO object
     */
    public Page convertV2ChildPageToPage(V2ChildPage v2ChildPage, String parentPageId, String parentSpaceId) {
        // Extract basic fields from V2 POJO
        String id = v2ChildPage.getId() != null ? v2ChildPage.getId() : "";
        String title = v2ChildPage.getTitle() != null ? v2ChildPage.getTitle() : "";
        String status = v2ChildPage.getStatus() != null ? v2ChildPage.getStatus() : "";
        String type = v2ChildPage.getType() != null ? v2ChildPage.getType() : "page";

        // Use spaceId from response if available, otherwise use parent's spaceId
        String spaceId = v2ChildPage.getSpaceId() != null && !v2ChildPage.getSpaceId().isEmpty()
                ? v2ChildPage.getSpaceId()
                : parentSpaceId;

        // Use PageBuilder to create the Page object
        Page page = new Page.PageBuilder(id, title)
                .status(status)
                .type(type)
                .parentId(parentPageId)
                .parentType("page")
                .spaceId(spaceId)
                .build();

        // V2 direct-children doesn't include webui links in individual items
        // Links will be empty for now - can be fetched separately if needed
        page.setLinks(new HashMap<>());

        return page;
    }

    /**
     * Converts V1 API page POJO to Page DTO.
     * Kept for backward compatibility with other V1 API calls.
     *
     * @param v1Page the V1Page POJO from deserialization
     * @param parentPageId the parent page ID
     * @return Page DTO object
     */
    public Page convertV1PageToPage(V1Page v1Page, String parentPageId) {
        // Extract basic fields from POJO
        String id = v1Page.getId() != null ? v1Page.getId() : "";
        String title = v1Page.getTitle() != null ? v1Page.getTitle() : "";
        String status = v1Page.getStatus() != null ? v1Page.getStatus() : "";

        // Extract space information from expandable links
        String spaceId = "";
        if (v1Page.getExpandable() != null && v1Page.getExpandable().getSpace() != null) {
            String spaceLink = v1Page.getExpandable().getSpace();
            // Extract space key from link like "/rest/api/space/SSP"
            String[] parts = spaceLink.split("/");
            if (parts.length > 0) {
                spaceId = parts[parts.length - 1];
            }
        }

        // Extract _links from V1 API response
        Map<String, String> links = new HashMap<>();
        if (v1Page.getLinks() != null) {
            V1Page.V1PageLinks v1Links = v1Page.getLinks();
            if (v1Links.getWebui() != null) links.put("webui", v1Links.getWebui());
            if (v1Links.getSelf() != null) links.put("self", v1Links.getSelf());
            if (v1Links.getTinyui() != null) links.put("tinyui", v1Links.getTinyui());
            if (v1Links.getEditui() != null) links.put("editui", v1Links.getEditui());
            if (v1Links.getEdituiv2() != null) links.put("edituiv2", v1Links.getEdituiv2());
        }

        // Use PageBuilder to create the Page object
        Page page = new Page.PageBuilder(id, title)
                .status(status)
                .parentId(parentPageId)
                .parentType("page")
                .spaceId(spaceId)
                .build();

        // Set the links using the setter method
        page.setLinks(links);

        return page;
    }

    /**
     * Convert a V2 Ancestor object to a Page entity.
     * Since V2 ancestors endpoint only returns id and type, we need to fetch additional details.
     *
     * @param v2Ancestor the V2 ancestor object
     * @return Page entity with available information
     */
    public Page convertV2AncestorToPage(V2Ancestor v2Ancestor) {
        String ancestorId = v2Ancestor.getId();
        String ancestorType = v2Ancestor.getType();

        // Handle null or empty type - default to "page"
        if (ancestorType == null || ancestorType.isEmpty()) {
            ancestorType = "page";
        }

        // Fetch additional details for the ancestor using type-aware endpoint
        try {
            ContentType type = ContentType.fromString(ancestorType);
            String apiPathSegment = type.getApiPathSegment();

            HttpResponse<String> detailResponse = confluenceAttributes.getRestClient().doSyncGet(
                    APIVersion.V2,
                    Map.of(),
                    apiPathSegment, ancestorId);

            if (detailResponse.statusCode() == HttpURLConnection.HTTP_OK) {
                return buildPageFromV2Response(detailResponse.body(), ancestorId, ancestorType);
            }
        } catch (Exception e) {
            // If V2 API fails, try V1 API as fallback
        }

        // Fallback: Try to fetch details using V1 API for pages
        return fetchAncestorFromV1Api(ancestorId, ancestorType);
    }

    private Page buildPageFromV2Response(String responseBody, String ancestorId, String ancestorType) {
        JsonObject jsonObject = GsonFactory.getInstance().fromJson(responseBody, JsonObject.class);

        String title = jsonObject.has("title") ? jsonObject.get("title").getAsString() : "";
        String status = jsonObject.has("status") ? jsonObject.get("status").getAsString() : "";
        String spaceId = jsonObject.has("spaceId") ? jsonObject.get("spaceId").getAsString() : "";
        String parentId = jsonObject.has("parentId") && !jsonObject.get("parentId").isJsonNull()
                ? jsonObject.get("parentId").getAsString() : "";
        String parentType = jsonObject.has("parentType") && !jsonObject.get("parentType").isJsonNull()
                ? jsonObject.get("parentType").getAsString() : "";

        return new Page.PageBuilder(ancestorId, title)
                .status(status)
                .type(ancestorType)
                .spaceId(spaceId)
                .parentId(parentId)
                .parentType(parentType)
                .domainUrl(domainUrl)
                .build();
    }

    private Page fetchAncestorFromV1Api(String ancestorId, String ancestorType) {
        try {
            HttpResponse<String> v1Response = confluenceAttributes.getRestClient().doSyncGet(
                    APIVersion.V1,
                    Map.of("expand", "space"),
                    "content", ancestorId);

            if (v1Response.statusCode() == HttpURLConnection.HTTP_OK) {
                JsonObject jsonObject = GsonFactory.getInstance().fromJson(v1Response.body(), JsonObject.class);

                String title = jsonObject.has("title") ? jsonObject.get("title").getAsString() : "";
                String status = jsonObject.has("status") ? jsonObject.get("status").getAsString() : "";
                String spaceId = "";
                if (jsonObject.has("space") && !jsonObject.get("space").isJsonNull()) {
                    JsonObject spaceObj = jsonObject.getAsJsonObject("space");
                    if (spaceObj.has("id")) {
                        spaceId = spaceObj.get("id").getAsString();
                    }
                }

                // V1 API doesn't have parentId/parentType in the same way, so leave them empty
                return new Page.PageBuilder(ancestorId, title)
                        .status(status)
                        .type(ancestorType)
                        .spaceId(spaceId)
                        .domainUrl(domainUrl)
                        .build();
            }
        } catch (Exception e) {
            // If V1 API also fails, fall through to return minimal ancestor info
        }

        // Final fallback: return minimal ancestor with just id and type
        return new Page.PageBuilder(ancestorId, "")
                .type(ancestorType)
                .domainUrl(domainUrl)
                .build();
    }
}
