@echo off
:: One-command script for safe GitHub pushing
:: Handles JNDI replacement automatically

echo 🔧 Preparing for GitHub push...

:: Step 1: Prepare persistence.xml
call scripts\prepare-for-push.bat
if errorlevel 1 (
    echo ❌ Failed to prepare persistence.xml
    exit /b 1
)

:: Step 2: Add, commit, and push
git add src\main\resources\META-INF\persistence.xml
git commit -m "chore: substitute JNDI names for push" --no-verify
git push %*

:: Step 3: Restore local configuration
echo 🔄 Restoring local configuration...
call scripts\restore-local-jndi.bat
if errorlevel 1 (
    echo ❌ Failed to restore local configuration
    echo 💡 You may need to manually restore your local JNDI names
    exit /b 1
)

echo ✅ Push complete and local config restored!