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

package com.kristasoft.extensions.essentials.collaboration.confluence.catalog.extresp;

import app.krista.extension.executor.ExtensionResponse;
import app.krista.extension.executor.RemediationAction;
import app.krista.extension.executor.RemediationActions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@DisplayName("ExtensionResponseBuilder Tests")
class ExtensionResponseBuilderTest {

    private ExtensionResponseBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new ExtensionResponseBuilder();
    }

    @Test
    @DisplayName("Should build response with default SUCCESS result")
    void shouldBuildResponseWithDefaultSuccessResult() {
        ExtensionResponse response = builder.build();
        assertNotNull(response);
        assertEquals(ExtensionResponse.Result.SUCCESS, response.getResult());
    }

    @Test
    @DisplayName("Should set result and return builder for chaining")
    void shouldSetResultAndReturnBuilder() {
        ExtensionResponseBuilder returned = builder.result(ExtensionResponse.Result.FAILURE);
        assertSame(builder, returned, "result() should return the same builder instance");

        ExtensionResponse response = returned.build();
        assertEquals(ExtensionResponse.Result.FAILURE, response.getResult());
    }

    @Test
    @DisplayName("Should set values and return builder for chaining")
    void shouldSetValuesAndReturnBuilder() {
        Map<String, Object> values = new HashMap<>();
        values.put("key1", "value1");
        values.put("key2", 42);

        ExtensionResponseBuilder returned = builder.values(values);
        assertSame(builder, returned, "values() should return the same builder instance");

        ExtensionResponse response = returned.build();
        assertNotNull(response.getResponseValue());
        assertEquals("value1", response.getResponseValue().get("key1"));
        assertEquals(42, response.getResponseValue().get("key2"));
    }

    @Test
    @DisplayName("Should set error with exception, message, and exception type")
    void shouldSetErrorWithException() {
        Exception cause = new RuntimeException("Something went wrong");
        ExtensionResponseBuilder returned = builder.error(
                cause,
                "An error occurred",
                ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR
        );

        assertSame(builder, returned, "error() should return the same builder instance");

        ExtensionResponse response = returned.build();
        assertNotNull(response.getError());
        assertEquals("An error occurred", response.getError().getErrorMessage());
        assertEquals(ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR, response.getError().getExceptionType());
    }

    @Test
    @DisplayName("Should set error with message and exception type only")
    void shouldSetErrorWithMessageOnly() {
        ExtensionResponseBuilder returned = builder.error(
                "Validation failed",
                ExtensionResponse.Error.ExceptionType.INPUT_ERROR
        );

        assertSame(builder, returned, "error() should return the same builder instance");

        ExtensionResponse response = returned.build();
        assertNotNull(response.getError());
        assertEquals("Validation failed", response.getError().getErrorMessage());
        assertEquals(ExtensionResponse.Error.ExceptionType.INPUT_ERROR, response.getError().getExceptionType());
    }

    @Test
    @DisplayName("Should set remediation actions with list and subCatalogRequestName")
    void shouldSetRemediationActionsWithList() {
        RemediationAction action = mock(RemediationAction.class);
        List<RemediationAction> actions = List.of(action);

        ExtensionResponseBuilder returned = builder.remediationActions(actions, "subCatalog1");
        assertSame(builder, returned, "remediationActions() should return the same builder instance");

        ExtensionResponse response = returned.build();
        assertNotNull(response.getRemediationActions());
    }

    @Test
    @DisplayName("Should set remediation actions with RemediationActions object")
    void shouldSetRemediationActionsWithObject() {
        RemediationAction action = mock(RemediationAction.class);
        RemediationActions remediationActions = new RemediationActions(List.of(action), "catalog");

        ExtensionResponseBuilder returned = builder.remediationActions(remediationActions);
        assertSame(builder, returned, "remediationActions() should return the same builder instance");

        ExtensionResponse response = returned.build();
        assertNotNull(response.getRemediationActions());
    }

    @Test
    @DisplayName("Should set state and return builder for chaining")
    void shouldSetStateAndReturnBuilder() {
        Map<String, Object> state = new HashMap<>();
        state.put("step", "completed");
        state.put("count", 5);

        ExtensionResponseBuilder returned = builder.state(state);
        assertSame(builder, returned, "state() should return the same builder instance");

        ExtensionResponse response = returned.build();
        assertNotNull(response.getState());
        assertEquals("completed", response.getState().get("step"));
        assertEquals(5, response.getState().get("count"));
    }

    @Test
    @DisplayName("success() should set result to SUCCESS and values")
    void successShouldSetResultAndValues() {
        Map<String, Object> values = Map.of("data", "test-data");

        ExtensionResponseBuilder returned = builder.success(values);
        assertSame(builder, returned, "success() should return the same builder instance");

        ExtensionResponse response = returned.build();
        assertEquals(ExtensionResponse.Result.SUCCESS, response.getResult());
        assertNotNull(response.getResponseValue());
        assertEquals("test-data", response.getResponseValue().get("data"));
    }

    @Test
    @DisplayName("failure() should set result to FAILURE and error")
    void failureShouldSetResultAndError() {
        ExtensionResponse.Error error = new ExtensionResponse.Error(
                "Failure message",
                System.currentTimeMillis(),
                ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR,
                ""
        );

        ExtensionResponseBuilder returned = builder.failure(error);
        assertSame(builder, returned, "failure() should return the same builder instance");

        ExtensionResponse response = returned.build();
        assertEquals(ExtensionResponse.Result.FAILURE, response.getResult());
        assertNotNull(response.getError());
        assertEquals("Failure message", response.getError().getErrorMessage());
    }

    @Test
    @DisplayName("Should support full builder chain pattern")
    void shouldSupportFullBuilderChain() {
        Map<String, Object> values = Map.of("result", "ok");
        Map<String, Object> state = Map.of("phase", "done");

        ExtensionResponse response = builder
                .result(ExtensionResponse.Result.SUCCESS)
                .values(values)
                .state(state)
                .build();

        assertAll("Full builder chain should produce correct response",
                () -> assertEquals(ExtensionResponse.Result.SUCCESS, response.getResult()),
                () -> assertNotNull(response.getResponseValue()),
                () -> assertEquals("ok", response.getResponseValue().get("result")),
                () -> assertNotNull(response.getState()),
                () -> assertEquals("done", response.getState().get("phase"))
        );
    }

    @Test
    @DisplayName("Should build response with null values when not set")
    void shouldBuildResponseWithNullValuesWhenNotSet() {
        ExtensionResponse response = builder.build();
        assertNull(response.getResponseValue());
        assertNull(response.getError());
        assertNull(response.getState());
    }

    @Test
    @DisplayName("Should override result when success() called after failure()")
    void shouldOverrideResultWhenSuccessCalledAfterFailure() {
        ExtensionResponse.Error error = new ExtensionResponse.Error(
                "error", System.currentTimeMillis(),
                ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR, ""
        );

        ExtensionResponse response = builder
                .failure(error)
                .success(Map.of("recovered", true))
                .build();

        assertEquals(ExtensionResponse.Result.SUCCESS, response.getResult());
    }

    @Test
    @DisplayName("Should set error with exception that has stack trace")
    void shouldSetErrorWithExceptionStackTrace() {
        Exception cause = new RuntimeException("test exception");
        builder.error(cause, "Error with stack trace", ExtensionResponse.Error.ExceptionType.LOGIC_ERROR);

        ExtensionResponse response = builder.build();
        assertNotNull(response.getError());
        assertNotNull(response.getError().getStackTrace());
    }

    @Test
    @DisplayName("Should set remediation actions with empty list")
    void shouldSetRemediationActionsWithEmptyList() {
        List<RemediationAction> emptyActions = new ArrayList<>();

        ExtensionResponseBuilder returned = builder.remediationActions(emptyActions, "emptyCatalog");
        assertSame(builder, returned);

        ExtensionResponse response = returned.build();
        assertNotNull(response.getRemediationActions());
    }
}
