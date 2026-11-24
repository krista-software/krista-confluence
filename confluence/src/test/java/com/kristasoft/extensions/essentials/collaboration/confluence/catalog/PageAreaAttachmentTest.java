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

import app.krista.extension.executor.ExtensionResponse;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.kristasoft.extensions.essentials.collaboration.confluence.api.APIVersion;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.entities.Attachment;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.errorhandlers.ErrorHandlingStateManager;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.errorhandlers.ExtensionResponseGenerator;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.validators.ValidationOrchestrator;
import com.kristasoft.extensions.essentials.collaboration.confluence.service.ConfluenceService;
import com.kristasoft.extensions.essentials.collaboration.confluence.service.PageDocxService;
import com.kristasoft.extensions.essentials.collaboration.confluence.api.ConfluenceAuditService;
import com.kristasoft.extensions.essentials.collaboration.confluence.service.PageMarkdownService;
import com.kristasoft.extensions.essentials.collaboration.confluence.utils.GsonFactory;
import com.kristasoft.extensions.essentials.collaboration.confluence.utils.TelemetryHelper;
import com.kristasoft.extensions.essentials.collaboration.confluence.ConfluenceAttributes;
import com.kristasoft.extensions.essentials.collaboration.confluence.client.ConfluenceRESTClient;
import app.krista.ksdk.context.RequestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.HttpURLConnection;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test class for PageArea attachment-related functionality.
 * Tests the Get Attachments for Page catalog request with pagination support.
 */
@DisplayName("PageArea Attachment Tests")
public class PageAreaAttachmentTest {

    @Mock
    private ConfluenceService confluenceService;

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
    private ConfluenceAttributes confluenceAttributes;

    @Mock
    private ConfluenceRESTClient restClient;

    @Mock
    private HttpResponse<String> httpResponse;

    @Mock
    private app.krista.ksdk.files.FileRepository fileRepository;

    @Mock
    private ConfluenceAuditService confluenceAuditService;

    @Mock
    private RequestContext requestContext;

    @Mock
    private PageMarkdownService pageMarkdownService;

    private PageArea pageArea;
    private PageAreaAttachmentHelper attachmentHelper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Create the attachment helper with the mocked service
        attachmentHelper = new PageAreaAttachmentHelper(confluenceService, telemetryHelper, fileRepository);

        pageArea = new PageArea(confluenceService, validationOrchestrator, errorHandlingStateManager,
                responseGenerator, pageDocxService, pageMarkdownService, telemetryHelper, attachmentHelper, fileRepository,
                confluenceAuditService, requestContext);

