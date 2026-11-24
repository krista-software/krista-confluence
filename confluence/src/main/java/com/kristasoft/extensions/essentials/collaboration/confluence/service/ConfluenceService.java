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
import com.kristasoft.extensions.essentials.collaboration.confluence.dto.Page;
import com.kristasoft.extensions.essentials.collaboration.confluence.dto.Space;
import com.kristasoft.extensions.essentials.collaboration.confluence.dto.Template;
import com.kristasoft.extensions.essentials.collaboration.confluence.dto.User;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

/**
 * Facade service that provides a unified interface to all Confluence operations.
 * This service delegates to specialized service implementations that follow the Single Responsibility Principle.
 * <p>
 * Internally uses:
 * <ul>
 *   <li>{@link SpaceServiceImpl} - for space operations</li>
 *   <li>{@link TemplateServiceImpl} - for template operations</li>
 *   <li>{@link PageServiceImpl} - for page CRUD operations</li>
 *   <li>{@link PageHierarchyService} - for page hierarchy operations</li>
 *   <li>{@link UserServiceImpl} - for user operations</li>
 * </ul>
 */
@Service
public class ConfluenceService implements SpaceService, TemplateService, PageService, UserService {
    private final ConfluenceAttributes confluenceAttributes;
    private final SpaceService spaceService;
    private final TemplateService templateService;
    private final PageService pageService;
    private final PageHierarchyService pageHierarchyService;
    private final UserService userService;

    @Inject
    public ConfluenceService(ConfluenceAttributes confluenceAttributes,
                            SpaceService spaceService,
                            TemplateService templateService,
                            PageService pageService,
                            PageHierarchyService pageHierarchyService,
                            UserService userService) {
        this.confluenceAttributes = confluenceAttributes;
        this.spaceService = spaceService;
        this.templateService = templateService;
        this.pageService = pageService;
        this.pageHierarchyService = pageHierarchyService;
        this.userService = userService;
    }

    public ConfluenceService(String domainName, String accountEmail, String apiKey) {
        this.confluenceAttributes = new ConfluenceAttributes(domainName, accountEmail, apiKey);
        this.spaceService = new SpaceServiceImpl(confluenceAttributes);
        this.templateService = new TemplateServiceImpl(confluenceAttributes, spaceService);
        this.pageHierarchyService = new PageHierarchyService(confluenceAttributes);
        this.pageService = new PageServiceImpl(confluenceAttributes, spaceService, templateService, pageHierarchyService);
        this.userService = new UserServiceImpl(confluenceAttributes);
    }

    @Override
    public Page[] getPagesInSpaceV2(String spaceId, boolean excludeRestricted, boolean excludeContent,
                                    String status) {
        return pageService.getPagesInSpaceV2(spaceId, excludeRestricted, excludeContent, status);
    }


    // SpaceService delegation
    @Override
    public Space[] getSpaces() {
        return spaceService.getSpaces();
    }

    @Override
    public Space getSpaceById(String id) {
        return spaceService.getSpaceById(id);
    }

    @Override
    public Space getSpaceIdByAlias(String alias) {
        return spaceService.getSpaceIdByAlias(alias);
    }

    // TemplateService delegation
    @Override
    public Template getTemplateById(String templateId) {
        return templateService.getTemplateById(templateId);
    }

    @Override
    public Template[] getTemplatesBySpaceId(String spaceId) {
        return templateService.getTemplatesBySpaceId(spaceId);
    }

    // PageService delegation
    @Override
    public Page[] getPagesBySpaceId(String spaceId) {
        return pageService.getPagesBySpaceId(spaceId);
    }

    @Override
    public Page getPageById(String id) {
        return pageService.getPageById(id);
    }

    @Override
    public Page createPageFromTemplate(String spaceId, String templateId, String title, List<Map<String, Object>> variables) {
        return pageService.createPageFromTemplate(spaceId, templateId, title, variables);
    }

    @Override
    public Page createPageFromContent(String spaceId, String title, List<Content> contents) {
        return pageService.createPageFromContent(spaceId, title, contents);
    }

    @Override
    public Page updatePageFromTemplate(String id, String templateId, String status, String title, String comment, List<Map<String, Object>> variables) {
        return pageService.updatePageFromTemplate(id, templateId, status, title, comment, variables);
    }

    @Override
    public Page updatePageFromContent(String id, String status, String title, String comment, List<Content> contents) {
        return pageService.updatePageFromContent(id, status, title, comment, contents);
    }

    @Override
    public Page[] getDirectChildPagesOfSpace(String spaceId) {
        return pageService.getDirectChildPagesOfSpace(spaceId);
    }

    @Override
    public Page[] getDirectChildPagesOfPage(String pageId) {
        return pageService.getDirectChildPagesOfPage(pageId);
    }

    @Override
    public boolean hasChildPages(String pageId) {
        return pageService.hasChildPages(pageId);
    }

    @Override
    public boolean hasChildren(String contentId, String contentType) {
        return pageService.hasChildren(contentId, contentType);
    }

    @Override
    public Page[] getDirectChildrenOfContent(String contentId, String contentType) {
        return pageService.getDirectChildrenOfContent(contentId, contentType);
    }

    @Override
    public Page[] getAncestorsOfContent(String contentId, String contentType) {
        return pageService.getAncestorsOfContent(contentId, contentType);
    }

    // Additional public methods used by catalog classes (not part of interfaces)
    
    /**
     * Get all nested descendants of a page recursively.
     * This is a convenience method that defaults to "page" content type.
     * For type-aware retrieval, use {@link #getAllNestedDescendants(String, String)}.
     *
     * @param pageId the page ID to get all descendants from
     * @return Array of all descendant pages (children, grandchildren, etc.)
     */
    public Page[] getAllNestedDescendants(String pageId) {
        return pageHierarchyService.getAllNestedDescendants(pageId);
    }

    /**
     * Get all nested descendants of a content item recursively.
     * This method traverses the entire content hierarchy starting from the given content ID.
     * Supports all content types: page, folder, whiteboard, database, embed.
     *
     * @param contentId the content ID to get all descendants from
     * @param contentType the type of content ("page", "folder", "whiteboard", "database", "embed")
     * @return Array of all descendant content items (children, grandchildren, etc.)
     */
    public Page[] getAllNestedDescendants(String contentId, String contentType) {
        return pageHierarchyService.getAllNestedDescendants(contentId, contentType);
    }

    /**
     * Get all ancestors of a page.
     * This is a convenience method that defaults to "page" content type.
     * For type-aware retrieval, use {@link #getAncestorsOfContent(String, String)}.
     *
     * @param pageId the page ID to get ancestors from
     * @return Array of all ancestor pages
     */
    public Page[] getAncestorsOfPage(String pageId) {
        return pageHierarchyService.getAncestorsOfPage(pageId);
    }

    // UserService delegation
    @Override
    public User getUserByAccountId(String accountId) {
        return userService.getUserByAccountId(accountId);
    }

    /**
     * Get the ConfluenceAttributes instance.
     * Used by catalog classes to access the REST client.
     *
     * @return the ConfluenceAttributes instance
     */
    public ConfluenceAttributes getConfluenceAttributes() {
        return confluenceAttributes;
    }
}

