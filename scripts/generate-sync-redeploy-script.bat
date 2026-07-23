@echo off
setlocal enabledelayedexpansion
REM Detects this machine's Java/Payara/domain/port/app setup and generates a
REM personal %USERPROFILE%\sync-and-redeploy.bat tailored to it.
REM
REM See developer_docs\deployment\local-sync-redeploy-script-guide.md for the
REM full detection order and rationale.
REM
REM Usage: scripts\generate-sync-redeploy-script.bat [--yes] [--domain NAME]
REM
REM NOTE: Detection/generation logic exercised end-to-end on a real Windows
REM 10 machine (BuddhikaDesktop, see DEVELOPMENT_ENVIRONMENTS.md) - the
REM generator itself ran successfully and produced a correct config file and
REM runtime script. The generated runtime script's actual merge/build/deploy
REM steps were NOT executed end-to-end (would have redeployed over the
REM developer's live local Payara instance); see the PR description.

REM Capture the script's own directory BEFORE any `shift` call below - in
REM this cmd.exe, `shift` (even without /0) also shifts %0, so %~dp0 silently
REM changes to the wrong directory once argument parsing has shifted past
REM the first flag. Everything that needs the real script location must use
REM %SCRIPT_DIR% from here on, never %~dp0 directly.
set "SCRIPT_DIR=%~dp0"

set "YES=0"
set "FORCED_DOMAIN="
:parse_args
if "%~1"=="" goto args_done
if /i "%~1"=="--yes" set "YES=1" & shift & goto parse_args
if /i "%~1"=="--domain" set "FORCED_DOMAIN=%~2" & shift & shift & goto parse_args
echo Unknown argument: %~1
exit /b 1
:args_done

for %%I in ("%SCRIPT_DIR%..") do set "REPO_ROOT=%%~fI"
set "CONFIG_FILE=%USERPROFILE%\hmis-sync-redeploy.conf.bat"
set "OUTPUT_SCRIPT=%USERPROFILE%\sync-and-redeploy.bat"

echo === HMIS sync-and-redeploy generator ===
echo.
echo OS: Windows (%OS%)

REM --- pom.xml required JDK release ---
set "POM_RELEASE="
REM tokens=3 (not 2): splitting "    <release>11</release>" on <> gives
REM token1=leading whitespace, token2="release" (tag name), token3="11" (value).
for /f "tokens=3 delims=<>" %%A in ('findstr /c:"<release>" "%REPO_ROOT%\pom.xml"') do set "POM_RELEASE=%%A"
echo pom.xml requires JDK release: %POM_RELEASE%

REM --- Java detection ---
set "JAVA_HOME_DETECTED=%JAVA_HOME%"
if "%JAVA_HOME_DETECTED%"=="" (
    for /f "delims=" %%J in ('where java 2^>nul') do (
        if "!JAVA_HOME_DETECTED!"=="" (
            for %%K in ("%%~dpJ..") do set "JAVA_HOME_DETECTED=%%~fK"
        )
    )
)
echo JAVA_HOME detected: %JAVA_HOME_DETECTED%

REM --- Payara home detection ---
set "PAYARA_HOME_DETECTED=%PAYARA_HOME%"
if "%PAYARA_HOME_DETECTED%"=="" (
    for %%P in ("C:\Payara5" "D:\Payara" "%USERPROFILE%\Payara_Server") do (
        if exist "%%~P\bin\asadmin.bat" if "!PAYARA_HOME_DETECTED!"=="" set "PAYARA_HOME_DETECTED=%%~P"
    )
)
if "%PAYARA_HOME_DETECTED%"=="" (
    for %%R in ("C:\" "D:\" "%USERPROFILE%") do (
        if "!PAYARA_HOME_DETECTED!"=="" (
            for /f "delims=" %%F in ('where /r "%%~R" asadmin.bat 2^>nul') do (
                if "!PAYARA_HOME_DETECTED!"=="" (
                    for %%K in ("%%~dpF..") do set "PAYARA_HOME_DETECTED=%%~fK"
                )
            )
        )
    )
)
if "%PAYARA_HOME_DETECTED%"=="" (
    echo ERROR: Could not locate a Payara install. Set PAYARA_HOME and re-run.
    exit /b 1
)
echo Payara home: %PAYARA_HOME_DETECTED%
set "ASADMIN=%PAYARA_HOME_DETECTED%\bin\asadmin.bat"

REM --- Domain detection ---
set "DOMAINS_DIR=%PAYARA_HOME_DETECTED%\glassfish\domains"
set "DOMAIN_COUNT=0"
set "FIRST_DOMAIN="
for /f "delims=" %%D in ('dir /b /ad "%DOMAINS_DIR%" 2^>nul') do (
    set /a DOMAIN_COUNT+=1
    if "!FIRST_DOMAIN!"=="" set "FIRST_DOMAIN=%%D"
)
if "%DOMAIN_COUNT%"=="0" (
    echo ERROR: No domains found under %DOMAINS_DIR%
    exit /b 1
)

set "DOMAIN_NAME=%FORCED_DOMAIN%"
if "%DOMAIN_NAME%"=="" (
    if "%DOMAIN_COUNT%"=="1" (
        set "DOMAIN_NAME=%FIRST_DOMAIN%"
    ) else (
        echo Multiple domains found under %DOMAINS_DIR%:
        dir /b /ad "%DOMAINS_DIR%"
        set /p DOMAIN_NAME="Which domain should this script target? "
    )
)
echo Domain: %DOMAIN_NAME%

set "DOMAIN_XML=%PAYARA_HOME_DETECTED%\glassfish\domains\%DOMAIN_NAME%\config\domain.xml"
if not exist "%DOMAIN_XML%" (
    echo ERROR: domain.xml not found at %DOMAIN_XML%
    exit /b 1
)

REM Use the detect-domain-port.ps1 helper (not an inline -Command string) -
REM cmd/PowerShell quoting for a regex containing both " and $ characters is
REM too fragile to embed reliably inline.
for /f "delims=" %%A in ('powershell -NoProfile -File "%SCRIPT_DIR%detect-domain-port.ps1" -DomainXmlPath "%DOMAIN_XML%" -ListenerName "admin-listener" 2^>nul') do set "ADMIN_PORT=%%A"
for /f "delims=" %%A in ('powershell -NoProfile -File "%SCRIPT_DIR%detect-domain-port.ps1" -DomainXmlPath "%DOMAIN_XML%" -ListenerName "http-listener-1" 2^>nul') do set "HTTP_PORT=%%A"
if "%ADMIN_PORT%"=="" set "ADMIN_PORT=4848"
if "%HTTP_PORT%"=="" set "HTTP_PORT=8080"
echo Admin port: %ADMIN_PORT%
echo HTTP port: %HTTP_PORT%

REM --- Already-deployed app name/contextroot ---
REM asadmin list-applications prints "Nothing to list." when empty, or one
REM application name per line (first token) otherwise. Deliberately not
REM piped through findstr here: a FOR /F command string that both starts
REM with a quote (from "%ASADMIN%") and ends with one (from a quoted findstr
REM search term) gets misparsed by this cmd.exe - "The filename, directory
REM name, or volume label syntax is incorrect" - even though the string is
REM already single-quoted for FOR /F. Reading just the first line/token of
REM the raw output and checking it against "Nothing" avoids the pipe
REM entirely and sidesteps the quirk.
set "APP_NAME="
for /f "tokens=1" %%A in ('"%ASADMIN%" --port %ADMIN_PORT% list-applications 2^>nul') do (
    if "!APP_NAME!"=="" set "APP_NAME=%%A"
)
if /i "%APP_NAME%"=="Nothing" set "APP_NAME="
if "%APP_NAME%"=="" (
    echo No application currently deployed on domain '%DOMAIN_NAME%' - defaulting to name/contextroot 'rh'.
    set "APP_NAME=rh"
    set "CONTEXT_ROOT=rh"
) else (
    echo Detected already-deployed application: %APP_NAME%
    set "CONTEXT_ROOT=%APP_NAME%"
)
set /p APP_NAME_INPUT="Application name to deploy as [%APP_NAME%]: "
if not "%APP_NAME_INPUT%"=="" set "APP_NAME=%APP_NAME_INPUT%"
set /p CONTEXT_ROOT_INPUT="Context root [%CONTEXT_ROOT%]: "
if not "%CONTEXT_ROOT_INPUT%"=="" set "CONTEXT_ROOT=%CONTEXT_ROOT_INPUT%"

REM --- Local JNDI names ---
set "LOCAL_TEST_PERSISTENCE=%REPO_ROOT%\src\main\resources\META-INF\persistence_for_local_testing.xml"
set "MAIN_JNDI="
set "AUDIT_JNDI="
if exist "%LOCAL_TEST_PERSISTENCE%" (
    for /f "tokens=3 delims=<>" %%A in ('findstr /c:"<jta-data-source>" "%LOCAL_TEST_PERSISTENCE%"') do (
        if "!MAIN_JNDI!"=="" (set "MAIN_JNDI=%%A") else (set "AUDIT_JNDI=%%A")
    )
)
set /p MAIN_JNDI_INPUT="Local main JNDI name [%MAIN_JNDI%]: "
if not "%MAIN_JNDI_INPUT%"=="" set "MAIN_JNDI=%MAIN_JNDI_INPUT%"
set /p AUDIT_JNDI_INPUT="Local audit JNDI name [%AUDIT_JNDI%]: "
if not "%AUDIT_JNDI_INPUT%"=="" set "AUDIT_JNDI=%AUDIT_JNDI_INPUT%"

echo.
echo === Detected configuration ===
echo Repo path:      %REPO_ROOT%
echo JAVA_HOME:      %JAVA_HOME_DETECTED%
echo Payara home:    %PAYARA_HOME_DETECTED%
echo Domain:         %DOMAIN_NAME%
echo Admin port:     %ADMIN_PORT%
echo HTTP port:      %HTTP_PORT%
echo App name:       %APP_NAME%
echo Context root:   %CONTEXT_ROOT%
echo Main JNDI:      %MAIN_JNDI%
echo Audit JNDI:     %AUDIT_JNDI%
echo Config file:    %CONFIG_FILE%
echo Output script:  %OUTPUT_SCRIPT%
echo.

if not "%YES%"=="1" (
    set /p CONFIRM="Write this configuration and generate the script? [y/N] "
    if /i not "!CONFIRM!"=="y" if /i not "!CONFIRM!"=="yes" (
        echo Aborted, nothing written.
        exit /b 0
    )
)

REM --- Write config file ---
(
echo REM Generated by scripts\generate-sync-redeploy-script.bat - do not commit this file
echo set "REPO_ROOT=%REPO_ROOT%"
echo set "JAVA_HOME_DETECTED=%JAVA_HOME_DETECTED%"
echo set "PAYARA_HOME=%PAYARA_HOME_DETECTED%"
echo set "DOMAIN_NAME=%DOMAIN_NAME%"
echo set "ADMIN_PORT=%ADMIN_PORT%"
echo set "HTTP_PORT=%HTTP_PORT%"
echo set "APP_NAME=%APP_NAME%"
echo set "CONTEXT_ROOT=%CONTEXT_ROOT%"
echo set "MAIN_JNDI=%MAIN_JNDI%"
echo set "AUDIT_JNDI=%AUDIT_JNDI%"
) > "%CONFIG_FILE%"
echo Wrote %CONFIG_FILE%

REM --- Write runtime script ---
REM Copied verbatim from a static template (rather than built via nested
REM echo statements) so the generator never has to fight batch's escaping
REM rules for (), %%, and > inside a redirected block. The template reads
REM every machine-specific value from the config file at run time, so no
REM substitution is needed here.
copy /y "%SCRIPT_DIR%sync-and-redeploy.bat.template" "%OUTPUT_SCRIPT%" >nul
echo Wrote %OUTPUT_SCRIPT%

echo.
echo Run it with: %OUTPUT_SCRIPT%
exit /b 0
