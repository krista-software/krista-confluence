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
import com.kristasoft.extensions.essentials.collaboration.confluence.dto.Page;
import com.kristasoft.extensions.essentials.collaboration.confluence.service.ConfluenceService;
import com.kristasoft.extensions.essentials.collaboration.confluence.service.PageDocxService;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.stores.Pair;
import com.kristasoft.extensions.essentials.collaboration.confluence.utils.ConfluenceConstants;
import org.docx4j.convert.in.xhtml.XHTMLImporterImpl;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

/**
 * Implementation of PageDocxService for converting Confluence pages to Docx format
 * Inspired by Zendesk HelpCenterDaoImpl downloadArticleAsPDFOrWordFile functionality
 */
@Service
@ContractsProvided(PageDocxService.class)
public class PageDocxServiceImpl implements PageDocxService {

    private static final Logger LOG = LoggerFactory.getLogger(PageDocxServiceImpl.class);

    private final ConfluenceService confluenceService;
    private final FileRepository fileRepository;

    // Thread-local storage for original HTML content to use in fallbacks
    private static final ThreadLocal<String> originalHtmlContent = new ThreadLocal<>();

    @Inject
    public PageDocxServiceImpl(ConfluenceService confluenceService, FileRepository fileRepository) {
        this.confluenceService = confluenceService;
        this.fileRepository = fileRepository;
    }
    
    @Override
    public File downloadPageAsDocxFile(String pageId, String exportFormat) {
        return downloadPageAsDocxFile(pageId, exportFormat, null);
    }
    
    @Override
    public File downloadPageAsDocxFile(String pageId, String exportFormat, String customFileName) {
        try {
            LOG.info("→ downloadPageAsDocxFile: Starting operation for pageId: {}, exportFormat: {}", pageId, exportFormat);

            // Step 1: Get page content
            PageContent pageContent = retrievePageContent(pageId);
            LOG.debug("→ Retrieved page content for pageId: {}", pageId);

            // Step 2: Create the document content
            String documentContent = createDocumentContent(pageContent.getTitle(), pageContent.getBody());
            LOG.debug("→ Created document content, length: {}", documentContent.length());

            // Step 3: Create the file and convert to requested format
            String fileName = customFileName != null ? customFileName :
                sanitizeFileName(pageContent.getTitle()) + "_" + pageId + "." + exportFormat;
            java.io.File tempFile = createTempFile(fileName, documentContent, exportFormat);

            // Step 4: Convert to Krista File object
            File result = convertToKristaFile(tempFile, fileName);
            LOG.info("→ downloadPageAsDocxFile: Successfully created Docx file for pageId: {}", pageId);

            return result;

        } catch (Exception e) {
            LOG.error("→ downloadPageAsDocxFile: Error occurred - {}", e.getMessage(), e);
            throw new RuntimeException("Failed to download page as Docx file: " + e.getMessage(), e);
        } finally {
            // Clean up ThreadLocal to prevent memory leaks
            originalHtmlContent.remove();
        }
    }

    @Override
    public Pair<File, Boolean> downloadPageAsDocxFileWithStatus(String pageId, String exportFormat) {
        return downloadPageAsDocxFileWithStatus(pageId, exportFormat, null);
    }

    @Override
    public Pair<File, Boolean> downloadPageAsDocxFileWithStatus(String pageId, String exportFormat, String customFileName) {
        boolean parsingSuccess = true;
        File result = null;

        try {
            LOG.info("→ downloadPageAsDocxFileWithStatus: Starting operation for pageId: {}, exportFormat: {}", pageId, exportFormat);

            // Step 1: Get page content
            PageContent pageContent = retrievePageContent(pageId);
            LOG.debug("→ Retrieved page content for pageId: {}", pageId);

            // Step 2: Create the document content
            String documentContent = createDocumentContent(pageContent.getTitle(), pageContent.getBody());
            LOG.debug("→ Created document content, length: {}", documentContent.length());

            // Step 3: Create the file and convert to requested format
            String fileName = customFileName != null ? customFileName :
                sanitizeFileName(pageContent.getTitle()) + "_" + pageId + "." + exportFormat;
            java.io.File tempFile = createTempFile(fileName, documentContent, exportFormat);

            // Step 4: Convert to Krista File object
            result = convertToKristaFile(tempFile, fileName);
            LOG.info("→ downloadPageAsDocxFileWithStatus: Successfully created Docx file for pageId: {}", pageId);

        } catch (Exception e) {
            LOG.error("→ downloadPageAsDocxFileWithStatus: Error occurred during parsing - {}", e.getMessage(), e);
            parsingSuccess = false;

            // Create empty DOCX file as fallback
            try {
                String fallbackFileName = customFileName != null ? customFileName :
                    "error_page_" + pageId + "." + exportFormat;
                result = createEmptyDocxFile(fallbackFileName);
                LOG.info("→ downloadPageAsDocxFileWithStatus: Created empty DOCX file due to parsing failure");
            } catch (Exception fallbackException) {
                LOG.error("→ downloadPageAsDocxFileWithStatus: Failed to create empty DOCX file - {}", fallbackException.getMessage(), fallbackException);
                // Return null file if even empty file creation fails
                result = null;
            }
        } finally {
            // Clean up ThreadLocal to prevent memory leaks
            originalHtmlContent.remove();
        }

        return Pair.of(result, parsingSuccess);
    }
    
    /**
     * Retrieves page content including title and body
     */
    private PageContent retrievePageContent(String pageId) {
        Page page = confluenceService.getPageById(pageId);
        if (page == null) {
            throw new IllegalArgumentException("Page not found with ID: " + pageId);
        }
        
        String pageTitle = page.getTitle();
        String pageBody = "";
        
        // Extract body content from the page
        if (page.getBody() != null && page.getBody().getStorage() != null) {
            pageBody = page.getBody().getStorage().getValue();
        }
        
        return new PageContent(pageTitle, pageBody);
    }
    
    /**
     * Creates the complete document content with HTML structure
     */
    private String createDocumentContent(String pageTitle, String pageBody) {
        StringBuilder documentContent = new StringBuilder();
        documentContent.append("<html><head><title>").append(sanitizeForXml(pageTitle)).append("</title></head><body>");
        documentContent.append("<h1>").append(sanitizeForXml(pageTitle)).append("</h1>");
        documentContent.append(pageBody != null ? pageBody : "");
        documentContent.append("</body></html>");
        
        LOG.debug("→ Document content created, length: {}", documentContent.length());
        return documentContent.toString();
    }
    
    /**
     * Creates a temporary file with the given content and format
     */
    private java.io.File createTempFile(String fileName, String htmlContent, String format) throws IOException {
        String tempDir = System.getProperty("java.io.tmpdir");
        if (tempDir == null) {
            tempDir = "/tmp";
        }
        
        java.io.File tempFile = new java.io.File(tempDir, fileName);
        
        if ("docx".equalsIgnoreCase(format)) {
            // Generate proper DOCX file using docx4j (inspired by Zendesk implementation)
            return generateDocxFile(tempFile, htmlContent);
        } else {
            throw new IllegalArgumentException("Unsupported format: " + format);
        }
    }
    
    /**
     * Generates a proper DOCX file from HTML content using docx4j
     * Inspired by Zendesk DocumentManager.generatedDocxFile() and DocumentTemplateUtil.addRichTextPara()
     */
    private java.io.File generateDocxFile(java.io.File outputFile, String htmlContent) throws IOException {
        try {
            LOG.debug("→ Creating WordprocessingMLPackage for DOCX generation");
            
            // Create Word package and convert content
            WordprocessingMLPackage wordPackage = createWordPackage();
            String xhtmlContent = convertToValidXhtml(htmlContent);
            
            // Import content and save document
            importXhtmlContentToWordDocument(wordPackage, xhtmlContent);
            saveWordDocument(wordPackage, outputFile);
            
            return outputFile;
            
        } catch (Docx4JException e) {
            handleDocx4JException(e);
            throw new IOException("Failed to generate DOCX file: " + extractDocx4JErrorDetails(e), e);
        } catch (Exception e) {
            LOG.error("→ Unexpected error generating DOCX file: {}", e.getMessage(), e);
            throw new IOException("Unexpected error generating DOCX file: " + e.getMessage(), e);
        }
    }
    
