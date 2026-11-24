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

package com.kristasoft.extensions.essentials.collaboration.confluence;

import app.krista.extension.authorization.MustAuthorizeException;
import app.krista.extension.executor.Invoker;
import app.krista.ksdk.context.AuthorizationContext;
import app.krista.ksdk.context.RequestContext;
import app.krista.sdk.atlassian.attributes.BaseAtlassianAttributes;
import app.krista.sdk.atlassian.attributes.CredentialFieldNames;
import app.krista.sdk.atlassian.oauth.AccessibleResource;
import app.krista.sdk.atlassian.oauth.ClientCredentialsTokenProvider;
import com.kristasoft.extensions.essentials.collaboration.confluence.api.APIVersion;
import com.kristasoft.extensions.essentials.collaboration.confluence.authentication.ConfluenceOAuthService;
import com.kristasoft.extensions.essentials.collaboration.confluence.authentication.SessionManager;
import com.kristasoft.extensions.essentials.collaboration.confluence.client.ConfluenceRESTClient;
import com.kristasoft.extensions.essentials.collaboration.confluence.utils.ConfluenceConstants;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.lang.reflect.Proxy;
import java.net.HttpURLConnection;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Confluence credential attributes built on the shared Atlassian SDK.
 *
 * <p>The credential bookkeeping (lazy resolution, presence/partial/mixed
 * checks) is inherited from {@link BaseAtlassianAttributes}, parameterised
 * with Confluence's historical field labels. The Confluence-specific getter
 * names ({@code getDomainName()}, {@code getAccountEmail()},
 * {@code getApiKey()}) and the REST-client factory are preserved so existing
 * call sites keep working unchanged.</p>
 */
@Service
public class ConfluenceAttributes extends BaseAtlassianAttributes {

    private static final Logger logger = LoggerFactory.getLogger(ConfluenceAttributes.class);

    private static final CredentialFieldNames FIELD_NAMES = new CredentialFieldNames(
            ConfluenceConstants.DOMAIN_NAME,
            ConfluenceConstants.AUTHORIZATION_EMAIL,
            ConfluenceConstants.API_KEY,
            ConfluenceConstants.CLIENT_ID,
            ConfluenceConstants.CLIENT_SECRET);

    /**
     * Key prefix for credential snapshots persisted by {@code AbstractAtlassianAuthResource}
     * — mirrors the private constant there so per-request attribute reconstruction can
     * supplement missing fields from the Authentication tab's saved state.
     */
    private static final String CRED_PREFIX = "cred.";

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$");

    private static final int MIN_CLIENT_SECRET_LENGTH = 32;

    private Map<String, Object> attributes;

    // Basic Auth fields (cached like the legacy implementation)
    private String domainName;
    private String accountEmail;
    private String apiKey;

    private ConfluenceRESTClient restClient;

    /**
     * The user-facing base URL of the Atlassian site (e.g.
     * {@code https://acme.atlassian.net}), resolved from the OAuth accessible
     * resource when the REST client is built. Null under Basic Auth, where the
     * site URL is derived from {@link #getDomainName()} instead. Reset per
     * request alongside {@link #restClient} in {@link #setAttributes(Map)}.
     */
    private String siteBaseUrl;

    private final RequestContext requestContext;
    private final SessionManager sessionManager;
    private final AuthorizationContext authorizationContext;

    @Inject
    public ConfluenceAttributes(@Named("self") Invoker invoker, RequestContext requestContext,
                                SessionManager sessionManager, AuthorizationContext authorizationContext) {
        super(invoker, FIELD_NAMES);
        this.requestContext = requestContext;
        this.sessionManager = sessionManager;
        this.authorizationContext = authorizationContext;
    }

    /**
     * Credential-only constructor used by service facades that operate
     * outside the HK2 container (e.g. {@code ConfluenceService}). Backed by a
     * minimal {@link Invoker} view exposing just the supplied attributes.
     */
    public ConfluenceAttributes(String domainName, String accountEmail, String apiKey) {
        super(staticAttributesInvoker(domainName, accountEmail, apiKey), FIELD_NAMES);
        this.domainName = domainName;
        this.accountEmail = accountEmail;
        this.apiKey = apiKey;
        this.requestContext = null;
        this.sessionManager = null;
        this.authorizationContext = null;
    }

