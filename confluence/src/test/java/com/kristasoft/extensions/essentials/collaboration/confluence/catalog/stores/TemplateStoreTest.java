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

package com.kristasoft.extensions.essentials.collaboration.confluence.catalog.stores;

import app.krista.extension.executor.SearchCondition;
import app.krista.model.base.EntityValue;
import app.krista.model.entity.EntityType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TemplateStore")
class TemplateStoreTest {

    private TemplateStore templateStore;

    @Mock
    private EntityType entityType;

    @BeforeEach
    void setUp() {
        TemplateStore.store.clear();
        templateStore = new TemplateStore();
        lenient().when(entityType.getEntityName()).thenReturn("Template");
    }

    @Nested
    @DisplayName("create()")
    class CreateTests {

        @Test
        @DisplayName("should store the entity and return an EntityValue with correct entity name")
        void create_storesEntityAndReturnsEntityValue() {
            Map<String, Object> fields = new LinkedHashMap<>();
            fields.put("Name", "My Template");
            fields.put("Id", "tmpl-1");
            fields.put("Body", "<p>Template body</p>");

            EntityValue result = templateStore.create(entityType, fields);

            assertNotNull(result);
            assertEquals("Template", result.getEntityName());
            assertEquals(fields, result.getFields());
        }

        @Test
        @DisplayName("should store the entity in the static store map using primary key 'Name'")
        void create_entityIsStoredInStaticMap() {
            Map<String, Object> fields = new LinkedHashMap<>();
            fields.put("Name", "Template Alpha");
            fields.put("Id", "tmpl-2");
            fields.put("Type", "page");

            templateStore.create(entityType, fields);

            Pair<String, String> expectedKey = Pair.of("Template", "Template Alpha");
            assertTrue(TemplateStore.store.containsKey(expectedKey));
            assertEquals(fields, TemplateStore.store.get(expectedKey));
        }

        @Test
        @DisplayName("should overwrite existing entity with the same primary key")
        void create_overwritesExistingEntity() {
            Map<String, Object> fields1 = new LinkedHashMap<>();
            fields1.put("Name", "DupTemplate");
            fields1.put("Body", "Original");

            Map<String, Object> fields2 = new LinkedHashMap<>();
            fields2.put("Name", "DupTemplate");
            fields2.put("Body", "Updated");

            templateStore.create(entityType, fields1);
            templateStore.create(entityType, fields2);

            assertEquals(1, TemplateStore.store.size());
            Pair<String, String> key = Pair.of("Template", "DupTemplate");
            assertEquals("Updated", TemplateStore.store.get(key).get("Body"));
        }

        @Test
        @DisplayName("should store multiple entities with different primary keys")
        void create_multipleEntities() {
            Map<String, Object> fields1 = Map.of("Name", "Tmpl1", "Id", "t1");
            Map<String, Object> fields2 = Map.of("Name", "Tmpl2", "Id", "t2");

            templateStore.create(entityType, fields1);
            templateStore.create(entityType, fields2);

            assertEquals(2, TemplateStore.store.size());
        }
    }

    @Nested
    @DisplayName("get()")
    class GetTests {

        @Test
        @DisplayName("should retrieve a previously stored entity by primary key")
        void get_returnsStoredEntity() {
            Map<String, Object> fields = Map.of("Name", "GetTemplate", "Id", "g1", "Body", "<p>body</p>");
            templateStore.create(entityType, fields);

            EntityValue result = templateStore.get(entityType, "GetTemplate");

            assertNotNull(result);
            assertEquals("Template", result.getEntityName());
            assertEquals(fields, result.getFields());
        }

        @Test
        @DisplayName("should return EntityValue with null fields when key does not exist")
        void get_returnsNullFieldsForMissingKey() {
            EntityValue result = templateStore.get(entityType, "nonexistent-template");

            assertNotNull(result);
            assertEquals("Template", result.getEntityName());
            assertNull(result.getFields());
        }
    }

    @Nested
    @DisplayName("search()")
    class SearchTests {

        @Mock
        private SearchCondition searchCondition;

