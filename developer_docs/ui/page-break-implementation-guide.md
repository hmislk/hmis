# Page Break Implementation Guide

## Overview
This guide explains how to implement page breaks between components when printing in the HMIS system, specifically for pharmacy token and bill printing.

## Problem Statement
When printing combined documents (like token + bill), JSF/PrimeFaces composite components may have conflicting CSS that prevents proper page breaks during printing.

## Solution Pattern

### Simple Page Break Implementation
For components that need to be printed on separate pages:

```xhtml
<!-- Token Section -->
<h:panelGroup id="tokenSection">
    <div style="page-break-after: always;">
        <!-- Token component content -->
        <phi:saleBillToken bill="#{controller.bill}"/>
    </div>
</h:panelGroup>

<!-- Bill Section -->
<h:panelGroup id="billSection">
    <div>
        <!-- Bill component content -->
        <phi:saleBill bill="#{controller.bill}"/>
    </div>
</h:panelGroup>
```

### Working Example
From `pharmacy_bill_retail_sale_for_cashier.xhtml`:

```xhtml
<h:panelGroup id="billAndTokenPrint">
    <!-- Token with page break -->
    <h:panelGroup id="tokenPrint">
        <div style="page-break-after: always;">
            <phi:saleBill_five_five_token bill="#{pharmacySaleController.printBill}"/>
        </div>
    </h:panelGroup>
    
    <!-- Bill on new page -->
    <h:panelGroup id="gpBillPreview">
        <phi:saleBill_for_Cashier bill="#{pharmacySaleController.printBill}"/>
    </h:panelGroup>
</h:panelGroup>
```

## Key Principles

### 1. Wrapper-Level Page Breaks
- Apply `page-break-after: always;` to wrapper divs, not directly to composite components
- This prevents conflicts with internal component CSS

### 2. Avoid Complex CSS
- Simple `page-break-after: always;` works better than complex CSS with `!important`
- Avoid mixing multiple page break properties unless necessary

### 3. Component-Specific Considerations
- Pharmacy components (saleBill, saleBillToken) have built-in page breaks
- Override these by applying page breaks at the wrapper level

## Testing Approach

### 1. Simple Content Test
Before implementing with complex components, test with simple content:

```xhtml
<div style="page-break-after: always;">Simple Token Content</div>
<div>Simple Bill Content</div>
```

### 2. Print Preview Verification
- Use browser print preview to verify page breaks
- Look for "2 pages" instead of "1 page" in print dialog

## Common Issues

### 1. CSS Conflicts
**Problem**: Composite components have built-in `page-break-after: always!important;`
**Solution**: Apply page breaks at wrapper level to override internal CSS

### 2. Missing Page Breaks
**Problem**: Complex CSS rules not working in print mode
**Solution**: Use simple `page-break-after: always;` without additional properties

### 3. Multiple Components
**Problem**: Multiple conditional components (PosPaper, FiveFivePaper, etc.)
**Solution**: Apply page break to each component's wrapper div

## Browser Compatibility
- Works with Chrome, Firefox, Edge print engines
- Uses standard CSS page break properties
- No JavaScript required

## Related Files
- `pharmacy_bill_retail_sale_for_cashier.xhtml` - Main implementation
- `resources/pharmacy/saleBill*.xhtml` - Individual bill components
- `resources/css/pharmacypos.css` - Print styles

## Best Practices
1. Always test with simple content first
2. Apply page breaks to wrapper elements
3. Use minimal CSS for page breaks
4. Test across different paper types and preferences
5. Verify print preview shows correct page count

## Troubleshooting
If page breaks don't work:
1. Check for CSS conflicts in component files
2. Verify wrapper div hierarchy
3. Test with simple content to isolate the issue
4. Check browser print preview vs actual printing