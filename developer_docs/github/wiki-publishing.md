# Wiki Publishing Guide

## Overview

This project maintains user documentation in the GitHub Wiki at https://github.com/hmislk/hmis/wiki. The wiki is automatically synchronized from the repository's `docs/wiki/` directory using automated scripts.

## Workflow

### 1. Create Documentation
1. Create markdown files in `docs/wiki/` directory
2. Follow the [Wiki Writing Guidelines](../../CLAUDE.md#wiki-writing-guidelines) 
3. Focus on user instructions, not technical implementation details
4. Structure content for end users (pharmacy staff, nurses, doctors, administrators)

### 2. Organize Files
```
docs/wiki/
├── Pharmacy/
│   ├── Ward-Pharmacy-BHT-Substitute-Functionality.md
│   └── Other-Pharmacy-Features.md
├── Lab/
│   └── Lab-Features.md
└── General/
    └── User-Guide.md
```

### 3. Sync to GitHub Wiki

#### Automatic Sync (Recommended)
Use the provided sync scripts to publish documentation to GitHub Wiki:

**Windows:**
```batch
scripts\sync-wiki.bat
```

**Linux/Mac:**
```bash
./scripts/sync-wiki.sh
```

#### Manual Sync Process
If scripts don't work, follow these steps:

1. **Clone Wiki Repository**
   ```bash
   git clone https://github.com/hmislk/hmis.wiki.git wiki-temp
   ```

2. **Copy Documentation**
   ```bash
   cp -r docs/wiki/* wiki-temp/
   ```

3. **Commit and Push**
   ```bash
   cd wiki-temp
   git add .
   git commit -m "Update wiki documentation"
   git push origin HEAD
   ```

4. **Cleanup**
   ```bash
   cd ..
   rm -rf wiki-temp
   ```

## Sync Script Details

### What the Scripts Do
- Clone the GitHub Wiki repository temporarily
- Copy all files from `docs/wiki/` to wiki root
- Commit changes with automatic message including:
  - Original commit hash
  - Source branch name
  - Original commit message
  - Auto-sync attribution
- Push to GitHub Wiki
- Clean up temporary files

### Script Locations
- **Windows**: `scripts/sync-wiki.bat`
- **Unix/Linux**: `scripts/sync-wiki.sh`

### Prerequisites
- Git must be installed and configured
- GitHub authentication configured (SSH key or token)
- Write access to the HMIS repository wiki

## Best Practices

### File Naming
- Use descriptive kebab-case names: `Ward-Pharmacy-BHT-Substitute-Functionality.md`
- Group related features in subdirectories
- Avoid spaces and special characters

### Content Guidelines
- **User-focused**: Write for end users, not developers
- **Step-by-step**: Provide clear, numbered procedures
- **Screenshots**: Include UI screenshots when helpful
- **Error handling**: Document error messages and solutions
- **Navigation**: Start with how to access the feature
- **Best practices**: Include tips for effective use

### Structure Template
```markdown
# Feature Name

## Overview
Brief description of what the feature does

## When to Use
Specific scenarios when users need this feature

## How to Use
### Step 1: Access the Feature
### Step 2: Perform Action
### Step 3: Complete Process

## Understanding Messages
### Success Messages
### Warning Messages  
### Error Messages

## Best Practices
## Troubleshooting
## Configuration Options (Admin)
## FAQ
```

## Troubleshooting

### Sync Script Fails
1. **Authentication Issues**: Ensure GitHub credentials are configured
2. **Network Issues**: Check internet connection and GitHub accessibility
3. **Permission Issues**: Verify write access to wiki repository
4. **File Conflicts**: Check for wiki conflicts and resolve manually

### Wiki Not Updating
1. **Check Script Output**: Look for error messages in script execution
2. **Manual Verification**: Check GitHub Wiki directly for updates
3. **Cache Issues**: GitHub Wiki may take a few minutes to refresh
4. **File Format**: Ensure markdown files are properly formatted

### Common Errors
- **"Failed to clone wiki repository"**: Authentication or network issue
- **"Failed to push changes"**: Permission issue or conflicts
- **Files not copying**: Check `docs/wiki/` directory exists and has content

## Integration with Development

### When to Update Wiki
- New user-facing features added
- Existing feature workflows change
- User interface modifications
- Configuration options change
- Error messages or workflows updated

### During Pull Requests
1. Create wiki documentation in `docs/wiki/`
2. Include in pull request for review
3. After PR merge, run sync script to publish
4. Verify wiki updates appear at https://github.com/hmislk/hmis/wiki

### Automated Options
Consider setting up automated wiki sync via:
- GitHub Actions on push to main branch
- Post-merge hooks
- CI/CD pipeline integration

---

## Quick Reference

**Create Documentation**: `docs/wiki/YourFeature.md`  
**Sync to Wiki**: `./scripts/sync-wiki.sh`  
**View Published**: https://github.com/hmislk/hmis/wiki  
**Guidelines**: [Wiki Writing Guidelines](../../CLAUDE.md#wiki-writing-guidelines)