# Generate Anchor Tag

## Overview

Generates properly formatted Confluence storage format markup for hyperlinks (anchor tags), simplifying the creation of links to internal pages or external URLs.

## Request Details

- **Area**: Content
- **Type**: QUERY_SYSTEM
- **Retry Support**: ❌ No (Simple content generation helper)

## Input Parameters

| Parameter Name | Type | Required | Description | Example |
|----------------|------|----------|-------------|---------|
| Anchor Text | Text | Yes | The visible text of the link | `"Click here for more information"` |
| Anchor URL | Text | Yes | The URL or page reference for the link | `"https://example.com"` or `"/wiki/spaces/ENG/pages/123456"` |

### Parameter Details

**Anchor Text**
- **Format**: Text string
- **Purpose**: The visible, clickable text displayed to users
- **Can Include**: Plain text (special characters are escaped)
- **Best Practice**: Use descriptive text that indicates link destination
- **Required**: Yes

**Anchor URL**
- **Format**: Text string (URL or Confluence page path)
- **Purpose**: The destination of the link
- **Types**:
  - External URLs: `https://example.com`
  - Internal Confluence pages: `/wiki/spaces/SPACE/pages/123456`
  - Relative paths: `../other-page`
- **Required**: Yes

## Output Parameters

| Parameter Name | Type | Description |
|----------------|------|-------------|
| Anchor Content | Rich Text | Confluence storage format markup for the hyperlink |

## Usage Examples

### Example 1: External Link

**Scenario**: Create a link to an external website.

**Input**:
```
Anchor Text: "Visit our website"
Anchor URL: "https://www.example.com"
```

**Output**:
```xml
<a href="https://www.example.com">Visit our website</a>
```

**Result**: External link markup generated.

### Example 2: Internal Confluence Page Link

**Scenario**: Link to another page in Confluence.

**Input**:
```
Anchor Text: "See the Installation Guide"
Anchor URL: "/wiki/spaces/ENG/pages/987654"
```

**Output**:
```xml
<a href="/wiki/spaces/ENG/pages/987654">See the Installation Guide</a>
```

**Result**: Internal page link markup generated.

### Example 3: Link in Paragraph

**Scenario**: Create content with embedded link.

**Workflow**:
```
1. Generate anchor: "documentation", "https://docs.example.com"
2. Combine with text:
   "<p>For more details, see our " + [Anchor Content] + ".</p>"
3. Result:
   "<p>For more details, see our <a href='https://docs.example.com'>documentation</a>.</p>"
4. Use in Create Page from Content
```

**Result**: Paragraph with embedded link.

### Example 4: Multiple Links

**Scenario**: Create content with multiple links.

**Workflow**:
```
1. Generate link 1: "Getting Started", "/wiki/spaces/ENG/pages/111"
2. Generate link 2: "API Reference", "/wiki/spaces/ENG/pages/222"
3. Generate link 3: "Support", "https://support.example.com"
4. Combine:
   "<p>Quick links: " + [Link 1] + " | " + [Link 2] + " | " + [Link 3] + "</p>"
```

**Result**: Multiple links in one paragraph.

## Business Rules

1. **Text Required**: Anchor text cannot be empty
2. **URL Required**: Anchor URL cannot be empty
3. **Storage Format**: Output is in Confluence storage format
4. **No Styling**: Basic link format without custom styling
5. **Automatic Escaping**: Special characters are automatically escaped
6. **URL Validation**: URLs should be valid format

## Limitations

1. **No Custom Styling**: Cannot apply colors, fonts, or other styling
2. **No Target Attribute**: Cannot specify opening in new window/tab
3. **No Title Attribute**: Cannot add tooltip text
4. **Plain Text Only**: Anchor text is plain text (no bold, italic, etc.)
5. **No Nested Elements**: Cannot include other HTML elements within link

## Best Practices

### 1. Use Descriptive Link Text
Use clear, descriptive text that indicates where the link goes.

**✅ Good:**
- "View the Installation Guide"
- "Download the latest release"
- "Contact support"

**❌ Avoid:**
- "Click here"
- "Link"
- "Read more"

### 2. Validate URLs
Ensure URLs are correct and accessible before creating links.

### 3. Use Relative Paths for Internal Links
For Confluence pages, use relative paths when possible.

### 4. Keep Link Text Concise
Use short, clear link text that fits naturally in sentences.

### 5. Test Links
Verify links work correctly after page creation.

## Common Use Cases

### 1. External References
```
Scenario: Link to external documentation
Action: Generate link to external URL
Result: Users can access external resources
```

### 2. Cross-References
```
Scenario: Link to related Confluence pages
Action: Generate links to internal pages
Result: Connected documentation
```

