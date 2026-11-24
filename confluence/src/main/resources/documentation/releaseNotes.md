# Release Notes for Confluence Extension

## Version 4.0.4 - Current Release (Feature: Check Page Restriction; Improvement: Download Attachment v1 Path Support)

- **Developer**: Simran Sethi
- **Krista Service APIs (Java)**: 1.0.125
- **Global Catalog Version**: GC-2026.7.5
- **Release Date**: July 2026

### New Features

#### Check Page Restriction

Added a new catalog request, **Check Page Restriction**, that reports whether a page has read or update restrictions.

- **Input**: Page ID
- **Output**: Page ID and a Message field (`Restricted` or `Not Restricted`)
- Checks the page's own read/update restrictions via the same v1 restriction endpoint already used by "Get Pages in Space"'s exclude-restricted filter, then walks the page's ancestor chain — fetched one hop at a time via `Get Page By ID`-style lookups reading each page's `parentId` — since Confluence's restriction API only ever reports restrictions set directly on a page, never ones inherited from a parent. A child page with no restriction of its own can still be effectively restricted if any ancestor is.
- Deliberately avoids the dedicated v2 ancestors endpoint (`GET /pages/{id}/ancestors`), which requires the `read:content.metadata:confluence` scope that this app doesn't currently request.

### Improvements

#### Download Attachment (v1) — Additional Accepted Link Format

"Download Attachment" (v1) now also accepts the v1 REST API child-attachment download path (`/wiki/rest/api/content/{pageId}/child/attachment/{attachmentId}/download`), in addition to the existing `/wiki/download/...` and `/download/attachments/...` formats. This is the same download path already used internally by "Download Attachment (v2)" — the one that reliably works under API-token/OAuth auth.

### Breaking Changes

**None** — both changes are additive; existing "Download Attachment" links and workflows continue to work unchanged.

---

## Version 4.0.3 - Previous Release (Bug Fix: Attachment Pagination Status Filter)

- **Developer**: Simran Sethi
- **Krista Service APIs (Java)**: 1.0.125
- **Global Catalog Version**: GC-2026.7.4
- **Release Date**: July 2026

### Bug Fix

- **[KE-3413] Confluence: Get Attachments for Page returning all versions instead of only current** — Fixed a regression where "Get Attachments for Page" returned every version/status of an attachment (current, archived, trashed) instead of honoring the selected "Status" filter, once a page's results spanned more than one page of pagination.

**Details:**

- Confluence's cursor-based pagination `next` link only carries the pagination cursor — it does not echo back the original `status`/`mediaType` filters that were sent on the first request.
- `PageAreaAttachmentHelper` was using only the `next` link's query parameters when fetching subsequent pages, silently dropping the `status`/`mediaType` filters after the first page and causing every following page to return unfiltered (all-status) results.
- Subsequent-page requests now re-merge the original `status` and `mediaType` filters with the pagination cursor, so filtering is honored consistently across all pages, not just the first.

### Breaking Changes

**None** — This is a bug fix; attachment counts returned by "Get Attachments for Page" with a status filter will now be lower and accurate (previously inflated by unfiltered pagination pages).

---

## Version 4.0.2 - Current Release (Improvement: Get Full Page URL)

- **Developer**: Simran Sethi
- **Krista Service APIs (Java)**: 1.0.125
- **Global Catalog Version**: GC-2026.7.3
- **Release Date**: July 2026

### Improvement

- **[KE-3413] Confluence: Get full page URL** — Page and attachment links now resolve to fully-qualified, browser-openable URLs using the site's real base URL instead of an assumed `{domain}.atlassian.net` host.

**Details:**

- `getDomainURL()` now resolves the user-facing Confluence base URL from the OAuth accessible resource (3LO and 2LO), falling back to the configured domain name under Basic Auth. Resolution failures fall back gracefully so link building never throws.
- Attachment `downloadLink` and `webUILink` values (returned by the v2 API as `/wiki`-relative paths) are now prefixed with the resolved site base URL, so they open correctly in a browser.
- Download validation and correction now normalize fully-qualified download URLs to their site-relative path before checking the Confluence download prefix, keeping "Download Attachment" working for both relative and absolute links.

