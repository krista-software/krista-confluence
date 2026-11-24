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

package com.kristasoft.extensions.essentials.collaboration.confluence.catalog.validators;

import app.krista.extension.impl.anno.Attribute;
import app.krista.extension.impl.anno.Field;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.validators.strategy.FieldAnnotationStrategy;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.validators.strategy.FieldAnnotationStrategyRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for extracting field attributes from method parameters using reflection.
 * <p>
 * This class provides methods to dynamically extract field attributes from catalog request methods,
 * eliminating the need for hardcoded field attributes in validation orchestrators.
 * </p>
 * <p>
 * This class uses the Strategy pattern to handle different field annotation types, making it
 * easy to add new annotation types without modifying existing code.
 * </p>
 */
public class FieldAttributeExtractor {

    private static final Logger LOGGER = LoggerFactory.getLogger(FieldAttributeExtractor.class);
    private static final FieldAnnotationStrategyRegistry strategyRegistry = new FieldAnnotationStrategyRegistry();

    /**
     * Extracts field attributes for a specific field name from a given method.
     * <p>
     * This method uses reflection to examine the method parameters and their annotations
     * to extract the attributes associated with a field that matches the given field name.
     * </p>
     * <p>
     * Uses the Strategy pattern to delegate attribute extraction to the appropriate strategy
     * based on the annotation type found.
     * </p>
     *
     * @param method the method to examine for field annotations
     * @param fieldName the name of the field to extract attributes for
     * @return a map containing the field attributes, or an empty map if no attributes are found
     */
    public static Map<String, Object> extractFieldAttributes(Method method, String fieldName) {
        LOGGER.debug("→ FieldAttributeExtractor.extractFieldAttributes()");
        LOGGER.debug("→ Method: {}", method != null ? method.getName() : "null");
        LOGGER.debug("→ Field Name: {}", fieldName);

        if (method == null || fieldName == null) {
            LOGGER.debug("→ Null method or field name, returning empty map");
            return Map.of();
        }

        Parameter[] parameters = method.getParameters();
        LOGGER.debug("→ Examining {} parameters...", parameters.length);

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            LOGGER.debug("→ Parameter {}: {}", i, parameter.getType().getSimpleName());

            // Use strategy pattern to extract attributes
            Map<String, Object> attributes = strategyRegistry.extractAttributes(parameter, fieldName);
            if (!attributes.isEmpty()) {
                LOGGER.debug("→ ✓ Field name matches! Attributes extracted using strategy pattern");
                return attributes;
            }
        }

