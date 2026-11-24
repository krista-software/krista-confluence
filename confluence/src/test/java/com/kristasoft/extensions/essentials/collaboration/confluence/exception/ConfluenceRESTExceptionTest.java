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

package com.kristasoft.extensions.essentials.collaboration.confluence.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for the ConfluenceRESTException class.
 */
@DisplayName("ConfluenceRESTException Tests")
class ConfluenceRESTExceptionTest {

    @Nested
    @DisplayName("Constructor with message only")
    class MessageConstructor {

        @Test
        @DisplayName("Should create exception with message")
        void shouldCreateExceptionWithMessage() {
            String message = "Test error message";
            ConfluenceRESTException exception = new ConfluenceRESTException(message);

            assertEquals(message, exception.getMessage());
        }

        @Test
        @DisplayName("Should have null cause when created with message only")
        void shouldHaveNullCauseWhenCreatedWithMessageOnly() {
            ConfluenceRESTException exception = new ConfluenceRESTException("Error");

            assertNull(exception.getCause());
        }

        @Test
        @DisplayName("Should create exception with null message")
        void shouldCreateExceptionWithNullMessage() {
            ConfluenceRESTException exception = new ConfluenceRESTException((String) null);

            assertNull(exception.getMessage());
        }

        @Test
        @DisplayName("Should create exception with empty message")
        void shouldCreateExceptionWithEmptyMessage() {
            ConfluenceRESTException exception = new ConfluenceRESTException("");

            assertEquals("", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Constructor with message and cause")
    class MessageAndCauseConstructor {

        @Test
        @DisplayName("Should create exception with message and cause")
        void shouldCreateExceptionWithMessageAndCause() {
            String message = "REST call failed";
            Throwable cause = new RuntimeException("Connection refused");
            ConfluenceRESTException exception = new ConfluenceRESTException(message, cause);

            assertEquals(message, exception.getMessage());
            assertEquals(cause, exception.getCause());
        }

        @Test
        @DisplayName("Should preserve cause message")
        void shouldPreserveCauseMessage() {
            String causeMessage = "Underlying IO error";
            Throwable cause = new java.io.IOException(causeMessage);
            ConfluenceRESTException exception = new ConfluenceRESTException("Wrapper message", cause);

            assertEquals(causeMessage, exception.getCause().getMessage());
        }

        @Test
        @DisplayName("Should create exception with null cause")
        void shouldCreateExceptionWithNullCause() {
            ConfluenceRESTException exception = new ConfluenceRESTException("Error", null);

            assertEquals("Error", exception.getMessage());
            assertNull(exception.getCause());
        }

        @Test
        @DisplayName("Should create exception with both null message and cause")
        void shouldCreateExceptionWithBothNullMessageAndCause() {
            ConfluenceRESTException exception = new ConfluenceRESTException(null, null);

            assertNull(exception.getMessage());
            assertNull(exception.getCause());
        }
    }

    @Nested
    @DisplayName("Constructor with cause only")
    class CauseOnlyConstructor {

        @Test
        @DisplayName("Should create exception with cause")
        void shouldCreateExceptionWithCause() {
            Throwable cause = new IllegalArgumentException("Invalid argument");
            ConfluenceRESTException exception = new ConfluenceRESTException(cause);

            assertEquals(cause, exception.getCause());
        }

        @Test
        @DisplayName("Should derive message from cause")
        void shouldDeriveMessageFromCause() {
            Throwable cause = new IllegalStateException("Bad state");
            ConfluenceRESTException exception = new ConfluenceRESTException(cause);

            assertNotNull(exception.getMessage());
            assertTrue(exception.getMessage().contains("Bad state"));
        }

        @Test
        @DisplayName("Should create exception with null cause")
        void shouldCreateExceptionWithNullCause() {
            ConfluenceRESTException exception = new ConfluenceRESTException((Throwable) null);

            assertNull(exception.getCause());
            assertNull(exception.getMessage());
        }
    }

    @Test
    @DisplayName("Should extend RuntimeException")
    void shouldExtendRuntimeException() {
        ConfluenceRESTException exception = new ConfluenceRESTException("Test");

        assertInstanceOf(RuntimeException.class, exception);
    }

    @Test
    @DisplayName("Should be throwable and catchable")
    void shouldBeThrowableAndCatchable() {
        String errorMessage = "API call failed";

        ConfluenceRESTException caught = assertThrows(
                ConfluenceRESTException.class,
                () -> { throw new ConfluenceRESTException(errorMessage); }
        );

        assertEquals(errorMessage, caught.getMessage());
    }

    @Test
    @DisplayName("Should support exception chaining")
    void shouldSupportExceptionChaining() {
        Exception root = new Exception("Root cause");
        ConfluenceRESTException middle = new ConfluenceRESTException("Middle", root);
        ConfluenceRESTException outer = new ConfluenceRESTException("Outer", middle);

        assertEquals(middle, outer.getCause());
        assertEquals(root, outer.getCause().getCause());
    }

    @Test
    @DisplayName("Should have stack trace")
    void shouldHaveStackTrace() {
        ConfluenceRESTException exception = new ConfluenceRESTException("Test");

        assertNotNull(exception.getStackTrace());
        assertTrue(exception.getStackTrace().length > 0);
    }
}
