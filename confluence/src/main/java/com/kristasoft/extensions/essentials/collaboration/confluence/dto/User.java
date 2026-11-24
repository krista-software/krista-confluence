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

package com.kristasoft.extensions.essentials.collaboration.confluence.dto;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

/**
 * Confluence User DTO representing a user from the Confluence API.
 * Maps to the response from /wiki/rest/api/user endpoint.
 */
public class User {
    private String type;
    private String accountId;
    private String accountType;
    private String email;
    private String publicName;
    private String timeZone;
    private ProfilePicture profilePicture;
    private String displayName;
    private Boolean isExternalCollaborator;
    private Boolean isGuest;
    private String locale;
    private String accountStatus;
    
    @SerializedName("_expandable")
    private Map<String, String> expandable;
    
    @SerializedName("_links")
    private Map<String, String> links;

    /**
     * Default no-argument constructor required by Gson for JSON deserialization.
     * Gson uses reflection to create instances and populate fields directly,
     * so this constructor intentionally has no initialization logic.
     */
    public User() {
        // Empty constructor - Gson handles field initialization via reflection
    }

    // Getters and Setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPublicName() {
        return publicName;
    }

    public void setPublicName(String publicName) {
        this.publicName = publicName;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public ProfilePicture getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(ProfilePicture profilePicture) {
        this.profilePicture = profilePicture;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Boolean getIsExternalCollaborator() {
        return isExternalCollaborator;
    }

    public void setIsExternalCollaborator(Boolean isExternalCollaborator) {
        this.isExternalCollaborator = isExternalCollaborator;
    }

    public Boolean getIsGuest() {
        return isGuest;
    }

    public void setIsGuest(Boolean isGuest) {
        this.isGuest = isGuest;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(String accountStatus) {
        this.accountStatus = accountStatus;
    }

    public Map<String, String> getExpandable() {
        return expandable;
    }

    public void setExpandable(Map<String, String> expandable) {
        this.expandable = expandable;
    }

    public Map<String, String> getLinks() {
        return links;
    }

    public void setLinks(Map<String, String> links) {
        this.links = links;
    }

    @Override
    public String toString() {
        return "User{" +
                "type='" + type + '\'' +
                ", accountId='" + accountId + '\'' +
                ", accountType='" + accountType + '\'' +
                ", email='" + email + '\'' +
                ", publicName='" + publicName + '\'' +
                ", displayName='" + displayName + '\'' +
                ", accountStatus='" + accountStatus + '\'' +
                '}';
    }
}

