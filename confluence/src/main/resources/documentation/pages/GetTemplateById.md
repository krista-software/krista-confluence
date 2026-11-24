# Get Template By ID

## Overview

Retrieves detailed information about a specific Confluence template using its unique identifier, providing access to template content and metadata.

## Request Details

- **Area**: Template
- **Type**: QUERY_SYSTEM
- **Retry Support**: ✅ Yes (Validation errors prompt for confirmation to retry with corrected input)

## Input Parameters

| Parameter Name | Type | Required | Description | Example |
|----------------|------|----------|-------------|---------|
| Template ID | Text, Identifier | Yes | The unique identifier of the Confluence template | `789012` |

### Parameter Details

**Template ID**
- **Format**: Numeric string
- **Purpose**: Identifies the specific template to retrieve
- **Validation**: Must be a valid, existing template identifier
- **Where to Find**: Available from Get Templates By Space ID request

## Output Parameters

| Parameter Name | Type | Description |
|----------------|------|-------------|
| Template | Entity(Template) | Complete Template entity with all details |

### Template Entity Structure

| Field | Type | Description |
|-------|------|-------------|
| Id | Text | Unique identifier for the template |
| Name | Text | Display name of the template |
| Description | Text | Template description |
| Space ID | Identifier | ID of the space containing the template |
| Body | Rich Text | Template content in storage format |
| Labels | List of Text | Labels/tags associated with the template |

## Validation Rules

| Validation | Error Message | Resolution |
|------------|---------------|------------|
| Template ID is empty or null | "Template ID is required" | Provide a valid template identifier |
| Template ID format is invalid | "Invalid Template ID format" | Verify the template ID is correct |
| Template does not exist | "Template not found with ID: {id}" | Check the template ID and ensure the template exists |
| No permission to access template | "Access denied to template: {id}" | Request access from space administrator |

## Error Handling

### Input Errors (INPUT_ERROR)

**Cause**: Invalid or missing Template ID parameter

**Common Scenarios**:
- Template ID parameter is empty or null
- Template ID contains invalid characters
- Template ID format doesn't match expected pattern

**Error Messages**:
- "Template ID is required"
- "Invalid Template ID format"

**Resolution**:
1. Verify the Template ID is provided
2. Check the Template ID format (numeric string)
3. Use the retry confirmation to correct the input
4. Obtain valid Template ID from Get Templates By Space ID request

### Logic Errors (LOGIC_ERROR)

**Cause**: Template not found or access denied

**Common Scenarios**:
- Template ID doesn't exist in Confluence
- Template has been deleted
- User lacks permission to view the template

**Error Messages**:
- "Template not found with ID: {id}"
- "Access denied to template: {id}"

**Resolution**:
1. Verify the template exists using Get Templates By Space ID
2. Check if template has been deleted
3. Request access from space administrator
4. Use Get Templates By Space ID to find valid template IDs

### System Errors (SYSTEM_ERROR)

**Cause**: Confluence API issues or network problems

**Common Scenarios**:
- Confluence service temporarily unavailable
- Network connectivity issues
- API rate limiting

**Resolution**:
1. Check Confluence service status
2. Verify network connectivity
3. Wait and retry after a few moments
4. Contact support if issue persists

## Usage Examples

### Example 1: Retrieve Specific Template

**Scenario**: Get complete details of a known template.

**Input**:
```
Template ID: "789012"
```

**Output**:
```json
{
  "Template": {
    "Id": "789012",
    "Name": "Meeting Notes Template",
    "Description": "Standard template for meeting notes with agenda, attendees, and action items",
    "Space ID": "123456",
    "Body": "<ac:structured-macro ac:name=\"info\"><ac:rich-text-body><p>Meeting Date: </p></ac:rich-text-body></ac:structured-macro><h2>Attendees</h2><ul><li></li></ul><h2>Agenda</h2><ol><li></li></ol><h2>Action Items</h2><table>...</table>",
    "Labels": ["meetings", "notes", "agenda"]
  }
}
```

**Result**: Successfully retrieved template with full content and metadata.

### Example 2: Validation Error with Retry

**Scenario**: Invalid template ID triggers validation error with retry option.

**Input**:
```
Template ID: "invalid-template-999"
```

**Output** (First Attempt):
```json
{
  "Error": "Template not found with ID: invalid-template-999",
  "Type": "INPUT_ERROR",
  "Retry": true
}
```

**Action**: User corrects the Template ID and retries.

**Input** (Retry):
```
Template ID: "789012"
```

**Result**: Successfully retrieved template after correction.

### Example 3: Use Template for Page Creation

**Scenario**: Retrieve template details before creating a page.

**Workflow**:
```
1. Call Get Template By ID with Template ID: "789012"
2. Verify template exists and review content
3. Extract template information for confirmation
4. Use Template ID in Create Page from Template request
5. Create page using the verified template
```

