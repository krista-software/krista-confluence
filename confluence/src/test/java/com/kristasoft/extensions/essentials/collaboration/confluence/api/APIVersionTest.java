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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class APIVersionTest {

    @Test
    void testV1Endpoint() {
        assertEquals("wiki/rest/api/", APIVersion.V1.getEndpoint());
    }

    @Test
    void testV2Endpoint() {
        assertEquals("wiki/api/v2/", APIVersion.V2.getEndpoint());
    }

    @Test
    void testV1ToString() {
        assertEquals("wiki/rest/api/", APIVersion.V1.toString());
    }

    @Test
    void testV2ToString() {
        assertEquals("wiki/api/v2/", APIVersion.V2.toString());
    }

    @Test
    void testEnumValues() {
        APIVersion[] values = APIVersion.values();
        assertEquals(2, values.length);
    }

    @Test
    void testEnumValueOf() {
        assertEquals(APIVersion.V1, APIVersion.valueOf("V1"));
        assertEquals(APIVersion.V2, APIVersion.valueOf("V2"));
    }
}
