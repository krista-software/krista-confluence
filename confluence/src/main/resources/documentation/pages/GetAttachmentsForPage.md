# Get Attachments for Page

## Overview

Retrieves all file attachments on a Confluence page for document discovery and ingestion. Supports pagination to handle pages with many attachments and filtering by status and media type.

## Request Details

- **Area**: Page
- **Type**: CHANGE_SYSTEM
- **Retry Support**: ✅ Yes (Validation errors prompt for confirmation to retry with corrected input)

## Input Parameters

| Parameter Name | Type | Required | Description | Example |
|----------------|------|----------|-------------|---------|
| Page ID | Text | Yes | The unique identifier of the page | `"123456"` |
| Status | PickOne | No | Filter by attachment status | `"current"`, `"archived"`, `"any"`, `"trashed"` |
| Media Type | Text | No | Filter by MIME type | `"application/pdf"` |
| Allow Retry | Boolean | No | Enable retry on validation errors | `true` |

### Parameter Details

**Page ID**
- **Format**: Numeric string
- **Purpose**: Identifies the page to retrieve attachments from
- **Validation**: Must be a valid, existing page identifier
- **Required**: Yes

**Status**
- **Format**: One of: `current`, `archived`, `any`, `trashed`
- **Purpose**: Filter attachments by their status
- **Default**: `current` (if not specified)
- **Required**: No

**Media Type**
- **Format**: MIME type string (e.g., `application/pdf`, `image/png`)
- **Purpose**: Filter attachments by file type
- **Required**: No

## Output Parameters

| Parameter Name | Type | Description |
|----------------|------|-------------|
| Attachments | [ Entity(Attachment) ] | List of attachment entities |
| Total Count | Number | Total number of attachments returned |
| Success | Boolean | Whether the operation succeeded |

### Attachment Entity Structure

| Field | Type | Description |
|-------|------|-------------|
| Id | Text | Unique identifier for the attachment |
| Title | Text | Filename of the attachment |
| Page ID | Identifier | ID of the page containing the attachment |
| Media Type | Text | MIME type of the file |
| File Size | Number | Size of the file in bytes |
| Download Link | Text | URL path to download the attachment |
| Web UI Link | Text | URL to view the attachment in Confluence |
| Created At | Number | Timestamp when attachment was created |
| Status | Text | Attachment status (current/archived) |
| Version Number | Number | Current version number |
| Version Created At | Number | Timestamp of current version |

## Validation Rules

| Validation | Error Message | Resolution |
|------------|---------------|------------|
| Page ID is empty or null | "Page ID is required" | Provide a valid page identifier |
| Page does not exist | "Page not found with ID: {id}" | Verify page exists |
| No permission to access page | "Access denied to page" | Request access from administrator |

## Error Handling

### Input Errors (INPUT_ERROR)

**Cause**: Invalid or missing required parameters

**Common Scenarios**:
- Missing Page ID
- Invalid Page ID format

**Resolution**:
1. Verify Page ID is provided
2. Check Page ID format is correct
3. Use the retry confirmation to correct the input

### System Errors (SYSTEM_ERROR)

**Cause**: Confluence API issues or network problems

**Resolution**:
1. Check Confluence service status
2. Verify network connectivity
3. Wait and retry after a few moments

## Usage Examples

### Example 1: Get All Current Attachments

**Input**:
```
Page ID: "123456"
Status: "current"
```

**Output**:
```json
{
  "Attachments": [
    {
      "Id": "att-789",
      "Title": "document.pdf",
      "Page ID": "123456",
      "Media Type": "application/pdf",
      "File Size": 2048576,
      "Download Link": "/download/attachments/123456/document.pdf",
      "Status": "current",
      "Version Number": 3
    }
  ],
  "Total Count": 1,
  "Success": true
}
```

### Example 2: Filter by Media Type

**Input**:
```
Page ID: "123456"
Media Type: "image/png"
```

**Output**: Returns only PNG image attachments.

## Business Rules

1. **Pagination**: Automatically handles cursor-based pagination for large result sets
2. **Status Filter**: Default is "current" if not specified
3. **Permission-Based**: User must have permission to view the page
4. **Version Info**: Includes version history information for staleness detection

## Limitations

1. **Single Page Only**: Retrieves attachments from one page at a time
2. **No Content**: Returns metadata only, not file content (use Download Attachment)
3. **API Limits**: Subject to Confluence API rate limits

## Related Catalog Requests

- [Get Attachment By ID](pages/GetAttachmentById.md) - Get detailed metadata for a specific attachment
- [Download Attachment](pages/DownloadAttachment.md) - Download attachment file content
- [Get Page By ID](pages/GetPageById.md) - Get page details

## See Also

- [Entities](pages/Entities.md) - Attachment entity structure reference
- [Extension Configuration](pages/ExtensionConfiguration.md) - Setup and configuration

