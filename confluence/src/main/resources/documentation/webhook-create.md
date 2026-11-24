# Create Webhook

Register a new webhook in Confluence to receive real-time notifications for specific events

## Input Parameters

| Input parameters | Type        | Required |
|------------------|-------------|----------|
| Webhook Name     | Text        | Yes      |
| Webhook URL      | Text        | Yes      |
| Events           | List of Text| No       |
| Active           | Boolean     | No       |

## Output Parameters

| Output parameters | Type                              |
|-------------------|-----------------------------------|
| Webhook Response  | { Name: Text, Success: Boolean }  |

