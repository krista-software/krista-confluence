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

package com.kristasoft.extensions.essentials.collaboration.confluence.mcp;

import app.krista.model.base.FreeForm;
import app.krista.sdk.atlassian.mcp.McpProductConfig;
import app.krista.sdk.atlassian.mcp.McpSearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ConfluenceMcpProductConfig")
class ConfluenceMcpProductConfigTest {

    private ConfluenceMcpProductConfig config;

    @BeforeEach
    void setUp() {
        config = new ConfluenceMcpProductConfig();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Tests for basic product configuration methods
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("product configuration")
    class ProductConfigurationTests {

        @Test
        @DisplayName("productName should return 'Confluence'")
        void shouldReturnConfluenceAsProductName() {
            assertEquals("Confluence", config.productName());
        }

        @Test
        @DisplayName("queryLanguage should return 'CQL'")
        void shouldReturnCqlAsQueryLanguage() {
            assertEquals("CQL", config.queryLanguage());
        }

        @Test
        @DisplayName("queryLanguageFullName should return 'CQL (Confluence Query Language)'")
        void shouldReturnFullCqlName() {
            assertEquals("CQL (Confluence Query Language)", config.queryLanguageFullName());
        }

        @Test
        @DisplayName("searchHttpMethod should return 'GET'")
        void shouldReturnGetAsSearchMethod() {
            assertEquals("GET", config.searchHttpMethod());
        }

        @Test
        @DisplayName("basicSearchPath should return the wiki search path")
        void shouldReturnCorrectBasicSearchPath() {
            assertEquals("/wiki/rest/api/content/search", config.basicSearchPath());
        }

        @Test
        @DisplayName("searchUrlTemplate should contain cloud ID placeholder")
        void shouldContainCloudIdPlaceholder() {
            String template = config.searchUrlTemplate();

            assertNotNull(template);
            assertTrue(template.contains("%s"), "URL template should have a cloud ID placeholder");
            assertTrue(template.contains("confluence"), "URL template should reference confluence");
            assertTrue(template.contains("content/search"), "URL template should target content/search");
        }

        @Test
        @DisplayName("standardFields should include expected fields")
        void shouldReturnStandardFields() {
            List<String> fields = config.standardFields();

            assertNotNull(fields);
            assertFalse(fields.isEmpty());
            assertTrue(fields.contains("title"));
            assertTrue(fields.contains("type"));
            assertTrue(fields.contains("status"));
            assertTrue(fields.contains("space"));
            assertTrue(fields.contains("version"));
        }

        @Test
        @DisplayName("resultColumns should include expected column definitions")
        void shouldReturnResultColumns() {
            List<McpProductConfig.ColumnDef> columns = config.resultColumns();

            assertNotNull(columns);
            assertFalse(columns.isEmpty());

            // Verify at least title, type, and status are present
            assertTrue(columns.stream().anyMatch(c -> "title".equals(c.key())));
            assertTrue(columns.stream().anyMatch(c -> "type".equals(c.key())));
            assertTrue(columns.stream().anyMatch(c -> "status".equals(c.key())));
            assertTrue(columns.stream().anyMatch(c -> "space".equals(c.key())));
            assertTrue(columns.stream().anyMatch(c -> "author".equals(c.key())));
        }

        @Test
        @DisplayName("title column should have 'link' type")
        void shouldHaveLinkTypeForTitleColumn() {
            List<McpProductConfig.ColumnDef> columns = config.resultColumns();

            McpProductConfig.ColumnDef titleColumn = columns.stream()
                    .filter(c -> "title".equals(c.key()))
                    .findFirst()
                    .orElse(null);

            assertNotNull(titleColumn);
            assertEquals("Title", titleColumn.label());
            assertEquals("link", titleColumn.type());
        }

        @Test
        @DisplayName("type column should have 'pill' type")
        void shouldHavePillTypeForTypeColumn() {
            List<McpProductConfig.ColumnDef> columns = config.resultColumns();

            McpProductConfig.ColumnDef typeColumn = columns.stream()
                    .filter(c -> "type".equals(c.key()))
                    .findFirst()
                    .orElse(null);

            assertNotNull(typeColumn);
            assertEquals("Type", typeColumn.label());
            assertEquals("pill", typeColumn.type());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Tests for prompt generation
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("prompt generation")
    class PromptGenerationTests {

        @Test
        @DisplayName("queryGenerationPrompt should return non-empty string")
        void shouldReturnNonEmptyQueryPrompt() {
            String prompt = config.queryGenerationPrompt("Find all pages in DEV space");

            assertNotNull(prompt);
            assertFalse(prompt.isEmpty());
        }

        @Test
        @DisplayName("queryGenerationPrompt should include the question")
        void shouldIncludeQuestionInQueryPrompt() {
            String question = "Find all pages in DEV space";
            String prompt = config.queryGenerationPrompt(question);

            assertTrue(prompt.contains(question),
                    "Query prompt should include the user's question");
        }

        @Test
        @DisplayName("queryGenerationPrompt should reference CQL")
        void shouldReferenceCqlInQueryPrompt() {
            String prompt = config.queryGenerationPrompt("test question");

            assertTrue(prompt.contains("CQL"),
                    "Query prompt should reference CQL query language");
        }

        @Test
        @DisplayName("queryGenerationPrompt should mention JSON output format")
        void shouldMentionJsonOutputFormat() {
            String prompt = config.queryGenerationPrompt("test question");

            assertTrue(prompt.contains("JSON"),
                    "Query prompt should mention JSON output format");
        }

        @Test
        @DisplayName("answerExtractionPrompt should return non-empty string")
        void shouldReturnNonEmptyAnswerPrompt() {
            String prompt = config.answerExtractionPrompt("Who created the Home page?", "some data");

            assertNotNull(prompt);
            assertFalse(prompt.isEmpty());
        }

        @Test
        @DisplayName("answerExtractionPrompt should include the question and data")
        void shouldIncludeQuestionAndDataInAnswerPrompt() {
            String question = "Who created the Home page?";
            String data = "{\"title\":\"Home\",\"author\":\"John\"}";
            String prompt = config.answerExtractionPrompt(question, data);

            assertTrue(prompt.contains(question),
                    "Answer prompt should include the user's question");
            assertTrue(prompt.contains(data),
                    "Answer prompt should include the content data");
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Tests for buildSearchRequestBody
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("buildSearchRequestBody")
    class BuildSearchRequestBodyTests {

        @Test
        @DisplayName("should return null (GET-based search, no request body)")
        void shouldReturnNullForGetSearch() {
            String body = config.buildSearchRequestBody("type=page", 10, false);

            assertNull(body, "Confluence uses GET - no request body should be returned");
        }

        @Test
        @DisplayName("should return null even with allFields=true")
        void shouldReturnNullWithAllFields() {
            String body = config.buildSearchRequestBody("type=page", 50, true);

            assertNull(body);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Tests for URL building methods
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("URL building")
    class UrlBuildingTests {

        @Test
        @DisplayName("buildBasicSearchUrl should construct full URL from subdomain")
        void shouldConstructFullUrlFromSubdomain() {
            String url = config.buildBasicSearchUrl("mycompany");

            assertEquals("https://mycompany.atlassian.net/wiki/rest/api/content/search", url);
        }

        @Test
        @DisplayName("buildBasicSearchUrl should handle full URL input")
        void shouldHandleFullUrlInput() {
            String url = config.buildBasicSearchUrl("https://mycompany.atlassian.net");

            assertEquals("https://mycompany.atlassian.net/wiki/rest/api/content/search", url);
        }

        @Test
        @DisplayName("buildBasicSearchUrl should strip trailing slashes")
        void shouldStripTrailingSlashes() {
            String url = config.buildBasicSearchUrl("https://mycompany.atlassian.net///");

            assertEquals("https://mycompany.atlassian.net/wiki/rest/api/content/search", url);
        }

        @Test
        @DisplayName("buildBasicBaseUrl should construct wiki base URL from subdomain")
        void shouldConstructWikiBaseUrlFromSubdomain() {
            String url = config.buildBasicBaseUrl("mycompany");

            assertEquals("https://mycompany.atlassian.net/wiki", url);
        }

        @Test
        @DisplayName("buildBasicBaseUrl should handle full URL input")
        void shouldHandleFullUrlInputForBaseUrl() {
            String url = config.buildBasicBaseUrl("https://mycompany.atlassian.net");

            assertEquals("https://mycompany.atlassian.net/wiki", url);
        }

        @Test
        @DisplayName("buildBasicBaseUrl should strip trailing slashes")
        void shouldStripTrailingSlashesForBaseUrl() {
            String url = config.buildBasicBaseUrl("https://mycompany.atlassian.net///");

            assertEquals("https://mycompany.atlassian.net/wiki", url);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Tests for parseSearchResponse
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("parseSearchResponse")
    class ParseSearchResponseTests {

        @Test
        @DisplayName("should parse search response with results")
        void shouldParseSearchResponseWithResults() {
            String json = """
                    {
                      "results": [
                        {
                          "title": "Test Page",
                          "type": "page",
                          "status": "current",
                          "space": {"name": "Dev Space", "key": "DEV"},
                          "version": {"by": {"displayName": "John Doe"}},
                          "_links": {"webui": "/spaces/DEV/pages/123/Test+Page"}
                        }
                      ],
                      "size": 1
                    }
                    """;

            McpSearchResult result = config.parseSearchResponse(json, "https://mycompany.atlassian.net/wiki");

            assertNotNull(result);
            assertEquals("cql", result.type());
            assertEquals(1, result.total());
            assertEquals(1, result.shown());
            assertFalse(result.rows().isEmpty());

            Map<String, Object> row = result.rows().get(0);
            assertEquals("Test Page", row.get("title"));
            assertEquals("page", row.get("type"));
            assertEquals("current", row.get("status"));
            assertEquals("Dev Space", row.get("space"));
            assertEquals("John Doe", row.get("author"));
            assertTrue(((String) row.get("url")).contains("/spaces/DEV/pages/123/Test+Page"));
        }

        @Test
        @DisplayName("should parse search response with empty results")
        void shouldParseSearchResponseWithEmptyResults() {
            String json = "{\"results\": [], \"size\": 0}";

            McpSearchResult result = config.parseSearchResponse(json, "https://mycompany.atlassian.net/wiki");

            assertNotNull(result);
            assertEquals(0, result.total());
            assertTrue(result.rows().isEmpty());
        }

        @Test
        @DisplayName("should throw exception for Confluence error response")
        void shouldThrowExceptionForErrorResponse() {
            String json = "{\"message\": \"CQL syntax error\", \"statusCode\": 400}";

            assertThrows(IllegalArgumentException.class,
                    () -> config.parseSearchResponse(json, "https://mycompany.atlassian.net/wiki"));
        }

        @Test
        @DisplayName("should handle missing _links in results")
        void shouldHandleMissingLinks() {
            String json = """
                    {
                      "results": [
                        {
                          "title": "No Links Page",
                          "type": "page",
                          "status": "current"
                        }
                      ],
                      "size": 1
                    }
                    """;

            McpSearchResult result = config.parseSearchResponse(json, "https://mycompany.atlassian.net/wiki");

            assertNotNull(result);
            assertEquals(1, result.shown());
            Map<String, Object> row = result.rows().get(0);
            // When there is no webui link, URL should fall back to the base wiki URL
            assertEquals("https://mycompany.atlassian.net/wiki", row.get("url"));
        }

        @Test
        @DisplayName("should append /wiki to base URL if not already present")
        void shouldAppendWikiToBaseUrl() {
            String json = """
                    {
                      "results": [
                        {
                          "title": "Test",
                          "type": "page",
                          "status": "current",
                          "_links": {"webui": "/spaces/DEV/pages/1"}
                        }
                      ],
                      "size": 1
                    }
                    """;

            McpSearchResult result = config.parseSearchResponse(json, "https://mycompany.atlassian.net");

            Map<String, Object> row = result.rows().get(0);
            String url = (String) row.get("url");
            assertTrue(url.startsWith("https://mycompany.atlassian.net/wiki"),
                    "URL should have /wiki appended");
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Tests for parseStandardResponse
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("parseStandardResponse")
    class ParseStandardResponseTests {

        @Test
        @DisplayName("should parse standard response into FreeForm list")
        void shouldParseStandardResponse() {
            String json = """
                    {
                      "results": [
                        {
                          "title": "Test Page",
                          "type": "page",
                          "status": "current",
                          "space": {"name": "Dev Space"},
                          "version": {"by": {"displayName": "Jane Doe"}},
                          "_links": {"webui": "/spaces/DEV/pages/456"}
                        }
                      ]
                    }
                    """;

            List<FreeForm> rows = config.parseStandardResponse(json, "https://mycompany.atlassian.net/wiki");

            assertNotNull(rows);
            assertEquals(1, rows.size());
        }

        @Test
        @DisplayName("should return empty list for empty results")
        void shouldReturnEmptyListForEmptyResults() {
            String json = "{\"results\": []}";

            List<FreeForm> rows = config.parseStandardResponse(json, "https://mycompany.atlassian.net/wiki");

            assertNotNull(rows);
            assertTrue(rows.isEmpty());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Tests for parseAllFieldsResponse
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("parseAllFieldsResponse")
    class ParseAllFieldsResponseTests {

        @Test
        @DisplayName("should parse all fields response into maps")
        void shouldParseAllFieldsResponse() {
            String json = """
                    {
                      "results": [
                        {
                          "title": "API Docs",
                          "type": "page",
                          "status": "current",
                          "space": {"name": "Engineering", "key": "ENG"},
                          "version": {"by": {"displayName": "Developer"}},
                          "_links": {"webui": "/spaces/ENG/pages/789"}
                        }
                      ]
                    }
                    """;

            List<Map<String, String>> result = config.parseAllFieldsResponse(json, "https://mycompany.atlassian.net/wiki");

            assertNotNull(result);
            assertEquals(1, result.size());

            Map<String, String> row = result.get(0);
            assertEquals("API Docs", row.get("title"));
            assertEquals("page", row.get("type"));
            assertEquals("current", row.get("status"));
            assertEquals("ENG", row.get("spaceKey"));
            assertEquals("Engineering", row.get("spaceName"));
            assertEquals("Developer", row.get("author"));
            assertTrue(row.get("url").contains("/spaces/ENG/pages/789"));
        }

        @Test
        @DisplayName("should return empty list for empty results")
        void shouldReturnEmptyListForEmptyResults() {
            String json = "{\"results\": []}";

            List<Map<String, String>> result = config.parseAllFieldsResponse(json, "https://mycompany.atlassian.net/wiki");

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should handle results with missing nested fields")
        void shouldHandleMissingNestedFields() {
            String json = """
                    {
                      "results": [
                        {
                          "title": "Simple Page",
                          "type": "blogpost",
                          "status": "draft"
                        }
                      ]
                    }
                    """;

            List<Map<String, String>> result = config.parseAllFieldsResponse(json, "https://mycompany.atlassian.net/wiki");

            assertNotNull(result);
            assertEquals(1, result.size());

            Map<String, String> row = result.get(0);
            assertEquals("Simple Page", row.get("title"));
            assertEquals("blogpost", row.get("type"));
            assertEquals("draft", row.get("status"));
            assertEquals("", row.get("spaceKey"));
            assertEquals("", row.get("spaceName"));
            assertEquals("", row.get("author"));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Tests for McpProductConfig interface compliance
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("McpProductConfig interface compliance")
    class InterfaceComplianceTests {

        @Test
        @DisplayName("should implement McpProductConfig interface")
        void shouldImplementMcpProductConfig() {
            assertTrue(config instanceof McpProductConfig,
                    "ConfluenceMcpProductConfig should implement McpProductConfig");
        }

        @Test
        @DisplayName("should be a final class")
        void shouldBeFinalClass() {
            assertTrue(java.lang.reflect.Modifier.isFinal(ConfluenceMcpProductConfig.class.getModifiers()),
                    "ConfluenceMcpProductConfig should be declared final");
        }
    }
}
