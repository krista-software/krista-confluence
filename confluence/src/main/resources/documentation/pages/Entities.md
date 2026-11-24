# Entities Reference

## Overview

This document provides comprehensive reference information for all entity structures used throughout the Confluence
Extension, including field descriptions, data types, and examples.

## Entity Types

The Confluence Extension uses five main entity types:

1. **Space** - Represents a Confluence space
2. **Page** - Represents a Confluence page
3. **Template** - Represents a page template
4. **Content** - Represents generated content markup
5. **Attachment** - Represents a file attachment on a page

---

## Space Entity

Represents a Confluence space with its metadata and properties.

### Fields

| Field Name  | Type | Description                             | Example                                          |
|-------------|------|-----------------------------------------|--------------------------------------------------|
| Id          | Text | Unique identifier for the space         | `"123456"`                                       |
| Name        | Text | Display name of the space               | `"Engineering Documentation"`                    |
| Type        | Text | Space type (e.g., "global", "personal") | `"global"`                                       |
| Status      | Text | Current status of the space             | `"current"`                                      |
| Description | Text | Space description                       | `"Technical documentation for engineering team"` |
| Created At  | Date | Timestamp when the space was created    | `"2024-01-15T10:30:00Z"`                         |

### Field Details

**Id**

- Format: Numeric string
- Uniqueness: Unique across the Confluence instance
- Usage: Used to identify spaces in API requests

**Name**

- Format: Text string
- Purpose: Human-readable space name
- Display: Shown in Confluence UI and navigation

**Type**

- Format: Text string
- Common Values:
    - `"global"` - Standard team space
    - `"personal"` - Personal user space
- Purpose: Indicates space category

**Status**

- Format: Text string
- Common Values:
    - `"current"` - Active space
    - `"archived"` - Archived space
- Purpose: Indicates space state

**Description**

- Format: Text string (may be empty)
- Purpose: Provides context about space purpose
- Optional: May be null or empty

**Created At**

- Format: ISO 8601 date-time string
- Purpose: Tracks space creation date
- Timezone: UTC

### Example Space Entity

```json
{
  "Id": "123456",
  "Name": "Engineering Documentation",
  "Type": "global",
  "Status": "current",
  "Description": "Technical documentation and guides for the engineering team",
  "Created At": "2024-01-15T10:30:00Z"
}
```

### Used In

- [Get Spaces](pages/GetSpaces.md)
- [Get Space By ID](pages/GetSpaceById.md)

---

## Page Entity

Represents a Confluence page with its content, metadata, and hierarchy information.

### Fields

| Field Name   | Type       | Description                               | Example                                                          |
|--------------|------------|-------------------------------------------|------------------------------------------------------------------|
| Id           | Text       | Unique identifier for the page            | `"987654"`                                                       |
| Space ID     | Identifier | ID of the space containing the page       | `"123456"`                                                       |
| Title        | Text       | Page title                                | `"Getting Started Guide"`                                        |
| Status       | Text       | Page status                               | `"current"`                                                      |
| Page URL     | Rich Text  | Full URL to access the page               | `"https://mycompany.atlassian.net/wiki/spaces/ENG/pages/987654"` |
| Body         | Rich Text  | Page content in Confluence storage format | `"<h1>Welcome</h1><p>Content...</p>"`                            |
| Parent ID    | Text       | ID of parent page (null for root pages)   | `"987653"` or `null`                                             |
| Parent Type  | Text       | Type of parent (typically "page")         | `"page"` or `null`                                               |
| Has Children | Boolean    | Whether the page has child pages          | `true` or `false`                                                |

### Field Details

**Id**

- Format: Numeric string
- Uniqueness: Unique across the Confluence instance
- Usage: Used to identify pages in API requests

**Space ID**

- Format: Numeric string or space key
- Purpose: Links page to its containing space
- Relationship: References Space entity

**Title**

- Format: Text string
- Max Length: 255 characters
- Uniqueness: Must be unique within parent or space root
- Purpose: Human-readable page name

**Status**

- Format: Text string
- Common Values:
    - `"current"` - Published page
    - `"draft"` - Draft page
    - `"archived"` - Archived page
- Purpose: Indicates page state

**Page URL**

- Format: Full HTTPS URL
- Purpose: Direct link to view page in Confluence
- Usage: For navigation and sharing

**Body**

- Format: Confluence storage format (HTML-like)
- Purpose: Complete page content
- Includes: Text, headings, lists, tables, macros, images
- Size: Can be very large for content-rich pages

**Parent ID**

- Format: Numeric string or null
- Purpose: Indicates page hierarchy
- Null Value: Indicates root-level page
- Relationship: References another Page entity

**Parent Type**

- Format: Text string or null
- Common Value: `"page"`
- Purpose: Indicates type of parent
- Null Value: Indicates root-level page

**Has Children**

- Format: Boolean
- Purpose: Indicates if page has child pages
- Usage: For navigation and hierarchy decisions
- Performance: Avoids unnecessary child retrieval calls

### Example Page Entity

