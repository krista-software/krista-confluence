# Get Direct Child Pages of Space

## Overview

Retrieves only the direct child pages of a space (pages with no parent page, i.e., root-level pages) for hierarchical navigation and content organization.

## Request Details

- **Area**: Space
- **Type**: QUERY_SYSTEM
- **Retry Support**: ❌ No (Simple query operation without complex validation)

## Input Parameters

| Parameter Name | Type | Required | Description | Example |
|----------------|------|----------|-------------|---------|
| Space ID | Text, Identifier | Yes | The unique identifier of the Confluence space | `123456` |

### Parameter Details

**Space ID**
- **Format**: Numeric string or space key
- **Purpose**: Identifies the space to retrieve root pages from
- **Validation**: Must be a valid, existing space identifier
- **Where to Find**: Available from Get Spaces or Get Space By ID requests

## Output Parameters

| Parameter Name | Type | Description |
|----------------|------|-------------|
| Pages | List of Entity(Page) | List of root-level Page entities in the space |
| Page Identifiers | List of Identifier | List of page IDs for quick reference |
| Success | Boolean | Indicates whether the operation completed successfully |

### Page Entity Structure

| Field | Type | Description |
|-------|------|-------------|
| Id | Text | Unique identifier for the page |
| Space ID | Identifier | ID of the space containing the page |
| Title | Text | Page title |
| Status | Text | Page status (e.g., "current", "draft") |
| Page URL | Rich Text | Full URL to access the page |
| Body | Rich Text | Page content in storage format |
| Parent ID | Text | ID of parent page (null for root pages) |
| Parent Type | Text | Type of parent (null for root pages) |
| Has Children | Boolean | Whether the page has child pages |

## Usage Examples

### Example 1: Get Root Pages of a Space

**Scenario**: Retrieve all top-level pages in a space for navigation menu.

**Input**:
```
Space ID: "123456"
```

**Output**:
```json
{
  "Pages": [
    {
      "Id": "789012",
      "Space ID": "123456",
      "Title": "Getting Started",
      "Status": "current",
      "Page URL": "https://mycompany.atlassian.net/wiki/spaces/ENG/pages/789012",
      "Has Children": true,
      "Parent ID": null,
      "Parent Type": null
    },
    {
      "Id": "789013",
      "Title": "Architecture Overview",
      "Status": "current",
      "Has Children": false,
      "Parent ID": null
    }
  ],
  "Page Identifiers": ["789012", "789013"],
  "Success": true
}
```

**Result**: Successfully retrieved 2 root-level pages from the space.

### Example 2: Build Navigation Hierarchy

**Scenario**: Create a hierarchical navigation structure starting from root pages.

**Workflow**:
```
1. Call Get Direct Child Pages of Space with Space ID
2. Iterate through returned Pages
3. For pages with Has Children = true:
   - Call Get Direct Child Pages of Page to get next level
4. Build complete navigation tree
```

### Example 3: Error Handling

**Scenario**: Handle case where space has no root pages or doesn't exist.

**Input**:
```
Space ID: "invalid-space"
```

**Output**:
```json
{
  "Pages": [],
  "Page Identifiers": [],
  "Success": false,
  "Error": "Space not found or has no root pages"
}
```

**Result**: Empty result with error message, Success = false.

## Business Rules

1. **Root Pages Only**: Returns only pages without a parent page (top-level pages)
2. **Excludes Nested Pages**: Child pages of root pages are not included
3. **Has Children Flag**: Each page includes a flag indicating if it has child pages
4. **Permission-Based**: Only pages the user has permission to view are returned
5. **Active Pages Only**: Archived or deleted pages are not included

## Limitations

1. **No Pagination**: All root pages are returned at once
2. **No Filtering**: Cannot filter by page title, status, or other criteria
3. **Single Level Only**: Does not retrieve nested child pages (use Get Direct Child Pages of Page for that)
4. **No Content Preview**: Full page body is included, which may impact performance for large pages
5. **Permission Dependent**: Users with limited permissions may see fewer pages

## Best Practices

### 1. Check Has Children Flag
Use the `Has Children` flag to determine if you need to make additional requests for nested pages.

### 2. Build Navigation Incrementally
For large spaces, build navigation trees incrementally rather than loading all pages at once.

### 3. Cache Root Page List
Root pages typically don't change frequently, so caching can improve performance.

### 4. Handle Empty Results
Always check if the Pages list is empty before processing, as spaces may have no root pages.

### 5. Use for Space Overview
This request is ideal for getting a high-level overview of space content organization.

## Common Use Cases

