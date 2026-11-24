# Get Pages in Space (V2)

## Overview

Retrieves the pages within a Confluence space using the v2 REST API. Accepts the numeric Space Id directly (no Space Key lookup required), supports an `Exclude Restricted Pages` flag that returns only pages with no explicit read restrictions — i.e. pages "open to all users" who have space access — and an `Include Page Content` flag (unticked by default) that controls whether each page's storage-format body is loaded. The safe default omits bodies to keep responses small for large spaces.

This is the recommended replacement for the legacy `Get Pages in Space` request, whose v1 backing endpoint (`/wiki/rest/api/content`) has been removed by Atlassian (HTTP 410 Gone in Confluence Cloud as of 2026).

## Request Details

- **Area**: Page
- **Type**: QUERY_SYSTEM
- **Retry Support**: ✅ Yes (validation errors prompt for confirmation to retry with corrected input)

## Input Parameters

| Parameter Name           | Type    | Required | Description                                                                                                                                                                                                                  | Example   |
|--------------------------|---------|----------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------|
| Space ID                 | Text    | Yes      | The **numeric** Confluence Space Id (e.g. `163842`). Discover via `Get Spaces` or `Get Space Id by Alias`.                                                                                                                    | `163842`  |
| Exclude Restricted Pages | Boolean | No       | When enabled, only returns pages with no explicit read restrictions. Defaults to `false`.                                                                                                                                    | `true`    |
| Status                   | PickOne (`current`, `archived`, `draft`, `any`, `trashed`) | No | Returns only pages of the chosen status. Choose `any` (or leave unset) for no status filter. Passed to the v2 API as the `status` query param, so non-matching pages are never fetched or restriction-checked. Mirrors the `Status` field on `Get Attachments for Page`. | `current` |
| Include Page Content     | Boolean | No       | Unticked by default — page bodies are omitted. Tick to include full storage-format content on every page — **warning**: spaces with many or large pages can exhaust memory and fail with an out-of-memory error.              | `true`    |
| Allow Retry              | Boolean | No       | When enabled, validation errors prompt the user to re-enter input. Defaults to `false`.                                                                                                                                       | `true`    |

### Parameter Details

**Space ID**
- **Format**: Numeric string (e.g. `163842`).
- **Validation**: Must be a valid existing space identifier.
- **Where to Find**: Use `Get Spaces` to list all spaces, or `Get Space Id by Alias` if you only know the alias visible in Confluence URLs (e.g. `~johndoe`, `MFS`).

**Exclude Restricted Pages**
- **Format**: Boolean.
- **Default**: `false` (returns every page the connected user can see).
- **Scope when enabled**:
    - Excludes pages with **explicit page-level read restrictions** (user or group entries on the `read` operation).
    - Does **not** evaluate space-level permissions; the request presupposes space access.
    - Filters by `read` restrictions only — update/admin restrictions are not considered.
- **Defensive behaviour**: if the per-page restriction check returns non-200 or fails to parse, the page is **kept** rather than silently filtered. A transient API hiccup must never hide content.