### 3. Resource Links
```
Scenario: Provide links to downloads or tools
Action: Generate links to resources
Result: Easy access to resources
```

### 4. Navigation Links
```
Scenario: Create "See also" section
Action: Generate multiple related links
Result: Improved navigation
```

## Related Catalog Requests

- [Create Page from Content](pages/CreatePageFromContent.md) - Use generated links in page creation
- [Update Page by Content](pages/UpdatePageByContent.md) - Add links to existing page
- [Generate Content for Table](pages/GenerateContentForTable.md) - Generate table markup
- [Generate Heading Content](pages/GenerateHeadingContent.md) - Generate heading markup

## Technical Implementation

### Helper Class
- **Class**: ContentArea
- **Package**: com.kristasoft.extensions.essentials.collaboration.confluence.catalog
- **Method**: generateAnchorTag(String anchorText, String anchorURL)
- **Service**: ContentGenerator.generateAnchor()

### Processing Flow
1. Validate input parameters
2. Escape special characters in anchor text
3. Validate URL format
4. Generate opening anchor tag with href attribute
5. Add anchor text
6. Generate closing anchor tag
7. Return formatted markup

### Telemetry Metrics
- **confluence.generateAnchorTag**: Incremented on each request
- **confluence.generateAnchorTag.success**: Recorded with duration
- **confluence.generateAnchorTag.error**: Recorded on failures

## Error Handling

### System Errors (SYSTEM_ERROR)

**Cause**: Invalid input parameters

**Common Scenarios**:
- Empty anchor text
- Empty anchor URL
- Null parameters

**Resolution**:
1. Verify anchor text is not empty
2. Verify anchor URL is not empty
3. Check data types are correct
4. Provide valid input values

## Troubleshooting

### Issue: Link Not Clickable
**Cause**: Invalid markup in page

**Solution**:
1. Verify generated markup is valid XML
2. Check for unclosed tags
3. Test link markup in isolation
4. Validate complete page content

### Issue: Link Goes to Wrong Page
**Cause**: Incorrect URL

**Solution**:
1. Verify URL is correct
2. Check for typos in URL
3. Test URL in browser first
4. Use Get Page By ID to get correct page URL

### Issue: Special Characters in URL
**Cause**: URL contains unescaped special characters

**Solution**:
1. Special characters are automatically escaped
2. Use proper URL encoding
3. Avoid spaces in URLs
4. Let the generator handle escaping

### Issue: Broken Internal Links
**Cause**: Page ID or path is incorrect

**Solution**:
1. Verify page exists using Get Page By ID
2. Check page URL format
3. Use full page URL from Get Page By ID response
4. Test link after page creation

## Example Integration

### Complete Page Creation with Links

```
Step 1: Generate Links
Link 1: Generate Anchor Tag("Getting Started Guide", "/wiki/spaces/ENG/pages/987654")
Link 2: Generate Anchor Tag("API Documentation", "/wiki/spaces/ENG/pages/987655")
Link 3: Generate Anchor Tag("Support Portal", "https://support.example.com")

Step 2: Combine with Content
Page Body:
  "<h1>Welcome</h1>" +
  "<p>Welcome to our documentation. Here are some helpful resources:</p>" +
  "<ul>" +
  "<li>" + [Link 1] + " - Learn the basics</li>" +
  "<li>" + [Link 2] + " - Technical reference</li>" +
  "<li>" + [Link 3] + " - Get help</li>" +
  "</ul>" +
  "<p>For more information, see our " + [Link 1] + ".</p>"

Step 3: Create Page
Use Create Page from Content with combined content

Result: Page with multiple working links
```

## Link Types

### External Links
```
Format: https://example.com
Use: Linking to external websites
Example: "https://www.atlassian.com"
```

### Internal Confluence Links
```
Format: /wiki/spaces/SPACEKEY/pages/PAGEID
Use: Linking to other Confluence pages
Example: "/wiki/spaces/ENG/pages/987654"
```

### Relative Links
```
Format: ../page-name or ./page-name
Use: Linking to nearby pages
Example: "../installation-guide"
```

### Anchor Links
```
Format: #section-name
Use: Linking to sections within same page
Example: "#prerequisites"
```

## See Also

- [Extension Configuration](pages/ExtensionConfiguration.md) - Setup and configuration
- [Create Page from Content](pages/CreatePageFromContent.md) - Use links in pages
- [Update Page by Content](pages/UpdatePageByContent.md) - Add links to existing pages
- [Get Page By ID](pages/GetPageById.md) - Get page URLs for internal links
- [Generate Content for Table](pages/GenerateContentForTable.md) - Generate tables
- [Generate Heading Content](pages/GenerateHeadingContent.md) - Generate headings

