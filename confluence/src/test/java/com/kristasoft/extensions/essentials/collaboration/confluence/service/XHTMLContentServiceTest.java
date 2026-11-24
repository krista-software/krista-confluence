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
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.entities.Content;
import com.kristasoft.extensions.essentials.collaboration.confluence.client.ConfluenceRESTClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for the XHTMLContentService class.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("XHTMLContentService Tests")
class XHTMLContentServiceTest {

    @Mock
    private ConfluenceAttributes confluenceAttributes;

    @Mock
    private ConfluenceRESTClient restClient;

    private XHTMLContentService xhtmlContentService;

    @BeforeEach
    void setUp() {
        lenient().when(confluenceAttributes.getRestClient()).thenReturn(restClient);
        xhtmlContentService = new XHTMLContentService(confluenceAttributes);
    }

    @Test
    @DisplayName("Constructor with ConfluenceAttributes should create service")
    void constructorWithConfluenceAttributesShouldCreateService() {
        XHTMLContentService service = new XHTMLContentService(confluenceAttributes);
        assertNotNull(service);
    }

    @Test
    @DisplayName("Constructor with parameters should create service")
    void constructorWithParametersShouldCreateService() {
        XHTMLContentService service = new XHTMLContentService("domain", "email@test.com", "apikey");
        assertNotNull(service);
    }

    @Test
    @DisplayName("createContentForTable should create content with table HTML")
    void createContentForTableShouldCreateContentWithTableHtml() {
        // Arrange
        List<String> columns = Arrays.asList("Name", "Age", "City");
        List<Map<String, String>> table = Arrays.asList(
            Map.of("Name", "John", "Age", "30", "City", "New York"),
            Map.of("Name", "Jane", "Age", "25", "City", "Los Angeles")
        );

        // Act
        Content content = xhtmlContentService.createContentForTable(columns, table);

        // Assert
        assertNotNull(content);
        assertNotNull(content.iD);
        assertNotNull(content.body);
        assertTrue(content.body.contains("Name"));
        assertTrue(content.body.contains("Age"));
        assertTrue(content.body.contains("City"));
        assertTrue(content.body.contains("John"));
        assertTrue(content.body.contains("Jane"));
    }

    @Test
    @DisplayName("createContentForTable should handle empty table")
    void createContentForTableShouldHandleEmptyTable() {
        // Arrange
        List<String> columns = Arrays.asList("Column1", "Column2");
        List<Map<String, String>> table = Collections.emptyList();

        // Act
        Content content = xhtmlContentService.createContentForTable(columns, table);

        // Assert
        assertNotNull(content);
        assertNotNull(content.iD);
        assertNotNull(content.body);
    }

    @Test
    @DisplayName("createContentForTable should handle duplicate columns")
    void createContentForTableShouldHandleDuplicateColumns() {
        // Arrange
        List<String> columns = Arrays.asList("Name", "Name", "Age");
        List<Map<String, String>> table = Arrays.asList(
            Map.of("Name", "John", "Age", "30")
        );

        // Act
        Content content = xhtmlContentService.createContentForTable(columns, table);

        // Assert
        assertNotNull(content);
        assertNotNull(content.body);
    }

    @ParameterizedTest
    @CsvSource({
        "Hello World, h1, left, true",
        "Test Heading, h2, center, false",
        "Another Heading, h3, right, true",
        "Small Heading, h4, left, false",
        "Tiny Heading, h5, center, true",
        "Smallest Heading, h6, right, false"
    })
    @DisplayName("createHeadingContent should create heading with correct parameters")
    void createHeadingContentShouldCreateHeadingWithCorrectParameters(
            String text, String type, String alignment, boolean breakLine) {
        // Act
        Content content = xhtmlContentService.createHeadingContent(text, type, alignment, breakLine);

        // Assert
        assertNotNull(content);
        assertNotNull(content.iD);
        assertNotNull(content.body);
        assertTrue(content.body.contains(text));
    }

    @Test
    @DisplayName("createAnchorTag should create anchor tag content")
    void createAnchorTagShouldCreateAnchorTagContent() {
        // Arrange
        String text = "Click Here";
        String href = "https://example.com";

        // Act
        Content content = xhtmlContentService.createAnchorTag(text, href);

        // Assert
        assertNotNull(content);
        assertNotNull(content.iD);
        assertNotNull(content.body);
        assertTrue(content.body.contains(text));
        assertTrue(content.body.contains(href));
    }

