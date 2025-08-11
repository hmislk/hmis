# Wiki Automation System Guide

This document explains the automated wiki synchronization system for the HMIS project.

## Overview

The HMIS project now includes an automated wiki synchronization system that keeps the GitHub wiki in sync with documentation stored in the main repository. This provides a single source of truth for all documentation.

## How It Works

### 1. **Documentation Storage**
- All wiki documentation is stored in `docs/wiki/` directory in the main repository
- Files are organized by module (e.g., `Pharmacy/`, `LIMS/`, `OPD/`)
- Standard markdown format (`.md` files) is used

### 2. **Automatic Synchronization**
The system provides three ways to sync documentation:

#### A. **GitHub Actions (Automatic)**
- Triggers automatically when you push changes to `docs/wiki/` or `developer_docs/`
- Works on `development` and `master` branches
- Can be manually triggered from GitHub Actions tab

#### B. **Safe-Push Integration**
- Wiki sync is built into the existing `safe-push` scripts
- Automatically syncs when you use `scripts/safe-push.bat` or `scripts/safe-push.sh`
- Only syncs if `docs/wiki/` directory exists

#### C. **Manual Sync Scripts**
- Windows: `scripts/sync-wiki.bat`
- Linux/Mac: `scripts/sync-wiki.sh`
- Can be run manually anytime

## Directory Structure

```
docs/
└── wiki/
    ├── README.md                          # Main wiki documentation guide
    ├── Pharmacy-Issue.md                  # Main pharmacy issue documentation
    ├── Pharmacy-Issue-Configuration.md   # Configuration guide
    ├── Wiki-Automation-Guide.md          # This file
    └── Pharmacy/
        └── README.md                      # Pharmacy module overview
```

## Adding New Documentation

### Step 1: Create Documentation Files
1. Create your markdown file in the appropriate `docs/wiki/` subdirectory
2. Use kebab-case naming (e.g., `new-feature-guide.md`)
3. Include proper markdown headers and navigation links

### Step 2: Sync Methods

**Option A: Use Safe-Push (Recommended)**
```bash
# Windows
scripts\safe-push.bat

# Linux/Mac  
./scripts/safe-push.sh
```

**Option B: Manual Git Push (Auto-sync via GitHub Actions)**
```bash
git add docs/wiki/
git commit -m "Add new documentation"
git push
```

**Option C: Manual Sync Script**
```bash
# Windows
scripts\sync-wiki.bat

# Linux/Mac
./scripts/sync-wiki.sh
```

### Step 3: Verify Synchronization
1. Check GitHub Actions tab for sync status
2. Visit https://github.com/hmislk/hmis/wiki
3. Verify your documentation appears correctly

## Best Practices

### File Organization
```
docs/wiki/
├── Module-Name/
│   ├── README.md              # Module overview
│   ├── feature-guide.md       # Feature-specific guides
│   └── configuration.md       # Configuration instructions
├── images/                    # Shared images
└── Module-Main-Page.md        # Top-level module page
```

### Naming Conventions
- **Files**: Use kebab-case (e.g., `pharmacy-issue-configuration.md`)
- **Images**: Descriptive names (e.g., `pharmacy-issue-interface.png`)
- **Directories**: PascalCase for modules (e.g., `Pharmacy/`, `LIMS/`)

### Content Guidelines
1. **Headers**: Use proper markdown hierarchy (`#`, `##`, `###`)
2. **Links**: Use absolute GitHub wiki URLs for cross-references
3. **Images**: Store in `docs/wiki/images/` directory
4. **Code Examples**: Use proper code blocks with language specification

### Navigation Structure
- Each module should have a main page (e.g., `Pharmacy-Issue.md`)
- Link to sub-pages using full GitHub wiki URLs
- Update parent pages when adding new documentation

## Troubleshooting

### Common Issues

**1. Sync Not Working**
- Check GitHub Actions tab for error details
- Verify `docs/wiki/` directory exists
- Ensure files are properly committed

**2. Missing Links**
- Use full GitHub wiki URLs: `https://github.com/hmislk/hmis/wiki/Page-Name`
- Check file naming matches wiki page names

**3. Permission Errors**
- Ensure you have write access to the repository
- Check that GitHub token has appropriate permissions

**4. Formatting Issues**
- Verify markdown syntax is correct
- Use standard GitHub-flavored markdown
- Test locally with markdown preview

### Manual Verification
```bash
# Check sync status
cd wiki
git log --oneline -5

# View pending changes
git status

# Force sync (if needed)
cd ..
scripts/sync-wiki.bat  # Windows
# or
./scripts/sync-wiki.sh  # Linux/Mac
```

## Advanced Features

### GitHub Actions Workflow
- **File**: `.github/workflows/sync-wiki.yml`
- **Triggers**: Push to docs/wiki/, manual dispatch
- **Features**: 
  - Automatic navigation updates
  - Content validation
  - Detailed commit messages
  - Error handling and cleanup

### Manual Workflow Dispatch
1. Go to GitHub Actions tab
2. Select "Sync Wiki Documentation" workflow
3. Click "Run workflow"
4. Choose "Force sync all wiki content" if needed

### Developer Documentation Sync
- Files in `developer_docs/` automatically sync to wiki
- Converted to `Developer-*` pages in wiki
- Maintains original structure and formatting

## Monitoring and Maintenance

### Regular Tasks
1. **Weekly**: Review GitHub Actions logs for any sync failures
2. **Monthly**: Audit wiki content for accuracy and completeness
3. **Quarterly**: Update automation scripts if needed

### Performance Optimization
- Large image files should be optimized before adding
- Avoid frequent small commits to docs/wiki/ (batch changes)
- Use descriptive commit messages for better tracking

### Backup Strategy
- Main repository serves as primary backup
- GitHub wiki repository serves as secondary backup
- GitHub Actions maintain detailed sync logs

## Security Considerations

### Access Control
- Only repository collaborators can modify documentation
- Wiki sync uses GitHub's built-in authentication
- All changes are tracked via git history

### Content Validation
- Automatic link validation during sync
- Markdown format verification
- File size and type restrictions

## Support and Updates

### Getting Help
1. Check this guide for common issues
2. Review GitHub Actions logs for specific errors
3. Contact repository maintainers for advanced issues

### System Updates
- Automation scripts are versioned with the main repository
- GitHub Actions workflow updates deployed automatically
- Breaking changes will be documented in commit messages

---

**Related Documentation:**
- [HMIS Development Workflow](Developer-Workflow.md)
- [Pharmacy Issue Documentation](Pharmacy-Issue.md)
- [System Administration Guide](System-Administration.md)

**Last Updated:** Auto-generated on each sync