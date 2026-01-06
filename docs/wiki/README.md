# Wiki Documentation Structure

This directory contains documentation that gets automatically synced to the GitHub wiki.

## Structure
- `Pharmacy/` - Pharmacy module documentation
- `LIMS/` - Laboratory module documentation  
- `OPD/` - Outpatient department documentation
- `Inward/` - Inpatient module documentation
- `Administration/` - System administration guides

## Workflow
1. Edit files in this directory as part of your normal development
2. Commit changes to the main repository
3. GitHub Actions automatically syncs changes to the wiki
4. Changes appear on https://github.com/hmislk/hmis/wiki

## File Naming Convention
- Use kebab-case for filenames (e.g., `pharmacy-issue-configuration.md`)
- Files will be converted to proper wiki page names automatically
- Use `.md` extension for all documentation files