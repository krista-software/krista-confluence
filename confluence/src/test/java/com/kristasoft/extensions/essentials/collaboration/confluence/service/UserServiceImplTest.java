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

package com.kristasoft.extensions.essentials.collaboration.confluence.service;

import com.kristasoft.extensions.essentials.collaboration.confluence.ConfluenceAttributes;
import com.kristasoft.extensions.essentials.collaboration.confluence.client.ConfluenceRESTClient;
import com.kristasoft.extensions.essentials.collaboration.confluence.dto.User;
import com.kristasoft.extensions.essentials.collaboration.confluence.exception.ConfluenceRESTException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.HttpURLConnection;
import java.net.http.HttpResponse;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private ConfluenceAttributes confluenceAttributes;

    @Mock
    private ConfluenceRESTClient restClient;

    @Mock
    private HttpResponse<String> httpResponse;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        when(confluenceAttributes.getRestClient()).thenReturn(restClient);
        userService = new UserServiceImpl(confluenceAttributes);
    }

    @Test
    void testGetUserByAccountId_Success() {
        // Arrange
        String accountId = "5d5f9fbf1234567890abcdef";
        String responseBody = """
                {
                  "type": "known",
                  "accountId": "5d5f9fbf1234567890abcdef",
                  "accountType": "atlassian",
                  "email": "test@example.com",
                  "publicName": "Test User",
                  "timeZone": "America/New_York",
                  "profilePicture": {
                    "path": "/wiki/aa-avatar/5d5f9fbf1234567890abcdef",
                    "width": 48,
                    "height": 48,
                    "isDefault": false
                  },
                  "displayName": "Test User",
                  "isExternalCollaborator": false,
                  "isGuest": false,
                  "locale": "en_US",
                  "accountStatus": "active"
                }
                """;
        
        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse.body()).thenReturn(responseBody);
        when(restClient.doSyncGet(isNull(), eq(Map.of("accountId", accountId)), 
                eq("wiki"), eq("rest"), eq("api"), eq("user")))
                .thenReturn(httpResponse);

        // Act
        User user = userService.getUserByAccountId(accountId);

        // Assert
        assertNotNull(user);
        assertEquals("5d5f9fbf1234567890abcdef", user.getAccountId());
        assertEquals("Test User", user.getDisplayName());
        assertEquals("test@example.com", user.getEmail());
        assertEquals("Test User", user.getPublicName());
        assertEquals("atlassian", user.getAccountType());
        assertEquals("active", user.getAccountStatus());
        assertEquals("America/New_York", user.getTimeZone());
        assertEquals("en_US", user.getLocale());
        assertEquals(false, user.getIsExternalCollaborator());
        assertEquals(false, user.getIsGuest());
        assertNotNull(user.getProfilePicture());
        assertEquals("/wiki/aa-avatar/5d5f9fbf1234567890abcdef", user.getProfilePicture().getPath());
        
        verify(restClient, times(1)).doSyncGet(isNull(), eq(Map.of("accountId", accountId)), 
                eq("wiki"), eq("rest"), eq("api"), eq("user"));
    }

    @Test
    void testGetUserByAccountId_NotFound() {
        // Arrange
        String accountId = "invalid-account-id";
        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_NOT_FOUND);
        when(restClient.doSyncGet(isNull(), eq(Map.of("accountId", accountId)),
                eq("wiki"), eq("rest"), eq("api"), eq("user")))
                .thenReturn(httpResponse);

        // Act & Assert
        ConfluenceRESTException exception = assertThrows(ConfluenceRESTException.class, () -> {
            userService.getUserByAccountId(accountId);
        });

        assertTrue(exception.getMessage().contains("We couldn't find the user"));
        assertTrue(exception.getMessage().contains(accountId));
        assertTrue(exception.getMessage().contains("404"));
        
        verify(restClient, times(1)).doSyncGet(isNull(), eq(Map.of("accountId", accountId)), 
                eq("wiki"), eq("rest"), eq("api"), eq("user"));
    }

    @Test
    void testGetUserByAccountId_ServerError() {
        // Arrange
        String accountId = "5d5f9fbf1234567890abcdef";
        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_INTERNAL_ERROR);
        when(restClient.doSyncGet(isNull(), eq(Map.of("accountId", accountId)),
                eq("wiki"), eq("rest"), eq("api"), eq("user")))
                .thenReturn(httpResponse);

        // Act & Assert
        ConfluenceRESTException exception = assertThrows(ConfluenceRESTException.class, () -> {
            userService.getUserByAccountId(accountId);
        });

        assertTrue(exception.getMessage().contains("We couldn't find the user"));
        assertTrue(exception.getMessage().contains("500"));
        
        verify(restClient, times(1)).doSyncGet(isNull(), eq(Map.of("accountId", accountId)), 
                eq("wiki"), eq("rest"), eq("api"), eq("user"));
    }

    @Test
    void testGetUserByAccountId_WithMinimalData() {
        // Arrange
        String accountId = "5d5f9fbf1234567890abcdef";
        String responseBody = """
                {
                  "accountId": "5d5f9fbf1234567890abcdef",
                  "displayName": "Minimal User"
                }
                """;
        
        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse.body()).thenReturn(responseBody);
        when(restClient.doSyncGet(isNull(), eq(Map.of("accountId", accountId)), 
                eq("wiki"), eq("rest"), eq("api"), eq("user")))
                .thenReturn(httpResponse);

        // Act
        User user = userService.getUserByAccountId(accountId);

        // Assert
        assertNotNull(user);
        assertEquals("5d5f9fbf1234567890abcdef", user.getAccountId());
        assertEquals("Minimal User", user.getDisplayName());
        // Other fields should be null
        assertNull(user.getEmail());
        assertNull(user.getAccountType());
    }
}

