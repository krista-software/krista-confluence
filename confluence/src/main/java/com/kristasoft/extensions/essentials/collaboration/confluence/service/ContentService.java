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

import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.entities.Content;

import java.util.List;
import java.util.Map;

public interface ContentService {
    Content createContentForTable(List<String> columns, List<Map<String, String>> table);
    Content createTickerSymbolContentForCompany(String companyName);
    Content createHeadingContent(String text, String type, String textAlignment, Boolean breakLine);
    Map<String, Object> extractContentFromWebPage(String url);
    Content createAnchorTag(String text, String href);
}