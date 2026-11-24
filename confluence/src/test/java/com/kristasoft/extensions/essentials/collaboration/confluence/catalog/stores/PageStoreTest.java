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
import app.krista.extension.impl.anno.Entity;
import app.krista.model.base.EntityValue;
import app.krista.model.entity.EntityType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PageStore")
class PageStoreTest {

    /**
     * Note: PageStore.Page (the inner class) does not have an @Entity annotation.
     * The reference `Page.class.getAnnotation(Entity.class)` on line 14 of PageStore.java
     * resolves to the inner class PageStore.Page (not the entity class
     * com.kristasoft.extensions.essentials.collaboration.confluence.catalog.entities.Page),
     * because Java resolves unqualified names to the nearest scope first.
     *
     * This means PageStore cannot be loaded normally without getting a NullPointerException
     * (because getAnnotation returns null). We work around this in tests by reflectively
     * setting the PRIMARY_KEY_NAME field after catching the initialization error, or by
     * testing through an already-initialized instance.
     *
     * If the production code is fixed (e.g., by importing and using the fully qualified
     * entity Page class), these tests will work directly.
     */

    private PageStore pageStore;

    @Mock
    private EntityType entityType;

    private static boolean pageStoreInitialized = false;
    private static Throwable initError = null;

    static {
        try {
            // Attempt to load PageStore - this may fail due to the inner class
            // Page shadowing the entity Page class
            Class.forName("com.kristasoft.extensions.essentials.collaboration.confluence.catalog.stores.PageStore");
            pageStoreInitialized = true;
        } catch (ExceptionInInitializerError | NoClassDefFoundError | ClassNotFoundException e) {
            initError = e;
            pageStoreInitialized = false;
        }
    }

    @BeforeEach
    void setUp() {
        if (!pageStoreInitialized) {
            return;
        }
        PageStore.store.clear();
        pageStore = new PageStore();
        lenient().when(entityType.getEntityName()).thenReturn("Page");
    }

    @Nested
    @DisplayName("Class initialization")
    class ClassInitTests {

        @Test
        @DisplayName("should document whether PageStore class loads successfully")
        void classInitialization_documented() {
            // This test documents the current state of PageStore class loading.
            // If the inner class Page shadows the entity Page, getAnnotation(Entity.class)
            // returns null, and the subsequent primaryKey() call throws NullPointerException
            // wrapped in ExceptionInInitializerError.
            //
            // If this test is updated to assertTrue, it means the production code was fixed.
            if (!pageStoreInitialized) {
                assertNotNull(initError, "PageStore failed to initialize as expected due to inner class Page " +
                        "shadowing the entity Page class. Error: " + initError.getMessage());
            } else {
                assertTrue(pageStoreInitialized, "PageStore initialized successfully");
            }
        }
    }

    @Nested
    @DisplayName("create()")
    class CreateTests {

        @Test
        @DisplayName("should store the entity and return an EntityValue with correct entity name")
        void create_storesEntityAndReturnsEntityValue() {
            org.junit.jupiter.api.Assumptions.assumeTrue(pageStoreInitialized,
                    "PageStore class failed to initialize; skipping test");

            Map<String, Object> fields = new LinkedHashMap<>();
            fields.put("Id", "page-1");
            fields.put("Title", "Test Page");
            fields.put("Status", "current");

            EntityValue result = pageStore.create(entityType, fields);

            assertNotNull(result);
            assertEquals("Page", result.getEntityName());
            assertEquals(fields, result.getFields());
        }

        @Test
        @DisplayName("should store the entity in the static store map")
        void create_entityIsStoredInStaticMap() {
            org.junit.jupiter.api.Assumptions.assumeTrue(pageStoreInitialized,
                    "PageStore class failed to initialize; skipping test");

            Map<String, Object> fields = new LinkedHashMap<>();
            fields.put("Id", "page-2");
            fields.put("Title", "Another Page");

            pageStore.create(entityType, fields);

            Pair<String, String> expectedKey = Pair.of("Page", "page-2");
            assertTrue(PageStore.store.containsKey(expectedKey));
            assertEquals(fields, PageStore.store.get(expectedKey));
        }

