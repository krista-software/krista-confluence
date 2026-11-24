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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public final class Constants {

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final Gson GSON_OBJECT = new Gson();
    public static final String VALIDATION_RESULTS = "Validation Results";
    public static final String STATE_ID = "stateId";
    public static final String RE_ENTER = "re-enter";
    public static final String SPACE_ID = "Space Id";
    public static final String SPACE_NAME = "Space Name";
    public static final String SPACE_KEY = "Space Key";
    public static final String SPACES = "Spaces";
    public static final String SPACE_ALIAS = "Space Alias";
    public static final String SPACE_IDENTIFIERS = "Space Identifiers";
    public static final String FAILED_TO_GET_SPACE_BY_ID = "Failed to get space by id ";
    public static final String FAILED_TO_GET_SPACES = "Failed to get spaces ";
    public static final String FAILED_TO_GET_SPACE_BY_ID_VALIDATION = "Failed to get space by id - validation error ";
    public static final String FAILED_TO_GET_SPACES_VALIDATION = "Failed to get spaces - validation error ";
    public static final String HANDLE_REENTER_GET_SPACE_BY_ID = "handleReenterGetSpaceById";
    public static final String CONFIRM_REENTER_GET_SPACE_BY_ID = "confirmReenterGetSpaceById";
    public static final String HANDLE_REENTER_GET_SPACES = "handleReenterGetSpaces";
    public static final String CONFIRM_REENTER_GET_SPACES = "confirmReenterGetSpaces";
    public static final String HANDLE_REENTER_GET_DIRECT_CHILD_PAGES_OF_SPACE = "handleReenterGetDirectChildPagesOfSpace";
    public static final String CONFIRM_REENTER_GET_DIRECT_CHILD_PAGES_OF_SPACE = "confirmReenterGetDirectChildPagesOfSpace";

    // Page-specific constants
    public static final String PAGES = "Pages";
    public static final String PAGE_IDENTIFIERS = "Page Identifiers";
    public static final String PAGE_ID = "Page ID";
    public static final String FAILED_TO_GET_PAGES_IN_SPACE = "Failed to get pages in space ";
    public static final String FAILED_TO_GET_PAGES_IN_SPACE_VALIDATION = "Failed to get pages in space - validation error ";
    public static final String HANDLE_REENTER_GET_PAGES_IN_SPACE = "handleReenterGetPagesInSpace";
    public static final String CONFIRM_REENTER_GET_PAGES_IN_SPACE = "confirmReenterGetPagesInSpace";
    public static final String CONFIRM_REENTER_GET_PAGE_IN_SPACE = "confirmReenterGetPageInSpace";
    public static final String HANDLE_REENTER_GET_PAGE_IN_SPACE = "handleReenterGetPageInSpace";
    public static final String FAILED_TO_GET_PAGE_IN_SPACE = "Failed to get page in space ";
    public static final String FAILED_TO_GET_PAGE_IN_SPACE_VALIDATION = "Failed to get page in space - validation error ";
    public static final String CONFIRM_REENTER_GET_PAGE_BY_ID = "confirmReenterGetPageById";
    public static final String HANDLE_REENTER_GET_PAGE_BY_ID = "handleReenterGetPageById";
    public static final String FAILED_TO_GET_PAGE_BY_ID = "Failed to get page by id ";
    public static final String FAILED_TO_GET_PAGE_BY_ID_VALIDATION = "Failed to get page by id - validation error ";
    public static final String CONFIRM_REENTER_CREATE_PAGE_FROM_TEMPLATE = "confirmReenterCreatePageFromTemplate";
    public static final String HANDLE_REENTER_CREATE_PAGE_FROM_TEMPLATE = "handleReenterCreatePageFromTemplate";
    public static final String FAILED_TO_CREATE_PAGE_FROM_TEMPLATE = "Failed to create page from template ";
    public static final String FAILED_TO_CREATE_PAGE_FROM_TEMPLATE_VALIDATION = "Failed to create page from template - validation error ";
    public static final String CONFIRM_REENTER_CREATE_PAGE_FROM_CONTENT = "confirmReenterCreatePageFromContent";
    public static final String HANDLE_REENTER_CREATE_PAGE_FROM_CONTENT = "handleReenterCreatePageFromContent";
    public static final String FAILED_TO_CREATE_PAGE_FROM_CONTENT = "Failed to create page from content ";
    public static final String FAILED_TO_CREATE_PAGE_FROM_CONTENT_VALIDATION = "Failed to create page from content - validation error ";
    public static final String CONFIRM_REENTER_UPDATE_PAGE_BY_CONTENT = "confirmReenterUpdatePageByContent";
    public static final String HANDLE_REENTER_UPDATE_PAGE_BY_CONTENT = "handleReenterUpdatePageByContent";
    public static final String FAILED_TO_UPDATE_PAGE_BY_CONTENT = "Failed to update page by content ";
    public static final String FAILED_TO_UPDATE_PAGE_BY_CONTENT_VALIDATION = "Failed to update page by content - validation error ";
    public static final String CONFIRM_REENTER_UPDATE_PAGE_FROM_TEMPLATE = "confirmReenterUpdatePageFromTemplate";
    public static final String HANDLE_REENTER_UPDATE_PAGE_FROM_TEMPLATE = "handleReenterUpdatePageFromTemplate";
    public static final String FAILED_TO_UPDATE_PAGE_FROM_TEMPLATE = "Failed to update page from template ";
    public static final String FAILED_TO_UPDATE_PAGE_FROM_TEMPLATE_VALIDATION = "Failed to update page from template - validation error ";
    public static final String CONFIRM_REENTER_GET_ALL_NESTED_DESCENDANTS_OF_PAGE = "confirmReenterGetAllNestedDescendantsOfPage";
    public static final String HANDLE_REENTER_GET_ALL_NESTED_DESCENDANTS_OF_PAGE = "handleReenterGetAllNestedDescendantsOfPage";
    public static final String FAILED_TO_GET_ALL_NESTED_DESCENDANTS_OF_PAGE = "Failed to get all nested descendants of page ";
    public static final String FAILED_TO_GET_ALL_NESTED_DESCENDANTS_OF_PAGE_VALIDATION = "Failed to get all nested descendants of page - validation error ";
    public static final String HANDLE_REENTER_GET_DIRECT_CHILD_PAGES_OF_PAGE = "handleReenterGetDirectChildPagesOfPage";
    public static final String CONFIRM_REENTER_GET_DIRECT_CHILD_PAGES_OF_PAGE = "confirmReenterGetDirectChildPagesOfPage";
    public static final String HANDLE_REENTER_CHECK_PAGE_HAS_CHILDREN = "handleReenterCheckPageHasChildren";
    public static final String CONFIRM_REENTER_CHECK_PAGE_HAS_CHILDREN = "confirmReenterCheckPageHasChildren";
    public static final String CONFIRM_REENTER_GET_PAGES_IN_SPACE_V2 = "confirmReenterGetPagesInSpaceV2";
    public static final String HANDLE_REENTER_GET_PAGES_IN_SPACE_V2 = "handleReenterGetPagesInSpaceV2";
    public static final String FAILED_TO_GET_PAGES_IN_SPACE_V2 = "Failed to get pages in space (V2) ";
    public static final String FAILED_TO_GET_PAGES_IN_SPACE_V2_VALIDATION = "Failed to get pages in space (V2) - validation error ";

    public static final String HANDLE_REENTER_GET_PAGE_CONTENT_AS_FILE = "handleReenterGetPageContentAsFile";
    public static final String FAILED_TO_GET_PAGE_CONTENT_AS_FILE = "Failed to get page content as a file ";
    public static final String FAILED_TO_GET_PAGE_CONTENT_AS_FILE_VALIDATION = "Failed to get page content as a file - validation error ";

    // Template-specific constants
    public static final String TEMPLATES = "Templates";
    public static final String TEMPLATE_IDENTIFIERS = "Template Identifiers";
    public static final String FAILED_TO_GET_TEMPLATE_BY_ID = "Failed to get template by id ";
    public static final String FAILED_TO_GET_TEMPLATE_BY_ID_VALIDATION = "Failed to get template by id - validation error ";
    public static final String HANDLE_REENTER_GET_TEMPLATE_BY_ID = "handleReenterGetTemplateById";
    public static final String CONFIRM_REENTER_GET_TEMPLATE_BY_ID = "confirmReenterGetTemplateById";
    public static final String FAILED_TO_GET_TEMPLATES_BY_SPACE_ID = "Failed to get templates by space id ";
    public static final String FAILED_TO_GET_TEMPLATES_BY_SPACE_ID_VALIDATION = "Failed to get templates by space id - validation error ";
    public static final String HANDLE_REENTER_GET_TEMPLATES_BY_SPACE_ID = "handleReenterGetTemplatesBySpaceId";
    public static final String CONFIRM_REENTER_GET_TEMPLATES_BY_SPACE_ID = "confirmReenterGetTemplatesBySpaceId";
    public static final String CONFIRM_REENTER_GET_PAGE_CONTENT_AS_FILE = "confirmReenterGetPageContentAsFile";

    public static final String CONFLUENCE_DATA = "Confluence Data";
    public static final String CONFLUENCE_UPDATES = "Confluence Update";

    // Allow Retry feature constant
    public static final String ENABLE_ERROR_HANDLING = "Allow Retry";

    // Exclude Restricted Pages feature constant
    public static final String EXCLUDE_RESTRICTED_PAGES = "Exclude Restricted Pages";

    // Page status filter for Get Pages in Space (V2) — a PickOne (current/archived/any/trashed),
    // mirroring the "Status" field on Get Attachments for Page. Passed to the v2 pages endpoint as
    // the "status" query param so non-matching pages are never fetched or restriction-checked;
    // "any" (and null/empty) means no status filter.
    public static final String PAGE_STATUS = "Status";

    // Include Page Content feature constant — UI field is unticked by default
    // (Krista switches always render unticked and the platform offers no
    // default-state member on @Field.Boolean). The default-off state means
    // page bodies are excluded, which is the safe behaviour. Ticking opts into
    // loading full storage-format content per page.
    public static final String INCLUDE_PAGE_CONTENT = "Include Page Content";

    // User-facing message when Get Pages in Space (V2) fails because the response
    // exceeded available heap. Held as a constant so the message format does not
    // require allocating a new format-string instance while the JVM is recovering
    // from an OutOfMemoryError.
    public static final String PAGES_IN_SPACE_V2_OOM_MESSAGE = """
            We couldn't retrieve the pages for space '%s' because the response was too large to fit in memory.
            This happens when a space contains many pages or pages with large bodies and 'Include Page Content'
            is ticked — every page's full storage-format content is loaded at once.

            What to do next:
            1. Untick 'Include Page Content' (the default state) and run the request again — page metadata will
               be returned without body content, which keeps responses small.
            2. If you need page bodies, fetch them one page at a time using 'Get Page By ID' instead of pulling
               every page's content together.
            3. For very large spaces, consider narrowing the scope with 'Get Direct Child Pages of Space' or
               'Get Direct Child Pages of Page' before drilling in.""";

    // User-facing message when Download Attachment (V2) fails due to an unexpected exception.
    // Contains a single %s placeholder for the technical details (exception message).
    public static final String DOWNLOAD_ATTACHMENT_V2_ERROR_MESSAGE = """
            We couldn't download the attachment.
            This could mean:
            \u2022 The attachment ID is incorrect or the attachment has been deleted
            \u2022 You don't have permission to download this attachment
            \u2022 The resolved download link has expired
            \u2022 Your Confluence connection might be experiencing issues

            What to do next:
            1. Verify the attachment still exists using 'Get Attachment by ID'
            2. Check if you have permission to download attachments
            3. Try again in a moment for transient errors
            4. Ensure your Confluence connection is working properly

            Technical details: %s
            """;


    private Constants() {
    }
}
