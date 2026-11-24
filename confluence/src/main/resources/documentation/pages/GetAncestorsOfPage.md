# Get Ancestors of Page

## Overview

Retrieves the complete parent hierarchy of a specific page, from the space root to the immediate parent, enabling breadcrumb navigation and hierarchy understanding.

## Request Details

- **Area**: Page
- **Type**: QUERY_SYSTEM
- **Retry Support**: ❌ No (Simple query operation)

## Input Parameters

| Parameter Name | Type | Required | Description | Example |
|----------------|------|----------|-------------|---------|
| Page ID | Text, Identifier | Yes | The unique identifier of the page | `987656` |

### Parameter Details

**Page ID**
- **Format**: Numeric string
- **Purpose**: Identifies the page to retrieve ancestors for
- **Validation**: Must be a valid, existing page identifier
- **Where to Find**: Available from Get Pages in Space, Get Page By ID, or Confluence page URL

## Output Parameters

| Parameter Name | Type | Description |
|----------------|------|-------------|
| Ancestors | List of Entity(Page) | List of ancestor Page entities from root to immediate parent |
| Ancestor Identifiers | List of Identifier | List of ancestor page IDs in hierarchical order |
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
| Parent Type | Text | Type of parent (typically "page") |
| Has Children | Boolean | Whether the page has child pages |

## Usage Examples

### Example 1: Get Page Ancestors

**Scenario**: Retrieve the complete parent hierarchy for breadcrumb navigation.

**Input**:
```
Page ID: "987656"
```

**Output**:
```json
{
  "Ancestors": [
    {
      "Id": "987654",
      "Space ID": "123456",
      "Title": "Getting Started",
      "Status": "current",
      "Page URL": "https://mycompany.atlassian.net/wiki/spaces/ENG/pages/987654",
      "Parent ID": null,
      "Has Children": true
    },
    {
      "Id": "987655",
      "Title": "Installation Guide",
      "Status": "current",
      "Parent ID": "987654",
      "Has Children": true
    }
  ],
  "Ancestor Identifiers": ["987654", "987655"],
  "Success": true
}
```

**Result**: Successfully retrieved 2 ancestors (root page and immediate parent).

### Example 2: Build Breadcrumb Navigation

**Scenario**: Create breadcrumb trail for a page.

**Workflow**:
```
1. Call Get Ancestors of Page with Page ID: "987656"
2. Iterate through Ancestors list in order
3. Build breadcrumb: Home > Getting Started > Installation Guide > Current Page
4. Display breadcrumb navigation
```

### Example 3: Root-Level Page (No Ancestors)

**Scenario**: Page is at root level with no parents.

**Input**:
```
Page ID: "987654"
```

**Output**:
```json
{
  "Ancestors": [],
  "Ancestor Identifiers": [],
  "Success": true
}
```

**Result**: Empty result, page is at root level.

## Business Rules

1. **Ordered List**: Ancestors are returned in order from root to immediate parent
2. **Complete Hierarchy**: All levels from root to parent are included
3. **Excludes Current Page**: The page itself is not included in ancestors
4. **Permission-Based**: Only ancestors the user has permission to view are returned
5. **Active Pages Only**: Archived or deleted ancestors are not included

## Limitations

1. **No Descendants**: Does not include child pages or siblings
2. **No Pagination**: All ancestors are returned at once
3. **Permission Dependent**: Users with limited permissions may see fewer ancestors
4. **No Filtering**: Cannot filter ancestors by any criteria
5. **Read-Only**: Does not provide information about modifying hierarchy

## Best Practices

### 1. Use for Breadcrumb Navigation
This request is ideal for building breadcrumb trails showing page location.

### 2. Cache Ancestor Lists
Ancestor hierarchies typically don't change frequently, so caching can improve performance.

### 3. Handle Empty Results
Always check if the Ancestors list is empty (indicates root-level page).

### 4. Display in Correct Order
Ancestors are returned root-first; display them in the same order for breadcrumbs.

### 5. Combine with Current Page
Add the current page to the end of the ancestor list for complete breadcrumb.

## Common Use Cases

