# Get Page Content as a File

## Overview

Downloads a single Confluence page's content as a Markdown (`.md`) file. Takes one **Page ID** at a time, fetches
the page via `GET /wiki/api/v2/pages/{id}?body-format=storage`, converts the storage-format XHTML (including
Confluence-specific `ac:` macros) into Markdown, and returns a Krista `File` named after the sanitized page title.

## Request Details

- **Area**: Page
- **Type**: QUERY_SYSTEM
- **Retry Support**: ✅ Yes (validation errors prompt for confirmation to retry with corrected input)

## Input Parameters

| Parameter Name | Type    | Required | Description                                                                                                  | Example   |
|----------------|---------|----------|--------------------------------------------------------------------------------------------------------------|-----------|
| Page ID        | Text    | Yes      | The Confluence page id to export.                                                                            | `163842`  |
| Allow Retry    | Boolean | No       | When enabled, validation errors prompt the user to re-enter input. Defaults to `false`.                      | `true`    |

### Parameter Details

**Page ID**
- **Format**: numeric string identifying a Confluence page.
- **Validation**: must be non-empty; resolved via `GET /wiki/api/v2/pages/{id}` so a non-existent id returns the
  standard "page not found" error.
- **Where to find it**: `Get Pages in Space (V2)` lists ids; `Get Page By ID` confirms a single page.

## Output Parameters

| Parameter Name  | Type | Description                                                                                                                  |
|-----------------|------|------------------------------------------------------------------------------------------------------------------------------|
| Downloaded File | File | Markdown (`.md`) file containing the page's converted content. Filename is `<sanitized-title>.md` (fallback `<pageId>.md`).  |

## File Format

- **Extension**: `.md`
- **Encoding**: UTF-8
- **Structure**: an H1 (`# <title>`) followed by the converted body.

### Conversion Details

The page body returned by Confluence is XHTML extended with `ac:` and `ri:` namespaced elements
(code blocks, panels, emoticons, page/attachment links, images, layouts, task lists). Conversion proceeds in two passes:

1. **jsoup pre-pass** — rewrites Confluence-specific elements into standard HTML:
    - `ac:structured-macro[name=code]` → `<pre><code class="language-X">…</code></pre>`
    - `ac:structured-macro[name=info|note|warning|tip]` → `<blockquote>` with a bold label
    - `ac:emoticon` → unicode emoji where mapped (smile, sad, thumbs-up, …), else `:name:`
    - `ac:link` (page/attachment/user) → bracketed text placeholder. Confluence storage carries no resolved URL,
      so cross-page link targets cannot be reconstructed; the label is preserved.
    - `ac:image` → `[Image: filename]` placeholder for the same reason.
    - `ac:layout` / `ac:layout-section` / `ac:layout-cell` → unwrapped so their content flows inline.
    - `ac:task-list` → `<ul>` with `[ ]` / `[x]` prefixes per task status.
    - Unknown `ac:structured-macro` instances are flattened — their text content is kept and the wrapper is dropped —
      so a future macro this converter does not recognize still produces readable output instead of being silently
      stripped.
2. **flexmark HTML→Markdown** — `com.vladsch.flexmark:flexmark-html2md-converter:0.64.8` converts the cleaned tree.

### Filename

The filename is derived from the page title:

- Illegal filename characters (`<>:"/\\|?*`) and whitespace become `_`.
- Control characters are stripped.
- Lower-cased; leading/trailing `_` and `.` trimmed.
- Capped at 100 characters (filesystems get cranky beyond that).
- Falls back to the page id if the sanitized title is empty; falls back to `page` if both are empty.

## API Endpoints Used

```
GET /wiki/api/v2/pages/{pageId}?body-format=storage
```

This is the same endpoint that backs `Get Page By ID`. The response body's `body.storage.value` is the XHTML fed to
the converter.

## Validation Rules

| Validation                  | Error Message                  | Resolution                                              |
|-----------------------------|--------------------------------|---------------------------------------------------------|
| Page ID is empty or null    | `Page ID is required.`         | Provide a valid Confluence page id.                     |
| Page does not exist         | Page-not-found error           | Use `Get Pages in Space (V2)` to find a valid Page ID.  |
| No permission to view page  | API returns non-200            | Verify the page is in a space you can access.           |

## Usage Examples

### Example 1: Download a page

**Input:**
```
Page ID: 163842
```

**Output:** a `Downloaded File` whose name is `<sanitized-title>.md` and whose body is the page content in Markdown,
with the page title as the H1.

### Example 2: Validation retry

**Input:**
```
Page ID: ""  (empty)
Allow Retry: true
```

**Output (first attempt):** validation error with re-entry prompt.

**Action:** user provides a valid Page ID.

**Result:** request executes successfully on retry and returns the Markdown file.

## Technical Implementation

### Catalog Request
- **Class**: `PageArea`
- **Package**: `com.kristasoft.extensions.essentials.collaboration.confluence.catalog`
- **Method**: `getPageContentAsFile(String pageID, Boolean allowRetry)`
- **Retry handlers**: `PageAreaSubCatalogRequestHelper.confirmReenterGetPageContentAsFile`,
  `PageAreaSubCatalogRequestHelper.handleReenterGetPageContentAsFile`

### Conversion
- **Class**: `ConfluenceStorageToMarkdownConverter`
- **Package**: `com.kristasoft.extensions.essentials.collaboration.confluence.utils`
- **Library**: `com.vladsch.flexmark:flexmark-html2md-converter:0.64.8`

### Service
- **Interface**: `PageMarkdownService`
- **Implementation**: `PageMarkdownServiceImpl`
- **Method**: `downloadPageAsMarkdownFile(String pageId)` — returns the Krista `File`.

### Telemetry Metrics
- `confluence.getPageContentAsFile` — incremented on each request.
- `confluence.getPageContentAsFile.success` — recorded with `page_id` and duration on success.
- `confluence.getPageContentAsFile.error` — recorded on failures with error details.
- `confluence.getPageContentAsFile.retryPrompted` — recorded when validation errors trigger retry.

## Related Catalog Requests

- [Get Page By ID](pages/GetPageById.md) — Retrieves the full Page entity (same underlying endpoint).
- [Get Page in Space](pages/GetPageInSpace.md) — Returns the page as a Word (DOCX) file.
- [Get Pages in Space (V2)](pages/GetPagesInSpaceV2.md) — Lists pages in a space so you can find an id.

## See Also

- [Authentication](pages/Authentication.md)
- [Entities](pages/Entities.md)
