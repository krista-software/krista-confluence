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
@DisplayName("ContentStore")
class ContentStoreTest {

    private ContentStore contentStore;

    @Mock
    private EntityType entityType;

    @BeforeEach
    void setUp() {
        ContentStore.store.clear();
        contentStore = new ContentStore();
        lenient().when(entityType.getEntityName()).thenReturn("Content");
    }

    @Nested
    @DisplayName("create()")
    class CreateTests {

        @Test
        @DisplayName("should store the entity and return an EntityValue with correct entity name")
        void create_storesEntityAndReturnsEntityValue() {
            Map<String, Object> fields = new LinkedHashMap<>();
            fields.put("ID", "content-1");
            fields.put("Body", "<p>Hello</p>");

            EntityValue result = contentStore.create(entityType, fields);

            assertNotNull(result);
            assertEquals("Content", result.getEntityName());
            assertEquals(fields, result.getFields());
        }

        @Test
        @DisplayName("should store the entity in the static store map")
        void create_entityIsStoredInStaticMap() {
            Map<String, Object> fields = new LinkedHashMap<>();
            fields.put("ID", "content-2");
            fields.put("Body", "<p>World</p>");

            contentStore.create(entityType, fields);

            Pair<String, String> expectedKey = Pair.of("Content", "content-2");
            assertTrue(ContentStore.store.containsKey(expectedKey));
            assertEquals(fields, ContentStore.store.get(expectedKey));
        }

        @Test
        @DisplayName("should overwrite existing entity with the same primary key")
        void create_overwritesExistingEntity() {
            Map<String, Object> fields1 = new LinkedHashMap<>();
            fields1.put("ID", "content-dup");
            fields1.put("Body", "<p>Original</p>");

            Map<String, Object> fields2 = new LinkedHashMap<>();
            fields2.put("ID", "content-dup");
            fields2.put("Body", "<p>Updated</p>");

            contentStore.create(entityType, fields1);
            contentStore.create(entityType, fields2);

            assertEquals(1, ContentStore.store.size());
            Pair<String, String> key = Pair.of("Content", "content-dup");
            assertEquals("<p>Updated</p>", ContentStore.store.get(key).get("Body"));
        }

        @Test
        @DisplayName("should store multiple entities with different primary keys")
        void create_multipleEntities() {
            Map<String, Object> fields1 = Map.of("ID", "c1", "Body", "body1");
            Map<String, Object> fields2 = Map.of("ID", "c2", "Body", "body2");

            contentStore.create(entityType, fields1);
            contentStore.create(entityType, fields2);

            assertEquals(2, ContentStore.store.size());
        }
    }

    @Nested
    @DisplayName("get()")
    class GetTests {

        @Test
        @DisplayName("should retrieve a previously stored entity")
        void get_returnsStoredEntity() {
            Map<String, Object> fields = Map.of("ID", "get-1", "Body", "content body");
            contentStore.create(entityType, fields);

            EntityValue result = contentStore.get(entityType, "get-1");

            assertNotNull(result);
            assertEquals("Content", result.getEntityName());
            assertEquals(fields, result.getFields());
        }

        @Test
        @DisplayName("should return EntityValue with null fields when key does not exist")
        void get_returnsNullFieldsForMissingKey() {
            EntityValue result = contentStore.get(entityType, "nonexistent");

            assertNotNull(result);
            assertEquals("Content", result.getEntityName());
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
            Map<String, Object> fields1 = new HashMap<>(Map.of("ID", "s1", "Body", "body1"));
            Map<String, Object> fields2 = new HashMap<>(Map.of("ID", "s2", "Body", "body2"));
            contentStore.create(entityType, fields1);
            contentStore.create(entityType, fields2);

            List<EntityValue> results = contentStore.search(
                    entityType, List.of(searchCondition), 0L, 10);

            assertNotNull(results);
            assertEquals(2, results.size());
            results.forEach(ev -> assertEquals("Content", ev.getEntityName()));
        }

