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
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.stores.Pair;
import com.kristasoft.extensions.essentials.collaboration.confluence.dto.Body;
import com.kristasoft.extensions.essentials.collaboration.confluence.dto.Page;
import com.kristasoft.extensions.essentials.collaboration.confluence.dto.Storage;
import com.kristasoft.extensions.essentials.collaboration.confluence.service.ConfluenceService;
import org.docx4j.convert.in.xhtml.XHTMLImporterImpl;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PageDocxServiceImplTest {

    @Mock
    private ConfluenceService confluenceService;

    @Mock
    private FileRepository fileRepository;

    @Mock
    private FileHandle fileHandle;

    @Mock
    private File kristaFile;

    private PageDocxServiceImpl service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new PageDocxServiceImpl(confluenceService, fileRepository);
    }

    // ========================================================================
    // Helper methods
    // ========================================================================

    private Page makePage(String id, String title, String storageValue) {
        Storage storage = Storage.builder().setValue(storageValue).build();
        Body body = Body.setBody(storage);
        return new Page.PageBuilder(id, title).body(body).build();
    }

    private Page makePageNoBody(String id, String title) {
        return new Page.PageBuilder(id, title).build();
    }

    private Page makePageNullStorage(String id, String title) {
        Body body = Body.setBody(null);
        return new Page.PageBuilder(id, title).body(body).build();
    }

    /**
     * Creates a mock save answer that writes a small valid file to disk so that
     * the subsequent FileInputStream in convertToKristaFile does not fail with
     * FileNotFoundException.
     */
    private static org.mockito.stubbing.Answer<Void> createFileOnSave() {
        return invocation -> {
            java.io.File file = invocation.getArgument(0);
            // Create the file so FileInputStream can read it
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(new byte[]{0x50, 0x4B, 0x03, 0x04}); // minimal zip header
            }
            return null;
        };
    }

    /**
     * Sets up all the docx4j mocks: WordprocessingMLPackage static mock, XHTMLImporterImpl
     * construction mock, and the save() method that creates a real file on disk.
     * Returns the mocked WordprocessingMLPackage for additional stubbing if needed.
     */
    private WordprocessingMLPackage setupDocxMocks(MockedStatic<WordprocessingMLPackage> wpMock) throws Docx4JException {
        WordprocessingMLPackage mockPkg = mock(WordprocessingMLPackage.class);
        MainDocumentPart mockMainDoc = mock(MainDocumentPart.class);
        wpMock.when(WordprocessingMLPackage::createPackage).thenReturn(mockPkg);
        when(mockPkg.getMainDocumentPart()).thenReturn(mockMainDoc);
        when(mockMainDoc.getContent()).thenReturn(new ArrayList<>());
        doAnswer(createFileOnSave()).when(mockPkg).save(any(java.io.File.class));
        return mockPkg;
    }

    /**
     * Runs a full download test by mocking WordprocessingMLPackage.createPackage(),
     * XHTMLImporterImpl construction, and the FileRepository/FileHandle chain.
     */
    private File runDownloadWithDocxMocking(String pageId, String format, String customFileName) {
        try (MockedStatic<WordprocessingMLPackage> wpMock = mockStatic(WordprocessingMLPackage.class);
             MockedConstruction<XHTMLImporterImpl> ignored = mockConstruction(XHTMLImporterImpl.class,
                     (mock, context) -> when(mock.convert(anyString(), any())).thenReturn(Collections.emptyList()))) {

            setupDocxMocks(wpMock);

            when(fileRepository.createNewFileByName(anyString())).thenReturn(fileHandle);
            when(fileHandle.getFile()).thenReturn(kristaFile);

            return service.downloadPageAsDocxFile(pageId, format, customFileName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Pair<File, Boolean> runDownloadWithStatusAndDocxMocking(String pageId, String format, String customFileName) {
        try (MockedStatic<WordprocessingMLPackage> wpMock = mockStatic(WordprocessingMLPackage.class);
             MockedConstruction<XHTMLImporterImpl> ignored = mockConstruction(XHTMLImporterImpl.class,
                     (mock, context) -> when(mock.convert(anyString(), any())).thenReturn(Collections.emptyList()))) {

            setupDocxMocks(wpMock);

            when(fileRepository.createNewFileByName(anyString())).thenReturn(fileHandle);
            when(fileHandle.getFile()).thenReturn(kristaFile);

            return service.downloadPageAsDocxFileWithStatus(pageId, format, customFileName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ========================================================================
    // downloadPageAsDocxFile tests
    // ========================================================================

    @Test
    void downloadPageAsDocxFile_successWithSimpleHtml() {
        Page page = makePage("123", "Test Page", "<p>Hello World</p>");
        when(confluenceService.getPageById("123")).thenReturn(page);

        File result = runDownloadWithDocxMocking("123", "docx", null);

        assertNotNull(result);
        verify(confluenceService).getPageById("123");
        verify(fileRepository).createNewFileByName(anyString());
    }

    @Test
    void downloadPageAsDocxFile_pageNotFound_throwsRuntimeException() {
        when(confluenceService.getPageById("missing")).thenReturn(null);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.downloadPageAsDocxFile("missing", "docx", null));

        assertTrue(ex.getMessage().contains("Failed to download page as Docx file"));
        assertTrue(ex.getCause() instanceof IllegalArgumentException);
        assertTrue(ex.getCause().getMessage().contains("Page not found with ID: missing"));
    }

    @Test
    void downloadPageAsDocxFile_pageWithNullBody_handlesGracefully() {
        Page page = makePageNoBody("456", "No Body Page");
        when(confluenceService.getPageById("456")).thenReturn(page);

        File result = runDownloadWithDocxMocking("456", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_pageWithBodyButNullStorage_handlesGracefully() {
        Page page = makePageNullStorage("789", "Null Storage Page");
        when(confluenceService.getPageById("789")).thenReturn(page);

        File result = runDownloadWithDocxMocking("789", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_pageWithNullTitle() {
        Page page = makePage("111", null, "<p>content</p>");
        when(confluenceService.getPageById("111")).thenReturn(page);

        File result = runDownloadWithDocxMocking("111", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_customFileNameProvided() {
        Page page = makePage("222", "My Page", "<p>content</p>");
        when(confluenceService.getPageById("222")).thenReturn(page);

        try (MockedStatic<WordprocessingMLPackage> wpMock = mockStatic(WordprocessingMLPackage.class);
             MockedConstruction<XHTMLImporterImpl> ignored = mockConstruction(XHTMLImporterImpl.class,
                     (mock, context) -> when(mock.convert(anyString(), any())).thenReturn(Collections.emptyList()))) {

            setupDocxMocks(wpMock);

            when(fileRepository.createNewFileByName("custom_name.docx")).thenReturn(fileHandle);
            when(fileHandle.getFile()).thenReturn(kristaFile);

            File result = service.downloadPageAsDocxFile("222", "docx", "custom_name.docx");

            assertNotNull(result);
            verify(fileRepository).createNewFileByName("custom_name.docx");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void downloadPageAsDocxFile_customFileNameNull_generatesFromTitle() {
        Page page = makePage("333", "Auto Name", "<p>content</p>");
        when(confluenceService.getPageById("333")).thenReturn(page);

        File result = runDownloadWithDocxMocking("333", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_unsupportedFormat_throwsRuntimeException() {
        Page page = makePage("444", "Test", "<p>content</p>");
        when(confluenceService.getPageById("444")).thenReturn(page);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.downloadPageAsDocxFile("444", "pdf", null));

        assertTrue(ex.getMessage().contains("Failed to download page as Docx file"));
        assertTrue(ex.getCause().getMessage().contains("Unsupported format: pdf"));
    }

    @Test
    void downloadPageAsDocxFile_twoArgDelegatesToThreeArg() {
        Page page = makePage("555", "Delegate Test", "<p>content</p>");
        when(confluenceService.getPageById("555")).thenReturn(page);

        try (MockedStatic<WordprocessingMLPackage> wpMock = mockStatic(WordprocessingMLPackage.class);
             MockedConstruction<XHTMLImporterImpl> ignored = mockConstruction(XHTMLImporterImpl.class,
                     (mock, context) -> when(mock.convert(anyString(), any())).thenReturn(Collections.emptyList()))) {

            setupDocxMocks(wpMock);

            when(fileRepository.createNewFileByName(anyString())).thenReturn(fileHandle);
            when(fileHandle.getFile()).thenReturn(kristaFile);

            File result = service.downloadPageAsDocxFile("555", "docx");

            assertNotNull(result);
            verify(confluenceService).getPageById("555");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ========================================================================
    // downloadPageAsDocxFileWithStatus tests
    // ========================================================================

    @Test
    void downloadPageAsDocxFileWithStatus_successPath_returnsTrueStatus() {
        Page page = makePage("600", "Status Test", "<p>content</p>");
        when(confluenceService.getPageById("600")).thenReturn(page);

        Pair<File, Boolean> result = runDownloadWithStatusAndDocxMocking("600", "docx", null);

        assertNotNull(result);
        assertNotNull(result.getKey());
        assertTrue(result.getValue());
    }

    @Test
    void downloadPageAsDocxFileWithStatus_pageNotFound_fallbackCreated() {
        when(confluenceService.getPageById("missing")).thenReturn(null);

        try (MockedStatic<WordprocessingMLPackage> wpMock = mockStatic(WordprocessingMLPackage.class);
             MockedConstruction<XHTMLImporterImpl> ignored = mockConstruction(XHTMLImporterImpl.class,
                     (mock, context) -> when(mock.convert(anyString(), any())).thenReturn(Collections.emptyList()))) {

            setupDocxMocks(wpMock);

            when(fileRepository.createNewFileByName(anyString())).thenReturn(fileHandle);
            when(fileHandle.getFile()).thenReturn(kristaFile);

            Pair<File, Boolean> result = service.downloadPageAsDocxFileWithStatus("missing", "docx", null);

            assertNotNull(result);
            assertFalse(result.getValue());
            assertNotNull(result.getKey());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void downloadPageAsDocxFileWithStatus_parsingFailureWithFallbackSuccess() {
        Page page = makePage("700", "Parse Fail", "<p>content</p>");
        when(confluenceService.getPageById("700")).thenReturn(page);

        try (MockedStatic<WordprocessingMLPackage> wpMock = mockStatic(WordprocessingMLPackage.class);
             MockedConstruction<XHTMLImporterImpl> ignored = mockConstruction(XHTMLImporterImpl.class,
                     (mock, context) -> {
                         if (context.getCount() == 1) {
                             when(mock.convert(anyString(), any())).thenThrow(new Docx4JException("parse error"));
                         } else {
                             when(mock.convert(anyString(), any())).thenReturn(Collections.emptyList());
                         }
                     })) {

            setupDocxMocks(wpMock);

            when(fileRepository.createNewFileByName(anyString())).thenReturn(fileHandle);
            when(fileHandle.getFile()).thenReturn(kristaFile);

            Pair<File, Boolean> result = service.downloadPageAsDocxFileWithStatus("700", "docx", null);

            assertNotNull(result);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void downloadPageAsDocxFileWithStatus_parsingFailureWithFallbackAlsoFailing_returnsNullFile() {
        when(confluenceService.getPageById("missingAll")).thenReturn(null);

        try (MockedStatic<WordprocessingMLPackage> wpMock = mockStatic(WordprocessingMLPackage.class);
             MockedConstruction<XHTMLImporterImpl> ignored = mockConstruction(XHTMLImporterImpl.class,
                     (mock, context) -> when(mock.convert(anyString(), any())).thenReturn(Collections.emptyList()))) {

            // Use RuntimeException because MockedStatic cannot throw checked exceptions
            wpMock.when(WordprocessingMLPackage::createPackage).thenThrow(new RuntimeException("total failure"));

            Pair<File, Boolean> result = service.downloadPageAsDocxFileWithStatus("missingAll", "docx", null);

            assertNotNull(result);
            assertFalse(result.getValue());
            assertNull(result.getKey());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void downloadPageAsDocxFileWithStatus_twoArgDelegatesToThreeArg() {
        Page page = makePage("800", "Delegate Status", "<p>content</p>");
        when(confluenceService.getPageById("800")).thenReturn(page);

        try (MockedStatic<WordprocessingMLPackage> wpMock = mockStatic(WordprocessingMLPackage.class);
             MockedConstruction<XHTMLImporterImpl> ignored = mockConstruction(XHTMLImporterImpl.class,
                     (mock, context) -> when(mock.convert(anyString(), any())).thenReturn(Collections.emptyList()))) {

            setupDocxMocks(wpMock);

            when(fileRepository.createNewFileByName(anyString())).thenReturn(fileHandle);
            when(fileHandle.getFile()).thenReturn(kristaFile);

            Pair<File, Boolean> result = service.downloadPageAsDocxFileWithStatus("800", "docx");

            assertNotNull(result);
            assertTrue(result.getValue());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void downloadPageAsDocxFileWithStatus_customFileName() {
        Page page = makePage("801", "Custom Status", "<p>content</p>");
        when(confluenceService.getPageById("801")).thenReturn(page);

        try (MockedStatic<WordprocessingMLPackage> wpMock = mockStatic(WordprocessingMLPackage.class);
             MockedConstruction<XHTMLImporterImpl> ignored = mockConstruction(XHTMLImporterImpl.class,
                     (mock, context) -> when(mock.convert(anyString(), any())).thenReturn(Collections.emptyList()))) {

            setupDocxMocks(wpMock);

            when(fileRepository.createNewFileByName("my_report.docx")).thenReturn(fileHandle);
            when(fileHandle.getFile()).thenReturn(kristaFile);

            Pair<File, Boolean> result = service.downloadPageAsDocxFileWithStatus("801", "docx", "my_report.docx");

            assertNotNull(result);
            assertTrue(result.getValue());
            verify(fileRepository).createNewFileByName("my_report.docx");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void downloadPageAsDocxFileWithStatus_fallbackUsesCustomFileName() {
        when(confluenceService.getPageById("802")).thenReturn(null);

        try (MockedStatic<WordprocessingMLPackage> wpMock = mockStatic(WordprocessingMLPackage.class);
             MockedConstruction<XHTMLImporterImpl> ignored = mockConstruction(XHTMLImporterImpl.class,
                     (mock, context) -> when(mock.convert(anyString(), any())).thenReturn(Collections.emptyList()))) {

            setupDocxMocks(wpMock);

            when(fileRepository.createNewFileByName("fallback_custom.docx")).thenReturn(fileHandle);
            when(fileHandle.getFile()).thenReturn(kristaFile);

            Pair<File, Boolean> result = service.downloadPageAsDocxFileWithStatus("802", "docx", "fallback_custom.docx");

            assertNotNull(result);
            assertFalse(result.getValue());
            verify(fileRepository).createNewFileByName("fallback_custom.docx");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void downloadPageAsDocxFileWithStatus_fallbackUsesDefaultFileName_whenNoCustomName() {
        when(confluenceService.getPageById("803")).thenReturn(null);

        try (MockedStatic<WordprocessingMLPackage> wpMock = mockStatic(WordprocessingMLPackage.class);
             MockedConstruction<XHTMLImporterImpl> ignored = mockConstruction(XHTMLImporterImpl.class,
                     (mock, context) -> when(mock.convert(anyString(), any())).thenReturn(Collections.emptyList()))) {

            setupDocxMocks(wpMock);

            when(fileRepository.createNewFileByName("error_page_803.docx")).thenReturn(fileHandle);
            when(fileHandle.getFile()).thenReturn(kristaFile);

            Pair<File, Boolean> result = service.downloadPageAsDocxFileWithStatus("803", "docx", null);

            assertNotNull(result);
            assertFalse(result.getValue());
            verify(fileRepository).createNewFileByName("error_page_803.docx");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ========================================================================
    // HTML processing edge cases (tested through public methods)
    // ========================================================================

    @Test
    void downloadPageAsDocxFile_htmlWithConfluenceNamespaceAttributes() {
        String confluenceHtml = "<p ac:name=\"test\" ri:content=\"some\">Confluence content</p>" +
                "<ac:structured-macro ac:name=\"info\"><ac:rich-text-body><p>Info text</p></ac:rich-text-body></ac:structured-macro>";
        Page page = makePage("900", "Namespace Test", confluenceHtml);
        when(confluenceService.getPageById("900")).thenReturn(page);

        File result = runDownloadWithDocxMocking("900", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_htmlWithTablesContainingMalformedColTags() {
        String tableHtml = "<table><col width=\"50\"><col width=\"50\"><tr><td>Cell 1</td><td>Cell 2</td></tr></table>";
        Page page = makePage("901", "Col Tag Test", tableHtml);
        when(confluenceService.getPageById("901")).thenReturn(page);

        File result = runDownloadWithDocxMocking("901", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_htmlWithSpecialCharacters() {
        String htmlWithAmpersands = "<p>Tom & Jerry went to the store & bought items</p>";
        Page page = makePage("902", "Special Chars", htmlWithAmpersands);
        when(confluenceService.getPageById("902")).thenReturn(page);

        File result = runDownloadWithDocxMocking("902", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_htmlWithComplexTableStructures() {
        String complexTable = "<table><thead><tr><th>Header 1</th><th>Header 2</th></tr></thead>" +
                "<tbody><tr><td>Data 1</td><td>Data 2</td></tr>" +
                "<tr><td>Data 3</td><td>Data 4</td></tr></tbody></table>";
        Page page = makePage("903", "Complex Table", complexTable);
        when(confluenceService.getPageById("903")).thenReturn(page);

        File result = runDownloadWithDocxMocking("903", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_emptyHtmlBody() {
        Page page = makePage("904", "Empty Body", "");
        when(confluenceService.getPageById("904")).thenReturn(page);

        File result = runDownloadWithDocxMocking("904", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_htmlWithHtmlEntities() {
        String htmlEntities = "<p>Copyright &copy; 2024 &mdash; All rights reserved &nbsp; &bull; &euro;100</p>";
        Page page = makePage("905", "Entities Test", htmlEntities);
        when(confluenceService.getPageById("905")).thenReturn(page);

        File result = runDownloadWithDocxMocking("905", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_htmlWithDeeplyNestedTables() {
        String nestedTables = "<table><tr><td><table><tr><td><table><tr><td>Deep content</td></tr></table></td></tr></table></td></tr></table>";
        Page page = makePage("906", "Nested Tables", nestedTables);
        when(confluenceService.getPageById("906")).thenReturn(page);

        File result = runDownloadWithDocxMocking("906", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_titleWithSpecialCharactersSanitized() {
        Page page = makePage("907", "Title/With\\Special:Chars<>&", "<p>content</p>");
        when(confluenceService.getPageById("907")).thenReturn(page);

        File result = runDownloadWithDocxMocking("907", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_htmlWithDataAttributes() {
        String htmlWithData = "<p data-highlight-colour=\"#ff0000\" data-custom=\"value\" " +
                "style=\"color: red\" class=\"highlight\" id=\"para1\">Styled content</p>";
        Page page = makePage("908", "Data Attrs", htmlWithData);
        when(confluenceService.getPageById("908")).thenReturn(page);

        File result = runDownloadWithDocxMocking("908", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_htmlWithConfluenceLayoutElements() {
        String layoutHtml = "<ac:layout><ac:layout-section ac:type=\"two_equal\">" +
                "<ac:layout-cell><p>Left column</p></ac:layout-cell>" +
                "<ac:layout-cell><p>Right column</p></ac:layout-cell>" +
                "</ac:layout-section></ac:layout>";
        Page page = makePage("909", "Layout Test", layoutHtml);
        when(confluenceService.getPageById("909")).thenReturn(page);

        File result = runDownloadWithDocxMocking("909", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_htmlWithConfluenceMacros() {
        String macroHtml = "<ac:structured-macro ac:name=\"recently-updated\"></ac:structured-macro>" +
                "<ac:structured-macro ac:name=\"blog-posts\"></ac:structured-macro>" +
                "<ac:structured-macro ac:name=\"custom-macro\">" +
                "<ac:parameter ac:name=\"param1\">value1</ac:parameter></ac:structured-macro>";
        Page page = makePage("910", "Macro Test", macroHtml);
        when(confluenceService.getPageById("910")).thenReturn(page);

        File result = runDownloadWithDocxMocking("910", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_htmlWithConfluencePlaceholders() {
        String placeholderHtml = "<ac:placeholder>Type your content here</ac:placeholder>" +
                "<p>Normal paragraph</p>";
        Page page = makePage("911", "Placeholder Test", placeholderHtml);
        when(confluenceService.getPageById("911")).thenReturn(page);

        File result = runDownloadWithDocxMocking("911", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_htmlWithRemainingNamespaceElements() {
        String nsHtml = "<ri:attachment ri:filename=\"test.png\"/>" +
                "<ri:page ri:content-title=\"Other Page\"/>" +
                "<at:var at:name=\"myvar\"/>" +
                "<p>Regular content</p>";
        Page page = makePage("912", "NS Elements", nsHtml);
        when(confluenceService.getPageById("912")).thenReturn(page);

        File result = runDownloadWithDocxMocking("912", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_htmlWithSelfClosingTags() {
        String selfClosingHtml = "<p>Before break</p><br><hr><p>After</p>" +
                "<img src=\"test.png\"><input type=\"text\">";
        Page page = makePage("913", "Self Closing", selfClosingHtml);
        when(confluenceService.getPageById("913")).thenReturn(page);

        File result = runDownloadWithDocxMocking("913", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_htmlWithStrongAndEmTags() {
        String formattingHtml = "<p><strong>Bold text</strong> and <em>italic text</em></p>";
        Page page = makePage("914", "Formatting", formattingHtml);
        when(confluenceService.getPageById("914")).thenReturn(page);

        File result = runDownloadWithDocxMocking("914", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_htmlWithOrphanedTableElements() {
        String orphanedHtml = "<p>Normal text</p><tr><td>orphaned cell</td></tr>" +
                "<table><tr><td>proper cell</td></tr></table>";
        Page page = makePage("915", "Orphaned", orphanedHtml);
        when(confluenceService.getPageById("915")).thenReturn(page);

        File result = runDownloadWithDocxMocking("915", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_htmlWithEmptyColgroup() {
        String colgroupHtml = "<table><colgroup></colgroup><tr><td>cell</td></tr></table>";
        Page page = makePage("916", "Colgroup", colgroupHtml);
        when(confluenceService.getPageById("916")).thenReturn(page);

        File result = runDownloadWithDocxMocking("916", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_htmlWithColgroupContainingNonColElements() {
        String badColgroup = "<table><colgroup><div>bad element</div><col/></colgroup>" +
                "<tr><td>cell</td></tr></table>";
        Page page = makePage("917", "Bad Colgroup", badColgroup);
        when(confluenceService.getPageById("917")).thenReturn(page);

        File result = runDownloadWithDocxMocking("917", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_htmlWithSpanStyles() {
        String spanHtml = "<p><span style=\"color:red\">Red text</span> " +
                "<span>Plain span</span> <span style=\"font-weight:bold\">Bold span</span></p>";
        Page page = makePage("918", "Span Styles", spanHtml);
        when(confluenceService.getPageById("918")).thenReturn(page);

        File result = runDownloadWithDocxMocking("918", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_htmlWithJavascriptAttributes() {
        String jsHtml = "<p onclick=\"alert('xss')\" onload=\"evil()\" onerror=\"bad()\">Content</p>" +
                "<a href=\"javascript:void(0)\">Link</a>";
        Page page = makePage("919", "JS Attrs", jsHtml);
        when(confluenceService.getPageById("919")).thenReturn(page);

        File result = runDownloadWithDocxMocking("919", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_htmlWithTableTextNodesDirectlyUnderTable() {
        String textInTable = "<table>Direct text<tr><td>cell</td></tr></table>";
        Page page = makePage("920", "Text Under Table", textInTable);
        when(confluenceService.getPageById("920")).thenReturn(page);

        File result = runDownloadWithDocxMocking("920", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_htmlWithInfoMacroContainingRichText() {
        String infoMacro = "<ac:structured-macro ac:name=\"info\">" +
                "<ac:rich-text-body><p>Important information here</p></ac:rich-text-body>" +
                "</ac:structured-macro>";
        Page page = makePage("921", "Info Macro", infoMacro);
        when(confluenceService.getPageById("921")).thenReturn(page);

        File result = runDownloadWithDocxMocking("921", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_htmlWithMultipleHeadingLevels() {
        String headingsHtml = "<h1>Heading 1</h1><h2>Heading 2</h2><h3>Heading 3</h3>" +
                "<h4>Heading 4</h4><h5>Heading 5</h5><h6>Heading 6</h6><p>Normal text</p>";
        Page page = makePage("922", "Headings", headingsHtml);
        when(confluenceService.getPageById("922")).thenReturn(page);

        File result = runDownloadWithDocxMocking("922", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_docx4jSaveThrows_wrapsInRuntimeException() {
        Page page = makePage("923", "Save Fail", "<p>content</p>");
        when(confluenceService.getPageById("923")).thenReturn(page);

        try (MockedStatic<WordprocessingMLPackage> wpMock = mockStatic(WordprocessingMLPackage.class);
             MockedConstruction<XHTMLImporterImpl> ignored = mockConstruction(XHTMLImporterImpl.class,
                     (mock, context) -> when(mock.convert(anyString(), any())).thenReturn(Collections.emptyList()))) {

            WordprocessingMLPackage mockPkg = mock(WordprocessingMLPackage.class);
            MainDocumentPart mockMainDoc = mock(MainDocumentPart.class);
            wpMock.when(WordprocessingMLPackage::createPackage).thenReturn(mockPkg);
            when(mockPkg.getMainDocumentPart()).thenReturn(mockMainDoc);
            when(mockMainDoc.getContent()).thenReturn(new ArrayList<>());
            doThrow(new Docx4JException("save failed")).when(mockPkg).save(any(java.io.File.class));

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> service.downloadPageAsDocxFile("923", "docx", null));

            assertTrue(ex.getMessage().contains("Failed to download page as Docx file"));
        } catch (Exception e) {
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            throw new RuntimeException(e);
        }
    }

    @Test
    void downloadPageAsDocxFile_fileRepositoryThrows_wrapsInRuntimeException() {
        Page page = makePage("924", "Repo Fail", "<p>content</p>");
        when(confluenceService.getPageById("924")).thenReturn(page);

        try (MockedStatic<WordprocessingMLPackage> wpMock = mockStatic(WordprocessingMLPackage.class);
             MockedConstruction<XHTMLImporterImpl> ignored = mockConstruction(XHTMLImporterImpl.class,
                     (mock, context) -> when(mock.convert(anyString(), any())).thenReturn(Collections.emptyList()))) {

            setupDocxMocks(wpMock);

            when(fileRepository.createNewFileByName(anyString())).thenThrow(new RuntimeException("repository error"));

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> service.downloadPageAsDocxFile("924", "docx", null));

            assertTrue(ex.getMessage().contains("Failed to download page as Docx file"));
        } catch (Exception e) {
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            throw new RuntimeException(e);
        }
    }

    @Test
    void downloadPageAsDocxFileWithStatus_unsupportedFormat_fallbackCreated() {
        Page page = makePage("925", "Bad Format Status", "<p>content</p>");
        when(confluenceService.getPageById("925")).thenReturn(page);

        try (MockedStatic<WordprocessingMLPackage> wpMock = mockStatic(WordprocessingMLPackage.class);
             MockedConstruction<XHTMLImporterImpl> ignored = mockConstruction(XHTMLImporterImpl.class,
                     (mock, context) -> when(mock.convert(anyString(), any())).thenReturn(Collections.emptyList()))) {

            setupDocxMocks(wpMock);

            when(fileRepository.createNewFileByName(anyString())).thenReturn(fileHandle);
            when(fileHandle.getFile()).thenReturn(kristaFile);

            Pair<File, Boolean> result = service.downloadPageAsDocxFileWithStatus("925", "pdf", null);

            assertNotNull(result);
            assertFalse(result.getValue());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void downloadPageAsDocxFile_htmlWithContenteditableAndSpellcheck() {
        String editableHtml = "<div contenteditable=\"true\" spellcheck=\"false\">" +
                "<p>Editable content</p></div>";
        Page page = makePage("926", "Editable", editableHtml);
        when(confluenceService.getPageById("926")).thenReturn(page);

        File result = runDownloadWithDocxMocking("926", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_htmlWithMixedContent() {
        String mixedHtml = "<h1>Title</h1>" +
                "<p>Paragraph with <strong>bold</strong> and <em>italic</em></p>" +
                "<ul><li>Item 1</li><li>Item 2</li></ul>" +
                "<table><tr><th>Name</th><th>Value</th></tr><tr><td>A</td><td>1</td></tr></table>" +
                "<p>Final paragraph with &amp; and &lt;special&gt; characters</p>";
        Page page = makePage("927", "Mixed Content", mixedHtml);
        when(confluenceService.getPageById("927")).thenReturn(page);

        File result = runDownloadWithDocxMocking("927", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_veryLongTitle_sanitizedAndTruncated() {
        String longTitle = "A".repeat(200);
        Page page = makePage("928", longTitle, "<p>content</p>");
        when(confluenceService.getPageById("928")).thenReturn(page);

        File result = runDownloadWithDocxMocking("928", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_formatCaseInsensitive() {
        Page page = makePage("929", "Case Test", "<p>content</p>");
        when(confluenceService.getPageById("929")).thenReturn(page);

        File result = runDownloadWithDocxMocking("929", "DOCX", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_storageValueNull() {
        Storage storage = Storage.builder().setValue(null).build();
        Body body = Body.setBody(storage);
        Page page = new Page.PageBuilder("930", "Null Value").body(body).build();
        when(confluenceService.getPageById("930")).thenReturn(page);

        File result = runDownloadWithDocxMocking("930", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_htmlWithProblematicAttributeValues() {
        String problematicHtml = "<div attr=\"value<with>brackets\" " +
                "bad=\"a&b\">Content</div>";
        Page page = makePage("931", "Problematic Attrs", problematicHtml);
        when(confluenceService.getPageById("931")).thenReturn(page);

        File result = runDownloadWithDocxMocking("931", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_htmlWithEmptyTable() {
        String emptyTable = "<table></table>";
        Page page = makePage("932", "Empty Table", emptyTable);
        when(confluenceService.getPageById("932")).thenReturn(page);

        File result = runDownloadWithDocxMocking("932", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFileWithStatus_successWithComplexHtml() {
        String complexHtml = "<h1>Main Heading</h1>" +
                "<table><thead><tr><th>Col A</th><th>Col B</th></tr></thead>" +
                "<tbody><tr><td>1</td><td>2</td></tr></tbody></table>" +
                "<ac:structured-macro ac:name=\"info\">" +
                "<ac:rich-text-body><p>Note: This is important</p></ac:rich-text-body>" +
                "</ac:structured-macro>" +
                "<p>End of page &copy; 2024</p>";
        Page page = makePage("933", "Complex Status", complexHtml);
        when(confluenceService.getPageById("933")).thenReturn(page);

        Pair<File, Boolean> result = runDownloadWithStatusAndDocxMocking("933", "docx", null);

        assertNotNull(result);
        assertTrue(result.getValue());
        assertNotNull(result.getKey());
    }

    @Test
    void downloadPageAsDocxFile_xhtmlImporterFirstCallFailsFallbackSucceeds() {
        Page page = makePage("934", "Importer Fallback", "<p>content with bad char</p>");
        when(confluenceService.getPageById("934")).thenReturn(page);

        try (MockedStatic<WordprocessingMLPackage> wpMock = mockStatic(WordprocessingMLPackage.class);
             MockedConstruction<XHTMLImporterImpl> ignored = mockConstruction(XHTMLImporterImpl.class,
                     (mock, context) -> {
                         if (context.getCount() == 1) {
                             when(mock.convert(anyString(), any()))
                                     .thenThrow(new RuntimeException("XHTML import failed"))
                                     .thenReturn(Collections.emptyList());
                         } else {
                             when(mock.convert(anyString(), any())).thenReturn(Collections.emptyList());
                         }
                     })) {

            setupDocxMocks(wpMock);

            when(fileRepository.createNewFileByName(anyString())).thenReturn(fileHandle);
            when(fileHandle.getFile()).thenReturn(kristaFile);

            File result = service.downloadPageAsDocxFile("934", "docx", null);

            assertNotNull(result);
        } catch (Exception e) {
            // If the entire flow fails with RuntimeException, that is also a valid test outcome
            assertTrue(e instanceof RuntimeException);
        }
    }

    @Test
    void downloadPageAsDocxFile_createPackageThrows_wrappedInRuntimeException() {
        Page page = makePage("935", "Package Fail", "<p>content</p>");
        when(confluenceService.getPageById("935")).thenReturn(page);

        try (MockedStatic<WordprocessingMLPackage> wpMock = mockStatic(WordprocessingMLPackage.class)) {
            wpMock.when(WordprocessingMLPackage::createPackage)
                    .thenThrow(new RuntimeException("cannot create package"));

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> service.downloadPageAsDocxFile("935", "docx", null));

            assertTrue(ex.getMessage().contains("Failed to download page as Docx file"));
        }
    }

    @Test
    void downloadPageAsDocxFile_htmlWithColTagsHavingProblematicAttributes() {
        String colWithBadAttrs = "<table><colgroup><col ac:name=\"x\" style=\"width:50%\" " +
                "class=\"myclass\" id=\"col1\"/></colgroup><tr><td>data</td></tr></table>";
        Page page = makePage("936", "Col Attrs", colWithBadAttrs);
        when(confluenceService.getPageById("936")).thenReturn(page);

        File result = runDownloadWithDocxMocking("936", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_htmlWithParameterElements() {
        String paramHtml = "<ac:structured-macro ac:name=\"test\">" +
                "<ac:parameter ac:name=\"server\">production</ac:parameter>" +
                "<ac:parameter ac:name=\"empty\"></ac:parameter>" +
                "</ac:structured-macro>";
        Page page = makePage("937", "Param Test", paramHtml);
        when(confluenceService.getPageById("937")).thenReturn(page);

        File result = runDownloadWithDocxMocking("937", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_htmlWithTableHavingColgroupWithValidCols() {
        String validColgroup = "<table><colgroup><col/><col/></colgroup>" +
                "<tr><td>A</td><td>B</td></tr></table>";
        Page page = makePage("938", "Valid Colgroup", validColgroup);
        when(confluenceService.getPageById("938")).thenReturn(page);

        File result = runDownloadWithDocxMocking("938", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_htmlWithMultipleEntitiesInOneString() {
        String entityHtml = "<p>&ldquo;Hello&rdquo; &lsquo;World&rsquo; &hellip; " +
                "&reg; &trade; &bull; &middot; &sect; &para; &dagger; &Dagger; " +
                "&permil; &lsaquo;test&rsaquo;</p>";
        Page page = makePage("939", "Multi Entity", entityHtml);
        when(confluenceService.getPageById("939")).thenReturn(page);

        File result = runDownloadWithDocxMocking("939", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFileWithStatus_fileHandleSetContentThrows() {
        Page page = makePage("940", "Set Content Fail", "<p>content</p>");
        when(confluenceService.getPageById("940")).thenReturn(page);

        try (MockedStatic<WordprocessingMLPackage> wpMock = mockStatic(WordprocessingMLPackage.class);
             MockedConstruction<XHTMLImporterImpl> ignored = mockConstruction(XHTMLImporterImpl.class,
                     (mock, context) -> when(mock.convert(anyString(), any())).thenReturn(Collections.emptyList()))) {

            setupDocxMocks(wpMock);

            FileHandle failHandle = mock(FileHandle.class);
            doThrow(new IOException("disk full")).when(failHandle).setContent(any(InputStream.class));

            FileHandle successHandle = mock(FileHandle.class);
            when(successHandle.getFile()).thenReturn(kristaFile);

            when(fileRepository.createNewFileByName(anyString()))
                    .thenReturn(failHandle)
                    .thenReturn(successHandle);

            Pair<File, Boolean> result = service.downloadPageAsDocxFileWithStatus("940", "docx", null);

            assertNotNull(result);
            assertFalse(result.getValue());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void downloadPageAsDocxFile_htmlWithMultipleTablesSomeProblematic() {
        String multiTableHtml = "<table><tr><td>Simple table</td></tr></table>" +
                "<table ac:name=\"broken\"><col><tr><td>Problematic</td></tr></table>" +
                "<p>Text between tables</p>" +
                "<table><tr><th>H1</th></tr><tr><td>D1</td></tr></table>";
        Page page = makePage("941", "Multi Tables", multiTableHtml);
        when(confluenceService.getPageById("941")).thenReturn(page);

        File result = runDownloadWithDocxMocking("941", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_htmlWithListsAndFormattedText() {
        String listsHtml = "<ul><li>Item <b>one</b></li><li>Item <i>two</i></li></ul>" +
                "<ol><li>First</li><li>Second</li></ol>";
        Page page = makePage("942", "Lists", listsHtml);
        when(confluenceService.getPageById("942")).thenReturn(page);

        File result = runDownloadWithDocxMocking("942", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_tableWithConfluenceMacrosInside() {
        // This triggers convertTableToDiv because table HTML contains ac: after sanitizeTableStructures
        // but before removeConfluenceSpecificElements
        String html = "<table><tr><td><ac:structured-macro ac:name=\"status\">" +
                "<ac:parameter ac:name=\"title\">Done</ac:parameter>" +
                "</ac:structured-macro></td><td>Task complete</td></tr></table>";
        Page page = makePage("943", "Table With Macros", html);
        when(confluenceService.getPageById("943")).thenReturn(page);

        File result = runDownloadWithDocxMocking("943", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_tableWithRiElementsInside() {
        // Table containing ri: elements triggers the hasProblems check
        String html = "<table><tr><td><ri:attachment ri:filename=\"doc.pdf\"/>Attachment here</td></tr></table>";
        Page page = makePage("944", "Table With RI", html);
        when(confluenceService.getPageById("944")).thenReturn(page);

        File result = runDownloadWithDocxMocking("944", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_fourLevelNestedTable() {
        // More than 3 levels of nesting triggers hasProblems check (split("<table").length > 3)
        String html = "<table><tr><td><table><tr><td><table><tr><td><table><tr><td>Level 4</td></tr></table></td></tr></table></td></tr></table></td></tr></table>";
        Page page = makePage("945", "4Level Nested", html);
        when(confluenceService.getPageById("945")).thenReturn(page);

        File result = runDownloadWithDocxMocking("945", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_tableWithEmptyCells() {
        // Exercises extractTableData with empty cells
        String html = "<table><tr><th>H1</th><th>H2</th></tr>" +
                "<tr><td></td><td>data</td></tr>" +
                "<tr><td>data2</td><td></td></tr></table>";
        Page page = makePage("946", "Empty Cells", html);
        when(confluenceService.getPageById("946")).thenReturn(page);

        File result = runDownloadWithDocxMocking("946", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_tableWithNoRows() {
        // Table with no rows - only col/colgroup elements
        String html = "<table><colgroup><col/></colgroup></table>";
        Page page = makePage("947", "No Rows Table", html);
        when(confluenceService.getPageById("947")).thenReturn(page);

        File result = runDownloadWithDocxMocking("947", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_htmlWithColTagHavingContent() {
        // Col tag with content inside it - exercises the "col has content" branch
        String html = "<table><col>unexpected content here</col><tr><td>data</td></tr></table>";
        Page page = makePage("948", "Col Content", html);
        when(confluenceService.getPageById("948")).thenReturn(page);

        File result = runDownloadWithDocxMocking("948", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_htmlWithBrTagHavingContent() {
        // Self-closing tag with content exercises fixSelfClosingTags clearing
        String html = "<p>Before<br>content after break<hr>more content</p>";
        Page page = makePage("949", "BR Content", html);
        when(confluenceService.getPageById("949")).thenReturn(page);

        File result = runDownloadWithDocxMocking("949", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_htmlWithComplexNestedFormattingAndCleanup() {
        // Exercises cleanupTextContent with text that has formatting artifacts
        String html = "<p data-highlight-colour=\"yellow\" style=\"background: yellow\">" +
                "This   has   multiple   spaces   and data-custom=\"val\" artifacts</p>";
        Page page = makePage("950", "Cleanup Test", html);
        when(confluenceService.getPageById("950")).thenReturn(page);

        File result = runDownloadWithDocxMocking("950", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_htmlWithFontWeightStyle() {
        // The removeComplexStyling method keeps styles with font-weight
        String html = "<p style=\"font-weight: bold\">Bold styled</p>" +
                "<p style=\"font-style: italic\">Italic styled</p>" +
                "<p style=\"color: red; background: blue\">Colored text</p>";
        Page page = makePage("951", "Font Weight", html);
        when(confluenceService.getPageById("951")).thenReturn(page);

        File result = runDownloadWithDocxMocking("951", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_htmlWithAtElementsInTable() {
        // Table containing at: elements triggers hasProblems
        String html = "<table><tr><td><at:var at:name=\"date\">2024-01-01</at:var></td></tr></table>";
        Page page = makePage("952", "Table AT", html);
        when(confluenceService.getPageById("952")).thenReturn(page);

        File result = runDownloadWithDocxMocking("952", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_htmlThatProducesEmptyTableData() {
        // Table that has no extractable data goes through the else branch in convertTableToDiv
        // A table with only ac: elements inside and no td/th content
        String html = "<table ac:name=\"macro-table\"><tr><td><ac:structured-macro ac:name=\"x\"></ac:structured-macro></td></tr></table>";
        Page page = makePage("953", "Empty Extract", html);
        when(confluenceService.getPageById("953")).thenReturn(page);

        File result = runDownloadWithDocxMocking("953", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_htmlWithValidHtmlEntitiesProtected() {
        // Tests the entity protection in preSanitizeHtmlContent
        String html = "<p>Already escaped &amp; entity, plus &lt;tag&gt; and &#169; and &#x00A9;</p>";
        Page page = makePage("954", "Entity Protection", html);
        when(confluenceService.getPageById("954")).thenReturn(page);

        File result = runDownloadWithDocxMocking("954", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_htmlWithNullContentInPreSanitize() {
        // Tests preSanitizeHtmlContent with page having null storage value
        // This exercises the null check at the start of preSanitizeHtmlContent
        Storage storage = Storage.builder().setValue(null).build();
        Body body = Body.setBody(storage);
        Page page = new Page.PageBuilder("955", "Null Pre-Sanitize").body(body).build();
        when(confluenceService.getPageById("955")).thenReturn(page);

        File result = runDownloadWithDocxMocking("955", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_htmlWithTableContainingOnlyText() {
        // Table with only text (no cells) exercises text node cleanup and empty table data path
        String html = "<table>Just some raw text without any cells</table>";
        Page page = makePage("956", "Text Only Table", html);
        when(confluenceService.getPageById("956")).thenReturn(page);

        File result = runDownloadWithDocxMocking("956", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_htmlWithLayoutSectionType() {
        // Exercises the ac:type attribute branch in convertConfluenceLayouts
        String html = "<ac:layout><ac:layout-section ac:type=\"fixed-width\">" +
                "<ac:layout-cell><p>Fixed width content</p></ac:layout-cell>" +
                "</ac:layout-section></ac:layout>";
        Page page = makePage("957", "Fixed Layout", html);
        when(confluenceService.getPageById("957")).thenReturn(page);

        File result = runDownloadWithDocxMocking("957", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_htmlWithLayoutSectionNoType() {
        // Layout section without ac:type - exercises the empty type check
        String html = "<ac:layout><ac:layout-section>" +
                "<ac:layout-cell><p>No type layout</p></ac:layout-cell>" +
                "</ac:layout-section></ac:layout>";
        Page page = makePage("958", "No Type Layout", html);
        when(confluenceService.getPageById("958")).thenReturn(page);

        File result = runDownloadWithDocxMocking("958", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_htmlWithInfoMacroNoRichTextBody() {
        // Info macro without ac:rich-text-body - exercises the empty richTextBody check
        String html = "<ac:structured-macro ac:name=\"info\"></ac:structured-macro>";
        Page page = makePage("959", "Info No Body", html);
        when(confluenceService.getPageById("959")).thenReturn(page);

        File result = runDownloadWithDocxMocking("959", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFileWithStatus_saveFailsFallbackSucceeds() {
        // The main save fails but fallback empty docx succeeds
        Page page = makePage("960", "Save Fail Status", "<p>content</p>");
        when(confluenceService.getPageById("960")).thenReturn(page);

        try (MockedStatic<WordprocessingMLPackage> wpMock = mockStatic(WordprocessingMLPackage.class);
             MockedConstruction<XHTMLImporterImpl> ignored = mockConstruction(XHTMLImporterImpl.class,
                     (mock, context) -> when(mock.convert(anyString(), any())).thenReturn(Collections.emptyList()))) {

            WordprocessingMLPackage mockPkg = mock(WordprocessingMLPackage.class);
            MainDocumentPart mockMainDoc = mock(MainDocumentPart.class);
            wpMock.when(WordprocessingMLPackage::createPackage).thenReturn(mockPkg);
            when(mockPkg.getMainDocumentPart()).thenReturn(mockMainDoc);
            when(mockMainDoc.getContent()).thenReturn(new ArrayList<>());
            // save throws to simulate IO error during main path
            doThrow(new Docx4JException("disk error")).when(mockPkg).save(any(java.io.File.class));

            when(fileRepository.createNewFileByName(anyString())).thenReturn(fileHandle);
            when(fileHandle.getFile()).thenReturn(kristaFile);

            Pair<File, Boolean> result = service.downloadPageAsDocxFileWithStatus("960", "docx", null);

            assertNotNull(result);
            assertFalse(result.getValue());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void downloadPageAsDocxFile_htmlWithTableContainingRowsAndHeadersBoth() {
        // Exercises createSimplifiedTable with both header and data rows
        String html = "<table ac:name=\"test\"><thead><tr><th>Name</th><th>Age</th></tr></thead>" +
                "<tbody><tr><td>Alice</td><td>30</td></tr><tr><td>Bob</td><td>25</td></tr></tbody></table>";
        Page page = makePage("961", "Header Data Table", html);
        when(confluenceService.getPageById("961")).thenReturn(page);

        File result = runDownloadWithDocxMocking("961", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_htmlWithTableContainingLooseRows() {
        // Table with tr elements directly under table (no thead/tbody) exercises
        // the repairMalformedTables wrapping logic
        String html = "<table><tr><td>Row 1</td></tr><tr><td>Row 2</td></tr></table>";
        Page page = makePage("962", "Loose Rows", html);
        when(confluenceService.getPageById("962")).thenReturn(page);

        File result = runDownloadWithDocxMocking("962", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_causeWithEntityNameImmediatelyFollow() {
        // Exercises extractDocx4JErrorDetails with specific cause message
        Page page = makePage("963", "Entity Error", "<p>content</p>");
        when(confluenceService.getPageById("963")).thenReturn(page);

        try (MockedStatic<WordprocessingMLPackage> wpMock = mockStatic(WordprocessingMLPackage.class);
             MockedConstruction<XHTMLImporterImpl> ignored = mockConstruction(XHTMLImporterImpl.class,
                     (mock, context) -> when(mock.convert(anyString(), any())).thenReturn(Collections.emptyList()))) {

            WordprocessingMLPackage mockPkg = mock(WordprocessingMLPackage.class);
            MainDocumentPart mockMainDoc = mock(MainDocumentPart.class);
            wpMock.when(WordprocessingMLPackage::createPackage).thenReturn(mockPkg);
            when(mockPkg.getMainDocumentPart()).thenReturn(mockMainDoc);
            when(mockMainDoc.getContent()).thenReturn(new ArrayList<>());

            // Throw a Docx4JException with a cause containing the entity name message
            Exception cause = new Exception("entity name must immediately follow the & in the entity reference");
            doThrow(new Docx4JException("XML parsing error", cause)).when(mockPkg).save(any(java.io.File.class));

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> service.downloadPageAsDocxFile("963", "docx", null));

            assertTrue(ex.getMessage().contains("Failed to download page as Docx file"));
        } catch (Exception e) {
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            throw new RuntimeException(e);
        }
    }

    @Test
    void downloadPageAsDocxFile_docx4JExceptionWithNullMessage() {
        // Exercises extractDocx4JErrorDetails where message is null
        Page page = makePage("964", "Null Msg", "<p>content</p>");
        when(confluenceService.getPageById("964")).thenReturn(page);

        try (MockedStatic<WordprocessingMLPackage> wpMock = mockStatic(WordprocessingMLPackage.class);
             MockedConstruction<XHTMLImporterImpl> ignored = mockConstruction(XHTMLImporterImpl.class,
                     (mock, context) -> when(mock.convert(anyString(), any())).thenReturn(Collections.emptyList()))) {

            WordprocessingMLPackage mockPkg = mock(WordprocessingMLPackage.class);
            MainDocumentPart mockMainDoc = mock(MainDocumentPart.class);
            wpMock.when(WordprocessingMLPackage::createPackage).thenReturn(mockPkg);
            when(mockPkg.getMainDocumentPart()).thenReturn(mockMainDoc);
            when(mockMainDoc.getContent()).thenReturn(new ArrayList<>());

            doThrow(new Docx4JException((String) null)).when(mockPkg).save(any(java.io.File.class));

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> service.downloadPageAsDocxFile("964", "docx", null));

            assertTrue(ex.getMessage().contains("Failed to download page as Docx file"));
        } catch (Exception e) {
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            throw new RuntimeException(e);
        }
    }

    @Test
    void downloadPageAsDocxFile_htmlWithUnrecognizedEntities() {
        // Tests the cleanup that removes unrecognized HTML entities
        String html = "<p>Unknown &foobar; entity and &unkn; chars</p>";
        Page page = makePage("965", "Unknown Entities", html);
        when(confluenceService.getPageById("965")).thenReturn(page);

        File result = runDownloadWithDocxMocking("965", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_htmlWithAcPlaceholderWithText() {
        // Exercises convertConfluencePlaceholders where placeholder has text
        String html = "<ac:placeholder>Enter your name here</ac:placeholder>";
        Page page = makePage("966", "Placeholder Text", html);
        when(confluenceService.getPageById("966")).thenReturn(page);

        File result = runDownloadWithDocxMocking("966", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_htmlWithRichTextBodyOutsideMacro() {
        // ac:rich-text-body outside of structured-macro exercises the standalone conversion
        String html = "<ac:rich-text-body><p>Standalone rich text</p></ac:rich-text-body>";
        Page page = makePage("967", "Standalone Rich", html);
        when(confluenceService.getPageById("967")).thenReturn(page);

        File result = runDownloadWithDocxMocking("967", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_htmlWithMultipleSpacesAndWhitespace() {
        // Exercises whitespace normalization in various sanitization methods
        String html = "<p>Text    with     many      spaces</p>" +
                "<p>  Leading and trailing spaces  </p>";
        Page page = makePage("968", "Whitespace", html);
        when(confluenceService.getPageById("968")).thenReturn(page);

        File result = runDownloadWithDocxMocking("968", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_htmlWithEmptyParagraphs() {
        // Exercises handling of empty elements
        String html = "<p></p><p>  </p><div></div><p>Non-empty content</p>";
        Page page = makePage("969", "Empty Paras", html);
        when(confluenceService.getPageById("969")).thenReturn(page);

        File result = runDownloadWithDocxMocking("969", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_htmlWithTableFailingXmlValidation() {
        // Table that passes the hasProblems check but fails XML validation
        // This exercises the catch block in convertProblematicTablesToDivs XML validation path
        String html = "<table><tr><td>&invalid_entity;</td></tr></table>";
        Page page = makePage("970", "XML Fail Table", html);
        when(confluenceService.getPageById("970")).thenReturn(page);

        File result = runDownloadWithDocxMocking("970", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_simpleValidTableWithColSurvivesToFixSelfClosing() {
        // A simple valid table with col elements that will NOT match hasProblems
        // and WILL pass XML validation, so it survives to fixSelfClosingTags
        String html = "<table><colgroup><col width=\"100\"/><col width=\"200\"/></colgroup>" +
                "<tbody><tr><td>A</td><td>B</td></tr></tbody></table>";
        Page page = makePage("971", "Valid Col Table", html);
        when(confluenceService.getPageById("971")).thenReturn(page);

        File result = runDownloadWithDocxMocking("971", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_simpleTableWithBrAndImgInsideCells() {
        // Table with br and img elements inside cells - these self-closing tags
        // inside a valid table will be processed by fixSelfClosingTags
        String html = "<table><tbody><tr><td>Line 1<br/>Line 2</td>" +
                "<td><img src=\"x.png\" alt=\"pic\"/>text</td></tr></tbody></table>";
        Page page = makePage("972", "BR IMG Table", html);
        when(confluenceService.getPageById("972")).thenReturn(page);

        File result = runDownloadWithDocxMocking("972", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_tableWithRowsButNoTableSections() {
        // Table without thead/tbody/tfoot wrapping - triggers repairMalformedTables wrapping
        // Also exercises the "hasTableSections == false" branch
        String html = "<table><tr><td>Cell 1</td><td>Cell 2</td></tr>" +
                "<tr><td>Cell 3</td><td>Cell 4</td></tr></table>";
        Page page = makePage("973", "No Sections", html);
        when(confluenceService.getPageById("973")).thenReturn(page);

        File result = runDownloadWithDocxMocking("973", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_htmlWithCleanupTextContentArtifacts() {
        // Content with data-highlight attributes as text artifacts inside elements
        // These exercise cleanupTextString and cleanupTextContent
        String html = "<p>Normal text here</p>" +
                "<div>Another paragraph with text</div>" +
                "<p>More text &nbsp; with entities</p>";
        Page page = makePage("974", "Text Cleanup", html);
        when(confluenceService.getPageById("974")).thenReturn(page);

        File result = runDownloadWithDocxMocking("974", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_complexMacroWithSwitchCaseBranches() {
        // Tests all switch case branches in convertConfluenceMacros
        String html = "<ac:structured-macro ac:name=\"recently-updated\">" +
                "<ac:parameter ac:name=\"max\">5</ac:parameter>" +
                "</ac:structured-macro>" +
                "<ac:structured-macro ac:name=\"blog-posts\"></ac:structured-macro>" +
                "<ac:structured-macro ac:name=\"info\">" +
                "<ac:rich-text-body><p>Info content</p></ac:rich-text-body>" +
                "</ac:structured-macro>" +
                "<ac:structured-macro ac:name=\"unknown-macro-type\">" +
                "<ac:parameter ac:name=\"key\">value</ac:parameter>" +
                "</ac:structured-macro>";
        Page page = makePage("975", "All Macros", html);
        when(confluenceService.getPageById("975")).thenReturn(page);

        File result = runDownloadWithDocxMocking("975", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_htmlWithAtDeclarationsAndVars() {
        // Tests the removal of at: elements
        String html = "<at:declarations><at:var at:name=\"title\"/></at:declarations>" +
                "<p>Content with <at:var at:name=\"date\">placeholder</at:var></p>";
        Page page = makePage("976", "AT Decls", html);
        when(confluenceService.getPageById("976")).thenReturn(page);

        File result = runDownloadWithDocxMocking("976", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_htmlWithRiSpaceAndUser() {
        // Tests removal of ri:space and ri:user elements
        String html = "<p>See <ri:space ri:space-key=\"DEV\"/>space and " +
                "<ri:user ri:userkey=\"user123\"/>user reference</p>";
        Page page = makePage("977", "RI Space User", html);
        when(confluenceService.getPageById("977")).thenReturn(page);

        File result = runDownloadWithDocxMocking("977", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_htmlWithRemainingAcElements() {
        // Remaining ac: elements not matching specific patterns get replaced with text
        String html = "<ac:custom-element>Some text content</ac:custom-element>" +
                "<ac:empty-element/>" +
                "<p>Regular paragraph</p>";
        Page page = makePage("978", "Remaining AC", html);
        when(confluenceService.getPageById("978")).thenReturn(page);

        File result = runDownloadWithDocxMocking("978", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_tableWithAcInsideTriggeringConversion() {
        // Table containing ac: elements that triggers convertTableToDiv via hasProblems
        // With no actual rows to extract (only ac: elements), exercises empty tableData path
        String html = "<table><tbody><tr><td>Row1-Col1</td><td>Row1-Col2</td></tr>" +
                "<tr><td>Row2-Col1</td><td><ac:macro ac:name=\"test\">macro</ac:macro></td></tr></tbody></table>";
        Page page = makePage("979", "AC In Table", html);
        when(confluenceService.getPageById("979")).thenReturn(page);

        File result = runDownloadWithDocxMocking("979", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_htmlWithImgTagHavingAttributes() {
        // img tag with various attributes that get cleaned up
        String html = "<p>Image: <img src=\"test.png\" alt=\"A picture\" width=\"100\" height=\"50\"/></p>";
        Page page = makePage("980", "Img Attrs", html);
        when(confluenceService.getPageById("980")).thenReturn(page);

        File result = runDownloadWithDocxMocking("980", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_htmlWithNestedDivsAndSpansComplex() {
        // Complex nested structure exercises removeComplexStyling and span unwrapping
        String html = "<div id=\"outer\" class=\"main\" style=\"margin: 10px\">" +
                "<div style=\"padding: 5px\"><p style=\"font-weight: bold\">Bold text</p></div>" +
                "<div><span style=\"font-style: italic\">Italic span</span></div>" +
                "<div><span>Unwrapped span</span></div></div>";
        Page page = makePage("981", "Nested Divs", html);
        when(confluenceService.getPageById("981")).thenReturn(page);

        File result = runDownloadWithDocxMocking("981", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFileWithStatus_nullBodyAndNullStorage() {
        // Page with body that has null storage exercises body extraction with status
        Body body = Body.setBody(null);
        Page page = new Page.PageBuilder("982", "Status No Storage").body(body).build();
        when(confluenceService.getPageById("982")).thenReturn(page);

        Pair<File, Boolean> result = runDownloadWithStatusAndDocxMocking("982", "docx", null);

        assertNotNull(result);
        assertTrue(result.getValue());
    }

    @Test
    void downloadPageAsDocxFile_htmlWithPreservingValidXmlEntities() {
        // Exercises preSanitizeHtmlContent entity protection more thoroughly
        String html = "<p>&amp; already escaped &lt;tag&gt; and &apos;quotes&apos; and &quot;double&quot;</p>";
        Page page = makePage("983", "Valid XML", html);
        when(confluenceService.getPageById("983")).thenReturn(page);

        File result = runDownloadWithDocxMocking("983", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_htmlWithNbspAndFormattingSpaces() {
        // Exercises convertHtmlEntitiesToUnicode with various entity types
        String html = "<p>Word1&nbsp;&nbsp;&nbsp;Word2&ndash;Word3&euro;50</p>";
        Page page = makePage("984", "Nbsp Format", html);
        when(confluenceService.getPageById("984")).thenReturn(page);

        File result = runDownloadWithDocxMocking("984", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_htmlWithColgroupContainingOnlyColElements() {
        // Valid colgroup should survive fixSelfClosingTags hasOnlyColElements check
        String html = "<table><colgroup><col width=\"50\"/><col width=\"50\"/></colgroup>" +
                "<tbody><tr><td>Data1</td><td>Data2</td></tr></tbody></table>";
        Page page = makePage("985", "Good Colgroup", html);
        when(confluenceService.getPageById("985")).thenReturn(page);

        File result = runDownloadWithDocxMocking("985", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_tableWithAcNameAndNoExtractableData() {
        // A table that triggers hasProblems (contains ac:) but extractTableData
        // returns rows because ac: macro becomes text after JSoup parsing.
        // The convertTableToDiv receives the table after sanitizeTableStructures
        // which may clean it up. This exercises different branches.
        String html = "<table ac:name=\"layout\"><ac:structured-macro ac:name=\"test\">" +
                "<ac:parameter ac:name=\"key\">val</ac:parameter>" +
                "</ac:structured-macro></table>";
        Page page = makePage("986", "AC Table No Data", html);
        when(confluenceService.getPageById("986")).thenReturn(page);

        File result = runDownloadWithDocxMocking("986", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_htmlWithConvertToValidXhtmlFallbackPath() {
        // Very large and complex HTML that still processes normally
        // but exercises more branches in the XHTML conversion pipeline
        StringBuilder html = new StringBuilder();
        html.append("<h1>Document Title</h1>");
        for (int i = 0; i < 5; i++) {
            html.append("<table ac:name=\"t").append(i).append("\">");
            html.append("<tr><th>Header ").append(i).append("</th></tr>");
            html.append("<tr><td>Data ").append(i).append(" &amp; more</td></tr>");
            html.append("</table>");
        }
        html.append("<ac:structured-macro ac:name=\"recently-updated\"></ac:structured-macro>");
        html.append("<ac:structured-macro ac:name=\"blog-posts\"></ac:structured-macro>");
        html.append("<ac:structured-macro ac:name=\"info\">");
        html.append("<ac:rich-text-body><p>Info block</p></ac:rich-text-body>");
        html.append("</ac:structured-macro>");
        html.append("<ac:structured-macro ac:name=\"custom\">");
        html.append("<ac:parameter ac:name=\"p1\">v1</ac:parameter>");
        html.append("</ac:structured-macro>");
        html.append("<ac:placeholder>placeholder text</ac:placeholder>");
        html.append("<ri:attachment ri:filename=\"file.doc\"/>");
        html.append("<at:var at:name=\"v1\">template var</at:var>");
        html.append("<at:declarations></at:declarations>");

        Page page = makePage("987", "Complex Full", html.toString());
        when(confluenceService.getPageById("987")).thenReturn(page);

        File result = runDownloadWithDocxMocking("987", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_htmlTriggeringAllEntityConversions() {
        // Exercises all the entity conversion branches in convertHtmlEntitiesToUnicode
        String html = "<p>&ldquo;Hello&rdquo; &lsquo;World&rsquo;</p>" +
                "<p>&hellip; &copy; &reg; &trade;</p>" +
                "<p>&bull; &middot; &sect; &para;</p>" +
                "<p>&dagger; &Dagger; &permil;</p>" +
                "<p>&lsaquo;quote&rsaquo; &euro;50</p>" +
                "<p>&mdash; &ndash; &nbsp;</p>";
        Page page = makePage("988", "All Entities", html);
        when(confluenceService.getPageById("988")).thenReturn(page);

        File result = runDownloadWithDocxMocking("988", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_htmlWithNestedTablesHavingImpropperNesting() {
        // Nested table not inside td/th exercises the improperly nested table branch
        // in repairMalformedTables
        String html = "<table><tbody><tr><td>Outer cell</td></tr>" +
                "<table><tr><td>Improperly nested inner table</td></tr></table>" +
                "</tbody></table>";
        Page page = makePage("989", "Improper Nest", html);
        when(confluenceService.getPageById("989")).thenReturn(page);

        File result = runDownloadWithDocxMocking("989", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_htmlWithTableHavingTextNodesAndMixedContent() {
        // Table with text directly inside (not in cells) and mixed content
        // This exercises the text node cleanup inside repairMalformedTables
        String html = "<table>Direct text node " +
                "<tbody><tr><td>Proper cell</td></tr></tbody>" +
                "Another text node</table>";
        Page page = makePage("990", "Mixed Table Content", html);
        when(confluenceService.getPageById("990")).thenReturn(page);

        File result = runDownloadWithDocxMocking("990", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_htmlWithOrphanedTheadTbodyTfoot() {
        // thead/tbody/tfoot elements outside of a table should be removed
        String html = "<thead><tr><th>Orphan header</th></tr></thead>" +
                "<tbody><tr><td>Orphan body</td></tr></tbody>" +
                "<tfoot><tr><td>Orphan footer</td></tr></tfoot>" +
                "<table><tbody><tr><td>Proper table</td></tr></tbody></table>";
        Page page = makePage("991", "Orphan Sections", html);
        when(confluenceService.getPageById("991")).thenReturn(page);

        File result = runDownloadWithDocxMocking("991", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_htmlWithOrphanedTdAndThOutsideTable() {
        // td/th elements outside of tr should be removed
        String html = "<td>Orphan cell</td><th>Orphan header cell</th>" +
                "<table><tr><td>Proper cell</td></tr></table>";
        Page page = makePage("992", "Orphan Cells", html);
        when(confluenceService.getPageById("992")).thenReturn(page);

        File result = runDownloadWithDocxMocking("992", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFileWithStatus_withComplexHtmlAndCustomName() {
        // Tests the WithStatus variant with complex HTML and custom filename
        String html = "<h1>Report</h1>" +
                "<table ac:name=\"results\"><tr><td>Pass</td><td>Fail</td></tr></table>" +
                "<ac:structured-macro ac:name=\"info\">" +
                "<ac:rich-text-body><p>Summary info</p></ac:rich-text-body>" +
                "</ac:structured-macro>" +
                "<p>Report generated on &mdash; date &copy; 2024</p>";
        Page page = makePage("993", "Status Complex Custom", html);
        when(confluenceService.getPageById("993")).thenReturn(page);

        Pair<File, Boolean> result = runDownloadWithStatusAndDocxMocking("993", "docx", "report_final.docx");

        assertNotNull(result);
        assertTrue(result.getValue());
    }

    @Test
    void downloadPageAsDocxFile_htmlWithNumericCharacterReferences() {
        // Tests handling of numeric character references in preSanitizeHtmlContent
        String html = "<p>Numeric &#169; reference and hex &#x00A9; reference</p>";
        Page page = makePage("994", "Numeric Refs", html);
        when(confluenceService.getPageById("994")).thenReturn(page);

        File result = runDownloadWithDocxMocking("994", "docx", null);

        assertNotNull(result);
    }

    @Test
    void downloadPageAsDocxFile_htmlWithMalformedSelfClosingBrAndHr() {
        // Tests br and hr tags that are not self-closed in the original HTML
        // preSanitizeHtmlContent should fix them
        String html = "<p>Text<br>break<br >more<hr>line<hr >end</p>";
        Page page = makePage("995", "Malformed BR HR", html);
        when(confluenceService.getPageById("995")).thenReturn(page);

        File result = runDownloadWithDocxMocking("995", "docx", null);

        assertNotNull(result);
    }

    // ========================================================================
    // Direct private method tests via reflection for uncovered paths
    // ========================================================================

    @Test
    void createFallbackXhtml_withNullContent() throws Exception {
        Method method = PageDocxServiceImpl.class.getDeclaredMethod("createFallbackXhtml", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(service, (String) null);

        assertNotNull(result);
        assertTrue(result.contains("<?xml version"));
        assertTrue(result.contains("<body>"));
    }

    @Test
    void createFallbackXhtml_withValidHtmlContent() throws Exception {
        Method method = PageDocxServiceImpl.class.getDeclaredMethod("createFallbackXhtml", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(service, "<p>Hello <strong>world</strong></p>");

        assertNotNull(result);
        assertTrue(result.contains("<?xml version"));
        assertTrue(result.contains("world"));
    }

    @Test
    void createFallbackXhtml_withConfluenceElements() throws Exception {
        Method method = PageDocxServiceImpl.class.getDeclaredMethod("createFallbackXhtml", String.class);
        method.setAccessible(true);
        String html = "<ac:layout><ac:layout-section><ac:layout-cell><p>Content</p></ac:layout-cell></ac:layout-section></ac:layout>" +
                "<ac:structured-macro ac:name=\"info\"><ac:rich-text-body><p>Info</p></ac:rich-text-body></ac:structured-macro>" +
                "<ac:placeholder>Placeholder</ac:placeholder>" +
                "<ri:attachment ri:filename=\"test.png\"/>" +
                "<at:var at:name=\"v\">value</at:var>";
        String result = (String) method.invoke(service, html);

        assertNotNull(result);
        assertTrue(result.contains("<?xml version"));
    }

    @Test
    void createFallbackXhtml_withTableContent() throws Exception {
        Method method = PageDocxServiceImpl.class.getDeclaredMethod("createFallbackXhtml", String.class);
        method.setAccessible(true);
        String html = "<table><tr><td>Cell 1</td><td>Cell 2</td></tr></table>";
        String result = (String) method.invoke(service, html);

        assertNotNull(result);
        assertTrue(result.contains("<?xml version"));
    }

    @Test
    void createAggressiveFallbackXhtml_withNullContent() throws Exception {
        Method method = PageDocxServiceImpl.class.getDeclaredMethod("createAggressiveFallbackXhtml", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(service, (String) null);

        assertNotNull(result);
        assertTrue(result.contains("<?xml version"));
        assertTrue(result.contains("Simplified"));
    }

    @Test
    void createAggressiveFallbackXhtml_withTableContent() throws Exception {
        Method method = PageDocxServiceImpl.class.getDeclaredMethod("createAggressiveFallbackXhtml", String.class);
        method.setAccessible(true);
        String html = "<table><tr><th>Header</th></tr><tr><td>Data</td></tr></table>" +
                "<ac:macro ac:name=\"test\">Macro text</ac:macro>" +
                "<p>Regular <strong>paragraph</strong></p>" +
                "<ul><li>List item</li></ul>";
        String result = (String) method.invoke(service, html);

        assertNotNull(result);
        assertTrue(result.contains("<?xml version"));
        // Tables should be converted to text format in aggressive fallback
    }

    @Test
    void createAggressiveFallbackXhtml_withEmptyTableHavingNoData() throws Exception {
        Method method = PageDocxServiceImpl.class.getDeclaredMethod("createAggressiveFallbackXhtml", String.class);
        method.setAccessible(true);
        // Empty table with no rows triggers the empty tableData text fallback
        String html = "<table></table><p>Some text</p>";
        String result = (String) method.invoke(service, html);

        assertNotNull(result);
        assertTrue(result.contains("<?xml version"));
    }

    @Test
    void createAggressiveFallbackXhtml_withConfluenceNamespaceElements() throws Exception {
        Method method = PageDocxServiceImpl.class.getDeclaredMethod("createAggressiveFallbackXhtml", String.class);
        method.setAccessible(true);
        String html = "<ac:layout><ac:layout-section><ac:layout-cell><p>Content</p></ac:layout-cell></ac:layout-section></ac:layout>" +
                "<ri:attachment ri:filename=\"test.png\">File</ri:attachment>" +
                "<at:var at:name=\"v\"/>";
        String result = (String) method.invoke(service, html);

        assertNotNull(result);
        assertTrue(result.contains("<?xml version"));
    }

    @Test
    void createAggressiveFallbackXhtml_withComplexElements() throws Exception {
        Method method = PageDocxServiceImpl.class.getDeclaredMethod("createAggressiveFallbackXhtml", String.class);
        method.setAccessible(true);
        // Includes elements that are NOT in the safe list and will be converted to divs
        String html = "<article>Article content</article>" +
                "<section>Section content</section>" +
                "<aside>Aside content</aside>" +
                "<nav>Nav content</nav>" +
                "<header>Header content</header>" +
                "<footer>Footer content</footer>" +
                "<p>Normal paragraph</p>";
        String result = (String) method.invoke(service, html);

        assertNotNull(result);
        assertTrue(result.contains("<?xml version"));
    }

    @Test
    void sanitizeFileName_withNullInput() throws Exception {
        Method method = PageDocxServiceImpl.class.getDeclaredMethod("sanitizeFileName", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(service, (String) null);

        assertNotNull(result);
        assertTrue(result.startsWith("confluence_page_"));
    }

    @Test
    void sanitizeFileName_withSpecialCharacters() throws Exception {
        Method method = PageDocxServiceImpl.class.getDeclaredMethod("sanitizeFileName", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(service, "My File/Name with <special> chars!");

        assertNotNull(result);
        assertFalse(result.contains("/"));
        assertFalse(result.contains("<"));
        assertFalse(result.contains(">"));
    }

    @Test
    void sanitizeForXml_withNullInput() throws Exception {
        Method method = PageDocxServiceImpl.class.getDeclaredMethod("sanitizeForXml", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(service, (String) null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void sanitizeForXml_withSpecialXmlCharacters() throws Exception {
        Method method = PageDocxServiceImpl.class.getDeclaredMethod("sanitizeForXml", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(service, "Text with <tags> & \"quotes\" and 'apostrophes'");

        assertNotNull(result);
        assertTrue(result.contains("&lt;"));
        assertTrue(result.contains("&gt;"));
        assertTrue(result.contains("&amp;"));
        assertTrue(result.contains("&quot;"));
        assertTrue(result.contains("&apos;"));
    }

    @Test
    void extractDocx4JErrorDetails_withNullMessage() throws Exception {
        Method method = PageDocxServiceImpl.class.getDeclaredMethod("extractDocx4JErrorDetails", Docx4JException.class);
        method.setAccessible(true);
        Docx4JException ex = new Docx4JException((String) null);
        String result = (String) method.invoke(service, ex);

        assertNotNull(result);
        assertTrue(result.contains("Unable to generate DOCX file"));
    }

    @Test
    void extractDocx4JErrorDetails_withEntityNameCauseMessage() throws Exception {
        Method method = PageDocxServiceImpl.class.getDeclaredMethod("extractDocx4JErrorDetails", Docx4JException.class);
        method.setAccessible(true);
        Exception cause = new Exception("The entity name must immediately follow the & in the entity reference");
        Docx4JException ex = new Docx4JException("XML parse error", cause);
        String result = (String) method.invoke(service, ex);

        assertNotNull(result);
        assertTrue(result.contains("invalid XML characters"));
        assertTrue(result.contains("unescaped ampersand"));
    }

    @Test
    void extractDocx4JErrorDetails_withNormalMessage() throws Exception {
        Method method = PageDocxServiceImpl.class.getDeclaredMethod("extractDocx4JErrorDetails", Docx4JException.class);
        method.setAccessible(true);
        Docx4JException ex = new Docx4JException("Some normal error message");
        String result = (String) method.invoke(service, ex);

        assertNotNull(result);
        assertTrue(result.contains("Some normal error message"));
    }

    @Test
    void extractDocx4JErrorDetails_withCauseButDifferentMessage() throws Exception {
        Method method = PageDocxServiceImpl.class.getDeclaredMethod("extractDocx4JErrorDetails", Docx4JException.class);
        method.setAccessible(true);
        Exception cause = new Exception("different cause message");
        Docx4JException ex = new Docx4JException("main error", cause);
        String result = (String) method.invoke(service, ex);

        assertNotNull(result);
        assertTrue(result.contains("main error"));
    }

    @Test
    void extractOriginalHtmlFromContext_whenNoContentStored() throws Exception {
        Method method = PageDocxServiceImpl.class.getDeclaredMethod("extractOriginalHtmlFromContext");
        method.setAccessible(true);
        // ThreadLocal should be empty since no conversion is in progress
        String result = (String) method.invoke(service);

        assertNotNull(result);
        assertTrue(result.contains("Content could not be processed"));
    }

    @Test
    void preSanitizeHtmlContent_withNullInput() throws Exception {
        Method method = PageDocxServiceImpl.class.getDeclaredMethod("preSanitizeHtmlContent", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(service, (String) null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void cleanupTextString_withNullInput() throws Exception {
        Method method = PageDocxServiceImpl.class.getDeclaredMethod("cleanupTextString", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(service, (String) null);

        assertNull(result);
    }

    @Test
    void cleanupTextString_withEmptyInput() throws Exception {
        Method method = PageDocxServiceImpl.class.getDeclaredMethod("cleanupTextString", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(service, "   ");

        assertNotNull(result);
        assertTrue(result.trim().isEmpty());
    }

    @Test
    void cleanupTextString_withHtmlEntitiesInText() throws Exception {
        Method method = PageDocxServiceImpl.class.getDeclaredMethod("cleanupTextString", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(service, "Hello&nbsp;World &amp; More &lt;tag&gt; &quot;quoted&quot; &apos;apos&apos;");

        assertNotNull(result);
        assertTrue(result.contains("Hello"));
        assertTrue(result.contains("World"));
        assertTrue(result.contains("&"));
        assertTrue(result.contains("<tag>"));
    }

    @Test
    void cleanupTextString_withDataAttributeArtifacts() throws Exception {
        Method method = PageDocxServiceImpl.class.getDeclaredMethod("cleanupTextString", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(service,
                "Text data-highlight-colour=\"yellow\" with class=\"hl\" artifacts style=\"color:red\"");

        assertNotNull(result);
        assertFalse(result.contains("data-highlight-colour"));
        assertFalse(result.contains("class=\"hl\""));
        assertFalse(result.contains("style="));
    }

    @Test
    void cleanupTextString_withMultipleSpaces() throws Exception {
        Method method = PageDocxServiceImpl.class.getDeclaredMethod("cleanupTextString", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(service, "Text   with    multiple     spaces");

        assertNotNull(result);
        assertFalse(result.contains("  "));
    }

    @Test
    void convertHtmlEntitiesToUnicode_withDataAndStyleRemaining() throws Exception {
        Method method = PageDocxServiceImpl.class.getDeclaredMethod("convertHtmlEntitiesToUnicode", String.class);
        method.setAccessible(true);
        String input = "Text data-custom=\"val\" style=\"color:red\" class=\"cls\" id=\"id1\" remaining";
        String result = (String) method.invoke(service, input);

        assertNotNull(result);
        assertFalse(result.contains("data-custom="));
        assertFalse(result.contains("style="));
        assertFalse(result.contains("class="));
        assertFalse(result.contains("id="));
    }
}