### Breaking Changes

**None** — Existing catalog requests continue to work; links are simply returned in fully-qualified form.

---

## Version 4.0.1 - Current Release (Feature: AI Assistant / MCP Support)

- **Developer**: Simran Sethi
- **Krista Service APIs (Java)**: 1.0.125
- **Global Catalog Version**: GC-2026.7.3
- **Release Date**: July 2026
### Fixes

- Synced catalog requests that were missing compared to the previous version — restored the requests dropped during the refactor so parity with the earlier release is maintained.


## Version 4.0.0 - Current Release (Feature: AI Assistant / MCP Support)

- **Developer**: Simran Sethi
- **Krista Service APIs (Java)**: 1.0.121
- **Global Catalog Version**: GC-2026.6.5
- **Release Date**: June 2026
### New Features

#### AI Assistant Tab — MCP (Model Context Protocol) Support

Added a new **AI Assistant** tab powered by the Atlassian Authentication SDK's MCP infrastructure. This enables Krista's AI agent and external AI assistants to search Confluence content using natural language or CQL queries.

**Key Features:**

- **Connection Status** — Displays current authentication status, method, site URL, and user email
- **AI Provider Configuration** — Supports Claude (Anthropic), OpenAI, and Gemini (Google) for natural language query translation
- **Query Playground** — Test CQL queries directly against Confluence and view results in a formatted table
- **Natural Language Queries (Ask Agent)** — Type questions in plain English; the AI translates to CQL, executes the search, and returns a formatted table or a direct answer
- **MCP Server Configuration** — Ready-to-use configuration snippets for Claude Desktop, Cursor, VS Code (Copilot), and Windsurf
- **Per-User OAuth** — Each user authenticates independently via OAuth 2.0 popup for per-user permission enforcement

**New Files:**

- `mcp/MCPArea.java` — Catalog entry point for "Ask Agent" natural language query
- `mcp/ConfluenceMcpProductConfig.java` — CQL-based product configuration for Confluence search
- `mcp/ConfluenceMcpQueryService.java` — Two-phase NL query pipeline (question → CQL → results)
- `mcp/ConfluenceMcpSetupResource.java` — JAX-RS resource serving the MCP setup tab

**Modified Files:**

- `ConfluenceExtension.java` — Added "AI Assistant" custom tab
- `ConfluenceApplication.java` — Registered `ConfluenceMcpSetupResource`

### Documentation

- Created [AI Assistant](pages/AIAssistant.md) — Comprehensive guide covering setup, query playground, natural language queries, MCP server configuration, per-user auth, and troubleshooting
- Updated [Sidebar](_sidebar.md) — Added AI Assistant section

### Breaking Changes

**None** — AI Assistant is an additive feature. All existing catalog requests and authentication modes continue to work.

---

## Version 2.0.12

- **Developer**: Asem Abdelfattah, simran sethi
- **Krista Service APIs (Java)**: 1.0.121
- **Global Catalog Version**: GC-2026.3.4
- **Release Date**: March 2026

### Improvement :

- [KE-3097] : Added pagination support for Get Spaces — fetches all pages of spaces instead of just the first page.

## Version 2.0.11

- **Developer**: Asem Abdelfattah
- **Krista Service APIs (Java)**: 1.0.121
- **Global Catalog Version**: GC-2026.3.3
- **Release Date**: March 2026

### Improvement :

- [KE-3097] : Confluence - Space entity throws error for created time.

## Version 2.0.10 (Feature: OAuth 2.0 Authentication)

- **Developer**: Simran Sethi
- **Krista Service APIs (Java)**: 1.0.121
- **Global Catalog Version**: GC-2026.3.1
- **Release Date**: March 2026

### Improvement :

- [KE-3052] :Added documentation for enabling Sharing Mode in the Atlassian OAuth application for Confluence to allow
  multiple users to authorize and test the integration.

