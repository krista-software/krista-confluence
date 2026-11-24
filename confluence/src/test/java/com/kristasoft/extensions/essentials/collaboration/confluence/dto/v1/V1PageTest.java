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

package com.kristasoft.extensions.essentials.collaboration.confluence.dto.v1;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link V1Page} and its inner classes
 * {@link V1Page.V1Extensions}, {@link V1Page.V1Expandable}, {@link V1Page.V1PageLinks}.
 *
 * Covers: getters/setters for id, type, status, title, extensions, expandable, links.
 *         Inner classes: V1Extensions (position), V1Expandable (space),
 *         V1PageLinks (self, webui, tinyui, editui, edituiv2).
 */
@DisplayName("V1Page DTO")
class V1PageTest {

    // ══════════════════════════════════════════════════════════════════════
    //  V1Page top-level getters/setters
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("setId / getId round-trip")
    void setId_getId_roundTrip() {
        V1Page page = new V1Page();
        page.setId("12345");

        assertEquals("12345", page.getId());
    }

    @Test
    @DisplayName("setType / getType round-trip")
    void setType_getType_roundTrip() {
        V1Page page = new V1Page();
        page.setType("page");

        assertEquals("page", page.getType());
    }

    @Test
    @DisplayName("setStatus / getStatus round-trip")
    void setStatus_getStatus_roundTrip() {
        V1Page page = new V1Page();
        page.setStatus("current");

        assertEquals("current", page.getStatus());
    }

    @Test
    @DisplayName("setTitle / getTitle round-trip")
    void setTitle_getTitle_roundTrip() {
        V1Page page = new V1Page();
        page.setTitle("Test Page");

        assertEquals("Test Page", page.getTitle());
    }

    @Test
    @DisplayName("setExtensions / getExtensions round-trip")
    void setExtensions_getExtensions_roundTrip() {
        V1Page page = new V1Page();
        V1Page.V1Extensions extensions = new V1Page.V1Extensions();
        extensions.setPosition(42L);

        page.setExtensions(extensions);

        assertSame(extensions, page.getExtensions());
    }

    @Test
    @DisplayName("setExpandable / getExpandable round-trip")
    void setExpandable_getExpandable_roundTrip() {
        V1Page page = new V1Page();
        V1Page.V1Expandable expandable = new V1Page.V1Expandable();
        expandable.setSpace("/rest/api/space/MYSPACE");

        page.setExpandable(expandable);

        assertSame(expandable, page.getExpandable());
    }

    @Test
    @DisplayName("setLinks / getLinks round-trip")
    void setLinks_getLinks_roundTrip() {
        V1Page page = new V1Page();
        V1Page.V1PageLinks links = new V1Page.V1PageLinks();
        links.setSelf("/rest/api/content/12345");

        page.setLinks(links);

        assertSame(links, page.getLinks());
    }