    /**
     * Creates a new WordprocessingMLPackage for DOCX generation
     */
    private WordprocessingMLPackage createWordPackage() throws Docx4JException {
        return WordprocessingMLPackage.createPackage();
    }
    
    /**
     * Imports XHTML content into the Word document
     */
    private void importXhtmlContentToWordDocument(WordprocessingMLPackage wordPackage, String xhtmlContent) throws Docx4JException {
        LOG.debug("→ Converted HTML to XHTML format");

        try {
            // Import XHTML content into the Word document (inspired by DocumentTemplateUtil.addRichTextPara)
            XHTMLImporterImpl xhtmlImporter = new XHTMLImporterImpl(wordPackage);
            wordPackage.getMainDocumentPart().getContent().addAll(xhtmlImporter.convert(xhtmlContent, null));
            LOG.debug("→ Imported XHTML content into Word document");
        } catch (Exception e) {
            LOG.error("→ Failed to import XHTML content, trying aggressive fallback: {}", e.getMessage());

            // Extract the original HTML content from the error context and create aggressive fallback
            String originalHtml = extractOriginalHtmlFromContext();
            String fallbackXhtml = createAggressiveFallbackXhtml(originalHtml);

            try {
                XHTMLImporterImpl fallbackImporter = new XHTMLImporterImpl(wordPackage);
                wordPackage.getMainDocumentPart().getContent().addAll(fallbackImporter.convert(fallbackXhtml, null));
                LOG.info("→ Successfully imported content using aggressive fallback");
            } catch (Exception fallbackException) {
                LOG.error("→ Even aggressive fallback failed: {}", fallbackException.getMessage());
                throw new Docx4JException("Failed to import content even with aggressive fallback", fallbackException);
            }
        }
    }

    /**
     * Extracts original HTML content from the current processing context
     */
    private String extractOriginalHtmlFromContext() {
        String original = originalHtmlContent.get();
        if (original != null && !original.trim().isEmpty()) {
            return original;
        }
        // Fallback if no original content is available
        return "<p>Content could not be processed due to formatting issues. Please check the original Confluence page.</p>";
    }
    
    /**
     * Saves the Word document to the specified file
     */
    private void saveWordDocument(WordprocessingMLPackage wordPackage, java.io.File outputFile) throws Docx4JException {
        // Save the document (inspired by DocumentManager.generatedDocxFile)
        wordPackage.save(outputFile);
        LOG.debug("→ Saved DOCX file: {}", outputFile.getAbsolutePath());
    }
    
    /**
     * Handles Docx4JException by logging detailed error information
     */
    private void handleDocx4JException(Docx4JException e) {
        LOG.error("→ Error generating DOCX file: {}", e.getMessage(), e);
        
        // Extract more specific error information
        String errorDetails = extractDocx4JErrorDetails(e);
        LOG.error("→ Detailed error information: {}", errorDetails);
    }
    
    /**
     * Extracts detailed error information from Docx4JException
     */
    private String extractDocx4JErrorDetails(Docx4JException e) {
        String message = e.getMessage();

        // Check the cause for more details
        Throwable cause = e.getCause();
        if (cause != null) {
            String causeMessage = cause.getMessage();
            if (causeMessage != null && causeMessage.contains("entity name must immediately follow")) {
                return "The page content contains invalid XML characters (likely an unescaped ampersand '&'). " +
                       "Please check the page content and ensure special characters are properly formatted. Details: " + causeMessage;
            }
        }

        return message != null ? message : "Unable to generate DOCX file. Please verify the page content is valid and try again.";
    }
    
    /**
     * Converts HTML content to valid XHTML format
     * Inspired by Zendesk DocxContentProcessor.htmlToDocxContent() with enhanced HTML sanitization
     */
    private String convertToValidXhtml(String htmlContent) throws IOException {
        try {
            LOG.debug("→ Converting HTML to valid XHTML format");
            LOG.debug("→ Original HTML content length: {}", htmlContent.length());

            // Store original content for potential fallback use
            originalHtmlContent.set(htmlContent);

            // Pre-sanitize the HTML content to handle problematic ampersands
            String preSanitizedHtml = preSanitizeHtmlContent(htmlContent);
            LOG.debug("→ Pre-sanitized HTML content length: {}", preSanitizedHtml.length());
            
            // Process HTML with JSoup and clean it up
            String cleanedHtml = processHtmlWithJSoup(preSanitizedHtml);
            
            // Generate final XHTML structure
            String xhtml = generateXhtmlStructure(cleanedHtml);
            
            // Validate the generated XHTML
            validateXhtmlContent(xhtml);

            LOG.debug("→ Successfully converted to valid XHTML");
            LOG.debug("→ Final XHTML content length: {}", xhtml.length());
            return xhtml;

        } catch (Exception e) {
            LOG.error("→ Error converting HTML to XHTML: {}", e.getMessage(), e);
            // Try a more aggressive fallback that removes all tables
            return createAggressiveFallbackXhtml(htmlContent);
        }
    }
    
    /**
     * Pre-sanitizes HTML content to handle problematic characters and malformed tags
     */
    private String preSanitizeHtmlContent(String htmlContent) {
        if (htmlContent == null) {
            return "";
        }

        LOG.debug("→ Pre-sanitizing HTML content, original length: {}", htmlContent.length());

        String sanitized = htmlContent
                // First, protect valid HTML entities
                .replaceAll("&(amp|lt|gt|quot|apos|#\\d+|#x[0-9a-fA-F]+);", "ENTITY_PLACEHOLDER_$1_END")
                // Then replace remaining ampersands
                .replaceAll("&", "&amp;")
                // Finally, restore the protected entities
                .replaceAll("ENTITY_PLACEHOLDER_(amp|lt|gt|quot|apos|#\\d+|#x[0-9a-fA-F]+)_END", "&$1;")
                // Fix common malformed self-closing tags before JSoup processing
                .replaceAll("<(br|hr|img|input|meta|link|area|base|embed|source|track|wbr)([^>]*?)(?<!/)(?<!>)(?=\\s|$|<)", "<$1$2 />")
                .replaceAll("<(br|hr|img|input|meta|link|area|base|embed|source|track|wbr)([^>]*?)(?<!/)\\s*(?=[<\\s])", "<$1$2 />")
                // Remove Confluence namespace attributes that cause binding issues
                .replaceAll("\\s+(ac|ri|at):[^\\s>=]*=[\"'][^\"']*[\"']", "") // Remove namespace attributes with quoted values
                .replaceAll("\\s+(ac|ri|at):[^\\s>=]*=[^\\s>]*", "") // Remove namespace attributes with unquoted values
                .replaceAll("\\s+(ac|ri|at):[^\\s>=]*(?=\\s|>)", "") // Remove namespace attributes without values
                // Remove formatting attributes that cause artifacts in the final document
                .replaceAll("\\s+data-highlight-colou?r=[\"'][^\"']*[\"']", "") // Remove highlight color attributes
                .replaceAll("\\s+data-[^\\s>=]*=[\"'][^\"']*[\"']", "") // Remove all data attributes with quoted values
                .replaceAll("\\s+data-[^\\s>=]*=[^\\s>]*", "") // Remove all data attributes with unquoted values
                .replaceAll("\\s+style=[\"'][^\"']*[\"']", "") // Remove style attributes
                .replaceAll("\\s+class=[\"'][^\"']*[\"']", "") // Remove class attributes
                .replaceAll("\\s+id=[\"'][^\"']*[\"']", "") // Remove id attributes
                .replaceAll("\\s+contenteditable=[\"']?[^\"'\\s>]*[\"']?", "") // Remove contenteditable
                .replaceAll("\\s+spellcheck=[\"']?[^\"'\\s>]*[\"']?", "") // Remove spellcheck
                // Fix malformed table tags
                .replaceAll("<table([^>]*)(?<!>)(?=\\s|$|<)", "<table$1>")
                .replaceAll("<(col)([^>]*)(?<!/)(?<!>)(?=\\s|$|<)", "<$1$2 />")
                // Clean up any remaining problematic patterns
                .replaceAll("\\s{2,}", " ") // Normalize multiple spaces to single space
                .trim();

        LOG.debug("→ Pre-sanitized HTML content, new length: {}", sanitized.length());
        return sanitized;
    }
    
