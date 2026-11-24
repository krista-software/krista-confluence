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

package com.kristasoft.extensions.essentials.collaboration.confluence.catalog.extresp;

import app.krista.extension.executor.RemediationAction;
import app.krista.model.field.NamedField;
import app.krista.model.field.NamedValuedField;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RemediationActionFactory")
class RemediationActionFactoryTest {

    // ──────────────────────────────────────────────────────────────
    //  createAskAction
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createAskAction")
    class CreateAskAction {

        @Test
        @DisplayName("should return a non-null RemediationAction for ACTIVE_USER")
        void returnsNonNull() {
            List<NamedField> fields = List.of(
                    new NamedField("SpaceId", "com.krista.fields.Text", Map.of(), Map.of())
            );

            RemediationAction action = RemediationActionFactory.createAskAction("Please provide the Space ID", fields);

            assertNotNull(action, "AskAction should not be null");
        }

        @Test
        @DisplayName("should accept empty fields list")
        void emptyFields() {
            RemediationAction action = RemediationActionFactory.createAskAction("No fields needed", Collections.emptyList());

            assertNotNull(action);
        }

        @Test
        @DisplayName("should accept multiple fields")
        void multipleFields() {
            List<NamedField> fields = List.of(
                    new NamedField("Field1", "com.krista.fields.Text", Map.of(), Map.of()),
                    new NamedField("Field2", "com.krista.fields.Number", Map.of(), Map.of())
            );

            RemediationAction action = RemediationActionFactory.createAskAction("Provide details", fields);

            assertNotNull(action);
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  createInformAction
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createInformAction")
    class CreateInformAction {

        @Test
        @DisplayName("should return a non-null RemediationAction for ACTIVE_USER")
        void returnsNonNull() {
            List<NamedValuedField> fields = Collections.emptyList();

            RemediationAction action = RemediationActionFactory.createInformAction("Operation completed", fields);

            assertNotNull(action, "InformAction should not be null");
        }

        @Test
        @DisplayName("should accept empty fields list")
        void emptyFields() {
            RemediationAction action = RemediationActionFactory.createInformAction("Done", Collections.emptyList());

            assertNotNull(action);
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  createInformActionALLParticipants
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createInformActionALLParticipants")
    class CreateInformActionAllParticipants {

        @Test
        @DisplayName("should return a non-null RemediationAction for ALL_PARTICIPANTS")
        void returnsNonNull() {
            List<NamedValuedField> fields = Collections.emptyList();

            RemediationAction action = RemediationActionFactory.createInformActionALLParticipants("Broadcast message", fields);

            assertNotNull(action, "InformAction for ALL_PARTICIPANTS should not be null");
        }

        @Test
        @DisplayName("should accept empty fields list")
        void emptyFields() {
            RemediationAction action = RemediationActionFactory.createInformActionALLParticipants("Alert", Collections.emptyList());

            assertNotNull(action);
        }
    }
}
