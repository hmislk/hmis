# Wiki Publishing Guide

## Overview

This project maintains user documentation in the GitHub Wiki at https://github.com/hmislk/hmis/wiki. User documentation must be published to the wiki **immediately** after creation, not waiting for PR merge.

## Critical Rules

**🚨 IMMEDIATE PUBLICATION**: When creating user documentation, ALWAYS publish to GitHub Wiki immediately after creating the markdown file - don't wait for PR merge.

**📁 DIRECTORY**: Create documentation in `wiki-docs/` directory (e.g., `wiki-docs/Pharmacy/Feature-Name.md`)

**📝 GUIDELINES**: Follow the [Wiki Writing Guidelines](wiki-writing-guidelines.md)

## Publishing Workflow

### Step 1: Create Wiki Documentation

1. Create markdown files in `wiki-docs/` directory
   ```
   wiki-docs/
   ├── Pharmacy/
   │   ├── Feature-Name.md
   │   └── Other-Features.md
   ├── Lab/
   │   └── Lab-Features.md
   └── General/
       └── User-Guide.md
   ```

2. Follow [Wiki Writing Guidelines](wiki-writing-guidelines.md)
3. Write for end users (pharmacy staff, nurses, doctors, administrators)

### Step 2: Commit to Feature Branch

1. Add to git:
   ```bash
   git add wiki-docs/
   ```

2. Commit to current feature branch with proper message:
   ```bash
   git commit -m "Add [Feature Name] user documentation"
   ```

3. Push feature branch to GitHub:
   ```bash
   git push
   ```

### Step 3: Publish to GitHub Wiki (IMMEDIATE)

**Do this IMMEDIATELY after Step 2 - don't wait for PR merge**

#### Full Process with Sibling Directory Approach

```bash
# Verify current location (should be in project root)
if [ ! -d "wiki-docs" ]; then
    echo "❌ Error: Not in project root. Please cd to /home/buddhika/development/rh"
    exit 1
fi

# Check if sibling wiki directory exists
if [ ! -d "../hmis.wiki" ]; then
    echo "📥 Cloning wiki repository as sibling directory..."
    cd ..
    git clone https://github.com/hmislk/hmis.wiki.git
    cd rh
    echo "✅ Wiki repository cloned successfully"
else
    echo "📂 Using existing sibling wiki directory"
fi

# Verify wiki repository is up to date
echo "🔄 Updating wiki repository..."
cd ../hmis.wiki
git pull origin master || {
    echo "⚠️ Warning: Could not pull latest changes. Continuing with local version..."
}

# Copy documentation files to wiki
echo "📋 Copying documentation to wiki..."
# For entire module directories:
cp -r ../rh/wiki-docs/Pharmacy/* Pharmacy/ 2>/dev/null || echo "ℹ️  No Pharmacy docs to copy"
cp -r ../rh/wiki-docs/Lab/* Lab/ 2>/dev/null || echo "ℹ️  No Lab docs to copy"
cp -r ../rh/wiki-docs/General/* . 2>/dev/null || echo "ℹ️  No General docs to copy"

# Commit and push to wiki
echo "📝 Committing changes to wiki..."
git add .

# Check if there are any changes to commit
if git diff --staged --quiet; then
    echo "ℹ️  No changes to commit to wiki"
    cd ../rh
else
    git commit -m "Add [Feature Name] user documentation

[Brief description]

🤖 Generated with [Claude Code](https://claude.com/claude-code)

Co-Authored-By: Claude <noreply@anthropic.com>"

    echo "🚀 Pushing to wiki repository..."
    git push origin master || {
        echo "❌ Error: Could not push to wiki. Check your permissions and network."
        cd ../rh
        exit 1
    }

    echo "✅ Documentation published successfully to GitHub Wiki"
    cd ../rh
fi
```

#### Quick Command Template for Specific Files

When publishing specific file documentation:

```bash
# Verify location and setup
[ ! -d "wiki-docs" ] && echo "❌ Error: Not in project root" && exit 1
[ ! -d "../hmis.wiki" ] && echo "📥 Setting up wiki..." && cd .. && git clone https://github.com/hmislk/hmis.wiki.git && cd rh

# Navigate to wiki and copy specific file
cd ../hmis.wiki
git pull origin master 2>/dev/null || echo "⚠️ Using local wiki version"

# Copy specific documentation file
cp ../rh/wiki-docs/Pharmacy/[Your-File].md Pharmacy/ || {
    echo "❌ Error: Could not copy file. Check file path and permissions."
    cd ../rh
    exit 1
}

# Commit and push
git add Pharmacy/[Your-File].md
git commit -m "Add [Feature] documentation

🤖 Generated with [Claude Code](https://claude.com/claude-code)

Co-Authored-By: Claude <noreply@anthropic.com>"
git push origin master
cd ../rh
echo "✅ File published to wiki"
```

#### Smart Script Template

For automated workflow, save this as `publish-wiki.sh` in project root:

