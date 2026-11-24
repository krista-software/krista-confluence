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

package com.kristasoft.extensions.essentials.collaboration.confluence.catalog.errorhandlers;

import app.krista.extensions.util.KeyValueStore;
import com.kristasoft.extensions.essentials.collaboration.confluence.utils.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.jvnet.hk2.annotations.Service;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.inject.Inject;
import java.lang.reflect.Constructor;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ErrorHandlingStateManager Tests")
class ErrorHandlingStateManagerTest {

    @Mock
    private KeyValueStore keyValueStore;

    private ErrorHandlingStateManager stateManager;

    @BeforeEach
    void setUp() {
        stateManager = new ErrorHandlingStateManager(keyValueStore);
    }

    // ──────────────────────────────────────────────────────────────
    // Annotation verification
    // ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Class should be annotated with @Service")
    void classShouldBeAnnotatedWithService() {
        assertTrue(
                ErrorHandlingStateManager.class.isAnnotationPresent(Service.class),
                "ErrorHandlingStateManager should have @Service annotation"
        );
    }

    @Test
    @DisplayName("Constructor should be annotated with @Inject")
    void constructorShouldBeAnnotatedWithInject() throws NoSuchMethodException {
        Constructor<ErrorHandlingStateManager> constructor =
                ErrorHandlingStateManager.class.getConstructor(KeyValueStore.class);
        assertTrue(
                constructor.isAnnotationPresent(Inject.class),
                "Constructor should have @Inject annotation"
        );
    }

    @Test
    @DisplayName("Constructor should accept KeyValueStore parameter")
    void constructorShouldAcceptKeyValueStore() {
        assertNotNull(stateManager, "StateManager should be created with KeyValueStore");
    }

    // ──────────────────────────────────────────────────────────────
    // put(String key, Map<String, Object> value)
    // ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("put() should serialize map to JSON and store via parent")
    void putShouldSerializeMapToJsonAndStore() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("name", "test");
        value.put("count", 42);

        stateManager.put("testKey", value);

        String expectedJson = Constants.GSON.toJson(value);
        verify(keyValueStore).put(eq("testKey"), eq(expectedJson));
    }

    @Test
    @DisplayName("put() should produce pretty-printed JSON via Constants.GSON")
    void putShouldProducePrettyPrintedJson() {
        Map<String, Object> value = Map.of("key", "val");

        stateManager.put("myKey", value);

        // Constants.GSON uses GsonBuilder().setPrettyPrinting(), so JSON should contain newlines
        String expectedJson = Constants.GSON.toJson(value);
        assertTrue(expectedJson.contains("\n"), "Pretty-printed JSON should contain newlines");
        verify(keyValueStore).put(eq("myKey"), eq(expectedJson));
    }

    @Test
    @DisplayName("put() with empty map should serialize to empty JSON object")
    void putWithEmptyMapShouldSerializeToEmptyJsonObject() {
        Map<String, Object> emptyMap = Map.of();

        stateManager.put("emptyKey", emptyMap);

        String expectedJson = Constants.GSON.toJson(emptyMap);
        verify(keyValueStore).put(eq("emptyKey"), eq(expectedJson));
    }

    @Test
    @DisplayName("put() with nested map should serialize nested structure")
    void putWithNestedMapShouldSerializeNestedStructure() {
        Map<String, Object> nested = new LinkedHashMap<>();
        nested.put("inner", Map.of("deep", "value"));
        nested.put("list", java.util.List.of(1, 2, 3));

        stateManager.put("nestedKey", nested);

        String expectedJson = Constants.GSON.toJson(nested);
        verify(keyValueStore).put(eq("nestedKey"), eq(expectedJson));
    }

    // ──────────────────────────────────────────────────────────────
    // get(String key)
    // ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("get() should deserialize stored JSON back into a map")
    void getShouldDeserializeJsonIntoMap() {
        when(keyValueStore.get("testKey")).thenReturn("{\"name\":\"test\",\"count\":42}");

        Map<String, Object> result = stateManager.get("testKey");

        assertNotNull(result);
        assertEquals("test", result.get("name"));
        // Gson deserializes numbers to Double for Map<String, Object>.
        assertEquals(42.0, result.get("count"));
        verify(keyValueStore).get("testKey");
    }

    @Test
    @DisplayName("get() should round-trip a map stored via put()")
    void getShouldRoundTripValueStoredViaPut() {
        Map<String, Object> original = new LinkedHashMap<>();
        original.put("alpha", "one");
        original.put("beta", "two");
        // The value put() persists is what get() will read back.
        String stored = Constants.GSON.toJson(original);
        when(keyValueStore.get("roundTrip")).thenReturn(stored);

        Map<String, Object> result = stateManager.get("roundTrip");

        assertEquals("one", result.get("alpha"));
        assertEquals("two", result.get("beta"));
    }

    @Test
    @DisplayName("get() should return an empty map for a stored empty JSON object")
    void getShouldReturnEmptyMapForEmptyJsonObject() {
        when(keyValueStore.get("emptyKey")).thenReturn("{}");

        Map<String, Object> result = stateManager.get("emptyKey");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("get() should return null when the stored value is null")
    void getShouldReturnNullWhenStoredValueIsNull() {
        // entries.get() returns null → String.valueOf(null) == "null" → Gson parses to null.
        when(keyValueStore.get("missingKey")).thenReturn(null);

        Map<String, Object> result = stateManager.get("missingKey");

        assertNull(result);
        verify(keyValueStore).get("missingKey");
    }

    @Test
    @DisplayName("get() should return null when the key is absent (literal 'null' text)")
    void getShouldReturnNullForLiteralNullText() {
        when(keyValueStore.get("nullText")).thenReturn("null");

        Map<String, Object> result = stateManager.get("nullText");

        assertNull(result);
    }

    @Test
    @DisplayName("get() should deserialize nested structures")
    void getShouldDeserializeNestedStructures() {
        when(keyValueStore.get("nestedKey"))
                .thenReturn("{\"inner\":{\"deep\":\"value\"},\"list\":[1,2,3]}");

        Map<String, Object> result = stateManager.get("nestedKey");

        assertNotNull(result);
        @SuppressWarnings("unchecked")
        Map<String, Object> inner = (Map<String, Object>) result.get("inner");
        assertEquals("value", inner.get("deep"));
        assertEquals(java.util.List.of(1.0, 2.0, 3.0), result.get("list"));
    }
}
