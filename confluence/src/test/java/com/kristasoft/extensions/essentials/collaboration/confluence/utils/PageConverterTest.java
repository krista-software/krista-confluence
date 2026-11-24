/*
 * Confluence Extension for Krista
 * Copyright (C) 2025 Krista Software
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.kristasoft.extensions.essentials.collaboration.confluence.utils;

import com.kristasoft.extensions.essentials.collaboration.confluence.ConfluenceAttributes;
import com.kristasoft.extensions.essentials.collaboration.confluence.api.APIVersion;
import com.kristasoft.extensions.essentials.collaboration.confluence.client.ConfluenceRESTClient;
import com.kristasoft.extensions.essentials.collaboration.confluence.dto.Page;
import com.kristasoft.extensions.essentials.collaboration.confluence.dto.v1.V1Page;
import com.kristasoft.extensions.essentials.collaboration.confluence.dto.v2.V2Ancestor;
import com.kristasoft.extensions.essentials.collaboration.confluence.dto.v2.V2ChildPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.HttpURLConnection;
import java.net.http.HttpResponse;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PageConverter Tests")
class PageConverterTest {

    @Mock
    private ConfluenceAttributes confluenceAttributes;

    @Mock
    private ConfluenceRESTClient restClient;

    private PageConverter pageConverter;
    private static final String DOMAIN_URL = "https://test.atlassian.net/wiki";

    @BeforeEach
    void setUp() {
        pageConverter = new PageConverter(confluenceAttributes, DOMAIN_URL);
    }

    // ──────────────────────────────────────────────────────────────
    // convertV2ChildPageToPage()
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("convertV2ChildPageToPage")
    class ConvertV2ChildPageToPageTests {

        @Test
        @DisplayName("Should convert V2ChildPage with all fields populated")
        void shouldConvertWithAllFieldsPopulated() {
            V2ChildPage v2ChildPage = new V2ChildPage();
            v2ChildPage.setId("12345");
            v2ChildPage.setTitle("Test Child Page");
            v2ChildPage.setStatus("current");
            v2ChildPage.setType("page");
            v2ChildPage.setSpaceId("SPACE-001");

            Page result = pageConverter.convertV2ChildPageToPage(v2ChildPage, "parent-001", "parent-space");

            assertEquals("12345", result.getId());
            assertEquals("Test Child Page", result.getTitle());
            assertEquals("current", result.getStatus());
            assertEquals("page", result.getType());
            assertEquals("parent-001", result.getParentId());
            assertEquals("page", result.getParentType());
            assertEquals("SPACE-001", result.getSpaceId());
        }

        @Test
        @DisplayName("Should use parent space ID when V2ChildPage spaceId is null")
        void shouldUseParentSpaceIdWhenChildSpaceIdIsNull() {
            V2ChildPage v2ChildPage = new V2ChildPage();
            v2ChildPage.setId("12345");
            v2ChildPage.setTitle("Title");
            v2ChildPage.setSpaceId(null);

            Page result = pageConverter.convertV2ChildPageToPage(v2ChildPage, "parent-001", "fallback-space");

            assertEquals("fallback-space", result.getSpaceId());
        }

        @Test
        @DisplayName("Should use parent space ID when V2ChildPage spaceId is empty")
        void shouldUseParentSpaceIdWhenChildSpaceIdIsEmpty() {
            V2ChildPage v2ChildPage = new V2ChildPage();
            v2ChildPage.setId("12345");
            v2ChildPage.setTitle("Title");
            v2ChildPage.setSpaceId("");

            Page result = pageConverter.convertV2ChildPageToPage(v2ChildPage, "parent-001", "fallback-space");

            assertEquals("fallback-space", result.getSpaceId());
        }

        @Test
        @DisplayName("Should default to empty string when id is null")
        void shouldDefaultToEmptyStringWhenIdIsNull() {
            V2ChildPage v2ChildPage = new V2ChildPage();
            v2ChildPage.setId(null);
            v2ChildPage.setTitle("Title");

            Page result = pageConverter.convertV2ChildPageToPage(v2ChildPage, "parent", "space");

            assertEquals("", result.getId());
        }

        @Test
        @DisplayName("Should default to empty string when title is null")
        void shouldDefaultToEmptyStringWhenTitleIsNull() {
            V2ChildPage v2ChildPage = new V2ChildPage();
            v2ChildPage.setId("123");
            v2ChildPage.setTitle(null);

            Page result = pageConverter.convertV2ChildPageToPage(v2ChildPage, "parent", "space");

            assertEquals("", result.getTitle());
        }

        @Test
        @DisplayName("Should default to empty string when status is null")
        void shouldDefaultToEmptyStringWhenStatusIsNull() {
            V2ChildPage v2ChildPage = new V2ChildPage();
            v2ChildPage.setId("123");
            v2ChildPage.setTitle("Title");
            v2ChildPage.setStatus(null);

            Page result = pageConverter.convertV2ChildPageToPage(v2ChildPage, "parent", "space");

            assertEquals("", result.getStatus());
        }

        @Test
        @DisplayName("Should default type to 'page' when type is null")
        void shouldDefaultTypeToPageWhenTypeIsNull() {
            V2ChildPage v2ChildPage = new V2ChildPage();
            v2ChildPage.setId("123");
            v2ChildPage.setTitle("Title");
            v2ChildPage.setType(null);

            Page result = pageConverter.convertV2ChildPageToPage(v2ChildPage, "parent", "space");

            assertEquals("page", result.getType());
        }

        @Test
        @DisplayName("Should handle folder content type")
        void shouldHandleFolderContentType() {
            V2ChildPage v2ChildPage = new V2ChildPage();
            v2ChildPage.setId("folder-1");
            v2ChildPage.setTitle("My Folder");
            v2ChildPage.setType("folder");
            v2ChildPage.setStatus("current");
            v2ChildPage.setSpaceId("SPACE-001");

            Page result = pageConverter.convertV2ChildPageToPage(v2ChildPage, "parent", "space");

            assertEquals("folder", result.getType());
        }

        @Test
        @DisplayName("Should set empty links map")
        void shouldSetEmptyLinksMap() {
            V2ChildPage v2ChildPage = new V2ChildPage();
            v2ChildPage.setId("123");
            v2ChildPage.setTitle("Title");

            Page result = pageConverter.convertV2ChildPageToPage(v2ChildPage, "parent", "space");

            assertNotNull(result.getLinks());
            assertTrue(result.getLinks().isEmpty(),
                    "Links should be empty for V2 child page conversion");
        }
    }

    // ──────────────────────────────────────────────────────────────
    // convertV1PageToPage()
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("convertV1PageToPage")
    class ConvertV1PageToPageTests {

        @Test
        @DisplayName("Should convert V1Page with all fields populated")
        void shouldConvertWithAllFieldsPopulated() {
            V1Page v1Page = new V1Page();
            v1Page.setId("67890");
            v1Page.setTitle("V1 Test Page");
            v1Page.setStatus("current");

            V1Page.V1Expandable expandable = new V1Page.V1Expandable();
            expandable.setSpace("/rest/api/space/SSP");
            v1Page.setExpandable(expandable);

            V1Page.V1PageLinks links = new V1Page.V1PageLinks();
            links.setWebui("/display/SSP/V1+Test+Page");
            links.setSelf("https://test.atlassian.net/wiki/rest/api/content/67890");
            links.setTinyui("/x/abc123");
            links.setEditui("/pages/editpage.action?pageId=67890");
            v1Page.setLinks(links);

            Page result = pageConverter.convertV1PageToPage(v1Page, "parent-v1");

            assertEquals("67890", result.getId());
            assertEquals("V1 Test Page", result.getTitle());
            assertEquals("current", result.getStatus());
            assertEquals("parent-v1", result.getParentId());
            assertEquals("page", result.getParentType());
            assertEquals("SSP", result.getSpaceId());
        }

        @Test
        @DisplayName("Should extract space key from expandable links")
        void shouldExtractSpaceKeyFromExpandableLinks() {
            V1Page v1Page = new V1Page();
            v1Page.setId("123");
            v1Page.setTitle("Title");

            V1Page.V1Expandable expandable = new V1Page.V1Expandable();
            expandable.setSpace("/rest/api/space/MYSPACE");
            v1Page.setExpandable(expandable);

            Page result = pageConverter.convertV1PageToPage(v1Page, "parent");

            assertEquals("MYSPACE", result.getSpaceId());
        }

        @Test
        @DisplayName("Should set empty spaceId when expandable is null")
        void shouldSetEmptySpaceIdWhenExpandableIsNull() {
            V1Page v1Page = new V1Page();
            v1Page.setId("123");
            v1Page.setTitle("Title");
            v1Page.setExpandable(null);

            Page result = pageConverter.convertV1PageToPage(v1Page, "parent");

            assertEquals("", result.getSpaceId());
        }

        @Test
        @DisplayName("Should set empty spaceId when expandable space is null")
        void shouldSetEmptySpaceIdWhenExpandableSpaceIsNull() {
            V1Page v1Page = new V1Page();
            v1Page.setId("123");
            v1Page.setTitle("Title");

            V1Page.V1Expandable expandable = new V1Page.V1Expandable();
            expandable.setSpace(null);
            v1Page.setExpandable(expandable);

            Page result = pageConverter.convertV1PageToPage(v1Page, "parent");

            assertEquals("", result.getSpaceId());
        }

        @Test
        @DisplayName("Should extract webui link from V1 links")
        void shouldExtractWebuiLink() {
            V1Page v1Page = new V1Page();
            v1Page.setId("123");
            v1Page.setTitle("Title");

            V1Page.V1PageLinks links = new V1Page.V1PageLinks();
            links.setWebui("/display/SPACE/PageTitle");
            v1Page.setLinks(links);

            Page result = pageConverter.convertV1PageToPage(v1Page, "parent");

            assertNotNull(result.getLinks());
            assertEquals("/display/SPACE/PageTitle", result.getLinks().get("webui"));
        }

        @Test
        @DisplayName("Should extract self link from V1 links")
        void shouldExtractSelfLink() {
            V1Page v1Page = new V1Page();
            v1Page.setId("123");
            v1Page.setTitle("Title");

            V1Page.V1PageLinks links = new V1Page.V1PageLinks();
            links.setSelf("https://test.atlassian.net/wiki/rest/api/content/123");
            v1Page.setLinks(links);

            Page result = pageConverter.convertV1PageToPage(v1Page, "parent");

            assertNotNull(result.getLinks());
            assertEquals("https://test.atlassian.net/wiki/rest/api/content/123",
                    result.getLinks().get("self"));
        }

        @Test
        @DisplayName("Should extract tinyui link from V1 links")
        void shouldExtractTinyuiLink() {
            V1Page v1Page = new V1Page();
            v1Page.setId("123");
            v1Page.setTitle("Title");

            V1Page.V1PageLinks links = new V1Page.V1PageLinks();
            links.setTinyui("/x/shorturl");
            v1Page.setLinks(links);

            Page result = pageConverter.convertV1PageToPage(v1Page, "parent");

            assertNotNull(result.getLinks());
            assertEquals("/x/shorturl", result.getLinks().get("tinyui"));
        }

        @Test
        @DisplayName("Should extract editui link from V1 links")
        void shouldExtractEdituiLink() {
            V1Page v1Page = new V1Page();
            v1Page.setId("123");
            v1Page.setTitle("Title");

            V1Page.V1PageLinks links = new V1Page.V1PageLinks();
            links.setEditui("/pages/editpage.action?pageId=123");
            v1Page.setLinks(links);

            Page result = pageConverter.convertV1PageToPage(v1Page, "parent");

            assertNotNull(result.getLinks());
            assertEquals("/pages/editpage.action?pageId=123",
                    result.getLinks().get("editui"));
        }

        @Test
        @DisplayName("Should handle null links gracefully")
        void shouldHandleNullLinksGracefully() {
            V1Page v1Page = new V1Page();
            v1Page.setId("123");
            v1Page.setTitle("Title");
            v1Page.setLinks(null);

            Page result = pageConverter.convertV1PageToPage(v1Page, "parent");

            assertNotNull(result.getLinks());
            assertTrue(result.getLinks().isEmpty());
        }

        @Test
        @DisplayName("Should default id to empty string when null")
        void shouldDefaultIdToEmptyString() {
            V1Page v1Page = new V1Page();
            v1Page.setId(null);
            v1Page.setTitle("Title");

            Page result = pageConverter.convertV1PageToPage(v1Page, "parent");

            assertEquals("", result.getId());
        }

        @Test
        @DisplayName("Should default title to empty string when null")
        void shouldDefaultTitleToEmptyString() {
            V1Page v1Page = new V1Page();
            v1Page.setId("123");
            v1Page.setTitle(null);

            Page result = pageConverter.convertV1PageToPage(v1Page, "parent");

            assertEquals("", result.getTitle());
        }

        @Test
        @DisplayName("Should default status to empty string when null")
        void shouldDefaultStatusToEmptyString() {
            V1Page v1Page = new V1Page();
            v1Page.setId("123");
            v1Page.setTitle("Title");
            v1Page.setStatus(null);

            Page result = pageConverter.convertV1PageToPage(v1Page, "parent");

            assertEquals("", result.getStatus());
        }

        @Test
        @DisplayName("Should skip null link entries in V1 links")
        void shouldSkipNullLinkEntries() {
            V1Page v1Page = new V1Page();
            v1Page.setId("123");
            v1Page.setTitle("Title");

            V1Page.V1PageLinks links = new V1Page.V1PageLinks();
            links.setWebui("/display/SPACE/Page");
            // self, tinyui, editui, edituiv2 are all null
            v1Page.setLinks(links);

            Page result = pageConverter.convertV1PageToPage(v1Page, "parent");

            assertNotNull(result.getLinks());
            assertEquals(1, result.getLinks().size(),
                    "Only non-null links should be included");
            assertEquals("/display/SPACE/Page", result.getLinks().get("webui"));
        }

        @Test
        @DisplayName("Should extract edituiv2 link from V1 links")
        void shouldExtractEdituiv2Link() {
            V1Page v1Page = new V1Page();
            v1Page.setId("123");
            v1Page.setTitle("Title");

            V1Page.V1PageLinks links = new V1Page.V1PageLinks();
            links.setEdituiv2("/pages/resumedraft.action?draftId=123");
            v1Page.setLinks(links);

            Page result = pageConverter.convertV1PageToPage(v1Page, "parent");

            assertNotNull(result.getLinks());
            assertEquals("/pages/resumedraft.action?draftId=123",
                    result.getLinks().get("edituiv2"));
        }
    }

    // ──────────────────────────────────────────────────────────────
    // convertV2AncestorToPage()
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("convertV2AncestorToPage")
    class ConvertV2AncestorToPageTests {

        @SuppressWarnings("unchecked")
        private HttpResponse<String> createMockHttpResponse(int statusCode, String body) {
            HttpResponse<String> response = mock(HttpResponse.class);
            when(response.statusCode()).thenReturn(statusCode);
            lenient().when(response.body()).thenReturn(body);
            return response;
        }

        @Test
        @DisplayName("Should fetch details via V2 API and build page")
        void shouldFetchDetailsViaV2ApiAndBuildPage() {
            when(confluenceAttributes.getRestClient()).thenReturn(restClient);

            String v2ResponseBody = "{\"title\":\"Ancestor Page\",\"status\":\"current\","
                    + "\"spaceId\":\"SPACE-001\",\"parentId\":\"parent-1\",\"parentType\":\"page\"}";
            HttpResponse<String> mockResponse = createMockHttpResponse(
                    HttpURLConnection.HTTP_OK, v2ResponseBody);

            when(restClient.doSyncGet(eq(APIVersion.V2), eq(Map.of()), eq("pages"), eq("ancestor-1")))
                    .thenReturn(mockResponse);

            V2Ancestor ancestor = new V2Ancestor("ancestor-1", "page");

            Page result = pageConverter.convertV2AncestorToPage(ancestor);

            assertNotNull(result);
            assertEquals("ancestor-1", result.getId());
            assertEquals("Ancestor Page", result.getTitle());
            assertEquals("current", result.getStatus());
            assertEquals("SPACE-001", result.getSpaceId());
        }

        @Test
        @DisplayName("Should fallback to V1 API when V2 returns non-200")
        void shouldFallbackToV1ApiWhenV2ReturnsNon200() {
            when(confluenceAttributes.getRestClient()).thenReturn(restClient);

            HttpResponse<String> v2FailResponse = createMockHttpResponse(
                    HttpURLConnection.HTTP_NOT_FOUND, "");

            when(restClient.doSyncGet(eq(APIVersion.V2), eq(Map.of()), eq("pages"), eq("anc-2")))
                    .thenReturn(v2FailResponse);

            String v1ResponseBody = "{\"title\":\"V1 Ancestor\",\"status\":\"current\","
                    + "\"space\":{\"id\":\"V1-SPACE\"}}";
            HttpResponse<String> v1Response = createMockHttpResponse(
                    HttpURLConnection.HTTP_OK, v1ResponseBody);

            when(restClient.doSyncGet(eq(APIVersion.V1), eq(Map.of("expand", "space")),
                    eq("content"), eq("anc-2")))
                    .thenReturn(v1Response);

            V2Ancestor ancestor = new V2Ancestor("anc-2", "page");

            Page result = pageConverter.convertV2AncestorToPage(ancestor);

            assertNotNull(result);
            assertEquals("anc-2", result.getId());
            assertEquals("V1 Ancestor", result.getTitle());
        }

        @Test
        @DisplayName("Should fallback to V1 API when V2 throws exception")
        void shouldFallbackToV1ApiWhenV2ThrowsException() {
            when(confluenceAttributes.getRestClient()).thenReturn(restClient);

            when(restClient.doSyncGet(eq(APIVersion.V2), eq(Map.of()), eq("pages"), eq("anc-3")))
                    .thenThrow(new RuntimeException("V2 API failed"));

            String v1ResponseBody = "{\"title\":\"V1 Fallback\",\"status\":\"current\","
                    + "\"space\":{\"id\":\"FB-SPACE\"}}";
            HttpResponse<String> v1Response = createMockHttpResponse(
                    HttpURLConnection.HTTP_OK, v1ResponseBody);

            when(restClient.doSyncGet(eq(APIVersion.V1), eq(Map.of("expand", "space")),
                    eq("content"), eq("anc-3")))
                    .thenReturn(v1Response);

            V2Ancestor ancestor = new V2Ancestor("anc-3", "page");

            Page result = pageConverter.convertV2AncestorToPage(ancestor);

            assertNotNull(result);
            assertEquals("anc-3", result.getId());
            assertEquals("V1 Fallback", result.getTitle());
        }

        @Test
        @DisplayName("Should return minimal page when both V2 and V1 fail")
        void shouldReturnMinimalPageWhenBothApiFail() {
            when(confluenceAttributes.getRestClient()).thenReturn(restClient);

            when(restClient.doSyncGet(eq(APIVersion.V2), any(), any(), any()))
                    .thenThrow(new RuntimeException("V2 failed"));
            when(restClient.doSyncGet(eq(APIVersion.V1), any(), any(), any()))
                    .thenThrow(new RuntimeException("V1 failed"));

            V2Ancestor ancestor = new V2Ancestor("anc-4", "page");

            Page result = pageConverter.convertV2AncestorToPage(ancestor);

            assertNotNull(result, "Should still return a page even when APIs fail");
            assertEquals("anc-4", result.getId());
            assertEquals("", result.getTitle(), "Title should be empty for minimal fallback");
            assertEquals("page", result.getType());
        }

        @Test
        @DisplayName("Should default type to 'page' when ancestor type is null")
        void shouldDefaultTypeToPageWhenAncestorTypeIsNull() {
            when(confluenceAttributes.getRestClient()).thenReturn(restClient);

            when(restClient.doSyncGet(any(), any(), any(), any()))
                    .thenThrow(new RuntimeException("API failure"));

            V2Ancestor ancestor = new V2Ancestor("anc-5", null);

            Page result = pageConverter.convertV2AncestorToPage(ancestor);

            assertNotNull(result);
            assertEquals("page", result.getType(),
                    "Should default to 'page' when type is null");
        }

        @Test
        @DisplayName("Should default type to 'page' when ancestor type is empty")
        void shouldDefaultTypeToPageWhenAncestorTypeIsEmpty() {
            when(confluenceAttributes.getRestClient()).thenReturn(restClient);

            when(restClient.doSyncGet(any(), any(), any(), any()))
                    .thenThrow(new RuntimeException("API failure"));

            V2Ancestor ancestor = new V2Ancestor("anc-6", "");

            Page result = pageConverter.convertV2AncestorToPage(ancestor);

            assertNotNull(result);
            assertEquals("page", result.getType(),
                    "Should default to 'page' when type is empty");
        }

        @Test
        @DisplayName("Should use correct API path segment for folder type")
        void shouldUseCorrectApiPathForFolderType() {
            when(confluenceAttributes.getRestClient()).thenReturn(restClient);

            String v2ResponseBody = "{\"title\":\"Folder\",\"status\":\"current\","
                    + "\"spaceId\":\"SP-1\",\"parentId\":\"p1\",\"parentType\":\"page\"}";
            HttpResponse<String> mockResponse = createMockHttpResponse(
                    HttpURLConnection.HTTP_OK, v2ResponseBody);

            when(restClient.doSyncGet(eq(APIVersion.V2), eq(Map.of()),
                    eq("folders"), eq("folder-1")))
                    .thenReturn(mockResponse);

            V2Ancestor ancestor = new V2Ancestor("folder-1", "folder");

            Page result = pageConverter.convertV2AncestorToPage(ancestor);

            assertNotNull(result);
            assertEquals("folder-1", result.getId());
            verify(restClient).doSyncGet(eq(APIVersion.V2), eq(Map.of()),
                    eq("folders"), eq("folder-1"));
        }

        @Test
        @DisplayName("Should handle V2 response with null parentId and parentType")
        void shouldHandleV2ResponseWithNullParentFields() {
            when(confluenceAttributes.getRestClient()).thenReturn(restClient);

            String v2ResponseBody = "{\"title\":\"Orphan Page\",\"status\":\"current\","
                    + "\"spaceId\":\"SP-1\",\"parentId\":null,\"parentType\":null}";
            HttpResponse<String> mockResponse = createMockHttpResponse(
                    HttpURLConnection.HTTP_OK, v2ResponseBody);

            when(restClient.doSyncGet(eq(APIVersion.V2), eq(Map.of()),
                    eq("pages"), eq("orphan-1")))
                    .thenReturn(mockResponse);

            V2Ancestor ancestor = new V2Ancestor("orphan-1", "page");

            Page result = pageConverter.convertV2AncestorToPage(ancestor);

            assertNotNull(result);
            assertEquals("orphan-1", result.getId());
            assertEquals("Orphan Page", result.getTitle());
            assertEquals("", result.getParentId());
            assertEquals("", result.getParentType());
        }
    }
}
