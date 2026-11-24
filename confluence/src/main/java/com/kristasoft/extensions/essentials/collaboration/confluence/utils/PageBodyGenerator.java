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

import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.entities.Content;
import com.kristasoft.extensions.essentials.collaboration.confluence.constants.ConfluenceConstants;
import com.kristasoft.extensions.essentials.collaboration.confluence.dto.Page;
import com.kristasoft.extensions.essentials.collaboration.confluence.dto.Status;
import com.kristasoft.extensions.essentials.collaboration.confluence.dto.Storage;
import com.kristasoft.extensions.essentials.collaboration.confluence.dto.Template;
import com.kristasoft.extensions.essentials.collaboration.confluence.dto.Version;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.TextNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Utility class for generating request bodies for page creation and updates.
 */
public class PageBodyGenerator {

    /**
     * Generate request body for creating a page from a template.
     *
     * @param spaceId the space ID
     * @param template the template to use
     * @param title the page title
     * @param variables the template variables
     * @return request body map
     */
    public Map<String, Object> generateBodyForPageRequest(String spaceId, Template template, String title, List<Map<String, Object>> variables) {
        Map<String, Object> body = new HashMap<>();
        body.put("spaceId", spaceId);
        body.put(ConfluenceConstants.STATUS, Status.draft.toString());
        body.put(ConfluenceConstants.TITLE, title);
        body.put("body", Storage.builder().setRepresentation(ConfluenceConstants.STORAGE)
                .setValue(setVariables(template.getBody().getStorage().getValue(), variables)));
        return body;
    }

    /**
     * Generate request body for creating a page from content.
     *
     * @param spaceId the space ID
     * @param title the page title
     * @param contents the content list
     * @return request body map
     */
    public Map<String, Object> generateBodyForPageRequest(String spaceId, String title, List<Content> contents) {
        Map<String, Object> body = new HashMap<>();
        body.put("spaceId", spaceId);
        body.put(ConfluenceConstants.STATUS, Status.draft.toString());
        body.put(ConfluenceConstants.TITLE, title);
        body.put("body", Storage.builder().setRepresentation(ConfluenceConstants.STORAGE)
                .setValue(contents.stream().map(content -> content.body).collect(Collectors.joining("\n"))));
        return body;
    }

    /**
     * Generate request body for updating a page from a template.
     *
     * @param page the existing page
     * @param template the template to use
     * @param status the new status
     * @param title the new title
     * @param comment the version comment
     * @param variables the template variables
     * @return request body map
     */
    public Map<String, Object> generateBodyForPageUpdate(Page page, Template template, String status, String title, String comment, List<Map<String, Object>> variables) {
        Map<String, Object> body = new HashMap<>();
        body.put("id", page.getId());
        Optional.ofNullable(status).ifPresentOrElse(s -> {
            body.put(ConfluenceConstants.STATUS, s);
            if (s.equalsIgnoreCase(ConfluenceConstants.CURRENT)) {
                body.put("version", Version.builder()
                        .withNumber(page.getStatus().equalsIgnoreCase(ConfluenceConstants.DRAFT) ? 1 : page.getVersion().getNumber() + 1)
                        .withMessage(comment)
                        .build());
            }
        }, () -> body.put(ConfluenceConstants.STATUS, page.getStatus()));
        Optional.ofNullable(title).ifPresentOrElse(s -> body.put(ConfluenceConstants.TITLE, s),
                () -> body.put(ConfluenceConstants.TITLE, page.getTitle()));
        Optional.ofNullable(variables).ifPresentOrElse(vars -> body.put("body", Storage.builder().setRepresentation(ConfluenceConstants.STORAGE)
                        .setValue(setVariables(template.getBody().getStorage().getValue(), variables))),
                () -> body.put("body", Storage.builder().setRepresentation(ConfluenceConstants.STORAGE)
                        .setValue(Optional.ofNullable(page.getBody())
                                .map(b -> b.getStorage())
                                .map(s -> s.getValue())
                                .orElse(""))));
        return body;
    }

    /**
     * Generate request body for updating a page from content.
     *
     * @param page the existing page
     * @param status the new status
     * @param title the new title
     * @param comment the version comment
     * @param contents the content list
     * @return request body map
     */
    public Map<String, Object> generateBodyForPageUpdate(Page page, String status, String title, String comment, List<Content> contents) {
        Map<String, Object> body = new HashMap<>();
        body.put("id", page.getId());
        Optional.ofNullable(status).map(String::toLowerCase).ifPresentOrElse(newStatus -> {
            if (newStatus.equalsIgnoreCase(ConfluenceConstants.DRAFT) && page.getStatus().equalsIgnoreCase(ConfluenceConstants.DRAFT)) {
                body.put(ConfluenceConstants.STATUS, ConfluenceConstants.DRAFT);
            }
            else  {
                body.put(ConfluenceConstants.STATUS, ConfluenceConstants.CURRENT);
                body.put("version", Version.builder()
                        .withNumber( page.getVersion().getNumber() + 1)
                        .withMessage(comment)
                        .build());
            }
        }, () -> body.put(ConfluenceConstants.STATUS, page.getStatus()));
        Optional.ofNullable(title).ifPresentOrElse(s -> body.put(ConfluenceConstants.TITLE, s),
                () -> body.put(ConfluenceConstants.TITLE, page.getTitle()));
        Optional.ofNullable(contents).ifPresentOrElse(vars -> body.put("body", Storage.builder().setRepresentation(ConfluenceConstants.STORAGE)
                        .setValue(vars.stream().map(content -> content.body).collect(Collectors.joining("\n")))),
                () -> body.put("body", Storage.builder().setRepresentation(ConfluenceConstants.STORAGE)
                        .setValue(Optional.ofNullable(page.getBody().getStorage()).isPresent() ? page.getBody().getStorage().getValue() : "")));
        return body;
    }

    /**
     * Replace template variables in HTML content.
     *
     * @param html the HTML template
     * @param variables the variables to replace
     * @return processed HTML
     */
    private String setVariables(String html, List<Map<String, Object>> variables) {
        Document template = Jsoup.parseBodyFragment(html);
        template.getElementsByTag("at:declarations").remove();
        template.getElementsByTag("at:var").forEach(element -> {
            Optional<Attribute> optionalAttribute = element.attributes().asList().stream().filter(att -> att.getKey().equals("at:name")).findAny();
            optionalAttribute.ifPresent(attribute -> {
                String placeHolderValue = (String) variables.stream().filter(variable -> variable.containsValue(attribute.getValue()))
                        .map(m -> m.getOrDefault("Value", ""))
                        .reduce("", (o, o2) -> o + (String) o2);
                element.replaceWith(new TextNode(placeHolderValue));
            });
        });
        template.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
        return template.body().html();
    }
}

