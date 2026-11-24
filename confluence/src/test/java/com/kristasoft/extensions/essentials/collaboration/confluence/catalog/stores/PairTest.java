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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for the Pair class.
 */
@DisplayName("Pair Tests")
class PairTest {

    @Nested
    @DisplayName("Constructor tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create Pair with key and value")
        void shouldCreatePairWithKeyAndValue() {
            Pair<String, Integer> pair = new Pair<>("key", 42);

            assertEquals("key", pair.getKey());
            assertEquals(42, pair.getValue());
        }

        @Test
        @DisplayName("Should create Pair with null key")
        void shouldCreatePairWithNullKey() {
            Pair<String, String> pair = new Pair<>(null, "value");

            assertNull(pair.getKey());
            assertEquals("value", pair.getValue());
        }

        @Test
        @DisplayName("Should create Pair with null value")
        void shouldCreatePairWithNullValue() {
            Pair<String, String> pair = new Pair<>("key", null);

            assertEquals("key", pair.getKey());
            assertNull(pair.getValue());
        }

        @Test
        @DisplayName("Should create Pair with both null key and value")
        void shouldCreatePairWithBothNulls() {
            Pair<String, String> pair = new Pair<>(null, null);

            assertNull(pair.getKey());
            assertNull(pair.getValue());
        }
    }

    @Nested
    @DisplayName("Factory method 'of' tests")
    class OfFactoryMethodTests {

        @Test
        @DisplayName("Should create Pair using of() factory method")
        void shouldCreatePairUsingOfFactory() {
            Pair<String, Integer> pair = Pair.of("name", 100);

            assertEquals("name", pair.getKey());
            assertEquals(100, pair.getValue());
        }

        @Test
        @DisplayName("Should create Pair with different types using of()")
        void shouldCreatePairWithDifferentTypes() {
            Pair<Integer, Boolean> pair = Pair.of(1, true);

            assertEquals(1, pair.getKey());
            assertTrue(pair.getValue());
        }

        @Test
        @DisplayName("Should create Pair with null values using of()")
        void shouldCreatePairWithNullValuesUsingOf() {
            Pair<String, String> pair = Pair.of(null, null);

            assertNull(pair.getKey());
            assertNull(pair.getValue());
        }
    }

    @Nested
    @DisplayName("Factory method 'setOf' tests")
    class SetOfFactoryMethodTests {

        @Test
        @DisplayName("Should create set of Pairs from Map")
        void shouldCreateSetOfPairsFromMap() {
            Map<String, Integer> map = new LinkedHashMap<>();
            map.put("a", 1);
            map.put("b", 2);
            map.put("c", 3);

            Set<Pair<String, Integer>> pairSet = Pair.setOf(map);

            assertEquals(3, pairSet.size());
            assertTrue(pairSet.contains(Pair.of("a", 1)));
            assertTrue(pairSet.contains(Pair.of("b", 2)));
            assertTrue(pairSet.contains(Pair.of("c", 3)));
        }

        @Test
        @DisplayName("Should create empty set from empty Map")
        void shouldCreateEmptySetFromEmptyMap() {
            Map<String, String> emptyMap = new HashMap<>();

            Set<Pair<String, String>> pairSet = Pair.setOf(emptyMap);

            assertTrue(pairSet.isEmpty());
        }

        @Test
        @DisplayName("Should create set with single entry from single-entry Map")
        void shouldCreateSetWithSingleEntryFromSingleEntryMap() {
            Map<String, String> singleMap = Map.of("only", "one");

            Set<Pair<String, String>> pairSet = Pair.setOf(singleMap);

            assertEquals(1, pairSet.size());
            assertTrue(pairSet.contains(Pair.of("only", "one")));
        }

        @Test
        @DisplayName("Should return LinkedHashSet from setOf()")
        void shouldReturnLinkedHashSetFromSetOf() {
            Map<String, Integer> map = new LinkedHashMap<>();
            map.put("first", 1);
            map.put("second", 2);

            Set<Pair<String, Integer>> pairSet = Pair.setOf(map);

            assertInstanceOf(LinkedHashSet.class, pairSet);
        }
    }

    @Nested
    @DisplayName("Factory method 'create' tests")
    class CreateFactoryMethodTests {

        @Test
        @DisplayName("Should create Pair from Map.Entry")
        void shouldCreatePairFromMapEntry() {
            Map<String, Integer> map = Map.of("key", 42);
            Map.Entry<String, Integer> entry = map.entrySet().iterator().next();

            Pair<String, Integer> pair = Pair.create(entry);

            assertEquals("key", pair.getKey());
            assertEquals(42, pair.getValue());
        }

        @Test
        @DisplayName("Should create Pair from Map.Entry with null value")
        void shouldCreatePairFromMapEntryWithNullValue() {
            Map<String, String> map = new HashMap<>();
            map.put("key", null);
            Map.Entry<String, String> entry = map.entrySet().iterator().next();

            Pair<String, String> pair = Pair.create(entry);

            assertEquals("key", pair.getKey());
            assertNull(pair.getValue());
        }
    }

    @Nested
    @DisplayName("Getter tests")
    class GetterTests {

        @Test
        @DisplayName("getKey should return the key")
        void getKeyShouldReturnKey() {
            Pair<String, String> pair = Pair.of("myKey", "myValue");
            assertEquals("myKey", pair.getKey());
        }

        @Test
        @DisplayName("getValue should return the value")
        void getValueShouldReturnValue() {
            Pair<String, String> pair = Pair.of("myKey", "myValue");
            assertEquals("myValue", pair.getValue());
        }
    }

    @Nested
    @DisplayName("equals() tests")
    class EqualsTests {

        @Test
        @DisplayName("Same key and value should be equal")
        void sameKeyAndValueShouldBeEqual() {
            Pair<String, Integer> pair1 = Pair.of("a", 1);
            Pair<String, Integer> pair2 = Pair.of("a", 1);

            assertEquals(pair1, pair2);
        }

        @Test
        @DisplayName("Different keys should not be equal")
        void differentKeysShouldNotBeEqual() {
            Pair<String, Integer> pair1 = Pair.of("a", 1);
            Pair<String, Integer> pair2 = Pair.of("b", 1);

            assertNotEquals(pair1, pair2);
        }

        @Test
        @DisplayName("Different values should not be equal")
        void differentValuesShouldNotBeEqual() {
            Pair<String, Integer> pair1 = Pair.of("a", 1);
            Pair<String, Integer> pair2 = Pair.of("a", 2);

            assertNotEquals(pair1, pair2);
        }

        @Test
        @DisplayName("Both null key and value should be equal")
        void bothNullKeyAndValueShouldBeEqual() {
            Pair<String, String> pair1 = Pair.of(null, null);
            Pair<String, String> pair2 = Pair.of(null, null);

            assertEquals(pair1, pair2);
        }

        @Test
        @DisplayName("Should not be equal to null")
        void shouldNotBeEqualToNull() {
            Pair<String, Integer> pair = Pair.of("a", 1);

            assertNotEquals(null, pair);
        }

        @Test
        @DisplayName("Should not be equal to different type")
        void shouldNotBeEqualToDifferentType() {
            Pair<String, Integer> pair = Pair.of("a", 1);

            assertNotEquals("not a pair", pair);
        }

        @Test
        @DisplayName("Pair should be equal to itself (reflexive)")
        void shouldBeEqualToItself() {
            Pair<String, Integer> pair = Pair.of("a", 1);

            assertEquals(pair, pair);
        }

        @Test
        @DisplayName("equals should be symmetric")
        void equalsShouldBeSymmetric() {
            Pair<String, Integer> pair1 = Pair.of("a", 1);
            Pair<String, Integer> pair2 = Pair.of("a", 1);

            assertEquals(pair1, pair2);
            assertEquals(pair2, pair1);
        }

        @Test
        @DisplayName("equals should be transitive")
        void equalsShouldBeTransitive() {
            Pair<String, Integer> pair1 = Pair.of("a", 1);
            Pair<String, Integer> pair2 = Pair.of("a", 1);
            Pair<String, Integer> pair3 = Pair.of("a", 1);

            assertEquals(pair1, pair2);
            assertEquals(pair2, pair3);
            assertEquals(pair1, pair3);
        }
    }

    @Nested
    @DisplayName("hashCode() tests")
    class HashCodeTests {

        @Test
        @DisplayName("Equal pairs should have same hashCode")
        void equalPairsShouldHaveSameHashCode() {
            Pair<String, Integer> pair1 = Pair.of("a", 1);
            Pair<String, Integer> pair2 = Pair.of("a", 1);

            assertEquals(pair1.hashCode(), pair2.hashCode());
        }

        @Test
        @DisplayName("Different pairs may have different hashCodes")
        void differentPairsMayHaveDifferentHashCodes() {
            Pair<String, Integer> pair1 = Pair.of("a", 1);
            Pair<String, Integer> pair2 = Pair.of("b", 2);

            // Not guaranteed but very likely to differ
            assertNotEquals(pair1.hashCode(), pair2.hashCode());
        }

        @Test
        @DisplayName("hashCode should be consistent across calls")
        void hashCodeShouldBeConsistent() {
            Pair<String, Integer> pair = Pair.of("key", 42);

            int hash1 = pair.hashCode();
            int hash2 = pair.hashCode();

            assertEquals(hash1, hash2);
        }

        @Test
        @DisplayName("Null key and value should produce consistent hashCode")
        void nullKeyAndValueShouldProduceConsistentHashCode() {
            Pair<String, String> pair1 = Pair.of(null, null);
            Pair<String, String> pair2 = Pair.of(null, null);

            assertEquals(pair1.hashCode(), pair2.hashCode());
        }
    }

    @Nested
    @DisplayName("toString() tests")
    class ToStringTests {

        @Test
        @DisplayName("toString should return 'key = value' format")
        void toStringShouldReturnKeyEqualsValueFormat() {
            Pair<String, Integer> pair = Pair.of("name", 42);

            assertEquals("name = 42", pair.toString());
        }

        @Test
        @DisplayName("toString with null key should return 'null = value'")
        void toStringWithNullKeyShouldReturnNullEqualsValue() {
            Pair<String, String> pair = Pair.of(null, "value");

            assertEquals("null = value", pair.toString());
        }

        @Test
        @DisplayName("toString with null value should return 'key = null'")
        void toStringWithNullValueShouldReturnKeyEqualsNull() {
            Pair<String, String> pair = Pair.of("key", null);

            assertEquals("key = null", pair.toString());
        }

        @Test
        @DisplayName("toString with both nulls should return 'null = null'")
        void toStringWithBothNullsShouldReturnNullEqualsNull() {
            Pair<String, String> pair = Pair.of(null, null);

            assertEquals("null = null", pair.toString());
        }

        @Test
        @DisplayName("toString with string key and value")
        void toStringWithStringKeyAndValue() {
            Pair<String, String> pair = Pair.of("hello", "world");

            assertEquals("hello = world", pair.toString());
        }
    }

    @Test
    @DisplayName("Pair should work correctly as Set element")
    void pairShouldWorkCorrectlyAsSetElement() {
        Set<Pair<String, Integer>> set = new HashSet<>();
        set.add(Pair.of("a", 1));
        set.add(Pair.of("a", 1)); // duplicate
        set.add(Pair.of("b", 2));

        assertEquals(2, set.size());
    }

    @Test
    @DisplayName("Pair should work correctly as Map key")
    void pairShouldWorkCorrectlyAsMapKey() {
        Map<Pair<String, Integer>, String> map = new HashMap<>();
        map.put(Pair.of("a", 1), "first");
        map.put(Pair.of("a", 1), "second"); // should overwrite

        assertEquals(1, map.size());
        assertEquals("second", map.get(Pair.of("a", 1)));
    }
}
