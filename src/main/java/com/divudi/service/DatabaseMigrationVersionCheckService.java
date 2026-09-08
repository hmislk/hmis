package com.divudi.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

/**
 * Fetches the wiki-hosted DDL "Last Update" version string used to detect
 * whether a deployed schema is current, and runs the version comparison in
 * the background so callers never block on the outbound HTTP request.
 *
 * Shared by {@link DatabaseMigrationService}'s automatic post-startup check
 * and by the manual "Check Missing Fields" flow in
 * {@code DataAdministrationController} — both used to duplicate this
 * fetch/regex logic independently.
 *
 * @author Dr M H B Ariyaratne
 */
@Stateless
public class DatabaseMigrationVersionCheckService {

    private static final Logger LOGGER = Logger.getLogger(DatabaseMigrationVersionCheckService.class.getName());
    private static final String WIKI_DDL_URL = "https://github.com/hmislk/hmis/wiki/Database-Schema-DDL-Generation-Guide";
    private static final Pattern WIKI_DDL_VERSION_PATTERN
            = Pattern.compile("Last Update\\s*-\\s*(\\d{4}\\.\\d{2}\\.\\d{2}\\s+\\d{2}[.:]\\d{2})");

    /**
     * Fetches the wiki page and extracts the "Last Update" DDL version
     * string, e.g. "2026.08.07 04.00". Returns null on any network or parse
     * failure — callers must treat null as "unknown, fall back to the
     * legacy check" rather than raising an error.
     */
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public String fetchWikiDdlVersion() {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(WIKI_DDL_URL);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("User-Agent", "HMIS-Schema-Checker/1.0");
            int status = conn.getResponseCode();
            if (status == 200) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        Matcher m = WIKI_DDL_VERSION_PATTERN.matcher(line);
                        if (m.find()) {
                            return m.group(1).trim();
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Network or parse failure — return null so caller falls back to legacy check
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return null;
    }

    /**
     * Compares the wiki's current DDL version against the caller's stored
     * version and, on a match, marks migration complete. Runs on a
     * container-managed background thread — {@code @Asynchronous} only
     * takes effect when this method is invoked through an injected
     * (proxied) reference to this bean, never via self-invocation.
     *
     * Any failure (wiki unreachable, timeout, parse error) is swallowed at
     * FINE level: this is a best-effort convenience check, and it must
     * never surface an error to end users or affect the migrationPending
     * default the caller already has.
     */
    @Asynchronous
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void checkAndUpdateMigrationStatus(DatabaseMigrationService migrationService) {
        if (migrationService == null) {
            return;
        }
        try {
            String wikiVersion = fetchWikiDdlVersion();
            if (wikiVersion == null) {
                LOGGER.fine("DatabaseMigrationVersionCheckService: wiki DDL version unreachable during background check.");
                return;
            }
            if (wikiVersion.equals(migrationService.readStoredDdlVersion())) {
                migrationService.markMigrationComplete();
                LOGGER.info("DatabaseMigrationVersionCheckService: background check confirmed schema is up to date "
                        + "(version " + wikiVersion + ") — migration banner cleared.");
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "DatabaseMigrationVersionCheckService: background DDL version check failed.", e);
        }
    }
}
