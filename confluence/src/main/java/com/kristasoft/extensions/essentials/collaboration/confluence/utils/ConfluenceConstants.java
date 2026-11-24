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

public class ConfluenceConstants {

    private ConfluenceConstants() {
    }

    // Basic Auth attributes
    public static final String AUTHORIZATION_EMAIL = "Confluence Account Email";
    public static final String API_KEY = "API Key";
    public static final String DOMAIN_NAME = "Domain Name";

    // OAuth 2.0 attributes
    public static final String CLIENT_ID = "Client Id";
    public static final String CLIENT_SECRET = "Client Secret";
    public static final String CREDENTIALS_MODE = "Credentials Mode";
    public static final String SERVICE_ACCOUNT = "Service Account";

    // OAuth 2.0 constants
    public static final String BEARER = "Bearer ";
    public static final String REFRESH_TOKEN = "refresh_token";
    public static final String CODE = "code";
    public static final String STATE = "state";
    public static final String JAXRS_ID = "confluenceextn";
    public static final String AUTHORIZATION_PROMPT = "Authorize yourself and click Validate Attributes before saving changes to proceed further";

    // Catalog constants
    public static final String DOMAIN_ID = "catEntryDomain_1a86a3cc-0f2a-45fa-b9c0-b32487c830f8";
    public static final String KRISTA_DOMAIN_NAME = "Confluence";
    public static final String ECOSYSTEM_ID = "catEntryEcosystem_0aeef50c-bc48-4c2c-844a-948193e53e82";
    public static final String ECOSYSTEM_NAME = "Collaboration";
    public static final String ECOSYSTEM_VERSION = "77af6758-3926-41c4-9145-061944792713";
}
