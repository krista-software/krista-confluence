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

package com.kristasoft.extensions.essentials.collaboration.confluence.dto.v1;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link V1PageResponse} and its inner class {@link V1PageResponse.V1Links}.
 *
 * Covers: getters/setters for results, start, limit, size, links.
 *         V1Links inner class: base, context, next, self.
 */
@DisplayName("V1PageResponse DTO")
class V1PageResponseTest {

    // ── Helper ──

    private V1Page createV1Page(String id, String title) {
        V1Page page = new V1Page();
        page.setId(id);
        page.setTitle(title);
        return page;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  V1PageResponse top-level getters/setters
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("setResults / getResults round-trip")
    void setResults_getResults_roundTrip() {
        V1PageResponse response = new V1PageResponse();
        List<V1Page> pages = Arrays.asList(createV1Page("1", "Page 1"), createV1Page("2", "Page 2"));

        response.setResults(pages);

        assertSame(pages, response.getResults());
        assertEquals(2, response.getResults().size());
    }

    @Test
    @DisplayName("setResults with empty list")
    void setResults_emptyList() {
        V1PageResponse response = new V1PageResponse();
        response.setResults(new ArrayList<>());

        assertNotNull(response.getResults());
        assertTrue(response.getResults().isEmpty());
    }

    @Test
    @DisplayName("setResults with null")
    void setResults_null() {
        V1PageResponse response = new V1PageResponse();
        response.setResults(null);

        assertNull(response.getResults());
    }

    @Test
    @DisplayName("setStart / getStart round-trip")
    void setStart_getStart_roundTrip() {
        V1PageResponse response = new V1PageResponse();
        response.setStart(25);

        assertEquals(25, response.getStart());
    }

    @Test
    @DisplayName("setLimit / getLimit round-trip")
    void setLimit_getLimit_roundTrip() {
        V1PageResponse response = new V1PageResponse();
        response.setLimit(50);

        assertEquals(50, response.getLimit());
    }

    @Test
    @DisplayName("setSize / getSize round-trip")
    void setSize_getSize_roundTrip() {
        V1PageResponse response = new V1PageResponse();
        response.setSize(10);

        assertEquals(10, response.getSize());
    }

    @Test
    @DisplayName("setLinks / getLinks round-trip")
    void setLinks_getLinks_roundTrip() {
        V1PageResponse response = new V1PageResponse();
        V1PageResponse.V1Links links = new V1PageResponse.V1Links();
        links.setBase("https://example.atlassian.net/wiki");

        response.setLinks(links);

        assertSame(links, response.getLinks());
    }

    @Test
    @DisplayName("default values: start=0, limit=0, size=0, results=null, links=null")
    void defaultValues() {
        V1PageResponse response = new V1PageResponse();

        assertEquals(0, response.getStart());
        assertEquals(0, response.getLimit());
        assertEquals(0, response.getSize());
        assertNull(response.getResults());
        assertNull(response.getLinks());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  V1Links inner class
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("V1Links")
    class V1LinksTest {

        @Test
        @DisplayName("setBase / getBase round-trip")
        void setBase_getBase_roundTrip() {
            V1PageResponse.V1Links links = new V1PageResponse.V1Links();
            links.setBase("https://example.atlassian.net/wiki");

            assertEquals("https://example.atlassian.net/wiki", links.getBase());
        }

        @Test
        @DisplayName("setContext / getContext round-trip")
        void setContext_getContext_roundTrip() {
            V1PageResponse.V1Links links = new V1PageResponse.V1Links();
            links.setContext("/wiki");

            assertEquals("/wiki", links.getContext());
        }

        @Test
        @DisplayName("setNext / getNext round-trip")
        void setNext_getNext_roundTrip() {
            V1PageResponse.V1Links links = new V1PageResponse.V1Links();
            links.setNext("/rest/api/content/123/child/page?start=25&limit=25");

            assertEquals("/rest/api/content/123/child/page?start=25&limit=25", links.getNext());
        }

        @Test
        @DisplayName("setSelf / getSelf round-trip")
        void setSelf_getSelf_roundTrip() {
            V1PageResponse.V1Links links = new V1PageResponse.V1Links();
            links.setSelf("/rest/api/content/123/child/page");

            assertEquals("/rest/api/content/123/child/page", links.getSelf());
        }

        @Test
        @DisplayName("all default values are null")
        void defaultValues_allNull() {
            V1PageResponse.V1Links links = new V1PageResponse.V1Links();

            assertNull(links.getBase());
            assertNull(links.getContext());
            assertNull(links.getNext());
            assertNull(links.getSelf());
        }

        @Test
        @DisplayName("all fields can be set and retrieved together")
        void allFields_setAndRetrieved() {
            V1PageResponse.V1Links links = new V1PageResponse.V1Links();
            links.setBase("https://base.example.com");
            links.setContext("/wiki");
            links.setNext("/rest/api/content?start=50");
            links.setSelf("/rest/api/content");

            assertEquals("https://base.example.com", links.getBase());
            assertEquals("/wiki", links.getContext());
            assertEquals("/rest/api/content?start=50", links.getNext());
            assertEquals("/rest/api/content", links.getSelf());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Full response with all fields populated
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("full response with all fields populated returns correct values")
    void fullResponse_allFieldsPopulated() {
        V1PageResponse response = new V1PageResponse();

        List<V1Page> pages = Arrays.asList(
                createV1Page("1", "First"),
                createV1Page("2", "Second")
        );
        response.setResults(pages);
        response.setStart(0);
        response.setLimit(25);
        response.setSize(2);

        V1PageResponse.V1Links links = new V1PageResponse.V1Links();
        links.setBase("https://example.atlassian.net");
        links.setContext("/wiki");
        links.setSelf("/rest/api/content/100/child/page");
        response.setLinks(links);

        assertEquals(2, response.getResults().size());
        assertEquals("1", response.getResults().get(0).getId());
        assertEquals("Second", response.getResults().get(1).getTitle());
        assertEquals(0, response.getStart());
        assertEquals(25, response.getLimit());
        assertEquals(2, response.getSize());
        assertEquals("https://example.atlassian.net", response.getLinks().getBase());
        assertEquals("/wiki", response.getLinks().getContext());
        assertNull(response.getLinks().getNext());
        assertEquals("/rest/api/content/100/child/page", response.getLinks().getSelf());
    }
}
