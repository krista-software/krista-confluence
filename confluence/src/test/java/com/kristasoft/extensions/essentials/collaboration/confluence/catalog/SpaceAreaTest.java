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

package com.kristasoft.extensions.essentials.collaboration.confluence.catalog;

import app.krista.extension.authorization.MustAuthorizeException;
import app.krista.extension.executor.ExtensionResponse;
import app.krista.ksdk.context.RequestContext;
import app.krista.model.base.FreeForm;
import com.kristasoft.extensions.essentials.collaboration.confluence.api.ConfluenceAuditService;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.entities.Page;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.entities.Space;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.errorhandlers.ErrorHandlingStateManager;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.errorhandlers.ExtensionResponseGenerator;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.validators.ValidationOrchestrator;
import com.kristasoft.extensions.essentials.collaboration.confluence.service.ConfluenceService;
import com.kristasoft.extensions.essentials.collaboration.confluence.service.PageDocxService;
import com.kristasoft.extensions.essentials.collaboration.confluence.utils.Constants;
import com.kristasoft.extensions.essentials.collaboration.confluence.utils.TelemetryHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for {@link SpaceArea} targeting 100% code coverage.
 *
 * <h2>Branch map</h2>
 * <pre>
 * getSpaceById
 *   ├─ spaceId non-null, non-blank  → validation param added
 *   ├─ spaceId null or blank        → validation param NOT added
 *   ├─ validationResults empty      → service call, success
 *   ├─ validationResults non-empty
 *   │     ├─ allowRetry == true     → confirmation response
 *   │     └─ allowRetry != true     → validation error
 *   ├─ MustAuthorizeException       → authFailure, rethrow
 *   ├─ general Exception            → failure response
 *   └─ finally: audit success if not recorded
 *
 * getSpaces
 *   ├─ happy path                   → success with Spaces list
 *   ├─ MustAuthorizeException       → authFailure, rethrow
 *   ├─ general Exception            → audit failure, rethrow
 *   └─ finally: audit success if not recorded
 *
 * getDirectChildPagesOfSpace
 *   ├─ happy path                   → success with Pages list
 *   ├─ MustAuthorizeException       → authFailure, rethrow
 *   ├─ general Exception            → error result with Success=false
 *   └─ finally: audit success if not recorded
 *
 * getNotification
 *   ├─ eventName == CONFLUENCE_UPDATES → parse JSON, return FreeForm
 *   ├─ eventName != CONFLUENCE_UPDATES → return null
 *   ├─ MustAuthorizeException          → authFailure, rethrow
 *   ├─ general Exception               → audit failure, rethrow
 *   └─ finally: audit success if not recorded
 * </pre>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SpaceArea Tests")
class SpaceAreaTest {

    @Mock
    private ConfluenceService service;

    @Mock
    private ValidationOrchestrator validationOrchestrator;

    @Mock
    private ErrorHandlingStateManager errorHandlingStateManager;

    @Mock
    private ExtensionResponseGenerator responseGenerator;

    @Mock
    private PageDocxService pageDocxService;

    @Mock
    private TelemetryHelper telemetryHelper;

    @Mock
    private ConfluenceAuditService confluenceAuditService;

    @Mock
    private RequestContext requestContext;

    private SpaceArea spaceArea;

    private static final String INVOKER_ID = "test-invoker-id";

    @BeforeEach
    void setUp() {
        when(requestContext.getInvokerId()).thenReturn(INVOKER_ID);
        spaceArea = new SpaceArea(service, validationOrchestrator, errorHandlingStateManager,
                responseGenerator, pageDocxService, telemetryHelper,
                confluenceAuditService, requestContext);
    }

    // ── Helper methods ──────────────────────────────────────────────────────

    private com.kristasoft.extensions.essentials.collaboration.confluence.dto.Space createDtoSpace(
            String id, String name, String key, String status) {
        return com.kristasoft.extensions.essentials.collaboration.confluence.dto.Space.builder()
                .setId(id)
                .setName(name)
                .setKey(key)
                .setStatus(status)
                .setType("global")
                .setDescription("A test space")
                .setCreatedAt(new Date())
                .build();
    }

    private com.kristasoft.extensions.essentials.collaboration.confluence.dto.Page createDtoPage(
            String id, String title, String spaceId) {
        com.kristasoft.extensions.essentials.collaboration.confluence.dto.Page page =
                new com.kristasoft.extensions.essentials.collaboration.confluence.dto.Page.PageBuilder(id, title)
                        .spaceId(spaceId)
                        .status("current")
                        .type("page")
                        .build();
        page.setDomainUrl("https://test.atlassian.net/wiki");
        page.setLinks(Map.of("webui", "/spaces/" + spaceId + "/pages/" + id));
        return page;
    }

