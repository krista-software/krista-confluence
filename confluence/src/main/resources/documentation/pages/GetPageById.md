# Get Page By ID

## Overview

Retrieves detailed information about a specific Confluence page using its unique identifier, including content, metadata, and hierarchy information.

## Request Details

- **Area**: Page
- **Type**: QUERY_SYSTEM
- **Retry Support**: ✅ Yes (Validation errors prompt for confirmation to retry with corrected input)

## Input Parameters

| Parameter Name | Type | Required | Description | Example |
|----------------|------|----------|-------------|---------|
| Page ID | Text, Identifier | Yes | The unique identifier of the Confluence page to retrieve | `987654` |

### Parameter Details

**Page ID**
- **Format**: Numeric string
- **Purpose**: Identifies the specific page to retrieve
- **Validation**: Must be a valid, existing page identifier
- **Where to Find**: Available from Get Pages in Space, Create Page requests, or Confluence page URL

## Output Parameters

| Parameter Name | Type | Description |
|----------------|------|-------------|
| Page | Entity(Page) | Complete Page entity with all details |

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

## Validation Rules

| Validation | Error Message | Resolution |
|------------|---------------|------------|
| Page ID is empty or null | "Page ID is required" | Provide a valid page identifier |
| Page ID format is invalid | "Invalid Page ID format" | Verify the page ID is correct |
| Page does not exist | "Page not found with ID: {id}" | Check the page ID and ensure the page exists |
| No permission to access page | "Access denied to page: {id}" | Request access from page/space administrator |

## Error Handling

### Input Errors (INPUT_ERROR)

**Cause**: Invalid or missing Page ID parameter

**Common Scenarios**:
- Page ID parameter is empty or null
- Page ID contains invalid characters
- Page ID format doesn't match expected pattern

**Error Messages**:
- "Page ID is required"
- "Invalid Page ID format"

**Resolution**:
1. Verify the Page ID is provided
2. Check the Page ID format (numeric string)
3. Use the retry confirmation to correct the input
4. Obtain valid Page ID from Get Pages in Space request

### Logic Errors (LOGIC_ERROR)

**Cause**: Page not found or access denied

**Common Scenarios**:
- Page ID doesn't exist in Confluence
- Page has been deleted or archived
- User lacks permission to view the page

**Error Messages**:
- "Page not found with ID: {id}"
- "Access denied to page: {id}"

**Resolution**:
1. Verify the page exists in Confluence
2. Check if page has been archived or deleted
3. Request access from page/space administrator
4. Use Get Pages in Space to find valid, accessible page IDs

### System Errors (SYSTEM_ERROR)

**Cause**: Confluence API issues or network problems

**Resolution**:
1. Check Confluence service status
2. Verify network connectivity
3. Wait and retry after a few moments

## Usage Examples

### Example 1: Retrieve Page Details

**Scenario**: Get complete information about a specific page.

**Input**:
```
Page ID: "987654"
```

**Output**:
```json
{
  "Page": {
    "Id": "987654",
    "Space ID": "123456",
    "Title": "Getting Started Guide",
    "Status": "current",
    "Page URL": "https://mycompany.atlassian.net/wiki/spaces/ENG/pages/987654",
    "Body": "<h1>Welcome</h1><p>This guide will help you get started...</p>",
    "Parent ID": null,
    "Parent Type": null,
    "Has Children": true
  }
}
```

**Result**: Successfully retrieved page details.

### Example 2: Validation Error with Retry

**Scenario**: Invalid page ID triggers validation error.

**Input**:
```
Page ID: "invalid-page-999"
```

**Output** (First Attempt):
```json
{
  "Error": "Page not found with ID: invalid-page-999",
  "Type": "INPUT_ERROR",
  "Retry": true
}
```

**Action**: User corrects the Page ID and retries.

**Input** (Retry):
```
Page ID: "987654"
```

**Result**: Successfully retrieved page after correction.

### Example 3: Check Page Hierarchy

**Scenario**: Retrieve page to check if it has children.

**Workflow**:
```
1. Call Get Page By ID with Page ID: "987654"
2. Check Has Children field in response
3. If Has Children = true:
   - Call Get Direct Child Pages of Page to get children
4. Build navigation structure
```

## Business Rules

1. **Permission-Based Access**: Users can only retrieve pages they have permission to view
2. **Active Pages**: Archived pages may not be retrievable depending on permissions
3. **Page ID Uniqueness**: Each page has a unique identifier within Confluence
4. **Full Content**: Complete page body is returned in storage format
5. **Hierarchy Information**: Parent and children information is included