### 1. Space Navigation Menu
```
Scenario: Build a navigation menu showing space structure
Action: Get root pages and display as top-level menu items
Result: User sees main sections of the space
```

### 2. Content Organization Analysis
```
Scenario: Analyze how content is organized in a space
Action: Retrieve root pages and check Has Children flags
Result: Understand space hierarchy depth and structure
```

### 3. Bulk Operations on Root Pages
```
Scenario: Perform operations on all top-level pages
Action: Get root pages and iterate through the list
Result: Process each root page (e.g., update, export)
```

### 4. Space Content Discovery
```
Scenario: Help users discover main content areas in a space
Action: Display root pages as entry points to space content
Result: Users can navigate to major content sections
```

## Related Catalog Requests

- [Get Spaces](pages/GetSpaces.md) - Retrieve all accessible spaces
- [Get Space By ID](pages/GetSpaceById.md) - Get specific space details
- [Get Pages in Space](pages/GetPagesInSpace.md) - Search all pages in a space
- [Get Direct Child Pages of Page](pages/GetDirectChildPagesOfPage.md) - Get child pages of a specific page
- [Check if Page Has Children](pages/CheckIfPageHasChildren.md) - Verify if a page has children
- [Get All Nested Descendants of Page](pages/GetAllNestedDescendantsOfPage.md) - Get complete page tree

## Technical Implementation

### Helper Class
- **Class**: SpaceArea
- **Package**: com.kristasoft.extensions.essentials.collaboration.confluence.catalog
- **Method**: getDirectChildPagesOfSpace(String spaceID)
- **Service**: ConfluenceService.getDirectChildPagesOfSpace(spaceID)

### Processing Flow
1. Validate Space ID parameter
2. Call Confluence API to get pages with no parent
3. For each page, check if it has children using hasChildPages()
4. Set Has Children flag on each Page entity
5. Build response with Pages list and identifiers
6. Return success status

### Telemetry Metrics
- **confluence.getDirectChildPagesOfSpace**: Incremented on each request
- **confluence.getDirectChildPagesOfSpace.success**: Recorded with space ID, page count, and duration
- **confluence.getDirectChildPagesOfSpace.error**: Recorded on failures with error details

### API Endpoint
```
GET https://{domain}.atlassian.net/wiki/rest/api/space/{spaceId}/content/page
?depth=root
```

## Error Handling

### System Errors (SYSTEM_ERROR)

**Cause**: Network issues, Confluence API unavailability, or internal errors

**Common Scenarios**:
- Space doesn't exist
- Network connectivity issues
- Confluence service temporarily unavailable
- API rate limiting

**Resolution**:
1. Verify space ID is correct
2. Check network connectivity
3. Verify Confluence service status
4. Wait and retry after a few moments
5. Use Get Space By ID to verify space exists

### Authorization Errors

**Cause**: Invalid credentials or insufficient permissions

**Common Scenarios**:
- User lacks permission to view space
- API token has been revoked
- Account has been deactivated

**Resolution**:
1. Verify API token is valid
2. Check space permissions
3. Request access from space administrator
4. Regenerate API token if necessary

## Troubleshooting

### Issue: Empty Pages List
**Cause**: Space has no root pages or user lacks permissions

**Solution**:
1. Verify space exists using Get Space By ID
2. Check if space has any pages in Confluence UI
3. Verify user has permission to view space content
4. Check if all pages are nested under a parent (no root pages)

### Issue: Success = false
**Cause**: Error occurred during page retrieval

**Solution**:
1. Check the Error field in the response for details
2. Verify space ID is correct
3. Ensure space is not archived
4. Check network connectivity and Confluence service status

### Issue: Missing Expected Pages
**Cause**: Pages have parent pages (not root-level)

**Solution**:
1. Use Get Pages in Space to see all pages
2. Check page hierarchy in Confluence UI
3. Use Get Direct Child Pages of Page for nested pages
4. Verify pages haven't been moved to a parent

### Issue: Has Children flag incorrect
**Cause**: Page hierarchy changed after retrieval

**Solution**:
1. This is expected if pages are being actively edited
2. Refresh the data by calling the request again
3. Use Check if Page Has Children for real-time verification

## See Also

- [Extension Configuration](pages/ExtensionConfiguration.md) - Setup and configuration
- [Get Space By ID](pages/GetSpaceById.md) - Verify space exists
- [Get Direct Child Pages of Page](pages/GetDirectChildPagesOfPage.md) - Get next level of hierarchy
- [Get Pages in Space](pages/GetPagesInSpace.md) - Get all pages in space
- [Entities](pages/Entities.md) - Page entity structure reference

