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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.ws.rs.Path;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for {@link ConfluenceMcpSetupResource}.
 *
 * <p>Tests the constructor wiring, protected method overrides (via reflection),
 * and JAX-RS annotation presence.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ConfluenceMcpSetupResource Tests")
class ConfluenceMcpSetupResourceTest {

    @Mock
    private SessionManager sessionManager;

    @Mock
    private Invoker invoker;

    @Mock
    private ConfluenceAttributes confluenceAttributes;

    @Mock
    private ConfluenceOAuthService confluenceOAuthService;

    private ConfluenceMcpSetupResource resource;

    @BeforeEach
    void setUp() {
        resource = new ConfluenceMcpSetupResource(
                sessionManager,
                invoker,
                confluenceAttributes,
                confluenceOAuthService
        );
    }

    // ── Helper to invoke protected methods via reflection ──

    private Object invokeMethod(String methodName) throws Exception {
        Method method = ConfluenceMcpSetupResource.class.getDeclaredMethod(methodName);
        method.setAccessible(true);
        return method.invoke(resource);
    }

    // ── Constructor tests ──

    @Nested
    @DisplayName("Constructor")
    class ConstructorTests {

        @Test
        @DisplayName("Should construct successfully with valid dependencies")
        void shouldConstructSuccessfully() {
            assertNotNull(resource);
        }

        @Test
        @DisplayName("Should be an instance of AbstractMcpSetupResource")
        void shouldExtendAbstractMcpSetupResource() {
            assertInstanceOf(
                    app.krista.sdk.atlassian.mcp.AbstractMcpSetupResource.class,
                    resource
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
        @DisplayName("Should have @Path('/mcp-setup') annotation")
        void shouldHavePathAnnotation() {
            Path pathAnnotation = ConfluenceMcpSetupResource.class.getAnnotation(Path.class);

            assertNotNull(pathAnnotation);
            assertEquals("/mcp-setup", pathAnnotation.value());
        }

        @Test
        @DisplayName("Constructor should have @Inject annotation")
        void constructorShouldHaveInjectAnnotation() throws Exception {
            var constructor = ConfluenceMcpSetupResource.class.getDeclaredConstructor(
                    SessionManager.class,
                    Invoker.class,
                    ConfluenceAttributes.class,
                    ConfluenceOAuthService.class
            );
            assertNotNull(constructor.getAnnotation(javax.inject.Inject.class));
        }
    }
}
