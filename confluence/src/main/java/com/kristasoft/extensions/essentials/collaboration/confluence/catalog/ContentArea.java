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

package com.kristasoft.extensions.essentials.collaboration.confluence.catalog;

import app.krista.extension.impl.anno.Attribute;
import app.krista.extension.impl.anno.CatalogRequest;
import app.krista.extension.impl.anno.Domain;
import app.krista.extension.impl.anno.Field;
import app.krista.model.base.FreeForm;
import app.krista.ksdk.context.RequestContext;
import com.kristasoft.extensions.essentials.collaboration.confluence.api.ConfluenceAuditService;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.entities.Content;
import com.kristasoft.extensions.essentials.collaboration.confluence.service.XHTMLContentService;
import com.kristasoft.extensions.essentials.collaboration.confluence.utils.TelemetryHelper;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.kristasoft.extensions.essentials.collaboration.confluence.utils.TelemetryHelper.safeTagMap;

/**
 * Catalog area for Confluence content operations (XHTML content management).
 *
 * <p>All {@link app.krista.extension.impl.anno.CatalogRequest} methods are annotated with
 * {@code tool = true}, exposing them as MCP (Model Context Protocol) tools for AI agent
 * consumption. Each operation records TRIGGERED / SUCCESS / FAILURE audit events via
 * {@link com.kristasoft.extensions.essentials.collaboration.confluence.api.ConfluenceAuditService},
 * which are visible on the extension's audit dashboard.</p>
 */
@Domain(id = "catEntryDomain_1a86a3cc-0f2a-45fa-b9c0-b32487c830f8",
        name = "Confluence",
        ecosystemId = "catEntryEcosystem_0aeef50c-bc48-4c2c-844a-948193e53e82",
        ecosystemName = "Collaboration",
        ecosystemVersion = "7dcfc3e2-fd86-4999-89bb-c807fa9f9289")
public class ContentArea {

    private final XHTMLContentService service;
    private final TelemetryHelper telemetryHelper;
    private final ConfluenceAuditService confluenceAuditService;
    private final RequestContext requestContext;


    @Inject
    public ContentArea(XHTMLContentService service, TelemetryHelper telemetryHelper,
                       ConfluenceAuditService confluenceAuditService, RequestContext requestContext) {
        this.service = service;
        this.telemetryHelper = telemetryHelper;
        this.confluenceAuditService = confluenceAuditService;
        this.requestContext = requestContext;
    }


