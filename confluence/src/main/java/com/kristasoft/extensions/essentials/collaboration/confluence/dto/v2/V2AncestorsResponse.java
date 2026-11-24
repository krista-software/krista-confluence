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

package com.kristasoft.extensions.essentials.collaboration.confluence.dto.v2;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Response wrapper for V2 API ancestors endpoint.
 * Represents the response from /{contentType}/{id}/ancestors
 * 
 * Response structure:
 * {
 *   "results": [
 *     {
 *       "id": "<string>",
 *       "type": "page"
 *     }
 *   ],
 *   "_links": {
 *     "next": "<string>",
 *     "base": "<string>"
 *   }
 * }
 * 
 * Note: Ancestors are returned in top-to-bottom order (highest ancestor first).
 * The response uses cursor-based pagination via the _links.next field.
 */
public class V2AncestorsResponse {
    private List<V2Ancestor> results;
    
    @SerializedName("_links")
    private V2Links links;

    // Default constructor for Gson
    public V2AncestorsResponse() {
    }

    public List<V2Ancestor> getResults() {
        return results;
    }

    public void setResults(List<V2Ancestor> results) {
        this.results = results;
    }

    public V2Links getLinks() {
        return links;
    }

    public void setLinks(V2Links links) {
        this.links = links;
    }

    /**
     * Check if there are more results available (pagination)
     */
    public boolean hasNext() {
        return links != null && links.getNext() != null && !links.getNext().isEmpty();
    }

    /**
     * Get the next page URL for pagination
     */
    public String getNextUrl() {
        return links != null ? links.getNext() : null;
    }

    /**
     * Links object for V2 API pagination.
     * Reuses the same structure as V2ChildrenResponse.V2Links.
     */
    public static class V2Links {
        private String next;
        private String base;

        public String getNext() {
            return next;
        }

        public void setNext(String next) {
            this.next = next;
        }

        public String getBase() {
            return base;
        }

        public void setBase(String base) {
            this.base = base;
        }

        @Override
        public String toString() {
            return "V2Links{" +
                    "next='" + next + '\'' +
                    ", base='" + base + '\'' +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "V2AncestorsResponse{" +
                "results=" + results +
                ", links=" + links +
                '}';
    }
}

