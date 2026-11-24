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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for the UrlParser utility class.
 */
@DisplayName("UrlParser Tests")
class UrlParserTest {

    @Nested
    @DisplayName("V1 URL parsing tests")
    class V1UrlParsingTests {

        @Test
        @DisplayName("Should parse V1 URL with query parameters")
        void shouldParseV1UrlWithQueryParameters() {
            String url = "/rest/api/content/104169487/child/page?next=true&limit=2&start=2";

            UrlParser.ParsedUrl result = UrlParser.parseNextUrl(url);

            assertNotNull(result);
            String[] path = result.getPathComponents();
            assertNotNull(path);
            assertEquals("content", path[0]);
            assertEquals("104169487", path[1]);
            assertEquals("child", path[2]);
            assertEquals("page", path[3]);
        }

        @Test
        @DisplayName("Should extract V1 query parameters correctly")
        void shouldExtractV1QueryParametersCorrectly() {
            String url = "/rest/api/content/104169487/child/page?next=true&limit=2&start=2";

            UrlParser.ParsedUrl result = UrlParser.parseNextUrl(url);

            Map<String, String> params = result.getQueryParams();
            assertNotNull(params);
            assertEquals("true", params.get("next"));
            assertEquals("2", params.get("limit"));
            assertEquals("2", params.get("start"));
            assertEquals(3, params.size());
        }

        @Test
        @DisplayName("Should parse wiki/rest/api prefix")
        void shouldParseWikiRestApiPrefix() {
            String url = "/wiki/rest/api/content/12345/child/page?start=0&limit=25";

            UrlParser.ParsedUrl result = UrlParser.parseNextUrl(url);

            assertNotNull(result);
            String[] path = result.getPathComponents();
            assertEquals("content", path[0]);
            assertEquals("12345", path[1]);
        }
    }

    @Nested
    @DisplayName("V2 URL parsing tests")
    class V2UrlParsingTests {

        @Test
        @DisplayName("Should parse V2 URL with cursor parameter")
        void shouldParseV2UrlWithCursorParameter() {
            String url = "/wiki/api/v2/spaces/100171995/pages?cursor=abc";

            UrlParser.ParsedUrl result = UrlParser.parseNextUrl(url);

            assertNotNull(result);
            String[] path = result.getPathComponents();
            assertNotNull(path);
            assertEquals("spaces", path[0]);
            assertEquals("100171995", path[1]);
            assertEquals("pages", path[2]);
        }

        @Test
        @DisplayName("Should extract V2 cursor query parameter correctly")
        void shouldExtractV2CursorQueryParameterCorrectly() {
            String url = "/wiki/api/v2/spaces/100171995/pages?cursor=abc";

            UrlParser.ParsedUrl result = UrlParser.parseNextUrl(url);

            Map<String, String> params = result.getQueryParams();
            assertNotNull(params);
            assertEquals("abc", params.get("cursor"));
            assertEquals(1, params.size());
        }

        @Test
        @DisplayName("Should parse V2 URL with multiple query parameters")
        void shouldParseV2UrlWithMultipleQueryParameters() {
            String url = "/wiki/api/v2/pages/12345/children?cursor=xyz&limit=50";

            UrlParser.ParsedUrl result = UrlParser.parseNextUrl(url);

            assertNotNull(result);
            Map<String, String> params = result.getQueryParams();
            assertNotNull(params);
            assertEquals("xyz", params.get("cursor"));
            assertEquals("50", params.get("limit"));
        }
    }

    @Nested
    @DisplayName("URL with no query parameters")
    class NoQueryParamsTests {

        @Test
        @DisplayName("Should parse URL with no query parameters")
        void shouldParseUrlWithNoQueryParameters() {
            String url = "/rest/api/content/104169487/child/page";

            UrlParser.ParsedUrl result = UrlParser.parseNextUrl(url);

            assertNotNull(result);
            String[] path = result.getPathComponents();
            assertNotNull(path);
            assertEquals("content", path[0]);
            assertEquals("104169487", path[1]);
            assertEquals("child", path[2]);
            assertEquals("page", path[3]);
        }

        @Test
        @DisplayName("Should return null query params when URL has no query string")
        void shouldReturnNullQueryParamsWhenNoQueryString() {
            String url = "/rest/api/content/104169487/child/page";

            UrlParser.ParsedUrl result = UrlParser.parseNextUrl(url);

            assertNull(result.getQueryParams());
        }

        @Test
        @DisplayName("Should return null query params for V2 URL with no query string")
        void shouldReturnNullQueryParamsForV2UrlWithNoQueryString() {
            String url = "/wiki/api/v2/spaces/100171995/pages";

            UrlParser.ParsedUrl result = UrlParser.parseNextUrl(url);

            assertNull(result.getQueryParams());
        }
    }

    @Nested
    @DisplayName("URL without API prefix")
    class NoApiPrefixTests {

