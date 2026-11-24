# Download Attachment (v2)

## Overview

Downloads the binary content of an attachment **by its ID** using the Confluence **v2 REST API**.
Unlike [Download Attachment](DownloadAttachment.md) (v1), which requires you to supply a download link, this request
takes only the attachment ID. It first calls the v2 attachment endpoint (`GET /wiki/api/v2/attachments/{id}`) to resolve
a **fresh download link**, then retrieves the file for ingestion into Document Sets.

## Request Details

- **Area**: Page
- **Type**: CHANGE_SYSTEM
- **API Version**: v2 (`/wiki/api/v2/attachments/{id}`)
- **Retry Support**: ❌ No (Direct action without validation retry)

## Input Parameters

| Parameter Name | Type | Required | Description                                         | Example       |
|----------------|------|----------|-----------------------------------------------------|---------------|
| Attachment ID  | Text | Yes      | The unique identifier of the attachment to download | `"att123456"` |

### Parameter Details

**Attachment ID**

- **Format**: Text string
- **Purpose**: Identifies the attachment whose content should be downloaded
- **Source**: Obtained from the `Id` field of an Attachment entity returned
  by [Get Attachments for Page](GetAttachmentsForPage.md) or [Get Attachment by ID](GetAttachmentById.md)
- **Required**: Yes

## Output Parameters

| Parameter Name | Type    | Description                                         |
|----------------|---------|-----------------------------------------------------|
| File           | File    | The downloaded file object for use in Document Sets |
| File Name      | Text    | Name of the downloaded file (the attachment title)  |
| File Size      | Number  | Size of the file in bytes                           |
| Content Type   | Text    | MIME type of the file                               |
| Success        | Boolean | Whether the download succeeded                      |

## How It Works

1. **Resolve metadata** — Calls `GET /wiki/api/v2/attachments/{id}` to retrieve the attachment, including its
   `downloadLink` and `title`.
2. **Correct the link** — Prefixes the resolved relative download link with `/wiki/` when required.
3. **Download binary** — Performs an authenticated binary GET against the resolved link.
4. **Build the file** — Writes the bytes to a Krista File object, using the attachment `title` as the file name (falling
   back to the link, with any query string stripped, when the title is unavailable). The file name is sanitized for
   filesystem compatibility before writing (see [Non-ASCII Filename Support](#non-ascii-filename-support) below).

## Non-ASCII Filename Support

Attachment filenames containing non-ASCII characters (accented letters, CJK characters, emoji, Unicode punctuation,
etc.)
are fully supported. Before the file is written to disk, the attachment title is passed through a filename sanitizer
that:

1. **Maps Unicode punctuation to ASCII equivalents** — for example, `–` (en-dash) → `-`, `"` / `"` → `"`,
   `'` / `'` → `'`.
2. **Decomposes accented letters** — applies NFKD normalization so that characters like `é` become `e` (base letter
   with the combining accent discarded).
3. **Replaces remaining non-ASCII characters and path separators** — any character that is still outside the printable
   ASCII range, or a `/`, is replaced with `_`.
4. **Falls back to `"attachment"`** — if the sanitized result is blank, the file is named `attachment`.

**Examples**

| Original attachment title   | Sanitized file name         |
|-----------------------------|-----------------------------|
| `café–report.pdf`           | `cafe-report.pdf`           |
| `résumé 2024.docx`          | `resume 2024.docx`          |
| `Q1 "Summary" (draft).xlsx` | `Q1 "Summary" (draft).xlsx` |

> **Note**: The `File Name` output parameter always reflects the sanitized name that was used when writing the file.

## Error Handling

### Input Errors (INPUT_ERROR)

**Cause**: Missing attachment ID

**Resolution**: Provide a valid attachment ID from [Get Attachments for Page](GetAttachmentsForPage.md)
or [Get Attachment by ID](GetAttachmentById.md).

### System Errors (SYSTEM_ERROR)

**Common Scenarios**:

- Attachment does not exist or has been deleted (HTTP 404)
- Access denied (HTTP 403)
- The attachment returned no download link (no file content)
- The resolved download link has expired
- Network or server error

**Resolution**:

1. Verify the attachment still exists using [Get Attachment by ID](GetAttachmentById.md)
2. Check that you have permission to access the attachment
3. Retry after a few moments for transient errors

## Usage Example

**Input**:

```
Attachment ID: "att123456"
```

**Output**:

```json
{
  "File": "<File Object>",
  "File Name": "quarterly-report.pdf",
  "File Size": 5242880,
  "Content Type": "application/pdf",
  "Success": true
}
```

## When to Use v2 vs v1

|                         | Download Attachment (v1)       | Download Attachment (v2)                |
|-------------------------|--------------------------------|-----------------------------------------|
| Input                   | Download Link (Paragraph)      | Attachment ID (Text)                    |
| Download link freshness | Caller-supplied (may be stale) | Resolved fresh on each call             |
| API                     | Direct binary download path    | `v2/attachments/{id}` + binary download |

Prefer **v2** when you already have the attachment ID, since it always resolves a current download link and avoids
passing potentially stale links between requests.

## Related Catalog Requests

- [Download Attachment](DownloadAttachment.md) - v1 download using an explicit download link
- [Get Attachments for Page](GetAttachmentsForPage.md) - Discover attachments and their IDs
- [Get Attachment by ID](GetAttachmentById.md) - Get attachment metadata

## See Also

- [Entities](Entities.md) - Attachment entity structure reference