        LOGGER.debug("→ No matching field annotation found for: {}", fieldName);
        return Map.of();
    }

    /**
     * Extracts the field type for a specific field name from a given method.
     * <p>
     * This method uses reflection to examine the method parameters and their annotations
     * to extract the field type associated with a field that matches the given field name.
     * </p>
     * <p>
     * Uses the Strategy pattern to delegate field type extraction to the appropriate strategy
     * based on the annotation type found.
     * </p>
     *
     * @param method the method to examine for field annotations
     * @param fieldName the name of the field to extract the type for
     * @return the field type string, or null if no matching field is found
     */
    public static String extractFieldType(Method method, String fieldName) {
        LOGGER.debug("→ FieldAttributeExtractor.extractFieldType()");
        LOGGER.debug("→ Method: {}", method != null ? method.getName() : "null");
        LOGGER.debug("→ Field Name: {}", fieldName);

        if (method == null || fieldName == null) {
            LOGGER.debug("→ Null method or field name, returning null");
            return null;
        }

        Parameter[] parameters = method.getParameters();
        LOGGER.debug("→ Examining {} parameters for field type...", parameters.length);

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            LOGGER.debug("→ Parameter {}: {}", i, parameter.getType().getSimpleName());

            // Use strategy pattern to extract field type
            String fieldType = strategyRegistry.extractFieldType(parameter, fieldName);
            if (fieldType != null) {
                LOGGER.debug("→ ✓ Field name matches! Field type extracted using strategy pattern: {}", fieldType);
                return fieldType;
            }
        }

        LOGGER.debug("→ No matching field annotation found for field type extraction: {}", fieldName);
        return null;
    }

    /**
     * Gets the method by name from a given class.
     * <p>
     * This is a utility method to help find the specific catalog request method
     * that contains the field definitions we want to extract attributes from.
     * </p>
     *
     * @param clazz the class to search for the method
     * @param methodName the name of the method to find
     * @return the Method object if found, null otherwise
     */
    public static Method getMethodByName(Class<?> clazz, String methodName) {
        if (clazz == null || methodName == null) {
            return null;
        }

        for (Method method : clazz.getDeclaredMethods()) {
            if (methodName.equals(method.getName())) {
                return method;
            }
        }
        
        return null;
    }


    public static Map<String, Validator.ValidationResource> createFieldNameToResourceMapping(Method method) {
        LOGGER.debug("→ FieldAttributeExtractor.createFieldNameToResourceMapping()");
        LOGGER.debug("→ Method: {}", method != null ? method.getName() : "null");

        Map<String, Validator.ValidationResource> mapping = new HashMap<>();

        if (method == null) {
            LOGGER.debug("→ Method is null, returning empty mapping");
            return mapping;
        }

        Parameter[] parameters = method.getParameters();
        LOGGER.debug("→ Processing {} parameters to create field mappings...", parameters.length);

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            LOGGER.debug("→ Parameter {}: {}", i, parameter.getType().getSimpleName());

            // Use strategy pattern to find field name and validation resource
            for (FieldAnnotationStrategy strategy : strategyRegistry.getAllStrategies()) {
                String fieldName = extractFieldNameFromParameter(parameter, strategy);
                if (fieldName != null) {
                    LOGGER.debug("→ Found field annotation using {} strategy: name='{}'", strategy.getStrategyName(), fieldName);

                    Validator.ValidationResource resource = strategy.mapToValidationResource(parameter, fieldName);
                    LOGGER.debug("→ Mapped to validation resource: {}", resource);

                    if (resource != null) {
                        LOGGER.debug("→ ✓ Adding mapping: '{}' → {}", fieldName, resource);
                        mapping.put(fieldName, resource);
                    } else {
                        LOGGER.debug("→ ⚠ Field '{}' could not be mapped to a validation resource", fieldName);
                    }
                    break; // Found a matching strategy, no need to check others
                }
            }
        }

        LOGGER.debug("→ Final field name to resource mapping: {}", mapping);
        return mapping;
    }

    /**
     * Extracts the field name from a parameter using the given strategy.
     * <p>
     * This helper method attempts to extract the field name from a parameter's annotation
     * using the provided strategy. It returns null if the strategy cannot handle the parameter.
     * </p>
     *
     * @param parameter the method parameter to extract the field name from
     * @param strategy the strategy to use for extraction
     * @return the field name if found, null otherwise
     */
    private static String extractFieldNameFromParameter(Parameter parameter, FieldAnnotationStrategy strategy) {
        // Try to extract field name by checking if the strategy can handle any field name
        // We'll use a dummy field name to check if the strategy can handle this parameter
        // Then extract the actual field name from the annotation

        if (strategy.getAnnotationType() == Field.class) {
            Field annotation = parameter.getAnnotation(Field.class);
            return annotation != null ? annotation.name() : null;
        } else if (strategy.getAnnotationType() == Field.Date.class) {
            Field.Date annotation = parameter.getAnnotation(Field.Date.class);
            return annotation != null ? annotation.name() : null;
        } else if (strategy.getAnnotationType() == Field.Text.class) {
            Field.Text annotation = parameter.getAnnotation(Field.Text.class);
            return annotation != null ? annotation.name() : null;
        } else if (strategy.getAnnotationType() == Field.Boolean.class) {
            Field.Boolean annotation = parameter.getAnnotation(Field.Boolean.class);
            return annotation != null ? annotation.name() : null;
        } else if (strategy.getAnnotationType() == Field.Desc.class) {
            Field.Desc annotation = parameter.getAnnotation(Field.Desc.class);
            return annotation != null ? annotation.name() : null;
        } else if (strategy.getAnnotationType() == Field.File.class) {
            Field.File annotation = parameter.getAnnotation(Field.File.class);
            return annotation != null ? annotation.name() : null;
        }

        return null;
    }
}