```json
{
  "Id": "987654",
  "Space ID": "123456",
  "Title": "Getting Started Guide",
  "Status": "current",
  "Page URL": "https://mycompany.atlassian.net/wiki/spaces/ENG/pages/987654",
  "Body": "<h1>Welcome</h1><p>This guide will help you get started with our platform.</p><h2>Prerequisites</h2><ul><li>Account access</li><li>Basic knowledge</li></ul>",
  "Parent ID": null,
  "Parent Type": null,
  "Has Children": true
}
```

### Used In

- [Get Page By ID](pages/GetPageById.md)
- [Get Pages in Space](pages/GetPagesInSpace.md)
- [Get Direct Child Pages of Space](pages/GetDirectChildPagesOfSpace.md)
- [Get Direct Child Pages of Page](pages/GetDirectChildPagesOfPage.md)
- [Get All Nested Descendants of Page](pages/GetAllNestedDescendantsOfPage.md)
- [Get Ancestors of Page](pages/GetAncestorsOfPage.md)
- [Download Page](pages/DownloadPage.md)
- [Create Page from Content](pages/CreatePageFromContent.md)
- [Create Page from Template](pages/CreatePageFromTemplate.md)
- [Update Page by Content](pages/UpdatePageByContent.md)
- [Update Page from Template](pages/UpdatePageByTemplate.md)

---

## Template Entity

Represents a Confluence page template with its structure and metadata.

### Fields

| Field Name  | Type         | Description                                   | Example                                  |
|-------------|--------------|-----------------------------------------------|------------------------------------------|
| Id          | Text         | Unique identifier for the template            | `"789012"`                               |
| Name        | Text         | Display name of the template                  | `"Meeting Notes Template"`               |
| Description | Text         | Template description                          | `"Standard template for meeting notes"`  |
| Space ID    | Identifier   | ID of the space containing the template       | `"123456"`                               |
| Body        | Rich Text    | Template content in Confluence storage format | `"<h2>Attendees</h2><ul><li></li></ul>"` |
| Labels      | List of Text | Labels/tags associated with the template      | `["meetings", "notes"]`                  |

### Field Details

**Id**

- Format: Numeric string
- Uniqueness: Unique across the Confluence instance
- Usage: Used to identify templates in API requests

**Name**

- Format: Text string
- Purpose: Human-readable template name
- Display: Shown in template selection UI

**Description**

- Format: Text string (may be empty)
- Purpose: Explains template purpose and usage
- Optional: May be null or empty

**Space ID**

- Format: Numeric string or space key
- Purpose: Links template to its containing space
- Relationship: References Space entity

**Body**

- Format: Confluence storage format (HTML-like)
- Purpose: Template structure and default content
- Includes: Placeholders, default text, structure
- Usage: Copied to new pages when template is used

**Labels**

- Format: List of text strings
- Purpose: Categorizes and tags templates
- Usage: For filtering and organization
- Optional: May be empty list

### Example Template Entity

```json
{
  "Id": "789012",
  "Name": "Meeting Notes Template",
  "Description": "Standard template for team meeting notes with agenda, attendees, and action items",
  "Space ID": "123456",
  "Body": "<h2>Meeting Date</h2><p></p><h2>Attendees</h2><ul><li></li></ul><h2>Agenda</h2><ol><li></li></ol><h2>Discussion Notes</h2><p></p><h2>Action Items</h2><table><tbody><tr><th>Action</th><th>Owner</th><th>Due Date</th></tr><tr><td></td><td></td><td></td></tr></tbody></table>",
  "Labels": [
    "meetings",
    "notes",
    "agenda"
  ]
}
```

### Used In

- [Get Templates By Space ID](pages/GetTemplatesBySpaceId.md)
- [Get Template By ID](pages/GetTemplateById.md)
- [Create Page from Template](pages/CreatePageFromTemplate.md)
- [Update Page from Template](pages/UpdatePageByTemplate.md)

---

## Content Entity

Represents generated content markup for use in page creation and updates.

### Types

Content entities are generated by content helper requests and returned as Rich Text in Confluence storage format.

### Content Types

**Table Content**

- Generated By: [Generate Content for Table](pages/GenerateContentForTable.md)
- Format: `<table>` element with headers and rows
- Usage: Embedded in page body

**Heading Content**

- Generated By: [Generate Heading Content](pages/GenerateHeadingContent.md)
- Format: `<h1>` through `<h6>` elements
- Usage: Embedded in page body for structure

**Anchor Content**

- Generated By: [Generate Anchor Tag](pages/GenerateAnchorTag.md)
- Format: `<a href="...">` elements
- Usage: Embedded in page body for links

### Example Content Entities

**Table Content Example**:

```xml

<table>
    <tbody>
        <tr>
            <th>Name</th>
            <th>Email</th>
        </tr>
        <tr>
            <td>John Doe</td>
            <td>john@example.com</td>
        </tr>
    </tbody>
</table>
```

**Heading Content Example**:

```xml

<h2>Installation Instructions</h2>
```

**Anchor Content Example**:

```xml

<a href="https://example.com">Visit our website</a>
```

### Used In

- [Create Page from Content](pages/CreatePageFromContent.md)
- [Update Page by Content](pages/UpdatePageByContent.md)

