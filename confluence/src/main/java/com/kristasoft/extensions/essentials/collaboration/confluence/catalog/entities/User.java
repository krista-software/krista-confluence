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
@Entity(name = "User", id = "localDomainEntity_8f9a2b3c-4d5e-6f7a-8b9c-0d1e2f3a4b5c", primaryKey = "Account ID", options = {}, supportStore = false)
public class User {

    /** The unique Atlassian account ID of the user */
    @Field.Text(name = "Account ID", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public String accountID;

    /** The display name of the user as shown in Confluence */
    @Field.Text(name = "Display Name", required = false, attributes = {@Attribute(name = "visualWidth", value = "M")}, options = {})
    public String displayName;

    /** The email address associated with the user's account */
    @Field.Text(name = "Email", required = false, attributes = {@Attribute(name = "visualWidth", value = "M")}, options = {})
    public String email;

    /** The public name of the user visible to other users */
    @Field.Text(name = "Public Name", required = false, attributes = {@Attribute(name = "visualWidth", value = "M")}, options = {})
    public String publicName;

    /** The type of account (e.g., "atlassian", "app", "customer") */
    @Field.Text(name = "Account Type", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public String accountType;

    /** The current status of the account (e.g., "active", "inactive", "closed") */
    @Field.Text(name = "Account Status", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public String accountStatus;

    /** The timezone setting for the user (e.g., "America/New_York") */
    @Field.Text(name = "Time Zone", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public String timeZone;

    /** The locale setting for the user (e.g., "en_US", "fr_FR") */
    @Field.Text(name = "Locale", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public String locale;

    /** Indicates whether the user is an external collaborator */
    @Field.Boolean(name = "Is External Collaborator", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public Boolean isExternalCollaborator;

    /** Indicates whether the user is a guest user */
    @Field.Boolean(name = "Is Guest", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public Boolean isGuest;

    /** The path to the user's profile picture (e.g., "/wiki/aa-avatar/...") */
    @Field.Text(name = "Profile Picture Path", required = false, attributes = {@Attribute(name = "visualWidth", value = "M")}, options = {})
    public String profilePicturePath;

    @Override
    public String toString() {
        return "User{" +
                "accountID='" + accountID + '\'' +
                ", displayName='" + displayName + '\'' +
                ", email='" + email + '\'' +
                ", accountType='" + accountType + '\'' +
                ", accountStatus='" + accountStatus + '\'' +
                '}';
    }
}

