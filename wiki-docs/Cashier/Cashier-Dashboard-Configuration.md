# Cashier Dashboard - Configuration Guide

## Overview
The Cashier Dashboard serves as the central navigation hub for all cashier operations. It contains accordion-style tabs organizing functionality into Shift Management, Float Management, Income & Expense Management, Handover Management, Cash Book, and Reports. This page has **22 configuration options** that control which features are visible and how they behave.

## Accessing Configuration

1. Navigate to the Cashier Dashboard: `/cashier/index.xhtml`
2. Click the "Config" button at the top of the navigation panel (visible only to administrators)
3. Modify configuration options as needed
4. Save changes - they take effect immediately

## Dashboard Structure

### Main Navigation Tabs
1. **Shift Management** - Start/end shifts, drawer management, shift tracking
2. **Float Management** - Fund transfers between users
3. **Income & Expense Management** - Additional income and expense tracking
4. **Handover Management** - Shift handover operations
5. **Cash Book** - Cash book entry management
6. **Reports** - Various financial and operational reports

## Configuration Options

### Shift Management Tab Configurations

**Key**: `Legacy Handover is enabled`
**Default**: Varies by installation
**Description**: Shows legacy handover options in the Shift Management tab
**Affects**:
- "End Shift - OLD" button
- "Handover (OLD)" button
- "Handover Shift (OLD)" button
**Use Case**: Enable for organizations transitioning from old to new handover processes

**Key**: `Recording Shift End Cash is Required Before Viewing Shift Reports`
**Default**: Varies
**Description**: Shows "Record Shift End Cash in Hand" button for cash recording before report access
**Affects**: "Record Shift End Cash in Hand" button visibility
**Impact**: Enforces cash reconciliation before allowing report access

**Key**: `Current Shift Handover is enabled`
**Default**: Varies
**Description**: Shows "Handover Current Shift" button for current shift handover functionality
**Affects**: "Handover Current Shift" button visibility
**Use Case**: Enable for real-time shift handover workflows

**Key**: `Period Handover is enabled`
**Default**: Varies
**Description**: Shows "Handover By Period" button for period-based handover functionality
**Affects**: "Handover By Period" button visibility
**Use Case**: Enable for flexible period-based handover operations

**Key**: `Completed Shift Handover is enabled`
**Default**: Varies
**Description**: Shows "My Shifts" button to view completed shift handovers
**Affects**: "My Shifts" button visibility
**Use Case**: Enable for shift history tracking and auditing

**Key**: `Shift Shortage Bills are enabled`
**Default**: Varies
**Description**: Shows shift shortage tracking buttons
**Affects**:
- "Record Shift Shortage" button
- "Shift Shortage Bill Search" button
**Use Case**: Enable for environments requiring shortage tracking and reporting

**Key**: `Shift Excess Bills are enabled`
**Default**: Varies
**Description**: Shows "Record Shift Excess" button for tracking cash overages
**Affects**: "Record Shift Excess" button visibility
**Use Case**: Enable for complete cash variance tracking (both shortages and overages)

### Reports Tab Configurations

**Key**: `Lab Daily Summary Report - Legacy Method`
**Default**: Varies
**Description**: Shows daily lab summary by department report button using legacy reporting method
**Affects**: Laboratory daily summary report button visibility in Reports tab
**Use Case**: Enable for organizations still using legacy lab reporting systems

**Key**: `Daily Lab Summmary By Department Report Menu Name`
**Default**: "Daily Lab Summmary By Department"
**Description**: Customizes the display text for the daily lab summary report button
**Affects**: Button text in Reports tab
**Use Case**: Customize button text to match organizational terminology

### Core Workflow Configurations

**Key**: `Allow to Denomination for shift Starting Process`
**Default**: Disabled
**Description**: Allows denomination entry during shift start process
**Affects**: Shift start workflow from Shift Management tab
**Impact**: When enabled, users can enter detailed cash denominations when starting shifts

**Key**: `Restrict Float Transfer Until Shift Start`
**Default**: Disabled
**Description**: Restricts fund transfer operations until shift is started
**Affects**: Float Management tab functionality
**Impact**: Users must start shift before accessing float transfer features

**Key**: `Restrict Handover Until Shift Start`
**Default**: Disabled
**Description**: Restricts handover operations until shift is started
**Affects**: Handover Management tab functionality
**Impact**: Users must start shift before performing handover operations

**Key**: `Enable Drawer Manegment`
**Default**: Enabled
**Description**: Activates drawer management features for cash handling
**Affects**: All cash-related operations across Float, Income/Expense, and Handover Management tabs
**Impact**: Controls cash availability validation and drawer balance tracking

**Key**: `Select All Cash During Handover`
**Default**: Enabled
**Description**: Automatically selects all cash payments during handover process
**Affects**: Handover Management tab workflow
**Impact**: Streamlines handover process by pre-selecting cash payments

**Key**: `Select All Payments for Handovers`
**Default**: Varies
**Description**: Automatically selects all payments when handover values are created
**Affects**: Handover Management tab workflow when values already exist
**Impact**: Reduces manual selection during handover preparation

**Key**: `Select all payments by default for Handing over of the shift.`
**Default**: Disabled
**Description**: Selects all payments by default during shift handover
**Affects**: Handover Management tab default selections
**Impact**: Speeds up handover preparation by pre-selecting all payments

**Key**: `Maximum Allowed Cash Difference for Handover`
**Default**: 1.0
**Description**: Sets maximum allowed difference between collected and handed over cash
**Affects**: Handover Management tab validation
**Impact**: Controls tolerance for cash counting discrepancies during handover

