# Get Space By ID

## Overview

Retrieves detailed information about a specific Confluence space using its unique identifier.

## Request Details

- **Area**: Space
- **Type**: QUERY_SYSTEM
- **Retry Support**: ✅ Yes (Validation errors prompt for confirmation to retry with corrected input)

## Input Parameters

| Parameter Name | Type | Required | Description | Example |
|----------------|------|----------|-------------|---------|
| Space Id | Text, Identifier | Yes | The unique identifier of the Confluence space to retrieve | `123456` |

### Parameter Details

**Space Id**
- **Format**: Numeric string or space key
- **Validation**: Must be a valid, existing space identifier
- **Case Sensitivity**: Space keys are case-sensitive
- **Where to Find**: Available from Get Spaces request or Confluence space settings

## Output Parameters

| Parameter Name | Type | Description |
|----------------|------|-------------|
| Space | Entity(Space) | Complete Space entity with all details |

### Space Entity Structure

| Field | Type | Description |
|-------|------|-------------|
| Id | Text | Unique identifier for the space |
| Name | Text | Display name of the space |
| Type | Text | Space type (e.g., "global", "personal") |
| Status | Text | Current status of the space |
| Description | Text | Space description |
| Created At | Date | Timestamp when the space was created |

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

**Cause**: Space not found or access denied

**Common Scenarios**:
- Space ID doesn't exist in Confluence
- Space has been deleted or archived
- User lacks permission to view the space

**Error Messages**:
- "Space not found with ID: {id}"
- "Access denied to space: {id}"

**Resolution**:
1. Verify the space exists in Confluence
2. Check if space has been archived or deleted
3. Request access from space administrator
4. Use Get Spaces to find valid, accessible space IDs

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

### Example 1: Retrieve Specific Space Details

**Scenario**: Get complete information about a known space.

**Input**:
```
Space Id: "123456"
```

**Output**:
```json
{
  "Space": {
    "Id": "123456",
    "Name": "Engineering Documentation",
    "Type": "global",
    "Status": "current",
    "Description": "Technical documentation for the engineering team",
    "Created At": "2024-01-15T10:30:00Z"
  }
}
```

**Result**: Successfully retrieved space details for Engineering Documentation space.

### Example 2: Validation Error with Retry

**Scenario**: Invalid space ID triggers validation error with retry option.

**Input**:
```
Space Id: "invalid-id-999"
```

**Output** (First Attempt):
```json
{
  "Error": "Space not found with ID: invalid-id-999",
  "Type": "INPUT_ERROR",
  "Retry": true
}
```

**Action**: User corrects the Space ID and retries.

**Input** (Retry):
```
Space Id: "123456"
```

**Result**: Successfully retrieved space after correction.

### Example 3: Use Space Details for Page Creation

**Scenario**: Retrieve space details before creating a page in that space.

**Workflow**:
```
1. Call Get Space By ID with Space Id: "123456"
2. Verify space exists and user has access
3. Extract space information for confirmation
4. Use Space Id in Create Page from Content request
5. Create page in the verified space
```

## Business Rules

1. **Permission-Based Access**: Users can only retrieve spaces they have permission to view
2. **Active Spaces Only**: Archived spaces may not be retrievable depending on permissions
3. **Space ID Uniqueness**: Each space has a unique identifier within the Confluence instance
4. **Validation Before Retry**: Invalid space IDs trigger validation errors with retry options

## Limitations

1. **Single Space Only**: Retrieves one space at a time; use Get Spaces for multiple spaces
2. **No Nested Data**: Does not include pages, templates, or other nested space content
3. **Permission Dependent**: Users with limited permissions may not access all space details
4. **Archived Spaces**: May not be accessible even with valid space ID

## Best Practices

### 1. Validate Space ID Before Use
Always verify the space ID exists before using it in other operations to avoid errors in downstream processes.

### 2. Cache Space Information
If you need to reference space details multiple times, cache the result to reduce API calls.

