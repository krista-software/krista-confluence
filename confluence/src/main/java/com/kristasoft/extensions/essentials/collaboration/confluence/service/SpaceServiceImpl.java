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
import com.kristasoft.extensions.essentials.collaboration.confluence.exception.ConfluenceRESTException;
import com.kristasoft.extensions.essentials.collaboration.confluence.utils.GsonFactory;
import com.kristasoft.extensions.essentials.collaboration.confluence.utils.UrlParser;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.net.HttpURLConnection;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Implementation of SpaceService for managing Confluence spaces.
 * Handles space retrieval operations.
 */
@Service
@ContractsProvided(SpaceService.class)
public class SpaceServiceImpl implements SpaceService {

    private static final Pattern NUMERIC_ID = Pattern.compile("^\\d+$");

    private final ConfluenceAttributes confluenceAttributes;

    @Inject
    public SpaceServiceImpl(ConfluenceAttributes confluenceAttributes) {
        this.confluenceAttributes = confluenceAttributes;
    }

    @Override
    public Space[] getSpaces() {
        List<Space> allSpaces = new ArrayList<>();
        String nextUrl = null;

        do {
            HttpResponse<String> response;
            if (nextUrl == null) {
                response = confluenceAttributes.getRestClient().doSyncGet(APIVersion.V2, null, "spaces");
            } else {
                UrlParser.ParsedUrl parsedUrl = UrlParser.parseNextUrl(nextUrl);
                response = confluenceAttributes.getRestClient().doSyncGet(APIVersion.V2, parsedUrl.getQueryParams(), parsedUrl.getPathComponents());
            }

            if (response.statusCode() == HttpURLConnection.HTTP_OK) {
                JsonObject resultObject = GsonFactory.getInstance().fromJson(response.body(), JsonObject.class);

                if (resultObject.has(ConfluenceConstants.RESULTS)) {
                    Space[] spacesBatch = GsonFactory.getInstance().fromJson(
                            GsonFactory.getInstance().toJson(resultObject.get(ConfluenceConstants.RESULTS)),
                            TypeToken.getArray(Space.class).getType());
                    allSpaces.addAll(Arrays.asList(spacesBatch));
                }

                nextUrl = null;
                if (resultObject.has("_links")) {
                    JsonObject links = resultObject.getAsJsonObject("_links");
                    if (links.has("next")) {
                        nextUrl = links.get("next").getAsString();
                    }
                }
            } else {
                String errorMessage = String.format("""
                        We couldn't retrieve your Confluence spaces.
                        This could mean:
                        • Your Confluence connection needs to be refreshed
                        • You don't have permission to view spaces
                        • There's a temporary service issue

                        What to do next:
                        1. Verify your Confluence connection settings
                        2. Check your space permissions
                        3. Try again in a moment

                        Technical details: HTTP %d""", response.statusCode());
                throw new ConfluenceRESTException(errorMessage);
            }
        } while (nextUrl != null);

        return allSpaces.toArray(new Space[0]);
    }

    @Override
    public Space getSpaceById(String idOrKey) {
        String input = idOrKey == null ? "" : idOrKey.trim();
        String numericId = NUMERIC_ID.matcher(input).matches() ? input : resolveSpaceIdFromKey(input);
        return fetchSpaceByNumericId(numericId, idOrKey);
    }

    private Space fetchSpaceByNumericId(String numericId, String originalInput) {
        HttpResponse<String> response = confluenceAttributes.getRestClient().doSyncGet(APIVersion.V2, null, "spaces", numericId);
        if (response.statusCode() == HttpURLConnection.HTTP_OK) {
            return GsonFactory.getInstance().fromJson(response.body(), Space.class);
        }
        String errorMessage = String.format("""
                We couldn't find the space with ID or Key '%s'.
                This could mean:
                • The space doesn't exist or was deleted
                • You don't have permission to view this space
                • There's a temporary connection issue

                What to do next:
                1. Use 'Get Spaces' to see available spaces
                2. Verify the Space ID or Key is correct
                3. Check your space permissions
                4. Try again in a moment

                Technical details: HTTP %d""", originalInput, response.statusCode());
        throw new ConfluenceRESTException(errorMessage);
    }

