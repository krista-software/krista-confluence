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

package com.kristasoft.extensions.essentials.collaboration.confluence;

import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.entities.Content;
import com.kristasoft.extensions.essentials.collaboration.confluence.service.XHTMLContentService;
import com.kristasoft.extensions.essentials.collaboration.confluence.dto.*;
import com.kristasoft.extensions.essentials.collaboration.confluence.service.ConfluenceService;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.TextNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.Mockito.*;

public class ConfluenceTests {
    private static final Logger log = LoggerFactory.getLogger(ConfluenceTests.class);
    private static ConfluenceService confluenceService;
    private static XHTMLContentService xhtmlContentService;

    @BeforeAll
    public static void setService() throws IOException {
        Properties properties = new Properties();
        properties.load(ConfluenceTests.class.getClassLoader().getResourceAsStream("confluence.properties"));
        String domainName = properties.getProperty("confluence.domainName");
        String accountEmail = properties.getProperty("confluence.accountEmail");
        String apiKey = properties.getProperty("confluence.apiKey");
        confluenceService = mock(ConfluenceService.class);
        xhtmlContentService = mock(XHTMLContentService.class);
    }

    @Test
    public void createPageFromTemplateTest() {
        String id = UUID.randomUUID().toString();
        String spaceId = "262345";
        String templateId = "819201";
        String title = "Krista - Test";
        List<Map<String, Object>> variables = List.of(Map.of("Key", "question", "Value", "What is Krista?"),
                Map.of("Key", "answer", "Value", "Krista is a modern conversational Intelligent Automation platform designed to easily leverage existing IT assets."));

        when(confluenceService.createPageFromTemplate(spaceId, templateId, title, variables)).thenReturn(new Page
                .PageBuilder(id, title)
                .spaceId(spaceId)
                .build());

        Page page = confluenceService.createPageFromTemplate(spaceId, templateId, title, variables);
        Assertions.assertNotNull(page);
        Assertions.assertEquals(id, page.getId());
        Assertions.assertEquals(spaceId, page.getSpaceId());
        Assertions.assertEquals(title, page.getTitle());

        verify(confluenceService).createPageFromTemplate(spaceId, templateId, title, variables);
        reset(xhtmlContentService, confluenceService);
    }


    @Test
    public void updatePageByIdTest() {
        String id = "2261017";
        String templateId = "819201";
        String title = "Update Krista Test";
        List<Map<String, Object>> placeHolders = List.of(Map.of("Key", "question", "Value", "What is an apple?"), Map.of("Key", "answer", "Value", "An apple is a fruit"));

        when(confluenceService.updatePageFromTemplate(id, templateId, Status.current.toString(), title, null, placeHolders))
                .thenReturn(new Page.PageBuilder(id, title).build());
        Page updatePage = confluenceService.updatePageFromTemplate(id, templateId, Status.current.toString(), title, null, placeHolders);
        Assertions.assertNotNull(updatePage);
        Assertions.assertEquals(id, updatePage.getId());
        Assertions.assertEquals(title, updatePage.getTitle());

        verify(confluenceService).updatePageFromTemplate(id, templateId, Status.current.toString(), title, null, placeHolders);
        reset(xhtmlContentService, confluenceService);
    }

    @Test
    public void getSpacesTest() {
        Space[] spacesArray = Stream.of(
                Space.builder()
                        .setId(UUID.randomUUID().toString())
                        .setAuthorId(UUID.randomUUID().toString())
                        .setHomepageId(UUID.randomUUID().toString()).build(),
                Space.builder()
                        .setId(UUID.randomUUID().toString())
                        .setAuthorId(UUID.randomUUID().toString())
                        .setHomepageId(UUID.randomUUID().toString()).build())
                .toArray(Space[]::new);
        when(confluenceService.getSpaces()).thenReturn(spacesArray);

        Space[] spaces = confluenceService.getSpaces();

        Assertions.assertTrue(spaces.length >= 2);
        Assertions.assertNotNull(spaces);

        verify(confluenceService).getSpaces();
        reset(xhtmlContentService, confluenceService);
    }

    @Test
    public void getSpaceByIdTest() {
        String id = "262345";

        when(confluenceService.getSpaceById(id)).thenReturn(Space.builder().setId(id).build());

        Space space = confluenceService.getSpaceById(id);
        Assertions.assertNotNull(space);
        Assertions.assertEquals(id, space.getId());

        verify(confluenceService).getSpaceById(id);
        reset(xhtmlContentService, confluenceService);
    }

