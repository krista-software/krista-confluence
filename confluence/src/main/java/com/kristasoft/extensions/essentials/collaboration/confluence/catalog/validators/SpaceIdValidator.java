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
 * Validator for Space ID fields in Confluence operations.
 * <p>
 * This validator ensures that Space ID values are properly formatted and not empty.
 * It validates that the Space ID is a non-null, non-empty string that can be used
 * to identify a specific space in Confluence. It also validates against the actual
 * Confluence service to ensure the space exists.
 * </p>
 */
public class SpaceIdValidator implements Validator {

    private final ConfluenceService confluenceService;

    public SpaceIdValidator(ConfluenceService confluenceService) {
        this.confluenceService = confluenceService;
    }

    @Override
    public Boolean validate(String spaceId, Map<ValidationResource, String> context) {
        if (spaceId == null || spaceId.trim().isEmpty()) {
            return false;
        }

        String trimmedId = spaceId.trim();

        // Space ID should be a valid identifier (non-empty string)
        if (trimmedId.length() == 0) {
            return false;
        }

        // Validate against Confluence service if available
        if (confluenceService != null) {
            try {
                // Try to get the space by ID to validate it exists
                confluenceService.getSpaceById(trimmedId);
                return true;
            } catch (Exception e) {
                // If space doesn't exist or service call fails, validation fails
                return false;
            }
        }

        return true;
    }

    @Override
    public String getFetchFieldName() {
        return "Space Id";
    }

    @Override
    public String getFetchStepMessage() {
        return "Please enter a valid Space ID. You can find the Space ID in your Confluence space settings or by using the 'Get Spaces' request.";
    }

    @Override
    public String getConfirmationStepMessage(String spaceId, Map<ValidationResource, String> context) {
        return String.format("We couldn't find a space with ID '%s'. This space may not exist or you may not have permission to access it. Would you like to try a different Space ID?",
                spaceId != null ? spaceId : "not provided");
    }

    @Override
    public String getErrMessage(String spaceId) {
        if (spaceId == null) {
            return "Space ID is required. Please provide a Space ID to continue. Tip: Use 'Get Spaces' to view available spaces and their IDs.";
        } else if (spaceId.trim().isEmpty()) {
            return "Space ID cannot be empty. Please enter a valid Space ID. Tip: Use 'Get Spaces' to view available spaces and their IDs.";
        } else {
            return String.format("Space ID '%s' not found. This space may not exist or you may not have access to it. Please verify the Space ID and try again.", spaceId);
        }
    }
}
