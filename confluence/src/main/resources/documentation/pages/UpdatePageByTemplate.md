# Update Page by Template

## Overview

Updates an existing Confluence page using a predefined template, replacing the page content with the template structure while maintaining version history.

## Request Details

- **Area**: Page
- **Type**: CHANGE_SYSTEM
- **Retry Support**: ✅ Yes (Validation errors prompt for confirmation to retry with corrected input)

## Input Parameters

| Parameter Name | Type | Required | Description | Example |
|----------------|------|----------|-------------|---------|
| Page ID | Text, Identifier | Yes | The unique identifier of the page to update | `987654` |
| Page Title | Text | Yes | The new title for the page | `"Weekly Meeting - Jan 22"` |
| Template ID | Text, Identifier | Yes | The ID of the template to use for page content | `789012` |
| Version Number | Number | Yes | The current version number of the page (for conflict prevention) | `3` |

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

**Template ID**
- **Format**: Numeric string
- **Purpose**: Identifies the template to use for page structure and content
- **Validation**: Must be a valid, existing template identifier
- **Where to Find**: Use Get Templates By Space ID to discover templates
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

## Validation Rules

| Validation | Error Message | Resolution |
|------------|---------------|------------|
| Page ID is empty or null | "Page ID is required" | Provide a valid page identifier |
| Page Title is empty or null | "Page Title is required" | Provide a page title |
| Template ID is empty or null | "Template ID is required" | Provide a template identifier |
| Version Number is missing | "Version Number is required" | Provide current version number |
| Page does not exist | "Page not found with ID: {id}" | Verify page exists |
| Template does not exist | "Template not found with ID: {id}" | Verify template exists |
| Version mismatch | "Version conflict: expected {expected}, got {actual}" | Retrieve current version and retry |
| Duplicate page title | "Page with title '{title}' already exists" | Use a different title |

## Error Handling

### Input Errors (INPUT_ERROR)

**Cause**: Invalid or missing required parameters

**Common Scenarios**:
- Missing Page ID, Page Title, Template ID, or Version Number
- Invalid Page ID or Template ID format
- Empty or null required fields

**Error Messages**:
- "Page ID is required"
- "Page Title is required"
- "Template ID is required"
- "Version Number is required"

**Resolution**:
1. Verify all required parameters are provided
2. Check parameter formats are correct
3. Use the retry confirmation to correct the input
4. Use Get Page By ID to retrieve current version number
5. Use Get Templates By Space ID to find valid template IDs

### Logic Errors (LOGIC_ERROR)

**Cause**: Business logic violations

**Common Scenarios**:
- Page doesn't exist
- Template doesn't exist
- Version number doesn't match current version
- Duplicate page title in same location

**Error Messages**:
- "Page not found with ID: {id}"
- "Template not found with ID: {id}"
- "Version conflict: expected {expected}, got {actual}"
- "Page with title '{title}' already exists"

**Resolution**:
1. Verify page and template exist
2. Retrieve current version number before updating
3. Use unique page titles
4. Handle concurrent update conflicts

### System Errors (SYSTEM_ERROR)

**Cause**: Confluence API issues or network problems

**Resolution**:
1. Check Confluence service status
2. Verify network connectivity
3. Wait and retry after a few moments

## Usage Examples

### Example 1: Update Page with Template

**Scenario**: Update an existing page to use a standard template structure.

**Input**:
```
Page ID: "987654"
Page Title: "Weekly Team Meeting - January 22, 2024"
Template ID: "789012"
Version Number: 3
```

**Output**:
```json
{
  "Page": {
    "Id": "987654",
    "Space ID": "123456",
    "Title": "Weekly Team Meeting - January 22, 2024",
    "Status": "current",
    "Page URL": "https://mycompany.atlassian.net/wiki/spaces/ENG/pages/987654",
    "Body": "<h2>Attendees</h2><ul><li></li></ul><h2>Agenda</h2><ol><li></li></ol>",
    "Has Children": false
  },
  "Page ID": "987654",
  "Page URL": "https://mycompany.atlassian.net/wiki/spaces/ENG/pages/987654",
  "New Version": 4
}
```

**Result**: Successfully updated page with template structure, version incremented from 3 to 4.

### Example 2: Version Conflict Error

**Scenario**: Version mismatch triggers conflict error.

**Input**:
```
Page ID: "987654"
Page Title: "Updated Meeting Notes"
Template ID: "789012"
Version Number: 1
```

**Output** (First Attempt):
```json
{
  "Error": "Version conflict: expected 3, got 1",
  "Type": "LOGIC_ERROR",
  "Retry": true
}
```

**Action**: User retrieves current version using Get Page By ID and retries.

**Input** (Retry):
```
Page ID: "987654"
Page Title: "Updated Meeting Notes"
Template ID: "789012"
Version Number: 3
```

**Result**: Successfully updated page after providing correct version.

### Example 3: Template Not Found Error

**Scenario**: Invalid template ID triggers validation error.

