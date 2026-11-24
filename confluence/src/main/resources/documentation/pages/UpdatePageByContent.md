# Update Page by Content

## Overview

Updates an existing Confluence page with new custom content, allowing full control over page title and body while maintaining version history.

## Request Details

- **Area**: Page
- **Type**: CHANGE_SYSTEM
- **Retry Support**: ✅ Yes (Validation errors prompt for confirmation to retry with corrected input)

## Input Parameters

| Parameter Name | Type | Required | Description | Example |
|----------------|------|----------|-------------|---------|
| Page ID | Text, Identifier | Yes | The unique identifier of the page to update | `987654` |
| Page Title | Text | Yes | The new title for the page | `"Getting Started Guide (Updated)"` |
| Page Body | Rich Text | Yes | The new content for the page in Confluence storage format | `"<h1>Welcome</h1><p>Updated content...</p>"` |
| Version Number | Number | Yes | The current version number of the page (for conflict prevention) | `5` |

### Parameter Details

**Page ID**
- **Format**: Numeric string
- **Purpose**: Identifies the page to update
- **Validation**: Must be a valid, existing page identifier
- **Required**: Yes

**Page Title**
- **Format**: Text string
- **Purpose**: The new display title for the page
- **Validation**: Must be unique within the parent page or space root
- **Character Limit**: Maximum 255 characters
- **Required**: Yes

**Page Body**
- **Format**: Rich Text in Confluence storage format
- **Purpose**: The new content for the page
- **Format**: HTML-like Confluence storage format with macros
- **Can Include**: Text, headings, lists, tables, macros, images
- **Required**: Yes

**Version Number**
- **Format**: Positive integer
- **Purpose**: Prevents concurrent update conflicts
- **Validation**: Must match the current version of the page
- **How to Get**: Retrieve from Get Page By ID request
- **Required**: Yes

## Output Parameters

| Parameter Name | Type | Description |
|----------------|------|-------------|
| Page | Entity(Page) | The updated Page entity with all details |
| Page ID | Identifier | The unique identifier of the updated page |
| Page URL | Rich Text | The full URL to access the page |
| New Version | Number | The new version number after update |

### Page Entity Structure

| Field | Type | Description |
|-------|------|-------------|
| Id | Text | Unique identifier for the page |
| Space ID | Identifier | ID of the space containing the page |
| Title | Text | Updated page title |
| Status | Text | Page status (typically "current") |
| Page URL | Rich Text | Full URL to access the page |
| Body | Rich Text | Updated page content in storage format |
| Parent ID | Text | ID of parent page (unchanged) |
| Parent Type | Text | Type of parent (unchanged) |
| Has Children | Boolean | Whether the page has child pages |

## Validation Rules

| Validation | Error Message | Resolution |
|------------|---------------|------------|
| Page ID is empty or null | "Page ID is required" | Provide a valid page identifier |
| Page Title is empty or null | "Page Title is required" | Provide a page title |
| Page Body is empty or null | "Page Body is required" | Provide page content |
| Version Number is missing | "Version Number is required" | Provide current version number |
| Page does not exist | "Page not found with ID: {id}" | Verify page exists |
| Version mismatch | "Version conflict: expected {expected}, got {actual}" | Retrieve current version and retry |
| Duplicate page title | "Page with title '{title}' already exists" | Use a different title |
| Invalid storage format | "Invalid page body format" | Check Confluence storage format syntax |

## Error Handling

### Input Errors (INPUT_ERROR)

**Cause**: Invalid or missing required parameters

**Common Scenarios**:
- Missing Page ID, Page Title, Page Body, or Version Number
- Invalid Page ID format
- Empty or null required fields

**Error Messages**:
- "Page ID is required"
- "Page Title is required"
- "Page Body is required"
- "Version Number is required"

**Resolution**:
1. Verify all required parameters are provided
2. Check parameter formats are correct
3. Use the retry confirmation to correct the input
4. Use Get Page By ID to retrieve current version number

