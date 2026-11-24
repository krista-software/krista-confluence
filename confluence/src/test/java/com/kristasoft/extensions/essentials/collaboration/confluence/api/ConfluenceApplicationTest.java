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

package com.kristasoft.extensions.essentials.collaboration.confluence.api;

import app.krista.sdk.atlassian.extension.authentication.AtlassianManagedCallbackResource;
import com.kristasoft.extensions.essentials.collaboration.confluence.mcp.ConfluenceMcpSetupResource;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ConfluenceApplicationTest {

    @Test
    void testGetClasses_ContainsExpectedClasses() {
        ConfluenceApplication app = new ConfluenceApplication();
        Set<Class<?>> classes = app.getClasses();

        assertNotNull(classes);
        assertEquals(6, classes.size());
        assertTrue(classes.contains(ConfluenceResource.class));
        assertTrue(classes.contains(AuthenticationResource.class));
        assertTrue(classes.contains(ConfluenceAuthUiResource.class));
        assertTrue(classes.contains(ConfluenceAuditResource.class));
        assertTrue(classes.contains(ConfluenceMcpSetupResource.class));
        assertTrue(classes.contains(AtlassianManagedCallbackResource.class));
    }
}
