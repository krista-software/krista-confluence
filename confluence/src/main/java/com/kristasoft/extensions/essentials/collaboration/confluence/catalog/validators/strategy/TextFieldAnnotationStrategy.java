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
 * Strategy for handling @Field.Text annotations.
 * <p>
 * This strategy extracts attributes, field types, and validation resource mappings
 * from @Field.Text annotations.
 * </p>
 */
public class TextFieldAnnotationStrategy implements FieldAnnotationStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(TextFieldAnnotationStrategy.class);

    @Override
    public boolean canHandle(Parameter parameter, String fieldName) {
        LOGGER.debug("        → TextFieldAnnotationStrategy.canHandle()");
        LOGGER.debug("        → Parameter: {}, Field Name: {}", parameter.getType().getSimpleName(), fieldName);

        Field.Text textAnnotation = parameter.getAnnotation(Field.Text.class);
        boolean canHandle = textAnnotation != null && fieldName.equals(textAnnotation.name());

        if (textAnnotation != null) {
            LOGGER.debug("        → Found @Field.Text annotation with name: {}", textAnnotation.name());
            LOGGER.debug("        → Can handle: " + canHandle + " (field name match: " + fieldName.equals(textAnnotation.name()) + ")");
        } else {
            LOGGER.debug("        → No @Field.Text annotation found, cannot handle");
        }

        return canHandle;
    }

    @Override
    public Map<String, Object> extractAttributes(Parameter parameter, String fieldName) {
        LOGGER.debug("        → TextFieldAnnotationStrategy.extractAttributes()");
        LOGGER.debug("        → Parameter: {}, Field Name: {}", parameter.getType().getSimpleName(), fieldName);

        Field.Text textAnnotation = parameter.getAnnotation(Field.Text.class);
        if (textAnnotation == null) {
            LOGGER.debug("        → No @Field.Text annotation found, returning empty map");
            return Map.of();
        }

        if (!fieldName.equals(textAnnotation.name())) {
            LOGGER.debug("        → Field name mismatch: expected '" + fieldName + "', found '" + textAnnotation.name() + "', returning empty map");
            return Map.of();
        }

        LOGGER.debug("        → ✓ Field name matches! Extracting attributes from @Field.Text annotation");
        LOGGER.debug("        → @Field.Text annotation details: name='" + textAnnotation.name() + "'");

        Map<String, Object> attributes = new HashMap<>();

        // Add custom attributes from the attributes array
        Attribute[] customAttributes = textAnnotation.attributes();
        LOGGER.debug("        → Processing {} custom attributes", customAttributes.length);
        for (Attribute attribute : customAttributes) {
            LOGGER.debug("        → Adding attribute: {}={}", attribute.name(), attribute.value());
            attributes.put(attribute.name(), attribute.value());
        }

        LOGGER.debug("        → Final extracted attributes from TextFieldAnnotationStrategy: {}", attributes);
        return attributes;
    }

    @Override
    public String extractFieldType(Parameter parameter, String fieldName) {
        LOGGER.debug("        → TextFieldAnnotationStrategy.extractFieldType()");
        LOGGER.debug("        → Parameter: {}, Field Name: {}", parameter.getType().getSimpleName(), fieldName);

        Field.Text textAnnotation = parameter.getAnnotation(Field.Text.class);
        if (textAnnotation == null) {
            LOGGER.debug("        → No @Field.Text annotation found, returning null");
            return null;
        }

        if (!fieldName.equals(textAnnotation.name())) {
            LOGGER.debug("        → Field name mismatch: expected '" + fieldName + "', found '" + textAnnotation.name() + "', returning null");
            return null;
        }

        LOGGER.debug("        → ✓ Field name matches! Found @Field.Text annotation: name='" + fieldName + "' → type='Text'");
        LOGGER.debug("        → Returning field type from TextFieldAnnotationStrategy: Text");
        return "Text";
    }

    @Override
    public Validator.ValidationResource mapToValidationResource(Parameter parameter, String fieldName) {
        LOGGER.debug("        → TextFieldAnnotationStrategy.mapToValidationResource()");
        LOGGER.debug("        → Parameter: {}, Field Name: {}", parameter.getType().getSimpleName(), fieldName);

        Field.Text textAnnotation = parameter.getAnnotation(Field.Text.class);
        if (textAnnotation == null) {
            LOGGER.debug("        → No @Field.Text annotation found, returning null");
            return null;
        }

        if (!fieldName.equals(textAnnotation.name())) {
            LOGGER.debug("        → Field name mismatch: expected '" + fieldName + "', found '" + textAnnotation.name() + "', returning null");
            return null;
        }

        LOGGER.debug("        → ✓ Field name matches! Mapping @Field.Text annotation: name='" + fieldName + "'");
        Validator.ValidationResource resource = mapFieldTypeToValidationResource("Text", fieldName);
        LOGGER.debug("        → TextFieldAnnotationStrategy mapped to validation resource: {}", resource);
        return resource;
    }

    @Override
    public Class<? extends Annotation> getAnnotationType() {
        return Field.Text.class;
    }

    @Override
    public String getStrategyName() {
        return "TextField";
    }

    /**
     * Maps a field type and name to the appropriate ValidationResource for @Field.Text annotations.
     *
     * @param fieldType the type of the field (always "Text" for this strategy)
     * @param fieldName the name of the field
     * @return the corresponding ValidationResource
     */
    private Validator.ValidationResource mapFieldTypeToValidationResource(String fieldType, String fieldName) {


        // Space-specific field names
        if ("Space Key".equals(fieldName)) {
            LOGGER.debug("          → ✓ Matched specific field name 'Space Key' → SPACE_KEY");
            return Validator.ValidationResource.SPACE_KEY;
        }

        if ("Space Name".equals(fieldName)) {
            LOGGER.debug("          → ✓ Matched specific field name 'Space Name' → SPACE_NAME");
            return Validator.ValidationResource.SPACE_NAME;
        }

        if("Space Id".equals(fieldName) || "Space ID".equals(fieldName)){
            LOGGER.debug("          → ✓ Matched specific field name 'Space Id/Space ID' → SPACE_ID");
            return Validator.ValidationResource.SPACE_ID;
        }

        if("Page ID".equals(fieldName) || "PageId".equals(fieldName) || "Page Id".equals(fieldName)){
            LOGGER.debug("          → ✓ Matched specific field name 'Page ID/PageId' → PAGE_ID");
            return Validator.ValidationResource.PAGE_ID;
        }

        // Template-specific field names
        if ("Template ID".equals(fieldName) || "Template Id".equals(fieldName) || "TemplateId".equals(fieldName)) {
            LOGGER.debug("          → ✓ Matched specific field name 'Template ID/Template Id/TemplateId' → TEMPLATE_ID");
            return Validator.ValidationResource.TEMPLATE_ID;
        }

        // Default fallback
        LOGGER.debug("          → ⚠ No mapping found for text field '" + fieldName + "' → null");
        return null;
    }
}
