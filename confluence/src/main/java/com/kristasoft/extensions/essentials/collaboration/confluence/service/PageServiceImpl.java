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

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.kristasoft.extensions.essentials.collaboration.confluence.ConfluenceAttributes;
import com.kristasoft.extensions.essentials.collaboration.confluence.api.APIVersion;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.entities.Content;
import com.kristasoft.extensions.essentials.collaboration.confluence.constants.ConfluenceConstants;
import com.kristasoft.extensions.essentials.collaboration.confluence.dto.Page;
import com.kristasoft.extensions.essentials.collaboration.confluence.dto.Space;
import com.kristasoft.extensions.essentials.collaboration.confluence.dto.Template;
import com.kristasoft.extensions.essentials.collaboration.confluence.exception.ConfluenceRESTException;
import com.kristasoft.extensions.essentials.collaboration.confluence.utils.Constants;
import com.kristasoft.extensions.essentials.collaboration.confluence.utils.GsonFactory;
import com.kristasoft.extensions.essentials.collaboration.confluence.utils.PageBodyGenerator;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.net.HttpURLConnection;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import com.google.gson.JsonArray;
import com.kristasoft.extensions.essentials.collaboration.confluence.utils.UrlParser;
import com.google.gson.JsonElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * Implementation of PageService for managing Confluence pages.
 * Handles page CRUD operations.
 */
