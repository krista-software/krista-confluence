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

package com.kristasoft.extensions.essentials.collaboration.confluence.mcp;

import app.krista.extension.executor.Invoker;
import app.krista.sdk.atlassian.mcp.McpProductConfig;
import com.kristasoft.extensions.essentials.collaboration.confluence.ConfluenceAttributes;
import com.kristasoft.extensions.essentials.collaboration.confluence.authentication.ConfluenceOAuthService;
import com.kristasoft.extensions.essentials.collaboration.confluence.authentication.SessionManager;
import com.kristasoft.extensions.essentials.collaboration.confluence.utils.ConfluenceConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.jvnet.hk2.annotations.Service;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for {@link ConfluenceMcpQueryService}.
 *
 * <p>Tests the constructor wiring, protected method overrides (via reflection),
 * and annotation presence ({@code @Service}).</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ConfluenceMcpQueryService Tests")
class ConfluenceMcpQueryServiceTest {

    @Mock
    private SessionManager sessionManager;

    @Mock
    private Invoker invoker;

    @Mock
    private ConfluenceAttributes confluenceAttributes;

    @Mock
    private ConfluenceOAuthService confluenceOAuthService;

    private ConfluenceMcpQueryService service;

    @BeforeEach
    void setUp() {
        service = new ConfluenceMcpQueryService(
                sessionManager,
                invoker,
                confluenceAttributes,
                confluenceOAuthService
        );
    }

    // ── Helper to invoke protected methods via reflection ──

    private Object invokeMethod(String methodName) throws Exception {
        Method method = ConfluenceMcpQueryService.class.getDeclaredMethod(methodName);
        method.setAccessible(true);
        return method.invoke(service);
    }

    // ── Constructor tests ──

    @Nested
    @DisplayName("Constructor")
    class ConstructorTests {

        @Test
        @DisplayName("Should construct successfully with valid dependencies")
        void shouldConstructSuccessfully() {
            assertNotNull(service);
        }

        @Test
        @DisplayName("Should be an instance of AbstractMcpQueryService")
        void shouldExtendAbstractMcpQueryService() {
            assertInstanceOf(
                    app.krista.sdk.atlassian.mcp.AbstractMcpQueryService.class,
                    service
            );
        }
    }

    // ── getProductConfig tests ──

    @Nested
    @DisplayName("getProductConfig()")
    class GetProductConfigTests {

        @Test
        @DisplayName("Should return a ConfluenceMcpProductConfig instance")
        void shouldReturnConfluenceMcpProductConfig() throws Exception {
            McpProductConfig config = (McpProductConfig) invokeMethod("getProductConfig");

            assertNotNull(config);
            assertInstanceOf(ConfluenceMcpProductConfig.class, config);
        }

        @Test
        @DisplayName("Should return a new instance each call")
        void shouldReturnNewInstanceEachCall() throws Exception {
            McpProductConfig config1 = (McpProductConfig) invokeMethod("getProductConfig");
            McpProductConfig config2 = (McpProductConfig) invokeMethod("getProductConfig");

            assertNotNull(config1);
            assertNotNull(config2);
            assertNotSame(config1, config2);
        }

        @Test
        @DisplayName("Returned config should have productName 'Confluence'")
        void returnedConfigShouldHaveCorrectProductName() throws Exception {
            McpProductConfig config = (McpProductConfig) invokeMethod("getProductConfig");

            assertEquals("Confluence", config.productName());
        }

        @Test
        @DisplayName("Returned config should have queryLanguage 'CQL'")
        void returnedConfigShouldHaveCorrectQueryLanguage() throws Exception {
            McpProductConfig config = (McpProductConfig) invokeMethod("getProductConfig");

            assertEquals("CQL", config.queryLanguage());
        }

        @Test
        @DisplayName("Returned config should have correct full query language name")
        void returnedConfigShouldHaveCorrectQueryLanguageFullName() throws Exception {
            McpProductConfig config = (McpProductConfig) invokeMethod("getProductConfig");

            assertEquals("CQL (Confluence Query Language)", config.queryLanguageFullName());
        }

