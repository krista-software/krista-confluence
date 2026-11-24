# Download Attachment

## Overview

Downloads the binary content of an attachment file for ingestion into Document Sets. This request retrieves the actual file content using the download link from an Attachment entity.

## Request Details

- **Area**: Page
- **Type**: ACTION_SYSTEM
- **Retry Support**: ❌ No (Direct action without validation retry)

## Input Parameters

| Parameter Name | Type | Required | Description | Example |
|----------------|------|----------|-------------|---------|
| Download Link | Paragraph | Yes | The download URL path from the Attachment entity, or a v1 child-attachment download path | `"/wiki/rest/api/content/123456/child/attachment/att789/download"` |
| Attachment ID | Text | No | Optional attachment identifier for logging | `"att-123456"` |

### Parameter Details

**Download Link**
- **Format**: URL path string, either absolute (`https://your-site.atlassian.net/...`) or site-relative
- **Purpose**: Specifies the location to download the file from
- **Source**: Obtained from the `Download Link` field of an Attachment entity, or manually constructed as described below
- **Validation**: Must match one of three accepted formats:
  1. `/wiki/download/...` — legacy path returned in the `Download Link` field of an Attachment entity
  2. `/download/attachments/...` — same as above without the `/wiki/` prefix (auto-corrected)
  3. `/wiki/rest/api/content/{pageId}/child/attachment/{attachmentId}/download` — the v1 REST API child-attachment download path (with or without the `/wiki/` prefix)
- **Required**: Yes
- **⚠️ Important — auth compatibility**: formats 1 and 2 (`/wiki/download/...`) only work under session/cookie authentication and reliably return `401 Unauthorized` under API-token/OAuth auth. Format 3 (the child-attachment path) is the one confirmed to work under API-token/OAuth auth — construct it from the attachment's Page ID and Attachment ID rather than relying on the raw `Download Link` field when using OAuth. This is the same path **Download Attachment (v2)** uses internally, so prefer v2 (input: Attachment ID only) unless you specifically need to supply a pre-built link.

**Attachment ID**
- **Format**: Text string
- **Purpose**: Used for logging and telemetry tracking
- **Required**: No

## Output Parameters

| Parameter Name | Type | Description |
|----------------|------|-------------|
| File | File | The downloaded file object for use in Document Sets. Empty/absent when `Success` is `false` |
| File Name | Text | Name of the downloaded file. Empty string when `Success` is `false` |
| File Size | Number | Size of the file in bytes. `0` when `Success` is `false` |
| Content Type | Text | MIME type of the file. Empty string when `Success` is `false` |
| Success | Boolean | Whether the download succeeded |
| Error | Text | Present only when `Success` is `false` — the underlying failure message |

## Validation Rules

| Validation | Error Message | Resolution |
|------------|---------------|------------|
| Download Link is empty | "Download link is required" | Provide a valid download link |
| Invalid download link format | "Invalid download link format" | Use a link matching one of the three accepted formats (see Parameter Details above) |
| Path traversal attempt | "Invalid download link format" | Use only valid Confluence paths |

## Error Handling

**This request does not fail hard.** Any error — validation failure, a bad link, an HTTP error from Confluence, or an auth problem — is caught and returned as a normal `SUCCESS` response with `Success: false`, an `Error` field containing the failure message, and empty values for `File`, `File Name`, `File Size`, and `Content Type`. The error is still logged and recorded in telemetry server-side. The one exception is `MustAuthorizeException` (the connection needs re-authorization), which still propagates so the platform can trigger the re-auth flow.