        @Test
        @DisplayName("Should parse URL without any known API prefix")
        void shouldParseUrlWithoutAnyKnownApiPrefix() {
            String url = "/some/other/path?key=value";

            UrlParser.ParsedUrl result = UrlParser.parseNextUrl(url);

            assertNotNull(result);
            // Path starts with empty string due to leading slash split
            String[] path = result.getPathComponents();
            assertNotNull(path);
            // The url is not stripped, so the full path is preserved
            assertTrue(path.length > 0);
        }

        @Test
        @DisplayName("Should still extract query params from URL without API prefix")
        void shouldExtractQueryParamsFromUrlWithoutApiPrefix() {
            String url = "/some/other/path?key=value&foo=bar";

            UrlParser.ParsedUrl result = UrlParser.parseNextUrl(url);

            Map<String, String> params = result.getQueryParams();
            assertNotNull(params);
            assertEquals("value", params.get("key"));
            assertEquals("bar", params.get("foo"));
        }

        @Test
        @DisplayName("Should handle plain path without leading slash or prefix")
        void shouldHandlePlainPathWithoutLeadingSlash() {
            String url = "content/123/child?limit=10";

            UrlParser.ParsedUrl result = UrlParser.parseNextUrl(url);

            assertNotNull(result);
            String[] path = result.getPathComponents();
            assertEquals("content", path[0]);
            assertEquals("123", path[1]);
            assertEquals("child", path[2]);

            Map<String, String> params = result.getQueryParams();
            assertNotNull(params);
            assertEquals("10", params.get("limit"));
        }
    }

    @Nested
    @DisplayName("ParsedUrl inner class tests")
    class ParsedUrlTests {

        @Test
        @DisplayName("ParsedUrl getPathComponents should return the path array")
        void parsedUrlGetPathComponentsShouldReturnPathArray() {
            String[] components = {"spaces", "123", "pages"};
            Map<String, String> params = Map.of("cursor", "abc");

            UrlParser.ParsedUrl parsedUrl = new UrlParser.ParsedUrl(components, params);

            assertArrayEquals(components, parsedUrl.getPathComponents());
        }

        @Test
        @DisplayName("ParsedUrl getQueryParams should return the query params map")
        void parsedUrlGetQueryParamsShouldReturnQueryParamsMap() {
            String[] components = {"spaces", "123"};
            Map<String, String> params = Map.of("cursor", "abc", "limit", "25");

            UrlParser.ParsedUrl parsedUrl = new UrlParser.ParsedUrl(components, params);

            assertEquals(params, parsedUrl.getQueryParams());
        }

        @Test
        @DisplayName("ParsedUrl should accept null query params")
        void parsedUrlShouldAcceptNullQueryParams() {
            String[] components = {"content", "456"};

            UrlParser.ParsedUrl parsedUrl = new UrlParser.ParsedUrl(components, null);

            assertNotNull(parsedUrl.getPathComponents());
            assertNull(parsedUrl.getQueryParams());
        }

        @Test
        @DisplayName("ParsedUrl should accept null path components")
        void parsedUrlShouldAcceptNullPathComponents() {
            Map<String, String> params = Map.of("key", "value");

            UrlParser.ParsedUrl parsedUrl = new UrlParser.ParsedUrl(null, params);

            assertNull(parsedUrl.getPathComponents());
            assertNotNull(parsedUrl.getQueryParams());
        }
    }

    @Nested
    @DisplayName("Edge case tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle URL with empty query string after ?")
        void shouldHandleUrlWithEmptyQueryString() {
            String url = "/rest/api/content/123?";

            UrlParser.ParsedUrl result = UrlParser.parseNextUrl(url);

            assertNotNull(result);
            // Empty query string results in null query params
            assertNull(result.getQueryParams());
        }

        @Test
        @DisplayName("Should handle query parameter with = in value")
        void shouldHandleQueryParameterWithEqualsInValue() {
            String url = "/rest/api/content/123?cursor=abc=def";

            UrlParser.ParsedUrl result = UrlParser.parseNextUrl(url);

            Map<String, String> params = result.getQueryParams();
            assertNotNull(params);
            assertEquals("abc=def", params.get("cursor"));
        }

        @Test
        @DisplayName("Should handle single query parameter without value")
        void shouldHandleSingleQueryParameterWithoutValue() {
            String url = "/rest/api/content/123?keyonly";

            UrlParser.ParsedUrl result = UrlParser.parseNextUrl(url);

            // keyonly has no = sign, so keyValue.length != 2, should not be added
            assertNull(result.getQueryParams());
        }
    }

    @Test
    @DisplayName("Constructor should be private")
    void constructorShouldBePrivate() throws NoSuchMethodException {
        Constructor<UrlParser> constructor = UrlParser.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()),
                "UrlParser constructor should be private");
    }

    @Test
    @DisplayName("Private constructor should be invocable via reflection")
    void privateConstructorShouldBeInvocableViaReflection() throws Exception {
        Constructor<UrlParser> constructor = UrlParser.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        UrlParser instance = constructor.newInstance();
        assertNotNull(instance);
    }
}
