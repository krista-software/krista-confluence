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

import java.util.List;

public class Template {
    private String templateId;
    private String name;
    private String description;
    private String spaceId;
    private String spaceKey;
    private List<String> labels;
    private String templateType;
    private String editorVersion;
    private Body body;

    private Template(TemplateBuilder builder) {
        this.templateId = builder.templateId;
        this.name = builder.name;
        this.description = builder.description;
        this.spaceId = builder.spaceId;
        this.spaceKey = builder.spaceKey;
        this.labels = builder.labels;
        this.templateType = builder.templateType;
        this.editorVersion = builder.editorVersion;
        this.body = builder.body;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSpaceId() {
        return spaceId;
    }

    public void setSpaceId(String spaceId) {
        this.spaceId = spaceId;
    }

    public String getSpaceKey() {
        return spaceKey;
    }

    public void setSpaceKey(String spaceKey) {
        this.spaceKey = spaceKey;
    }

    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
    }

    public String getTemplateType() {
        return templateType;
    }

    public void setTemplateType(String templateType) {
        this.templateType = templateType;
    }

    public String getEditorVersion() {
        return editorVersion;
    }

    public void setEditorVersion(String editorVersion) {
        this.editorVersion = editorVersion;
    }

    public Body getBody() {
        return body;
    }

    public void setBody(Body body) {
        this.body = body;
    }

    public static TemplateBuilder builder() {
        return new TemplateBuilder();
    }

    public static class TemplateBuilder {
        private String templateId;
        private String name;
        private String description;
        private String spaceId;
        private String spaceKey;
        private List<String> labels;
        private String templateType;
        private String editorVersion;
        private Body body;

        public TemplateBuilder() {
        }

        public Template build() {
            return new Template(this);
        }

        public static TemplateBuilder builder() {
            return new TemplateBuilder();
        }

        public TemplateBuilder setTemplateId(String templateId) {
            this.templateId = templateId;
            return this;
        }

        public TemplateBuilder setName(String name) {
            this.name = name;
            return this;
        }

        public TemplateBuilder setDescription(String description) {
            this.description = description;
            return this;
        }

        public TemplateBuilder setSpaceId(String spaceId) {
            this.spaceId = spaceId;
            return this;
        }

        public TemplateBuilder setSpaceKey(String spaceKey) {
            this.spaceKey = spaceKey;
            return this;
        }

        public TemplateBuilder setLabels(List<String> labels) {
            this.labels = labels;
            return this;
        }

        public TemplateBuilder setTemplateType(String templateType) {
            this.templateType = templateType;
            return this;
        }

        public TemplateBuilder setEditorVersion(String editorVersion) {
            this.editorVersion = editorVersion;
            return this;
        }

        public TemplateBuilder setBody(Body body) {
            this.body = body;
            return this;
        }
    }

    @Override
    public String toString() {
        return "Template{" +
                "templateId='" + templateId + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", spaceId='" + spaceId + '\'' +
                ", spaceKey='" + spaceKey + '\'' +
                ", labels=" + labels +
                ", templateType='" + templateType + '\'' +
                ", editorVersion='" + editorVersion + '\'' +
                ", body=" + body +
                '}';
    }
}


