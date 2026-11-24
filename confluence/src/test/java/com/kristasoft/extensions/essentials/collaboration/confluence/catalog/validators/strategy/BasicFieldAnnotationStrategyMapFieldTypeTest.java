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
 * {@code BasicFieldAnnotationStrategy.mapFieldTypeToValidationResource},
 * exercised via the public
 * {@link BasicFieldAnnotationStrategy#mapToValidationResource}.
 *
 * <h2>Branch map – mapFieldTypeToValidationResource</h2>
 * <pre>
 *   if "Date From"                                          → FROM_DATE          (test 1)
 *   if "Date To"                                           → TO_DATE            (test 2)
 *   if "Event ID"                                          → EVENT_ID           (test 3)
 *   if "Number of meeting" || "Number of meetings"
 *       first  clause true  (Number of meeting)            → NUMBER_OF_MEETINGS (test 4)
 *       first  clause false, second true (Number of meetings) → NUMBER_OF_MEETINGS (test 5)
 *   if "Time Zone"                                         → TIME_ZONE          (test 6)
 *   if "Space Id" || "Space ID" || "Space Id or Key"
 *       first  clause true  (Space Id)                     → SPACE_ID           (test 7)
 *       first  false, second true (Space ID)               → SPACE_ID           (test 8)
 *       first+second false, third true (Space Id or Key)   → SPACE_ID           (test 9)
 *   default fallback (no condition matches)               → null               (test 10)
 * </pre>
 *
 * <p>{@link java.lang.reflect.Parameter#getAnnotation} is a {@code final} JDK
 * method and cannot be mocked with Mockito. Real {@link Parameter} instances are
 * obtained via reflection from static stub methods declared below, each carrying
 * the exact {@code @Field(name = …)} required for its branch.</p>
 */
@DisplayName("BasicFieldAnnotationStrategy.mapFieldTypeToValidationResource – 100% branch coverage")
class BasicFieldAnnotationStrategyMapFieldTypeTest {

    private BasicFieldAnnotationStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new BasicFieldAnnotationStrategy();
    }

    // ─── annotated stub methods ───────────────────────────────────────────────
    // One method per branch; name() in @Field matches the fieldName passed to the
    // strategy so mapToValidationResource delegates to mapFieldTypeToValidationResource.

    static void dateFrom   (@Field(name = "Date From",          type = "Text") String p) {}
    static void dateTo     (@Field(name = "Date To",            type = "Text") String p) {}
    static void eventId    (@Field(name = "Event ID",           type = "Text") String p) {}
    static void numMeeting (@Field(name = "Number of meeting",  type = "Text") String p) {}
    static void numMeetings(@Field(name = "Number of meetings", type = "Text") String p) {}
    static void timeZone   (@Field(name = "Time Zone",          type = "Text") String p) {}
    static void spaceId    (@Field(name = "Space Id",           type = "Text") String p) {}
    static void spaceIdUc  (@Field(name = "Space ID",           type = "Text") String p) {}
    static void spaceIdKey (@Field(name = "Space Id or Key",    type = "Text") String p) {}
    static void unknown    (@Field(name = "Unknown Field",      type = "Text") String p) {}

    /** Returns the first {@link Parameter} of the named stub method via reflection. */
    private Parameter param(String methodName) throws NoSuchMethodException {
        return BasicFieldAnnotationStrategyMapFieldTypeTest.class
                .getDeclaredMethod(methodName, String.class)
                .getParameters()[0];
    }

    // ─── tests ────────────────────────────────────────────────────────────────

    /**
     * Test 1 – {@code "Date From"} → {@code FROM_DATE}.
     */
    @Test
    @DisplayName("\"Date From\" → FROM_DATE")
    void dateFrom_returnsFromDate() throws NoSuchMethodException {
        assertEquals(Validator.ValidationResource.FROM_DATE,
                strategy.mapToValidationResource(param("dateFrom"), "Date From"));
    }

    /**
     * Test 2 – {@code "Date To"} → {@code TO_DATE}.
     */
    @Test
    @DisplayName("\"Date To\" → TO_DATE")
    void dateTo_returnsToDDate() throws NoSuchMethodException {
        assertEquals(Validator.ValidationResource.TO_DATE,
                strategy.mapToValidationResource(param("dateTo"), "Date To"));
    }

    /**
     * Test 3 – {@code "Event ID"} → {@code EVENT_ID}.
     */
    @Test
    @DisplayName("\"Event ID\" → EVENT_ID")
    void eventId_returnsEventId() throws NoSuchMethodException {
        assertEquals(Validator.ValidationResource.EVENT_ID,
                strategy.mapToValidationResource(param("eventId"), "Event ID"));
    }

    /**
     * Test 4 – {@code "Number of meeting"} → {@code NUMBER_OF_MEETINGS}.
     * Exercises the first {@code ||} clause (short-circuit; second clause not evaluated).
     */
    @Test
    @DisplayName("\"Number of meeting\" → NUMBER_OF_MEETINGS  (first || clause)")
    void numberOfMeeting_returnsNumberOfMeetings() throws NoSuchMethodException {
        assertEquals(Validator.ValidationResource.NUMBER_OF_MEETINGS,
                strategy.mapToValidationResource(param("numMeeting"), "Number of meeting"));
    }

    /**
     * Test 5 – {@code "Number of meetings"} → {@code NUMBER_OF_MEETINGS}.
     * First clause is false; exercises the second {@code ||} clause.
     */
    @Test
    @DisplayName("\"Number of meetings\" → NUMBER_OF_MEETINGS  (second || clause)")
    void numberOfMeetings_returnsNumberOfMeetings() throws NoSuchMethodException {
        assertEquals(Validator.ValidationResource.NUMBER_OF_MEETINGS,
                strategy.mapToValidationResource(param("numMeetings"), "Number of meetings"));
    }

    /**
     * Test 6 – {@code "Time Zone"} → {@code TIME_ZONE}.
     */
    @Test
    @DisplayName("\"Time Zone\" → TIME_ZONE")
    void timeZone_returnsTimeZone() throws NoSuchMethodException {
        assertEquals(Validator.ValidationResource.TIME_ZONE,
                strategy.mapToValidationResource(param("timeZone"), "Time Zone"));
    }

    /**
     * Test 7 – {@code "Space Id"} → {@code SPACE_ID}.
     * Exercises the first {@code ||} clause in the Space-id guard.
     */
    @Test
    @DisplayName("\"Space Id\" → SPACE_ID  (first || clause)")
    void spaceId_returnsSpaceId() throws NoSuchMethodException {
        assertEquals(Validator.ValidationResource.SPACE_ID,
                strategy.mapToValidationResource(param("spaceId"), "Space Id"));
    }

    /**
     * Test 8 – {@code "Space ID"} → {@code SPACE_ID}.
     * First clause is false; exercises the second {@code ||} clause.
     */
    @Test
    @DisplayName("\"Space ID\" → SPACE_ID  (second || clause)")
    void spaceIdUpperCase_returnsSpaceId() throws NoSuchMethodException {
        assertEquals(Validator.ValidationResource.SPACE_ID,
                strategy.mapToValidationResource(param("spaceIdUc"), "Space ID"));
    }

    /**
     * Test 9 – {@code "Space Id or Key"} → {@code SPACE_ID}.
     * First and second clauses are false; exercises the third {@code ||} clause.
     */
    @Test
    @DisplayName("\"Space Id or Key\" → SPACE_ID  (third || clause)")
    void spaceIdOrKey_returnsSpaceId() throws NoSuchMethodException {
        assertEquals(Validator.ValidationResource.SPACE_ID,
                strategy.mapToValidationResource(param("spaceIdKey"), "Space Id or Key"));
    }

    /**
     * Test 10 – Unrecognised field name → {@code null} (default fallback).
     * All conditions are false; exercises the {@code return null} branch.
     */
    @Test
    @DisplayName("unrecognised field name → null  (default fallback)")
    void unknownField_returnsNull() throws NoSuchMethodException {
        assertNull(strategy.mapToValidationResource(param("unknown"), "Unknown Field"));
    }
}
