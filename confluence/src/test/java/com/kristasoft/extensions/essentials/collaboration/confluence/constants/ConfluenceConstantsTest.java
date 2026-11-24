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

package com.kristasoft.extensions.essentials.collaboration.confluence.constants;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for the ConfluenceConstants class in the constants package.
 */
@DisplayName("ConfluenceConstants (constants package) Tests")
class ConfluenceConstantsTest {

    @Test
    @DisplayName("DRAFT constant should have value 'draft'")
    void draftConstantShouldHaveCorrectValue() {
        assertEquals("draft", ConfluenceConstants.DRAFT);
    }

    @Test
    @DisplayName("STORAGE constant should have value 'storage'")
    void storageConstantShouldHaveCorrectValue() {
        assertEquals("storage", ConfluenceConstants.STORAGE);
    }

    @Test
    @DisplayName("STATUS constant should have value 'status'")
    void statusConstantShouldHaveCorrectValue() {
        assertEquals("status", ConfluenceConstants.STATUS);
    }

    @Test
    @DisplayName("RESULTS constant should have value 'results'")
    void resultsConstantShouldHaveCorrectValue() {
        assertEquals("results", ConfluenceConstants.RESULTS);
    }

    @Test
    @DisplayName("CURRENT constant should have value 'current'")
    void currentConstantShouldHaveCorrectValue() {
        assertEquals("current", ConfluenceConstants.CURRENT);
    }

    @Test
    @DisplayName("PAGES constant should have value 'pages'")
    void pagesConstantShouldHaveCorrectValue() {
        assertEquals("pages", ConfluenceConstants.PAGES);
    }

    @Test
    @DisplayName("TITLE constant should have value 'title'")
    void titleConstantShouldHaveCorrectValue() {
        assertEquals("title", ConfluenceConstants.TITLE);
    }

    @Test
    @DisplayName("Constructor should be private")
    void constructorShouldBePrivate() throws NoSuchMethodException {
        Constructor<ConfluenceConstants> constructor = ConfluenceConstants.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()),
                "Constructor should be private");
    }

    @Test
    @DisplayName("Private constructor should be invocable via reflection")
    void privateConstructorShouldBeInvocableViaReflection() throws Exception {
        Constructor<ConfluenceConstants> constructor = ConfluenceConstants.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        ConfluenceConstants instance = constructor.newInstance();
        assertNotNull(instance);
    }

    @Test
    @DisplayName("All constants should be non-null")
    void allConstantsShouldBeNonNull() {
        assertNotNull(ConfluenceConstants.DRAFT);
        assertNotNull(ConfluenceConstants.STORAGE);
        assertNotNull(ConfluenceConstants.STATUS);
        assertNotNull(ConfluenceConstants.RESULTS);
        assertNotNull(ConfluenceConstants.CURRENT);
        assertNotNull(ConfluenceConstants.PAGES);
        assertNotNull(ConfluenceConstants.TITLE);
    }

    @Test
    @DisplayName("All constants should be non-empty strings")
    void allConstantsShouldBeNonEmpty() {
        assertFalse(ConfluenceConstants.DRAFT.isEmpty());
        assertFalse(ConfluenceConstants.STORAGE.isEmpty());
        assertFalse(ConfluenceConstants.STATUS.isEmpty());
        assertFalse(ConfluenceConstants.RESULTS.isEmpty());
        assertFalse(ConfluenceConstants.CURRENT.isEmpty());
        assertFalse(ConfluenceConstants.PAGES.isEmpty());
        assertFalse(ConfluenceConstants.TITLE.isEmpty());
    }
}
