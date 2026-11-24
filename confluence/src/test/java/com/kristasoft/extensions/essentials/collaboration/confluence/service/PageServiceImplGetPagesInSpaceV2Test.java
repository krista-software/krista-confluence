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

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kristasoft.extensions.essentials.collaboration.confluence.ConfluenceAttributes;
import com.kristasoft.extensions.essentials.collaboration.confluence.api.APIVersion;
import com.kristasoft.extensions.essentials.collaboration.confluence.client.ConfluenceRESTClient;
import com.kristasoft.extensions.essentials.collaboration.confluence.dto.Page;
import com.kristasoft.extensions.essentials.collaboration.confluence.exception.ConfluenceRESTException;
import com.kristasoft.extensions.essentials.collaboration.confluence.utils.GsonFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.net.HttpURLConnection;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Line + branch coverage for {@link PageServiceImpl#getPagesInSpaceV2(String, boolean, boolean, String)}
 * and every private helper it drives:
 * {@code fetchAllV2PagesInSpace}, {@code filterRestrictedPagesV2}, {@code hasV1ReadRestrictions},
 * {@code principalListNonEmpty}, {@code pageIdOf}, {@code extractV2NextUrl}, {@code truncate}.
 *
 * <p>All helpers are private and exercised through the public method with the relevant flag
 * combinations ({@code excludeRestricted} to reach the filter, {@code excludeContent}/{@code status}
 * to steer the fetch query).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PageServiceImpl.getPagesInSpaceV2 – full coverage")
@SuppressWarnings("unchecked")
class PageServiceImplGetPagesInSpaceV2Test {

    private static final String SPACE_ID = "42";
    private static final String DOMAIN = "https://test.atlassian.net";

    @Mock ConfluenceAttributes confluenceAttributes;
    @Mock ConfluenceRESTClient restClient;
    @Mock SpaceService spaceService;
    @Mock TemplateService templateService;
    @Mock PageHierarchyService pageHierarchyService;

    private PageServiceImpl pageService;

    @BeforeEach
    void setUp() {
        when(confluenceAttributes.getRestClient()).thenReturn(restClient);
        when(confluenceAttributes.getDomainURL()).thenReturn(DOMAIN);
        pageService = new PageServiceImpl(confluenceAttributes, spaceService, templateService, pageHierarchyService);
    }

    // ─── helpers ────────────────────────────────────────────────────────────────

    private HttpResponse<String> resp(int code, String body) {
        HttpResponse<String> r = mock(HttpResponse.class);
        when(r.statusCode()).thenReturn(code);
        when(r.body()).thenReturn(body);
        return r;
    }

    /** Stubs the initial V2 space-pages GET (matches any query map). */
    private void stubInitial(HttpResponse<String> r) {
        when(restClient.doSyncGet(eq(APIVersion.V2), anyMap(), eq("spaces"), eq(SPACE_ID), eq("pages")))
                .thenReturn(r);
    }

    /** Stubs the async v1 restriction-check for a page id with a completed future. */
    private void stubRestriction(String id, HttpResponse<String> r) {
        when(restClient.doAsyncGet(eq(APIVersion.V1), isNull(),
                eq("content"), eq(id), eq("restriction"), eq("byOperation"), eq("read")))
                .thenReturn(CompletableFuture.completedFuture(r));
    }

    /** excludeRestricted=true against a single page id "1" with the given restriction body. */
    private Page[] filterSinglePage(String restrictionBody) {
        stubInitial(resp(HttpURLConnection.HTTP_OK, "{\"results\":[{\"id\":\"1\"}]}"));
        stubRestriction("1", resp(HttpURLConnection.HTTP_OK, restrictionBody));
        return pageService.getPagesInSpaceV2(SPACE_ID, true, true, null);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  GUARD
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("null spaceId → throws 'Space ID is required'")
    void nullSpaceId_throws() {
        ConfluenceRESTException ex = assertThrows(ConfluenceRESTException.class,
                () -> pageService.getPagesInSpaceV2(null, false, true, null));
        assertTrue(ex.getMessage().contains("Space ID is required"));
        verifyNoInteractions(restClient);
    }

    @Test
    @DisplayName("blank spaceId → throws 'Space ID is required'")
    void blankSpaceId_throws() {
        ConfluenceRESTException ex = assertThrows(ConfluenceRESTException.class,
                () -> pageService.getPagesInSpaceV2("   ", false, true, null));
        assertTrue(ex.getMessage().contains("Space ID is required"));
        verifyNoInteractions(restClient);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  FETCH QUERY BUILDING (excludeContent / status)
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("includeContent (excludeContent=false) → body-format=storage sent; pages returned with domain+space set")
    void includeContent_sendsBodyFormat_andPopulatesPages() {
        stubInitial(resp(HttpURLConnection.HTTP_OK,
                "{\"results\":[{\"id\":\"1\",\"title\":\"A\"},{\"id\":\"2\",\"title\":\"B\"}]}"));

        Page[] pages = pageService.getPagesInSpaceV2(SPACE_ID, false, false, null);

        assertEquals(2, pages.length);
        assertEquals(DOMAIN, pages[0].getDomainUrl());
        assertEquals(SPACE_ID, pages[0].getSpaceId());

        ArgumentCaptor<Map<String, String>> cap = ArgumentCaptor.forClass(Map.class);
        verify(restClient).doSyncGet(eq(APIVersion.V2), cap.capture(), eq("spaces"), eq(SPACE_ID), eq("pages"));
        assertEquals("storage", cap.getValue().get("body-format"));
        assertEquals("250", cap.getValue().get("limit"));
    }

    @Test
    @DisplayName("excludeContent=true → body-format omitted")
    void excludeContent_omitsBodyFormat() {
        stubInitial(resp(HttpURLConnection.HTTP_OK, "{\"results\":[{\"id\":\"1\"}]}"));

        pageService.getPagesInSpaceV2(SPACE_ID, false, true, null);

        ArgumentCaptor<Map<String, String>> cap = ArgumentCaptor.forClass(Map.class);
        verify(restClient).doSyncGet(eq(APIVersion.V2), cap.capture(), eq("spaces"), eq(SPACE_ID), eq("pages"));
        assertFalse(cap.getValue().containsKey("body-format"));
    }

    @Test
    @DisplayName("status='current' → sent as status query param")
    void statusProvided_sendsStatusParam() {
        stubInitial(resp(HttpURLConnection.HTTP_OK, "{\"results\":[]}"));

        pageService.getPagesInSpaceV2(SPACE_ID, false, true, "current");

        ArgumentCaptor<Map<String, String>> cap = ArgumentCaptor.forClass(Map.class);
        verify(restClient).doSyncGet(eq(APIVersion.V2), cap.capture(), eq("spaces"), eq(SPACE_ID), eq("pages"));
        assertEquals("current", cap.getValue().get("status"));
    }

    @Test
    @DisplayName("status blank → status param omitted")
    void statusBlank_omitsStatusParam() {
        stubInitial(resp(HttpURLConnection.HTTP_OK, "{\"results\":[]}"));

        pageService.getPagesInSpaceV2(SPACE_ID, false, true, "   ");

        ArgumentCaptor<Map<String, String>> cap = ArgumentCaptor.forClass(Map.class);
        verify(restClient).doSyncGet(eq(APIVersion.V2), cap.capture(), eq("spaces"), eq(SPACE_ID), eq("pages"));
        assertFalse(cap.getValue().containsKey("status"));
    }

    @Test
    @DisplayName("status='any' (case-insensitive) → status param omitted")
    void statusAny_omitsStatusParam() {
        stubInitial(resp(HttpURLConnection.HTTP_OK, "{\"results\":[]}"));

        pageService.getPagesInSpaceV2(SPACE_ID, false, true, "ANY");

        ArgumentCaptor<Map<String, String>> cap = ArgumentCaptor.forClass(Map.class);
        verify(restClient).doSyncGet(eq(APIVersion.V2), cap.capture(), eq("spaces"), eq(SPACE_ID), eq("pages"));
        assertFalse(cap.getValue().containsKey("status"));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  RESULTS PARSING
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("no 'results' key → empty array")
    void noResultsKey_returnsEmpty() {
        stubInitial(resp(HttpURLConnection.HTTP_OK, "{\"_links\":{}}"));
        assertEquals(0, pageService.getPagesInSpaceV2(SPACE_ID, false, true, null).length);
    }

    @Test
    @DisplayName("'results' present but not an array → empty array")
    void resultsNotArray_returnsEmpty() {
        stubInitial(resp(HttpURLConnection.HTTP_OK, "{\"results\":{\"x\":1}}"));
        assertEquals(0, pageService.getPagesInSpaceV2(SPACE_ID, false, true, null).length);
    }

    @Test
    @DisplayName("'results' array with non-object elements → only objects kept")
    void resultsArrayWithNonObjects_skipsNonObjects() {
        stubInitial(resp(HttpURLConnection.HTTP_OK, "{\"results\":[{\"id\":\"1\"},\"str\",7]}"));
        assertEquals(1, pageService.getPagesInSpaceV2(SPACE_ID, false, true, null).length);
    }

    @Test
    @DisplayName("null response body → NPE at body.get (covers responseBody==null branch)")
    void nullResponseBody_throwsNpe() {
        stubInitial(resp(HttpURLConnection.HTTP_OK, null));
        assertThrows(NullPointerException.class,
                () -> pageService.getPagesInSpaceV2(SPACE_ID, false, true, null));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PAGINATION + extractV2NextUrl
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("_links.next non-empty → follows cursor to second page")
    void pagination_twoPages() {
        String page1 = "{\"results\":[{\"id\":\"1\"}]," +
                "\"_links\":{\"next\":\"/wiki/api/v2/spaces/42/pages?cursor=ABC&limit=250\"}}";
        String page2 = "{\"results\":[{\"id\":\"2\"}],\"_links\":{}}";
        HttpResponse<String> r1 = resp(HttpURLConnection.HTTP_OK, page1);
        HttpResponse<String> r2 = resp(HttpURLConnection.HTTP_OK, page2);
        when(restClient.doSyncGet(eq(APIVersion.V2), anyMap(), eq("spaces"), eq(SPACE_ID), eq("pages")))
                .thenReturn(r1)
                .thenReturn(r2);

        Page[] pages = pageService.getPagesInSpaceV2(SPACE_ID, false, true, null);

        assertEquals(2, pages.length);
        verify(restClient, times(2)).doSyncGet(eq(APIVersion.V2), anyMap(), eq("spaces"), eq(SPACE_ID), eq("pages"));
    }

    @Test
    @DisplayName("_links not an object → no pagination")
    void links_notObject_stops() {
        stubInitial(resp(HttpURLConnection.HTTP_OK, "{\"results\":[{\"id\":\"1\"}],\"_links\":\"x\"}"));
        assertEquals(1, pageService.getPagesInSpaceV2(SPACE_ID, false, true, null).length);
    }

    @Test
    @DisplayName("_links present without 'next' → no pagination")
    void links_noNext_stops() {
        stubInitial(resp(HttpURLConnection.HTTP_OK, "{\"results\":[{\"id\":\"1\"}],\"_links\":{\"base\":\"x\"}}"));
        assertEquals(1, pageService.getPagesInSpaceV2(SPACE_ID, false, true, null).length);
    }

    @Test
    @DisplayName("_links.next JSON null → no pagination")
    void nextJsonNull_stops() {
        stubInitial(resp(HttpURLConnection.HTTP_OK, "{\"results\":[{\"id\":\"1\"}],\"_links\":{\"next\":null}}"));
        assertEquals(1, pageService.getPagesInSpaceV2(SPACE_ID, false, true, null).length);
    }

    @Test
    @DisplayName("_links.next not a primitive → no pagination")
    void nextNotPrimitive_stops() {
        stubInitial(resp(HttpURLConnection.HTTP_OK, "{\"results\":[{\"id\":\"1\"}],\"_links\":{\"next\":{}}}"));
        assertEquals(1, pageService.getPagesInSpaceV2(SPACE_ID, false, true, null).length);
    }

    @Test
    @DisplayName("_links.next empty string → no pagination")
    void nextEmpty_stops() {
        stubInitial(resp(HttpURLConnection.HTTP_OK, "{\"results\":[{\"id\":\"1\"}],\"_links\":{\"next\":\"\"}}"));
        assertEquals(1, pageService.getPagesInSpaceV2(SPACE_ID, false, true, null).length);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  HTTP ERROR + truncate
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("non-200 → throws with status + short body (truncate: length<=max)")
    void httpError_shortBody() {
        stubInitial(resp(HttpURLConnection.HTTP_NOT_FOUND, "boom"));
        ConfluenceRESTException ex = assertThrows(ConfluenceRESTException.class,
                () -> pageService.getPagesInSpaceV2(SPACE_ID, false, true, null));
        assertTrue(ex.getMessage().contains("We couldn't retrieve pages"));
        assertTrue(ex.getMessage().contains("404"));
        assertTrue(ex.getMessage().contains("boom"));
    }

    @Test
    @DisplayName("non-200 with null body → truncate returns empty string")
    void httpError_nullBody() {
        stubInitial(resp(HttpURLConnection.HTTP_INTERNAL_ERROR, null));
        ConfluenceRESTException ex = assertThrows(ConfluenceRESTException.class,
                () -> pageService.getPagesInSpaceV2(SPACE_ID, false, true, null));
        assertTrue(ex.getMessage().contains("500"));
    }

    @Test
    @DisplayName("non-200 with body >500 chars → truncate adds ellipsis")
    void httpError_longBody_truncated() {
        String big = "x".repeat(600);
        stubInitial(resp(HttpURLConnection.HTTP_BAD_REQUEST, big));
        ConfluenceRESTException ex = assertThrows(ConfluenceRESTException.class,
                () -> pageService.getPagesInSpaceV2(SPACE_ID, false, true, null));
        assertTrue(ex.getMessage().contains("…"));
        assertFalse(ex.getMessage().contains("x".repeat(600)));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  OUT OF MEMORY paths
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("OOM while reading fetch response body → ConfluenceRESTException OOM message")
    void fetchOom_wrapped() {
        HttpResponse<String> r = mock(HttpResponse.class);
        when(r.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(r.body()).thenThrow(new OutOfMemoryError("heap"));
        when(restClient.doSyncGet(eq(APIVersion.V2), anyMap(), eq("spaces"), eq(SPACE_ID), eq("pages")))
                .thenReturn(r);

        ConfluenceRESTException ex = assertThrows(ConfluenceRESTException.class,
                () -> pageService.getPagesInSpaceV2(SPACE_ID, false, true, null));
        assertTrue(ex.getMessage().contains("too large to fit in memory"));
        assertTrue(ex.getMessage().contains(SPACE_ID));
    }

    @Test
    @DisplayName("OOM while materializing Page DTOs → ConfluenceRESTException OOM message")
    void materializeOom_wrapped() {
        stubInitial(resp(HttpURLConnection.HTTP_OK, "{\"results\":[{\"id\":\"1\"}]}"));

        Gson delegate = new Gson();
        Gson mockGson = mock(Gson.class);
        // fetch parse (String → JsonObject) delegates to a real Gson so pagination logic works…
        when(mockGson.fromJson(anyString(), eq(JsonObject.class)))
                .thenAnswer(inv -> delegate.fromJson((String) inv.getArgument(0), JsonObject.class));
        // …but materializing each Page DTO (JsonElement → Page) blows up with OOM.
        when(mockGson.fromJson(any(JsonElement.class), eq(Page.class)))
                .thenThrow(new OutOfMemoryError("dto"));

        try (var gf = mockStatic(GsonFactory.class)) {
            gf.when(GsonFactory::getInstance).thenReturn(mockGson);
            ConfluenceRESTException ex = assertThrows(ConfluenceRESTException.class,
                    () -> pageService.getPagesInSpaceV2(SPACE_ID, false, true, null));
            assertTrue(ex.getMessage().contains("too large to fit in memory"));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  filterRestrictedPagesV2  (excludeRestricted = true)
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("filterRestrictedPagesV2")
    class Filter {

        @Test
        @DisplayName("empty page list → early return, no restriction call")
        void emptyPages_earlyReturn() {
            stubInitial(resp(HttpURLConnection.HTTP_OK, "{\"results\":[]}"));
            Page[] pages = pageService.getPagesInSpaceV2(SPACE_ID, true, true, null);
            assertEquals(0, pages.length);
            verify(restClient, never()).doAsyncGet(any(), any(), any(String[].class));
        }

        @Test
        @DisplayName("open page (no restrictions) → kept")
        void openPage_kept() {
            Page[] pages = filterSinglePage(openBody());
            assertEquals(1, pages.length);
        }

        @Test
        @DisplayName("page with user read-restriction → filtered out")
        void restrictedPage_dropped() {
            Page[] pages = filterSinglePage(
                    "{\"restrictions\":{\"user\":{\"size\":1},\"group\":{\"size\":0}}}");
            assertEquals(0, pages.length);
        }

        @Test
        @DisplayName("page with no id → kept defensively, no restriction call for it")
        void nullIdPage_kept() {
            stubInitial(resp(HttpURLConnection.HTTP_OK, "{\"results\":[{}]}"));
            Page[] pages = pageService.getPagesInSpaceV2(SPACE_ID, true, true, null);
            assertEquals(1, pages.length);
        }

        @Test
        @DisplayName("page whose id is not a primitive → pageIdOf returns null (kept by filter, fails later on materialization)")
        void nonPrimitiveId_pageIdOfNull() {
            // A non-primitive "id" exercises pageIdOf's `id.isJsonPrimitive()` false branch during
            // filtering; the page is then kept, but materializing it into a Page (whose id is a String)
            // fails — the production code does not guard this, so a RuntimeException propagates.
            stubInitial(resp(HttpURLConnection.HTTP_OK, "{\"results\":[{\"id\":{\"nested\":1}}]}"));
            assertThrows(RuntimeException.class,
                    () -> pageService.getPagesInSpaceV2(SPACE_ID, true, true, null));
        }

        @Test
        @DisplayName("restriction future completes exceptionally → kept defensively")
        void futureExceptional_kept() {
            stubInitial(resp(HttpURLConnection.HTTP_OK, "{\"results\":[{\"id\":\"1\"}]}"));
            when(restClient.doAsyncGet(eq(APIVersion.V1), isNull(),
                    eq("content"), eq("1"), eq("restriction"), eq("byOperation"), eq("read")))
                    .thenReturn(CompletableFuture.failedFuture(new RuntimeException("net")));
            Page[] pages = pageService.getPagesInSpaceV2(SPACE_ID, true, true, null);
            assertEquals(1, pages.length);
        }

        @Test
        @DisplayName("restriction async returns null response → kept defensively")
        void nullResponseFuture_kept() {
            stubInitial(resp(HttpURLConnection.HTTP_OK, "{\"results\":[{\"id\":\"1\"}]}"));
            when(restClient.doAsyncGet(eq(APIVersion.V1), isNull(),
                    eq("content"), eq("1"), eq("restriction"), eq("byOperation"), eq("read")))
                    .thenReturn(CompletableFuture.completedFuture(null));
            Page[] pages = pageService.getPagesInSpaceV2(SPACE_ID, true, true, null);
            assertEquals(1, pages.length);
        }

        @Test
        @DisplayName("restriction check non-200 → kept defensively")
        void restrictionNon200_kept() {
            stubInitial(resp(HttpURLConnection.HTTP_OK, "{\"results\":[{\"id\":\"1\"}]}"));
            stubRestriction("1", resp(HttpURLConnection.HTTP_FORBIDDEN, "nope"));
            Page[] pages = pageService.getPagesInSpaceV2(SPACE_ID, true, true, null);
            assertEquals(1, pages.length);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  filterRestrictedPagesV2 — inherited restrictions (integration: full space
    //  fetch → explicit-restriction check → parentId ancestor walk, end to end)
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("filterRestrictedPagesV2 — inherited restrictions")
    class InheritedRestrictions {

        /** Space of 3 pages: "1" (root, restricted) → "2" (child, open) → "3" (grandchild, open). */
        private void stubHierarchy(String restrictedId, String... openIds) {
            StringBuilder results = new StringBuilder("{\"results\":[");
            results.append("{\"id\":\"1\",\"parentId\":null},");
            results.append("{\"id\":\"2\",\"parentId\":\"1\"},");
            results.append("{\"id\":\"3\",\"parentId\":\"2\"}");
            results.append("]}");
            stubInitial(resp(HttpURLConnection.HTTP_OK, results.toString()));

            stubRestriction(restrictedId,
                    resp(HttpURLConnection.HTTP_OK, "{\"restrictions\":{\"user\":{\"size\":1}}}"));
            for (String id : openIds) {
                stubRestriction(id, resp(HttpURLConnection.HTTP_OK, openBody()));
            }
        }

        @Test
        @DisplayName("child of an explicitly-restricted parent, with no restriction of its own → filtered out")
        void childOfRestrictedParent_filteredOut() {
            stubHierarchy("1", "2", "3");

            Page[] pages = pageService.getPagesInSpaceV2(SPACE_ID, true, true, null);

            assertEquals(0, pages.length,
                    "page 1 is explicitly restricted; pages 2 and 3 inherit it via the parentId chain");
        }

        @Test
        @DisplayName("grandchild two levels below an explicitly-restricted ancestor → filtered out")
        void grandchildOfRestrictedAncestor_filteredOut() {
            // Only page "3" (the grandchild) is asserted directly; verifies the ancestor
            // walk continues past the immediate parent, not just one hop up.
            stubHierarchy("1", "2", "3");

            Page[] pages = pageService.getPagesInSpaceV2(SPACE_ID, true, true, null);

            assertTrue(java.util.Arrays.stream(pages).noneMatch(p -> "3".equals(p.getId())),
                    "page 3's grandparent (page 1) is restricted, so page 3 must be excluded too");
        }

        @Test
        @DisplayName("sibling under an unrestricted parent is kept even when an unrelated branch is restricted")
        void unrelatedBranch_kept() {
            // Two independent trees in the same space: "1" (restricted root) with child "2",
            // and a separate, unrestricted root "10" with child "11". Only the "1" branch
            // should be filtered; "10"/"11" must survive untouched.
            String results = "{\"results\":[" +
                    "{\"id\":\"1\",\"parentId\":null}," +
                    "{\"id\":\"2\",\"parentId\":\"1\"}," +
                    "{\"id\":\"10\",\"parentId\":null}," +
                    "{\"id\":\"11\",\"parentId\":\"10\"}" +
                    "]}";
            stubInitial(resp(HttpURLConnection.HTTP_OK, results));
            stubRestriction("1", resp(HttpURLConnection.HTTP_OK, "{\"restrictions\":{\"user\":{\"size\":1}}}"));
            stubRestriction("2", resp(HttpURLConnection.HTTP_OK, openBody()));
            stubRestriction("10", resp(HttpURLConnection.HTTP_OK, openBody()));
            stubRestriction("11", resp(HttpURLConnection.HTTP_OK, openBody()));

            Page[] pages = pageService.getPagesInSpaceV2(SPACE_ID, true, true, null);

            java.util.List<String> ids = java.util.Arrays.stream(pages).map(Page::getId).toList();
            assertEquals(java.util.List.of("10", "11"), ids);
        }

        @Test
        @DisplayName("cyclic parentId references do not infinite-loop and default to kept")
        void cyclicParentChain_doesNotHang() {
            // Malformed data: "1" → parent "2" → parent "1". Neither page is explicitly
            // restricted, so both must be kept and the walk must terminate via the
            // visited-set cycle guard rather than looping forever.
            String results = "{\"results\":[" +
                    "{\"id\":\"1\",\"parentId\":\"2\"}," +
                    "{\"id\":\"2\",\"parentId\":\"1\"}" +
                    "]}";
            stubInitial(resp(HttpURLConnection.HTTP_OK, results));
            stubRestriction("1", resp(HttpURLConnection.HTTP_OK, openBody()));
            stubRestriction("2", resp(HttpURLConnection.HTTP_OK, openBody()));

            Page[] pages = assertTimeoutPreemptively(java.time.Duration.ofSeconds(5),
                    () -> pageService.getPagesInSpaceV2(SPACE_ID, true, true, null));

            assertEquals(2, pages.length);
        }

        @Test
        @DisplayName("ancestor explicitly restricted AND child explicitly restricted → child still filtered once")
        void bothExplicitlyRestricted_filteredOnce() {
            String results = "{\"results\":[" +
                    "{\"id\":\"1\",\"parentId\":null}," +
                    "{\"id\":\"2\",\"parentId\":\"1\"}" +
                    "]}";
            stubInitial(resp(HttpURLConnection.HTTP_OK, results));
            stubRestriction("1", resp(HttpURLConnection.HTTP_OK, "{\"restrictions\":{\"user\":{\"size\":1}}}"));
            stubRestriction("2", resp(HttpURLConnection.HTTP_OK, "{\"restrictions\":{\"user\":{\"size\":1}}}"));

            Page[] pages = pageService.getPagesInSpaceV2(SPACE_ID, true, true, null);

            assertEquals(0, pages.length);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  hasV1ReadRestrictions
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("hasV1ReadRestrictions")
    class Restrictions {

        @Test
        @DisplayName("empty body → Gson returns null json → not restricted (kept)")
        void emptyBodyNullJson_kept() {
            // Gson.fromJson("", JsonObject.class) returns null, exercising the `json == null` guard.
            assertEquals(1, filterSinglePage("").length);
        }

        @Test
        @DisplayName("no 'restrictions' key → not restricted (kept)")
        void noRestrictionsKey_kept() {
            assertEquals(1, filterSinglePage("{\"operation\":\"read\"}").length);
        }

        @Test
        @DisplayName("'restrictions' not an object → not restricted (kept)")
        void restrictionsNotObject_kept() {
            assertEquals(1, filterSinglePage("{\"restrictions\":[]}").length);
        }

        @Test
        @DisplayName("malformed JSON body → parse error → not restricted (kept)")
        void malformedBody_kept() {
            assertEquals(1, filterSinglePage("{not valid json").length);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  principalListNonEmpty (via restriction bodies)
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("principalListNonEmpty")
    class Principal {

        @Test
        @DisplayName("group size>0 (user open) → restricted")
        void groupSizePositive_dropped() {
            assertEquals(0, filterSinglePage(
                    "{\"restrictions\":{\"user\":{\"size\":0,\"results\":[]},\"group\":{\"size\":3}}}").length);
        }

        @Test
        @DisplayName("user totalSize>0 → restricted")
        void userTotalSizePositive_dropped() {
            assertEquals(0, filterSinglePage(
                    "{\"restrictions\":{\"user\":{\"size\":0,\"totalSize\":2}}}").length);
        }

        @Test
        @DisplayName("user results non-empty (no size/totalSize) → restricted")
        void userResultsNonEmpty_dropped() {
            assertEquals(0, filterSinglePage(
                    "{\"restrictions\":{\"user\":{\"results\":[{\"name\":\"x\"}]}}}").length);
        }

        @Test
        @DisplayName("user absent, group open → not restricted (element==null)")
        void userAbsentGroupOpen_kept() {
            assertEquals(1, filterSinglePage(
                    "{\"restrictions\":{\"group\":{\"size\":0,\"results\":[]}}}").length);
        }

        @Test
        @DisplayName("user JSON null → not restricted")
        void userJsonNull_kept() {
            assertEquals(1, filterSinglePage(
                    "{\"restrictions\":{\"user\":null,\"group\":{\"size\":0}}}").length);
        }

        @Test
        @DisplayName("user not an object (array) → not restricted")
        void userNotObject_kept() {
            assertEquals(1, filterSinglePage(
                    "{\"restrictions\":{\"user\":[],\"group\":{\"size\":0}}}").length);
        }

        @Test
        @DisplayName("size present but not primitive → ignored; totalSize/results absent → not restricted")
        void sizeNotPrimitive_kept() {
            assertEquals(1, filterSinglePage(
                    "{\"restrictions\":{\"user\":{\"size\":{}}}}").length);
        }

        @Test
        @DisplayName("totalSize present but not primitive → ignored → not restricted")
        void totalSizeNotPrimitive_kept() {
            assertEquals(1, filterSinglePage(
                    "{\"restrictions\":{\"user\":{\"size\":0,\"totalSize\":[]}}}").length);
        }

        @Test
        @DisplayName("results present but not an array → not restricted")
        void resultsNotArray_kept() {
            assertEquals(1, filterSinglePage(
                    "{\"restrictions\":{\"user\":{\"size\":0,\"results\":{}}}}").length);
        }

        @Test
        @DisplayName("results empty array → not restricted")
        void resultsEmptyArray_kept() {
            assertEquals(1, filterSinglePage(
                    "{\"restrictions\":{\"user\":{\"size\":0,\"totalSize\":0,\"results\":[]}}}").length);
        }
    }

    private static String openBody() {
        return "{\"operation\":\"read\",\"restrictions\":{" +
                "\"user\":{\"results\":[],\"size\":0}," +
                "\"group\":{\"results\":[],\"size\":0}}}";
    }
}