    @Test
    @DisplayName("createAnchorTag should handle special characters in text")
    void createAnchorTagShouldHandleSpecialCharactersInText() {
        // Arrange
        String text = "Link with <special> & characters";
        String href = "https://example.com/path?param=value&other=test";

        // Act
        Content content = xhtmlContentService.createAnchorTag(text, href);

        // Assert
        assertNotNull(content);
        assertNotNull(content.body);
    }

    @Test
    @DisplayName("createContentForTable should handle single column")
    void createContentForTableShouldHandleSingleColumn() {
        // Arrange
        List<String> columns = Collections.singletonList("Name");
        List<Map<String, String>> table = Arrays.asList(
            Map.of("Name", "John"),
            Map.of("Name", "Jane")
        );

        // Act
        Content content = xhtmlContentService.createContentForTable(columns, table);

        // Assert
        assertNotNull(content);
        assertNotNull(content.body);
        assertTrue(content.body.contains("John"));
        assertTrue(content.body.contains("Jane"));
    }

    @Test
    @DisplayName("createContentForTable should handle many rows")
    void createContentForTableShouldHandleManyRows() {
        // Arrange
        List<String> columns = Arrays.asList("ID", "Value");
        List<Map<String, String>> table = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            table.add(Map.of("ID", String.valueOf(i), "Value", "Value" + i));
        }

        // Act
        Content content = xhtmlContentService.createContentForTable(columns, table);

