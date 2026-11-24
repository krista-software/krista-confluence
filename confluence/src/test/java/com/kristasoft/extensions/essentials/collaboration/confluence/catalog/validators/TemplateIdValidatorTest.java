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

import com.kristasoft.extensions.essentials.collaboration.confluence.dto.Template;
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
@DisplayName("TemplateIdValidator Tests")
class TemplateIdValidatorTest {

    @Mock
    private ConfluenceService confluenceService;

    private TemplateIdValidator validator;

    @BeforeEach
    void setUp() {
        validator = new TemplateIdValidator(confluenceService);
    }

    // ──────────────────────────────────────────────────────────────
    // validate()
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("validate")
    class ValidateTests {

        @Test
        @DisplayName("Should return false for null template ID")
        void shouldReturnFalseForNullTemplateId() {
            assertFalse(validator.validate(null, Map.of()));
            verifyNoInteractions(confluenceService);
        }

        @Test
        @DisplayName("Should return false for empty template ID")
        void shouldReturnFalseForEmptyTemplateId() {
            assertFalse(validator.validate("", Map.of()));
            verifyNoInteractions(confluenceService);
        }

        @Test
        @DisplayName("Should return false for blank template ID")
        void shouldReturnFalseForBlankTemplateId() {
            assertFalse(validator.validate("   ", Map.of()));
            verifyNoInteractions(confluenceService);
        }

        @Test
        @DisplayName("Should return true when template exists")
        void shouldReturnTrueWhenTemplateExists() {
            Template mockTemplate = mock(Template.class);
            when(confluenceService.getTemplateById("tmpl-001")).thenReturn(mockTemplate);

            assertTrue(validator.validate("tmpl-001", Map.of()));
            verify(confluenceService).getTemplateById("tmpl-001");
        }

        @Test
        @DisplayName("Should return false when template does not exist (returns null)")
        void shouldReturnFalseWhenTemplateDoesNotExist() {
            when(confluenceService.getTemplateById("nonexistent")).thenReturn(null);

            assertFalse(validator.validate("nonexistent", Map.of()));
            verify(confluenceService).getTemplateById("nonexistent");
        }

        @Test
        @DisplayName("Should trim template ID before validation")
        void shouldTrimTemplateIdBeforeValidation() {
            Template mockTemplate = mock(Template.class);
            when(confluenceService.getTemplateById("tmpl-001")).thenReturn(mockTemplate);

            assertTrue(validator.validate("  tmpl-001  ", Map.of()));
            verify(confluenceService).getTemplateById("tmpl-001");
        }

        @Test
        @DisplayName("Should return false when service throws exception")
        void shouldReturnFalseWhenServiceThrowsException() {
            when(confluenceService.getTemplateById("error"))
                    .thenThrow(new RuntimeException("API error"));

            assertFalse(validator.validate("error", Map.of()));
            verify(confluenceService).getTemplateById("error");
        }
    }

    // ──────────────────────────────────────────────────────────────
    // getFetchFieldName()
    // ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getFetchFieldName should return 'Template ID'")
    void getFetchFieldNameShouldReturnTemplateId() {
        assertEquals("Template ID", validator.getFetchFieldName());
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
    @DisplayName("getFetchStepMessage should contain guidance about Template ID")
    void getFetchStepMessageShouldContainTemplateIdGuidance() {
        String message = validator.getFetchStepMessage();
        assertTrue(message.contains("Template ID"),
                "Fetch step message should mention Template ID");
    }

    // ──────────────────────────────────────────────────────────────
    // getConfirmationStepMessage()
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getConfirmationStepMessage")
    class GetConfirmationStepMessageTests {

        @Test
        @DisplayName("Should include template ID when provided")
        void shouldIncludeTemplateIdWhenProvided() {
            String message = validator.getConfirmationStepMessage("tmpl-abc", Map.of());
            assertNotNull(message);
            assertTrue(message.contains("tmpl-abc"),
                    "Confirmation message should include the template ID");
        }

        @Test
        @DisplayName("Should handle null template ID")
        void shouldHandleNullTemplateId() {
            String message = validator.getConfirmationStepMessage(null, Map.of());
            assertNotNull(message);
            assertFalse(message.isEmpty(),
                    "Should return a message even for null template ID");
        }

        @Test
        @DisplayName("Should handle empty template ID")
        void shouldHandleEmptyTemplateId() {
            String message = validator.getConfirmationStepMessage("", Map.of());
            assertNotNull(message);
            assertFalse(message.isEmpty(),
                    "Should return a message even for empty template ID");
        }
    }

    // ──────────────────────────────────────────────────────────────
    // getErrMessage()
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getErrMessage")
    class GetErrMessageTests {

        @Test
        @DisplayName("Should return required message for null template ID")
        void shouldReturnRequiredMessageForNullTemplateId() {
            String message = validator.getErrMessage(null);
            assertNotNull(message);
            assertTrue(message.toLowerCase().contains("required"),
                    "Error message for null should mention 'required'");
        }

        @Test
        @DisplayName("Should return cannot be empty message for empty template ID")
        void shouldReturnCannotBeEmptyMessageForEmptyTemplateId() {
            String message = validator.getErrMessage("");
            assertNotNull(message);
            assertTrue(message.toLowerCase().contains("cannot be empty"),
                    "Error message for empty string should mention 'cannot be empty'");
        }

        @Test
        @DisplayName("Should include template ID in error message when provided")
        void shouldIncludeTemplateIdInErrorMessage() {
            String message = validator.getErrMessage("BADTEMPLATE");
            assertNotNull(message);
            assertTrue(message.contains("BADTEMPLATE"),
                    "Error message should include the invalid template ID");
        }

        @Test
        @DisplayName("Should mention 'not found' for valid-format but nonexistent template ID")
        void shouldMentionNotFoundForNonexistentTemplateId() {
            String message = validator.getErrMessage("tmpl-xyz");
            assertNotNull(message);
            assertTrue(message.toLowerCase().contains("not found"),
                    "Error message should mention 'not found'");
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Interface compliance
    // ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should implement Validator interface")
    void shouldImplementValidatorInterface() {
        assertTrue(Validator.class.isAssignableFrom(TemplateIdValidator.class),
                "TemplateIdValidator should implement Validator interface");
    }
}
