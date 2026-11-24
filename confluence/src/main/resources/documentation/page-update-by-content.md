# Update Page by Content

Update a page given the ID. Set the status and title. Provide the dynamic content to use for the update

## Input Parameters

| Input parameters | Type                     | Required |
|------------------|--------------------------|----------|
| Page ID          | Identifier               | Yes      |
| Title            | Text                     | No       |
| Status           | PickOne (Draft, Current) | No       |
| Comments         | Text                     | No       |
| Content          | List of Content entities | No       |

## Output Parameters

| Output parameters | Type        |
|-------------------|-------------|
| Page              | Page entity |

