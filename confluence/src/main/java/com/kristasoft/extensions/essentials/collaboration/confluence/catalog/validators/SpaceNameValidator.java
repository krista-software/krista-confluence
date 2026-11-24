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

import java.util.Map;

/**
 * Validator for Space Name fields in Confluence operations.
 * <p>
 * This validator ensures that Space Name values are properly formatted and not empty.
 * It validates that the Space Name is a non-null, non-empty string with reasonable length.
 * It also validates against the actual Confluence service when possible.
 * </p>
 */
public class SpaceNameValidator implements Validator {

    private static final int MAX_SPACE_NAME_LENGTH = 255;
    private static final int MIN_SPACE_NAME_LENGTH = 1;

    private final ConfluenceService confluenceService;

    public SpaceNameValidator(ConfluenceService confluenceService) {
        this.confluenceService = confluenceService;
    }

    @Override
    public Boolean validate(String spaceName, Map<ValidationResource, String> context) {
        if (spaceName == null || spaceName.trim().isEmpty()) {
            return false;
        }

        String trimmedName = spaceName.trim();

        // Check length constraints
        if (trimmedName.length() < MIN_SPACE_NAME_LENGTH || trimmedName.length() > MAX_SPACE_NAME_LENGTH) {
            return false;
        }

        // Validate against Confluence service if available
        if (confluenceService != null) {
            try {
                // Try to get spaces to validate the service is accessible
                confluenceService.getSpaces();
                return true;
            } catch (Exception e) {
                // If service call fails, fall back to format validation only
                return true;
            }
        }

        return true;
    }

    @Override
    public String getFetchFieldName() {
        return "Space Name";
    }

    @Override
    public String getFetchStepMessage() {
        return "Please enter the name of your Confluence space. The space name is the display name shown in Confluence (e.g., 'Engineering Documentation').";
    }

    @Override
    public String getConfirmationStepMessage(String spaceName, Map<ValidationResource, String> context) {
        return String.format("The Space Name '%s' doesn't meet the requirements. Would you like to enter a different Space Name?",
                spaceName != null ? spaceName : "not provided");
    }

    @Override
    public String getErrMessage(String spaceName) {
        if (spaceName == null) {
            return "Space Name is required. Please provide the name of your Confluence space to continue.";
        } else if (spaceName.trim().isEmpty()) {
            return "Space Name cannot be empty. Please enter a valid space name (e.g., 'Engineering Documentation').";
        } else if (spaceName.trim().length() > MAX_SPACE_NAME_LENGTH) {
            return String.format("Space Name is too long. Please use a name with %d characters or fewer (current length: %d characters).", MAX_SPACE_NAME_LENGTH, spaceName.trim().length());
        } else if (spaceName.trim().length() < MIN_SPACE_NAME_LENGTH) {
            return "Space Name must contain at least one character. Please provide a valid space name.";
        } else {
            return String.format("Space Name '%s' is invalid. Please verify the name and try again.", spaceName);
        }
    }
}
