# Create Webhook

## Overview

Registers a new webhook in Confluence to receive real-time notifications for specific events such as page creation, updates, deletions, and space changes.

## Request Details

- **Area**: Webhook
- **Type**: CHANGE_SYSTEM
- **Retry Support**: ❌ No (Webhook registration is a one-time operation)

## Input Parameters

| Parameter Name | Type | Required | Description | Example |
|----------------|------|----------|-------------|---------|
| Webhook Name | Text | Yes | Descriptive name for the webhook | `"Extension Webhook - Page Created"` |
| Webhook URL | Text | Yes | The endpoint URL where Confluence will send webhook notifications | `"https://extension.solution.eng.krista.app/extension/api/ABC123/rest/webhook"` |
| Events | List of Text | No | List of Confluence event types to subscribe to | `["page_created", "page_updated"]` |
| Active | Boolean | No | Whether the webhook should be active immediately (defaults to true) | `true` |

### Parameter Details

**Webhook Name**
- **Format**: Descriptive text string
- **Purpose**: Identifies the webhook in Confluence settings
- **Best Practice**: Use clear names like "Extension Webhook - Page Created"
- **Example**: "Extension Webhook - Page Events"

**Webhook URL**
- **Format**: Full HTTPS URL to the extension webhook endpoint
- **Pattern**: `https://{extension-host}/extension/api/{extension-id}/rest/webhook`
- **Requirements**: Must be publicly accessible from Confluence Cloud
- **Security**: HTTPS required for production use

**Events**
- **Format**: Array of event type strings
- **Optional**: If not specified, webhook receives all event types
- **Recommendation**: Specify specific events to reduce noise
- **See**: Supported Events section below for complete list

**Active**
- **Format**: Boolean (true/false)
- **Default**: true if not specified
- **Purpose**: Controls whether webhook is immediately active
- **Use Case**: Set to false for testing before activation

## Output Parameters

| Parameter Name | Type | Description |
|----------------|------|-------------|
| Webhook Response | { Name: Text, Success: Boolean } | Contains webhook name and success status |

### Webhook Response Structure

| Field | Type | Description |
|-------|------|-------------|
| Name | Text | The name of the created webhook |
| Success | Boolean | true if webhook was created successfully, false otherwise |

## Supported Events

### Page Events

| Event Type | Description | When Triggered |
|------------|-------------|----------------|
| `page_created` | New page created | User creates a new page in any space |
| `page_updated` | Page modified | User edits page content or properties |
| `page_removed` | Page deleted | User deletes a page |
| `page_moved` | Page relocated | User moves page to different location |
| `page_restored` | Page undeleted | User restores a deleted page |

### Space Events

| Event Type | Description | When Triggered |
|------------|-------------|----------------|
| `space_created` | New space created | User creates a new Confluence space |
| `space_updated` | Space modified | User changes space properties |
| `space_removed` | Space deleted | User deletes a space |

## Usage Examples

### Example 1: Register Webhook for Page Creation

**Scenario**: Monitor when new pages are created in Confluence.

**Input**:
```
Webhook Name: "Extension Webhook - Page Created"
Webhook URL: "https://extension.solution.eng.krista.app/extension/api/J9knF9IWvK7ZygFLeB2obw_e_e/rest/webhook"
Events: ["page_created"]
Active: true
```

**Output**:
```json
{
  "Webhook Response": {
    "Name": "Extension Webhook - Page Created",
    "Success": true
  }
}
```

**Result**: Webhook successfully registered. Will receive notifications when pages are created.

### Example 2: Register Webhook for Multiple Page Events

**Scenario**: Monitor page creation, updates, and deletions.

**Input**:
```
Webhook Name: "Extension Webhook - Page Events"
Webhook URL: "https://extension.solution.eng.krista.app/extension/api/K8mN2PqR5tUvWxYz/rest/webhook"
Events: ["page_created", "page_updated", "page_removed"]
Active: true
```

**Output**:
```json
{
  "Webhook Response": {
    "Name": "Extension Webhook - Page Events",
    "Success": true
  }
}
```

**Result**: Webhook registered for three event types.

