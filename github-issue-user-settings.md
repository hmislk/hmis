# Implement Persistent User-Specific Settings (Column Visibility & UI Preferences)

## 📋 **Issue Summary**

Implement a mechanism to store and persist user-specific UI settings (like column visibility toggles) that survive user sessions. When a user toggles column visibility or other UI preferences, these settings should be automatically saved and restored on next login.

**Current Problem**: Users lose their UI customizations (column visibility, table preferences, etc.) every time they log out and log back in.

**Target Outcome**: Persistent, user-specific settings that enhance UX by remembering user preferences.

## 🔍 **Analysis of Existing Infrastructure**

### Current Configuration Systems
- ✅ **ConfigOption Entity**: General-purpose key-value configuration system
- ✅ **UserPreference Entity**: Predefined fields for specific module settings
- ✅ **OptionScope Enum**: `APPLICATION`, `INSTITUTION`, `DEPARTMENT`, `USER`
- ✅ **ConfigOptionController**: Already supports `OptionScope.USER` with `getOptionValueByKeyForWebUser()`

### Why ConfigOption System is Preferred
| Factor | ConfigOption | UserPreference |
|--------|-------------|----------------|
| **Flexibility** | ✅ Dynamic key-value pairs | ❌ Fixed predefined fields |
| **Type Safety** | ✅ OptionValueType enum | ❌ Mixed field types |
| **Scalability** | ✅ Easy to add new settings | ❌ Requires schema changes |
| **Existing Support** | ✅ USER scope ready | ⚠️ Mostly template settings |
| **JSON Support** | ✅ LONG_TEXT for complex data | ❌ Limited |

## 🎯 **Proposed Solution: Extend ConfigOption System**

### Database Schema (No Changes Required!)
The existing `ConfigOption` table already supports everything we need:

```sql
-- Existing ConfigOption table structure
CREATE TABLE ConfigOption (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    optionKey VARCHAR(255),        -- Setting identifier
    scope ENUM('APPLICATION', 'INSTITUTION', 'DEPARTMENT', 'USER'),
    valueType ENUM('LONG_TEXT', 'SHORT_TEXT', 'BOOLEAN', ...),
    optionValue LONGTEXT,          -- JSON or simple value
    institution_id BIGINT,         -- Nullable FK
    department_id BIGINT,          -- Nullable FK
    webUser_id BIGINT,             -- Nullable FK
    -- ... audit fields
);
```

### User Setting Key Convention
```java
// Pattern: "ui.{page/component}.{setting_type}.{specific_setting}"
"ui.pharmacy_grn_return_request.columns.visibility"
"ui.pharmacy_grn_return_request.table.page_size"
"ui.laboratory_reports.table.sort_preferences"
"ui.dashboard.widgets.layout"
```

### Hierarchical Configuration Priority
```
USER + DEPARTMENT → DEPARTMENT → INSTITUTION → APPLICATION → DEFAULT
```

## 💻 **Implementation Plan**

> **📝 IMPORTANT**: Before starting development, create a detailed implementation plan as an MD document (`user-settings-implementation-plan.md`) and track progress with status markers:
> - 📋 **TODO** - Not started
> - 🔄 **DOING** - In progress
> - ✅ **DONE** - Completed
> - ❌ **BLOCKED** - Needs attention
>
> This allows any developer to pick up from where previous work stopped if needed.

### Phase 1: Core Infrastructure 📋 TODO

#### 1.1 Create UserSettingsController 📋 TODO
```java
@Named
@SessionScoped  // Important: Session-scoped for performance
public class UserSettingsController {

    // Core setting methods
    public String getUserSetting(String key, String defaultValue);
    public Boolean getUserBooleanSetting(String key, boolean defaultValue);
    public <T> T getUserJsonSetting(String key, Class<T> clazz, T defaultValue);

    // Save methods
    public void saveUserSetting(String key, String value);
    public void saveUserBooleanSetting(String key, boolean value);
    public void saveUserJsonSetting(String key, Object value);

    // Column visibility specific
    public ColumnVisibility getColumnVisibility(String pageId);
    public void saveColumnVisibility(String pageId, ColumnVisibility settings);
}
```

#### 1.2 Create Column Visibility Data Structure 📋 TODO
```java
public class ColumnVisibility {
    private Map<String, Boolean> columnVisible;      // column_id -> visible
    private Map<String, Integer> columnOrder;        // column_id -> order
    private Integer pageSize;                        // rows per page
    private String sortField;                        // default sort
    private String sortOrder;                        // ASC/DESC
}
```

#### 1.3 Login Integration - Load User Settings 📋 TODO
**🔥 CRITICAL PERFORMANCE REQUIREMENT**:
- Load ONLY the current user's settings at login (not all users like ConfigOptions)
- Cache in session scope for performance
- Lazy load specific settings on demand

