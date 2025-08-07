#!/bin/bash

# HMIS Wiki Sync Script
# Automatically syncs documentation from main project to GitHub wiki

set -e

MAIN_REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
WIKI_DIR="$MAIN_REPO_DIR/wiki-temp"
DOCS_DIR="$MAIN_REPO_DIR/docs/wiki"

echo "🔄 Starting wiki sync process..."

# Clean up previous temporary directory
if [ -d "$WIKI_DIR" ]; then
    rm -rf "$WIKI_DIR"
fi

# Clone wiki repository
echo "📥 Cloning wiki repository..."
git clone https://github.com/hmislk/hmis.wiki.git "$WIKI_DIR"

# Copy documentation files if docs/wiki directory exists
if [ -d "$DOCS_DIR" ]; then
    echo "📋 Copying documentation files..."
    cp -r "$DOCS_DIR"/* "$WIKI_DIR"/
else
    echo "⚠️  No docs/wiki directory found, skipping file copy"
fi

# Navigate to wiki directory
cd "$WIKI_DIR"

# Check if there are any changes
if [ -n "$(git status --porcelain)" ]; then
    echo "📝 Changes detected, committing..."
    
    # Get current commit info from main repository
    cd "$MAIN_REPO_DIR"
    CURRENT_COMMIT=$(git rev-parse HEAD)
    CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
    COMMIT_MESSAGE=$(git log -1 --pretty=%B)
    
    # Return to wiki directory and commit
    cd "$WIKI_DIR"
    git add .
    git commit -m "Auto-sync from main repository

Original commit: $CURRENT_COMMIT
Branch: $CURRENT_BRANCH
Message: $COMMIT_MESSAGE

🤖 Auto-synced by sync-wiki.sh script"

    echo "🚀 Pushing changes to wiki..."
    git push origin master
    
    echo "✅ Wiki sync completed successfully!"
else
    echo "ℹ️  No changes to sync"
fi

# Cleanup
echo "🧹 Cleaning up temporary files..."
rm -rf "$WIKI_DIR"

echo "🎉 Wiki sync process finished!"