# Get Direct Child Pages of Page

## Overview

Retrieves only the direct child pages of a specific page (one level down) for hierarchical navigation and content organization.

## Request Details

- **Area**: Page
- **Type**: QUERY_SYSTEM
- **Retry Support**: ❌ No (Simple query operation without complex validation)

## Input Parameters

| Parameter Name | Type | Required | Description | Example |
|----------------|------|----------|-------------|---------|
| Page ID | Text, Identifier | Yes | The unique identifier of the parent page | `987654` |

### Parameter Details

**Page ID**
- **Format**: Numeric string
- **Purpose**: Identifies the parent page to retrieve children from
- **Validation**: Must be a valid, existing page identifier
- **Where to Find**: Available from Get Pages in Space, Get Page By ID, or Confluence page URL

## Output Parameters

| Parameter Name | Type | Description |
|----------------|------|-------------|
| Pages | List of Entity(Page) | List of direct child Page entities |
| Page Identifiers | List of Identifier | List of child page IDs for quick reference |
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
| Parent ID | Text | ID of parent page (the input Page ID) |
| Parent Type | Text | Type of parent (typically "page") |
| Has Children | Boolean | Whether the page has its own child pages |

## Usage Examples

### Example 1: Get Child Pages

**Scenario**: Retrieve all direct children of a parent page.

**Input**:
```
Page ID: "987654"
```

**Output**:
```json
{
  "Pages": [
    {
      "Id": "987655",
      "Space ID": "123456",
      "Title": "Installation Guide",
      "Status": "current",
      "Page URL": "https://mycompany.atlassian.net/wiki/spaces/ENG/pages/987655",
      "Parent ID": "987654",
      "Parent Type": "page",
      "Has Children": true
    },
    {
      "Id": "987656",
      "Title": "Configuration Guide",
      "Status": "current",
      "Parent ID": "987654",
      "Has Children": false
    }
  ],
  "Page Identifiers": ["987655", "987656"],
  "Success": true
}
```

**Result**: Successfully retrieved 2 direct child pages.

### Example 2: Build Navigation Tree

**Scenario**: Build hierarchical navigation starting from a page.

**Workflow**:
```
1. Call Get Direct Child Pages of Page with Page ID: "987654"
2. Iterate through returned Pages
3. For pages with Has Children = true:
   - Recursively call Get Direct Child Pages of Page
4. Build complete navigation tree
```

### Example 3: No Children

**Scenario**: Page has no child pages.

**Input**:
```
Page ID: "987656"
```

**Output**:
```json
{
  "Pages": [],
  "Page Identifiers": [],
  "Success": true
}
```

**Result**: Empty result, page has no children.

## Business Rules

1. **Direct Children Only**: Returns only immediate children (one level down)
2. **Excludes Nested Pages**: Grandchildren and deeper descendants are not included
3. **Has Children Flag**: Each page includes a flag indicating if it has its own children
4. **Permission-Based**: Only pages the user has permission to view are returned
5. **Active Pages Only**: Archived or deleted pages are not included

## Limitations

1. **Single Level Only**: Does not retrieve nested descendants (use Get All Nested Descendants for that)
2. **No Pagination**: All direct children are returned at once
3. **No Filtering**: Cannot filter by page title, status, or other criteria
4. **No Content Preview**: Full page body is included, which may impact performance
5. **Permission Dependent**: Users with limited permissions may see fewer pages

## Best Practices

### 1. Check Has Children Flag
Use the `Has Children` flag to determine if you need to make additional requests for deeper levels.

### 2. Build Navigation Incrementally
For large page trees, build navigation incrementally rather than loading all pages at once.

### 3. Cache Child Page Lists
Child pages typically don't change frequently, so caching can improve performance.

### 4. Handle Empty Results
Always check if the Pages list is empty before processing, as pages may have no children.

### 5. Use for Hierarchical Navigation
This request is ideal for building expandable navigation menus.

## Common Use Cases

### 1. Navigation Menu
```
Scenario: Build expandable navigation showing page hierarchy
Action: Get child pages and display as sub-menu items
Result: User sees page structure
```

