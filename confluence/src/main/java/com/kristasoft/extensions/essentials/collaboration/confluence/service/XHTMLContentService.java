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

package com.kristasoft.extensions.essentials.collaboration.confluence.service;

import com.kristasoft.extensions.essentials.collaboration.confluence.ConfluenceAttributes;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.entities.Content;
import com.kristasoft.extensions.essentials.collaboration.confluence.exception.ConfluenceRESTException;
import org.apache.http.client.utils.URIBuilder;
import org.htmlunit.BrowserVersion;
import org.htmlunit.Page;
import org.htmlunit.WebClient;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.jvnet.hk2.annotations.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import javax.inject.Inject;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class XHTMLContentService implements ContentService {
    private ClassLoaderTemplateResolver templateResolver;
    private TemplateEngine templateEngine;
    private final ConfluenceAttributes confluenceAttributes;

    @Inject
    public XHTMLContentService(ConfluenceAttributes confluenceAttributes) {
        this.confluenceAttributes = confluenceAttributes;
    }

    public XHTMLContentService(String domainName, String accountEmail, String apiKey) {
        this.confluenceAttributes = new ConfluenceAttributes(domainName, accountEmail, apiKey);
    }

    private ClassLoaderTemplateResolver getTemplateResolver() {
        if (templateResolver == null) {
            templateResolver = new ClassLoaderTemplateResolver();
            templateResolver.setSuffix(".html");
            templateResolver.setTemplateMode(TemplateMode.HTML);
        }
        return templateResolver;
    }

    private TemplateEngine getTemplateEngine() {
        if (templateEngine == null) {
            templateEngine = new TemplateEngine();
            templateEngine.setTemplateResolver(getTemplateResolver());
        }
        return templateEngine;
    }

    @Override
    public Content createContentForTable(List<String> columns, List<Map<String, String>> table) {
        Document document = Jsoup.parse(createHTMLTable(columns, table));
        document.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
        Content content = new Content();
        content.iD = String.valueOf(UUID.randomUUID());
        content.body = document.body().html();
        return content;
    }

    @Override
    public Content createTickerSymbolContentForCompany(String companyName) {
        Document document = Jsoup.parse(createTickerSymbolAnchorTag(companyName));
        document.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
        Content content = new Content();
        content.iD = String.valueOf(UUID.randomUUID());
        content.body = document.body().html();
        return content;
    }

    @Override
    public Content createHeadingContent(String text, String type, String textAlignment, Boolean breakLine) {
        Document document = Jsoup.parse(generateDynamicHeading(text, type, textAlignment, breakLine));
        document.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
        Content content = new Content();
        content.iD = String.valueOf(UUID.randomUUID());
        content.body = document.body().html();
        return content;
    }

    @Override
    public Map<String, Object> extractContentFromWebPage(String url) {
        WebClient webClient = new WebClient(BrowserVersion.CHROME);
        webClient.getOptions().setJavaScriptEnabled(false);
        webClient.getOptions().setCssEnabled(false);
        try {
            Page page = webClient.getPage(url);
            Document document = Jsoup.parse(page.getWebResponse().getContentAsStream(), StandardCharsets.UTF_8.name(), url);
            String title = extractWebPageTitle(document);
            List<String> data = extractParagraphContents(document);
            return Map.of("Web Page Title", title,
                    "Web Page Content", String.join("\n", data));
        } catch (IOException e) {
            throw new RuntimeException("Could not connect to web page: " + e.getMessage());
        } finally {
            webClient.close();
        }
    }

    @Override
    public Content createAnchorTag(String text, String href) {
        Document document = Jsoup.parse(generateAnchorTag(text, href));
        document.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
        Content content = new Content();
        content.iD = String.valueOf(UUID.randomUUID());
        content.body = document.body().html();
        return content;
    }

    private String generateAnchorTag(String text, String href) {
        Context context = new Context();
        context.setVariable("text", text);
        context.setVariable("href", href);
        return getTemplateEngine().process("templates/anchor.html", context);
    }

    private String extractWebPageTitle(Document document) {
        return document.getElementsByTag("title").stream()
                .findAny().orElse(new Element("title")).ownText();
    }

    private List<String> extractParagraphContents(Document document) {
        return document.select("h1, h2, h3, h4, h5, h6, span, p")
                .stream().map(Element::ownText)
                .filter(s -> !(s.isEmpty() || s.isBlank()))
                .collect(Collectors.toList());
    }

    private String generateDynamicHeading(String text, String type, String textAlignment, Boolean breakLine) {
        Context context = new Context();
        context.setVariable("content", text);
        context.setVariable("type", type);
        context.setVariable("alignment", textAlignment.toLowerCase());
        context.setVariable("break", breakLine);
        return getTemplateEngine().process("templates/heading.html", context);
    }

    private String createHTMLTable(List<String> columns, List<Map<String, String>> table) {
        Context context = new Context();
        context.setVariable("headers", columns.stream().distinct().collect(Collectors.toList()));
        context.setVariable("rows", table);
        return getTemplateEngine().process("templates/table.html", context);
    }

    private String createTickerSymbolAnchorTag(String companyName) {
        try {
            Context context = new Context();
            Map<String, String> tickerSymbol = getTickerSymbol(companyName);
            context.setVariable("symbol", tickerSymbol.keySet().stream().findAny().orElse(""));
            context.setVariable("link", tickerSymbol.values().stream().findAny().orElse(""));
            return getTemplateEngine().process("templates/ticker.html", context);
        } catch (InterruptedException e){
            Thread.currentThread().interrupt();
            throw new ConfluenceRESTException(e.getMessage());
        } catch (URISyntaxException  | IOException e) {
            throw new ConfluenceRESTException(e.getMessage());
        }
    }

    private Map<String, String> getTickerSymbol(String companyName) throws URISyntaxException, IOException, InterruptedException {
        URIBuilder builder = new URIBuilder();
        builder.setScheme("https");
        builder.setHost("finance.yahoo.com");
        builder.setPath("lookup");
        builder.addParameter("s", companyName.replaceAll("[^\\w\\s]", " "));
        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(builder.build())
                .GET()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML)
                .build();
        HttpResponse<InputStream> response = confluenceAttributes.getRestClient().client().send(getRequest, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() == HttpURLConnection.HTTP_OK) {
            Document document = Jsoup.parse(response.body(), StandardCharsets.UTF_8.name(), builder.build().toString());
            Elements elements = document.getElementsByClass("data-col0");
            Element element = elements.stream().findFirst().orElseThrow(() -> new RuntimeException("No Ticker Symbol found for Company: " + companyName));
            String tickerSymbol = element.child(0).attr("data-symbol");
            String stockUrl = element.child(0).attr("href");
            return Map.of(tickerSymbol, builder.setPath(stockUrl).clearParameters().build().toString());
        } else throw new RemoteException("Yahoo Finance Service not available");
    }
}