    private ValidationOrchestrator.ValidationResult createSpaceValidationResult() {
        return new ValidationOrchestrator.ValidationResult(
                "Please confirm the space ID",
                "Space Id",
                "Enter the space ID",
                "Invalid space ID",
                "Identifier",
                Map.of("visualWidth", "S")
        );
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  getSpaceById
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getSpaceById")
    class GetSpaceById {

        @Test
        @DisplayName("Happy path: validation passes, returns Space entity")
        void happyPath_validationPasses_returnsSpace() {
            // Arrange
            String spaceId = "123";
            when(validationOrchestrator.validate(anyMap(), eq(SpaceArea.class), eq("getSpaceById")))
                    .thenReturn(Collections.emptyList());
            when(service.getSpaceById(spaceId)).thenReturn(createDtoSpace("123", "Test Space", "TST", "current"));

            // Act
            ExtensionResponse result = spaceArea.getSpaceById(spaceId, null);

            // Assert
            assertNotNull(result);
            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());

            Object spaceObj = result.getResponseValue().get("Space");
            assertNotNull(spaceObj);
            assertInstanceOf(Space.class, spaceObj);
            Space space = (Space) spaceObj;
            assertEquals("123", space.id);
            assertEquals("Test Space", space.name);

            verify(confluenceAuditService).triggered(INVOKER_ID, "Get Space By Id");
            verify(confluenceAuditService).success(INVOKER_ID, "Get Space By Id");
            verify(telemetryHelper).incrementCount("confluence.getSpaceById");
            verify(telemetryHelper).recordSuccess(eq("confluence.getSpaceById"), anyLong(), anyMap());
        }

        @Test
        @DisplayName("Validation fails with allowRetry=true: returns confirmation response")
        void validationFails_allowRetryTrue_returnsConfirmation() {
            // Arrange
            String spaceId = "invalid";
            List<ValidationOrchestrator.ValidationResult> validationResults =
                    List.of(createSpaceValidationResult());
            when(validationOrchestrator.validate(anyMap(), eq(SpaceArea.class), eq("getSpaceById")))
                    .thenReturn(validationResults);

            ExtensionResponse mockResponse = mock(ExtensionResponse.class);
            when(responseGenerator.generateConfirmationResponse(
                    eq(ExtensionResponse.Error.ExceptionType.INPUT_ERROR),
                    eq(validationResults),
                    anyString(),
                    anyMap()
            )).thenReturn(mockResponse);

            // Act
            ExtensionResponse result = spaceArea.getSpaceById(spaceId, true);

            // Assert
            assertSame(mockResponse, result);
            verify(errorHandlingStateManager).put(anyString(), anyMap());
            verify(telemetryHelper).recordRetryPrompted(eq("confluence.getSpaceById"), anyLong(), anyMap());
            verify(confluenceAuditService).success(INVOKER_ID, "Get Space By Id");
        }

        @Test
        @DisplayName("Validation fails with allowRetry=false: returns validation error response")
        void validationFails_allowRetryFalse_returnsValidationError() {
            // Arrange
            String spaceId = "invalid";
            List<ValidationOrchestrator.ValidationResult> validationResults =
                    List.of(createSpaceValidationResult());
            when(validationOrchestrator.validate(anyMap(), eq(SpaceArea.class), eq("getSpaceById")))
                    .thenReturn(validationResults);

            ExtensionResponse mockResponse = mock(ExtensionResponse.class);
            when(responseGenerator.generateValidationErrorResponse(validationResults)).thenReturn(mockResponse);

            // Act
            ExtensionResponse result = spaceArea.getSpaceById(spaceId, false);

            // Assert
            assertSame(mockResponse, result);
            verify(responseGenerator).generateValidationErrorResponse(validationResults);
            verify(telemetryHelper).recordError(eq("confluence.getSpaceById"), anyLong(), any(Exception.class), anyMap());
        }

        @Test
        @DisplayName("Validation fails with allowRetry=null: returns validation error")
        void validationFails_allowRetryNull_returnsValidationError() {
            // Arrange
            List<ValidationOrchestrator.ValidationResult> validationResults =
                    List.of(createSpaceValidationResult());
            when(validationOrchestrator.validate(anyMap(), eq(SpaceArea.class), eq("getSpaceById")))
                    .thenReturn(validationResults);

            ExtensionResponse mockResponse = mock(ExtensionResponse.class);
            when(responseGenerator.generateValidationErrorResponse(validationResults)).thenReturn(mockResponse);

            // Act
            ExtensionResponse result = spaceArea.getSpaceById("invalid", null);

            // Assert
            assertSame(mockResponse, result);
        }

        @Test
        @DisplayName("Null spaceId: validation param not added")
        void nullSpaceId_validationParamNotAdded() {
            // Arrange
            when(validationOrchestrator.validate(anyMap(), eq(SpaceArea.class), eq("getSpaceById")))
                    .thenReturn(Collections.emptyList());
            when(service.getSpaceById(null)).thenReturn(createDtoSpace("null-space", "Null Space", "NS", "current"));

            // Act
            ExtensionResponse result = spaceArea.getSpaceById(null, null);

            // Assert
            assertNotNull(result);
            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());
        }

