# UI Styling Guidelines for HMIS Project

## Button Styling

### ❌ Do NOT Use
- **Bootstrap button classes**: `btn btn-primary`, `btn btn-success`, `btn btn-warning`, etc.
- **Bootstrap utility classes for buttons**: These don't integrate well with PrimeFaces themes

### ✅ Use PrimeFaces Button Classes
```xhtml
<!-- Primary Actions -->
<p:commandButton class="ui-button-success" />

<!-- Secondary Actions -->  
<p:commandButton class="ui-button-info" />

<!-- Warning Actions -->
<p:commandButton class="ui-button-warning" />

<!-- Danger Actions -->
<p:commandButton class="ui-button-danger" />

<!-- Outlined Buttons -->
<p:commandButton class="ui-button-success ui-button-outlined" />
<p:commandButton class="ui-button-info ui-button-outlined" />

<!-- Disabled/Secondary -->
<p:commandButton class="ui-button-secondary" />
```

## Currency Display

### ❌ Avoid Currency-Specific Icons/Labels
```xhtml
<!-- Don't use these -->
<i class="fas fa-dollar-sign"></i>
<i class="fas fa-rupee-sign"></i>
<label>Requested Value (Rs)</label>
<label>Amount (USD)</label>
```

### ✅ Use Neutral Currency Icons/Labels
```xhtml
<!-- Use neutral money icons -->
<i class="fas fa-coins"></i>
<i class="fas fa-money-bill"></i>

<!-- Use neutral labels -->
<label>Requested Value</label>
<label>Total Amount</label>
<label>Net Value</label>
```

### Reason
- Different implementations may use different currencies (USD, Rs, EUR, etc.)
- Neutral icons and labels work for all currency types
- System-level currency configuration can handle display formatting

## Icon Usage

### Financial/Currency Icons (Neutral)
- `fa-coins` - For monetary values, amounts
- `fa-money-bill` - For payment amounts, totals
- `fa-chart-line` - For financial reports, trends

### Action Icons
- `fa-edit` - For edit actions
- `fa-check-circle` - For approval/finalize actions  
- `fa-check-double` - For completed/finalized status
- `fa-search` - For search actions
- `fa-ban` - For cancelled/blocked status

### Data Type Icons  
- `fa-calendar-alt` - For date fields
- `fa-user` - For person/user fields
- `fa-building` - For institution/company fields
- `fa-hospital` - For department/medical units
- `fa-hashtag` - For ID/reference numbers
- `fa-list-ol` - For count/quantity fields

## Bootstrap Integration

### ✅ Safe to Use (Layout & Utility)
- **Grid system**: `row`, `col-*`, `col-lg-*`, etc.
- **Spacing utilities**: `mb-3`, `mt-2`, `p-2`, etc.
- **Flexbox utilities**: `d-flex`, `justify-content-center`, `align-items-center`
- **Text utilities**: `text-center`, `text-end`, `fw-bold`, `text-success`

### ⚠️ Use with Caution (Component Styling)  
- **Form classes**: `form-control`, `form-label` - okay for basic styling
- **Card/Panel classes**: May conflict with PrimeFaces panel styling

### ❌ Avoid (Component-Specific)
- **Button classes**: Use PrimeFaces button classes instead
- **Alert classes**: Use PrimeFaces messages/growl components
- **Modal classes**: Use PrimeFaces dialog components

## Best Practices

1. **Consistency**: Use the same icon for the same data type across all pages
2. **Neutrality**: Avoid region/currency-specific icons and labels  
3. **PrimeFaces First**: Prefer PrimeFaces components and classes over Bootstrap alternatives
4. **Theme Compatibility**: Ensure styling works with different PrimeFaces themes
5. **Responsive Design**: Use Bootstrap grid system for responsive layouts

## Common Patterns

### Search Form Layout
```xhtml
<div class="row">
    <div class="col-lg-3 col-md-4">
        <p:panel header="Search Filters" styleClass="shadow-sm">
            <!-- Primary filters visible -->
            <p:commandButton class="ui-button-warning w-100" />
            
            <!-- Advanced filters in collapsible panel -->
            <p:panel header="Advanced Filters" toggleable="true" collapsed="true">
                <!-- Secondary filters -->
            </p:panel>
        </p:panel>
    </div>
    <div class="col-lg-9 col-md-8">
        <!-- Data table -->
    </div>
</div>
```

### Data Table with Actions
```xhtml
<p:dataTable styleClass="table-striped" sortMode="multiple">
    <p:column headerText="Actions" styleClass="text-center" exportable="false">
        <p:commandButton 
            class="ui-button-success"
            disabled="#{condition}"
            icon="fas fa-edit" />
    </p:column>
</p:dataTable>
```

---
This behavior should persist across all Claude Code sessions for this project.