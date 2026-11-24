# Get Pages in Space

## Overview

Searches for and retrieves pages within a specific Confluence space, with optional search query filtering for targeted page discovery.

## Request Details

- **Area**: Page
- **Type**: QUERY_SYSTEM
- **Retry Support**: ✅ Yes (Validation errors prompt for confirmation to retry with corrected input)

## Input Parameters

| Parameter Name | Type | Required | Description | Example |
|----------------|------|----------|-------------|---------|
| Space ID | Text, Identifier | Yes | The unique identifier of the Confluence space | `123456` |
| Search Query | Text | No | Optional search term to filter pages by title or content | `"getting started"` |

### Parameter Details

**Space ID**
- **Format**: Numeric string or space key
- **Purpose**: Identifies the space to search within
- **Validation**: Must be a valid, existing space identifier
- **Required**: Yes

**Search Query**
- **Format**: Text string
- **Purpose**: Filters pages by title or content matching the query
- **Optional**: If not provided, returns all pages in the space
- **Case Sensitivity**: Case-insensitive search
- **Matching**: Partial matches supported

## Output Parameters

| Parameter Name | Type | Description |
|----------------|------|-------------|
| Pages | List of Entity(Page) | List of Page entities matching the search criteria |
| Page Identifiers | List of Identifier | List of page IDs for quick reference |

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
1. Verify the space exists using Get Space By ID
2. Check if space has been archived or deleted
3. Request access from space administrator
4. Use Get Spaces to find valid, accessible space IDs

### System Errors (SYSTEM_ERROR)

**Cause**: Confluence API issues or network problems

**Resolution**:
1. Check Confluence service status
2. Verify network connectivity
3. Wait and retry after a few moments

## Usage Examples

### Example 1: Get All Pages in a Space

**Scenario**: Retrieve all pages from a space without filtering.

**Input**:
```
Space ID: "123456"
Search Query: (not provided)
```

**Output**:
```json
{
  "Pages": [
    {
      "Id": "987654",
      "Space ID": "123456",
      "Title": "Getting Started Guide",
      "Status": "current",
      "Page URL": "https://mycompany.atlassian.net/wiki/spaces/ENG/pages/987654",
      "Has Children": true
    },
    {
      "Id": "987655",
      "Title": "Architecture Overview",
      "Status": "current",
      "Has Children": false
    },
    {
      "Id": "987656",
      "Title": "API Documentation",
      "Status": "current",
      "Has Children": true
    }
  ],
  "Page Identifiers": ["987654", "987655", "987656"]
}
```

**Result**: Successfully retrieved 3 pages from the space.

### Example 2: Search Pages by Query

**Scenario**: Find pages containing specific keywords.

**Input**:
```
Space ID: "123456"
Search Query: "getting started"
```

**Output**:
```json
{
  "Pages": [
    {
      "Id": "987654",
      "Space ID": "123456",
      "Title": "Getting Started Guide",
      "Status": "current",
      "Page URL": "https://mycompany.atlassian.net/wiki/spaces/ENG/pages/987654",
      "Has Children": true
    }
  ],
  "Page Identifiers": ["987654"]
}
```

**Result**: Successfully found 1 page matching the search query.

### Example 3: Validation Error with Retry

**Scenario**: Invalid space ID triggers validation error.

**Input**:
```
Space ID: "invalid-space"
Search Query: "documentation"
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
Search Query: "documentation"
```

**Result**: Successfully retrieved pages after correction.

## Business Rules

1. **Permission-Based Access**: Only pages the user has permission to view are returned
2. **Active Pages Only**: Archived or deleted pages are not included
3. **Case-Insensitive Search**: Search query is case-insensitive
4. **Partial Matching**: Search matches partial words in title and content
5. **All Hierarchy Levels**: Returns pages at all levels (root and nested)
6. **Full Content Included**: Complete page body is returned for each page

## Limitations

1. **No Pagination**: All matching pages are returned at once
2. **Performance Impact**: Large spaces may have slow response times
3. **No Advanced Filtering**: Cannot filter by date, author, labels, or other metadata
4. **No Sorting Options**: Results are returned in Confluence's default order
5. **Permission Dependent**: Users with limited permissions may see fewer pages
6. **Search Limitations**: Search is basic text matching, not full-text search

