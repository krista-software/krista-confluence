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
@DisplayName("SpaceKeyValidator Tests")
class SpaceKeyValidatorTest {

    @Mock
    private ConfluenceService confluenceService;

    private SpaceKeyValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SpaceKeyValidator(confluenceService);
    }

    @Nested
    @DisplayName("validate()")
    class ValidateTests {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("Should return false for null, empty, or blank space keys")
        void shouldReturnFalseForNullEmptyOrBlankKeys(String spaceKey) {
            assertFalse(validator.validate(spaceKey, Collections.emptyMap()));
        }

        @ParameterizedTest
        @ValueSource(strings = {"ENG", "DOCS", "my-space", "space_1", "A", "abc123", "ABC-DEF_123"})
        @DisplayName("Should return true for valid space keys matching the pattern")
        void shouldReturnTrueForValidSpaceKeys(String spaceKey) {
            assertTrue(validator.validate(spaceKey, Collections.emptyMap()));
        }

        @ParameterizedTest
        @ValueSource(strings = {"space key", "my.space", "space@name", "space!key", "space#1", "key/path", "space\\key"})
        @DisplayName("Should return false for space keys with invalid characters")
        void shouldReturnFalseForKeysWithInvalidCharacters(String spaceKey) {
            assertFalse(validator.validate(spaceKey, Collections.emptyMap()));
        }

        @Test
        @DisplayName("Should return true for space key at maximum length boundary (255 chars)")
        void shouldReturnTrueForMaxLengthKey() {
            String maxLengthKey = "A".repeat(255);
            assertTrue(validator.validate(maxLengthKey, Collections.emptyMap()));
        }

        @Test
        @DisplayName("Should return false for space key exceeding maximum length (256 chars)")
        void shouldReturnFalseForKeyExceedingMaxLength() {
            String tooLongKey = "A".repeat(256);
            assertFalse(validator.validate(tooLongKey, Collections.emptyMap()));
        }

        @Test
        @DisplayName("Should return true for single character space key (minimum length)")
        void shouldReturnTrueForSingleCharKey() {
            assertTrue(validator.validate("X", Collections.emptyMap()));
        }

        @Test
        @DisplayName("Should trim the space key before validation")
        void shouldTrimSpaceKeyBeforeValidation() {
            assertTrue(validator.validate("  ENG  ", Collections.emptyMap()));
        }

        @Test
        @DisplayName("Should return true when ConfluenceService is null")
        void shouldReturnTrueWhenServiceIsNull() {
            SpaceKeyValidator validatorWithoutService = new SpaceKeyValidator(null);
            assertTrue(validatorWithoutService.validate("VALID", Collections.emptyMap()));
        }

        @Test
        @DisplayName("Should return true when ConfluenceService.getSpaces() succeeds")
        void shouldReturnTrueWhenServiceCallSucceeds() {
            when(confluenceService.getSpaces()).thenReturn(new com.kristasoft.extensions.essentials.collaboration.confluence.dto.Space[0]);
            assertTrue(validator.validate("ENG", Collections.emptyMap()));
            verify(confluenceService).getSpaces();
        }

        @Test
        @DisplayName("Should return true even when ConfluenceService.getSpaces() throws exception")
        void shouldReturnTrueWhenServiceCallFails() {
            when(confluenceService.getSpaces()).thenThrow(new RuntimeException("Service unavailable"));
            assertTrue(validator.validate("ENG", Collections.emptyMap()));
        }

        @Test
        @DisplayName("Should accept context map parameter without affecting validation")
        void shouldAcceptContextMapWithoutAffectingValidation() {
            Map<ValidationResource, String> context = Map.of(
                    ValidationResource.SPACE_ID, "12345"
            );
            assertTrue(validator.validate("ENG", context));
        }
    }

    @Nested
    @DisplayName("getFetchFieldName()")
    class GetFetchFieldNameTests {

        @Test
        @DisplayName("Should return 'Space Key'")
        void shouldReturnSpaceKey() {
            assertEquals("Space Key", validator.getFetchFieldName());
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
        @DisplayName("Should mention Space Key in the message")
        void shouldMentionSpaceKeyInMessage() {
            assertTrue(validator.getFetchStepMessage().contains("Space Key"));
        }
    }

    @Nested
    @DisplayName("getConfirmationStepMessage()")
    class GetConfirmationStepMessageTests {

        @Test
        @DisplayName("Should include the provided space key in the message")
        void shouldIncludeProvidedSpaceKey() {
            String message = validator.getConfirmationStepMessage("MYKEY", Collections.emptyMap());
            assertTrue(message.contains("MYKEY"));
        }

        @Test
        @DisplayName("Should handle null space key gracefully")
        void shouldHandleNullSpaceKey() {
            String message = validator.getConfirmationStepMessage(null, Collections.emptyMap());
            assertNotNull(message);
            assertTrue(message.contains("not provided"));
        }
    }

    @Nested
    @DisplayName("getErrMessage()")
    class GetErrMessageTests {

        @Test
        @DisplayName("Should return appropriate error for null space key")
        void shouldReturnErrorForNullKey() {
            String message = validator.getErrMessage(null);
            assertNotNull(message);
            assertTrue(message.contains("required"));
        }

        @Test
        @DisplayName("Should return appropriate error for empty space key")
        void shouldReturnErrorForEmptyKey() {
            String message = validator.getErrMessage("");
            assertNotNull(message);
            assertTrue(message.contains("cannot be empty"));
        }

        @Test
        @DisplayName("Should return appropriate error for space key with only whitespace")
        void shouldReturnErrorForWhitespaceKey() {
            String message = validator.getErrMessage("   ");
            assertNotNull(message);
            assertTrue(message.contains("cannot be empty"));
        }

        @Test
        @DisplayName("Should return appropriate error for space key exceeding max length")
        void shouldReturnErrorForTooLongKey() {
            String tooLong = "A".repeat(256);
            String message = validator.getErrMessage(tooLong);
            assertNotNull(message);
            assertTrue(message.contains("too long"));
            assertTrue(message.contains("255"));
        }

        @Test
        @DisplayName("Should return appropriate error for space key with invalid characters")
        void shouldReturnErrorForInvalidCharacters() {
            String message = validator.getErrMessage("my space!");
            assertNotNull(message);
            assertTrue(message.contains("invalid characters"));
        }

        @Test
        @DisplayName("Should return generic error for valid format but otherwise invalid key")
        void shouldReturnGenericErrorForOtherwiseInvalidKey() {
            // A key that passes all format checks but somehow is still invalid
            String message = validator.getErrMessage("VALIDFORMAT");
            assertNotNull(message);
            assertTrue(message.contains("invalid"));
        }
    }
}
