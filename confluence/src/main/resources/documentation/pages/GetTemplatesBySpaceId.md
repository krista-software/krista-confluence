# Get Templates By Space ID

## Overview

Retrieves all page templates available in a specific Confluence space, enabling users to discover and use templates for consistent page creation.

## Request Details

- **Area**: Template
- **Type**: QUERY_SYSTEM
- **Retry Support**: ✅ Yes (Validation errors prompt for confirmation to retry with corrected input)

## Input Parameters

| Parameter Name | Type | Required | Description | Example |
|----------------|------|----------|-------------|---------|
| Space ID | Text, Identifier | Yes | The unique identifier of the Confluence space | `123456` |

### Parameter Details

**Space ID**
- **Format**: Numeric string or space key
- **Purpose**: Identifies the space to retrieve templates from
- **Validation**: Must be a valid, existing space identifier
- **Where to Find**: Available from Get Spaces or Get Space By ID requests

## Output Parameters

| Parameter Name | Type | Description |
|----------------|------|-------------|
| Templates | List of Entity(Template) | Complete list of Template entities with full details |
| Template Identifiers | List of Identifier | List of template IDs for quick reference |

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
| Space ID is empty or null | "Space ID is required" | Provide a valid space identifier |
| Space ID format is invalid | "Invalid Space ID format" | Verify the space ID is correct |
| Space does not exist | "Space not found with ID: {id}" | Check the space ID and ensure the space exists |
| No permission to access space | "Access denied to space: {id}" | Request access from space administrator |

## Error Handling

### Input Errors (INPUT_ERROR)

**Cause**: Invalid or missing Space ID parameter

**Common Scenarios**:
- Space ID parameter is empty or null
- Space ID contains invalid characters
- Space ID format doesn't match expected pattern

**Error Messages**:
- "Space ID is required"
- "Invalid Space ID format"

**Resolution**:
1. Verify the Space ID is provided
2. Check the Space ID format (numeric or valid space key)
3. Use the retry confirmation to correct the input
4. Obtain valid Space ID from Get Spaces request

### Logic Errors (LOGIC_ERROR)

**Cause**: Space not found or no templates available

**Common Scenarios**:
- Space ID doesn't exist in Confluence
- Space has no templates defined
- User lacks permission to view templates

**Error Messages**:
- "Space not found with ID: {id}"
- "No templates found in space: {id}"
- "Access denied to space: {id}"

**Resolution**:
1. Verify the space exists using Get Space By ID
2. Check if space has templates in Confluence UI
3. Request access from space administrator
4. Create templates in the space if none exist

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

### Example 1: Retrieve All Templates from a Space

**Scenario**: Get all available templates from the Engineering space.

**Input**:
```
Space ID: "123456"
```

**Output**:
```json
{
  "Templates": [
    {
      "Id": "789012",
      "Name": "Meeting Notes Template",
      "Description": "Standard template for meeting notes",
      "Space ID": "123456",
      "Body": "<ac:structured-macro>...</ac:structured-macro>",
      "Labels": ["meetings", "notes"]
    },
    {
      "Id": "789013",
      "Name": "Technical Design Document",
      "Description": "Template for technical design documents",
      "Space ID": "123456",
      "Body": "<h1>Overview</h1><p>...</p>",
      "Labels": ["design", "technical"]
    }
  ],
  "Template Identifiers": ["789012", "789013"]
}
```

**Result**: Successfully retrieved 2 templates from the space.

### Example 2: Use Template for Page Creation

**Scenario**: Retrieve templates and use one to create a new page.

**Workflow**:
```
1. Call Get Templates By Space ID with Space ID: "123456"
2. Display template list to user
3. User selects "Meeting Notes Template" (ID: 789012)
4. Call Create Page from Template with Template ID: "789012"
5. New page created using the selected template
```

### Example 3: Validation Error with Retry

**Scenario**: Invalid space ID triggers validation error with retry option.

**Input**:
```
Space ID: "invalid-space"
```

**Output** (First Attempt):
```json
{
  "Error": "Space not found with ID: invalid-space",
  "Type": "INPUT_ERROR",
  "Retry": true
}
```

**Action**: User corrects the Space ID and retries.

**Input** (Retry):
```
Space ID: "123456"
```

**Result**: Successfully retrieved templates after correction.

## Business Rules

1. **Space-Specific Templates**: Only templates from the specified space are returned
2. **Permission-Based Access**: Users can only see templates they have permission to view
3. **Active Templates Only**: Archived or deleted templates are not included
4. **Complete Template Content**: Full template body is included in the response
5. **Label Preservation**: Template labels are included for categorization

