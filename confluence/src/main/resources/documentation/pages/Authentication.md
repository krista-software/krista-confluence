# Authentication

## Overview

The Confluence Extension supports four authentication configurations, each suited for different needs:

| # | Configuration | Auth Flow | User Sign-In | Best For |
|---|--------------|-----------|--------------|----------|
| 1 | **Quick Setup** | Authorization Code (Delegated) | Yes | Quick setup, testing, small teams |
| 2 | **OAuth 2.0 — User Authentication** | Authorization Code (Delegated) | Yes | Enterprise control, custom permissions |
| 3 | **OAuth 2.0 — Server to Server** | Client Credentials (2LO) | No (service account) | Automated workflows, service accounts |
| 4 | **Basic Auth** | HTTP Basic | No (API token) | Simple automation, development |

## Authentication Tab

The **Authentication** tab in the extension provides a unified interface for all authentication types. It is organized with a left sidebar and a main panel:

- **Quick Setup** — OAuth 2.0 with Krista's pre-registered Atlassian app (managed credentials). Scopes are displayed as read-only chips.
- **Basic Auth** — Domain Name, Account Email, and API Key (HTTP Basic authentication).
- **OAuth 2.0** — Your own Atlassian Developer Console app with two sub-tabs:
  - **User Authentication** — Individual user signs in via browser popup (Authorization Code / 3LO flow).
  - **Server to Server** — App-only authentication with no user sign-in (Client Credentials / 2LO flow).

After selecting the type, enter the required fields, click **Test Connection**, and then **Save Changes**.

---

## Quick Setup

### What It Is

Uses Krista's pre-registered Atlassian OAuth 2.0 application for authentication. The user signs in via a browser popup and grants delegated permissions. No Atlassian Developer Console configuration is required.

### When to Use

- Getting started quickly with minimal setup
- Development and testing environments
- Organizations where creating an Atlassian OAuth app is not feasible
- Small teams that don't need custom permission policies

### Required Fields

| Field | Required | Description |
|-------|----------|-------------|
| Client ID | Auto-filled | Provided by Krista's pre-registered app |
| Client Secret | Auto-filled | Provided by Krista's pre-registered app |

### How It Works

1. Select the **Quick Setup** tab in the Authentication screen
2. Click **Test Connection** — a Confluence sign-in popup opens
3. Sign in with your Atlassian account and grant the requested permissions
4. After the popup closes, the connection is verified automatically
5. Click **Save Changes** to persist the configuration

![Quick Setup tab](../_media/auth_quick_setup.png)

### Authentication Flow

```
User clicks Test Connection
  → Browser popup opens Atlassian login page
  → User signs in and grants consent to Krista's Atlassian OAuth app
  → Atlassian returns authorization code to callback URL
  → Extension exchanges code for access + refresh tokens
  → Tokens stored securely under the invoker ID
  → Subsequent API calls use stored tokens with automatic refresh
```

### Scopes

All scopes are fixed and displayed as **read-only chips** in the Quick Setup tab. They cannot be changed in Quick Setup mode:

| Scope | Purpose |
|-------|---------|
| `read:space:confluence` | Read space metadata and listings |
| `write:space:confluence` | Create and update spaces |
| `read:page:confluence` | Read page content |
| `write:page:confluence` | Create and update pages |
| `read:content:confluence` | Read general content |
| `write:content:confluence` | Write general content |
| `read:content-details:confluence` | Read detailed content properties |
| `read:user:confluence` | Read user profiles |
| `read:template:confluence` | Read page templates |
| `write:template:confluence` | Create and update templates |
| `read:attachment:confluence` | Read attachments |
| `write:attachment:confluence` | Upload and update attachments |
| `read:configuration:confluence` | Read space configuration |
| `write:configuration:confluence` | Update space configuration |
| `offline_access` | Obtain refresh tokens for automatic renewal |

### Limitations

- Cannot customize which scopes are requested
- Cannot apply organization-specific conditional access policies
- Krista's Atlassian app is shared across all Quick Setup users

