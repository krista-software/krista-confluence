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

public class Version {
    private int number;
    private String message;
    private String createdAt;


    public Version(VersionBuilder builder) {
        this.number = builder.number;
        this.message = builder.message;
        this.createdAt = builder.createdAt;

    }

    public static VersionBuilder builder() {
        return new VersionBuilder();
    }


    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }


    public static class VersionBuilder {
        private int number;
        private String message;
        private String createdAt;

        public VersionBuilder() {
        }

        public VersionBuilder(int number, String message) {
            this.number = number;
            this.message = message;
        }

        public VersionBuilder withNumber(int number) {
            this.number = number;
            return this;
        }

        public VersionBuilder withMessage(String message) {
            this.message = message;
            return this;
        }

        public VersionBuilder withCreatedAt(String createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Version build() {
            return new Version(this);
        }
    }
}
