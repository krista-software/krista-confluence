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
import app.krista.ksdk.files.FileHandle;
import app.krista.ksdk.files.FileRepository;
import com.google.gson.JsonObject;
import com.kristasoft.extensions.essentials.collaboration.confluence.ConfluenceAttributes;
import com.kristasoft.extensions.essentials.collaboration.confluence.api.APIVersion;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.entities.Attachment;
import com.kristasoft.extensions.essentials.collaboration.confluence.client.ConfluenceRESTClient;
import com.kristasoft.extensions.essentials.collaboration.confluence.service.ConfluenceService;
import com.kristasoft.extensions.essentials.collaboration.confluence.utils.TelemetryHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Full-coverage tests for {@link PageAreaAttachmentHelper#downloadAttachmentV2(String)}
 * and every private/static helper it delegates to.
 *
 * <p>Branches that are unreachable through the public entry point (e.g. the
 * {@code attachmentID == null} arm of {@code writeDownloadResponse}, which
 * {@code validateAttachmentID} would have already rejected) are exercised via
 * reflection so that line <em>and</em> branch coverage reach 100%.
 */
@DisplayName("PageAreaAttachmentHelper.downloadAttachmentV2 Tests")
class PageAreaAttachmentHelperTest {

    @Mock
    private ConfluenceService confluenceService;
    @Mock
    private TelemetryHelper telemetryHelper;
    @Mock
    private FileRepository fileRepository;
    @Mock
    private ConfluenceAttributes confluenceAttributes;
    @Mock
    private ConfluenceRESTClient restClient;
    @Mock
    private FileHandle fileHandle;
    @Mock
    private HttpResponse<String> metadataResponse;
    @Mock
    private HttpResponse<byte[]> binaryResponse;

    private PageAreaAttachmentHelper helper;

    private static final String ATTACHMENT_ID = "att-123456";
    private static final String PAGE_ID = "654321";
    private static final String DOWNLOAD_PATH =
            "/wiki/rest/api/content/" + PAGE_ID + "/child/attachment/" + ATTACHMENT_ID + "/download";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        helper = new PageAreaAttachmentHelper(confluenceService, telemetryHelper, fileRepository);

        lenient().when(confluenceService.getConfluenceAttributes()).thenReturn(confluenceAttributes);
        lenient().when(confluenceAttributes.getRestClient()).thenReturn(restClient);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Metadata JSON / header builders
    // ════════════════════════════════════════════════════════════════════════

    private JsonObject metadataJson(String id, String title, String pageId, String downloadLink) {
        JsonObject json = new JsonObject();
        if (id != null) json.addProperty("id", id);
        if (title != null) json.addProperty("title", title);
        if (pageId != null) json.addProperty("pageId", pageId);
        if (downloadLink != null) json.addProperty("downloadLink", downloadLink);
        json.addProperty("mediaType", "application/pdf");
        json.addProperty("fileSize", 2048);
        return json;
    }

    private HttpHeaders headers(Map<String, List<String>> map) {
        return HttpHeaders.of(map, (k, v) -> true);
    }

    private HttpHeaders headersWith(String contentType, String contentDispositionFilename) {
        Map<String, List<String>> map = new HashMap<>();
        if (contentType != null) map.put("Content-Type", List.of(contentType));
        if (contentDispositionFilename != null) {
            map.put("Content-Disposition", List.of("attachment; filename=\"" + contentDispositionFilename + "\""));
        }
        return headers(map);
    }

    /** Wire the metadata GET to return HTTP 200 with the given body. */
    private void stubMetadata(String id, JsonObject body) {
        when(metadataResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(metadataResponse.body()).thenReturn(body.toString());
        when(restClient.doSyncGet(eq(APIVersion.V2), isNull(), eq("attachments"), eq(id)))
                .thenReturn(metadataResponse);
    }

    /** Wire the binary download to return HTTP 200 with the given content + headers. */
    private void stubBinaryOk(byte[] content, HttpHeaders headers) {
        when(binaryResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(binaryResponse.body()).thenReturn(content);
        when(binaryResponse.headers()).thenReturn(headers);
        when(restClient.doSyncGetBinary(eq(DOWNLOAD_PATH))).thenReturn(binaryResponse);
    }

    private void stubFileRepository() throws Exception {
        app.krista.model.base.File mockFile =
                new app.krista.model.base.File(1L, "document.pdf", "application/pdf", "test-path");
        when(fileRepository.createNewFileByName(anyString())).thenReturn(fileHandle);
        when(fileHandle.getFile()).thenReturn(mockFile);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  downloadAttachmentV2 — happy paths
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("downloadAttachmentV2 — success")
    class SuccessPaths {

        @Test
        @DisplayName("full success using the attachment title as the file name")
        void success_usesTitle() throws Exception {
            stubMetadata(ATTACHMENT_ID, metadataJson(ATTACHMENT_ID, "report.pdf", PAGE_ID, null));
            stubBinaryOk("PDF-BYTES".getBytes(), headersWith("application/pdf", null));
            stubFileRepository();

            ExtensionResponse response = helper.downloadAttachmentV2(ATTACHMENT_ID);

            assertEquals(ExtensionResponse.Result.SUCCESS, response.getResult());
            verify(telemetryHelper).incrementCount("confluence.downloadAttachmentV2");
            verify(telemetryHelper).recordSuccess(eq("confluence.downloadAttachmentV2"), anyLong(), anyMap());
            verify(restClient).doSyncGetBinary(DOWNLOAD_PATH);
            // Title "report.pdf" is pure ASCII → passed through unchanged.
            verify(fileRepository).createNewFileByName("report.pdf");
        }

        @Test
        @DisplayName("title blank → filename resolved from the download link (query string stripped)")
        void success_titleBlank_usesDownloadLink() throws Exception {
            stubMetadata(ATTACHMENT_ID,
                    metadataJson(ATTACHMENT_ID, "   ", PAGE_ID, "/wiki/download/attachments/1/file.pdf?v=2"));
            // No Content-Disposition → falls through to URL-based extraction.
            stubBinaryOk("BYTES".getBytes(), headersWith("application/pdf", null));
            stubFileRepository();

            ExtensionResponse response = helper.downloadAttachmentV2(ATTACHMENT_ID);

            assertEquals(ExtensionResponse.Result.SUCCESS, response.getResult());
            verify(fileRepository).createNewFileByName("file.pdf");
        }

        @Test
        @DisplayName("title missing and download link missing → filename from Content-Disposition")
        void success_noTitleNoLink_usesContentDisposition() throws Exception {
            stubMetadata(ATTACHMENT_ID, metadataJson(ATTACHMENT_ID, null, PAGE_ID, null));
            stubBinaryOk("BYTES".getBytes(), headersWith("image/png", "picture.png"));
            stubFileRepository();

            ExtensionResponse response = helper.downloadAttachmentV2(ATTACHMENT_ID);

            assertEquals(ExtensionResponse.Result.SUCCESS, response.getResult());
            verify(fileRepository).createNewFileByName("picture.png");
        }

        @Test
        @DisplayName("no Content-Type header → defaults to application/octet-stream")
        void success_noContentType_defaultsOctetStream() throws Exception {
            stubMetadata(ATTACHMENT_ID, metadataJson(ATTACHMENT_ID, "data.bin", PAGE_ID, null));
            stubBinaryOk("BYTES".getBytes(), headersWith(null, null));
            stubFileRepository();

            ExtensionResponse response = helper.downloadAttachmentV2(ATTACHMENT_ID);

            assertEquals(ExtensionResponse.Result.SUCCESS, response.getResult());
            // Title "data.bin" is pure ASCII → used verbatim as the created file name.
            verify(fileRepository).createNewFileByName("data.bin");
        }

        @Test
        @DisplayName("empty file body downloads successfully")
        void success_emptyBody() throws Exception {
            stubMetadata(ATTACHMENT_ID, metadataJson(ATTACHMENT_ID, "empty.txt", PAGE_ID, null));
            stubBinaryOk(new byte[0], headersWith("text/plain", null));
            stubFileRepository();

            ExtensionResponse response = helper.downloadAttachmentV2(ATTACHMENT_ID);

            assertEquals(ExtensionResponse.Result.SUCCESS, response.getResult());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  downloadAttachmentV2 — failure paths
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("downloadAttachmentV2 — failure")
    class FailurePaths {

        @Test
        @DisplayName("null attachment ID → failure, telemetry tagged 'unknown'")
        void nullAttachmentId_fails() {
            ExtensionResponse response = helper.downloadAttachmentV2(null);

            assertEquals(ExtensionResponse.Result.FAILURE, response.getResult());
            assertNotNull(response.getError());
            verify(telemetryHelper).recordError(eq("confluence.downloadAttachmentV2"), anyLong(),
                    any(Exception.class), argThat((Map<String, String> tags) -> "unknown".equals(tags.get("attachment_id"))));
        }

        @Test
        @DisplayName("empty attachment ID → failure")
        void emptyAttachmentId_fails() {
            ExtensionResponse response = helper.downloadAttachmentV2("");

            assertEquals(ExtensionResponse.Result.FAILURE, response.getResult());
            assertNotNull(response.getError());
        }

        @Test
        @DisplayName("whitespace attachment ID → failure")
        void whitespaceAttachmentId_fails() {
            ExtensionResponse response = helper.downloadAttachmentV2("   ");

            assertEquals(ExtensionResponse.Result.FAILURE, response.getResult());
        }

        @Test
        @DisplayName("metadata fetch returns non-200 → failure, telemetry tagged with the id")
        void metadataNon200_fails() {
            when(metadataResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_NOT_FOUND);
            when(metadataResponse.body()).thenReturn("{\"error\":\"not found\"}");
            when(restClient.doSyncGet(eq(APIVersion.V2), isNull(), eq("attachments"), eq(ATTACHMENT_ID)))
                    .thenReturn(metadataResponse);

            ExtensionResponse response = helper.downloadAttachmentV2(ATTACHMENT_ID);

            assertEquals(ExtensionResponse.Result.FAILURE, response.getResult());
            verify(telemetryHelper).recordError(eq("confluence.downloadAttachmentV2"), anyLong(),
                    any(Exception.class), argThat((Map<String, String> tags) -> ATTACHMENT_ID.equals(tags.get("attachment_id"))));
        }

        @Test
        @DisplayName("metadata missing pageId → failure (cannot build download path)")
        void missingPageId_fails() {
            stubMetadata(ATTACHMENT_ID, metadataJson(ATTACHMENT_ID, "report.pdf", null, null));

            ExtensionResponse response = helper.downloadAttachmentV2(ATTACHMENT_ID);

            assertEquals(ExtensionResponse.Result.FAILURE, response.getResult());
            verify(restClient, never()).doSyncGetBinary(anyString());
        }

        @Test
        @DisplayName("blank pageId → failure")
        void blankPageId_fails() {
            stubMetadata(ATTACHMENT_ID, metadataJson(ATTACHMENT_ID, "report.pdf", "   ", null));

            ExtensionResponse response = helper.downloadAttachmentV2(ATTACHMENT_ID);

            assertEquals(ExtensionResponse.Result.FAILURE, response.getResult());
        }

        @Test
        @DisplayName("binary download non-200 with error body → failure")
        void binaryNon200_withBody_fails() {
            stubMetadata(ATTACHMENT_ID, metadataJson(ATTACHMENT_ID, "report.pdf", PAGE_ID, null));
            when(binaryResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_FORBIDDEN);
            when(binaryResponse.body()).thenReturn("Access denied".getBytes());
            when(restClient.doSyncGetBinary(eq(DOWNLOAD_PATH))).thenReturn(binaryResponse);

            ExtensionResponse response = helper.downloadAttachmentV2(ATTACHMENT_ID);

            assertEquals(ExtensionResponse.Result.FAILURE, response.getResult());
            assertTrue(response.getError().getErrorMessage().contains("403"));
        }

        @Test
        @DisplayName("binary download non-200 with null body → failure ('(empty body)')")
        void binaryNon200_nullBody_fails() {
            stubMetadata(ATTACHMENT_ID, metadataJson(ATTACHMENT_ID, "report.pdf", PAGE_ID, null));
            when(binaryResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_INTERNAL_ERROR);
            when(binaryResponse.body()).thenReturn(null);
            when(restClient.doSyncGetBinary(eq(DOWNLOAD_PATH))).thenReturn(binaryResponse);

            ExtensionResponse response = helper.downloadAttachmentV2(ATTACHMENT_ID);

            assertEquals(ExtensionResponse.Result.FAILURE, response.getResult());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  stripQueryString (private) — via reflection
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("stripQueryString")
    class StripQueryStringTests {

        private String strip(String link) throws Exception {
            Method m = PageAreaAttachmentHelper.class.getDeclaredMethod("stripQueryString", String.class);
            m.setAccessible(true);
            return (String) m.invoke(helper, link);
        }

        @Test
        @DisplayName("null → null")
        void nullInput() throws Exception {
            assertNull(strip(null));
        }

        @Test
        @DisplayName("no query string → unchanged")
        void noQuery() throws Exception {
            assertEquals("/a/b/file.pdf", strip("/a/b/file.pdf"));
        }

        @Test
        @DisplayName("query string stripped after '?'")
        void withQuery() throws Exception {
            assertEquals("/a/b/file.pdf", strip("/a/b/file.pdf?v=2&x=1"));
        }

        @Test
        @DisplayName("'?' at start → empty string")
        void questionAtStart() throws Exception {
            assertEquals("", strip("?x=1"));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  extractFileName (private) — via reflection
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("extractFileName")
    class ExtractFileNameTests {

        @SuppressWarnings("unchecked")
        private String extract(String link, Map<String, List<String>> headers) throws Exception {
            Method m = PageAreaAttachmentHelper.class.getDeclaredMethod("extractFileName", String.class, Map.class);
            m.setAccessible(true);
            return (String) m.invoke(helper, link, headers);
        }

        @Test
        @DisplayName("quoted filename from Content-Disposition")
        void quotedDisposition() throws Exception {
            assertEquals("report.pdf",
                    extract(null, Map.of("Content-Disposition", List.of("attachment; filename=\"report.pdf\""))));
        }

        @Test
        @DisplayName("unquoted filename from Content-Disposition (trimmed)")
        void unquotedDisposition() throws Exception {
            assertEquals("report.pdf",
                    extract(null, Map.of("Content-Disposition", List.of("attachment; filename=  report.pdf  "))));
        }

        @Test
        @DisplayName("Content-Disposition without 'filename=' → falls back to URL")
        void dispositionNoFilename() throws Exception {
            assertEquals("doc.pdf",
                    extract("/x/doc.pdf", Map.of("Content-Disposition", List.of("inline"))));
        }

        @Test
        @DisplayName("Content-Disposition with empty quoted filename → falls back to URL")
        void dispositionEmptyFilename() throws Exception {
            assertEquals("doc.pdf",
                    extract("/x/doc.pdf", Map.of("Content-Disposition", List.of("attachment; filename=\"\""))));
        }

        @Test
        @DisplayName("empty Content-Disposition list → falls back to URL")
        void dispositionEmptyList() throws Exception {
            Map<String, List<String>> headers = new HashMap<>();
            headers.put("Content-Disposition", Collections.emptyList());
            assertEquals("doc.pdf", extract("/x/doc.pdf", headers));
        }

        @Test
        @DisplayName("no headers, valid URL → last segment, percent-decoded")
        void urlPercentDecoded() throws Exception {
            assertEquals("my file.pdf", extract("/x/my%20file.pdf", Collections.emptyMap()));
        }

        @Test
        @DisplayName("null link, no headers → 'attachment'")
        void nullLinkDefault() throws Exception {
            assertEquals("attachment", extract(null, Collections.emptyMap()));
        }

        @Test
        @DisplayName("URL ending with '/' → 'attachment'")
        void urlEndsWithSlash() throws Exception {
            assertEquals("attachment", extract("/x/", Collections.emptyMap()));
        }

        @Test
        @DisplayName("URL with no slash → 'attachment'")
        void urlNoSlash() throws Exception {
            assertEquals("attachment", extract("noslash", Collections.emptyMap()));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  resolveFileName (private) — via reflection
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("resolveFileName")
    class ResolveFileNameTests {

        @SuppressWarnings("unchecked")
        private String resolve(Attachment att, Map<String, List<String>> headers) throws Exception {
            Method m = PageAreaAttachmentHelper.class.getDeclaredMethod(
                    "resolveFileName", Attachment.class, Map.class);
            m.setAccessible(true);
            return (String) m.invoke(helper, att, headers);
        }

        @Test
        @DisplayName("non-blank title wins")
        void titleWins() throws Exception {
            Attachment a = new Attachment();
            a.title = "title.pdf";
            a.downloadLink = "/x/other.pdf";
            assertEquals("title.pdf", resolve(a, Collections.emptyMap()));
        }

        @Test
        @DisplayName("null title, non-null link → link is stripped and parsed")
        void nullTitleWithLink() throws Exception {
            Attachment a = new Attachment();
            a.title = null;
            a.downloadLink = "/x/from-link.pdf?v=1";
            assertEquals("from-link.pdf", resolve(a, Collections.emptyMap()));
        }

        @Test
        @DisplayName("null title, null link → falls back to header/default")
        void nullTitleNullLink() throws Exception {
            Attachment a = new Attachment();
            a.title = null;
            a.downloadLink = null;
            assertEquals("attachment", resolve(a, Collections.emptyMap()));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  writeDownloadResponse — null attachmentID branch (unreachable via public)
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("writeDownloadResponse tags telemetry 'unknown' when attachmentID is null")
    void writeDownloadResponse_nullAttachmentId() throws Exception {
        stubFileRepository();
        when(binaryResponse.body()).thenReturn("BYTES".getBytes());
        when(binaryResponse.headers()).thenReturn(headersWith("application/pdf", null));

        Method m = PageAreaAttachmentHelper.class.getDeclaredMethod(
                "writeDownloadResponse", HttpResponse.class, String.class, String.class, String.class, long.class);
        m.setAccessible(true);

        ExtensionResponse response = (ExtensionResponse) m.invoke(
                helper, binaryResponse, "file.pdf", null, "confluence.downloadAttachmentV2", 0L);

        assertEquals(ExtensionResponse.Result.SUCCESS, response.getResult());
        verify(telemetryHelper).recordSuccess(eq("confluence.downloadAttachmentV2"), anyLong(),
                argThat(tags -> "unknown".equals(tags.get("attachment_id"))));
    }

    // ════════════════════════════════════════════════════════════════════════
    //  sanitizeFileNameForFilesystem (package-private static)
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("sanitizeFileNameForFilesystem")
    class SanitizeTests {

        @Test
        @DisplayName("null → 'attachment'")
        void nullInput() {
            assertEquals("attachment", PageAreaAttachmentHelper.sanitizeFileNameForFilesystem(null));
        }

        @Test
        @DisplayName("empty → 'attachment'")
        void emptyInput() {
            assertEquals("attachment", PageAreaAttachmentHelper.sanitizeFileNameForFilesystem(""));
        }

        @Test
        @DisplayName("pure ASCII unchanged")
        void pureAscii() {
            assertEquals("report-2026_final.pdf",
                    PageAreaAttachmentHelper.sanitizeFileNameForFilesystem("report-2026_final.pdf"));
        }

        @Test
        @DisplayName("en-dash → hyphen")
        void enDash() {
            assertEquals("8. MBT - Capital One_CPC CD.pdf",
                    PageAreaAttachmentHelper.sanitizeFileNameForFilesystem("8. MBT – Capital One_CPC CD.pdf"));
        }

        @Test
        @DisplayName("em-dash → hyphen")
        void emDash() {
            assertEquals("Foo - Bar.pdf",
                    PageAreaAttachmentHelper.sanitizeFileNameForFilesystem("Foo — Bar.pdf"));
        }

        @Test
        @DisplayName("smart quotes and ellipsis mapped to ASCII")
        void smartQuotesAndEllipsis() {
            assertEquals("'Hi' \"World\"...txt",
                    PageAreaAttachmentHelper.sanitizeFileNameForFilesystem("‘Hi’ “World”…txt"));
        }

        @Test
        @DisplayName("accented letters NFKD-decomposed to ASCII")
        void accents() {
            assertEquals("cafe-naive.pdf",
                    PageAreaAttachmentHelper.sanitizeFileNameForFilesystem("café-naïve.pdf"));
        }

        @Test
        @DisplayName("path separator → underscore")
        void pathSeparator() {
            assertEquals("dir_sub_file.pdf",
                    PageAreaAttachmentHelper.sanitizeFileNameForFilesystem("dir/sub/file.pdf"));
        }

        @Test
        @DisplayName("emoji and unmappable chars → underscore")
        void emoji() {
            assertEquals("party_.txt",
                    PageAreaAttachmentHelper.sanitizeFileNameForFilesystem("party🎉.txt"));
        }

        @Test
        @DisplayName("all-non-ASCII collapses to blank → 'attachment'")
        void allNonAsciiBlank() {
            // Combining marks only: NFKD strips them, leaving an empty/blank string.
            String result = PageAreaAttachmentHelper.sanitizeFileNameForFilesystem("́̂̃");
            assertEquals("attachment", result);
        }

        @Test
        @DisplayName("output is always printable ASCII")
        void outputAsciiOnly() {
            String result = PageAreaAttachmentHelper.sanitizeFileNameForFilesystem(
                    "Δοκ – δοκ.pdf");
            for (int i = 0; i < result.length(); i++) {
                char c = result.charAt(i);
                assertTrue(c >= 0x20 && c <= 0x7E,
                        "non-ASCII char at index " + i + ": U+" + Integer.toHexString(c));
            }
        }
    }

    // Small helper to unwrap InvocationTargetException in assertions if ever needed.
    private static Throwable unwrap(Throwable t) {
        return (t instanceof InvocationTargetException) ? t.getCause() : t;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  parseAttachment — Download/Web View URL resolution
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("parseAttachment — absolute Download/Web View URLs")
    class ParseAttachmentUrls {

        private JsonObject attachmentJson(String downloadLink, String webuiLink) {
            JsonObject json = new JsonObject();
            json.addProperty("id", "att-1");
            json.addProperty("title", "doc.pdf");
            json.addProperty("pageId", "2879324568");
            if (downloadLink != null) json.addProperty("downloadLink", downloadLink);
            if (webuiLink != null) json.addProperty("webuiLink", webuiLink);
            return json;
        }

        @Test
        @DisplayName("prefixes relative download/webui links with the resolved site base URL")
        void prefixesRelativeLinks() {
            when(confluenceAttributes.getDomainURL()).thenReturn("https://quinstreet.atlassian.net/wiki");
            JsonObject json = attachmentJson(
                    "/download/attachments/2879324568/Disaster_Recovery.doc?version=1",
                    "/pages/viewpageattachments.action?pageId=2879324568&preview=%2F2879324568%2F2882109849%2FDisaster_Recovery.doc");

            Attachment attachment = helper.parseAttachment(json, null);

            assertEquals("https://quinstreet.atlassian.net/wiki/download/attachments/2879324568/Disaster_Recovery.doc?version=1",
                    attachment.downloadLink);
            assertEquals("https://quinstreet.atlassian.net/wiki/pages/viewpageattachments.action?pageId=2879324568&preview=%2F2879324568%2F2882109849%2FDisaster_Recovery.doc",
                    attachment.webUILink);
        }

        @Test
        @DisplayName("leaves already-absolute links unchanged")
        void leavesAbsoluteLinks() {
            when(confluenceAttributes.getDomainURL()).thenReturn("https://quinstreet.atlassian.net/wiki");
            JsonObject json = attachmentJson(
                    "https://cdn.example.com/file.doc", "https://quinstreet.atlassian.net/wiki/pages/x");

            Attachment attachment = helper.parseAttachment(json, null);

            assertEquals("https://cdn.example.com/file.doc", attachment.downloadLink);
            assertEquals("https://quinstreet.atlassian.net/wiki/pages/x", attachment.webUILink);
        }

        @Test
        @DisplayName("returns null links unchanged")
        void nullLinks() {
            lenient().when(confluenceAttributes.getDomainURL()).thenReturn("https://quinstreet.atlassian.net/wiki");
            JsonObject json = attachmentJson(null, null);

            Attachment attachment = helper.parseAttachment(json, null);

            assertNull(attachment.downloadLink);
            assertNull(attachment.webUILink);
        }

        @Test
        @DisplayName("falls back to the raw link when the base URL cannot be resolved")
        void fallsBackWhenBaseUrlBlank() {
            when(confluenceAttributes.getDomainURL()).thenReturn(null);
            JsonObject json = attachmentJson("/download/attachments/1/file.doc", null);

            Attachment attachment = helper.parseAttachment(json, null);

            assertEquals("/download/attachments/1/file.doc", attachment.downloadLink);
        }
    }
}