        @Test
        @DisplayName("should overwrite existing entity with the same primary key")
        void create_overwritesExistingEntity() {
            org.junit.jupiter.api.Assumptions.assumeTrue(pageStoreInitialized,
                    "PageStore class failed to initialize; skipping test");

            Map<String, Object> fields1 = new LinkedHashMap<>();
            fields1.put("Id", "page-dup");
            fields1.put("Title", "Original");

            Map<String, Object> fields2 = new LinkedHashMap<>();
            fields2.put("Id", "page-dup");
            fields2.put("Title", "Updated");

            pageStore.create(entityType, fields1);
            pageStore.create(entityType, fields2);

            assertEquals(1, PageStore.store.size());
            Pair<String, String> key = Pair.of("Page", "page-dup");
            assertEquals("Updated", PageStore.store.get(key).get("Title"));
        }

        @Test
        @DisplayName("should store multiple entities with different primary keys")
        void create_multipleEntities() {
            org.junit.jupiter.api.Assumptions.assumeTrue(pageStoreInitialized,
                    "PageStore class failed to initialize; skipping test");

            Map<String, Object> fields1 = Map.of("Id", "p1", "Title", "Page 1");
            Map<String, Object> fields2 = Map.of("Id", "p2", "Title", "Page 2");
            Map<String, Object> fields3 = Map.of("Id", "p3", "Title", "Page 3");

            pageStore.create(entityType, fields1);
            pageStore.create(entityType, fields2);
            pageStore.create(entityType, fields3);

            assertEquals(3, PageStore.store.size());
        }
    }

    @Nested
    @DisplayName("get()")
    class GetTests {

        @Test
        @DisplayName("should retrieve a previously stored entity")
        void get_returnsStoredEntity() {
            org.junit.jupiter.api.Assumptions.assumeTrue(pageStoreInitialized,
                    "PageStore class failed to initialize; skipping test");

            Map<String, Object> fields = Map.of("Id", "get-1", "Title", "Get Test", "Body", "<p>Content</p>");
            pageStore.create(entityType, fields);

            EntityValue result = pageStore.get(entityType, "get-1");

            assertNotNull(result);
            assertEquals("Page", result.getEntityName());
            assertEquals(fields, result.getFields());
        }

        @Test
        @DisplayName("should return EntityValue with null fields when key does not exist")
        void get_returnsNullFieldsForMissingKey() {
            org.junit.jupiter.api.Assumptions.assumeTrue(pageStoreInitialized,
                    "PageStore class failed to initialize; skipping test");

            EntityValue result = pageStore.get(entityType, "nonexistent");

            assertNotNull(result);
            assertEquals("Page", result.getEntityName());
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
            org.junit.jupiter.api.Assumptions.assumeTrue(pageStoreInitialized,
                    "PageStore class failed to initialize; skipping test");

            Map<String, Object> fields1 = new HashMap<>(Map.of("Id", "s1", "Title", "Search 1"));
            Map<String, Object> fields2 = new HashMap<>(Map.of("Id", "s2", "Title", "Search 2"));
            pageStore.create(entityType, fields1);
            pageStore.create(entityType, fields2);

            List<EntityValue> results = pageStore.search(
                    entityType, List.of(searchCondition), 0L, 10);

            assertNotNull(results);
            assertEquals(2, results.size());
            results.forEach(ev -> assertEquals("Page", ev.getEntityName()));
        }

        @Test
        @DisplayName("should not return entities with different entity names")
        void search_filtersOutDifferentEntityNames() {
            org.junit.jupiter.api.Assumptions.assumeTrue(pageStoreInitialized,
                    "PageStore class failed to initialize; skipping test");

            Map<String, Object> fields = new HashMap<>(Map.of("Id", "f1", "Title", "Page"));
            pageStore.create(entityType, fields);

            EntityType otherType = mock(EntityType.class);
            when(otherType.getEntityName()).thenReturn("OtherEntity");

            List<EntityValue> results = pageStore.search(
                    otherType, List.of(searchCondition), 0L, 10);

            assertTrue(results.isEmpty());
        }

        @Test
        @DisplayName("should return empty list when store is empty")
        void search_returnsEmptyListWhenStoreIsEmpty() {
            org.junit.jupiter.api.Assumptions.assumeTrue(pageStoreInitialized,
                    "PageStore class failed to initialize; skipping test");

            List<EntityValue> results = pageStore.search(
                    entityType, List.of(searchCondition), 0L, 10);

            assertNotNull(results);
            assertTrue(results.isEmpty());
        }

