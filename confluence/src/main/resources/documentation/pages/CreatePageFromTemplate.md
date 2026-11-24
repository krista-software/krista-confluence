# Create Page from Template

## Overview

Creates a new Confluence page using a predefined template, ensuring consistent page structure and formatting across your documentation.

## Request Details

- **Area**: Page
- **Type**: CHANGE_SYSTEM
- **Retry Support**: ✅ Yes (Validation errors prompt for confirmation to retry with corrected input)

## Input Parameters

| Parameter Name | Type | Required | Description | Example |
|----------------|------|----------|-------------|---------|
| Space ID | Text, Identifier | Yes | The unique identifier of the space where the page will be created | `123456` |
| Page Title | Text | Yes | The title of the new page | `"Weekly Team Meeting - Jan 15"` |
| Template ID | Text, Identifier | Yes | The ID of the template to use for page creation | `789012` |
| Parent Page ID | Text, Identifier | No | The ID of the parent page (if creating a child page) | `987654` |

### Parameter Details

**Space ID**
- **Format**: Numeric string or space key
- **Purpose**: Identifies the space where the page will be created
- **Validation**: Must be a valid, existing space identifier
- **Required**: Yes

**Page Title**
- **Format**: Text string
- **Purpose**: The display title of the new page
- **Validation**: Must be unique within the parent page or space root
- **Character Limit**: Maximum 255 characters
- **Required**: Yes

**Template ID**
- **Format**: Numeric string
- **Purpose**: Identifies the template to use for page structure and content
- **Validation**: Must be a valid, existing template identifier
- **Where to Find**: Use Get Templates By Space ID to discover templates
- **Required**: Yes

**Parent Page ID**
- **Format**: Numeric string
- **Purpose**: Creates the page as a child of the specified parent
- **Optional**: If not provided, page is created at space root level
- **Validation**: Parent page must exist in the same space

## Output Parameters

| Parameter Name | Type | Description |
|----------------|------|-------------|
| Page | Entity(Page) | The newly created Page entity with all details |
| Page ID | Identifier | The unique identifier of the created page |
| Page URL | Rich Text | The full URL to access the new page |

### Page Entity Structure

| Field | Type | Description |
|-------|------|-------------|
| Id | Text | Unique identifier for the page |
| Space ID | Identifier | ID of the space containing the page |
| Title | Text | Page title |
| Status | Text | Page status (typically "current") |
| Page URL | Rich Text | Full URL to access the page |
| Body | Rich Text | Page content from template in storage format |
| Parent ID | Text | ID of parent page (if any) |
| Parent Type | Text | Type of parent (typically "page") |
| Has Children | Boolean | Whether the page has child pages (false for new pages) |

## Validation Rules

| Validation | Error Message | Resolution |
|------------|---------------|------------|
| Space ID is empty or null | "Space ID is required" | Provide a valid space identifier |
| Page Title is empty or null | "Page Title is required" | Provide a page title |
| Template ID is empty or null | "Template ID is required" | Provide a template identifier |
| Space does not exist | "Space not found with ID: {id}" | Verify space exists |
| Template does not exist | "Template not found with ID: {id}" | Verify template exists |
| Parent page does not exist | "Parent page not found with ID: {id}" | Verify parent page exists |
| Duplicate page title | "Page with title '{title}' already exists" | Use a different title |

## Error Handling

### Input Errors (INPUT_ERROR)

**Cause**: Invalid or missing required parameters

**Common Scenarios**:
- Missing Space ID, Page Title, or Template ID
- Invalid Space ID, Template ID, or Parent Page ID format
- Empty or null required fields

**Error Messages**:
- "Space ID is required"
- "Page Title is required"
- "Template ID is required"

**Resolution**:
1. Verify all required parameters are provided
2. Check parameter formats are correct
3. Use the retry confirmation to correct the input
4. Use Get Templates By Space ID to find valid template IDs

### Logic Errors (LOGIC_ERROR)

**Cause**: Business logic violations

**Common Scenarios**:
- Space doesn't exist
- Template doesn't exist
- Parent page doesn't exist
- Duplicate page title in same location

