/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;

/**
 * Post-processes the EclipseLink-generated {@code createDDL.jdbc} file at
 * application startup.
 *
 * <p>EclipseLink's {@code create-or-extend-tables} with
 * {@code output-mode=sql-script} generates only {@code CREATE TABLE} and
 * {@code ALTER TABLE ADD CONSTRAINT} (FK) statements. It does <b>not</b>
 * produce {@code ALTER TABLE ADD COLUMN} statements because the "extend"
 * logic requires a live database comparison ({@code output-mode=database}).
 *
 * <p>This service fills that gap: it reads every {@code CREATE TABLE} in the
 * generated file, extracts every column definition, and appends a
 * corresponding {@code ALTER TABLE ... ADD COLUMN ...} statement to the end of
 * the file. The result is a self-contained DDL script that can update
 * <b>any</b> database regardless of its current schema — new tables are
 * created, and missing columns in existing tables are added.
 *
 * @author Dr M H B Ariyaratne
 */
@Startup
@Singleton
public class DdlFileEnhancerService {

    private static final Logger LOGGER = Logger.getLogger(DdlFileEnhancerService.class.getName());

    private static final String DDL_FILE_NAME = "createDDL.jdbc";
    private static final String APPLICATION_LOCATION = "/home/buddhika/bin";

    @PostConstruct
    public void enhanceDdlFile() {
        Path ddlFilePath = Paths.get(APPLICATION_LOCATION, DDL_FILE_NAME);
        if (!Files.exists(ddlFilePath)) {
            return;
        }

        try {
            String content = new String(Files.readAllBytes(ddlFilePath), StandardCharsets.UTF_8);

            if (content.toUpperCase().contains("ADD COLUMN")) {
                LOGGER.info("DdlFileEnhancer: ALTER TABLE ADD COLUMN already present, skipping.");
                return;
            }

            StringBuilder alterStatements = new StringBuilder();
            alterStatements.append("\n");

            String[] lines = content.split("\\n");
            for (String line : lines) {
                line = line.trim();
                if (line.toUpperCase().startsWith("CREATE TABLE")) {
                    appendAlterStatementsForCreateTable(line, alterStatements);
                }
            }

            if (alterStatements.length() > 1) {
                Files.write(ddlFilePath, alterStatements.toString().getBytes(StandardCharsets.UTF_8),
                        StandardOpenOption.APPEND);
                LOGGER.info("DdlFileEnhancer: Appended ALTER TABLE ADD COLUMN statements to " + ddlFilePath);
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "DdlFileEnhancer: Failed to enhance DDL file at " + ddlFilePath, e);
        }
    }

    private void appendAlterStatementsForCreateTable(String createTableSql, StringBuilder result) {
        String tableName = extractTableName(createTableSql);
        if (tableName == null) {
            return;
        }

        List<String> columns = extractColumnDefinitions(createTableSql);
        for (String colDef : columns) {
            int firstSpace = colDef.indexOf(' ');
            if (firstSpace == -1) {
                continue;
            }
            String columnName = colDef.substring(0, firstSpace).trim();
            String columnType = colDef.substring(firstSpace).trim();

            result.append("ALTER TABLE ")
                    .append(tableName)
                    .append(" ADD COLUMN ")
                    .append(columnName)
                    .append(" ")
                    .append(columnType)
                    .append("\n");
        }
    }

    /**
     * Extract the table name from a CREATE TABLE statement.
     */
    private String extractTableName(String sql) {
        String upper = sql.toUpperCase();
        int ctIdx = upper.indexOf("CREATE TABLE");
        if (ctIdx == -1) {
            return null;
        }
        int start = ctIdx + "CREATE TABLE".length();
        while (start < sql.length() && Character.isWhitespace(sql.charAt(start))) {
            start++;
        }
        int end = sql.indexOf('(', start);
        if (end == -1) {
            return null;
        }
        return sql.substring(start, end).trim().replaceAll("[`\"']", "");
    }

    /**
     * Extract column definitions from a CREATE TABLE statement, skipping
     * constraints (PRIMARY KEY, FOREIGN KEY, etc.).
     */
    private List<String> extractColumnDefinitions(String sql) {
        List<String> columns = new ArrayList<>();
        int start = sql.indexOf('(');
        int end = sql.lastIndexOf(')');
        if (start == -1 || end == -1 || end <= start) {
            return columns;
        }
        String body = sql.substring(start + 1, end);

        StringBuilder current = new StringBuilder();
        int parens = 0;
        for (char c : body.toCharArray()) {
            if (c == '(') {
                parens++;
            } else if (c == ')') {
                parens--;
            }
            if (c == ',' && parens == 0) {
                String def = current.toString().trim();
                if (!def.isEmpty() && !isConstraint(def)) {
                    columns.add(def);
                }
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0) {
            String def = current.toString().trim();
            if (!def.isEmpty() && !isConstraint(def)) {
                columns.add(def);
            }
        }
        return columns;
    }

    private boolean isConstraint(String def) {
        String upper = def.toUpperCase();
        return upper.startsWith("PRIMARY KEY")
                || upper.startsWith("FOREIGN KEY")
                || upper.startsWith("CONSTRAINT")
                || upper.startsWith("UNIQUE")
                || upper.startsWith("CHECK");
    }
}
