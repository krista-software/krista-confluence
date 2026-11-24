# Get Attachment By ID

## Overview

Retrieves detailed metadata for a specific attachment including version history for staleness detection. Use this to get complete information about a single attachment before downloading.

## Request Details

- **Area**: Page
- **Type**: QUERY_SYSTEM
- **Retry Support**: ✅ Yes (Validation errors prompt for confirmation to retry with corrected input)

## Input Parameters

| Parameter Name | Type | Required | Description | Example |
|----------------|------|----------|-------------|---------|
| Attachment ID | Text | Yes | The unique identifier of the attachment | `"att-123456"` |
| Allow Retry | Boolean | No | Enable retry on validation errors | `true` |

### Parameter Details

**Attachment ID**
- **Format**: Text string (typically prefixed with "att-")
- **Purpose**: Identifies the specific attachment to retrieve
- **Validation**: Must be a valid, existing attachment identifier
- **Required**: Yes

**Allow Retry**
- **Format**: Boolean
- **Purpose**: When true, validation errors will prompt for confirmation to retry
- **Default**: false
- **Required**: No

## Output Parameters

| Parameter Name | Type | Description |
|----------------|------|-------------|
| Attachment | Entity(Attachment) | Complete attachment entity with all metadata |
| Success | Boolean | Whether the operation succeeded |

### Attachment Entity Structure

| Field | Type | Description |
|-------|------|-------------|
| Id | Text | Unique identifier for the attachment |
| Title | Text | Filename of the attachment |
| Page ID | Identifier | ID of the page containing the attachment |
| Media Type | Text | MIME type of the file (e.g., "application/pdf") |
| File Size | Number | Size of the file in bytes |
| Download Link | Text | URL path to download the attachment |
| Web UI Link | Text | URL to view the attachment in Confluence UI |
| Created At | Number | Timestamp when attachment was first created |
| Status | Text | Attachment status ("current" or "archived") |
| Version Number | Number | Current version number of the attachment |
| Version Created At | Number | Timestamp when current version was created |

## Validation Rules

| Validation | Error Message | Resolution |
|------------|---------------|------------|
| Attachment ID is empty or null | "Attachment ID is required" | Provide a valid attachment identifier |
| Attachment does not exist | "Attachment not found with ID: {id}" | Verify attachment exists |
| No permission to access | "Access denied to attachment" | Request access from administrator |

## Error Handling

### Input Errors (INPUT_ERROR)

**Cause**: Invalid or missing required parameters

**Common Scenarios**:
- Missing Attachment ID
- Invalid Attachment ID format

**Resolution**:
1. Verify Attachment ID is provided
2. Check Attachment ID format is correct
3. Use the retry confirmation to correct the input

### Logic Errors (LOGIC_ERROR)

**Cause**: Business logic violations

**Common Scenarios**:
- Attachment doesn't exist
- Attachment was deleted
- User lacks permission to view attachment

**Resolution**:
1. Verify attachment exists using Get Attachments for Page
2. Check if attachment was recently deleted
3. Request access from page/space administrator

### System Errors (SYSTEM_ERROR)

**Cause**: Confluence API issues or network problems

**Resolution**:
1. Check Confluence service status
2. Verify network connectivity
3. Wait and retry after a few moments

## Usage Examples

### Example 1: Get Attachment Details

**Input**:
```
Attachment ID: "att-123456"
Allow Retry: false
```

**Output**:
```json
{
  "Attachment": {
    "Id": "att-123456",
    "Title": "quarterly-report.pdf",
    "Page ID": "789012",
    "Media Type": "application/pdf",
    "File Size": 5242880,
    "Download Link": "/download/attachments/789012/quarterly-report.pdf",
    "Web UI Link": "/pages/789012/attachments/att-123456",
    "Created At": 1704067200000,
    "Status": "current",
    "Version Number": 5,
    "Version Created At": 1735689600000
  },
  "Success": true
}
```

### Example 2: Check for Staleness

**Scenario**: Determine if a cached attachment needs to be re-downloaded.

**Input**:
```
Attachment ID: "att-123456"
```

**Action**: Compare `Version Created At` with your cached version timestamp.

**Result**: If `Version Created At` is newer than your cache, re-download the attachment.

## Business Rules

1. **Version Tracking**: Includes complete version information for staleness detection
2. **Permission-Based**: User must have permission to view the parent page
3. **Single Attachment**: Returns metadata for one attachment at a time
4. **Real-Time Data**: Always returns current metadata from Confluence

## Limitations

1. **Metadata Only**: Returns metadata, not file content (use Download Attachment)
2. **Single Attachment**: Cannot retrieve multiple attachments in one request
3. **No Version History List**: Returns current version info only, not full history

## Related Catalog Requests

- [Get Attachments for Page](pages/GetAttachmentsForPage.md) - Discover all attachments on a page
- [Download Attachment](pages/DownloadAttachment.md) - Download attachment file content
- [Get Page By ID](pages/GetPageById.md) - Get parent page details

## See Also

- [Entities](pages/Entities.md) - Attachment entity structure reference
- [Extension Configuration](pages/ExtensionConfiguration.md) - Setup and configuration