    /**
     * Processes HTML with JSoup and cleans it up
     */
    private String processHtmlWithJSoup(String preSanitizedHtml) {
        Document jsoupDoc = Jsoup.parse(preSanitizedHtml);

        // Configure JSoup to output XHTML syntax for proper XML parsing
        jsoupDoc.outputSettings().syntax(Document.OutputSettings.Syntax.xml);

        // Sanitize table structures first (before other processing)
        sanitizeTableStructures(jsoupDoc);

        // Convert problematic tables to div-based layouts if needed
        convertProblematicTablesToDivs(jsoupDoc);

        // Remove Confluence-specific elements that cause namespace issues
        removeConfluenceSpecificElements(jsoupDoc);

        // Clean up problematic attributes that cause XML parsing issues
        sanitizeHtmlAttributes(jsoupDoc);

        // Convert formatting tags to simple versions (inspired by msword copy)
        simplifyHtmlTags(jsoupDoc);

        // Remove complex styling that XHTMLImporter can't handle
        removeComplexStyling(jsoupDoc);

        // Fix self-closing HTML tags to be XHTML compliant
        fixSelfClosingTags(jsoupDoc);

        // Clean up text content to remove formatting artifacts
        cleanupTextContent(jsoupDoc);

        String cleanedHtml = jsoupDoc.body().html();
        LOG.debug("→ Cleaned HTML content length: {}", cleanedHtml.length());

        // Additional HTML entity cleanup on the final HTML
        return convertHtmlEntitiesToUnicode(cleanedHtml);
    }

    /**
     * Sanitizes table structures to prevent XML parsing issues
     * This method specifically addresses malformed col, colgroup, and other table elements
     */
    private void sanitizeTableStructures(Document jsoupDoc) {
        LOG.debug("→ Sanitizing table structures for XML compliance");

        // First, handle malformed col tags - these are the main culprits
        jsoupDoc.select("col").forEach(col -> {
            LOG.debug("→ Processing col element: {}", col.outerHtml());

            // Remove any content from col tags (they should be self-closing)
            if (!col.html().trim().isEmpty()) {
                LOG.debug("→ Removing content from col tag: {}", col.html());
                col.html("");
            }

            // Ensure col has proper attributes or remove problematic ones
            col.attributes().forEach(attr -> {
                String key = attr.getKey();
                String value = attr.getValue();
                if (value == null || value.trim().isEmpty() || value.contains("<") || value.contains(">")) {
                    LOG.debug("→ Removing problematic attribute from col: {}={}", key, value);
                    col.removeAttr(key);
                }
            });
        });

        // Handle colgroup elements
        jsoupDoc.select("colgroup").forEach(colgroup -> {
            // If colgroup is empty or contains only whitespace, remove it
            if (colgroup.children().isEmpty() && colgroup.text().trim().isEmpty()) {
                LOG.debug("→ Removing empty colgroup");
                colgroup.remove();
            } else {
                // Ensure colgroup only contains col elements
                colgroup.children().forEach(child -> {
                    if (!"col".equals(child.tagName())) {
                        LOG.debug("→ Removing non-col element from colgroup: {}", child.tagName());
                        child.remove();
                    }
                });
            }
        });

        // Handle malformed or nested table structures
        repairMalformedTables(jsoupDoc);

        // Remove any orphaned table elements that might cause issues
        jsoupDoc.select("thead, tbody, tfoot").forEach(section -> {
            if (section.parent() == null || !"table".equals(section.parent().tagName())) {
                LOG.debug("→ Removing orphaned table section: {}", section.tagName());
                section.remove();
            }
        });

        // Clean up table rows and cells
        jsoupDoc.select("tr").forEach(row -> {
            if (row.parent() == null ||
                (!row.parent().tagName().matches("table|thead|tbody|tfoot"))) {
                LOG.debug("→ Removing orphaned table row");
                row.remove();
            }
        });

        jsoupDoc.select("td, th").forEach(cell -> {
            if (cell.parent() == null || !"tr".equals(cell.parent().tagName())) {
                LOG.debug("→ Removing orphaned table cell: {}", cell.tagName());
                cell.remove();
            }
        });

        LOG.debug("→ Table structures sanitized");
    }

    /**
     * Repairs malformed table structures that could cause XML parsing issues
     */
    private void repairMalformedTables(Document jsoupDoc) {
        LOG.debug("→ Repairing malformed table structures");

        // Find and fix nested tables that might be causing issues
        jsoupDoc.select("table").forEach(table -> {
            LOG.debug("→ Processing table: {}", table.outerHtml().substring(0, Math.min(100, table.outerHtml().length())));

            // Check for nested tables and flatten them if problematic
            table.select("table").forEach(nestedTable -> {
                if (nestedTable != table) { // Don't process the same table
                    LOG.debug("→ Found nested table, checking if it's properly contained");

                    // If nested table is not properly contained in a td/th, move it out
                    org.jsoup.nodes.Element parent = nestedTable.parent();
                    boolean properlyNested = false;
                    while (parent != null && parent != table) {
                        if ("td".equals(parent.tagName()) || "th".equals(parent.tagName())) {
                            properlyNested = true;
                            break;
                        }
                        parent = parent.parent();
                    }

                    if (!properlyNested) {
                        LOG.debug("→ Moving improperly nested table outside parent table");
                        table.after(nestedTable);
                    }
                }
            });

            // Ensure table has proper structure - wrap loose content in tbody if needed
            boolean hasTableSections = !table.select("thead, tbody, tfoot").isEmpty();
            if (!hasTableSections) {
                // Check if there are direct tr children
                org.jsoup.select.Elements directTrs = table.children().select("tr");
                if (!directTrs.isEmpty()) {
                    LOG.debug("→ Wrapping loose tr elements in tbody");
                    org.jsoup.nodes.Element tbody = new org.jsoup.nodes.Element("tbody");
                    directTrs.forEach(tr -> tbody.appendChild(tr.clone()));
                    directTrs.remove();
                    table.appendChild(tbody);
                }
            }

            // Remove any text nodes directly under table (should be in cells)
            table.textNodes().forEach(textNode -> {
                if (!textNode.text().trim().isEmpty()) {
                    LOG.debug("→ Removing text node directly under table: {}", textNode.text().trim());
                    textNode.remove();
                }
            });
        });

        LOG.debug("→ Malformed table structures repaired");
    }