        @Test
        @DisplayName("should return matching entities for the given entity name")
        void search_returnsMatchingEntities() {
            Map<String, Object> fields1 = new HashMap<>(Map.of("Name", "T1", "Id", "id1"));
            Map<String, Object> fields2 = new HashMap<>(Map.of("Name", "T2", "Id", "id2"));
            Map<String, Object> fields3 = new HashMap<>(Map.of("Name", "T3", "Id", "id3"));
            templateStore.create(entityType, fields1);
            templateStore.create(entityType, fields2);
            templateStore.create(entityType, fields3);

            List<EntityValue> results = templateStore.search(
                    entityType, List.of(searchCondition), 0L, 10);

            assertNotNull(results);
            assertEquals(3, results.size());
            results.forEach(ev -> assertEquals("Template", ev.getEntityName()));
        }

        @Test
        @DisplayName("should not return entities with different entity names")
        void search_filtersOutDifferentEntityNames() {
            Map<String, Object> fields = new HashMap<>(Map.of("Name", "T1", "Id", "id1"));
            templateStore.create(entityType, fields);

            EntityType otherType = mock(EntityType.class);
            when(otherType.getEntityName()).thenReturn("NonTemplate");

            List<EntityValue> results = templateStore.search(
                    otherType, List.of(searchCondition), 0L, 10);

            assertTrue(results.isEmpty());
        }

        @Test
        @DisplayName("should return empty list when store is empty")
        void search_returnsEmptyListWhenStoreIsEmpty() {
            List<EntityValue> results = templateStore.search(
                    entityType, List.of(searchCondition), 0L, 10);

            assertNotNull(results);
            assertTrue(results.isEmpty());
        }

        @Test
        @DisplayName("should apply pagination correctly for first page")
        void search_paginationFirstPage() {
            for (int i = 0; i < 7; i++) {
                Map<String, Object> fields = new HashMap<>(Map.of("Name", "T" + i, "Id", "id" + i));
                templateStore.create(entityType, fields);
            }

            List<EntityValue> results = templateStore.search(
                    entityType, List.of(searchCondition), 0L, 4);

            assertEquals(4, results.size());
        }

        @Test
        @DisplayName("should apply pagination correctly for second page")
        void search_paginationSecondPage() {
            for (int i = 0; i < 7; i++) {
                Map<String, Object> fields = new HashMap<>(Map.of("Name", "T" + i, "Id", "id" + i));
                templateStore.create(entityType, fields);
            }

            List<EntityValue> results = templateStore.search(
                    entityType, List.of(searchCondition), 1L, 4);

            assertEquals(3, results.size());
        }

        @Test
        @DisplayName("should return empty list for page beyond available data")
        void search_paginationBeyondData() {
            for (int i = 0; i < 3; i++) {
                Map<String, Object> fields = new HashMap<>(Map.of("Name", "T" + i, "Id", "id" + i));
                templateStore.create(entityType, fields);
            }

            List<EntityValue> results = templateStore.search(
                    entityType, List.of(searchCondition), 5L, 3);

            assertTrue(results.isEmpty());
        }

        @Test
        @DisplayName("should return all items when page size covers all data")
        void search_pageSizeCoversAllData() {
            for (int i = 0; i < 3; i++) {
                Map<String, Object> fields = new HashMap<>(Map.of("Name", "T" + i, "Id", "id" + i));
                templateStore.create(entityType, fields);
            }

            List<EntityValue> results = templateStore.search(
                    entityType, List.of(searchCondition), 0L, 100);

            assertEquals(3, results.size());
        }
    }

    @Nested
    @DisplayName("update()")
    class UpdateTests {

        @Test
        @DisplayName("should update the entity in the store and return the updated EntityValue")
        void update_replacesDataInStore() {
            Map<String, Object> originalFields = new HashMap<>(Map.of("Name", "UpdateTmpl", "Body", "old body"));
            templateStore.create(entityType, originalFields);

            Map<String, Object> updatedFields = new HashMap<>(Map.of("Name", "UpdateTmpl", "Body", "new body"));
            EntityValue updatedEntity = new EntityValue("Template", updatedFields);

            EntityValue result = templateStore.update(entityType, updatedEntity);

            assertNotNull(result);
            assertEquals(updatedFields, result.getFields());

            Pair<String, String> key = Pair.of("Template", "UpdateTmpl");
            assertEquals("new body", TemplateStore.store.get(key).get("Body"));
        }

