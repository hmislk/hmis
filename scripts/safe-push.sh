#!/bin/bash

# One-command script for safe GitHub pushing
# Handles JNDI replacement automatically

echo "🔧 Preparing for GitHub push..."

# Step 1: Prepare persistence.xml
./scripts/prepare-for-push.sh
if [ $? -ne 0 ]; then
    echo "❌ Failed to prepare persistence.xml"
    exit 1
fi

# Step 2: Add, commit, and push
git add src/main/resources/META-INF/persistence.xml
git commit -m "chore: substitute JNDI names for push" --no-verify
git push "$@"

# Step 3: Restore local configuration
echo "🔄 Restoring local configuration..."
./scripts/restore-local-jndi.sh
if [ $? -ne 0 ]; then
    echo "❌ Failed to restore local configuration"
    echo "💡 You may need to manually restore your local JNDI names"
    exit 1
fi

echo "✅ Push complete and local config restored!"