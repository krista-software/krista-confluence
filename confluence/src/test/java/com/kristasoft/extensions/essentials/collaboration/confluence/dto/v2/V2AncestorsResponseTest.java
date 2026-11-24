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

package com.kristasoft.extensions.essentials.collaboration.confluence.dto.v2;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link V2AncestorsResponse} and its inner class {@link V2AncestorsResponse.V2Links}.
 *
 * Covers: getters/setters for results and links, hasNext(), getNextUrl(),
 *         V2Links inner class (next, base, toString()), toString().
 */
@DisplayName("V2AncestorsResponse DTO")
class V2AncestorsResponseTest {

    // ══════════════════════════════════════════════════════════════════════
    //  Constructor and default values
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("no-arg constructor creates instance with null fields")
    void noArgConstructor_nullFields() {
        V2AncestorsResponse response = new V2AncestorsResponse();

        assertNull(response.getResults());
        assertNull(response.getLinks());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  results getters/setters
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("setResults / getResults round-trip")
    void setResults_getResults_roundTrip() {
        V2AncestorsResponse response = new V2AncestorsResponse();
        List<V2Ancestor> ancestors = Arrays.asList(
                new V2Ancestor("1", "page"),
                new V2Ancestor("2", "page")
        );

        response.setResults(ancestors);

        assertSame(ancestors, response.getResults());
        assertEquals(2, response.getResults().size());
    }

    @Test
    @DisplayName("setResults with empty list")
    void setResults_emptyList() {
        V2AncestorsResponse response = new V2AncestorsResponse();
        response.setResults(new ArrayList<>());

        assertNotNull(response.getResults());
        assertTrue(response.getResults().isEmpty());
    }

    @Test
    @DisplayName("setResults with null")
    void setResults_null() {
        V2AncestorsResponse response = new V2AncestorsResponse();
        response.setResults(null);

        assertNull(response.getResults());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  links getters/setters
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("setLinks / getLinks round-trip")
    void setLinks_getLinks_roundTrip() {
        V2AncestorsResponse response = new V2AncestorsResponse();
        V2AncestorsResponse.V2Links links = new V2AncestorsResponse.V2Links();
        links.setBase("https://example.atlassian.net");

        response.setLinks(links);

        assertSame(links, response.getLinks());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  hasNext()
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("hasNext returns false when links is null")
    void hasNext_linksNull_returnsFalse() {
        V2AncestorsResponse response = new V2AncestorsResponse();

        assertFalse(response.hasNext());
    }

    @Test
    @DisplayName("hasNext returns false when next is null")
    void hasNext_nextNull_returnsFalse() {
        V2AncestorsResponse response = new V2AncestorsResponse();
        V2AncestorsResponse.V2Links links = new V2AncestorsResponse.V2Links();
        links.setNext(null);
        response.setLinks(links);

        assertFalse(response.hasNext());
    }

    @Test
    @DisplayName("hasNext returns false when next is empty string")
    void hasNext_nextEmpty_returnsFalse() {
        V2AncestorsResponse response = new V2AncestorsResponse();
        V2AncestorsResponse.V2Links links = new V2AncestorsResponse.V2Links();
        links.setNext("");
        response.setLinks(links);

        assertFalse(response.hasNext());
    }

    @Test
    @DisplayName("hasNext returns true when next is a non-empty URL")
    void hasNext_nextPresent_returnsTrue() {
        V2AncestorsResponse response = new V2AncestorsResponse();
        V2AncestorsResponse.V2Links links = new V2AncestorsResponse.V2Links();
        links.setNext("/wiki/api/v2/pages/123/ancestors?cursor=abc");
        response.setLinks(links);

        assertTrue(response.hasNext());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  getNextUrl()
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("getNextUrl returns null when links is null")
    void getNextUrl_linksNull_returnsNull() {
        V2AncestorsResponse response = new V2AncestorsResponse();

        assertNull(response.getNextUrl());
    }

    @Test
    @DisplayName("getNextUrl returns null when next is null")
    void getNextUrl_nextNull_returnsNull() {
        V2AncestorsResponse response = new V2AncestorsResponse();
        V2AncestorsResponse.V2Links links = new V2AncestorsResponse.V2Links();
        response.setLinks(links);

        assertNull(response.getNextUrl());
    }

    @Test
    @DisplayName("getNextUrl returns the next URL when present")
    void getNextUrl_nextPresent_returnsUrl() {
        V2AncestorsResponse response = new V2AncestorsResponse();
        V2AncestorsResponse.V2Links links = new V2AncestorsResponse.V2Links();
        links.setNext("/wiki/api/v2/pages/123/ancestors?cursor=xyz");
        response.setLinks(links);

        assertEquals("/wiki/api/v2/pages/123/ancestors?cursor=xyz", response.getNextUrl());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  V2Links inner class
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("V2Links")
    class V2LinksTest {

        @Test
        @DisplayName("setNext / getNext round-trip")
        void setNext_getNext_roundTrip() {
            V2AncestorsResponse.V2Links links = new V2AncestorsResponse.V2Links();
            links.setNext("/api/v2/pages?cursor=abc");

            assertEquals("/api/v2/pages?cursor=abc", links.getNext());
        }

        @Test
        @DisplayName("setBase / getBase round-trip")
        void setBase_getBase_roundTrip() {
            V2AncestorsResponse.V2Links links = new V2AncestorsResponse.V2Links();
            links.setBase("https://example.atlassian.net/wiki");

            assertEquals("https://example.atlassian.net/wiki", links.getBase());
        }

        @Test
        @DisplayName("default values are null")
        void defaultValues_allNull() {
            V2AncestorsResponse.V2Links links = new V2AncestorsResponse.V2Links();

            assertNull(links.getNext());
            assertNull(links.getBase());
        }

        @Test
        @DisplayName("toString contains next and base")
        void toString_containsFields() {
            V2AncestorsResponse.V2Links links = new V2AncestorsResponse.V2Links();
            links.setNext("/next-url");
            links.setBase("https://base.example.com");

            String result = links.toString();

            assertNotNull(result);
            assertTrue(result.startsWith("V2Links{"));
            assertTrue(result.contains("next='/next-url'"));
            assertTrue(result.contains("base='https://base.example.com'"));
            assertTrue(result.endsWith("}"));
        }

        @Test
        @DisplayName("toString with null fields contains null literals")
        void toString_withNullFields() {
            V2AncestorsResponse.V2Links links = new V2AncestorsResponse.V2Links();

            String result = links.toString();

            assertNotNull(result);
            assertTrue(result.startsWith("V2Links{"));
            assertTrue(result.contains("null"));
            assertTrue(result.endsWith("}"));
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  toString on V2AncestorsResponse
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("toString with populated fields")
    void toString_withPopulatedFields() {
        V2AncestorsResponse response = new V2AncestorsResponse();
        response.setResults(Arrays.asList(new V2Ancestor("1", "page")));

        V2AncestorsResponse.V2Links links = new V2AncestorsResponse.V2Links();
        links.setBase("https://example.com");
        response.setLinks(links);

        String result = response.toString();

        assertNotNull(result);
        assertTrue(result.startsWith("V2AncestorsResponse{"));
        assertTrue(result.contains("results="));
        assertTrue(result.contains("links="));
        assertTrue(result.endsWith("}"));
    }

    @Test
    @DisplayName("toString with null fields")
    void toString_withNullFields() {
        V2AncestorsResponse response = new V2AncestorsResponse();

        String result = response.toString();

        assertNotNull(result);
        assertTrue(result.startsWith("V2AncestorsResponse{"));
        assertTrue(result.contains("null"));
        assertTrue(result.endsWith("}"));
    }
}
