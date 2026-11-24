# Get Notification

## Overview

Receives webhook-based notifications from Confluence with real-time updates about Space changes, Page updates, and other events.

## Request Details

- **Area**: Space
- **Type**: WAIT_FOR_EVENT
- **Retry Support**: ❌ No (Event-driven request that waits for webhook notifications)

## Input Parameters

| Parameter Name | Type | Required | Description | Example |
|----------------|------|----------|-------------|---------|
| eventName | Text | Yes | The name of the webhook event received from Confluence | `confluence_updates` |
| eventData | FreeForm | Yes | The webhook payload data from Confluence | `{...}` |

### Parameter Details

**eventName**
- **Format**: String identifier for the event type
- **Purpose**: Identifies which webhook event was triggered
- **Expected Value**: `confluence_updates` for Confluence webhook events
- **Automatic**: Populated automatically by the webhook system

**eventData**
- **Format**: FreeForm object containing webhook payload
- **Purpose**: Contains the actual event data from Confluence
- **Structure**: Varies based on event type (page_created, page_updated, etc.)
- **Automatic**: Populated automatically from webhook POST request

## Output Parameters

| Parameter Name | Type | Description |
|----------------|------|-------------|
| Notification Data | FreeForm | Parsed webhook data containing event details |

### Notification Data Structure

The notification data structure varies based on the event type but typically includes:

| Field | Type | Description |
|-------|------|-------------|
| event | Text | Event type (e.g., "page_created", "page_updated") |
| page | Object | Page details (if page event) |
| space | Object | Space details (if space event) |
| user | Object | User who triggered the event |
| timestamp | Text | When the event occurred |

## Usage Examples

### Example 1: Page Created Event

**Scenario**: Receive notification when a new page is created in Confluence.

**Input** (Automatic from webhook):
```json
{
  "eventName": "confluence_updates",
  "eventData": {
    "confluence_data": "{\"event\":\"page_created\",\"page\":{\"id\":\"789012\",\"title\":\"New Documentation\",\"space\":{\"key\":\"ENG\",\"name\":\"Engineering\"}},\"user\":{\"displayName\":\"John Doe\"}}"
  }
}
```

**Output**:
```json
{
  "Notification Data": {
    "event": "page_created",
    "page": {
      "id": "789012",
      "title": "New Documentation",
      "space": {
        "key": "ENG",
        "name": "Engineering"
      }
    },
    "user": {
      "displayName": "John Doe"
    }
  }
}
```

**Result**: Notification parsed and ready for processing.

### Example 2: Page Updated Event

**Scenario**: Receive notification when a page is modified.

**Input** (Automatic from webhook):
```json
{
  "eventName": "confluence_updates",
  "eventData": {
    "confluence_data": "{\"event\":\"page_updated\",\"page\":{\"id\":\"789012\",\"title\":\"Updated Documentation\",\"version\":2}}"
  }
}
```

**Output**:
```json
{
  "Notification Data": {
    "event": "page_updated",
    "page": {
      "id": "789012",
      "title": "Updated Documentation",
      "version": 2
    }
  }
}
```

**Result**: Update notification received and parsed.

### Example 3: Space Created Event

**Scenario**: Receive notification when a new space is created.

**Input** (Automatic from webhook):
```json
{
  "eventName": "confluence_updates",
  "eventData": {
    "confluence_data": "{\"event\":\"space_created\",\"space\":{\"key\":\"NEWSPACE\",\"name\":\"New Project Space\"}}"
  }
}
```

**Output**:
```json
{
  "Notification Data": {
    "event": "space_created",
    "space": {
      "key": "NEWSPACE",
      "name": "New Project Space"
    }
  }
}
```

**Result**: Space creation notification received.

## Business Rules

1. **Event-Driven**: This request waits for webhook events and does not actively poll
2. **Automatic Invocation**: Triggered automatically when webhooks are received
3. **JSON Parsing**: Webhook data is automatically parsed from JSON string to FreeForm object
4. **Event Filtering**: Only processes events with eventName = "confluence_updates"
5. **Real-Time**: Provides near real-time notifications of Confluence changes

## Limitations

1. **Webhook Required**: Requires webhook to be registered in Confluence (see Create Webhook)
2. **Event Types Limited**: Only receives events configured in the webhook registration
3. **No Historical Data**: Only receives events that occur after webhook registration
4. **Network Dependent**: Requires reliable network connectivity for webhook delivery
5. **No Retry**: Failed webhook deliveries are not automatically retried by Confluence

## Best Practices

### 1. Register Webhooks First
Before using this request, register webhooks using the Create Webhook catalog request.

### 2. Handle All Event Types
Implement logic to handle different event types (page_created, page_updated, page_removed, etc.).

### 3. Validate Event Data
Always validate the structure of notification data before processing.

### 4. Implement Idempotency
Handle duplicate webhook deliveries gracefully (Confluence may send the same event multiple times).

### 5. Log Webhook Events
Log all received webhooks for debugging and audit purposes.

## Common Use Cases

### 1. Real-Time Content Monitoring
```
Scenario: Monitor when pages are created or updated
Action: Register webhook for page events, process notifications
Result: Real-time awareness of content changes
```

