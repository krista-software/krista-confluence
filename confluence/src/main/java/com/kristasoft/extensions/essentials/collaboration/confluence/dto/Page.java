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

package com.kristasoft.extensions.essentials.collaboration.confluence.dto;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

public class Page {
    private String parentType;
    private String authorId;
    private String id;
    private int position;
    private String title;
    private String status;
    private String type;  // "page", "folder", "whiteboard", "database", or "embed"
    private Body body;
    private String parentId;
    private String spaceId;
    private Version version;
    private String createdAt;
    @SerializedName("_links")
    private Map<String, String> links;
    private String domainUrl;

    private Page(PageBuilder builder) {
        this.parentType = builder.parentType;
        this.authorId = builder.authorId;
        this.id = builder.id;
        this.position = builder.position;
        this.title = builder.title;
        this.status = builder.status;
        this.type = builder.type;
        this.body = builder.body;
        this.parentId = builder.parentId;
        this.spaceId = builder.spaceId;
        this.version = builder.version;
        this.createdAt = builder.createdAt;
    }

    public String getParentType() {
        return parentType;
    }

    public String getAuthorId() {
        return authorId;
    }

    public String getId() {
        return id;
    }

    public int getPosition() {
        return position;
    }

    public String getTitle() {
        return title;
    }

    public String getStatus() {
        return status;
    }

    public String getType() {
        return type;
    }

    public Body getBody() {
        return body;
    }

    public String getParentId() {
        return parentId;
    }

    public String getSpaceId() {
        return spaceId;
    }

    public Version getVersion() {
        return version;
    }

    public Page setParentType(String parentType) {
        this.parentType = parentType;
        return this;
    }

    public Page setAuthorId(String authorId) {
        this.authorId = authorId;
        return this;
    }

    public Page setId(String id) {
        this.id = id;
        return this;
    }

    public Page setPosition(int position) {
        this.position = position;
        return this;
    }

    public Page setTitle(String title) {
        this.title = title;
        return this;
    }

    public Page setStatus(String status) {
        this.status = status;
        return this;
    }

    public Page setType(String type) {
        this.type = type;
        return this;
    }

    public Page setBody(Body body) {
        this.body = body;
        return this;
    }

    public Page setParentId(String parentId) {
        this.parentId = parentId;
        return this;
    }

    public Page setSpaceId(String spaceId) {
        this.spaceId = spaceId;
        return this;
    }

    public Page setVersion(Version version) {
        this.version = version;
        return this;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public Page setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public Map<String, String> getLinks() {
        return links;
    }

    public void setLinks(Map<String, String> links) {
        this.links = links;
    }

    public String getDomainUrl() {
        return domainUrl;
    }

    public void setDomainUrl(String domainUrl) {
        this.domainUrl = domainUrl;
    }

    public static class PageBuilder {
        private String parentType;
        private String authorId;
        private String id;
        private int position;
        private String title;
        private String status;
        private String type;
        private Body body;
        private String parentId;
        private String spaceId;
        private Version version;
        private String createdAt;
        private String domainUrl;

        public PageBuilder(String id, String title) {
            this.id = id;
            this.title = title;
        }

        public PageBuilder parentType(String parentType) {
            this.parentType = parentType;
            return this;
        }

        public PageBuilder authorId(String authorId) {
            this.authorId = authorId;
            return this;
        }

        public PageBuilder position(int position) {
            this.position = position;
            return this;
        }

        public PageBuilder status(String status) {
            this.status = status;
            return this;
        }

        public PageBuilder type(String type) {
            this.type = type;
            return this;
        }

        public PageBuilder body(Body body) {
            this.body = body;
            return this;
        }

        public PageBuilder parentId(String parentId) {
            this.parentId = parentId;
            return this;
        }

        public PageBuilder spaceId(String spaceId) {
            this.spaceId = spaceId;
            return this;
        }

        public PageBuilder version(Version version) {
            this.version = version;
            return this;
        }

        public PageBuilder createdAt(String createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public PageBuilder domainUrl(String domainUrl) {
            this.domainUrl = domainUrl;
            return this;
        }

        public Page build() {
            return new Page(this);
        }
    }

    @Override
    public String toString() {
        return "Page{" +
                "parentType='" + parentType + '\'' +
                ", authorId='" + authorId + '\'' +
                ", id='" + id + '\'' +
                ", position=" + position +
                ", title='" + title + '\'' +
                ", status='" + status + '\'' +
                ", type='" + type + '\'' +
                ", body=" + body +
                ", parentId='" + parentId + '\'' +
                ", spaceId='" + spaceId + '\'' +
                ", version=" + version +
                ", createdAt='" + createdAt + '\'' +
                ", links=" + links +
                ", domainUrl='" + domainUrl + '\'' +
                '}';
    }
}