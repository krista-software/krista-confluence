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
import app.krista.ksdk.context.RequestContext;
import app.krista.model.base.FreeForm;
import com.kristasoft.extensions.essentials.collaboration.confluence.api.ConfluenceAuditService;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.entities.Content;
import com.kristasoft.extensions.essentials.collaboration.confluence.service.XHTMLContentService;
import com.kristasoft.extensions.essentials.collaboration.confluence.utils.TelemetryHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for {@link ContentArea} targeting 100% code coverage.
 *
 * <h2>Branch map per method</h2>
 * <pre>
 * generateContentForTable / generateHeadingContent / generateAnchorTag
 *   ├─ happy path          → service call succeeds, auditRecorded=false, finally calls success
 *   ├─ MustAuthorizeException  → auditRecorded=true, authFailure called, rethrown
 *   └─ general Exception       → auditRecorded=true, failure called, recordError, rethrown
 * </pre>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ContentArea Tests")
class ContentAreaTest {

    @Mock
    private XHTMLContentService service;

    @Mock
    private TelemetryHelper telemetryHelper;

    @Mock
    private ConfluenceAuditService confluenceAuditService;

    @Mock
    private RequestContext requestContext;

    private ContentArea contentArea;

    private static final String INVOKER_ID = "test-invoker-id";

    @BeforeEach
    void setUp() {
        when(requestContext.getInvokerId()).thenReturn(INVOKER_ID);
        contentArea = new ContentArea(service, telemetryHelper, confluenceAuditService, requestContext);
    }

    // ── Helper methods ──────────────────────────────────────────────────────

    private Content createContent(String id, String body) {
        Content content = new Content();
        content.iD = id;
        content.body = body;
        return content;
    }