**Key**: `Should Select All Collections for Handover`
**Default**: Disabled
**Description**: Automatically selects all collected payments for handover
**Affects**: Handover Management tab collection selection
**Impact**: Ensures all collections are included in handover process

**Key**: `Patient Deposits are considered in handingover`
**Default**: Disabled
**Description**: Includes patient deposit transactions in handover process
**Affects**: Handover Management tab calculations and selections
**Impact**: When enabled, patient deposits become part of shift handover totals

### Shift Closure Validation Configurations

**Key**: `Must Receive All Fund Transfers Before Closing Shift`
**Default**: Disabled
**Description**: Prevents shift closure until all pending fund transfers are received
**Affects**: All shift closure operations across tabs
**Impact**: Users cannot end shift until they accept all incoming fund transfers

**Key**: `Must Wait Until Other User Accepts All Fund Transfers Before Closing Shift`
**Default**: Disabled
**Description**: Prevents shift closure until recipients accept all outgoing fund transfers
**Affects**: Shift closure validation
**Impact**: Users cannot end shift until all their outgoing transfers are accepted

**Key**: `Must Receive All Handovers Before Closing Shift`
**Default**: Disabled
**Description**: Prevents shift closure until all pending handovers are received
**Affects**: Shift Management tab end shift operations
**Impact**: Users cannot end shift until they accept all incoming handovers

**Key**: `Must Wait Until Other User Accepts All Handovers Before Closing Shift`
**Default**: Disabled
**Description**: Prevents shift closure until recipients accept all outgoing handovers
**Affects**: Shift closure validation
**Impact**: Users cannot end shift until all their outgoing handovers are accepted

## Required Privileges

### Admin
**Description**: Administrative access to system configuration
**Required For**: Viewing and accessing the Config button
**Effects**: Config button visibility at top of navigation panel

## Common Configuration Scenarios

### Basic Setup (Minimal Control)
```
Legacy Handover is enabled: Enabled
Enable Drawer Management: Enabled
All other restrictive options: Disabled
```
- Maximum flexibility for users
- Basic drawer management
- Access to both old and new features

### Controlled Environment (Medium Control)
```
Restrict Float Transfer Until Shift Start: Enabled
Restrict Handover Until Shift Start: Enabled
Enable Drawer Management: Enabled
Shift Shortage Bills are enabled: Enabled
Shift Excess Bills are enabled: Enabled
Recording Shift End Cash is Required: Enabled
```
- Requires active shifts for major operations
- Full cash variance tracking
- Cash recording requirements

### High-Security Setup (Maximum Control)
```
All shift closure validation options: Enabled
All restriction options: Enabled
All automatic selection options: Disabled (force manual review)
Maximum Allowed Cash Difference: 0.1 (tight tolerance)
Recording Shift End Cash is Required: Enabled
```
- Maximum accountability and control
- Zero tolerance for discrepancies
- Manual review required for all operations

### Streamlined Workflow (High Automation)
```
All automatic selection options: Enabled
Legacy options: Disabled
Current/Period handover options: Enabled
Restriction options: Disabled
```
- Maximum automation and speed
- Modern workflow only
- Flexible operation timing

## Tab-by-Tab Impact

### Shift Management Tab
- **7 configuration options** directly control button visibility
- Most critical for shift operations and workflow control
- Affects fundamental cashier operations

### Float Management Tab
- Inherits configurations from main fund transfer functionality
- Affected by restriction and drawer management settings
- Badge count shows pending transfers (configured separately)

### Income & Expense Management Tab
- Affected by drawer management settings
- Uses core workflow configurations
- Minimal direct configuration impact

### Handover Management Tab
- **11 configuration options** affect handover behavior
- Most heavily configured tab
- Critical for end-of-shift operations

### Cash Book Tab
- Minimal configuration impact
- Uses standard navigation patterns
- Affected by overall access controls

### Reports Tab
- **2 specific configuration options**
- Laboratory report customization
- Menu text customization capabilities

## Troubleshooting

### Missing Navigation Options
- **Cause**: Related configuration option is disabled
- **Solution**: Enable the specific configuration for desired functionality
- **Check**: Review tab-specific configurations in admin interface

### Cannot Perform Operations
- **Cause**: Restriction configurations prevent access
- **Examples**: "Must start shift first" errors
- **Solution**: Start shift or adjust restriction settings

### Automatic Selections Not Working
- **Cause**: Selection automation configurations disabled
- **Solution**: Enable relevant "Select All" configurations
- **Impact**: May require manual selection during operations

### Cash Handling Issues
- **Cause**: Drawer management or tolerance configurations
- **Solution**: Adjust drawer management settings or cash difference tolerances
- **Check**: Review drawer balance and cash handling configurations

## Performance Considerations

- Dashboard loads all tab content dynamically
- Badge counters perform real-time database queries
- Heavy configuration changes may require application restart
- Accordion state is maintained per user session

## Integration Points

- **All Cashier Pages**: Inherit configurations from dashboard
- **Shift Management**: Core to all cashier operations
- **Security System**: Privilege-based access control
- **Reporting System**: Report availability controlled by configurations
- **Audit System**: Configuration changes are logged

## Related Pages
- Fund Transfer Bill: `/cashier/fund_transfer_bill.xhtml`
- Fund Transfer Bills to Receive: `/cashier/fund_transfer_bills_for_me_to_receive.xhtml`
- All shift management pages
- All handover management pages
- Admin Configuration: `/admin/page_configuration_view.xhtml`