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

import app.krista.extensions.util.KeyValueStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionManagerTest {

    @Mock
    private KeyValueStore keyValueStore;

    private SessionManager sessionManager;

    @BeforeEach
    void setUp() {
        sessionManager = new SessionManager(keyValueStore);
    }

    @Test
    void testStoreRefToken() {
        String key = "testKey";
        String token = "testRefreshToken123";

        sessionManager.storeRefToken(key, token);

        verify(keyValueStore).put(key, "\"testRefreshToken123\"");
    }

    @Test
    void testGetRefToken_WhenTokenExists() {
        String key = "testKey";
        String expectedToken = "testRefreshToken123";
        when(keyValueStore.get(key, String.class)).thenReturn(expectedToken);

        String result = sessionManager.getRefToken(key);

        assertEquals(expectedToken, result);
        verify(keyValueStore).get(key, String.class);
    }

    @Test
    void testGetRefToken_WhenTokenDoesNotExist() {
        String key = "nonExistentKey";
        when(keyValueStore.get(key, String.class)).thenReturn(null);

        String result = sessionManager.getRefToken(key);

        assertNull(result);
        verify(keyValueStore).get(key, String.class);
    }

    @Test
    void testStoreAndRetrieveRefToken() {
        String key = "clientId#clientSecret#invokerId";
        String token = "refreshToken_abc123";

        sessionManager.storeRefToken(key, token);
        when(keyValueStore.get(key, String.class)).thenReturn(token);

        String retrieved = sessionManager.getRefToken(key);

        assertEquals(token, retrieved);
        verify(keyValueStore).put(key, "\"refreshToken_abc123\"");
        verify(keyValueStore).get(key, String.class);
    }

    @Test
    void testStoreRefToken_OverwriteExistingToken() {
        String key = "sameKey";
        String oldToken = "oldToken";
        String newToken = "newToken";

        sessionManager.storeRefToken(key, oldToken);
        sessionManager.storeRefToken(key, newToken);

        verify(keyValueStore).put(key, "\"oldToken\"");
        verify(keyValueStore).put(key, "\"newToken\"");
    }

    @Test
    void testStoreRefToken_WithNullToken() {
        String key = "testKey";

        sessionManager.storeRefToken(key, null);

        verify(keyValueStore).put(key, "null");
    }

    @Test
    void testGetRefToken_WithCompositeKey() {
        String compositeKey = "accountId#invokerId";
        String expectedToken = "compositeRefreshToken";
        when(keyValueStore.get(compositeKey, String.class)).thenReturn(expectedToken);

        String result = sessionManager.getRefToken(compositeKey);

        assertEquals(expectedToken, result);
    }

    @Test
    void testStoreRefToken_WithAppWideKey() {
        String appKey = "clientId#clientSecret#invokerId";
        String token = "appWideRefreshToken";

        sessionManager.storeRefToken(appKey, token);

        verify(keyValueStore).put(appKey, "\"appWideRefreshToken\"");
    }

    @Test
    void testStoreRefToken_WithUserSpecificKey() {
        String userKey = "userId#invokerId";
        String token = "userSpecificRefreshToken";

        sessionManager.storeRefToken(userKey, token);

        verify(keyValueStore).put(userKey, "\"userSpecificRefreshToken\"");
    }
}
