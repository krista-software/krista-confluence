# Get All Nested Descendants of Page

## Overview

Retrieves all descendant pages of a specific page at all hierarchy levels, providing a complete view of the entire page tree below the specified page.

## Request Details

- **Area**: Page
- **Type**: QUERY_SYSTEM
- **Retry Support**: ❌ No (Recursive query operation)

## Input Parameters

| Parameter Name | Type | Required | Description | Example |
|----------------|------|----------|-------------|---------|
| Page ID | Text, Identifier | Yes | The unique identifier of the parent page | `987654` |

### Parameter Details

**Page ID**
- **Format**: Numeric string
- **Purpose**: Identifies the root page to retrieve all descendants from
- **Validation**: Must be a valid, existing page identifier
- **Where to Find**: Available from Get Pages in Space, Get Page By ID, or Confluence page URL

## Output Parameters

| Parameter Name | Type | Description |
|----------------|------|-------------|
| Pages | List of Entity(Page) | List of all descendant Page entities at all levels |
| Page Identifiers | List of Identifier | List of all descendant page IDs for quick reference |
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
| Parent ID | Text | ID of parent page |
| Parent Type | Text | Type of parent (typically "page") |
| Has Children | Boolean | Whether the page has its own child pages |

## Usage Examples

### Example 1: Get Complete Page Tree

