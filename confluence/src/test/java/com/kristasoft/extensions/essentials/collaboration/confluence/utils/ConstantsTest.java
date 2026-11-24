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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for the Constants utility class.
 */
@DisplayName("Constants (utils package) Tests")
class ConstantsTest {

    @Nested
    @DisplayName("Gson instance tests")
    class GsonInstanceTests {

        @Test
        @DisplayName("GSON should be a non-null Gson instance")
        void gsonShouldBeNonNull() {
            assertNotNull(Constants.GSON);
            assertInstanceOf(Gson.class, Constants.GSON);
        }

        @Test
        @DisplayName("GSON_OBJECT should be a non-null Gson instance")
        void gsonObjectShouldBeNonNull() {
            assertNotNull(Constants.GSON_OBJECT);
            assertInstanceOf(Gson.class, Constants.GSON_OBJECT);
        }

        @Test
        @DisplayName("GSON should produce pretty-printed JSON")
        void gsonShouldProducePrettyPrintedJson() {
            Map<String, String> testMap = Map.of("key", "value");
            String json = Constants.GSON.toJson(testMap);
            assertTrue(json.contains("\n"), "GSON should produce pretty-printed output with newlines");
        }

        @Test
        @DisplayName("GSON_OBJECT should produce compact JSON")
        void gsonObjectShouldProduceCompactJson() {
            Map<String, String> testMap = Map.of("key", "value");
            String json = Constants.GSON_OBJECT.toJson(testMap);
            assertFalse(json.contains("\n"), "GSON_OBJECT should produce compact output without newlines");
        }

        @Test
        @DisplayName("GSON and GSON_OBJECT should be different instances")
        void gsonAndGsonObjectShouldBeDifferentInstances() {
            assertNotSame(Constants.GSON, Constants.GSON_OBJECT);
        }
    }

    @Nested
    @DisplayName("Space-related constants")
    class SpaceRelatedConstants {

        @Test
        @DisplayName("SPACE_ID should have correct value")
        void spaceIdShouldHaveCorrectValue() {
            assertEquals("Space Id", Constants.SPACE_ID);
        }

        @Test
        @DisplayName("SPACE_NAME should have correct value")
        void spaceNameShouldHaveCorrectValue() {
            assertEquals("Space Name", Constants.SPACE_NAME);
        }

        @Test
        @DisplayName("SPACE_KEY should have correct value")
        void spaceKeyShouldHaveCorrectValue() {
            assertEquals("Space Key", Constants.SPACE_KEY);
        }

        @Test
        @DisplayName("SPACES should have correct value")
        void spacesShouldHaveCorrectValue() {
            assertEquals("Spaces", Constants.SPACES);
        }

        @Test
        @DisplayName("SPACE_IDENTIFIERS should have correct value")
        void spaceIdentifiersShouldHaveCorrectValue() {
            assertEquals("Space Identifiers", Constants.SPACE_IDENTIFIERS);
        }

        @Test
        @DisplayName("FAILED_TO_GET_SPACE_BY_ID should have correct value")
        void failedToGetSpaceByIdShouldHaveCorrectValue() {
            assertEquals("Failed to get space by id ", Constants.FAILED_TO_GET_SPACE_BY_ID);
        }

        @Test
        @DisplayName("FAILED_TO_GET_SPACES should have correct value")
        void failedToGetSpacesShouldHaveCorrectValue() {
            assertEquals("Failed to get spaces ", Constants.FAILED_TO_GET_SPACES);
        }

        @Test
        @DisplayName("FAILED_TO_GET_SPACE_BY_ID_VALIDATION should have correct value")
        void failedToGetSpaceByIdValidationShouldHaveCorrectValue() {
            assertEquals("Failed to get space by id - validation error ", Constants.FAILED_TO_GET_SPACE_BY_ID_VALIDATION);
        }

        @Test
        @DisplayName("FAILED_TO_GET_SPACES_VALIDATION should have correct value")
        void failedToGetSpacesValidationShouldHaveCorrectValue() {
            assertEquals("Failed to get spaces - validation error ", Constants.FAILED_TO_GET_SPACES_VALIDATION);
        }

