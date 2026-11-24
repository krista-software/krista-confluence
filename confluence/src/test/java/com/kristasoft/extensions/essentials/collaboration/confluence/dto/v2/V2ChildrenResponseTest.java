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
 * Tests for {@link V2ChildrenResponse} and its inner class {@link V2ChildrenResponse.V2Links}.
 *
 * Covers: getters/setters for results and links, hasNext(), getNextUrl(),
 *         V2Links inner class (next, base, toString()), toString().
 */
@DisplayName("V2ChildrenResponse DTO")
class V2ChildrenResponseTest {

    // ── Helper ──

    private V2ChildPage createChildPage(String id, String title) {
        V2ChildPage child = new V2ChildPage();
        child.setId(id);
        child.setTitle(title);
        return child;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Constructor and default values
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("no-arg constructor creates instance with null fields")
    void noArgConstructor_nullFields() {
        V2ChildrenResponse response = new V2ChildrenResponse();

        assertNull(response.getResults());
        assertNull(response.getLinks());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  results getters/setters
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("setResults / getResults round-trip")
    void setResults_getResults_roundTrip() {
        V2ChildrenResponse response = new V2ChildrenResponse();
        List<V2ChildPage> children = Arrays.asList(
                createChildPage("1", "Child 1"),
                createChildPage("2", "Child 2"),
                createChildPage("3", "Child 3")
        );

        response.setResults(children);

        assertSame(children, response.getResults());
        assertEquals(3, response.getResults().size());
    }

    @Test
    @DisplayName("setResults with empty list")
    void setResults_emptyList() {
        V2ChildrenResponse response = new V2ChildrenResponse();
        response.setResults(new ArrayList<>());

        assertNotNull(response.getResults());
        assertTrue(response.getResults().isEmpty());
    }

    @Test
    @DisplayName("setResults with null")
    void setResults_null() {
        V2ChildrenResponse response = new V2ChildrenResponse();
        response.setResults(null);

        assertNull(response.getResults());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  links getters/setters
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("setLinks / getLinks round-trip")
    void setLinks_getLinks_roundTrip() {
        V2ChildrenResponse response = new V2ChildrenResponse();
        V2ChildrenResponse.V2Links links = new V2ChildrenResponse.V2Links();
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
        V2ChildrenResponse response = new V2ChildrenResponse();

        assertFalse(response.hasNext());
    }

    @Test
    @DisplayName("hasNext returns false when next is null")
    void hasNext_nextNull_returnsFalse() {
        V2ChildrenResponse response = new V2ChildrenResponse();
        V2ChildrenResponse.V2Links links = new V2ChildrenResponse.V2Links();
        links.setNext(null);
        response.setLinks(links);

        assertFalse(response.hasNext());
    }

    @Test
    @DisplayName("hasNext returns false when next is empty string")
    void hasNext_nextEmpty_returnsFalse() {
        V2ChildrenResponse response = new V2ChildrenResponse();
        V2ChildrenResponse.V2Links links = new V2ChildrenResponse.V2Links();
        links.setNext("");
        response.setLinks(links);

        assertFalse(response.hasNext());
    }

    @Test
    @DisplayName("hasNext returns true when next is a non-empty URL")
    void hasNext_nextPresent_returnsTrue() {
        V2ChildrenResponse response = new V2ChildrenResponse();
        V2ChildrenResponse.V2Links links = new V2ChildrenResponse.V2Links();
        links.setNext("/wiki/api/v2/pages/123/children?cursor=abc");
        response.setLinks(links);

        assertTrue(response.hasNext());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  getNextUrl()
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("getNextUrl returns null when links is null")
    void getNextUrl_linksNull_returnsNull() {
        V2ChildrenResponse response = new V2ChildrenResponse();

        assertNull(response.getNextUrl());
    }

    @Test
    @DisplayName("getNextUrl returns null when next is null")
    void getNextUrl_nextNull_returnsNull() {
        V2ChildrenResponse response = new V2ChildrenResponse();
        V2ChildrenResponse.V2Links links = new V2ChildrenResponse.V2Links();
        response.setLinks(links);

        assertNull(response.getNextUrl());
    }

    @Test
    @DisplayName("getNextUrl returns the next URL when present")
    void getNextUrl_nextPresent_returnsUrl() {
        V2ChildrenResponse response = new V2ChildrenResponse();
        V2ChildrenResponse.V2Links links = new V2ChildrenResponse.V2Links();
        links.setNext("/wiki/api/v2/pages/456/children?cursor=xyz");
        response.setLinks(links);

        assertEquals("/wiki/api/v2/pages/456/children?cursor=xyz", response.getNextUrl());
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
            V2ChildrenResponse.V2Links links = new V2ChildrenResponse.V2Links();
            links.setNext("/api/v2/pages?cursor=abc");

            assertEquals("/api/v2/pages?cursor=abc", links.getNext());
        }

        @Test
        @DisplayName("setBase / getBase round-trip")
        void setBase_getBase_roundTrip() {
            V2ChildrenResponse.V2Links links = new V2ChildrenResponse.V2Links();
            links.setBase("https://example.atlassian.net/wiki");

            assertEquals("https://example.atlassian.net/wiki", links.getBase());
        }

        @Test
        @DisplayName("default values are null")
        void defaultValues_allNull() {
            V2ChildrenResponse.V2Links links = new V2ChildrenResponse.V2Links();

            assertNull(links.getNext());
            assertNull(links.getBase());
        }

        @Test
        @DisplayName("toString contains next and base")
        void toString_containsFields() {
            V2ChildrenResponse.V2Links links = new V2ChildrenResponse.V2Links();
            links.setNext("/next-page");
            links.setBase("https://base.example.com");

            String result = links.toString();

            assertNotNull(result);
            assertTrue(result.startsWith("V2Links{"));
            assertTrue(result.contains("next='/next-page'"));
            assertTrue(result.contains("base='https://base.example.com'"));
            assertTrue(result.endsWith("}"));
        }

        @Test
        @DisplayName("toString with null fields contains null literals")
        void toString_withNullFields() {
            V2ChildrenResponse.V2Links links = new V2ChildrenResponse.V2Links();

            String result = links.toString();

            assertNotNull(result);
            assertTrue(result.startsWith("V2Links{"));
            assertTrue(result.contains("null"));
            assertTrue(result.endsWith("}"));
        }

        @Test
        @DisplayName("setNext to null clears next")
        void setNext_null_clearsNext() {
            V2ChildrenResponse.V2Links links = new V2ChildrenResponse.V2Links();
            links.setNext("something");
            links.setNext(null);

            assertNull(links.getNext());
        }

        @Test
        @DisplayName("setBase to null clears base")
        void setBase_null_clearsBase() {
            V2ChildrenResponse.V2Links links = new V2ChildrenResponse.V2Links();
            links.setBase("something");
            links.setBase(null);

            assertNull(links.getBase());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  toString on V2ChildrenResponse
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("toString with populated fields")
    void toString_withPopulatedFields() {
        V2ChildrenResponse response = new V2ChildrenResponse();
        response.setResults(Arrays.asList(createChildPage("1", "Child")));

        V2ChildrenResponse.V2Links links = new V2ChildrenResponse.V2Links();
        links.setBase("https://example.com");
        links.setNext("/next");
        response.setLinks(links);

        String result = response.toString();

        assertNotNull(result);
        assertTrue(result.startsWith("V2ChildrenResponse{"));
        assertTrue(result.contains("results="));
        assertTrue(result.contains("links="));
        assertTrue(result.endsWith("}"));
    }

    @Test
    @DisplayName("toString with null fields")
    void toString_withNullFields() {
        V2ChildrenResponse response = new V2ChildrenResponse();

        String result = response.toString();

        assertNotNull(result);
        assertTrue(result.startsWith("V2ChildrenResponse{"));
        assertTrue(result.contains("null"));
        assertTrue(result.endsWith("}"));
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Full response with all fields populated
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("full response with all fields populated returns correct values")
    void fullResponse_allFieldsPopulated() {
        V2ChildrenResponse response = new V2ChildrenResponse();

        V2ChildPage child1 = new V2ChildPage();
        child1.setId("100");
        child1.setStatus("current");
        child1.setTitle("First Child");
        child1.setType("page");
        child1.setSpaceId("sp-1");
        child1.setChildPosition(1);

        V2ChildPage child2 = new V2ChildPage();
        child2.setId("101");
        child2.setTitle("Second Child");

        response.setResults(Arrays.asList(child1, child2));

        V2ChildrenResponse.V2Links links = new V2ChildrenResponse.V2Links();
        links.setBase("https://example.atlassian.net/wiki");
        links.setNext("/wiki/api/v2/pages/99/children?cursor=next123");
        response.setLinks(links);

        assertEquals(2, response.getResults().size());
        assertEquals("100", response.getResults().get(0).getId());
        assertEquals("current", response.getResults().get(0).getStatus());
        assertEquals("First Child", response.getResults().get(0).getTitle());
        assertEquals("page", response.getResults().get(0).getType());
        assertEquals("sp-1", response.getResults().get(0).getSpaceId());
        assertEquals(1, response.getResults().get(0).getChildPosition());
        assertEquals("101", response.getResults().get(1).getId());
        assertTrue(response.hasNext());
        assertEquals("/wiki/api/v2/pages/99/children?cursor=next123", response.getNextUrl());
        assertEquals("https://example.atlassian.net/wiki", response.getLinks().getBase());
    }
}