## Business Rules

1. **Permission-Based Access**: Users can only retrieve templates they have permission to view
2. **Template ID Uniqueness**: Each template has a unique identifier within Confluence
3. **Full Content Included**: Complete template body is returned in storage format
4. **Space Association**: Template is associated with a specific space
5. **Validation Before Retry**: Invalid template IDs trigger validation errors with retry options

## Limitations

1. **Single Template Only**: Retrieves one template at a time
2. **No Template Listing**: Use Get Templates By Space ID to discover templates
3. **Permission Dependent**: Users with limited permissions may not access all templates
4. **No Version History**: Does not include template version history
5. **Storage Format Only**: Template body is in Confluence storage format, not rendered HTML

## Best Practices

### 1. Validate Template ID Before Use
Always verify the template ID exists before using it in page creation operations.

### 2. Cache Template Content
If you need to reference template content multiple times, cache the result to reduce API calls.

### 3. Handle Validation Errors Gracefully
Implement proper error handling for validation failures and provide clear feedback to users.

### 4. Use Get Templates By Space ID for Discovery
When you don't know the template ID, use Get Templates By Space ID first to discover available templates.

### 5. Check Permissions Proactively
Before attempting to use a template, verify the user has appropriate access using this request.

## Common Use Cases

### 1. Template Verification
```
Scenario: Verify a template exists before creating pages
Action: Call Get Template By ID with the target template ID
Result: Confirm template exists and is accessible
```

### 2. Template Content Preview
```
Scenario: Display template content to user before page creation
Action: Retrieve template and show name, description, and preview
Result: User sees what the template contains
```

### 3. Pre-Operation Validation
```
Scenario: Validate template access before bulk page creation
Action: Check template exists and user has permissions
Result: Prevent errors in subsequent operations
```

### 4. Template Metadata Extraction
```
Scenario: Extract template labels and description for categorization
Action: Retrieve template details and extract specific fields
Result: Metadata available for organization and search
```

## Related Catalog Requests

- [Get Templates By Space ID](pages/GetTemplatesBySpaceId.md) - Retrieve all templates in a space
- [Create Page from Template](pages/CreatePageFromTemplate.md) - Create a page using a template
- [Get Space By ID](pages/GetSpaceById.md) - Verify space exists
- [Get Spaces](pages/GetSpaces.md) - Discover available spaces

## Technical Implementation

### Helper Class
- **Class**: TemplateArea
- **Package**: com.kristasoft.extensions.essentials.collaboration.confluence.catalog
- **Method**: getTemplateById(String templateID)
- **Service**: ConfluenceService.getTemplateById(templateID)

### Validation Flow
1. Check if Template ID is provided and not empty
2. Validate Template ID format
3. Call ValidationOrchestrator with TEMPLATE_ID resource
4. If validation fails, store state and prompt for retry
5. If validation passes, call Confluence API
6. Parse and return Template entity

### Telemetry Metrics
- **confluence.getTemplateById**: Incremented on each request
- **confluence.getTemplateById.success**: Recorded with template details and duration
- **confluence.getTemplateById.error**: Recorded on failures with error details
- **confluence.getTemplateById.retryPrompted**: Recorded when validation errors trigger retry

### API Endpoint
```
GET https://{domain}.atlassian.net/wiki/rest/api/template/{templateId}
```

## Troubleshooting

### Issue: "Template not found"
**Cause**: Invalid template ID or template doesn't exist

**Solution**:
1. Verify the template ID is correct
2. Use Get Templates By Space ID to find valid template IDs
3. Check if template has been deleted
4. Ensure you're using the correct Confluence instance

### Issue: "Access denied"
**Cause**: User lacks permission to view the template

**Solution**:
1. Request access from space administrator
2. Verify account has appropriate Confluence permissions
3. Check space-specific permission settings
4. Ensure account is not restricted

### Issue: Validation error loop
**Cause**: Repeatedly entering invalid template IDs

**Solution**:
1. Use Get Templates By Space ID to obtain valid template IDs
2. Copy template ID exactly as shown
3. Verify template exists in your Confluence instance
4. Check for typos or extra characters

### Issue: Empty template body
**Cause**: Template exists but has no content

**Solution**:
1. This is normal for newly created templates
2. Template may be a placeholder
3. Check Confluence directly to verify template content
4. Update template content in Confluence if needed

## See Also

- [Extension Configuration](pages/ExtensionConfiguration.md) - Setup and configuration
- [Authentication](pages/Authentication.md) - Authentication details
- [Get Templates By Space ID](pages/GetTemplatesBySpaceId.md) - Retrieve all templates
- [Create Page from Template](pages/CreatePageFromTemplate.md) - Use template to create page
- [Entities](pages/Entities.md) - Template entity structure reference

