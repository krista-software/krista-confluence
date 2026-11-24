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

package com.kristasoft.extensions.essentials.collaboration.confluence.authentication;

import app.krista.sdk.atlassian.registry.AtlassianExtensionDescriptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for the ConfluenceExtensionDescriptor class.
 */
@DisplayName("ConfluenceExtensionDescriptor Tests")
class ConfluenceExtensionDescriptorTest {

    private ConfluenceExtensionDescriptor descriptor;

    @BeforeEach
    void setUp() {
        descriptor = new ConfluenceExtensionDescriptor();
    }

    @Test
    @DisplayName("Should implement AtlassianExtensionDescriptor interface")
    void shouldImplementAtlassianExtensionDescriptor() {
        assertInstanceOf(AtlassianExtensionDescriptor.class, descriptor);
    }

    @Test
    @DisplayName("getConfigClass should return ConfluenceExtensionConfig.class")
    void getConfigClassShouldReturnConfluenceExtensionConfig() {
        Class<?> configClass = descriptor.getConfigClass();
        assertEquals(ConfluenceExtensionConfig.class, configClass);
    }

    @Test
    @DisplayName("getConfigClass should return non-null value")
    void getConfigClassShouldReturnNonNull() {
        assertNotNull(descriptor.getConfigClass());
    }

    @Test
    @DisplayName("getConfigClass should always return the same class")
    void getConfigClassShouldAlwaysReturnSameClass() {
        Class<?> firstCall = descriptor.getConfigClass();
        Class<?> secondCall = descriptor.getConfigClass();
        assertSame(firstCall, secondCall);
    }

    @Test
    @DisplayName("Multiple descriptor instances should return the same config class")
    void multipleDescriptorInstancesShouldReturnSameConfigClass() {
        ConfluenceExtensionDescriptor anotherDescriptor = new ConfluenceExtensionDescriptor();
        assertEquals(descriptor.getConfigClass(), anotherDescriptor.getConfigClass());
    }

    @Test
    @DisplayName("Class should be declared final")
    void classShouldBeFinal() {
        assertTrue(Modifier.isFinal(ConfluenceExtensionDescriptor.class.getModifiers()),
                "ConfluenceExtensionDescriptor should be declared final");
    }

    @Test
    @DisplayName("Config class should have AtlassianExtension annotation")
    void configClassShouldHaveAtlassianExtensionAnnotation() {
        Class<?> configClass = descriptor.getConfigClass();
        assertTrue(configClass.isAnnotationPresent(
                app.krista.sdk.atlassian.registry.annotations.AtlassianExtension.class),
                "Config class should be annotated with @AtlassianExtension");
    }
}