    /**
     * Converts problematic tables to div-based layouts to avoid XML parsing issues
     * This is a fallback for tables that can't be properly sanitized
     */
    private void convertProblematicTablesToDivs(Document jsoupDoc) {
        LOG.debug("→ Converting problematic tables to div-based layouts");

        // Get all tables and convert them to a list to avoid concurrent modification
        java.util.List<org.jsoup.nodes.Element> tables = new java.util.ArrayList<>(jsoupDoc.select("table"));

        for (org.jsoup.nodes.Element table : tables) {
            try {
                // Test if this table would cause XML parsing issues
                String tableHtml = table.outerHtml();

                // Check for specific problematic patterns that require conversion
                boolean hasProblems =
                    // Check for malformed col tags
                    (tableHtml.contains("<col") && !tableHtml.matches(".*<col[^>]*/>.*")) ||
                    // Check for incomplete table tags
                    tableHtml.matches(".*<table[^>]*(?<!>)\\s*(?=<|$).*") ||
                    tableHtml.matches(".*<(thead|tbody|tfoot|tr|td|th)[^>]*(?<!>)\\s*(?=<|$).*") ||
                    // Check for namespace attributes that cause XML binding issues
                    (tableHtml.contains("ac:") || tableHtml.contains("ri:") || tableHtml.contains("at:")) ||
                    // Check for deeply nested tables (more than 2 levels)
                    (tableHtml.split("<table").length > 3);

                if (hasProblems) {
                    LOG.debug("→ Converting problematic table to div layout (detected issues)");
                    convertTableToDiv(table);
                } else {
                    // Try to parse the table HTML as XML to verify it's valid
                    try {
                        javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
                        factory.setNamespaceAware(true);
                        factory.setValidating(false);
                        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
                        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
                        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

                        javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
                        String testXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root xmlns:xhtml=\"http://www.w3.org/1999/xhtml\">" + tableHtml + "</root>";
                        builder.parse(new java.io.ByteArrayInputStream(testXml.getBytes("UTF-8")));
                        LOG.debug("→ Table passed XML validation");
                    } catch (Exception xmlException) {
                        LOG.warn("→ Table failed XML validation, converting to div: {}", xmlException.getMessage());
                        convertTableToDiv(table);
                    }
                }
            } catch (Exception e) {
                LOG.warn("→ Error processing table, converting to div: {}", e.getMessage());
                convertTableToDiv(table);
            }
        }

        LOG.debug("→ Problematic tables converted to div layouts");
    }

    /**
     * Converts a table element to a proper HTML table or simple text layout
     */
    private void convertTableToDiv(org.jsoup.nodes.Element table) {
        LOG.debug("→ Converting problematic table to simple format");

        try {
            // First, try to create a simplified but proper HTML table
            org.jsoup.nodes.Element newTable = createSimplifiedTable(table);
            if (newTable != null) {
                table.replaceWith(newTable);
                LOG.debug("→ Table converted to simplified HTML table successfully");
                return;
            }
        } catch (Exception e) {
            LOG.warn("→ Failed to create simplified table, falling back to text layout: {}", e.getMessage());
        }

        // Fallback: Convert to simple text layout with clear structure
        org.jsoup.nodes.Element textDiv = new org.jsoup.nodes.Element("div");
        textDiv.addClass("table-as-text");

        // Extract table content as structured text
        java.util.List<java.util.List<String>> tableData = extractTableData(table);

        if (!tableData.isEmpty()) {
            // Create a simple text representation
            for (int i = 0; i < tableData.size(); i++) {
                java.util.List<String> row = tableData.get(i);
                if (!row.isEmpty()) {
                    org.jsoup.nodes.Element rowP = new org.jsoup.nodes.Element("p");

                    // Add row indicator for clarity
                    if (i == 0) {
                        rowP.addClass("table-header-text");
                        rowP.html("<strong>Header:</strong> " + String.join(" | ", row));
                    } else {
                        rowP.addClass("table-row-text");
                        rowP.html("<strong>Row " + i + ":</strong> " + String.join(" | ", row));
                    }

                    textDiv.appendChild(rowP);
                }
            }
        } else {
            // If no data could be extracted, just preserve any text content
            String tableText = table.text().trim();
            if (!tableText.isEmpty()) {
                org.jsoup.nodes.Element fallbackP = new org.jsoup.nodes.Element("p");
                fallbackP.text("Table content: " + tableText);
                textDiv.appendChild(fallbackP);
            }
        }

        // Replace the table with the text structure
        table.replaceWith(textDiv);
        LOG.debug("→ Table converted to text layout successfully");
    }

