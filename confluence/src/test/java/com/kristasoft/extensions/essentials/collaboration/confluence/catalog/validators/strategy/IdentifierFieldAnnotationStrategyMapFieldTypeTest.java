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

import app.krista.extension.impl.anno.Field;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.validators.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Parameter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 100% branch-coverage tests for the private helper
 * {@code IdentifierFieldAnnotationStrategy.mapFieldTypeToValidationResource},
 * exercised via the public
 * {@link IdentifierFieldAnnotationStrategy#mapToValidationResource}.
 *
 * <h2>Branch map – mapFieldTypeToValidationResource</h2>
 * <pre>
 *   if "Space ID" || "Space Id" || "Space Id or Key"
 *       first  clause true  (Space ID)                     → SPACE_ID           (test 1)
 *       first  false, second true (Space Id)               → SPACE_ID           (test 2)
 *       first+second false, third true (Space Id or Key)   → SPACE_ID           (test 3)
 *   if "Space Key"                                         → SPACE_KEY          (test 4)
 *   if "Space Name"                                        → SPACE_NAME         (test 5)
 *   if "Page ID" || "Page Id" || "PageId"
 *       first  clause true  (Page ID)                      → PAGE_ID            (test 6)
 *       first  false, second true (Page Id)                → PAGE_ID            (test 7)
 *       first+second false, third true (PageId)            → PAGE_ID            (test 8)
 *   if "Template ID" || "Template Id" || "TemplateId"
 *       first  clause true  (Template ID)                  → TEMPLATE_ID        (test 9)
 *       first  false, second true (Template Id)            → TEMPLATE_ID        (test 10)
 *       first+second false, third true (TemplateId)        → TEMPLATE_ID        (test 11)
 *   default fallback (no condition matches)               → null               (test 12)
 * </pre>
 *
 * <p>{@link java.lang.reflect.Parameter#getAnnotation} is a {@code final} JDK
 * method that cannot be mocked with Mockito. Real {@link Parameter} instances are
 * obtained via reflection from static stub methods annotated with
 * {@code @Field(name = …, type = "Identifier")}.  The {@code type = "Identifier"}
 * guard in {@code mapToValidationResource} must match for the call to reach the
 * private helper.</p>
 */
@DisplayName("IdentifierFieldAnnotationStrategy.mapFieldTypeToValidationResource – 100% branch coverage")
class IdentifierFieldAnnotationStrategyMapFieldTypeTest {

    private IdentifierFieldAnnotationStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new IdentifierFieldAnnotationStrategy();
    }

    // ─── annotated stub methods ───────────────────────────────────────────────
    // type = "Identifier" is required so mapToValidationResource passes the type guard.

    static void spaceIdUc    (@Field(name = "Space ID",          type = "Identifier") String p) {}
    static void spaceId      (@Field(name = "Space Id",          type = "Identifier") String p) {}
    static void spaceIdOrKey (@Field(name = "Space Id or Key",   type = "Identifier") String p) {}
    static void spaceKey     (@Field(name = "Space Key",         type = "Identifier") String p) {}
    static void spaceName    (@Field(name = "Space Name",        type = "Identifier") String p) {}
    static void pageIdUc     (@Field(name = "Page ID",           type = "Identifier") String p) {}
    static void pageId       (@Field(name = "Page Id",           type = "Identifier") String p) {}
    static void pageIdNoSp   (@Field(name = "PageId",            type = "Identifier") String p) {}
    static void templateIdUc (@Field(name = "Template ID",       type = "Identifier") String p) {}
    static void templateId   (@Field(name = "Template Id",       type = "Identifier") String p) {}
    static void templateIdNoSp(@Field(name = "TemplateId",       type = "Identifier") String p) {}
    static void unknown      (@Field(name = "Unknown Field",     type = "Identifier") String p) {}

    /** Returns the first {@link Parameter} of the named stub method via reflection. */
    private Parameter param(String methodName) throws NoSuchMethodException {
        return IdentifierFieldAnnotationStrategyMapFieldTypeTest.class
                .getDeclaredMethod(methodName, String.class)
                .getParameters()[0];
    }

    // ─── tests ────────────────────────────────────────────────────────────────

    /**
     * Test 1 – {@code "Space ID"} → {@code SPACE_ID}  (first {@code ||} clause).
     */
    @Test
    @DisplayName("\"Space ID\" → SPACE_ID  (first || clause)")
    void spaceIdUpperCase_returnsSpaceId() throws NoSuchMethodException {
        assertEquals(Validator.ValidationResource.SPACE_ID,
                strategy.mapToValidationResource(param("spaceIdUc"), "Space ID"));
    }

    /**
     * Test 2 – {@code "Space Id"} → {@code SPACE_ID}  (second {@code ||} clause;
     * first clause {@code "Space ID".equals("Space Id")} is false).
     */
    @Test
    @DisplayName("\"Space Id\" → SPACE_ID  (second || clause)")
    void spaceIdMixedCase_returnsSpaceId() throws NoSuchMethodException {
        assertEquals(Validator.ValidationResource.SPACE_ID,
                strategy.mapToValidationResource(param("spaceId"), "Space Id"));
    }

    /**
     * Test 3 – {@code "Space Id or Key"} → {@code SPACE_ID}  (third {@code ||} clause;
     * first and second clauses are both false).
     */
    @Test
    @DisplayName("\"Space Id or Key\" → SPACE_ID  (third || clause)")
    void spaceIdOrKey_returnsSpaceId() throws NoSuchMethodException {
        assertEquals(Validator.ValidationResource.SPACE_ID,
                strategy.mapToValidationResource(param("spaceIdOrKey"), "Space Id or Key"));
    }

    /**
     * Test 4 – {@code "Space Key"} → {@code SPACE_KEY}.
     */
    @Test
    @DisplayName("\"Space Key\" → SPACE_KEY")
    void spaceKey_returnsSpaceKey() throws NoSuchMethodException {
        assertEquals(Validator.ValidationResource.SPACE_KEY,
                strategy.mapToValidationResource(param("spaceKey"), "Space Key"));
    }

    /**
     * Test 5 – {@code "Space Name"} → {@code SPACE_NAME}.
     */
    @Test
    @DisplayName("\"Space Name\" → SPACE_NAME")
    void spaceName_returnsSpaceName() throws NoSuchMethodException {
        assertEquals(Validator.ValidationResource.SPACE_NAME,
                strategy.mapToValidationResource(param("spaceName"), "Space Name"));
    }

    /**
     * Test 6 – {@code "Page ID"} → {@code PAGE_ID}  (first {@code ||} clause).
     */
    @Test
    @DisplayName("\"Page ID\" → PAGE_ID  (first || clause)")
    void pageIdUpperCase_returnsPageId() throws NoSuchMethodException {
        assertEquals(Validator.ValidationResource.PAGE_ID,
                strategy.mapToValidationResource(param("pageIdUc"), "Page ID"));
    }

    /**
     * Test 7 – {@code "Page Id"} → {@code PAGE_ID}  (second {@code ||} clause).
     */
    @Test
    @DisplayName("\"Page Id\" → PAGE_ID  (second || clause)")
    void pageIdMixedCase_returnsPageId() throws NoSuchMethodException {
        assertEquals(Validator.ValidationResource.PAGE_ID,
                strategy.mapToValidationResource(param("pageId"), "Page Id"));
    }

    /**
     * Test 8 – {@code "PageId"} → {@code PAGE_ID}  (third {@code ||} clause;
     * first and second clauses are both false).
     */
    @Test
    @DisplayName("\"PageId\" → PAGE_ID  (third || clause)")
    void pageIdNoSpace_returnsPageId() throws NoSuchMethodException {
        assertEquals(Validator.ValidationResource.PAGE_ID,
                strategy.mapToValidationResource(param("pageIdNoSp"), "PageId"));
    }

    /**
     * Test 9 – {@code "Template ID"} → {@code TEMPLATE_ID}  (first {@code ||} clause).
     */
    @Test
    @DisplayName("\"Template ID\" → TEMPLATE_ID  (first || clause)")
    void templateIdUpperCase_returnsTemplateId() throws NoSuchMethodException {
        assertEquals(Validator.ValidationResource.TEMPLATE_ID,
                strategy.mapToValidationResource(param("templateIdUc"), "Template ID"));
    }

    /**
     * Test 10 – {@code "Template Id"} → {@code TEMPLATE_ID}  (second {@code ||} clause).
     */
    @Test
    @DisplayName("\"Template Id\" → TEMPLATE_ID  (second || clause)")
    void templateIdMixedCase_returnsTemplateId() throws NoSuchMethodException {
        assertEquals(Validator.ValidationResource.TEMPLATE_ID,
                strategy.mapToValidationResource(param("templateId"), "Template Id"));
    }

    /**
     * Test 11 – {@code "TemplateId"} → {@code TEMPLATE_ID}  (third {@code ||} clause;
     * first and second clauses are both false).
     */
    @Test
    @DisplayName("\"TemplateId\" → TEMPLATE_ID  (third || clause)")
    void templateIdNoSpace_returnsTemplateId() throws NoSuchMethodException {
        assertEquals(Validator.ValidationResource.TEMPLATE_ID,
                strategy.mapToValidationResource(param("templateIdNoSp"), "TemplateId"));
    }

    /**
     * Test 12 – Unrecognised field name → {@code null}  (default fallback).
     * All conditions are false; exercises the {@code return null} branch.
     */
    @Test
    @DisplayName("unrecognised field name → null  (default fallback)")
    void unknownField_returnsNull() throws NoSuchMethodException {
        assertNull(strategy.mapToValidationResource(param("unknown"), "Unknown Field"));
    }
}
