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

import java.util.List;
import java.util.Map;

@Domain(id = ConfluenceConstants.DOMAIN_ID,
        name = ConfluenceConstants.KRISTA_DOMAIN_NAME,
        ecosystemId = ConfluenceConstants.ECOSYSTEM_ID,
        ecosystemName = ConfluenceConstants.ECOSYSTEM_NAME,
        ecosystemVersion = ConfluenceConstants.ECOSYSTEM_VERSION)
@Entity(name = "Template", id = "localDomainEntity_397db3e6-2109-4bf5-8238-162320912670", primaryKey = "Name", options = {}, supportStore = false)
public class Template {

      @Field(name = "Id", type = "Identifier", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
      public String id;

      @Field.Text(name = "Name", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
      public String name;

      @Field(name = "Space Id", type = "Identifier", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
      public String spaceId;

      @Field.Text(name = "Type", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
      public String type;

      @Field(name = "Body", type = "RichText", required = true, attributes = {@Attribute(name = "visualWidth", value = "L")}, options = {})
      public String body;

      @Field.Desc(name = "Variables", type = "[ { Key: Text, Value: Text, Options: [ Text ] } ]",required = true)
      public List<Map<String, Object>> variables;

}