**Common Scenarios**:
- Missing or empty Download Link
- Invalid download link format (doesn't match any of the three accepted patterns)
- Path traversal attempt (security violation)
- File no longer exists (HTTP 404)
- Access denied (HTTP 403) — commonly caused by using a `/wiki/download/...` link under OAuth/API-token auth (see the auth-compatibility note above)
- Server error (HTTP 500) or network timeout

**Resolution**:
1. Always check the `Success` field before using `File` — don't assume a `SUCCESS` result means the download worked
2. Verify the attachment still exists using Get Attachment By ID
3. If failures mention 401/403 and you're using a `/wiki/download/...` link, switch to the v1 child-attachment path format or use **Download Attachment (v2)** instead
4. Retry after a few moments for transient errors

## Usage Examples

### Example 1: Download PDF Attachment

**Input**:
```
Download Link: "/download/attachments/123456/quarterly-report.pdf"
Attachment ID: "att-789"
```

**Output**:
```json
{
  "File": <File Object>,
  "File Name": "quarterly-report.pdf",
  "File Size": 5242880,
  "Content Type": "application/pdf",
  "Success": true
}
```

### Example 2: Download Image Attachment

**Input**:
```
Download Link: "/download/attachments/123456/diagram.png"
```

**Output**:
```json
{
  "File": <File Object>,
  "File Name": "diagram.png",
  "File Size": 102400,
  "Content Type": "image/png",
  "Success": true
}
```

### Example 3: Download via v1 Child-Attachment Path (recommended for OAuth/API-token auth)

**Input**:
```
Download Link: "/wiki/rest/api/content/7491551233/child/attachment/att7491551269/download"
Attachment ID: "att7491551269"
```

**Output**:
```json
{
  "File": <File Object>,
  "File Name": "board book notes.docx",
  "File Size": 14335,
  "Content Type": "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
  "Success": true
}
```

### Example 4: Failed Download (graceful error response)

**Input**:
```
Download Link: "/wiki/download/attachments/123456/document.pdf"
Attachment ID: "att-123456"
```

**Output** (attachment no longer exists, or link is unauthorized under OAuth):
```json
{
  "File Name": "",
  "File Size": 0,
  "Content Type": "",
  "Success": false,
  "Error": "The Confluence server returned an error (status code: 404). This usually means the attachment is not accessible or the download link is invalid."
}
```
Note the response is still a `SUCCESS`-status `ExtensionResponse` — check the `Success` field, not the response status, to detect this failure.

## Business Rules

1. **Link Correction**: Automatically adds `/wiki/` prefix if missing from a `/download/attachments/...` or child-attachment-path download link
2. **Security Validation**: Rejects paths that don't match any of the three accepted Confluence download patterns
3. **File Handling**: Creates proper Krista File objects for Document Set ingestion
4. **Temporary Files**: Cleans up temporary files after processing
5. **Graceful Failure**: Never returns a hard `FAILURE` response (except `MustAuthorizeException`) — failures are reported via `Success: false` and an `Error` field instead

## Limitations

1. **Single File**: Downloads one attachment at a time
2. **No Streaming**: Entire file is loaded into memory
3. **Size Limits**: Very large files may cause memory issues
4. **Link Expiry**: Download links may expire; refresh from Attachment entity if needed

## Best Practices

### 1. Always Get Fresh Attachment Metadata
Before downloading, use Get Attachment By ID to ensure the download link is current.

### 2. Check File Size First
For large files, verify File Size from attachment metadata before downloading.

### 3. Use for Document Ingestion
The returned File object is designed for ingestion into Krista Document Sets.

### 4. Handle Errors Gracefully
Implement retry logic for transient network errors.

### 5. Always Check the Success Field
This request returns `SUCCESS` even when the download failed — inspect `Success`/`Error` in the response body rather than relying on the response status to detect failures.

### 6. Prefer the v1 Child-Attachment Path (or v2) Under OAuth/API-Token Auth
If your connection uses OAuth or an API token rather than session/cookie auth, the raw `Download Link` from an Attachment entity (`/wiki/download/...`) will typically fail with 401. Construct the v1 child-attachment path instead (`/wiki/rest/api/content/{pageId}/child/attachment/{attachmentId}/download`), or use **Download Attachment (v2)**, which builds and uses this path automatically from just the Attachment ID.

## Related Catalog Requests

- [Download Attachment (v2)](pages/DownloadAttachmentV2.md) - Recommended under OAuth/API-token auth; takes only an Attachment ID, no link required
- [Get Attachments for Page](pages/GetAttachmentsForPage.md) - Discover attachments and get download links
- [Get Attachment By ID](pages/GetAttachmentById.md) - Get fresh download link for specific attachment
- [Get Page By ID](pages/GetPageById.md) - Get parent page details

## See Also

- [Entities](pages/Entities.md) - Attachment entity structure reference
- [Extension Configuration](pages/ExtensionConfiguration.md) - Setup and configuration

