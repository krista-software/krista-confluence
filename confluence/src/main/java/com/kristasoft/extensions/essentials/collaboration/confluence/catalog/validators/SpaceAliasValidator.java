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
 * Validator for Space Alias fields used by the {@code Get Space Id by Alias} request.
 * Resolution semantics live in {@link com.kristasoft.extensions.essentials.collaboration.confluence.service.SpaceServiceImpl#getSpaceIdByAlias};
 * this validator only checks non-empty input and that the alias resolves successfully.
 */
public class SpaceAliasValidator implements Validator {

    private final ConfluenceService confluenceService;

    public SpaceAliasValidator(ConfluenceService confluenceService) {
        this.confluenceService = confluenceService;
    }

    @Override
    public Boolean validate(String spaceAlias, Map<ValidationResource, String> context) {
        if (spaceAlias == null || spaceAlias.trim().isEmpty()) {
            return false;
        }

        if (confluenceService == null) {
            return true;
        }

        try {
            confluenceService.getSpaceIdByAlias(spaceAlias.trim());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getFetchFieldName() {
        return "Space Alias";
    }

    @Override
    public String getFetchStepMessage() {
        return "Please enter the Space Alias as shown in Confluence URLs (e.g. 'johndoe', '~johndoe', or 'MFS'). Use 'Get Spaces' to see each space's currentActiveAlias.";
    }

    @Override
    public String getConfirmationStepMessage(String spaceAlias, Map<ValidationResource, String> context) {
        return String.format("We couldn't find a space with alias '%s'. The alias may be misspelled, or the space may have been renamed (only the current active alias is matched). Would you like to try a different value?",
                spaceAlias != null ? spaceAlias : "not provided");
    }

    @Override
    public String getErrMessage(String spaceAlias) {
        if (spaceAlias == null) {
            return "Space Alias is required. Provide the alias shown in Confluence URLs (e.g. 'johndoe', '~johndoe', 'MFS').";
        } else if (spaceAlias.trim().isEmpty()) {
            return "Space Alias cannot be empty. Provide the alias shown in Confluence URLs (e.g. 'johndoe', '~johndoe', 'MFS').";
        } else {
            return String.format("No space found with alias '%s'. Only the current active alias is matched (renamed spaces use the new alias). Use 'Get Spaces' to see available aliases.", spaceAlias);
        }
    }
}
