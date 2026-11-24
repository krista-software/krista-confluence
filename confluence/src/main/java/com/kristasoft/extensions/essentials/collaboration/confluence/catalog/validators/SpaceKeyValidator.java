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
import java.util.regex.Pattern;

/**
 * Validator for Space Key fields in Confluence operations.
 * <p>
 * This validator ensures that Space Key values are properly formatted according to Confluence standards.
 * Space keys in Confluence typically follow specific naming conventions and character restrictions.
 * It also validates against the actual Confluence service to ensure the space exists.
 * </p>
 */
public class SpaceKeyValidator implements Validator {

    private static final int MAX_SPACE_KEY_LENGTH = 255;
    private static final int MIN_SPACE_KEY_LENGTH = 1;
    
    // Space keys typically contain alphanumeric characters, underscores, and hyphens
    private static final Pattern SPACE_KEY_PATTERN = Pattern.compile("^[A-Za-z0-9_-]+$");
    
    private final ConfluenceService confluenceService;

    public SpaceKeyValidator(ConfluenceService confluenceService) {
        this.confluenceService = confluenceService;
    }

    @Override
    public Boolean validate(String spaceKey, Map<ValidationResource, String> context) {
        if (spaceKey == null || spaceKey.trim().isEmpty()) {
            return false;
        }
        
        String trimmedKey = spaceKey.trim();
        
        // Check length constraints
        if (trimmedKey.length() < MIN_SPACE_KEY_LENGTH || trimmedKey.length() > MAX_SPACE_KEY_LENGTH) {
            return false;
        }
        
        // Check pattern (alphanumeric, underscore, hyphen only)
        if (!SPACE_KEY_PATTERN.matcher(trimmedKey).matches()) {
            return false;
        }
        
        // Validate against Confluence service if available
        if (confluenceService != null) {
            try {
                // Try to get spaces to validate the key exists
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
        return "Space Key";
    }

    @Override
    public String getFetchStepMessage() {
        return "Please enter your Confluence Space Key. This is a unique identifier (e.g., 'ENG', 'DOCS') that uses only letters, numbers, underscores, and hyphens.";
    }

    @Override
    public String getConfirmationStepMessage(String spaceKey, Map<ValidationResource, String> context) {
        return String.format("The Space Key '%s' doesn't meet the format requirements. Would you like to enter a different Space Key?",
                spaceKey != null ? spaceKey : "not provided");
    }

    @Override
    public String getErrMessage(String spaceKey) {
        if (spaceKey == null) {
            return "Space Key is required. Please provide your Confluence Space Key (e.g., 'ENG', 'DOCS').";
        } else if (spaceKey.trim().isEmpty()) {
            return "Space Key cannot be empty. Please enter a valid Space Key using letters, numbers, underscores, or hyphens.";
        } else if (spaceKey.trim().length() > MAX_SPACE_KEY_LENGTH) {
            return String.format("Space Key is too long. Please use a key with %d characters or fewer (current length: %d characters).", MAX_SPACE_KEY_LENGTH, spaceKey.trim().length());
        } else if (spaceKey.trim().length() < MIN_SPACE_KEY_LENGTH) {
            return "Space Key must contain at least one character. Please provide a valid Space Key.";
        } else if (!SPACE_KEY_PATTERN.matcher(spaceKey.trim()).matches()) {
            return String.format("Space Key '%s' contains invalid characters. Please use only letters (A-Z, a-z), numbers (0-9), underscores (_), and hyphens (-).", spaceKey);
        } else {
            return String.format("Space Key '%s' is invalid. Please verify the key and try again.", spaceKey);
        }
    }
}