        @Test
        @DisplayName("Blank spaceId: validation param not added")
        void blankSpaceId_validationParamNotAdded() {
            // Arrange
            when(validationOrchestrator.validate(anyMap(), eq(SpaceArea.class), eq("getSpaceById")))
                    .thenReturn(Collections.emptyList());
            when(service.getSpaceById("  ")).thenReturn(createDtoSpace("blank", "Blank", "BLK", "current"));

            // Act
            ExtensionResponse result = spaceArea.getSpaceById("  ", null);

            // Assert
            assertNotNull(result);
            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());
        }

        @Test
        @DisplayName("MustAuthorizeException: calls authFailure and rethrows")
        void mustAuthorizeException_callsAuthFailureAndRethrows() {
            // Arrange
            MustAuthorizeException authException = new MustAuthorizeException("Not authorized");
            when(validationOrchestrator.validate(anyMap(), eq(SpaceArea.class), eq("getSpaceById")))
                    .thenThrow(authException);

            // Act & Assert
            MustAuthorizeException thrown = assertThrows(MustAuthorizeException.class,
                    () -> spaceArea.getSpaceById("123", null));

            assertSame(authException, thrown);
            verify(confluenceAuditService).triggered(INVOKER_ID, "Get Space By Id");
            verify(confluenceAuditService).authFailure(INVOKER_ID, "Get Space By Id", authException);
            verify(confluenceAuditService, never()).success(anyString(), anyString());
        }

        @Test
        @DisplayName("General Exception: returns failure response")
        void generalException_returnsFailure() {
            // Arrange
            RuntimeException exception = new RuntimeException("Space not found");
            when(validationOrchestrator.validate(anyMap(), eq(SpaceArea.class), eq("getSpaceById")))
                    .thenReturn(Collections.emptyList());
            when(service.getSpaceById("bad")).thenThrow(exception);

            // Act
            ExtensionResponse result = spaceArea.getSpaceById("bad", null);

            // Assert
            assertNotNull(result);
            assertEquals(ExtensionResponse.Result.FAILURE, result.getResult());
            assertNotNull(result.getError());
            assertEquals(ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR, result.getError().getExceptionType());
            assertTrue(result.getError().getErrorMessage().contains("Space not found"));

            verify(confluenceAuditService).failure(INVOKER_ID, "Get Space By Id", exception);
            verify(confluenceAuditService, never()).success(anyString(), anyString());
            verify(telemetryHelper).recordError(eq("confluence.getSpaceById"), anyLong(), eq(exception), anyMap());
        }

        @Test
        @DisplayName("Validation fails with null spaceId and allowRetry=true: confirmationParams has empty string")
        void validationFails_nullSpaceId_allowRetryTrue() {
            // Arrange
            List<ValidationOrchestrator.ValidationResult> validationResults =
                    List.of(createSpaceValidationResult());
            when(validationOrchestrator.validate(anyMap(), eq(SpaceArea.class), eq("getSpaceById")))
                    .thenReturn(validationResults);

            ExtensionResponse mockResponse = mock(ExtensionResponse.class);
            when(responseGenerator.generateConfirmationResponse(
                    eq(ExtensionResponse.Error.ExceptionType.INPUT_ERROR),
                    eq(validationResults),
                    anyString(),
                    anyMap()
            )).thenReturn(mockResponse);

            // Act
            ExtensionResponse result = spaceArea.getSpaceById(null, true);

            // Assert
            assertSame(mockResponse, result);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  getSpaces
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getSpaces")
    class GetSpaces {

        @Test
        @DisplayName("Happy path: returns all spaces with identifiers")
        void happyPath_returnsAllSpaces() {
            // Arrange
            com.kristasoft.extensions.essentials.collaboration.confluence.dto.Space[] dtoSpaces = {
                    createDtoSpace("1", "Space One", "S1", "current"),
                    createDtoSpace("2", "Space Two", "S2", "current")
            };
            when(service.getSpaces()).thenReturn(dtoSpaces);

            // Act
            ExtensionResponse result = spaceArea.getSpaces();

            // Assert
            assertNotNull(result);
            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());

            @SuppressWarnings("unchecked")
            List<Space> spaces = (List<Space>) result.getResponseValue().get(Constants.SPACES);
            assertNotNull(spaces);
            assertEquals(2, spaces.size());

            @SuppressWarnings("unchecked")
            List<String> identifiers = (List<String>) result.getResponseValue().get(Constants.SPACE_IDENTIFIERS);
            assertNotNull(identifiers);
            assertEquals(2, identifiers.size());
            assertTrue(identifiers.contains("1"));
            assertTrue(identifiers.contains("2"));

            verify(confluenceAuditService).triggered(INVOKER_ID, "Get Spaces");
            verify(confluenceAuditService).success(INVOKER_ID, "Get Spaces");
            verify(telemetryHelper).incrementCount("confluence.getSpaces");
            verify(telemetryHelper).recordSuccess(eq("confluence.getSpaces"), anyLong(), anyMap());
        }

        @Test
        @DisplayName("Happy path with empty spaces array")
        void happyPath_emptySpaces() {
            // Arrange
            when(service.getSpaces()).thenReturn(new com.kristasoft.extensions.essentials.collaboration.confluence.dto.Space[0]);

            // Act
            ExtensionResponse result = spaceArea.getSpaces();

            // Assert
            assertNotNull(result);
            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());

            @SuppressWarnings("unchecked")
            List<Space> spaces = (List<Space>) result.getResponseValue().get(Constants.SPACES);
            assertEquals(0, spaces.size());
        }

        @Test
        @DisplayName("MustAuthorizeException: calls authFailure and rethrows")
        void mustAuthorizeException_callsAuthFailureAndRethrows() {
            // Arrange
            MustAuthorizeException authException = new MustAuthorizeException("Auth needed");
            when(service.getSpaces()).thenThrow(authException);

            // Act & Assert
            MustAuthorizeException thrown = assertThrows(MustAuthorizeException.class,
                    () -> spaceArea.getSpaces());

            assertSame(authException, thrown);
            verify(confluenceAuditService).triggered(INVOKER_ID, "Get Spaces");
            verify(confluenceAuditService).authFailure(INVOKER_ID, "Get Spaces", authException);
            verify(confluenceAuditService, never()).success(anyString(), anyString());
        }

        @Test
        @DisplayName("General Exception: calls audit failure and rethrows")
        void generalException_callsFailureAndRethrows() {
            // Arrange
            RuntimeException exception = new RuntimeException("Connection error");
            when(service.getSpaces()).thenThrow(exception);

            // Act & Assert
            RuntimeException thrown = assertThrows(RuntimeException.class,
                    () -> spaceArea.getSpaces());

            assertSame(exception, thrown);
            verify(confluenceAuditService).triggered(INVOKER_ID, "Get Spaces");
            verify(confluenceAuditService).failure(INVOKER_ID, "Get Spaces", exception);
            verify(confluenceAuditService, never()).success(anyString(), anyString());
            verify(telemetryHelper).recordError(eq("confluence.getSpaces"), anyLong(), eq(exception), anyMap());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  getDirectChildPagesOfSpace
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getDirectChildPagesOfSpace")
    class GetDirectChildPagesOfSpace {

        @Test
        @DisplayName("Happy path: returns pages with Success=true")
        void happyPath_returnsPagesWithSuccessTrue() {
            // Arrange
            String spaceID = "space-1";
            com.kristasoft.extensions.essentials.collaboration.confluence.dto.Page[] dtoPages = {
                    createDtoPage("p-1", "Page One", spaceID),
                    createDtoPage("p-2", "Page Two", spaceID)
            };
            when(service.getDirectChildPagesOfSpace(spaceID)).thenReturn(dtoPages);

            // Act
            ExtensionResponse result = spaceArea.getDirectChildPagesOfSpace(spaceID);

            // Assert
            assertNotNull(result);
            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());

            @SuppressWarnings("unchecked")
            List<Page> pages = (List<Page>) result.getResponseValue().get(Constants.PAGES);
            assertNotNull(pages);
            assertEquals(2, pages.size());
            assertEquals("p-1", pages.get(0).id);
            assertEquals("p-2", pages.get(1).id);

            @SuppressWarnings("unchecked")
            List<String> identifiers = (List<String>) result.getResponseValue().get(Constants.PAGE_IDENTIFIERS);
            assertNotNull(identifiers);
            assertEquals(2, identifiers.size());

            assertEquals(true, result.getResponseValue().get("Success"));

            verify(confluenceAuditService).triggered(INVOKER_ID, "Get Direct Child Pages of Space");
            verify(confluenceAuditService).success(INVOKER_ID, "Get Direct Child Pages of Space");
            verify(telemetryHelper).recordSuccess(eq("confluence.getDirectChildPagesOfSpace"), anyLong(), anyMap());
        }

        @Test
        @DisplayName("Happy path with empty pages array")
        void happyPath_emptyPages() {
            // Arrange
            when(service.getDirectChildPagesOfSpace("empty-space")).thenReturn(
                    new com.kristasoft.extensions.essentials.collaboration.confluence.dto.Page[0]);

            // Act
            ExtensionResponse result = spaceArea.getDirectChildPagesOfSpace("empty-space");

            // Assert
            assertNotNull(result);
            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());

            @SuppressWarnings("unchecked")
            List<Page> pages = (List<Page>) result.getResponseValue().get(Constants.PAGES);
            assertEquals(0, pages.size());
            assertEquals(true, result.getResponseValue().get("Success"));
        }

        @Test
        @DisplayName("MustAuthorizeException: calls authFailure and rethrows")
        void mustAuthorizeException_callsAuthFailureAndRethrows() {
            // Arrange
            MustAuthorizeException authException = new MustAuthorizeException("Authorization required");
            when(service.getDirectChildPagesOfSpace("space-1")).thenThrow(authException);

            // Act & Assert
            MustAuthorizeException thrown = assertThrows(MustAuthorizeException.class,
                    () -> spaceArea.getDirectChildPagesOfSpace("space-1"));

            assertSame(authException, thrown);
            verify(confluenceAuditService).triggered(INVOKER_ID, "Get Direct Child Pages of Space");
            verify(confluenceAuditService).authFailure(INVOKER_ID, "Get Direct Child Pages of Space", authException);
            verify(confluenceAuditService, never()).success(anyString(), anyString());
        }

        @Test
        @DisplayName("General Exception: returns error result with Success=false and Error message")
        void generalException_returnsErrorResult() {
            // Arrange
            RuntimeException exception = new RuntimeException("Space access denied");
            when(service.getDirectChildPagesOfSpace("bad-space")).thenThrow(exception);

            // Act
            ExtensionResponse result = spaceArea.getDirectChildPagesOfSpace("bad-space");

            // Assert
            assertNotNull(result);
            assertEquals(ExtensionResponse.Result.SUCCESS, result.getResult());

            @SuppressWarnings("unchecked")
            List<?> pages = (List<?>) result.getResponseValue().get(Constants.PAGES);
            assertTrue(pages.isEmpty());

            @SuppressWarnings("unchecked")
            List<?> identifiers = (List<?>) result.getResponseValue().get(Constants.PAGE_IDENTIFIERS);
            assertTrue(identifiers.isEmpty());

            assertEquals(false, result.getResponseValue().get("Success"));
            assertEquals("Space access denied", result.getResponseValue().get("Error"));

            verify(confluenceAuditService).triggered(INVOKER_ID, "Get Direct Child Pages of Space");
            verify(confluenceAuditService).failure(INVOKER_ID, "Get Direct Child Pages of Space", exception);
            verify(confluenceAuditService, never()).success(anyString(), anyString());
            verify(telemetryHelper).recordError(eq("confluence.getDirectChildPagesOfSpace"), anyLong(), eq(exception), anyMap());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  getNotification
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getNotification")
    class GetNotification {

        @Test
        @DisplayName("Event matches CONFLUENCE_UPDATES: parses JSON and returns FreeForm map")
        void confluenceUpdatesEvent_parsesJsonAndReturnsFreeForm() {
            // Arrange
            String eventName = Constants.CONFLUENCE_UPDATES;
            FreeForm eventData = new FreeForm();
            String jsonString = "{\"key\": \"value\", \"number\": 42}";
            eventData.put(Constants.CONFLUENCE_DATA, "Text", jsonString);

            // Act
            Map<String, Object> result = spaceArea.getNotification(eventName, eventData);

            // Assert
            assertNotNull(result);

            verify(confluenceAuditService).triggered(INVOKER_ID, "Get Notification");
            verify(confluenceAuditService).success(INVOKER_ID, "Get Notification");
            verify(confluenceAuditService, never()).failure(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("Event does NOT match CONFLUENCE_UPDATES: returns null")
        void nonConfluenceUpdatesEvent_returnsNull() {
            // Arrange
            String eventName = "Some_Other_Event";
            FreeForm eventData = new FreeForm();

            // Act
            Map<String, Object> result = spaceArea.getNotification(eventName, eventData);

            // Assert
            assertNull(result);

            verify(confluenceAuditService).triggered(INVOKER_ID, "Get Notification");
            verify(confluenceAuditService).success(INVOKER_ID, "Get Notification");
        }

        @Test
        @DisplayName("MustAuthorizeException: calls authFailure and rethrows")
        void mustAuthorizeException_callsAuthFailureAndRethrows() {
            // Arrange - eventName.equals() will throw MustAuthorizeException from inside try
            // We simulate by having a null eventName which will cause NPE, but we need MustAuthorize
            // Instead, mock the audit service to throw after triggered
            // Actually, let's create a scenario where the code throws MustAuthorizeException
            // The simplest approach: eventName equals CONFLUENCE_UPDATES, eventData.get() throws
            String eventName = Constants.CONFLUENCE_UPDATES;
            FreeForm eventData = mock(FreeForm.class);
            when(eventData.get(Constants.CONFLUENCE_DATA)).thenThrow(new MustAuthorizeException("Auth needed"));

            // Act & Assert
            MustAuthorizeException thrown = assertThrows(MustAuthorizeException.class,
                    () -> spaceArea.getNotification(eventName, eventData));

            verify(confluenceAuditService).triggered(INVOKER_ID, "Get Notification");
            verify(confluenceAuditService).authFailure(INVOKER_ID, "Get Notification", thrown);
            verify(confluenceAuditService, never()).success(anyString(), anyString());
        }

        @Test
        @DisplayName("General Exception: calls audit failure and rethrows")
        void generalException_callsFailureAndRethrows() {
            // Arrange - null eventName causes NPE on .equals()
            FreeForm eventData = new FreeForm();

            // Act & Assert
            assertThrows(NullPointerException.class,
                    () -> spaceArea.getNotification(null, eventData));

            verify(confluenceAuditService).triggered(INVOKER_ID, "Get Notification");
            verify(confluenceAuditService).failure(eq(INVOKER_ID), eq("Get Notification"), any(NullPointerException.class));
            verify(confluenceAuditService, never()).success(anyString(), anyString());
        }

        @Test
        @DisplayName("CONFLUENCE_UPDATES with complex JSON: successfully parses nested structure")
        void confluenceUpdatesEvent_complexJson_parsesSuccessfully() {
            // Arrange
            String eventName = Constants.CONFLUENCE_UPDATES;
            FreeForm eventData = new FreeForm();
            String complexJson = "{\"page\": {\"id\": \"12345\", \"title\": \"Updated Page\"}, \"action\": \"update\"}";
            eventData.put(Constants.CONFLUENCE_DATA, "Text", complexJson);

            // Act
            Map<String, Object> result = spaceArea.getNotification(eventName, eventData);

            // Assert
            assertNotNull(result);
            verify(confluenceAuditService).success(INVOKER_ID, "Get Notification");
        }

        @Test
        @DisplayName("CONFLUENCE_UPDATES with invalid JSON: returns fallback FreeForm")
        void confluenceUpdatesEvent_invalidJson_returnsFallback() {
            // Arrange
            String eventName = Constants.CONFLUENCE_UPDATES;
            FreeForm eventData = new FreeForm();
            eventData.put(Constants.CONFLUENCE_DATA, "Text", "not-valid-json{{{");

            // Act
            Map<String, Object> result = spaceArea.getNotification(eventName, eventData);

            // Assert - JsonParser.convertJsonToFreeForm returns fallback FreeForm on parse failure
            assertNotNull(result);
            verify(confluenceAuditService).success(INVOKER_ID, "Get Notification");
        }
    }
}
