# **Catalog Requests in Confluence Extension**:

In this section, each Confluence extension request and its input parameters are explained.

## **Space**

### 1. Get Spaces

Get all spaces available in the Confluence Environment

| Output parameters | Type                   |
|-------------------|------------------------|
| Spaces            | List of Space entities |
| Space Identifiers | List of Identifiers    |

### 2. Get Space by ID

Get a single space with the associate ID

| Input parameters | Type             | Required |
|------------------|------------------|----------|
| Space Id         | Text, Identifier | Yes      |

| Output parameters | Type         |
|-------------------|--------------|
| Space             | Space entity |

### 3. Get Direct Child Pages of Space

Get only the direct child pages of a space (pages with no parent page, i.e., root pages of the space) for hierarchical navigation

| Input parameters | Type             | Required |
|------------------|------------------|----------|
| Space ID         | Text, Identifier | Yes      |

| Output parameters | Type                  |
|-------------------|-----------------------|
| Pages             | List of Page entities |
| Page Identifiers  | List of Identifiers   |
| Success           | Boolean               |

### 4. Get Notification

Receive webhook-based notifications from Confluence with real-time updates about Space changes, Page updates, and other events

| Input parameters | Type     | Required |
|------------------|----------|----------|
| eventName        | Text     | Yes      |
| eventData        | FreeForm | Yes      |

| Output parameters   | Type     |
|---------------------|----------|
| Notification Data   | FreeForm |

## **Webhook**

### 1. Create Webhook

Register a new webhook in Confluence to receive real-time notifications for specific events

| Input parameters | Type        | Required |
|------------------|-------------|----------|
| Webhook Name     | Text        | Yes      |
| Webhook URL      | Text        | Yes      |
| Events           | List of Text| No       |
| Active           | Boolean     | No       |

| Output parameters | Type                              |
|-------------------|-----------------------------------|
| Webhook Response  | { Name: Text, Success: Boolean }  |

## **Webhook Configuration**

### Overview

The Confluence extension provides comprehensive webhook functionality for real-time notifications. You can now register webhooks directly through the **Create Webhook** catalog request, eliminating the need for manual API calls.

### Webhook Registration Methods

#### Method 1: Catalog Request (Recommended)

Use the **Create Webhook** catalog request for easy webhook registration:

**Input Parameters:**
- **Webhook Name**: Descriptive name for the webhook (e.g., "Extension Webhook - Page Created")
- **Webhook URL**: Your extension's webhook endpoint URL
- **Events**: List of Confluence events to subscribe to (optional)
- **Active**: Whether the webhook should be active immediately (defaults to true)

**Output:**
- **Webhook Response**: Contains the webhook name and success status

**Example Usage:**
- Webhook Name: `"Extension Webhook - Page Events"`
- Webhook URL: `"https://extension.solution.eng.krista.app/extension/api/J9knF9IWvK7ZygFLeB2obw_e_e/rest/webhook"`
- Events: `["page_created", "page_updated", "page_removed"]`
- Active: `true`

### Supported Events

The following Confluence events can be configured for webhook notifications:

#### Space Events
- `space_created` - Triggered when a new space is created
- `space_updated` - Triggered when space properties are modified
- `space_removed` - Triggered when a space is deleted

#### Page Events
- `page_created` - Triggered when a new page is created
- `page_updated` - Triggered when page content or properties are modified
- `page_removed` - Triggered when a page is deleted
- `page_moved` - Triggered when a page is moved to a different location
- `page_restored` - Triggered when a deleted page is restored

### Problem with Single Webhook Registration

**Issue**: When registering a single webhook for multiple CRUD operations (Create, Read, Update, Delete), Confluence fires more than 8 events for each operation, creating significant duplication and processing overhead.

**Root Cause**: Confluence's webhook payloads for different actions (create/update/delete) are nearly identical and lack clear markers to distinguish the exact action performed. This makes it difficult to process events correctly and leads to:
- Event duplication
- Ambiguous event processing
- Increased system load
- Potential data inconsistencies

### Recommended Solution: Use Catalog Request with Separate Event Types

**Best Practice**: Use the **Create Webhook** catalog request to register separate webhooks for each specific event type.

**Workflow**:
1. Use the **Create Webhook** catalog request
2. Create separate webhooks for each event type (e.g., one for `page_created`, another for `page_updated`) , each registered to a unique invoker
3. Monitor webhook responses through the catalog interface
4. Events are automatically processed through the **Get Notification** request

### Webhook Registration Options

#### Option 1: Catalog Request (Recommended)

The extension now provides a **Create Webhook** catalog request that handles webhook registration . This is the recommended approach for most users as it:
- Simplifies the registration process
- Provides immediate feedback on success/failure
- Integrates seamlessly with the extension workflow
- Eliminates the need for API token management


### Example Webhook Registrations Using Catalog Request

#### 1. Page Created Webhook

**Create Webhook Catalog Request Parameters:**
- **Webhook Name**: `"Extension Webhook - Page Created"`
- **Webhook URL**: `"https://extension.solution.eng.krista.app/extension/api/J9knF9IWvK7ZygFLeB2obw_e_e/rest/webhook"`
- **Events**: `["page_created"]`
- **Active**: `true`

