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

/**
 * V2 API Child Page object from /pages/{id}/direct-children endpoint.
 *
 * Response structure from Confluence V2 API:
 * {
 *   "id": "<string>",
 *   "status": "current",
 *   "title": "<string>",
 *   "type": "<string>",
 *   "spaceId": "<string>",
 *   "childPosition": 238
 * }
 *
 * Note: The V2 direct-children endpoint returns a lightweight response
 * without body content. The 'type' field can be "page", "folder", etc.
 */
public class V2ChildPage {
    private String id;
    private String status;
    private String title;
    private String type;

    @SerializedName("spaceId")
    private String spaceId;

    @SerializedName("childPosition")
    private Integer childPosition;

    // Default constructor for Gson
    public V2ChildPage() {
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getStatus() {
        return status;
    }

    public String getTitle() {
        return title;
    }

    public String getType() {
        return type;
    }

    public String getSpaceId() {
        return spaceId;
    }

    public Integer getChildPosition() {
        return childPosition;
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setSpaceId(String spaceId) {
        this.spaceId = spaceId;
    }

    public void setChildPosition(Integer childPosition) {
        this.childPosition = childPosition;
    }

    @Override
    public String toString() {
        return "V2ChildPage{" +
                "id='" + id + '\'' +
                ", status='" + status + '\'' +
                ", title='" + title + '\'' +
                ", type='" + type + '\'' +
                ", spaceId='" + spaceId + '\'' +
                ", childPosition=" + childPosition +
                '}';
    }
}

