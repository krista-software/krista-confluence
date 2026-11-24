# Get Page Content as a File

Given a page's unique ID, download the page content as a Markdown (`.md`) file. Storage-format XHTML and Confluence
`ac:` macros (code, panels, links, images, emoticons, layouts, task lists) are converted to Markdown; the file is
named after the sanitized page title (fallback: page id).

## Input Parameters

| Input parameters | Type    | Required |
|------------------|---------|----------|
| Page ID          | Text    | Yes      |
| Allow Retry      | Boolean | No       |

## Output Parameters

| Output parameters | Type |
|-------------------|------|
| Downloaded File   | File |
