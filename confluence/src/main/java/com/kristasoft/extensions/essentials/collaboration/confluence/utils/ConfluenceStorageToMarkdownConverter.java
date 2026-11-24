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

import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Converts Confluence storage-format XHTML into Markdown.
 *
 * <p>Confluence storage format is XHTML extended with custom {@code ac:} and {@code ri:}
 * namespaced elements (code blocks, panels, page links, attachments, emoticons, layouts).
 * Flexmark's HTML→MD converter handles standard HTML well, but does not know about these
 * macros, so we first rewrite them into standard HTML equivalents with jsoup, then feed
 * the cleaned tree to flexmark.
 *
 * <p>Unsupported or unknown {@code ac:} elements are flattened — their text content is
 * preserved while the wrapper element is dropped — so a future macro that this converter
 * does not recognize still produces readable output instead of being silently stripped.
 */
public final class ConfluenceStorageToMarkdownConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfluenceStorageToMarkdownConverter.class);

    private static final FlexmarkHtmlConverter CONVERTER = FlexmarkHtmlConverter.builder(
            new MutableDataSet()
                    .set(FlexmarkHtmlConverter.SETEXT_HEADINGS, false)
                    .set(FlexmarkHtmlConverter.OUTPUT_UNKNOWN_TAGS, false))
            .build();

    private static final Map<String, String> PANEL_LABELS = Map.of(
            "info", "Info",
            "note", "Note",
            "warning", "Warning",
            "tip", "Tip");

    private static final Map<String, String> EMOTICONS = Map.ofEntries(
            Map.entry("smile", "🙂"),
            Map.entry("sad", "🙁"),
            Map.entry("cheeky", "😜"),
            Map.entry("laugh", "😄"),
            Map.entry("wink", "😉"),
            Map.entry("thumbs-up", "👍"),
            Map.entry("thumbs-down", "👎"),
            Map.entry("information", "ℹ️"),
            Map.entry("tick", "✅"),
            Map.entry("cross", "❌"),
            Map.entry("warning", "⚠️"),
            Map.entry("plus", "➕"),
            Map.entry("minus", "➖"),
            Map.entry("question", "❓"),
            Map.entry("light-on", "💡"),
            Map.entry("yellow-star", "⭐"),
            Map.entry("red-star", "⭐"),
            Map.entry("green-star", "⭐"),
            Map.entry("blue-star", "⭐"));

    private ConfluenceStorageToMarkdownConverter() {
    }

    /**
     * Convert a page's title + storage-format body into a Markdown document.
     * The title is prepended as a top-level heading. A {@code null} or blank body
     * produces a document containing just the title heading.
     */
    public static String convert(String pageTitle, String storageBody) {
        String bodyMarkdown = convertBody(storageBody);
        String safeTitle = pageTitle == null ? "" : pageTitle.trim();
        if (safeTitle.isEmpty()) {
            return bodyMarkdown;
        }
        if (bodyMarkdown.isEmpty()) {
            return "# " + safeTitle + "\n";
        }
        return "# " + safeTitle + "\n\n" + bodyMarkdown;
    }

    private static String convertBody(String storageBody) {
        if (storageBody == null || storageBody.isBlank()) {
            return "";
        }
        try {
            Document doc = Jsoup.parse(storageBody, "", Parser.xmlParser());
            rewriteAcMacros(doc);
            String cleanedHtml = doc.html();
            String markdown = CONVERTER.convert(cleanedHtml);
            return markdown == null ? "" : markdown.strip();
        } catch (Exception e) {
            LOGGER.warn("→ ConfluenceStorageToMarkdownConverter: conversion failed, falling back to raw text: {}",
                    e.getMessage());
            return Jsoup.parse(storageBody).text();
        }
    }

    private static void rewriteAcMacros(Document doc) {
        for (Element macro : doc.getElementsByTag("ac:structured-macro")) {
            rewriteStructuredMacro(macro);
        }
        for (Element emoticon : doc.getElementsByTag("ac:emoticon")) {
            rewriteEmoticon(emoticon);
        }
        for (Element link : doc.getElementsByTag("ac:link")) {
            rewriteLink(link);
        }
        for (Element image : doc.getElementsByTag("ac:image")) {
            rewriteImage(image);
        }
        for (Element section : doc.getElementsByTag("ac:layout-section")) {
            unwrap(section);
        }
        for (Element cell : doc.getElementsByTag("ac:layout-cell")) {
            unwrap(cell);
        }
        for (Element layout : doc.getElementsByTag("ac:layout")) {
            unwrap(layout);
        }
        for (Element taskList : doc.getElementsByTag("ac:task-list")) {
            rewriteTaskList(taskList);
        }
    }

    private static void rewriteStructuredMacro(Element macro) {
        String name = macro.attr("ac:name");
        if ("code".equalsIgnoreCase(name)) {
            String language = "";
            for (Element param : macro.getElementsByTag("ac:parameter")) {
                if ("language".equalsIgnoreCase(param.attr("ac:name"))) {
                    language = param.text().trim();
                    break;
                }
            }
            Element body = macro.getElementsByTag("ac:plain-text-body").first();
            String code = body == null ? "" : body.wholeText();
            if (!code.endsWith("\n")) code = code + "\n";
            Element pre = new Element("pre");
            Element codeEl = new Element("code");
            if (!language.isEmpty()) {
                codeEl.attr("class", "language-" + language);
            }
            codeEl.text(code);
            pre.appendChild(codeEl);
            macro.replaceWith(pre);
            return;
        }

        String panelLabel = PANEL_LABELS.get(name == null ? "" : name.toLowerCase());
        if (panelLabel != null) {
            Element rich = macro.getElementsByTag("ac:rich-text-body").first();
            Element blockquote = new Element("blockquote");
            Element labelP = new Element("p");
            labelP.appendChild(new Element("strong").text(panelLabel + ":"));
            blockquote.appendChild(labelP);
            if (rich != null) {
                for (Node child : rich.childNodesCopy()) {
                    blockquote.appendChild(child);
                }
            }
            macro.replaceWith(blockquote);
            return;
        }

        Element rich = macro.getElementsByTag("ac:rich-text-body").first();
        if (rich != null) {
            Element wrapper = new Element("div");
            for (Node child : rich.childNodesCopy()) {
                wrapper.appendChild(child);
            }
            macro.replaceWith(wrapper);
        } else {
            unwrap(macro);
        }
    }

    private static void rewriteEmoticon(Element emoticon) {
        String name = emoticon.attr("ac:name");
        String replacement = EMOTICONS.getOrDefault(name == null ? "" : name.toLowerCase(),
                name == null || name.isEmpty() ? "" : ":" + name + ":");
        emoticon.replaceWith(new TextNode(replacement));
    }

    private static void rewriteLink(Element link) {
        String linkText = extractLinkText(link);
        Element pageRef = link.getElementsByTag("ri:page").first();
        Element attachmentRef = link.getElementsByTag("ri:attachment").first();
        Element userRef = link.getElementsByTag("ri:user").first();

        String label;
        if (pageRef != null) {
            String title = pageRef.attr("ri:content-title");
            label = linkText.isEmpty() ? title : linkText;
            if (label.isEmpty()) label = "linked page";
            label = "[Page: " + label + "]";
        } else if (attachmentRef != null) {
            String filename = attachmentRef.attr("ri:filename");
            label = linkText.isEmpty() ? filename : linkText;
            if (label.isEmpty()) label = "attachment";
            label = "[Attachment: " + label + "]";
        } else if (userRef != null) {
            String accountId = userRef.attr("ri:account-id");
            label = linkText.isEmpty()
                    ? (accountId.isEmpty() ? "user" : accountId)
                    : linkText;
            label = "[User: " + label + "]";
        } else {
            label = linkText.isEmpty() ? "[link]" : "[" + linkText + "]";
        }
        link.replaceWith(new TextNode(label));
    }

    private static String extractLinkText(Element link) {
        Element plainBody = link.getElementsByTag("ac:plain-text-link-body").first();
        if (plainBody != null) return plainBody.wholeText().trim();
        Element richBody = link.getElementsByTag("ac:link-body").first();
        if (richBody != null) return richBody.text().trim();
        return "";
    }

    private static void rewriteImage(Element image) {
        String alt;
        Element attachment = image.getElementsByTag("ri:attachment").first();
        Element url = image.getElementsByTag("ri:url").first();
        if (attachment != null) {
            String filename = attachment.attr("ri:filename");
            alt = filename.isEmpty() ? "embedded image" : filename;
        } else if (url != null) {
            String href = url.attr("ri:value");
            alt = href.isEmpty() ? "embedded image" : href;
        } else {
            alt = "embedded image";
        }
        image.replaceWith(new TextNode("[Image: " + alt + "]"));
    }

    private static void rewriteTaskList(Element taskList) {
        Element ul = new Element("ul");
        for (Element task : taskList.getElementsByTag("ac:task")) {
            Element status = task.getElementsByTag("ac:task-status").first();
            Element bodyEl = task.getElementsByTag("ac:task-body").first();
            boolean done = status != null && "complete".equalsIgnoreCase(status.text().trim());
            String bodyText = bodyEl == null ? "" : bodyEl.text().trim();
            Element li = new Element("li");
            li.text((done ? "[x] " : "[ ] ") + bodyText);
            ul.appendChild(li);
        }
        taskList.replaceWith(ul);
    }

    private static void unwrap(Element el) {
        Element parent = el.parent();
        if (parent == null) {
            el.remove();
            return;
        }
        int index = el.siblingIndex();
        Elements children = new Elements(el.children());
        el.remove();
        for (int i = 0; i < children.size(); i++) {
            parent.insertChildren(index + i, children.get(i));
        }
    }
}
