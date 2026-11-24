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

import com.google.gson.*;
import com.kristasoft.extensions.essentials.collaboration.confluence.dto.Space;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Custom deserializer for Space objects to handle cases where the description field
 * might be returned as an object instead of a string from the Confluence API.
 */
public class SpaceDeserializer implements JsonDeserializer<Space> {
    private static final Logger logger = LoggerFactory.getLogger(SpaceDeserializer.class);
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    @Override
    public Space deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        
        Space.SpaceBuilder builder = Space.builder();
        
        // Handle standard string fields
        if (jsonObject.has("authorId") && !jsonObject.get("authorId").isJsonNull()) {
            builder.setAuthorId(jsonObject.get("authorId").getAsString());
        }
        
        if (jsonObject.has("homepageId") && !jsonObject.get("homepageId").isJsonNull()) {
            builder.setHomepageId(jsonObject.get("homepageId").getAsString());
        }
        
        if (jsonObject.has("icon") && !jsonObject.get("icon").isJsonNull()) {
            builder.setIcon(jsonObject.get("icon").getAsString());
        }
        
        if (jsonObject.has("name") && !jsonObject.get("name").isJsonNull()) {
            builder.setName(jsonObject.get("name").getAsString());
        }
        
        if (jsonObject.has("key") && !jsonObject.get("key").isJsonNull()) {
            builder.setKey(jsonObject.get("key").getAsString());
        }
        
        if (jsonObject.has("id") && !jsonObject.get("id").isJsonNull()) {
            builder.setId(jsonObject.get("id").getAsString());
        }
        
        if (jsonObject.has("type") && !jsonObject.get("type").isJsonNull()) {
            builder.setType(jsonObject.get("type").getAsString());
        }
        
        if (jsonObject.has("status") && !jsonObject.get("status").isJsonNull()) {
            builder.setStatus(jsonObject.get("status").getAsString());
        }

        if (jsonObject.has("currentActiveAlias") && !jsonObject.get("currentActiveAlias").isJsonNull()) {
            builder.setCurrentActiveAlias(jsonObject.get("currentActiveAlias").getAsString());
        }

        // Handle description field - can be either string or object
        if (jsonObject.has("description") && !jsonObject.get("description").isJsonNull()) {
            JsonElement descriptionElement = jsonObject.get("description");
            String description = null;
            
            if (descriptionElement.isJsonPrimitive() && descriptionElement.getAsJsonPrimitive().isString()) {
                // Description is a string
                description = descriptionElement.getAsString();
                logger.debug("Description parsed as string: {}", description);
            } else if (descriptionElement.isJsonObject()) {
                // Description is an object - extract meaningful content or set to empty string
                JsonObject descObj = descriptionElement.getAsJsonObject();
                if (descObj.has("value")) {
                    description = descObj.get("value").getAsString();
                    logger.debug("Description parsed from object.value: {}", description);
                } else {
                    description = ""; // Empty object, set to empty string
                    logger.debug("Description is empty object, setting to empty string");
                }
            } else {
                description = ""; // Fallback to empty string
                logger.warn("Description field has unexpected format, setting to empty string");
            }
            
            builder.setDescription(description);
        }
        
        // Handle createdAt date field
        if (jsonObject.has("createdAt") && !jsonObject.get("createdAt").isJsonNull()) {
            try {
                String dateString = jsonObject.get("createdAt").getAsString();
                Date createdAt = dateFormat.parse(dateString);
                builder.setCreatedAt(createdAt);
                logger.debug("CreatedAt parsed successfully: {}", createdAt);
            } catch (ParseException e) {
                logger.warn("Failed to parse createdAt date: {}", jsonObject.get("createdAt").getAsString(), e);
                // Continue without setting the date
            }
        }
        
        return builder.build();
    }
}
