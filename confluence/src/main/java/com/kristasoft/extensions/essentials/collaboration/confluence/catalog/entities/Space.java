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
@Entity(name = "Space", id = "localDomainEntity_340d866c-62e9-421a-9353-a5f144a81027", primaryKey = "Id", options = {} , supportStore = false)
public class Space {

      @Field.Text(name = "Id", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
      public String id;

      @Field.Text(name = "Name", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
      public String name;

      @Field.Text(name = "Type", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
      public String type;

      @Field.Text(name = "Status", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
      public String status;

      @Field.Text(name = "Description", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
      public String description;

      @Field.Date(name = "Created At", required = true, includeTimeOfDay = true, showHowManyDaysInViewer = 60, allowPast = true, allowToday = true, allowFuture = true, defaultTimeSpan = 1, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) 
      public Long createdAt;

}