### Logic Errors (LOGIC_ERROR)

**Cause**: Business logic violations

**Common Scenarios**:
- Page doesn't exist
- Version number doesn't match current version
- Duplicate page title in same location
- Invalid Confluence storage format

**Error Messages**:
- "Page not found with ID: {id}"
- "Version conflict: expected {expected}, got {actual}"
- "Page with title '{title}' already exists"

**Resolution**:
1. Verify page exists using Get Page By ID
2. Retrieve current version number before updating
3. Use unique page titles
4. Validate Confluence storage format
5. Handle concurrent update conflicts

### System Errors (SYSTEM_ERROR)

**Cause**: Confluence API issues or network problems

**Resolution**:
1. Check Confluence service status
2. Verify network connectivity
3. Wait and retry after a few moments

## Usage Examples

### Example 1: Update Page Content

**Scenario**: Update an existing page with new content.

**Input**:
```
Page ID: "987654"
Page Title: "Getting Started Guide (Updated)"
Page Body: "<h1>Welcome</h1><p>This is the updated content with new information.</p><h2>New Section</h2><p>Additional details...</p>"
Version Number: 5
```

**Output**:
```json
{
  "Page": {
    "Id": "987654",
    "Space ID": "123456",
    "Title": "Getting Started Guide (Updated)",
    "Status": "current",
    "Page URL": "https://mycompany.atlassian.net/wiki/spaces/ENG/pages/987654",
    "Body": "<h1>Welcome</h1><p>This is the updated content...</p>",
    "Parent ID": null,
    "Has Children": true
  },
  "Page ID": "987654",
  "Page URL": "https://mycompany.atlassian.net/wiki/spaces/ENG/pages/987654",
  "New Version": 6
}
```

**Result**: Successfully updated page, version incremented from 5 to 6.

### Example 2: Version Conflict Error

**Scenario**: Version mismatch triggers conflict error.

**Input**:
```
Page ID: "987654"
Page Title: "Getting Started Guide"
Page Body: "<h1>Updated Content</h1>"
Version Number: 3
```

**Output** (First Attempt):
```json
{
  "Error": "Version conflict: expected 5, got 3",
  "Type": "LOGIC_ERROR",
  "Retry": true
}
```

**Action**: User retrieves current version using Get Page By ID and retries.

**Input** (Retry):
```
Page ID: "987654"
Page Title: "Getting Started Guide"
Page Body: "<h1>Updated Content</h1>"
Version Number: 5
```

**Result**: Successfully updated page after providing correct version.

### Example 3: Update with Macros

**Scenario**: Update page with Confluence macros.

**Input**:
```
Page ID: "987654"
Page Title: "Important Notice"
Page Body: "<ac:structured-macro ac:name=\"warning\"><ac:rich-text-body><p>This page has been updated with critical information.</p></ac:rich-text-body></ac:structured-macro>"
Version Number: 2
```

**Result**: Page updated with warning macro displayed.

## Business Rules

1. **Version Control**: Version number must match current page version
2. **Unique Titles**: Page titles must be unique within the same parent or space root
3. **Permission Required**: User must have permission to edit the page
4. **Version Increment**: Version number automatically increments after successful update
5. **Storage Format**: Page body must be in valid Confluence storage format
6. **History Preserved**: Previous versions are preserved in page history

## Limitations

1. **No Partial Updates**: Must provide complete page content (cannot update only title or only body)
2. **Single Page Only**: Updates one page at a time
3. **Storage Format Required**: Must use Confluence storage format, not plain text or markdown
4. **Title Length**: Maximum 255 characters for page title
5. **Concurrent Updates**: Version conflicts occur if page is updated by another user
6. **No Draft Support**: Updates are immediately published

## Best Practices

### 1. Always Retrieve Current Version First
Use Get Page By ID to get the current version number before updating to avoid conflicts.

### 2. Handle Version Conflicts Gracefully
Implement retry logic that retrieves the latest version when conflicts occur.