### 3. Handle Validation Errors Gracefully
Implement proper error handling for validation failures and provide clear feedback to users.

### 4. Use Get Spaces for Discovery
When you don't know the space ID, use Get Spaces first to discover available spaces and their IDs.

### 5. Check Permissions Proactively
Before attempting operations on a space, verify the user has appropriate access using this request.

## Common Use Cases

### 1. Space Verification
```
Scenario: Verify a space exists before creating content
Action: Call Get Space By ID with the target space ID
Result: Confirm space exists and is accessible
```

### 2. Space Information Display
```
Scenario: Display space details in a user interface
Action: Retrieve space by ID and show name, description, and metadata
Result: User sees complete space information
```

### 3. Pre-Operation Validation
```
Scenario: Validate space access before bulk operations
Action: Check space exists and user has permissions
Result: Prevent errors in subsequent operations
```

### 4. Space Metadata Extraction
```
Scenario: Extract space creation date and type for reporting
Action: Retrieve space details and extract specific fields
Result: Metadata available for analytics and reporting
```

## Related Catalog Requests

- [Get Spaces](pages/GetSpaces.md) - Retrieve all accessible spaces
- [Get Direct Child Pages of Space](pages/GetDirectChildPagesOfSpace.md) - Get root-level pages in a space
- [Get Pages in Space](pages/GetPagesInSpace.md) - Search for pages within a space
- [Get Templates By Space ID](pages/GetTemplatesBySpaceId.md) - Retrieve templates from a space
- [Create Page from Content](pages/CreatePageFromContent.md) - Create a page in a space

## Technical Implementation

### Helper Class
- **Class**: SpaceArea
- **Package**: com.kristasoft.extensions.essentials.collaboration.confluence.catalog
- **Method**: getSpaceById(String spaceId)
- **Service**: ConfluenceService.getSpaceById(spaceId)

### Validation Flow
1. Check if Space ID is provided and not empty
2. Validate Space ID format
3. Call ValidationOrchestrator with SPACE_ID resource
4. If validation fails, store state and prompt for retry
5. If validation passes, call Confluence API
6. Parse and return Space entity

### Telemetry Metrics
- **confluence.getSpaceById**: Incremented on each request
- **confluence.getSpaceById.success**: Recorded with space details and duration
- **confluence.getSpaceById.error**: Recorded on failures with error details
- **confluence.getSpaceById.retryPrompted**: Recorded when validation errors trigger retry

### API Endpoint
```
GET https://{domain}.atlassian.net/wiki/rest/api/space/{spaceId}
```

## Troubleshooting

### Issue: "Space not found"
**Cause**: Invalid space ID or space doesn't exist

**Solution**:
1. Verify the space ID is correct
2. Use Get Spaces to find valid space IDs
3. Check if space has been deleted or archived
4. Ensure you're using the correct Confluence instance

### Issue: "Access denied"
**Cause**: User lacks permission to view the space

**Solution**:
1. Request access from space administrator
2. Verify account has appropriate Confluence permissions
3. Check space-specific permission settings
4. Ensure account is not restricted

### Issue: Validation error loop
**Cause**: Repeatedly entering invalid space IDs

**Solution**:
1. Use Get Spaces to obtain valid space IDs
2. Copy space ID exactly as shown (case-sensitive)
3. Verify space exists in your Confluence instance
4. Check for typos or extra characters

### Issue: Empty space details
**Cause**: Space exists but has minimal information

**Solution**:
1. This is normal for newly created spaces
2. Space description may be empty
3. Check Confluence directly to verify space content
4. Update space details in Confluence if needed

## See Also

- [Extension Configuration](pages/ExtensionConfiguration.md) - Setup and configuration
- [Authentication](pages/Authentication.md) - Authentication details
- [Get Spaces](pages/GetSpaces.md) - Retrieve all spaces
- [Entities](pages/Entities.md) - Space entity structure reference