**Expected Response:**
```json
{
  "Webhook Response": {
    "Name": "Extension Webhook - Page Created",
    "Success": true
  }
}
```

#### 2. Page Updated Webhook

**Create Webhook Catalog Request Parameters:**
- **Webhook Name**: `"Extension Webhook - Page Updated"`
- **Webhook URL**: `"https://extension.solution.eng.krista.app/extension/api/K8mN2PqR5tUvWxYz/rest/webhook"`
- **Events**: `["page_updated"]`
- **Active**: `true`

**Expected Response:**
```json
{
  "Webhook Response": {
    "Name": "Extension Webhook - Page Updated",
    "Success": true
  }
}
```

#### 3. Space Created Webhook

**Create Webhook Catalog Request Parameters:**
- **Webhook Name**: `"Extension Webhook - Space Created"`
- **Webhook URL**: `"https://extension.solution.eng.krista.app/extension/api/L9oP3QsT6uVwXyZa/rest/webhook"`
- **Events**: `["space_created"]`
- **Active**: `true`

**Expected Response:**
```json
{
  "Webhook Response": {
    "Name": "Extension Webhook - Space Created",
    "Success": true
  }
}
```

#### 4. Page Removed Webhook

**Create Webhook Catalog Request Parameters:**
- **Webhook Name**: `"Extension Webhook - Page Removed"`
- **Webhook URL**: `"https://extension.solution.eng.krista.app/extension/api/M0qR4StU7vWxYzAb/rest/webhook"`
- **Events**: `["page_removed"]`
- **Active**: `true`

**Expected Response:**
```json
{
  "Webhook Response": {
    "Name": "Extension Webhook - Page Removed",
    "Success": true
  }
}
```


### Best Practices for New Team Members

#### Using the Catalog Request (Recommended)

1. **Start with Catalog**: Use the **Create Webhook** catalog request for all webhook registrations
2. **One Event Per Webhook**: Create separate webhooks for each event type to avoid duplication
3. **Descriptive Names**: Use clear webhook names like "Extension Webhook - Page Created"
4. **Test Individual Events**: Register and test one event type at a time
5. **Monitor Success**: Check the webhook response for success/failure status
6. **Use Get Notification**: Monitor incoming events through the **Get Notification** catalog request


### Extension Base URL Construction

The webhook URL follows this pattern:
```
https://{extension-host}/extension/api/{extension-id}/rest/webhook
```

Where:
- `{extension-host}`: The domain hosting the extension (e.g., `extension.solution.eng.krista.app`)
- `{extension-id}`: The unique identifier for your extension instance (e.g., `J9knF9IWvK7ZygFLeB2obw_e_e`)

## **Template**

### 1. Get Templates By Space ID

Get the templates inside the space identified by the ID

| Input parameters | Type             | Required |
|------------------|------------------|----------|
| Space ID         | Text, Identifier | Yes      |

| Output parameters     | Type                      |
|-----------------------|---------------------------|
| Templates             | List of Template entities |
| Template Identifiers  | List of Identifiers       |

### 2. Get Template by ID

Get a template associated with the given ID

| Input parameters | Type             | Required |
|------------------|------------------|----------|
| Template ID      | Text, Identifier | Yes      |

| Output parameters | Type            |
|-------------------|-----------------|
| Template          | Template entity |

## **Page**

### 1. Create Page from Content

Create a new page given the content in order

| Input parameters | Type                    | Required |
|------------------|-------------------------|----------|
| Page Title       | Identifier              | Yes      |
| Space ID         | Identifier              | Yes      |
| Content          | List of Content entities| No       |

| Output parameters | Type        |
|-------------------|-------------|
| Page              | Page entity |

### 2. Create Page from Template

Create a new page given the template identifier and the placeholders of the template

| Input parameters | Type                                                | Required |
|------------------|-----------------------------------------------------|----------|
| Space ID         | Identifier                                          | Yes      |
| Template ID      | Identifier                                          | Yes      |
| Page Title       | Text                                                | Yes      |
| Variables        | List of { Key: Identifier, Value: Text } entities  | No       |

| Output parameters | Type        |
|-------------------|-------------|
| Page              | Page entity |

### 3. Get Page By ID

Get the page with the given ID

| Input parameters | Type       | Required |
|------------------|------------|----------|
| Page ID          | Identifier | Yes      |

| Output parameters | Type        |
|-------------------|-------------|
| Page              | Page entity |

### 4. Get Pages in Space

Get the pages if any found in the space with the given ID

| Input parameters | Type             | Required |
|------------------|------------------|----------|
| Space ID         | Text, Identifier | Yes      |
| Title            | Text             | No       |

| Output parameters | Type                  |
|-------------------|-----------------------|
| Pages             | List of Page entities |
| Page Identifiers  | List of Identifiers   |

### 5. Update Page by Content

Update a page given the ID. Set the status and title. Provide the dynamic content to use for the update

