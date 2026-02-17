/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.context.ApplicationScoped;

/**
 * Service for discovering and parsing migration files and metadata
 *
 * @author Dr M H B Ariyaratne <buddhika.ari@gmail.com>
 */
@ApplicationScoped
public class MigrationDiscoveryService {

    private static final Logger LOGGER = Logger.getLogger(MigrationDiscoveryService.class.getName());
    private static final String MIGRATIONS_BASE_PATH = "/db/migrations";
    private static final String MIGRATION_INFO_FILENAME = "migration-info.json";
    private static final String MIGRATION_SQL_FILENAME = "migration.sql";
    private static final String ROLLBACK_SQL_FILENAME = "rollback.sql";

    private final ObjectMapper objectMapper;

    public MigrationDiscoveryService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Discover all available migration versions
     */
    public List<String> discoverMigrationVersions() {
        List<String> versions = new ArrayList<>();

        try {
            URL migrationsUrl = getClass().getResource(MIGRATIONS_BASE_PATH);
            if (migrationsUrl == null) {
                LOGGER.warning("Migrations directory not found: " + MIGRATIONS_BASE_PATH);
                return versions;
            }

            // Convert URL to URI to properly handle percent-encoding (e.g., %20 for spaces)
            URI migrationsUri = migrationsUrl.toURI();
            File migrationsDir = new File(migrationsUri);

            if (migrationsDir.exists() && migrationsDir.isDirectory()) {
                File[] versionDirs = migrationsDir.listFiles(File::isDirectory);
                if (versionDirs != null) {
                    for (File versionDir : versionDirs) {
                        String version = versionDir.getName();
                        if (version.startsWith("v") && hasMigrationFiles(versionDir)) {
                            versions.add(version);
                        }
                    }
                }
            }
        } catch (URISyntaxException e) {
            LOGGER.log(Level.SEVERE, "Invalid URL format for migrations directory: " + MIGRATIONS_BASE_PATH, e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error discovering migration versions", e);
        }

        // Sort versions using semantic versioning
        Collections.sort(versions, this::compareVersions);
        return versions;
    }

    /**
     * Check if a version directory has required migration files
     */
    private boolean hasMigrationFiles(File versionDir) {
        File migrationSql = new File(versionDir, MIGRATION_SQL_FILENAME);
        return migrationSql.exists() && migrationSql.isFile();
    }

    /**
     * Load migration info from JSON file
     */
    public MigrationInfo loadMigrationInfo(String version) {
        try {
            String infoPath = MIGRATIONS_BASE_PATH + "/" + version + "/" + MIGRATION_INFO_FILENAME;
            InputStream infoStream = getClass().getResourceAsStream(infoPath);

            if (infoStream == null) {
                LOGGER.warning("Migration info not found for version: " + version);
                // Return minimal info if JSON not found
                return new MigrationInfo(version, "Migration " + version);
            }

            MigrationInfo info = objectMapper.readValue(infoStream, MigrationInfo.class);
            return info;

        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error loading migration info for version: " + version, e);
            return new MigrationInfo(version, "Migration " + version + " (metadata error)");
        }
    }

    /**
     * Load migration SQL content
     */
    public String loadMigrationSql(String version) {
        return loadFileContent(version, MIGRATION_SQL_FILENAME);
    }

    /**
     * Load rollback SQL content
     */
    public String loadRollbackSql(String version) {
        return loadFileContent(version, ROLLBACK_SQL_FILENAME);
    }

    /**
     * Load content from a specific file in migration directory
     */
    private String loadFileContent(String version, String filename) {
        try {
            String filePath = MIGRATIONS_BASE_PATH + "/" + version + "/" + filename;
            InputStream fileStream = getClass().getResourceAsStream(filePath);

            if (fileStream == null) {
                LOGGER.warning("File not found: " + filePath);
                return null;
            }

            byte[] bytes = fileStream.readAllBytes();
            return new String(bytes);

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error loading file content: " + filename + " for version: " + version, e);
            return null;
        }
    }

    /**
     * Check if migration files exist for a version
     */
    public boolean migrationExists(String version) {
        String migrationPath = MIGRATIONS_BASE_PATH + "/" + version + "/" + MIGRATION_SQL_FILENAME;
        InputStream stream = getClass().getResourceAsStream(migrationPath);
        return stream != null;
    }

    /**
     * Check if rollback file exists for a version
     */
    public boolean rollbackExists(String version) {
        String rollbackPath = MIGRATIONS_BASE_PATH + "/" + version + "/" + ROLLBACK_SQL_FILENAME;
        InputStream stream = getClass().getResourceAsStream(rollbackPath);
        return stream != null;
    }

    /**
     * Get all migration info objects sorted by version
     */
    public List<MigrationInfo> loadAllMigrationInfo() {
        List<String> versions = discoverMigrationVersions();
        List<MigrationInfo> infos = new ArrayList<>();

        for (String version : versions) {
            MigrationInfo info = loadMigrationInfo(version);
            if (info != null) {
                infos.add(info);
            }
        }

        return infos;
    }

    /**
     * Find migrations between two versions (exclusive start, inclusive end)
     */
    public List<String> findVersionsBetween(String fromVersion, String toVersion) {
        List<String> allVersions = discoverMigrationVersions();
        List<String> result = new ArrayList<>();

        for (String version : allVersions) {
            if (compareVersions(version, fromVersion) > 0 && compareVersions(version, toVersion) <= 0) {
                result.add(version);
            }
        }

        return result;
    }

    /**
     * Compare two version strings using semantic versioning
     */
    private int compareVersions(String version1, String version2) {
        if (version1 == null && version2 == null) {
            return 0;
        }
        if (version1 == null) {
            return -1;
        }
        if (version2 == null) {
            return 1;
        }

        // Remove 'v' prefix if present
        String v1 = version1.startsWith("v") ? version1.substring(1) : version1;
        String v2 = version2.startsWith("v") ? version2.substring(1) : version2;

        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");

        int maxLength = Math.max(parts1.length, parts2.length);

        for (int i = 0; i < maxLength; i++) {
            int num1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int num2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;

            if (num1 != num2) {
                return Integer.compare(num1, num2);
            }
        }

        return 0;
    }

    /**
     * Get latest available migration version
     */
    public String getLatestMigrationVersion() {
        List<String> versions = discoverMigrationVersions();
        return versions.isEmpty() ? null : versions.get(versions.size() - 1);
    }

    /**
     * Validate migration directory structure
     */
    public List<String> validateMigrationStructure() {
        List<String> issues = new ArrayList<>();
        List<String> versions = discoverMigrationVersions();

        for (String version : versions) {
            // Check required files
            if (!migrationExists(version)) {
                issues.add("Missing migration.sql for version: " + version);
            }

            // Check metadata
            MigrationInfo info = loadMigrationInfo(version);
            if (info.getVersion() == null || !info.getVersion().equals(version)) {
                issues.add("Version mismatch in metadata for: " + version);
            }
        }

        return issues;
    }
}