---

## Attachment Entity

Represents a file attachment on a Confluence page with its metadata and version information.

### Fields

| Field Name         | Type       | Description                                   | Example                                                                                   |
|--------------------|------------|-----------------------------------------------|-------------------------------------------------------------------------------------------|
| Id                 | Text       | Unique identifier for the attachment          | `"att-123456"`                                                                            |
| Title              | Text       | Filename of the attachment                    | `"quarterly-report.pdf"`                                                                  |
| Page ID            | Identifier | ID of the page containing the attachment      | `"987654"`                                                                                |
| Media Type         | Text       | MIME type of the file                         | `"application/pdf"`                                                                       |
| File Size          | Number     | Size of the file in bytes                     | `5242880`                                                                                 |
| Download Link      | Text       | Full URL to download the attachment           | `"https://mycompany.atlassian.net/wiki/download/attachments/987654/quarterly-report.pdf"` |
| Web UI Link        | Text       | Full URL to view the attachment in Confluence | `"https://mycompany.atlassian.net/wiki/pages/viewpageattachments.action?pageId=987654"`   |
| Created At         | Number     | Timestamp when attachment was first created   | `1704067200000`                                                                           |
| Status             | Text       | Attachment status                             | `"current"` or `"archived"`                                                               |
| Version Number     | Number     | Current version number                        | `5`                                                                                       |
| Version Created At | Number     | Timestamp when current version was created    | `1735689600000`                                                                           |

### Field Details

**Id**

- Format: Text string (typically prefixed with "att-")
- Uniqueness: Unique across the Confluence instance
- Usage: Used to identify attachments in API requests

**Title**

- Format: Text string (filename)
- Purpose: Original filename of the uploaded file
- Includes: File extension

**Page ID**

- Format: Numeric string
- Purpose: Links attachment to its parent page
- Relationship: References Page entity

**Media Type**

- Format: MIME type string
- Common Values:
    - `"application/pdf"` - PDF documents
    - `"image/png"` - PNG images
    - `"image/jpeg"` - JPEG images
    - `"application/vnd.openxmlformats-officedocument.wordprocessingml.document"` - Word documents
- Purpose: Indicates file type for processing

**File Size**

- Format: Number (bytes)
- Purpose: Size of the file for download planning
- Usage: Check before downloading large files

**Download Link**

- Format: Full, browser-usable URL (site base + `/wiki/download/...`)
- Purpose: Used to download the file content
- Usage: Open directly in a browser, or pass to the Download Attachment request (which also accepts the full URL)

**Web UI Link**

- Format: Full, browser-usable URL (site base + `/wiki/pages/...`)
- Purpose: Link to view attachment in Confluence UI
- Usage: For navigation and sharing

**Created At**

- Format: Unix timestamp (milliseconds)
- Purpose: When the attachment was first uploaded
- Timezone: UTC

**Status**

- Format: Text string
- Values:
    - `"current"` - Active attachment
    - `"archived"` - Archived attachment
- Purpose: Indicates attachment state

**Version Number**

- Format: Number
- Purpose: Current version of the attachment
- Usage: Track changes and updates

**Version Created At**

- Format: Unix timestamp (milliseconds)
- Purpose: When the current version was uploaded
- Usage: Staleness detection for caching

### Example Attachment Entity

```json
{
  "Id": "att-123456",
  "Title": "quarterly-report.pdf",
  "Page ID": "987654",
  "Media Type": "application/pdf",
  "File Size": 5242880,
  "Download Link": "https://mycompany.atlassian.net/wiki/download/attachments/987654/quarterly-report.pdf",
  "Web UI Link": "https://mycompany.atlassian.net/wiki/pages/viewpageattachments.action?pageId=987654",
  "Created At": 1704067200000,
  "Status": "current",
  "Version Number": 5,
  "Version Created At": 1735689600000
}
```

### Used In

- [Get Attachments for Page](pages/GetAttachmentsForPage.md)
- [Get Attachment By ID](pages/GetAttachmentById.md)
- [Download Attachment](pages/DownloadAttachment.md)

---

## Common Patterns

### Hierarchy Relationships

Pages form a tree structure:

```
Space
└── Root Page (Parent ID = null)
    ├── Child Page 1 (Parent ID = Root Page ID)
    │   └── Grandchild Page (Parent ID = Child Page 1 ID)
    └── Child Page 2 (Parent ID = Root Page ID)
```

### Entity Relationships

```
Space (1) ──── (many) Pages
Space (1) ──── (many) Templates
Page (1) ──── (many) Child Pages
Page (many) ──── (1) Parent Page
Page (1) ──── (many) Attachments
```

### Identifier Types

- **Space ID**: Identifies spaces
- **Page ID**: Identifies pages
- **Template ID**: Identifies templates
- **Parent ID**: References parent page
- **Attachment ID**: Identifies attachments

All IDs are text strings and unique within their entity type.

## See Also

- [Extension Configuration](pages/ExtensionConfiguration.md) - Setup and configuration
- [Authentication](pages/Authentication.md) - Authentication details
- All catalog request pages for entity usage examples