## Best Practices

### 1. Use Search Query to Narrow Results
When working with large spaces, always provide a search query to reduce the result set.

### 2. Cache Results When Appropriate
If you need to reference the page list multiple times, cache the results to reduce API calls.

### 3. Handle Empty Results Gracefully
Always check if the Pages list is empty before processing, as searches may return no results.

### 4. Validate Space Before Searching
Use Get Space By ID to verify the space exists before searching for pages.

### 5. Use Page Identifiers for Efficiency
When you only need page IDs, use the `Page Identifiers` output field instead of extracting from full entities.

## Common Use Cases

### 1. Page Discovery
```
Scenario: Find all pages related to a specific topic
Action: Search with relevant keywords
Result: List of matching pages for review
```

### 2. Content Inventory
```
Scenario: Generate inventory of all pages in a space
Action: Get all pages without search query
Result: Complete list of space content
```

### 3. Targeted Page Retrieval
```
Scenario: Find specific documentation pages
Action: Search with specific terms like "API" or "guide"
Result: Filtered list of relevant pages
```

### 4. Bulk Operations
```
Scenario: Perform operations on multiple pages
Action: Get pages matching criteria, iterate through results
Result: Batch processing of pages
```

## Related Catalog Requests

- [Get Space By ID](pages/GetSpaceById.md) - Verify space exists
- [Get Page By ID](pages/GetPageById.md) - Retrieve specific page details
- [Get Direct Child Pages of Space](pages/GetDirectChildPagesOfSpace.md) - Get only root-level pages
- [Create Page from Content](pages/CreatePageFromContent.md) - Create new pages
- [Update Page by Content](pages/UpdatePageByContent.md) - Modify pages

## Technical Implementation

### Helper Class
- **Class**: PageArea
- **Package**: com.kristasoft.extensions.essentials.collaboration.confluence.catalog
- **Method**: getPagesInSpace(String spaceID, String searchQuery)
- **Service**: ConfluenceService.getPagesInSpace(spaceID, searchQuery)

### Validation Flow
1. Check if Space ID is provided and not empty
2. Validate Space ID format
3. Call ValidationOrchestrator with SPACE_ID resource
4. If validation fails, store state and prompt for retry
5. If validation passes, call Confluence API with optional search query
6. For each page, check if it has children using hasChildPages()
7. Parse and return Page entities

### Telemetry Metrics
- **confluence.getPagesInSpace**: Incremented on each request
- **confluence.getPagesInSpace.success**: Recorded with space ID, page count, and duration
- **confluence.getPagesInSpace.error**: Recorded on failures
- **confluence.getPagesInSpace.retryPrompted**: Recorded when validation errors trigger retry

### API Endpoint
```
GET https://{domain}.atlassian.net/wiki/rest/api/content
?spaceKey={spaceId}&type=page&title={searchQuery}
```

## Troubleshooting

### Issue: Empty Pages List
**Cause**: No pages match the search criteria or user lacks permissions

**Solution**:
1. Verify space exists using Get Space By ID
2. Check if space has pages in Confluence UI
3. Try without search query to see all pages
4. Verify user has permission to view space content
5. Request access from space administrator

### Issue: "Space not found"
**Cause**: Invalid space ID or space doesn't exist

**Solution**:
1. Verify the space ID is correct
2. Use Get Spaces to find valid space IDs
3. Check if space has been deleted or archived
4. Ensure you're using the correct Confluence instance

### Issue: Missing Expected Pages
**Cause**: User lacks permission to view certain pages

**Solution**:
1. Verify page permissions in Confluence
2. Request access to pages from space administrator
3. Check if pages are archived
4. Confirm pages haven't been deleted

### Issue: Slow Response Time
**Cause**: Large number of pages in the space

**Solution**:
1. Use search query to narrow results
2. Consider caching results if calling frequently
3. Use more specific requests (Get Page By ID) when possible
4. This is expected behavior for large spaces

## See Also

- [Extension Configuration](pages/ExtensionConfiguration.md) - Setup and configuration
- [Authentication](pages/Authentication.md) - Authentication details
- [Get Space By ID](pages/GetSpaceById.md) - Verify space exists
- [Get Page By ID](pages/GetPageById.md) - Get specific page
- [Entities](pages/Entities.md) - Page entity structure reference

