@echo off
REM Auto-detect Maven location based on machine
REM Usage: detect-maven.bat [maven-args]
REM Example: detect-maven.bat test -Dtest="*BigDecimal*Test"

echo Detecting machine configuration...
echo Computer: %COMPUTERNAME%
echo User: %USERNAME%
echo.

REM Check if standard Maven is available
mvn --version >nul 2>&1
if %errorlevel% == 0 (
    echo Using standard Maven from PATH
    mvn %*
    exit /b %errorlevel%
)

REM Machine-specific Maven paths
if /i "%COMPUTERNAME%"=="CARECODE-LAP" (
    echo Using NetBeans bundled Maven for cclap machine
    "C:\Program Files\NetBeans-20\netbeans\java\maven\bin\mvn.cmd" %*
    exit /b %errorlevel%
)

if /i "%COMPUTERNAME%"=="DESKTOP-B7TA39C" (
    echo Using installed Maven for DESKTOP-B7TA39C machine
    "C:\Tools\apache-maven-3.9.8\bin\mvn.cmd" %*
    exit /b %errorlevel%
)

REM Add other machines here as they are documented
REM if /i "%COMPUTERNAME%"=="OTHER-MACHINE" (
REM     echo Using Maven for other machine
REM     "path\to\maven\bin\mvn.cmd" %*
REM     exit /b %errorlevel%
REM )

echo ERROR: Maven not found and no machine-specific configuration available
echo Please update detect-maven.bat with your Maven location
echo Current machine: %COMPUTERNAME%
echo Current user: %USERNAME%
exit /b 1