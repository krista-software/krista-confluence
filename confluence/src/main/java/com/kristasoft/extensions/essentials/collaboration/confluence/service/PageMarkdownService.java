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

import app.krista.model.base.File;

/**
 * Service for exporting a Confluence page's content as a Markdown ({@code .md}) file.
 *
 * <p>The page body is retrieved via {@link ConfluenceService#getPageById(String)} in
 * storage format and converted to Markdown by {@link
 * com.kristasoft.extensions.essentials.collaboration.confluence.utils.ConfluenceStorageToMarkdownConverter}.
 */
public interface PageMarkdownService {

    /**
     * Download a single Confluence page as a Markdown file. The filename is derived from
     * the sanitized page title, falling back to the page id when the title is blank or
     * sanitizes to empty.
     *
     * @param pageId the Confluence page id
     * @return Krista {@link File} backed by the generated {@code .md} file
     */
    File downloadPageAsMarkdownFile(String pageId);
}