        @Test
        @DisplayName("should return the same EntityValue instance that was passed in")
        void update_returnsSameEntityValue() {
            Map<String, Object> fields = new HashMap<>(Map.of("Name", "Tmpl", "Body", "body"));
            EntityValue entityValue = new EntityValue("Template", fields);

            EntityValue result = templateStore.update(entityType, entityValue);

            assertSame(entityValue, result);
        }

        @Test
        @DisplayName("should insert a new entity when key does not exist")
        void update_insertsNewEntityIfNotExists() {
            Map<String, Object> fields = new HashMap<>(Map.of("Name", "NewTmpl", "Body", "body"));
            EntityValue entityValue = new EntityValue("Template", fields);

            templateStore.update(entityType, entityValue);

            Pair<String, String> key = Pair.of("Template", "NewTmpl");
            assertTrue(TemplateStore.store.containsKey(key));
        }
    }

    @Nested
    @DisplayName("delete()")
    class DeleteTests {

        @Test
        @DisplayName("should remove the entity from the store")
        void delete_removesEntityFromStore() {
            Map<String, Object> fields = new HashMap<>(Map.of("Name", "DeleteMe", "Body", "body"));
            templateStore.create(entityType, fields);

            Pair<String, String> key = Pair.of("Template", "DeleteMe");
            assertTrue(TemplateStore.store.containsKey(key));

            templateStore.delete(entityType, "DeleteMe");

            assertFalse(TemplateStore.store.containsKey(key));
        }

        @Test
        @DisplayName("should return null")
        void delete_returnsNull() {
            Map<String, Object> fields = new HashMap<>(Map.of("Name", "D2", "Body", "body"));
            templateStore.create(entityType, fields);

            Void result = templateStore.delete(entityType, "D2");

            assertNull(result);
        }

        @Test
        @DisplayName("should not throw when deleting a non-existent key")
        void delete_noExceptionForMissingKey() {
            assertDoesNotThrow(() -> templateStore.delete(entityType, "nonexistent"));
        }

        @Test
        @DisplayName("should not affect other entries in the store")
        void delete_doesNotAffectOtherEntries() {
            Map<String, Object> fields1 = new HashMap<>(Map.of("Name", "Keep", "Body", "keep body"));
            Map<String, Object> fields2 = new HashMap<>(Map.of("Name", "Remove", "Body", "remove body"));
            templateStore.create(entityType, fields1);
            templateStore.create(entityType, fields2);

            templateStore.delete(entityType, "Remove");

            assertEquals(1, TemplateStore.store.size());
            assertTrue(TemplateStore.store.containsKey(Pair.of("Template", "Keep")));
        }
    }

    @Nested
    @DisplayName("Page inner class (pagination)")
    class PageTests {

        @Test
        @DisplayName("should return true for items within the first page")
        void nextHitIsInPage_firstPageItems() {
            TemplateStore.Page page = new TemplateStore.Page(0, 3);

            assertTrue(page.nextHitIsInPage());  // item 1
            assertTrue(page.nextHitIsInPage());  // item 2
            assertTrue(page.nextHitIsInPage());  // item 3
            assertFalse(page.nextHitIsInPage()); // item 4 - beyond first page
        }

        @Test
        @DisplayName("should return true for items within the second page only")
        void nextHitIsInPage_secondPageItems() {
            TemplateStore.Page page = new TemplateStore.Page(1, 3);

            assertFalse(page.nextHitIsInPage()); // item 1
            assertFalse(page.nextHitIsInPage()); // item 2
            assertFalse(page.nextHitIsInPage()); // item 3
            assertTrue(page.nextHitIsInPage());  // item 4
            assertTrue(page.nextHitIsInPage());  // item 5
            assertTrue(page.nextHitIsInPage());  // item 6
            assertFalse(page.nextHitIsInPage()); // item 7
        }

        @Test
        @DisplayName("should handle page size of 1 correctly")
        void nextHitIsInPage_pageSizeOne() {
            TemplateStore.Page page = new TemplateStore.Page(0, 1);

            assertTrue(page.nextHitIsInPage());
            assertFalse(page.nextHitIsInPage());
        }

        @Test
        @DisplayName("should handle page size of 1 for second page")
        void nextHitIsInPage_pageSizeOneSecondPage() {
            TemplateStore.Page page = new TemplateStore.Page(1, 1);

            assertFalse(page.nextHitIsInPage());
            assertTrue(page.nextHitIsInPage());
            assertFalse(page.nextHitIsInPage());
        }

