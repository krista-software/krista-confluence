# Create Page from Content

## Overview

Creates a new Confluence page with custom content in a specified space, allowing full control over page title, body, and parent page hierarchy.

## Request Details

- **Area**: Page
- **Type**: CHANGE_SYSTEM
- **Retry Support**: ✅ Yes (Validation errors prompt for confirmation to retry with corrected input)

## Input Parameters

| Parameter Name | Type | Required | Description | Example |
|----------------|------|----------|-------------|---------|
| Space ID | Text, Identifier | Yes | The unique identifier of the space where the page will be created | `123456` |
| Page Title | Text | Yes | The title of the new page | `"Getting Started Guide"` |
| Page Body | Rich Text | Yes | The content of the page in Confluence storage format | `"<h1>Welcome</h1><p>This is the content...</p>"` |
| Parent Page ID | Text, Identifier | No | The ID of the parent page (if creating a child page) | `789012` |

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

**Page Body**
- **Format**: Rich Text in Confluence storage format
- **Purpose**: The actual content of the page
- **Format**: HTML-like Confluence storage format with macros
- **Can Include**: Text, headings, lists, tables, macros, images
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
| Body | Rich Text | Page content in storage format |
| Parent ID | Text | ID of parent page (if any) |
| Parent Type | Text | Type of parent (typically "page") |
| Has Children | Boolean | Whether the page has child pages (false for new pages) |

## Validation Rules

| Validation | Error Message | Resolution |
|------------|---------------|------------|
| Space ID is empty or null | "Space ID is required" | Provide a valid space identifier |
| Page Title is empty or null | "Page Title is required" | Provide a page title |
| Page Body is empty or null | "Page Body is required" | Provide page content |
| Space does not exist | "Space not found with ID: {id}" | Verify space exists |
| Parent page does not exist | "Parent page not found with ID: {id}" | Verify parent page exists |
| Duplicate page title | "Page with title '{title}' already exists" | Use a different title or update existing page |
| Invalid storage format | "Invalid page body format" | Check Confluence storage format syntax |

## Error Handling

### Input Errors (INPUT_ERROR)

**Cause**: Invalid or missing required parameters

**Common Scenarios**:
- Missing Space ID, Page Title, or Page Body
- Invalid Space ID or Parent Page ID format
- Empty or null required fields

**Error Messages**:
- "Space ID is required"
- "Page Title is required"
- "Page Body is required"

**Resolution**:
1. Verify all required parameters are provided
2. Check parameter formats are correct
3. Use the retry confirmation to correct the input

### Logic Errors (LOGIC_ERROR)

**Cause**: Business logic violations

**Common Scenarios**:
- Space doesn't exist
- Parent page doesn't exist
- Duplicate page title in same location
- Invalid Confluence storage format

**Error Messages**:
- "Space not found with ID: {id}"
- "Parent page not found with ID: {id}"
- "Page with title '{title}' already exists"

**Resolution**:
1. Verify space and parent page exist
2. Use unique page titles
3. Validate Confluence storage format
4. Check permissions to create pages

### System Errors (SYSTEM_ERROR)

**Cause**: Confluence API issues or network problems

**Resolution**:
1. Check Confluence service status
2. Verify network connectivity
3. Wait and retry after a few moments

## Usage Examples

### Example 1: Create Root-Level Page

**Scenario**: Create a new page at the root of a space.

**Input**:
```
Space ID: "123456"
Page Title: "Getting Started Guide"
Page Body: "<h1>Welcome</h1><p>This guide will help you get started with our platform.</p><h2>Prerequisites</h2><ul><li>Account access</li><li>Basic knowledge</li></ul>"
Parent Page ID: (not provided)
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
    "Has Children": false
  },
  "Page ID": "987654",
  "Page URL": "https://mycompany.atlassian.net/wiki/spaces/ENG/pages/987654"
}
```

**Result**: Successfully created root-level page.

### Example 2: Create Child Page

**Scenario**: Create a page as a child of an existing page.

**Input**:
```
Space ID: "123456"
Page Title: "Installation Steps"
Page Body: "<h2>Step 1: Download</h2><p>Download the installer...</p><h2>Step 2: Install</h2><p>Run the installer...</p>"
Parent Page ID: "987654"
```

**Output**:
```json
{
  "Page": {
    "Id": "987655",
    "Space ID": "123456",
    "Title": "Installation Steps",
    "Status": "current",
    "Page URL": "https://mycompany.atlassian.net/wiki/spaces/ENG/pages/987655",
    "Body": "<h2>Step 1: Download</h2><p>Download the installer...</p>",
    "Parent ID": "987654",
    "Parent Type": "page",
    "Has Children": false
  },
  "Page ID": "987655",
  "Page URL": "https://mycompany.atlassian.net/wiki/spaces/ENG/pages/987655"
}
```

**Result**: Successfully created child page under parent page 987654.

### Example 3: Create Page with Macros

**Scenario**: Create a page with Confluence macros.