### 3. Validate Content Format
Ensure page body is in valid Confluence storage format before updating.

### 4. Preserve Important Content
Consider retrieving current content and merging changes rather than replacing entirely.

### 5. Use Descriptive Titles
Update titles to reflect content changes when appropriate.

## Common Use Cases

### 1. Content Refresh
```
Scenario: Update outdated documentation with current information
Action: Retrieve page, modify content, update with new version
Result: Documentation kept up-to-date
```

### 2. Automated Content Updates
```
Scenario: Automatically update pages with generated content
Action: Format content in storage format, update page
Result: Automated documentation maintenance
```

### 3. Bulk Content Migration
```
Scenario: Migrate updated content from external system
Action: Convert content to storage format, update pages
Result: Content synchronized across systems
```

### 4. Collaborative Editing
```
Scenario: Multiple users editing same page
Action: Retrieve current version, make changes, handle conflicts
Result: Coordinated updates with conflict resolution
```

## Related Catalog Requests

- [Get Page By ID](../pages/GetPageById.md) - Retrieve current page version
- [Update Page from Template](pages/UpdatePageByTemplate.md) - Update using a template
- [Create Page from Content](pages/CreatePageFromContent.md) - Create new pages
- [Generate Content for Table](pages/GenerateContentForTable.md) - Generate table content
- [Generate Heading Content](pages/GenerateHeadingContent.md) - Generate heading content

## Technical Implementation

### Helper Class
- **Class**: PageArea
- **Package**: com.kristasoft.extensions.essentials.collaboration.confluence.catalog
- **Method**: updatePageByContent(String pageID, String pageTitle, RichText pageBody, Number versionNumber)
- **Service**: ConfluenceService.updatePage()

### Validation Flow
1. Validate required parameters (Page ID, Page Title, Page Body, Version Number)
2. Call ValidationOrchestrator for Page ID
3. If validation fails, store state and prompt for retry
4. If validation passes, build page update request
5. Call Confluence API to update page
6. Handle version conflicts
7. Parse response and return updated Page entity

### Telemetry Metrics
- **confluence.updatePageByContent**: Incremented on each request
- **confluence.updatePageByContent.success**: Recorded with page details and duration
- **confluence.updatePageByContent.error**: Recorded on failures
- **confluence.updatePageByContent.versionConflict**: Recorded when version conflicts occur
- **confluence.updatePageByContent.retryPrompted**: Recorded when validation errors trigger retry

### API Endpoint
```
PUT https://{domain}.atlassian.net/wiki/rest/api/content/{pageId}
```

## Troubleshooting

### Issue: "Version conflict"
**Cause**: Version number doesn't match current page version

**Solution**:
1. Use Get Page By ID to retrieve current version
2. Update with the correct version number
3. Implement automatic version retrieval in your workflow
4. Handle concurrent updates appropriately

### Issue: "Page with title already exists"
**Cause**: Duplicate page title in same location

**Solution**:
1. Use a different, unique page title
2. Check existing pages in the location
3. Keep the original title if not changing
4. Add distinguishing information to title

### Issue: "Invalid page body format"
**Cause**: Malformed Confluence storage format

**Solution**:
1. Validate HTML structure in page body
2. Check macro syntax is correct
3. Use content generation helpers for complex content
4. Test with simple content first

### Issue: "Access denied"
**Cause**: Insufficient permissions

**Solution**:
1. Verify user has permission to edit the page
2. Check page-level permissions
3. Request access from page/space administrator
4. Ensure account is not restricted

## See Also

- [Extension Configuration](pages/ExtensionConfiguration.md) - Setup and configuration
- [Get Page By ID](pages/GetPageById.md) - Retrieve current version
- [Update Page from Template](pages/UpdatePageByTemplate.md) - Update with template
- [Create Page from Content](pages/CreatePageFromContent.md) - Create pages
- [Entities](pages/Entities.md) - Page entity structure reference

