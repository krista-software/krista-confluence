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
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Confluence-specific MCP product configuration.
 *
 * <p>Confluence uses CQL (Confluence Query Language) over a GET-based search
 * endpoint rather than the POST-based JQL endpoint used by Jira products.
 * Basic Auth credentials store only the subdomain (e.g. {@code "mycompany"})
 * rather than a full URL, so {@link #buildBasicSearchUrl} and
 * {@link #buildBasicBaseUrl} are overridden to reconstruct the full URL.</p>
 */
public final class ConfluenceMcpProductConfig implements McpProductConfig {

    private static final Gson GSON = new Gson();

    private static final String CONFLUENCE_SEARCH_URL =
            "https://api.atlassian.com/ex/confluence/%s/wiki/rest/api/content/search";

    private static final List<String> STANDARD_FIELDS = List.of(
            "title", "type", "status", "space", "version"
    );

    private static final List<ColumnDef> COLUMNS = List.of(
            new ColumnDef("title",  "Title",  "link"),
            new ColumnDef("type",   "Type",   "pill"),
            new ColumnDef("space",  "Space",  "text"),
            new ColumnDef("author", "Author", "text"),
            new ColumnDef("status", "Status", "pill")
    );

    @Override public String productName()          { return "Confluence"; }
    @Override public String queryLanguage()         { return "CQL"; }
    @Override public String queryLanguageFullName()  { return "CQL (Confluence Query Language)"; }
    @Override public String searchUrlTemplate()      { return CONFLUENCE_SEARCH_URL; }
    @Override public String basicSearchPath()        { return "/wiki/rest/api/content/search"; }
    @Override public List<String> standardFields()   { return STANDARD_FIELDS; }
    @Override public List<ColumnDef> resultColumns() { return COLUMNS; }
    @Override public String searchHttpMethod()       { return "GET"; }
    @Override public String oauthApiBaseTemplate() { return "https://api.atlassian.com/ex/confluence/%s/wiki/rest/api"; }

    /**
     * Confluence Basic Auth stores only the subdomain (e.g. {@code "mycompany"}).
     * Reconstruct the full search URL.
     */
    @Override
    public String buildBasicSearchUrl(String hostUrl) {
        String base = hostUrl.replaceAll("/+$", "");
        if (!base.startsWith("http")) {
            base = "https://" + base + ".atlassian.net";
        }
        return base + basicSearchPath();
    }

    /**
     * Returns the full Confluence wiki base URL for building page links.
     */
    @Override
    public String buildBasicBaseUrl(String hostUrl) {
        String base = hostUrl.replaceAll("/+$", "");
        if (!base.startsWith("http")) {
            base = "https://" + base + ".atlassian.net";
        }
        return base + "/wiki";
    }

    @Override
    public String queryGenerationPrompt(String question) {
        return "Given this Confluence question, output a JSON object with exactly three fields:\n"
                + "  \"query\": the CQL query string to find the relevant page(s) or content\n"
                + "  \"specific\": true if the question asks about a specific field or attribute of a page "
                + "(e.g. author, status, space, type of a named page), "
                + "false if it asks for a list of pages (e.g. show pages in DEV space, find blog posts about Jira)\n"
                + "  \"field\": when specific is true, the exact result-row key being asked about "
                + "(one of: title, type, space, author, status); null when specific is false\n\n"
                + "Rules:\n"
                + "- Output ONLY raw JSON. No markdown, no backticks, no explanation.\n"
                + "- Use standard Confluence CQL syntax.\n\n"
                + "Examples:\n"
                + "  Q: Find all pages in DEV space         -> {\"query\":\"type=page AND space.key=\\\"DEV\\\"\",\"specific\":false,\"field\":null}\n"
                + "  Q: Who created the Home page?          -> {\"query\":\"title=\\\"Home\\\"\",\"specific\":true,\"field\":\"author\"}\n"
                + "  Q: Show blog posts about deployment    -> {\"query\":\"type=blogpost AND text~\\\"deployment\\\"\",\"specific\":false,\"field\":null}\n"
                + "  Q: What space is the API Docs page in? -> {\"query\":\"title=\\\"API Docs\\\"\",\"specific\":true,\"field\":\"space\"}\n\n"
                + "Question: " + question;
    }

    @Override
    public String answerExtractionPrompt(String question, String contentData) {
        return "Based on the following Confluence content data, answer the user's question directly and concisely.\n\n"
                + "User's question: " + question + "\n\n"
                + "Confluence data:\n" + contentData
                + "\nRules:\n"
                + "- Answer in one or two sentences.\n"
                + "- If the specific field is not present, say so clearly.\n"
                + "- Do NOT wrap your answer in backticks or markdown.\n"
                + "- Do NOT repeat the question.";
    }

    /** Confluence uses GET — no request body needed. */
    @Override
    public String buildSearchRequestBody(String query, int maxResults, boolean allFields) {
        return null;
    }

    @Override
    public McpSearchResult parseSearchResponse(String json, String baseUrl) {
        JsonObject raw = GSON.fromJson(json, JsonObject.class);

        // Confluence error responses
        if (raw.has("message") && raw.has("statusCode")) {
            throw new IllegalArgumentException(raw.get("message").getAsString());
        }

        JsonArray results = raw.has("results") ? raw.getAsJsonArray("results") : new JsonArray();
        int total = raw.has("size") ? raw.get("size").getAsInt() : results.size();

        // baseUrl from OAuth already ends with /wiki (resource.getUrl() = "https://xxx.atlassian.net/wiki")
        String wikiBase = baseUrl.endsWith("/wiki") ? baseUrl : baseUrl + "/wiki";

        List<Map<String, Object>> rows = new ArrayList<>();
        for (JsonElement el : results) {
            JsonObject item = el.getAsJsonObject();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("title",  str(item, "title"));
            row.put("type",   str(item, "type"));
            row.put("status", str(item, "status"));
            row.put("space",  nestedStr(item, "space", "name"));
            row.put("author", deepStr(item, "version", "by", "displayName"));

            // Build the page URL
            String webui = "";
            if (item.has("_links") && item.get("_links").isJsonObject()) {
                webui = str(item.getAsJsonObject("_links"), "webui");
            }
            row.put("url", webui.isEmpty() ? wikiBase : wikiBase + webui);
            rows.add(row);
        }
        return new McpSearchResult("cql", total, rows.size(), rows);
    }

    @Override
    public List<FreeForm> parseStandardResponse(String json, String baseUrl) {
        JsonObject raw = GSON.fromJson(json, JsonObject.class);
        JsonArray results = raw.has("results") ? raw.getAsJsonArray("results") : new JsonArray();
        String wikiBase = baseUrl.endsWith("/wiki") ? baseUrl : baseUrl + "/wiki";
        List<FreeForm> rows = new ArrayList<>();
        for (JsonElement el : results) {
            JsonObject item = el.getAsJsonObject();
            String webui = "";
            if (item.has("_links") && item.get("_links").isJsonObject()) {
                webui = str(item.getAsJsonObject("_links"), "webui");
            }
            FreeForm row = new FreeForm();
            row.put("title",  " Text ", str(item, "title"));
            row.put("type",   " Text ", str(item, "type"));
            row.put("status", " Text ", str(item, "status"));
            row.put("space",  " Text ", nestedStr(item, "space", "name"));
            row.put("author", " Text ", deepStr(item, "version", "by", "displayName"));
            row.put("url",    " Text ", webui.isEmpty() ? wikiBase : wikiBase + webui);
            rows.add(row);
        }
        return rows;
    }

    @Override
    public List<Map<String, String>> parseAllFieldsResponse(String json, String baseUrl) {
        JsonObject raw = GSON.fromJson(json, JsonObject.class);
        JsonArray results = raw.has("results") ? raw.getAsJsonArray("results") : new JsonArray();
        String wikiBase = baseUrl.endsWith("/wiki") ? baseUrl : baseUrl + "/wiki";
        List<Map<String, String>> result = new ArrayList<>();
        for (JsonElement el : results) {
            JsonObject item = el.getAsJsonObject();
            Map<String, String> row = new LinkedHashMap<>();
            row.put("title",  str(item, "title"));
            row.put("type",   str(item, "type"));
            row.put("status", str(item, "status"));
            row.put("spaceKey",  nestedStr(item, "space", "key"));
            row.put("spaceName", nestedStr(item, "space", "name"));
            row.put("author",    deepStr(item, "version", "by", "displayName"));
            String webui = "";
            if (item.has("_links") && item.get("_links").isJsonObject()) {
                webui = str(item.getAsJsonObject("_links"), "webui");
            }
            row.put("url", webui.isEmpty() ? wikiBase : wikiBase + webui);
            result.add(row);
        }
        return result;
    }

    // ── JSON helpers ──────────────────────────────────────────────────────────

    private static String str(JsonObject obj, String key) {
        return (obj != null && obj.has(key) && !obj.get(key).isJsonNull())
                ? obj.get(key).getAsString() : "";
    }

    private static String nestedStr(JsonObject obj, String outer, String inner) {
        if (obj == null || !obj.has(outer) || obj.get(outer).isJsonNull()) return "";
        JsonElement el = obj.get(outer);
        if (!el.isJsonObject()) return "";
        return str(el.getAsJsonObject(), inner);
    }

    private static String deepStr(JsonObject obj, String k1, String k2, String k3) {
        if (obj == null || !obj.has(k1) || obj.get(k1).isJsonNull()) return "";
        JsonElement e1 = obj.get(k1);
        if (!e1.isJsonObject()) return "";
        return nestedStr(e1.getAsJsonObject(), k2, k3);
    }
}
