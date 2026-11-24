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

package com.kristasoft.extensions.essentials.collaboration.confluence.catalog.validators;

import com.kristasoft.extensions.essentials.collaboration.confluence.service.ConfluenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;

/**
 * Validator for Page ID validation
 * Inspired by Zendesk ArticleIdValidator
 */
public class PageIdValidator implements Validator {

    private static final Logger LOGGER = LoggerFactory.getLogger(PageIdValidator.class);

    private final ConfluenceService confluenceService;

    @Inject
    public PageIdValidator(ConfluenceService confluenceService) {
        this.confluenceService = confluenceService;
    }

    @Override
    public Boolean validate(String pageId, Map<ValidationResource, String> context) {
        LOGGER.debug("→PageIdValidator: Validating page ID: {}", pageId);

        if (pageId == null || pageId.trim().isEmpty()) {
            LOGGER.warn("→PageIdValidator: Page ID is null or empty");
            return false;
        }

        // Validate that the page actually exists by making an API call
        try {
            var page = confluenceService.getPageById(pageId.trim());
            if (page != null) {
                LOGGER.debug("→PageIdValidator: Page ID {} exists in Confluence", pageId);
                return true;
            } else {
                LOGGER.warn("→PageIdValidator: Page ID {} does not exist in Confluence", pageId);
                return false;
            }
        } catch (Exception e) {
            LOGGER.error("→PageIdValidator: Error validating page ID {}: {}", pageId, e.getMessage());
            return false;
        }
    }

    @Override
    public String getFetchFieldName() {
        return "Page ID";
    }

    @Override
    public String getFetchStepMessage() {
        return "Please enter a valid Page ID. You can find the Page ID in the page URL or by using the 'Get Pages in Space' request.";
    }

    @Override
    public String getConfirmationStepMessage(String pageId, Map<ValidationResource, String> context) {
        if (pageId == null || pageId.trim().isEmpty()) {
            return "Page ID is required to continue. Would you like to provide a valid Page ID? Tip: Use 'Get Pages in Space' to find available pages and their IDs.";
        } else {
            return "We couldn't find a page with ID '" + pageId + "'. This page may not exist or you may not have permission to access it. Would you like to try a different Page ID?";
        }
    }

    @Override
    public String getErrMessage(String pageId) {
        if (pageId == null || pageId.trim().isEmpty()) {
            return "Page ID is required. Please provide a Page ID to continue. Tip: Use 'Get Pages in Space' to view available pages and their IDs.";
        }
        return "Page ID '" + pageId + "' not found. This page may not exist or you may not have access to it. Please verify the Page ID and try again.";
    }
}
