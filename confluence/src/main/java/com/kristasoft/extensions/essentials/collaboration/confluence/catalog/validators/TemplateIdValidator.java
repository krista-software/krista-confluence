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

import java.util.Map;

/**
 * Validator for Template ID fields in Confluence operations.
 * <p>
 * This validator ensures that Template ID values are properly formatted and not empty.
 * It validates that the Template ID is a non-null, non-empty string that can be used
 * to identify a specific template in Confluence. It also validates against the actual
 * Confluence service to ensure the template exists.
 * </p>
 */
public class TemplateIdValidator implements Validator {

    private static final Logger LOGGER = LoggerFactory.getLogger(TemplateIdValidator.class);

    private final ConfluenceService confluenceService;

    public TemplateIdValidator(ConfluenceService confluenceService) {
        this.confluenceService = confluenceService;
    }

    @Override
    public Boolean validate(String templateId, Map<ValidationResource, String> context) {
        LOGGER.debug("→TemplateIdValidator: Validating template ID: {}", templateId);

        if (templateId == null || templateId.trim().isEmpty()) {
            LOGGER.warn("→TemplateIdValidator: Template ID is null or empty");
            return false;
        }

        // Validate that the template actually exists by making an API call
        try {
            var template = confluenceService.getTemplateById(templateId.trim());
            if (template != null) {
                LOGGER.debug("→TemplateIdValidator: Template ID {} exists in Confluence", templateId);
                return true;
            } else {
                LOGGER.warn("→TemplateIdValidator: Template ID {} does not exist in Confluence", templateId);
                return false;
            }
        } catch (Exception e) {
            LOGGER.error("→TemplateIdValidator: Error validating template ID {}: {}", templateId, e.getMessage());
            return false;
        }
    }

    @Override
    public String getFetchFieldName() {
        return "Template ID";
    }

    @Override
    public String getFetchStepMessage() {
        return "Please enter a valid Template ID. You can find available templates using the 'Get Templates By Space ID' request.";
    }

    @Override
    public String getConfirmationStepMessage(String templateId, Map<ValidationResource, String> context) {
        if (templateId == null || templateId.trim().isEmpty()) {
            return "Template ID is required to continue. Would you like to provide a valid Template ID? Tip: Use 'Get Templates By Space ID' to view available templates.";
        } else {
            return "We couldn't find a template with ID '" + templateId + "'. This template may not exist or you may not have permission to access it. Would you like to try a different Template ID?";
        }
    }

    @Override
    public String getErrMessage(String templateId) {
        if (templateId == null) {
            return "Template ID is required. Please provide a Template ID to continue. Tip: Use 'Get Templates By Space ID' to view available templates and their IDs.";
        } else if (templateId.trim().isEmpty()) {
            return "Template ID cannot be empty. Please enter a valid Template ID. Tip: Use 'Get Templates By Space ID' to view available templates.";
        } else {
            return String.format("Template ID '%s' not found. This template may not exist or you may not have access to it. Please verify the Template ID and try again.", templateId);
        }
    }
}
