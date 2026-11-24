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

package com.kristasoft.extensions.essentials.collaboration.confluence.catalog.validators.strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import app.krista.extension.impl.anno.Attribute;
import app.krista.extension.impl.anno.Field;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.validators.Validator;

import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

/**
 * Strategy for handling @Field annotations with type="Identifier".
 * <p>
 * This strategy extracts attributes, field types, and validation resource mappings
 * from @Field annotations that have type="Identifier".
 * </p>
 */
public class IdentifierFieldAnnotationStrategy implements FieldAnnotationStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(IdentifierFieldAnnotationStrategy.class);

    @Override
    public boolean canHandle(Parameter parameter, String fieldName) {
        LOGGER.debug("        → IdentifierFieldAnnotationStrategy.canHandle()");
        LOGGER.debug("        → Parameter: {}, Field Name: {}", parameter.getType().getSimpleName(), fieldName);

        Field fieldAnnotation = parameter.getAnnotation(Field.class);
        boolean canHandle = fieldAnnotation != null && 
                           fieldName.equals(fieldAnnotation.name()) && 
                           "Identifier".equals(fieldAnnotation.type());

        if (fieldAnnotation != null) {
            LOGGER.debug("        → Found @Field annotation with name: {}, type: {}", fieldAnnotation.name(), fieldAnnotation.type());
            LOGGER.debug("        → Can handle: " + canHandle + " (field name match: " + fieldName.equals(fieldAnnotation.name()) + ", type match: " + "Identifier".equals(fieldAnnotation.type()) + ")");
        } else {
            LOGGER.debug("        → No @Field annotation found, cannot handle");
        }

        return canHandle;
    }

    @Override
    public Map<String, Object> extractAttributes(Parameter parameter, String fieldName) {
        LOGGER.debug("        → IdentifierFieldAnnotationStrategy.extractAttributes()");
        LOGGER.debug("        → Parameter: {}, Field Name: {}", parameter.getType().getSimpleName(), fieldName);

        Field fieldAnnotation = parameter.getAnnotation(Field.class);
        if (fieldAnnotation == null) {
            LOGGER.debug("        → No @Field annotation found, returning empty map");
            return Map.of();
        }

        if (!fieldName.equals(fieldAnnotation.name()) || !"Identifier".equals(fieldAnnotation.type())) {
            LOGGER.debug("        → Field name or type mismatch: expected '{}' with type 'Identifier', found '{}' with type '{}', returning empty map", 
                        fieldName, fieldAnnotation.name(), fieldAnnotation.type());
            return Map.of();
        }

        LOGGER.debug("        → ✓ Field name and type match! Extracting attributes from @Field annotation");
        LOGGER.debug("        → @Field annotation details: name='{}', type='{}'", fieldAnnotation.name(), fieldAnnotation.type());

        Map<String, Object> attributes = new HashMap<>();

        // Add custom attributes from the attributes array
        Attribute[] customAttributes = fieldAnnotation.attributes();
        LOGGER.debug("        → Processing {} custom attributes", customAttributes.length);
        for (Attribute attribute : customAttributes) {
            LOGGER.debug("        → Adding attribute: {}={}", attribute.name(), attribute.value());
            attributes.put(attribute.name(), attribute.value());
        }

        LOGGER.debug("        → Final extracted attributes from IdentifierFieldAnnotationStrategy: {}", attributes);
        return attributes;
    }

    @Override
    public String extractFieldType(Parameter parameter, String fieldName) {
        LOGGER.debug("        → IdentifierFieldAnnotationStrategy.extractFieldType()");
        LOGGER.debug("        → Parameter: {}, Field Name: {}", parameter.getType().getSimpleName(), fieldName);

        Field fieldAnnotation = parameter.getAnnotation(Field.class);
        if (fieldAnnotation == null) {
            LOGGER.debug("        → No @Field annotation found, returning null");
            return null;
        }

        if (!fieldName.equals(fieldAnnotation.name()) || !"Identifier".equals(fieldAnnotation.type())) {
            LOGGER.debug("        → Field name or type mismatch: expected '{}' with type 'Identifier', found '{}' with type '{}', returning null", 
                        fieldName, fieldAnnotation.name(), fieldAnnotation.type());
            return null;
        }

        LOGGER.debug("        → ✓ Field name and type match! Found @Field annotation: name='{}' → type='Identifier'", fieldName);
        LOGGER.debug("        → Returning field type from IdentifierFieldAnnotationStrategy: Identifier");
        return "Identifier";
    }

    @Override
    public Validator.ValidationResource mapToValidationResource(Parameter parameter, String fieldName) {
        LOGGER.debug("        → IdentifierFieldAnnotationStrategy.mapToValidationResource()");
        LOGGER.debug("        → Parameter: {}, Field Name: {}", parameter.getType().getSimpleName(), fieldName);

        Field fieldAnnotation = parameter.getAnnotation(Field.class);
        if (fieldAnnotation == null) {
            LOGGER.debug("        → No @Field annotation found, returning null");
            return null;
        }

        if (!fieldName.equals(fieldAnnotation.name()) || !"Identifier".equals(fieldAnnotation.type())) {
            LOGGER.debug("        → Field name or type mismatch: expected '{}' with type 'Identifier', found '{}' with type '{}', returning null", 
                        fieldName, fieldAnnotation.name(), fieldAnnotation.type());
            return null;
        }

        LOGGER.debug("        → ✓ Field name and type match! Mapping @Field annotation: name='{}', type='Identifier'", fieldName);
        Validator.ValidationResource resource = mapFieldTypeToValidationResource("Identifier", fieldName);
        LOGGER.debug("        → IdentifierFieldAnnotationStrategy mapped to validation resource: {}", resource);
        return resource;
    }

    @Override
    public Class<? extends Annotation> getAnnotationType() {
        return Field.class;
    }

    @Override
    public String getStrategyName() {
        return "IdentifierField";
    }

    /**
     * Maps a field type and name to the appropriate ValidationResource for @Field annotations with type="Identifier".
     *
     * @param fieldType the type of the field (always "Identifier" for this strategy)
     * @param fieldName the name of the field
     * @return the corresponding ValidationResource
     */
    private Validator.ValidationResource mapFieldTypeToValidationResource(String fieldType, String fieldName) {
        LOGGER.debug("          → IdentifierFieldAnnotationStrategy.mapFieldTypeToValidationResource()");
        LOGGER.debug("          → Mapping field '{}' (type: '{}') to validation resource", fieldName, fieldType);

        // Space-specific field names
        if ("Space ID".equals(fieldName) || "Space Id".equals(fieldName) || "Space Id or Key".equals(fieldName)) {
            LOGGER.debug("          → ✓ Matched specific field name 'Space ID/Space Id/Space Id or Key' → SPACE_ID");
            return Validator.ValidationResource.SPACE_ID;
        }

        if ("Space Key".equals(fieldName)) {
            LOGGER.debug("          → ✓ Matched specific field name 'Space Key' → SPACE_KEY");
            return Validator.ValidationResource.SPACE_KEY;
        }

        if ("Space Name".equals(fieldName)) {
            LOGGER.debug("          → ✓ Matched specific field name 'Space Name' → SPACE_NAME");
            return Validator.ValidationResource.SPACE_NAME;
        }

        // Page-specific field names
        if ("Page ID".equals(fieldName) || "Page Id".equals(fieldName) || "PageId".equals(fieldName)) {
            LOGGER.debug("          → ✓ Matched specific field name 'Page ID/Page Id/PageId' → PAGE_ID");
            return Validator.ValidationResource.PAGE_ID;
        }

        // Template-specific field names
        if ("Template ID".equals(fieldName) || "Template Id".equals(fieldName) || "TemplateId".equals(fieldName)) {
            LOGGER.debug("          → ✓ Matched specific field name 'Template ID/Template Id/TemplateId' → TEMPLATE_ID");
            return Validator.ValidationResource.TEMPLATE_ID;
        }

        // Default fallback
        LOGGER.debug("          → ⚠ No mapping found for identifier field '{}' → null", fieldName);
        return null;
    }
}