```bash
#!/bin/bash

# Smart wiki publishing script with path detection and error handling

set -e  # Exit on any error

# Function to print colored output
print_status() {
    case $1 in
        "info") echo -e "\033[34mℹ️  $2\033[0m" ;;
        "success") echo -e "\033[32m✅ $2\033[0m" ;;
        "warning") echo -e "\033[33m⚠️  $2\033[0m" ;;
        "error") echo -e "\033[31m❌ $2\033[0m" ;;
    esac
}

# Verify we're in the correct project directory
if [ ! -d "wiki-docs" ] || [ ! -f "CLAUDE.md" ]; then
    print_status "error" "Not in HMIS project root directory"
    echo "Please run this script from /path/to/rh/ directory"
    exit 1
fi

# Check for pending documentation
if [ ! "$(find wiki-docs -name "*.md" 2>/dev/null)" ]; then
    print_status "warning" "No documentation files found in wiki-docs/"
    exit 0
fi

# Setup wiki repository
WIKI_PATH="../hmis.wiki"
if [ ! -d "$WIKI_PATH" ]; then
    print_status "info" "Cloning wiki repository as sibling directory..."
    cd ..
    git clone https://github.com/hmislk/hmis.wiki.git || {
        print_status "error" "Failed to clone wiki repository"
        exit 1
    }
    cd rh
    print_status "success" "Wiki repository cloned successfully"
fi

# Navigate to wiki and update
print_status "info" "Updating wiki repository..."
cd "$WIKI_PATH"
git pull origin master &>/dev/null || print_status "warning" "Could not pull latest changes"

# Copy documentation with structure preservation
print_status "info" "Copying documentation to wiki..."
COPIED_FILES=0

# Copy files maintaining directory structure
find ../rh/wiki-docs -name "*.md" | while read -r file; do
    # Calculate relative path from wiki-docs
    rel_path="${file#../rh/wiki-docs/}"
    target_dir="$(dirname "$rel_path")"

    # Create target directory if it doesn't exist
    [ "$target_dir" != "." ] && mkdir -p "$target_dir"

    # Copy file
    cp "$file" "$rel_path"
    print_status "success" "Copied: $rel_path"
    COPIED_FILES=$((COPIED_FILES + 1))
done

# Commit changes if any
git add .
if git diff --staged --quiet; then
    print_status "info" "No changes to commit"
else
    # Use first line of most recent documentation as commit subject
    RECENT_DOC=$(find ../rh/wiki-docs -name "*.md" -type f -printf '%T@ %p\n' | sort -nr | head -1 | cut -d' ' -f2-)
    FEATURE_NAME=$(basename "$RECENT_DOC" .md | sed 's/-/ /g' | sed 's/\b\w/\U&/g')

    git commit -m "Add ${FEATURE_NAME} user documentation

Updated wiki documentation for recent feature additions.

🤖 Generated with [Claude Code](https://claude.com/claude-code)

Co-Authored-By: Claude <noreply@anthropic.com>"

    print_status "info" "Pushing to wiki repository..."
    git push origin master || {
        print_status "error" "Failed to push to wiki"
        cd ../rh
        exit 1
    }

    print_status "success" "Documentation published to GitHub Wiki"
fi

cd ../rh
print_status "success" "Wiki publishing completed"
```

## Verification

After publishing, verify your documentation is live:

- Wiki URL: `https://github.com/hmislk/hmis/wiki/[Page-Name]`
- File name becomes page name (dashes become spaces)
- Example: `Stock-Ledger-Report.md` → https://github.com/hmislk/hmis/wiki/Stock-Ledger-Report

## Best Practices

### File Naming
- Use descriptive kebab-case names: `Stock-Ledger-Report.md`
- Group related features in subdirectories by module
- Avoid spaces and special characters
- File name becomes the wiki page title

### When to Publish
- **Immediately** after creating user documentation
- When adding new user-facing features
- When existing feature workflows change
- When user interface is modified
- When error messages or user workflows are updated

### Content Quality
See [Wiki Writing Guidelines](wiki-writing-guidelines.md) for detailed content guidance.

## Troubleshooting

### Common Issues

#### Wiki Clone Fails
- **Cause**: Authentication or network issue
- **Solution**: Verify GitHub credentials and internet connection

#### Push Rejected
- **Cause**: Wiki repository out of sync
- **Solution**: Pull latest changes first:
  ```bash
  cd hmis.wiki
  git pull origin master
  # Then retry your changes
  ```

#### File Not Appearing
- **Cause**: Incorrect directory or file format
- **Solution**:
  - Verify file is in correct wiki subdirectory
  - Ensure file has `.md` extension
  - Check markdown formatting is valid

#### Permission Denied
- **Cause**: No write access to wiki repository
- **Solution**: Request repository access from maintainers

## Integration with Development

### Workflow Timeline
1. **During feature development**: Create documentation in `wiki-docs/`
2. **Before committing code**: Review and finalize documentation
3. **After committing to feature branch**: Publish to wiki immediately
4. **During PR review**: Documentation already available for testing
5. **After PR merge**: Documentation already published (no additional steps)

### Why Publish Before PR Merge?
- QA testers need documentation during testing
- Stakeholders can review features immediately
- Documentation is available during PR review
- Reduces post-merge tasks

---

## Quick Reference

**Create Documentation**: `wiki-docs/Pharmacy/YourFeature.md`
**Publish to Wiki**: See [Step 3 above](#step-3-publish-to-github-wiki-immediate)
**View Published**: https://github.com/hmislk/hmis/wiki
**Content Guidelines**: [Wiki Writing Guidelines](wiki-writing-guidelines.md)