        @Test
        @DisplayName("Space re-enter handler constants should have correct values")
        void spaceReenterHandlersShouldHaveCorrectValues() {
            assertEquals("handleReenterGetSpaceById", Constants.HANDLE_REENTER_GET_SPACE_BY_ID);
            assertEquals("confirmReenterGetSpaceById", Constants.CONFIRM_REENTER_GET_SPACE_BY_ID);
            assertEquals("handleReenterGetSpaces", Constants.HANDLE_REENTER_GET_SPACES);
            assertEquals("confirmReenterGetSpaces", Constants.CONFIRM_REENTER_GET_SPACES);
            assertEquals("handleReenterGetDirectChildPagesOfSpace", Constants.HANDLE_REENTER_GET_DIRECT_CHILD_PAGES_OF_SPACE);
            assertEquals("confirmReenterGetDirectChildPagesOfSpace", Constants.CONFIRM_REENTER_GET_DIRECT_CHILD_PAGES_OF_SPACE);
        }
    }

    @Nested
    @DisplayName("Page-related constants")
    class PageRelatedConstants {

        @Test
        @DisplayName("PAGES should have correct value")
        void pagesShouldHaveCorrectValue() {
            assertEquals("Pages", Constants.PAGES);
        }

        @Test
        @DisplayName("PAGE_IDENTIFIERS should have correct value")
        void pageIdentifiersShouldHaveCorrectValue() {
            assertEquals("Page Identifiers", Constants.PAGE_IDENTIFIERS);
        }

        @Test
        @DisplayName("PAGE_ID should have correct value")
        void pageIdShouldHaveCorrectValue() {
            assertEquals("Page ID", Constants.PAGE_ID);
        }

        @Test
        @DisplayName("Failed to get pages in space constants should have correct values")
        void failedToGetPagesInSpaceConstantsShouldHaveCorrectValues() {
            assertEquals("Failed to get pages in space ", Constants.FAILED_TO_GET_PAGES_IN_SPACE);
            assertEquals("Failed to get pages in space - validation error ", Constants.FAILED_TO_GET_PAGES_IN_SPACE_VALIDATION);
        }

        @Test
        @DisplayName("Page re-enter handler constants should have correct values")
        void pageReenterHandlersShouldHaveCorrectValues() {
            assertEquals("handleReenterGetPagesInSpace", Constants.HANDLE_REENTER_GET_PAGES_IN_SPACE);
            assertEquals("confirmReenterGetPagesInSpace", Constants.CONFIRM_REENTER_GET_PAGES_IN_SPACE);
            assertEquals("confirmReenterGetPageInSpace", Constants.CONFIRM_REENTER_GET_PAGE_IN_SPACE);
            assertEquals("handleReenterGetPageInSpace", Constants.HANDLE_REENTER_GET_PAGE_IN_SPACE);
        }

        @Test
        @DisplayName("Failed page in space constants should have correct values")
        void failedPageInSpaceConstantsShouldHaveCorrectValues() {
            assertEquals("Failed to get page in space ", Constants.FAILED_TO_GET_PAGE_IN_SPACE);
            assertEquals("Failed to get page in space - validation error ", Constants.FAILED_TO_GET_PAGE_IN_SPACE_VALIDATION);
        }

        @Test
        @DisplayName("Page by ID handler constants should have correct values")
        void pageByIdHandlersShouldHaveCorrectValues() {
            assertEquals("confirmReenterGetPageById", Constants.CONFIRM_REENTER_GET_PAGE_BY_ID);
            assertEquals("handleReenterGetPageById", Constants.HANDLE_REENTER_GET_PAGE_BY_ID);
            assertEquals("Failed to get page by id ", Constants.FAILED_TO_GET_PAGE_BY_ID);
            assertEquals("Failed to get page by id - validation error ", Constants.FAILED_TO_GET_PAGE_BY_ID_VALIDATION);
        }

        @Test
        @DisplayName("Create page from template constants should have correct values")
        void createPageFromTemplateConstantsShouldHaveCorrectValues() {
            assertEquals("confirmReenterCreatePageFromTemplate", Constants.CONFIRM_REENTER_CREATE_PAGE_FROM_TEMPLATE);
            assertEquals("handleReenterCreatePageFromTemplate", Constants.HANDLE_REENTER_CREATE_PAGE_FROM_TEMPLATE);
            assertEquals("Failed to create page from template ", Constants.FAILED_TO_CREATE_PAGE_FROM_TEMPLATE);
            assertEquals("Failed to create page from template - validation error ", Constants.FAILED_TO_CREATE_PAGE_FROM_TEMPLATE_VALIDATION);
        }