## Version 2.0.9 - Current Release (Feature: OAuth 2.0 Authentication)

- **Developer**: Simran Sethi
- **Krista Service APIs (Java)**: 1.0.120
- **Global Catalog Version**: GC-2026.2.1
- **Release Date**: February 2026

### New Features

#### OAuth 2.0 Authentication Support

Added OAuth 2.0 (Authorization Code Grant) as an alternative authentication method alongside existing Basic Auth (API
Token). The implementation follows the same pattern as the Jira Service Management extension.

**Key Features:**

- OAuth 2.0 Authorization Code Grant flow with Atlassian Cloud
- Automatic token refresh using stored refresh tokens
- Popup-based authorization flow with "Authentication successful" confirmation
- User-level and app-wide token storage for flexible authentication
- Alternative user authentication via checkbox (admin configures credentials, users authorize individually)

**New Extension Attributes:**

- **Client Id** — OAuth 2.0 Client ID from the Atlassian Developer Console
- **Client Secret** — OAuth 2.0 Client Secret (stored securely)

**Credential Validation:**

- Detects and rejects mixed credentials (both OAuth and Basic Auth provided)
- Detects incomplete credentials (partial OAuth or partial Basic Auth)
- Client ID format validation (alphanumeric, hyphens, underscores only)
- Client Secret minimum length validation (32 characters)
- Clear, actionable error messages for all validation scenarios

**OAuth Scopes (Granular):**

- User Identity API: `read:me`
- Confluence API: `read:space:confluence`, `write:space:confluence`, `read:page:confluence`, `write:page:confluence`,
  `read:content:confluence`, `write:content:confluence`, `read:content-details:confluence`, `read:user:confluence`,
  `read:template:confluence`, `write:template:confluence`, `read:attachment:confluence`, `write:attachment:confluence`,
  `read:configuration:confluence`, `write:configuration:confluence`

### New Files

- `authentication/ConfluenceApi20.java` — Atlassian OAuth 2.0 API endpoint configuration
- `authentication/SessionManager.java` — OAuth refresh token storage using KeyValueStore
- `authentication/ConfluenceOAuthService.java` — Core OAuth service with token management and accessible resources
- `authentication/ConfluenceRequestAuthenticator.java` — Request authenticator for OAuth popup flow
- `api/AuthenticationResource.java` — JAX-RS callback endpoint for OAuth authorization

### Modified Files

- `ConfluenceExtension.java` — Added OAuth field annotations, jaxrsId, and authenticator support; version bumped to
  2.0.9
- `ConfluenceAttributes.java` — Added dual-auth validation logic, OAuth REST client creation, and credential validation
  methods
- `client/ConfluenceRESTClient.java` — Added OAuth constructor with Bearer token support and Atlassian Cloud base URL
  routing
- `utils/ConfluenceConstants.java` — Added OAuth-related constants
- `api/ConfluenceApplication.java` — Registered AuthenticationResource for callback handling
- `build.gradle` — Added ScribeJava dependency (`com.github.scribejava:scribejava-apis:8.3.3`)

### Documentation

- Created [Getting OAuth 2.0 Credentials](pages/GettingOAuth2Credentials.md) — Step-by-step guide for creating an OAuth
  app in the Atlassian Developer Console
- Updated [Authentication](pages/Authentication.md) — Now covers both OAuth 2.0 and Basic Auth methods with
  troubleshooting
- Updated [Extension Configuration](pages/ExtensionConfiguration.md) — Now covers both authentication method
  configurations
- Updated [config.md](config.md) — Added Option A (OAuth 2.0) and Option B (Basic Auth) setup flows
- Updated [Sidebar](_sidebar.md) — Added OAuth 2.0 credentials link

### Breaking Changes

**None** — Basic Auth (API Token) continues to work as before. OAuth 2.0 is an additional authentication option.

---

## Version 2.0.8 - Previous Release (Feature: User Management)

- **Developer**: Avadh Dobaria
- **Krista Service APIs (Java)**: 1.0.120
- **Global Catalog Version**: GC-2026.2.1
- **Release Date**: February 2026