        @Test
        @DisplayName("should not return entities with different entity names")
        void search_filtersOutDifferentEntityNames() {
            Map<String, Object> fields = new HashMap<>(Map.of("ID", "f1", "Body", "body"));
            contentStore.create(entityType, fields);

            EntityType otherType = mock(EntityType.class);
            when(otherType.getEntityName()).thenReturn("OtherEntity");

            List<EntityValue> results = contentStore.search(
                    otherType, List.of(searchCondition), 0L, 10);

            assertTrue(results.isEmpty());
        }

        @Test
        @DisplayName("should return empty list when store is empty")
        void search_returnsEmptyListWhenStoreIsEmpty() {
            List<EntityValue> results = contentStore.search(
                    entityType, List.of(searchCondition), 0L, 10);

            assertNotNull(results);
            assertTrue(results.isEmpty());
        }

        @Test
        @DisplayName("should apply pagination correctly with first page")
        void search_paginationFirstPage() {
            for (int i = 0; i < 5; i++) {
                Map<String, Object> fields = new HashMap<>(Map.of("ID", "p" + i, "Body", "body" + i));
                contentStore.create(entityType, fields);
            }

            // Page 0 with pageSize 3 should return 3 items
            List<EntityValue> results = contentStore.search(
                    entityType, List.of(searchCondition), 0L, 3);

            assertEquals(3, results.size());
        }

        @Test
        @DisplayName("should apply pagination correctly with second page")
        void search_paginationSecondPage() {
            for (int i = 0; i < 5; i++) {
                Map<String, Object> fields = new HashMap<>(Map.of("ID", "p" + i, "Body", "body" + i));
                contentStore.create(entityType, fields);
            }

            // Page 1 with pageSize 3 should return 2 items (items 4 and 5)
            List<EntityValue> results = contentStore.search(
                    entityType, List.of(searchCondition), 1L, 3);

            assertEquals(2, results.size());
        }

        @Test
        @DisplayName("should return empty list for page beyond available data")
        void search_paginationBeyondData() {
            for (int i = 0; i < 3; i++) {
                Map<String, Object> fields = new HashMap<>(Map.of("ID", "p" + i, "Body", "body" + i));
                contentStore.create(entityType, fields);
            }

            // Page 5 with pageSize 3 should return 0 items
            List<EntityValue> results = contentStore.search(
                    entityType, List.of(searchCondition), 5L, 3);

            assertTrue(results.isEmpty());
        }

        @Test
        @DisplayName("should return all items when page size is larger than total count")
        void search_pageSizeLargerThanTotal() {
            for (int i = 0; i < 3; i++) {
                Map<String, Object> fields = new HashMap<>(Map.of("ID", "p" + i, "Body", "body" + i));
                contentStore.create(entityType, fields);
            }

            List<EntityValue> results = contentStore.search(
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
            Map<String, Object> originalFields = new HashMap<>(Map.of("ID", "u1", "Body", "original body"));
            contentStore.create(entityType, originalFields);

            Map<String, Object> updatedFields = new HashMap<>(Map.of("ID", "u1", "Body", "updated body"));
            EntityValue updatedEntity = new EntityValue("Content", updatedFields);

            EntityValue result = contentStore.update(entityType, updatedEntity);

            assertNotNull(result);
            assertEquals(updatedFields, result.getFields());

            Pair<String, String> key = Pair.of("Content", "u1");
            assertEquals("updated body", ContentStore.store.get(key).get("Body"));
        }

        @Test
        @DisplayName("should return the same EntityValue that was passed in")
        void update_returnsSameEntityValue() {
            Map<String, Object> fields = new HashMap<>(Map.of("ID", "u2", "Body", "body"));
            EntityValue entityValue = new EntityValue("Content", fields);

            EntityValue result = contentStore.update(entityType, entityValue);

            assertSame(entityValue, result);
        }

        @Test
        @DisplayName("should insert a new entity if the primary key does not exist")
        void update_insertsNewEntityIfNotExists() {
            Map<String, Object> fields = new HashMap<>(Map.of("ID", "new-entity", "Body", "new body"));
            EntityValue entityValue = new EntityValue("Content", fields);

            contentStore.update(entityType, entityValue);

            Pair<String, String> key = Pair.of("Content", "new-entity");
            assertTrue(ContentStore.store.containsKey(key));
        }
    }

