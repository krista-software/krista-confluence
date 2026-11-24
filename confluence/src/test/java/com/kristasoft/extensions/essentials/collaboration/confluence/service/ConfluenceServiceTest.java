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

package com.kristasoft.extensions.essentials.collaboration.confluence.service;

import com.kristasoft.extensions.essentials.collaboration.confluence.ConfluenceAttributes;
import com.kristasoft.extensions.essentials.collaboration.confluence.api.APIVersion;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.entities.Content;
import com.kristasoft.extensions.essentials.collaboration.confluence.client.ConfluenceRESTClient;
import com.kristasoft.extensions.essentials.collaboration.confluence.dto.*;
import com.kristasoft.extensions.essentials.collaboration.confluence.exception.ConfluenceRESTException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.HttpURLConnection;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class ConfluenceServiceTest {

    @Mock
    private ConfluenceAttributes confluenceAttributes;

    @Mock
    private ConfluenceRESTClient restClient;

    @Mock
    private HttpResponse<String> httpResponse;

    @Mock
    private HttpResponse<String> httpResponse2;

    private SpaceService spaceService;
    private TemplateService templateService;
    private PageService pageService;
    private PageHierarchyService pageHierarchyService;

    private ConfluenceService confluenceService;

    @BeforeEach
    void setUp() {
        lenient().when(confluenceAttributes.getRestClient()).thenReturn(restClient);
        lenient().when(confluenceAttributes.getDomainURL()).thenReturn("https://test.atlassian.net");

        // Create real service implementations with mocked ConfluenceAttributes
        spaceService = new SpaceServiceImpl(confluenceAttributes);
        pageHierarchyService = new PageHierarchyService(confluenceAttributes);
        templateService = new TemplateServiceImpl(confluenceAttributes, spaceService);
        pageService = new PageServiceImpl(confluenceAttributes, spaceService, templateService, pageHierarchyService);
        UserService userService = new UserServiceImpl(confluenceAttributes);

        confluenceService = new ConfluenceService(confluenceAttributes, spaceService, templateService, pageService, pageHierarchyService, userService);
    }

    // Constructor tests
    @Test
    void testConstructorWithServices() {
        UserService userService = new UserServiceImpl(confluenceAttributes);
        ConfluenceService service = new ConfluenceService(confluenceAttributes, spaceService, templateService, pageService, pageHierarchyService, userService);
        assertNotNull(service);
    }

    @Test
    void testConstructorWithParameters() {
        ConfluenceService service = new ConfluenceService("test", "test@example.com", "apikey");
        assertNotNull(service);
    }

    // Template tests
    @Test
    void testGetTemplateById_Success() {
        // Arrange
        String templateId = "123";
        String responseBody = "{\"templateId\":\"123\",\"name\":\"Test Template\",\"body\":{\"storage\":{\"value\":\"<p>Test content</p>\",\"representation\":\"storage\"}}}";
        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse.body()).thenReturn(responseBody);
        when(restClient.doSyncGet(eq(APIVersion.V1), any(Map.class), eq("template"), eq(templateId))).thenReturn(httpResponse);

        // Act
        Template template = confluenceService.getTemplateById(templateId);

        // Assert
        assertNotNull(template);
        assertEquals("123", template.getTemplateId());
        assertEquals("Test Template", template.getName());
    }

    @Test
    void testGetTemplateById_Failure() {
        // Arrange
        String templateId = "123";
        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_NOT_FOUND);
        when(restClient.doSyncGet(eq(APIVersion.V1), any(Map.class), eq("template"), eq(templateId))).thenReturn(httpResponse);

        // Act & Assert
        ConfluenceRESTException exception = assertThrows(ConfluenceRESTException.class,
            () -> confluenceService.getTemplateById(templateId));
        assertTrue(exception.getMessage().contains("We couldn't find the template with ID '" + templateId + "'"));
        assertTrue(exception.getMessage().contains("404"));
    }

    @Test
    void testGetSpaces_Success() {
        // Arrange
        String responseBody = "{\"results\":[{\"id\":\"123\",\"name\":\"Test Space\",\"key\":\"TEST\"}]}";
        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse.body()).thenReturn(responseBody);
        when(restClient.doSyncGet(eq(APIVersion.V2), isNull(), eq("spaces"))).thenReturn(httpResponse);

        // Act
        Space[] spaces = confluenceService.getSpaces();

        // Assert
        assertNotNull(spaces);
        assertEquals(1, spaces.length);
        assertEquals("123", spaces[0].getId());
        assertEquals("Test Space", spaces[0].getName());
        assertEquals("TEST", spaces[0].getKey());
    }

    @Test
    void testGetSpaces_EmptyResults() {
        // Arrange
        String responseBody = "{}";
        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse.body()).thenReturn(responseBody);
        when(restClient.doSyncGet(eq(APIVersion.V2), isNull(), eq("spaces"))).thenReturn(httpResponse);

        // Act
        Space[] spaces = confluenceService.getSpaces();

        // Assert
        assertNotNull(spaces);
        assertEquals(0, spaces.length);
    }

    @Test
    void testGetSpaces_Failure() {
        // Arrange
        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_BAD_REQUEST);
        when(restClient.doSyncGet(eq(APIVersion.V2), isNull(), eq("spaces"))).thenReturn(httpResponse);

        // Act & Assert
        ConfluenceRESTException exception = assertThrows(ConfluenceRESTException.class,
            () -> confluenceService.getSpaces());
        assertTrue(exception.getMessage().contains("We couldn't retrieve your Confluence spaces"));
        assertTrue(exception.getMessage().contains("400"));
    }

    @Test
    void testGetSpaces_WithPagination() {
        // Arrange - Two pages of results
        String firstPageResponse = "{\"results\":[" +
                "{\"id\":\"1\",\"name\":\"Space 1\",\"key\":\"S1\"}" +
                "],\"_links\":{\"next\":\"/wiki/api/v2/spaces?cursor=abc123\"}}";
        String secondPageResponse = "{\"results\":[" +
                "{\"id\":\"2\",\"name\":\"Space 2\",\"key\":\"S2\"}" +
                "]}";

        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse.body()).thenReturn(firstPageResponse);
        when(httpResponse2.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse2.body()).thenReturn(secondPageResponse);

        // First request (no params)
        when(restClient.doSyncGet(eq(APIVersion.V2), isNull(), eq("spaces")))
                .thenReturn(httpResponse);

        // Second request (pagination)
        when(restClient.doSyncGet(APIVersion.V2, Map.of("cursor", "abc123"), "spaces"))
                .thenReturn(httpResponse2);

        // Act
        Space[] spaces = confluenceService.getSpaces();

        // Assert
        assertNotNull(spaces);
        assertEquals(2, spaces.length);
        assertEquals("1", spaces[0].getId());
        assertEquals("2", spaces[1].getId());
    }

    @Test
    void testGetSpaces_WithMultiplePaginationPages() {
        // Arrange - Three pages of results
        String firstPageResponse = "{\"results\":[" +
                "{\"id\":\"1\",\"name\":\"Space 1\",\"key\":\"S1\"}" +
                "],\"_links\":{\"next\":\"/wiki/api/v2/spaces?cursor=cursor1\"}}";
        String secondPageResponse = "{\"results\":[" +
                "{\"id\":\"2\",\"name\":\"Space 2\",\"key\":\"S2\"}" +
                "],\"_links\":{\"next\":\"/wiki/api/v2/spaces?cursor=cursor2\"}}";
        String thirdPageResponse = "{\"results\":[" +
                "{\"id\":\"3\",\"name\":\"Space 3\",\"key\":\"S3\"}" +
                "]}";

        HttpResponse<String> httpResponse3 = mock(HttpResponse.class);

        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse.body()).thenReturn(firstPageResponse);
        when(httpResponse2.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse2.body()).thenReturn(secondPageResponse);
        when(httpResponse3.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse3.body()).thenReturn(thirdPageResponse);

        // First request
        when(restClient.doSyncGet(eq(APIVersion.V2), isNull(), eq("spaces")))
                .thenReturn(httpResponse);

        // Second request
        when(restClient.doSyncGet(APIVersion.V2, Map.of("cursor", "cursor1"), "spaces"))
                .thenReturn(httpResponse2);

        // Third request
        when(restClient.doSyncGet(APIVersion.V2, Map.of("cursor", "cursor2"), "spaces"))
                .thenReturn(httpResponse3);

        // Act
        Space[] spaces = confluenceService.getSpaces();

        // Assert
        assertNotNull(spaces);
        assertEquals(3, spaces.length);
        assertEquals("1", spaces[0].getId());
        assertEquals("2", spaces[1].getId());
        assertEquals("3", spaces[2].getId());
    }

    @Test
    void testGetSpaces_WithPaginationAndEmptyLastPage() {
        // Arrange - First page has results, second page is empty
        String firstPageResponse = "{\"results\":[" +
                "{\"id\":\"1\",\"name\":\"Space 1\",\"key\":\"S1\"}" +
                "],\"_links\":{\"next\":\"/wiki/api/v2/spaces?cursor=abc123\"}}";
        String secondPageResponse = "{\"results\":[]}";

        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse.body()).thenReturn(firstPageResponse);
        when(httpResponse2.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse2.body()).thenReturn(secondPageResponse);

        // First request
        when(restClient.doSyncGet(eq(APIVersion.V2), isNull(), eq("spaces")))
                .thenReturn(httpResponse);

        // Second request (pagination)
        when(restClient.doSyncGet(APIVersion.V2, Map.of("cursor", "abc123"), "spaces"))
                .thenReturn(httpResponse2);

        // Act
        Space[] spaces = confluenceService.getSpaces();

        // Assert
        assertNotNull(spaces);
        assertEquals(1, spaces.length);
        assertEquals("1", spaces[0].getId());
    }

    @Test
    void testGetSpaces_PaginationFailsOnSecondPage() {
        // Arrange - First page succeeds, second page fails
        String firstPageResponse = "{\"results\":[" +
                "{\"id\":\"1\",\"name\":\"Space 1\",\"key\":\"S1\"}" +
                "],\"_links\":{\"next\":\"/wiki/api/v2/spaces?cursor=abc123\"}}";

        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse.body()).thenReturn(firstPageResponse);
        when(httpResponse2.statusCode()).thenReturn(HttpURLConnection.HTTP_BAD_REQUEST);

        // First request
        when(restClient.doSyncGet(eq(APIVersion.V2), isNull(), eq("spaces")))
                .thenReturn(httpResponse);

        // Second request fails
        when(restClient.doSyncGet(APIVersion.V2, Map.of("cursor", "abc123"), "spaces"))
                .thenReturn(httpResponse2);

        // Act & Assert
        ConfluenceRESTException exception = assertThrows(ConfluenceRESTException.class,
                () -> confluenceService.getSpaces());
        assertTrue(exception.getMessage().contains("We couldn't retrieve your Confluence spaces"));
        assertTrue(exception.getMessage().contains("400"));
    }

    @Test
    void testGetSpaceById_Success() {
        // Arrange
        String spaceId = "123";
        String responseBody = "{\"id\":\"123\",\"name\":\"Test Space\",\"key\":\"TEST\"}";
        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse.body()).thenReturn(responseBody);
        when(restClient.doSyncGet(eq(APIVersion.V2), isNull(), eq("spaces"), eq(spaceId))).thenReturn(httpResponse);

        // Act
        Space space = confluenceService.getSpaceById(spaceId);

        // Assert
        assertNotNull(space);
        assertEquals("123", space.getId());
        assertEquals("Test Space", space.getName());
        assertEquals("TEST", space.getKey());
    }

    @Test
    void testGetSpaceById_Failure() {
        // Arrange
        String spaceId = "123";
        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_NOT_FOUND);
        when(restClient.doSyncGet(eq(APIVersion.V2), isNull(), eq("spaces"), eq(spaceId))).thenReturn(httpResponse);

        // Act & Assert
        ConfluenceRESTException exception = assertThrows(ConfluenceRESTException.class,
            () -> confluenceService.getSpaceById(spaceId));
        assertTrue(exception.getMessage().contains("We couldn't find the space with ID or Key '" + spaceId + "'"));
        assertTrue(exception.getMessage().contains("404"));
    }

    @Test
    void testGetTemplatesBySpaceId_Success() {
        // Arrange
        String spaceId = "123";
        String spaceResponseBody = "{\"id\":\"123\",\"key\":\"TEST\"}";
        String templatesResponseBody = "{\"results\":[{\"templateId\":\"456\",\"name\":\"Test Template\",\"body\":{\"storage\":{\"value\":\"<p>Test</p>\",\"representation\":\"storage\"}}}]}";
        
        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse.body()).thenReturn(spaceResponseBody);
        when(restClient.doSyncGet(eq(APIVersion.V2), isNull(), eq("spaces"), eq(spaceId))).thenReturn(httpResponse);
        
        when(httpResponse2.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse2.body()).thenReturn(templatesResponseBody);
        when(restClient.doSyncGet(eq(APIVersion.V1), any(Map.class), eq("template"), eq("page"))).thenReturn(httpResponse2);

        // Act
        Template[] templates = confluenceService.getTemplatesBySpaceId(spaceId);

        // Assert
        assertNotNull(templates);
        assertEquals(1, templates.length);
        assertEquals("456", templates[0].getTemplateId());
        assertEquals("Test Template", templates[0].getName());
        assertEquals(spaceId, templates[0].getSpaceId());
    }

    @Test
    void testGetTemplatesBySpaceId_SpaceNotFound() {
        // Arrange
        String spaceId = "123";
        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_NOT_FOUND);
        when(restClient.doSyncGet(eq(APIVersion.V2), isNull(), eq("spaces"), eq(spaceId))).thenReturn(httpResponse);

        // Act & Assert
        ConfluenceRESTException exception = assertThrows(ConfluenceRESTException.class,
            () -> confluenceService.getTemplatesBySpaceId(spaceId));
        assertTrue(exception.getMessage().contains("We couldn't find the space with ID or Key '" + spaceId + "'"));
    }

    @Test
    void testGetTemplatesBySpaceId_SpaceKeyNull() {
        // Arrange
        String spaceId = "123";
        String spaceResponseBody = "{\"id\":\"123\",\"key\":null}";
        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse.body()).thenReturn(spaceResponseBody);
        when(restClient.doSyncGet(eq(APIVersion.V2), isNull(), eq("spaces"), eq(spaceId))).thenReturn(httpResponse);

        // Act & Assert
        ConfluenceRESTException exception = assertThrows(ConfluenceRESTException.class,
            () -> confluenceService.getTemplatesBySpaceId(spaceId));
        assertTrue(exception.getMessage().contains("We couldn't find the space with ID '" + spaceId + "'"));
    }

    @Test
    void testGetTemplatesBySpaceId_TemplatesRequestFailure() {
        // Arrange
        String spaceId = "123";
        String spaceResponseBody = "{\"id\":\"123\",\"key\":\"TEST\"}";

        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse.body()).thenReturn(spaceResponseBody);
        when(restClient.doSyncGet(eq(APIVersion.V2), isNull(), eq("spaces"), eq(spaceId))).thenReturn(httpResponse);

        when(httpResponse2.statusCode()).thenReturn(HttpURLConnection.HTTP_BAD_REQUEST);
        when(restClient.doSyncGet(eq(APIVersion.V1), any(Map.class), eq("template"), eq("page"))).thenReturn(httpResponse2);

        // Act & Assert
        ConfluenceRESTException exception = assertThrows(ConfluenceRESTException.class,
            () -> confluenceService.getTemplatesBySpaceId(spaceId));
        assertTrue(exception.getMessage().contains("We couldn't retrieve templates from space '" + spaceId + "'"));
        assertTrue(exception.getMessage().contains("400"));
    }

    @Test
    void testGetPagesBySpaceId_Success() {
        // Arrange
        String spaceId = "123";
        String spaceResponseBody = "{\"id\":\"123\",\"key\":\"TEST\"}";
        String draftPagesResponse = "{\"results\":[{\"id\":\"456\",\"title\":\"Draft Page\",\"status\":\"draft\"}]}";
        String currentPagesResponse = "{\"results\":[{\"id\":\"789\",\"title\":\"Current Page\",\"status\":\"current\"}]}";

        // Mock space call
        HttpResponse<String> spaceResponse = mock(HttpResponse.class);
        when(spaceResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(spaceResponse.body()).thenReturn(spaceResponseBody);
        when(restClient.doSyncGet(eq(APIVersion.V2), isNull(), eq("spaces"), eq(spaceId))).thenReturn(spaceResponse);

        // Mock draft pages call
        HttpResponse<String> draftResponse = mock(HttpResponse.class);
        when(draftResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(draftResponse.body()).thenReturn(draftPagesResponse);

        // Mock current pages call
        HttpResponse<String> currentResponse = mock(HttpResponse.class);
        when(currentResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(currentResponse.body()).thenReturn(currentPagesResponse);

        CompletableFuture<HttpResponse<String>> draftFuture = CompletableFuture.completedFuture(draftResponse);
        CompletableFuture<HttpResponse<String>> currentFuture = CompletableFuture.completedFuture(currentResponse);

        when(restClient.doAsyncGet(eq(APIVersion.V1), any(Map.class), eq("content")))
            .thenReturn(draftFuture)
            .thenReturn(currentFuture);

        // Act
        Page[] pages = confluenceService.getPagesBySpaceId(spaceId);

        // Assert
        assertNotNull(pages);
        assertEquals(2, pages.length);
        verify(restClient, times(2)).doAsyncGet(eq(APIVersion.V1), any(Map.class), eq("content"));
    }

    @Test
    void testGetPagesBySpaceId_SpaceNotFound() {
        // Arrange
        String spaceId = "123";
        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_NOT_FOUND);
        when(restClient.doSyncGet(eq(APIVersion.V2), isNull(), eq("spaces"), eq(spaceId))).thenReturn(httpResponse);

        // Act & Assert
        ConfluenceRESTException exception = assertThrows(ConfluenceRESTException.class,
            () -> confluenceService.getPagesBySpaceId(spaceId));
        assertTrue(exception.getMessage().contains("We couldn't find the space with ID or Key '" + spaceId + "'"));
    }

    @Test
    void testGetPagesBySpaceId_SpaceKeyNull() {
        // Arrange
        String spaceId = "123";
        String spaceResponseBody = "{\"id\":\"123\",\"key\":null}";
        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse.body()).thenReturn(spaceResponseBody);
        when(restClient.doSyncGet(eq(APIVersion.V2), isNull(), eq("spaces"), eq(spaceId))).thenReturn(httpResponse);

        // Act & Assert
        ConfluenceRESTException exception = assertThrows(ConfluenceRESTException.class,
            () -> confluenceService.getPagesBySpaceId(spaceId));
        assertTrue(exception.getMessage().contains("We couldn't find the space with ID '" + spaceId + "'"));
    }

    @Test
    void testGetPagesBySpaceId_EmptyResults() {
        // Arrange
        String spaceId = "123";
        String spaceResponseBody = "{\"id\":\"123\",\"key\":\"TEST\"}";
        String emptyResponse = "{\"results\":[]}";

        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse.body()).thenReturn(spaceResponseBody);
        when(restClient.doSyncGet(eq(APIVersion.V2), isNull(), eq("spaces"), eq(spaceId))).thenReturn(httpResponse);

        CompletableFuture<HttpResponse<String>> emptyFuture = CompletableFuture.completedFuture(httpResponse2);
        when(httpResponse2.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse2.body()).thenReturn(emptyResponse);

        when(restClient.doAsyncGet(eq(APIVersion.V1), any(Map.class), eq("content")))
            .thenReturn(emptyFuture)
            .thenReturn(emptyFuture);

        // Act
        Page[] pages = confluenceService.getPagesBySpaceId(spaceId);

        // Assert
        assertNotNull(pages);
        assertEquals(0, pages.length);
    }

    @Test
    void testGetPageById_Success() {
        // Arrange
        String pageId = "456";
        String responseBody = "{\"id\":\"456\",\"title\":\"Test Page\"}";
        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse.body()).thenReturn(responseBody);
        when(restClient.doSyncGet(eq(APIVersion.V2), any(), eq("pages"), eq(pageId))).thenReturn(httpResponse);

        // Act
        Page page = confluenceService.getPageById(pageId);

        // Assert
        assertNotNull(page);
        assertEquals("456", page.getId());
        assertEquals("Test Page", page.getTitle());
    }

    @Test
    void testGetPageById_Failure() {
        // Arrange
        String pageId = "456";
        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_NOT_FOUND);
        when(httpResponse.body()).thenReturn("Page not found");
        when(restClient.doSyncGet(eq(APIVersion.V2), any(), eq("pages"), eq(pageId))).thenReturn(httpResponse);

        // Act & Assert
        ConfluenceRESTException exception = assertThrows(ConfluenceRESTException.class,
            () -> confluenceService.getPageById(pageId));
        assertTrue(exception.getMessage().contains("Failed to retrieve page by ID: " + pageId));
        assertTrue(exception.getMessage().contains("404"));
    }

    @Test
    void testCreatePageFromTemplate_Success() {
        // Arrange
        String spaceId = "123";
        String templateId = "456";
        String title = "New Page";
        List<Map<String, Object>> variables = Arrays.asList(
            Map.of("Key", "var1", "Value", "value1"),
            Map.of("Key", "var2", "Value", "value2")
        );

        String templateResponseBody = "{\"templateId\":\"456\",\"body\":{\"storage\":{\"value\":\"<p><at:var at:name=\\\"var1\\\"></at:var></p>\",\"representation\":\"storage\"}}}";
        String pageResponseBody = "{\"id\":\"789\",\"title\":\"New Page\"}";

        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse.body()).thenReturn(templateResponseBody);
        when(restClient.doSyncGet(eq(APIVersion.V1), any(Map.class), eq("template"), eq(templateId))).thenReturn(httpResponse);

        when(httpResponse2.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse2.body()).thenReturn(pageResponseBody);
        when(restClient.doSyncPost(eq(APIVersion.V2), any(Map.class), eq("pages"))).thenReturn(httpResponse2);

        // Act
        Page page = confluenceService.createPageFromTemplate(spaceId, templateId, title, variables);

        // Assert
        assertNotNull(page);
        assertEquals("789", page.getId());
        assertEquals("New Page", page.getTitle());
        verify(restClient).doSyncPost(eq(APIVersion.V2), any(Map.class), eq("pages"));
    }

    @Test
    void testCreatePageFromTemplate_Failure() {
        // Arrange
        String spaceId = "123";
        String templateId = "456";
        String title = "New Page";
        List<Map<String, Object>> variables = Arrays.asList(Map.of("Key", "var1", "Value", "value1"));

        String templateResponseBody = "{\"templateId\":\"456\",\"body\":{\"storage\":{\"value\":\"<p>Test</p>\",\"representation\":\"storage\"}}}";

        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse.body()).thenReturn(templateResponseBody);
        when(restClient.doSyncGet(eq(APIVersion.V1), any(Map.class), eq("template"), eq(templateId))).thenReturn(httpResponse);

        when(httpResponse2.statusCode()).thenReturn(HttpURLConnection.HTTP_BAD_REQUEST);
        when(restClient.doSyncPost(eq(APIVersion.V2), any(Map.class), eq("pages"))).thenReturn(httpResponse2);

        // Act & Assert
        ConfluenceRESTException exception = assertThrows(ConfluenceRESTException.class,
            () -> confluenceService.createPageFromTemplate(spaceId, templateId, title, variables));
        assertTrue(exception.getMessage().contains("We couldn't create your Confluence page from the template"));
        assertTrue(exception.getMessage().contains("400"));
    }

    @Test
    void testCreatePageFromContent_Success() {
        // Arrange
        String spaceId = "123";
        String title = "New Page";
        List<Content> contents = Arrays.asList(
            createContent("1", "<p>Content 1</p>"),
            createContent("2", "<p>Content 2</p>")
        );

        String pageResponseBody = "{\"id\":\"789\",\"title\":\"New Page\"}";

        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse.body()).thenReturn(pageResponseBody);
        when(restClient.doSyncPost(eq(APIVersion.V2), any(Map.class), eq("pages"))).thenReturn(httpResponse);

        // Act
        Page page = confluenceService.createPageFromContent(spaceId, title, contents);

        // Assert
        assertNotNull(page);
        assertEquals("789", page.getId());
        assertEquals("New Page", page.getTitle());
        verify(restClient).doSyncPost(eq(APIVersion.V2), any(Map.class), eq("pages"));
    }

    @Test
    void testCreatePageFromContent_Failure() {
        // Arrange
        String spaceId = "123";
        String title = "New Page";
        List<Content> contents = Arrays.asList(createContent("1", "<p>Content 1</p>"));

        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_BAD_REQUEST);
        when(restClient.doSyncPost(eq(APIVersion.V2), any(Map.class), eq("pages"))).thenReturn(httpResponse);

        // Act & Assert
        ConfluenceRESTException exception = assertThrows(ConfluenceRESTException.class,
            () -> confluenceService.createPageFromContent(spaceId, title, contents));
        assertTrue(exception.getMessage().contains("We couldn't create your Confluence page from the content"));
        assertTrue(exception.getMessage().contains("400"));
    }

    @Test
    void testUpdatePageFromTemplate_Success() {
        // Arrange
        String pageId = "123";
        String templateId = "456";
        String status = "current";
        String title = "Updated Page";
        String comment = "Update comment";
        List<Map<String, Object>> variables = Arrays.asList(Map.of("Key", "var1", "Value", "value1"));

        String templateResponseBody = "{\"templateId\":\"456\",\"body\":{\"storage\":{\"value\":\"<p><at:var at:name=\\\"var1\\\"></at:var></p>\",\"representation\":\"storage\"}}}";
        String pageResponseBody = "{\"id\":\"123\",\"title\":\"Old Page\",\"status\":\"draft\",\"version\":{\"number\":1},\"body\":{\"storage\":{\"value\":\"<p>Old content</p>\",\"representation\":\"storage\"}}}";
        String updateResponseBody = "{\"id\":\"123\",\"title\":\"Updated Page\"}";

        // Mock template call
        HttpResponse<String> templateResponse = mock(HttpResponse.class);
        when(templateResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(templateResponse.body()).thenReturn(templateResponseBody);
        when(restClient.doSyncGet(eq(APIVersion.V1), any(Map.class), eq("template"), eq(templateId))).thenReturn(templateResponse);

        // Mock page call
        HttpResponse<String> pageResponse = mock(HttpResponse.class);
        when(pageResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(pageResponse.body()).thenReturn(pageResponseBody);
        when(restClient.doSyncGet(eq(APIVersion.V2), any(Map.class), eq("pages"), eq(pageId))).thenReturn(pageResponse);

        // Mock update call
        HttpResponse<String> updateResponse = mock(HttpResponse.class);
        when(updateResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(updateResponse.body()).thenReturn(updateResponseBody);
        when(restClient.doSyncPut(eq(APIVersion.V2), any(Map.class), eq("pages"), eq(pageId))).thenReturn(updateResponse);

        // Act
        Page page = confluenceService.updatePageFromTemplate(pageId, templateId, status, title, comment, variables);

        // Assert
        assertNotNull(page);
        assertEquals("123", page.getId());
        assertEquals("Updated Page", page.getTitle());
        verify(restClient).doSyncPut(eq(APIVersion.V2), any(Map.class), eq("pages"), eq(pageId));
    }

    @Test
    void testUpdatePageFromTemplate_Failure() {
        // Arrange
        String pageId = "123";
        String templateId = "456";
        String status = "current";
        String title = "Updated Page";
        String comment = "Update comment";
        List<Map<String, Object>> variables = Arrays.asList(Map.of("Key", "var1", "Value", "value1"));

        String templateResponseBody = "{\"templateId\":\"456\",\"body\":{\"storage\":{\"value\":\"<p>Test</p>\",\"representation\":\"storage\"}}}";
        String pageResponseBody = "{\"id\":\"123\",\"title\":\"Old Page\",\"status\":\"draft\",\"version\":{\"number\":1}}";

        // Mock template call
        HttpResponse<String> templateResponse = mock(HttpResponse.class);
        when(templateResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(templateResponse.body()).thenReturn(templateResponseBody);
        when(restClient.doSyncGet(eq(APIVersion.V1), any(Map.class), eq("template"), eq(templateId))).thenReturn(templateResponse);

        // Mock page call
        HttpResponse<String> pageResponse = mock(HttpResponse.class);
        when(pageResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(pageResponse.body()).thenReturn(pageResponseBody);
        when(restClient.doSyncGet(eq(APIVersion.V2), any(Map.class), eq("pages"), eq(pageId))).thenReturn(pageResponse);

        // Mock update call
        HttpResponse<String> updateResponse = mock(HttpResponse.class);
        when(updateResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_BAD_REQUEST);
        when(updateResponse.body()).thenReturn("Bad request");
        when(restClient.doSyncPut(eq(APIVersion.V2), any(Map.class), eq("pages"), eq(pageId))).thenReturn(updateResponse);

        // Act & Assert
        ConfluenceRESTException exception = assertThrows(ConfluenceRESTException.class,
            () -> confluenceService.updatePageFromTemplate(pageId, templateId, status, title, comment, variables));
        assertTrue(exception.getMessage().contains("Failed to update page from template"));
        assertTrue(exception.getMessage().contains("400"));
    }

    @Test
    void testUpdatePageFromContent_Success() {
        // Arrange
        String pageId = "123";
        String status = "current";
        String title = "Updated Page";
        String comment = "Update comment";
        List<Content> contents = Arrays.asList(createContent("1", "<p>Updated content</p>"));

        String pageResponseBody = "{\"id\":\"123\",\"title\":\"Old Page\",\"status\":\"draft\",\"version\":{\"number\":1},\"body\":{\"storage\":{\"value\":\"<p>Old content</p>\",\"representation\":\"storage\"}}}";
        String updateResponseBody = "{\"id\":\"123\",\"title\":\"Updated Page\"}";

        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse.body()).thenReturn(pageResponseBody);
        when(restClient.doSyncGet(eq(APIVersion.V2), any(Map.class), eq("pages"), eq(pageId))).thenReturn(httpResponse);

        when(httpResponse2.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse2.body()).thenReturn(updateResponseBody);
        when(restClient.doSyncPut(eq(APIVersion.V2), any(Map.class), eq("pages"), eq(pageId))).thenReturn(httpResponse2);

        // Act
        Page page = confluenceService.updatePageFromContent(pageId, status, title, comment, contents);

        // Assert
        assertNotNull(page);
        assertEquals("123", page.getId());
        assertEquals("Updated Page", page.getTitle());
        verify(restClient).doSyncPut(eq(APIVersion.V2), any(Map.class), eq("pages"), eq(pageId));
    }

    @Test
    void testUpdatePageFromContent_Failure() {
        // Arrange
        String pageId = "123";
        String status = "current";
        String title = "Updated Page";
        String comment = "Update comment";
        List<Content> contents = Arrays.asList(createContent("1", "<p>Updated content</p>"));

        String pageResponseBody = "{\"id\":\"123\",\"title\":\"Old Page\",\"status\":\"draft\",\"version\":{\"number\":1}}";

        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse.body()).thenReturn(pageResponseBody);
        when(restClient.doSyncGet(eq(APIVersion.V2), any(Map.class), eq("pages"), eq(pageId))).thenReturn(httpResponse);

        when(httpResponse2.statusCode()).thenReturn(HttpURLConnection.HTTP_BAD_REQUEST);
        when(httpResponse2.body()).thenReturn("Bad request");
        when(restClient.doSyncPut(eq(APIVersion.V2), any(Map.class), eq("pages"), eq(pageId))).thenReturn(httpResponse2);

        // Act & Assert
        ConfluenceRESTException exception = assertThrows(ConfluenceRESTException.class,
            () -> confluenceService.updatePageFromContent(pageId, status, title, comment, contents));
        assertTrue(exception.getMessage().contains("Failed to update page from content"));
        assertTrue(exception.getMessage().contains("400"));
    }

    // Tests for hierarchical page navigation methods

    @Test
    void testGetDirectChildPagesOfSpace_Success() {
        // Arrange
        String spaceId = "123";
        String pagesResponseBody = "{\"results\":[{\"id\":\"789\",\"title\":\"Root Page\",\"status\":\"current\"}]}";

        // Mock the getPagesInSpace call with depth=root parameter
        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse.body()).thenReturn(pagesResponseBody);
        when(restClient.doSyncGet(APIVersion.V2, Map.of("depth", "root"), "spaces", spaceId, "pages")).thenReturn(httpResponse);

        // Act
        Page[] pages = confluenceService.getDirectChildPagesOfSpace(spaceId);

        // Assert
        assertNotNull(pages);
    }

    @Test
    void testGetDirectChildPagesOfPage_Success() {
        // Arrange
        String pageId = "456";
        String childrenResponseBody = "{\"results\":[{\"id\":\"789\",\"title\":\"Child Page\",\"type\":\"page\"}]}";
        String pageDetailResponse = "{\"id\":\"456\",\"title\":\"Parent Page\",\"spaceId\":\"123\"}";

        // Mock page detail call for getting spaceId
        HttpResponse<String> pageDetailHttpResponse = mock(HttpResponse.class);
        when(pageDetailHttpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(pageDetailHttpResponse.body()).thenReturn(pageDetailResponse);
        when(restClient.doSyncGet(APIVersion.V2, Map.of(), "pages", pageId)).thenReturn(pageDetailHttpResponse);

        // Mock children call
        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse.body()).thenReturn(childrenResponseBody);
        when(restClient.doSyncGet(eq(APIVersion.V2), any(Map.class), eq("pages"), eq(pageId), eq("direct-children"))).thenReturn(httpResponse);

        // Act
        Page[] pages = confluenceService.getDirectChildPagesOfPage(pageId);

        // Assert
        assertNotNull(pages);
    }

    @Test
    void testHasChildPages_True() {
        // Arrange
        String pageId = "456";
        String responseBody = "{\"results\":[{\"id\":\"789\",\"title\":\"Child Page\"}]}";

        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse.body()).thenReturn(responseBody);
        when(restClient.doSyncGet(eq(APIVersion.V2), any(Map.class), eq("pages"), eq(pageId), eq("direct-children"))).thenReturn(httpResponse);

        // Act
        boolean hasChildren = confluenceService.hasChildPages(pageId);

        // Assert
        assertTrue(hasChildren);
    }

    @Test
    void testHasChildPages_False() {
        // Arrange
        String pageId = "456";
        String responseBody = "{\"results\":[]}";

        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse.body()).thenReturn(responseBody);
        when(restClient.doSyncGet(eq(APIVersion.V2), any(Map.class), eq("pages"), eq(pageId), eq("direct-children"))).thenReturn(httpResponse);

        // Act
        boolean hasChildren = confluenceService.hasChildPages(pageId);

        // Assert
        assertFalse(hasChildren);
    }

    @Test
    void testHasChildren_WithFolderType() {
        // Arrange
        String folderId = "789";
        String responseBody = "{\"results\":[{\"id\":\"101\",\"title\":\"Child Item\"}]}";

        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse.body()).thenReturn(responseBody);
        when(restClient.doSyncGet(eq(APIVersion.V2), any(Map.class), eq("folders"), eq(folderId), eq("direct-children"))).thenReturn(httpResponse);

        // Act
        boolean hasChildren = confluenceService.hasChildren(folderId, "folder");

        // Assert
        assertTrue(hasChildren);
    }

    @Test
    void testHasChildren_Failure() {
        // Arrange
        String pageId = "456";

        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_NOT_FOUND);
        when(httpResponse.body()).thenReturn("Not found");
        when(restClient.doSyncGet(eq(APIVersion.V2), any(Map.class), eq("pages"), eq(pageId), eq("direct-children"))).thenReturn(httpResponse);

        // Act & Assert
        ConfluenceRESTException exception = assertThrows(ConfluenceRESTException.class,
            () -> confluenceService.hasChildren(pageId, "page"));
        assertTrue(exception.getMessage().contains("Failed to check if page has children"));
    }

    @Test
    void testGetAncestorsOfPage_Success() {
        // Arrange
        String pageId = "789";
        String ancestorsResponseBody = "{\"results\":[{\"id\":\"456\",\"type\":\"page\"},{\"id\":\"123\",\"type\":\"page\"}]}";
        String ancestorDetailResponse1 = "{\"id\":\"456\",\"title\":\"Parent Page\",\"status\":\"current\",\"spaceId\":\"100\"}";
        String ancestorDetailResponse2 = "{\"id\":\"123\",\"title\":\"Grandparent Page\",\"status\":\"current\",\"spaceId\":\"100\"}";

        // Mock ancestors call
        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse.body()).thenReturn(ancestorsResponseBody);
        when(restClient.doSyncGet(eq(APIVersion.V2), any(Map.class), eq("pages"), eq(pageId), eq("ancestors"))).thenReturn(httpResponse);

        // Mock ancestor detail calls
        HttpResponse<String> detailResponse1 = mock(HttpResponse.class);
        when(detailResponse1.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(detailResponse1.body()).thenReturn(ancestorDetailResponse1);
        when(restClient.doSyncGet(APIVersion.V2, Map.of(), "pages", "456")).thenReturn(detailResponse1);

        HttpResponse<String> detailResponse2 = mock(HttpResponse.class);
        when(detailResponse2.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(detailResponse2.body()).thenReturn(ancestorDetailResponse2);
        when(restClient.doSyncGet(APIVersion.V2, Map.of(), "pages", "123")).thenReturn(detailResponse2);

        // Act
        Page[] ancestors = confluenceService.getAncestorsOfPage(pageId);

        // Assert
        assertNotNull(ancestors);
        assertEquals(2, ancestors.length);
    }

    @Test
    void testGetAncestorsOfContent_WithFolderType() {
        // Arrange
        String folderId = "789";
        String ancestorsResponseBody = "{\"results\":[{\"id\":\"456\",\"type\":\"folder\"}]}";
        String ancestorDetailResponse = "{\"id\":\"456\",\"title\":\"Parent Folder\",\"status\":\"current\",\"spaceId\":\"100\"}";

        // Mock ancestors call
        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse.body()).thenReturn(ancestorsResponseBody);
        when(restClient.doSyncGet(eq(APIVersion.V2), any(Map.class), eq("folders"), eq(folderId), eq("ancestors"))).thenReturn(httpResponse);

        // Mock ancestor detail call
        HttpResponse<String> detailResponse = mock(HttpResponse.class);
        when(detailResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(detailResponse.body()).thenReturn(ancestorDetailResponse);
        when(restClient.doSyncGet(APIVersion.V2, Map.of(), "folders", "456")).thenReturn(detailResponse);

        // Act
        Page[] ancestors = confluenceService.getAncestorsOfContent(folderId, "folder");

        // Assert
        assertNotNull(ancestors);
        assertEquals(1, ancestors.length);
    }

    @Test
    void testGetAncestorsOfContent_Failure() {
        // Arrange
        String pageId = "789";

        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_NOT_FOUND);
        when(httpResponse.body()).thenReturn("Not found");
        when(restClient.doSyncGet(eq(APIVersion.V2), any(Map.class), eq("pages"), eq(pageId), eq("ancestors"))).thenReturn(httpResponse);

        // Act & Assert
        ConfluenceRESTException exception = assertThrows(ConfluenceRESTException.class,
            () -> confluenceService.getAncestorsOfPage(pageId));
        assertTrue(exception.getMessage().contains("Failed to retrieve ancestors"));
    }

    @Test
    void testGetDirectChildrenOfContent_Success() {
        // Arrange
        String pageId = "456";
        String childrenResponseBody = "{\"results\":[{\"id\":\"789\",\"title\":\"Child Page\",\"type\":\"page\"}]}";
        String pageDetailResponse = "{\"id\":\"456\",\"title\":\"Parent Page\",\"spaceId\":\"123\"}";

        // Mock page detail call for getting spaceId
        HttpResponse<String> pageDetailHttpResponse = mock(HttpResponse.class);
        when(pageDetailHttpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(pageDetailHttpResponse.body()).thenReturn(pageDetailResponse);
        when(restClient.doSyncGet(APIVersion.V2, Map.of(), "pages", pageId)).thenReturn(pageDetailHttpResponse);

        // Mock children call
        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse.body()).thenReturn(childrenResponseBody);
        when(restClient.doSyncGet(eq(APIVersion.V2), any(Map.class), eq("pages"), eq(pageId), eq("direct-children"))).thenReturn(httpResponse);

        // Act
        Page[] children = confluenceService.getDirectChildrenOfContent(pageId, "page");

        // Assert
        assertNotNull(children);
    }

    @Test
    void testGetDirectChildrenOfContent_Failure() {
        // Arrange
        String pageId = "456";
        String pageDetailResponse = "{\"id\":\"456\",\"title\":\"Parent Page\",\"spaceId\":\"123\"}";

        // Mock page detail call for getting spaceId
        HttpResponse<String> pageDetailHttpResponse = mock(HttpResponse.class);
        when(pageDetailHttpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(pageDetailHttpResponse.body()).thenReturn(pageDetailResponse);
        when(restClient.doSyncGet(APIVersion.V2, Map.of(), "pages", pageId)).thenReturn(pageDetailHttpResponse);

        // Mock children call failure
        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_BAD_REQUEST);
        when(httpResponse.body()).thenReturn("Bad request");
        when(restClient.doSyncGet(eq(APIVersion.V2), any(Map.class), eq("pages"), eq(pageId), eq("direct-children"))).thenReturn(httpResponse);

        // Act & Assert
        ConfluenceRESTException exception = assertThrows(ConfluenceRESTException.class,
            () -> confluenceService.getDirectChildrenOfContent(pageId, "page"));
        assertTrue(exception.getMessage().contains("Failed to retrieve children"));
    }

    @Test
    void testGetAllNestedDescendants_Success() {
        // Arrange
        String pageId = "456";
        // Include status:"current" so the recursive call happens (only current pages are traversed for children)
        String childrenResponseBody = "{\"results\":[{\"id\":\"789\",\"title\":\"Child Page\",\"type\":\"page\",\"status\":\"current\"}]}";
        String grandchildrenResponseBody = "{\"results\":[]}";
        String pageDetailResponse = "{\"id\":\"456\",\"title\":\"Parent Page\",\"spaceId\":\"123\"}";
        String childDetailResponse = "{\"id\":\"789\",\"title\":\"Child Page\",\"spaceId\":\"123\"}";

        // Mock page detail call for getting spaceId
        HttpResponse<String> pageDetailHttpResponse = mock(HttpResponse.class);
        when(pageDetailHttpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(pageDetailHttpResponse.body()).thenReturn(pageDetailResponse);
        when(restClient.doSyncGet(APIVersion.V2, Map.of(), "pages", pageId)).thenReturn(pageDetailHttpResponse);

        // Mock child detail call for getting spaceId
        HttpResponse<String> childDetailHttpResponse = mock(HttpResponse.class);
        when(childDetailHttpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(childDetailHttpResponse.body()).thenReturn(childDetailResponse);
        when(restClient.doSyncGet(APIVersion.V2, Map.of(), "pages", "789")).thenReturn(childDetailHttpResponse);

        // Mock children call for parent
        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse.body()).thenReturn(childrenResponseBody);
        when(restClient.doSyncGet(eq(APIVersion.V2), any(Map.class), eq("pages"), eq(pageId), eq("direct-children"))).thenReturn(httpResponse);

        // Mock children call for child (no grandchildren)
        when(httpResponse2.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse2.body()).thenReturn(grandchildrenResponseBody);
        when(restClient.doSyncGet(eq(APIVersion.V2), any(Map.class), eq("pages"), eq("789"), eq("direct-children"))).thenReturn(httpResponse2);

        // Act
        Page[] descendants = confluenceService.getAllNestedDescendants(pageId);

        // Assert
        assertNotNull(descendants);
    }

    @Test
    void testGetAllNestedDescendants_WithContentType() {
        // Arrange
        String folderId = "456";
        String childrenResponseBody = "{\"results\":[]}";
        String folderDetailResponse = "{\"id\":\"456\",\"title\":\"Parent Folder\",\"spaceId\":\"123\"}";

        // Mock folder detail call for getting spaceId
        HttpResponse<String> folderDetailHttpResponse = mock(HttpResponse.class);
        when(folderDetailHttpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(folderDetailHttpResponse.body()).thenReturn(folderDetailResponse);
        when(restClient.doSyncGet(APIVersion.V2, Map.of(), "folders", folderId)).thenReturn(folderDetailHttpResponse);

        // Mock children call
        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse.body()).thenReturn(childrenResponseBody);
        when(restClient.doSyncGet(eq(APIVersion.V2), any(Map.class), eq("folders"), eq(folderId), eq("direct-children"))).thenReturn(httpResponse);

        // Act
        Page[] descendants = confluenceService.getAllNestedDescendants(folderId, "folder");

        // Assert
        assertNotNull(descendants);
        assertEquals(0, descendants.length);
    }

    @Test
    void testGetConfluenceAttributes() {
        // Act
        ConfluenceAttributes attributes = confluenceService.getConfluenceAttributes();

        // Assert
        assertNotNull(attributes);
        assertSame(confluenceAttributes, attributes);
    }

    @Test
    void testGetTemplatesBySpaceId_Failure() {
        // Arrange
        String spaceId = "123";
        String spaceResponseBody = "{\"id\":\"123\",\"key\":\"TEST\"}";

        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse.body()).thenReturn(spaceResponseBody);
        when(restClient.doSyncGet(eq(APIVersion.V2), isNull(), eq("spaces"), eq(spaceId))).thenReturn(httpResponse);

        when(httpResponse2.statusCode()).thenReturn(HttpURLConnection.HTTP_INTERNAL_ERROR);
        when(restClient.doSyncGet(eq(APIVersion.V1), any(Map.class), eq("template"), eq("page"))).thenReturn(httpResponse2);

        // Act & Assert
        assertThrows(ConfluenceRESTException.class, () -> confluenceService.getTemplatesBySpaceId(spaceId));
    }

    @Test
    void testGetPageById_NotFound() {
        // Arrange
        String pageId = "nonexistent";
        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_NOT_FOUND);
        when(httpResponse.body()).thenReturn("Page not found");
        when(restClient.doSyncGet(eq(APIVersion.V2), any(Map.class), eq("pages"), eq(pageId))).thenReturn(httpResponse);

        // Act & Assert
        assertThrows(ConfluenceRESTException.class, () -> confluenceService.getPageById(pageId));
    }

    @Test
    void testGetDirectChildPagesOfSpace_Failure() {
        // Arrange
        String spaceId = "123";
        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_INTERNAL_ERROR);
        when(httpResponse.body()).thenReturn("Internal error");
        when(restClient.doSyncGet(eq(APIVersion.V2), any(Map.class), eq("spaces"), eq(spaceId), eq("pages"))).thenReturn(httpResponse);

        // Act & Assert
        assertThrows(ConfluenceRESTException.class, () -> confluenceService.getDirectChildPagesOfSpace(spaceId));
    }

    @Test
    void testGetDirectChildPagesOfPage_Failure() {
        // Arrange
        String pageId = "123";
        // Mock getContentSpaceId call
        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse.body()).thenReturn("{\"spaceId\":\"456\"}");
        when(restClient.doSyncGet(APIVersion.V2, Map.of(), "pages", pageId)).thenReturn(httpResponse);

        // Mock the direct-children call to fail
        when(httpResponse2.statusCode()).thenReturn(HttpURLConnection.HTTP_INTERNAL_ERROR);
        when(httpResponse2.body()).thenReturn("Internal error");
        when(restClient.doSyncGet(eq(APIVersion.V2), any(Map.class), eq("pages"), eq(pageId), eq("direct-children"))).thenReturn(httpResponse2);

        // Act & Assert
        assertThrows(ConfluenceRESTException.class, () -> confluenceService.getDirectChildPagesOfPage(pageId));
    }

    @Test
    void testGetAncestorsOfPage_Failure() {
        // Arrange
        String pageId = "123";
        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_INTERNAL_ERROR);
        when(httpResponse.body()).thenReturn("Internal error");
        when(restClient.doSyncGet(eq(APIVersion.V2), any(Map.class), eq("pages"), eq(pageId), eq("ancestors"))).thenReturn(httpResponse);

        // Act & Assert
        assertThrows(ConfluenceRESTException.class, () -> confluenceService.getAncestorsOfPage(pageId));
    }

    @Test
    void testGetAllNestedDescendants_Failure() {
        // Arrange
        String pageId = "123";
        // Mock getContentSpaceId call
        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse.body()).thenReturn("{\"spaceId\":\"456\"}");
        when(restClient.doSyncGet(APIVersion.V2, Map.of(), "pages", pageId)).thenReturn(httpResponse);

        // Mock the direct-children call to fail
        when(httpResponse2.statusCode()).thenReturn(HttpURLConnection.HTTP_INTERNAL_ERROR);
        when(httpResponse2.body()).thenReturn("Internal error");
        when(restClient.doSyncGet(eq(APIVersion.V2), any(Map.class), eq("pages"), eq(pageId), eq("direct-children"))).thenReturn(httpResponse2);

        // Act & Assert
        assertThrows(ConfluenceRESTException.class, () -> confluenceService.getAllNestedDescendants(pageId));
    }

    @ParameterizedTest
    @CsvSource({
        "whiteboard, whiteboards, '{\"results\":[{\"id\":\"456\",\"title\":\"Child\",\"type\":\"page\"}]}', true",
        "database, databases, '{\"results\":[]}', false",
        "embed, embeds, '{\"results\":[{\"id\":\"456\",\"title\":\"Child\",\"type\":\"page\"}]}', true"
    })
    void testHasChildren_WithDifferentContentTypes(String contentType, String endpoint, String responseBody, boolean expectedResult) {
        // Arrange
        String contentId = "123";

        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse.body()).thenReturn(responseBody);
        when(restClient.doSyncGet(eq(APIVersion.V2), any(Map.class), eq(endpoint), eq(contentId), eq("direct-children"))).thenReturn(httpResponse);

        // Act
        boolean result = confluenceService.hasChildren(contentId, contentType);

        // Assert
        assertEquals(expectedResult, result);
    }

    @Test
    void testGetAncestorsOfContent_WithWhiteboardType() {
        // Arrange
        String contentId = "123";
        String contentType = "whiteboard";
        String ancestorsResponse = "{\"results\":[{\"id\":\"parent1\",\"type\":\"page\"}]}";
        String detailResponse = "{\"id\":\"parent1\",\"title\":\"Parent Page\",\"status\":\"current\",\"spaceId\":\"456\"}";

        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse.body()).thenReturn(ancestorsResponse);
        when(restClient.doSyncGet(eq(APIVersion.V2), any(Map.class), eq("whiteboards"), eq(contentId), eq("ancestors"))).thenReturn(httpResponse);

        when(httpResponse2.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse2.body()).thenReturn(detailResponse);
        when(restClient.doSyncGet(APIVersion.V2, Map.of(), "pages", "parent1")).thenReturn(httpResponse2);

        // Act
        Page[] ancestors = confluenceService.getAncestorsOfContent(contentId, contentType);

        // Assert
        assertNotNull(ancestors);
    }

    @ParameterizedTest
    @CsvSource({
        "whiteboard, whiteboards",
        "folder, folders",
        "database, databases",
        "embed, embeds"
    })
    void testGetDirectChildrenOfContent_WithDifferentContentTypes(String contentType, String endpoint) {
        // Arrange
        String contentId = "123";
        String childrenResponse = "{\"results\":[{\"id\":\"child1\",\"title\":\"Child Page\",\"status\":\"current\",\"type\":\"page\"}]}";

        // Mock getContentSpaceId call
        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse.body()).thenReturn("{\"spaceId\":\"456\"}");
        when(restClient.doSyncGet(APIVersion.V2, Map.of(), endpoint, contentId)).thenReturn(httpResponse);

        // Mock the direct-children call
        when(httpResponse2.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse2.body()).thenReturn(childrenResponse);
        when(restClient.doSyncGet(eq(APIVersion.V2), any(Map.class), eq(endpoint), eq(contentId), eq("direct-children"))).thenReturn(httpResponse2);

        // Act
        Page[] children = confluenceService.getDirectChildrenOfContent(contentId, contentType);

        // Assert
        assertNotNull(children);
    }

    @Test
    void testGetTemplatesBySpaceId_SpaceWithNullKey() {
        // Arrange
        String spaceId = "123";
        String spaceResponse = "{\"id\":\"123\",\"name\":\"Test Space\"}"; // No key

        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse.body()).thenReturn(spaceResponse);
        when(restClient.doSyncGet(eq(APIVersion.V2), isNull(), eq("spaces"), eq(spaceId))).thenReturn(httpResponse);

        // Act & Assert
        ConfluenceRESTException exception = assertThrows(ConfluenceRESTException.class,
            () -> confluenceService.getTemplatesBySpaceId(spaceId));
        assertTrue(exception.getMessage().contains("We couldn't find the space with ID '" + spaceId + "'"));
    }

    // Additional tests for getDirectChildPagesOfSpace
    @Test
    void testGetDirectChildPagesOfSpace_WithEmptyResults() {
        // Arrange
        String spaceId = "empty-space";
        String emptyResponse = "{\"results\":[]}";

        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse.body()).thenReturn(emptyResponse);
        when(restClient.doSyncGet(APIVersion.V2, Map.of("depth", "root"), "spaces", spaceId, "pages")).thenReturn(httpResponse);

        // Act
        Page[] pages = confluenceService.getDirectChildPagesOfSpace(spaceId);

        // Assert
        assertNotNull(pages);
        assertEquals(0, pages.length);
    }

    @Test
    void testGetDirectChildPagesOfSpace_WithSinglePage() {
        // Arrange
        String spaceId = "single-page-space";
        String singlePageResponse = "{\"results\":[{\"id\":\"page1\",\"title\":\"Single Page\",\"status\":\"current\"}]}";

        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse.body()).thenReturn(singlePageResponse);
        when(restClient.doSyncGet(APIVersion.V2, Map.of("depth", "root"), "spaces", spaceId, "pages")).thenReturn(httpResponse);

        // Act
        Page[] pages = confluenceService.getDirectChildPagesOfSpace(spaceId);

        // Assert
        assertNotNull(pages);
        assertEquals(1, pages.length);
        assertEquals("page1", pages[0].getId());
    }

    @Test
    void testGetDirectChildPagesOfSpace_WithMultiplePages() {
        // Arrange
        String spaceId = "multi-page-space";
        String multiPageResponse = "{\"results\":[" +
                "{\"id\":\"page1\",\"title\":\"Page 1\",\"status\":\"current\"}," +
                "{\"id\":\"page2\",\"title\":\"Page 2\",\"status\":\"current\"}," +
                "{\"id\":\"page3\",\"title\":\"Page 3\",\"status\":\"current\"}" +
                "]}";

        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse.body()).thenReturn(multiPageResponse);
        when(restClient.doSyncGet(APIVersion.V2, Map.of("depth", "root"), "spaces", spaceId, "pages")).thenReturn(httpResponse);

        // Act
        Page[] pages = confluenceService.getDirectChildPagesOfSpace(spaceId);

        // Assert
        assertNotNull(pages);
        assertEquals(3, pages.length);
    }

    @Test
    void testGetDirectChildPagesOfSpace_WithDraftPages() {
        // Arrange
        String spaceId = "draft-space";
        String draftPageResponse = "{\"results\":[{\"id\":\"draft1\",\"title\":\"Draft Page\",\"status\":\"draft\"}]}";

        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse.body()).thenReturn(draftPageResponse);
        when(restClient.doSyncGet(APIVersion.V2, Map.of("depth", "root"), "spaces", spaceId, "pages")).thenReturn(httpResponse);

        // Act
        Page[] pages = confluenceService.getDirectChildPagesOfSpace(spaceId);

        // Assert
        assertNotNull(pages);
        assertEquals(1, pages.length);
        assertEquals("draft", pages[0].getStatus());
    }

    // Pagination tests for getAncestorsOfContent (public method that calls getAllAncestorsOfContent)
    @Test
    void testGetAncestorsOfContent_WithPagination() {
        // Arrange
        String contentId = "page123";
        String contentType = "page";

        // First page response with _links.next
        String firstPageResponse = "{\"results\":[" +
                "{\"id\":\"ancestor1\",\"title\":\"Ancestor 1\",\"type\":\"page\",\"status\":\"current\"}" +
                "],\"_links\":{\"next\":\"/wiki/api/v2/pages/page123/ancestors?cursor=abc123&limit=50\"}}";

        // Second page response without _links.next (last page)
        String secondPageResponse = "{\"results\":[" +
                "{\"id\":\"ancestor2\",\"title\":\"Ancestor 2\",\"type\":\"page\",\"status\":\"current\"}" +
                "]}";

        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse.body()).thenReturn(firstPageResponse);
        when(httpResponse2.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse2.body()).thenReturn(secondPageResponse);

        // First request
        when(restClient.doSyncGet(APIVersion.V2, Map.of("limit", "50"), "pages", contentId, "ancestors"))
                .thenReturn(httpResponse);

        // Second request (pagination)
        when(restClient.doSyncGet(APIVersion.V2, Map.of("cursor", "abc123", "limit", "50"), "pages", "page123", "ancestors"))
                .thenReturn(httpResponse2);

        // Act
        Page[] ancestors = confluenceService.getAncestorsOfContent(contentId, contentType);

        // Assert
        assertNotNull(ancestors);
        assertEquals(2, ancestors.length);
    }

    @Test
    void testGetAncestorsOfContent_WithV1StylePagination() {
        // Arrange
        String contentId = "page456";
        String contentType = "page";

        // First page response with V1-style _links.next
        String firstPageResponse = "{\"results\":[" +
                "{\"id\":\"ancestor1\",\"title\":\"Ancestor 1\",\"type\":\"page\",\"status\":\"current\"}" +
                "],\"_links\":{\"next\":\"/rest/api/pages/page456/ancestors?start=50&limit=50\"}}";

        // Second page response without _links.next (last page)
        String secondPageResponse = "{\"results\":[" +
                "{\"id\":\"ancestor2\",\"title\":\"Ancestor 2\",\"type\":\"page\",\"status\":\"current\"}" +
                "]}";

        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse.body()).thenReturn(firstPageResponse);
        when(httpResponse2.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse2.body()).thenReturn(secondPageResponse);

        // First request
        when(restClient.doSyncGet(APIVersion.V2, Map.of("limit", "50"), "pages", contentId, "ancestors"))
                .thenReturn(httpResponse);

        // Second request (pagination with V1 style URL)
        when(restClient.doSyncGet(APIVersion.V2, Map.of("start", "50", "limit", "50"), "pages", "page456", "ancestors"))
                .thenReturn(httpResponse2);

        // Act
        Page[] ancestors = confluenceService.getAncestorsOfContent(contentId, contentType);

        // Assert
        assertNotNull(ancestors);
        assertEquals(2, ancestors.length);
    }

    // Pagination tests for getDirectChildPagesOfSpace (public method that calls getRootPagesInSpace -> getPagesInSpace)
    @Test
    void testGetDirectChildPagesOfSpace_WithPagination() {
        // Arrange
        String spaceId = "space123";

        // First page response with _links.next (depth=root for getDirectChildPagesOfSpace)
        String firstPageResponse = "{\"results\":[" +
                "{\"id\":\"page1\",\"title\":\"Page 1\",\"status\":\"current\"}" +
                "],\"_links\":{\"next\":\"/wiki/api/v2/spaces/space123/pages?cursor=def456&depth=root\"}}";

        // Second page response without _links.next (last page)
        String secondPageResponse = "{\"results\":[" +
                "{\"id\":\"page2\",\"title\":\"Page 2\",\"status\":\"current\"}" +
                "]}";

        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse.body()).thenReturn(firstPageResponse);
        when(httpResponse2.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse2.body()).thenReturn(secondPageResponse);

        // First request (depth=root for root pages)
        when(restClient.doSyncGet(APIVersion.V2, Map.of("depth", "root"), "spaces", spaceId, "pages"))
                .thenReturn(httpResponse);

        // Second request (pagination)
        when(restClient.doSyncGet(APIVersion.V2, Map.of("cursor", "def456", "depth", "root"), "spaces", "space123", "pages"))
                .thenReturn(httpResponse2);

        // Act
        Page[] pages = confluenceService.getDirectChildPagesOfSpace(spaceId);

        // Assert
        assertNotNull(pages);
        assertEquals(2, pages.length);
    }

    @Test
    void testGetDirectChildPagesOfSpace_WithMultiplePaginationPages() {
        // Arrange
        String spaceId = "bigspace";

        // First page response
        String firstPageResponse = "{\"results\":[" +
                "{\"id\":\"page1\",\"title\":\"Page 1\",\"status\":\"current\"}" +
                "],\"_links\":{\"next\":\"/wiki/api/v2/spaces/bigspace/pages?cursor=cursor1\"}}";

        // Second page response
        String secondPageResponse = "{\"results\":[" +
                "{\"id\":\"page2\",\"title\":\"Page 2\",\"status\":\"current\"}" +
                "],\"_links\":{\"next\":\"/wiki/api/v2/spaces/bigspace/pages?cursor=cursor2\"}}";

        // Third page response (last page)
        String thirdPageResponse = "{\"results\":[" +
                "{\"id\":\"page3\",\"title\":\"Page 3\",\"status\":\"current\"}" +
                "]}";

        HttpResponse<String> httpResponse3 = mock(HttpResponse.class);

        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse.body()).thenReturn(firstPageResponse);
        when(httpResponse2.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse2.body()).thenReturn(secondPageResponse);
        when(httpResponse3.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse3.body()).thenReturn(thirdPageResponse);

        // First request (depth=root for root pages)
        when(restClient.doSyncGet(APIVersion.V2, Map.of("depth", "root"), "spaces", spaceId, "pages"))
                .thenReturn(httpResponse);

        // Second request
        when(restClient.doSyncGet(APIVersion.V2, Map.of("cursor", "cursor1"), "spaces", "bigspace", "pages"))
                .thenReturn(httpResponse2);

        // Third request
        when(restClient.doSyncGet(APIVersion.V2, Map.of("cursor", "cursor2"), "spaces", "bigspace", "pages"))
                .thenReturn(httpResponse3);

        // Act
        Page[] pages = confluenceService.getDirectChildPagesOfSpace(spaceId);

        // Assert
        assertNotNull(pages);
        assertEquals(3, pages.length);
    }

    @Test
    void testGetAllNestedDescendants_SkipsNonCurrentPagesForRecursion() {
        // Arrange
        String pageId = "456";
        // Child page has status "archived" - should be included in results but NOT traversed for children
        String childrenResponseBody = "{\"results\":[{\"id\":\"789\",\"title\":\"Archived Child Page\",\"type\":\"page\",\"status\":\"archived\"}]}";
        String pageDetailResponse = "{\"id\":\"456\",\"title\":\"Parent Page\",\"spaceId\":\"123\"}";

        // Mock page detail call for getting spaceId
        HttpResponse<String> pageDetailHttpResponse = mock(HttpResponse.class);
        when(pageDetailHttpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(pageDetailHttpResponse.body()).thenReturn(pageDetailResponse);
        when(restClient.doSyncGet(APIVersion.V2, Map.of(), "pages", pageId)).thenReturn(pageDetailHttpResponse);

        // Mock children call for parent - returns archived child
        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse.body()).thenReturn(childrenResponseBody);
        when(restClient.doSyncGet(eq(APIVersion.V2), any(Map.class), eq("pages"), eq(pageId), eq("direct-children"))).thenReturn(httpResponse);

        // NOTE: We do NOT mock the direct-children call for the archived child (789)
        // because the fix should skip recursion for non-current pages.
        // If the code tries to call direct-children on the archived page, the test will fail.

        // Act
        Page[] descendants = confluenceService.getAllNestedDescendants(pageId);

        // Assert
        assertNotNull(descendants);
        assertEquals(1, descendants.length);
        assertEquals("789", descendants[0].getId());
        assertEquals("archived", descendants[0].getStatus());

        // Verify that direct-children was NOT called for the archived child page
        verify(restClient, never()).doSyncGet(eq(APIVersion.V2), any(Map.class), eq("pages"), eq("789"), eq("direct-children"));
    }

    @Test
    void testGetAllNestedDescendants_MixedStatusPages() {
        // Arrange
        String pageId = "456";
        // Mix of current and non-current children
        String childrenResponseBody = "{\"results\":[" +
                "{\"id\":\"current1\",\"title\":\"Current Page\",\"type\":\"page\",\"status\":\"current\"}," +
                "{\"id\":\"archived1\",\"title\":\"Archived Page\",\"type\":\"page\",\"status\":\"archived\"}," +
                "{\"id\":\"trashed1\",\"title\":\"Trashed Page\",\"type\":\"page\",\"status\":\"trashed\"}" +
                "]}";
        String grandchildrenResponseBody = "{\"results\":[]}";
        String pageDetailResponse = "{\"id\":\"456\",\"title\":\"Parent Page\",\"spaceId\":\"123\"}";
        String currentChildDetailResponse = "{\"id\":\"current1\",\"title\":\"Current Page\",\"spaceId\":\"123\"}";

        // Mock page detail call for getting spaceId
        HttpResponse<String> pageDetailHttpResponse = mock(HttpResponse.class);
        when(pageDetailHttpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(pageDetailHttpResponse.body()).thenReturn(pageDetailResponse);
        when(restClient.doSyncGet(APIVersion.V2, Map.of(), "pages", pageId)).thenReturn(pageDetailHttpResponse);

        // Mock current child detail call for getting spaceId
        HttpResponse<String> currentChildDetailHttpResponse = mock(HttpResponse.class);
        when(currentChildDetailHttpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(currentChildDetailHttpResponse.body()).thenReturn(currentChildDetailResponse);
        when(restClient.doSyncGet(APIVersion.V2, Map.of(), "pages", "current1")).thenReturn(currentChildDetailHttpResponse);

        // Mock children call for parent - returns mixed status children
        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse.body()).thenReturn(childrenResponseBody);
        when(restClient.doSyncGet(eq(APIVersion.V2), any(Map.class), eq("pages"), eq(pageId), eq("direct-children"))).thenReturn(httpResponse);

        // Mock children call for current child only (no grandchildren)
        when(httpResponse2.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse2.body()).thenReturn(grandchildrenResponseBody);
        when(restClient.doSyncGet(eq(APIVersion.V2), any(Map.class), eq("pages"), eq("current1"), eq("direct-children"))).thenReturn(httpResponse2);

        // Act
        Page[] descendants = confluenceService.getAllNestedDescendants(pageId);

        // Assert - all 3 children should be in results
        assertNotNull(descendants);
        assertEquals(3, descendants.length);

        // Verify that direct-children was called for current page
        verify(restClient).doSyncGet(eq(APIVersion.V2), any(Map.class), eq("pages"), eq("current1"), eq("direct-children"));

        // Verify that direct-children was NOT called for archived and trashed pages
        verify(restClient, never()).doSyncGet(eq(APIVersion.V2), any(Map.class), eq("pages"), eq("archived1"), eq("direct-children"));
        verify(restClient, never()).doSyncGet(eq(APIVersion.V2), any(Map.class), eq("pages"), eq("trashed1"), eq("direct-children"));
    }

    // ─── Delegation tests (ConfluenceService → PageService / SpaceService) ──────

    /**
     * Builds a {@link ConfluenceService} backed by the supplied {@link PageService} and
     * {@link SpaceService} mocks so pass-through delegation can be verified in isolation.
     */
    private ConfluenceService serviceWithDelegates(PageService page, SpaceService space) {
        return new ConfluenceService(confluenceAttributes, space, templateService, page,
                pageHierarchyService, new UserServiceImpl(confluenceAttributes));
    }

    @Test
    void testGetPagesInSpaceV2_delegatesToPageService() {
        PageService pageMock = mock(PageService.class);
        Page[] expected = {new Page.PageBuilder("100", "Page A").build()};
        when(pageMock.getPagesInSpaceV2("42", true, false, "current")).thenReturn(expected);

        ConfluenceService svc = serviceWithDelegates(pageMock, mock(SpaceService.class));
        Page[] result = svc.getPagesInSpaceV2("42", true, false, "current");

        assertSame(expected, result);
        verify(pageMock).getPagesInSpaceV2("42", true, false, "current");
    }

    @Test
    void testGetPagesInSpaceV2_passesFlagsThroughVerbatim() {
        PageService pageMock = mock(PageService.class);
        Page[] expected = new Page[0];
        when(pageMock.getPagesInSpaceV2("7", false, true, null)).thenReturn(expected);

        ConfluenceService svc = serviceWithDelegates(pageMock, mock(SpaceService.class));
        Page[] result = svc.getPagesInSpaceV2("7", false, true, null);

        assertSame(expected, result);
        verify(pageMock).getPagesInSpaceV2("7", false, true, null);
    }

    @Test
    void testGetSpaceIdByAlias_delegatesToSpaceService() {
        SpaceService spaceMock = mock(SpaceService.class);
        Space expected = Space.builder().setId("294916").setName("My first space").build();
        when(spaceMock.getSpaceIdByAlias("MFS")).thenReturn(expected);

        ConfluenceService svc = serviceWithDelegates(mock(PageService.class), spaceMock);
        Space result = svc.getSpaceIdByAlias("MFS");

        assertSame(expected, result);
        verify(spaceMock).getSpaceIdByAlias("MFS");
    }

    // Helper methods
    private Content createContent(String id, String body) {
        Content content = new Content();
        content.iD = id;
        content.body = body;
        return content;
    }
}