| Input parameters | Type                     | Required |
|------------------|--------------------------|----------|
| Page ID          | Identifier               | Yes      |
| Title            | Text                     | No       |
| Status           | PickOne (Draft, Current) | No       |
| Comments         | Text                     | No       |
| Content          | List of Content entities | No       |

| Output parameters | Type        |
|-------------------|-------------|
| Page              | Page entity |

### 6. Update Page by Template

Update a page given the ID. Set the status and title. Provide the template and variables to use for the update

| Input parameters | Type                                                | Required |
|------------------|-----------------------------------------------------|----------|
| Page ID          | Identifier                                          | Yes      |
| Template ID      | Identifier                                          | Yes      |
| Status           | PickOne (Current, Draft)                            | No       |
| Title            | Text                                                | No       |
| Comment          | Text                                                | No       |
| Variables        | List of { Key: Identifier, Value: Text } entities  | No       |

| Output parameters | Type        |
|-------------------|-------------|
| Page              | Page entity |

### 7. Get Direct Child Pages of Page

Get the direct child pages of a specific page for hierarchical navigation

| Input parameters | Type             | Required |
|------------------|------------------|----------|
| Page ID          | Text, Identifier | Yes      |

| Output parameters | Type                  |
|-------------------|-----------------------|
| Pages             | List of Page entities |
| Page Identifiers  | List of Identifiers   |
| Success           | Boolean               |

### 8. Check if Page Has Children

Check if a specific page has child pages for hierarchical navigation hints

| Input parameters | Type             | Required |
|------------------|------------------|----------|
| Page ID          | Text, Identifier | Yes      |

| Output parameters | Type    |
|-------------------|---------|
| Has Children      | Boolean |
| Success           | Boolean |

### 9. Get All Nested Descendants of Page

Get all nested descendants (children, grandchildren, etc.) of a specific page recursively for complete hierarchical navigation

| Input parameters | Type | Required |
|------------------|------|----------|
| Page ID          | Text | Yes      |

| Output parameters | Type                  |
|-------------------|-----------------------|
| Pages             | List of Page entities |
| Success           | Boolean               |
| Total Count       | Number                |

### 10. Get Ancestors of Page

Get all ancestors (parent pages) of a specific page for hierarchical navigation and breadcrumb display

| Input parameters | Type | Required |
|------------------|------|----------|
| Page ID          | Text | Yes      |

| Output parameters | Type                  |
|-------------------|-----------------------|
| Ancestors         | List of Page entities |
| Success           | Boolean               |
| Total Count       | Number                |

### 11. Get Page in Space

Given a page's unique ID, download the full page including content and formatting as a Word (DOCX) file

| Input parameters | Type | Required |
|------------------|------|----------|
| Page ID          | Text | Yes      |

| Output parameters | Type    |
|-------------------|---------|
| Downloaded File   | File    |
| Parsing Success   | Boolean |

## **Content**

### 1. Generate Content for Table

Generate a content entity for a dynamic table

| Input parameters | Type              | Required |
|------------------|-------------------|----------|
| Columns          | List of Text      | Yes      |
| Table            | List of Composite | Yes      |

| Output parameters | Type           |
|-------------------|----------------|
| Table Content     | Content entity |

### 2. Generate Heading Content

Generate a dynamic heading for a confluence page

| Input parameters | Type                                        | Required |
|------------------|---------------------------------------------|----------|
| Text             | Text                                        | Yes      |
| Type             | PickOne (h1, h2, h3, h4, h5, h6)            | Yes      |
| Text Alignment   | PickOne (Center, Left, Right, Justify)      | Yes      |
| Break Line       | Boolean                                     | No       |

| Output parameters | Type           |
|-------------------|----------------|
| Heading           | Content entity |



### 3. Generate Anchor Tag

Generate an anchor tag content

| Input parameters | Type | Required |
|------------------|------|----------|
| Text             | Text | Yes      |
| Href             | Text | Yes      |

| Output parameters    | Type           |
|----------------------|----------------|
| Anchor Tag Content   | Content entity |

# **Entities in Confluence Extension**:

- **Space**

| Fields      | Type       |
|-------------|------------|
| Id          | Text       |
| Name        | Text       |
| Type        | Text       |
| Status      | Text       |
| Description | Text       |
| Created At  | Date       |

- **Template**

| Fields    | Type                                                |
|-----------|-----------------------------------------------------|
| Id        | Identifier                                          |
| Name      | Text                                                |
| Space Id  | Identifier                                          |
| Type      | Text                                                |
| Body      | Rich Text                                           |
| Variables | List of { Key: Text, Value: Text, Options: [ Text ] } |

- **Page**

| Fields       | Type       |
|--------------|------------|
| Id           | Text       |
| Space ID     | Identifier |
| Title        | Text       |
| Status       | Text       |
| Page URL     | Rich Text  |
| Body         | Rich Text  |
| Parent ID    | Text       |
| Parent Type  | Text       |
| Has Children | Boolean    |

- **Content**

| Fields | Type      |
|--------|-----------|
| ID     | Text      |
| Body   | Rich Text |
