# Generate Content for Table

## Overview

Generates properly formatted Confluence storage format markup for tables, simplifying the creation of table content for pages.

## Request Details

- **Area**: Content
- **Type**: QUERY_SYSTEM
- **Retry Support**: ❌ No (Simple content generation helper)

## Input Parameters

| Parameter Name | Type | Required | Description | Example |
|----------------|------|----------|-------------|---------|
| Table Headers | List of Text | Yes | Column headers for the table | `["Name", "Email", "Role"]` |
| Table Rows | List of List of Text | Yes | Data rows for the table (each row is a list of cell values) | `[["John Doe", "john@example.com", "Developer"], ["Jane Smith", "jane@example.com", "Manager"]]` |

### Parameter Details

**Table Headers**
- **Format**: List of text strings
- **Purpose**: Defines the column headers
- **Order**: Headers appear in the order provided
- **Required**: Yes (at least one header)

**Table Rows**
- **Format**: List of lists (2D array)
- **Purpose**: Defines the table data
- **Structure**: Each inner list represents one row
- **Cell Count**: Each row should have same number of cells as headers
- **Required**: Yes (at least one row)

## Output Parameters

| Parameter Name | Type | Description |
|----------------|------|-------------|
| Table Content | Rich Text | Confluence storage format markup for the table |

## Usage Examples

### Example 1: Simple Table

**Scenario**: Generate a table with team member information.

**Input**:
```
Table Headers: ["Name", "Email", "Role"]
Table Rows: [
  ["John Doe", "john@example.com", "Developer"],
  ["Jane Smith", "jane@example.com", "Manager"],
  ["Bob Johnson", "bob@example.com", "Designer"]
]
```

**Output**:
```xml
<table>
  <tbody>
    <tr>
      <th>Name</th>
      <th>Email</th>
      <th>Role</th>
    </tr>
    <tr>
      <td>John Doe</td>
      <td>john@example.com</td>
      <td>Developer</td>
    </tr>
    <tr>
      <td>Jane Smith</td>
      <td>jane@example.com</td>
      <td>Manager</td>
    </tr>
    <tr>
      <td>Bob Johnson</td>
      <td>bob@example.com</td>
      <td>Designer</td>
    </tr>
  </tbody>
</table>
```

**Result**: Properly formatted table markup ready for page creation.

### Example 2: Use in Page Creation

**Scenario**: Create a page with a table.

**Workflow**:
```
1. Call Generate Content for Table with headers and rows
2. Receive table markup in Table Content
3. Combine with other content:
   "<h1>Team Directory</h1><p>Our team members:</p>" + Table Content
4. Use combined content in Create Page from Content
5. Page created with formatted table
```

### Example 3: Two-Column Table

**Scenario**: Generate a simple key-value table.

**Input**:
```
Table Headers: ["Property", "Value"]
Table Rows: [
  ["Version", "2.5.0"],
  ["Release Date", "2024-01-15"],
  ["Status", "Stable"]
]
```

**Output**:
```xml
<table>
  <tbody>
    <tr>
      <th>Property</th>
      <th>Value</th>
    </tr>
    <tr>
      <td>Version</td>
      <td>2.5.0</td>
    </tr>
    <tr>
      <td>Release Date</td>
      <td>2024-01-15</td>
    </tr>
    <tr>
      <td>Status</td>
      <td>Stable</td>
    </tr>
  </tbody>
</table>
```

**Result**: Key-value table markup generated.

## Business Rules

1. **Header Row Required**: Table must have at least one header
2. **Data Rows Required**: Table must have at least one data row
3. **Consistent Columns**: All rows should have same number of cells as headers
4. **Storage Format**: Output is in Confluence storage format
5. **No Styling**: Basic table format without custom styling

## Limitations

1. **No Cell Merging**: Cannot merge cells across columns or rows
2. **No Custom Styling**: Cannot apply colors, borders, or other styling
3. **No Nested Tables**: Cannot create tables within table cells
4. **Text Only**: Cell content is plain text (no rich formatting within cells)
5. **No Sorting**: Does not add sortable column functionality

## Best Practices

### 1. Validate Row Consistency
Ensure all rows have the same number of cells as headers.

### 2. Use Descriptive Headers
Choose clear, concise header names that describe the column content.

### 3. Keep Tables Simple
For complex tables, consider breaking into multiple simpler tables.

