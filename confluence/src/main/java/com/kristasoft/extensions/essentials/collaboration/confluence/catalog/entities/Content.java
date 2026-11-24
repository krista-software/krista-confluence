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

@Domain(id = "catEntryDomain_1a86a3cc-0f2a-45fa-b9c0-b32487c830f8",
        name = "Confluence",
        ecosystemId = "catEntryEcosystem_0aeef50c-bc48-4c2c-844a-948193e53e82",
        ecosystemName = "Collaboration",
        ecosystemVersion = "77af6758-3926-41c4-9145-061944792713")
@Entity(name = "Content", id = "localDomainEntity_3d378ea7-749b-4837-9b7f-2ce96ea0a954", primaryKey = "ID", options = {} ,supportStore = false)
public class Content {

      @Field.Text(name = "ID", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
      public String iD;

      @Field(name = "Body", type = "RichText", required = true, attributes = {@Attribute(name = "visualWidth", value = "L")}, options = {})
      public String body;

}