    @Test
    public void getTemplateBySpaceIdTest() {
        String id = "262345";
        Template[] templateArray = Stream.of(Template.builder().setSpaceId(id).build(),
                Template.builder().setSpaceId(id).build()).toArray(Template[]::new);
        when(confluenceService.getTemplatesBySpaceId(id)).thenReturn(templateArray);

        Template[] templates = confluenceService.getTemplatesBySpaceId(id);

        Assertions.assertNotNull(templates);
        Arrays.stream(templates).forEach(template -> Assertions.assertEquals(id, template.getSpaceId()));

        verify(confluenceService).getTemplatesBySpaceId(id);
        reset(xhtmlContentService, confluenceService);
    }

    @Test
    public void getTemplateByIdTest() {
        String id = "2228225";

        when(confluenceService.getTemplateById(id)).thenReturn(Template.builder()
                        .setTemplateId(id)
                .build());
        Template template = confluenceService.getTemplateById(id);
        Assertions.assertNotNull(template);
        Assertions.assertEquals(id, template.getTemplateId());

        verify(confluenceService).getTemplateById(id);
        reset(xhtmlContentService, confluenceService);
    }

    @Test
    public void getPagesBySpaceIdTest() {
        String id = "262345";
        String title = "KristaSoft";
        Page[] pagesArray = Stream.of(new Page.PageBuilder(UUID.randomUUID().toString(), title).spaceId(id).build()).toArray(Page[]::new);
        when(confluenceService.getPagesBySpaceId(id)).thenReturn(pagesArray);

        Page[] pages = confluenceService.getPagesBySpaceId(id);

        Arrays.stream(pagesArray).findAny().ifPresent(page -> Assertions.assertEquals(id, page.getSpaceId()));
        Assertions.assertNotNull(pages);

        verify(confluenceService).getPagesBySpaceId(id);
        reset(xhtmlContentService, confluenceService);
    }

    @Test
    public void getPageByIdTest() {
        String id = "2064422";
        String title = "KristaSoft";

        when(confluenceService.getPageById(id)).thenReturn(new Page.PageBuilder(id, title)
                .build());
        Page page = confluenceService.getPageById(id);
        Assertions.assertNotNull(page);
        Assertions.assertEquals(id, page.getId());

        verify(confluenceService).getPageById(id);
        reset(xhtmlContentService, confluenceService);
    }

    @Test
    public void generateTableTest() {
        List<String> columns = List.of("Question", "Answer", "Explanation", "Confidence");
        List<Map<String, String>> table = new ArrayList<>();
        table.add(Map.of("Question", "Q1", "Answer", "A1", "Explanation", "E1", "Confidence", "C1"));
        table.add(Map.of("Question", "Q2", "Answer", "A2", "Explanation", "E2", "Confidence", "C2"));
        when(xhtmlContentService.createContentForTable(columns, table)).thenReturn(new Content());

        Content content = xhtmlContentService.createContentForTable(columns, table);
        Assertions.assertNotNull(content);

        verify(xhtmlContentService).createContentForTable(columns, table);
        reset(xhtmlContentService, confluenceService);
    }

    @Test
    public void createPageFromContent() {
        String spaceID = "262345";
        String template = "Krista Test Dynamic";
        String title = "KristaSoft Dynamic Table";

        // Anchor tag
        Content anchorTag = xhtmlContentService.createTickerSymbolContentForCompany("Johnson & Johnson");
        // Dynamic Table
        List<Map<String, String>> table = new ArrayList<>();
        List<String> columns = List.of("First Name", "Last Name");
        table.add(Map.of("First Name", "Alex", "Last Name", "Matos"));
        table.add(Map.of("First Name", "Steve", "Last Name", "Jobs"));


        when(xhtmlContentService.createContentForTable(columns, table)).thenReturn(new Content());
        Content content = xhtmlContentService.createContentForTable(columns, table);
        List<Content> contents = Stream.of(anchorTag, content).collect(Collectors.toList());

        when(confluenceService.createPageFromContent(spaceID, template, contents))
                .thenReturn(new Page.PageBuilder(UUID.randomUUID().toString(), title)
                        .spaceId(spaceID).build());

        Page dynamicPage = confluenceService.createPageFromContent(spaceID, template, contents);
        Assertions.assertNotNull(dynamicPage);
        Assertions.assertEquals(spaceID, dynamicPage.getSpaceId());

        reset(xhtmlContentService, confluenceService);
    }
}