        // Assert
        assertNotNull(content);
        assertNotNull(content.body);
    }

    @Test
    @DisplayName("createHeadingContent should handle empty text")
    void createHeadingContentShouldHandleEmptyText() {
        // Act
        Content content = xhtmlContentService.createHeadingContent("", "h1", "left", false);

        // Assert
        assertNotNull(content);
        assertNotNull(content.iD);
        assertNotNull(content.body);
    }

    @Test
    @DisplayName("createHeadingContent should handle uppercase alignment")
    void createHeadingContentShouldHandleUppercaseAlignment() {
        // Act
        Content content = xhtmlContentService.createHeadingContent("Test", "h2", "CENTER", true);

        // Assert
        assertNotNull(content);
        assertNotNull(content.body);
    }

    @Test
    @DisplayName("createAnchorTag should handle empty text")
    void createAnchorTagShouldHandleEmptyText() {
        // Act
        Content content = xhtmlContentService.createAnchorTag("", "https://example.com");

        // Assert
        assertNotNull(content);
        assertNotNull(content.body);
    }

    @Test
    @DisplayName("createAnchorTag should handle empty href")
    void createAnchorTagShouldHandleEmptyHref() {
        // Act
        Content content = xhtmlContentService.createAnchorTag("Click Here", "");

        // Assert
        assertNotNull(content);
        assertNotNull(content.body);
    }

    @Test
    @DisplayName("createContentForTable should handle missing column values in rows")
    void createContentForTableShouldHandleMissingColumnValuesInRows() {
        // Arrange
        List<String> columns = Arrays.asList("Name", "Age", "City");
        List<Map<String, String>> table = Arrays.asList(
            Map.of("Name", "John"),  // Missing Age and City
            Map.of("Name", "Jane", "Age", "25")  // Missing City
        );

        // Act
        Content content = xhtmlContentService.createContentForTable(columns, table);

        // Assert
        assertNotNull(content);
        assertNotNull(content.body);
    }

    @Test
    @DisplayName("createHeadingContent should handle special characters in text")
    void createHeadingContentShouldHandleSpecialCharactersInText() {
        // Act
        Content content = xhtmlContentService.createHeadingContent("Test <>&\"' Heading", "h1", "left", false);

        // Assert
        assertNotNull(content);
        assertNotNull(content.body);
    }

    @Test
    @DisplayName("Content ID should be unique for each call")
    void contentIdShouldBeUniqueForEachCall() {
        // Act
        Content content1 = xhtmlContentService.createAnchorTag("Link1", "https://example1.com");
        Content content2 = xhtmlContentService.createAnchorTag("Link2", "https://example2.com");

        // Assert
        assertNotNull(content1.iD);
        assertNotNull(content2.iD);
        assertNotEquals(content1.iD, content2.iD);
    }

    @Test
    @DisplayName("createHeadingContent with h2 type")
    void createHeadingContentWithH2Type() {
        // Act
        Content content = xhtmlContentService.createHeadingContent("H2 Heading", "h2", "center", true);

        // Assert
        assertNotNull(content);
        assertNotNull(content.body);
    }

    @Test
    @DisplayName("createHeadingContent with h3 type")
    void createHeadingContentWithH3Type() {
        // Act
        Content content = xhtmlContentService.createHeadingContent("H3 Heading", "h3", "right", false);

        // Assert
        assertNotNull(content);
        assertNotNull(content.body);
    }

    @Test
    @DisplayName("createHeadingContent with h4 type")
    void createHeadingContentWithH4Type() {
        // Act
        Content content = xhtmlContentService.createHeadingContent("H4 Heading", "h4", "justify", true);

        // Assert
        assertNotNull(content);
        assertNotNull(content.body);
    }

    @Test
    @DisplayName("createHeadingContent with h5 type")
    void createHeadingContentWithH5Type() {
        // Act
        Content content = xhtmlContentService.createHeadingContent("H5 Heading", "h5", "left", false);

        // Assert
        assertNotNull(content);
        assertNotNull(content.body);
    }

    @Test
    @DisplayName("createHeadingContent with h6 type")
    void createHeadingContentWithH6Type() {
        // Act
        Content content = xhtmlContentService.createHeadingContent("H6 Heading", "h6", "center", true);

        // Assert
        assertNotNull(content);
        assertNotNull(content.body);
    }

    @Test
    @DisplayName("createAnchorTag with empty text")
    void createAnchorTagWithEmptyText() {
        // Act
        Content content = xhtmlContentService.createAnchorTag("", "https://example.com");

        // Assert
        assertNotNull(content);
        assertNotNull(content.body);
    }

    @Test
    @DisplayName("createAnchorTag with empty href")
    void createAnchorTagWithEmptyHref() {
        // Act
        Content content = xhtmlContentService.createAnchorTag("Link Text", "");

        // Assert
        assertNotNull(content);
        assertNotNull(content.body);
    }

    @Test
    @DisplayName("createContentForTable with single column")
    void createContentForTableWithSingleColumn() {
        // Arrange
        List<String> columns = Arrays.asList("Name");
        List<Map<String, String>> table = Arrays.asList(
            Map.of("Name", "John"),
            Map.of("Name", "Jane")
        );

        // Act
        Content content = xhtmlContentService.createContentForTable(columns, table);

        // Assert
        assertNotNull(content);
        assertNotNull(content.body);
        assertTrue(content.body.contains("John"));
        assertTrue(content.body.contains("Jane"));
    }

    @Test
    @DisplayName("createContentForTable with many columns")
    void createContentForTableWithManyColumns() {
        // Arrange
        List<String> columns = Arrays.asList("Col1", "Col2", "Col3", "Col4", "Col5", "Col6", "Col7", "Col8", "Col9", "Col10");
        List<Map<String, String>> table = Arrays.asList(
            Map.of("Col1", "V1", "Col2", "V2", "Col3", "V3", "Col4", "V4", "Col5", "V5")
        );

        // Act
        Content content = xhtmlContentService.createContentForTable(columns, table);

        // Assert
        assertNotNull(content);
        assertNotNull(content.body);
    }

    @Test
    @DisplayName("createContentForTable with numeric values")
    void createContentForTableWithNumericValues() {
        // Arrange
        List<String> columns = Arrays.asList("ID", "Amount", "Quantity");
        List<Map<String, String>> table = Arrays.asList(
            Map.of("ID", "1", "Amount", "100.50", "Quantity", "5"),
            Map.of("ID", "2", "Amount", "200.75", "Quantity", "10")
        );

        // Act
        Content content = xhtmlContentService.createContentForTable(columns, table);

        // Assert
        assertNotNull(content);
        assertNotNull(content.body);
        assertTrue(content.body.contains("100.50"));
        assertTrue(content.body.contains("200.75"));
    }

    @Test
    @DisplayName("createHeadingContent with long text")
    void createHeadingContentWithLongText() {
        // Arrange
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            longText.append("Word ");
        }

        // Act
        Content content = xhtmlContentService.createHeadingContent(longText.toString(), "h1", "left", false);

        // Assert
        assertNotNull(content);
        assertNotNull(content.body);
    }

    @Test
    @DisplayName("createAnchorTag with special characters in URL")
    void createAnchorTagWithSpecialCharactersInUrl() {
        // Act
        Content content = xhtmlContentService.createAnchorTag("Link", "https://example.com/path?param=value&other=123");

        // Assert
        assertNotNull(content);
        assertNotNull(content.body);
    }

    @Test
    @DisplayName("createTickerSymbolContentForCompany should throw exception when HTTP client fails")
    void createTickerSymbolContentForCompanyShouldThrowExceptionWhenHttpClientFails() throws Exception {
        // Arrange
        java.net.http.HttpClient mockHttpClient = mock(java.net.http.HttpClient.class);
        when(restClient.client()).thenReturn(mockHttpClient);
        @SuppressWarnings("unchecked")
        var stubbing = when(mockHttpClient.send(any(java.net.http.HttpRequest.class), any(java.net.http.HttpResponse.BodyHandler.class)))
                .thenThrow(new java.io.IOException("Connection failed"));

        // Act & Assert
        assertThrows(com.kristasoft.extensions.essentials.collaboration.confluence.exception.ConfluenceRESTException.class,
                () -> xhtmlContentService.createTickerSymbolContentForCompany("Apple Inc"));
    }

    @Test
    @DisplayName("createTickerSymbolContentForCompany should throw exception when interrupted")
    void createTickerSymbolContentForCompanyShouldThrowExceptionWhenInterrupted() throws Exception {
        // Arrange
        java.net.http.HttpClient mockHttpClient = mock(java.net.http.HttpClient.class);
        when(restClient.client()).thenReturn(mockHttpClient);
        @SuppressWarnings("unchecked")
        var stubbing = when(mockHttpClient.send(any(java.net.http.HttpRequest.class), any(java.net.http.HttpResponse.BodyHandler.class)))
                .thenThrow(new InterruptedException("Thread interrupted"));

        // Act & Assert
        assertThrows(com.kristasoft.extensions.essentials.collaboration.confluence.exception.ConfluenceRESTException.class,
                () -> xhtmlContentService.createTickerSymbolContentForCompany("Apple Inc"));
    }

    @Test
    @DisplayName("createTickerSymbolContentForCompany should throw exception when service unavailable")
    void createTickerSymbolContentForCompanyShouldThrowExceptionWhenServiceUnavailable() throws Exception {
        // Arrange
        java.net.http.HttpClient mockHttpClient = mock(java.net.http.HttpClient.class);
        @SuppressWarnings("unchecked")
        java.net.http.HttpResponse<java.io.InputStream> mockResponse = mock(java.net.http.HttpResponse.class);
        when(restClient.client()).thenReturn(mockHttpClient);
        @SuppressWarnings("unchecked")
        var stubbing = when(mockHttpClient.send(any(java.net.http.HttpRequest.class), any(java.net.http.HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);
        when(mockResponse.statusCode()).thenReturn(503); // Service Unavailable

        // Act & Assert - RemoteException is wrapped in ConfluenceRESTException
        assertThrows(com.kristasoft.extensions.essentials.collaboration.confluence.exception.ConfluenceRESTException.class,
                () -> xhtmlContentService.createTickerSymbolContentForCompany("Apple Inc"));
    }

    @Test
    @DisplayName("createTickerSymbolContentForCompany should throw exception when no ticker found")
    void createTickerSymbolContentForCompanyShouldThrowExceptionWhenNoTickerFound() throws Exception {
        // Arrange
        java.net.http.HttpClient mockHttpClient = mock(java.net.http.HttpClient.class);
        @SuppressWarnings("unchecked")
        java.net.http.HttpResponse<java.io.InputStream> mockResponse = mock(java.net.http.HttpResponse.class);
        when(restClient.client()).thenReturn(mockHttpClient);
        @SuppressWarnings("unchecked")
        var stubbing = when(mockHttpClient.send(any(java.net.http.HttpRequest.class), any(java.net.http.HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);
        when(mockResponse.statusCode()).thenReturn(200);
        // Return HTML without the expected data-col0 class
        String htmlWithoutTicker = "<html><body><div>No ticker data</div></body></html>";
        when(mockResponse.body()).thenReturn(new java.io.ByteArrayInputStream(htmlWithoutTicker.getBytes(java.nio.charset.StandardCharsets.UTF_8)));

        // Act & Assert
        assertThrows(RuntimeException.class,
                () -> xhtmlContentService.createTickerSymbolContentForCompany("NonExistentCompany"));
    }

    @Test
    @DisplayName("createTickerSymbolContentForCompany should create content when ticker found")
    void createTickerSymbolContentForCompanyShouldCreateContentWhenTickerFound() throws Exception {
        // Arrange
        java.net.http.HttpClient mockHttpClient = mock(java.net.http.HttpClient.class);
        @SuppressWarnings("unchecked")
        java.net.http.HttpResponse<java.io.InputStream> mockResponse = mock(java.net.http.HttpResponse.class);
        when(restClient.client()).thenReturn(mockHttpClient);
        @SuppressWarnings("unchecked")
        var stubbing = when(mockHttpClient.send(any(java.net.http.HttpRequest.class), any(java.net.http.HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);
        when(mockResponse.statusCode()).thenReturn(200);
        // Return HTML with the expected data-col0 class and ticker data - proper table structure
        String htmlWithTicker = "<html><body><table><tbody><tr><td class=\"data-col0\"><a data-symbol=\"AAPL\" href=\"/quote/AAPL\">Apple Inc</a></td></tr></tbody></table></body></html>";
        when(mockResponse.body()).thenReturn(new java.io.ByteArrayInputStream(htmlWithTicker.getBytes(java.nio.charset.StandardCharsets.UTF_8)));

        // Act
        Content content = xhtmlContentService.createTickerSymbolContentForCompany("Apple Inc");

        // Assert
        assertNotNull(content);
        assertNotNull(content.iD);
        assertNotNull(content.body);
    }

    @Test
    @DisplayName("createTickerSymbolContentForCompany should handle special characters in company name")
    void createTickerSymbolContentForCompanyShouldHandleSpecialCharactersInCompanyName() throws Exception {
        // Arrange
        java.net.http.HttpClient mockHttpClient = mock(java.net.http.HttpClient.class);
        @SuppressWarnings("unchecked")
        java.net.http.HttpResponse<java.io.InputStream> mockResponse = mock(java.net.http.HttpResponse.class);
        when(restClient.client()).thenReturn(mockHttpClient);
        @SuppressWarnings("unchecked")
        var stubbing = when(mockHttpClient.send(any(java.net.http.HttpRequest.class), any(java.net.http.HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);
        when(mockResponse.statusCode()).thenReturn(200);
        // Return HTML with the expected data-col0 class and ticker data - proper table structure
        String htmlWithTicker = "<html><body><table><tbody><tr><td class=\"data-col0\"><a data-symbol=\"MSFT\" href=\"/quote/MSFT\">Microsoft</a></td></tr></tbody></table></body></html>";
        when(mockResponse.body()).thenReturn(new java.io.ByteArrayInputStream(htmlWithTicker.getBytes(java.nio.charset.StandardCharsets.UTF_8)));

        // Act - company name with special characters that should be replaced
        Content content = xhtmlContentService.createTickerSymbolContentForCompany("Microsoft (Corp.)");

        // Assert
        assertNotNull(content);
        assertNotNull(content.body);
    }

    @Test
    @DisplayName("extractContentFromWebPage should throw exception for invalid URL")
    void extractContentFromWebPageShouldThrowExceptionForInvalidUrl() {
        // Act & Assert
        assertThrows(RuntimeException.class,
                () -> xhtmlContentService.extractContentFromWebPage("invalid-url"));
    }

    @Test
    @DisplayName("extractContentFromWebPage should throw exception for unreachable URL")
    void extractContentFromWebPageShouldThrowExceptionForUnreachableUrl() {
        // Act & Assert
        assertThrows(RuntimeException.class,
                () -> xhtmlContentService.extractContentFromWebPage("https://this-domain-does-not-exist-12345.com"));
    }
}

