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

public class Storage {
    private String value;
    private String representation;

    private Storage(StorageBuilder builder) {
        this.value = builder.value;
        this.representation = builder.representation;
    }

    public static StorageBuilder builder() {
        return new StorageBuilder();
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getRepresentation() {
        return representation;
    }

    public void setRepresentation(String representation) {
        this.representation = representation;
    }

    public static class StorageBuilder {
        private String value;
        private String representation;

        public StorageBuilder() {
        }

        public Storage build() {
            return new Storage(this);
        }

        public StorageBuilder setValue(String value) {
            this.value = value;
            return this;
        }

        public StorageBuilder setRepresentation(String representation) {
            this.representation = representation;
            return this;
        }
    }

    @Override
    public String toString() {
        return "Storage{" +
                "value='" + value + '\'' +
                ", representation='" + representation + '\'' +
                '}';
    }
}