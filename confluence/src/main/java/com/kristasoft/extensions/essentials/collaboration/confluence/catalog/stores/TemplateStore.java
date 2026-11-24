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
import app.krista.extension.impl.anno.EntityRequest;
import app.krista.model.base.EntityValue;
import app.krista.model.entity.EntityType;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.entities.Template;

import java.util.*;
import java.util.stream.Collectors;

public class TemplateStore {
    public static Map<Pair<String, String>, Map<String, Object>> store = new LinkedHashMap<>(); // Pair<entityName, primaryKey> -> fields
    private static final String PRIMARY_KEY_NAME = Template.class.getAnnotation(Entity.class).primaryKey();

    private static final List<Class<?>> entityClass = List.of(
            Template.class
    );

    // FIXME - Sonar cube unused warning
    private static final Map<String, String> primaryKeyMap = createPrimaryKeyMap();

    private static Map<String, String> createPrimaryKeyMap() {
        Map<String, String> map = new HashMap<>();
        entityClass.forEach(aClass -> {
            Entity entity = aClass.getAnnotation(Entity.class);
            map.put(entity.name(), entity.primaryKey());
        });
        return Collections.unmodifiableMap(map);
    }

    @EntityRequest(EntityRequest.Type.CREATE)
    public EntityValue create(EntityType entityType, Map<String, Object> fields) {
        String primaryKey = (String) fields.get(PRIMARY_KEY_NAME);
        Pair<String, String> pairKey = Pair.of(entityType.getEntityName(), primaryKey);
        store.put(pairKey, fields);
        return new EntityValue(entityType.getEntityName(), fields);
    }

    @EntityRequest(EntityRequest.Type.GET)
    public EntityValue get(EntityType entityType, String primaryKey) {
        Pair<String, String> pairKey = Pair.of(entityType.getEntityName(), primaryKey);
        return new EntityValue(entityType.getEntityName(), store.get(pairKey));
    }

    @EntityRequest(EntityRequest.Type.SEARCH)
    public List<EntityValue> search(EntityType entityType, List<SearchCondition> conditions, Long pageIndex, Integer pageSize) {
        Page page = new Page(pageIndex, pageSize);
        return store.entrySet().stream()
                .filter(entry -> Objects.equals(entry.getKey().getKey(), entityType.getEntityName()))
                .map(Map.Entry::getValue)
                .filter(fields -> filterEntity(fields, conditions))
                .filter(shipment -> page.nextHitIsInPage())
                .map(field -> new EntityValue(entityType.getEntityName(), field)).collect(Collectors.toList());
    }

    private boolean filterEntity(Map<String, Object> data, List<SearchCondition> conditions) {
        return conditions.stream().anyMatch(condition -> filterEntity(data, condition));
    }

    private boolean filterEntity(Map<String, Object> data, SearchCondition condition) {
        // implement your filter condition here
        return true;
    }

    @EntityRequest(EntityRequest.Type.UPDATE)
    public EntityValue update(EntityType entityType, EntityValue entityValue) {
        String primaryKey = (String) entityValue.getFields().get(PRIMARY_KEY_NAME);
        Pair<String, String> pairKey = Pair.of(entityType.getEntityName(), primaryKey);
        store.put(pairKey, entityValue.getFields());
        return entityValue;
    }

    @EntityRequest(EntityRequest.Type.DELETE)
    public Void delete(EntityType entityType, String primaryKey) {
        Pair<String, String> pairKey = Pair.of(entityType.getEntityName(), primaryKey);
        store.remove(pairKey);
        return null;
    }

    public static class Page {

        private final long pageIndex;
        private final int pageSize;

        private long currentIndex;

        public Page(long pageIndex, int pageSize) {
            this.pageIndex = pageIndex;
            this.pageSize = pageSize;
        }

        public boolean nextHitIsInPage() {
            currentIndex++;
            return pageIndex * pageSize < currentIndex && (pageIndex + 1) * pageSize >= currentIndex;
        }
    }
}
