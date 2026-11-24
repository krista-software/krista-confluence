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

import com.google.gson.Gson;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for the GsonFactory utility class.
 */
@DisplayName("GsonFactory Tests")
class GsonFactoryTest {

    @Test
    @DisplayName("getInstance should return non-null Gson instance")
    void getInstanceShouldReturnNonNullGson() {
        Gson gson = GsonFactory.getInstance();

        assertNotNull(gson);
    }

    @Test
    @DisplayName("getInstance should return a Gson instance")
    void getInstanceShouldReturnGsonType() {
        Gson gson = GsonFactory.getInstance();

        assertInstanceOf(Gson.class, gson);
    }

    @Test
    @DisplayName("getInstance should return the same instance on subsequent calls (singleton)")
    void getInstanceShouldReturnSameInstanceOnSubsequentCalls() {
        Gson first = GsonFactory.getInstance();
        Gson second = GsonFactory.getInstance();

        assertSame(first, second, "getInstance should return the same Gson singleton instance");
    }

    @Test
    @DisplayName("Gson instance should serialize objects to JSON")
    void gsonInstanceShouldSerializeObjects() {
        Gson gson = GsonFactory.getInstance();
        TestData data = new TestData("test", 42);

        String json = gson.toJson(data);

        assertNotNull(json);
        assertTrue(json.contains("\"name\""));
        assertTrue(json.contains("\"test\""));
        assertTrue(json.contains("42"));
    }

    @Test
    @DisplayName("Gson instance should produce pretty-printed output")
    void gsonInstanceShouldProducePrettyPrintedOutput() {
        Gson gson = GsonFactory.getInstance();
        TestData data = new TestData("test", 42);

        String json = gson.toJson(data);

        assertTrue(json.contains("\n"),
                "Gson instance should produce pretty-printed output with newlines");
    }

    @Test
    @DisplayName("Gson instance should deserialize JSON strings")
    void gsonInstanceShouldDeserializeJson() {
        Gson gson = GsonFactory.getInstance();
        String json = "{\"name\":\"hello\",\"count\":99}";

        TestData data = gson.fromJson(json, TestData.class);

        assertNotNull(data);
        assertEquals("hello", data.name);
        assertEquals(99, data.count);
    }

    @Test
    @DisplayName("Constructor should be private")
    void constructorShouldBePrivate() throws NoSuchMethodException {
        Constructor<GsonFactory> constructor = GsonFactory.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()),
                "GsonFactory constructor should be private");
    }

    @Test
    @DisplayName("Private constructor should be invocable via reflection")
    void privateConstructorShouldBeInvocableViaReflection() throws Exception {
        Constructor<GsonFactory> constructor = GsonFactory.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        GsonFactory instance = constructor.newInstance();
        assertNotNull(instance);
    }

    /**
     * Simple test data class for serialization/deserialization tests.
     */
    private static class TestData {
        String name;
        int count;

        TestData(String name, int count) {
            this.name = name;
            this.count = count;
        }
    }
}
