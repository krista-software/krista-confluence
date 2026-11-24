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

public class Variable {
    private String placeHolderKey;
    private String placeHolderValue;

    private Variable(PlaceHolderBuilder builder) {
        this.placeHolderKey = builder.placeHolderKey;
        this.placeHolderValue = builder.placeHolderValue;
    }

    public String getPlaceHolderKey() {
        return placeHolderKey;
    }

    public String getPlaceHolderValue() {
        return placeHolderValue;
    }

    public static PlaceHolderBuilder builder() {
        return new PlaceHolderBuilder();
    }

    public static class PlaceHolderBuilder {
        private String placeHolderKey;
        private String placeHolderValue;

        public PlaceHolderBuilder() {
            // Set default values if needed
        }

        public PlaceHolderBuilder placeHolderKey(String placeHolderKey) {
            this.placeHolderKey = placeHolderKey;
            return this;
        }

        public PlaceHolderBuilder placeHolderValue(String placeHolderValue) {
            this.placeHolderValue = placeHolderValue;
            return this;
        }

        public Variable build() {
            return new Variable(this);
        }
    }
}