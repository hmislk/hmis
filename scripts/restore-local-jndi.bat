@echo off
:: Script to restore local JNDI names after GitHub push
:: This restores your local development configuration

set PERSISTENCE_FILE=src\main\resources\META-INF\persistence.xml

if not exist ".jndi-backup-main" (
    echo ❌ No JNDI backup found. Cannot restore.
    echo 💡 Manually edit %PERSISTENCE_FILE% to set your local JNDI names
    exit /b 1
)

if not exist ".jndi-backup-audit" (
    echo ❌ No JNDI backup found. Cannot restore.
    echo 💡 Manually edit %PERSISTENCE_FILE% to set your local JNDI names
    exit /b 1
)

:: Read backed up JNDI names
set /p MAIN_JNDI=<.jndi-backup-main
set /p AUDIT_JNDI=<.jndi-backup-audit

echo 🔄 Restoring local JNDI configuration:
echo    Main: %MAIN_JNDI%
echo    Audit: %AUDIT_JNDI%

:: Restore original JNDI names using PowerShell
powershell -Command "(Get-Content '%PERSISTENCE_FILE%') -replace '<jta-data-source>\\$\\{JDBC_DATASOURCE\\}</jta-data-source>', '<jta-data-source>%MAIN_JNDI%</jta-data-source>' | Set-Content '%PERSISTENCE_FILE%'"
powershell -Command "(Get-Content '%PERSISTENCE_FILE%') -replace '<jta-data-source>\\$\\{JDBC_AUDIT_DATASOURCE\\}</jta-data-source>', '<jta-data-source>%AUDIT_JNDI%</jta-data-source>' | Set-Content '%PERSISTENCE_FILE%'"

:: Clean up backup files
del .jndi-backup-main .jndi-backup-audit

echo ✅ Local JNDI configuration restored
echo 💻 Ready for local development!