# Get Spaces

## Overview

Retrieves all spaces available in your Confluence environment, providing a comprehensive list of spaces that the authenticated user has permission to access.

## Request Details

- **Area**: Space
- **Type**: QUERY_SYSTEM
- **Retry Support**: ❌ No (This is a simple query operation that doesn't require retry logic)

## Input Parameters

This catalog request does not require any input parameters.

## Output Parameters

| Parameter Name | Type | Description |
|----------------|------|-------------|
| Spaces | List of Entity(Space) | Complete list of Space entities with full details |
| Space Identifiers | List of Identifier | List of space IDs for quick reference |

### Space Entity Structure

Each Space entity in the output contains:

| Field | Type | Description |
|-------|------|-------------|
| Id | Text | Unique identifier for the space |
| Name | Text | Display name of the space |
| Type | Text | Space type (e.g., "global", "personal") |
| Status | Text | Current status of the space |
| Description | Text | Space description |
| Created At | Date | Timestamp when the space was created |

## Usage Examples

### Example 1: Retrieve All Spaces

**Scenario**: Get a complete list of all accessible spaces in your Confluence environment.

**Input**:
```
No input parameters required
```

**Output**:
```json
{
  "Spaces": [
    {
      "Id": "123456",
      "Name": "Engineering Documentation",
      "Type": "global",
      "Status": "current",
      "Description": "Technical documentation for engineering team",
      "Created At": "2024-01-15T10:30:00Z"
    },
    {
      "Id": "789012",
      "Name": "Product Roadmap",
      "Type": "global",
      "Status": "current",
      "Description": "Product planning and roadmap",
      "Created At": "2024-02-20T14:45:00Z"
    }
  ],
  "Space Identifiers": ["123456", "789012"]
}
```

**Result**: Successfully retrieved 2 spaces with complete details.

### Example 2: Use Space IDs for Subsequent Operations

**Scenario**: Retrieve spaces and use the IDs to get pages from a specific space.

**Workflow**:
```
1. Call Get Spaces
2. Extract Space Identifiers from output
3. Use a specific Space ID in Get Pages in Space request
4. Process pages from the selected space
```

### Example 3: Filter Spaces by Name

**Scenario**: Find a specific space by searching through the returned list.

**Workflow**:
```
1. Call Get Spaces to retrieve all spaces
2. Iterate through Spaces list
3. Filter by Name field matching your criteria
4. Use the matching space's Id for further operations
```

## Business Rules

1. **Permission-Based Access**: Only spaces that the authenticated user has permission to view will be returned
2. **Active Spaces Only**: Archived or deleted spaces are not included in the results
3. **Complete List**: All accessible spaces are returned in a single response (no pagination)
4. **Consistent Ordering**: Spaces are returned in a consistent order based on Confluence's internal sorting

## Limitations

1. **No Filtering**: This request returns all accessible spaces without filtering options
2. **No Pagination**: All spaces are returned at once, which may impact performance for organizations with many spaces
3. **Permission Dependent**: Users with limited permissions will see fewer spaces
4. **No Archived Spaces**: Archived spaces are not included in the results

## Best Practices

### 1. Cache Space Information
Cache the space list if you need to reference it multiple times within a short period to reduce API calls.

### 2. Use Space Identifiers for Efficiency
When you only need space IDs for subsequent operations, use the `Space Identifiers` output field instead of extracting IDs from the full Space entities.

### 3. Handle Empty Results
Always check if the Spaces list is empty before processing, as users with no space access will receive an empty list.

### 4. Filter Client-Side
Since this request doesn't support server-side filtering, implement client-side filtering when searching for specific spaces.

## Common Use Cases

### 1. Space Discovery
```
Scenario: New user wants to see all available spaces
Action: Call Get Spaces
Result: Display list of accessible spaces with names and descriptions
```

### 2. Space Selection Interface
```
Scenario: Build a dropdown menu of available spaces
Action: Call Get Spaces and extract Space Names and IDs
Result: Populate UI element with space options
```

### 3. Audit and Reporting
```
Scenario: Generate report of all spaces in the organization
Action: Call Get Spaces and export to CSV/Excel
Result: Complete space inventory with metadata
```

### 4. Automated Space Monitoring
```
Scenario: Monitor space creation and changes
Action: Periodically call Get Spaces and compare with previous results
Result: Detect new spaces or changes to existing spaces
```

## Related Catalog Requests

- [Get Space By ID](pages/GetSpaceById.md) - Retrieve detailed information about a specific space
- [Get Direct Child Pages of Space](pages/GetDirectChildPagesOfSpace.md) - Get root-level pages in a space
- [Get Pages in Space](pages/GetPagesInSpace.md) - Search for pages within a specific space
- [Get Templates By Space ID](pages/GetTemplatesBySpaceId.md) - Retrieve templates from a space

## Technical Implementation

### Helper Class
- **Class**: SpaceArea
- **Package**: com.kristasoft.extensions.essentials.collaboration.confluence.catalog
- **Method**: getSpaces()
- **Service**: ConfluenceService.getSpaces()

### Telemetry Metrics
- **confluence.getSpaces**: Incremented on each request
- **confluence.getSpaces.success**: Recorded with space count and duration
- **confluence.getSpaces.error**: Recorded on failures with error details

### API Endpoint
```
GET https://{domain}.atlassian.net/wiki/rest/api/space
```

## Error Handling

### System Errors (SYSTEM_ERROR)

**Cause**: Network issues, Confluence API unavailability, or internal errors

**Common Scenarios**:
- Confluence service is temporarily unavailable
- Network connectivity issues
- API rate limiting

**Resolution**:
1. Verify network connectivity to Atlassian services
2. Check Confluence service status at status.atlassian.com
3. Wait a few moments and retry the request
4. Contact support if the issue persists

### Authorization Errors

**Cause**: Invalid or expired API token

**Common Scenarios**:
- API token has been revoked
- Incorrect credentials in configuration
- Account has been deactivated

**Resolution**:
1. Verify API token is still valid in Atlassian account settings
2. Check extension configuration for correct credentials
3. Regenerate API token if necessary
4. Update extension configuration and validate

## Troubleshooting

### Issue: Empty Spaces List
**Cause**: User has no access to any spaces

**Solution**:
1. Verify the account has appropriate Confluence permissions
2. Check that spaces exist in the Confluence instance
3. Contact Confluence administrator to grant space access
4. Ensure the account is not restricted

### Issue: Missing Expected Spaces
**Cause**: User lacks permission to view certain spaces

**Solution**:
1. Verify space permissions in Confluence
2. Request access to missing spaces from space administrators
3. Check if spaces are archived (archived spaces are not returned)
4. Confirm spaces haven't been deleted

### Issue: Slow Response Time
**Cause**: Large number of spaces in the organization

**Solution**:
1. This is expected behavior for organizations with many spaces
2. Consider caching results if calling frequently
3. Implement client-side pagination for display purposes
4. Use more specific requests (Get Space By ID) when possible

## See Also

- [Extension Configuration](pages/ExtensionConfiguration.md) - Setup and configuration
- [Authentication](pages/Authentication.md) - Authentication details
- [Get Space By ID](pages/GetSpaceById.md) - Get specific space details
- [Entities](pages/Entities.md) - Space entity structure reference