**Status**
- **Format**: PickOne — `current`, `archived`, `draft`, `any`, `trashed`.
- **Default**: unset, treated as `any` (no status filter — the endpoint's default set is returned).
- **Scope when set**: passes `status=<value>` to `GET /wiki/api/v2/spaces/{id}/pages`, so Confluence returns only pages of that status. `any` (or unset) omits the parameter.
- **Where it runs**: server-side at the v2 list call — non-matching pages are never fetched, so they also never reach the restriction check. This mirrors how `Get Attachments for Page` handles its `Status` field.

**Include Page Content**
- **Format**: Boolean.
- **Default**: unticked (`false`/null/unset — all treated identically). Page bodies are **not** loaded.
- **Why this exists**: spaces with many or large pages were causing OOM errors because every page body was loaded into memory at once. Krista's `@Field.Boolean` switches always render unticked, so the field is named `Include Page Content` rather than `Exclude Page Content` to keep the visible UI state aligned with the server's behaviour — the safe default is the one with no clicks.
- **When unticked (default)**: the v2 list call omits `body-format`, so Confluence returns each page's metadata without its `body.storage` content. The resulting `Page` entity has a null `Body` field.
- **When ticked**: the v2 list call passes `body-format=storage`, returning storage-format body content on each page.
- **On failure**: if the JVM cannot hold the full response (`OutOfMemoryError`), the service catches the error and surfaces a `ConfluenceRESTException` whose message explicitly names this flag and recommends unticking it. The opt-in is also logged at WARN before the fetch (`Include Page Content is ticked for space {} — every page body will be loaded.`) and each pagination batch logs its response size and running totals at INFO, so the memory-growth pattern is visible in the logs leading up to a failure.

**Allow Retry**
- **Format**: Boolean.
- **Default**: `false`.
- **Behaviour**: identical to the standard validation-retry pattern used across the extension. On validation failure with `Allow Retry = true`, the user is prompted to re-enter input.

## Output Parameters

| Parameter Name   | Type                  | Description                                                       |
|------------------|-----------------------|-------------------------------------------------------------------|
| Pages            | List of Entity(Page)  | The list of Page entities matching the criteria.                  |
| Page Identifiers | List of Identifier    | List of page IDs for quick reference / chaining into other calls. |

### Page Entity Structure

| Field        | Type      | Description                                                                                                            |
|--------------|-----------|------------------------------------------------------------------------------------------------------------------------|
| Id           | Text      | Unique page identifier.                                                                                                |
| Space ID     | Identifier| The space the page belongs to.                                                                                         |
| Title        | Text      | Page title.                                                                                                            |
| Status       | Text      | Page status (`current`).                                                                                               |
| Page URL     | Rich Text | Link to view the page in Confluence.                                                                                   |
| Body         | Rich Text | Page content in storage format.                                                                                        |
| Parent ID    | Text      | Parent page's id (null for root pages).                                                                                |
| Parent Type  | Text      | Type of parent (typically `page`).                                                                                     |
| Has Children | Boolean   | Whether the page has child content.                                                                                    |
| Created At   | Date/Time | When the page was first created — parsed from the v2 response's top-level `createdAt`. Null if missing or unparseable. |
| Last Updated | Date/Time | Timestamp of the latest version — parsed from `version.createdAt` on the v2 response. Null if missing or unparseable.  |

## API Endpoints Used

### Listing pages (v2)

```
# Include Page Content unticked (default) — no body in response
GET /wiki/api/v2/spaces/{spaceId}/pages?limit=250

# Include Page Content ticked — full storage-format body included
GET /wiki/api/v2/spaces/{spaceId}/pages?body-format=storage&limit=250
```

Cursor-paginated via `_links.next`. The cursor URL returned by Confluence echoes the original query, so the body-format choice is preserved across pages. Throws `ConfluenceRESTException` with status + truncated body on non-200 — no silent empty array.

### Restriction check (v1, when `Exclude Restricted Pages = true`)

```
GET /wiki/rest/api/content/{pageId}/restriction/byOperation/read
```

The v2 API has no per-page restrictions endpoint (only `/pages/{id}/operations`, which describes the connected user's permissions, and `/spaces/{id}/permissions`, which is space-level). The canonical, still-supported endpoint for per-page read restrictions is the v1 `restriction/byOperation/read`. Issued in parallel for all returned pages.

Response shape (per [Atlassian v1 docs](https://developer.atlassian.com/cloud/confluence/rest/v1/api-group-content-restrictions/)):

```json
{
  "operation": "read",
  "restrictions": {
    "user":  { "results": [...], "size": N, "limit": 200, "start": 0 },
    "group": { "results": [...], "size": N, "limit": 200, "start": 0 }
  }
}
```

A page is treated as restricted iff `restrictions.user.size > 0 || restrictions.group.size > 0`. Parsing also accepts non-empty `results` arrays or non-zero `totalSize` (permissive).

## Validation Rules

| Validation                  | Error Message            | Resolution                                                                  |
|-----------------------------|--------------------------|-----------------------------------------------------------------------------|
| Space ID is empty or null   | `Space ID is required.`  | Provide a numeric space identifier.                                          |
| Space does not exist        | Space-not-found error    | Use `Get Spaces` to find a valid Space Id, or `Get Space Id by Alias`.       |
| No permission to view space | API returns non-200      | Surfaced as `ConfluenceRESTException` with status and truncated body.        |

## Behaviour Differences from `Get Pages in Space` (v1)

| Behaviour                      | v1 (`Get Pages in Space`)                | v2 (`Get Pages in Space (V2)`)                                       |
|--------------------------------|------------------------------------------|----------------------------------------------------------------------|
| Backing endpoint               | `/wiki/rest/api/content` (HTTP 410 Gone) | `/wiki/api/v2/spaces/{id}/pages` (active)                            |
| Identifies space by            | Space Key (requires `Get Space By Id` lookup) | Numeric Space Id (direct)                                       |
| DRAFT pages returned           | Yes                                       | No (CURRENT-only)                                                    |
| Pagination                     | None — `limit=1000` cap                  | Cursor-based, 250-page batches via `_links.next`                     |
| Exclude restricted pages       | Not supported                            | Supported via `Exclude Restricted Pages` flag                        |
| Non-200 behaviour              | Silent empty array                       | Throws `ConfluenceRESTException` with status + body                  |

## Usage Examples

### Example 1: Return all pages in a space (metadata only — default)

**Input:**
```
Space ID: 163842
Exclude Restricted Pages: false
Include Page Content: false  # default — unticked, bodies are omitted
```

**Output:** all current pages in space 163842, each without its storage body.

### Example 1b: Include page bodies (opt-in)

**Input:**
```
Space ID: 163842
Exclude Restricted Pages: false
Include Page Content: true   # ticked — full storage body returned on every page
```

**Output:** all current pages in space 163842 with body content. Use only for spaces with a manageable page count; large spaces may run out of memory.

### Example 2: Return only unrestricted pages

**Input:**
```
Space ID: 163842
Exclude Restricted Pages: true
```

**Workflow:**
1. v2 `/spaces/163842/pages` is paginated to collect all pages.
2. For each page, v1 `/content/{pageId}/restriction/byOperation/read` is fetched in parallel.
3. Pages with any user or group entry on the read operation are filtered out.
4. Remaining pages are returned.

### Example 3: Chain from alias

**Workflow:**
```
1. Call Get Space Id by Alias with Space Alias: "johndoe" → numeric Space Id 163842.
2. Call Get Pages in Space (V2) with Space ID: 163842, Exclude Restricted Pages: true.
3. Iterate over Page Identifiers for downstream operations.
```

### Example 4: Validation retry

**Input:**
```
Space ID: "" (empty)
Exclude Restricted Pages: false
Allow Retry: true
```

**Output (first attempt):** validation error with re-entry prompt.

**Action:** user provides a valid Space ID.

**Result:** request executes successfully on retry.

## Technical Implementation

### Helper Class
- **Class**: `PageArea`
- **Package**: `com.kristasoft.extensions.essentials.collaboration.confluence.catalog`
- **Method**: `getPagesInSpaceV2(String spaceId, Boolean excludeRestricted, String pageStatus, Boolean includeContent, Boolean allowRetry)` — the catalog field is `Include Page Content`; the service layer keeps the equivalent `excludeContent` semantics (negated at the boundary).
- **Service**: `ConfluenceService.getPagesInSpaceV2(spaceId, excludeRestricted, excludeContent, status)`
- **Retry handlers**: `PageAreaSubCatalogRequestHelper.confirmReenterGetPagesInSpaceV2`, `PageAreaSubCatalogRequestHelper.handleReenterGetPagesInSpaceV2`

### Validation Flow
1. Validate `SPACE_ID` via `ValidationOrchestrator`.
2. On validation pass: list pages via v2, optionally filter via v1 restrictions, return Pages + Page Identifiers.
3. On validation failure with `Allow Retry = true`: store state and prompt for re-entry; flow returns through `confirmReenterGetPagesInSpaceV2`.
4. On validation failure with `Allow Retry = false`: return direct validation error response.

### Telemetry Metrics
- `confluence.getPagesInSpaceV2` — incremented on each request.
- `confluence.getPagesInSpaceV2.success` — recorded with space_id, exclude_restricted, exclude_content, page_count, duration.
- `confluence.getPagesInSpaceV2.error` — recorded on failures with error details.
- `confluence.getPagesInSpaceV2.retryPrompted` — recorded when validation errors trigger retry.

## Related Catalog Requests

- [Get Spaces](pages/GetSpaces.md) — Discover numeric Space Ids.
- [Get Space Id by Alias](pages/GetSpaceIdByAlias.md) — Resolve a UI-visible Space Alias to a numeric Space Id.
- [Get Space By ID](pages/GetSpaceById.md) — Retrieve a single space by its Id or Key.
- [Get Pages in Space](pages/GetPagesInSpace.md) — Legacy v1 request (currently broken in Cloud due to the v1 `/content` removal).
- [Get Page By ID](pages/GetPageById.md) — Retrieve a single page by its Id.
- [Get Page Content as a File](pages/GetPageContentAsFile.md) — Export one page's content as a Markdown (`.md`) file.

## See Also

- [Authentication](pages/Authentication.md)
- [Entities](pages/Entities.md)
