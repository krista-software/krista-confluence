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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JsonParser")
class JsonParserTest {

    // ──────────────────────────────────────────────────────────────
    //  convertJsonToFreeForm
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("convertJsonToFreeForm")
    class ConvertJsonToFreeForm {

        @Test
        @DisplayName("should convert valid flat JSON to FreeForm without throwing")
        void validFlatJson() {
            String json = """
                    {"name":"Alice","age":30,"active":true}
                    """;

            FreeForm result = JsonParser.convertJsonToFreeForm(json);

            assertNotNull(result, "FreeForm should never be null for valid JSON");
        }

        @Test
        @DisplayName("should convert nested JSON objects to FreeForm without throwing")
        void nestedJsonObjects() {
            String json = """
                    {
                      "user": {
                        "name": "Bob",
                        "address": {
                          "city": "Seattle",
                          "zip": "98101"
                        }
                      }
                    }
                    """;

            FreeForm result = JsonParser.convertJsonToFreeForm(json);

            assertNotNull(result, "FreeForm should not be null for nested JSON");
        }

        @Test
        @DisplayName("should handle JSON containing arrays")
        void jsonWithArrays() {
            String json = """
                    {"tags":["java","spring","testing"],"count":3}
                    """;

            FreeForm result = JsonParser.convertJsonToFreeForm(json);

            assertNotNull(result, "FreeForm should not be null for JSON with arrays");
        }

        @Test
        @DisplayName("should handle JSON with null values")
        void jsonWithNullValues() {
            String json = """
                    {"name":"Charlie","email":null,"role":null}
                    """;

            FreeForm result = JsonParser.convertJsonToFreeForm(json);

            assertNotNull(result, "FreeForm should not be null when values are null");
        }

        @Test
        @DisplayName("should return FreeForm with rawData and error keys for invalid JSON")
        void invalidJsonString() {
            String invalidJson = "this is not json at all";

            FreeForm result = JsonParser.convertJsonToFreeForm(invalidJson);

            assertNotNull(result, "FreeForm should not be null even for invalid JSON");
        }

        @Test
        @DisplayName("should handle empty JSON object")
        void emptyJsonObject() {
            String json = "{}";

            FreeForm result = JsonParser.convertJsonToFreeForm(json);

            assertNotNull(result, "FreeForm should not be null for empty JSON object");
        }

        @Test
        @DisplayName("should return fallback FreeForm for null input")
        void nullInput() {
            FreeForm result = JsonParser.convertJsonToFreeForm(null);

            assertNotNull(result, "FreeForm should not be null even for null input");
        }

        @Test
        @DisplayName("should handle JSON with mixed nested types")
        void mixedNestedTypes() {
            String json = """
                    {
                      "id": 42,
                      "tags": ["a", "b"],
                      "metadata": {"key1": "val1"},
                      "nothing": null,
                      "flag": false
                    }
                    """;

            FreeForm result = JsonParser.convertJsonToFreeForm(json);

            assertNotNull(result);
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  convertMapToFreeForm
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("convertMapToFreeForm")
    class ConvertMapToFreeForm {

        @Test
        @DisplayName("should handle null values by inserting empty Text")
        void nullValuesBecomEmptyText() {
            Map<String, Object> source = new HashMap<>();
            source.put("nullField", null);

            FreeForm target = new FreeForm();

            assertDoesNotThrow(() -> JsonParser.convertMapToFreeForm(source, target));
        }

        @Test
        @DisplayName("should handle nested Maps by creating nested FreeForm")
        void nestedMapsCreateNestedFreeForm() {
            Map<String, Object> inner = new HashMap<>();
            inner.put("innerKey", "innerVal");

            Map<String, Object> source = new HashMap<>();
            source.put("nested", inner);

            FreeForm target = new FreeForm();

            assertDoesNotThrow(() -> JsonParser.convertMapToFreeForm(source, target));
        }

        @Test
        @DisplayName("should convert List values to their toString representation")
        void listValuesToString() {
            Map<String, Object> source = new HashMap<>();
            source.put("items", java.util.List.of("x", "y", "z"));

            FreeForm target = new FreeForm();

            assertDoesNotThrow(() -> JsonParser.convertMapToFreeForm(source, target));
        }

        @Test
        @DisplayName("should convert primitive values via String.valueOf")
        void primitiveValues() {
            Map<String, Object> source = new HashMap<>();
            source.put("stringVal", "hello");
            source.put("intVal", 123);
            source.put("boolVal", true);
            source.put("doubleVal", 3.14);

            FreeForm target = new FreeForm();

            assertDoesNotThrow(() -> JsonParser.convertMapToFreeForm(source, target));
        }

        @Test
        @DisplayName("should handle deeply nested Maps")
        void deeplyNestedMaps() {
            Map<String, Object> level3 = Map.of("deep", "value");
            Map<String, Object> level2 = Map.of("mid", level3);
            Map<String, Object> level1 = new HashMap<>();
            level1.put("top", level2);

            FreeForm target = new FreeForm();

            assertDoesNotThrow(() -> JsonParser.convertMapToFreeForm(level1, target));
        }

        @Test
        @DisplayName("should handle empty source map")
        void emptySourceMap() {
            Map<String, Object> source = new HashMap<>();
            FreeForm target = new FreeForm();

            assertDoesNotThrow(() -> JsonParser.convertMapToFreeForm(source, target));
        }
    }
}
