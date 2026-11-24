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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link Template} and its inner {@link Template.TemplateBuilder}.
 *
 * Covers: TemplateBuilder (all setter methods, static builder(), build()),
 *         Template static builder(), all getters/setters, toString().
 */
@DisplayName("Template DTO")
class TemplateTest {

    // ── Helper ──

    private Body createBody() {
        Storage storage = Storage.builder().setValue("<p>body</p>").setRepresentation("storage").build();
        return Body.setBody(storage);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Static factory: builder()
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Template.builder() returns a non-null TemplateBuilder")
    void builder_returnsNonNullBuilder() {
        assertNotNull(Template.builder());
    }

    @Test
    @DisplayName("TemplateBuilder.builder() static method returns a non-null TemplateBuilder")
    void templateBuilderStaticBuilder_returnsNonNullBuilder() {
        assertNotNull(Template.TemplateBuilder.builder());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  TemplateBuilder tests
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("TemplateBuilder")
    class TemplateBuilderTest {

        @Test
        @DisplayName("build with all fields populates Template correctly")
        void build_withAllFields() {
            List<String> labels = Arrays.asList("label1", "label2");
            Body body = createBody();

            Template template = Template.builder()
                    .setTemplateId("tmpl-1")
                    .setName("Meeting Notes")
                    .setDescription("Template for meeting notes")
                    .setSpaceId("space-10")
                    .setSpaceKey("MN")
                    .setLabels(labels)
                    .setTemplateType("page")
                    .setEditorVersion("v2")
                    .setBody(body)
                    .build();

            assertNotNull(template);
            assertEquals("tmpl-1", template.getTemplateId());
            assertEquals("Meeting Notes", template.getName());
            assertEquals("Template for meeting notes", template.getDescription());
            assertEquals("space-10", template.getSpaceId());
            assertEquals("MN", template.getSpaceKey());
            assertSame(labels, template.getLabels());
            assertEquals("page", template.getTemplateType());
            assertEquals("v2", template.getEditorVersion());
            assertSame(body, template.getBody());
        }

        @Test
        @DisplayName("build with no fields leaves all values null")
        void build_withNoFields_allNull() {
            Template template = Template.builder().build();

            assertNotNull(template);
            assertNull(template.getTemplateId());
            assertNull(template.getName());
            assertNull(template.getDescription());
            assertNull(template.getSpaceId());
            assertNull(template.getSpaceKey());
            assertNull(template.getLabels());
            assertNull(template.getTemplateType());
            assertNull(template.getEditorVersion());
            assertNull(template.getBody());
        }

        @Test
        @DisplayName("builder setters return the same builder instance (fluent API)")
        void builderSetters_returnSameInstance() {
            Template.TemplateBuilder builder = Template.builder();
            List<String> labels = Arrays.asList("a");
            Body body = createBody();

            assertSame(builder, builder.setTemplateId("id"));
            assertSame(builder, builder.setName("name"));
            assertSame(builder, builder.setDescription("desc"));
            assertSame(builder, builder.setSpaceId("sid"));
            assertSame(builder, builder.setSpaceKey("key"));
            assertSame(builder, builder.setLabels(labels));
            assertSame(builder, builder.setTemplateType("type"));
            assertSame(builder, builder.setEditorVersion("ev"));
            assertSame(builder, builder.setBody(body));
        }

        @Test
        @DisplayName("no-arg TemplateBuilder constructor creates a usable builder")
        void noArgConstructor_createsUsableBuilder() {
            Template.TemplateBuilder builder = new Template.TemplateBuilder();
            Template template = builder.setName("test").build();

            assertEquals("test", template.getName());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Setter tests on Template
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Setters")
    class SetterTests {

        @Test
        @DisplayName("setTemplateId updates the templateId")
        void setTemplateId_updatesField() {
            Template template = Template.builder().setTemplateId("old").build();
            template.setTemplateId("new-id");

            assertEquals("new-id", template.getTemplateId());
        }

        @Test
        @DisplayName("setName updates the name")
        void setName_updatesField() {
            Template template = Template.builder().build();
            template.setName("New Name");

            assertEquals("New Name", template.getName());
        }

        @Test
        @DisplayName("setDescription updates the description")
        void setDescription_updatesField() {
            Template template = Template.builder().build();
            template.setDescription("New Description");

            assertEquals("New Description", template.getDescription());
        }

        @Test
        @DisplayName("setSpaceId updates the spaceId")
        void setSpaceId_updatesField() {
            Template template = Template.builder().build();
            template.setSpaceId("new-space");

            assertEquals("new-space", template.getSpaceId());
        }

        @Test
        @DisplayName("setSpaceKey updates the spaceKey")
        void setSpaceKey_updatesField() {
            Template template = Template.builder().build();
            template.setSpaceKey("NSK");

            assertEquals("NSK", template.getSpaceKey());
        }

        @Test
        @DisplayName("setLabels updates the labels list")
        void setLabels_updatesField() {
            Template template = Template.builder().build();
            List<String> labels = Arrays.asList("x", "y", "z");
            template.setLabels(labels);

            assertSame(labels, template.getLabels());
            assertEquals(3, template.getLabels().size());
        }

        @Test
        @DisplayName("setTemplateType updates the templateType")
        void setTemplateType_updatesField() {
            Template template = Template.builder().build();
            template.setTemplateType("blog");

            assertEquals("blog", template.getTemplateType());
        }

        @Test
        @DisplayName("setEditorVersion updates the editorVersion")
        void setEditorVersion_updatesField() {
            Template template = Template.builder().build();
            template.setEditorVersion("v1");

            assertEquals("v1", template.getEditorVersion());
        }

        @Test
        @DisplayName("setBody updates the body")
        void setBody_updatesField() {
            Template template = Template.builder().build();
            Body body = createBody();
            template.setBody(body);

            assertSame(body, template.getBody());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  toString
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("toString with populated fields contains all values")
    void toString_withPopulatedFields() {
        List<String> labels = Arrays.asList("label1");

        Template template = Template.builder()
                .setTemplateId("tmpl-1")
                .setName("Name")
                .setDescription("Desc")
                .setSpaceId("sid")
                .setSpaceKey("SK")
                .setLabels(labels)
                .setTemplateType("page")
                .setEditorVersion("v2")
                .build();

        String result = template.toString();

        assertNotNull(result);
        assertTrue(result.startsWith("Template{"));
        assertTrue(result.contains("templateId='tmpl-1'"));
        assertTrue(result.contains("name='Name'"));
        assertTrue(result.contains("description='Desc'"));
        assertTrue(result.contains("spaceId='sid'"));
        assertTrue(result.contains("spaceKey='SK'"));
        assertTrue(result.contains("labels=[label1]"));
        assertTrue(result.contains("templateType='page'"));
        assertTrue(result.contains("editorVersion='v2'"));
        assertTrue(result.endsWith("}"));
    }

    @Test
    @DisplayName("toString with null fields contains null literals")
    void toString_withNullFields() {
        Template template = Template.builder().build();

        String result = template.toString();

        assertNotNull(result);
        assertTrue(result.startsWith("Template{"));
        assertTrue(result.contains("null"));
        assertTrue(result.endsWith("}"));
    }
}
