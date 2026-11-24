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

import app.krista.model.base.FreeForm;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class JsonParser {



    public static FreeForm convertJsonToFreeForm(String jsonString) {
        try {
            Gson gson = new Gson();
            Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> jsonMap = gson.fromJson(jsonString, mapType);

            FreeForm freeForm = new FreeForm();
            convertMapToFreeForm(jsonMap, freeForm);

            return freeForm;
        } catch (Exception e) {
            // Return a FreeForm with the raw JSON string as fallback
            FreeForm fallbackFreeForm = new FreeForm();
            fallbackFreeForm.put("rawData", "String", jsonString);
            fallbackFreeForm.put("error", "String", "Failed to parse JSON: " + e.getMessage());
            return fallbackFreeForm;
        }
    }

    /**
     * Recursively converts a Map structure to FreeForm, handling deeply nested Maps.
     *
     * @param sourceMap The source Map to convert
     * @param targetFreeForm The target FreeForm to populate
     */
    @SuppressWarnings("unchecked")
    public static void convertMapToFreeForm(Map<String, Object> sourceMap, FreeForm targetFreeForm) {
        for (Map.Entry<String, Object> entry : sourceMap.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value == null) {
                targetFreeForm.put(key, "Text", "");
            } else if (value instanceof Map) {
                // Handle nested Map - create a new FreeForm for the nested structure
                FreeForm nestedFreeForm = new FreeForm();
                convertMapToFreeForm((Map<String, Object>) value, nestedFreeForm);
                targetFreeForm.put(key, "FreeForm", nestedFreeForm);
            } else if (value instanceof List) {
                // Handle List - convert to string representation for now
                // Could be enhanced to handle Lists of Maps if needed
                targetFreeForm.put(key, "Text", value.toString());
            } else {
                // Handle primitive values (String, Number, Boolean, etc.)
                targetFreeForm.put(key, "Text", String.valueOf(value));
            }
        }
    }
}
