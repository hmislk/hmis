package com.divudi.service;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;

/**
 * Singleton startup EJB that controls access to the migration page (mf.xhtml).
 *
 * On every deployment/restart, migrationPending is true, making the page
 * accessible to anyone. After someone visits mf.xhtml and marks the migration
 * as complete or not necessary, the page is restricted to Admin users only.
 *
 * @author Dr M H B Ariyaratne
 */
@Singleton
@Startup
public class DatabaseMigrationService {

    private volatile boolean migrationPending = true;

    @PostConstruct
    public void init() {
        System.out.println("DatabaseMigrationService: Migration page is open to all users until marked as complete or not necessary.");
    }

    public boolean isMigrationPending() {
        return migrationPending;
    }

    public void markMigrationComplete() {
        migrationPending = false;
        System.out.println("DatabaseMigrationService: Migration marked as complete. Page now restricted to Admin only.");
    }

    public void markMigrationNotNecessary() {
        migrationPending = false;
        System.out.println("DatabaseMigrationService: Migration marked as not necessary. Page now restricted to Admin only.");
    }
}