### Example 3: Register Inactive Webhook for Testing

**Scenario**: Create webhook but don't activate it yet for testing purposes.

**Input**:
```
Webhook Name: "Extension Webhook - Test"
Webhook URL: "https://extension.solution.eng.krista.app/extension/api/TEST123/rest/webhook"
Events: ["page_created"]
Active: false
```

**Output**:
```json
{
  "Webhook Response": {
    "Name": "Extension Webhook - Test",
    "Success": true
  }
}
```

**Result**: Webhook created but not active. Can be activated later in Confluence settings.

### Example 4: Registration Failure

**Scenario**: Webhook registration fails due to invalid URL.

**Input**:
```
Webhook Name: "Extension Webhook - Invalid"
Webhook URL: "http://invalid-url"
Events: ["page_created"]
Active: true
```

**Output**:
```json
{
  "Webhook Response": {
    "Name": "Extension Webhook - Invalid",
    "Success": false
  }
}
```

**Result**: Webhook registration failed. Check URL and try again.

## Business Rules

1. **Unique Webhook Names**: Each webhook should have a unique, descriptive name
2. **HTTPS Required**: Webhook URLs must use HTTPS protocol for security
3. **Public Accessibility**: Webhook endpoint must be accessible from Confluence Cloud
4. **Event Specificity**: Specify only the events you need to reduce processing overhead
5. **One Event Per Webhook**: Best practice is to create separate webhooks for each event type
6. **Automatic Activation**: Webhooks are active by default unless explicitly set to false

## Limitations

1. **No Webhook Listing**: This request creates webhooks but doesn't list existing ones
2. **No Webhook Deletion**: Use Confluence UI to delete webhooks
3. **No Webhook Updates**: Cannot modify existing webhooks; must delete and recreate
4. **Event Duplication**: Single webhook with multiple events may receive duplicate notifications
5. **Rate Limiting**: Confluence may rate-limit webhook deliveries under high load
6. **No Filtering**: Cannot filter events by space, user, or other criteria
7. **Delivery Guarantee**: Confluence does not guarantee webhook delivery (at-most-once delivery)

## Best Practices

### 1. Use Separate Webhooks per Event Type
Create individual webhooks for each event type to avoid duplication and simplify processing.

**✅ Recommended:**
```
Webhook 1: ["page_created"]
Webhook 2: ["page_updated"]
Webhook 3: ["page_removed"]
```

**❌ Avoid:**
```
Webhook 1: ["page_created", "page_updated", "page_removed"]
```

### 2. Use Descriptive Webhook Names
Include the event type and purpose in the webhook name for easy identification.

**✅ Good Names:**
- "Extension Webhook - Page Created"
- "Krista Integration - Page Updates"
- "Automation - Space Events"

**❌ Poor Names:**
- "Webhook 1"
- "Test"
- "My Webhook"

### 3. Verify Webhook URL Before Registration
Test that your webhook endpoint is accessible before registering the webhook.

### 4. Start with Specific Events
Begin with specific events you need rather than subscribing to all events.

### 5. Monitor Webhook Health
Regularly check webhook delivery status in Confluence settings.

### 6. Implement Idempotency
Handle duplicate webhook deliveries gracefully in your processing logic.

## Common Use Cases

### 1. Real-Time Content Monitoring
```
Scenario: Track all page changes in real-time
Action: Register webhooks for page_created, page_updated, page_removed
Result: Receive immediate notifications of content changes
```

### 2. Automated Workflows
```
Scenario: Trigger workflows when pages are created
Action: Register webhook for page_created event
Result: Automatically process new pages
```

### 3. Audit and Compliance
```
Scenario: Log all Confluence activities for compliance
Action: Register webhooks for all event types
Result: Complete audit trail of changes
```

### 4. Integration Synchronization
```
Scenario: Keep external systems in sync with Confluence
Action: Register webhooks for relevant events
Result: External systems updated in real-time
```

## Related Catalog Requests

- [Get Notification](pages/GetNotification.md) - Receive and process webhook notifications
- [Get Page By ID](pages/GetPageById.md) - Retrieve page details from webhook events
- [Get Space By ID](pages/GetSpaceById.md) - Retrieve space details from webhook events

