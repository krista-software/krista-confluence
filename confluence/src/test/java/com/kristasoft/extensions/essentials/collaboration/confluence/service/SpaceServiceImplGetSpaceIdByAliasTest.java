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
import com.kristasoft.extensions.essentials.collaboration.confluence.client.ConfluenceRESTClient;
import com.kristasoft.extensions.essentials.collaboration.confluence.dto.Space;
import com.kristasoft.extensions.essentials.collaboration.confluence.exception.ConfluenceRESTException;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 100% line + branch coverage for {@link SpaceServiceImpl#getSpaceIdByAlias(String)},
 * its private helper {@code extractNextUrl}, and the {@code resolveSpaceIdFromKey}
 * helper reached through {@link SpaceServiceImpl#getSpaceById(String)}.
 *
 * <h2>Branch map — getSpaceIdByAlias</h2>
 * <pre>
 *   input = alias == null ? "" : alias.trim()
 *     ├─ alias == null                → input ""            (nullAlias)
 *     └─ alias != null                → trimmed             (all others)
 *   if (input.isEmpty())
 *     ├─ T → throw "Space Alias is required"                (nullAlias, blankAlias)
 *     └─ F                                                  (all others)
 *   tildePrefixed = input.startsWith("~") ? null : "~"+input
 *     ├─ starts with ~ → null                               (tildeInputMatch)
 *     └─ no ~          → "~"+input                          (all others)
 *   nextUrl == null (first request) vs != null (paginated)  (pagination test)
 *   statusCode != HTTP_OK → throw "We couldn't search…"     (httpError)
 *   resultObject.has(RESULTS)
 *     ├─ T → iterate                                        (most)
 *     └─ F → skip                                           (noResultsKey)
 *   alias1 != null && (equals(input) || (tilde!=null && equals(tilde)))
 *     ├─ alias1 == null → skip                              (nullAliasEntrySkipped)
 *     ├─ equals(input) → return                             (plainMatch/tildeInputMatch)
 *     ├─ tilde match   → return                             (tildePrefixedMatch)
 *     └─ no match                                           (notFound family)
 *   fall through loop → throw "No space found with alias"   (notFound family)
 * </pre>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SpaceServiceImpl.getSpaceIdByAlias – full coverage")
class SpaceServiceImplGetSpaceIdByAliasTest {

    @Mock private ConfluenceAttributes confluenceAttributes;
    @Mock private ConfluenceRESTClient restClient;

    private SpaceServiceImpl spaceService;

    @BeforeEach
    void setUp() {
        // lenient: the null/blank-alias guard returns before the REST client is ever touched.
        lenient().when(confluenceAttributes.getRestClient()).thenReturn(restClient);
        spaceService = new SpaceServiceImpl(confluenceAttributes);
    }

    /** Builds a stubbed HttpResponse. Must be called and stored BEFORE any doSyncGet stub. */
    private HttpResponse<String> response(int status, String body) {
        @SuppressWarnings("unchecked")
        HttpResponse<String> resp = mock(HttpResponse.class);
        when(resp.statusCode()).thenReturn(status);
        if (body != null) {
            lenient().when(resp.body()).thenReturn(body);
        }
        return resp;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  GUARD BRANCH – null / blank alias
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("null alias → throws 'Space Alias is required'; REST client never called")
    void nullAlias_throws() {
        ConfluenceRESTException ex = assertThrows(ConfluenceRESTException.class,
                () -> spaceService.getSpaceIdByAlias(null));

        assertTrue(ex.getMessage().contains("Space Alias is required"));
        verify(confluenceAttributes, never()).getRestClient();
        verifyNoInteractions(restClient);
    }

    @Test
    @DisplayName("blank alias → throws 'Space Alias is required'; REST client never called")
    void blankAlias_throws() {
        ConfluenceRESTException ex = assertThrows(ConfluenceRESTException.class,
                () -> spaceService.getSpaceIdByAlias("   "));

        assertTrue(ex.getMessage().contains("Space Alias is required"));
        verifyNoInteractions(restClient);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  HTTP ERROR
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("non-200 status → throws 'We couldn't search…' with alias and HTTP code")
    void httpError_throws() {
        HttpResponse<String> resp = response(HttpURLConnection.HTTP_FORBIDDEN, null);
        when(restClient.doSyncGet(eq(APIVersion.V2), eq(Map.of("limit", "250")), eq("spaces")))
                .thenReturn(resp);

        ConfluenceRESTException ex = assertThrows(ConfluenceRESTException.class,
                () -> spaceService.getSpaceIdByAlias("johndoe"));

        assertTrue(ex.getMessage().contains("We couldn't search Confluence spaces"));
        assertTrue(ex.getMessage().contains("johndoe"));
        assertTrue(ex.getMessage().contains("403"));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  SUCCESS – matching paths
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("plain alias equals currentActiveAlias → returns space")
    void plainMatch_returnsSpace() {
        String body = """
                { "results": [
                    { "id": "100", "name": "Marketing", "currentActiveAlias": "MFS" }
                ] }
                """;
        HttpResponse<String> resp = response(HttpURLConnection.HTTP_OK, body);
        when(restClient.doSyncGet(eq(APIVersion.V2), eq(Map.of("limit", "250")), eq("spaces")))
                .thenReturn(resp);

        Space space = spaceService.getSpaceIdByAlias("MFS");

        assertNotNull(space);
        assertEquals("100", space.getId());
        assertEquals("MFS", space.getCurrentActiveAlias());
    }

    @Test
    @DisplayName("tilde-prefixed input matches directly (tildePrefixed == null branch)")
    void tildeInputMatch_returnsSpace() {
        String body = """
                { "results": [
                    { "id": "200", "name": "John Doe", "currentActiveAlias": "~johndoe" }
                ] }
                """;
        HttpResponse<String> resp = response(HttpURLConnection.HTTP_OK, body);
        when(restClient.doSyncGet(eq(APIVersion.V2), eq(Map.of("limit", "250")), eq("spaces")))
                .thenReturn(resp);

        // input starts with "~" → tildePrefixed stays null; match is via alias1.equals(input)
        Space space = spaceService.getSpaceIdByAlias("~johndoe");

        assertNotNull(space);
        assertEquals("200", space.getId());
    }

    @Test
    @DisplayName("input matches via '~'+input (tildePrefixed branch)")
    void tildePrefixedMatch_returnsSpace() {
        String body = """
                { "results": [
                    { "id": "300", "name": "John Doe", "currentActiveAlias": "~johndoe" }
                ] }
                """;
        HttpResponse<String> resp = response(HttpURLConnection.HTTP_OK, body);
        when(restClient.doSyncGet(eq(APIVersion.V2), eq(Map.of("limit", "250")), eq("spaces")))
                .thenReturn(resp);

        // input "johndoe" != "~johndoe", but tildePrefixed "~johndoe" == currentActiveAlias
        Space space = spaceService.getSpaceIdByAlias("johndoe");

        assertNotNull(space);
        assertEquals("300", space.getId());
    }

    @Test
    @DisplayName("entry with null currentActiveAlias skipped; later matching entry returned")
    void nullAliasEntrySkipped_thenMatch() {
        String body = """
                { "results": [
                    { "id": "400", "name": "No Alias" },
                    { "id": "401", "name": "Has Alias", "currentActiveAlias": "MFS" }
                ] }
                """;
        HttpResponse<String> resp = response(HttpURLConnection.HTTP_OK, body);
        when(restClient.doSyncGet(eq(APIVersion.V2), eq(Map.of("limit", "250")), eq("spaces")))
                .thenReturn(resp);

        Space space = spaceService.getSpaceIdByAlias("MFS");

        assertNotNull(space);
        assertEquals("401", space.getId());
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PAGINATION – extractNextUrl → non-null, second request via UrlParser
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("no match on page 1 + _links.next → follows cursor and matches on page 2")
    void pagination_matchOnSecondPage() {
        String page1 = """
                { "results": [
                    { "id": "500", "name": "First", "currentActiveAlias": "OTHER" }
                  ],
                  "_links": { "next": "/wiki/api/v2/spaces?cursor=abc&limit=250" }
                }
                """;
        String page2 = """
                { "results": [
                    { "id": "501", "name": "Second", "currentActiveAlias": "MFS" }
                  ],
                  "_links": {}
                }
                """;
        HttpResponse<String> resp1 = response(HttpURLConnection.HTTP_OK, page1);
        HttpResponse<String> resp2 = response(HttpURLConnection.HTTP_OK, page2);
        when(restClient.doSyncGet(eq(APIVersion.V2), anyMap(), eq("spaces")))
                .thenReturn(resp1)
                .thenReturn(resp2);

        Space space = spaceService.getSpaceIdByAlias("MFS");

        assertNotNull(space);
        assertEquals("501", space.getId());
        verify(restClient, times(2)).doSyncGet(eq(APIVersion.V2), anyMap(), eq("spaces"));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  NOT FOUND – exercises every extractNextUrl exit
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("response without 'results' key → No space found (extractNextUrl: no _links)")
    void noResultsKey_notFound() {
        HttpResponse<String> resp = response(HttpURLConnection.HTTP_OK, "{}");
        when(restClient.doSyncGet(eq(APIVersion.V2), eq(Map.of("limit", "250")), eq("spaces")))
                .thenReturn(resp);

        ConfluenceRESTException ex = assertThrows(ConfluenceRESTException.class,
                () -> spaceService.getSpaceIdByAlias("MFS"));

        assertTrue(ex.getMessage().contains("No space found with alias"));
        assertTrue(ex.getMessage().contains("MFS"));
    }

    @Test
    @DisplayName("no match, _links present but no 'next' key → No space found")
    void linksWithoutNext_notFound() {
        String body = """
                { "results": [ { "id": "600", "currentActiveAlias": "OTHER" } ],
                  "_links": { "base": "https://x" } }
                """;
        HttpResponse<String> resp = response(HttpURLConnection.HTTP_OK, body);
        when(restClient.doSyncGet(eq(APIVersion.V2), eq(Map.of("limit", "250")), eq("spaces")))
                .thenReturn(resp);

        ConfluenceRESTException ex = assertThrows(ConfluenceRESTException.class,
                () -> spaceService.getSpaceIdByAlias("MFS"));

        assertTrue(ex.getMessage().contains("No space found with alias"));
    }

    @Test
    @DisplayName("tilde input, non-matching alias → No space found (tildePrefixed == null, short-circuit right side false)")
    void tildeInput_noMatch_notFound() {
        // input starts with "~" so tildePrefixed stays null; alias1.equals(input) is false,
        // which forces the '(tildePrefixed != null && …)' branch to evaluate to false.
        String body = """
                { "results": [ { "id": "800", "currentActiveAlias": "OTHER" } ] }
                """;
        HttpResponse<String> resp = response(HttpURLConnection.HTTP_OK, body);
        when(restClient.doSyncGet(eq(APIVersion.V2), eq(Map.of("limit", "250")), eq("spaces")))
                .thenReturn(resp);

        ConfluenceRESTException ex = assertThrows(ConfluenceRESTException.class,
                () -> spaceService.getSpaceIdByAlias("~ghost"));

        assertTrue(ex.getMessage().contains("No space found with alias"));
        assertTrue(ex.getMessage().contains("~ghost"));
    }

    @Test
    @DisplayName("no match, _links.next is JSON null → No space found (extractNextUrl: isJsonNull)")
    void linksNextJsonNull_notFound() {
        String body = """
                { "results": [ { "id": "700", "currentActiveAlias": "OTHER" } ],
                  "_links": { "next": null } }
                """;
        HttpResponse<String> resp = response(HttpURLConnection.HTTP_OK, body);
        when(restClient.doSyncGet(eq(APIVersion.V2), eq(Map.of("limit", "250")), eq("spaces")))
                .thenReturn(resp);

        ConfluenceRESTException ex = assertThrows(ConfluenceRESTException.class,
                () -> spaceService.getSpaceIdByAlias("MFS"));

        assertTrue(ex.getMessage().contains("No space found with alias"));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  resolveSpaceIdFromKey – reached via getSpaceById(non-numeric key)
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("resolveSpaceIdFromKey (via getSpaceById)")
    class ResolveSpaceIdFromKey {

        @Test
        @DisplayName("non-200 on key lookup → throws 'We couldn't look up the space by key'")
        void keyLookupHttpError_throws() {
            HttpResponse<String> resp = response(HttpURLConnection.HTTP_UNAVAILABLE, null);
            when(restClient.doSyncGet(eq(APIVersion.V2), eq(Map.of("keys", "MFS")), eq("spaces")))
                    .thenReturn(resp);

            ConfluenceRESTException ex = assertThrows(ConfluenceRESTException.class,
                    () -> spaceService.getSpaceById("MFS"));

            assertTrue(ex.getMessage().contains("We couldn't look up the space by key"));
            assertTrue(ex.getMessage().contains("MFS"));
            assertTrue(ex.getMessage().contains("503"));
        }

        @Test
        @DisplayName("empty results for key → throws 'No space found with key'")
        void keyLookupEmptyResults_throws() {
            HttpResponse<String> resp = response(HttpURLConnection.HTTP_OK, "{ \"results\": [] }");
            when(restClient.doSyncGet(eq(APIVersion.V2), eq(Map.of("keys", "MFS")), eq("spaces")))
                    .thenReturn(resp);

            ConfluenceRESTException ex = assertThrows(ConfluenceRESTException.class,
                    () -> spaceService.getSpaceById("MFS"));

            assertTrue(ex.getMessage().contains("No space found with key"));
            assertTrue(ex.getMessage().contains("MFS"));
        }

        @Test
        @DisplayName("no 'results' key → throws 'No space found with key'")
        void keyLookupNoResultsKey_throws() {
            HttpResponse<String> resp = response(HttpURLConnection.HTTP_OK, "{}");
            when(restClient.doSyncGet(eq(APIVersion.V2), eq(Map.of("keys", "MFS")), eq("spaces")))
                    .thenReturn(resp);

            ConfluenceRESTException ex = assertThrows(ConfluenceRESTException.class,
                    () -> spaceService.getSpaceById("MFS"));

            assertTrue(ex.getMessage().contains("No space found with key"));
        }

        @Test
        @DisplayName("key resolves to id → getSpaceById returns the fetched space")
        void keyResolves_returnsSpace() {
            String keyBody = """
                    { "results": [ { "id": "12345", "key": "MFS", "name": "Marketing" } ] }
                    """;
            String spaceBody = """
                    { "id": "12345", "key": "MFS", "name": "Marketing", "status": "current" }
                    """;
            HttpResponse<String> keyResp = response(HttpURLConnection.HTTP_OK, keyBody);
            HttpResponse<String> spaceResp = response(HttpURLConnection.HTTP_OK, spaceBody);
            when(restClient.doSyncGet(eq(APIVersion.V2), eq(Map.of("keys", "MFS")), eq("spaces")))
                    .thenReturn(keyResp);
            when(restClient.doSyncGet(eq(APIVersion.V2), isNull(), eq("spaces"), eq("12345")))
                    .thenReturn(spaceResp);

            Space space = spaceService.getSpaceById("MFS");

            assertNotNull(space);
            assertEquals("12345", space.getId());
            assertEquals("MFS", space.getKey());
        }
    }
}