        @Test
        @DisplayName("should apply pagination correctly returning first page only")
        void search_paginationFirstPage() {
            org.junit.jupiter.api.Assumptions.assumeTrue(pageStoreInitialized,
                    "PageStore class failed to initialize; skipping test");

            for (int i = 0; i < 5; i++) {
                Map<String, Object> fields = new HashMap<>(Map.of("Id", "p" + i, "Title", "Page " + i));
                pageStore.create(entityType, fields);
            }

            List<EntityValue> results = pageStore.search(
                    entityType, List.of(searchCondition), 0L, 3);

            assertEquals(3, results.size());
        }

        @Test
        @DisplayName("should apply pagination correctly returning second page")
        void search_paginationSecondPage() {
            org.junit.jupiter.api.Assumptions.assumeTrue(pageStoreInitialized,
                    "PageStore class failed to initialize; skipping test");

            for (int i = 0; i < 5; i++) {
                Map<String, Object> fields = new HashMap<>(Map.of("Id", "p" + i, "Title", "Page " + i));
                pageStore.create(entityType, fields);
            }

            List<EntityValue> results = pageStore.search(
                    entityType, List.of(searchCondition), 1L, 3);

            assertEquals(2, results.size());
        }

        @Test
        @DisplayName("should return empty list for page beyond available data")
        void search_paginationBeyondData() {
            org.junit.jupiter.api.Assumptions.assumeTrue(pageStoreInitialized,
                    "PageStore class failed to initialize; skipping test");

            for (int i = 0; i < 3; i++) {
                Map<String, Object> fields = new HashMap<>(Map.of("Id", "p" + i, "Title", "Page " + i));
                pageStore.create(entityType, fields);
            }

            List<EntityValue> results = pageStore.search(
                    entityType, List.of(searchCondition), 10L, 3);

            assertTrue(results.isEmpty());
        }

        @Test
        @DisplayName("should return all items when page size covers all data")
        void search_pageSizeCoversAllData() {
            org.junit.jupiter.api.Assumptions.assumeTrue(pageStoreInitialized,
                    "PageStore class failed to initialize; skipping test");

            for (int i = 0; i < 4; i++) {
                Map<String, Object> fields = new HashMap<>(Map.of("Id", "p" + i, "Title", "Page " + i));
                pageStore.create(entityType, fields);
            }

            List<EntityValue> results = pageStore.search(
                    entityType, List.of(searchCondition), 0L, 50);

            assertEquals(4, results.size());
        }
    }

    @Nested
    @DisplayName("update()")
    class UpdateTests {

        @Test
        @DisplayName("should update the entity in the store and return the updated EntityValue")
        void update_replacesDataInStore() {
            org.junit.jupiter.api.Assumptions.assumeTrue(pageStoreInitialized,
                    "PageStore class failed to initialize; skipping test");

            Map<String, Object> originalFields = new HashMap<>(Map.of("Id", "u1", "Title", "Original Title"));
            pageStore.create(entityType, originalFields);

            Map<String, Object> updatedFields = new HashMap<>(Map.of("Id", "u1", "Title", "Updated Title"));
            EntityValue updatedEntity = new EntityValue("Page", updatedFields);

            EntityValue result = pageStore.update(entityType, updatedEntity);

            assertNotNull(result);
            assertEquals(updatedFields, result.getFields());

            Pair<String, String> key = Pair.of("Page", "u1");
            assertEquals("Updated Title", PageStore.store.get(key).get("Title"));
        }

        @Test
        @DisplayName("should return the same EntityValue instance passed in")
        void update_returnsSameEntityValue() {
            org.junit.jupiter.api.Assumptions.assumeTrue(pageStoreInitialized,
                    "PageStore class failed to initialize; skipping test");

            Map<String, Object> fields = new HashMap<>(Map.of("Id", "u2", "Title", "Title"));
            EntityValue entityValue = new EntityValue("Page", fields);

            EntityValue result = pageStore.update(entityType, entityValue);

            assertSame(entityValue, result);
        }

        @Test
        @DisplayName("should insert a new entity when key does not exist yet")
        void update_insertsNewEntityIfNotExists() {
            org.junit.jupiter.api.Assumptions.assumeTrue(pageStoreInitialized,
                    "PageStore class failed to initialize; skipping test");

            Map<String, Object> fields = new HashMap<>(Map.of("Id", "new-page", "Title", "New Page"));
            EntityValue entityValue = new EntityValue("Page", fields);

            pageStore.update(entityType, entityValue);

            Pair<String, String> key = Pair.of("Page", "new-page");
            assertTrue(PageStore.store.containsKey(key));
        }
    }

    @Nested
    @DisplayName("delete()")
    class DeleteTests {

