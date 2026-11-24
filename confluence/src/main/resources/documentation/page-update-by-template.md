# Update Page by Template

Update a page given the ID. Set the status and title. Provide the template and variables to use for the update

## Input Parameters

| Input parameters | Type                                                | Required |
|------------------|-----------------------------------------------------|----------|
| Page ID          | Identifier                                          | Yes      |
| Template ID      | Identifier                                          | Yes      |
| Status           | PickOne (Current, Draft)                            | No       |
| Title            | Text                                                | No       |
| Comment          | Text                                                | No       |
| Variables        | List of { Key: Identifier, Value: Text } entities  | No       |

## Output Parameters

| Output parameters | Type        |
|-------------------|-------------|
| Page              | Page entity |