    @Override
    public Space getSpaceIdByAlias(String alias) {
        String input = alias == null ? "" : alias.trim();
        if (input.isEmpty()) {
            throw new ConfluenceRESTException("Space Alias is required. Provide the alias shown in Confluence URLs (e.g. 'johndoe', '~johndoe', 'MFS').");
        }

        String tildePrefixed = input.startsWith("~") ? null : "~" + input;

        String nextUrl = null;
        do {
            HttpResponse<String> response;
            if (nextUrl == null) {
                response = confluenceAttributes.getRestClient()
                        .doSyncGet(APIVersion.V2, Map.of("limit", "250"), "spaces");
            } else {
                UrlParser.ParsedUrl parsed = UrlParser.parseNextUrl(nextUrl);
                response = confluenceAttributes.getRestClient()
                        .doSyncGet(APIVersion.V2, parsed.getQueryParams(), parsed.getPathComponents());
            }

            if (response.statusCode() != HttpURLConnection.HTTP_OK) {
                throw new ConfluenceRESTException(String.format("""
                        We couldn't search Confluence spaces to resolve alias '%s'.
                        This could mean:
                        • Your Confluence connection needs to be refreshed
                        • You don't have permission to view spaces
                        • There's a temporary service issue

                        What to do next:
                        1. Verify your Confluence connection settings
                        2. Try again in a moment

                        Technical details: HTTP %d""", input, response.statusCode()));
            }

            JsonObject resultObject = GsonFactory.getInstance().fromJson(response.body(), JsonObject.class);
            if (resultObject.has(ConfluenceConstants.RESULTS)) {
                Space[] page = GsonFactory.getInstance().fromJson(
                        GsonFactory.getInstance().toJson(resultObject.get(ConfluenceConstants.RESULTS)),
                        TypeToken.getArray(Space.class).getType());
                for (Space space : page) {
                    String alias1 = space.getCurrentActiveAlias();
                    if (alias1 != null && (alias1.equals(input) || (tildePrefixed != null && alias1.equals(tildePrefixed)))) {
                        return space;
                    }
                }
            }

            nextUrl = extractNextUrl(resultObject);
        } while (nextUrl != null);

        throw new ConfluenceRESTException(String.format("""
                No space found with alias '%s'.
                This could mean:
                • The alias is misspelled
                • The space was renamed (only the current active alias is matched; previous aliases are not)
                • You don't have permission to view this space

                What to do next:
                1. Use 'Get Spaces' to see each space's currentActiveAlias
                2. If the space was renamed, use the current alias""", input));
    }

    private String extractNextUrl(JsonObject resultObject) {
        if (!resultObject.has("_links")) {
            return null;
        }
        JsonObject links = resultObject.getAsJsonObject("_links");
        if (links.has("next") && !links.get("next").isJsonNull()) {
            return links.get("next").getAsString();
        }
        return null;
    }

    private String resolveSpaceIdFromKey(String key) {
        HttpResponse<String> response = confluenceAttributes.getRestClient()
                .doSyncGet(APIVersion.V2, Map.of("keys", key), "spaces");
        if (response.statusCode() != HttpURLConnection.HTTP_OK) {
            throw new ConfluenceRESTException(String.format("""
                    We couldn't look up the space by key '%s'.
                    This could mean:
                    • Your Confluence connection needs to be refreshed
                    • You don't have permission to view spaces
                    • There's a temporary service issue

                    What to do next:
                    1. Verify your Confluence connection settings
                    2. Try again in a moment

                    Technical details: HTTP %d""", key, response.statusCode()));
        }
        JsonObject resultObject = GsonFactory.getInstance().fromJson(response.body(), JsonObject.class);
        Space[] matches = resultObject.has(ConfluenceConstants.RESULTS)
                ? GsonFactory.getInstance().fromJson(
                GsonFactory.getInstance().toJson(resultObject.get(ConfluenceConstants.RESULTS)),
                TypeToken.getArray(Space.class).getType())
                : new Space[0];
        if (matches.length == 0) {
            throw new ConfluenceRESTException(String.format("""
                    No space found with key '%s'.
                    This could mean:
                    • The Space Key is misspelled
                    • The space was renamed (only the current Space Key is matched; historical aliases are not)
                    • You don't have permission to view this space

                    What to do next:
                    1. Use 'Get Spaces' to see available spaces and their current keys
                    2. Verify the Space Key in Confluence (Space settings → Space details)
                    3. If the space was renamed, use the current key""", key));
        }
        return matches[0].getId();
    }
}