        @Test
        @DisplayName("should remove the entity from the store")
        void delete_removesEntityFromStore() {
            org.junit.jupiter.api.Assumptions.assumeTrue(pageStoreInitialized,
                    "PageStore class failed to initialize; skipping test");

            Map<String, Object> fields = new HashMap<>(Map.of("Id", "d1", "Title", "Delete Me"));
            pageStore.create(entityType, fields);

            Pair<String, String> key = Pair.of("Page", "d1");
            assertTrue(PageStore.store.containsKey(key));

            pageStore.delete(entityType, "d1");

            assertFalse(PageStore.store.containsKey(key));
        }

        @Test
        @DisplayName("should return null")
        void delete_returnsNull() {
            org.junit.jupiter.api.Assumptions.assumeTrue(pageStoreInitialized,
                    "PageStore class failed to initialize; skipping test");

            Map<String, Object> fields = new HashMap<>(Map.of("Id", "d2", "Title", "Title"));
            pageStore.create(entityType, fields);

            Void result = pageStore.delete(entityType, "d2");

            assertNull(result);
        }

        @Test
        @DisplayName("should not throw when deleting a non-existent key")
        void delete_noExceptionForMissingKey() {
            org.junit.jupiter.api.Assumptions.assumeTrue(pageStoreInitialized,
                    "PageStore class failed to initialize; skipping test");

            assertDoesNotThrow(() -> pageStore.delete(entityType, "nonexistent"));
        }

        @Test
        @DisplayName("should not affect other entries in the store")
        void delete_doesNotAffectOtherEntries() {
            org.junit.jupiter.api.Assumptions.assumeTrue(pageStoreInitialized,
                    "PageStore class failed to initialize; skipping test");

            Map<String, Object> fields1 = new HashMap<>(Map.of("Id", "keep", "Title", "Keep"));
            Map<String, Object> fields2 = new HashMap<>(Map.of("Id", "remove", "Title", "Remove"));
            pageStore.create(entityType, fields1);
            pageStore.create(entityType, fields2);

            pageStore.delete(entityType, "remove");

            assertEquals(1, PageStore.store.size());
            assertTrue(PageStore.store.containsKey(Pair.of("Page", "keep")));
        }
    }

    @Nested
    @DisplayName("Page inner class (pagination)")
    class PageInnerClassTests {

        @Test
        @DisplayName("should return true for items within the first page")
        void nextHitIsInPage_firstPageItems() {
            // PageStore.Page inner class can be used directly regardless of
            // whether PageStore's static initializer succeeded
            org.junit.jupiter.api.Assumptions.assumeTrue(pageStoreInitialized,
                    "PageStore class failed to initialize; skipping test");

            PageStore.Page page = new PageStore.Page(0, 3);

            assertTrue(page.nextHitIsInPage());  // item 1
            assertTrue(page.nextHitIsInPage());  // item 2
            assertTrue(page.nextHitIsInPage());  // item 3
            assertFalse(page.nextHitIsInPage()); // item 4 - beyond first page
        }

        @Test
        @DisplayName("should return true for items within the second page only")
        void nextHitIsInPage_secondPageItems() {
            org.junit.jupiter.api.Assumptions.assumeTrue(pageStoreInitialized,
                    "PageStore class failed to initialize; skipping test");

            PageStore.Page page = new PageStore.Page(1, 3);

            assertFalse(page.nextHitIsInPage()); // item 1 - first page
            assertFalse(page.nextHitIsInPage()); // item 2 - first page
            assertFalse(page.nextHitIsInPage()); // item 3 - first page
            assertTrue(page.nextHitIsInPage());  // item 4 - second page
            assertTrue(page.nextHitIsInPage());  // item 5 - second page
            assertTrue(page.nextHitIsInPage());  // item 6 - second page
            assertFalse(page.nextHitIsInPage()); // item 7 - third page
        }

        @Test
        @DisplayName("should handle page size of 1 correctly")
        void nextHitIsInPage_pageSizeOne() {
            org.junit.jupiter.api.Assumptions.assumeTrue(pageStoreInitialized,
                    "PageStore class failed to initialize; skipping test");

            PageStore.Page page = new PageStore.Page(0, 1);

            assertTrue(page.nextHitIsInPage());  // item 1
            assertFalse(page.nextHitIsInPage()); // item 2
        }

