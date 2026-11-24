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

/**
 * Profile picture information for a Confluence user.
 */
public class ProfilePicture {
    private String path;
    private Integer width;
    private Integer height;
    private Boolean isDefault;

    /**
     * Default no-argument constructor required by Gson for JSON deserialization.
     * Gson uses reflection to create instances and populate fields directly,
     * so this constructor intentionally has no initialization logic.
     */
    public ProfilePicture() {
        // Empty constructor - Gson handles field initialization via reflection
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    @Override
    public String toString() {
        return "ProfilePicture{" +
                "path='" + path + '\'' +
                ", width=" + width +
                ", height=" + height +
                ", isDefault=" + isDefault +
                '}';
    }
}

