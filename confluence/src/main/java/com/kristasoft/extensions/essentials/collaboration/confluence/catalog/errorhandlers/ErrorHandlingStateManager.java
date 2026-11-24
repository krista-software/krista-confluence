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

package com.kristasoft.extensions.essentials.collaboration.confluence.catalog.errorhandlers;

import app.krista.extensions.util.KeyValueStore;
import com.kristasoft.extensions.essentials.collaboration.confluence.utils.Constants;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.util.Map;

@Service
public class ErrorHandlingStateManager {

    private final KeyValueStore entries;

    @Inject
    public ErrorHandlingStateManager(KeyValueStore internalState) {
        this.entries = internalState;
    }

    public void put(String key, Map<String, Object> value) {
        Object keystoreValue = Constants.GSON.toJson(value);
        entries.put(key, keystoreValue);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> get(String key) {
        final String value = String.valueOf(entries.get(key));
        return Constants.GSON_OBJECT.fromJson(value, Map.class);
    }
}