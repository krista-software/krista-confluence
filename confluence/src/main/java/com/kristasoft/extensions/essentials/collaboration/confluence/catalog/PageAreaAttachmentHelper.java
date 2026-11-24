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
import app.krista.extension.executor.ExtensionResponseBuilder;
import app.krista.ksdk.files.FileHandle;
import app.krista.ksdk.files.FileRepository;
import app.krista.model.base.File;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kristasoft.extensions.essentials.collaboration.confluence.api.APIVersion;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.entities.Attachment;
import com.kristasoft.extensions.essentials.collaboration.confluence.service.ConfluenceService;
import com.kristasoft.extensions.essentials.collaboration.confluence.utils.Constants;
import com.kristasoft.extensions.essentials.collaboration.confluence.utils.GsonFactory;
import com.kristasoft.extensions.essentials.collaboration.confluence.utils.TelemetryHelper;
import com.kristasoft.extensions.essentials.collaboration.confluence.utils.UrlParser;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URLDecoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.kristasoft.extensions.essentials.collaboration.confluence.utils.TelemetryHelper.safeTagMap;

/**
 * Helper class for handling attachment-related operations in PageArea.
 * Encapsulates logic for retrieving, parsing, and paginating through attachments.
 */
@Service
public class PageAreaAttachmentHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(PageAreaAttachmentHelper.class);
    private static final int DEFAULT_PAGINATION_LIMIT = 250; // Max allowed by Confluence API

    private final ConfluenceService service;
    private final TelemetryHelper telemetryHelper;
    private final FileRepository fileRepository;

    @Inject
    public PageAreaAttachmentHelper(ConfluenceService service,
                                    TelemetryHelper telemetryHelper,
                                    FileRepository fileRepository) {
        this.service = service;
        this.telemetryHelper = telemetryHelper;
        this.fileRepository = fileRepository;
    }

    public List<Attachment> retrieveAllAttachments(String pageID, String status, String mediaType) {
        List<Attachment> allAttachments = new ArrayList<>();
        String nextUrl = null;
        int pageCount = 0;

        LOGGER.debug("→ retrieveAllAttachments: Starting pagination for page ID: {}", pageID);

        do {
            pageCount++;
            LOGGER.debug("→ retrieveAllAttachments: Fetching page {}", pageCount);

            HttpResponse<String> response = fetchAttachmentsPage(pageID, status, mediaType, nextUrl);
            JsonObject responseBody = handleResponse(response, pageID);

            List<Attachment> pageAttachments = parseAttachments(responseBody, pageID);
            allAttachments.addAll(pageAttachments);

            logPageResult(pageAttachments, allAttachments.size());

            nextUrl = extractNextUrl(responseBody);
            logPaginationState(nextUrl);

        } while (nextUrl != null);

        LOGGER.info(
                "→ retrieveAllAttachments: Completed pagination. Total pages fetched: {}, Total attachments: {}",
                pageCount, allAttachments.size()
        );

        return allAttachments;
    }

    private HttpResponse<String> fetchAttachmentsPage(
            String pageID,
            String status,
            String mediaType,
            String nextUrl
    ) {
        Map<String, String> queryParams = buildQueryParams(status, mediaType);

        if (nextUrl == null) {
            LOGGER.debug("→ fetchAttachmentsPage: First request with query params: {}", queryParams);

            return service.getConfluenceAttributes()
                    .getRestClient()
                    .doSyncGet(APIVersion.V2, queryParams, "pages", pageID, "attachments");
        }

        LOGGER.debug("→ fetchAttachmentsPage: Following pagination cursor: {}", nextUrl);
        UrlParser.ParsedUrl parsedUrl = UrlParser.parseNextUrl(nextUrl);

        // Confluence's cursor-based "next" link only carries the cursor (and sometimes
        // limit) - it never echoes back the original status/mediaType filters. Without
        // re-merging them here, every page after the first returns unfiltered results
        // (all statuses/versions), silently inflating the attachment count.
        Map<String, String> mergedParams = new LinkedHashMap<>(queryParams);
        if (parsedUrl.getQueryParams() != null) {
            mergedParams.putAll(parsedUrl.getQueryParams());
        }
        LOGGER.debug("→ fetchAttachmentsPage: Cursor request with merged query params: {}", mergedParams);

        return service.getConfluenceAttributes()
                .getRestClient()
                .doSyncGet(
                        APIVersion.V2,
                        mergedParams,
                        parsedUrl.getPathComponents()
                );
    }

    private JsonObject handleResponse(HttpResponse<String> response, String pageID) {
        if (response.statusCode() != HttpURLConnection.HTTP_OK) {
            LOGGER.error(
                    "→ handleResponse: Failed to retrieve attachments. Status: {}, Response: {}",
                    response.statusCode(), response.body()
            );
            throw new RuntimeException(
                    "Failed to retrieve attachments for page: " + pageID +
                            ". Status: " + response.statusCode() +
                            ", Response: " + response.body()
            );
        }

        return GsonFactory.getInstance()
                .fromJson(response.body(), JsonObject.class);
    }

    private void logPageResult(List<Attachment> pageAttachments, int totalSoFar) {
        LOGGER.debug(
                "→ retrieveAllAttachments: Retrieved {} attachments in this page, total so far: {}",
                pageAttachments.size(),
                totalSoFar
        );
    }

    private void logPaginationState(String nextUrl) {
        if (nextUrl != null) {
            LOGGER.debug("→ retrieveAllAttachments: More pages available, continuing pagination");
        } else {
            LOGGER.debug("→ retrieveAllAttachments: No more pages, pagination complete");
        }
    }


    /**
     * Builds query parameters for the attachment API request.
     *
     * @param status    Filter by status: "current", "archived", or "any"
     * @param mediaType Optional MIME type filter
     * @return Map of query parameters
     */
    private Map<String, String> buildQueryParams(String status, String mediaType) {
        Map<String, String> queryParams = new LinkedHashMap<>();
        if (status != null && !"any".equalsIgnoreCase(status)) {
            queryParams.put("status", status);
        }
        if (mediaType != null && !mediaType.trim().isEmpty()) {
            queryParams.put("mediaType", mediaType);
        }
        queryParams.put("lim" +
                "it", String.valueOf(DEFAULT_PAGINATION_LIMIT));
        return queryParams;
    }

    /**
     * Parses attachment data from the API response.
     *
     * @param responseBody The JSON response from the API
     * @param pageID       The page ID (used as fallback if not in response)
     * @return List of parsed Attachment entities
     */
    private List<Attachment> parseAttachments(JsonObject responseBody, String pageID) {
        List<Attachment> attachments = new ArrayList<>();

        if (!responseBody.has("results")) {
            LOGGER.debug("→ parseAttachments: No 'results' field in response");
            return attachments;
        }

        JsonArray results = responseBody.getAsJsonArray("results");
        LOGGER.debug("→ parseAttachments: Parsing {} attachment results", results.size());

        for (JsonElement element : results) {
            JsonObject att = element.getAsJsonObject();
            Attachment attachment = parseAttachment(att, pageID);
            attachments.add(attachment);
        }

        return attachments;
    }

    /**
     * Parses a single attachment from JSON.
     *
     * @param att    The JSON object representing an attachment
     * @param pageID The page ID (used as fallback if not in response)
     * @return Parsed Attachment entity
     */
    public Attachment parseAttachment(JsonObject att, String pageID) {
        Attachment attachment = new Attachment();

        attachment.id = att.has("id") ? att.get("id").getAsString() : null;
        attachment.title = att.has("title") ? att.get("title").getAsString() : null;
        attachment.pageId = att.has("pageId") ? att.get("pageId").getAsString() : pageID;
        attachment.mediaType = att.has("mediaType") ? att.get("mediaType").getAsString() : null;
        attachment.fileSize = att.has("fileSize") ? att.get("fileSize").getAsDouble() : 0.0;
        attachment.downloadLink = toAbsoluteUrl(att.has("downloadLink") ? att.get("downloadLink").getAsString() : null);
        attachment.webUILink = toAbsoluteUrl(att.has("webuiLink") ? att.get("webuiLink").getAsString() : null);
        attachment.createdAt = att.has("createdAt") ? parseDateToLong(att.get("createdAt").getAsString()) : null;
        attachment.status = att.has("status") ? att.get("status").getAsString() : null;

        // Parse version information - critical for staleness detection
        if (att.has("version")) {
            JsonObject version = att.getAsJsonObject("version");
            attachment.versionNumber = version.has("number") ? version.get("number").getAsDouble() : 0.0;
            attachment.versionCreatedAt = version.has("createdAt") ? parseDateToLong(version.get("createdAt").getAsString()) : null;
        } else {
            attachment.versionNumber = 0.0;
            attachment.versionCreatedAt = null;
        }

        return attachment;
    }

    /**
     * Turns a Confluence attachment link into a fully-qualified, working URL.
     *
     * <p>The v2 API returns {@code downloadLink}/{@code webuiLink} as paths relative
     * to the site's {@code /wiki} context (e.g. {@code /download/attachments/...} and
     * {@code /pages/viewpageattachments.action?...}). On their own they 404 in a
     * browser, so they are prefixed with the resolved site base URL
     * (e.g. {@code https://acme.atlassian.net/wiki}). Values that are already
     * absolute, null, or blank are returned unchanged.</p>
     *
     * @param link the raw link from the attachment response
     * @return an absolute URL, or the original value when it cannot be qualified
     */
    private String toAbsoluteUrl(String link) {
        if (link == null || link.isBlank()) {
            return link;
        }
        if (link.startsWith("http://") || link.startsWith("https://")) {
            return link;
        }
        String baseUrl = service.getConfluenceAttributes().getDomainURL();
        if (baseUrl == null || baseUrl.isBlank()) {
            return link;
        }
        String path = link.startsWith("/") ? link : "/" + link;
        return baseUrl + path;
    }

    /**
     * Parses an ISO 8601 instant string (e.g. "2020-04-28T14:36:57Z", with or without
     * fractional seconds) to Long (epoch milliseconds).
     *
     * <p>Uses {@link Instant#parse(CharSequence)}, which accepts the trailing "Z" zone
     * designator and is thread-safe — unlike a shared {@link java.text.SimpleDateFormat}.
     *
     * @param dateString The ISO 8601 instant string
     * @return The epoch milliseconds, or null if the value is empty or cannot be parsed
     */
    private Long parseDateToLong(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return null;
        }
        try {
            return Instant.parse(dateString).toEpochMilli();
        } catch (DateTimeParseException e) {
            LOGGER.warn("→ parseDateToLong: Failed to parse date: {} — leaving date field null", dateString);
            return null;
        }
    }

    /**
     * Extracts the next URL from the _links section of the response.
     *
     * @param responseBody The JSON response from the API
     * @return The next URL if available, null otherwise
     */
    private String extractNextUrl(JsonObject responseBody) {
        if (responseBody.has("_links")) {
            JsonObject links = responseBody.getAsJsonObject("_links");
            if (links.has("next")) {
                String nextUrl = links.get("next").getAsString();
                LOGGER.debug("→ extractNextUrl: Found next URL: {}", nextUrl);
                return nextUrl;
            }
        }
        LOGGER.debug("→ extractNextUrl: No next URL found");
        return null;
    }

    // ==================== Download Attachment (v2) ====================

    /**
     * Downloads an attachment's binary content by its ID using the Confluence v2 API.
     * The attachment metadata is fetched first to resolve a fresh download link, then
     * the file is retrieved for ingestion into Document Sets.
     *
     * @param attachmentID The ID of the attachment to download
     * @return ExtensionResponse containing the file, metadata, and success flag
     */
    public ExtensionResponse downloadAttachmentV2(String attachmentID) {
        long startTime = System.currentTimeMillis();
        String methodName = "downloadAttachmentV2";
        String metricName = "confluence.downloadAttachmentV2";
        LOGGER.info("→ {}: Starting operation with attachmentID: {}", methodName, attachmentID);

        try {
            telemetryHelper.incrementCount(metricName);
            validateAttachmentID(attachmentID);

            Attachment attachment = fetchAttachmentMetadata(attachmentID);

            // Use the v1 REST API download endpoint which accepts API-token Basic Auth.
            // The legacy downloadLink from the v2 metadata response uses the /wiki/download/
            // path which requires cookie/session auth and returns 401 for API tokens.
            HttpResponse<byte[]> binaryResponse = downloadBinaryContent(attachment);
            String fileName = resolveFileName(attachment, binaryResponse.headers().map());
            return writeDownloadResponse(binaryResponse, fileName, attachmentID, metricName, startTime);
        } catch (Exception cause) {
            return buildDownloadSystemErrorResponse(cause, buildDownloadErrorMessage(cause), metricName, startTime,
                    safeTagMap("attachment_id", attachmentID != null ? attachmentID : "unknown"), methodName);
        }
    }

    private void validateAttachmentID(String attachmentID) {
        if (attachmentID == null || attachmentID.trim().isEmpty()) {
            LOGGER.error("→ downloadAttachmentV2: Attachment ID is required");
            throw new IllegalArgumentException("""
                    Attachment ID is required to download the attachment. \
                    Please provide a valid attachment ID from 'Get Attachments for Page' or 'Get Attachment by ID'.
                    """);
        }
    }

    private Attachment fetchAttachmentMetadata(String attachmentID) {
        LOGGER.info("→ fetchAttachmentMetadata: Fetching metadata for attachmentID='{}'", attachmentID);
        HttpResponse<String> metadataResponse = service.getConfluenceAttributes().getRestClient()
                .doSyncGet(APIVersion.V2, null, "attachments", attachmentID);

        LOGGER.info("→ fetchAttachmentMetadata: Metadata response status={}", metadataResponse.statusCode());
        if (metadataResponse.statusCode() != HttpURLConnection.HTTP_OK) {
            LOGGER.error("→ fetchAttachmentMetadata: Failed to retrieve attachment metadata. Status: {}, Body: {}",
                    metadataResponse.statusCode(), metadataResponse.body());
            throw new RuntimeException(String.format(
                    "Failed to retrieve attachment metadata for ID '%s'. Status: %d, Response: %s",
                    attachmentID, metadataResponse.statusCode(), metadataResponse.body()));
        }

        JsonObject responseBody = GsonFactory.getInstance().fromJson(metadataResponse.body(), JsonObject.class);
        Attachment attachment = parseAttachment(responseBody, null);
        LOGGER.info("→ fetchAttachmentMetadata: Parsed attachment — id='{}', title='{}', pageId='{}', mediaType='{}', fileSize={}",
                attachment.id, attachment.title, attachment.pageId, attachment.mediaType, attachment.fileSize);
        return attachment;
    }

    private HttpResponse<byte[]> downloadBinaryContent(Attachment attachment) {
        String attachmentId = attachment.id;
        String pageId = attachment.pageId;

        if (pageId == null || pageId.trim().isEmpty()) {
            LOGGER.error("→ downloadBinaryContent: Missing pageId for attachment '{}' — cannot construct download path", attachmentId);
            throw new RuntimeException(
                    "Cannot download attachment '" + attachmentId + "': the attachment metadata did not include a page ID. " +
                            "Please verify the attachment exists and belongs to an accessible page.");
        }

        // v1 REST API child-attachment download endpoint — the only Confluence Cloud path
        // that accepts API-token Basic Auth for binary attachment downloads.
        // • /wiki/download/attachments/...           → 401 (session/cookie auth only)
        // • /wiki/api/v2/attachments/{id}/download   → 404 (endpoint does not exist)
        // • /wiki/rest/api/content/{id}/download     → 404 (endpoint retired in Cloud)
        // Returns a 302 redirect to the actual file; the redirect is followed automatically
        // by doSyncGetBinary with the Authorization header re-sent on each same-host hop.
        String downloadPath = "/wiki/rest/api/content/" + pageId + "/child/attachment/" + attachmentId + "/download";
        LOGGER.info("→ downloadBinaryContent: attachmentID='{}', pageID='{}', download path='{}'",
                attachmentId, pageId, downloadPath);

        HttpResponse<byte[]> response = service.getConfluenceAttributes().getRestClient()
                .doSyncGetBinary(downloadPath);

        int status = response.statusCode();
        LOGGER.info("→ downloadBinaryContent: Response status={}, body length={} bytes",
                status, response.body() != null ? response.body().length : -1);

        if (status != HttpURLConnection.HTTP_OK) {
            String errorBody = response.body() != null
                    ? new String(response.body(), StandardCharsets.UTF_8)
                    : "(empty body)";
            LOGGER.error("→ downloadBinaryContent: Download failed. status={}, path='{}', errorBody='{}'",
                    status, downloadPath, errorBody);
            throw new RuntimeException(String.format(
                    "The Confluence server returned an error (status code: %d) while downloading the attachment. " +
                            "path used: '%s'. Server response: %s",
                    status, downloadPath, errorBody));
        }

        return response;
    }

    private String resolveFileName(Attachment attachment, Map<String, List<String>> responseHeaders) {
        if (attachment.title != null && !attachment.title.trim().isEmpty()) {
            return attachment.title;
        }
        // Fall back to the download link (strip query string) or Content-Disposition header.
        // downloadLink may be null when the v2 metadata response omits it.
        String linkWithoutQuery = attachment.downloadLink != null
                ? stripQueryString(attachment.downloadLink)
                : null;
        return extractFileName(linkWithoutQuery, responseHeaders);
    }

    private String buildDownloadErrorMessage(Exception cause) {
        return String.format(Constants.DOWNLOAD_ATTACHMENT_V2_ERROR_MESSAGE, cause.getMessage());
    }

    private String stripQueryString(String link) {
        if (link == null) {
            return null;
        }
        int queryStart = link.indexOf('?');
        return queryStart >= 0 ? link.substring(0, queryStart) : link;
    }

    private ExtensionResponse writeDownloadResponse(HttpResponse<byte[]> response, String fileName,
                                                    String attachmentID, String metricName, long startTime) throws IOException {
        byte[] content = response.body();
        String contentType = response.headers().firstValue("Content-Type").orElse("application/octet-stream");
        LOGGER.info("→ writeDownloadResponse: Downloaded {} bytes, contentType='{}', fileName='{}'",
                content.length, contentType, fileName);

        // Sanitize before the MediaStore turns the name into a temp Path — see
        // sanitizeFileNameForFilesystem for the why (sun.jnu.encoding can't represent non-ASCII).
        String safeFileName = sanitizeFileNameForFilesystem(fileName);

        // Write directly via ByteArrayInputStream — no temp file needed.
        // Same pattern as SharePoint KristaMediaClient.toKristaFile().
        File kristaFile;
        try (final FileHandle fileHandle = fileRepository.createNewFileByName(safeFileName)) {
            fileHandle.setContent(new ByteArrayInputStream(content));
            kristaFile = fileHandle.getFile();
        }
        LOGGER.info("→ writeDownloadResponse: Krista file created successfully — fileName='{}'", safeFileName);

        Map<String, Object> result = Map.of(
                "File", kristaFile,
                "File Name", safeFileName,
                "File Size", (double) content.length,
                "Content Type", contentType,
                "Success", true
        );

        telemetryHelper.recordSuccess(metricName, startTime,
                safeTagMap("attachment_id", attachmentID != null ? attachmentID : "unknown",
                        "file_size", String.valueOf(content.length),
                        "content_type", contentType));

        return new ExtensionResponseBuilder().success(result).build();
    }

    private String extractFileName(String downloadLink, Map<String, List<String>> headers) {
        List<String> contentDisposition = headers.get("Content-Disposition");
        if (contentDisposition != null && !contentDisposition.isEmpty()) {
            String disposition = contentDisposition.get(0);
            if (disposition.contains("filename=")) {
                String fileName = disposition
                        .substring(disposition.indexOf("filename=") + 9)
                        .replace("\"", "")
                        .trim();
                if (!fileName.isEmpty()) {
                    LOGGER.debug("→ extractFileName: Extracted from Content-Disposition: {}", fileName);
                    return fileName;
                }
            }
        }

        if (downloadLink == null) {
            LOGGER.debug("→ extractFileName: No download link available, using default filename");
            return "attachment";
        }

        int lastSlash = downloadLink.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < downloadLink.length() - 1) {
            String fileName = URLDecoder.decode(
                    downloadLink.substring(lastSlash + 1),
                    StandardCharsets.UTF_8);
            LOGGER.debug("→ extractFileName: Extracted from URL: {}", fileName);
            return fileName;
        }

        LOGGER.debug("→ extractFileName: Using default filename");
        return "attachment";
    }

    private ExtensionResponse buildDownloadSystemErrorResponse(
            Exception cause,
            String errorMessagePrefix,
            String metricName,
            long startTime,
            Map<String, String> telemetryTags,
            String methodName) {

        LOGGER.error("→ {}: Exception occurred - {}", methodName, cause.getMessage(), cause);
        telemetryHelper.recordError(metricName, startTime, cause, telemetryTags);

        ExtensionResponse.Error error = new ExtensionResponse.Error(
                errorMessagePrefix + cause.getMessage(),
                System.currentTimeMillis(),
                ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR,
                ""
        );

        return new ExtensionResponseBuilder().failure(error).build();
    }

    /**
     * Sanitize an attachment filename for safe use on the runtime's filesystem.
     *
     * <p>The container's JVM can have {@code sun.jnu.encoding} set to a non-UTF-8 codepage
     * (commonly when {@code LANG=C} or {@code POSIX}), in which case {@link java.nio.file.Path#of}
     * throws {@link java.nio.file.InvalidPathException} the moment the path contains a char
     * the encoding can't represent. The Krista MediaStore builds a temp Path from this name,
     * so any non-ASCII Confluence title (en-dash, smart quotes, accented letters, emoji, …)
     * takes down the whole request <em>after</em> the bytes have already been downloaded.
     *
     * <p>Strategy: map common Unicode punctuation lookalikes to their ASCII equivalents so the
     * result stays readable, NFKD-decompose so accented letters split into base + combining
     * mark (then strip the marks), and replace anything still outside printable ASCII with
     * {@code _}. The forward-slash is also replaced — it would be interpreted as a path
     * separator. Null/blank input falls back to {@code "attachment"}.
     *
     * <p>Shared by v1 {@code PageArea.writeDownloadResponse} and v2
     * {@code PageAreaAttachmentHelper.writeDownloadResponse} so both download paths get
     * the same protection.
     */
    static String sanitizeFileNameForFilesystem(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "attachment";
        }
        // Common Unicode punctuation → ASCII equivalents. Keeps the name readable rather
        // than littering it with underscores for chars the user would recognize.
        String mapped = fileName
                .replace('–', '-')   // en dash
                .replace('—', '-')   // em dash
                .replace('‐', '-')   // hyphen
                .replace('‑', '-')   // non-breaking hyphen
                .replace('‒', '-')   // figure dash
                .replace('−', '-')   // minus sign
                .replace('‘', '\'')  // left single quote
                .replace('’', '\'')  // right single quote
                .replace('‚', '\'')  // single low-9 quote
                .replace('“', '"')   // left double quote
                .replace('”', '"')   // right double quote
                .replace('„', '"')   // double low-9 quote
                .replace("…", "...") // horizontal ellipsis
                .replace(' ', ' ');  // non-breaking space → regular space
        // Decompose so accented letters split into ASCII base + combining mark, then drop marks.
        String decomposed = Normalizer.normalize(mapped, Normalizer.Form.NFKD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        // Anything still outside printable ASCII or any path separator → underscore.
        String sanitized = decomposed.replaceAll("[^\\x20-\\x7E]", "_").replace('/', '_');
        if (sanitized.isBlank()) {
            return "attachment";
        }
        if (!sanitized.equals(fileName)) {
            LOGGER.info("→ sanitizeFileNameForFilesystem: '{}' → '{}'", fileName, sanitized);
        }
        return sanitized;
    }
}
