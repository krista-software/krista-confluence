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

package com.kristasoft.extensions.essentials.collaboration.confluence.catalog.entities;

import java.util.Map;
import java.util.List;
import app.krista.extension.impl.anno.*;
import app.krista.ksdk.entities.EntityReference;
import app.krista.model.base.*;

@Domain(id = "catEntryDomain_1a86a3cc-0f2a-45fa-b9c0-b32487c830f8",
        name = "Confluence",
        ecosystemId = "catEntryEcosystem_0aeef50c-bc48-4c2c-844a-948193e53e82",
        ecosystemName = "Collaboration",
        ecosystemVersion ="e59b7bbb-77a9-4af4-a51f-52db62e78ebf")
@Entity(name = "Attachment", id = "localDomainEntity_e846fb62-e0fe-4f46-b27f-9ba52536db5a", primaryKey = "Id", options = {} , supportStore = false)
public class Attachment {

      @Field.Text(name = "Id", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
      public String id;

      @Field.Text(name = "Title", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
      public String title;

      @Field.Text(name = "Page Id", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
      public String pageId;

      @Field.Text(name = "Media Type", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
      public String mediaType;

      @Field(name = "File Size", type = "Number", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
      public Double fileSize;

      @Field(name = "Download Link", type = "Paragraph", required = false, attributes = {@Attribute(name = "visualWidth", value = "L")}, options = {})
      public String downloadLink;

      @Field(name = "Web UI Link", type = "Paragraph", required = false, attributes = {@Attribute(name = "visualWidth", value = "L")}, options = {})
      public String webUILink;

      @Field.Date(name = "Created At", required = false, includeTimeOfDay = true, allowPast = true, allowToday = true, allowFuture = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) 
      public Long createdAt;

      @Field(name = "Version Number", type = "Number", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
      public Double versionNumber;

      @Field.Date(name = "Version Created At", required = false, includeTimeOfDay = true, allowPast = true, allowToday = true, allowFuture = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) 
      public Long versionCreatedAt;

      @Field.Text(name = "Status", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
      public String status;

}