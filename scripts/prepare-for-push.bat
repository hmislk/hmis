@echo off
:: Script to prepare persistence.xml for GitHub push
:: Run this before pushing to ensure QA deployment compatibility

set PERSISTENCE_FILE=src\main\resources\META-INF\persistence.xml

if not exist "%PERSISTENCE_FILE%" (
    echo ❌ persistence.xml not found at %PERSISTENCE_FILE%
    exit /b 1
)

echo 📝 Detecting current JNDI configuration...

:: Extract JNDI names using findstr and for loop
for /f "tokens=2 delims=<>" %%i in ('findstr "<jta-data-source>" "%PERSISTENCE_FILE%"') do (
    if not defined MAIN_JNDI (
        set MAIN_JNDI=%%i
    ) else (
        set AUDIT_JNDI=%%i
    )
)

echo    Main: %MAIN_JNDI%
echo    Audit: %AUDIT_JNDI%

:: Store for restoration
echo %MAIN_JNDI% > .jndi-backup-main
echo %AUDIT_JNDI% > .jndi-backup-audit

:: Replace with environment variables using PowerShell for reliable text replacement
powershell -Command "(Get-Content '%PERSISTENCE_FILE%') -replace '<jta-data-source>%MAIN_JNDI%</jta-data-source>', '<jta-data-source>${JDBC_DATASOURCE}</jta-data-source>' | Set-Content '%PERSISTENCE_FILE%'"
powershell -Command "(Get-Content '%PERSISTENCE_FILE%') -replace '<jta-data-source>%AUDIT_JNDI%</jta-data-source>', '<jta-data-source>${JDBC_AUDIT_DATASOURCE}</jta-data-source>' | Set-Content '%PERSISTENCE_FILE%'"

echo ✅ Replaced JNDI names with environment variables
echo 🚀 Ready for GitHub push!
echo.
echo After pushing, run: scripts\restore-local-jndi.bat