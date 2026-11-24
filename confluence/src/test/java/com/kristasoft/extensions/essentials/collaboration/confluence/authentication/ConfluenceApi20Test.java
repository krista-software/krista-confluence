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

package com.kristasoft.extensions.essentials.collaboration.confluence.authentication;

import com.github.scribejava.core.extractors.OAuth2AccessTokenJsonExtractor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfluenceApi20Test {

    @Test
    void testInstanceReturnsSameInstance() {
        ConfluenceApi20 first = ConfluenceApi20.instance();
        ConfluenceApi20 second = ConfluenceApi20.instance();

        assertNotNull(first);
        assertSame(first, second, "Singleton should return the same instance");
    }

    @Test
    void testGetAccessTokenEndpoint() {
        ConfluenceApi20 api = ConfluenceApi20.instance();

        assertEquals("https://auth.atlassian.com/oauth/token", api.getAccessTokenEndpoint());
    }

    @Test
    void testGetAccessTokenExtractor() {
        ConfluenceApi20 api = ConfluenceApi20.instance();

        assertNotNull(api.getAccessTokenExtractor());
        assertInstanceOf(OAuth2AccessTokenJsonExtractor.class, api.getAccessTokenExtractor());
    }

    @Test
    void testGetAccessTokenExtractorReturnsSingleton() {
        ConfluenceApi20 api = ConfluenceApi20.instance();

        assertSame(api.getAccessTokenExtractor(), OAuth2AccessTokenJsonExtractor.instance(),
                "Should return the singleton OAuth2AccessTokenJsonExtractor instance");
    }

    @Test
    void testGetAuthorizationBaseUrl_ViaFullSignature() {
        ConfluenceApi20 api = ConfluenceApi20.instance();

        // getAuthorizationBaseUrl is protected; test through the 6-arg public method on DefaultApi20
        String authUrl = api.getAuthorizationUrl("responseType", "apiKey", "callback", "scope", "state", null);
        assertNotNull(authUrl);
        assertTrue(authUrl.startsWith("https://auth.atlassian.com/authorize"),
                "Authorization URL should start with Atlassian auth base URL");
    }

    @Test
    void testGetAuthorizationUrl_ContainsState() {
        ConfluenceApi20 api = ConfluenceApi20.instance();

        String authUrl = api.getAuthorizationUrl("code", "myKey", "http://cb", "read", "my-state", null);
        assertTrue(authUrl.contains("state=my-state"),
                "Authorization URL should contain the state parameter");
    }

    @Test
    void testGetAuthorizationUrl_ContainsResponseType() {
        ConfluenceApi20 api = ConfluenceApi20.instance();

        String authUrl = api.getAuthorizationUrl("code", "myKey", "http://cb", "read", "st", null);
        assertTrue(authUrl.contains("response_type=code"),
                "Authorization URL should contain response_type");
    }
}
