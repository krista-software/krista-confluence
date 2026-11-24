# Get Space Id by Alias

## Overview

Resolves a Confluence Space **Alias** (the value visible in Confluence URLs — e.g. `johndoe`, `~johndoe`, `MFS`) to its numeric Space Id. The numeric Space Id is not exposed in Confluence Cloud's UI, so this request is the canonical way to obtain it when you only have what the user sees.

The returned Space Id can be chained directly into requests like `Get Space By ID`, `Get Pages in Space (V2)`, and other space-scoped operations.

## Request Details

- **Area**: Space
- **Type**: QUERY_SYSTEM
- **Retry Support**: ❌ No (alias resolution failure returns a direct, well-formed error)

## Input Parameters

| Parameter Name | Type | Required | Description                                                                                                                                                          | Example   |
|----------------|------|----------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------|
| Space Alias    | Text | Yes      | The Space Alias as shown in Confluence URLs. For personal spaces this is typically a name like `~johndoe` (or just `johndoe`); for global spaces it usually equals the Space Key (e.g. `MFS`). | `johndoe`    |

### Parameter Details

**Space Alias**
- **Format**: Free-form text.
- **Tilde auto-prepend**: If the input does not start with `~`, the resolver also matches against `~{input}`. This handles the common case where the user types the personal-space alias without the leading tilde (e.g. `johndoe` instead of `~johndoe`).
- **Matched against**: the `currentActiveAlias` field on each space. Historical aliases (set before a space was renamed) are **not** matched — only the current active alias.
- **Where to Find**:
    - **Personal space**: the alias appears in the URL `/spaces/~<alias>/...` (the tilde is part of the alias for personal spaces in v2).
    - **Global space**: the alias usually equals the Space Key (e.g. `MFS`) and appears in the URL `/spaces/<KEY>/...`.

## Output Parameters

| Parameter Name | Type             | Description                                              |
|----------------|------------------|----------------------------------------------------------|
| Space          | Entity(Space)    | The full matched Space entity.                            |
| Space Id       | Identifier (Text)| The numeric Space Id, ready to chain into other requests. |

### Space Entity Structure

| Field               | Type | Description                                                                                  |
|---------------------|------|----------------------------------------------------------------------------------------------|
| Id                  | Text | Numeric Space Id (e.g. `163842`).                                                            |
| Name                | Text | Human-readable space name.                                                                   |
| Type                | Text | `global` or `personal`.                                                                      |
| Status              | Text | Current status of the space.                                                                 |
| Description         | Text | Space description.                                                                           |
| Created At          | Date | Timestamp when the space was created.                                                        |
| Current Active Alias| Text | The alias used in URLs (e.g. `~johndoe`, `MFS`).                                                |

## Implementation

```
GET /wiki/api/v2/spaces?limit=250
```

Cursor-paginated via `_links.next` (using the existing `UrlParser`). On each page of results, every space's `currentActiveAlias` is compared against the input. The first match wins.

### Tilde auto-prepend

For an input like `johndoe`, the resolver matches both `johndoe` and `~johndoe`. For an input that already starts with `~`, only that exact value is matched. This makes the request DWIM for the common personal-space case while remaining strict for global spaces.

### Cost

Bounded by total space count in the instance. For typical instances this is one to two API calls. For very large instances (thousands of spaces) it scales linearly. The request is opt-in — only used when the caller doesn't have a numeric Space Id.

## Validation Rules

| Validation              | Error Message                                              | Resolution                                            |
|-------------------------|------------------------------------------------------------|-------------------------------------------------------|
| Space Alias is empty    | `Space Alias is required. Provide the alias shown in Confluence URLs (e.g. 'johndoe', '~johndoe', 'MFS').` | Provide a non-empty Space Alias.                       |
| No space matches alias  | `No space found with alias '<input>'. ... If the space was renamed, use the current alias.` | Verify the alias via `Get Spaces`; ensure it's current. |
| API failure             | API status surfaced in error message                        | Verify connection / try again.                         |

## Limitations

1. **Current active alias only.** Historical aliases (preserved when a space was renamed) are not matched. If your stored alias is stale, the resolver returns "no space found"; update the saved input to the current alias.
2. **Space-level access required.** The connected user must have access to the space; otherwise the API will not return it in `/spaces` and the resolver will report no match.
3. **No retry flow.** Unlike `Get Space By ID`, this request does not use the Allow-Retry sub-catalog flow — the alias is treated as a one-shot lookup.

## Usage Examples

### Example 1: Personal space by short alias

**Input:**
```
Space Alias: johndoe
```

The resolver matches `~johndoe` (via tilde auto-prepend).

**Output:**
```json
{
  "Space": {
    "Id": "163842",
    "Name": "John Doe",
    "Type": "personal",
    "Status": "current",
    "Current Active Alias": "~johndoe"
  },
  "Space Id": "163842"
}
```

### Example 2: Global space by key

**Input:**
```
Space Alias: MFS
```

**Output:**
```json
{
  "Space": {
    "Id": "294916",
    "Name": "My First Space",
    "Type": "global",
    "Status": "current",
    "Current Active Alias": "MFS"
  },
  "Space Id": "294916"
}
```

### Example 3: Chain into Get Pages in Space (V2)

**Workflow:**
```
1. Get Space Id by Alias with Space Alias: "johndoe" → Space Id: "163842"
2. Get Pages in Space (V2) with Space ID: "163842", Exclude Restricted Pages: true
3. Iterate over Page Identifiers for downstream operations.
```

### Example 4: Alias not found

**Input:**
```
Space Alias: nonexistent
```

**Output (error):**
```
No space found with alias 'nonexistent'.
This could mean:
• The alias is misspelled
• The space was renamed (only the current active alias is matched; previous aliases are not)
• You don't have permission to view this space

What to do next:
1. Use 'Get Spaces' to see each space's currentActiveAlias
2. If the space was renamed, use the current alias
```

## Technical Implementation

### Helper Class
- **Class**: `SpaceArea`
- **Package**: `com.kristasoft.extensions.essentials.collaboration.confluence.catalog`
- **Method**: `getSpaceIdByAlias(String spaceAlias)`
- **Service**: `ConfluenceService.getSpaceIdByAlias(alias)`

### Validation Flow
1. Trim input.
2. If empty: return `INPUT_ERROR` with a clear "Space Alias is required" message.
3. Paginate `/spaces` (via cursor) and compare each space's `currentActiveAlias`.
4. If a match is found: return the Space entity + the numeric `id`.
5. If no match after exhausting pagination: throw `ConfluenceRESTException` with a clear "no space found" message.

### Telemetry Metrics
- `confluence.getSpaceIdByAlias` — incremented on each request.
- `confluence.getSpaceIdByAlias.success` — recorded with space_alias, resolved space_id, space_name.
- `confluence.getSpaceIdByAlias.error` — recorded on failures with error details.

## Related Catalog Requests

- [Get Spaces](pages/GetSpaces.md) — List all spaces (returns each space's `currentActiveAlias` and numeric `id`).
- [Get Space By ID](pages/GetSpaceById.md) — Retrieve a single space by its Id or Key.
- [Get Pages in Space (V2)](pages/GetPagesInSpaceV2.md) — List pages in a space using the numeric Space Id.

## See Also

- [Authentication](pages/Authentication.md)
- [Entities](pages/Entities.md)
