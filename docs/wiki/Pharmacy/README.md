# Pharmacy Module Documentation

This directory contains comprehensive documentation for the Pharmacy module of the HMIS system.

## Available Documentation

### Core Functionality
- [Pharmacy Issue (Disposal)](../Pharmacy-Issue.md) - Main dispensing functionality
- [Pharmacy Issue Configuration](../Pharmacy-Issue-Configuration.md) - Administrator configuration guide
- [Bill Number Configuration Guide](Bill-Number-Configuration-Guide.md) - Complete guide for configuring yearly bill number generation

### Related Features
- Multiple Store Management
- Sale Management
- Ordering Process
- Goods Receipt
- Supplier Payment Management
- Transactions between units
- Inward Issue
- Staff Sale
- Summaries & Reports
- Adjustments
- Token Management
- Administration
- Analytics

### Configuration & Administration
- [Bill Number Configuration Guide](Bill-Number-Configuration-Guide.md) - Configure yearly bill number generation for pharmacy modules

## Documentation Standards

When adding new pharmacy documentation:

1. **File Naming**: Use kebab-case (e.g., `pharmacy-feature-name.md`)
2. **Structure**: Follow the existing template structure
3. **Links**: Use relative links to other documentation
4. **Images**: Store in `images/` subdirectory
5. **Testing**: Include testing procedures where applicable

## Auto-Sync Process

This documentation is automatically synchronized with the GitHub wiki when changes are committed to the main repository. The sync process:

1. Monitors changes in the `docs/wiki/` directory
2. Automatically updates the GitHub wiki
3. Maintains proper navigation and links
4. Preserves formatting and structure

Any changes made here will appear on https://github.com/hmislk/hmis/wiki automatically.