# Development Agent Guidelines

## High-Level Agent Roles

### Branch Management
- **Naming Convention**: Issue-based branches (`12875-implement-full-lab-workflow`)
- **PR Integration**: Auto-close with `Closes #issueNumber`
- **Status Tracking**: Project board updates

### UI/UX Development
- **Icon Management**: SVG-only, consistent sizing (80x80)
- **Theming**: Use `currentColor` for dynamic styling
- **Accessibility**: Proper tooltips and labeling

### Privilege System
- **Enum-based**: Add to `Privileges.java`
- **UI Integration**: Use `webUserController.hasPrivilege()`
- **Assignment**: Via User Privileges admin interface

### Configuration Management
- **Dynamic Values**: Use `configOptionApplicationController`
- **Color Schemes**: Centralized color configuration
- **Feature Toggles**: Boolean flags for UI elements

## Detailed Guidelines

For comprehensive implementation details, see:
- **Branch & PR Management**: [Branch workflow details](developer_docs/git/branch-management.md)
- **QA Deployment Guide**: [Bot-Friendly QA Deployment](developer_docs/deployment/qa-deployment-guide.md)
- **Icon & UI Standards**: [UI guidelines](developer_docs/ui/icon-management.md)
- **Privilege Implementation**: [Security patterns](developer_docs/security/privilege-system.md)
- **Configuration Patterns**: [Config usage](developer_docs/config/application-options.md)

## Key Principles
1. **Consistency**: Follow established patterns
2. **Security**: Validate privileges, sanitize inputs
3. **Maintainability**: Use centralized configuration
4. **Accessibility**: Ensure inclusive design
5. **JSF-only changes**: When modifying only XHTML/JSF files (no Java changes), compilation/testing is not required

---
These guidelines apply to the entire repository.