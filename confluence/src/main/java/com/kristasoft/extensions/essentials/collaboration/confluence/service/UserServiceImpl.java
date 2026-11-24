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
import com.kristasoft.extensions.essentials.collaboration.confluence.dto.User;
import com.kristasoft.extensions.essentials.collaboration.confluence.exception.ConfluenceRESTException;
import com.kristasoft.extensions.essentials.collaboration.confluence.utils.GsonFactory;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.net.HttpURLConnection;
import java.net.http.HttpResponse;
import java.util.Map;

/**
 * Implementation of UserService for managing Confluence users.
 * Handles user retrieval operations using Confluence REST API v1.
 */
@Service
@ContractsProvided(UserService.class)
public class UserServiceImpl implements UserService {

    private final ConfluenceAttributes confluenceAttributes;

    @Inject
    public UserServiceImpl(ConfluenceAttributes confluenceAttributes) {
        this.confluenceAttributes = confluenceAttributes;
    }

    @Override
    public User getUserByAccountId(String accountId) {
        // Note: This endpoint doesn't use APIVersion enum as it's a legacy endpoint
        HttpResponse<String> response = confluenceAttributes.getRestClient()
                .doSyncGet(null, Map.of("accountId", accountId), "wiki", "rest", "api", "user");
        
        if (response.statusCode() == HttpURLConnection.HTTP_OK) {
            return GsonFactory.getInstance().fromJson(response.body(), User.class);
        } else {
            String errorMessage = String.format("""
                    We couldn't find the user with Account ID '%s'.
                    This could mean:
                    • The Account ID doesn't exist in your Confluence instance
                    • You don't have permission to view this user's information
                    • There's a temporary connection issue

                    What to do next:
                    1. Verify the Account ID is correct
                    2. Check your Confluence permissions
                    3. Try again in a moment

                    Technical details: HTTP %d""", accountId, response.statusCode());
            throw new ConfluenceRESTException(errorMessage);
        }
    }
}

