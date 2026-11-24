# Check if Page Has Children

## Overview

Quickly checks whether a specific Confluence page has any child pages, providing a boolean result for efficient hierarchy navigation decisions.

## Request Details

- **Area**: Page
- **Type**: QUERY_SYSTEM
- **Retry Support**: ❌ No (Simple boolean check operation)

## Input Parameters

| Parameter Name | Type | Required | Description | Example |
|----------------|------|----------|-------------|---------|
| Page ID | Text, Identifier | Yes | The unique identifier of the page to check | `987654` |

### Parameter Details

**Page ID**
- **Format**: Numeric string
- **Purpose**: Identifies the page to check for children
- **Validation**: Must be a valid, existing page identifier
- **Where to Find**: Available from Get Pages in Space, Get Page By ID, or Confluence page URL

## Output Parameters

| Parameter Name | Type | Description |
|----------------|------|-------------|
| Has Children | Boolean | true if the page has child pages, false otherwise |
| Page ID | Identifier | The page ID that was checked |

## Usage Examples

### Example 1: Page Has Children

**Scenario**: Check if a page has child pages before building navigation.

**Input**:
```
Page ID: "987654"
```

**Output**:
```json
{
  "Has Children": true,
  "Page ID": "987654"
}
```

**Result**: Page has child pages. Can proceed to retrieve them.

### Example 2: Page Has No Children

**Scenario**: Check if a page is a leaf node in the hierarchy.

**Input**:
```
Page ID: "987656"
```

**Output**:
```json
{
  "Has Children": false,
  "Page ID": "987656"
}
```

**Result**: Page has no children. It's a leaf node.

### Example 3: Conditional Navigation Building

**Scenario**: Build navigation only for pages with children.

**Workflow**:
```
1. Call Check if Page Has Children with Page ID: "987654"
2. If Has Children = true:
   - Call Get Direct Child Pages of Page
   - Build sub-navigation
3. If Has Children = false:
   - Skip child retrieval
   - Mark as leaf node
```

## Business Rules

1. **Boolean Result**: Returns simple true/false, not the actual children
2. **Permission-Based**: Only counts children the user has permission to view
3. **Active Pages Only**: Only counts current (non-archived) child pages
4. **Efficient Check**: Faster than retrieving all children when you only need to know if any exist
5. **Real-Time**: Reflects current state of page hierarchy

## Limitations

1. **No Child Details**: Does not return information about the child pages
2. **No Child Count**: Does not indicate how many children exist
3. **Permission Dependent**: May return false if user can't see any children
4. **No Nested Check**: Only checks direct children, not grandchildren

## Best Practices

### 1. Use Before Retrieving Children
Check if children exist before making the more expensive call to retrieve them.

### 2. Cache Results Appropriately
Cache the result if page hierarchy doesn't change frequently.

### 3. Use for Navigation Decisions
Determine whether to show expand/collapse icons in navigation.

### 4. Combine with Other Requests
Use in conjunction with Get Direct Child Pages of Page for efficient navigation building.

### 5. Handle Permission Scenarios
Be aware that false may mean "no children" or "no visible children due to permissions".

## Common Use Cases

### 1. Navigation UI
```
Scenario: Show expand icon only for pages with children
Action: Check if page has children
Result: Display appropriate UI element
```

### 2. Hierarchy Analysis
```
Scenario: Identify leaf nodes in documentation
Action: Check all pages for children
Result: List of pages without children
```

### 3. Conditional Processing
```
Scenario: Process pages differently based on hierarchy
Action: Check if page has children, branch logic accordingly
Result: Appropriate processing for each page type
```

### 4. Performance Optimization
```
Scenario: Avoid unnecessary API calls
Action: Check if children exist before retrieving them
Result: Reduced API calls and improved performance
```

## Related Catalog Requests

- [Get Page By ID](pages/GetPageById.md) - Retrieve page details (includes Has Children field)
- [Get Direct Child Pages of Page](pages/GetDirectChildPagesOfPage.md) - Retrieve actual child pages
- [Get All Nested Descendants of Page](pages/GetAllNestedDescendantsOfPage.md) - Get complete page tree
- [Get Direct Child Pages of Space](pages/GetDirectChildPagesOfSpace.md) - Get root-level pages

## Technical Implementation

### Helper Class
- **Class**: PageArea
- **Package**: com.kristasoft.extensions.essentials.collaboration.confluence.catalog
- **Method**: checkIfPageHasChildren(String pageID)
- **Service**: ConfluenceService.hasChildPages(pageID)

### Processing Flow
1. Validate Page ID parameter
2. Call Confluence API to check for child pages
3. Return boolean result
4. Include Page ID in response for reference

### Telemetry Metrics
- **confluence.checkIfPageHasChildren**: Incremented on each request
- **confluence.checkIfPageHasChildren.success**: Recorded with page ID, result, and duration
- **confluence.checkIfPageHasChildren.error**: Recorded on failures

### API Endpoint
```
GET https://{domain}.atlassian.net/wiki/rest/api/content/{pageId}/child/page
?limit=1
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
- API token has been revoked
- Account has been deactivated

**Resolution**:
1. Verify API token is valid
2. Check page permissions
3. Request access from page/space administrator
4. Regenerate API token if necessary

## Troubleshooting

### Issue: Always Returns false
**Cause**: User lacks permission to view child pages

**Solution**:
1. Verify user has permission to view the page
2. Check child page permissions in Confluence
3. Request access from page/space administrator
4. Verify child pages exist in Confluence UI

### Issue: "Page not found"
**Cause**: Invalid page ID or page doesn't exist

**Solution**:
1. Verify the page ID is correct
2. Use Get Page By ID to confirm page exists
3. Check if page has been deleted or archived
4. Ensure you're using the correct Confluence instance

### Issue: Result Doesn't Match UI
**Cause**: Permission differences or archived pages

**Solution**:
1. Check if child pages are archived (not counted)
2. Verify user permissions match between API and UI
3. Refresh Confluence UI to see current state
4. Check if pages were recently moved or deleted

### Issue: Inconsistent Results
**Cause**: Page hierarchy changing during checks

**Solution**:
1. This is expected if pages are being actively edited
2. Results reflect the state at the time of the request
3. Call again to get current state
4. Implement appropriate caching strategy

## See Also

- [Extension Configuration](pages/ExtensionConfiguration.md) - Setup and configuration
- [Get Page By ID](pages/GetPageById.md) - Get page details with Has Children field
- [Get Direct Child Pages of Page](pages/GetDirectChildPagesOfPage.md) - Retrieve children
- [Get All Nested Descendants of Page](pages/GetAllNestedDescendantsOfPage.md) - Get complete tree
- [Entities](pages/Entities.md) - Page entity structure reference

