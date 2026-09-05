# Entity/DTO Dual Navigation Pattern

When a report or list page has both an Entity-based version (existing, slower) and a DTO-based version (optimized, per [DTO Implementation Guidelines](../dto/implementation-guidelines.md)), expose them as **two separate navigation entries** rather than a toggle inside one page.

## Navigation Structure
1. **Separate navigation entries** for Entity and DTO versions
   - Example: "Transfer Reports (Entity)" and "Transfer Reports (DTO)"
   - Double the navigation buttons, but clearer separation of concerns
2. **Each page has single purpose** - either Entity OR DTO, not both
3. **Simple Fill button** on each page - no switching within page

## Page Design Principles
1. **Entity page**: Contains only entity-based Fill button and entity-specific actions
2. **DTO page**: Contains only DTO-based Fill button and DTO-specific actions
3. **No cross-navigation buttons** within pages - navigation choice made at menu level
4. **Clear page headers** indicating which approach (Entity vs DTO)

## Recommended Naming Convention
- Original page: `feature_name.xhtml` (Entity-based for backward compatibility)
- DTO page: `feature_name_dto.xhtml` (DTO-based, optimized)
- Navigation labels: "Feature Name" and "Feature Name (DTO - Recommended)"

## Configuration
1. **DTO approach should be the default** where applicable
2. **Label DTO version clearly** in navigation to indicate it's the recommended approach
3. **Maintain entity version** for backward compatibility and business logic needs

## Implementation Example

**Navigation Configuration (pharmacy_analytics.xhtml):**
```xml
<!-- Entity Version - Traditional -->
<p:commandButton rendered="#{configOptionApplicationController.getBooleanValueByKey('Pharmacy Analytics - Show Transfer Issue by Bill')}"
                 value="Transfer Issue by Bill (Entity)"
                 action="#{reportsTransfer.navigateToTransferIssueByBill}"
                 ajax="false"
                 icon="fa fa-file-export"
                 class="w-100"/>

<!-- DTO Version - Recommended (defaults to enabled) -->
<p:commandButton rendered="#{configOptionApplicationController.getBooleanValueByKey('Pharmacy Analytics - Show Transfer Issue by Bill (DTO)', true)}"
                 value="Transfer Issue by Bill (DTO - Fast)"
                 action="/pharmacy/reports/disbursement_reports/pharmacy_report_transfer_issue_bill_dto?faces-redirect=true"
                 ajax="false"
                 icon="fa fa-rocket"
                 class="w-100 ui-button-success"
                 title="High-performance DTO-based report - Recommended"/>
```

**Configuration Key Pattern:**
- **Entity version**: `'Feature Name'` (existing configuration)
- **DTO version**: `'Feature Name (DTO)'` with `true` as default value
- This allows administrators to disable DTO versions if needed while defaulting to enabled

**Resulting Navigation Menu Structure:**
```
Pharmacy Analytics → Disbursement Reports
├── Transfer Issue by Bill (Entity)           → pharmacy_report_transfer_issue_bill.xhtml
└── Transfer Issue by Bill (DTO - Fast)       → pharmacy_report_transfer_issue_bill_dto.xhtml
```

**Entity Page Content:**
- Single "Fill" button → `fillDepartmentTransfersIssueByBillEntity()`
- Excel/Print buttons specific to entity data
- Uses `#{reportsTransfer.transferBills}` for data binding

**DTO Page Content:**
- Single "Fill" button → `fillDepartmentTransfersIssueByBillDto()`
- Excel/Print buttons specific to DTO data
- Uses `#{reportsTransfer.transferIssueDtos}` for data binding

**Controller Structure:**
```java
// Keep both properties for backward compatibility
private List<Bill> transferBills;              // For entity approach
private List<PharmacyTransferIssueDTO> transferIssueDtos; // For DTO approach

// Separate methods for each approach
public void fillDepartmentTransfersIssueByBillEntity() { ... }
public void fillDepartmentTransfersIssueByBillDto() { ... }

// Navigation control method
public boolean isTransferIssueDtoEnabled() { return true; }
```

**Benefits of Navigation-Level Selection:**
1. **Clear user choice** before entering report
2. **No switching confusion** within pages
3. **Easy configuration control** via controller methods
4. **Gradual migration path** - can disable DTO option if needed
5. **Performance awareness** - users can choose fast DTO version consciously
