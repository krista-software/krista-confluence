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

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for parsing Confluence API URLs.
 * Handles both V1 and V2 API URL formats.
 */
public class UrlParser {

    /**
     * Helper class to hold parsed URL components
     */
    public static class ParsedUrl {
        private final String[] pathComponents;
        private final Map<String, String> queryParams;

        public ParsedUrl(String[] pathComponents, Map<String, String> queryParams) {
            this.pathComponents = pathComponents;
            this.queryParams = queryParams;
        }

        public String[] getPathComponents() {
            return pathComponents;
        }

        public Map<String, String> getQueryParams() {
            return queryParams;
        }
    }

    /**
     * Parses a next URL from _links.next to extract path components and query parameters.
     * Handles both V1 and V2 API URLs.
     * V1 Example: "/rest/api/content/104169487/child/page?next=true&limit=2&start=2"
     * V2 Example: "/wiki/api/v2/spaces/100171995/pages?cursor=..."
     *
     * @param nextUrl the full next URL from _links.next
     * @return ParsedUrl containing path components and query parameters
     */
    public static ParsedUrl parseNextUrl(String nextUrl) {
        String cleanUrl = nextUrl;

        // Remove API prefixes
        if (cleanUrl.startsWith("/wiki/api/v2/")) {
            cleanUrl = cleanUrl.substring("/wiki/api/v2/".length());
        } else if (cleanUrl.startsWith("/rest/api/")) {
            cleanUrl = cleanUrl.substring("/rest/api/".length());
        } else if (cleanUrl.startsWith("/wiki/rest/api/")) {
            cleanUrl = cleanUrl.substring("/wiki/rest/api/".length());
        }

        // Split path and query string
        String[] urlParts = cleanUrl.split("\\?", 2);
        String pathPart = urlParts[0];
        String queryPart = urlParts.length > 1 ? urlParts[1] : null;

        // Parse path components
        String[] pathComponents = pathPart.split("/");

        // Parse query parameters
        Map<String, String> queryParams = new HashMap<>();
        if (queryPart != null && !queryPart.isEmpty()) {
            String[] queryPairs = queryPart.split("&");
            for (String pair : queryPairs) {
                String[] keyValue = pair.split("=", 2);
                if (keyValue.length == 2) {
                    queryParams.put(keyValue[0], keyValue[1]);
                }
            }
        }

        return new ParsedUrl(pathComponents, queryParams.isEmpty() ? null : queryParams);
    }

    private UrlParser() {
        // Utility class - prevent instantiation
    }
}

