# Download Page

## Overview

Retrieves and exports a Confluence page by space ID and page title, providing complete page content and metadata for backup, migration, or offline access.

## Request Details

- **Area**: Page
- **Type**: QUERY_SYSTEM
- **Retry Support**: ✅ Yes (Validation errors prompt for confirmation to retry with corrected input)

## Input Parameters

| Parameter Name | Type | Required | Description | Example |
|----------------|------|----------|-------------|---------|
| Space ID | Text, Identifier | Yes | The unique identifier of the space containing the page | `123456` |
| Page Title | Text | Yes | The exact title of the page to download | `"Getting Started Guide"` |

### Parameter Details

**Space ID**
- **Format**: Numeric string or space key
- **Purpose**: Identifies the space containing the page
- **Validation**: Must be a valid, existing space identifier
- **Required**: Yes

**Page Title**
- **Format**: Text string
- **Purpose**: Identifies the specific page to download
- **Validation**: Must match an existing page title exactly
- **Case Sensitivity**: Case-sensitive matching
- **Required**: Yes

## Output Parameters

| Parameter Name | Type | Description |
|----------------|------|-------------|
| Page | Entity(Page) | Complete Page entity with all details |
| Page ID | Identifier | The unique identifier of the page |
| Page URL | Rich Text | The full URL to access the page |

### Page Entity Structure

| Field | Type | Description |
|-------|------|-------------|
| Id | Text | Unique identifier for the page |
| Space ID | Identifier | ID of the space containing the page |
| Title | Text | Page title |
| Status | Text | Page status (e.g., "current", "draft") |
| Page URL | Rich Text | Full URL to access the page |
| Body | Rich Text | Complete page content in storage format |
| Parent ID | Text | ID of parent page (null for root pages) |
| Parent Type | Text | Type of parent (typically "page") |
| Has Children | Boolean | Whether the page has child pages |

## Validation Rules

| Validation | Error Message | Resolution |
|------------|---------------|------------|
| Space ID is empty or null | "Space ID is required" | Provide a valid space identifier |
| Page Title is empty or null | "Page Title is required" | Provide a page title |
| Space does not exist | "Space not found with ID: {id}" | Verify space exists |
| Page not found in space | "Page '{title}' not found in space {id}" | Verify page title is correct |
| No permission to access page | "Access denied to page '{title}'" | Request access from administrator |

## Error Handling

### Input Errors (INPUT_ERROR)

**Cause**: Invalid or missing required parameters

**Common Scenarios**:
- Missing Space ID or Page Title
- Invalid Space ID format
- Empty or null required fields

**Error Messages**:
- "Space ID is required"
- "Page Title is required"

**Resolution**:
1. Verify all required parameters are provided
2. Check parameter formats are correct
3. Use the retry confirmation to correct the input

### Logic Errors (LOGIC_ERROR)

**Cause**: Business logic violations

**Common Scenarios**:
- Space doesn't exist
- Page with specified title not found in space
- Multiple pages with same title (ambiguous)
- User lacks permission to view page

**Error Messages**:
- "Space not found with ID: {id}"
- "Page '{title}' not found in space {id}"
- "Access denied to page '{title}'"

**Resolution**:
1. Verify space exists using Get Space By ID
2. Check page title is exact (case-sensitive)
3. Use Get Pages in Space to find correct title
4. Request access from page/space administrator

### System Errors (SYSTEM_ERROR)

**Cause**: Confluence API issues or network problems

**Resolution**:
1. Check Confluence service status
2. Verify network connectivity
3. Wait and retry after a few moments

## Usage Examples

### Example 1: Download Page by Title

**Scenario**: Export a specific page for backup.

**Input**:
```
Space ID: "123456"
Page Title: "Getting Started Guide"
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
    "Body": "<h1>Welcome</h1><p>This guide will help you get started...</p><h2>Prerequisites</h2><ul><li>Account access</li><li>Basic knowledge</li></ul>",
    "Parent ID": null,
    "Has Children": true
  },
  "Page ID": "987654",
  "Page URL": "https://mycompany.atlassian.net/wiki/spaces/ENG/pages/987654"
}
```

**Result**: Successfully downloaded page with complete content.

### Example 2: Validation Error with Retry

**Scenario**: Page title not found triggers validation error.

**Input**:
```
Space ID: "123456"
Page Title: "Getting Started"
```

**Output** (First Attempt):
```json
{
  "Error": "Page 'Getting Started' not found in space 123456",
  "Type": "LOGIC_ERROR",
  "Retry": true
}
```

**Action**: User corrects the page title and retries.

**Input** (Retry):
```
Space ID: "123456"
Page Title: "Getting Started Guide"
```

**Result**: Successfully downloaded page after correction.

### Example 3: Case-Sensitive Title Matching

**Scenario**: Title case must match exactly.

**Input**:
```
Space ID: "123456"
Page Title: "getting started guide"
```

