/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.facade;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Temporary remedy for running two application builds against the same
 * database when they use different ID generation strategies:
 *
 * <ul>
 * <li>an old production build with {@code GenerationType.AUTO} — EclipseLink
 * table sequencing, which allocates IDs from the {@code SEQUENCE} table and
 * sends them explicitly on INSERT, and</li>
 * <li>a new build with {@code GenerationType.IDENTITY} — MySQL
 * {@code AUTO_INCREMENT}.</li>
 * </ul>
 *
 * The two allocators are independent, and MySQL's auto_increment counter
 * always chases the highest inserted ID, so the IDENTITY build's next insert
 * lands exactly on the AUTO build's next preallocated sequence value —
 * a guaranteed duplicate-primary-key collision (e.g. REPORTLOG at Ruhunu).
 * Simply bumping the sequence cannot fix this; the two allocators must be
 * separated into disjoint ranges:
 *
 * <ol>
 * <li>every {@code SEQUENCE} row is raised just above the highest existing ID,
 * so the AUTO build stops re-issuing IDs the IDENTITY build already used;</li>
 * <li>every table's {@code AUTO_INCREMENT} counter is pushed
 * {@link #AUTO_INCREMENT_OFFSET} above the highest existing ID, so the
 * IDENTITY build allocates in a high band the sequence will not reach.
 * Explicit inserts below an auto_increment counter never move it, so the
 * bands stay disjoint.</li>
 * </ol>
 *
 * The AUTO build's servers must be restarted after this runs — they hold a
 * preallocated sequence block in memory and keep using it until restarted.
 *
 * Full background, runbook and removal plan:
 * developer_docs/database/dual-version-id-allocator-separation.md
 */
final class IdAllocatorSeparation {

    /**
     * SEQUENCE rows are raised to global max ID + this margin. The margin
     * absorbs IDENTITY-build inserts that happen between scanning the max and
     * applying the AUTO_INCREMENT offsets below.
     */
    static final long SEQUENCE_SAFETY_MARGIN = 10_000L;

    /**
     * AUTO_INCREMENT counters jump to global max ID + this offset. The AUTO
     * build's sequence would need to allocate this many IDs to catch up —
     * far beyond the temporary dual-version window.
     */
    static final long AUTO_INCREMENT_OFFSET = 1_000_000_000L;

    private IdAllocatorSeparation() {
    }

    /**
     * Runs the separation on the schema of the given connection. The caller
     * must supply a raw, autoCommit=true connection outside JTA (ALTER TABLE
     * causes implicit commits) and is responsible for closing it.
     *
     * @return human-readable report of what was scanned and changed
     */
    static List<String> separate(Connection conn) throws SQLException {
        List<String> report = new ArrayList<>();

        // Step 1: inventory entity tables — BIGINT primary key named ID.
        // The stored case of both table and column names varies between
        // deployments (lower_case_table_names), so take them verbatim from
        // INFORMATION_SCHEMA instead of hardcoding either case.
        Map<String, String> idColumnByTable = new LinkedHashMap<>();
        String tableQuery = "SELECT TABLE_NAME, COLUMN_NAME FROM information_schema.COLUMNS "
                + "WHERE TABLE_SCHEMA = DATABASE() "
                + "AND UPPER(COLUMN_NAME) = 'ID' "
                + "AND COLUMN_KEY = 'PRI' "
                + "AND DATA_TYPE = 'bigint' "
                + "ORDER BY TABLE_NAME";
        try (PreparedStatement ps = conn.prepareStatement(tableQuery);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                idColumnByTable.put(rs.getString(1), rs.getString(2));
            }
        }
        if (idColumnByTable.isEmpty()) {
            report.add("No tables with a BIGINT ID primary key found — nothing to do.");
            return report;
        }

        // Step 2: global max ID across every table. Both targets below are
        // derived from one shared maximum so the sequence band and the
        // auto_increment band are disjoint across ALL tables, not per table.
        // MAX(primary key) is an index-end lookup — cheap even on BILL/BILLITEM.
        long globalMaxId = 0;
        for (Map.Entry<String, String> e : idColumnByTable.entrySet()) {
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                         "SELECT MAX(`" + e.getValue() + "`) FROM `" + e.getKey() + "`")) {
                if (rs.next()) {
                    globalMaxId = Math.max(globalMaxId, rs.getLong(1));
                }
            }
        }
        report.add("Scanned " + idColumnByTable.size() + " tables; highest existing ID = " + globalMaxId + ".");

        // Step 3: raise every SEQUENCE row (not just SEQ_GEN — custom
        // generators would collide the same way) above every existing ID.
        // Rows already at or beyond the target are left alone, so re-running
        // never rewinds a sequence.
        String sequenceTable = findSequenceTableName(conn);
        if (sequenceTable == null) {
            report.add("No SEQUENCE table in this schema — sequence bump skipped.");
        } else {
            long sequenceTarget = globalMaxId + SEQUENCE_SAFETY_MARGIN;
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT SEQ_NAME, SEQ_COUNT FROM `" + sequenceTable + "`")) {
                while (rs.next()) {
                    report.add("Sequence " + rs.getString(1) + " was at " + rs.getLong(2) + ".");
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE `" + sequenceTable + "` SET SEQ_COUNT = ? WHERE SEQ_COUNT < ?")) {
                ps.setLong(1, sequenceTarget);
                ps.setLong(2, sequenceTarget);
                int updated = ps.executeUpdate();
                report.add("Raised " + updated + " sequence row(s) to " + sequenceTarget + ".");
            }
        }

        // Step 4: move every table's AUTO_INCREMENT counter into the high
        // band — including empty tables, because the IDENTITY build must
        // allocate high everywhere, even in tables it has not written yet.
        // ALTER ... AUTO_INCREMENT is a metadata-only change (no row rewrite),
        // and InnoDB ignores a target below the current counter, so re-running
        // is harmless. Failures are collected per table rather than aborting:
        // a partial separation still protects every table that was altered.
        long autoIncrementTarget = globalMaxId + AUTO_INCREMENT_OFFSET;
        int altered = 0;
        List<String> failures = new ArrayList<>();
        for (String tableName : idColumnByTable.keySet()) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE `" + tableName + "` AUTO_INCREMENT = " + autoIncrementTarget);
                altered++;
            } catch (SQLException e) {
                failures.add("Could not set AUTO_INCREMENT on `" + tableName + "`: " + e.getMessage());
            }
        }
        report.add("Set AUTO_INCREMENT to " + autoIncrementTarget + " on " + altered + " of "
                + idColumnByTable.size() + " table(s).");
        report.addAll(failures);
        return report;
    }

    private static String findSequenceTableName(Connection conn) throws SQLException {
        String query = "SELECT TABLE_NAME FROM information_schema.TABLES "
                + "WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'SEQUENCE'";
        try (PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getString(1) : null;
        }
    }
}
