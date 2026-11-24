# Generate Heading Content

## Overview

Generates properly formatted Confluence storage format markup for headings (h1-h6), simplifying the creation of structured page content.

## Request Details

- **Area**: Content
- **Type**: QUERY_SYSTEM
- **Retry Support**: ❌ No (Simple content generation helper)

## Input Parameters

| Parameter Name | Type | Required | Description | Example |
|----------------|------|----------|-------------|---------|
| Heading Text | Text | Yes | The text content of the heading | `"Getting Started"` |
| Heading Level | Number | Yes | The heading level (1-6, where 1 is largest) | `1` |

### Parameter Details

**Heading Text**
- **Format**: Text string
- **Purpose**: The content to display in the heading
- **Can Include**: Plain text (special characters are escaped)
- **Required**: Yes

**Heading Level**
- **Format**: Integer from 1 to 6
- **Purpose**: Defines the heading size and hierarchy
- **Levels**:
  - 1 = h1 (largest, typically page title)
  - 2 = h2 (major sections)
  - 3 = h3 (subsections)
  - 4 = h4 (sub-subsections)
  - 5 = h5 (minor headings)
  - 6 = h6 (smallest headings)
- **Required**: Yes

## Output Parameters

| Parameter Name | Type | Description |
|----------------|------|-------------|
| Heading Content | Rich Text | Confluence storage format markup for the heading |

## Usage Examples

### Example 1: Generate H1 Heading

**Scenario**: Create a main page title.

**Input**:
```
Heading Text: "Getting Started Guide"
Heading Level: 1
```

**Output**:
```xml
<h1>Getting Started Guide</h1>
```

**Result**: H1 heading markup generated.

### Example 2: Generate H2 Section Heading

**Scenario**: Create a major section heading.

**Input**:
```
Heading Text: "Installation Instructions"
Heading Level: 2
```

**Output**:
```xml
<h2>Installation Instructions</h2>
```

**Result**: H2 heading markup generated.

### Example 3: Build Structured Content

**Scenario**: Create hierarchical page structure.

**Workflow**:
```
1. Generate H1: "User Guide"
2. Generate H2: "Getting Started"
3. Generate H3: "Prerequisites"
4. Generate H3: "Installation"
5. Generate H2: "Configuration"
6. Combine all headings with content between them
7. Use in Create Page from Content
```

**Result**: Well-structured page with proper heading hierarchy.

### Example 4: All Heading Levels

**Scenario**: Demonstrate all heading levels.

**Input/Output**:
```
Level 1: <h1>Main Title</h1>
Level 2: <h2>Major Section</h2>
Level 3: <h3>Subsection</h3>
Level 4: <h4>Sub-subsection</h4>
Level 5: <h5>Minor Heading</h5>
Level 6: <h6>Smallest Heading</h6>
```

## Business Rules

1. **Valid Levels Only**: Heading level must be between 1 and 6
2. **Text Required**: Heading text cannot be empty
3. **Storage Format**: Output is in Confluence storage format
4. **No Styling**: Basic heading format without custom styling
5. **Automatic Escaping**: Special characters are automatically escaped

## Limitations

1. **No Custom Styling**: Cannot apply colors, fonts, or other styling
2. **No Nested Elements**: Cannot include other HTML elements within heading
3. **Plain Text Only**: Heading content is plain text (no bold, italic, etc.)
4. **No IDs or Classes**: Cannot add custom IDs or CSS classes
5. **No Anchors**: Does not automatically create anchor links

## Best Practices

### 1. Follow Heading Hierarchy
Use headings in logical order (h1, then h2, then h3, etc.) without skipping levels.

### 2. Use H1 for Page Title
Typically use one h1 at the top of the page as the main title.

### 3. Use H2 for Major Sections
Use h2 for main sections of your content.

### 4. Keep Headings Concise
Use short, descriptive headings that clearly indicate section content.

### 5. Maintain Consistency
Use consistent heading styles throughout your documentation.

## Common Use Cases

### 1. Page Structure
```
Scenario: Create well-structured documentation page
Action: Generate headings for each section
Result: Organized, hierarchical content
```

### 2. Table of Contents
```
Scenario: Create sections that appear in TOC
Action: Generate h2 and h3 headings for major sections
Result: Automatic table of contents generation
```

### 3. Content Organization
```
Scenario: Break long content into readable sections
Action: Add headings to divide content
Result: Improved readability and navigation
```

### 4. Documentation Standards
```
Scenario: Maintain consistent documentation format
Action: Use heading generator for all section titles
Result: Standardized documentation structure
```