### 1. Breadcrumb Navigation
```
Scenario: Display page location in documentation
Action: Get ancestors and build breadcrumb trail
Result: User sees: Home > Section > Subsection > Current Page
```

### 2. Hierarchy Validation
```
Scenario: Verify page is in correct location
Action: Retrieve ancestors and check expected parents
Result: Confirm page organization
```

### 3. Context Display
```
Scenario: Show page context in search results
Action: Get ancestors to display page location
Result: Users understand where page fits in documentation
```

### 4. Navigation Path
```
Scenario: Provide "back to parent" navigation
Action: Get ancestors and link to immediate parent
Result: Easy navigation up the hierarchy
```

## Related Catalog Requests

- [Get Page By ID](pages/GetPageById.md) - Retrieve current page details
- [Get Direct Child Pages of Page](pages/GetDirectChildPagesOfPage.md) - Get children
- [Get All Nested Descendants of Page](pages/GetAllNestedDescendantsOfPage.md) - Get complete tree below
- [Get Direct Child Pages of Space](pages/GetDirectChildPagesOfSpace.md) - Get root-level pages
- [Check if Page Has Children](pages/CheckIfPageHasChildren.md) - Check for children

## Technical Implementation

### Helper Class
- **Class**: PageArea
- **Package**: com.kristasoft.extensions.essentials.collaboration.confluence.catalog
- **Method**: getAncestorsOfPage(String pageID)
- **Service**: ConfluenceService.getAncestors(pageID)

### Processing Flow
1. Validate Page ID parameter
2. Call Confluence API to get page ancestors
3. For each ancestor, check if it has children using hasChildPages()
4. Set Has Children flag on each Page entity
5. Return ancestors in order from root to immediate parent

### Telemetry Metrics
- **confluence.getAncestorsOfPage**: Incremented on each request
- **confluence.getAncestorsOfPage.success**: Recorded with page ID, ancestor count, and duration
- **confluence.getAncestorsOfPage.error**: Recorded on failures

### API Endpoint
```
GET https://{domain}.atlassian.net/wiki/rest/api/content/{pageId}
?expand=ancestors
```

## Error Handling

### System Errors (SYSTEM_ERROR)

**Cause**: Network issues, Confluence API unavailability, or internal errors

**Common Scenarios**:
- Page doesn't exist
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
- User lacks permission to view the page
- User lacks permission to view ancestor pages
- API token has been revoked

**Resolution**:
1. Verify API token is valid
2. Check page permissions
3. Request access from page/space administrator
4. Regenerate API token if necessary

## Troubleshooting

### Issue: Empty Ancestors List
**Cause**: Page is at root level or user lacks permissions

**Solution**:
1. Verify page exists using Get Page By ID
2. Check if page is at root level in Confluence UI
3. Verify user has permission to view ancestor pages
4. This is normal for root-level pages

### Issue: Missing Expected Ancestors
**Cause**: User lacks permission to view certain ancestors

**Solution**:
1. Verify ancestor page permissions in Confluence
2. Request access from page/space administrator
3. Check if ancestors are archived
4. Confirm ancestors haven't been deleted

### Issue: "Page not found"
**Cause**: Invalid page ID or page doesn't exist

**Solution**:
1. Verify the page ID is correct
2. Use Get Pages in Space to find valid page IDs
3. Check if page has been deleted or archived
4. Ensure you're using the correct Confluence instance

### Issue: Incorrect Order
**Cause**: Misunderstanding of return order

**Solution**:
1. Ancestors are returned root-first (correct order for breadcrumbs)
2. First item is the root page
3. Last item is the immediate parent
4. No need to reverse the list for breadcrumb display

## See Also

- [Extension Configuration](pages/ExtensionConfiguration.md) - Setup and configuration
- [Get Page By ID](pages/GetPageById.md) - Get current page details
- [Get Direct Child Pages of Page](pages/GetDirectChildPagesOfPage.md) - Get children
- [Get All Nested Descendants of Page](pages/GetAllNestedDescendantsOfPage.md) - Get descendants
- [Entities](pages/Entities.md) - Page entity structure reference

