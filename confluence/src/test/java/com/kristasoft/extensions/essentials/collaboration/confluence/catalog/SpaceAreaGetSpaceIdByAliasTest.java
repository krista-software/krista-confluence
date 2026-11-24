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

package com.kristasoft.extensions.essentials.collaboration.confluence.catalog;

import app.krista.extension.executor.ExtensionResponse;
import app.krista.ksdk.context.RequestContext;
import com.kristasoft.extensions.essentials.collaboration.confluence.api.ConfluenceAuditService;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.entities.Space;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.errorhandlers.ErrorHandlingStateManager;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.errorhandlers.ExtensionResponseGenerator;
import com.kristasoft.extensions.essentials.collaboration.confluence.catalog.validators.ValidationOrchestrator;
import com.kristasoft.extensions.essentials.collaboration.confluence.exception.ConfluenceRESTException;
import com.kristasoft.extensions.essentials.collaboration.confluence.service.ConfluenceService;
import com.kristasoft.extensions.essentials.collaboration.confluence.service.PageDocxService;
import com.kristasoft.extensions.essentials.collaboration.confluence.utils.TelemetryHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 100% branch-coverage tests for {@link SpaceArea#getSpaceIdByAlias}.
 *
 * <h2>Branch map</h2>
 * <pre>
 * getSpaceIdByAlias
 *   ├─ if (spaceAlias == null || spaceAlias.trim().isEmpty())
 *   │     T – null alias  (1st operand true)              → test 1
 *   │     T – blank alias (1st false, 2nd true)           → test 2
 *   │     F – valid alias (both false)                    → tests 3, 4
 *   └─ catch (Exception cause)
 *         reached (service throws) → SYSTEM_ERROR failure → test 5
 *         not reached (normal)                            → tests 3, 4
 * </pre>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SpaceArea.getSpaceIdByAlias – 100% branch coverage")
class SpaceAreaGetSpaceIdByAliasTest {

    // ─── mocks ────────────────────────────────────────────────────────────────

    @Mock private ConfluenceService service;
    @Mock private ValidationOrchestrator validationOrchestrator;
    @Mock private ErrorHandlingStateManager errorHandlingStateManager;
    @Mock private ExtensionResponseGenerator responseGenerator;
    @Mock private PageDocxService pageDocxService;
    @Mock private TelemetryHelper telemetryHelper;
    @Mock private ConfluenceAuditService confluenceAuditService;
    @Mock private RequestContext requestContext;

    private SpaceArea spaceArea;

    @BeforeEach
    void setUp() {
        spaceArea = new SpaceArea(service, validationOrchestrator, errorHandlingStateManager,
                responseGenerator, pageDocxService, telemetryHelper,
                confluenceAuditService, requestContext);
    }

    // ─── helper ───────────────────────────────────────────────────────────────

    /** Builds a minimal {@code dto.Space} that {@code ParserUtils.setSpaceEntity} can process. */
    private static com.kristasoft.extensions.essentials.collaboration.confluence.dto.Space dtoSpace(
            String id, String name) {
        return com.kristasoft.extensions.essentials.collaboration.confluence.dto.Space.builder()
                .setId(id)
                .setName(name)
                .setKey("TST")
                .setStatus("current")
                .build();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  GUARD BRANCH – null / blank alias
    // ══════════════════════════════════════════════════════════════════════════

    /** Test 1 – {@code spaceAlias == null} → INPUT_ERROR failure; service never called. */
    @Test
    @DisplayName("null alias → INPUT_ERROR failure; service never called")
    void nullAlias_returnsInputErrorFailure() {
        ExtensionResponse response = spaceArea.getSpaceIdByAlias(null);

        assertNotNull(response);
        assertEquals(ExtensionResponse.Result.FAILURE, response.getResult());
        assertEquals(ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                response.getError().getExceptionType());
        assertTrue(response.getError().getErrorMessage().contains("Space Alias is required"));
        verify(service, never()).getSpaceIdByAlias(anyString());
        verify(telemetryHelper).incrementCount("confluence.getSpaceIdByAlias");
        verify(telemetryHelper).recordError(eq("confluence.getSpaceIdByAlias"), anyLong(), any(), anyMap());
    }

    /** Test 2 – blank alias ({@code trim().isEmpty()}) → INPUT_ERROR failure; service never called. */
    @Test
    @DisplayName("blank alias → INPUT_ERROR failure; service never called")
    void blankAlias_returnsInputErrorFailure() {
        ExtensionResponse response = spaceArea.getSpaceIdByAlias("   ");

        assertEquals(ExtensionResponse.Result.FAILURE, response.getResult());
        assertEquals(ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                response.getError().getExceptionType());
        verify(service, never()).getSpaceIdByAlias(anyString());
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  SUCCESS PATH
    // ══════════════════════════════════════════════════════════════════════════

    /** Test 3 – Valid alias resolves → SUCCESS with "Space" entity and "Space Id". */
    @Test
    @DisplayName("valid alias → SUCCESS with Space entity and Space Id")
    void validAlias_returnsSuccessWithSpaceAndId() {
        when(service.getSpaceIdByAlias("MFS")).thenReturn(dtoSpace("294916", "My first space"));

        ExtensionResponse response = spaceArea.getSpaceIdByAlias("MFS");

        assertNotNull(response);
        assertEquals(ExtensionResponse.Result.SUCCESS, response.getResult());

        Object spaceObj = response.getResponseValue().get("Space");
        assertInstanceOf(Space.class, spaceObj);
        assertEquals("294916", ((Space) spaceObj).id);
        assertEquals("294916", response.getResponseValue().get("Space Id"));

        verify(service).getSpaceIdByAlias("MFS");
        verify(telemetryHelper).recordSuccess(eq("confluence.getSpaceIdByAlias"), anyLong(), anyMap());
    }

    /** Test 4 – Alias with surrounding whitespace is trimmed before the service call. */
    @Test
    @DisplayName("alias with whitespace → trimmed before service call")
    void aliasWithWhitespace_trimmedBeforeServiceCall() {
        when(service.getSpaceIdByAlias("~johndoe")).thenReturn(dtoSpace("163842", "John Doe"));

        ExtensionResponse response = spaceArea.getSpaceIdByAlias("  ~johndoe  ");

        assertEquals(ExtensionResponse.Result.SUCCESS, response.getResult());
        verify(service).getSpaceIdByAlias("~johndoe");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  CATCH BLOCK
    // ══════════════════════════════════════════════════════════════════════════

    /** Test 5 – Service throws → catch block → SYSTEM_ERROR failure with cause message. */
    @Test
    @DisplayName("service throws → SYSTEM_ERROR failure; recordError called")
    void serviceThrows_returnsSystemErrorFailure() {
        when(service.getSpaceIdByAlias("bad-alias"))
                .thenThrow(new ConfluenceRESTException("No space found with alias 'bad-alias'."));

        ExtensionResponse response = spaceArea.getSpaceIdByAlias("bad-alias");

        assertNotNull(response);
        assertEquals(ExtensionResponse.Result.FAILURE, response.getResult());
        assertEquals(ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR,
                response.getError().getExceptionType());
        assertEquals("No space found with alias 'bad-alias'.", response.getError().getErrorMessage());
        verify(telemetryHelper).recordError(eq("confluence.getSpaceIdByAlias"), anyLong(), any(), anyMap());
    }
}