**Input**:
```
Page ID: "987654"
Page Title: "New Format"
Template ID: "invalid-template"
Version Number: 2
```

**Output** (First Attempt):
```json
{
  "Error": "Template not found with ID: invalid-template",
  "Type": "INPUT_ERROR",
  "Retry": true
}
```

**Action**: User finds valid template using Get Templates By Space ID and retries.

**Result**: Successfully updated page after providing valid template ID.

## Business Rules

1. **Version Control**: Version number must match current page version
2. **Unique Titles**: Page titles must be unique within the same parent or space root
3. **Permission Required**: User must have permission to edit the page
4. **Template Validation**: Template must exist and be accessible
5. **Version Increment**: Version number automatically increments after successful update
6. **Content Replacement**: Existing page content is completely replaced with template content
7. **History Preserved**: Previous versions are preserved in page history

## Limitations

1. **Complete Replacement**: Template replaces all existing content (cannot merge)
2. **Single Page Only**: Updates one page at a time
3. **Template Required**: Must use existing template
4. **Title Length**: Maximum 255 characters for page title
5. **Concurrent Updates**: Version conflicts occur if page is updated by another user
6. **No Draft Support**: Updates are immediately published
7. **Same Space**: Template should be from the same space or globally accessible

## Best Practices

### 1. Always Retrieve Current Version First
Use Get Page By ID to get the current version number before updating to avoid conflicts.

### 2. Validate Template Before Use
Use Get Template By ID to verify template exists and review its structure.

### 3. Handle Version Conflicts Gracefully
Implement retry logic that retrieves the latest version when conflicts occur.

### 4. Backup Important Content
Consider backing up current page content before replacing with template.

### 5. Use for Standardization
Use this request to standardize page formats across your documentation.

## Common Use Cases

### 1. Format Standardization
```
Scenario: Update old pages to use new standard template
Action: Retrieve pages, update each with new template
Result: Consistent formatting across documentation
```

### 2. Recurring Content Reset
```
Scenario: Reset weekly meeting notes to blank template
Action: Update page with meeting notes template
Result: Fresh template ready for new content
```

### 3. Template Migration
```
Scenario: Migrate pages to improved template version
Action: Update pages with new template ID
Result: Pages use updated template structure
```

### 4. Bulk Reformatting
```
Scenario: Apply template to multiple pages
Action: Iterate through pages, update each with template
Result: Batch reformatting complete
```

## Related Catalog Requests

- [Get Page By ID](pages/GetPageById.md) - Retrieve current page version
- [Update Page by Content](pages/UpdatePageByContent.md) - Update with custom content
- [Get Templates By Space ID](pages/GetTemplatesBySpaceId.md) - Discover available templates
- [Get Template By ID](pages/GetTemplateById.md) - Verify template details
- [Create Page from Template](pages/CreatePageFromTemplate.md) - Create new pages with template

## Technical Implementation

### Helper Class
- **Class**: PageArea
- **Package**: com.kristasoft.extensions.essentials.collaboration.confluence.catalog
- **Method**: updatePageFromTemplate(String pageID, String pageTitle, String templateID, Number versionNumber)
- **Service**: ConfluenceService.updatePage()

### Validation Flow
1. Validate required parameters (Page ID, Page Title, Template ID, Version Number)
2. Call ValidationOrchestrator for Page ID and Template ID
3. If validation fails, store state and prompt for retry
4. If validation passes, retrieve template content
5. Build page update request with template body
6. Call Confluence API to update page
7. Handle version conflicts
8. Parse response and return updated Page entity

### Telemetry Metrics
- **confluence.updatePageFromTemplate**: Incremented on each request
- **confluence.updatePageFromTemplate.success**: Recorded with page details and duration
- **confluence.updatePageFromTemplate.error**: Recorded on failures
- **confluence.updatePageFromTemplate.versionConflict**: Recorded when version conflicts occur
- **confluence.updatePageFromTemplate.retryPrompted**: Recorded when validation errors trigger retry

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

### Issue: "Template not found"
**Cause**: Invalid Template ID

**Solution**:
1. Use Get Templates By Space ID to find valid templates
2. Verify template ID is correct
3. Check template hasn't been deleted
4. Ensure template is accessible from the page's space

### Issue: "Page with title already exists"
**Cause**: Duplicate page title in same location

**Solution**:
1. Use a different, unique page title
2. Check existing pages in the location
3. Keep the original title if not changing
4. Add distinguishing information to title

### Issue: "Access denied"
**Cause**: Insufficient permissions

**Solution**:
1. Verify user has permission to edit the page
2. Check page-level permissions
3. Verify user has access to the template
4. Request access from page/space administrator

## See Also

- [Extension Configuration](pages/ExtensionConfiguration.md) - Setup and configuration
- [Get Page By ID](pages/GetPageById.md) - Retrieve current version
- [Update Page by Content](pages/UpdatePageByContent.md) - Update with custom content
- [Get Templates By Space ID](pages/GetTemplatesBySpaceId.md) - Find templates
- [Entities](pages/Entities.md) - Page entity structure reference