        @Test
        @DisplayName("Returned config should use GET search HTTP method")
        void returnedConfigShouldUseGetSearchMethod() throws Exception {
            McpProductConfig config = (McpProductConfig) invokeMethod("getProductConfig");

            assertEquals("GET", config.searchHttpMethod());
        }
    }

    // ── getJaxrsId tests ──

    @Nested
    @DisplayName("getJaxrsId()")
    class GetJaxrsIdTests {

        @Test
        @DisplayName("Should return ConfluenceConstants.JAXRS_ID")
        void shouldReturnJaxrsIdConstant() throws Exception {
            String jaxrsId = (String) invokeMethod("getJaxrsId");

            assertEquals(ConfluenceConstants.JAXRS_ID, jaxrsId);
        }

        @Test
        @DisplayName("Should return 'confluenceextn'")
        void shouldReturnConfluenceExtn() throws Exception {
            String jaxrsId = (String) invokeMethod("getJaxrsId");

            assertEquals("confluenceextn", jaxrsId);
        }
    }

    // ── Annotation tests ──

    @Nested
    @DisplayName("Annotations")
    class AnnotationTests {

        @Test
        @DisplayName("Should have @Service annotation")
        void shouldHaveServiceAnnotation() {
            Service serviceAnnotation = ConfluenceMcpQueryService.class.getAnnotation(Service.class);

            assertNotNull(serviceAnnotation,
                    "ConfluenceMcpQueryService should be annotated with @Service");
        }

        @Test
        @DisplayName("Constructor should have @Inject annotation")
        void constructorShouldHaveInjectAnnotation() throws Exception {
            var constructor = ConfluenceMcpQueryService.class.getDeclaredConstructor(
                    SessionManager.class,
                    Invoker.class,
                    ConfluenceAttributes.class,
                    ConfluenceOAuthService.class
            );
            assertNotNull(constructor.getAnnotation(javax.inject.Inject.class));
        }

        @Test
        @DisplayName("Should NOT have @Path annotation (it is a service, not a resource)")
        void shouldNotHavePathAnnotation() {
            javax.ws.rs.Path pathAnnotation = ConfluenceMcpQueryService.class
                    .getAnnotation(javax.ws.rs.Path.class);

            assertNull(pathAnnotation,
                    "ConfluenceMcpQueryService should NOT have @Path annotation");
        }
    }

    // ── Consistency tests ──

    @Nested
    @DisplayName("Consistency with ConfluenceMcpSetupResource")
    class ConsistencyTests {

        @Test
        @DisplayName("Should return same JAXRS_ID as ConfluenceMcpSetupResource")
        void shouldReturnSameJaxrsIdAsSetupResource() throws Exception {
            ConfluenceMcpSetupResource setupResource = new ConfluenceMcpSetupResource(
                    sessionManager, invoker, confluenceAttributes, confluenceOAuthService);

            Method setupGetJaxrsId = ConfluenceMcpSetupResource.class.getDeclaredMethod("getJaxrsId");
            setupGetJaxrsId.setAccessible(true);
            String setupJaxrsId = (String) setupGetJaxrsId.invoke(setupResource);

            String queryServiceJaxrsId = (String) invokeMethod("getJaxrsId");

            assertEquals(setupJaxrsId, queryServiceJaxrsId,
                    "Both MCP classes should use the same JAXRS_ID");
        }

        @Test
        @DisplayName("Should return same product config type as ConfluenceMcpSetupResource")
        void shouldReturnSameProductConfigType() throws Exception {
            ConfluenceMcpSetupResource setupResource = new ConfluenceMcpSetupResource(
                    sessionManager, invoker, confluenceAttributes, confluenceOAuthService);

            Method setupGetConfig = ConfluenceMcpSetupResource.class.getDeclaredMethod("getProductConfig");
            setupGetConfig.setAccessible(true);
            McpProductConfig setupConfig = (McpProductConfig) setupGetConfig.invoke(setupResource);

            McpProductConfig queryConfig = (McpProductConfig) invokeMethod("getProductConfig");

            assertEquals(setupConfig.getClass(), queryConfig.getClass(),
                    "Both MCP classes should use the same product config class");
        }
    }
}
