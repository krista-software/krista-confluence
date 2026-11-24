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

package com.kristasoft.extensions.essentials.collaboration.confluence.dto.v1;

import com.google.gson.annotations.SerializedName;

/**
 * V1 API Page object from /rest/api/content/{pageId}/child/page endpoint.
 */
public class V1Page {
    private String id;
    private String type;
    private String status;
    private String title;
    private V1Extensions extensions;
    
    @SerializedName("_expandable")
    private V1Expandable expandable;
    
    @SerializedName("_links")
    private V1PageLinks links;

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public V1Extensions getExtensions() { return extensions; }
    public void setExtensions(V1Extensions extensions) { this.extensions = extensions; }
    
    public V1Expandable getExpandable() { return expandable; }
    public void setExpandable(V1Expandable expandable) { this.expandable = expandable; }
    
    public V1PageLinks getLinks() { return links; }
    public void setLinks(V1PageLinks links) { this.links = links; }

    public static class V1Extensions {
        private long position;
        public long getPosition() { return position; }
        public void setPosition(long position) { this.position = position; }
    }

    public static class V1Expandable {
        private String space;
        public String getSpace() { return space; }
        public void setSpace(String space) { this.space = space; }
    }

    public static class V1PageLinks {
        private String self;
        private String webui;
        private String tinyui;
        private String editui;
        private String edituiv2;

        public String getSelf() { return self; }
        public void setSelf(String self) { this.self = self; }
        
        public String getWebui() { return webui; }
        public void setWebui(String webui) { this.webui = webui; }
        
        public String getTinyui() { return tinyui; }
        public void setTinyui(String tinyui) { this.tinyui = tinyui; }
        
        public String getEditui() { return editui; }
        public void setEditui(String editui) { this.editui = editui; }
        
        public String getEdituiv2() { return edituiv2; }
        public void setEdituiv2(String edituiv2) { this.edituiv2 = edituiv2; }
    }
}
