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

import com.kristasoft.extensions.essentials.collaboration.confluence.exception.ConfluenceRESTException;
import com.kristasoft.extensions.essentials.collaboration.confluence.service.ConfluenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpaceIdValidatorTest {

    @Mock
    private ConfluenceService confluenceService;

    private SpaceIdValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SpaceIdValidator(confluenceService);
    }

    // ──────────────────────────────────────────────────────────────
    // validate()
    // Note: the inner `if (trimmedId.length() == 0)` branch is dead
    // code — it can never be true because trim().isEmpty() is checked
    // first. Its `return false` cannot be reached by any test.
    // ──────────────────────────────────────────────────────────────

    @Test
    void validate_NullSpaceId_ReturnsFalse() {
        assertFalse(validator.validate(null, Map.of()));
        verifyNoInteractions(confluenceService);
    }

    @Test
    void validate_EmptySpaceId_ReturnsFalse() {
        assertFalse(validator.validate("", Map.of()));
        verifyNoInteractions(confluenceService);
    }

    @Test
    void validate_BlankSpaceId_ReturnsFalse() {
        assertFalse(validator.validate("   ", Map.of()));
        verifyNoInteractions(confluenceService);
    }

    @Test
    void validate_ServiceResolvesSpaceId_ReturnsTrue() {
        when(confluenceService.getSpaceById("12345")).thenReturn(null);

        assertTrue(validator.validate("12345", Map.of()));
        verify(confluenceService).getSpaceById("12345");
    }

    @Test
    void validate_TrimsSpaceId_BeforeCallingService() {
        when(confluenceService.getSpaceById("12345")).thenReturn(null);

        assertTrue(validator.validate("  12345  ", Map.of()));
        verify(confluenceService).getSpaceById("12345");
    }

    @Test
    void validate_ServiceThrows_ReturnsFalse() {
        when(confluenceService.getSpaceById("unknown"))
                .thenThrow(new ConfluenceRESTException("Space not found"));

        assertFalse(validator.validate("unknown", Map.of()));
        verify(confluenceService).getSpaceById("unknown");
    }

    @Test
    void validate_NullService_ReturnsTrue() {
        SpaceIdValidator nullServiceValidator = new SpaceIdValidator(null);
        assertTrue(nullServiceValidator.validate("LEG", Map.of()));
    }

    // ──────────────────────────────────────────────────────────────
    // getFetchFieldName()
    // ──────────────────────────────────────────────────────────────

    @Test
    void getFetchFieldName_ReturnsSpaceIdOrKey() {
        assertEquals("Space Id", validator.getFetchFieldName());
    }

    // ──────────────────────────────────────────────────────────────
    // getFetchStepMessage()
    // ──────────────────────────────────────────────────────────────

    @Test
    void getFetchStepMessage_ContainsUsageGuidance() {
        String message = validator.getFetchStepMessage();
        assertNotNull(message);
        assertTrue(message.contains("Space ID"));
        assertTrue(message.contains("Get Spaces"));
    }

    // ──────────────────────────────────────────────────────────────
    // getConfirmationStepMessage()
    // ──────────────────────────────────────────────────────────────

    @Test
    void getConfirmationStepMessage_WithSpaceId_ContainsSpaceId() {
        String message = validator.getConfirmationStepMessage("LEG", Map.of());
        assertNotNull(message);
        assertTrue(message.contains("LEG"));
    }

    @Test
    void getConfirmationStepMessage_NullSpaceId_ContainsNotProvided() {
        String message = validator.getConfirmationStepMessage(null, Map.of());
        assertNotNull(message);
        assertTrue(message.contains("not provided"));
    }

    // ──────────────────────────────────────────────────────────────
    // getErrMessage()
    // ──────────────────────────────────────────────────────────────

    @Test
    void getErrMessage_NullSpaceId_ReturnsRequiredMessage() {
        String message = validator.getErrMessage(null);
        assertNotNull(message);
        assertTrue(message.contains("required"));
    }

    @Test
    void getErrMessage_EmptySpaceId_ReturnsCannotBeEmptyMessage() {
        String message = validator.getErrMessage("");
        assertNotNull(message);
        assertTrue(message.contains("cannot be empty"));
    }

    @Test
    void getErrMessage_BlankSpaceId_ReturnsCannotBeEmptyMessage() {
        String message = validator.getErrMessage("   ");
        assertNotNull(message);
        assertTrue(message.contains("cannot be empty"));
    }

    @Test
    void getErrMessage_ValidSpaceId_ContainsSpaceIdInMessage() {
        String message = validator.getErrMessage("MISSING-SPACE");
        assertNotNull(message);
        assertTrue(message.contains("MISSING-SPACE"));
    }
}