### 2. Automated Workflows
```
Scenario: Trigger workflows when specific events occur
Action: Receive notification, check event type, trigger appropriate workflow
Result: Automated response to Confluence changes
```

### 3. Audit and Compliance
```
Scenario: Track all changes to Confluence content
Action: Log all webhook notifications with timestamps
Result: Complete audit trail of Confluence activities
```

### 4. Integration Synchronization
```
Scenario: Keep external systems in sync with Confluence
Action: Receive notifications, update external systems
Result: Synchronized data across platforms
```

## Related Catalog Requests

- [Create Webhook](pages/CreateWebhook.md) - Register webhooks to receive notifications
- [Get Page By ID](pages/GetPageById.md) - Retrieve full page details from notification
- [Get Space By ID](pages/GetSpaceById.md) - Retrieve space details from notification
- [Get Pages in Space](pages/GetPagesInSpace.md) - Query pages after space events

## Technical Implementation

### Helper Class
- **Class**: SpaceArea
- **Package**: com.kristasoft.extensions.essentials.collaboration.confluence.catalog
- **Method**: getNotification(String eventName, FreeForm eventData)
- **Service**: JsonParser.convertJsonToFreeForm()

### Processing Flow
1. Receive webhook POST request at extension endpoint
2. Extract eventName and eventData from request
3. Check if eventName equals "confluence_updates"
4. Extract JSON string from eventData.confluence_data
5. Parse JSON string to FreeForm object
6. Return parsed notification data

### Webhook Endpoint
```
POST https://{extension-host}/extension/api/{extension-id}/rest/webhook
```

### Event Processing
```java
if (eventName.equals(CONFLUENCE_UPDATES)) {
    String jsonString = (String) eventData.get(CONFLUENCE_DATA);
    return JsonParser.convertJsonToFreeForm(jsonString);
}
```

## Error Handling

### System Errors (SYSTEM_ERROR)

**Cause**: JSON parsing errors or malformed webhook data

**Common Scenarios**:
- Invalid JSON in webhook payload
- Missing required fields in event data
- Unexpected event structure

**Resolution**:
1. Verify webhook is properly configured in Confluence
2. Check webhook payload format
3. Ensure Confluence webhook version is compatible
4. Review webhook logs for malformed requests

### Authorization Errors

**Cause**: Webhook authentication failures

**Common Scenarios**:
- Webhook URL is incorrect
- Extension endpoint is not accessible
- Network firewall blocking webhook delivery

**Resolution**:
1. Verify webhook URL in Confluence settings
2. Ensure extension endpoint is publicly accessible
3. Check firewall rules allow Confluence IP ranges
4. Test webhook delivery manually

## Troubleshooting

### Issue: No Notifications Received
**Cause**: Webhook not registered or misconfigured

**Solution**:
1. Verify webhook is registered using Create Webhook
2. Check webhook URL is correct
3. Ensure webhook is active (not disabled)
4. Test webhook delivery from Confluence settings
5. Check network connectivity and firewall rules

### Issue: Malformed Notification Data
**Cause**: Unexpected webhook payload structure

**Solution**:
1. Log raw webhook payload for inspection
2. Verify Confluence webhook API version
3. Check if Confluence has updated webhook format
4. Update parsing logic if needed

### Issue: Duplicate Notifications
**Cause**: Confluence sends same event multiple times

**Solution**:
1. This is expected behavior from Confluence
2. Implement idempotency using event IDs
3. Track processed events to avoid duplicates
4. Use timestamps to detect duplicates

### Issue: Missing Event Data
**Cause**: Incomplete webhook payload

**Solution**:
1. Check webhook configuration in Confluence
2. Verify all required event types are enabled
3. Review Confluence webhook documentation
4. Contact Atlassian support if issue persists

## Webhook Configuration

### Supported Event Types

The following Confluence events can trigger notifications:

**Page Events:**
- `page_created` - New page created
- `page_updated` - Page content or properties modified
- `page_removed` - Page deleted
- `page_moved` - Page moved to different location
- `page_restored` - Deleted page restored

**Space Events:**
- `space_created` - New space created
- `space_updated` - Space properties modified
- `space_removed` - Space deleted

### Webhook Registration

To receive notifications, you must first register a webhook:

1. Use the [Create Webhook](pages/CreateWebhook.md) catalog request
2. Specify the events you want to monitor
3. Provide the extension webhook URL
4. Activate the webhook

**Example Webhook URL:**
```
https://extension.solution.eng.krista.app/extension/api/J9knF9IWvK7ZygFLeB2obw_e_e/rest/webhook
```

### Best Practices for Webhook Setup

1. **Separate Webhooks per Event Type**: Register separate webhooks for each event type to avoid duplication
2. **Descriptive Names**: Use clear webhook names like "Extension Webhook - Page Created"
3. **Test Before Production**: Test webhooks in development environment first
4. **Monitor Webhook Health**: Regularly check webhook delivery status in Confluence

## See Also

- [Create Webhook](pages/CreateWebhook.md) - Register webhooks for notifications
- [Extension Configuration](pages/ExtensionConfiguration.md) - Setup and configuration
- [Get Page By ID](pages/GetPageById.md) - Retrieve page details from events
- [Get Space By ID](pages/GetSpaceById.md) - Retrieve space details from events