    @CatalogRequest(
            id = "localDomainRequest_2ff8beac-89ae-40be-b067-8507aa0efbf8",
            name = "Generate Content for Table",
            description = "Generate a content entity for a dynamic table",
            area = "Content",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.Desc(name = "Table Content", type = "Entity(Content)",required = false)
    public Content generateContentForTable(
            @Field.Desc(name = "Columns", type = "[ Text ]",required = true) List<String> columns,
            @Field.Desc(name = "Table", type = "[ Composite ]",required = true) List<FreeForm> table) {
        String invokerId = requestContext.getInvokerId();
        confluenceAuditService.triggered(invokerId, "Generate Content for Table");
        boolean auditRecorded = false;
        long startTime = System.currentTimeMillis();
        try {
            telemetryHelper.incrementCount("confluence.generateContentForTable");

            Content content = service.createContentForTable(columns, table.stream().map(FreeForm::entrySet)
                    .map(entries -> entries.stream()
                            .collect(Collectors.toMap(Map.Entry::getKey, stringObjectEntry -> (String) stringObjectEntry.getValue())))
                    .collect(Collectors.toList()));

            telemetryHelper.recordSuccess("confluence.generateContentForTable", startTime,
                    safeTagMap("column_count", String.valueOf(columns != null ? columns.size() : 0),
                            "row_count", String.valueOf(table != null ? table.size() : 0),
                            "content_id", content != null ? content.iD : "null"));

            return content;
        } catch (app.krista.extension.authorization.MustAuthorizeException cause) {
            auditRecorded = true;
            confluenceAuditService.authFailure(invokerId, "Generate Content for Table", cause);
            throw cause;
        } catch (Exception cause) {
            auditRecorded = true;
            confluenceAuditService.failure(invokerId, "Generate Content for Table", cause);
            telemetryHelper.recordError("confluence.generateContentForTable", startTime, cause,
                    safeTagMap("column_count", String.valueOf(columns != null ? columns.size() : 0),
                            "row_count", String.valueOf(table != null ? table.size() : 0)));
            throw cause;
        } finally {
            if (!auditRecorded) {
                confluenceAuditService.success(invokerId, "Generate Content for Table");
            }
        }

    }

    @CatalogRequest(
            id = "localDomainRequest_86e6fff9-d608-475a-9481-9518db264a4b",
            name = "Generate Heading Content",
            description = "Generate a dynamic heading for a confluence page",
            area = "Content",
            type = CatalogRequest.Type.CHANGE_SYSTEM, tool = true)
    @Field.Desc(name = "Heading", type = "Entity(Content)", required = false)
    public Content generateHeadingContent(
            @Field.Text(name = "Text", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String text,
            @Field.PickOne(name = "Type", values = {"h1", "h2", "h3", "h4", "h5", "h6"}, required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String type,
            @Field.PickOne(name = "Text Alignment", values = {"Center", "Left", "Right", "Justify"}, required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String textAlignment,
            @Field.Boolean(name = "Break Line", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) Boolean breakLine) {
        String invokerId = requestContext.getInvokerId();
        confluenceAuditService.triggered(invokerId, "Generate Heading Content");
        boolean auditRecorded = false;
        long startTime = System.currentTimeMillis();
        try {
            telemetryHelper.incrementCount("confluence.generateHeadingContent");

            Content content = service.createHeadingContent(text, type, textAlignment, breakLine);

            telemetryHelper.recordSuccess("confluence.generateHeadingContent", startTime,
                    safeTagMap("text", text, "type", type, "text_alignment", textAlignment,
                            "break_line", String.valueOf(breakLine), "content_id", content != null ? content.iD : "null"));

            return content;
        } catch (app.krista.extension.authorization.MustAuthorizeException cause) {
            auditRecorded = true;
            confluenceAuditService.authFailure(invokerId, "Generate Heading Content", cause);
            throw cause;
        } catch (Exception cause) {
            auditRecorded = true;
            confluenceAuditService.failure(invokerId, "Generate Heading Content", cause);
            telemetryHelper.recordError("confluence.generateHeadingContent", startTime, cause,
                    safeTagMap("text", text, "type", type, "text_alignment", textAlignment, "break_line", String.valueOf(breakLine)));
            throw cause;
        } finally {
            if (!auditRecorded) {
                confluenceAuditService.success(invokerId, "Generate Heading Content");
            }
        }
    }



    @CatalogRequest(
            id = "localDomainRequest_b702abb5-6353-4378-8636-9732142f774b",
            name = "Generate Anchor Tag",
            description = "Generate an anchor tag content",
            area = "Content",
            type = CatalogRequest.Type.CHANGE_SYSTEM, tool = true)
    @Field.Desc(name = "Anchor Tag Content", type = "Entity(Content)", required = false)
    public Content generateAnchorTag(
            @Field.Text(name = "Text", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String text,
            @Field.Text(name = "Href", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String href) {

        String invokerId = requestContext.getInvokerId();
        confluenceAuditService.triggered(invokerId, "Generate Anchor Tag");
        boolean auditRecorded = false;
        long startTime = System.currentTimeMillis();
        try {
            telemetryHelper.incrementCount("confluence.generateAnchorTag");

            Content content = service.createAnchorTag(text, href);

            telemetryHelper.recordSuccess("confluence.generateAnchorTag", startTime,
                    safeTagMap("text", text, "href", href, "content_id", content != null ? content.iD : "null"));

            return content;
        } catch (app.krista.extension.authorization.MustAuthorizeException cause) {
            auditRecorded = true;
            confluenceAuditService.authFailure(invokerId, "Generate Anchor Tag", cause);
            throw cause;
        } catch (Exception cause) {
            auditRecorded = true;
            confluenceAuditService.failure(invokerId, "Generate Anchor Tag", cause);
            telemetryHelper.recordError("confluence.generateAnchorTag", startTime, cause,
                    safeTagMap("text", text, "href", href));
            throw cause;
        } finally {
            if (!auditRecorded) {
                confluenceAuditService.success(invokerId, "Generate Anchor Tag");
            }
        }
    }
}