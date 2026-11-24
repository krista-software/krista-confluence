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

package com.kristasoft.extensions.essentials.collaboration.confluence.utils;

import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.entities.Page;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.entities.Space;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.entities.Template;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.entities.User;
import com.kristasoft.extensions.essentials.collaboration.confluence.dto.Version;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class ParserUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParserUtils.class);

    private ParserUtils() {
    }

    public static Template setTemplateEntity(com.kristasoft.extensions.essentials.collaboration.confluence.dto.Template dto) {
        Template entity = new Template();
        Optional.ofNullable(dto.getBody()).ifPresent(body -> {
            entity.body = body.getStorage().getValue().trim();
            Document document = Jsoup.parse(body.getStorage().getValue().trim());
            entity.variables = extractVariables(document);
        });
        entity.id = dto.getTemplateId();
        entity.spaceId = dto.getSpaceId();
        entity.name = dto.getName();
        entity.type = dto.getTemplateType();
        return entity;
    }

    public static Space setSpaceEntity(com.kristasoft.extensions.essentials.collaboration.confluence.dto.Space dto) {
        Space entity = new Space();
        entity.id = dto.getId();
        entity.status = dto.getStatus();
        entity.createdAt = dto.getCreatedAt() != null ? dto.getCreatedAt().getTime() : null;
        entity.type = dto.getType();
        entity.name = dto.getName();
        entity.description = dto.getDescription();
        return entity;
    }

    public static Page setPageEntity(com.kristasoft.extensions.essentials.collaboration.confluence.dto.Page dto) {
        Page entity = new Page();
        entity.id = dto.getId();

        // Safely handle links that might be null (especially for V1 API responses)
        String webui = "";
        if (dto.getLinks() != null) {
            webui = dto.getLinks().getOrDefault("webui", "");
        }
        // Plain, clickable URL (no <a href> wrapper). domainUrl already resolves to the
        // real site subdomain (e.g. https://acme.atlassian.net/wiki); webui is the
        // page-relative path (e.g. /spaces/ENG/pages/12345/Title).
        String domainUrl = dto.getDomainUrl() != null ? dto.getDomainUrl() : "";
        entity.pageURL = domainUrl + webui;

        entity.status = dto.getStatus();
        // Set type - defaults to "page" if not specified (for backward compatibility)
        entity.type = dto.getType() != null ? dto.getType() : "page";
        Optional.ofNullable(dto.getBody()).ifPresent(body ->
            Optional.ofNullable(body.getStorage()).ifPresent(storage -> entity.body = storage.getValue().trim()));
        entity.title = dto.getTitle();
        entity.spaceID = dto.getSpaceId();
        entity.parentID = dto.getParentId();
        entity.parentType = dto.getParentType();
        // hasChildren will be set separately when needed
        entity.hasChildren = false;
        entity.createdAt = parseIsoToEpochMillis(dto.getCreatedAt());
        Version version = dto.getVersion();
        entity.lastUpdated = version != null ? parseIsoToEpochMillis(version.getCreatedAt()) : null;
        return entity;
    }

    /**
     * Converts a User DTO to a User entity for catalog operations.
     * <p>
     * Note: Null values are expected and acceptable for optional fields such as
     * displayName, email, publicName, accountType, accountStatus, timeZone, locale,
     * isExternalCollaborator, isGuest, and profilePicturePath. These fields will be
     * set to null in the entity if not present in the DTO response from the Confluence API.
     * Only the accountID field is guaranteed to be non-null.
     * </p>
     *
     * @param dto the User DTO from Confluence API response
     * @return User entity with all available fields populated (some may be null)
     */
    public static User setUserEntity(com.kristasoft.extensions.essentials.collaboration.confluence.dto.User dto) {
        User entity = new User();
        entity.accountID = dto.getAccountId();
        entity.displayName = dto.getDisplayName();
        entity.email = dto.getEmail();
        entity.publicName = dto.getPublicName();
        entity.accountType = dto.getAccountType();
        entity.accountStatus = dto.getAccountStatus();
        entity.timeZone = dto.getTimeZone();
        entity.locale = dto.getLocale();
        entity.isExternalCollaborator = dto.getIsExternalCollaborator();
        entity.isGuest = dto.getIsGuest();
        // Extract profile picture path if available
        if (dto.getProfilePicture() != null) {
            entity.profilePicturePath = dto.getProfilePicture().getPath();
        }
        return entity;
    }

    private static List<Map<String, Object>> extractVariables(Document document) {
        List<String> keys = document.getElementsByTag("at:var").stream().map(element -> element.attributes().asList()
                        .stream().filter(att -> att.getKey().equals("at:name")).findAny())
                .filter(Optional::isPresent).map(attribute -> attribute.get().getValue()).toList();
        Map<String, List<String>> fields = document.getElementsByTag("at:list")
                .stream()
                .collect(Collectors.toMap(element -> element.attr("at:name").trim(),
                        element -> element.getElementsByTag("at:option").stream().map(option -> option.attr("at:value").trim()).toList()));
        return keys.stream().map(key ->
                Map.of("Key", key,
                "Value", "",
                "Options", fields.getOrDefault(key, Collections.emptyList()))).toList();
    }

    /**
     * Parse an ISO 8601 timestamp (e.g. {@code 2024-01-01T00:00:00.000Z}) to epoch milliseconds.
     * Returns null for null/blank input or unparseable values — so a malformed Confluence
     * response never breaks page listing, the date field simply ends up empty.
     */
    private static Long parseIsoToEpochMillis(String iso) {
        if (iso == null || iso.isEmpty()) {
            return null;
        }
        try {
            return Instant.parse(iso).toEpochMilli();
        } catch (DateTimeParseException e) {
            LOGGER.warn("→ parseIsoToEpochMillis: failed to parse '{}' — leaving date field null", iso);
            return null;
        }
    }
}