    private List<FreeForm> createTableData() {
        FreeForm row1 = new FreeForm();
        row1.put("Name", "Text", "Alice");
        row1.put("Age", "Text", "30");
        FreeForm row2 = new FreeForm();
        row2.put("Name", "Text", "Bob");
        row2.put("Age", "Text", "25");
        return List.of(row1, row2);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  generateContentForTable
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("generateContentForTable")
    class GenerateContentForTable {

        @Test
        @DisplayName("Happy path: returns Content and calls audit success")
        void happyPath_returnsContentAndCallsAuditSuccess() {
            // Arrange
            List<String> columns = List.of("Name", "Age");
            List<FreeForm> table = createTableData();
            Content expected = createContent("uuid-1", "<table>...</table>");

            when(service.createContentForTable(eq(columns), anyList())).thenReturn(expected);

            // Act
            Content result = contentArea.generateContentForTable(columns, table);

            // Assert
            assertNotNull(result);
            assertEquals("uuid-1", result.iD);
            assertEquals("<table>...</table>", result.body);

            verify(confluenceAuditService).triggered(INVOKER_ID, "Generate Content for Table");
            verify(confluenceAuditService).success(INVOKER_ID, "Generate Content for Table");
            verify(confluenceAuditService, never()).failure(anyString(), anyString(), any());
            verify(confluenceAuditService, never()).authFailure(anyString(), anyString(), any());
            verify(telemetryHelper).incrementCount("confluence.generateContentForTable");
            verify(telemetryHelper).recordSuccess(eq("confluence.generateContentForTable"), anyLong(), anyMap());
        }

        @Test
        @DisplayName("MustAuthorizeException: calls authFailure and rethrows")
        void mustAuthorizeException_callsAuthFailureAndRethrows() {
            // Arrange
            List<String> columns = List.of("Name");
            List<FreeForm> table = createTableData();
            MustAuthorizeException authException = new MustAuthorizeException("Auth required");

            when(service.createContentForTable(eq(columns), anyList())).thenThrow(authException);

            // Act & Assert
            MustAuthorizeException thrown = assertThrows(MustAuthorizeException.class,
                    () -> contentArea.generateContentForTable(columns, table));

            assertSame(authException, thrown);
            verify(confluenceAuditService).triggered(INVOKER_ID, "Generate Content for Table");
            verify(confluenceAuditService).authFailure(INVOKER_ID, "Generate Content for Table", authException);
            verify(confluenceAuditService, never()).success(anyString(), anyString());
            verify(confluenceAuditService, never()).failure(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("General Exception: calls failure, recordError and rethrows")
        void generalException_callsFailureAndRethrows() {
            // Arrange
            List<String> columns = List.of("Name");
            List<FreeForm> table = createTableData();
            RuntimeException exception = new RuntimeException("Service error");

            when(service.createContentForTable(eq(columns), anyList())).thenThrow(exception);

            // Act & Assert
            RuntimeException thrown = assertThrows(RuntimeException.class,
                    () -> contentArea.generateContentForTable(columns, table));

            assertSame(exception, thrown);
            verify(confluenceAuditService).triggered(INVOKER_ID, "Generate Content for Table");
            verify(confluenceAuditService).failure(INVOKER_ID, "Generate Content for Table", exception);
            verify(confluenceAuditService, never()).success(anyString(), anyString());
            verify(confluenceAuditService, never()).authFailure(anyString(), anyString(), any());
            verify(telemetryHelper).incrementCount("confluence.generateContentForTable");
            verify(telemetryHelper).recordError(eq("confluence.generateContentForTable"), anyLong(), eq(exception), anyMap());
        }

        @Test
        @DisplayName("Happy path with null content ID handled in telemetry")
        void happyPath_nullContentId_handledInTelemetry() {
            // Arrange
            List<String> columns = List.of("Col1");
            List<FreeForm> table = createTableData();
            Content content = new Content();
            content.iD = null;
            content.body = "<table/>";

            when(service.createContentForTable(eq(columns), anyList())).thenReturn(content);

            // Act
            Content result = contentArea.generateContentForTable(columns, table);

            // Assert
            assertNotNull(result);
            assertNull(result.iD);
            verify(telemetryHelper).recordSuccess(eq("confluence.generateContentForTable"), anyLong(), anyMap());
            verify(confluenceAuditService).success(INVOKER_ID, "Generate Content for Table");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  generateHeadingContent
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("generateHeadingContent")
    class GenerateHeadingContent {

        @Test
        @DisplayName("Happy path: returns Content and calls audit success")
        void happyPath_returnsContentAndCallsAuditSuccess() {
            // Arrange
            Content expected = createContent("heading-1", "<h1>Title</h1>");
            when(service.createHeadingContent("Title", "h1", "Center", true)).thenReturn(expected);

            // Act
            Content result = contentArea.generateHeadingContent("Title", "h1", "Center", true);

            // Assert
            assertNotNull(result);
            assertEquals("heading-1", result.iD);
            assertEquals("<h1>Title</h1>", result.body);

            verify(confluenceAuditService).triggered(INVOKER_ID, "Generate Heading Content");
            verify(confluenceAuditService).success(INVOKER_ID, "Generate Heading Content");
            verify(confluenceAuditService, never()).failure(anyString(), anyString(), any());
            verify(confluenceAuditService, never()).authFailure(anyString(), anyString(), any());
            verify(telemetryHelper).incrementCount("confluence.generateHeadingContent");
            verify(telemetryHelper).recordSuccess(eq("confluence.generateHeadingContent"), anyLong(), anyMap());
        }

        @Test
        @DisplayName("Happy path with breakLine false")
        void happyPath_breakLineFalse() {
            // Arrange
            Content expected = createContent("heading-2", "<h3>Sub</h3>");
            when(service.createHeadingContent("Sub", "h3", "Left", false)).thenReturn(expected);

            // Act
            Content result = contentArea.generateHeadingContent("Sub", "h3", "Left", false);

            // Assert
            assertNotNull(result);
            assertEquals("heading-2", result.iD);
            verify(confluenceAuditService).success(INVOKER_ID, "Generate Heading Content");
        }

        @Test
        @DisplayName("MustAuthorizeException: calls authFailure and rethrows")
        void mustAuthorizeException_callsAuthFailureAndRethrows() {
            // Arrange
            MustAuthorizeException authException = new MustAuthorizeException("Not authorized");
            when(service.createHeadingContent(anyString(), anyString(), anyString(), any()))
                    .thenThrow(authException);

            // Act & Assert
            MustAuthorizeException thrown = assertThrows(MustAuthorizeException.class,
                    () -> contentArea.generateHeadingContent("Text", "h2", "Right", false));

            assertSame(authException, thrown);
            verify(confluenceAuditService).triggered(INVOKER_ID, "Generate Heading Content");
            verify(confluenceAuditService).authFailure(INVOKER_ID, "Generate Heading Content", authException);
            verify(confluenceAuditService, never()).success(anyString(), anyString());
            verify(confluenceAuditService, never()).failure(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("General Exception: calls failure, recordError and rethrows")
        void generalException_callsFailureAndRethrows() {
            // Arrange
            RuntimeException exception = new RuntimeException("Template engine error");
            when(service.createHeadingContent(anyString(), anyString(), anyString(), any()))
                    .thenThrow(exception);

            // Act & Assert
            RuntimeException thrown = assertThrows(RuntimeException.class,
                    () -> contentArea.generateHeadingContent("Text", "h1", "Justify", true));

            assertSame(exception, thrown);
            verify(confluenceAuditService).triggered(INVOKER_ID, "Generate Heading Content");
            verify(confluenceAuditService).failure(INVOKER_ID, "Generate Heading Content", exception);
            verify(confluenceAuditService, never()).success(anyString(), anyString());
            verify(telemetryHelper).recordError(eq("confluence.generateHeadingContent"), anyLong(), eq(exception), anyMap());
        }

        @Test
        @DisplayName("Null content returned by service is handled in telemetry")
        void nullContentReturned_handledInTelemetry() {
            // Arrange
            when(service.createHeadingContent("Text", "h4", "Left", null)).thenReturn(null);

            // Act
            Content result = contentArea.generateHeadingContent("Text", "h4", "Left", null);

            // Assert
            assertNull(result);
            verify(telemetryHelper).recordSuccess(eq("confluence.generateHeadingContent"), anyLong(), anyMap());
            verify(confluenceAuditService).success(INVOKER_ID, "Generate Heading Content");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  generateAnchorTag
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("generateAnchorTag")
    class GenerateAnchorTag {

        @Test
        @DisplayName("Happy path: returns Content and calls audit success")
        void happyPath_returnsContentAndCallsAuditSuccess() {
            // Arrange
            Content expected = createContent("anchor-1", "<a href=\"https://example.com\">Click</a>");
            when(service.createAnchorTag("Click", "https://example.com")).thenReturn(expected);

            // Act
            Content result = contentArea.generateAnchorTag("Click", "https://example.com");

            // Assert
            assertNotNull(result);
            assertEquals("anchor-1", result.iD);
            assertTrue(result.body.contains("example.com"));

            verify(confluenceAuditService).triggered(INVOKER_ID, "Generate Anchor Tag");
            verify(confluenceAuditService).success(INVOKER_ID, "Generate Anchor Tag");
            verify(confluenceAuditService, never()).failure(anyString(), anyString(), any());
            verify(confluenceAuditService, never()).authFailure(anyString(), anyString(), any());
            verify(telemetryHelper).incrementCount("confluence.generateAnchorTag");
            verify(telemetryHelper).recordSuccess(eq("confluence.generateAnchorTag"), anyLong(), anyMap());
        }

        @Test
        @DisplayName("MustAuthorizeException: calls authFailure and rethrows")
        void mustAuthorizeException_callsAuthFailureAndRethrows() {
            // Arrange
            MustAuthorizeException authException = new MustAuthorizeException("Forbidden");
            when(service.createAnchorTag(anyString(), anyString())).thenThrow(authException);

            // Act & Assert
            MustAuthorizeException thrown = assertThrows(MustAuthorizeException.class,
                    () -> contentArea.generateAnchorTag("Link", "https://foo.com"));

            assertSame(authException, thrown);
            verify(confluenceAuditService).triggered(INVOKER_ID, "Generate Anchor Tag");
            verify(confluenceAuditService).authFailure(INVOKER_ID, "Generate Anchor Tag", authException);
            verify(confluenceAuditService, never()).success(anyString(), anyString());
            verify(confluenceAuditService, never()).failure(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("General Exception: calls failure, recordError and rethrows")
        void generalException_callsFailureAndRethrows() {
            // Arrange
            RuntimeException exception = new RuntimeException("Anchor generation failed");
            when(service.createAnchorTag(anyString(), anyString())).thenThrow(exception);

            // Act & Assert
            RuntimeException thrown = assertThrows(RuntimeException.class,
                    () -> contentArea.generateAnchorTag("Link", "bad-url"));

            assertSame(exception, thrown);
            verify(confluenceAuditService).triggered(INVOKER_ID, "Generate Anchor Tag");
            verify(confluenceAuditService).failure(INVOKER_ID, "Generate Anchor Tag", exception);
            verify(confluenceAuditService, never()).success(anyString(), anyString());
            verify(telemetryHelper).incrementCount("confluence.generateAnchorTag");
            verify(telemetryHelper).recordError(eq("confluence.generateAnchorTag"), anyLong(), eq(exception), anyMap());
        }

        @Test
        @DisplayName("Null content returned by service is handled in telemetry")
        void nullContentReturned_handledInTelemetry() {
            // Arrange
            when(service.createAnchorTag("Text", "href")).thenReturn(null);

            // Act
            Content result = contentArea.generateAnchorTag("Text", "href");

            // Assert
            assertNull(result);
            verify(telemetryHelper).recordSuccess(eq("confluence.generateAnchorTag"), anyLong(), anyMap());
            verify(confluenceAuditService).success(INVOKER_ID, "Generate Anchor Tag");
        }
    }
}