## Technical Implementation

### Helper Class
- **Class**: WebhookArea
- **Package**: com.kristasoft.extensions.essentials.collaboration.confluence.catalog
- **Method**: createWebhook(String webhookName, String webhookURL, List<String> events, Boolean active)
- **Service**: ConfluenceService.getConfluenceAttributes().getRestClient().doSyncPost()

### Processing Flow
1. Validate input parameters
2. Set default active state (true if not specified)
3. Build webhook request payload
4. Call Confluence webhook API
5. Parse response and check status code
6. Return success/failure status

### API Endpoint
```
POST https://{domain}.atlassian.net/wiki/rest/webhooks/1.0/webhook
```

### Request Payload
```json
{
  "name": "Extension Webhook - Page Created",
  "url": "https://extension.solution.eng.krista.app/extension/api/ABC123/rest/webhook",
  "events": ["page_created"],
  "active": true
}
```

## Error Handling

### System Errors (SYSTEM_ERROR)

**Cause**: Confluence API errors or network issues

**Common Scenarios**:
- Invalid webhook URL format
- Webhook endpoint not accessible
- Confluence API temporarily unavailable
- Network connectivity issues

**Resolution**:
1. Verify webhook URL is correct and accessible
2. Check HTTPS is used (not HTTP)
3. Ensure endpoint is publicly accessible
4. Verify network connectivity
5. Check Confluence service status

### Authorization Errors

**Cause**: Insufficient permissions to create webhooks

**Common Scenarios**:
- User lacks admin permissions
- API token doesn't have webhook creation rights
- Space-level restrictions

**Resolution**:
1. Verify account has admin or space admin permissions
2. Check API token permissions
3. Contact Confluence administrator for access
4. Ensure account is not restricted

## Troubleshooting

### Issue: Webhook Registration Fails
**Cause**: Invalid URL or inaccessible endpoint

**Solution**:
1. Verify webhook URL is correct
2. Test endpoint accessibility from external network
3. Ensure HTTPS is used
4. Check firewall rules allow incoming connections
5. Verify extension is running and accessible

### Issue: Success = false in Response
**Cause**: Confluence rejected the webhook registration

**Solution**:
1. Check webhook URL format
2. Verify endpoint is publicly accessible
3. Ensure webhook name is unique
4. Review Confluence webhook limits
5. Check Confluence logs for specific error

### Issue: Webhooks Not Firing
**Cause**: Webhook registered but not receiving events

**Solution**:
1. Verify webhook is active in Confluence settings
2. Check event types are correct
3. Test by manually triggering events in Confluence
4. Verify endpoint is receiving POST requests
5. Check webhook delivery logs in Confluence

### Issue: Duplicate Webhooks
**Cause**: Multiple registrations with same or similar names

**Solution**:
1. Use unique, descriptive names for each webhook
2. Delete duplicate webhooks in Confluence UI
3. Implement webhook name versioning if needed

## Webhook URL Construction

### URL Pattern
```
https://{extension-host}/extension/api/{extension-id}/rest/webhook
```

### Components

**extension-host**
- The domain hosting the Krista extension
- Example: `extension.solution.eng.krista.app`
- Must be publicly accessible from internet

**extension-id**
- Unique identifier for your extension instance
- Found in extension configuration
- Example: `J9knF9IWvK7ZygFLeB2obw_e_e`

### Example URLs
```
Production:
https://extension.solution.eng.krista.app/extension/api/J9knF9IWvK7ZygFLeB2obw_e_e/rest/webhook

Staging:
https://extension-staging.company.com/extension/api/K8mN2PqR5tUvWxYz/rest/webhook
```

## See Also

- [Get Notification](pages/GetNotification.md) - Process webhook notifications
- [Extension Configuration](pages/ExtensionConfiguration.md) - Setup and configuration
- [Authentication](pages/Authentication.md) - Authentication details
- [Confluence Webhooks Documentation](https://developer.atlassian.com/cloud/confluence/modules/webhook/) - Official Atlassian guide