        @Test
        @DisplayName("Create page from content constants should have correct values")
        void createPageFromContentConstantsShouldHaveCorrectValues() {
            assertEquals("confirmReenterCreatePageFromContent", Constants.CONFIRM_REENTER_CREATE_PAGE_FROM_CONTENT);
            assertEquals("handleReenterCreatePageFromContent", Constants.HANDLE_REENTER_CREATE_PAGE_FROM_CONTENT);
            assertEquals("Failed to create page from content ", Constants.FAILED_TO_CREATE_PAGE_FROM_CONTENT);
            assertEquals("Failed to create page from content - validation error ", Constants.FAILED_TO_CREATE_PAGE_FROM_CONTENT_VALIDATION);
        }

        @Test
        @DisplayName("Update page by content constants should have correct values")
        void updatePageByContentConstantsShouldHaveCorrectValues() {
            assertEquals("confirmReenterUpdatePageByContent", Constants.CONFIRM_REENTER_UPDATE_PAGE_BY_CONTENT);
            assertEquals("handleReenterUpdatePageByContent", Constants.HANDLE_REENTER_UPDATE_PAGE_BY_CONTENT);
            assertEquals("Failed to update page by content ", Constants.FAILED_TO_UPDATE_PAGE_BY_CONTENT);
            assertEquals("Failed to update page by content - validation error ", Constants.FAILED_TO_UPDATE_PAGE_BY_CONTENT_VALIDATION);
        }

        @Test
        @DisplayName("Update page from template constants should have correct values")
        void updatePageFromTemplateConstantsShouldHaveCorrectValues() {
            assertEquals("confirmReenterUpdatePageFromTemplate", Constants.CONFIRM_REENTER_UPDATE_PAGE_FROM_TEMPLATE);
            assertEquals("handleReenterUpdatePageFromTemplate", Constants.HANDLE_REENTER_UPDATE_PAGE_FROM_TEMPLATE);
            assertEquals("Failed to update page from template ", Constants.FAILED_TO_UPDATE_PAGE_FROM_TEMPLATE);
            assertEquals("Failed to update page from template - validation error ", Constants.FAILED_TO_UPDATE_PAGE_FROM_TEMPLATE_VALIDATION);
        }

        @Test
        @DisplayName("Nested descendants constants should have correct values")
        void nestedDescendantsConstantsShouldHaveCorrectValues() {
            assertEquals("confirmReenterGetAllNestedDescendantsOfPage", Constants.CONFIRM_REENTER_GET_ALL_NESTED_DESCENDANTS_OF_PAGE);
            assertEquals("handleReenterGetAllNestedDescendantsOfPage", Constants.HANDLE_REENTER_GET_ALL_NESTED_DESCENDANTS_OF_PAGE);
            assertEquals("Failed to get all nested descendants of page ", Constants.FAILED_TO_GET_ALL_NESTED_DESCENDANTS_OF_PAGE);
            assertEquals("Failed to get all nested descendants of page - validation error ", Constants.FAILED_TO_GET_ALL_NESTED_DESCENDANTS_OF_PAGE_VALIDATION);
        }

        @Test
        @DisplayName("Direct child pages and check children constants should have correct values")
        void directChildPagesAndCheckChildrenConstantsShouldHaveCorrectValues() {
            assertEquals("handleReenterGetDirectChildPagesOfPage", Constants.HANDLE_REENTER_GET_DIRECT_CHILD_PAGES_OF_PAGE);
            assertEquals("confirmReenterGetDirectChildPagesOfPage", Constants.CONFIRM_REENTER_GET_DIRECT_CHILD_PAGES_OF_PAGE);
            assertEquals("handleReenterCheckPageHasChildren", Constants.HANDLE_REENTER_CHECK_PAGE_HAS_CHILDREN);
            assertEquals("confirmReenterCheckPageHasChildren", Constants.CONFIRM_REENTER_CHECK_PAGE_HAS_CHILDREN);
        }
    }