**Error Messages**:
- "Space not found with ID: {id}"
- "Template not found with ID: {id}"
- "Parent page not found with ID: {id}"
- "Page with title '{title}' already exists"

**Resolution**:
1. Verify space, template, and parent page exist
2. Use unique page titles
3. Check permissions to create pages
4. Use Get Template By ID to verify template

### System Errors (SYSTEM_ERROR)

**Cause**: Confluence API issues or network problems

**Resolution**:
1. Check Confluence service status
2. Verify network connectivity
3. Wait and retry after a few moments

## Usage Examples

### Example 1: Create Page from Meeting Notes Template

**Scenario**: Create a new meeting notes page using a standard template.

**Input**:
```
Space ID: "123456"
Page Title: "Weekly Team Meeting - January 15, 2024"
Template ID: "789012"
Parent Page ID: (not provided)
```

**Output**:
```json
{
  "Page": {
    "Id": "987654",
    "Space ID": "123456",
    "Title": "Weekly Team Meeting - January 15, 2024",
    "Status": "current",
    "Page URL": "https://mycompany.atlassian.net/wiki/spaces/ENG/pages/987654",
    "Body": "<h2>Attendees</h2><ul><li></li></ul><h2>Agenda</h2><ol><li></li></ol><h2>Action Items</h2><table>...</table>",
    "Parent ID": null,
    "Parent Type": null,
    "Has Children": false
  },
  "Page ID": "987654",
  "Page URL": "https://mycompany.atlassian.net/wiki/spaces/ENG/pages/987654"
}
```

**Result**: Successfully created meeting notes page with template structure.

### Example 2: Create Child Page from Template

**Scenario**: Create a child page under an existing parent using a template.

**Input**:
```
Space ID: "123456"
Page Title: "Q1 2024 Planning"
Template ID: "789013"
Parent Page ID: "987654"
```

**Output**:
```json
{
  "Page": {
    "Id": "987655",
    "Space ID": "123456",
    "Title": "Q1 2024 Planning",
    "Status": "current",
    "Page URL": "https://mycompany.atlassian.net/wiki/spaces/ENG/pages/987655",
    "Body": "<h1>Overview</h1><p>...</p><h2>Goals</h2><ul>...</ul>",
    "Parent ID": "987654",
    "Parent Type": "page",
    "Has Children": false
  },
  "Page ID": "987655",
  "Page URL": "https://mycompany.atlassian.net/wiki/spaces/ENG/pages/987655"
}
```

**Result**: Successfully created child page with template structure.

### Example 3: Validation Error with Retry

**Scenario**: Invalid template ID triggers validation error.

**Input**:
```
Space ID: "123456"
Page Title: "New Document"
Template ID: "invalid-template"
Parent Page ID: (not provided)
```

**Output** (First Attempt):
```json
{
  "Error": "Template not found with ID: invalid-template",
  "Type": "INPUT_ERROR",
  "Retry": true
}
```

**Action**: User corrects the Template ID and retries.

**Input** (Retry):
```
Space ID: "123456"
Page Title: "New Document"
Template ID: "789012"
```

**Result**: Successfully created page after correction.

## Business Rules

1. **Unique Titles**: Page titles must be unique within the same parent or space root
2. **Space Permissions**: User must have permission to create pages in the space
3. **Template Validation**: Template must exist and be accessible
4. **Parent Validation**: If Parent Page ID is provided, it must exist in the same space
5. **Template Content**: Page inherits structure and content from template
6. **Automatic Status**: New pages are created with "current" status

## Limitations

1. **No Draft Support**: Pages are created as published (current status)
2. **No Bulk Creation**: Creates one page at a time
3. **Template Required**: Must use existing template; cannot create without one
4. **Title Length**: Maximum 255 characters for page title
5. **No Template Modification**: Cannot modify template content during page creation
6. **Same Space Only**: Template must be from the same space or accessible globally

## Best Practices

### 1. Discover Templates First
Use Get Templates By Space ID to find available templates before creating pages.

