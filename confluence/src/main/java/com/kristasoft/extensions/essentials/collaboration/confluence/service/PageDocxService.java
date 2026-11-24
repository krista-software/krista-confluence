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
import com.kristasoft.extensions.essentials.collaboration.confluence.dto.Page;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.stores.Pair;

/**
 * Service interface for converting Confluence pages to Docx format
 * Inspired by Zendesk HelpCenterDao downloadArticleAsPDFOrWordFile functionality
 */
public interface PageDocxService {

    /**
     * Downloads a Confluence page as a Docx file
     * @param pageId The ID of the page to download
     * @param exportFormat The export format (currently supports "docx")
     * @return File object representing the generated Docx file
     */
    File downloadPageAsDocxFile(String pageId, String exportFormat);

    /**
     * Downloads a Confluence page as a Docx file with custom filename
     * @param pageId The ID of the page to download
     * @param exportFormat The export format (currently supports "docx")
     * @param customFileName Custom filename for the generated file
     * @return File object representing the generated Docx file
     */
    File downloadPageAsDocxFile(String pageId, String exportFormat, String customFileName);

    /**
     * Downloads a Confluence page as a Docx file with parsing success flag
     * @param pageId The ID of the page to download
     * @param exportFormat The export format (currently supports "docx")
     * @return Pair containing the File object and boolean success flag
     */
    Pair<File, Boolean> downloadPageAsDocxFileWithStatus(String pageId, String exportFormat);

    /**
     * Downloads a Confluence page as a Docx file with custom filename and parsing success flag
     * @param pageId The ID of the page to download
     * @param exportFormat The export format (currently supports "docx")
     * @param customFileName Custom filename for the generated file
     * @return Pair containing the File object and boolean success flag
     */
    Pair<File, Boolean> downloadPageAsDocxFileWithStatus(String pageId, String exportFormat, String customFileName);
}
