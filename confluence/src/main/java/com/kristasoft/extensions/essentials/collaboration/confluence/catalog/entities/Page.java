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

import app.krista.extension.impl.anno.Attribute;
import app.krista.extension.impl.anno.Domain;
import app.krista.extension.impl.anno.Entity;
import app.krista.extension.impl.anno.Field;
import com.kristasoft.extensions.essentials.collaboration.confluence.utils.ConfluenceConstants;

@Domain(id = ConfluenceConstants.DOMAIN_ID,
        name = ConfluenceConstants.KRISTA_DOMAIN_NAME,
        ecosystemId = ConfluenceConstants.ECOSYSTEM_ID,
        ecosystemName = ConfluenceConstants.ECOSYSTEM_NAME,
        ecosystemVersion = ConfluenceConstants.ECOSYSTEM_VERSION)
@Entity(name = "Page", id = "localDomainEntity_69687da7-4c87-4234-81d1-bb6961e2ff18", primaryKey = "Id", options = {} , supportStore =  false)
public class Page {

      @Field.Text(name = "Id", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
      public String id;

      @Field(name = "Space ID", type = "Identifier", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
      public String spaceID;

      @Field.Text(name = "Title", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
      public String title;

      @Field.Text(name = "Status", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
      public String status;


      @Field(name = "Page URL", type = "RichText", required = true, attributes = {@Attribute(name = "visualWidth", value = "L")}, options = {})
      public String pageURL;

      @Field(name = "Body", type = "RichText", required = true, attributes = {@Attribute(name = "visualWidth", value = "L")}, options = {})
      public String body;

      @Field.Text(name = "Parent ID", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
      public String parentID;

      @Field.Text(name = "Parent Type", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
      public String parentType;

      @Field.Boolean(name = "Has Children", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
      public Boolean hasChildren;

      @Field.Date(name = "Created At", required = false, includeTimeOfDay = true, allowPast = true, allowToday = true, allowFuture = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
      public Long createdAt;

      @Field.Date(name = "Last Updated", required = false, includeTimeOfDay = true, allowPast = true, allowToday = true, allowFuture = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
      public Long lastUpdated;

      /**
       * The type of content. Possible values from V2 API direct-children endpoint:
       * - "page" - A regular Confluence page with content
       * - "folder" - An organizational container for pages and other content
       * - "whiteboard" - A collaborative whiteboard
       * - "database" - A Confluence database
       * - "embed" - An embedded content item
       */
      @Field.Text(name = "Type", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
      public String type;

      @Override
      public String toString() {
            return "Page{" +
                    "id='" + id + '\'' +
                    ", spaceID='" + spaceID + '\'' +
                    ", title='" + title + '\'' +
                    ", status='" + status + '\'' +
                    ", type='" + type + '\'' +
                    ", pageURL='" + pageURL + '\'' +
                    ", body='" + body + '\'' +
                    ", parentId='" + parentID + '\'' +
                    ", parentType='" + parentType + '\'' +
                    ", hasChildren=" + hasChildren +
                    ", createdAt=" + createdAt +
                    ", lastUpdated=" + lastUpdated +
                    '}';
      }
}