        // Setup default mocks
        when(confluenceService.getConfluenceAttributes()).thenReturn(confluenceAttributes);
        when(confluenceAttributes.getRestClient()).thenReturn(restClient);
        when(validationOrchestrator.validate(anyMap(), any(), anyString())).thenReturn(Collections.emptyList());
    }

    @Test
    @DisplayName("Should retrieve attachments successfully with single page")
    void testGetAttachmentsForPage_Success() {
        // Arrange
        String pageId = "123456";
        String status = "current";
        String mediaType = null;

        JsonObject responseBody = createAttachmentResponse(2, false);
        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse.body()).thenReturn(responseBody.toString());
        when(restClient.doSyncGet(eq(APIVersion.V2), anyMap(), eq("pages"), eq(pageId), eq("attachments")))
                .thenReturn(httpResponse);

        // Act
        ExtensionResponse response = pageArea.getAttachmentsForPage(pageId, status, mediaType, false);

        // Assert
        assertNotNull(response);
        assertEquals(ExtensionResponse.Result.SUCCESS, response.getResult());
        // Verify the REST client was called correctly
        verify(restClient).doSyncGet(eq(APIVersion.V2), anyMap(), eq("pages"), eq(pageId), eq("attachments"));
    }

    @Test
    @DisplayName("Should handle pagination and retrieve all attachments across multiple pages")
    void testGetAttachmentsForPage_WithPagination() {
        // Arrange
        String pageId = "123456";
        String status = "current";

        // First page response with next link
        JsonObject firstPageResponse = createAttachmentResponse(2, true);
        // Second page response without next link
        JsonObject secondPageResponse = createAttachmentResponse(1, false);

        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse.body())
                .thenReturn(firstPageResponse.toString())
                .thenReturn(secondPageResponse.toString());

        when(restClient.doSyncGet(eq(APIVersion.V2), anyMap(), eq("pages"), eq(pageId), eq("attachments")))
                .thenReturn(httpResponse);
        when(restClient.doSyncGet(eq(APIVersion.V2), anyMap(), any(String[].class)))
                .thenReturn(httpResponse);

        // Act
        ExtensionResponse response = pageArea.getAttachmentsForPage(pageId, status, null, false);

        // Assert
        assertNotNull(response);
        assertEquals(ExtensionResponse.Result.SUCCESS, response.getResult());
        // Verify pagination was handled - should have called doSyncGet twice
        verify(restClient, times(2)).doSyncGet(eq(APIVersion.V2), anyMap(), any(String[].class));
    }

    @Test
    @DisplayName("Should preserve status filter across pagination cursor pages")
    void testGetAttachmentsForPage_StatusFilterPersistsAcrossPagination() {
        // Arrange
        String pageId = "123456";
        String status = "current";

        // First page response with a next link. Confluence's next link only ever
        // carries the cursor (see production example baked into createAttachmentResponse),
        // never the original status/mediaType filters.
        JsonObject firstPageResponse = createAttachmentResponse(2, true);
        JsonObject secondPageResponse = createAttachmentResponse(1, false);

        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse.body())
                .thenReturn(firstPageResponse.toString())
                .thenReturn(secondPageResponse.toString());

        when(restClient.doSyncGet(eq(APIVersion.V2), anyMap(), eq("pages"), eq(pageId), eq("attachments")))
                .thenReturn(httpResponse);
        when(restClient.doSyncGet(eq(APIVersion.V2), anyMap(), any(String[].class)))
                .thenReturn(httpResponse);

        // Act
        ExtensionResponse response = pageArea.getAttachmentsForPage(pageId, status, null, false);

        // Assert
        assertNotNull(response);
        assertEquals(ExtensionResponse.Result.SUCCESS, response.getResult());

        // Both the initial request and the cursor-following request must carry
        // status=current. Confluence's next link never echoes the filter back, so
        // the second call has to re-apply it itself.
        verify(restClient, times(2)).doSyncGet(eq(APIVersion.V2), argThat(map ->
                        map.containsKey("status") && "current".equals(map.get("status"))),
                any(String[].class));
    }

    @Test
    @DisplayName("Should filter attachments by status")
    void testGetAttachmentsForPage_WithStatusFilter() {
        // Arrange
        String pageId = "123456";
        String status = "archived";

        JsonObject responseBody = createAttachmentResponse(1, false);
        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse.body()).thenReturn(responseBody.toString());
        when(restClient.doSyncGet(eq(APIVersion.V2), anyMap(), eq("pages"), eq(pageId), eq("attachments")))
                .thenReturn(httpResponse);

        // Act
        ExtensionResponse response = pageArea.getAttachmentsForPage(pageId, status, null, false);

        // Assert
        assertNotNull(response);
        assertEquals(ExtensionResponse.Result.SUCCESS, response.getResult());
        verify(restClient).doSyncGet(eq(APIVersion.V2), argThat(map ->
                map.containsKey("status") && "archived".equals(map.get("status"))),
                eq("pages"), eq(pageId), eq("attachments"));
    }

    @Test
    @DisplayName("Should filter attachments by media type")
    void testGetAttachmentsForPage_WithMediaTypeFilter() {
        // Arrange
        String pageId = "123456";
        String mediaType = "image/png";

        JsonObject responseBody = createAttachmentResponse(1, false);
        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse.body()).thenReturn(responseBody.toString());
        when(restClient.doSyncGet(eq(APIVersion.V2), anyMap(), eq("pages"), eq(pageId), eq("attachments")))
                .thenReturn(httpResponse);

        // Act
        ExtensionResponse response = pageArea.getAttachmentsForPage(pageId, "current", mediaType, false);

        // Assert
        assertNotNull(response);
        assertEquals(ExtensionResponse.Result.SUCCESS, response.getResult());
        verify(restClient).doSyncGet(eq(APIVersion.V2), argThat(map ->
                map.containsKey("mediaType") && "image/png".equals(map.get("mediaType"))),
                eq("pages"), eq(pageId), eq("attachments"));
    }

    @Test
    @DisplayName("Should not filter when status is 'any'")
    void testGetAttachmentsForPage_StatusAny() {
        // Arrange
        String pageId = "123456";
        String status = "any";

        JsonObject responseBody = createAttachmentResponse(1, false);
        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse.body()).thenReturn(responseBody.toString());
        when(restClient.doSyncGet(eq(APIVersion.V2), anyMap(), eq("pages"), eq(pageId), eq("attachments")))
                .thenReturn(httpResponse);

        // Act
        ExtensionResponse response = pageArea.getAttachmentsForPage(pageId, status, null, false);

        // Assert
        assertNotNull(response);
        assertEquals(ExtensionResponse.Result.SUCCESS, response.getResult());
        verify(restClient).doSyncGet(eq(APIVersion.V2), argThat(map ->
                !map.containsKey("status") || !"any".equals(map.get("status"))),
                eq("pages"), eq(pageId), eq("attachments"));
    }

    @Test
    @DisplayName("Should return empty list when page has no attachments")
    void testGetAttachmentsForPage_NoAttachments() {
        // Arrange
        String pageId = "123456";

        JsonObject responseBody = createAttachmentResponse(0, false);
        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse.body()).thenReturn(responseBody.toString());
        when(restClient.doSyncGet(eq(APIVersion.V2), anyMap(), eq("pages"), eq(pageId), eq("attachments")))
                .thenReturn(httpResponse);

        // Act
        ExtensionResponse response = pageArea.getAttachmentsForPage(pageId, "current", null, false);

        // Assert
        assertNotNull(response);
        assertEquals(ExtensionResponse.Result.SUCCESS, response.getResult());
        verify(restClient).doSyncGet(eq(APIVersion.V2), anyMap(), eq("pages"), eq(pageId), eq("attachments"));
    }

    @Test
    @DisplayName("Should handle API error gracefully")
    void testGetAttachmentsForPage_ApiError() {
        // Arrange
        String pageId = "123456";

        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_NOT_FOUND);
        when(httpResponse.body()).thenReturn("{\"error\": \"Page not found\"}");
        when(restClient.doSyncGet(eq(APIVersion.V2), anyMap(), eq("pages"), eq(pageId), eq("attachments")))
                .thenReturn(httpResponse);

        // Act
        ExtensionResponse response = pageArea.getAttachmentsForPage(pageId, "current", null, false);

        // Assert
        assertNotNull(response);
        assertEquals(ExtensionResponse.Result.FAILURE, response.getResult());
        assertNotNull(response.getError());
    }

    @Test
    @DisplayName("Should successfully retrieve attachments with version information")
    void testGetAttachmentsForPage_WithVersionInfo() {
        // Arrange
        String pageId = "123456";

        JsonObject responseBody = createAttachmentResponseWithVersion();
        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse.body()).thenReturn(responseBody.toString());
        when(restClient.doSyncGet(eq(APIVersion.V2), anyMap(), eq("pages"), eq(pageId), eq("attachments")))
                .thenReturn(httpResponse);

        // Act
        ExtensionResponse response = pageArea.getAttachmentsForPage(pageId, "current", null, false);

        // Assert
        assertNotNull(response);
        assertEquals(ExtensionResponse.Result.SUCCESS, response.getResult());
        verify(restClient).doSyncGet(eq(APIVersion.V2), anyMap(), eq("pages"), eq(pageId), eq("attachments"));
    }

    /**
     * Helper method to create a mock attachment response
     */
    private JsonObject createAttachmentResponse(int count, boolean hasNext) {
        JsonObject response = new JsonObject();
        JsonArray results = new JsonArray();

        for (int i = 1; i <= count; i++) {
            JsonObject attachment = new JsonObject();
            attachment.addProperty("id", "att-" + i);
            attachment.addProperty("title", "attachment-" + i + ".pdf");
            attachment.addProperty("pageId", "123456");
            attachment.addProperty("mediaType", "application/pdf");
            attachment.addProperty("fileSize", 1024 * i);
            attachment.addProperty("downloadLink", "/download/att-" + i);
            attachment.addProperty("webuiLink", "/pages/123456/att-" + i);
            attachment.addProperty("createdAt", "2024-01-01T10:00:00.000Z");
            attachment.addProperty("status", "current");
            results.add(attachment);
        }

        response.add("results", results);

        if (hasNext) {
            JsonObject links = new JsonObject();
            links.addProperty("next", "/wiki/api/v2/pages/123456/attachments?cursor=next123");
            response.add("_links", links);
        }

        return response;
    }

    /**
     * Helper method to create attachment response with version information
     */
    private JsonObject createAttachmentResponseWithVersion() {
        JsonObject response = new JsonObject();
        JsonArray results = new JsonArray();

        JsonObject attachment = new JsonObject();
        attachment.addProperty("id", "att-1");
        attachment.addProperty("title", "document.pdf");
        attachment.addProperty("pageId", "123456");
        attachment.addProperty("mediaType", "application/pdf");
        attachment.addProperty("fileSize", 2048);
        attachment.addProperty("downloadLink", "/download/att-1");
        attachment.addProperty("webuiLink", "/pages/123456/att-1");
        attachment.addProperty("createdAt", "2024-01-01T10:00:00.000Z");
        attachment.addProperty("status", "current");

        JsonObject version = new JsonObject();
        version.addProperty("number", 3);
        version.addProperty("createdAt", "2024-01-15T14:30:00.000Z");
        attachment.add("version", version);

        results.add(attachment);
        response.add("results", results);

        return response;
    }

    // ==================== Tests for Get Attachment by ID ====================

    @Test
    @DisplayName("Should handle API error when getting attachment by ID")
    void testGetAttachmentByID_ApiError() {
        // Arrange
        String attachmentId = "att-invalid";

        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_NOT_FOUND);
        when(httpResponse.body()).thenReturn("{\"error\": \"Attachment not found\"}");
        when(restClient.doSyncGet(eq(APIVersion.V2), anyMap(), eq("attachments"), eq(attachmentId)))
                .thenReturn(httpResponse);

        // Act
        ExtensionResponse response = pageArea.getAttachmentByID(attachmentId, false);

        // Assert
        assertNotNull(response);
        assertEquals(ExtensionResponse.Result.FAILURE, response.getResult());
        assertNotNull(response.getError());
    }

    @Test
    @DisplayName("Should handle retry mechanism for transient errors")
    void testGetAttachmentByID_WithRetry() {
        // Arrange
        String attachmentId = "att-123456";

        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_INTERNAL_ERROR);
        when(httpResponse.body()).thenReturn("{\"error\": \"Internal server error\"}");
        when(restClient.doSyncGet(eq(APIVersion.V2), anyMap(), eq("attachments"), eq(attachmentId)))
                .thenReturn(httpResponse);

        // Act
        ExtensionResponse response = pageArea.getAttachmentByID(attachmentId, true);

        // Assert
        assertNotNull(response);
        assertEquals(ExtensionResponse.Result.FAILURE, response.getResult());
        assertNotNull(response.getError());
    }

    /**
     * Helper method to create a single attachment response
     */
    private JsonObject createSingleAttachmentResponse() {
        JsonObject attachment = new JsonObject();
        attachment.addProperty("id", "att-123456");
        attachment.addProperty("title", "document.pdf");
        attachment.addProperty("pageId", "123456");
        attachment.addProperty("mediaType", "application/pdf");
        attachment.addProperty("fileSize", 2048);
        attachment.addProperty("downloadLink", "/wiki/download/attachments/123456/document.pdf");
        attachment.addProperty("webuiLink", "/pages/123456/att-123456");
        attachment.addProperty("createdAt", "2024-01-01T10:00:00.000Z");
        attachment.addProperty("status", "current");

        JsonObject version = new JsonObject();
        version.addProperty("number", 3);
        version.addProperty("createdAt", "2024-01-15T14:30:00.000Z");
        attachment.add("version", version);

        return attachment;
    }

    // ==================== Tests for Download Attachment ====================

    @Mock
    private HttpResponse<byte[]> binaryHttpResponse;

    @Mock
    private app.krista.ksdk.files.FileHandle fileHandle;

    @Test
    @DisplayName("Should download attachment successfully")
    void testDownloadAttachment_Success() throws Exception {
        // Arrange
        String downloadLink = "/wiki/download/attachments/123456/document.pdf";
        String attachmentId = "att-123456";
        byte[] fileContent = "PDF file content".getBytes();
        app.krista.model.base.File mockFile = new app.krista.model.base.File(1L, "document.pdf", "application/pdf", "test-path");

        when(binaryHttpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(binaryHttpResponse.body()).thenReturn(fileContent);
        when(binaryHttpResponse.headers()).thenReturn(createMockHeaders("application/pdf", "document.pdf"));
        when(restClient.doSyncGetBinary(eq(downloadLink))).thenReturn(binaryHttpResponse);
        when(fileRepository.createNewFileByName(anyString())).thenReturn(fileHandle);
        when(fileHandle.getFile()).thenReturn(mockFile);

        // Act
        ExtensionResponse response = pageArea.downloadAttachment(downloadLink, attachmentId);

        // Assert
        assertNotNull(response);
        assertEquals(ExtensionResponse.Result.SUCCESS, response.getResult());
        verify(restClient).doSyncGetBinary(eq(downloadLink));
    }

    @Test
    @DisplayName("Should reject invalid download link")
    void testDownloadAttachment_InvalidLink() {
        // Arrange
        String invalidLink = "http://malicious.com/file.pdf";
        String attachmentId = "att-123456";

        // Act
        ExtensionResponse response = pageArea.downloadAttachment(invalidLink, attachmentId);

        // Assert
        assertNotNull(response);
        assertEquals(ExtensionResponse.Result.SUCCESS, response.getResult());
        assertEquals(false, response.getResponseValue().get("Success"));
        assertNotNull(response.getResponseValue().get("Error"));
    }

    @Test
    @DisplayName("Should handle download failure")
    void testDownloadAttachment_DownloadFailure() {
        // Arrange
        String downloadLink = "/wiki/download/attachments/123456/document.pdf";
        String attachmentId = "att-123456";

        when(binaryHttpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_NOT_FOUND);
        when(restClient.doSyncGetBinary(eq(downloadLink))).thenReturn(binaryHttpResponse);

        // Act
        ExtensionResponse response = pageArea.downloadAttachment(downloadLink, attachmentId);

        // Assert
        assertNotNull(response);
        assertEquals(ExtensionResponse.Result.SUCCESS, response.getResult());
        assertEquals(false, response.getResponseValue().get("Success"));
        assertNotNull(response.getResponseValue().get("Error"));
    }

    @Test
    @DisplayName("Should require download link")
    void testDownloadAttachment_MissingLink() {
        // Act
        ExtensionResponse response = pageArea.downloadAttachment(null, "att-123456");

        // Assert
        assertNotNull(response);
        assertEquals(ExtensionResponse.Result.SUCCESS, response.getResult());
        assertEquals(false, response.getResponseValue().get("Success"));
        assertNotNull(response.getResponseValue().get("Error"));
    }

    @Test
    @DisplayName("Should correct download link missing /wiki/ prefix")
    void testDownloadAttachment_CorrectDownloadLink() throws Exception {
        // Arrange - download link without /wiki/ prefix (as returned by Confluence API)
        String downloadLinkWithoutWiki = "/download/attachments/123456/document.pdf";
        String expectedCorrectedLink = "/wiki/download/attachments/123456/document.pdf";
        String attachmentId = "att-123456";
        byte[] fileContent = "PDF file content".getBytes();
        app.krista.model.base.File mockFile = new app.krista.model.base.File(1L, "document.pdf", "application/pdf", "test-path");

        when(binaryHttpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(binaryHttpResponse.body()).thenReturn(fileContent);
        when(binaryHttpResponse.headers()).thenReturn(createMockHeaders("application/pdf", "document.pdf"));
        when(restClient.doSyncGetBinary(eq(expectedCorrectedLink))).thenReturn(binaryHttpResponse);
        when(fileRepository.createNewFileByName(anyString())).thenReturn(fileHandle);
        when(fileHandle.getFile()).thenReturn(mockFile);

        // Act
        ExtensionResponse response = pageArea.downloadAttachment(downloadLinkWithoutWiki, attachmentId);

        // Assert
        assertNotNull(response);
        assertEquals(ExtensionResponse.Result.SUCCESS, response.getResult());
        // Verify that the corrected link (with /wiki/ prefix) was used
        verify(restClient).doSyncGetBinary(eq(expectedCorrectedLink));
    }

    @Test
    @DisplayName("Should download different file types successfully")
    void testDownloadAttachment_DifferentFileTypes() throws Exception {
        // Test with image file
        String downloadLink = "/wiki/download/attachments/123456/image.png";
        String attachmentId = "att-image";
        byte[] imageContent = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47}; // PNG header
        app.krista.model.base.File mockFile = new app.krista.model.base.File(1L, "image.png", "image/png", "test-path");

        when(binaryHttpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(binaryHttpResponse.body()).thenReturn(imageContent);
        when(binaryHttpResponse.headers()).thenReturn(createMockHeaders("image/png", "image.png"));
        when(restClient.doSyncGetBinary(eq(downloadLink))).thenReturn(binaryHttpResponse);
        when(fileRepository.createNewFileByName(anyString())).thenReturn(fileHandle);
        when(fileHandle.getFile()).thenReturn(mockFile);

        // Act
        ExtensionResponse response = pageArea.downloadAttachment(downloadLink, attachmentId);

        // Assert
        assertNotNull(response);
        assertEquals(ExtensionResponse.Result.SUCCESS, response.getResult());
        verify(restClient).doSyncGetBinary(eq(downloadLink));
    }

    @Test
    @DisplayName("Should extract filename from Content-Disposition header")
    void testDownloadAttachment_FilenameFromHeader() throws Exception {
        // Arrange
        String downloadLink = "/wiki/download/attachments/123456/file";
        String attachmentId = "att-123456";
        byte[] fileContent = "Document content".getBytes();
        app.krista.model.base.File mockFile = new app.krista.model.base.File(1L, "report.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "test-path");

        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", List.of("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
        headers.put("Content-Disposition", List.of("attachment; filename=\"report.docx\""));

        when(binaryHttpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(binaryHttpResponse.body()).thenReturn(fileContent);
        when(binaryHttpResponse.headers()).thenReturn(java.net.http.HttpHeaders.of(headers, (k, v) -> true));
        when(restClient.doSyncGetBinary(eq(downloadLink))).thenReturn(binaryHttpResponse);
        when(fileRepository.createNewFileByName(anyString())).thenReturn(fileHandle);
        when(fileHandle.getFile()).thenReturn(mockFile);

        // Act
        ExtensionResponse response = pageArea.downloadAttachment(downloadLink, attachmentId);

        // Assert
        assertNotNull(response);
        assertEquals(ExtensionResponse.Result.SUCCESS, response.getResult());
    }

    @Test
    @DisplayName("Should handle large file downloads")
    void testDownloadAttachment_LargeFile() throws Exception {
        // Arrange
        String downloadLink = "/wiki/download/attachments/123456/large-file.zip";
        String attachmentId = "att-large";
        byte[] largeContent = new byte[10 * 1024 * 1024]; // 10 MB
        app.krista.model.base.File mockFile = new app.krista.model.base.File(1L, "large-file.zip", "application/zip", "test-path");

        when(binaryHttpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(binaryHttpResponse.body()).thenReturn(largeContent);
        when(binaryHttpResponse.headers()).thenReturn(createMockHeaders("application/zip", "large-file.zip"));
        when(restClient.doSyncGetBinary(eq(downloadLink))).thenReturn(binaryHttpResponse);
        when(fileRepository.createNewFileByName(anyString())).thenReturn(fileHandle);
        when(fileHandle.getFile()).thenReturn(mockFile);

        // Act
        ExtensionResponse response = pageArea.downloadAttachment(downloadLink, attachmentId);

        // Assert
        assertNotNull(response);
        assertEquals(ExtensionResponse.Result.SUCCESS, response.getResult());
        verify(restClient).doSyncGetBinary(eq(downloadLink));
    }

    @Test
    @DisplayName("Should reject path traversal attempts in download link")
    void testDownloadAttachment_PathTraversalAttempt() {
        // Arrange
        String maliciousLink = "/wiki/download/../../../etc/passwd";
        String attachmentId = "att-malicious";

        // Act
        ExtensionResponse response = pageArea.downloadAttachment(maliciousLink, attachmentId);

        // Assert
        assertNotNull(response);
        assertEquals(ExtensionResponse.Result.SUCCESS, response.getResult());
        assertEquals(false, response.getResponseValue().get("Success"));
        assertNotNull(response.getResponseValue().get("Error"));
    }

    @Test
    @DisplayName("Should handle HTTP error codes appropriately")
    void testDownloadAttachment_HttpErrors() {
        // Test 403 Forbidden
        String downloadLink = "/wiki/download/attachments/123456/restricted.pdf";
        String attachmentId = "att-restricted";

        when(binaryHttpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_FORBIDDEN);
        when(restClient.doSyncGetBinary(eq(downloadLink))).thenReturn(binaryHttpResponse);

        ExtensionResponse response = pageArea.downloadAttachment(downloadLink, attachmentId);

        assertNotNull(response);
        assertEquals(ExtensionResponse.Result.SUCCESS, response.getResult());
        assertEquals(false, response.getResponseValue().get("Success"));
        assertNotNull(response.getResponseValue().get("Error"));
    }

    @Test
    @DisplayName("Should handle empty file downloads")
    void testDownloadAttachment_EmptyFile() throws Exception {
        // Arrange
        String downloadLink = "/wiki/download/attachments/123456/empty.txt";
        String attachmentId = "att-empty";
        byte[] emptyContent = new byte[0];
        app.krista.model.base.File mockFile = new app.krista.model.base.File(1L, "empty.txt", "text/plain", "test-path");

        when(binaryHttpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(binaryHttpResponse.body()).thenReturn(emptyContent);
        when(binaryHttpResponse.headers()).thenReturn(createMockHeaders("text/plain", "empty.txt"));
        when(restClient.doSyncGetBinary(eq(downloadLink))).thenReturn(binaryHttpResponse);
        when(fileRepository.createNewFileByName(anyString())).thenReturn(fileHandle);
        when(fileHandle.getFile()).thenReturn(mockFile);

        // Act
        ExtensionResponse response = pageArea.downloadAttachment(downloadLink, attachmentId);

        // Assert
        assertNotNull(response);
        assertEquals(ExtensionResponse.Result.SUCCESS, response.getResult());
        verify(restClient).doSyncGetBinary(eq(downloadLink));
    }

    /**
     * Helper method to create mock HTTP headers
     */
    private java.net.http.HttpHeaders createMockHeaders(String contentType, String fileName) {
        Map<String, List<String>> headersMap = new HashMap<>();
        headersMap.put("Content-Type", List.of(contentType));
        headersMap.put("Content-Disposition", List.of("attachment; filename=\"" + fileName + "\""));

        return java.net.http.HttpHeaders.of(headersMap, (k, v) -> true);
    }
}
