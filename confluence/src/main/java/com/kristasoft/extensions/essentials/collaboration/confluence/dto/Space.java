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

import java.util.Date;

public class Space {
    private String authorId;
    private String homepageId;
    private String icon;
    private String name;
    private String key;
    private String id;
    private String type;
    private String description;
    private String status;
    private Date createdAt;
    private String currentActiveAlias;


    private Space(SpaceBuilder builder) {
        this.authorId = builder.authorId;
        this.homepageId = builder.homepageId;
        this.icon = builder.icon;
        this.name = builder.name;
        this.key = builder.key;
        this.id = builder.id;
        this.type = builder.type;
        this.description = builder.description;
        this.status = builder.status;
        this.createdAt = builder.createdAt;
        this.currentActiveAlias = builder.currentActiveAlias;

    }

    public String getAuthorId() {
        return authorId;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    public String getHomepageId() {
        return homepageId;
    }

    public void setHomepageId(String homepageId) {
        this.homepageId = homepageId;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getCurrentActiveAlias() {
        return currentActiveAlias;
    }

    public void setCurrentActiveAlias(String currentActiveAlias) {
        this.currentActiveAlias = currentActiveAlias;
    }

    public static SpaceBuilder builder() {
        return new SpaceBuilder();
    }

    public static class SpaceBuilder {
        private String authorId;
        private String homepageId;
        private String icon;
        private String name;
        private String key;
        private String id;
        private String type;
        private String description;
        private String status;
        private Date createdAt;
        private String currentActiveAlias;


        public SpaceBuilder setAuthorId(String authorId) {
            this.authorId = authorId;
            return this;
        }

        public SpaceBuilder setHomepageId(String homepageId) {
            this.homepageId = homepageId;
            return this;
        }

        public SpaceBuilder setIcon(String icon) {
            this.icon = icon;
            return this;
        }

        public SpaceBuilder setName(String name) {
            this.name = name;
            return this;
        }

        public SpaceBuilder setKey(String key) {
            this.key = key;
            return this;
        }

        public SpaceBuilder setId(String id) {
            this.id = id;
            return this;
        }

        public SpaceBuilder setType(String type) {
            this.type = type;
            return this;
        }

        public SpaceBuilder setDescription(String description) {
            this.description = description;
            return this;
        }

        public SpaceBuilder setStatus(String status) {
            this.status = status;
            return this;
        }

        public SpaceBuilder setCreatedAt(Date createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public SpaceBuilder setCurrentActiveAlias(String currentActiveAlias) {
            this.currentActiveAlias = currentActiveAlias;
            return this;
        }

        public Space build() {
            return new Space(this);
        }
    }

    @Override
    public String toString() {
        return "Space{" +
                "authorId='" + authorId + '\'' +
                ", homepageId='" + homepageId + '\'' +
                ", icon='" + icon + '\'' +
                ", name='" + name + '\'' +
                ", key='" + key + '\'' +
                ", id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", description='" + description + '\'' +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                ", currentActiveAlias='" + currentActiveAlias + '\'' +
                '}';
    }
}