@Service
@ContractsProvided(PageService.class)
public class PageServiceImpl implements PageService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PageServiceImpl.class);
    private static final String V2_PAGE_LIMIT = "250";

    private final ConfluenceAttributes confluenceAttributes;
    private final SpaceService spaceService;
    private final TemplateService templateService;
    private final PageHierarchyService pageHierarchyService;
    private final PageBodyGenerator pageBodyGenerator;
    private final String domainUrl;

    @Inject
    public PageServiceImpl(ConfluenceAttributes confluenceAttributes, 
                          SpaceService spaceService,
                          TemplateService templateService,
                          PageHierarchyService pageHierarchyService) {
        this.confluenceAttributes = confluenceAttributes;
        this.spaceService = spaceService;
        this.templateService = templateService;
        this.pageHierarchyService = pageHierarchyService;
        this.pageBodyGenerator = new PageBodyGenerator();
        this.domainUrl = confluenceAttributes.getDomainURL();
    }

    @Override
    public Page[] getPagesBySpaceId(String spaceId) {
        Space space = spaceService.getSpaceById(spaceId);
        Optional<String> spaceKey = Optional.ofNullable(space.getKey());
        if (spaceKey.isPresent()) {
            return Stream.of(ConfluenceConstants.DRAFT, ConfluenceConstants.CURRENT).map(status -> confluenceAttributes.getRestClient().doAsyncGet(APIVersion.V1, Map.of("body-format", ConfluenceConstants.STORAGE,
                            "spaceKey", spaceKey.get(),
                            "type", "page",
                            ConfluenceConstants.STATUS, status,
                            "limit", "1000",
                            "expand", "body.storage"), "content"))
                    .map(CompletableFuture::join)
                    .map(response -> {
                        if (response.statusCode() == HttpURLConnection.HTTP_OK) {
                            JsonObject resultObject = GsonFactory.getInstance().fromJson(response.body(), JsonObject.class);
                            Page[] pages = GsonFactory.getInstance().fromJson(GsonFactory.getInstance().toJson(resultObject.get(ConfluenceConstants.RESULTS)),
                                    TypeToken.getArray(Page.class).getType());
                            return Arrays.stream(pages)
                                    .peek(page -> page.setDomainUrl(domainUrl))
                                    .peek(page -> page.setSpaceId(spaceId)).toArray(Page[]::new);
                        } else return new Page[0];
                    }).flatMap(Arrays::stream).toArray(Page[]::new);
        } else {
            throw new ConfluenceRESTException(String.format("""
                    We couldn't find the space with ID '%s'.
                    This could mean:
                    • The space doesn't have a valid key
                    • The space was deleted or moved
                    • You don't have permission to access this space

                    What to do next:
                    1. Use 'Get Spaces' to see available spaces
                    2. Verify you have the correct Space ID
                    3. Check your space permissions""", spaceId));
        }
    }

    @Override
    public Page getPageById(String id) {
        HttpResponse<String> response = confluenceAttributes.getRestClient().doSyncGet(APIVersion.V2, Map.of("body-format", ConfluenceConstants.STORAGE), "pages", id);
        if (response.statusCode() == HttpURLConnection.HTTP_OK) {
            Page page = GsonFactory.getInstance().fromJson(response.body(), Page.class);
            page.setDomainUrl(domainUrl);
            return page;
        } else {
            throw new ConfluenceRESTException("Failed to retrieve page by ID: " + id + ". Status: " + response.statusCode() + ", Response: " + response.body());
        }
    }

    @Override
    public Page createPageFromTemplate(String spaceId, String templateId, String title, List<Map<String, Object>> variables) {
        Template template = templateService.getTemplateById(templateId);
        Map<String, Object> body = pageBodyGenerator.generateBodyForPageRequest(spaceId, template, title, variables);
        HttpResponse<String> response = confluenceAttributes.getRestClient().doSyncPost(APIVersion.V2, body, ConfluenceConstants.PAGES);
        if (response.statusCode() == HttpURLConnection.HTTP_OK) {
            Page page = GsonFactory.getInstance().fromJson(response.body(), Page.class);
            page.setDomainUrl(domainUrl);
            return page;
        } else {
            String errorMessage = String.format("""
                    We couldn't create your Confluence page from the template.
                    This could mean:
                    • The space or template doesn't exist
                    • You don't have permission to create pages in this space
                    • Required fields are missing or invalid
                    • There's a temporary service issue

                    What to do next:
                    1. Verify the space exists and you have edit permissions
                    2. Check that all required fields are provided
                    3. Try creating the page again

                    Technical details: HTTP %d""", response.statusCode());
            throw new ConfluenceRESTException(errorMessage);
        }
    }

    @Override
    public Page createPageFromContent(String spaceId, String title, List<Content> contents) {
        Map<String, Object> body = pageBodyGenerator.generateBodyForPageRequest(spaceId, title, contents);
        HttpResponse<String> response = confluenceAttributes.getRestClient().doSyncPost(APIVersion.V2, body, ConfluenceConstants.PAGES);
        if (response.statusCode() == HttpURLConnection.HTTP_OK) {
            Page page = GsonFactory.getInstance().fromJson(response.body(), Page.class);
            page.setDomainUrl(domainUrl);
            return page;
        } else {
            String errorMessage = String.format("""
                    We couldn't create your Confluence page from the content.
                    This could mean:
                    • The space doesn't exist
                    • You don't have permission to create pages in this space
                    • The content format is invalid
                    • There's a temporary service issue

                    What to do next:
                    1. Verify the space exists and you have edit permissions
                    2. Check that the content is properly formatted
                    3. Try creating the page again

                    Technical details: HTTP %d""", response.statusCode());
            throw new ConfluenceRESTException(errorMessage);
        }
    }

    @Override
    public Page updatePageFromTemplate(String id, String templateId, String status, String title, String comment, List<Map<String, Object>> variables) {
        Page page = getPageById(id != null ? id : "0");
        Template template = templateService.getTemplateById(templateId != null ? templateId : "0");
        Map<String, Object> body = pageBodyGenerator.generateBodyForPageUpdate(page, template, status, title, comment, variables);
        HttpResponse<String> response = confluenceAttributes.getRestClient().doSyncPut(APIVersion.V2, body, ConfluenceConstants.PAGES, id);
        if (response.statusCode() == HttpURLConnection.HTTP_OK) {
            Page update = GsonFactory.getInstance().fromJson(response.body(), Page.class);
            update.setDomainUrl(domainUrl);
            return update;
        } else {
            throw new ConfluenceRESTException("Failed to update page from template. Status: " + response.statusCode() + ", Response: " + response.body());
        }
    }

    @Override
    public Page updatePageFromContent(String id, String status, String title, String comment, List<Content> contents) {
        Page page = getPageById(id != null ? id : "0");
        Map<String, Object> body = pageBodyGenerator.generateBodyForPageUpdate(page, status, title, comment, contents);
        HttpResponse<String> response = confluenceAttributes.getRestClient().doSyncPut(APIVersion.V2, body, ConfluenceConstants.PAGES, id);
        if (response.statusCode() == HttpURLConnection.HTTP_OK) {
            Page update = GsonFactory.getInstance().fromJson(response.body(), Page.class);
            update.setDomainUrl(domainUrl);
            return update;
        } else {
            throw new ConfluenceRESTException("Failed to update page from content. Status: " + response.statusCode() + ", Response: " + response.body());
        }
    }

    @Override
    public Page[] getDirectChildPagesOfSpace(String spaceId) {
        return pageHierarchyService.getDirectChildPagesOfSpace(spaceId);
    }

    @Override
    public Page[] getDirectChildPagesOfPage(String pageId) {
        return pageHierarchyService.getDirectChildPagesOfPage(pageId);
    }

    @Override
    public boolean hasChildPages(String pageId) {
        return pageHierarchyService.hasChildPages(pageId);
    }

    @Override
    public boolean hasChildren(String contentId, String contentType) {
        return pageHierarchyService.hasChildren(contentId, contentType);
    }

    @Override
    public Page[] getDirectChildrenOfContent(String contentId, String contentType) {
        return pageHierarchyService.getDirectChildrenOfContent(contentId, contentType);
    }

    @Override
    public Page[] getAncestorsOfContent(String contentId, String contentType) {
        return pageHierarchyService.getAncestorsOfContent(contentId, contentType);
    }

    @Override
    public Page[] getPagesInSpaceV2(String spaceId, boolean excludeRestricted, boolean excludeContent,
                                    String status) {
        if (spaceId == null || spaceId.trim().isEmpty()) {
            throw new ConfluenceRESTException("Space ID is required.");
        }

        String trimmedSpaceId = spaceId.trim();

        if (!excludeContent) {
            LOGGER.warn("→ getPagesInSpaceV2: Include Page Content is ticked for space {} — every page body will be loaded. " +
                            "Spaces with many or large pages may exhaust the heap; if this fails, re-run with Include Page Content unticked.",
                    spaceId);
        }

        // Filtering by status server-side keeps non-matching pages out of the fetch entirely, so
        // they never reach the restriction check either.
        List<JsonObject> rawPages = fetchAllV2PagesInSpace(trimmedSpaceId, excludeContent, status);
        LOGGER.info("→ getPagesInSpaceV2: Fetched {} pages from space {}, excludeRestricted={}, excludeContent={}, status={}",
                rawPages.size(), spaceId, excludeRestricted, excludeContent, status);

        List<JsonObject> kept = excludeRestricted ? filterRestrictedPagesV2(rawPages) : rawPages;
        LOGGER.info("→ getPagesInSpaceV2: Returning {} pages after filtering", kept.size());

        try {
            return kept.stream()
                    .map(json -> {
                        Page page = GsonFactory.getInstance().fromJson(json, Page.class);
                        page.setDomainUrl(domainUrl);
                        page.setSpaceId(spaceId);
                        return page;
                    })
                    .toArray(Page[]::new);
        } catch (OutOfMemoryError oom) {
            LOGGER.error("→ getPagesInSpaceV2: OutOfMemoryError while materializing {} Page DTOs for space {} " +
                            "(excludeContent={}, excludeRestricted={}). Untick Include Page Content to drop page bodies " +
                            "from the response.",
                    kept.size(), spaceId, excludeContent, excludeRestricted);
            throw new ConfluenceRESTException(
                    String.format(Constants.PAGES_IN_SPACE_V2_OOM_MESSAGE, spaceId));
        }
    }

    /**
     * Paginate {@code GET /wiki/api/v2/spaces/{spaceId}/pages?limit=250}, optionally
     * passing {@code body-format=storage} to include each page's storage-format body.
     * Follows {@code _links.next} cursors. Throws on any non-200 — does not silently
     * swallow errors as an empty array.
     *
     * <p>When {@code excludeContent} is {@code true}, {@code body-format} is omitted so
     * Confluence returns pages without body content — needed to keep memory bounded for
     * large spaces.
     *
     * <p>When {@code status} is non-blank and not {@code "any"}, it is sent as the {@code status}
     * query parameter so Confluence returns only pages of that status. The cursor in
     * {@code _links.next} carries the parameter forward across pages.
     *
     * <p>An {@link OutOfMemoryError} during this loop (usually triggered by an oversized
     * response body buffer or by accumulated {@link JsonObject} trees) is caught and
     * surfaced as a {@link ConfluenceRESTException} naming the {@code Include Page Content}
     * flag so the caller learns the concrete remediation rather than seeing an opaque
     * framework error.
     */
    private List<JsonObject> fetchAllV2PagesInSpace(String spaceId, boolean excludeContent, String status) {
        Map<String, String> initialQuery = new LinkedHashMap<>();
        if (!excludeContent) {
            initialQuery.put("body-format", ConfluenceConstants.STORAGE);
        }
        if (status != null && !status.isBlank() && !"any".equalsIgnoreCase(status)) {
            initialQuery.put(ConfluenceConstants.STATUS, status);
        }
        initialQuery.put("limit", V2_PAGE_LIMIT);

        List<JsonObject> all = new ArrayList<>();
        long totalResponseChars = 0L;
        int batchIndex = 0;
        String nextUrl = null;
        try {
            do {
                HttpResponse<String> response;
                if (nextUrl == null) {
                    response = confluenceAttributes.getRestClient().doSyncGet(
                            APIVersion.V2,
                            initialQuery,
                            "spaces", spaceId, "pages");
                } else {
                    UrlParser.ParsedUrl parsed = UrlParser.parseNextUrl(nextUrl);
                    response = confluenceAttributes.getRestClient().doSyncGet(
                            APIVersion.V2, parsed.getQueryParams(), parsed.getPathComponents());
                }

                if (response.statusCode() != HttpURLConnection.HTTP_OK) {
                    throw new ConfluenceRESTException(String.format("""
                            We couldn't retrieve pages from the space.
                            This could mean:
                            • The Space ID is incorrect or the space was deleted
                            • You don't have permission to view pages in this space
                            • Your Confluence connection needs to be refreshed

                            Technical details: HTTP %d — %s""",
                            response.statusCode(), truncate(response.body(), 500)));
                }

                String responseBody = response.body();
                int responseChars = responseBody == null ? 0 : responseBody.length();
                totalResponseChars += responseChars;
                batchIndex++;

                JsonObject body = GsonFactory.getInstance().fromJson(responseBody, JsonObject.class);
                JsonElement results = body.get(ConfluenceConstants.RESULTS);
                int batchPageCount = 0;
                if (results != null && results.isJsonArray()) {
                    for (JsonElement element : results.getAsJsonArray()) {
                        if (element.isJsonObject()) {
                            all.add(element.getAsJsonObject());
                            batchPageCount++;
                        }
                    }
                }
                LOGGER.info("→ fetchAllV2PagesInSpace: batch {} for space {} — {} pages, response body ≈ {} chars " +
                                "(running totals: {} pages, ≈ {} chars; excludeContent={})",
                        batchIndex, spaceId, batchPageCount, responseChars, all.size(), totalResponseChars, excludeContent);

                nextUrl = extractV2NextUrl(body);
            } while (nextUrl != null);
            return all;
        } catch (OutOfMemoryError oom) {
            LOGGER.error("→ fetchAllV2PagesInSpace: OutOfMemoryError fetching pages for space {} after batch {} " +
                            "({} pages, ≈ {} chars accumulated; excludeContent={}). " +
                            "Untick Include Page Content (omits body-format=storage) and re-run.",
                    spaceId, batchIndex, all.size(), totalResponseChars, excludeContent);
            throw new ConfluenceRESTException(
                    String.format(Constants.PAGES_IN_SPACE_V2_OOM_MESSAGE, spaceId));
        }
    }

    /**
     * For each page, parallel-fetch the v1 read-restriction endpoint
     * {@code GET /wiki/rest/api/content/{id}/restriction/byOperation/read} and keep only
     * pages that are neither explicitly restricted nor descend from an explicitly
     * restricted ancestor. The v2 API has no per-page restrictions endpoint (only
     * space-level permissions and operations-for-current-user); the v1
     * individual-content endpoint is the canonical way to read page-level restrictions
     * and is still supported in Confluence Cloud.
     *
     * <p>Confluence's REST API only reports restrictions <em>explicitly</em> set on a
     * page — it never surfaces ones inherited from a parent (see
     * {@code CONFCLOUD-71108} / Atlassian KB "Confluence Get Page Restrictions API
     * doesn't display inherited restrictions"). A child page with no restriction of
     * its own is still effectively read-restricted in the UI if any ancestor has one,
     * so this walks each page's {@code parentId} chain (within the space's already
     * fetched pages) and treats an explicit restriction on any ancestor the same as
     * one on the page itself.</p>
     *
     * <p>Pages whose restriction check fails (non-200, exception) are kept defensively
     * so a transient API error never silently hides content. Each response body and
     * status is logged at INFO so behaviour is auditable on real instances.
     */
    private List<JsonObject> filterRestrictedPagesV2(List<JsonObject> pages) {
        if (pages.isEmpty()) {
            return pages;
        }

        Set<String> explicitlyRestrictedIds = fetchExplicitlyRestrictedIds(pages);

        Map<String, String> parentById = new HashMap<>();
        for (JsonObject page : pages) {
            String id = pageIdOf(page);
            if (id != null) {
                parentById.put(id, parentIdOf(page));
            }
        }

        List<JsonObject> kept = new ArrayList<>(pages.size());
        for (JsonObject page : pages) {
            String pageId = pageIdOf(page);
            if (isRestrictedOrInherited(pageId, explicitlyRestrictedIds, parentById)) {
                LOGGER.info("→ getPagesInSpaceV2: filtering page {} (explicit or inherited read restriction)", pageId);
            } else {
                kept.add(page);
            }
        }
        return kept;
    }

    /**
     * Parallel-fetches the read-restriction status for every page and returns the ids
     * of those with an explicit restriction. Failures are logged and treated as
     * "not restricted" here — the defensive "keep the page" behaviour is applied by
     * the caller, which decides whether the page is kept based on this set.
     */
    private Set<String> fetchExplicitlyRestrictedIds(List<JsonObject> pages) {
        List<CompletableFuture<HttpResponse<String>>> futures = pages.stream()
                .map(p -> {
                    String id = pageIdOf(p);
                    if (id == null) {
                        return CompletableFuture.<HttpResponse<String>>completedFuture(null);
                    }
                    return confluenceAttributes.getRestClient()
                            .doAsyncGet(APIVersion.V1, null, "content", id, "restriction", "byOperation", "read");
                })
                .collect(Collectors.toList());

        Set<String> restrictedIds = new HashSet<>();
        for (int i = 0; i < pages.size(); i++) {
            String pageId = pageIdOf(pages.get(i));
            HttpResponse<String> response;
            try {
                response = futures.get(i).join();
            } catch (Exception e) {
                LOGGER.warn("→ getPagesInSpaceV2: restriction check failed for page {} — keeping defensively: {}",
                        pageId, e.getMessage());
                continue;
            }
            if (response == null) {
                LOGGER.warn("→ getPagesInSpaceV2: restriction check returned no response for page {} — keeping defensively", pageId);
                continue;
            }
            int status = response.statusCode();
            String body = response.body();
            LOGGER.debug("→ getPagesInSpaceV2: /content/{}/restriction/byOperation/read → status={}, body={}", pageId, status, body);

            if (status != HttpURLConnection.HTTP_OK) {
                LOGGER.warn("→ getPagesInSpaceV2: restriction check returned status {} for page {} — keeping defensively",
                        status, pageId);
                continue;
            }
            if (hasV1ReadRestrictions(body)) {
                restrictedIds.add(pageId);
            }
        }
        return restrictedIds;
    }

    /**
     * Walks {@code pageId}'s ancestor chain via {@code parentById}, returning true if
     * the page itself or any ancestor (within the space's fetched pages) is explicitly
     * restricted. Guards against malformed cyclic parent references with a visited set.
     */
    private static boolean isRestrictedOrInherited(String pageId, Set<String> explicitlyRestrictedIds,
                                                    Map<String, String> parentById) {
        Set<String> visited = new HashSet<>();
        String currentId = pageId;
        while (currentId != null && visited.add(currentId)) {
            if (explicitlyRestrictedIds.contains(currentId)) {
                return true;
            }
            currentId = parentById.get(currentId);
        }
        return false;
    }

    private static String parentIdOf(JsonObject page) {
        JsonElement parentId = page.get("parentId");
        return parentId != null && parentId.isJsonPrimitive() ? parentId.getAsString() : null;
    }

    /**
     * Parse the v1 {@code /content/{id}/restriction/byOperation/read} response and
     * return true iff the page has any user or group entry on the read operation.
     *
     * <p>Documented response shape:
     * <pre>{@code
     * {
     *   "operation": "read",
     *   "restrictions": {
     *     "user":  { "results": [...], "size": N, "limit": 200, "start": 0 },
     *     "group": { "results": [...], "size": N, "limit": 200, "start": 0 }
     *   }
     * }
     * }</pre>
     *
     * Parsing is permissive: any of {@code size > 0}, {@code results} non-empty, or
     * {@code totalSize > 0} on either {@code user} or {@code group} is treated as
     * restricted. On parse error, defaults to {@code false} (open) — defensive behaviour
     * matches the keep-on-failure stance in {@link #filterRestrictedPagesV2}.
     */
    private static boolean hasV1ReadRestrictions(String responseBody) {
        try {
            JsonObject json = GsonFactory.getInstance().fromJson(responseBody, JsonObject.class);
            if (json == null) return false;
            JsonElement restrictions = json.get("restrictions");
            if (restrictions == null || !restrictions.isJsonObject()) return false;
            JsonObject inner = restrictions.getAsJsonObject();
            return principalListNonEmpty(inner.get("user")) || principalListNonEmpty(inner.get("group"));
        } catch (Exception e) {
            LOGGER.warn("→ hasV1ReadRestrictions: failed to parse — defaulting to NOT restricted: {}", e.getMessage());
            return false;
        }
    }

    private static boolean principalListNonEmpty(JsonElement element) {
        if (element == null || element.isJsonNull() || !element.isJsonObject()) return false;
        JsonObject obj = element.getAsJsonObject();
        JsonElement size = obj.get("size");
        if (size != null && size.isJsonPrimitive() && size.getAsInt() > 0) return true;
        JsonElement totalSize = obj.get("totalSize");
        if (totalSize != null && totalSize.isJsonPrimitive() && totalSize.getAsInt() > 0) return true;
        JsonElement results = obj.get("results");
        return results != null && results.isJsonArray() && results.getAsJsonArray().size() > 0;
    }

    private static String pageIdOf(JsonObject page) {
        JsonElement id = page.get("id");
        return id != null && id.isJsonPrimitive() ? id.getAsString() : null;
    }

    private static String extractV2NextUrl(JsonObject body) {
        JsonElement linksElement = body.get("_links");
        if (linksElement == null || !linksElement.isJsonObject()) return null;
        JsonElement nextElement = linksElement.getAsJsonObject().get("next");
        if (nextElement == null || nextElement.isJsonNull() || !nextElement.isJsonPrimitive()) return null;
        String next = nextElement.getAsString();
        return next.isEmpty() ? null : next;
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}