    @Test
    @DisplayName("default values are null for all fields")
    void defaultValues_allNull() {
        V1Page page = new V1Page();

        assertNull(page.getId());
        assertNull(page.getType());
        assertNull(page.getStatus());
        assertNull(page.getTitle());
        assertNull(page.getExtensions());
        assertNull(page.getExpandable());
        assertNull(page.getLinks());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  V1Extensions inner class
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("V1Extensions")
    class V1ExtensionsTest {

        @Test
        @DisplayName("setPosition / getPosition round-trip")
        void setPosition_getPosition_roundTrip() {
            V1Page.V1Extensions ext = new V1Page.V1Extensions();
            ext.setPosition(100L);

            assertEquals(100L, ext.getPosition());
        }

        @Test
        @DisplayName("default position is 0")
        void defaultPosition_isZero() {
            V1Page.V1Extensions ext = new V1Page.V1Extensions();

            assertEquals(0L, ext.getPosition());
        }

        @Test
        @DisplayName("setPosition with negative value")
        void setPosition_negativeValue() {
            V1Page.V1Extensions ext = new V1Page.V1Extensions();
            ext.setPosition(-1L);

            assertEquals(-1L, ext.getPosition());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  V1Expandable inner class
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("V1Expandable")
    class V1ExpandableTest {

        @Test
        @DisplayName("setSpace / getSpace round-trip")
        void setSpace_getSpace_roundTrip() {
            V1Page.V1Expandable expandable = new V1Page.V1Expandable();
            expandable.setSpace("/rest/api/space/TEST");

            assertEquals("/rest/api/space/TEST", expandable.getSpace());
        }

        @Test
        @DisplayName("default space is null")
        void defaultSpace_isNull() {
            V1Page.V1Expandable expandable = new V1Page.V1Expandable();

            assertNull(expandable.getSpace());
        }

        @Test
        @DisplayName("setSpace to null clears space")
        void setSpace_null_clearsSpace() {
            V1Page.V1Expandable expandable = new V1Page.V1Expandable();
            expandable.setSpace("something");
            expandable.setSpace(null);

            assertNull(expandable.getSpace());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  V1PageLinks inner class
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("V1PageLinks")
    class V1PageLinksTest {

        @Test
        @DisplayName("setSelf / getSelf round-trip")
        void setSelf_getSelf_roundTrip() {
            V1Page.V1PageLinks links = new V1Page.V1PageLinks();
            links.setSelf("/rest/api/content/123");

            assertEquals("/rest/api/content/123", links.getSelf());
        }

        @Test
        @DisplayName("setWebui / getWebui round-trip")
        void setWebui_getWebui_roundTrip() {
            V1Page.V1PageLinks links = new V1Page.V1PageLinks();
            links.setWebui("/spaces/TEST/pages/123/Test+Page");

            assertEquals("/spaces/TEST/pages/123/Test+Page", links.getWebui());
        }

        @Test
        @DisplayName("setTinyui / getTinyui round-trip")
        void setTinyui_getTinyui_roundTrip() {
            V1Page.V1PageLinks links = new V1Page.V1PageLinks();
            links.setTinyui("/x/AbCd");

            assertEquals("/x/AbCd", links.getTinyui());
        }

        @Test
        @DisplayName("setEditui / getEditui round-trip")
        void setEditui_getEditui_roundTrip() {
            V1Page.V1PageLinks links = new V1Page.V1PageLinks();
            links.setEditui("/pages/resumedraft.action?draftId=123");

            assertEquals("/pages/resumedraft.action?draftId=123", links.getEditui());
        }

        @Test
        @DisplayName("setEdituiv2 / getEdituiv2 round-trip")
        void setEdituiv2_getEdituiv2_roundTrip() {
            V1Page.V1PageLinks links = new V1Page.V1PageLinks();
            links.setEdituiv2("/spaces/TEST/pages/edit-v2/123");

            assertEquals("/spaces/TEST/pages/edit-v2/123", links.getEdituiv2());
        }

        @Test
        @DisplayName("all default link values are null")
        void defaultValues_allNull() {
            V1Page.V1PageLinks links = new V1Page.V1PageLinks();

            assertNull(links.getSelf());
            assertNull(links.getWebui());
            assertNull(links.getTinyui());
            assertNull(links.getEditui());
            assertNull(links.getEdituiv2());
        }

        @Test
        @DisplayName("all links can be set and retrieved together")
        void allLinks_setAndRetrieved() {
            V1Page.V1PageLinks links = new V1Page.V1PageLinks();
            links.setSelf("/self");
            links.setWebui("/webui");
            links.setTinyui("/tinyui");
            links.setEditui("/editui");
            links.setEdituiv2("/edituiv2");

            assertEquals("/self", links.getSelf());
            assertEquals("/webui", links.getWebui());
            assertEquals("/tinyui", links.getTinyui());
            assertEquals("/editui", links.getEditui());
            assertEquals("/edituiv2", links.getEdituiv2());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Full V1Page with all inner objects populated
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("V1Page with all fields populated returns correct values")
    void fullPage_allFieldsPopulated() {
        V1Page page = new V1Page();
        page.setId("99");
        page.setType("page");
        page.setStatus("current");
        page.setTitle("Full Test Page");

        V1Page.V1Extensions ext = new V1Page.V1Extensions();
        ext.setPosition(5L);
        page.setExtensions(ext);

        V1Page.V1Expandable exp = new V1Page.V1Expandable();
        exp.setSpace("/rest/api/space/FULL");
        page.setExpandable(exp);

        V1Page.V1PageLinks links = new V1Page.V1PageLinks();
        links.setSelf("/rest/api/content/99");
        links.setWebui("/spaces/FULL/pages/99");
        links.setTinyui("/x/Zz");
        links.setEditui("/edit/99");
        links.setEdituiv2("/edit-v2/99");
        page.setLinks(links);

        assertEquals("99", page.getId());
        assertEquals("page", page.getType());
        assertEquals("current", page.getStatus());
        assertEquals("Full Test Page", page.getTitle());
        assertEquals(5L, page.getExtensions().getPosition());
        assertEquals("/rest/api/space/FULL", page.getExpandable().getSpace());
        assertEquals("/rest/api/content/99", page.getLinks().getSelf());
        assertEquals("/spaces/FULL/pages/99", page.getLinks().getWebui());
        assertEquals("/x/Zz", page.getLinks().getTinyui());
        assertEquals("/edit/99", page.getLinks().getEditui());
        assertEquals("/edit-v2/99", page.getLinks().getEdituiv2());
    }
}
