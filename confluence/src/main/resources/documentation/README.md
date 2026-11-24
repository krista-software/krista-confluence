# Confluence Extension Documentation

## Overview

The Confluence Extension for Krista enables seamless integration with Atlassian Confluence, allowing you to automate content management, collaborate on documentation, and streamline knowledge sharing workflows. This extension provides comprehensive functionality for managing spaces, pages, templates, and content within your Confluence environment.

Confluence is a web-based corporate wiki and collaboration platform that helps teams work together and share information on projects and ideas. With this extension, you can programmatically interact with your Confluence instance to create, read, update, and manage content at scale.

## Key Features

✅ **Space Management**
- Retrieve all spaces in your Confluence environment
- Get detailed information about specific spaces
- Access direct child pages of spaces for hierarchical navigation
- Receive real-time webhook notifications for space changes

✅ **Page Operations**
- Create pages from dynamic content or templates
- Update existing pages with new content or template variables
- Retrieve page details and content
- Search for pages within spaces
- Download pages as Word (DOCX) files
- Navigate page hierarchies (children, descendants, ancestors)

✅ **Attachment Operations**
- Discover and retrieve all attachments on a page
- Get detailed attachment metadata including version history
- Download attachment file content for document ingestion
- Filter attachments by status and media type
- Support for pagination when handling many attachments

✅ **Template Management**
- Retrieve templates from specific spaces
- Get template details including variables and structure
- Use templates to create consistent page layouts

✅ **Dynamic Content Generation**
- Generate tables with custom columns and data
- Create formatted headings with alignment options
- Generate anchor tags for hyperlinks

✅ **Webhook Integration**
- Register webhooks for real-time event notifications
- Monitor page creation, updates, and deletions
- Track space changes and modifications

## Quick Start Guide

### 1. Prerequisites
- Active Atlassian Confluence Cloud account
- Admin or appropriate permissions in Confluence
- Krista workspace with extension capabilities

### 2. Setup Steps

1. **Generate Confluence API Token**
   - Follow the guide in [Creating Confluence App](pages/CreatingConfluenceApp.md)
   - Save your API token securely

2. **Configure the Extension**
   - Add the Confluence Extension to your Krista workspace
   - Configure connection parameters as described in [Extension Configuration](pages/ExtensionConfiguration.md)
   - Validate your configuration

3. **Start Using Catalog Requests**
   - Browse available catalog requests by category
   - Follow usage examples in individual request documentation
   - Implement your automation workflows

## Documentation Structure

### 📚 Getting Started
- [Extension Configuration](pages/ExtensionConfiguration.md) - Setup and configuration guide
- [Authentication](pages/Authentication.md) - API token authentication and security
- [Creating Confluence App](pages/CreatingConfluenceApp.md) - Step-by-step Confluence setup

### 🌐 Space Operations
- [Get Spaces](pages/GetSpaces.md) - Retrieve all available spaces
- [Get Space By ID](pages/GetSpaceById.md) - Get specific space details
- [Get Direct Child Pages of Space](pages/GetDirectChildPagesOfSpace.md) - Access root-level pages
- [Get Notification](pages/GetNotification.md) - Receive webhook notifications

### 🔔 Webhook Management
- [Create Webhook](pages/CreateWebhook.md) - Register webhooks for real-time notifications

### 📋 Template Operations
- [Get Templates By Space ID](pages/GetTemplatesBySpaceId.md) - Retrieve space templates
- [Get Template By ID](pages/GetTemplateById.md) - Get specific template details

### 📄 Page Management
- [Create Page from Content](pages/CreatePageFromContent.md) - Create pages with dynamic content
- [Create Page from Template](pages/CreatePageFromTemplate.md) - Create pages using templates
- [Get Page By ID](pages/GetPageById.md) - Retrieve specific page details
- [Get Pages in Space](pages/GetPagesInSpace.md) - Search and list pages
- [Update Page by Content](pages/UpdatePageByContent.md) - Update pages with new content
- [Update Page by Template](pages/UpdatePageByTemplate.md) - Update pages using templates
- [Get Direct Child Pages of Page](pages/GetDirectChildPagesOfPage.md) - Navigate page hierarchy
- [Check if Page Has Children](pages/CheckIfPageHasChildren.md) - Verify child page existence
- [Get All Nested Descendants of Page](pages/GetAllNestedDescendantsOfPage.md) - Retrieve complete page tree
- [Get Ancestors of Page](pages/GetAncestorsOfPage.md) - Get parent page hierarchy
- [Download Page](pages/DownloadPage.md) - Export page as DOCX file

### 📎 Attachment Operations
- [Get Attachments for Page](pages/GetAttachmentsForPage.md) - Retrieve all attachments on a page
- [Get Attachment By ID](pages/GetAttachmentById.md) - Get detailed attachment metadata
- [Download Attachment](pages/DownloadAttachment.md) - Download attachment file content

### 🎨 Content Generation
- [Generate Content for Table](pages/GenerateContentForTable.md) - Create dynamic tables
- [Generate Heading Content](pages/GenerateHeadingContent.md) - Create formatted headings
- [Generate Anchor Tag](pages/GenerateAnchorTag.md) - Create hyperlinks

### 📖 Reference
- [Entities](pages/Entities.md) - Data structures and entity definitions
- [Release Notes](releaseNotes.md) - Version history and updates

## Support & Resources

### Extension Information
- **Version**: 1.0.0
- **API Version**: Confluence Cloud REST API v2
- **Ecosystem**: Collaboration
- **Domain**: Confluence

### Additional Resources
- [Atlassian Confluence Documentation](https://confluence.atlassian.com/doc/)
- [Confluence REST API Reference](https://developer.atlassian.com/cloud/confluence/rest/v2/)
- [Krista Documentation](https://docs.krista.ai/)

### Getting Help
For support with this extension:
1. Review the troubleshooting sections in individual catalog request pages
2. Check the [Authentication](pages/Authentication.md) guide for common issues
3. Consult the [Extension Configuration](pages/ExtensionConfiguration.md) for setup problems
4. Contact your Krista administrator for workspace-specific assistance

---

**Ready to get started?** Begin with the [Extension Configuration](pages/ExtensionConfiguration.md) guide to set up your Confluence integration.