## Limitations

1. **Single Page Only**: Retrieves one page at a time
2. **No Version History**: Does not include page version history
3. **Storage Format**: Page body is in Confluence storage format, not rendered HTML
4. **Permission Dependent**: Users with limited permissions may not access all page details
5. **No Attachments**: Does not include page attachments (use separate API for attachments)

## Best Practices

### 1. Validate Page ID Before Use
Always verify the page ID exists before using it in other operations.

### 2. Cache Page Information
If you need to reference page details multiple times, cache the result to reduce API calls.

### 3. Handle Validation Errors Gracefully
Implement proper error handling for validation failures.

### 4. Use Get Pages in Space for Discovery
When you don't know the page ID, use Get Pages in Space first to discover pages.

### 5. Check Has Children for Navigation
Use the Has Children flag to determine if you need to retrieve child pages.

## Common Use Cases

### 1. Page Verification
```
Scenario: Verify a page exists before updating
Action: Call Get Page By ID with the target page ID
Result: Confirm page exists and is accessible
```

### 2. Content Extraction
```
Scenario: Extract page content for processing
Action: Retrieve page and parse Body field
Result: Page content available for analysis
```

### 3. Navigation Building
```
Scenario: Build breadcrumb navigation
Action: Get page, check Parent ID, retrieve ancestors
Result: Complete navigation path
```

### 4. Pre-Update Validation
```
Scenario: Validate page before updating
Action: Retrieve page to get current version and content
Result: Ready for update operation
```

## Related Catalog Requests

- [Get Pages in Space](pages/GetPagesInSpace.md) - Search for pages in a space
- [Create Page from Content](pages/CreatePageFromContent.md) - Create new pages
- [Update Page by Content](pages/UpdatePageByContent.md) - Modify page content
- [Get Direct Child Pages of Page](pages/GetDirectChildPagesOfPage.md) - Get child pages
- [Get Ancestors of Page](pages/GetAncestorsOfPage.md) - Get parent hierarchy
- [Check if Page Has Children](pages/CheckIfPageHasChildren.md) - Verify children exist

## Technical Implementation

### Helper Class
- **Class**: PageArea
- **Package**: com.kristasoft.extensions.essentials.collaboration.confluence.catalog
- **Method**: getPageById(String pageID)
- **Service**: ConfluenceService.getPageById(pageID)

### Validation Flow
1. Check if Page ID is provided and not empty
2. Validate Page ID format
3. Call ValidationOrchestrator with PAGE_ID resource
4. If validation fails, store state and prompt for retry
5. If validation passes, call Confluence API
6. Check if page has children using hasChildPages()
7. Parse and return Page entity

### Telemetry Metrics
- **confluence.getPageById**: Incremented on each request
- **confluence.getPageById.success**: Recorded with page details and duration
- **confluence.getPageById.error**: Recorded on failures
- **confluence.getPageById.retryPrompted**: Recorded when validation errors trigger retry

### API Endpoint
```
GET https://{domain}.atlassian.net/wiki/rest/api/content/{pageId}
?expand=body.storage,space,ancestors
```

## Troubleshooting

### Issue: "Page not found"
**Cause**: Invalid page ID or page doesn't exist

**Solution**:
1. Verify the page ID is correct
2. Use Get Pages in Space to find valid page IDs
3. Check if page has been deleted or archived
4. Ensure you're using the correct Confluence instance

### Issue: "Access denied"
**Cause**: User lacks permission to view the page

**Solution**:
1. Request access from page/space administrator
2. Verify account has appropriate Confluence permissions
3. Check page-specific permission settings
4. Ensure account is not restricted

### Issue: Validation error loop
**Cause**: Repeatedly entering invalid page IDs

**Solution**:
1. Use Get Pages in Space to obtain valid page IDs
2. Copy page ID exactly as shown
3. Verify page exists in your Confluence instance
4. Check for typos or extra characters

### Issue: Empty page body
**Cause**: Page exists but has no content

**Solution**:
1. This is normal for newly created pages
2. Page may be a placeholder
3. Check Confluence directly to verify page content
4. Update page content if needed

## See Also

- [Extension Configuration](pages/ExtensionConfiguration.md) - Setup and configuration
- [Authentication](pages/Authentication.md) - Authentication details
- [Get Pages in Space](pages/GetPagesInSpace.md) - Search for pages
- [Update Page by Content](pages/UpdatePageByContent.md) - Modify pages
- [Entities](pages/Entities.md) - Page entity structure reference

