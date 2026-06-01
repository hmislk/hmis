/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.service.sap;

import com.divudi.bean.common.ConfigOptionApplicationController;
import java.io.Serializable;
import java.io.StringReader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.util.Base64;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

/**
 * Fetches and caches SAP S/4HANA Cloud OAuth 2.0 client-credentials tokens.
 *
 * Singleton so the cached token survives across requests. Refreshes automatically
 * when the token has expired or when the caller signals a 401 via {@link #invalidate()}.
 */
@Singleton
public class SapTokenService implements Serializable {

    private static final Logger LOG = Logger.getLogger(SapTokenService.class.getName());
    private static final long serialVersionUID = 1L;

    // Config keys — values stored in ConfigOption, never hard-coded
    public static final String KEY_BASE_URL     = "SAP Integration - Base URL";
    public static final String KEY_TOKEN_URL    = "SAP Integration - Token URL";
    public static final String KEY_CLIENT_ID    = "SAP Integration - Client ID";
    public static final String KEY_CLIENT_SECRET = "SAP Integration - Client Secret";
    public static final String KEY_ENABLED      = "SAP Integration - Enabled";

    @Inject
    private ConfigOptionApplicationController configController;

    private String cachedToken;
    private Instant tokenExpiresAt;

    @PostConstruct
    public void init() {
        cachedToken = null;
        tokenExpiresAt = Instant.EPOCH;
    }

    /**
     * Returns a valid bearer token, fetching a new one if the cache is empty or expired.
     *
     * @return bearer token string, or null if SAP integration is disabled or config is incomplete
     * @throws SapIntegrationException if the token fetch fails
     */
    @Lock(LockType.WRITE)
    public String getBearerToken() throws SapIntegrationException {
        if (!isEnabled()) {
            return null;
        }
        if (isTokenValid()) {
            return cachedToken;
        }
        return fetchNewToken();
    }

    /**
     * Forces token eviction so the next {@link #getBearerToken()} call fetches a fresh one.
     * Call this when a downstream SAP API returns 401.
     */
    @Lock(LockType.WRITE)
    public void invalidate() {
        cachedToken = null;
        tokenExpiresAt = Instant.EPOCH;
    }

    /**
     * Returns true if SAP integration is toggled on and all required config keys are non-empty.
     */
    @Lock(LockType.READ)
    public boolean isEnabled() {
        if (!configController.getBooleanValueByKey(KEY_ENABLED, false)) {
            return false;
        }
        String tokenUrl  = configController.getShortTextValueByKey(KEY_TOKEN_URL, "");
        String clientId  = configController.getShortTextValueByKey(KEY_CLIENT_ID, "");
        String clientSecret = configController.getShortTextValueByKey(KEY_CLIENT_SECRET, "");
        return !tokenUrl.trim().isEmpty() && !clientId.trim().isEmpty() && !clientSecret.trim().isEmpty();
    }

    /**
     * Returns the configured SAP base URL (e.g. {@code https://{tenant}.s4hana.cloud.sap}).
     */
    @Lock(LockType.READ)
    public String getBaseUrl() {
        return configController.getShortTextValueByKey(KEY_BASE_URL, "");
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private boolean isTokenValid() {
        return cachedToken != null && Instant.now().isBefore(tokenExpiresAt);
    }

    private String fetchNewToken() throws SapIntegrationException {
        String tokenUrl    = configController.getShortTextValueByKey(KEY_TOKEN_URL, "");
        String clientId    = configController.getShortTextValueByKey(KEY_CLIENT_ID, "");
        String clientSecret = configController.getShortTextValueByKey(KEY_CLIENT_SECRET, "");

        String body = "grant_type=client_credentials"
                + "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                + "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8);

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(15))
                    .build();

            String basicCredentials = Base64.getEncoder().encodeToString(
                    (URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                            + ":" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8))
                            .getBytes(StandardCharsets.UTF_8));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(tokenUrl))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Authorization", "Basic " + basicCredentials)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new SapIntegrationException("SAP token endpoint returned HTTP " + response.statusCode()
                        + ": " + response.body());
            }

            try (JsonReader reader = Json.createReader(new StringReader(response.body()))) {
                JsonObject json = reader.readObject();

                if (!json.containsKey("access_token")) {
                    throw new SapIntegrationException("SAP token response missing access_token: " + response.body());
                }

                cachedToken = json.getString("access_token");

                // expires_in is in seconds; subtract 30s buffer so we refresh before actual expiry.
                // Use 0 as the floor (not 30) so short-lived tokens don't get cached past their real expiry.
                int expiresIn = json.containsKey("expires_in") ? json.getInt("expires_in") : 3600;
                long refreshIn = Math.max(expiresIn - 30L, 0L);
                tokenExpiresAt = Instant.now().plusSeconds(refreshIn);

                LOG.log(Level.INFO, "SAP bearer token refreshed, expires in {0}s", expiresIn);
                return cachedToken;
            }

        } catch (SapIntegrationException e) {
            throw e;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to fetch SAP bearer token", e);
            throw new SapIntegrationException("Failed to fetch SAP bearer token: " + e.getMessage(), e);
        }
    }
}
