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

package com.kristasoft.extensions.essentials.collaboration.confluence.catalog.validators;

import com.kristasoft.extensions.essentials.collaboration.confluence.dto.Page;
import com.kristasoft.extensions.essentials.collaboration.confluence.service.ConfluenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PageIdValidator Tests")
class PageIdValidatorTest {

    @Mock
    private ConfluenceService confluenceService;

    private PageIdValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PageIdValidator(confluenceService);
    }

    // ──────────────────────────────────────────────────────────────
    // validate()
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("validate")
    class ValidateTests {

        @Test
        @DisplayName("Should return false for null page ID")
        void shouldReturnFalseForNullPageId() {
            assertFalse(validator.validate(null, Map.of()));
            verifyNoInteractions(confluenceService);
        }

        @Test
        @DisplayName("Should return false for empty page ID")
        void shouldReturnFalseForEmptyPageId() {
            assertFalse(validator.validate("", Map.of()));
            verifyNoInteractions(confluenceService);
        }

        @Test
        @DisplayName("Should return false for blank page ID")
        void shouldReturnFalseForBlankPageId() {
            assertFalse(validator.validate("   ", Map.of()));
            verifyNoInteractions(confluenceService);
        }

        @Test
        @DisplayName("Should return true when page exists")
        void shouldReturnTrueWhenPageExists() {
            Page mockPage = new Page.PageBuilder("12345", "Test Page").build();
            when(confluenceService.getPageById("12345")).thenReturn(mockPage);

            assertTrue(validator.validate("12345", Map.of()));
            verify(confluenceService).getPageById("12345");
        }

        @Test
        @DisplayName("Should return false when page does not exist (returns null)")
        void shouldReturnFalseWhenPageDoesNotExist() {
            when(confluenceService.getPageById("99999")).thenReturn(null);

            assertFalse(validator.validate("99999", Map.of()));
            verify(confluenceService).getPageById("99999");
        }

        @Test
        @DisplayName("Should trim page ID before validation")
        void shouldTrimPageIdBeforeValidation() {
            Page mockPage = new Page.PageBuilder("12345", "Test Page").build();
            when(confluenceService.getPageById("12345")).thenReturn(mockPage);

            assertTrue(validator.validate("  12345  ", Map.of()));
            verify(confluenceService).getPageById("12345");
        }

        @Test
        @DisplayName("Should return false when service throws exception")
        void shouldReturnFalseWhenServiceThrowsException() {
            when(confluenceService.getPageById("error"))
                    .thenThrow(new RuntimeException("API error"));

            assertFalse(validator.validate("error", Map.of()));
            verify(confluenceService).getPageById("error");
        }
    }

    // ──────────────────────────────────────────────────────────────
    // getFetchFieldName()
    // ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getFetchFieldName should return 'Page ID'")
    void getFetchFieldNameShouldReturnPageId() {
        assertEquals("Page ID", validator.getFetchFieldName());
    }

    // ──────────────────────────────────────────────────────────────
    // getFetchStepMessage()
    // ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getFetchStepMessage should return non-empty string")
    void getFetchStepMessageShouldReturnNonEmptyString() {
        String message = validator.getFetchStepMessage();
        assertNotNull(message);
        assertFalse(message.isEmpty(), "Fetch step message should not be empty");
    }

    @Test
    @DisplayName("getFetchStepMessage should contain guidance about Page ID")
    void getFetchStepMessageShouldContainPageIdGuidance() {
        String message = validator.getFetchStepMessage();
        assertTrue(message.contains("Page ID"),
                "Fetch step message should mention Page ID");
    }

    // ──────────────────────────────────────────────────────────────
    // getConfirmationStepMessage()
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getConfirmationStepMessage")
    class GetConfirmationStepMessageTests {

        @Test
        @DisplayName("Should include page ID when provided")
        void shouldIncludePageIdWhenProvided() {
            String message = validator.getConfirmationStepMessage("12345", Map.of());
            assertNotNull(message);
            assertTrue(message.contains("12345"),
                    "Confirmation message should include the page ID");
        }

        @Test
        @DisplayName("Should handle null page ID")
        void shouldHandleNullPageId() {
            String message = validator.getConfirmationStepMessage(null, Map.of());
            assertNotNull(message);
            assertFalse(message.isEmpty(),
                    "Should return a message even for null page ID");
        }

        @Test
        @DisplayName("Should handle empty page ID")
        void shouldHandleEmptyPageId() {
            String message = validator.getConfirmationStepMessage("", Map.of());
            assertNotNull(message);
            assertFalse(message.isEmpty(),
                    "Should return a message even for empty page ID");
        }
    }

    // ──────────────────────────────────────────────────────────────
    // getErrMessage()
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getErrMessage")
    class GetErrMessageTests {

        @Test
        @DisplayName("Should return required message for null page ID")
        void shouldReturnRequiredMessageForNullPageId() {
            String message = validator.getErrMessage(null);
            assertNotNull(message);
            assertTrue(message.toLowerCase().contains("required"),
                    "Error message for null should mention 'required'");
        }

        @Test
        @DisplayName("Should include page ID in error message when provided")
        void shouldIncludePageIdInErrorMessage() {
            String message = validator.getErrMessage("BADPAGE");
            assertNotNull(message);
            assertTrue(message.contains("BADPAGE"),
                    "Error message should include the invalid page ID");
        }

        @Test
        @DisplayName("Should return non-empty error message for empty page ID")
        void shouldReturnNonEmptyErrorForEmptyPageId() {
            String message = validator.getErrMessage("");
            assertNotNull(message);
            assertFalse(message.isEmpty());
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Interface compliance
    // ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should implement Validator interface")
    void shouldImplementValidatorInterface() {
        assertTrue(Validator.class.isAssignableFrom(PageIdValidator.class),
                "PageIdValidator should implement Validator interface");
    }
}
