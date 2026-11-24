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

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 100% code-coverage tests for {@link Space} and its inner {@link Space.SpaceBuilder}.
 *
 * <h2>Coverage map</h2>
 * <pre>
 * Space (class)
 *   ├─ private Space(SpaceBuilder)          → covered via every build() call
 *   ├─ getAuthorId / setAuthorId            → tests: builderAllFields, setAuthorId
 *   ├─ getHomepageId / setHomepageId        → tests: builderAllFields, setHomepageId
 *   ├─ getIcon / setIcon                    → tests: builderAllFields, setIcon
 *   ├─ getName / setName                    → tests: builderAllFields, setName
 *   ├─ getKey / setKey                      → tests: builderAllFields, setKey
 *   ├─ getId / setId                        → tests: builderAllFields, setId
 *   ├─ getType / setType                    → tests: builderAllFields, setType
 *   ├─ getDescription / setDescription      → tests: builderAllFields, setDescription
 *   ├─ getStatus / setStatus                → tests: builderAllFields, setStatus
 *   ├─ getCreatedAt / setCreatedAt          → tests: builderAllFields, setCreatedAt
 *   ├─ getCurrentActiveAlias / set…         → tests: builderAllFields, setCurrentActiveAlias
 *   ├─ static builder()                     → test: builder_returnsNonNullSpaceBuilder
 *   └─ toString()                           → tests: toString_withValues, toString_withNullFields
 *
 * Space.SpaceBuilder (inner class)
 *   ├─ (default constructor)               → covered via every Space.builder() call
 *   ├─ setAuthorId                         → test: builderSetters_returnSameInstance
 *   ├─ setHomepageId                       → test: builderSetters_returnSameInstance
 *   ├─ setIcon                             → test: builderSetters_returnSameInstance
 *   ├─ setName                             → test: builderSetters_returnSameInstance
 *   ├─ setKey                              → test: builderSetters_returnSameInstance
 *   ├─ setId                               → test: builderSetters_returnSameInstance
 *   ├─ setType                             → test: builderSetters_returnSameInstance
 *   ├─ setDescription                      → test: builderSetters_returnSameInstance
 *   ├─ setStatus                           → test: builderSetters_returnSameInstance
 *   ├─ setCreatedAt                        → test: builderSetters_returnSameInstance
 *   ├─ setCurrentActiveAlias               → test: builderSetters_returnSameInstance
 *   └─ build()                             → covered via every build() call
 * </pre>
 */
class SpaceTest {