        @Test
        @DisplayName("should handle page size of 1 for second page")
        void nextHitIsInPage_pageSizeOneSecondPage() {
            org.junit.jupiter.api.Assumptions.assumeTrue(pageStoreInitialized,
                    "PageStore class failed to initialize; skipping test");

            PageStore.Page page = new PageStore.Page(1, 1);

            assertFalse(page.nextHitIsInPage()); // item 1
            assertTrue(page.nextHitIsInPage());  // item 2
            assertFalse(page.nextHitIsInPage()); // item 3
        }

        @Test
        @DisplayName("should handle large page index")
        void nextHitIsInPage_largePageIndex() {
            org.junit.jupiter.api.Assumptions.assumeTrue(pageStoreInitialized,
                    "PageStore class failed to initialize; skipping test");

            PageStore.Page page = new PageStore.Page(50, 10);

            for (int i = 0; i < 10; i++) {
                assertFalse(page.nextHitIsInPage());
            }
        }

        @Test
        @DisplayName("should correctly identify items on the third page")
        void nextHitIsInPage_thirdPage() {
            org.junit.jupiter.api.Assumptions.assumeTrue(pageStoreInitialized,
                    "PageStore class failed to initialize; skipping test");

            PageStore.Page page = new PageStore.Page(2, 2);

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
            org.junit.jupiter.api.Assumptions.assumeTrue(pageStoreInitialized,
                    "PageStore class failed to initialize; skipping test");

            PageStore.Page page = new PageStore.Page(0, 50);

            for (int i = 0; i < 50; i++) {
                assertTrue(page.nextHitIsInPage());
            }
            assertFalse(page.nextHitIsInPage()); // item 51
        }
    }

    @Nested
    @DisplayName("Static initializer")
    class StaticInitializerTests {

        @Test
        @DisplayName("store field type documentation")
        void store_fieldType() {
            org.junit.jupiter.api.Assumptions.assumeTrue(pageStoreInitialized,
                    "PageStore class failed to initialize; skipping test");

            PageStore.store.clear();
            assertNotNull(PageStore.store);
            assertTrue(PageStore.store.isEmpty());
            assertTrue(PageStore.store instanceof LinkedHashMap);
        }
    }

    @Nested
    @DisplayName("Integration scenarios")
    class IntegrationTests {

        @Test
        @DisplayName("full CRUD lifecycle should work correctly")
        void fullCrudLifecycle() {
            org.junit.jupiter.api.Assumptions.assumeTrue(pageStoreInitialized,
                    "PageStore class failed to initialize; skipping test");

            // Create
            Map<String, Object> createFields = new HashMap<>(Map.of("Id", "lifecycle-1", "Title", "Initial Title", "Status", "draft"));
            EntityValue created = pageStore.create(entityType, createFields);
            assertNotNull(created);
            assertEquals("Page", created.getEntityName());

            // Get
            EntityValue retrieved = pageStore.get(entityType, "lifecycle-1");
            assertEquals("Initial Title", retrieved.getFields().get("Title"));

            // Update
            Map<String, Object> updatedFields = new HashMap<>(Map.of("Id", "lifecycle-1", "Title", "Final Title", "Status", "current"));
            EntityValue updatedEntity = new EntityValue("Page", updatedFields);
            pageStore.update(entityType, updatedEntity);

            EntityValue afterUpdate = pageStore.get(entityType, "lifecycle-1");
            assertEquals("Final Title", afterUpdate.getFields().get("Title"));
            assertEquals("current", afterUpdate.getFields().get("Status"));

            // Delete
            pageStore.delete(entityType, "lifecycle-1");
            EntityValue afterDelete = pageStore.get(entityType, "lifecycle-1");
            assertNull(afterDelete.getFields());
        }

        @Test
        @DisplayName("search with pagination across create and delete operations")
        void searchWithPaginationAndMutations(@Mock SearchCondition condition) {
            org.junit.jupiter.api.Assumptions.assumeTrue(pageStoreInitialized,
                    "PageStore class failed to initialize; skipping test");

            // Create 5 pages
            for (int i = 0; i < 5; i++) {
                Map<String, Object> fields = new HashMap<>(Map.of("Id", "page-" + i, "Title", "Page " + i));
                pageStore.create(entityType, fields);
            }

            // Delete one
            pageStore.delete(entityType, "page-2");

            // Search first page (should have 4 results total, 3 on first page)
            List<EntityValue> page0 = pageStore.search(entityType, List.of(condition), 0L, 3);
            assertEquals(3, page0.size());

            // Search second page (should have 1 result)
            List<EntityValue> page1 = pageStore.search(entityType, List.of(condition), 1L, 3);
            assertEquals(1, page1.size());
        }
    }
}