---

## OAuth 2.0 — User Authentication

### What It Is

Uses your own Atlassian OAuth 2.0 application for delegated (3LO) authentication. Each user signs in via a browser popup through your own Atlassian Developer Console app. This provides full control over permissions, scopes, and audit logging.

### When to Use

- Production environments requiring enterprise-grade security
- Organizations that need to restrict the OAuth app to their Atlassian instance
- When you need full audit trails through Atlassian's admin logs
- When you need to customize exactly which scopes are requested
- Per-user authentication (each user authenticates with their own account)

### Prerequisites

- A registered Atlassian OAuth 2.0 app (see [Getting OAuth 2.0 Credentials](GettingOAuth2Credentials.md))
- Application (Client) ID and Client Secret
- Required scopes granted in the Atlassian Developer Console
- Callback URL configured in your Atlassian app (see [Callback URL](#callback-url) below)

### Required Fields

| Field | Required | Description |
|-------|----------|-------------|
| Client ID | Yes | OAuth app Client ID from Atlassian Developer Console |
| Client Secret | Yes | OAuth app Client Secret |
| Account Email | Optional | Atlassian account email that will authorize this OAuth app |

### How It Works

1. Select the **OAuth 2.0** tab → **User Authentication** sub-tab in the Authentication screen
2. Enter your Client ID and Client Secret
3. Toggle the scopes you want to request in the **Permissions (Scopes)** panel
4. Click **Test Connection** — a Confluence sign-in popup opens
5. Sign in with your Atlassian account and grant consent to your app
6. After the popup closes, the connection is verified
7. Click **Save Changes** to persist the configuration

![OAuth 2.0 User Authentication tab](../_media/auth_user_authentication.png)

### Authentication Flow

```
User clicks Test Connection
  → Browser popup opens: https://auth.atlassian.com/authorize
  → User authenticates against Atlassian
  → User grants consent to your OAuth app
  → Atlassian returns authorization code to callback URL
  → Extension exchanges code + client_secret for access + refresh tokens
  → Tokens stored securely under the invoker ID
```

### Scope Configuration

When using User Authentication, you can toggle individual scopes in the **Permissions (Scopes)** panel. Only select scopes that match the permissions granted in your Atlassian app.

In the Atlassian Developer Console, go to **Permissions** → **Confluence API** → **Edit Scopes** and enable the following:

![Confluence API Scopes](../_media/confluence_api_scopes.png)

| Scope | Catalog Requests Enabled |
|-------|--------------------------|
| `read:content:confluence` | View detailed contents |
| `write:content:confluence` | Create and update contents |
| `read:content-details:confluence` | View content details |
| `read:page:confluence` | Get Page By Id, Get Pages In Space, Get Direct Child Pages, Get All Descendants, Get Ancestors |
| `write:page:confluence` | Create Page From Content, Create Page From Template, Update Page By Content, Update Page By Template |
| `read:attachment:confluence` | Get Attachments For Page, Get Attachment By Id, Download Attachment |
| `write:attachment:confluence` | Attach File |
| `read:template:confluence` | Get Template By Id, Get Templates By Space Id |
| `write:template:confluence` | Create, update and delete content templates |
| `read:user:confluence` | View user details |
| `read:configuration:confluence` | View Confluence settings, Get Notification |
| `write:configuration:confluence` | Update Confluence settings |
| `read:space:confluence` | Get Spaces, Get Space By Id, Get Space By Alias |
| `write:space:confluence` | Create and update spaces |
| `offline_access` | Required — enables refresh tokens for automatic renewal |

> **Important**: Ensure all selected scopes are granted in your Atlassian Developer Console. Missing scopes will cause specific catalog requests to fail. Use **granular scopes** (e.g., `read:space:confluence`), not classic scopes.

### Callback URL

The callback URL is based on your **Extension Base URL**, which is unique to each installed extension. You can find it on the extension's details page in Krista:

![Extension Base URL](../_media/extension_base_url.png)

**Format:**
```
{Extension Base URL}/confluenceextn/callback
```

**Example:**
```
https://extension.poc.krista.app/extension/api/1_stxUNzSHtEs2pz_svomLvw_e_e/confluenceextn/callback
```

Copy your **Extension Base URL** from the Krista extension details page, append `/confluenceextn/callback`, and register that full URL as the Callback URL in your Atlassian Developer Console under the **Authorization** tab.

---

## OAuth 2.0 — Server to Server

### What It Is

Uses your own Atlassian OAuth 2.0 application for app-only (2LO) authentication. No user sign-in is required — the app acts on behalf of the organization using application permissions. Requires a service-account credential from the Atlassian organization admin.

### When to Use

- Fully automated workflows where no user browser popup is feasible
- Service-account based integrations
- Workflows that should run as the organization/admin account, not a specific user
- CI/CD pipelines and background automation

### Prerequisites

- A registered Atlassian OAuth 2.0 app with 2LO (service account) enabled
- Application (Client) ID and Client Secret from an organization admin service-account credential
- Account Email of the Atlassian admin or service account

### Required Fields

| Field | Required | Description |
|-------|----------|-------------|
| Client ID | Yes | OAuth app Client ID (organization admin service-account credential) |
| Client Secret | Yes | OAuth app Client Secret |
| Account Email | Yes | Email of the Atlassian admin or service account |

### How It Works

1. Select the **OAuth 2.0** tab → **Server to Server** sub-tab in the Authentication screen
2. Enter your Client ID, Client Secret, and Account Email
3. Click **Test Connection** — the extension authenticates directly (no popup)
4. Click **Save Changes** to persist the configuration

![OAuth 2.0 Server to Server tab](../_media/auth_server_to_server.png)

---

## Basic Auth

### What It Is

Uses an Atlassian API token combined with your account email and Confluence subdomain for HTTP Basic authentication. No browser popup or OAuth flow is required.

### When to Use

- Fully automated workflows where no browser popup is feasible
- Development and testing environments
- Read-only automation tasks
- Environments where OAuth is not supported

### Prerequisites

- A Confluence user account with appropriate permissions
- An Atlassian API token (see [Creating Confluence App](CreatingConfluenceApp.md))

### Required Fields

| Field | Required | Description |
|-------|----------|-------------|
| Domain Name | Yes | Your Confluence subdomain (e.g., `your-org`) from `https://your-org.atlassian.net` |
| Account Email | Yes | Email address of the Atlassian account that generated the API token |
| API Key | Yes | API token generated from Atlassian account settings |

### How It Works

1. Select the **Basic Auth** tab in the Authentication screen
2. Enter Domain Name, Account Email, and API Key
3. Click **Test Connection** — the extension validates credentials directly (no popup)
4. Click **Save Changes** to persist the configuration

![Basic Auth tab](../_media/auth_basic_auth.png)

### Generating an API Token

1. Go to [Atlassian Account Settings](https://id.atlassian.com/manage-profile/security/api-tokens)
2. Click **Create API token**
3. Enter a label (e.g., "Krista Confluence Integration")
4. Copy the token immediately — it will not be shown again

---

## MCP Authentication (Per-User OAuth)

When the Krista MCP (Model Context Protocol) server calls catalog requests on behalf of individual users, each user authenticates independently via their own OAuth 2.0 flow.

### How It Works

1. The MCP agent calls a catalog request with `invokeAsUser=true`
2. The extension detects no existing token for that user
3. A `MustAuthorizeException` is raised — the Krista runtime presents an **authorization link** to the user
4. The user clicks the link and signs in to their Atlassian account in a browser popup
5. After authorization, the popup closes and the token is stored under the user's account ID
6. The MCP agent retries the catalog request — the user's token is found and the call succeeds

### Supported Auth Modes for MCP

| Auth Mode | MCP Support |
|----------|-------------|
| Quick Setup | Yes — per-user auth via popup |
| OAuth 2.0 — User Authentication | Yes — per-user auth via popup |
| OAuth 2.0 — Server to Server | No — single shared service account token |
| Basic Auth | No — single shared API token |

---

## Token Management

### Token Errors

| Error | Action |
|-------|--------|
| Token expired | Remove stored token, user must re-authenticate |
| Permissions revoked | Remove stored token, user must re-consent |
| Invalid client credentials | Surface error, do not clear token |
| Service unavailable | Retry, keep token |

---

## Audit Dashboard

The **Confluence Audit** tab provides visibility into authentication events and catalog request activity. Every catalog request is automatically tracked:

| Event | When Recorded |
|-------|---------------|
| `CATALOG_REQUEST_TRIGGERED` | When a catalog request starts execution |
| `CATALOG_REQUEST_SUCCESS` | When a catalog request completes successfully |
| `CATALOG_REQUEST_FAILURE` | When a catalog request fails |
| `CREDENTIALS_TEST_TRIGGERED` | When Test Connection is initiated |
| `CREDENTIALS_TEST_SUCCESS` | When Test Connection passes |
| `CREDENTIALS_TEST_FAILURE` | When Test Connection fails |
| `CREDENTIALS_SAVE_TRIGGERED` | When Save Changes is initiated |
| `CREDENTIALS_SAVE_SUCCESS` | When credentials are saved |
| `CREDENTIALS_SAVE_FAILURE` | When saving credentials fails |

---

## Troubleshooting

### Test Connection Fails with No Popup (OAuth 2.0)

**Cause**: Browser popup blocked or callback URL mismatch.
**Fix**: Allow popups for the Krista domain. Verify the callback URL in the Atlassian Developer Console matches your Extension Base URL + `/confluenceextn/callback` (e.g. `https://extension.poc.krista.app/extension/api/{your-routing-id}/confluenceextn/callback`). See [Callback URL](#callback-url) for how to find your Extension Base URL.

### Error: "Unauthorized; scope does not match"

**Cause**: Classic scopes used instead of granular scopes, or a required scope is missing.
**Fix**: Verify you added **granular scopes** (e.g., `read:space:confluence`) in the Atlassian Developer Console, not classic scopes. After updating scopes, re-authorize.

### Error: "Authentication failed" (Basic Auth)

**Cause**: Account Email or Domain Name is incorrect.
**Fix**: Verify the email matches the account that generated the API token. Domain Name should be only the subdomain (e.g., `your-org`, not `https://your-org.atlassian.net`).

### Common Validation Errors

| Error | Cause | Solution |
|-------|-------|----------|
| "Please choose ONE authentication method" | No credentials or incomplete credentials provided | Provide either full OAuth credentials or all three Basic Auth fields |
| "Do not mix credentials from both methods" | OAuth and Basic Auth fields both filled | Choose one method and clear the other's fields |
| "Incomplete OAuth 2.0 credentials" | Only Client ID or only Client Secret provided | Provide both Client ID and Client Secret |
| "Incomplete Basic Auth credentials" | Only 1 or 2 of 3 Basic Auth fields provided | Provide all three: Domain Name, Account Email, API Key |

---

## Security Best Practices

1. **Use OAuth 2.0 for production** — tokens auto-refresh; API tokens never expire automatically
2. **Use Quick Setup for testing** — no app registration overhead
3. **Use User Authentication for enterprise** — provides audit trails and conditional access policies
4. **Use Server to Server for automation** — no user interaction required
5. **Grant minimal scopes** — only add the scopes your workflows actually need
6. **Rotate API tokens every 90 days** — set reminders before expiry
7. **Monitor the Audit Dashboard** — watch for repeated `CREDENTIALS_TEST_FAILURE` events

---

## See Also

- [Extension Configuration](ExtensionConfiguration.md) — Step-by-step setup for each auth type
- [Getting OAuth 2.0 Credentials](GettingOAuth2Credentials.md) — Registering an Atlassian OAuth app
- [Creating Confluence App (API Token)](CreatingConfluenceApp.md) — Generating an Atlassian API token
