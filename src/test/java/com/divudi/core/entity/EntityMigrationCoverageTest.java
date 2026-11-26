package com.divudi.core.entity;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Fails when an entity maps a table or column that no migration under
 * src/main/resources/db/migrations mentions and that is not in the checked-in
 * baseline (src/test/resources/entity-migration-baseline.txt). Issue #26178.
 *
 * The baseline lists every mapped table and column that had no migration when
 * the check was introduced (the schema that the base installation and
 * EclipseLink DDL already created). A new mapped column or table must either
 * be created by a migration, or be added to the baseline on purpose.
 *
 * The check is advisory: it never blocks a merge. It runs only when
 * -Dentity.migration.guard=true is set (the Entity migration guard workflow),
 * so a normal mvn test skips it. Each gap is printed as a GitHub ::warning
 * annotation line.
 *
 * Regenerate the baseline: mvn -q test -Dtest=EntityMigrationCoverageTest
 * -Dentity.baseline.regenerate=true
 *
 * Names follow EclipseLink defaults: table = entity class name (or @Table),
 * column = field name (or @Column), join column = FIELD_ID (or @JoinColumn).
 * Subclasses of an entity share the root entity's table (single table).
 * Names compare case-insensitively (migrations detect table-name case).
 * A migration "mentions" a column when the same .sql file contains both the
 * table name and the column name as whole identifiers.
 */
public class EntityMigrationCoverageTest {

    private static final Path MIGRATIONS = Paths.get("src/main/resources/db/migrations");
    private static final Path BASELINE = Paths.get("src/test/resources/entity-migration-baseline.txt");
    private static final Path CLASSES = Paths.get("target/classes");
    private static final Pattern IDENT = Pattern.compile("[A-Za-z0-9_]+");

    /** One mapped item: a table (column == null) or a column. */
    static final class Mapped {
        final String table;
        final String column;
        final String where;

        Mapped(String table, String column, String where) {
            this.table = table.toUpperCase(Locale.ROOT);
            this.column = column == null ? null : column.toUpperCase(Locale.ROOT);
            this.where = where;
        }

        String key() {
            return column == null ? table : table + "." + column;
        }
    }

    @Test
    public void everyMappedTableAndColumnHasAMigrationOrIsInTheBaseline() throws Exception {
        assumeTrue(Boolean.getBoolean("entity.migration.guard") || Boolean.getBoolean("entity.baseline.regenerate"),
                "advisory check; set -Dentity.migration.guard=true to run it");
        List<Mapped> mapped = collectMapped();
        List<Set<String>> files = readMigrationTokens();
        Set<String> baseline = readBaseline();

        Set<String> uncovered = new TreeSet<>();
        Map<String, Mapped> byKey = new java.util.TreeMap<>();
        Set<String> failedTables = new HashSet<>();
        List<String> failures = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        for (Mapped m : mapped) {
            byKey.putIfAbsent(m.key(), m);
        }
        // tables first, so a missing table is reported once and not once per column
        for (Mapped m : byKey.values()) {
            if (m.column == null && !covered(files, m)) {
                uncovered.add(m.key());
                if (!baseline.contains(m.key())) {
                    failedTables.add(m.table);
                    String msg = m.table + " (entity " + m.where + ") has no migration that creates it";
                    failures.add("Table " + msg + ".");
                    warnings.add("::warning title=Table with no migration::" + msg);
                }
            }
        }
        for (Mapped m : byKey.values()) {
            if (m.column != null && !covered(files, m)) {
                uncovered.add(m.key());
                if (!baseline.contains(m.key()) && !failedTables.contains(m.table)) {
                    String msg = m.table + "." + m.column + " (" + m.where + ") has no migration that adds it";
                    failures.add("Column " + msg + ".");
                    warnings.add("::warning title=Column with no migration::" + msg);
                }
            }
        }

        if (Boolean.getBoolean("entity.baseline.regenerate")) {
            Files.write(BASELINE, uncovered.stream().collect(Collectors.toList()), StandardCharsets.UTF_8);
            return;
        }
        for (String w : warnings) {
            System.out.println(w);
        }
        assertTrue(failures.isEmpty(), "\nMapped entity columns or tables with no migration:\n  "
                + String.join("\n  ", failures)
                + "\nFix: add a migration under src/main/resources/db/migrations/ that creates it"
                + " (see developer_docs/database/migration-development-guide.md)."
                + "\nOnly if the base schema already has it: add the TABLE or TABLE.COLUMN line to "
                + BASELINE + ".\n");
    }

    private static boolean covered(List<Set<String>> files, Mapped m) {
        for (Set<String> tokens : files) {
            if (tokens.contains(m.table) && (m.column == null || tokens.contains(m.column))) {
                return true;
            }
        }
        return false;
    }