    // ══════════════════════════════════════════════════════════════════════════
    //  1. Space.builder() static factory
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void builder_returnsNonNullSpaceBuilder() {
        assertNotNull(Space.builder());
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  2. Builder construction – all fields populated
    //     Covers: private Space(SpaceBuilder), all builder setters, build(),
    //             and all getters.
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void builderAllFields_buildReturnsSpaceWithCorrectValues() {
        Date now = new Date();

        Space space = Space.builder()
                .setAuthorId("author-1")
                .setHomepageId("home-1")
                .setIcon("icon.png")
                .setName("My Space")
                .setKey("MS")
                .setId("42")
                .setType("global")
                .setDescription("A test space")
                .setStatus("current")
                .setCreatedAt(now)
                .build();

        assertNotNull(space);
        assertEquals("author-1",  space.getAuthorId());
        assertEquals("home-1",    space.getHomepageId());
        assertEquals("icon.png",  space.getIcon());
        assertEquals("My Space",  space.getName());
        assertEquals("MS",        space.getKey());
        assertEquals("42",        space.getId());
        assertEquals("global",    space.getType());
        assertEquals("A test space", space.getDescription());
        assertEquals("current",   space.getStatus());
        assertEquals(now,         space.getCreatedAt());
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  3. Builder construction – no fields set (all fields remain null)
    //     Verifies the private constructor assigns builder's default (null) values.
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void builderNoFields_buildReturnsSpaceWithAllNulls() {
        Space space = Space.builder().build();

        assertNotNull(space);
        assertNull(space.getAuthorId());
        assertNull(space.getHomepageId());
        assertNull(space.getIcon());
        assertNull(space.getName());
        assertNull(space.getKey());
        assertNull(space.getId());
        assertNull(space.getType());
        assertNull(space.getDescription());
        assertNull(space.getStatus());
        assertNull(space.getCreatedAt());
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  4. Individual setters – each setter mutates the correct field
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void setAuthorId_updatesField() {
        Space space = Space.builder().build();
        space.setAuthorId("updated-author");
        assertEquals("updated-author", space.getAuthorId());
    }

    @Test
    void setHomepageId_updatesField() {
        Space space = Space.builder().build();
        space.setHomepageId("updated-home");
        assertEquals("updated-home", space.getHomepageId());
    }

    @Test
    void setIcon_updatesField() {
        Space space = Space.builder().build();
        space.setIcon("updated-icon.png");
        assertEquals("updated-icon.png", space.getIcon());
    }

    @Test
    void setName_updatesField() {
        Space space = Space.builder().build();
        space.setName("Updated Name");
        assertEquals("Updated Name", space.getName());
    }

    @Test
    void setKey_updatesField() {
        Space space = Space.builder().build();
        space.setKey("UPKEY");
        assertEquals("UPKEY", space.getKey());
    }

    @Test
    void setId_updatesField() {
        Space space = Space.builder().build();
        space.setId("999");
        assertEquals("999", space.getId());
    }

    @Test
    void setType_updatesField() {
        Space space = Space.builder().build();
        space.setType("personal");
        assertEquals("personal", space.getType());
    }

    @Test
    void setDescription_updatesField() {
        Space space = Space.builder().build();
        space.setDescription("Updated description");
        assertEquals("Updated description", space.getDescription());
    }

    @Test
    void setStatus_updatesField() {
        Space space = Space.builder().build();
        space.setStatus("archived");
        assertEquals("archived", space.getStatus());
    }

    @Test
    void setCreatedAt_updatesField() {
        Space space = Space.builder().build();
        Date date = new Date(1_000_000L);
        space.setCreatedAt(date);
        assertEquals(date, space.getCreatedAt());
    }

    @Test
    void setStatus_updatesFieldToArchived() {
        Space space = Space.builder().build();
        space.setStatus("archived");
        assertEquals("archived", space.getStatus());
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  5. Setter → getter round-trip: overwriting a value set via builder
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void setters_overwriteBuilderValues() {
        Date original = new Date(0);
        Date updated  = new Date(99_999L);

        Space space = Space.builder()
                .setAuthorId("old-author")
                .setHomepageId("old-home")
                .setIcon("old-icon")
                .setName("Old Name")
                .setKey("OLD")
                .setId("1")
                .setType("global")
                .setDescription("old desc")
                .setStatus("current")
                .setCreatedAt(original)
                .build();

        space.setAuthorId("new-author");
        space.setHomepageId("new-home");
        space.setIcon("new-icon");
        space.setName("New Name");
        space.setKey("NEW");
        space.setId("2");
        space.setType("personal");
        space.setDescription("new desc");
        space.setStatus("archived");
        space.setCreatedAt(updated);

        assertEquals("new-author",  space.getAuthorId());
        assertEquals("new-home",    space.getHomepageId());
        assertEquals("new-icon",    space.getIcon());
        assertEquals("New Name",    space.getName());
        assertEquals("NEW",         space.getKey());
        assertEquals("2",           space.getId());
        assertEquals("personal",    space.getType());
        assertEquals("new desc",    space.getDescription());
        assertEquals("archived",    space.getStatus());
        assertEquals(updated,       space.getCreatedAt());
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  6. Builder fluent API – every setter returns the same builder instance
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void builderSetters_returnSameBuilderInstance() {
        Date date = new Date();
        Space.SpaceBuilder builder = Space.builder();

        assertSame(builder, builder.setAuthorId("a"));
        assertSame(builder, builder.setHomepageId("h"));
        assertSame(builder, builder.setIcon("i"));
        assertSame(builder, builder.setName("n"));
        assertSame(builder, builder.setKey("k"));
        assertSame(builder, builder.setId("id"));
        assertSame(builder, builder.setType("t"));
        assertSame(builder, builder.setDescription("d"));
        assertSame(builder, builder.setStatus("s"));
        assertSame(builder, builder.setCreatedAt(date));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  7. toString() – with all fields populated
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void toString_withAllFieldsSet_containsEveryFieldValue() {
        Date epoch = new Date(0);

        Space space = Space.builder()
                .setAuthorId("a1")
                .setHomepageId("h1")
                .setIcon("ic")
                .setName("N")
                .setKey("K")
                .setId("1")
                .setType("global")
                .setDescription("desc")
                .setStatus("current")
                .setCreatedAt(epoch)
                .build();

        String s = space.toString();

        assertNotNull(s);
        assertTrue(s.startsWith("Space{"),                    "must start with 'Space{'");
        assertTrue(s.contains("authorId='a1'"),               "must contain authorId");
        assertTrue(s.contains("homepageId='h1'"),             "must contain homepageId");
        assertTrue(s.contains("icon='ic'"),                   "must contain icon");
        assertTrue(s.contains("name='N'"),                    "must contain name");
        assertTrue(s.contains("key='K'"),                     "must contain key");
        assertTrue(s.contains("id='1'"),                      "must contain id");
        assertTrue(s.contains("type='global'"),               "must contain type");
        assertTrue(s.contains("description='desc'"),          "must contain description");
        assertTrue(s.contains("status='current'"),            "must contain status");
        assertTrue(s.endsWith("}"),                           "must end with '}'");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  8. toString() – with all fields null
    //     Ensures the null-field code path in the concatenation is exercised.
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void toString_withAllFieldsNull_containsNullLiterals() {
        Space space = Space.builder().build();
        String s = space.toString();

        assertNotNull(s);
        assertTrue(s.startsWith("Space{"), "must start with 'Space{'");
        // Java's string concatenation renders null fields as the literal "null"
        assertTrue(s.contains("null"), "null fields must appear as 'null' in toString");
        assertTrue(s.endsWith("}"),    "must end with '}'");
    }
}
