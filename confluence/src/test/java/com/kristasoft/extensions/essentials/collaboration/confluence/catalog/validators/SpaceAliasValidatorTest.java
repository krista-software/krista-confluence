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
class SpaceAliasValidatorTest {

    @Mock
    private ConfluenceService confluenceService;

    private SpaceAliasValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SpaceAliasValidator(confluenceService);
    }

    // ──────────────────────────────────────────────────────────────
    // validate()
    // ──────────────────────────────────────────────────────────────

    @Test
    void validate_NullAlias_ReturnsFalse() {
        assertFalse(validator.validate(null, Map.of()));
        verifyNoInteractions(confluenceService);
    }

    @Test
    void validate_BlankAlias_ReturnsFalse() {
        assertFalse(validator.validate("   ", Map.of()));
        verifyNoInteractions(confluenceService);
    }

    @Test
    void validate_NullService_ReturnsTrue() {
        SpaceAliasValidator nullServiceValidator = new SpaceAliasValidator(null);
        assertTrue(nullServiceValidator.validate("MFS", Map.of()));
    }

    @Test
    void validate_ServiceResolvesAlias_ReturnsTrue() {
        when(confluenceService.getSpaceIdByAlias("MFS")).thenReturn(null);

        assertTrue(validator.validate("MFS", Map.of()));
        verify(confluenceService).getSpaceIdByAlias("MFS");
    }

    @Test
    void validate_ServiceThrows_ReturnsFalse() {
        when(confluenceService.getSpaceIdByAlias("unknown"))
                .thenThrow(new ConfluenceRESTException("No space found"));

        assertFalse(validator.validate("unknown", Map.of()));
        verify(confluenceService).getSpaceIdByAlias("unknown");
    }

    @Test
    void validate_TrimsAlias_BeforeCallingService() {
        when(confluenceService.getSpaceIdByAlias("MFS")).thenReturn(null);

        assertTrue(validator.validate("  MFS  ", Map.of()));
        verify(confluenceService).getSpaceIdByAlias("MFS");
    }

    // ──────────────────────────────────────────────────────────────
    // getFetchFieldName()
    // ──────────────────────────────────────────────────────────────

    @Test
    void getFetchFieldName_ReturnsSpaceAlias() {
        assertEquals("Space Alias", validator.getFetchFieldName());
    }

    // ──────────────────────────────────────────────────────────────
    // getFetchStepMessage()
    // ──────────────────────────────────────────────────────────────

    @Test
    void getFetchStepMessage_ContainsUsageGuidance() {
        String message = validator.getFetchStepMessage();
        assertNotNull(message);
        assertTrue(message.contains("Space Alias"));
        assertTrue(message.contains("currentActiveAlias"));
    }

    // ──────────────────────────────────────────────────────────────
    // getConfirmationStepMessage()
    // ──────────────────────────────────────────────────────────────

    @Test
    void getConfirmationStepMessage_WithAlias_ContainsAlias() {
        String message = validator.getConfirmationStepMessage("~johndoe", Map.of());
        assertNotNull(message);
        assertTrue(message.contains("~johndoe"));
    }

    @Test
    void getConfirmationStepMessage_NullAlias_ContainsNotProvided() {
        String message = validator.getConfirmationStepMessage(null, Map.of());
        assertNotNull(message);
        assertTrue(message.contains("not provided"));
    }

    // ──────────────────────────────────────────────────────────────
    // getErrMessage()
    // ──────────────────────────────────────────────────────────────

    @Test
    void getErrMessage_NullAlias_ReturnsRequiredMessage() {
        String message = validator.getErrMessage(null);
        assertNotNull(message);
        assertTrue(message.contains("required"));
    }

    @Test
    void getErrMessage_EmptyAlias_ReturnsCannotBeEmptyMessage() {
        String message = validator.getErrMessage("");
        assertNotNull(message);
        assertTrue(message.contains("cannot be empty"));
    }

    @Test
    void getErrMessage_BlankAlias_ReturnsCannotBeEmptyMessage() {
        String message = validator.getErrMessage("   ");
        assertNotNull(message);
        assertTrue(message.contains("cannot be empty"));
    }

    @Test
    void getErrMessage_ValidAlias_ContainsAliasInMessage() {
        String message = validator.getErrMessage("unknown-space");
        assertNotNull(message);
        assertTrue(message.contains("unknown-space"));
    }
}
