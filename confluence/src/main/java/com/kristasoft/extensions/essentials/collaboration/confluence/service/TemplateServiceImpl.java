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

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.kristasoft.extensions.essentials.collaboration.confluence.ConfluenceAttributes;
import com.kristasoft.extensions.essentials.collaboration.confluence.api.APIVersion;
import com.kristasoft.extensions.essentials.collaboration.confluence.constants.ConfluenceConstants;
import com.kristasoft.extensions.essentials.collaboration.confluence.dto.Space;
import com.kristasoft.extensions.essentials.collaboration.confluence.dto.Template;
import com.kristasoft.extensions.essentials.collaboration.confluence.exception.ConfluenceRESTException;
import com.kristasoft.extensions.essentials.collaboration.confluence.utils.GsonFactory;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.net.HttpURLConnection;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation of TemplateService for managing Confluence templates.
 * Handles template retrieval operations.
 */
@Service
@ContractsProvided(TemplateService.class)
public class TemplateServiceImpl implements TemplateService {

    private final ConfluenceAttributes confluenceAttributes;
    private final SpaceService spaceService;

    @Inject
    public TemplateServiceImpl(ConfluenceAttributes confluenceAttributes, SpaceService spaceService) {
        this.confluenceAttributes = confluenceAttributes;
        this.spaceService = spaceService;
    }

    @Override
    public Template getTemplateById(String templateId) {
        HttpResponse<String> response = confluenceAttributes.getRestClient().doSyncGet(APIVersion.V1, Map.of("expand", "body.storage"), "template", templateId);
        if (response.statusCode() == HttpURLConnection.HTTP_OK) {
            return GsonFactory.getInstance().fromJson(response.body(), Template.class);
        } else {
            String errorMessage = String.format("""
                    We couldn't find the template with ID '%s'.
                    This could mean:
                    • The Template ID doesn't exist
                    • The template was deleted or moved
                    • You don't have permission to access this template

                    What to do next:
                    1. Use 'Get Templates By Space ID' to see available templates
                    2. Verify you have the correct Template ID
                    3. Check your template permissions

                    Technical details: HTTP %d""", templateId, response.statusCode());
            throw new ConfluenceRESTException(errorMessage);
        }
    }

    @Override
    public Template[] getTemplatesBySpaceId(String spaceId) {
        Space space = spaceService.getSpaceById(spaceId);
        Optional<String> spaceKey = Optional.ofNullable(space.getKey());
        if (spaceKey.isPresent()) {
            HttpResponse<String> response = confluenceAttributes.getRestClient().doSyncGet(APIVersion.V1, Map.of("spaceKey", spaceKey.get(),
                    "expand", "body.storage"), "template", "page");
            if (response.statusCode() == HttpURLConnection.HTTP_OK) {
                JsonObject resultObject = GsonFactory.getInstance().fromJson(response.body(), JsonObject.class);
                Template[] templates = GsonFactory.getInstance().fromJson(GsonFactory.getInstance().toJson(resultObject.get(ConfluenceConstants.RESULTS)),
                        TypeToken.getArray(Template.class).getType());
                return Arrays.stream(templates).peek(template -> template.setSpaceId(spaceId)).toArray(Template[]::new);
            } else {
                String errorMessage = String.format("""
                        We couldn't retrieve templates from space '%s'.
                        This could mean:
                        • The space doesn't exist or has no templates
                        • You don't have permission to view templates in this space
                        • There's a temporary connection issue

                        What to do next:
                        1. Use 'Get Spaces' to verify the space exists
                        2. Check your space and template permissions
                        3. Try again in a moment

                        Technical details: HTTP %d""", spaceId, response.statusCode());
                throw new ConfluenceRESTException(errorMessage);
            }
        } else {
            String errorMessage = String.format("""
                    We couldn't find the space with ID '%s'.
                    This could mean:
                    • The Space ID doesn't exist
                    • The space was deleted
                    • You don't have permission to access this space

                    What to do next:
                    1. Use 'Get Spaces' to see available spaces
                    2. Verify the Space ID is correct
                    3. Check your space permissions""", spaceId);
            throw new ConfluenceRESTException(errorMessage);
        }
    }
}