    @Nested
    @DisplayName("delete()")
    class DeleteTests {

        @Test
        @DisplayName("should remove the entity from the store")
        void delete_removesEntityFromStore() {
            Map<String, Object> fields = new HashMap<>(Map.of("ID", "d1", "Body", "to be deleted"));
            contentStore.create(entityType, fields);

            Pair<String, String> key = Pair.of("Content", "d1");
            assertTrue(ContentStore.store.containsKey(key));

            contentStore.delete(entityType, "d1");

            assertFalse(ContentStore.store.containsKey(key));
        }

        @Test
        @DisplayName("should return null")
        void delete_returnsNull() {
            Map<String, Object> fields = new HashMap<>(Map.of("ID", "d2", "Body", "body"));
            contentStore.create(entityType, fields);

            Void result = contentStore.delete(entityType, "d2");

            assertNull(result);
        }

        @Test
        @DisplayName("should not throw when deleting a non-existent key")
        void delete_noExceptionForMissingKey() {
            assertDoesNotThrow(() -> contentStore.delete(entityType, "nonexistent"));
        }

        @Test
        @DisplayName("should not affect other entries in the store")
        void delete_doesNotAffectOtherEntries() {
            Map<String, Object> fields1 = new HashMap<>(Map.of("ID", "keep", "Body", "keep body"));
            Map<String, Object> fields2 = new HashMap<>(Map.of("ID", "remove", "Body", "remove body"));
            contentStore.create(entityType, fields1);
            contentStore.create(entityType, fields2);

            contentStore.delete(entityType, "remove");

            assertEquals(1, ContentStore.store.size());
            Pair<String, String> keepKey = Pair.of("Content", "keep");
            assertTrue(ContentStore.store.containsKey(keepKey));
        }
    }

    @Nested
    @DisplayName("Page inner class (pagination)")
    class PageTests {

        @Test
        @DisplayName("should return true for items within the first page")
        void nextHitIsInPage_firstPageItems() {
            ContentStore.Page page = new ContentStore.Page(0, 3);

            assertTrue(page.nextHitIsInPage());  // item 1
            assertTrue(page.nextHitIsInPage());  // item 2
            assertTrue(page.nextHitIsInPage());  // item 3
            assertFalse(page.nextHitIsInPage()); // item 4 - beyond first page
        }

        @Test
        @DisplayName("should return true for items within the second page")
        void nextHitIsInPage_secondPageItems() {
            ContentStore.Page page = new ContentStore.Page(1, 3);

            assertFalse(page.nextHitIsInPage()); // item 1 - before second page
            assertFalse(page.nextHitIsInPage()); // item 2 - before second page
            assertFalse(page.nextHitIsInPage()); // item 3 - before second page
            assertTrue(page.nextHitIsInPage());  // item 4 - in second page
            assertTrue(page.nextHitIsInPage());  // item 5 - in second page
            assertTrue(page.nextHitIsInPage());  // item 6 - in second page
            assertFalse(page.nextHitIsInPage()); // item 7 - beyond second page
        }

        @Test
        @DisplayName("should handle page size of 1")
        void nextHitIsInPage_pageSizeOne() {
            ContentStore.Page page = new ContentStore.Page(0, 1);

            assertTrue(page.nextHitIsInPage());  // item 1
            assertFalse(page.nextHitIsInPage()); // item 2 - beyond first page
        }

