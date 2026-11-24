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

package com.kristasoft.extensions.essentials.collaboration.confluence.catalog;

import app.krista.extension.executor.ExtensionResponse;
import app.krista.ksdk.context.RequestContext;
import com.kristasoft.extensions.essentials.collaboration.confluence.api.ConfluenceAuditService;
import com.kristasoft.extensions.essentials.collaboration.confluence.dto.ProfilePicture;
import com.kristasoft.extensions.essentials.collaboration.confluence.exception.ConfluenceRESTException;
import com.kristasoft.extensions.essentials.collaboration.confluence.service.ConfluenceService;
import com.kristasoft.extensions.essentials.collaboration.confluence.utils.TelemetryHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserArea Tests")
class UserAreaTest {

    @Mock
    private ConfluenceService confluenceService;

    @Mock
    private TelemetryHelper telemetryHelper;

    @Mock
    private ConfluenceAuditService confluenceAuditService;

    @Mock
    private RequestContext requestContext;

    private UserArea userArea;

    @BeforeEach
    void setUp() {
        userArea = new UserArea(confluenceService, telemetryHelper, confluenceAuditService, requestContext);
    }

    @Test
    @DisplayName("Should retrieve user successfully by account ID")
    void testGetUserByAccountID_Success() {
        // Arrange
        String accountId = "5d5f9fbf1234567890abcdef";
        com.kristasoft.extensions.essentials.collaboration.confluence.dto.User userDto = createUserDto(accountId);

        when(confluenceService.getUserByAccountId(accountId)).thenReturn(userDto);

        // Act
        ExtensionResponse response = userArea.getUserByAccountID(accountId);

        // Assert
        assertNotNull(response);
        assertEquals(ExtensionResponse.Result.SUCCESS, response.getResult());

        verify(confluenceService, times(1)).getUserByAccountId(accountId);
        verify(telemetryHelper, times(1)).incrementCount("confluence.getUserByAccountID");
        verify(telemetryHelper, times(1)).recordSuccess(eq("confluence.getUserByAccountID"), anyLong(), anyMap());
    }

    @Test
    @DisplayName("Should return error when account ID is null")
    void testGetUserByAccountID_NullAccountId() {
        // Act
        ExtensionResponse response = userArea.getUserByAccountID(null);

        // Assert
        assertNotNull(response);
        assertEquals(ExtensionResponse.Result.FAILURE, response.getResult());
        assertNotNull(response.getError());
        assertEquals(ExtensionResponse.Error.ExceptionType.INPUT_ERROR, response.getError().getExceptionType());
        assertTrue(response.getError().getErrorMessage().contains("Account ID is required"));
        
        verify(confluenceService, never()).getUserByAccountId(anyString());
        verify(telemetryHelper, times(1)).incrementCount("confluence.getUserByAccountID");
        verify(telemetryHelper, times(1)).recordError(eq("confluence.getUserByAccountID"), anyLong(), any(), anyMap());
    }

    @Test
    @DisplayName("Should return error when account ID is empty")
    void testGetUserByAccountID_EmptyAccountId() {
        // Act
        ExtensionResponse response = userArea.getUserByAccountID("   ");

        // Assert
        assertNotNull(response);
        assertEquals(ExtensionResponse.Result.FAILURE, response.getResult());
        assertNotNull(response.getError());
        assertEquals(ExtensionResponse.Error.ExceptionType.INPUT_ERROR, response.getError().getExceptionType());
        assertTrue(response.getError().getErrorMessage().contains("Account ID is required"));
        
        verify(confluenceService, never()).getUserByAccountId(anyString());
        verify(telemetryHelper, times(1)).incrementCount("confluence.getUserByAccountID");
        verify(telemetryHelper, times(1)).recordError(eq("confluence.getUserByAccountID"), anyLong(), any(), anyMap());
    }

    @Test
    @DisplayName("Should handle service exception gracefully")
    void testGetUserByAccountID_ServiceException() {
        // Arrange
        String accountId = "5d5f9fbf1234567890abcdef";
        ConfluenceRESTException exception = new ConfluenceRESTException("User not found");
        
        when(confluenceService.getUserByAccountId(accountId)).thenThrow(exception);

        // Act
        ExtensionResponse response = userArea.getUserByAccountID(accountId);

        // Assert
        assertNotNull(response);
        assertEquals(ExtensionResponse.Result.FAILURE, response.getResult());
        assertNotNull(response.getError());
        assertEquals(ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR, response.getError().getExceptionType());
        assertTrue(response.getError().getErrorMessage().contains("We couldn't retrieve the user information"));
        assertTrue(response.getError().getErrorMessage().contains("User not found"));
        
        verify(confluenceService, times(1)).getUserByAccountId(accountId);
        verify(telemetryHelper, times(1)).incrementCount("confluence.getUserByAccountID");
        verify(telemetryHelper, times(1)).recordError(eq("confluence.getUserByAccountID"), anyLong(), eq(exception), anyMap());
    }

    @Test
    @DisplayName("Should handle user with minimal data")
    void testGetUserByAccountID_MinimalData() {
        // Arrange
        String accountId = "5d5f9fbf1234567890abcdef";
        com.kristasoft.extensions.essentials.collaboration.confluence.dto.User userDto = new com.kristasoft.extensions.essentials.collaboration.confluence.dto.User();
        userDto.setAccountId(accountId);
        userDto.setDisplayName("Minimal User");
        
        when(confluenceService.getUserByAccountId(accountId)).thenReturn(userDto);

        // Act
        ExtensionResponse response = userArea.getUserByAccountID(accountId);

        // Assert
        assertNotNull(response);
        assertEquals(ExtensionResponse.Result.SUCCESS, response.getResult());

        verify(confluenceService, times(1)).getUserByAccountId(accountId);
        verify(telemetryHelper, times(1)).incrementCount("confluence.getUserByAccountID");
        verify(telemetryHelper, times(1)).recordSuccess(eq("confluence.getUserByAccountID"), anyLong(), anyMap());
    }

    // Helper method to create a complete User DTO for testing
    private com.kristasoft.extensions.essentials.collaboration.confluence.dto.User createUserDto(String accountId) {
        com.kristasoft.extensions.essentials.collaboration.confluence.dto.User user = new com.kristasoft.extensions.essentials.collaboration.confluence.dto.User();
        user.setAccountId(accountId);
        user.setDisplayName("Test User");
        user.setEmail("test@example.com");
        user.setPublicName("Test User");
        user.setAccountType("atlassian");
        user.setAccountStatus("active");
        user.setTimeZone("America/New_York");
        user.setLocale("en_US");
        user.setIsExternalCollaborator(false);
        user.setIsGuest(false);
        
        ProfilePicture profilePicture = new ProfilePicture();
        profilePicture.setPath("/wiki/aa-avatar/5d5f9fbf1234567890abcdef");
        profilePicture.setWidth(48);
        profilePicture.setHeight(48);
        profilePicture.setIsDefault(false);
        user.setProfilePicture(profilePicture);
        
        return user;
    }
}

