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
import com.kristasoft.extensions.essentials.collaboration.confluence.dto.Space;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SpaceDeserializerTest {

    @Test
    public void testDeserializeSpaceWithObjectDescription() {
        // This is the actual JSON response that was causing the error
        String jsonResponse = """
            {
                "spaceOwnerId":"6408e11ffeb4b150c579cc7b",
                "authorId":"6408e11ffeb4b150c579cc7b",
                "homepageId":"1900745",
                "icon":null,
                "description":{},
                "createdAt":"2023-03-08T22:27:01.137Z",
                "status":"archived",
                "name":"Jim Mooney",
                "key":"~6408e11ffeb4b150c579cc7b",
                "id":"1900546",
                "type":"personal",
                "_links":{"webui":"/spaces/~6408e11ffeb4b150c579cc7b","base":"https://smartpayllc.atlassian.net/wiki"},
                "currentActiveAlias":"~6408e11ffeb4b150c579cc7b"
            }
            """;

        Gson gson = GsonFactory.getInstance();
        
        // This should not throw an exception anymore
        assertDoesNotThrow(() -> {
            Space space = gson.fromJson(jsonResponse, Space.class);
            
            // Verify the space was deserialized correctly
            assertNotNull(space);
            assertEquals("1900546", space.getId());
            assertEquals("Jim Mooney", space.getName());
            assertEquals("personal", space.getType());
            assertEquals("archived", space.getStatus());
            assertEquals("6408e11ffeb4b150c579cc7b", space.getAuthorId());
            assertEquals("1900745", space.getHomepageId());
            assertEquals("~6408e11ffeb4b150c579cc7b", space.getKey());
            
            // Description should be empty string since it was an empty object
            assertEquals("", space.getDescription());
            
            // CreatedAt should be parsed correctly
            assertNotNull(space.getCreatedAt());

            // currentActiveAlias must be mapped (used by Get Space Id by Alias)
            assertEquals("~6408e11ffeb4b150c579cc7b", space.getCurrentActiveAlias());
        });
    }

    @Test
    public void testDeserializeMapsCurrentActiveAliasForGlobalSpace() {
        String jsonResponse = """
            {
                "authorId":"712020:7bb6ed77-0353-4d41-a78d-55379f8a6604",
                "homepageId":"295021",
                "icon":null,
                "description":null,
                "createdAt":"2025-12-23T20:51:04.911Z",
                "status":"current",
                "name":"My first space",
                "key":"MFS",
                "id":"294916",
                "type":"global",
                "_links":{"webui":"/spaces/MFS"},
                "currentActiveAlias":"MFS"
            }
            """;

        Space space = GsonFactory.getInstance().fromJson(jsonResponse, Space.class);

        assertNotNull(space);
        assertEquals("294916", space.getId());
        assertEquals("MFS", space.getCurrentActiveAlias());
    }

    @Test
    public void testDeserializeSpaceWithStringDescription() {
        String jsonResponse = """
            {
                "authorId":"6408e11ffeb4b150c579cc7b",
                "homepageId":"1900745",
                "icon":null,
                "description":"This is a test space description",
                "createdAt":"2023-03-08T22:27:01.137Z",
                "status":"current",
                "name":"Test Space",
                "key":"TEST",
                "id":"123456",
                "type":"global"
            }
            """;

        Gson gson = GsonFactory.getInstance();
        
        assertDoesNotThrow(() -> {
            Space space = gson.fromJson(jsonResponse, Space.class);
            
            assertNotNull(space);
            assertEquals("123456", space.getId());
            assertEquals("Test Space", space.getName());
            assertEquals("global", space.getType());
            assertEquals("current", space.getStatus());
            assertEquals("This is a test space description", space.getDescription());
        });
    }

    @Test
    public void testDeserializeSpaceWithDescriptionObjectWithValue() {
        String jsonResponse = """
            {
                "authorId":"6408e11ffeb4b150c579cc7b",
                "homepageId":"1900745",
                "icon":null,
                "description":{"value":"Description from object"},
                "createdAt":"2023-03-08T22:27:01.137Z",
                "status":"current",
                "name":"Test Space",
                "key":"TEST",
                "id":"123456",
                "type":"global"
            }
            """;

        Gson gson = GsonFactory.getInstance();
        
        assertDoesNotThrow(() -> {
            Space space = gson.fromJson(jsonResponse, Space.class);
            
            assertNotNull(space);
            assertEquals("Description from object", space.getDescription());
        });
    }
}