        @Test
        @DisplayName("should handle page size of 1 for second page")
        void nextHitIsInPage_pageSizeOneSecondPage() {
            ContentStore.Page page = new ContentStore.Page(1, 1);

            assertFalse(page.nextHitIsInPage()); // item 1 - in first page
            assertTrue(page.nextHitIsInPage());  // item 2 - in second page
            assertFalse(page.nextHitIsInPage()); // item 3 - beyond second page
        }

        @Test
        @DisplayName("should handle large page index with no items on that page")
        void nextHitIsInPage_largePageIndex() {
            ContentStore.Page page = new ContentStore.Page(100, 10);

            // Items 1-10 should all be false (page 0)
            for (int i = 0; i < 10; i++) {
                assertFalse(page.nextHitIsInPage());
            }
        }

        @Test
        @DisplayName("should handle page index 0 with large page size")
        void nextHitIsInPage_largePageSize() {
            ContentStore.Page page = new ContentStore.Page(0, 100);

            for (int i = 0; i < 100; i++) {
                assertTrue(page.nextHitIsInPage());
            }
            assertFalse(page.nextHitIsInPage()); // item 101 - beyond
        }

        @Test
        @DisplayName("should return false for third page items when only two pages of data exist")
        void nextHitIsInPage_thirdPage() {
            ContentStore.Page page = new ContentStore.Page(2, 2);

            assertFalse(page.nextHitIsInPage()); // item 1 - page 0
            assertFalse(page.nextHitIsInPage()); // item 2 - page 0
            assertFalse(page.nextHitIsInPage()); // item 3 - page 1
            assertFalse(page.nextHitIsInPage()); // item 4 - page 1
            assertTrue(page.nextHitIsInPage());  // item 5 - page 2
            assertTrue(page.nextHitIsInPage());  // item 6 - page 2
            assertFalse(page.nextHitIsInPage()); // item 7 - page 3
        }
    }

    @Nested
    @DisplayName("Static initializer and createPrimaryKeyMap")
    class StaticInitializerTests {

        @Test
        @DisplayName("ContentStore class should load successfully indicating createPrimaryKeyMap executed")
        void staticInitializer_loadsSuccessfully() {
            // The fact that we can instantiate ContentStore means the static initializer ran
            assertNotNull(new ContentStore());
        }

        @Test
        @DisplayName("store should be a LinkedHashMap initially empty after clear")
        void store_isLinkedHashMap() {
            ContentStore.store.clear();
            assertNotNull(ContentStore.store);
            assertTrue(ContentStore.store.isEmpty());
            assertTrue(ContentStore.store instanceof LinkedHashMap);
        }
    }

    @Nested
    @DisplayName("Integration scenarios")
    class IntegrationTests {

        @Test
        @DisplayName("full CRUD lifecycle should work correctly")
        void fullCrudLifecycle() {
            // Create
            Map<String, Object> createFields = new HashMap<>(Map.of("ID", "lifecycle-1", "Body", "initial body"));
            EntityValue created = contentStore.create(entityType, createFields);
            assertNotNull(created);

            // Get
            EntityValue retrieved = contentStore.get(entityType, "lifecycle-1");
            assertEquals("initial body", retrieved.getFields().get("Body"));

            // Update
            Map<String, Object> updatedFields = new HashMap<>(Map.of("ID", "lifecycle-1", "Body", "updated body"));
            EntityValue updatedEntity = new EntityValue("Content", updatedFields);
            contentStore.update(entityType, updatedEntity);

            EntityValue afterUpdate = contentStore.get(entityType, "lifecycle-1");
            assertEquals("updated body", afterUpdate.getFields().get("Body"));

            // Delete
            contentStore.delete(entityType, "lifecycle-1");
            EntityValue afterDelete = contentStore.get(entityType, "lifecycle-1");
            assertNull(afterDelete.getFields());
        }
    }
}
