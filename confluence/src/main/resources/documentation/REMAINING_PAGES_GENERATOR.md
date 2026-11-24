# Remaining Documentation Pages Generator Guide

## Overview

This document provides templates and guidance for creating the remaining 14 catalog request documentation pages for the Confluence Extension.

## Remaining Pages Checklist

### Page Management (10 pages)
- [ ] CreatePageFromTemplate.md
- [ ] GetPageById.md
- [ ] GetPagesInSpace.md
- [ ] UpdatePageByContent.md
- [ ] UpdatePageByTemplate.md
- [ ] GetDirectChildPagesOfPage.md
- [ ] CheckIfPageHasChildren.md
- [ ] GetAllNestedDescendantsOfPage.md
- [ ] GetAncestorsOfPage.md
- [ ] DownloadPage.md

### Content Generation (3 pages)
- [ ] GenerateContentForTable.md
- [ ] GenerateHeadingContent.md
- [ ] GenerateAnchorTag.md

### Reference (1 page)
- [ ] Entities.md

## Source Code References

### Page Operations
**File**: `confluence/src/main/java/com/kristasoft/extensions/essentials/collaboration/confluence/catalog/PageArea.java`

**Methods and Line Numbers**:
- createPageFromTemplate() - lines 150-241
- getPageById() - lines 243-327
- getPagesInSpace() - lines 329-427
- updatePageByContent() - lines 429-520
- updatePageFromTemplate() - lines 522-619
- getPageInSpace() (Download) - lines 621-714
- getDirectChildPagesOfPage() - lines 716-764
- checkIfPageHasChildren() - lines 766-806
- getAllNestedDescendantsOfPage() - lines 808-858
- getAncestorsOfPage() - lines 860-910

### Content Operations
**File**: `confluence/src/main/java/com/kristasoft/extensions/essentials/collaboration/confluence/catalog/ContentArea.java`

**Methods and Line Numbers**:
- generateContentForTable() - lines 37-69
- generateHeadingContent() - lines 71-98
- generateAnchorTag() - lines 102-127

## Quick Reference: Common Patterns

### Request Details Section
```markdown
## Request Details

- **Area**: [Page|Content]
- **Type**: [QUERY_SYSTEM|CHANGE_SYSTEM]
- **Retry Support**: [✅ Yes|❌ No] (explanation)
```

### Input Parameters Table
```markdown
| Parameter Name | Type | Required | Description | Example |
|----------------|------|----------|-------------|---------|
| [Name] | [Type] | [Yes/No] | [Description] | `"example"` |
```

### Validation Rules Table
```markdown
| Validation | Error Message | Resolution |
|------------|---------------|------------|
| [Rule] | "[Message]" | [Solution] |
```

### Error Handling Structure
```markdown
### Input Errors (INPUT_ERROR)
**Cause**: [Description]
**Common Scenarios**: [List]
**Error Messages**: [List]
**Resolution**: [Steps]

### Logic Errors (LOGIC_ERROR)
**Cause**: [Description]
**Resolution**: [Steps]

### System Errors (SYSTEM_ERROR)
**Cause**: [Description]
**Resolution**: [Steps]
```

### Technical Implementation
```markdown
### Helper Class
- **Class**: [ClassName]
- **Package**: com.kristasoft.extensions.essentials.collaboration.confluence.catalog
- **Method**: [methodName]
- **Service**: [ServiceMethod]

### Telemetry Metrics
- **confluence.[requestName]**: Incremented on each request
- **confluence.[requestName].success**: Recorded with details and duration
- **confluence.[requestName].error**: Recorded on failures
- **confluence.[requestName].retryPrompted**: Recorded when validation errors trigger retry (if applicable)
```

## Page-Specific Notes

### CreatePageFromTemplate.md
- Similar to CreatePageFromContent but uses Template ID instead of Page Body
- Input: Space ID, Page Title, Template ID, Parent Page ID (optional)
- Emphasize template selection and validation

### GetPageById.md
- Simple query operation
- Input: Page ID
- Output: Complete Page entity
- Include retry support for validation

### GetPagesInSpace.md
- Query operation with search capability
- Input: Space ID, Search Query (optional)
- Output: List of pages matching criteria
- Discuss search syntax and filtering

### UpdatePageByContent.md
- Change operation requiring Page ID and new content
- Input: Page ID, Page Title, Page Body, Version Number
- Emphasize version number requirement for conflict prevention

### UpdatePageByTemplate.md
- Similar to UpdatePageByContent but uses Template ID
- Input: Page ID, Page Title, Template ID, Version Number

### GetDirectChildPagesOfPage.md
- Similar to GetDirectChildPagesOfSpace but for a specific page
- Input: Page ID
- Output: List of direct child pages

### CheckIfPageHasChildren.md
- Simple boolean check
- Input: Page ID
- Output: Boolean (Has Children)
- Quick operation for hierarchy checks

### GetAllNestedDescendantsOfPage.md
- Recursive operation to get entire page tree
- Input: Page ID
- Output: List of all descendant pages (all levels)
- Discuss performance considerations

### GetAncestorsOfPage.md
- Get parent hierarchy of a page
- Input: Page ID
- Output: List of ancestor pages from root to immediate parent
- Useful for breadcrumb navigation

### DownloadPage.md
- Export page content
- Input: Space ID, Page Title
- Output: Page content with metadata
- Discuss export formats and use cases

### GenerateContentForTable.md
- Helper for creating table markup
- Input: Table Headers, Table Rows
- Output: Confluence storage format for table
- Include table formatting examples

### GenerateHeadingContent.md
- Helper for creating heading markup
- Input: Heading Text, Heading Level
- Output: Confluence storage format for heading
- Discuss heading levels (h1-h6)

### GenerateAnchorTag.md
- Helper for creating anchor links
- Input: Anchor Text, Anchor URL
- Output: Confluence storage format for link
- Include internal and external link examples

### Entities.md
- Reference documentation for all entity structures
- Include: Space, Page, Template, Content entities
- Provide complete field tables with types and descriptions
- Include examples of each entity type

## Template Structure Reminder

Every catalog request page MUST include these 15 sections:

1. Overview
2. Request Details
3. Input Parameters
4. Output Parameters
5. Validation Rules (if retry supported)
6. Error Handling
7. Usage Examples (2-3 examples)
8. Business Rules
9. Limitations
10. Best Practices
11. Common Use Cases
12. Related Catalog Requests
13. Technical Implementation
14. Troubleshooting
15. See Also

## Next Steps

1. Review source code for each catalog request
2. Extract exact validation logic and error messages
3. Create each page following the template
4. Cross-reference related catalog requests
5. Verify all links work correctly
6. Test examples for accuracy

## Completion Checklist

After creating all pages:
- [ ] All 14 pages created in pages/ directory
- [ ] All pages follow 15-section structure
- [ ] All error messages match source code
- [ ] All validation rules match source code
- [ ] All telemetry metrics match source code
- [ ] All cross-references are correct
- [ ] All examples are realistic and helpful
- [ ] Images renamed according to pattern
- [ ] Old documentation files removed
- [ ] _sidebar.md links verified
- [ ] README.md links verified