```java
// In SessionController or dedicated service
@PostConstruct
public void loadUserSettingsAtLogin() {
    if (loggedUser != null) {
        // Load only current user's settings - NOT all users!
        userSettingsController.loadSettingsForUser(loggedUser, loggedDepartment);
    }
}
```

### Phase 2: Frontend Integration 📋 TODO

#### 2.1 JavaScript Helpers 📋 TODO
```javascript
// Column toggle persistence
function saveColumnToggle(pageId, columnId, visible) {
    // AJAX call to UserSettingsController
}

// Auto-save on page unload
window.addEventListener('beforeunload', function() {
    saveCurrentTablePreferences();
});
```

#### 2.2 Enhanced PrimeFaces Integration 📋 TODO
```xhtml
<p:dataTable
    value="#{searchController.bills}"
    columns="#{userSettingsController.getVisibleColumns('pharmacy_grn_return_request')}"
    rows="#{userSettingsController.getPageSize('pharmacy_grn_return_request', 10)}">

    <p:ajax event="toggleColumn"
            listener="#{userSettingsController.onColumnToggle}"
            process="@this"/>
</p:dataTable>
```

#### 2.3 Pilot Implementation 📋 TODO
Start with `pharmacy_grn_return_request.xhtml` as the pilot page:
- Column visibility toggles
- Page size preferences
- Sort preferences

### Phase 3: Settings Management UI 📋 TODO

#### 3.1 User Preferences Page 📋 TODO
- Manage all user-specific settings
- Reset to defaults option
- Export/import settings

#### 3.2 Admin Interface 📋 TODO
- Set department/institution defaults
- Bulk settings management
- Usage analytics

## 📊 **Usage Examples**

### Column Visibility Storage (JSON Format)
```json
{
    "columns": {
        "supplier": {"visible": true, "order": 1},
        "grnNo": {"visible": true, "order": 2},
        "invoiceNo": {"visible": false, "order": 3},
        "grnDate": {"visible": true, "order": 4}
    },
    "pageSize": 25,
    "sortField": "createdAt",
    "sortOrder": "DESC"
}
```

### Sample Setting Keys
```java
// User + Department specific settings
"ui.pharmacy_grn_return_request.columns.visibility"
"ui.laboratory_reports.table.preferences"
"ui.dashboard.widget.layout"

// Department defaults
"ui.pharmacy_grn_return_request.columns.default"

// Global application defaults
"ui.datatable.default.page_size"
```

## ⚠️ **Critical Performance Considerations**

1. **Session-Scoped Controller**: UserSettingsController MUST be `@SessionScoped` to avoid repeated database queries
2. **Selective Loading**: Load only current user's settings at login (not application-wide like ConfigOptions)
3. **Lazy Loading**: Load specific page settings only when needed
4. **Caching Strategy**: Cache frequently accessed settings in memory
5. **Async Saving**: Save settings asynchronously to avoid UI blocking

## ✅ **Acceptance Criteria**

- [ ] User column visibility toggles persist across sessions
- [ ] Settings load automatically at login (performance optimized)
- [ ] Hierarchical fallback: User → Department → Institution → Application
- [ ] Multiple pages support the same mechanism
- [ ] Admin interface for managing defaults
- [ ] No performance degradation on login
- [ ] Settings export/import functionality

## 🔧 **Technical Requirements**

- **Framework**: JSF 2.3, CDI, JPA 2.1
- **Database**: Use existing ConfigOption table
- **Caching**: Session-scoped beans for performance
- **Frontend**: PrimeFaces components with AJAX
- **JSON Handling**: Built-in Java JSON processing

## 🎯 **Priority Pages for Implementation**

1. `pharmacy_grn_return_request.xhtml` (Pilot)
2. `pharmacy_grn_list_for_return.xhtml`
3. `pharmacy_direct_purchase_return_request.xhtml`
4. Laboratory report tables
5. Dashboard components

## 📝 **Development Guidelines**

1. **Create Implementation Plan**: Detailed MD document with task tracking before starting
2. **Performance First**: Always consider login performance impact
3. **Progressive Enhancement**: Start with one page, expand gradually
4. **Error Handling**: Graceful fallback if settings load fails
5. **Testing**: Test with multiple users, departments, and institutions
6. **Documentation**: Update user guides with new functionality

## 🔗 **Related Issues**

- Column toggles in pharmacy returns pages
- User experience improvements
- Performance optimization initiatives

---

**Labels**: `enhancement`, `user-experience`, `performance`, `frontend`, `backend`
**Priority**: `Medium`
**Effort**: `Large` (multiple phases)
**Components**: `UI Framework`, `Session Management`, `Database`, `Frontend`