        @Test
        @DisplayName("should handle large page index returning false for early items")
        void nextHitIsInPage_largePageIndex() {
            TemplateStore.Page page = new TemplateStore.Page(100, 5);

            for (int i = 0; i < 20; i++) {
                assertFalse(page.nextHitIsInPage());
            }
        }

        @Test
        @DisplayName("should correctly identify items on the third page")
        void nextHitIsInPage_thirdPage() {
            TemplateStore.Page page = new TemplateStore.Page(2, 2);

            assertFalse(page.nextHitIsInPage()); // item 1 - page 0
            assertFalse(page.nextHitIsInPage()); // item 2 - page 0
            assertFalse(page.nextHitIsInPage()); // item 3 - page 1
            assertFalse(page.nextHitIsInPage()); // item 4 - page 1
            assertTrue(page.nextHitIsInPage());  // item 5 - page 2
            assertTrue(page.nextHitIsInPage());  // item 6 - page 2
            assertFalse(page.nextHitIsInPage()); // item 7 - page 3
        }

        @Test
        @DisplayName("should handle page index 0 with large page size")
        void nextHitIsInPage_largePageSize() {
            TemplateStore.Page page = new TemplateStore.Page(0, 100);

            for (int i = 0; i < 100; i++) {
                assertTrue(page.nextHitIsInPage());
            }
            assertFalse(page.nextHitIsInPage());
        }
    }

    @Nested
    @DisplayName("Static initializer and createPrimaryKeyMap")
    class StaticInitializerTests {

        @Test
        @DisplayName("TemplateStore class should load successfully indicating createPrimaryKeyMap executed")
        void staticInitializer_loadsSuccessfully() {
            assertNotNull(new TemplateStore());
        }

        @Test
        @DisplayName("store should be a LinkedHashMap initially empty after clear")
        void store_isLinkedHashMap() {
            TemplateStore.store.clear();
            assertNotNull(TemplateStore.store);
            assertTrue(TemplateStore.store.isEmpty());
            assertTrue(TemplateStore.store instanceof LinkedHashMap);
        }
    }

    @Nested
    @DisplayName("Integration scenarios")
    class IntegrationTests {

        @Test
        @DisplayName("full CRUD lifecycle should work correctly")
        void fullCrudLifecycle() {
            // Create
            Map<String, Object> createFields = new HashMap<>(Map.of("Name", "LifecycleTmpl", "Id", "lt-1", "Body", "initial body", "Type", "page"));
            EntityValue created = templateStore.create(entityType, createFields);
            assertNotNull(created);
            assertEquals("Template", created.getEntityName());

            // Get
            EntityValue retrieved = templateStore.get(entityType, "LifecycleTmpl");
            assertEquals("initial body", retrieved.getFields().get("Body"));

            // Update
            Map<String, Object> updatedFields = new HashMap<>(Map.of("Name", "LifecycleTmpl", "Id", "lt-1", "Body", "updated body", "Type", "page"));
            EntityValue updatedEntity = new EntityValue("Template", updatedFields);
            templateStore.update(entityType, updatedEntity);

            EntityValue afterUpdate = templateStore.get(entityType, "LifecycleTmpl");
            assertEquals("updated body", afterUpdate.getFields().get("Body"));

            // Delete
            templateStore.delete(entityType, "LifecycleTmpl");
            EntityValue afterDelete = templateStore.get(entityType, "LifecycleTmpl");
            assertNull(afterDelete.getFields());
        }

        @Test
        @DisplayName("search with different entity names only returns matching ones")
        void searchFiltersByEntityName(@Mock SearchCondition condition) {
            // Add entities with the default entity type ("Template")
            Map<String, Object> tmplFields = new HashMap<>(Map.of("Name", "T1", "Id", "id1"));
            templateStore.create(entityType, tmplFields);

            // Manually add an entry with a different entity name
            TemplateStore.store.put(Pair.of("Other", "O1"), Map.of("Name", "O1"));

            List<EntityValue> results = templateStore.search(entityType, List.of(condition), 0L, 10);

            assertEquals(1, results.size());
            assertEquals("Template", results.get(0).getEntityName());
        }
    }
}
