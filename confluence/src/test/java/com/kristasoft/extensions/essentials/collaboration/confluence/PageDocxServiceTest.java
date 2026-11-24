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

import app.krista.model.base.File;
import app.krista.ksdk.files.FileHandle;
import app.krista.ksdk.files.FileRepository;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.stores.Pair;
import com.kristasoft.extensions.essentials.collaboration.confluence.dto.Body;
import com.kristasoft.extensions.essentials.collaboration.confluence.dto.Page;
import com.kristasoft.extensions.essentials.collaboration.confluence.dto.Storage;
import com.kristasoft.extensions.essentials.collaboration.confluence.service.ConfluenceService;
import com.kristasoft.extensions.essentials.collaboration.confluence.service.PageDocxService;
import com.kristasoft.extensions.essentials.collaboration.confluence.service.impl.PageDocxServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.InputStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Test class for PageDocxService functionality
 */
 class PageDocxServiceTest {

    @Mock
    private ConfluenceService confluenceService;

    @Mock
    private FileRepository fileRepository;

    @Mock
    private FileHandle mockFileHandle;

    @Mock
    private File mockFile;

    private PageDocxService pageDocxService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        pageDocxService = new PageDocxServiceImpl(confluenceService, fileRepository);
    }

    /**
     * Helper method to setup FileRepository mock for successful file creation
     */
    private void setupFileRepositoryMock() {
        when(fileRepository.createNewFileByName(anyString())).thenReturn(mockFileHandle);
        try {
            when(mockFileHandle.getFile()).thenReturn(mockFile);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    @Test
    public void testDownloadPageAsDocxFileWithStatus_Success() {
        // Arrange
        String pageId = "test-page-123";
        String exportFormat = "docx";
        String pageTitle = "Test Page";
        String pageBody = "<p>This is a test page content</p>";
        
        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);
        
        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);
        
        // Assert
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getValue()); // Boolean should not be null
        
        // Verify that the service was called
        verify(confluenceService).getPageById(pageId);
    }
    
    @Test
    public void testDownloadPageAsDocxFileWithStatus_ParseFailure() {
        // Arrange
        String pageId = "test-page-456";
        String exportFormat = "docx";
        
        // Simulate a failure by throwing an exception
        when(confluenceService.getPageById(pageId)).thenThrow(new RuntimeException("Page not found"));
        
        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);
        
        // Assert
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getValue()); // Should be false due to parsing failure
        
        // Verify that the service was called
        verify(confluenceService).getPageById(pageId);
    }
    
    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithCustomFileName() {
        // Arrange
        String pageId = "test-page-789";
        String exportFormat = "docx";
        String customFileName = "custom-test-file.docx";
        String pageTitle = "Test Page with Custom Name";
        String pageBody = "<p>This is a test page with custom filename</p>";
        
        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);
        
        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat, customFileName);
        
        // Assert
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getValue()); // Boolean should not be null
        
        // Verify that the service was called
        verify(confluenceService).getPageById(pageId);
    }
    
    @Test
     void testDownloadPageAsDocxFile_Success() {
        // Arrange
        String pageId = "test-page-001";
        String exportFormat = "docx";
        String pageTitle = "Test Page for Download";
        String pageBody = "<p>This is a test page content for download</p>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);
        setupFileRepositoryMock();

        // Act
        File result = pageDocxService.downloadPageAsDocxFile(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);

        // Verify that the service was called
        verify(confluenceService).getPageById(pageId);
        verify(fileRepository).createNewFileByName(anyString());
    }

    @Test
     void testDownloadPageAsDocxFile_WithCustomFileName() {
        // Arrange
        String pageId = "test-page-002";
        String exportFormat = "docx";
        String customFileName = "my-custom-file.docx";
        String pageTitle = "Test Page with Custom Name";
        String pageBody = "<p>This is a test page with custom filename</p>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);
        setupFileRepositoryMock();

        // Act
        File result = pageDocxService.downloadPageAsDocxFile(pageId, exportFormat, customFileName);

        // Assert
        Assertions.assertNotNull(result);

        // Verify that the service was called
        verify(confluenceService).getPageById(pageId);
        verify(fileRepository).createNewFileByName(customFileName);
    }

    @Test
     void testDownloadPageAsDocxFile_Failure() {
        // Arrange
        String pageId = "test-page-003";
        String exportFormat = "docx";

        // Simulate a failure by throwing an exception
        when(confluenceService.getPageById(pageId)).thenThrow(new RuntimeException("Page not found"));

        // Act & Assert
        Assertions.assertThrows(RuntimeException.class, () -> {
            pageDocxService.downloadPageAsDocxFile(pageId, exportFormat);
        });

        // Verify that the service was called
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_EmptyBody() {
        // Arrange
        String pageId = "test-page-empty";
        String exportFormat = "docx";
        String pageTitle = "Test Page with Empty Body";

        // Create page with null body
        Page mockPage = new Page.PageBuilder(pageId, pageTitle).build();
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);

        // Verify that the service was called
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_ComplexHtml() {
        // Arrange
        String pageId = "test-page-complex";
        String exportFormat = "docx";
        String pageTitle = "Test Page with Complex HTML";
        String pageBody = "<h1>Heading</h1><p>Paragraph with <strong>bold</strong> and <em>italic</em> text.</p>" +
                "<ul><li>Item 1</li><li>Item 2</li></ul>" +
                "<table><tr><th>Header</th></tr><tr><td>Cell</td></tr></table>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getValue());

        // Verify that the service was called
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_SpecialCharacters() {
        // Arrange
        String pageId = "test-page-special";
        String exportFormat = "docx";
        String pageTitle = "Test Page with Special Characters <>&\"'";
        String pageBody = "<p>Content with special chars: &amp; &lt; &gt; &quot;</p>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);

        // Verify that the service was called
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFile_NullPage() {
        // Arrange
        String pageId = "test-page-null";
        String exportFormat = "docx";

        // Return null page
        when(confluenceService.getPageById(pageId)).thenReturn(null);

        // Act & Assert
        Assertions.assertThrows(RuntimeException.class, () -> {
            pageDocxService.downloadPageAsDocxFile(pageId, exportFormat);
        });

        // Verify that the service was called
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_NullPage() {
        // Arrange
        String pageId = "test-page-null";
        String exportFormat = "docx";

        // Return null page
        when(confluenceService.getPageById(pageId)).thenReturn(null);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getValue()); // Should be false due to null page

        // Verify that the service was called
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithCustomFileName_Failure() {
        // Arrange
        String pageId = "test-page-fail";
        String exportFormat = "docx";
        String customFileName = "custom-fail.docx";

        // Simulate a failure
        when(confluenceService.getPageById(pageId)).thenThrow(new RuntimeException("API Error"));

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat, customFileName);

        // Assert
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getValue()); // Should be false due to failure

        // Verify that the service was called
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFile_WithNestedHtml() {
        // Arrange
        String pageId = "test-page-nested";
        String exportFormat = "docx";
        String pageTitle = "Test Page with Nested HTML";
        String pageBody = "<div><p>Outer <span>Inner <strong>Bold</strong></span></p></div>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);
        setupFileRepositoryMock();

        // Act
        File result = pageDocxService.downloadPageAsDocxFile(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);

        // Verify that the service was called
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithLinks() {
        // Arrange
        String pageId = "test-page-links";
        String exportFormat = "docx";
        String pageTitle = "Test Page with Links";
        String pageBody = "<p>Visit <a href=\"https://example.com\">Example</a> for more info.</p>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getValue());

        // Verify that the service was called
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithImages() {
        // Arrange
        String pageId = "test-page-images";
        String exportFormat = "docx";
        String pageTitle = "Test Page with Images";
        String pageBody = "<p>Image: <img src=\"https://example.com/image.png\" alt=\"Test Image\"/></p>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);

        // Verify that the service was called
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithCodeBlock() {
        // Arrange
        String pageId = "test-page-code";
        String exportFormat = "docx";
        String pageTitle = "Test Page with Code";
        String pageBody = "<pre><code>public class Test { }</code></pre>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);

        // Verify that the service was called
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithOrderedList() {
        // Arrange
        String pageId = "test-page-ol";
        String exportFormat = "docx";
        String pageTitle = "Test Page with Ordered List";
        String pageBody = "<ol><li>First</li><li>Second</li><li>Third</li></ol>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);

        // Verify that the service was called
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithBlockquote() {
        // Arrange
        String pageId = "test-page-quote";
        String exportFormat = "docx";
        String pageTitle = "Test Page with Blockquote";
        String pageBody = "<blockquote>This is a quoted text.</blockquote>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);

        // Verify that the service was called
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithMalformedHtml() {
        // Arrange
        String pageId = "test-page-malformed";
        String exportFormat = "docx";
        String pageTitle = "Test Page with Malformed HTML";
        String pageBody = "<p>Unclosed paragraph<div>Nested incorrectly</p></div>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);

        // Verify that the service was called
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithAmpersand() {
        // Arrange
        String pageId = "test-page-ampersand";
        String exportFormat = "docx";
        String pageTitle = "Test Page with Ampersand";
        String pageBody = "<p>This & that, also this &amp; that</p>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);

        // Verify that the service was called
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithConfluenceMacros() {
        // Arrange
        String pageId = "test-page-macros";
        String exportFormat = "docx";
        String pageTitle = "Test Page with Confluence Macros";
        String pageBody = "<ac:structured-macro ac:name=\"code\"><ac:plain-text-body>System.out.println(\"Hello\");</ac:plain-text-body></ac:structured-macro>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);

        // Verify that the service was called
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithEmoticons() {
        // Arrange
        String pageId = "test-page-emoticons";
        String exportFormat = "docx";
        String pageTitle = "Test Page with Emoticons";
        String pageBody = "<p>Hello <ac:emoticon ac:name=\"smile\"/> World</p>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);

        // Verify that the service was called
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithMultipleHeadings() {
        // Arrange
        String pageId = "test-page-headings";
        String exportFormat = "docx";
        String pageTitle = "Test Page with Multiple Headings";
        String pageBody = "<h1>Heading 1</h1><h2>Heading 2</h2><h3>Heading 3</h3><h4>Heading 4</h4><h5>Heading 5</h5><h6>Heading 6</h6>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);

        // Verify that the service was called
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithNestedLists() {
        // Arrange
        String pageId = "test-page-nested-lists";
        String exportFormat = "docx";
        String pageTitle = "Test Page with Nested Lists";
        String pageBody = "<ul><li>Item 1<ul><li>Nested 1</li><li>Nested 2</li></ul></li><li>Item 2</li></ul>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);

        // Verify that the service was called
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithComplexTable() {
        // Arrange
        String pageId = "test-page-complex-table";
        String exportFormat = "docx";
        String pageTitle = "Test Page with Complex Table";
        String pageBody = "<table><thead><tr><th>Header 1</th><th>Header 2</th></tr></thead><tbody><tr><td>Cell 1</td><td>Cell 2</td></tr><tr><td colspan=\"2\">Merged Cell</td></tr></tbody></table>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);

        // Verify that the service was called
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFile_WithLongContent() {
        // Arrange
        String pageId = "test-page-long";
        String exportFormat = "docx";
        String pageTitle = "Test Page with Long Content";
        StringBuilder longContent = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            longContent.append("<p>Paragraph ").append(i).append(": Lorem ipsum dolor sit amet.</p>");
        }

        Page mockPage = createMockPage(pageId, pageTitle, longContent.toString());
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);
        setupFileRepositoryMock();

        // Act
        File result = pageDocxService.downloadPageAsDocxFile(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);

        // Verify that the service was called
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithNullBody() {
        // Arrange
        String pageId = "test-page-null-body";
        String exportFormat = "docx";
        String pageTitle = "Test Page with Null Body";

        // Create page with null body
        Page mockPage = new Page.PageBuilder(pageId, pageTitle).build();
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);

        // Verify that the service was called
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithSpecialCharactersInTitle() {
        // Arrange
        String pageId = "test-page-special-title";
        String exportFormat = "docx";
        String pageTitle = "Test <Page> with & Special \"Characters\"";
        String pageBody = "<p>Test content</p>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);

        // Verify that the service was called
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithVeryLongTitle() {
        // Arrange
        String pageId = "test-page-long-title";
        String exportFormat = "docx";
        StringBuilder longTitle = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            longTitle.append("A");
        }
        String pageBody = "<p>Test content</p>";

        Page mockPage = createMockPage(pageId, longTitle.toString(), pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);

        // Verify that the service was called
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithDefinitionList() {
        // Arrange
        String pageId = "test-page-dl";
        String exportFormat = "docx";
        String pageTitle = "Test Page with Definition List";
        String pageBody = "<dl><dt>Term 1</dt><dd>Definition 1</dd><dt>Term 2</dt><dd>Definition 2</dd></dl>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);

        // Verify that the service was called
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithPreformattedText() {
        // Arrange
        String pageId = "test-page-pre";
        String exportFormat = "docx";
        String pageTitle = "Test Page with Preformatted Text";
        String pageBody = "<pre>function hello() {\n    console.log('Hello World');\n}</pre>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);

        // Verify that the service was called
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithHorizontalRule() {
        // Arrange
        String pageId = "test-page-hr";
        String exportFormat = "docx";
        String pageTitle = "Test Page with Horizontal Rule";
        String pageBody = "<p>Section 1</p><hr/><p>Section 2</p>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);

        // Verify that the service was called
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithStrongAndEmphasis() {
        // Arrange
        String pageId = "test-page-formatting";
        String exportFormat = "docx";
        String pageTitle = "Test Page with Formatting";
        String pageBody = "<p><strong>Bold text</strong> and <em>italic text</em> and <u>underlined text</u></p>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);

        // Verify that the service was called
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithNestedTables() {
        // Arrange
        String pageId = "test-page-nested-tables";
        String exportFormat = "docx";
        String pageTitle = "Test Page with Nested Tables";
        String pageBody = "<table><tr><td><table><tr><td>Nested</td></tr></table></td></tr></table>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);

        // Verify that the service was called
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithColgroup() {
        // Arrange
        String pageId = "test-page-colgroup";
        String exportFormat = "docx";
        String pageTitle = "Test Page with Colgroup";
        String pageBody = "<table><colgroup><col/><col/></colgroup><tr><td>Cell 1</td><td>Cell 2</td></tr></table>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);

        // Verify that the service was called
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithTheadTbodyTfoot() {
        // Arrange
        String pageId = "test-page-table-sections";
        String exportFormat = "docx";
        String pageTitle = "Test Page with Table Sections";
        String pageBody = "<table><thead><tr><th>Header</th></tr></thead><tbody><tr><td>Body</td></tr></tbody><tfoot><tr><td>Footer</td></tr></tfoot></table>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);

        // Verify that the service was called
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithConfluencePanel() {
        // Arrange
        String pageId = "test-page-panel";
        String exportFormat = "docx";
        String pageTitle = "Test Page with Confluence Panel";
        String pageBody = "<ac:structured-macro ac:name=\"panel\"><ac:rich-text-body><p>Panel content</p></ac:rich-text-body></ac:structured-macro>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);

        // Verify that the service was called
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithConfluenceStatus() {
        // Arrange
        String pageId = "test-page-status";
        String exportFormat = "docx";
        String pageTitle = "Test Page with Confluence Status";
        String pageBody = "<ac:structured-macro ac:name=\"status\"><ac:parameter ac:name=\"colour\">Green</ac:parameter><ac:parameter ac:name=\"title\">Done</ac:parameter></ac:structured-macro>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);

        // Verify that the service was called
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithConfluenceExpand() {
        // Arrange
        String pageId = "test-page-expand";
        String exportFormat = "docx";
        String pageTitle = "Test Page with Confluence Expand";
        String pageBody = "<ac:structured-macro ac:name=\"expand\"><ac:parameter ac:name=\"title\">Click to expand</ac:parameter><ac:rich-text-body><p>Hidden content</p></ac:rich-text-body></ac:structured-macro>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);

        // Verify that the service was called
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithConfluenceImage() {
        // Arrange
        String pageId = "test-page-image";
        String exportFormat = "docx";
        String pageTitle = "Test Page with Confluence Image";
        String pageBody = "<ac:image><ri:attachment ri:filename=\"image.png\"/></ac:image>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);

        // Verify that the service was called
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithConfluenceLink() {
        // Arrange
        String pageId = "test-page-link";
        String exportFormat = "docx";
        String pageTitle = "Test Page with Confluence Link";
        String pageBody = "<ac:link><ri:page ri:content-title=\"Other Page\"/><ac:plain-text-link-body><![CDATA[Link Text]]></ac:plain-text-link-body></ac:link>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);

        // Verify that the service was called
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithHtmlEntities() {
        // Arrange
        String pageId = "test-page-entities";
        String exportFormat = "docx";
        String pageTitle = "Test Page with HTML Entities";
        String pageBody = "<p>&nbsp;&copy;&reg;&trade;&mdash;&ndash;&hellip;</p>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);

        // Verify that the service was called
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithSubscriptSuperscript() {
        // Arrange
        String pageId = "test-page-sub-sup";
        String exportFormat = "docx";
        String pageTitle = "Test Page with Subscript and Superscript";
        String pageBody = "<p>H<sub>2</sub>O and E=mc<sup>2</sup></p>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);

        // Verify that the service was called
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithStrikethrough() {
        // Arrange
        String pageId = "test-page-strike";
        String exportFormat = "docx";
        String pageTitle = "Test Page with Strikethrough";
        String pageBody = "<p><s>Strikethrough text</s> and <del>deleted text</del></p>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);

        // Verify that the service was called
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithSpanStyles() {
        // Arrange
        String pageId = "test-page-span";
        String exportFormat = "docx";
        String pageTitle = "Test Page with Span Styles";
        String pageBody = "<p><span style=\"color: red;\">Red text</span> and <span style=\"font-weight: bold;\">Bold text</span></p>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);

        // Verify that the service was called
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithDivElements() {
        // Arrange
        String pageId = "test-page-div";
        String exportFormat = "docx";
        String pageTitle = "Test Page with Div Elements";
        String pageBody = "<div class=\"panel\"><div class=\"panelContent\"><p>Panel content</p></div></div>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);

        // Verify that the service was called
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithBreakTags() {
        // Arrange
        String pageId = "test-page-br";
        String exportFormat = "docx";
        String pageTitle = "Test Page with Break Tags";
        String pageBody = "<p>Line 1<br/>Line 2<br/>Line 3</p>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);

        // Verify that the service was called
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithMixedContent() {
        // Arrange
        String pageId = "test-page-mixed";
        String exportFormat = "docx";
        String pageTitle = "Test Page with Mixed Content";
        String pageBody = "<h1>Title</h1><p>Paragraph with <strong>bold</strong> and <em>italic</em>.</p>" +
                "<ul><li>Item 1</li><li>Item 2</li></ul>" +
                "<table><tr><th>Header</th></tr><tr><td>Cell</td></tr></table>" +
                "<blockquote>Quote</blockquote>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);

        // Verify that the service was called
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithUnicodeCharacters() {
        // Arrange
        String pageId = "test-page-unicode";
        String exportFormat = "docx";
        String pageTitle = "Test Page with Unicode Characters";
        String pageBody = "<p>Unicode: 日本語 中文 한국어 العربية עברית</p>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);

        // Verify that the service was called
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithEmptyParagraphs() {
        // Arrange
        String pageId = "test-page-empty-p";
        String exportFormat = "docx";
        String pageTitle = "Test Page with Empty Paragraphs";
        String pageBody = "<p></p><p>Content</p><p></p><p>More content</p><p></p>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);

        // Verify that the service was called
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithComplexNestedStructure() {
        // Arrange
        String pageId = "test-page-complex-nested";
        String exportFormat = "docx";
        String pageTitle = "Complex Nested Structure";
        String pageBody = "<div><div><div><p>Deeply nested content</p></div></div></div>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithMixedListTypes() {
        // Arrange
        String pageId = "test-page-mixed-lists";
        String exportFormat = "docx";
        String pageTitle = "Mixed List Types";
        String pageBody = "<ul><li>Unordered item 1</li><li>Unordered item 2</li></ul>" +
                         "<ol><li>Ordered item 1</li><li>Ordered item 2</li></ol>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithUnderline() {
        // Arrange
        String pageId = "test-page-underline";
        String exportFormat = "docx";
        String pageTitle = "Underline";
        String pageBody = "<p><u>Underlined text</u></p>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithAbbreviation() {
        // Arrange
        String pageId = "test-page-abbr";
        String exportFormat = "docx";
        String pageTitle = "Abbreviation";
        String pageBody = "<p>The <abbr title=\"World Wide Web\">WWW</abbr> is great.</p>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithAddress() {
        // Arrange
        String pageId = "test-page-address";
        String exportFormat = "docx";
        String pageTitle = "Address";
        String pageBody = "<address>123 Main St<br/>City, State 12345</address>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithFigure() {
        // Arrange
        String pageId = "test-page-figure";
        String exportFormat = "docx";
        String pageTitle = "Figure";
        String pageBody = "<figure><img src=\"image.png\" alt=\"Image\"/><figcaption>Caption</figcaption></figure>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithMark() {
        // Arrange
        String pageId = "test-page-mark";
        String exportFormat = "docx";
        String pageTitle = "Mark";
        String pageBody = "<p>This is <mark>highlighted</mark> text.</p>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithTime() {
        // Arrange
        String pageId = "test-page-time";
        String exportFormat = "docx";
        String pageTitle = "Time";
        String pageBody = "<p>Meeting at <time datetime=\"2024-01-15T14:00\">2:00 PM</time></p>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithDetails() {
        // Arrange
        String pageId = "test-page-details";
        String exportFormat = "docx";
        String pageTitle = "Details";
        String pageBody = "<details><summary>Click to expand</summary><p>Hidden content</p></details>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithKbd() {
        // Arrange
        String pageId = "test-page-kbd";
        String exportFormat = "docx";
        String pageTitle = "Keyboard";
        String pageBody = "<p>Press <kbd>Ctrl</kbd>+<kbd>C</kbd> to copy.</p>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithSamp() {
        // Arrange
        String pageId = "test-page-samp";
        String exportFormat = "docx";
        String pageTitle = "Sample Output";
        String pageBody = "<p>Output: <samp>Hello World</samp></p>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithVar() {
        // Arrange
        String pageId = "test-page-var";
        String exportFormat = "docx";
        String pageTitle = "Variable";
        String pageBody = "<p>The variable <var>x</var> equals 5.</p>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithScript() {
        // Arrange
        String pageId = "test-page-script";
        String exportFormat = "docx";
        String pageTitle = "Script";
        String pageBody = "<p>Text</p><script>alert('test');</script><p>More text</p>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithStyle() {
        // Arrange
        String pageId = "test-page-style";
        String exportFormat = "docx";
        String pageTitle = "Style";
        String pageBody = "<style>.test { color: red; }</style><p class=\"test\">Styled text</p>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithIframe() {
        // Arrange
        String pageId = "test-page-iframe";
        String exportFormat = "docx";
        String pageTitle = "Iframe";
        String pageBody = "<p>Text</p><iframe src=\"https://example.com\"></iframe><p>More text</p>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithObject() {
        // Arrange
        String pageId = "test-page-object";
        String exportFormat = "docx";
        String pageTitle = "Object";
        String pageBody = "<p>Text</p><object data=\"movie.swf\" type=\"application/x-shockwave-flash\"></object>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithEmbed() {
        // Arrange
        String pageId = "test-page-embed";
        String exportFormat = "docx";
        String pageTitle = "Embed";
        String pageBody = "<p>Text</p><embed src=\"video.mp4\" type=\"video/mp4\">";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithCanvas() {
        // Arrange
        String pageId = "test-page-canvas";
        String exportFormat = "docx";
        String pageTitle = "Canvas";
        String pageBody = "<p>Text</p><canvas id=\"myCanvas\"></canvas>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithSvg() {
        // Arrange
        String pageId = "test-page-svg";
        String exportFormat = "docx";
        String pageTitle = "SVG";
        String pageBody = "<p>Text</p><svg width=\"100\" height=\"100\"><circle cx=\"50\" cy=\"50\" r=\"40\"/></svg>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithMath() {
        // Arrange
        String pageId = "test-page-math";
        String exportFormat = "docx";
        String pageTitle = "Math";
        String pageBody = "<p>Text</p><math><mi>x</mi><mo>=</mo><mn>5</mn></math>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithAudio() {
        // Arrange
        String pageId = "test-page-audio";
        String exportFormat = "docx";
        String pageTitle = "Audio";
        String pageBody = "<p>Text</p><audio controls><source src=\"audio.mp3\" type=\"audio/mpeg\"></audio>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithVideo() {
        // Arrange
        String pageId = "test-page-video";
        String exportFormat = "docx";
        String pageTitle = "Video";
        String pageBody = "<p>Text</p><video controls><source src=\"video.mp4\" type=\"video/mp4\"></video>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithConfluenceLayout() {
        // Arrange
        String pageId = "test-page-layout";
        String exportFormat = "docx";
        String pageTitle = "Layout";
        String pageBody = "<ac:layout><ac:layout-section ac:type=\"two_equal\"><ac:layout-cell><p>Left</p></ac:layout-cell><ac:layout-cell><p>Right</p></ac:layout-cell></ac:layout-section></ac:layout>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithConfluenceMacro() {
        // Arrange
        String pageId = "test-page-macro";
        String exportFormat = "docx";
        String pageTitle = "Macro";
        String pageBody = "<ac:structured-macro ac:name=\"code\"><ac:parameter ac:name=\"language\">java</ac:parameter><ac:plain-text-body><![CDATA[public class Test {}]]></ac:plain-text-body></ac:structured-macro>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithConfluencePlaceholder() {
        // Arrange
        String pageId = "test-page-placeholder";
        String exportFormat = "docx";
        String pageTitle = "Placeholder";
        String pageBody = "<p>Text with <ac:placeholder>placeholder text</ac:placeholder> here</p>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithRiAttachment() {
        // Arrange
        String pageId = "test-page-attachment";
        String exportFormat = "docx";
        String pageTitle = "Attachment";
        String pageBody = "<p>Text with <ri:attachment ri:filename=\"file.pdf\"/> here</p>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithAtVar() {
        // Arrange
        String pageId = "test-page-atvar";
        String exportFormat = "docx";
        String pageTitle = "AtVar";
        String pageBody = "<at:declarations><at:string at:name=\"myVar\" at:default=\"default\"/></at:declarations><p>Value: <at:var at:name=\"myVar\"/></p>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithMalformedTable() {
        // Arrange
        String pageId = "test-page-malformed-table";
        String exportFormat = "docx";
        String pageTitle = "Malformed Table";
        String pageBody = "<table><tr><td>Cell 1</td><td>Cell 2</tr><tr><td>Cell 3</td></tr></table>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithNestedTable() {
        // Arrange
        String pageId = "test-page-nested-table";
        String exportFormat = "docx";
        String pageTitle = "Nested Table";
        String pageBody = "<table><tr><td><table><tr><td>Inner</td></tr></table></td></tr></table>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithColspan() {
        // Arrange
        String pageId = "test-page-colspan";
        String exportFormat = "docx";
        String pageTitle = "Colspan";
        String pageBody = "<table><tr><td colspan=\"2\">Merged</td></tr><tr><td>A</td><td>B</td></tr></table>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithRowspan() {
        // Arrange
        String pageId = "test-page-rowspan";
        String exportFormat = "docx";
        String pageTitle = "Rowspan";
        String pageBody = "<table><tr><td rowspan=\"2\">Merged</td><td>A</td></tr><tr><td>B</td></tr></table>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithThead() {
        // Arrange
        String pageId = "test-page-thead";
        String exportFormat = "docx";
        String pageTitle = "Thead";
        String pageBody = "<table><thead><tr><th>Header</th></tr></thead><tbody><tr><td>Data</td></tr></tbody></table>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithTfoot() {
        // Arrange
        String pageId = "test-page-tfoot";
        String exportFormat = "docx";
        String pageTitle = "Tfoot";
        String pageBody = "<table><tbody><tr><td>Data</td></tr></tbody><tfoot><tr><td>Footer</td></tr></tfoot></table>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithCaption() {
        // Arrange
        String pageId = "test-page-caption";
        String exportFormat = "docx";
        String pageTitle = "Caption";
        String pageBody = "<table><caption>Table Caption</caption><tr><td>Data</td></tr></table>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithInlineStyles() {
        // Arrange
        String pageId = "test-page-inline-styles";
        String exportFormat = "docx";
        String pageTitle = "Inline Styles";
        String pageBody = "<p style=\"color: red; font-size: 14px; background-color: yellow;\">Styled text</p>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithDataAttributes() {
        // Arrange
        String pageId = "test-page-data-attrs";
        String exportFormat = "docx";
        String pageTitle = "Data Attributes";
        String pageBody = "<div data-custom=\"value\" data-id=\"123\"><p>Content</p></div>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithEmoji() {
        // Arrange
        String pageId = "test-page-emoji";
        String exportFormat = "docx";
        String pageTitle = "Emoji";
        String pageBody = "<p>Emoji: \uD83D\uDE00 \uD83D\uDC4D \u2764\uFE0F</p>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithInvalidXmlCharacters() {
        // Arrange - Test content with invalid XML characters that should trigger fallback
        String pageId = "test-page-invalid-xml";
        String exportFormat = "docx";
        String pageTitle = "Invalid XML";
        // Content with unescaped ampersand that might cause XML parsing issues
        String pageBody = "<p>Test & content with & multiple & ampersands</p>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithComplexTableStructure() {
        // Arrange - Test complex table with colspan and rowspan
        String pageId = "test-page-complex-table";
        String exportFormat = "docx";
        String pageTitle = "Complex Table";
        String pageBody = "<table>" +
                "<thead><tr><th colspan=\"2\">Header</th></tr></thead>" +
                "<tbody>" +
                "<tr><td rowspan=\"2\">Cell 1</td><td>Cell 2</td></tr>" +
                "<tr><td>Cell 3</td></tr>" +
                "</tbody>" +
                "<tfoot><tr><td colspan=\"2\">Footer</td></tr></tfoot>" +
                "</table>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithEmptyTableCells() {
        // Arrange - Test table with empty cells
        String pageId = "test-page-empty-cells";
        String exportFormat = "docx";
        String pageTitle = "Empty Cells";
        String pageBody = "<table><tr><td></td><td></td></tr><tr><td></td><td></td></tr></table>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithSelfClosingTags() {
        // Arrange - Test content with self-closing tags
        String pageId = "test-page-self-closing";
        String exportFormat = "docx";
        String pageTitle = "Self Closing Tags";
        String pageBody = "<p>Line 1<br/>Line 2<br/>Line 3</p><hr/><img src=\"test.png\"/>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithMixedNamespaceElements() {
        // Arrange - Test content with various Confluence namespace elements
        String pageId = "test-page-namespaces";
        String exportFormat = "docx";
        String pageTitle = "Namespace Elements";
        String pageBody = "<ac:layout><ac:layout-section><ac:layout-cell>" +
                "<p>Content in layout</p>" +
                "</ac:layout-cell></ac:layout-section></ac:layout>" +
                "<ri:attachment ri:filename=\"test.pdf\"/>" +
                "<at:var at:name=\"variable\">value</at:var>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithComplexStyling() {
        // Arrange - Test content with complex inline styles
        String pageId = "test-page-complex-styles";
        String exportFormat = "docx";
        String pageTitle = "Complex Styles";
        String pageBody = "<div style=\"background-color: #fff; color: #000; font-size: 14px;\">" +
                "<p style=\"text-align: center; margin: 10px;\">Styled paragraph</p>" +
                "<span style=\"font-weight: bold; text-decoration: underline;\">Styled span</span>" +
                "</div>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithTableWithoutTbody() {
        // Arrange - Test table without explicit tbody
        String pageId = "test-page-no-tbody";
        String exportFormat = "docx";
        String pageTitle = "No Tbody";
        String pageBody = "<table><tr><td>Cell 1</td><td>Cell 2</td></tr></table>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithDeeplyNestedElements() {
        // Arrange - Test deeply nested elements
        String pageId = "test-page-deeply-nested";
        String exportFormat = "docx";
        String pageTitle = "Deeply Nested";
        String pageBody = "<div><div><div><div><div><p>Deeply nested content</p></div></div></div></div></div>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithMixedContentTypes() {
        // Arrange - Test mixed content types
        String pageId = "test-page-mixed-types";
        String exportFormat = "docx";
        String pageTitle = "Mixed Types";
        String pageBody = "<p>Text</p>" +
                "<table><tr><td>Table</td></tr></table>" +
                "<ul><li>List</li></ul>" +
                "<blockquote>Quote</blockquote>" +
                "<pre>Code</pre>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithWhitespaceOnlyContent() {
        // Arrange - Test content with only whitespace
        String pageId = "test-page-whitespace";
        String exportFormat = "docx";
        String pageTitle = "Whitespace";
        String pageBody = "<p>   </p><div>   </div>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithSpecialHtmlEntities() {
        // Arrange - Test content with various HTML entities
        String pageId = "test-page-entities";
        String exportFormat = "docx";
        String pageTitle = "HTML Entities";
        String pageBody = "<p>&nbsp;&copy;&reg;&trade;&euro;&pound;&yen;&cent;</p>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithTableMissingCells() {
        // Arrange - Test table with missing cells in rows
        String pageId = "test-page-missing-cells";
        String exportFormat = "docx";
        String pageTitle = "Missing Cells";
        String pageBody = "<table><tr><td>Cell 1</td><td>Cell 2</td><td>Cell 3</td></tr>" +
                "<tr><td>Cell 4</td></tr></table>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithConfluenceCodeMacro() {
        // Arrange - Test Confluence code macro
        String pageId = "test-page-code-macro";
        String exportFormat = "docx";
        String pageTitle = "Code Macro";
        String pageBody = "<ac:structured-macro ac:name=\"code\">" +
                "<ac:parameter ac:name=\"language\">java</ac:parameter>" +
                "<ac:plain-text-body><![CDATA[public class Test {}]]></ac:plain-text-body>" +
                "</ac:structured-macro>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithConfluenceInfoMacro() {
        // Arrange - Test Confluence info macro
        String pageId = "test-page-info-macro";
        String exportFormat = "docx";
        String pageTitle = "Info Macro";
        String pageBody = "<ac:structured-macro ac:name=\"info\">" +
                "<ac:rich-text-body><p>This is an info message</p></ac:rich-text-body>" +
                "</ac:structured-macro>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithConfluenceWarningMacro() {
        // Arrange - Test Confluence warning macro
        String pageId = "test-page-warning-macro";
        String exportFormat = "docx";
        String pageTitle = "Warning Macro";
        String pageBody = "<ac:structured-macro ac:name=\"warning\">" +
                "<ac:rich-text-body><p>This is a warning message</p></ac:rich-text-body>" +
                "</ac:structured-macro>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithConfluenceNoteMacro() {
        // Arrange - Test Confluence note macro
        String pageId = "test-page-note-macro";
        String exportFormat = "docx";
        String pageTitle = "Note Macro";
        String pageBody = "<ac:structured-macro ac:name=\"note\">" +
                "<ac:rich-text-body><p>This is a note</p></ac:rich-text-body>" +
                "</ac:structured-macro>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithConfluenceTipMacro() {
        // Arrange - Test Confluence tip macro
        String pageId = "test-page-tip-macro";
        String exportFormat = "docx";
        String pageTitle = "Tip Macro";
        String pageBody = "<ac:structured-macro ac:name=\"tip\">" +
                "<ac:rich-text-body><p>This is a tip</p></ac:rich-text-body>" +
                "</ac:structured-macro>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithTableWithAttributes() {
        // Arrange - Test table with various attributes
        String pageId = "test-page-table-attrs";
        String exportFormat = "docx";
        String pageTitle = "Table Attributes";
        String pageBody = "<table class=\"confluenceTable\" data-layout=\"default\" style=\"width: 100%;\">" +
                "<colgroup><col style=\"width: 50%;\"/><col style=\"width: 50%;\"/></colgroup>" +
                "<tr class=\"confluenceTr\"><td class=\"confluenceTd\" data-highlight-colour=\"#f0f0f0\">Cell</td></tr>" +
                "</table>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithEmptyTable() {
        // Arrange - Test empty table
        String pageId = "test-page-empty-table";
        String exportFormat = "docx";
        String pageTitle = "Empty Table";
        String pageBody = "<table></table>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithTableOnlyHeaders() {
        // Arrange - Test table with only headers
        String pageId = "test-page-headers-only";
        String exportFormat = "docx";
        String pageTitle = "Headers Only";
        String pageBody = "<table><thead><tr><th>Header 1</th><th>Header 2</th></tr></thead></table>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithControlCharacters() {
        // Arrange - Test content with control characters that need to be sanitized
        String pageId = "test-page-control-chars";
        String exportFormat = "docx";
        String pageTitle = "Control Characters";
        String pageBody = "<p>Text with control chars: \u0000\u0001\u0002</p>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithMultipleNestedTables() {
        // Arrange - Test multiple levels of nested tables
        String pageId = "test-page-multi-nested";
        String exportFormat = "docx";
        String pageTitle = "Multi Nested Tables";
        String pageBody = "<table><tr><td>" +
                "<table><tr><td>" +
                "<table><tr><td>Deeply nested</td></tr></table>" +
                "</td></tr></table>" +
                "</td></tr></table>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithLargeColspan() {
        // Arrange - Test table with large colspan
        String pageId = "test-page-large-colspan";
        String exportFormat = "docx";
        String pageTitle = "Large Colspan";
        String pageBody = "<table><tr><td colspan=\"10\">Wide cell</td></tr>" +
                "<tr><td>1</td><td>2</td><td>3</td><td>4</td><td>5</td>" +
                "<td>6</td><td>7</td><td>8</td><td>9</td><td>10</td></tr></table>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithLargeRowspan() {
        // Arrange - Test table with large rowspan
        String pageId = "test-page-large-rowspan";
        String exportFormat = "docx";
        String pageTitle = "Large Rowspan";
        String pageBody = "<table>" +
                "<tr><td rowspan=\"5\">Tall cell</td><td>Row 1</td></tr>" +
                "<tr><td>Row 2</td></tr>" +
                "<tr><td>Row 3</td></tr>" +
                "<tr><td>Row 4</td></tr>" +
                "<tr><td>Row 5</td></tr>" +
                "</table>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithMixedColspanRowspan() {
        // Arrange - Test table with mixed colspan and rowspan
        String pageId = "test-page-mixed-span";
        String exportFormat = "docx";
        String pageTitle = "Mixed Span";
        String pageBody = "<table>" +
                "<tr><td colspan=\"2\" rowspan=\"2\">Big cell</td><td>Cell 1</td></tr>" +
                "<tr><td>Cell 2</td></tr>" +
                "<tr><td>Cell 3</td><td>Cell 4</td><td>Cell 5</td></tr>" +
                "</table>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithUnescapedAmpersand2() {
        // Arrange - Test content with unescaped ampersand that might cause XML parsing issues
        String pageId = "test-page-ampersand-2";
        String exportFormat = "docx";
        String pageTitle = "Ampersand Test";
        String pageBody = "<p>Company A & Company B partnership</p>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithConfluenceLayoutMacros2() {
        // Arrange - Test content with Confluence layout macros
        String pageId = "test-page-layout-2";
        String exportFormat = "docx";
        String pageTitle = "Layout Test";
        String pageBody = "<ac:layout><ac:layout-section ac:type=\"two_equal\">" +
                "<ac:layout-cell><p>Left column</p></ac:layout-cell>" +
                "<ac:layout-cell><p>Right column</p></ac:layout-cell>" +
                "</ac:layout-section></ac:layout>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithConfluenceStructuredMacro2() {
        // Arrange - Test content with Confluence structured macro
        String pageId = "test-page-macro-2";
        String exportFormat = "docx";
        String pageTitle = "Macro Test";
        String pageBody = "<ac:structured-macro ac:name=\"code\">" +
                "<ac:parameter ac:name=\"language\">java</ac:parameter>" +
                "<ac:plain-text-body><![CDATA[public class Test {}]]></ac:plain-text-body>" +
                "</ac:structured-macro>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithConfluencePlaceholder2() {
        // Arrange - Test content with Confluence placeholder
        String pageId = "test-page-placeholder-2";
        String exportFormat = "docx";
        String pageTitle = "Placeholder Test";
        String pageBody = "<p>Before <ac:placeholder>Enter text here</ac:placeholder> After</p>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithRiNamespaceElements() {
        // Arrange - Test content with ri: namespace elements
        String pageId = "test-page-ri-namespace";
        String exportFormat = "docx";
        String pageTitle = "RI Namespace Test";
        String pageBody = "<p>Link to <ri:page ri:content-title=\"Other Page\">page</ri:page></p>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithAtNamespaceElements() {
        // Arrange - Test content with at: namespace elements
        String pageId = "test-page-at-namespace";
        String exportFormat = "docx";
        String pageTitle = "AT Namespace Test";
        String pageBody = "<p>Attachment: <at:attachment at:filename=\"file.pdf\"/></p>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithTableMissingTbody() {
        // Arrange - Test table without tbody
        String pageId = "test-page-no-tbody";
        String exportFormat = "docx";
        String pageTitle = "No Tbody";
        String pageBody = "<table><tr><td>Cell 1</td><td>Cell 2</td></tr></table>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithTableMissingThead() {
        // Arrange - Test table without thead
        String pageId = "test-page-no-thead";
        String exportFormat = "docx";
        String pageTitle = "No Thead";
        String pageBody = "<table><tbody><tr><td>Cell 1</td><td>Cell 2</td></tr></tbody></table>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithEmptyTableCells2() {
        // Arrange - Test table with empty cells
        String pageId = "test-page-empty-cells-2";
        String exportFormat = "docx";
        String pageTitle = "Empty Cells";
        String pageBody = "<table><tr><td></td><td></td></tr><tr><td></td><td></td></tr></table>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithSelfClosingTags2() {
        // Arrange - Test content with self-closing tags
        String pageId = "test-page-self-closing-2";
        String exportFormat = "docx";
        String pageTitle = "Self Closing";
        String pageBody = "<p>Line 1<br/>Line 2<hr/>Line 3</p>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithImgTag() {
        // Arrange - Test content with img tag
        String pageId = "test-page-img";
        String exportFormat = "docx";
        String pageTitle = "Image Test";
        String pageBody = "<p>Image: <img src=\"https://example.com/image.png\" alt=\"Test\"/></p>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithInputTag() {
        // Arrange - Test content with input tag
        String pageId = "test-page-input";
        String exportFormat = "docx";
        String pageTitle = "Input Test";
        String pageBody = "<p>Input: <input type=\"text\" value=\"test\"/></p>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithMetaTag() {
        // Arrange - Test content with meta tag
        String pageId = "test-page-meta";
        String exportFormat = "docx";
        String pageTitle = "Meta Test";
        String pageBody = "<meta charset=\"UTF-8\"/><p>Content</p>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithLinkTag() {
        // Arrange - Test content with link tag
        String pageId = "test-page-link";
        String exportFormat = "docx";
        String pageTitle = "Link Test";
        String pageBody = "<link rel=\"stylesheet\" href=\"style.css\"/><p>Content</p>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithAreaTag() {
        // Arrange - Test content with area tag
        String pageId = "test-page-area";
        String exportFormat = "docx";
        String pageTitle = "Area Test";
        String pageBody = "<map name=\"test\"><area shape=\"rect\" coords=\"0,0,100,100\" href=\"#\"/></map><p>Content</p>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithBaseTag() {
        // Arrange - Test content with base tag
        String pageId = "test-page-base";
        String exportFormat = "docx";
        String pageTitle = "Base Test";
        String pageBody = "<base href=\"https://example.com/\"/><p>Content</p>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithColTag() {
        // Arrange - Test content with col tag
        String pageId = "test-page-col";
        String exportFormat = "docx";
        String pageTitle = "Col Test";
        String pageBody = "<table><colgroup><col span=\"2\"/></colgroup><tr><td>A</td><td>B</td></tr></table>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithEmbedTag() {
        // Arrange - Test content with embed tag
        String pageId = "test-page-embed";
        String exportFormat = "docx";
        String pageTitle = "Embed Test";
        String pageBody = "<embed src=\"video.mp4\" type=\"video/mp4\"/><p>Content</p>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithSourceTag() {
        // Arrange - Test content with source tag
        String pageId = "test-page-source";
        String exportFormat = "docx";
        String pageTitle = "Source Test";
        String pageBody = "<video><source src=\"video.mp4\" type=\"video/mp4\"/></video><p>Content</p>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithTrackTag() {
        // Arrange - Test content with track tag
        String pageId = "test-page-track";
        String exportFormat = "docx";
        String pageTitle = "Track Test";
        String pageBody = "<video><track kind=\"subtitles\" src=\"subs.vtt\"/></video><p>Content</p>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithWbrTag() {
        // Arrange - Test content with wbr tag
        String pageId = "test-page-wbr";
        String exportFormat = "docx";
        String pageTitle = "Wbr Test";
        String pageBody = "<p>Very<wbr/>Long<wbr/>Word</p>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithVeryDeepNesting() {
        // Arrange - Test content with very deep nesting
        String pageId = "test-page-deep-nesting";
        String exportFormat = "docx";
        String pageTitle = "Deep Nesting";
        String pageBody = "<div><div><div><div><div><div><div><div><div><div>" +
                "<p>Deeply nested content</p>" +
                "</div></div></div></div></div></div></div></div></div></div>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithMultipleAmpersands() {
        // Arrange - Test content with multiple ampersands
        String pageId = "test-page-multi-amp";
        String exportFormat = "docx";
        String pageTitle = "Multi Ampersand";
        String pageBody = "<p>A & B & C & D & E</p>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithHtmlEntities2() {
        // Arrange - Test content with HTML entities
        String pageId = "test-page-entities-2";
        String exportFormat = "docx";
        String pageTitle = "HTML Entities";
        String pageBody = "<p>&lt;tag&gt; &amp; &quot;quoted&quot; &apos;apostrophe&apos;</p>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithNumericEntities() {
        // Arrange - Test content with numeric entities
        String pageId = "test-page-numeric-entities";
        String exportFormat = "docx";
        String pageTitle = "Numeric Entities";
        String pageBody = "<p>&#60;tag&#62; &#38; &#34;quoted&#34;</p>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithCdataSection() {
        // Arrange - Test content with CDATA section
        String pageId = "test-page-cdata";
        String exportFormat = "docx";
        String pageTitle = "CDATA Test";
        String pageBody = "<p>Before</p><![CDATA[<script>alert('test')</script>]]><p>After</p>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithComments() {
        // Arrange - Test content with HTML comments
        String pageId = "test-page-comments";
        String exportFormat = "docx";
        String pageTitle = "Comments Test";
        String pageBody = "<p>Before</p><!-- This is a comment --><p>After</p>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithTableInTable() {
        // Arrange - Test nested tables
        String pageId = "test-page-nested-table";
        String exportFormat = "docx";
        String pageTitle = "Nested Table";
        String pageBody = "<table><tr><td><table><tr><td>Inner</td></tr></table></td></tr></table>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithTableWithStyles() {
        // Arrange - Test table with inline styles
        String pageId = "test-page-styled-table";
        String exportFormat = "docx";
        String pageTitle = "Styled Table";
        String pageBody = "<table style=\"border: 1px solid black;\">" +
                "<tr style=\"background-color: #f0f0f0;\">" +
                "<td style=\"padding: 10px;\">Cell</td></tr></table>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithTableWithClasses() {
        // Arrange - Test table with CSS classes
        String pageId = "test-page-class-table";
        String exportFormat = "docx";
        String pageTitle = "Class Table";
        String pageBody = "<table class=\"confluenceTable\">" +
                "<tr class=\"confluenceRow\">" +
                "<td class=\"confluenceTd\">Cell</td></tr></table>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithTableWithDataAttributes() {
        // Arrange - Test table with data attributes
        String pageId = "test-page-data-table";
        String exportFormat = "docx";
        String pageTitle = "Data Table";
        String pageBody = "<table data-table-id=\"123\">" +
                "<tr data-row-id=\"1\">" +
                "<td data-cell-id=\"1\">Cell</td></tr></table>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithConfluenceNamespaceAttributes() {
        // Arrange - Test table with Confluence namespace attributes (ac:, ri:, at:)
        // This should trigger convertTableToDiv
        String pageId = "test-page-namespace-attrs";
        String exportFormat = "docx";
        String pageTitle = "Namespace Attributes";
        String pageBody = "<table ac:name=\"test\" data-layout=\"default\">" +
                "<tr><td ac:parameter=\"value\">Cell with ac: attribute</td></tr>" +
                "</table>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithRiNamespaceAttributes() {
        // Arrange - Test table with ri: namespace attributes
        String pageId = "test-page-ri-namespace";
        String exportFormat = "docx";
        String pageTitle = "RI Namespace";
        String pageBody = "<table ri:content-id=\"123\">" +
                "<tr><td ri:version=\"1\">Cell with ri: attribute</td></tr>" +
                "</table>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithAtNamespaceAttributes() {
        // Arrange - Test table with at: namespace attributes
        String pageId = "test-page-at-namespace";
        String exportFormat = "docx";
        String pageTitle = "AT Namespace";
        String pageBody = "<table at:type=\"custom\">" +
                "<tr><td at:value=\"test\">Cell with at: attribute</td></tr>" +
                "</table>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithDeeplyNestedTables() {
        // Arrange - Test deeply nested tables (more than 3 levels) to trigger convertTableToDiv
        String pageId = "test-page-deeply-nested";
        String exportFormat = "docx";
        String pageTitle = "Deeply Nested Tables";
        String pageBody = "<table><tr><td>" +
                "<table><tr><td>" +
                "<table><tr><td>" +
                "<table><tr><td>Level 4 nested</td></tr></table>" +
                "</td></tr></table>" +
                "</td></tr></table>" +
                "</td></tr></table>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithMalformedColTags() {
        // Arrange - Test table with malformed col tags (not self-closing)
        String pageId = "test-page-malformed-col";
        String exportFormat = "docx";
        String pageTitle = "Malformed Col";
        String pageBody = "<table><colgroup><col><col></colgroup>" +
                "<tr><td>Cell 1</td><td>Cell 2</td></tr></table>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithTableContainingEmptyRows() {
        // Arrange - Test table with empty rows to trigger fallback path
        String pageId = "test-page-empty-rows";
        String exportFormat = "docx";
        String pageTitle = "Empty Rows";
        String pageBody = "<table><tr></tr><tr></tr><tr><td>Only cell</td></tr></table>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithTableContainingOnlyText() {
        // Arrange - Test table with only text content (no cells)
        String pageId = "test-page-text-only-table";
        String exportFormat = "docx";
        String pageTitle = "Text Only Table";
        String pageBody = "<table>Some text without proper structure</table>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithConfluenceMacroInTable() {
        // Arrange - Test table containing Confluence macros
        String pageId = "test-page-macro-table";
        String exportFormat = "docx";
        String pageTitle = "Macro Table";
        String pageBody = "<table><tr><td>" +
                "<ac:structured-macro ac:name=\"code\">" +
                "<ac:parameter ac:name=\"language\">java</ac:parameter>" +
                "<ac:plain-text-body>System.out.println(\"Hello\");</ac:plain-text-body>" +
                "</ac:structured-macro>" +
                "</td></tr></table>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithConfluenceLayoutInTable() {
        // Arrange - Test table containing Confluence layout elements
        String pageId = "test-page-layout-table";
        String exportFormat = "docx";
        String pageTitle = "Layout Table";
        String pageBody = "<table><tr><td>" +
                "<ac:layout><ac:layout-section ac:type=\"single\">" +
                "<ac:layout-cell><p>Content in layout</p></ac:layout-cell>" +
                "</ac:layout-section></ac:layout>" +
                "</td></tr></table>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithRiAttachmentInTable() {
        // Arrange - Test table containing ri:attachment elements
        String pageId = "test-page-attachment-table";
        String exportFormat = "docx";
        String pageTitle = "Attachment Table";
        String pageBody = "<table><tr><td>" +
                "<ri:attachment ri:filename=\"document.pdf\"/>" +
                "</td></tr></table>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithComplexConfluenceContent() {
        // Arrange - Test complex Confluence content with multiple namespace elements
        String pageId = "test-page-complex-confluence";
        String exportFormat = "docx";
        String pageTitle = "Complex Confluence";
        String pageBody = "<ac:layout>" +
                "<ac:layout-section ac:type=\"two_equal\">" +
                "<ac:layout-cell><p>Left column</p></ac:layout-cell>" +
                "<ac:layout-cell><p>Right column</p></ac:layout-cell>" +
                "</ac:layout-section>" +
                "</ac:layout>" +
                "<table ac:local-id=\"123\">" +
                "<tr><td><ac:placeholder>Enter text</ac:placeholder></td></tr>" +
                "</table>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithEscapedXmlCharacters() {
        // Arrange - Test content with properly escaped XML characters
        String pageId = "test-page-escaped-xml";
        String exportFormat = "docx";
        String pageTitle = "Escaped XML Chars";
        String pageBody = "<p>Content with special chars: &amp; &lt; &gt; &quot;</p>" +
                "<table><tr><td>Cell with &amp; ampersand</td></tr></table>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithSelfClosingTagIssues() {
        // Arrange - Test content with self-closing tag issues
        String pageId = "test-page-self-closing";
        String exportFormat = "docx";
        String pageTitle = "Self Closing Tags";
        String pageBody = "<p>Paragraph</p><br><hr><img src=\"test.png\">" +
                "<table><tr><td>Cell</td></tr></table>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithVeryLongTableContent() {
        // Arrange - Test table with very long content
        String pageId = "test-page-long-content";
        String exportFormat = "docx";
        String pageTitle = "Long Content";
        StringBuilder longContent = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            longContent.append("This is a very long text content that repeats many times. ");
        }
        String pageBody = "<table><tr><td>" + longContent + "</td></tr></table>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithTableContainingImages() {
        // Arrange - Test table containing image elements
        String pageId = "test-page-image-table";
        String exportFormat = "docx";
        String pageTitle = "Image Table";
        String pageBody = "<table><tr><td>" +
                "<ac:image><ri:attachment ri:filename=\"image.png\"/></ac:image>" +
                "</td></tr></table>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithTableNoRowsButNamespace() {
        // Arrange - Table with namespace attributes but no rows - triggers fallback path
        String pageId = "test-page-no-rows-namespace";
        String exportFormat = "docx";
        String pageTitle = "No Rows Namespace";
        String pageBody = "<table ac:local-id=\"123\"></table>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithTableRowsNoCells() {
        // Arrange - Table with rows but no cells - triggers fallback path
        String pageId = "test-page-rows-no-cells";
        String exportFormat = "docx";
        String pageTitle = "Rows No Cells";
        String pageBody = "<table ac:name=\"test\"><tr></tr><tr></tr></table>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithTableTextOnlyNoStructure() {
        // Arrange - Table with text but no proper structure - triggers text fallback
        String pageId = "test-page-text-no-structure";
        String exportFormat = "docx";
        String pageTitle = "Text No Structure";
        String pageBody = "<table ac:local-id=\"456\">Just some text without any rows or cells</table>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithFiveNestedTables() {
        // Arrange - Five levels of nested tables to trigger aggressive fallback
        String pageId = "test-page-five-nested";
        String exportFormat = "docx";
        String pageTitle = "Five Nested Tables";
        String pageBody = "<table><tr><td>" +
                "<table><tr><td>" +
                "<table><tr><td>" +
                "<table><tr><td>" +
                "<table><tr><td>Level 5</td></tr></table>" +
                "</td></tr></table>" +
                "</td></tr></table>" +
                "</td></tr></table>" +
                "</td></tr></table>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithMixedNamespaceElementsExtended() {
        // Arrange - Mixed namespace elements to trigger various conversion paths
        String pageId = "test-page-mixed-namespace-ext";
        String exportFormat = "docx";
        String pageTitle = "Mixed Namespace Extended";
        String pageBody = "<ac:layout><ac:layout-section ac:type=\"single\">" +
                "<ac:layout-cell><p>Content</p></ac:layout-cell>" +
                "</ac:layout-section></ac:layout>" +
                "<ri:page ri:content-title=\"Test\"/>" +
                "<at:type at:value=\"custom\">Custom type</at:type>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithPlaceholderElements() {
        // Arrange - Confluence placeholder elements
        String pageId = "test-page-placeholders";
        String exportFormat = "docx";
        String pageTitle = "Placeholders";
        String pageBody = "<p>Before</p>" +
                "<ac:placeholder>Enter your text here</ac:placeholder>" +
                "<ac:placeholder>Another placeholder</ac:placeholder>" +
                "<p>After</p>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithCodeMacro() {
        // Arrange - Code macro with language parameter
        String pageId = "test-page-code-macro";
        String exportFormat = "docx";
        String pageTitle = "Code Macro";
        String pageBody = "<ac:structured-macro ac:name=\"code\">" +
                "<ac:parameter ac:name=\"language\">java</ac:parameter>" +
                "<ac:parameter ac:name=\"title\">Example</ac:parameter>" +
                "<ac:plain-text-body>public class Test { }</ac:plain-text-body>" +
                "</ac:structured-macro>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithInfoMacro() {
        // Arrange - Info macro
        String pageId = "test-page-info-macro";
        String exportFormat = "docx";
        String pageTitle = "Info Macro";
        String pageBody = "<ac:structured-macro ac:name=\"info\">" +
                "<ac:rich-text-body><p>This is an info panel</p></ac:rich-text-body>" +
                "</ac:structured-macro>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithWarningMacro() {
        // Arrange - Warning macro
        String pageId = "test-page-warning-macro";
        String exportFormat = "docx";
        String pageTitle = "Warning Macro";
        String pageBody = "<ac:structured-macro ac:name=\"warning\">" +
                "<ac:rich-text-body><p>This is a warning</p></ac:rich-text-body>" +
                "</ac:structured-macro>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithExpandMacro() {
        // Arrange - Expand macro
        String pageId = "test-page-expand-macro";
        String exportFormat = "docx";
        String pageTitle = "Expand Macro";
        String pageBody = "<ac:structured-macro ac:name=\"expand\">" +
                "<ac:parameter ac:name=\"title\">Click to expand</ac:parameter>" +
                "<ac:rich-text-body><p>Hidden content</p></ac:rich-text-body>" +
                "</ac:structured-macro>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithTocMacro() {
        // Arrange - Table of contents macro
        String pageId = "test-page-toc-macro";
        String exportFormat = "docx";
        String pageTitle = "TOC Macro";
        String pageBody = "<ac:structured-macro ac:name=\"toc\">" +
                "<ac:parameter ac:name=\"printable\">true</ac:parameter>" +
                "</ac:structured-macro>" +
                "<h1>Heading 1</h1><p>Content</p>" +
                "<h2>Heading 2</h2><p>More content</p>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithStatusMacro() {
        // Arrange - Status macro
        String pageId = "test-page-status-macro";
        String exportFormat = "docx";
        String pageTitle = "Status Macro";
        String pageBody = "<p>Status: " +
                "<ac:structured-macro ac:name=\"status\">" +
                "<ac:parameter ac:name=\"colour\">Green</ac:parameter>" +
                "<ac:parameter ac:name=\"title\">DONE</ac:parameter>" +
                "</ac:structured-macro></p>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithEmoticon() {
        // Arrange - Emoticon element
        String pageId = "test-page-emoticon";
        String exportFormat = "docx";
        String pageTitle = "Emoticon";
        String pageBody = "<p>Hello <ac:emoticon ac:name=\"smile\"/> World</p>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithUserMention() {
        // Arrange - User mention element
        String pageId = "test-page-user-mention";
        String exportFormat = "docx";
        String pageTitle = "User Mention";
        String pageBody = "<p>Assigned to <ri:user ri:account-id=\"123456\">John Doe</ri:user></p>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithPageLink() {
        // Arrange - Page link element
        String pageId = "test-page-page-link";
        String exportFormat = "docx";
        String pageTitle = "Page Link";
        String pageBody = "<p>See <ac:link><ri:page ri:content-title=\"Other Page\"/></ac:link></p>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithAttachmentLink() {
        // Arrange - Attachment link element
        String pageId = "test-page-attachment-link";
        String exportFormat = "docx";
        String pageTitle = "Attachment Link";
        String pageBody = "<p>Download <ac:link><ri:attachment ri:filename=\"document.pdf\"/></ac:link></p>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithMultipleLayoutSections() {
        // Arrange - Multiple layout sections
        String pageId = "test-page-multi-layout";
        String exportFormat = "docx";
        String pageTitle = "Multi Layout";
        String pageBody = "<ac:layout>" +
                "<ac:layout-section ac:type=\"two_equal\">" +
                "<ac:layout-cell><p>Left</p></ac:layout-cell>" +
                "<ac:layout-cell><p>Right</p></ac:layout-cell>" +
                "</ac:layout-section>" +
                "<ac:layout-section ac:type=\"three_equal\">" +
                "<ac:layout-cell><p>Col 1</p></ac:layout-cell>" +
                "<ac:layout-cell><p>Col 2</p></ac:layout-cell>" +
                "<ac:layout-cell><p>Col 3</p></ac:layout-cell>" +
                "</ac:layout-section>" +
                "</ac:layout>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithTaskList() {
        // Arrange - Task list macro
        String pageId = "test-page-task-list";
        String exportFormat = "docx";
        String pageTitle = "Task List";
        String pageBody = "<ac:task-list>" +
                "<ac:task><ac:task-status>complete</ac:task-status><ac:task-body>Task 1</ac:task-body></ac:task>" +
                "<ac:task><ac:task-status>incomplete</ac:task-status><ac:task-body>Task 2</ac:task-body></ac:task>" +
                "</ac:task-list>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithInlineComment() {
        // Arrange - Inline comment
        String pageId = "test-page-inline-comment";
        String exportFormat = "docx";
        String pageTitle = "Inline Comment";
        String pageBody = "<p>This is <ac:inline-comment-marker ac:ref=\"123\">commented text</ac:inline-comment-marker></p>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithJiraMacro() {
        // Arrange - Jira macro
        String pageId = "test-page-jira-macro";
        String exportFormat = "docx";
        String pageTitle = "Jira Macro";
        String pageBody = "<ac:structured-macro ac:name=\"jira\">" +
                "<ac:parameter ac:name=\"server\">Jira</ac:parameter>" +
                "<ac:parameter ac:name=\"key\">PROJ-123</ac:parameter>" +
                "</ac:structured-macro>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    @Test
    public void testDownloadPageAsDocxFileWithStatus_WithPanelMacro() {
        // Arrange - Panel macro
        String pageId = "test-page-panel-macro";
        String exportFormat = "docx";
        String pageTitle = "Panel Macro";
        String pageBody = "<ac:structured-macro ac:name=\"panel\">" +
                "<ac:parameter ac:name=\"title\">Panel Title</ac:parameter>" +
                "<ac:parameter ac:name=\"borderStyle\">solid</ac:parameter>" +
                "<ac:rich-text-body><p>Panel content</p></ac:rich-text-body>" +
                "</ac:structured-macro>";

        Page mockPage = createMockPage(pageId, pageTitle, pageBody);
        when(confluenceService.getPageById(pageId)).thenReturn(mockPage);

        // Act
        Pair<File, Boolean> result = pageDocxService.downloadPageAsDocxFileWithStatus(pageId, exportFormat);

        // Assert
        Assertions.assertNotNull(result);
        verify(confluenceService).getPageById(pageId);
    }

    /**
     * Helper method to create a mock Page object
     */
    private Page createMockPage(String pageId, String title, String bodyContent) {
        Storage storage = Storage.builder()
                .setValue(bodyContent)
                .setRepresentation("storage")
                .build();

        Body body = Body.setBody(storage);

        return new Page.PageBuilder(pageId, title)
                .body(body)
                .build();
    }
}
