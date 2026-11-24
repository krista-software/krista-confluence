# Current Confluence Extension API Usage & Folders Support

## 📊 Current API Version Usage

The Confluence extension currently uses a **HYBRID approach** with both V1 and V2 APIs:

### ✅ **V2 API** (Modern - Recommended)
Used for:
- ✅ **Spaces**: `GET /wiki/api/v2/spaces`
- ✅ **Space by ID**: `GET /wiki/api/v2/spaces/{id}`
- ✅ **Pages by ID**: `GET /wiki/api/v2/pages/{id}`
- ✅ **Create Page**: `POST /wiki/api/v2/pages`
- ✅ **Update Page**: `PUT /wiki/api/v2/pages/{id}`
- ✅ **Pages in Space**: `GET /wiki/api/v2/spaces/{spaceId}/pages`

### ⚠️ **V1 API** (Legacy - Being Deprecated)
Still used for:
- ⚠️ **Templates**: `GET /wiki/rest/api/template/{id}`
- ⚠️ **Templates by Space**: `GET /wiki/rest/api/template/page?spaceKey={key}`
- ⚠️ **Pages by Space**: `GET /wiki/rest/api/content?spaceKey={key}&type=page`
- ⚠️ **Child Pages**: `GET /wiki/rest/api/content/{id}/child/page`
- ⚠️ **Page Ancestors**: `GET /wiki/rest/api/content/{id}?expand=ancestors`
- ⚠️ **Check Children**: `GET /wiki/rest/api/content/{id}/child/page?limit=1`

---

## 🎯 Folders API Support

### **Good News**: Folders are ONLY available in V2 API ✅

Since the extension already uses V2 for most page operations, adding folder support will be straightforward!

### **Folder Endpoints** (V2 Only)

```
Base URL: https://{domain}/wiki/api/v2/
```

| Endpoint | Method | Description | Status |
|----------|--------|-------------|--------|
| `/folders/{id}` | GET | Get folder by ID | ✅ Available |
| `/spaces/{spaceId}/folders` | GET | Get folders in space | ✅ Available |
| `/folders/{id}/children` | GET | Get direct children | ✅ Available |
| `/folders/{id}/descendants` | GET | Get all descendants | ✅ Available |
| `/folders` | POST | Create folder | ❌ Not needed (KE-2725) |
| `/folders/{id}` | PUT | Update folder | ❌ Not needed (KE-2725) |
| `/folders/{id}` | DELETE | Delete folder | ❌ Not needed (KE-2725) |

---

## 🔧 Implementation Strategy

### **Option 1: Pure V2 Approach** (Recommended)
Use V2 API for all folder operations since:
- ✅ Extension already uses V2 for pages
- ✅ Folders ONLY exist in V2
- ✅ Consistent with modern API usage
- ✅ Future-proof (V1 is being deprecated)

### **Option 2: Hybrid Approach** (Current Pattern)
Continue using V1 where needed, V2 for folders:
- ⚠️ Maintains current architecture
- ⚠️ But increases technical debt
- ⚠️ V1 APIs are being deprecated

**Recommendation**: Use **Option 1** - Pure V2 for all folder operations

---

## 📝 Code Structure

### Current API Version Enum

<augment_code_snippet path="confluence/src/main/java/com/kristasoft/extensions/essentials/collaboration/confluence/api/APIVersion.java" mode="EXCERPT">
````java
public enum APIVersion {
    V1("wiki/rest/api/"),
    V2("wiki/api/v2/");
    
    private final String endpoint;
    // ...
}
````
</augment_code_snippet>

### Current REST Client Usage

<augment_code_snippet path="confluence/src/main/java/com/kristasoft/extensions/essentials/collaboration/confluence/service/ConfluenceService.java" mode="EXCERPT">
````java
// V2 API - Spaces
HttpResponse<String> response = confluenceAttributes.getRestClient()
    .doSyncGet(APIVersion.V2, null, "spaces");

