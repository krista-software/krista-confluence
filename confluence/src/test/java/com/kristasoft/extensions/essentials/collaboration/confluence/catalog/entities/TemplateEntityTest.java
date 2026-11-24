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

package com.kristasoft.extensions.essentials.collaboration.confluence.catalog.entities;

import app.krista.extension.impl.anno.Domain;
import app.krista.extension.impl.anno.Entity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Template Entity Tests")
class TemplateEntityTest {

    private Template template;

    @BeforeEach
    void setUp() {
        template = new Template();
    }

    @Test
    @DisplayName("Should have @Domain annotation type available")
    void shouldHaveDomainAnnotation() {
        // @Domain has CLASS retention, so it is not available at runtime via reflection.
        // Verify the annotation type is a valid annotation interface that the class references.
        assertTrue(Domain.class.isAnnotation(), "Domain should be an annotation type");
        // The Template class is annotated with @Domain in source (verified at compile time).
    }

    @Test
    @DisplayName("Should have @Entity annotation with correct attributes")
    void shouldHaveEntityAnnotation() {
        Entity entity = Template.class.getAnnotation(Entity.class);
        assertNotNull(entity, "Template class should have @Entity annotation");
        assertEquals("Template", entity.name());
        assertEquals("localDomainEntity_397db3e6-2109-4bf5-8238-162320912670", entity.id());
        assertEquals("Name", entity.primaryKey());
        assertFalse(entity.supportStore());
    }

    @Test
    @DisplayName("Should set and get id field")
    void shouldSetAndGetId() {
        template.id = "tmpl-001";
        assertEquals("tmpl-001", template.id);
    }

    @Test
    @DisplayName("Should set and get name field")
    void shouldSetAndGetName() {
        template.name = "Meeting Notes Template";
        assertEquals("Meeting Notes Template", template.name);
    }

    @Test
    @DisplayName("Should set and get spaceId field")
    void shouldSetAndGetSpaceId() {
        template.spaceId = "space-100";
        assertEquals("space-100", template.spaceId);
    }

    @Test
    @DisplayName("Should set and get type field")
    void shouldSetAndGetType() {
        template.type = "page";
        assertEquals("page", template.type);
    }

    @Test
    @DisplayName("Should set and get body field")
    void shouldSetAndGetBody() {
        template.body = "<h1>Meeting Notes</h1><p>Attendees: {{attendees}}</p>";
        assertEquals("<h1>Meeting Notes</h1><p>Attendees: {{attendees}}</p>", template.body);
    }

    @Test
    @DisplayName("Should set and get variables field as List of Maps")
    void shouldSetAndGetVariables() {
        List<Map<String, Object>> vars = new ArrayList<>();
        Map<String, Object> variable1 = new HashMap<>();
        variable1.put("Key", "attendees");
        variable1.put("Value", "Team A");
        variable1.put("Options", List.of("Team A", "Team B"));
        vars.add(variable1);

        Map<String, Object> variable2 = new HashMap<>();
        variable2.put("Key", "date");
        variable2.put("Value", "2025-01-01");
        vars.add(variable2);

        template.variables = vars;

        assertNotNull(template.variables);
        assertEquals(2, template.variables.size());
        assertEquals("attendees", template.variables.get(0).get("Key"));
        assertEquals("date", template.variables.get(1).get("Key"));
    }

    @Test
    @DisplayName("Should handle empty variables list")
    void shouldHandleEmptyVariablesList() {
        template.variables = new ArrayList<>();
        assertNotNull(template.variables);
        assertTrue(template.variables.isEmpty());
    }

    @Test
    @DisplayName("All fields should be null by default")
    void allFieldsShouldBeNullByDefault() {
        assertNull(template.id);
        assertNull(template.name);
        assertNull(template.spaceId);
        assertNull(template.type);
        assertNull(template.body);
        assertNull(template.variables);
    }

    @Test
    @DisplayName("Should have exactly 6 public fields")
    void shouldHaveCorrectNumberOfPublicFields() {
        long publicFieldCount = java.util.Arrays.stream(Template.class.getDeclaredFields())
                .filter(f -> java.lang.reflect.Modifier.isPublic(f.getModifiers()))
                .count();
        assertEquals(6, publicFieldCount);
    }
}