    @Nested
    @DisplayName("Template-related constants")
    class TemplateRelatedConstants {

        @Test
        @DisplayName("TEMPLATES should have correct value")
        void templatesShouldHaveCorrectValue() {
            assertEquals("Templates", Constants.TEMPLATES);
        }

        @Test
        @DisplayName("TEMPLATE_IDENTIFIERS should have correct value")
        void templateIdentifiersShouldHaveCorrectValue() {
            assertEquals("Template Identifiers", Constants.TEMPLATE_IDENTIFIERS);
        }

        @Test
        @DisplayName("Template by ID constants should have correct values")
        void templateByIdConstantsShouldHaveCorrectValues() {
            assertEquals("Failed to get template by id ", Constants.FAILED_TO_GET_TEMPLATE_BY_ID);
            assertEquals("Failed to get template by id - validation error ", Constants.FAILED_TO_GET_TEMPLATE_BY_ID_VALIDATION);
            assertEquals("handleReenterGetTemplateById", Constants.HANDLE_REENTER_GET_TEMPLATE_BY_ID);
            assertEquals("confirmReenterGetTemplateById", Constants.CONFIRM_REENTER_GET_TEMPLATE_BY_ID);
        }

        @Test
        @DisplayName("Templates by space ID constants should have correct values")
        void templatesBySpaceIdConstantsShouldHaveCorrectValues() {
            assertEquals("Failed to get templates by space id ", Constants.FAILED_TO_GET_TEMPLATES_BY_SPACE_ID);
            assertEquals("Failed to get templates by space id - validation error ", Constants.FAILED_TO_GET_TEMPLATES_BY_SPACE_ID_VALIDATION);
            assertEquals("handleReenterGetTemplatesBySpaceId", Constants.HANDLE_REENTER_GET_TEMPLATES_BY_SPACE_ID);
            assertEquals("confirmReenterGetTemplatesBySpaceId", Constants.CONFIRM_REENTER_GET_TEMPLATES_BY_SPACE_ID);
        }
    }

    @Nested
    @DisplayName("General constants")
    class GeneralConstants {

        @Test
        @DisplayName("VALIDATION_RESULTS should have correct value")
        void validationResultsShouldHaveCorrectValue() {
            assertEquals("Validation Results", Constants.VALIDATION_RESULTS);
        }

        @Test
        @DisplayName("STATE_ID should have correct value")
        void stateIdShouldHaveCorrectValue() {
            assertEquals("stateId", Constants.STATE_ID);
        }

        @Test
        @DisplayName("RE_ENTER should have correct value")
        void reEnterShouldHaveCorrectValue() {
            assertEquals("re-enter", Constants.RE_ENTER);
        }

        @Test
        @DisplayName("CONFLUENCE_DATA should have correct value")
        void confluenceDataShouldHaveCorrectValue() {
            assertEquals("Confluence Data", Constants.CONFLUENCE_DATA);
        }

        @Test
        @DisplayName("CONFLUENCE_UPDATES should have correct value")
        void confluenceUpdatesShouldHaveCorrectValue() {
            assertEquals("Confluence Update", Constants.CONFLUENCE_UPDATES);
        }

        @Test
        @DisplayName("ENABLE_ERROR_HANDLING should have correct value")
        void enableErrorHandlingShouldHaveCorrectValue() {
            assertEquals("Allow Retry", Constants.ENABLE_ERROR_HANDLING);
        }
    }

    @Test
    @DisplayName("Constructor should be private")
    void constructorShouldBePrivate() throws NoSuchMethodException {
        Constructor<Constants> constructor = Constants.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()),
                "Constructor should be private");
    }

    @Test
    @DisplayName("Private constructor should be invocable via reflection")
    void privateConstructorShouldBeInvocableViaReflection() throws Exception {
        Constructor<Constants> constructor = Constants.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        Constants instance = constructor.newInstance();
        assertNotNull(instance);
    }

    @Test
    @DisplayName("Class should be declared final")
    void classShouldBeFinal() {
        assertTrue(Modifier.isFinal(Constants.class.getModifiers()),
                "Constants class should be declared final");
    }
}
