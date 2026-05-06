@echo off
:: HMIS Wiki Sync Script (Windows)
:: Automatically syncs documentation from main project to GitHub wiki

setlocal enabledelayedexpansion

echo 🔄 Starting wiki sync process...

:: Get the directory where this script is located
set "SCRIPT_DIR=%~dp0"
set "MAIN_REPO_DIR=%SCRIPT_DIR%.."
set "WIKI_DIR=%MAIN_REPO_DIR%\wiki-temp"
set "DOCS_DIR=%MAIN_REPO_DIR%\docs\wiki"

:: Clean up previous temporary directory
if exist "%WIKI_DIR%" (
    echo 🧹 Cleaning up previous temporary directory...
    rmdir /s /q "%WIKI_DIR%"
)

:: Clone wiki repository
echo 📥 Cloning wiki repository...
git clone https://github.com/hmislk/hmis.wiki.git "%WIKI_DIR%"
if errorlevel 1 (
    echo ❌ Failed to clone wiki repository
    exit /b 1
)

:: Copy documentation files if docs/wiki directory exists
if exist "%DOCS_DIR%" (
    echo 📋 Copying documentation files...
    
    :: Copy all files from docs/wiki to wiki root
    xcopy "%DOCS_DIR%\*" "%WIKI_DIR%\" /E /H /C /I /Y
    if errorlevel 1 (
        echo ⚠️  Warning: Some files may not have been copied correctly
    )
) else (
    echo ⚠️  No docs/wiki directory found, skipping file copy
)

:: Navigate to wiki directory and check for changes
cd /d "%WIKI_DIR%"

:: Check if there are any changes
git status --porcelain > temp_status.txt
set /p STATUS_OUTPUT=<temp_status.txt
del temp_status.txt

if not "%STATUS_OUTPUT%"=="" (
    echo 📝 Changes detected, committing...
    
    :: Get current commit info from main repository
    cd /d "%MAIN_REPO_DIR%"
    for /f "tokens=*" %%a in ('git rev-parse HEAD') do set CURRENT_COMMIT=%%a
    for /f "tokens=*" %%a in ('git rev-parse --abbrev-ref HEAD') do set CURRENT_BRANCH=%%a
    for /f "tokens=*" %%a in ('git log -1 --pretty^=%%B') do set COMMIT_MESSAGE=%%a
    
    :: Return to wiki directory and commit
    cd /d "%WIKI_DIR%"
    git add .
    
    :: Create commit message
    echo Auto-sync from main repository > commit_message.tmp
    echo. >> commit_message.tmp
    echo Original commit: %CURRENT_COMMIT% >> commit_message.tmp
    echo Branch: %CURRENT_BRANCH% >> commit_message.tmp
    echo Message: %COMMIT_MESSAGE% >> commit_message.tmp
    echo. >> commit_message.tmp
    echo 🤖 Auto-synced by sync-wiki.bat script >> commit_message.tmp
    
    git commit -F commit_message.tmp
    del commit_message.tmp
    
    if errorlevel 1 (
        echo ❌ Failed to commit changes
        cd /d "%MAIN_REPO_DIR%"
        rmdir /s /q "%WIKI_DIR%"
        exit /b 1
    )
    
    echo 🚀 Pushing changes to wiki...
    git push origin master
    
    if errorlevel 1 (
        echo ❌ Failed to push changes
        cd /d "%MAIN_REPO_DIR%"
        rmdir /s /q "%WIKI_DIR%"
        exit /b 1
    )
    
    echo ✅ Wiki sync completed successfully!
) else (
    echo ℹ️  No changes to sync
)

:: Cleanup
echo 🧹 Cleaning up temporary files...
cd /d "%MAIN_REPO_DIR%"
rmdir /s /q "%WIKI_DIR%"

echo 🎉 Wiki sync process finished!
goto :eof