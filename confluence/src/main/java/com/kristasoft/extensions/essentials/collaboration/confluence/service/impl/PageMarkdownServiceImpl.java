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

package com.kristasoft.extensions.essentials.collaboration.confluence.service.impl;

import app.krista.ksdk.files.FileHandle;
import app.krista.ksdk.files.FileRepository;
import app.krista.model.base.File;
import com.kristasoft.extensions.essentials.collaboration.confluence.dto.Page;
import com.kristasoft.extensions.essentials.collaboration.confluence.exception.ConfluenceRESTException;
import com.kristasoft.extensions.essentials.collaboration.confluence.service.ConfluenceService;
import com.kristasoft.extensions.essentials.collaboration.confluence.service.PageMarkdownService;
import com.kristasoft.extensions.essentials.collaboration.confluence.utils.ConfluenceStorageToMarkdownConverter;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
@ContractsProvided(PageMarkdownService.class)
public class PageMarkdownServiceImpl implements PageMarkdownService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PageMarkdownServiceImpl.class);
    private static final int MAX_FILENAME_STEM = 100;

    private final ConfluenceService confluenceService;
    private final FileRepository fileRepository;

    @Inject
    public PageMarkdownServiceImpl(ConfluenceService confluenceService, FileRepository fileRepository) {
        this.confluenceService = confluenceService;
        this.fileRepository = fileRepository;
    }

    @Override
    public File downloadPageAsMarkdownFile(String pageId) {
        LOGGER.info("→ downloadPageAsMarkdownFile: pageId={}", pageId);

        Page page = confluenceService.getPageById(pageId);
        if (page == null) {
            throw new ConfluenceRESTException(
                    "We couldn't find a Confluence page with ID '" + pageId + "'. " +
                    "Verify the ID via 'Get Pages in Space (V2)' and try again.");
        }

        String title = page.getTitle();
        String storageBody = (page.getBody() != null && page.getBody().getStorage() != null)
                ? page.getBody().getStorage().getValue()
                : "";

        String markdown = ConfluenceStorageToMarkdownConverter.convert(title, storageBody);
        byte[] bytes = markdown.getBytes(StandardCharsets.UTF_8);
        String fileName = buildFileName(title, pageId);
        LOGGER.info("→ downloadPageAsMarkdownFile: pageId={}, fileName={}, bytes={}", pageId, fileName, bytes.length);

        try (FileHandle handle = fileRepository.createNewFileByName(fileName);
             ByteArrayInputStream in = new ByteArrayInputStream(bytes)) {
            handle.setContent(in);
            return handle.getFile();
        } catch (IOException e) {
            throw new ConfluenceRESTException(
                    "We couldn't write the page content to a Markdown file: " + e.getMessage());
        }
    }

    static String buildFileName(String title, String pageId) {
        String stem = sanitize(title);
        if (stem.isEmpty()) {
            stem = pageId == null ? "page" : sanitize(pageId);
            if (stem.isEmpty()) stem = "page";
        }
        if (stem.length() > MAX_FILENAME_STEM) {
            stem = stem.substring(0, MAX_FILENAME_STEM);
        }
        return stem + ".md";
    }

    private static String sanitize(String input) {
        if (input == null) return "";
        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c < 32 || c == 127) continue;
            switch (c) {
                case '<', '>', ':', '"', '/', '\\', '|', '?', '*' -> sb.append('_');
                case ' ', '\t' -> sb.append('_');
                default -> sb.append(c);
            }
        }
        String result = sb.toString();
        while (result.startsWith(".") || result.startsWith("_")) result = result.substring(1);
        while (result.endsWith(".") || result.endsWith("_")) result = result.substring(0, result.length() - 1);
        return result.toLowerCase(Locale.ROOT);
    }
}
