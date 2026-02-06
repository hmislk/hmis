package com.divudi.service;

import com.divudi.core.facade.AuditDatabaseFacade;
import com.divudi.core.facade.AbstractFacade;
import com.divudi.core.facade.ItemFacade;
import java.sql.SQLSyntaxErrorException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.persistence.Entity;
import org.reflections.Reflections;

/**
 * Singleton startup EJB that detects missing database fields/tables on
 * application startup. This enables access control for the migration page
 * (mf.xhtml): when migration is pending, anyone can access the page; when no
 * migration is needed, only Admin users can access it.
 *
 * @author Dr M H B Ariyaratne
 */
@Singleton
@Startup
public class DatabaseMigrationService {

    @EJB
    private ItemFacade itemFacade;

    @EJB
    private AuditDatabaseFacade auditDatabaseFacade;

    private volatile boolean migrationPending = true;

    @PostConstruct
    public void init() {
        System.out.println("DatabaseMigrationService: Checking for missing database fields...");
        try {
            int missingCount = countMissingFieldsForDatabase(itemFacade, "Main Database")
                    + countMissingFieldsForDatabase(auditDatabaseFacade, "Audit Database");
            if (missingCount == 0) {
                migrationPending = false;
                System.out.println("DatabaseMigrationService: No missing fields detected. Migration page locked to admin-only.");
            } else {
                migrationPending = true;
                System.out.println("DatabaseMigrationService: " + missingCount + " missing fields/tables detected. Migration page open to all users.");
            }
        } catch (Exception e) {
            migrationPending = true;
            System.out.println("DatabaseMigrationService: Error during check — keeping migration page open. Error: " + e.getMessage());
        }
    }

    private int countMissingFieldsForDatabase(AbstractFacade<?> facade, String databaseName) {
        int count = 0;
        for (Class<?> entityClass : findRootEntityClasses()) {
            String entityName = entityClass.getSimpleName();
            String jpql = "SELECT e FROM " + entityName + " e";
            try {
                facade.executeQueryFirstResult(entityClass, jpql);
            } catch (Exception e) {
                Throwable cause = e;
                while (cause != null && !(cause instanceof SQLSyntaxErrorException)) {
                    cause = cause.getCause();
                }
                if (cause != null) {
                    String message = cause.getMessage();
                    Pattern tablePattern = Pattern.compile("Table '.*?\\.(.*?)' doesn't exist");
                    Matcher tableMatcher = tablePattern.matcher(message);
                    if (tableMatcher.find()) {
                        count++;
                        continue;
                    }
                    Pattern columnPattern = Pattern.compile("Unknown column '([^']+)' in 'field list'");
                    Matcher columnMatcher = columnPattern.matcher(message);
                    while (columnMatcher.find()) {
                        count++;
                    }
                }
            }
        }
        System.out.println("DatabaseMigrationService: " + databaseName + " — " + count + " missing fields/tables.");
        return count;
    }

    private List<Class<?>> findRootEntityClasses() {
        List<Class<?>> rootClasses = new ArrayList<>();
        Reflections reflections = new Reflections("com.divudi.core.entity");
        Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(Entity.class);
        for (Class<?> entityClass : annotated) {
            Class<?> rootEntityClass = entityClass;
            while (rootEntityClass.getSuperclass() != null
                    && rootEntityClass.getSuperclass().getAnnotation(Entity.class) != null) {
                rootEntityClass = rootEntityClass.getSuperclass();
            }
            if (entityClass.equals(rootEntityClass) && !rootClasses.contains(entityClass)) {
                rootClasses.add(entityClass);
            }
        }
        return rootClasses;
    }

    public boolean isMigrationPending() {
        return migrationPending;
    }

    public void markMigrationComplete() {
        migrationPending = false;
        System.out.println("DatabaseMigrationService: Migration marked as complete.");
    }

    public void markMigrationNotNecessary() {
        migrationPending = false;
        System.out.println("DatabaseMigrationService: Migration marked as not necessary.");
    }
}