## Limitations

1. **Single Space Only**: Retrieves templates from one space at a time
2. **No Pagination**: All templates are returned at once
3. **No Filtering**: Cannot filter by template name, labels, or other criteria
4. **No Global Templates**: Only space-specific templates are returned
5. **Permission Dependent**: Users with limited permissions may see fewer templates

## Best Practices

### 1. Cache Template List
Cache the template list if you need to reference it multiple times to reduce API calls.

### 2. Use Template Identifiers for Efficiency
When you only need template IDs, use the `Template Identifiers` output field instead of extracting from full entities.

### 3. Validate Space Before Retrieval
Use Get Space By ID to verify the space exists before retrieving templates.

### 4. Handle Empty Results
Always check if the Templates list is empty, as spaces may have no templates defined.

### 5. Display Template Descriptions
Show template descriptions to users to help them choose the right template.

## Common Use Cases

### 1. Template Selection Interface
```
Scenario: Build a dropdown menu of available templates
Action: Call Get Templates By Space ID and display template names
Result: User can select appropriate template for page creation
```

### 2. Template Discovery
```
Scenario: User wants to see what templates are available
Action: Retrieve and display all templates with descriptions
Result: User discovers available templates for their needs
```

### 3. Automated Page Creation
```
Scenario: Automatically create pages using specific templates
Action: Retrieve templates, find matching template by name, use for page creation
Result: Consistent page creation with predefined structure
```

### 4. Template Audit
```
Scenario: Generate report of all templates in a space
Action: Retrieve templates and export metadata
Result: Complete inventory of space templates
```

## Related Catalog Requests

- [Get Template By ID](pages/GetTemplateById.md) - Retrieve a specific template
- [Create Page from Template](pages/CreatePageFromTemplate.md) - Create a page using a template
- [Get Space By ID](pages/GetSpaceById.md) - Verify space exists
- [Get Spaces](pages/GetSpaces.md) - Discover available spaces

## Technical Implementation

### Helper Class
- **Class**: TemplateArea
- **Package**: com.kristasoft.extensions.essentials.collaboration.confluence.catalog
- **Method**: getTemplatesBySpaceId(String spaceID)
- **Service**: ConfluenceService.getTemplatesBySpaceId(spaceID)

### Validation Flow
1. Check if Space ID is provided and not empty
2. Validate Space ID format
3. Call ValidationOrchestrator with SPACE_ID resource
4. If validation fails, store state and prompt for retry
5. If validation passes, call Confluence API
6. Parse and return Template entities

### Telemetry Metrics
- **confluence.getTemplatesBySpaceId**: Incremented on each request
- **confluence.getTemplatesBySpaceId.success**: Recorded with space ID, template count, and duration
- **confluence.getTemplatesBySpaceId.error**: Recorded on failures with error details
- **confluence.getTemplatesBySpaceId.retryPrompted**: Recorded when validation errors trigger retry

### API Endpoint
```
GET https://{domain}.atlassian.net/wiki/rest/api/template/page
?spaceKey={spaceId}
```

## Troubleshooting

### Issue: Empty Templates List
**Cause**: Space has no templates or user lacks permissions

**Solution**:
1. Verify space exists using Get Space By ID
2. Check if space has templates in Confluence UI
3. Verify user has permission to view templates
4. Create templates in the space if none exist
5. Request access from space administrator

### Issue: "Space not found"
**Cause**: Invalid space ID or space doesn't exist

**Solution**:
1. Verify the space ID is correct
2. Use Get Spaces to find valid space IDs
3. Check if space has been deleted or archived
4. Ensure you're using the correct Confluence instance

### Issue: Missing Expected Templates
**Cause**: User lacks permission to view certain templates

**Solution**:
1. Verify template permissions in Confluence
2. Request access to templates from space administrator
3. Check if templates are archived
4. Confirm templates haven't been deleted

### Issue: Validation error loop
**Cause**: Repeatedly entering invalid space IDs

**Solution**:
1. Use Get Spaces to obtain valid space IDs
2. Copy space ID exactly as shown (case-sensitive)
3. Verify space exists in your Confluence instance
4. Check for typos or extra characters

## See Also

- [Extension Configuration](pages/ExtensionConfiguration.md) - Setup and configuration
- [Authentication](pages/Authentication.md) - Authentication details
- [Get Template By ID](pages/GetTemplateById.md) - Get specific template
- [Create Page from Template](pages/CreatePageFromTemplate.md) - Use template to create page
- [Entities](pages/Entities.md) - Template entity structure reference

