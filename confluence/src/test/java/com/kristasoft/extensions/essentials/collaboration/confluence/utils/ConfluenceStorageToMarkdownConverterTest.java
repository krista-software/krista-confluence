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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfluenceStorageToMarkdownConverterTest {

    @Test
    void blankBodyProducesTitleOnlyDocument() {
        String md = ConfluenceStorageToMarkdownConverter.convert("My Title", "");
        assertEquals("# My Title\n", md);
    }

    @Test
    void nullBodyProducesTitleOnlyDocument() {
        String md = ConfluenceStorageToMarkdownConverter.convert("My Title", null);
        assertEquals("# My Title\n", md);
    }

    @Test
    void blankTitleAndBlankBodyProduceEmptyString() {
        String md = ConfluenceStorageToMarkdownConverter.convert("", null);
        assertEquals("", md);
    }

    @Test
    void titleIsPrependedAsH1AboveBody() {
        String md = ConfluenceStorageToMarkdownConverter.convert("Hello", "<p>world</p>");
        assertTrue(md.startsWith("# Hello\n\n"), "title heading missing: " + md);
        assertTrue(md.contains("world"), "body missing: " + md);
    }

    @Test
    void plainHtmlHeadingsAndListsConvert() {
        String body = "<h2>Section</h2><ul><li>One</li><li>Two</li></ul>";
        String md = ConfluenceStorageToMarkdownConverter.convert("T", body);
        assertTrue(md.contains("## Section"), md);
        assertTrue(md.contains("One"), md);
        assertTrue(md.contains("Two"), md);
    }

    @Test
    void codeMacroProducesFencedBlockWithLanguage() {
        String body = """
                <ac:structured-macro ac:name="code">
                  <ac:parameter ac:name="language">java</ac:parameter>
                  <ac:plain-text-body><![CDATA[int x = 1;]]></ac:plain-text-body>
                </ac:structured-macro>""";
        String md = ConfluenceStorageToMarkdownConverter.convert("T", body);
        assertTrue(md.contains("```java"), "expected java fence in: " + md);
        assertTrue(md.contains("int x = 1;"), "expected code body in: " + md);
    }

    @Test
    void codeMacroWithoutLanguagePreservesContent() {
        String body = "<ac:structured-macro ac:name=\"code\">"
                + "<ac:plain-text-body><![CDATA[println(\"hi\")]]></ac:plain-text-body>"
                + "</ac:structured-macro>";
        String md = ConfluenceStorageToMarkdownConverter.convert("T", body);
        assertTrue(md.contains("println"), "code body missing: " + md);
        assertTrue(md.contains("\"hi\""), "code body missing: " + md);
    }

    @Test
    void infoPanelBecomesBlockquoteWithLabel() {
        String body = "<ac:structured-macro ac:name=\"info\"><ac:rich-text-body><p>heads up</p></ac:rich-text-body></ac:structured-macro>";
        String md = ConfluenceStorageToMarkdownConverter.convert("T", body);
        assertTrue(md.contains(">"), "blockquote missing: " + md);
        assertTrue(md.contains("**Info:**"), "label missing: " + md);
        assertTrue(md.contains("heads up"), md);
    }

    @Test
    void warningPanelBecomesBlockquoteWithWarningLabel() {
        String body = "<ac:structured-macro ac:name=\"warning\"><ac:rich-text-body><p>danger</p></ac:rich-text-body></ac:structured-macro>";
        String md = ConfluenceStorageToMarkdownConverter.convert("T", body);
        assertTrue(md.contains("**Warning:**"), md);
        assertTrue(md.contains("danger"), md);
    }

    @Test
    void unknownStructuredMacroIsFlattenedNotDropped() {
        String body = "<ac:structured-macro ac:name=\"future-macro\"><ac:rich-text-body><p>some content</p></ac:rich-text-body></ac:structured-macro>";
        String md = ConfluenceStorageToMarkdownConverter.convert("T", body);
        assertTrue(md.contains("some content"), "unknown macro should preserve text: " + md);
    }

    @Test
    void emoticonMapsToUnicode() {
        String md = ConfluenceStorageToMarkdownConverter.convert("T",
                "<p>Hello <ac:emoticon ac:name=\"smile\"/> world</p>");
        assertTrue(md.contains("🙂"), md);
        assertTrue(md.contains("Hello"), md);
        assertTrue(md.contains("world"), md);
    }

    @Test
    void unknownEmoticonFallsBackToColonName() {
        String md = ConfluenceStorageToMarkdownConverter.convert("T",
                "<p>see <ac:emoticon ac:name=\"sparkles\"/> me</p>");
        assertTrue(md.contains(":sparkles:"), md);
    }

    @Test
    void pageLinkUsesContentTitleAsLabel() {
        String body = "<ac:link><ri:page ri:content-title=\"Onboarding\" ri:space-key=\"DEV\"/></ac:link>";
        String md = ConfluenceStorageToMarkdownConverter.convert("T", body);
        assertTrue(unescapeBrackets(md).contains("[Page: Onboarding]"), md);
    }

    @Test
    void pageLinkPrefersExplicitLinkText() {
        String body = "<ac:link><ri:page ri:content-title=\"Onboarding\"/>"
                + "<ac:plain-text-link-body><![CDATA[Read this first]]></ac:plain-text-link-body></ac:link>";
        String md = ConfluenceStorageToMarkdownConverter.convert("T", body);
        assertTrue(unescapeBrackets(md).contains("[Page: Read this first]"), md);
        assertFalse(md.contains("Onboarding"), "explicit body should win: " + md);
    }

    @Test
    void attachmentLinkUsesFilename() {
        String body = "<ac:link><ri:attachment ri:filename=\"specs.pdf\"/></ac:link>";
        String md = ConfluenceStorageToMarkdownConverter.convert("T", body);
        assertTrue(unescapeBrackets(md).contains("[Attachment: specs.pdf]"), md);
    }

    @Test
    void userLinkUsesAccountId() {
        String body = "<ac:link><ri:user ri:account-id=\"abc123\"/></ac:link>";
        String md = ConfluenceStorageToMarkdownConverter.convert("T", body);
        assertTrue(unescapeBrackets(md).contains("[User: abc123]"), md);
    }

    @Test
    void imageWithAttachmentUsesFilename() {
        String body = "<ac:image><ri:attachment ri:filename=\"screenshot.png\"/></ac:image>";
        String md = ConfluenceStorageToMarkdownConverter.convert("T", body);
        assertTrue(unescapeBrackets(md).contains("[Image: screenshot.png]"), md);
    }

    /**
     * Flexmark escapes literal {@code [} and {@code ]} in output text as {@code \[} and
     * {@code \]} so that they render as text rather than be parsed as a link by markdown
     * viewers. That is the correct rendering, but the raw string contains the backslashes;
     * tests assert on the post-render content.
     */
    private static String unescapeBrackets(String md) {
        return md.replace("\\[", "[").replace("\\]", "]");
    }

    @Test
    void imageWithExternalUrlUsesUrlValue() {
        String body = "<ac:image><ri:url ri:value=\"https://example.com/x.png\"/></ac:image>";
        String md = ConfluenceStorageToMarkdownConverter.convert("T", body);
        assertTrue(md.contains("https://example.com/x.png"), md);
    }

    @Test
    void layoutWrappersAreFlattened() {
        String body = "<ac:layout><ac:layout-section ac:type=\"two_equal\"><ac:layout-cell><p>left</p></ac:layout-cell>"
                + "<ac:layout-cell><p>right</p></ac:layout-cell></ac:layout-section></ac:layout>";
        String md = ConfluenceStorageToMarkdownConverter.convert("T", body);
        assertTrue(md.contains("left"), md);
        assertTrue(md.contains("right"), md);
    }

    @Test
    void taskListBecomesCheckboxList() {
        String body = "<ac:task-list>"
                + "<ac:task><ac:task-status>complete</ac:task-status><ac:task-body>done item</ac:task-body></ac:task>"
                + "<ac:task><ac:task-status>incomplete</ac:task-status><ac:task-body>open item</ac:task-body></ac:task>"
                + "</ac:task-list>";
        String md = ConfluenceStorageToMarkdownConverter.convert("T", body);
        String unescaped = unescapeBrackets(md);
        assertTrue(unescaped.contains("[x] done item"), md);
        assertTrue(unescaped.contains("[ ] open item"), md);
    }

    @Test
    void plainTableConvertsToMarkdownTable() {
        String body = "<table><thead><tr><th>A</th><th>B</th></tr></thead>"
                + "<tbody><tr><td>1</td><td>2</td></tr></tbody></table>";
        String md = ConfluenceStorageToMarkdownConverter.convert("T", body);
        assertTrue(md.contains("| A"), md);
        assertTrue(md.contains("| B"), md);
        assertTrue(md.contains("| 1"), md);
        assertTrue(md.contains("| 2"), md);
    }

    @Test
    void resultIsNeverNull() {
        assertNotNull(ConfluenceStorageToMarkdownConverter.convert("T", "<p>hi</p>"));
    }
}