    private static Invoker staticAttributesInvoker(String domainName, String accountEmail, String apiKey) {
        Map<String, Object> staticAttributes = new HashMap<>();
        staticAttributes.put(ConfluenceConstants.DOMAIN_NAME, domainName);
        staticAttributes.put(ConfluenceConstants.AUTHORIZATION_EMAIL, accountEmail);
        staticAttributes.put(ConfluenceConstants.API_KEY, apiKey);
        return (Invoker) Proxy.newProxyInstance(Invoker.class.getClassLoader(), new Class<?>[]{Invoker.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getAttributes" -> staticAttributes;
                    case "toString" -> "ConfluenceAttributes.staticAttributesInvoker";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(
                            "Invoker." + method.getName() + " is not available on credential-only ConfluenceAttributes");
                });
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public void setAttributes(Map<String, Object> attributes) {
        logger.info("ConfluenceAttributes.setAttributes() called");
        super.setAttributes(attributes);
        this.attributes = attributes;
        this.restClient = null;
        this.siteBaseUrl = null;
        persistOAuthToSnapshot(attributes);
        logger.info("ConfluenceAttributes.setAttributes() completed, REST client reset");
    }

    /**
     * Persists OAuth credentials from the attribute map into the KeyValueStore
     * credential snapshot so they are available during the OAuth callback.
     *
     * <p>The OAuth callback URL bypasses normal invoker routing, so
     * {@code setAttributes(invoker.getAttributes())} is never called during
     * the callback request. By persisting credentials to the snapshot whenever
     * they are set via normal requests, the callback can retrieve them through
     * {@link #credSnap(String)}.</p>
     */
    private void persistOAuthToSnapshot(Map<String, Object> attributes) {
        if (sessionManager == null || requestContext == null || attributes == null) return;
        String clientId = (String) attributes.get(ConfluenceConstants.CLIENT_ID);
        String clientSecret = (String) attributes.get(ConfluenceConstants.CLIENT_SECRET);
        if (clientId != null && !clientId.isBlank()
                && clientSecret != null && !clientSecret.isBlank()) {
            String invokerId = requestContext.getInvokerId();
            if (invokerId == null) return;
            String prefix = CRED_PREFIX + invokerId + ".";
            sessionManager.store(prefix + "clientId", clientId);
            sessionManager.store(prefix + "clientSecret", clientSecret);
            String mode = (String) attributes.get(ConfluenceConstants.CREDENTIALS_MODE);
            if (mode != null && !mode.isBlank()) {
                sessionManager.store(prefix + "credentialsMode", mode);
            }
            logger.info("Persisted OAuth credentials to snapshot for invoker {}", invokerId);
        }
    }

    public String getDomainName() {
        if (attributes != null) {
            this.domainName = (String) attributes.getOrDefault(ConfluenceConstants.DOMAIN_NAME, "");
        }
        if (domainName == null || domainName.isBlank()) {
            String snap = credSnap("hostUrl");
            if (snap != null && !snap.isBlank()) return snap;
        }
        return domainName;
    }

    public String getAccountEmail() {
        if (attributes != null) {
            this.accountEmail = (String) attributes.getOrDefault(ConfluenceConstants.AUTHORIZATION_EMAIL, "");
        }
        if (accountEmail == null || accountEmail.isBlank()) {
            String snap = credSnap("userName");
            if (snap != null && !snap.isBlank()) return snap;
        }
        return accountEmail;
    }

    public String getApiKey() {
        if (attributes != null) {
            this.apiKey = (String) attributes.getOrDefault(ConfluenceConstants.API_KEY, "");
        }
        if (apiKey == null || apiKey.isBlank()) {
            String snap = credSnap("authToken");
            if (snap != null && !snap.isBlank()) return snap;
        }
        return apiKey;
    }

    // The SDK-facing credential getters delegate to the legacy Confluence
    // getters so SDK components observe exactly the legacy values
    // (including the empty-string default when an attribute is absent).

    @Override
    public String getHostUrl() {
        return getDomainName();
    }

    @Override
    public String getUserName() {
        return getAccountEmail();
    }

    @Override
    public String getAuthToken() {
        return getApiKey();
    }

    // ── Credential snapshot supplementing ────────────────────────────────────
    //
    // ConfluenceAttributes is recreated per-request from invoker.getAttributes()
    // (the Krista/Setup attribute store). When credentials were saved exclusively
    // through the Authentication tab they live only in the KeyValueStore snapshot
    // (keys: "cred.<invokerId>.<field>") and are absent from the attribute store.
    // Overriding getClientId()/getClientSecret() to fall back to that snapshot
    // makes areOAuthCredentialsPresent() return the correct answer so
    // AbstractAtlassianOAuthService.getOauthService() can build the ScribeJava
    // service with the correct invoker routing callback URL.

    private String credSnap(String field) {
        if (sessionManager == null || requestContext == null) return null;
        String key = CRED_PREFIX + requestContext.getInvokerId() + "." + field;
        String value = sessionManager.get(key);
        logger.debug("credSnap: key='{}' value={}", key,
                value != null ? "present(" + value.length() + " chars)" : "NULL");
        return value;
    }

    @Override
    public String getClientId() {
        String snap = credSnap("clientId");
        if (snap != null && !snap.isBlank()) return snap;
        String sup = super.getClientId();
        logger.debug("getClientId: snap={} super={}", snap != null ? "'" + snap + "'" : "null",
                sup != null ? "present(" + sup.length() + " chars)" : "null");
        return sup;
    }

    @Override
    public String getClientSecret() {
        String snap = credSnap("clientSecret");
        if (snap != null && !snap.isBlank()) return snap;
        String sup = super.getClientSecret();
        logger.debug("getClientSecret: snap={} super={}", snap != null ? "'" + snap + "'" : "null",
                sup != null ? "present(" + sup.length() + " chars)" : "null");
        return sup;
    }

    /**
     * Three-tier resolution of the credentials mode:
     * <ol>
     *   <li><b>In-memory value</b> — set by {@code setAttributes(candidate)} or
     *       {@code setCredentialsMode()} during the current request.  This
     *       represents the user's <em>active intent</em> and must take priority
     *       (per {@link BaseAtlassianAttributes#getCredentialsModeValue()}'s
     *       contract).</li>
     *   <li><b>Credential snapshot</b> — the last intentionally saved mode,
     *       persisted across requests.  Prevents the auto-detection fallback
     *       from being fooled by stale own-credentials lingering in the
     *       snapshot after switching to managed mode.</li>
     *   <li><b>Auto-detect (super)</b> — invoker attributes +
     *       {@link app.krista.sdk.atlassian.credentials.ManagedCredentialStore}
     *       heuristic.</li>
     * </ol>
     */
    @Override
    public boolean isManagedCredentialsEnabled() {
        // 1. In-memory value first — active request intent wins over any persisted state.
        String inMemory = getCredentialsModeValue();
        if (inMemory != null && !inMemory.isBlank()) {
            String trimmed = inMemory.trim();
            if ("managed".equalsIgnoreCase(trimmed) || "true".equalsIgnoreCase(trimmed)) {
                return true;
            }
            if ("own".equalsIgnoreCase(trimmed) || "false".equalsIgnoreCase(trimmed)) {
                return false;
            }
        }
        // 2. Credential snapshot — last saved mode, survives across requests.
        String storedMode = credSnap("credentialsMode");
        if ("managed".equalsIgnoreCase(storedMode)) {
            return true;
        }
        if ("own".equalsIgnoreCase(storedMode)) {
            return false;
        }
        // 3. Auto-detect via super (invoker attributes + ManagedCredentialStore heuristic).
        return super.isManagedCredentialsEnabled();
    }

    /**
     * Returns the REST client configured for the current authentication mode.
     * For Basic Auth: uses domain name, email, and API key.
     * For OAuth: uses cloud ID from accessible resources and bearer token.
     */
    public ConfluenceRESTClient getRestClient() {
        logger.info("ConfluenceAttributes.getRestClient() called");
        if (restClient == null) {
            if (areOAuthCredentialsPresent() && isServiceAccountModeEnabled()) {
                // Server-to-server (2LO): machine identity, no user consent.
                logger.info("Service-account mode, creating 2LO (client-credentials) REST client");
                ClientCredentialsTokenProvider provider = new ClientCredentialsTokenProvider(this);
                AccessibleResource resource = provider.getAccessibleResources();
                this.siteBaseUrl = resource.getUrl();
                restClient = new ConfluenceRESTClient(resource.getId(), provider::getToken);
                logger.info("2LO REST client created with cloudId: {}, siteUrl: {}", resource.getId(), siteBaseUrl);
            } else if (areOAuthCredentialsPresent()) {
                logger.info("OAuth credentials detected, creating OAuth REST client");
                ConfluenceOAuthService oAuthService = new ConfluenceOAuthService(
                        requestContext, sessionManager, getInvoker(), authorizationContext, this);
                // Use calledFromSetup=true only to resolve the cloud ID with the admin
                // (app-wide) token. After that, reset to false so subsequent token calls
                // via oAuthService::getConfluenceToken honour invokeAsUser: when
                // invokeAsUser=true and no per-user refresh token is stored,
                // getAccessToken() will throw MustAuthorizeException and the platform
                // will return an authentication link to the MCP caller.
                oAuthService.setCalledFromSetup(true);
                AccessibleResource resources = oAuthService.getAccessibleResources();
                oAuthService.setCalledFromSetup(false);
                this.siteBaseUrl = resources.getUrl();
                restClient = new ConfluenceRESTClient(resources.getId(), () -> {
                    String accessToken = oAuthService.getConfluenceToken();
                    System.out.println("OAuth access token for this request: {}"+ accessToken);
                    return accessToken;
                });
                logger.info("OAuth REST client created with cloudId: {}, siteUrl: {}", resources.getId(), siteBaseUrl);
            } else {
                logger.info("Basic Auth mode, creating Basic Auth REST client");
                String domain = getDomainName();
                String email = getAccountEmail();
                String key = getApiKey();
                restClient = new ConfluenceRESTClient(domain, email, key);
                logger.info("Basic Auth REST client created");
            }
        }
        return restClient;
    }

    /**
     * Returns the user-facing Confluence base URL (ending in {@code /wiki}),
     * suitable for building browser links such as page and attachment URLs —
     * e.g. {@code https://acme.atlassian.net/wiki}.
     *
     * <p>Under OAuth (3LO or 2LO) the real site subdomain is known only from
     * the accessible resource, so this resolves the REST client (which fetches
     * and caches that resource) and uses its site URL. Under Basic Auth the URL
     * is derived from the configured domain name. Resolution failures fall back
     * to the domain-name form so link building never throws — the actual data
     * request surfaces any auth error.</p>
     */
    public String getDomainURL() {
        if (areOAuthCredentialsPresent()) {
            try {
                if (siteBaseUrl == null || siteBaseUrl.isBlank()) {
                    // Building the REST client resolves and caches the site URL.
                    getRestClient();
                }
            } catch (RuntimeException cause) {
                logger.warn("Could not resolve OAuth site URL for link building: {}", cause.getMessage());
            }
            if (siteBaseUrl != null && !siteBaseUrl.isBlank()) {
                return withWikiSuffix(stripTrailingSlash(siteBaseUrl));
            }
        }
        String domain = getDomainName();
        if (domain != null && (domain.startsWith("http://") || domain.startsWith("https://"))) {
            return withWikiSuffix(stripTrailingSlash(domain));
        }
        return String.format("https://%s.atlassian.net/wiki", domain);
    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private static String withWikiSuffix(String baseUrl) {
        return baseUrl.endsWith("/wiki") ? baseUrl : baseUrl + "/wiki";
    }

    /**
     * Exposes the "Krista Managed" credentials chooser: when the
     * {@value ConfluenceConstants#CREDENTIALS_MODE} attribute is
     * {@code "managed"}, the OAuth client id/secret come from the SDK's
     * bundled {@code atlassian-managed-credentials.properties}.
     */
    @Override
    protected String getCredentialsModeFieldName() {
        return ConfluenceConstants.CREDENTIALS_MODE;
    }

    /**
     * Exposes the server-to-server (2LO) toggle: when the
     * {@value ConfluenceConstants#SERVICE_ACCOUNT} attribute is {@code true},
     * OAuth credentials are used with the client-credentials grant instead of
     * the 3LO user-consent flow.
     */
    @Override
    protected String getServiceAccountFieldName() {
        return ConfluenceConstants.SERVICE_ACCOUNT;
    }

    /** Extension type for per-extension managed-credential overrides. */
    @Override
    protected String getExtensionType() {
        return "confluence";
    }

    // --- Validation ---

    /**
     * Structural validation of a candidate attribute map.
     *
     * <p>Intentionally overrides the SDK template <em>without</em> invoking
     * the live {@link #testConnection(Map)} step: the legacy Confluence flow
     * performs the live connectivity check in
     * {@code ConfluenceExtension#validateAttributes} itself, and the legacy
     * user-facing messages (field labels "Domain Name", "Account Email",
     * "API Key" and their ordering) are preserved verbatim.</p>
     */
    @Override
    public void validateAttributes(Map<String, Object> attributes) {
        // Check for mixed credentials
        if (hasMixedCredentials(attributes)) {
            throw new IllegalArgumentException(
                    "Please choose ONE authentication method:\n" +
                            " - For OAuth 2.0: Provide ONLY Client Id and Client Secret (remove Domain Name, Account Email, API Key)\n" +
                            " - For Basic Auth: Provide ONLY Domain Name, Account Email, and API Key (remove Client Id, Client Secret)\n\n" +
                            "Do not mix credentials from both authentication methods."
            );
        }

        // Ensure at least one complete credential set is provided
        if (!basicCredentialsPresentInAttributes(attributes) && !oAuthCredentialsPresentInAttributes(attributes)) {
            throw new IllegalArgumentException(
                    "Please choose ONE authentication method:\n" +
                            " - For OAuth 2.0: Provide Client Id and Client Secret\n" +
                            " - For Basic Auth: Provide Domain Name, Account Email, and API Key"
            );
        }

        // Check for partial credentials
        if (isPartialOAuthCredentialPresent(attributes)) {
            throw new IllegalArgumentException(
                    "Incomplete OAuth 2.0 credentials detected.\n" +
                            "Please provide BOTH Client Id and Client Secret for OAuth authentication."
            );
        }
        if (isPartialBasicCredentialPresent(attributes)) {
            throw new IllegalArgumentException(
                    "Incomplete Basic Auth credentials detected.\n" +
                            "Please provide ALL three fields: Domain Name, Account Email, and API Key for Basic authentication."
            );
        }

        // Validate email format for basic auth
        if (basicCredentialsPresentInAttributes(attributes)) {
            String email = (String) attributes.get(ConfluenceConstants.AUTHORIZATION_EMAIL);
            if (email != null && !isValidEmailAddress(email)) {
                throw new IllegalArgumentException("Account Email is not a valid email address");
            }
        }

        // Validate OAuth credential format
        if (oAuthCredentialsPresentInAttributes(attributes)) {
            validateOAuthCredentialsFormat(attributes);
        }
    }

    /**
     * Live connectivity check against the Confluence "spaces" endpoint,
     * mirroring the check performed by
     * {@code ConfluenceExtension#validateAttributes}. Available through the
     * SDK contract; the Confluence {@link #validateAttributes(Map)} override
     * does not invoke it (see its javadoc). On failure the previous attribute
     * state is restored from the invoker.
     */
    @Override
    protected void testConnection(Map<String, Object> candidateAttributes) {
        try {
            HttpResponse<String> response = getRestClient().doSyncGet(APIVersion.V2, null, "spaces");
            if (response == null || response.statusCode() != HttpURLConnection.HTTP_OK) {
                String errorMessage = response != null ? response.body() : "Failed to get api response";
                throw new IllegalArgumentException(errorMessage);
            }
        } catch (MustAuthorizeException cause) {
            throw cause;
        } catch (RuntimeException cause) {
            setAttributes(getInvoker().getAttributes());
            throw cause;
        }
    }

    private void validateOAuthCredentialsFormat(Map<String, Object> attributes) {
        String testClientId = (String) attributes.get(ConfluenceConstants.CLIENT_ID);
        String testClientSecret = (String) attributes.get(ConfluenceConstants.CLIENT_SECRET);

        if (testClientId == null || !testClientId.matches("^[a-zA-Z0-9_-]+$")) {
            logger.error("Validation FAILED: Client ID is null or contains invalid characters");
            throw new IllegalArgumentException(
                    "Client ID format is invalid. It should contain only alphanumeric characters, hyphens, and underscores.");
        }

        if (testClientSecret == null || testClientSecret.length() < MIN_CLIENT_SECRET_LENGTH) {
            logger.debug("Please verify you copied the complete Client Secret from Atlassian Developer Console.");
        }
        logger.info("OAuth credentials format validation passed");
    }

    private boolean isValidEmailAddress(String email) {
        return EMAIL_PATTERN.matcher(email).matches();
    }
}