### New Features

#### User Management Operations

Added new catalog request for retrieving Confluence user information:

**Get User By Account ID**

- Retrieves detailed user information by Atlassian Account ID
- Returns comprehensive user profile data including display name, email, account type, and status
- Includes profile picture information when available
- Input validation with meaningful error messages
- Telemetry tracking for monitoring and debugging

### New Entity

**User Entity** - Represents a Confluence user with the following fields:

- Account ID, Display Name, Email, Public Name
- Account Type, Account Status
- Time Zone, Locale
- Is External Collaborator, Is Guest
- Profile Picture Path

### Code Quality Improvements

- Replaced string concatenations with Java 21 text blocks for better readability
- Fixed SonarQube issue java:S6068 (simplified Mockito method calls)
- Refactored duplicate tests into parameterized tests
- Modernized Stream API usage (replaced `.collect(Collectors.toList())` with `.toList()`)
- Added comprehensive JavaDoc comments for empty constructors
- Defined constants for duplicate string literals

### User Experience Improvements

#### Enhanced Error Messages

Completely redesigned all error messages across the extension to be more user-friendly, actionable, and consistent:

**Error Message Structure:**

- Clear explanation of what happened
- Possible causes (bullet points)
- Actionable next steps (numbered list)
- Technical details for debugging

**Files Updated:**

- Service Layer: `UserServiceImpl`, `SpaceServiceImpl`, `TemplateServiceImpl`, `PageServiceImpl`
- REST Client: `ConfluenceRESTClient` (connection and interruption errors)
- Catalog Areas: `UserArea`, `SpaceArea`, `TemplateArea`, `PageArea` (14 messages), `WebhookArea` (2 messages)

**Benefits:**

- Users can quickly understand what went wrong
- Clear guidance on how to resolve issues
- Reduced support burden with self-service troubleshooting
- Consistent error experience across all operations

### Documentation

- Added comprehensive documentation for Get User By Account ID catalog request
- Updated Entities.md with User entity reference
- Added detailed JavaDoc comments explaining Gson deserialization requirements
- Created ERROR_MESSAGES_IMPROVEMENT_PLAN.md documenting error message improvements

### Breaking Changes

**None**

---

## Version 2.0.7 (Feature: Attachment Operations)

- **Developer**: Avadh Dobaria
- **Krista Service APIs (Java)**: 1.0.120
- **Global Catalog Version**: GC-2026.1.1
- **Release Date**: December 2025

### New Features

#### Attachment Operations for Document Discovery and Ingestion

Added three new catalog requests for working with Confluence page attachments, enabling document discovery and ingestion
workflows:

**1. Get Attachments for Page**

- Retrieves all file attachments on a Confluence page
- Supports cursor-based pagination for pages with many attachments
- Filter by status (current, archived, any) and media type (MIME type)
- Returns comprehensive attachment metadata including version information

**2. Get Attachment By ID**

- Retrieves detailed metadata for a specific attachment
- Includes version history information for staleness detection
- Supports retry mechanism for validation errors

**3. Download Attachment**

- Downloads the binary content of an attachment file
- Creates Krista File objects for ingestion into Document Sets
- Automatic download link correction (adds `/wiki/` prefix if missing)
- Security validation to prevent path traversal attacks
- Filename extraction from URL or Content-Disposition header

### New Entity

**Attachment Entity** - Represents a file attachment with the following fields:

- Id, Title, Page ID, Media Type, File Size
- Download Link, Web UI Link
- Created At, Status
- Version Number, Version Created At

### Documentation

- Added individual documentation pages for each attachment catalog request
- Updated Entities.md with Attachment entity reference
- Updated README.md with Attachment Operations section

### Breaking Changes

**None**

---

## Version 2.0.6 - Previous Release (Bug Fix: Get Direct Child Pages of Space)

- **Developer**: Avadh Dobaria
- **Krista Service APIs (Java)**: 1.0.118
- **Global Catalog Version**: GC-2025.12.3
- **Release Date**: December 2025

### Bug Fixes