**Input**:
```
Space ID: "123456"
Page Title: "Important Notice"
Page Body: "<ac:structured-macro ac:name=\"info\"><ac:rich-text-body><p>This is an important notice.</p></ac:rich-text-body></ac:structured-macro>"
Parent Page ID: (not provided)
```

**Result**: Page created with info macro displayed.

## Business Rules

1. **Unique Titles**: Page titles must be unique within the same parent or space root
2. **Space Permissions**: User must have permission to create pages in the space
3. **Parent Validation**: If Parent Page ID is provided, it must exist in the same space
4. **Storage Format**: Page body must be in valid Confluence storage format
5. **Automatic Status**: New pages are created with "current" status
6. **URL Generation**: Confluence automatically generates the page URL

## Limitations

1. **No Draft Support**: Pages are created as published (current status)
2. **No Bulk Creation**: Creates one page at a time
3. **Storage Format Required**: Must use Confluence storage format, not plain text or markdown
4. **Title Length**: Maximum 255 characters for page title
5. **No Automatic Formatting**: Content must be pre-formatted in storage format

## Best Practices

### 1. Validate Space and Parent Before Creation
Use Get Space By ID and Get Page By ID to verify they exist before creating pages.

### 2. Use Unique, Descriptive Titles
Choose clear, unique titles that describe the page content.

### 3. Format Content Properly
Ensure page body is in valid Confluence storage format with proper HTML structure.

### 4. Build Page Hierarchy Thoughtfully
Plan your page structure before creating nested pages.

### 5. Handle Duplicate Title Errors
Implement logic to handle duplicate title errors gracefully.

## Common Use Cases

### 1. Automated Documentation Generation
```
Scenario: Generate documentation pages from external sources
Action: Format content in storage format and create pages
Result: Automated documentation in Confluence
```

### 2. Content Migration
```
Scenario: Migrate content from another system to Confluence
Action: Convert content to storage format and create pages
Result: Content successfully migrated
```

### 3. Template-Based Page Creation
```
Scenario: Create pages with standard structure
Action: Use predefined content template and create pages
Result: Consistent page structure across space
```

### 4. Hierarchical Documentation
```
Scenario: Build nested documentation structure
Action: Create parent pages, then create child pages with Parent Page ID
Result: Organized hierarchical documentation
```

## Related Catalog Requests

- [Create Page from Template](pages/CreatePageFromTemplate.md) - Create page using a template
- [Update Page by Content](pages/UpdatePageByContent.md) - Modify existing page content
- [Get Page By ID](pages/GetPageById.md) - Retrieve page details
- [Get Space By ID](pages/GetSpaceById.md) - Verify space exists
- [Generate Content for Table](pages/GenerateContentForTable.md) - Generate table content
- [Generate Heading Content](pages/GenerateHeadingContent.md) - Generate heading content

## Technical Implementation

### Helper Class
- **Class**: PageArea
- **Package**: com.kristasoft.extensions.essentials.collaboration.confluence.catalog
- **Method**: createPageFromContent(String spaceID, String pageTitle, RichText pageBody, String parentPageID)
- **Service**: ConfluenceService.createPage()

### Validation Flow
1. Validate required parameters (Space ID, Page Title, Page Body)
2. Call ValidationOrchestrator for Space ID and Parent Page ID (if provided)
3. If validation fails, store state and prompt for retry
4. If validation passes, build page creation request
5. Call Confluence API to create page
6. Parse response and return Page entity

### Telemetry Metrics
- **confluence.createPageFromContent**: Incremented on each request
- **confluence.createPageFromContent.success**: Recorded with page details and duration
- **confluence.createPageFromContent.error**: Recorded on failures
- **confluence.createPageFromContent.retryPrompted**: Recorded when validation errors trigger retry

### API Endpoint
```
POST https://{domain}.atlassian.net/wiki/rest/api/content
```

## Troubleshooting

### Issue: "Page with title already exists"
**Cause**: Duplicate page title in same location

**Solution**:
1. Use a different, unique page title
2. Check existing pages in the location
3. Update existing page instead of creating new one
4. Add distinguishing information to title

### Issue: "Invalid page body format"
**Cause**: Malformed Confluence storage format

**Solution**:
1. Validate HTML structure in page body
2. Check macro syntax is correct
3. Use content generation helpers for complex content
4. Test with simple content first

### Issue: "Parent page not found"
**Cause**: Invalid Parent Page ID

**Solution**:
1. Verify parent page exists using Get Page By ID
2. Ensure parent page is in the same space
3. Check Parent Page ID is correct
4. Create parent page first if needed

### Issue: "Access denied"
**Cause**: Insufficient permissions

**Solution**:
1. Verify user has permission to create pages in space
2. Check space-level permissions
3. Request access from space administrator
4. Ensure account is not restricted

## See Also

- [Extension Configuration](pages/ExtensionConfiguration.md) - Setup and configuration
- [Create Page from Template](pages/CreatePageFromTemplate.md) - Use templates
- [Update Page by Content](pages/UpdatePageByContent.md) - Modify pages
- [Get Page By ID](pages/GetPageById.md) - Retrieve pages
- [Entities](pages/Entities.md) - Page entity structure reference

