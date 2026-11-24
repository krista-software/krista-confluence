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

import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.validators.Validator.ValidationResource;
import com.kristasoft.extensions.essentials.collaboration.confluence.service.ConfluenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SpaceNameValidator Tests")
class SpaceNameValidatorTest {

    @Mock
    private ConfluenceService confluenceService;

    private SpaceNameValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SpaceNameValidator(confluenceService);
    }

    @Nested
    @DisplayName("validate()")
    class ValidateTests {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("Should return false for null, empty, or blank space names")
        void shouldReturnFalseForNullEmptyOrBlankNames(String spaceName) {
            assertFalse(validator.validate(spaceName, Collections.emptyMap()));
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "Engineering Documentation",
                "My Space",
                "A",
                "Test-Space_123",
                "Space with special chars: @#$%",
                "Unicode: \u00e9\u00e8\u00ea"
        })
        @DisplayName("Should return true for valid non-empty space names")
        void shouldReturnTrueForValidSpaceNames(String spaceName) {
            assertTrue(validator.validate(spaceName, Collections.emptyMap()));
        }

        @Test
        @DisplayName("Should return true for space name at maximum length boundary (255 chars)")
        void shouldReturnTrueForMaxLengthName() {
            String maxLengthName = "A".repeat(255);
            assertTrue(validator.validate(maxLengthName, Collections.emptyMap()));
        }

        @Test
        @DisplayName("Should return false for space name exceeding maximum length (256 chars)")
        void shouldReturnFalseForNameExceedingMaxLength() {
            String tooLongName = "A".repeat(256);
            assertFalse(validator.validate(tooLongName, Collections.emptyMap()));
        }

        @Test
        @DisplayName("Should return true for single character space name")
        void shouldReturnTrueForSingleCharName() {
            assertTrue(validator.validate("X", Collections.emptyMap()));
        }

        @Test
        @DisplayName("Should trim the space name before validation")
        void shouldTrimSpaceNameBeforeValidation() {
            assertTrue(validator.validate("  Engineering Docs  ", Collections.emptyMap()));
        }

        @Test
        @DisplayName("Should return true when ConfluenceService is null")
        void shouldReturnTrueWhenServiceIsNull() {
            SpaceNameValidator validatorWithoutService = new SpaceNameValidator(null);
            assertTrue(validatorWithoutService.validate("Valid Name", Collections.emptyMap()));
        }

        @Test
        @DisplayName("Should return true when ConfluenceService.getSpaces() succeeds")
        void shouldReturnTrueWhenServiceCallSucceeds() {
            when(confluenceService.getSpaces()).thenReturn(new com.kristasoft.extensions.essentials.collaboration.confluence.dto.Space[0]);
            assertTrue(validator.validate("Engineering", Collections.emptyMap()));
            verify(confluenceService).getSpaces();
        }

        @Test
        @DisplayName("Should return true even when ConfluenceService.getSpaces() throws exception")
        void shouldReturnTrueWhenServiceCallFails() {
            when(confluenceService.getSpaces()).thenThrow(new RuntimeException("Service unavailable"));
            assertTrue(validator.validate("Engineering", Collections.emptyMap()));
        }

        @Test
        @DisplayName("Should accept context map parameter without affecting validation")
        void shouldAcceptContextMapWithoutAffectingValidation() {
            Map<ValidationResource, String> context = Map.of(
                    ValidationResource.SPACE_ID, "12345"
            );
            assertTrue(validator.validate("Engineering", context));
        }
    }

    @Nested
    @DisplayName("getFetchFieldName()")
    class GetFetchFieldNameTests {

        @Test
        @DisplayName("Should return 'Space Name'")
        void shouldReturnSpaceName() {
            assertEquals("Space Name", validator.getFetchFieldName());
        }
    }

    @Nested
    @DisplayName("getFetchStepMessage()")
    class GetFetchStepMessageTests {

        @Test
        @DisplayName("Should return a non-null, non-empty message")
        void shouldReturnNonEmptyMessage() {
            String message = validator.getFetchStepMessage();
            assertNotNull(message);
            assertFalse(message.isEmpty());
        }

        @Test
        @DisplayName("Should mention space name in the message")
        void shouldMentionSpaceNameInMessage() {
            assertTrue(validator.getFetchStepMessage().toLowerCase().contains("space"));
        }
    }

    @Nested
    @DisplayName("getConfirmationStepMessage()")
    class GetConfirmationStepMessageTests {

        @Test
        @DisplayName("Should include the provided space name in the message")
        void shouldIncludeProvidedSpaceName() {
            String message = validator.getConfirmationStepMessage("My Space", Collections.emptyMap());
            assertTrue(message.contains("My Space"));
        }

        @Test
        @DisplayName("Should handle null space name gracefully")
        void shouldHandleNullSpaceName() {
            String message = validator.getConfirmationStepMessage(null, Collections.emptyMap());
            assertNotNull(message);
            assertTrue(message.contains("not provided"));
        }
    }

    @Nested
    @DisplayName("getErrMessage()")
    class GetErrMessageTests {

        @Test
        @DisplayName("Should return appropriate error for null space name")
        void shouldReturnErrorForNullName() {
            String message = validator.getErrMessage(null);
            assertNotNull(message);
            assertTrue(message.contains("required"));
        }

        @Test
        @DisplayName("Should return appropriate error for empty space name")
        void shouldReturnErrorForEmptyName() {
            String message = validator.getErrMessage("");
            assertNotNull(message);
            assertTrue(message.contains("cannot be empty"));
        }

        @Test
        @DisplayName("Should return appropriate error for blank space name")
        void shouldReturnErrorForBlankName() {
            String message = validator.getErrMessage("   ");
            assertNotNull(message);
            assertTrue(message.contains("cannot be empty"));
        }

        @Test
        @DisplayName("Should return appropriate error for space name exceeding max length")
        void shouldReturnErrorForTooLongName() {
            String tooLong = "A".repeat(256);
            String message = validator.getErrMessage(tooLong);
            assertNotNull(message);
            assertTrue(message.contains("too long"));
            assertTrue(message.contains("255"));
        }

        @Test
        @DisplayName("Should return generic error for otherwise invalid name")
        void shouldReturnGenericErrorForOtherwiseInvalidName() {
            String message = validator.getErrMessage("Some Valid Looking Name");
            assertNotNull(message);
            assertTrue(message.contains("invalid"));
        }
    }
}