// V2 API - Pages
HttpResponse<String> response = confluenceAttributes.getRestClient()
    .doSyncGet(APIVersion.V2, Map.of("body-format", "storage"), "pages", id);

// V1 API - Templates (legacy)
HttpResponse<String> response = confluenceAttributes.getRestClient()
    .doSyncGet(APIVersion.V1, null, "template", templateId);
````
</augment_code_snippet>

---

## 🚀 Folder Implementation Plan

### Phase 1: Add Folder Service Methods

Add to `ConfluenceService.java`:

```java
// Get folder by ID
public Folder getFolderById(String folderId) {
    HttpResponse<String> response = confluenceAttributes.getRestClient()
        .doSyncGet(APIVersion.V2, null, "folders", folderId);
    if (response.statusCode() == HttpURLConnection.HTTP_OK) {
        return GsonFactory.getInstance().fromJson(response.body(), Folder.class);
    } else {
        throw new ConfluenceRESTException("Folder not found - " + folderId);
    }
}

// Get folders in space
public Folder[] getFoldersBySpaceId(String spaceId) {
    HttpResponse<String> response = confluenceAttributes.getRestClient()
        .doSyncGet(APIVersion.V2, null, "spaces", spaceId, "folders");
    if (response.statusCode() == HttpURLConnection.HTTP_OK) {
        JsonObject resultObject = GsonFactory.getInstance()
            .fromJson(response.body(), JsonObject.class);
        if (resultObject.has("results")) {
            return GsonFactory.getInstance().fromJson(
                resultObject.get("results").toString(),
                TypeToken.getArray(Folder.class).getType()
            );
        }
        return new Folder[0];
    } else {
        throw new ConfluenceRESTException("Failed to retrieve folders");
    }
}

// Get folder descendants
public Object[] getFolderDescendants(String folderId) {
    HttpResponse<String> response = confluenceAttributes.getRestClient()
        .doSyncGet(APIVersion.V2, null, "folders", folderId, "descendants");
    // Parse mixed content types (pages, folders, whiteboards, etc.)
    // ...
}

// Get folder children
public Object[] getFolderChildren(String folderId) {
    HttpResponse<String> response = confluenceAttributes.getRestClient()
        .doSyncGet(APIVersion.V2, null, "folders", folderId, "children");
    // Parse mixed content types
    // ...
}
```

---

## 🔍 Key Differences: V1 vs V2 for Hierarchy

### **V1 API** (Current - Legacy)
```
GET /wiki/rest/api/content/{id}/child/page
GET /wiki/rest/api/content/{id}?expand=ancestors
```

**Issues**:
- ❌ Does NOT return folders
- ❌ Only returns pages
- ❌ Being deprecated

### **V2 API** (Modern - Folders Supported)
```
GET /wiki/api/v2/pages/{id}?include-ancestors=true
GET /wiki/api/v2/folders/{id}/children
GET /wiki/api/v2/folders/{id}/descendants
```

**Benefits**:
- ✅ Returns folders in hierarchy
- ✅ Returns all content types (pages, folders, whiteboards, databases)
- ✅ Future-proof
- ✅ Better pagination support

---

## ⚠️ Migration Considerations

### **Methods That Need Updates for Folder Support**

1. **`getAncestorsOfPage()`** - Currently uses V1
   - **Current**: `GET /wiki/rest/api/content/{id}?expand=ancestors`
   - **Should use**: `GET /wiki/api/v2/pages/{id}?include-ancestors=true`
   - **Why**: V2 includes folders in ancestor chain

2. **`getDirectChildPagesOfPage()`** - Currently uses V1
   - **Current**: `GET /wiki/rest/api/content/{id}/child/page`
   - **Should use**: `GET /wiki/api/v2/pages/{id}/children`
   - **Why**: V2 returns folders as children

3. **`hasChildPages()`** - Currently uses V1
   - **Current**: `GET /wiki/rest/api/content/{id}/child/page?limit=1`
   - **Should use**: `GET /wiki/api/v2/pages/{id}/children?limit=1`
   - **Why**: Need to check for folders too