### 4. Combine with Other Content
Use table markup as part of larger page content.

### 5. Test with Small Tables First
Verify table generation with small examples before creating large tables.

## Common Use Cases

### 1. Team Directories
```
Scenario: Create team member directory table
Action: Generate table with names, emails, roles
Result: Formatted team directory
```

### 2. Configuration Tables
```
Scenario: Document configuration parameters
Action: Generate table with parameter names and values
Result: Configuration reference table
```

### 3. Status Reports
```
Scenario: Create status report with metrics
Action: Generate table with metrics and values
Result: Formatted status report
```

### 4. Comparison Tables
```
Scenario: Compare features across options
Action: Generate table with features and comparisons
Result: Feature comparison table
```

## Related Catalog Requests

- [Create Page from Content](pages/CreatePageFromContent.md) - Use generated table in page creation
- [Update Page by Content](pages/UpdatePageByContent.md) - Add table to existing page
- [Generate Heading Content](pages/GenerateHeadingContent.md) - Generate heading markup
- [Generate Anchor Tag](pages/GenerateAnchorTag.md) - Generate link markup

## Technical Implementation

### Helper Class
- **Class**: ContentArea
- **Package**: com.kristasoft.extensions.essentials.collaboration.confluence.catalog
- **Method**: generateContentForTable(List<String> tableHeaders, List<List<String>> tableRows)
- **Service**: ContentGenerator.generateTable()

### Processing Flow
1. Validate input parameters
2. Create table opening tag
3. Create tbody opening tag
4. Generate header row with th elements
5. Iterate through data rows
6. Generate each row with td elements
7. Close tbody and table tags
8. Return formatted markup

### Telemetry Metrics
- **confluence.generateContentForTable**: Incremented on each request
- **confluence.generateContentForTable.success**: Recorded with row count and duration
- **confluence.generateContentForTable.error**: Recorded on failures

## Error Handling

### System Errors (SYSTEM_ERROR)

**Cause**: Invalid input parameters or processing errors

**Common Scenarios**:
- Empty headers or rows
- Inconsistent row lengths
- Invalid data types

**Resolution**:
1. Verify headers list is not empty
2. Verify rows list is not empty
3. Ensure all rows have same number of cells
4. Check data types are correct

## Troubleshooting

### Issue: Malformed Table Output
**Cause**: Inconsistent row lengths

**Solution**:
1. Verify all rows have same number of cells as headers
2. Check for missing or extra cells in rows
3. Validate input data structure
4. Test with simple example first

### Issue: Table Not Rendering
**Cause**: Invalid markup in page

**Solution**:
1. Verify generated markup is valid XML
2. Check for special characters that need escaping
3. Test table markup in isolation
4. Validate complete page content

### Issue: Empty Cells
**Cause**: Null or empty values in rows

**Solution**:
1. Replace null values with empty strings
2. Provide default values for missing data
3. Validate data before generating table
4. Handle missing data appropriately

### Issue: Special Characters
**Cause**: Unescaped special characters in cell content

**Solution**:
1. HTML special characters are automatically escaped
2. Use plain text in cells
3. Avoid using < > & characters directly
4. Let the generator handle escaping

## Example Integration

### Complete Page Creation with Table

```
Step 1: Generate Table
Input:
  Table Headers: ["Feature", "Status", "Notes"]
  Table Rows: [
    ["Authentication", "Complete", "OAuth 2.0 implemented"],
    ["API Integration", "In Progress", "70% complete"],
    ["Documentation", "Planned", "Starting next sprint"]
  ]

Step 2: Combine with Page Content
Page Body:
  "<h1>Project Status</h1>" +
  "<p>Current status of project features:</p>" +
  [Table Content from Step 1] +
  "<p>Last updated: 2024-01-15</p>"

Step 3: Create Page
Use Create Page from Content with combined content

Result: Page with formatted status table
```

## See Also

- [Extension Configuration](pages/ExtensionConfiguration.md) - Setup and configuration
- [Create Page from Content](pages/CreatePageFromContent.md) - Use table in pages
- [Update Page by Content](pages/UpdatePageByContent.md) - Add table to existing pages
- [Generate Heading Content](pages/GenerateHeadingContent.md) - Generate headings
- [Generate Anchor Tag](pages/GenerateAnchorTag.md) - Generate links

