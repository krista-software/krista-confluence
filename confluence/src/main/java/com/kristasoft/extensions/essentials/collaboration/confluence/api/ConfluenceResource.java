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
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.lang.reflect.Type;
import java.util.Map;

import static com.kristasoft.extensions.essentials.collaboration.confluence.utils.Constants.CONFLUENCE_DATA;
import static com.kristasoft.extensions.essentials.collaboration.confluence.utils.Constants.CONFLUENCE_UPDATES;


@Path("/")
public class ConfluenceResource {
    private final EventHandler eventHandler;

    @Inject
    public ConfluenceResource(EventHandler eventHandler) {
        this.eventHandler = eventHandler;
    }


    @POST
    @Path("/webhook")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getConfluenceWebhookResponse(String webhookData) {
        if (webhookData != null && !webhookData.isBlank()) {
            FreeForm freeForm = new FreeForm();
            freeForm.put(CONFLUENCE_DATA, "Text", webhookData);
            eventHandler.handleEvent(CONFLUENCE_UPDATES, freeForm);
        }
        return Response.status(Response.Status.OK).entity("Webhook received and processed").build();
    }

}