**Scenario**: Retrieve all descendants of a parent page.

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
      "Parent ID": "987654",
      "Has Children": true
    },
    {
      "Id": "987656",
      "Title": "Windows Installation",
      "Status": "current",
      "Parent ID": "987655",
      "Has Children": false
    },
    {
      "Id": "987657",
      "Title": "Linux Installation",
      "Status": "current",
      "Parent ID": "987655",
      "Has Children": false
    },
    {
      "Id": "987658",
      "Title": "Configuration Guide",
      "Status": "current",
      "Parent ID": "987654",
      "Has Children": false
    }
  ],
  "Page Identifiers": ["987655", "987656", "987657", "987658"],
  "Success": true
}
```

**Result**: Successfully retrieved 4 descendants across multiple levels.

### Example 2: Build Complete Navigation Tree

**Scenario**: Build full hierarchical navigation from a starting point.

**Workflow**:
```
1. Call Get All Nested Descendants of Page with Page ID: "987654"
2. Organize pages by Parent ID to build tree structure
3. Display complete hierarchy in navigation
```

### Example 3: No Descendants

**Scenario**: Page has no descendants at any level.

**Input**:
```
Page ID: "987658"
```

**Output**:
```json
{
  "Pages": [],
  "Page Identifiers": [],
  "Success": true
}
```

**Result**: Empty result, page has no descendants.

## Business Rules

1. **All Levels Included**: Returns descendants at all hierarchy levels (children, grandchildren, etc.)
2. **Recursive Retrieval**: Automatically traverses the entire page tree
3. **Has Children Flag**: Each page includes a flag indicating if it has its own children
4. **Permission-Based**: Only pages the user has permission to view are returned
5. **Active Pages Only**: Archived or deleted pages are not included
6. **Flat List**: Returns a flat list of all descendants, not a hierarchical structure

## Limitations

1. **No Hierarchy Structure**: Returns flat list; you must organize by Parent ID for tree structure
2. **No Pagination**: All descendants are returned at once
3. **Performance Impact**: Large page trees may have slow response times
4. **No Filtering**: Cannot filter by page title, status, or other criteria
5. **Permission Dependent**: Users with limited permissions may see fewer pages
6. **No Level Indicator**: Does not indicate the depth level of each page

## Best Practices

### 1. Use for Complete Tree Operations
Use this request when you need to process all descendants, not just direct children.

### 2. Organize Results by Parent ID
Build hierarchical structure by grouping pages by their Parent ID.

### 3. Cache Results for Large Trees
Cache the result for large page trees to avoid repeated expensive calls.

### 4. Handle Empty Results
Always check if the Pages list is empty before processing.

### 5. Consider Performance
Be aware that large page trees may take time to retrieve; use direct child requests for incremental loading.

## Common Use Cases

### 1. Complete Navigation Tree
```
Scenario: Build full expandable navigation tree
Action: Get all descendants and organize by parent
Result: Complete hierarchical navigation
```

### 2. Bulk Operations
```
Scenario: Perform operations on entire page tree
Action: Get all descendants and iterate through list
Result: Process all pages in tree
```

### 3. Content Inventory
```
Scenario: Generate complete inventory of page section
Action: Retrieve all descendants and export metadata
Result: Complete documentation inventory
```

### 4. Hierarchy Analysis
```
Scenario: Analyze documentation structure and depth
Action: Get all descendants and calculate tree depth
Result: Understanding of documentation organization
```

## Related Catalog Requests

- [Get Page By ID](pages/GetPageById.md) - Retrieve root page details
- [Get Direct Child Pages of Page](pages/GetDirectChildPagesOfPage.md) - Get only direct children
- [Check if Page Has Children](pages/CheckIfPageHasChildren.md) - Quick check for descendants
- [Get Ancestors of Page](pages/GetAncestorsOfPage.md) - Get parent hierarchy
- [Get Direct Child Pages of Space](pages/GetDirectChildPagesOfSpace.md) - Get root-level pages

## Technical Implementation

### Helper Class
- **Class**: PageArea
- **Package**: com.kristasoft.extensions.essentials.collaboration.confluence.catalog
- **Method**: getAllNestedDescendantsOfPage(String pageID)
- **Service**: ConfluenceService.getAllDescendants(pageID)

### Processing Flow
1. Validate Page ID parameter
2. Call Confluence API recursively to get all descendants
3. For each page, check if it has children using hasChildPages()
4. Set Has Children flag on each Page entity
5. Build flat list of all descendants
6. Return Pages list and identifiers

### Telemetry Metrics
- **confluence.getAllNestedDescendantsOfPage**: Incremented on each request
- **confluence.getAllNestedDescendantsOfPage.success**: Recorded with page ID, descendant count, and duration
- **confluence.getAllNestedDescendantsOfPage.error**: Recorded on failures

### API Endpoint
```
GET https://{domain}.atlassian.net/wiki/rest/api/content/{pageId}/descendant/page
```

## Error Handling

### System Errors (SYSTEM_ERROR)

**Cause**: Network issues, Confluence API unavailability, or internal errors

**Common Scenarios**:
- Parent page doesn't exist
- Network connectivity issues
- Confluence service temporarily unavailable
- API rate limiting
- Timeout for very large page trees

**Resolution**:
1. Verify page ID is correct using Get Page By ID
2. Check network connectivity
3. Verify Confluence service status
4. Wait and retry after a few moments
5. For large trees, consider using direct child requests instead

### Authorization Errors

**Cause**: Invalid credentials or insufficient permissions

**Common Scenarios**:
- User lacks permission to view parent page
- User lacks permission to view descendant pages
- API token has been revoked

**Resolution**:
1. Verify API token is valid
2. Check page permissions
3. Request access from page/space administrator
4. Regenerate API token if necessary

## Troubleshooting

### Issue: Empty Pages List
**Cause**: Page has no descendants or user lacks permissions

**Solution**:
1. Verify parent page exists using Get Page By ID
2. Check if page has children in Confluence UI
3. Verify user has permission to view descendant pages
4. Use Check if Page Has Children to verify

### Issue: Missing Expected Pages
**Cause**: User lacks permission to view certain descendants

**Solution**:
1. Verify descendant page permissions in Confluence
2. Request access from page/space administrator
3. Check if pages are archived
4. Confirm pages haven't been moved or deleted

### Issue: Slow Response Time
**Cause**: Large page tree with many descendants

**Solution**:
1. This is expected behavior for large page trees
2. Consider caching results if calling frequently
3. Use Get Direct Child Pages of Page for incremental loading
4. Implement pagination in your application layer

### Issue: Timeout Errors
**Cause**: Extremely large page tree

**Solution**:
1. Use Get Direct Child Pages of Page instead
2. Build tree incrementally level by level
3. Implement client-side pagination
4. Contact support if tree is unreasonably large

## See Also

- [Extension Configuration](pages/ExtensionConfiguration.md) - Setup and configuration
- [Get Page By ID](pages/GetPageById.md) - Verify parent page exists
- [Get Direct Child Pages of Page](pages/GetDirectChildPagesOfPage.md) - Get one level
- [Check if Page Has Children](pages/CheckIfPageHasChildren.md) - Quick check
- [Entities](pages/Entities.md) - Page entity structure reference

