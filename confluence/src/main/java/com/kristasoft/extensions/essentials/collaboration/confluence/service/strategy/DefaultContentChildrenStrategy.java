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

package com.kristasoft.extensions.essentials.collaboration.confluence.service.strategy;

/**
 * Default implementation of ContentChildrenStrategy that uses the ContentType enum
 * to determine the correct API endpoint for retrieving children.
 * 
 * <p>This implementation is flexible and can handle any content type defined in the
 * ContentType enum. It delegates the path construction to the ContentType enum itself.</p>
 */
public class DefaultContentChildrenStrategy implements ContentChildrenStrategy {

    private final ContentType contentType;

    /**
     * Creates a new strategy for the specified content type.
     * 
     * @param contentType the content type this strategy handles
     */
    public DefaultContentChildrenStrategy(ContentType contentType) {
        this.contentType = contentType;
    }

    @Override
    public String[] getChildrenPathComponents(String contentId) {
        return contentType.getChildrenPathComponents(contentId);
    }

    @Override
    public ContentType getContentType() {
        return contentType;
    }

    @Override
    public boolean supports(String type) {
        return contentType.matches(type);
    }

    @Override
    public String toString() {
        return "DefaultContentChildrenStrategy{" +
                "contentType=" + contentType +
                '}';
    }
}

