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

package com.kristasoft.extensions.essentials.collaboration.confluence.service.impl;

import app.krista.ksdk.files.FileHandle;
import app.krista.ksdk.files.FileRepository;
import app.krista.model.base.File;
import com.kristasoft.extensions.essentials.collaboration.confluence.dto.Body;
import com.kristasoft.extensions.essentials.collaboration.confluence.dto.Page;
import com.kristasoft.extensions.essentials.collaboration.confluence.dto.Storage;
import com.kristasoft.extensions.essentials.collaboration.confluence.exception.ConfluenceRESTException;
import com.kristasoft.extensions.essentials.collaboration.confluence.service.ConfluenceService;
import com.kristasoft.extensions.essentials.collaboration.confluence.service.PageMarkdownService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PageMarkdownServiceImplTest {

    @Mock
    private ConfluenceService confluenceService;

    @Mock
    private FileRepository fileRepository;

    @Mock
    private FileHandle fileHandle;

    @Mock
    private File file;

    private PageMarkdownService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new PageMarkdownServiceImpl(confluenceService, fileRepository);
    }

    @Test
    void buildFileNameSanitizesIllegalCharsAndWhitespace() {
        assertEquals("my_page_title.md",
                PageMarkdownServiceImpl.buildFileName("My Page Title", "123"));
    }

    @Test
    void buildFileNameReplacesPathAndQuoteCharacters() {
        assertEquals("a_b_c_d.md",
                PageMarkdownServiceImpl.buildFileName("a/b\\c:d", "999"));
    }

    @Test
    void buildFileNameFallsBackToPageIdWhenTitleSanitizesToEmpty() {
        assertEquals("12345.md",
                PageMarkdownServiceImpl.buildFileName("...", "12345"));
    }

    @Test
    void buildFileNameFallsBackToPageWhenTitleAndIdAreBlank() {
        assertEquals("page.md",
                PageMarkdownServiceImpl.buildFileName("", ""));
    }

    @Test
    void buildFileNameCapsLengthAt100() {
        String longTitle = "a".repeat(250);
        String result = PageMarkdownServiceImpl.buildFileName(longTitle, "id");
        assertTrue(result.endsWith(".md"));
        assertEquals(103, result.length(), "100 chars + .md = 103: " + result);
    }

    @Test
    void buildFileNameTrimsLeadingAndTrailingPunctuation() {
        assertEquals("clean.md",
                PageMarkdownServiceImpl.buildFileName(".__clean__.", "id"));
    }

    @Test
    void downloadConvertsBodyAndPassesUtf8BytesToFileRepository() throws IOException {
        Page page = makePage("999", "Release Plan", "<p>hello <strong>world</strong></p>");
        when(confluenceService.getPageById("999")).thenReturn(page);

        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        when(fileRepository.createNewFileByName(anyString())).thenReturn(fileHandle);
        when(fileHandle.getFile()).thenReturn(file);
        doAnswer(inv -> {
            InputStream in = inv.getArgument(0);
            in.transferTo(captured);
            return null;
        }).when(fileHandle).setContent(any(InputStream.class));

        File result = service.downloadPageAsMarkdownFile("999");

        assertNotNull(result);
        ArgumentCaptor<String> nameCap = ArgumentCaptor.forClass(String.class);
        verify(fileRepository).createNewFileByName(nameCap.capture());
        assertEquals("release_plan.md", nameCap.getValue());

        String md = captured.toString(StandardCharsets.UTF_8);
        assertTrue(md.startsWith("# Release Plan"), "title heading missing: " + md);
        assertTrue(md.contains("hello"), md);
        assertTrue(md.contains("world"), md);
    }

    @Test
    void downloadFallsBackToPageIdInFilenameWhenTitleIsBlank() throws IOException {
        Page page = makePage("777", "   ", "<p>body</p>");
        when(confluenceService.getPageById("777")).thenReturn(page);
        when(fileRepository.createNewFileByName(anyString())).thenReturn(fileHandle);
        when(fileHandle.getFile()).thenReturn(file);

        service.downloadPageAsMarkdownFile("777");

        ArgumentCaptor<String> nameCap = ArgumentCaptor.forClass(String.class);
        verify(fileRepository).createNewFileByName(nameCap.capture());
        assertEquals("777.md", nameCap.getValue());
    }

    @Test
    void downloadHandlesNullStorageBody() throws IOException {
        Page page = new Page.PageBuilder("abc", "T").build();
        when(confluenceService.getPageById("abc")).thenReturn(page);
        when(fileRepository.createNewFileByName(anyString())).thenReturn(fileHandle);
        when(fileHandle.getFile()).thenReturn(file);

        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        doAnswer(inv -> {
            InputStream in = inv.getArgument(0);
            in.transferTo(captured);
            return null;
        }).when(fileHandle).setContent(any(InputStream.class));

        File result = service.downloadPageAsMarkdownFile("abc");

        assertNotNull(result);
        assertEquals("# T\n", captured.toString(StandardCharsets.UTF_8));
    }

    @Test
    void downloadThrowsConfluenceRestExceptionWhenPageNotFound() {
        when(confluenceService.getPageById("missing")).thenReturn(null);

        ConfluenceRESTException ex = assertThrows(ConfluenceRESTException.class,
                () -> service.downloadPageAsMarkdownFile("missing"));
        assertTrue(ex.getMessage().contains("missing"), ex.getMessage());
        verify(fileRepository, never()).createNewFileByName(anyString());
    }

    @Test
    void downloadSurfacesIoFailureAsActionableException() throws IOException {
        Page page = makePage("p", "T", "<p>x</p>");
        when(confluenceService.getPageById("p")).thenReturn(page);
        when(fileRepository.createNewFileByName(anyString())).thenReturn(fileHandle);
        doAnswer(inv -> { throw new IOException("disk full"); })
                .when(fileHandle).setContent(any(InputStream.class));

        ConfluenceRESTException ex = assertThrows(ConfluenceRESTException.class,
                () -> service.downloadPageAsMarkdownFile("p"));
        assertTrue(ex.getMessage().contains("disk full"), ex.getMessage());
    }

    private Page makePage(String id, String title, String storageValue) {
        Storage storage = Storage.builder().setValue(storageValue).setRepresentation("storage").build();
        Body body = Body.setBody(storage);
        return new Page.PageBuilder(id, title).body(body).build();
    }
}
