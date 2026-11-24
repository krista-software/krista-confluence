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

import app.krista.extensions.util.EventHandler;
import app.krista.model.base.FreeForm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.ws.rs.core.Response;

import static com.kristasoft.extensions.essentials.collaboration.confluence.utils.Constants.CONFLUENCE_DATA;
import static com.kristasoft.extensions.essentials.collaboration.confluence.utils.Constants.CONFLUENCE_UPDATES;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfluenceResourceTest {

    @Mock
    private EventHandler eventHandler;

    private ConfluenceResource confluenceResource;

    @BeforeEach
    void setUp() {
        confluenceResource = new ConfluenceResource(eventHandler);
    }

    @Test
    void testGetConfluenceWebhookResponse_WithValidData() {
        String webhookData = "{\"event\":\"page_created\",\"page\":{\"id\":\"123\"}}";

        Response response = confluenceResource.getConfluenceWebhookResponse(webhookData);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("Webhook received and processed", response.getEntity());

        ArgumentCaptor<FreeForm> freeFormCaptor = ArgumentCaptor.forClass(FreeForm.class);
        verify(eventHandler).handleEvent(eq(CONFLUENCE_UPDATES), freeFormCaptor.capture());
        assertNotNull(freeFormCaptor.getValue());
    }

    @Test
    void testGetConfluenceWebhookResponse_WithNullData() {
        Response response = confluenceResource.getConfluenceWebhookResponse(null);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("Webhook received and processed", response.getEntity());
        verify(eventHandler, never()).handleEvent(anyString(), any(FreeForm.class));
    }

    @Test
    void testGetConfluenceWebhookResponse_WithEmptyData() {
        Response response = confluenceResource.getConfluenceWebhookResponse("");

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        verify(eventHandler, never()).handleEvent(anyString(), any(FreeForm.class));
    }

    @Test
    void testGetConfluenceWebhookResponse_WithBlankData() {
        Response response = confluenceResource.getConfluenceWebhookResponse("   ");

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        verify(eventHandler, never()).handleEvent(anyString(), any(FreeForm.class));
    }

    @Test
    void testGetConfluenceWebhookResponse_WithSimpleStringData() {
        Response response = confluenceResource.getConfluenceWebhookResponse("simple text");

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        verify(eventHandler).handleEvent(eq(CONFLUENCE_UPDATES), any(FreeForm.class));
    }
}