## Related Catalog Requests

- [Create Page from Content](pages/CreatePageFromContent.md) - Use generated headings in page creation
- [Update Page by Content](pages/UpdatePageByContent.md) - Add headings to existing page
- [Generate Content for Table](pages/GenerateContentForTable.md) - Generate table markup
- [Generate Anchor Tag](pages/GenerateAnchorTag.md) - Generate link markup

## Technical Implementation

### Helper Class
- **Class**: ContentArea
- **Package**: com.kristasoft.extensions.essentials.collaboration.confluence.catalog
- **Method**: generateHeadingContent(String headingText, Number headingLevel)
- **Service**: ContentGenerator.generateHeading()

### Processing Flow
1. Validate input parameters
2. Validate heading level is between 1 and 6
3. Escape special characters in heading text
4. Generate opening tag (e.g., `<h2>`)
5. Add heading text
6. Generate closing tag (e.g., `</h2>`)
7. Return formatted markup

### Telemetry Metrics
- **confluence.generateHeadingContent**: Incremented on each request
- **confluence.generateHeadingContent.success**: Recorded with heading level and duration
- **confluence.generateHeadingContent.error**: Recorded on failures

## Error Handling

### System Errors (SYSTEM_ERROR)

**Cause**: Invalid input parameters

**Common Scenarios**:
- Empty heading text
- Invalid heading level (not 1-6)
- Null parameters

**Resolution**:
1. Verify heading text is not empty
2. Verify heading level is between 1 and 6
3. Check data types are correct
4. Provide valid input values

## Troubleshooting

### Issue: Invalid Heading Level
**Cause**: Heading level outside 1-6 range

**Solution**:
1. Use only levels 1 through 6
2. Level 1 is largest, level 6 is smallest
3. Verify input is a number
4. Check for typos in level value

### Issue: Heading Not Rendering
**Cause**: Invalid markup in page

**Solution**:
1. Verify generated markup is valid XML
2. Check for unclosed tags
3. Test heading markup in isolation
4. Validate complete page content

### Issue: Special Characters Display Incorrectly
**Cause**: Characters not properly escaped

**Solution**:
1. Special characters are automatically escaped
2. Use plain text in headings
3. Avoid using HTML entities directly
4. Let the generator handle escaping

### Issue: Heading Hierarchy Problems
**Cause**: Skipping heading levels

**Solution**:
1. Don't skip levels (e.g., h1 to h3)
2. Follow logical hierarchy (h1 → h2 → h3)
3. Use h2 for main sections after h1
4. Use h3 for subsections under h2

## Example Integration

### Complete Page Creation with Headings

```
Step 1: Generate Headings
H1: Generate Heading Content("User Guide", 1)
H2: Generate Heading Content("Getting Started", 2)
H3: Generate Heading Content("Prerequisites", 3)
H3: Generate Heading Content("Installation", 3)
H2: Generate Heading Content("Configuration", 2)

Step 2: Combine with Content
Page Body:
  [H1] +
  "<p>Welcome to the user guide.</p>" +
  [H2] +
  "<p>This section covers getting started.</p>" +
  [H3] +
  "<p>Before you begin, ensure you have:</p>" +
  "<ul><li>Account access</li><li>Required permissions</li></ul>" +
  [H3] +
  "<p>Follow these steps to install...</p>" +
  [H2] +
  "<p>Configuration options include...</p>"

Step 3: Create Page
Use Create Page from Content with combined content

Result: Well-structured page with proper heading hierarchy
```

## Heading Level Guidelines

### When to Use Each Level

**H1 (Level 1)**
- Page title
- One per page
- Largest and most prominent

**H2 (Level 2)**
- Major sections
- Main topics
- Appears in table of contents

**H3 (Level 3)**
- Subsections under H2
- Secondary topics
- Appears in table of contents

**H4 (Level 4)**
- Sub-subsections
- Detailed topics
- May appear in table of contents

**H5 (Level 5)**
- Minor headings
- Rarely used
- Typically not in table of contents

**H6 (Level 6)**
- Smallest headings
- Very rarely used
- Typically not in table of contents

## See Also

- [Extension Configuration](pages/ExtensionConfiguration.md) - Setup and configuration
- [Create Page from Content](pages/CreatePageFromContent.md) - Use headings in pages
- [Update Page by Content](pages/UpdatePageByContent.md) - Add headings to existing pages
- [Generate Content for Table](pages/GenerateContentForTable.md) - Generate tables
- [Generate Anchor Tag](pages/GenerateAnchorTag.md) - Generate links