### 2. Validate Template Before Use
Use Get Template By ID to verify template exists and review its structure.

### 3. Use Descriptive Titles
Choose clear, unique titles that include dates or identifiers for recurring pages.

### 4. Leverage Template Consistency
Use templates to ensure consistent structure across similar pages.

### 5. Handle Duplicate Title Errors
Implement logic to append dates or numbers to titles if duplicates occur.

## Common Use Cases

### 1. Recurring Meeting Notes
```
Scenario: Create weekly meeting notes with consistent structure
Action: Use meeting notes template with date in title
Result: Consistent meeting documentation
```

### 2. Project Documentation
```
Scenario: Create project pages with standard sections
Action: Use project template for each new project
Result: Standardized project documentation
```

### 3. Onboarding Documentation
```
Scenario: Create onboarding pages for new team members
Action: Use onboarding template with employee name in title
Result: Personalized onboarding documentation
```

### 4. Report Generation
```
Scenario: Generate monthly reports with standard format
Action: Use report template with month/year in title
Result: Consistent monthly reporting
```

## Related Catalog Requests

- [Create Page from Content](pages/CreatePageFromContent.md) - Create page with custom content
- [Get Templates By Space ID](pages/GetTemplatesBySpaceId.md) - Discover available templates
- [Get Template By ID](pages/GetTemplateById.md) - Verify template details
- [Update Page from Template](pages/UpdatePageByTemplate.md) - Update existing page with template
- [Get Page By ID](pages/GetPageById.md) - Retrieve page details
- [Get Space By ID](pages/GetSpaceById.md) - Verify space exists

## Technical Implementation

### Helper Class
- **Class**: PageArea
- **Package**: com.kristasoft.extensions.essentials.collaboration.confluence.catalog
- **Method**: createPageFromTemplate(String spaceID, String pageTitle, String templateID, String parentPageID)
- **Service**: ConfluenceService.createPage()

### Validation Flow
1. Validate required parameters (Space ID, Page Title, Template ID)
2. Call ValidationOrchestrator for Space ID, Template ID, and Parent Page ID (if provided)
3. If validation fails, store state and prompt for retry
4. If validation passes, retrieve template content
5. Build page creation request with template body
6. Call Confluence API to create page
7. Parse response and return Page entity

### Telemetry Metrics
- **confluence.createPageFromTemplate**: Incremented on each request
- **confluence.createPageFromTemplate.success**: Recorded with page details and duration
- **confluence.createPageFromTemplate.error**: Recorded on failures
- **confluence.createPageFromTemplate.retryPrompted**: Recorded when validation errors trigger retry

### API Endpoint
```
POST https://{domain}.atlassian.net/wiki/rest/api/content
```

## Troubleshooting

### Issue: "Template not found"
**Cause**: Invalid Template ID

**Solution**:
1. Use Get Templates By Space ID to find valid templates
2. Verify template ID is correct
3. Check template hasn't been deleted
4. Ensure template is accessible from the space

### Issue: "Page with title already exists"
**Cause**: Duplicate page title in same location

**Solution**:
1. Use a different, unique page title
2. Add date or identifier to title
3. Check existing pages in the location
4. Update existing page instead of creating new one

### Issue: "Template not accessible"
**Cause**: Template permissions issue

**Solution**:
1. Verify user has access to the template
2. Check template is in the same space or globally accessible
3. Request access from space administrator
4. Use a different template

### Issue: "Access denied"
**Cause**: Insufficient permissions

**Solution**:
1. Verify user has permission to create pages in space
2. Check space-level permissions
3. Request access from space administrator
4. Ensure account is not restricted

## See Also

- [Extension Configuration](pages/ExtensionConfiguration.md) - Setup and configuration
- [Create Page from Content](pages/CreatePageFromContent.md) - Create with custom content
- [Get Templates By Space ID](pages/GetTemplatesBySpaceId.md) - Find templates
- [Get Template By ID](pages/GetTemplateById.md) - Verify template
- [Entities](pages/Entities.md) - Page entity structure reference