**Output**:
```json
{
  "Error": "Page 'getting started guide' not found in space 123456",
  "Type": "LOGIC_ERROR",
  "Retry": true
}
```

**Action**: User provides exact title with correct case.

**Result**: Title must match exactly including capitalization.

## Business Rules

1. **Exact Title Match**: Page title must match exactly (case-sensitive)
2. **Space Scoped**: Searches only within the specified space
3. **Permission-Based**: User must have permission to view the page
4. **Active Pages Only**: Archived pages may not be retrievable
5. **Complete Content**: Full page body is returned in storage format
6. **Current Version**: Returns the current published version

## Limitations

1. **Single Page Only**: Downloads one page at a time
2. **No Bulk Download**: Cannot download multiple pages in one request
3. **Title Required**: Cannot download by page ID (use Get Page By ID for that)
4. **Case-Sensitive**: Title matching is case-sensitive
5. **No Attachments**: Does not include page attachments
6. **No Version History**: Returns only current version, not history
7. **Storage Format Only**: Content is in Confluence storage format, not rendered HTML

## Best Practices

### 1. Use Exact Page Titles
Always use the exact page title including capitalization and punctuation.

### 2. Validate Space First
Use Get Space By ID to verify the space exists before downloading pages.

### 3. Find Correct Title
Use Get Pages in Space to discover the exact page title if unsure.

### 4. Handle Case Sensitivity
Be aware that title matching is case-sensitive; copy titles exactly.

### 5. Use for Backup
This request is ideal for backing up page content for archival purposes.

## Common Use Cases

### 1. Page Backup
```
Scenario: Backup important documentation pages
Action: Download pages and store content locally
Result: Offline backup of page content
```

### 2. Content Migration
```
Scenario: Migrate pages to another system
Action: Download pages and convert to target format
Result: Content migrated to new platform
```

### 3. Offline Access
```
Scenario: Provide offline access to documentation
Action: Download pages and generate offline version
Result: Documentation available without internet
```

### 4. Content Analysis
```
Scenario: Analyze page content for reporting
Action: Download pages and parse content
Result: Content metrics and analysis
```

## Related Catalog Requests

- [Get Page By ID](pages/GetPageById.md) - Download page by ID instead of title
- [Get Pages in Space](pages/GetPagesInSpace.md) - Find pages and their exact titles
- [Get Space By ID](pages/GetSpaceById.md) - Verify space exists
- [Create Page from Content](pages/CreatePageFromContent.md) - Restore downloaded content

## Technical Implementation

### Helper Class
- **Class**: PageArea
- **Package**: com.kristasoft.extensions.essentials.collaboration.confluence.catalog
- **Method**: getPageInSpace(String spaceID, String pageTitle)
- **Service**: ConfluenceService.getPageByTitle(spaceID, pageTitle)

### Validation Flow
1. Validate required parameters (Space ID, Page Title)
2. Call ValidationOrchestrator for Space ID
3. If validation fails, store state and prompt for retry
4. If validation passes, search for page by title in space
5. If page not found, return error with retry option
6. If page found, check if it has children using hasChildPages()
7. Parse and return Page entity

### Telemetry Metrics
- **confluence.getPageInSpace**: Incremented on each request
- **confluence.getPageInSpace.success**: Recorded with space ID, page title, and duration
- **confluence.getPageInSpace.error**: Recorded on failures
- **confluence.getPageInSpace.retryPrompted**: Recorded when validation errors trigger retry

### API Endpoint
```
GET https://{domain}.atlassian.net/wiki/rest/api/content
?spaceKey={spaceId}&title={pageTitle}&type=page&expand=body.storage
```

## Troubleshooting

### Issue: "Page not found"
**Cause**: Page title doesn't match exactly

**Solution**:
1. Use Get Pages in Space to find exact page title
2. Copy page title exactly including case
3. Check for extra spaces or special characters
4. Verify page exists in the specified space

### Issue: Case sensitivity problems
**Cause**: Title case doesn't match

**Solution**:
1. Page titles are case-sensitive
2. "Getting Started" ≠ "getting started"
3. Copy title exactly from Confluence UI
4. Use Get Pages in Space to get exact title

### Issue: "Access denied"
**Cause**: Insufficient permissions

**Solution**:
1. Verify user has permission to view the page
2. Check space-level permissions
3. Request access from page/space administrator
4. Ensure account is not restricted

### Issue: Multiple pages with same title
**Cause**: Ambiguous page title

**Solution**:
1. This is rare but possible in Confluence
2. Use Get Page By ID instead if you know the page ID
3. Rename one of the pages to make titles unique
4. Use more specific search criteria

## See Also

- [Extension Configuration](pages/ExtensionConfiguration.md) - Setup and configuration
- [Get Page By ID](pages/GetPageById.md) - Download by ID instead
- [Get Pages in Space](pages/GetPagesInSpace.md) - Find exact page titles
- [Get Space By ID](pages/GetSpaceById.md) - Verify space exists
- [Entities](pages/Entities.md) - Page entity structure reference

