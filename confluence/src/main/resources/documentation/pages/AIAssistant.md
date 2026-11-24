# AI Assistant Tab

## Overview

The **AI Assistant** tab enables Krista's AI agent to interact with your Confluence instance using natural language. It is powered by the Krista MCP (Model Context Protocol) server and exposes Confluence catalog requests as tools the AI can invoke on your behalf.

The tab has three main sections:

1. **Setup** — Shows connection status and lets you configure an AI provider (Claude, OpenAI, or Gemini) for the natural language query pipeline.
2. **Query & AI Settings** — Provides a query playground for testing CQL queries directly and a natural language interface where you type a question in plain English; the AI translates it into a CQL query, fetches results from Confluence, and returns a formatted table or a direct answer.
3. **Help & Troubleshooting** — Shows MCP server configuration snippets for popular AI assistants (Claude Desktop, Cursor, VS Code, Windsurf) and troubleshooting tips.

---

## Prerequisites

Before using the AI Assistant tab, ensure:

- The extension is installed in your Krista workspace
- Authentication is configured and **Test Connection** passes in the **Authentication** tab
- Your authentication mode supports per-user MCP (see [Supported Auth Modes](#supported-auth-modes))

---

## Setup Panel

### Connection Status

The Setup panel displays the current connection status based on the credentials saved in the Authentication tab:

| Status | Meaning |
|--------|---------|
| **Connected** | Credentials are valid and the extension can query Confluence |
| **Not Connected** | No credentials configured or credentials are invalid |

The panel also shows:

- **Authentication method** — Quick Setup, OAuth 2.0 (User Auth), OAuth 2.0 (Server to Server), or Basic Auth
- **Site URL** — The Confluence instance being connected to
- **User email** — The email address of the authenticated user (for OAuth modes)

### AI Provider Configuration

To use the **natural language query** feature (Ask Agent), you must configure an AI provider. The AI provider is used to translate natural language questions into CQL queries and to extract answers from search results.

| Provider | Model | API Key Source |
|----------|-------|----------------|
| **Claude** (Anthropic) | claude-sonnet-4-20250514 | [console.anthropic.com](https://console.anthropic.com/) |
| **OpenAI** | gpt-4o | [platform.openai.com](https://platform.openai.com/) |
| **Gemini** (Google) | gemini-2.0-flash | [aistudio.google.com](https://aistudio.google.com/) |

To configure:

1. Select your preferred AI provider from the dropdown
2. Enter your API key
3. Click **Validate** to verify the key works
4. The key is stored securely per user — each user can configure their own provider

> **Note**: The AI provider is only needed for the natural language query pipeline. Direct CQL queries in the playground do not require an AI provider.

---

## Query Playground

The query playground lets you test CQL queries directly against your Confluence instance:

1. Enter a CQL query in the text field (e.g., `type=page AND space.key="DEV"`)
2. Click **Search** to execute the query
3. Results are displayed in a table with the following columns:

| Column | Display Type | Description |
|--------|-------------|-------------|
| Title  | Link        | Page title, links to the Confluence page |
| Type   | Pill        | Content type (page, blogpost, comment) |
| Space  | Text        | Space name where the content resides |
| Author | Text        | Display name of the last modifier |
| Status | Pill        | Content status (current, draft, etc.) |

### Example CQL Queries

| Query | Description |
|-------|-------------|
| `type=page AND space.key="DEV"` | All pages in the DEV space |
| `type=blogpost AND text~"release"` | Blog posts containing "release" |
| `title="Meeting Notes"` | Pages titled "Meeting Notes" |
| `creator=currentUser() AND type=page` | Pages created by the current user |
| `lastModified>now("-7d") AND type=page` | Pages modified in the last 7 days |
| `ancestor=12345` | All descendant pages of page 12345 |

---

## Natural Language Queries (Ask Agent)

When an AI provider is configured, you can type questions in plain English:

```
User types a natural language question
  → AI generates a CQL query from the question
  → Extension executes CQL search against Confluence
  → Results are returned as a formatted table or a direct answer
```

### Example Questions

| Question | What the AI does |
|----------|-----------------|
| `Find all pages in DEV space` | CQL: `type=page AND space.key="DEV"` → returns list |
| `Show blog posts about deployment` | CQL: `type=blogpost AND text~"deployment"` → returns list |
| `Who created the Home page?` | CQL: `title="Home"` → returns `author` field value |
| `What space is the API Docs page in?` | CQL: `title="API Docs"` → returns `space` field value |
| `Show pages modified this week` | CQL: `lastModified>now("-7d") AND type=page` → returns list |
| `Find all attachments in the Engineering space` | CQL: `type=attachment AND space.key="ENG"` → returns list |

For **list questions** (e.g., "Find all pages in DEV space"), the AI returns a results table.

For **specific questions** (e.g., "Who created the Home page?"), the AI returns a single concise answer.

---

## MCP Server Configuration

The Help panel provides ready-to-use configuration snippets for connecting popular AI assistants to your Confluence instance via the Krista MCP server:

### Supported AI Assistants

| Assistant | Configuration File |
|-----------|-------------------|
| **Claude Desktop** | `claude_desktop_config.json` |
| **Cursor** | `.cursor/mcp.json` |
| **VS Code (Copilot)** | `.vscode/mcp.json` |
| **Windsurf** | `~/.windsurf/mcp.json` |

Each snippet includes the MCP server URL derived from your extension's base URL and JAXRS path. Copy the snippet into the corresponding configuration file to enable your AI assistant to search Confluence data.

---

## Per-User Authentication (MCP OAuth Flow)

Each user who interacts with the AI Assistant authenticates independently with their own Atlassian account. This ensures that:

- The AI acts on behalf of the individual user, not a shared service account
- Permissions are correctly enforced per user
- The audit trail in the **Confluence Audit** tab reflects the correct user for every AI-triggered request

### First-Time Authorization

When a user sends their first question through the AI Assistant:

1. The extension detects no existing token for that user
2. Krista presents an **authorization link** in the AI chat
3. The user clicks the link — a Confluence sign-in popup opens
4. The user signs in and grants consent to the OAuth app
5. After the popup closes, the token is stored under the user's Krista contact ID
6. The AI retries the request automatically — this time the token is found and the query succeeds

Subsequent questions from the same user proceed without interruption; tokens are refreshed automatically.

### Supported Auth Modes

| Auth Mode | MCP Per-User Support |
|-----------|----------------------|
| Quick Setup | Yes — per-user OAuth popup |
| OAuth 2.0 — User Authentication | Yes — per-user OAuth popup |
| OAuth 2.0 — Server to Server | No — single shared service account |
| Basic Auth | No — single shared API token |

> **Recommendation**: Use **Quick Setup** or **OAuth 2.0 — User Authentication** to enable full per-user AI assistant access. Server to Server and Basic Auth modes will use a single shared credential for all AI queries.

---

## Ask Agent Catalog Request

The AI Assistant tab exposes an **Ask Agent** catalog request that can also be invoked directly from Krista workflows:

| Property | Value |
|----------|-------|
| **Name** | Ask Agent |
| **Type** | Query System |
| **Input** | `Query` (Paragraph, required) — your natural language question |
| **Output** | `Response` (FreeForm array) — matching Confluence content |

This catalog request is available in the **Collaboration** domain under the **Essentials** ecosystem, alongside all other Confluence catalog requests.

---

## Audit Visibility

All AI-triggered catalog requests are automatically recorded in the **Confluence Audit** tab:

| Event | When |
|-------|------|
| `CATALOG_REQUEST_TRIGGERED` | AI sends a query to Confluence |
| `CATALOG_REQUEST_SUCCESS` | Query returns results successfully |
| `CATALOG_REQUEST_FAILURE` | Query fails (permissions, network, etc.) |

To review AI activity, open the **Confluence Audit** tab and filter for `Ask Agent` events.

---

## Troubleshooting

### Authorization Popup Appears on Every Question

**Cause**: The per-user OAuth token was not saved, or the token has expired.

**Fix**:
1. Complete the authorization popup when prompted — wait for the "Authentication successful" message before closing it
2. Verify the callback URL in your Atlassian OAuth app matches your Extension Base URL + `/confluenceextn/callback`
3. If the popup keeps appearing, check the **Confluence Audit** tab for `AUTH_FAILURE` events

### AI Returns No Results

**Cause**: The CQL query generated from the question returned no matching content.

**Fix**:
1. Rephrase the question to be more specific (e.g., include the space key or content type)
2. Verify that the authenticated user has permission to view the content
3. Try the equivalent CQL query directly in the Query Playground to isolate the issue

### AI Returns "Not authorized" or Permission Error

**Cause**: The authenticated user does not have the required Confluence permissions, or the OAuth scopes are insufficient.

**Fix**:
1. Ensure the OAuth app has the `read:content:confluence` scope (required for content queries)
2. Verify the user account has at least **View** permission in the target Confluence space
3. Check that the correct granular scopes are granted in the Atlassian Developer Console (not classic scopes)

### AI Provider Key Validation Fails

**Cause**: The API key is invalid, expired, or has insufficient quota.

**Fix**:
1. Verify the API key is correct — re-copy it from your provider's dashboard
2. Check that the API key has available quota/credits
3. Ensure the key is for the correct provider (e.g., an Anthropic key for the Claude provider)

### MCP Not Available (Server to Server or Basic Auth)

Per-user AI queries are not supported in Server to Server or Basic Auth modes. All AI queries will run as the single configured service account or API token user. To enable per-user AI access, switch to **Quick Setup** or **OAuth 2.0 — User Authentication** in the Authentication tab.

---

## See Also

- [Authentication](/pages/Authentication.md) — Configure credentials before using the AI Assistant
- [MCP Authentication (Per-User OAuth)](/pages/Authentication.md#mcp-authentication-per-user-oauth) — Detailed per-user OAuth flow
- [Confluence Audit Dashboard](/pages/Authentication.md#audit-dashboard) — Monitor AI-triggered activity
