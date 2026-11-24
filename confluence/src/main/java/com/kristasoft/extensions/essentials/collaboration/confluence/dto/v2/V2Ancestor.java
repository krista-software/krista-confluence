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

/**
 * V2 API Ancestor object from /{contentType}/{id}/ancestors endpoint.
 *
 * Response structure from Confluence V2 API:
 * {
 *   "id": "<string>",
 *   "type": "page"
 * }
 *
 * Note: The V2 ancestors endpoint returns a minimal response with only id and type.
 * The 'type' field can be "page", "folder", "whiteboard", "database", or "embed".
 * Ancestors are returned in top-to-bottom order (highest ancestor first).
 */
public class V2Ancestor {
    private String id;
    private String type;

    // Default constructor for Gson
    public V2Ancestor() {
    }

    // Constructor with parameters
    public V2Ancestor(String id, String type) {
        this.id = id;
        this.type = type;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "V2Ancestor{" +
                "id='" + id + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}