### 2. Content Organization Analysis
```
Scenario: Analyze page hierarchy depth
Action: Retrieve child pages and check Has Children flags
Result: Understand documentation structure
```

### 3. Bulk Operations on Children
```
Scenario: Perform operations on all child pages
Action: Get child pages and iterate through the list
Result: Process each child page
```

### 4. Hierarchy Validation
```
Scenario: Verify expected page structure exists
Action: Check if expected child pages are present
Result: Validate documentation organization
```

## Related Catalog Requests

- [Get Page By ID](pages/GetPageById.md) - Retrieve parent page details
- [Get Direct Child Pages of Space](pages/GetDirectChildPagesOfSpace.md) - Get root-level pages
- [Get All Nested Descendants of Page](pages/GetAllNestedDescendantsOfPage.md) - Get complete page tree
- [Check if Page Has Children](pages/CheckIfPageHasChildren.md) - Quick check for children
- [Get Ancestors of Page](pages/GetAncestorsOfPage.md) - Get parent hierarchy

## Technical Implementation

### Helper Class
- **Class**: PageArea
- **Package**: com.kristasoft.extensions.essentials.collaboration.confluence.catalog
- **Method**: getDirectChildPagesOfPage(String pageID)
- **Service**: ConfluenceService.getDirectChildPagesOfPage(pageID)

### Processing Flow
1. Validate Page ID parameter
2. Call Confluence API to get pages with specified parent
3. For each page, check if it has children using hasChildPages()
4. Set Has Children flag on each Page entity
5. Build response with Pages list and identifiers
6. Return success status

### Telemetry Metrics
- **confluence.getDirectChildPagesOfPage**: Incremented on each request
- **confluence.getDirectChildPagesOfPage.success**: Recorded with page ID, child count, and duration
- **confluence.getDirectChildPagesOfPage.error**: Recorded on failures

### API Endpoint
```
GET https://{domain}.atlassian.net/wiki/rest/api/content/{pageId}/child/page
```

## Error Handling

### System Errors (SYSTEM_ERROR)

**Cause**: Network issues, Confluence API unavailability, or internal errors

**Common Scenarios**:
- Parent page doesn't exist
- Network connectivity issues
- Confluence service temporarily unavailable
- API rate limiting

**Resolution**:
1. Verify page ID is correct using Get Page By ID
2. Check network connectivity
3. Verify Confluence service status
4. Wait and retry after a few moments

### Authorization Errors

**Cause**: Invalid credentials or insufficient permissions

**Common Scenarios**:
- User lacks permission to view parent page
- User lacks permission to view child pages
- API token has been revoked

**Resolution**:
1. Verify API token is valid
2. Check page permissions
3. Request access from page/space administrator
4. Regenerate API token if necessary

## Troubleshooting

### Issue: Empty Pages List
**Cause**: Page has no children or user lacks permissions

**Solution**:
1. Verify parent page exists using Get Page By ID
2. Check if page has children in Confluence UI
3. Verify user has permission to view child pages
4. Use Check if Page Has Children to verify

### Issue: Success = false
**Cause**: Error occurred during page retrieval

**Solution**:
1. Check the Error field in the response for details
2. Verify page ID is correct
3. Ensure page is not archived
4. Check network connectivity and Confluence service status

### Issue: Missing Expected Pages
**Cause**: User lacks permission to view certain child pages

**Solution**:
1. Verify child page permissions in Confluence
2. Request access from page/space administrator
3. Check if pages are archived
4. Confirm pages haven't been moved or deleted

### Issue: Has Children flag incorrect
**Cause**: Page hierarchy changed after retrieval

**Solution**:
1. This is expected if pages are being actively edited
2. Refresh the data by calling the request again
3. Use Check if Page Has Children for real-time verification

## See Also

- [Extension Configuration](pages/ExtensionConfiguration.md) - Setup and configuration
- [Get Page By ID](pages/GetPageById.md) - Verify parent page exists
- [Get All Nested Descendants of Page](pages/GetAllNestedDescendantsOfPage.md) - Get complete tree
- [Check if Page Has Children](pages/CheckIfPageHasChildren.md) - Quick children check
- [Entities](pages/Entities.md) - Page entity structure reference