    /**
     * Creates a simplified HTML table from a problematic table
     */
    private org.jsoup.nodes.Element createSimplifiedTable(org.jsoup.nodes.Element originalTable) {
        try {
            org.jsoup.nodes.Element newTable = new org.jsoup.nodes.Element("table");

            // Extract table data
            java.util.List<java.util.List<String>> tableData = extractTableData(originalTable);

            if (tableData.isEmpty()) {
                return null;
            }

            // Create table body
            org.jsoup.nodes.Element tbody = new org.jsoup.nodes.Element("tbody");

            for (int i = 0; i < tableData.size(); i++) {
                java.util.List<String> rowData = tableData.get(i);
                if (!rowData.isEmpty()) {
                    org.jsoup.nodes.Element tr = new org.jsoup.nodes.Element("tr");

                    for (String cellData : rowData) {
                        org.jsoup.nodes.Element cell;
                        if (i == 0) {
                            // First row as header
                            cell = new org.jsoup.nodes.Element("th");
                        } else {
                            cell = new org.jsoup.nodes.Element("td");
                        }

                        // Clean the cell data
                        String cleanCellData = cleanupTextString(cellData);
                        cell.text(cleanCellData);
                        tr.appendChild(cell);
                    }

                    tbody.appendChild(tr);
                }
            }

            newTable.appendChild(tbody);
            return newTable;

        } catch (Exception e) {
            LOG.warn("→ Failed to create simplified table: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extracts table data as a list of rows, where each row is a list of cell contents
     */
    private java.util.List<java.util.List<String>> extractTableData(org.jsoup.nodes.Element table) {
        java.util.List<java.util.List<String>> tableData = new java.util.ArrayList<>();

        try {
            // Get all rows from the table
            org.jsoup.select.Elements rows = table.select("tr");

            for (org.jsoup.nodes.Element row : rows) {
                java.util.List<String> rowData = new java.util.ArrayList<>();

                // Get all cells in this row
                org.jsoup.select.Elements cells = row.select("td, th");

                for (org.jsoup.nodes.Element cell : cells) {
                    String cellText = cell.text().trim();
                    if (cellText.isEmpty()) {
                        cellText = " "; // Preserve empty cells with a space
                    }
                    rowData.add(cellText);
                }

                if (!rowData.isEmpty()) {
                    tableData.add(rowData);
                }
            }

        } catch (Exception e) {
            LOG.warn("→ Error extracting table data: {}", e.getMessage());
        }

        return tableData;
    }

    /**
     * Converts Confluence-specific elements to HTML equivalents while preserving rich formatting
     * Instead of removing elements, we convert them to standard HTML that docx4j can handle
     */
    private void removeConfluenceSpecificElements(Document jsoupDoc) {
        LOG.debug("→ Converting Confluence-specific elements to HTML equivalents");

        // Convert Confluence layout elements to HTML equivalents
        convertConfluenceLayouts(jsoupDoc);

        // Convert Confluence macros to HTML equivalents
        convertConfluenceMacros(jsoupDoc);

        // Convert Confluence placeholders
        convertConfluencePlaceholders(jsoupDoc);

        // Remove remaining problematic namespace elements that can't be converted
        removeRemainingNamespaceElements(jsoupDoc);

        LOG.debug("→ Confluence-specific elements converted to HTML");
    }

    /**
     * Converts Confluence layout elements to HTML table/div structures
     */
    private void convertConfluenceLayouts(Document jsoupDoc) {
        // Convert ac:layout to div with class
        jsoupDoc.select("ac\\:layout").forEach(layout -> {
            layout.tagName("div");
            layout.addClass("confluence-layout");
        });

        // Convert ac:layout-section to div
        jsoupDoc.select("ac\\:layout-section").forEach(section -> {
            section.tagName("div");
            section.addClass("confluence-section");

            // Preserve section type information
            String type = section.attr("ac:type");
            if (!type.isEmpty()) {
                section.addClass("section-" + type.replaceAll("[^a-zA-Z0-9-]", "-"));
            }
        });

        // Convert ac:layout-cell to div
        jsoupDoc.select("ac\\:layout-cell").forEach(cell -> {
            cell.tagName("div");
            cell.addClass("confluence-cell");
        });
    }

    /**
     * Converts Confluence macros to HTML equivalents
     */
    private void convertConfluenceMacros(Document jsoupDoc) {
        // Convert ac:structured-macro to div with descriptive content
        jsoupDoc.select("ac\\:structured-macro").forEach(macro -> {
            String macroName = macro.attr("ac:name");

            // Create a div to replace the macro
            org.jsoup.nodes.Element div = new org.jsoup.nodes.Element("div");
            div.addClass("confluence-macro");
            div.addClass("macro-" + macroName.replaceAll("[^a-zA-Z0-9-]", "-"));

            // Add macro description
            switch (macroName) {
                case "recently-updated":
                    div.append("<p><strong>[Recently Updated Pages]</strong></p>");
                    break;
                case "blog-posts":
                    div.append("<p><strong>[Recent Blog Posts]</strong></p>");
                    break;
                case "info":
                    div.addClass("info-macro");
                    // Preserve the rich text body content
                    org.jsoup.select.Elements richTextBody = macro.select("ac\\:rich-text-body");
                    if (!richTextBody.isEmpty()) {
                        div.append(richTextBody.html());
                    }
                    break;
                default:
                    div.append("<p><strong>[" + macroName + " macro]</strong></p>");
                    break;
            }

            // Preserve any parameters as data attributes or text
            macro.select("ac\\:parameter").forEach(param -> {
                String paramName = param.attr("ac:name");
                String paramValue = param.text();
                if (!paramValue.isEmpty()) {
                    div.append("<small>(" + paramName + ": " + paramValue + ")</small>");
                }
            });

            macro.replaceWith(div);
        });

        // Convert ac:rich-text-body to div (preserve content)
        jsoupDoc.select("ac\\:rich-text-body").forEach(richText -> {
            richText.tagName("div");
            richText.addClass("rich-text-content");
        });

        // Convert ac:parameter elements (remove but preserve text if any)
        jsoupDoc.select("ac\\:parameter").forEach(param -> {
            if (param.hasText()) {
                param.replaceWith(new org.jsoup.nodes.TextNode(param.text()));
            } else {
                param.remove();
            }
        });
    }

    /**
     * Converts Confluence placeholders to HTML equivalents
     */
    private void convertConfluencePlaceholders(Document jsoupDoc) {
        jsoupDoc.select("ac\\:placeholder").forEach(placeholder -> {
            String placeholderText = placeholder.text();
            org.jsoup.nodes.Element span = new org.jsoup.nodes.Element("span");
            span.addClass("confluence-placeholder");
            span.attr("style", "color: #999; font-style: italic;");
            span.text("[" + placeholderText + "]");
            placeholder.replaceWith(span);
        });
    }

    /**
     * Removes remaining namespace elements that can't be meaningfully converted
     */
    private void removeRemainingNamespaceElements(Document jsoupDoc) {
        // Remove remaining ri: elements (attachments, links, etc.)
        jsoupDoc.select("ri\\:attachment, ri\\:page, ri\\:space, ri\\:user").remove();

        // Remove at: elements (template variables)
        jsoupDoc.select("at\\:var, at\\:declarations").remove();

        // Remove any remaining ac: elements that weren't converted above
        jsoupDoc.select("*").forEach(element -> {
            String tagName = element.tagName().toLowerCase();
            if (tagName.startsWith("ac:") || tagName.startsWith("ri:") || tagName.startsWith("at:")) {
                LOG.debug("→ Removing unconverted namespace element: {}", tagName);
                if (element.hasText()) {
                    element.replaceWith(new org.jsoup.nodes.TextNode(element.text()));
                } else {
                    element.remove();
                }
            }
        });
    }

    /**
     * Sanitizes HTML attributes that cause XML parsing issues and removes formatting artifacts
     */
    private void sanitizeHtmlAttributes(Document jsoupDoc) {
        LOG.debug("→ Sanitizing HTML attributes and removing formatting artifacts");

        // Remove problematic attributes
        jsoupDoc.select("*").forEach(element -> {
            // Remove JavaScript and event handlers
            element.removeAttr("onclick");
            element.removeAttr("onload");
            element.removeAttr("onerror");

            // Create a list of attributes to remove to avoid concurrent modification
            java.util.List<String> attributesToRemove = new java.util.ArrayList<>();

            element.attributes().forEach(attr -> {
                String key = attr.getKey();
                String value = attr.getValue();

                // Remove attributes with namespace prefixes that aren't bound
                if (key.startsWith("ac:") || key.startsWith("ri:") || key.startsWith("at:")) {
                    LOG.debug("→ Removing namespace attribute: {}={}", key, value);
                    attributesToRemove.add(key);
                }
                // Remove styling attributes that cause formatting artifacts
                else if (key.equals("style") || key.equals("class") || key.equals("id") ||
                         key.startsWith("data-") || key.equals("contenteditable") ||
                         key.equals("spellcheck") || key.equals("data-highlight-colour") ||
                         key.equals("data-highlight-color")) {
                    LOG.debug("→ Removing formatting attribute: {}={}", key, value);
                    attributesToRemove.add(key);
                }
                // Remove any attribute containing JavaScript
                else if (value != null && value.toLowerCase().contains("javascript:")) {
                    LOG.debug("→ Removing JavaScript attribute: {}={}", key, value);
                    attributesToRemove.add(key);
                }
                // Remove attributes with problematic characters
                else if (value != null && (value.contains("<") || value.contains(">") ||
                        (value.contains("&") && !value.matches(".*&(amp|lt|gt|quot|apos);.*")))) {
                    LOG.debug("→ Removing attribute with problematic characters: {}={}", key, value);
                    attributesToRemove.add(key);
                }
                // Remove empty or whitespace-only attributes
                else if (value == null || value.trim().isEmpty()) {
                    attributesToRemove.add(key);
                }
            });

            // Remove all the problematic attributes
            attributesToRemove.forEach(element::removeAttr);
        });

        LOG.debug("→ HTML attributes sanitized and formatting artifacts removed");
    }
    
    /**
     * Simplifies HTML tags to versions that work better with docx4j
     */
    private void simplifyHtmlTags(Document jsoupDoc) {
        // Convert complex formatting to simple tags
        jsoupDoc.select("strong").tagName("b");
        jsoupDoc.select("em").tagName("i");
        jsoupDoc.select("br").forEach(element -> element.tagName("br"));
    }
    
    /**
     * Removes complex styling that XHTMLImporter can't handle
     */
    private void removeComplexStyling(Document jsoupDoc) {
        // Remove complex span styling
        jsoupDoc.select("span[style]").forEach(element -> element.unwrap());
        jsoupDoc.select("span").forEach(element -> element.unwrap());

        // Remove other complex styling
        jsoupDoc.select("*").forEach(element -> {
            element.removeAttr("class");
            element.removeAttr("id");
            if (element.hasAttr("style")) {
                String style = element.attr("style");
                // Keep only basic styling
                if (!style.contains("font-weight") && !style.contains("font-style")) {
                    element.removeAttr("style");
                }
            }
        });
    }

    /**
     * Fixes self-closing HTML tags to be XHTML compliant
     * This addresses the specific issue with <col> tags that need proper closing delimiters
     */
    private void fixSelfClosingTags(Document jsoupDoc) {
        LOG.debug("→ Fixing self-closing HTML tags for XHTML compliance");

        // Handle table-related self-closing tags that commonly cause issues
        jsoupDoc.select("col").forEach(element -> {
            LOG.debug("→ Processing col tag: {}", element.outerHtml());
            // Ensure col tags are properly self-closed
            if (!element.html().isEmpty()) {
                // If it has content, it shouldn't - col tags should be self-closing
                LOG.warn("→ Col tag has content, clearing it: {}", element.html());
                element.html("");
            }

            // Clean up any problematic attributes
            cleanupElementAttributes(element);
        });

        // Handle other common self-closing tags - be more aggressive
        jsoupDoc.select("br, hr, img, input, meta, link, area, base, embed, source, track, wbr").forEach(element -> {
            LOG.debug("→ Processing self-closing tag: {}", element.tagName());

            // Clear any content from self-closing tags
            if (!element.html().isEmpty()) {
                LOG.debug("→ Clearing content from self-closing tag: {}", element.tagName());
                element.html("");
            }

            // Clean up any problematic attributes
            cleanupElementAttributes(element);
        });

        // Remove any malformed table elements that might cause issues
        jsoupDoc.select("colgroup").forEach(colgroup -> {
            if (colgroup.children().isEmpty()) {
                LOG.debug("→ Removing empty colgroup element");
                colgroup.remove();
            } else {
                // Check if colgroup contains only col elements
                boolean hasOnlyColElements = colgroup.children().stream()
                    .allMatch(child -> "col".equals(child.tagName()));
                if (!hasOnlyColElements) {
                    LOG.warn("→ Colgroup contains non-col elements, removing it");
                    colgroup.remove();
                }
            }
        });

        // Remove any problematic table structures that might cause XML parsing issues
        jsoupDoc.select("table").forEach(table -> {
            // Remove any text nodes directly under table (should be in td/th)
            table.textNodes().forEach(textNode -> {
                if (!textNode.text().trim().isEmpty()) {
                    LOG.debug("→ Removing text node directly under table: {}", textNode.text().trim());
                    textNode.remove();
                }
            });

            // Clean up table attributes
            cleanupElementAttributes(table);
        });

        LOG.debug("→ Self-closing tags fixed for XHTML compliance");
    }

    /**
     * Cleans up problematic attributes from an element
     */
    private void cleanupElementAttributes(org.jsoup.nodes.Element element) {
        // Create a list of attributes to remove to avoid concurrent modification
        java.util.List<String> attributesToRemove = new java.util.ArrayList<>();

        element.attributes().forEach(attr -> {
            String key = attr.getKey();
            String value = attr.getValue();

            // Remove namespace attributes
            if (key.startsWith("ac:") || key.startsWith("ri:") || key.startsWith("at:")) {
                attributesToRemove.add(key);
            }
            // Remove attributes with problematic values
            else if (value != null && (value.contains("<") || value.contains(">") ||
                    (value.contains("&") && !value.matches(".*&(amp|lt|gt|quot|apos);.*")))) {
                attributesToRemove.add(key);
            }
            // Remove empty or whitespace-only attributes
            else if (value == null || value.trim().isEmpty()) {
                attributesToRemove.add(key);
            }
        });

        // Remove the problematic attributes
        attributesToRemove.forEach(attr -> {
            LOG.debug("→ Removing problematic attribute: {}={}", attr, element.attr(attr));
            element.removeAttr(attr);
        });
    }
    
    /**
     * Converts HTML entities to Unicode characters and cleans up formatting artifacts
     */
    private String convertHtmlEntitiesToUnicode(String cleanedHtml) {
        LOG.debug("→ Converting HTML entities to Unicode and cleaning formatting artifacts");

        String result = cleanedHtml
                // Convert common HTML entities to proper characters
                .replaceAll("&nbsp;", " ")           // Non-breaking space to regular space
                .replaceAll("&mdash;", "\u2014")     // Em dash
                .replaceAll("&ndash;", "\u2013")     // En dash
                .replaceAll("&ldquo;", "\u201C")     // Left double quote
                .replaceAll("&rdquo;", "\u201D")     // Right double quote
                .replaceAll("&lsquo;", "\u2018")     // Left single quote
                .replaceAll("&rsquo;", "\u2019")     // Right single quote
                .replaceAll("&hellip;", "\u2026")    // Ellipsis
                .replaceAll("&copy;", "\u00A9")      // Copyright
                .replaceAll("&reg;", "\u00AE")       // Registered trademark
                .replaceAll("&trade;", "\u2122")     // Trademark
                .replaceAll("&bull;", "\u2022")      // Bullet
                .replaceAll("&middot;", "\u00B7")    // Middle dot
                .replaceAll("&sect;", "\u00A7")      // Section sign
                .replaceAll("&para;", "\u00B6")      // Paragraph sign
                .replaceAll("&dagger;", "\u2020")    // Dagger
                .replaceAll("&Dagger;", "\u2021")    // Double dagger
                .replaceAll("&permil;", "\u2030")    // Per mille
                .replaceAll("&lsaquo;", "\u2039")    // Left single angle quote
                .replaceAll("&rsaquo;", "\u203A")    // Right single angle quote
                .replaceAll("&euro;", "\u20AC")      // Euro sign
                // Remove any remaining HTML entities that we don't recognize (but preserve XML entities)
                .replaceAll("&(?!amp;|lt;|gt;|quot;|apos;)[a-zA-Z][a-zA-Z0-9]*;", "")
                // Clean up multiple consecutive spaces
                .replaceAll("\\s{2,}", " ")
                // Remove any remaining data attributes that might have leaked through
                .replaceAll("data-[^\\s>=]*=[\"'][^\"']*[\"']", "")
                .replaceAll("data-[^\\s>=]*=[^\\s>]*", "")
                // Clean up any remaining style fragments
                .replaceAll("style=[\"'][^\"']*[\"']", "")
                .replaceAll("class=[\"'][^\"']*[\"']", "")
                .replaceAll("id=[\"'][^\"']*[\"']", "");

        LOG.debug("→ HTML entities converted and formatting artifacts cleaned");
        return result.trim();
    }

    /**
     * Cleans up text content to remove formatting artifacts and normalize whitespace
     */
    private void cleanupTextContent(Document jsoupDoc) {
        LOG.debug("→ Cleaning up text content and removing formatting artifacts");

        // Process all text nodes to clean up formatting artifacts
        jsoupDoc.select("*").forEach(element -> {
            // Clean up the text content of each element
            if (element.hasText()) {
                String originalText = element.ownText();
                if (!originalText.trim().isEmpty()) {
                    String cleanedText = cleanupTextString(originalText);
                    if (!cleanedText.equals(originalText)) {
                        LOG.debug("→ Cleaned text: '{}' → '{}'", originalText, cleanedText);
                        // Replace the text node content
                        element.textNodes().forEach(textNode -> {
                            String nodeText = textNode.text();
                            if (nodeText.equals(originalText)) {
                                textNode.text(cleanedText);
                            }
                        });
                    }
                }
            }
        });

        // Also clean up any remaining text nodes that might contain artifacts
        jsoupDoc.textNodes().forEach(textNode -> {
            String originalText = textNode.text();
            String cleanedText = cleanupTextString(originalText);
            if (!cleanedText.equals(originalText)) {
                textNode.text(cleanedText);
            }
        });

        LOG.debug("→ Text content cleanup completed");
    }

    /**
     * Cleans up a text string by removing formatting artifacts and normalizing whitespace
     */
    private String cleanupTextString(String text) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }

        return text
                // Convert HTML entities to proper characters
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&apos;", "'")
                // Remove any remaining HTML-like artifacts
                .replaceAll("data-highlight-colou?r=[\"'][^\"']*[\"']", "")
                .replaceAll("data-[^\\s>=]*=[\"'][^\"']*[\"']", "")
                .replaceAll("style=[\"'][^\"']*[\"']", "")
                .replaceAll("class=[\"'][^\"']*[\"']", "")
                // Normalize whitespace
                .replaceAll("\\s{2,}", " ")
                .trim();
    }

    /**
     * Generates the final XHTML structure with proper XML declaration and namespace
     */
    private String generateXhtmlStructure(String cleanedHtml) {
        // Additional cleanup to remove any remaining namespace prefixes that might have been missed
        String finalCleanedHtml = cleanedHtml
                .replaceAll("</?ac:[^>]*>", "") // Remove any remaining ac: tags
                .replaceAll("</?ri:[^>]*>", "") // Remove any remaining ri: tags
                .replaceAll("</?at:[^>]*>", "") // Remove any remaining at: tags
                .replaceAll("ac:[^\\s>]*", "")  // Remove ac: attributes
                .replaceAll("ri:[^\\s>]*", "")  // Remove ri: attributes
                .replaceAll("at:[^\\s>]*", "")  // Remove at: attributes
                // Fix malformed self-closing tags that are missing closing brackets or proper syntax
                .replaceAll("<(col|br|hr|img|input|meta|link|area|base|embed|source|track|wbr)([^/>]*?)(?<!/)(?<!>)(?=\\s|$|<)", "<$1$2 />") // Fix incomplete self-closing tags
                .replaceAll("<(col|br|hr|img|input|meta|link|area|base|embed|source|track|wbr)([^/>]*?)(?<!/)\\s*(?=[<\\s])", "<$1$2 />") // Fix incomplete tags before other content
                .replaceAll("<(col|br|hr|img|input|meta|link|area|base|embed|source|track|wbr)\\s*([^>]*?)\\s*>", "<$1 $2 />") // Convert to proper XHTML self-closing
                .replaceAll("<(col|br|hr|img|input|meta|link|area|base|embed|source|track|wbr)\\s*([^>]*?)\\s*/>", "<$1 $2 />") // Normalize spacing
                .replaceAll("</(col|br|hr|img|input|meta|link|area|base|embed|source|track|wbr)>", "") // Remove any incorrect closing tags for self-closing elements
                // Fix malformed table structures
                .replaceAll("<table([^>]*)(?<!>)(?=\\s|$|<)", "<table$1>") // Fix incomplete table opening tags
                .replaceAll("</table(?!>)", "</table>") // Fix incomplete table closing tags
                .replaceAll("<(thead|tbody|tfoot|tr|td|th)([^>]*)(?<!>)(?=\\s|$|<)", "<$1$2>") // Fix incomplete table element tags
                // Remove any remaining Confluence namespace attributes that might have been missed
                .replaceAll("\\s+(ac|ri|at):[^\\s>=]*=[^\\s>]*", "") // Remove namespace attributes
                .replaceAll("\\s+(ac|ri|at):[^\\s>=]*", "") // Remove namespace attributes without values
                // Additional safety: remove any remaining malformed tags entirely if they can't be fixed
                .replaceAll("<(col|br|hr|img|input|meta|link|area|base|embed|source|track|wbr)[^>]*(?<!/)(?<!>)(?=\\s|$|<)", "") // Remove incomplete self-closing tags
                .replaceAll("<table[^>]*(?<!>)(?=\\s|$|<)", "") // Remove incomplete table tags
                .replaceAll("\\s+", " ") // Normalize whitespace
                .trim();

        LOG.debug("→ Final cleaned HTML after namespace and self-closing tag fixes, length: {}", finalCleanedHtml.length());

        // Create CSS for better formatting of converted Confluence elements
        String css = "<style type=\"text/css\">\n" +
                ".confluence-layout { margin: 10px 0; }\n" +
                ".confluence-section { margin: 5px 0; }\n" +
                ".section-two_equal .confluence-cell { display: inline-block; width: 48%; vertical-align: top; margin-right: 2%; }\n" +
                ".section-fixed-width { width: 100%; }\n" +
                ".confluence-cell { padding: 5px; }\n" +
                ".confluence-macro { margin: 10px 0; padding: 10px; border-left: 3px solid #ccc; background-color: #f9f9f9; }\n" +
                ".info-macro { border-left-color: #0052cc; background-color: #e6f2ff; }\n" +
                ".confluence-placeholder { color: #999; font-style: italic; }\n" +
                "table { border-collapse: collapse; width: 100%; margin: 10px 0; }\n" +
                "th, td { border: 1px solid #ddd; padding: 8px; text-align: left; vertical-align: top; }\n" +
                "th { background-color: #f5f5f5; font-weight: bold; }\n" +
                ".table-as-text { margin: 10px 0; padding: 10px; border: 1px solid #ccc; background-color: #f9f9f9; }\n" +
                ".table-header-text { font-weight: bold; margin: 5px 0; }\n" +
                ".table-row-text { margin: 3px 0; }\n" +
                "h1, h2, h3, h4, h5, h6 { color: #333; margin: 15px 0 10px 0; }\n" +
                "p { margin: 8px 0; line-height: 1.4; }\n" +
                "</style>\n";

        // Wrap in proper XHTML structure with CSS styling
        String finalXhtml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
                "<head>\n" +
                "<title>Confluence Page</title>\n" +
                css +
                "</head>\n" +
                "<body>" + finalCleanedHtml + "</body>\n" +
                "</html>";

        LOG.debug("→ Generated final XHTML structure, total length: {}", finalXhtml.length());
        return finalXhtml;
    }
    
    /**
     * Validates the generated XHTML content
     */
    private void validateXhtmlContent(String xhtmlContent) {
        try {
            // Basic XML validation
            javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setValidating(false);

            javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
            builder.parse(new java.io.ByteArrayInputStream(xhtmlContent.getBytes("UTF-8")));
            LOG.debug("→ XHTML content passed XML validation");
        } catch (org.xml.sax.SAXParseException saxException) {
            LOG.warn("→ XHTML content failed XML validation at line {}, column {}: {}",
                    saxException.getLineNumber(), saxException.getColumnNumber(), saxException.getMessage());
            // Log a snippet around the error location for debugging
            if (saxException.getLineNumber() > 0) {
                String[] lines = xhtmlContent.split("\n");
                int errorLine = saxException.getLineNumber() - 1; // Convert to 0-based index
                int startLine = Math.max(0, errorLine - 2);
                int endLine = Math.min(lines.length - 1, errorLine + 2);

                LOG.warn("→ XHTML content around error (lines {}-{}):", startLine + 1, endLine + 1);
                for (int i = startLine; i <= endLine; i++) {
                    String marker = (i == errorLine) ? " >>> " : "     ";
                    LOG.warn("{}Line {}: {}", marker, i + 1, lines[i]);
                }
            }
            // Don't throw here - let docx4j handle it with its own error handling
        } catch (Exception xmlException) {
            LOG.warn("→ XHTML content failed XML validation: {}", xmlException.getMessage());
            // Don't throw here - let docx4j handle it with its own error handling
        }
    }
    
    /**
     * Creates fallback XHTML when conversion fails
     */
    private String createFallbackXhtml(String htmlContent) {
        LOG.warn("→ Creating fallback XHTML due to conversion error");

        // Try to preserve as much content as possible in fallback mode
        String safeContent = "";
        if (htmlContent != null) {
            try {
                // First try to parse with JSoup and do basic conversion
                Document fallbackDoc = Jsoup.parse(htmlContent);

                // Configure JSoup to output XHTML syntax for proper XML parsing
                fallbackDoc.outputSettings().syntax(Document.OutputSettings.Syntax.xml);

                // Sanitize table structures first
                sanitizeTableStructures(fallbackDoc);

                // Convert problematic tables to divs in fallback mode too
                convertProblematicTablesToDivs(fallbackDoc);

                // Do basic Confluence element conversion for fallback
                fallbackDoc.select("ac\\:layout, ac\\:layout-section, ac\\:layout-cell").forEach(element -> {
                    element.tagName("div");
                });

                fallbackDoc.select("ac\\:structured-macro").forEach(macro -> {
                    String macroName = macro.attr("ac:name");
                    macro.tagName("div");
                    macro.text("[" + macroName + " macro]");
                });

                fallbackDoc.select("ac\\:placeholder").forEach(placeholder -> {
                    placeholder.tagName("span");
                    placeholder.text("[" + placeholder.text() + "]");
                });

                // Remove remaining namespace elements
                fallbackDoc.select("*").forEach(element -> {
                    String tagName = element.tagName().toLowerCase();
                    if (tagName.startsWith("ac:") || tagName.startsWith("ri:") || tagName.startsWith("at:")) {
                        if (element.hasText()) {
                            element.replaceWith(new org.jsoup.nodes.TextNode(element.text()));
                        } else {
                            element.remove();
                        }
                    }
                });

                // Fix self-closing tags in fallback mode too
                fixSelfClosingTags(fallbackDoc);

                safeContent = fallbackDoc.body().html();

            } catch (Exception e) {
                LOG.warn("→ JSoup fallback parsing failed, using text-only fallback: {}", e.getMessage());
                // Last resort: just extract text and escape it
                safeContent = htmlContent
                        .replaceAll("<[^>]*>", " ") // Remove all HTML tags
                        .replaceAll("\\s+", " ")    // Normalize whitespace
                        .trim()
                        .replaceAll("&", "&amp;")
                        .replaceAll("<", "&lt;")
                        .replaceAll(">", "&gt;");
            }
        }

        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
                "<head><title>Confluence Page</title></head>\n" +
                "<body>" + safeContent + "</body>\n" +
                "</html>";
    }

    /**
     * Creates an aggressive fallback XHTML when normal conversion fails
     * This removes all tables and complex structures to ensure XML validity
     */
    private String createAggressiveFallbackXhtml(String htmlContent) {
        LOG.warn("→ Creating aggressive fallback XHTML due to persistent conversion errors");

        String safeContent = "";
        if (htmlContent != null) {
            try {
                // Parse with JSoup but be very aggressive about removing problematic content
                Document fallbackDoc = Jsoup.parse(htmlContent);
                fallbackDoc.outputSettings().syntax(Document.OutputSettings.Syntax.xml);

                // Convert tables to simple text format in aggressive fallback
                LOG.debug("→ Converting all table elements to text format in aggressive fallback");
                fallbackDoc.select("table").forEach(table -> {
                    // Extract table data and convert to readable text
                    java.util.List<java.util.List<String>> tableData = extractTableData(table);

                    org.jsoup.nodes.Element tableDiv = new org.jsoup.nodes.Element("div");
                    tableDiv.addClass("table-as-text");

                    if (!tableData.isEmpty()) {
                        for (int i = 0; i < tableData.size(); i++) {
                            java.util.List<String> row = tableData.get(i);
                            if (!row.isEmpty()) {
                                org.jsoup.nodes.Element rowP = new org.jsoup.nodes.Element("p");

                                if (i == 0) {
                                    rowP.html("<strong>Header:</strong> " + String.join(" | ", row));
                                } else {
                                    rowP.html("<strong>Row " + i + ":</strong> " + String.join(" | ", row));
                                }

                                tableDiv.appendChild(rowP);
                            }
                        }
                    } else {
                        // Fallback: just extract all text from the table
                        String tableText = table.text().trim();
                        if (!tableText.isEmpty()) {
                            org.jsoup.nodes.Element textP = new org.jsoup.nodes.Element("p");
                            textP.text("Table content: " + tableText);
                            tableDiv.appendChild(textP);
                        }
                    }

                    table.replaceWith(tableDiv);
                });

                // Remove any remaining table-related elements
                fallbackDoc.select("thead, tbody, tfoot, tr, td, th, col, colgroup").remove();

                // Remove all Confluence-specific elements
                fallbackDoc.select("*").forEach(element -> {
                    String tagName = element.tagName().toLowerCase();
                    if (tagName.startsWith("ac:") || tagName.startsWith("ri:") || tagName.startsWith("at:")) {
                        String text = element.text().trim();
                        if (!text.isEmpty()) {
                            element.replaceWith(new org.jsoup.nodes.TextNode(text));
                        } else {
                            element.remove();
                        }
                    }
                });

                // Remove all attributes that might cause namespace issues
                fallbackDoc.select("*").forEach(element -> {
                    element.attributes().asList().forEach(attr -> {
                        String key = attr.getKey();
                        if (key.contains(":") || key.startsWith("ac") || key.startsWith("ri") || key.startsWith("at")) {
                            element.removeAttr(key);
                        }
                    });
                });

                // Keep only safe, simple HTML elements
                fallbackDoc.select("*").forEach(element -> {
                    String tagName = element.tagName().toLowerCase();
                    if (!tagName.matches("html|head|title|body|p|div|span|h1|h2|h3|h4|h5|h6|strong|b|em|i|u|br|hr")) {
                        // Convert complex elements to divs or paragraphs
                        if (element.hasText()) {
                            org.jsoup.nodes.Element replacement = new org.jsoup.nodes.Element("div");
                            replacement.html(element.html());
                            element.replaceWith(replacement);
                        } else {
                            element.remove();
                        }
                    }
                });

                // Clean up text content in the fallback as well
                cleanupTextContent(fallbackDoc);

                safeContent = fallbackDoc.body().html();

            } catch (Exception e) {
                LOG.warn("→ Aggressive JSoup fallback failed, using text-only approach: {}", e.getMessage());
                // Ultimate fallback: extract all text and wrap in paragraphs
                safeContent = htmlContent
                        .replaceAll("<[^>]*>", " ") // Remove all HTML tags
                        .replaceAll("\\s+", " ")    // Normalize whitespace
                        .trim()
                        .replaceAll("&", "&amp;")
                        .replaceAll("<", "&lt;")
                        .replaceAll(">", "&gt;");

                // Split into paragraphs for better readability
                String[] sentences = safeContent.split("\\. ");
                StringBuilder paragraphs = new StringBuilder();
                for (String sentence : sentences) {
                    if (!sentence.trim().isEmpty()) {
                        paragraphs.append("<p>").append(sentence.trim()).append(".</p>\n");
                    }
                }
                safeContent = paragraphs.toString();
            }
        }

        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
                "<head><title>Confluence Page (Simplified)</title></head>\n" +
                "<body>" + safeContent + "</body>\n" +
                "</html>";
    }
    
    /**
     * Sanitizes filename for safe file system usage
     */
    private String sanitizeFileName(String fileName) {
        if (fileName == null) {
            return "confluence_page_" + UUID.randomUUID();
        }
        
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_").substring(0, Math.min(fileName.length(), 100));
    }
    
    /**
     * Sanitizes text for XML usage
     */
    private String sanitizeForXml(String text) {
        if (text == null) {
            return "";
        }
        
        return text.replaceAll("&", "&amp;")
                   .replaceAll("<", "&lt;")
                   .replaceAll(">", "&gt;")
                   .replaceAll("\"", "&quot;")
                   .replaceAll("'", "&apos;");
    }
    
    /**
     * Creates an empty DOCX file when parsing fails
     */
    private File createEmptyDocxFile(String fileName) throws IOException {
        try {
            LOG.debug("→ Creating empty DOCX file: {}", fileName);

            // Create a minimal DOCX document with error message
            WordprocessingMLPackage wordPackage = WordprocessingMLPackage.createPackage();

            // Add a simple error message paragraph
            String errorMessage = "Error: The original page content could not be processed due to parsing issues. " +
                                 "Please check the original Confluence page for the content.";

            // Create XHTML content for the error message
            String errorXhtml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                               "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
                               "<head><title>Processing Error</title></head>\n" +
                               "<body><p>" + errorMessage + "</p></body>\n" +
                               "</html>";

            // Import the error content
            XHTMLImporterImpl xhtmlImporter = new XHTMLImporterImpl(wordPackage);
            wordPackage.getMainDocumentPart().getContent().addAll(xhtmlImporter.convert(errorXhtml, null));

            // Create temporary file and save
            java.io.File tempFile = java.io.File.createTempFile("empty_docx_", ".docx");
            wordPackage.save(tempFile);

            // Convert to Krista File object
            File result = convertToKristaFile(tempFile, fileName);
            LOG.debug("→ Successfully created empty DOCX file");

            return result;

        } catch (Exception e) {
            LOG.error("→ Failed to create empty DOCX file: {}", e.getMessage(), e);
            throw new IOException("Failed to create empty DOCX file: " + e.getMessage(), e);
        }
    }

    /**
     * Converts java.io.File to Krista File object using FileRepository
     * Inspired by Zendesk KristaMediaClient.toKristaFile()
     */
    private File convertToKristaFile(java.io.File tempFile, String fileName) throws IOException {
        try (final FileHandle fileHandle = fileRepository.createNewFileByName(fileName)) {
            fileHandle.setContent(new FileInputStream(tempFile));
            File result = fileHandle.getFile();

            // Clean up temporary file
            if (tempFile.exists()) {
                tempFile.delete();
            }

            return result;
        }
    }
    
    /**
     * Inner class to hold page content
     */
    private static class PageContent {
        private final String title;
        private final String body;
        
        public PageContent(String title, String body) {
            this.title = title;
            this.body = body;
        }
        
        public String getTitle() {
            return title;
        }
        
        public String getBody() {
            return body;
        }
    }
}