---

## 📋 Action Items for Folder Support

### **Immediate (Required for KE-2725)**

1. ✅ Create `Folder.java` DTO (similar to `Page.java` but NO body field)
2. ✅ Create `Folder` catalog entity
3. ✅ Add folder methods to `ConfluenceService.java` (using V2 API)
4. ✅ Create `FolderArea.java` catalog area
5. ✅ Create `FolderIdValidator.java`
6. ✅ Update `ParserUtils.java` for folder entities

### **Optional (Future Enhancement)**

1. ⚠️ Migrate `getAncestorsOfPage()` from V1 to V2
2. ⚠️ Migrate `getDirectChildPagesOfPage()` from V1 to V2
3. ⚠️ Migrate `hasChildPages()` from V1 to V2
4. ⚠️ Migrate template operations to V2 (when available)

---

## 🎯 Folder-Specific Considerations

### **1. Folders Have NO Body Content**

```java
// Page DTO
public class Page {
    private String id;
    private String title;
    private Body body;  // ✅ Has body content
    // ...
}

// Folder DTO
public class Folder {
    private String id;
    private String title;
    // ❌ NO body field!
    // ...
}
```

### **2. Mixed Content Types in Responses**

When getting folder children/descendants, you'll receive:
- Pages
- Folders
- Whiteboards
- Databases
- Smart links

**Solution**: Check `type` field to discriminate:

```java
JsonObject item = ...;
String type = item.get("type").getAsString();

switch (type) {
    case "page":
        Page page = gson.fromJson(item, Page.class);
        break;
    case "folder":
        Folder folder = gson.fromJson(item, Folder.class);
        break;
    case "whiteboard":
        // Handle whiteboard
        break;
    // ...
}
```

### **3. Hierarchy Navigation**

Folders can be parents of pages:

```json
{
  "id": "456789",
  "type": "page",
  "title": "My Page",
  "parentId": "789012",
  "parentType": "folder"  // ← Parent is a folder!
}
```

---

## 📚 API Documentation Links

### **Official Atlassian Documentation**

- **Folders API**: https://developer.atlassian.com/cloud/confluence/rest/v2/api-group-folder/
- **Children API**: https://developer.atlassian.com/cloud/confluence/rest/v2/api-group-children/
- **Descendants API**: https://developer.atlassian.com/cloud/confluence/rest/v2/api-group-descendants/
- **V2 API Overview**: https://developer.atlassian.com/cloud/confluence/rest/v2/intro/

### **Community Resources**

- **RFC-52**: https://community.developer.atlassian.com/t/rfc-52-folders-as-a-new-confluence-content-type/80001

---

## ✅ Summary

| Aspect | Current State | For Folders |
|--------|---------------|-------------|
| **API Version** | Hybrid (V1 + V2) | V2 Only ✅ |
| **REST Client** | ✅ Already supports V2 | ✅ Ready to use |
| **Page Operations** | ✅ Mostly V2 | ✅ Compatible |
| **Template Operations** | ⚠️ Still V1 | N/A (folders have no templates) |
| **Hierarchy Navigation** | ⚠️ Mostly V1 | Need V2 for folder support |
| **Implementation Effort** | - | Low (V2 already in use) |

---

## 🚀 Next Steps

1. ✅ **Postman Collection Created** - Test folder APIs
2. ⏭️ **Create Folder DTOs** - Data models for folders
3. ⏭️ **Add Service Methods** - Folder operations in ConfluenceService
4. ⏭️ **Create Catalog Area** - FolderArea with catalog requests
5. ⏭️ **Add Validation** - FolderIdValidator
6. ⏭️ **Update Utilities** - ParserUtils for folder entities
7. ⏭️ **Testing** - Unit and integration tests

---

**Created**: 2024-11-25  
**Last Updated**: 2024-11-25  
**Jira Ticket**: KE-2725