#### Fixed "Get Direct Child Pages of Space" Returning Nested Pages

The `Get Direct Child Pages of Space` catalog request was incorrectly returning all pages in the space, including child
pages of other pages (grandchildren). Now it correctly returns only the root-level pages that are direct children of the
space.

**Technical Details:**

- Changed the API call to use the `depth=root` query parameter instead of fetching all pages and filtering client-side
- This is more efficient as the filtering happens at the Confluence API level
-
Reference: [Confluence REST API v2 - Get pages in space](https://developer.atlassian.com/cloud/confluence/rest/v2/api-group-page/#api-spaces-id-pages-get)

### Breaking Changes

**None**

---

## Version 2.0.5 - Previous Release (Bug Fix: Content Children API Endpoints)

- **Developer**: Krista Development Team
- **Krista Service APIs (Java)**: 1.0.115
- **Global Catalog Version**: GC-2025.12.1
- **Release Date**: December 2025

### Bug Fixes

#### Fixed Incorrect API Endpoints for Content Children Operations

Corrected the V2 API endpoint paths for `WHITEBOARD`, `DATABASE`, and `EMBED` content types in `ContentType.java`. The
endpoints now correctly use `direct-children` instead of `children`, matching the Confluence REST API v2 specification.

| Content Type | Before (Incorrect)              | After (Correct)                     |
|--------------|---------------------------------|-------------------------------------|
| `whiteboard` | `/whiteboards/{id}/children`    | `/whiteboards/{id}/direct-children` |
| `database`   | `/databases/{id}/children`      | `/databases/{id}/direct-children`   |
| `embed`      | `/custom-content/{id}/children` | `/embeds/{id}/direct-children`      |

#### Fixed EMBED Content Type API Path Segment

Changed the API path segment for `EMBED` content type from `custom-content` to `embeds` to match the Confluence API
specification.

### Affected Operations

- `hasChildren()` - Now correctly checks for children of whiteboards, databases, and embeds
- `getDirectChildPagesOfPage()` - Now works correctly for all content types
- Any operation using `ContentChildrenStrategyFactory` for non-page content types

### Breaking Changes

**None**

---

## Version 2.0.4 - Previous Release (Feature: Confluence Folders Support)

- **Developer**: Krista Development Team
- **Krista Service APIs (Java)**: 1.0.115
- **Global Catalog Version**: GC-2025.11.1
- **Release Date**: November 2025
- **Branch**: feature/KE-2725

### New Features

#### Confluence Folders and Multi-Content Type Support

Added comprehensive support for Confluence folders and all V2 API content types with new catalog requests for
hierarchical content navigation:

**Supported Content Types:**

- `page` - Regular Confluence pages with content
- `folder` - Organizational containers for pages and other content
- `whiteboard` - Collaborative whiteboards
- `database` - Confluence databases
- `embed` - Embedded content items

**Key Features:**

- V2 API migration for improved performance and folder support
- Type-aware endpoint routing using Strategy pattern
- Cursor-based pagination for large result sets
- Full parent/child relationship tracking with parentId and parentType fields
- Backward compatible - existing catalog requests continue to work

### Technical Improvements

- **V1 to V2 API Migration**: Migrated children and ancestors endpoints from deprecated V1 API to modern V2 API
- **Strategy Pattern**: Implemented `ContentType` enum and `ContentChildrenStrategy` for type-aware API routing
- **New DTOs**: Added V2-specific DTOs for children and ancestors responses
- **Enhanced Page Entity**: Added `type`, `parentId`, and `parentType` fields to Page entity
- **Fallback Handling**: V1 API fallback for ancestor details when V2 fails

### V2 API Endpoints Used

| Content Type | Children Endpoint                               | Ancestors Endpoint                        |
|--------------|-------------------------------------------------|-------------------------------------------|
| `page`       | `/wiki/api/v2/pages/{id}/direct-children`       | `/wiki/api/v2/pages/{id}/ancestors`       |
| `folder`     | `/wiki/api/v2/folders/{id}/direct-children`     | `/wiki/api/v2/folders/{id}/ancestors`     |
| `whiteboard` | `/wiki/api/v2/whiteboards/{id}/direct-children` | `/wiki/api/v2/whiteboards/{id}/ancestors` |
| `database`   | `/wiki/api/v2/databases/{id}/direct-children`   | `/wiki/api/v2/databases/{id}/ancestors`   |
| `embed`      | `/wiki/api/v2/embeds/{id}/direct-children`      | `/wiki/api/v2/embeds/{id}/ancestors`      |

### Documentation

- Updated Page entity documentation with new fields
- Added Hierarchy area documentation
- Updated existing catalog request descriptions

### Backward Compatibility

**100% Backward Compatible** - All existing catalog requests continue to work. New features are additive only.

### Breaking Changes

**None**

---

## Version 2.0.3 - Previous Release

- **Developer**: Vrushali Gaikwad
- **Krista Service APIs (Java)**: 1.0.115
- **Global Catalog Version**: GC-2025.11.1
- **Release Date**: October 7th, 2025
- **Appliance Release Version**: 3.5.4

---

## Support and Feedback

### Getting Help

- Documentation and guides available online
- Community forum for user discussions
- Email support for technical issues
- Phone support for enterprise customers

### Feature Requests

- Submit requests through our product portal
- Email suggestions to our product team
- Discuss ideas in the community forum
- Work with customer success for enterprise features

### Bug Reports

- Submit detailed reports through our support portal
- Email critical issues directly to support
- Check our status page for known issues
- Use emergency contact for production issues

---

## Version History Summary

| Version    | Release Date  | Key Features                                                                                                                                                                                                           |
|------------|---------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **4.0.4**  | July 2026     | Check Page Restriction catalog request (page + ancestor-chain read/update restriction check); Download Attachment (v1) now also accepts the v1 child-attachment download path                                          |
| **4.0.3**  | July 2026     | Bug Fix — Get Attachments for Page status filter dropped on subsequent pagination pages                                                                                                                                 |
| **4.0.2**  | July 2026     | [KE-3413] Get full page URL — page and attachment links resolved to fully-qualified, browser-openable URLs using the site's real base URL                                                                              |
| **2.0.13** | June 2026     | AI Assistant Tab — MCP support with CQL query playground, natural language queries (Ask Agent), AI provider configuration (Claude/OpenAI/Gemini), MCP server config for external AI assistants, per-user OAuth          |
| **2.0.12** | March 2026    | Pagination support for Get Spaces — fetches all pages of spaces instead of just the first page                                                                                                                         |
| **2.0.11** | March 2026    | Bug Fix — Space entity throws error for created time                                                                                                                                                                   |
| **2.0.10** | March 2026    | Documentation — Added Sharing Mode documentation for Atlassian OAuth application                                                                                                                                       |
| **2.0.9**  | February 2026 | OAuth 2.0 Authentication - Authorization Code Grant flow with Atlassian Cloud, dual auth support (OAuth + Basic Auth), credential validation, granular scopes, comprehensive documentation                             |
| **2.0.8** | February 2026 | User Management - Get User By Account ID, User entity, comprehensive error message improvements (30+ messages across 10 files), code quality improvements (text blocks, parameterized tests, Stream API modernization) |
| **2.0.7** | December 2025 | Attachment Operations - Get Attachments for Page, Get Attachment By ID, Download Attachment for document ingestion                                                                                                     |
| **2.0.6** | December 2025 | Bug Fix - Fixed "Get Direct Child Pages of Space" returning nested pages instead of only root-level pages                                                                                                              |
| **2.0.5** | December 2025 | Bug Fix - Fixed incorrect API endpoints for whiteboard, database, and embed content children operations                                                                                                                |
| **2.0.4** | November 2025 | Confluence Folders Support - V2 API migration, 4 new Hierarchy catalog requests, multi-content type support                                                                                                            |
| **2.0.3** | October 2025  | Previous stable release                                                                                                                                                                                                |

Stay updated with the latest features and improvements by subscribing to our release notifications!