    private static List<Set<String>> readMigrationTokens() throws IOException {
        List<Set<String>> out = new ArrayList<>();
        try (Stream<Path> s = Files.walk(MIGRATIONS)) {
            for (Path p : s.filter(f -> f.toString().endsWith(".sql")).collect(Collectors.toList())) {
                // rollback scripts remove columns; they do not create them
                if (p.getFileName().toString().toLowerCase(Locale.ROOT).startsWith("rollback")) {
                    continue;
                }
                String text = new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
                Set<String> tokens = new HashSet<>();
                Matcher m = IDENT.matcher(text);
                while (m.find()) {
                    tokens.add(m.group().toUpperCase(Locale.ROOT));
                }
                out.add(tokens);
            }
        }
        return out;
    }

    private static Set<String> readBaseline() throws IOException {
        Set<String> out = new HashSet<>();
        if (!Files.exists(BASELINE)) {
            return out;
        }
        for (String line : Files.readAllLines(BASELINE, StandardCharsets.UTF_8)) {
            String t = line.trim();
            if (!t.isEmpty() && !t.startsWith("#")) {
                out.add(t.toUpperCase(Locale.ROOT));
            }
        }
        return out;
    }

    private static List<Mapped> collectMapped() throws Exception {
        List<Class<?>> entities = new ArrayList<>();
        ClassLoader cl = EntityMigrationCoverageTest.class.getClassLoader();
        try (Stream<Path> s = Files.walk(CLASSES)) {
            for (Path p : s.filter(f -> f.toString().endsWith(".class")).collect(Collectors.toList())) {
                String rel = CLASSES.relativize(p).toString();
                String name = rel.substring(0, rel.length() - 6).replace('/', '.').replace('\\', '.');
                if (!name.startsWith("com.divudi.")) {
                    continue;
                }
                Class<?> c = Class.forName(name, false, cl);
                if (c.isAnnotationPresent(Entity.class)) {
                    entities.add(c);
                }
            }
        }
        List<Mapped> out = new ArrayList<>();
        for (Class<?> c : entities) {
            Class<?> root = c;
            while (root.getSuperclass() != null && root.getSuperclass().isAnnotationPresent(Entity.class)) {
                root = root.getSuperclass();
            }
            String table = root.isAnnotationPresent(Table.class) && !root.getAnnotation(Table.class).name().isEmpty()
                    ? root.getAnnotation(Table.class).name() : root.getSimpleName();
            if (c == root) {
                out.add(new Mapped(table, null, c.getName()));
            }
            // fields declared on non-entity superclasses (mapped superclasses) belong to this entity
            for (Class<?> k = c; k != null && k != Object.class; k = k.getSuperclass()) {
                if (k != c && k.isAnnotationPresent(Entity.class)) {
                    break; // its own fields are handled when k is processed
                }
                for (Field f : k.getDeclaredFields()) {
                    addField(out, table, c, f);
                }
            }
        }
        return out;
    }

    private static void addField(List<Mapped> out, String table, Class<?> entity, Field f) {
        int mod = f.getModifiers();
        if (Modifier.isStatic(mod) || Modifier.isTransient(mod) || f.isSynthetic()
                || f.isAnnotationPresent(Transient.class)) {
            return;
        }
        String where = "field " + entity.getSimpleName() + "." + f.getName();
        if (f.isAnnotationPresent(ManyToMany.class) || f.isAnnotationPresent(ElementCollection.class)) {
            JoinTable jt = f.getAnnotation(JoinTable.class);
            if (jt != null && !jt.name().isEmpty()) {
                out.add(new Mapped(jt.name(), null, where));
            }
            return;
        }
        if (f.isAnnotationPresent(OneToMany.class)) {
            return; // the foreign key sits on the other table
        }
        OneToOne oo = f.getAnnotation(OneToOne.class);
        if (oo != null && !oo.mappedBy().isEmpty()) {
            return;
        }
        boolean toOne = f.isAnnotationPresent(ManyToOne.class) || oo != null
                || (f.getType().isAnnotationPresent(Entity.class) && !f.isAnnotationPresent(Column.class));
        if (toOne) {
            List<JoinColumn> jcs = new ArrayList<>();
            if (f.isAnnotationPresent(JoinColumn.class)) {
                jcs.add(f.getAnnotation(JoinColumn.class));
            }
            if (f.isAnnotationPresent(JoinColumns.class)) {
                for (JoinColumn j : f.getAnnotation(JoinColumns.class).value()) {
                    jcs.add(j);
                }
            }
            if (jcs.isEmpty()) {
                out.add(new Mapped(table, f.getName() + "_ID", where));
            }
            for (JoinColumn j : jcs) {
                out.add(new Mapped(table, j.name().isEmpty() ? f.getName() + "_ID" : j.name(), where));
            }
            return;
        }
        Column col = f.getAnnotation(Column.class);
        String name = col != null && !col.name().isEmpty() ? col.name() : f.getName();
        out.add(new Mapped(table, name, where));
    }
}
