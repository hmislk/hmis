# Fund Transfer Bill - Configuration Guide

## Overview
The Fund Transfer Bill page allows cashiers to transfer float funds between users. This page includes various configuration options to control transfer restrictions, bill numbering, and integration with shift management workflows.

## Accessing Configuration

1. Navigate to the Fund Transfer Bill page: `/cashier/fund_transfer_bill.xhtml`
2. Click the "Config" button in the page header (visible only to administrators)
3. Modify configuration options as needed
4. Save changes - they take effect immediately

## Configuration Options

### Bill Number Generation

**Key**: `Bill Number Suffix for FUND_TRANSFER_BILL`
**Default**: Empty (no suffix)
**Description**: Custom suffix to append to fund transfer bill numbers. This suffix is automatically applied by the system when generating bill numbers using the yearly department-based numbering strategy.

### Transfer Restrictions

**Key**: `Restrict Float Transfer Until Shift Start`
**Default**: Disabled
**Description**: When enabled, users cannot create fund transfers until they have started their shift. The system checks for an active shift start fund bill before allowing transfers.
**Effect**: If enabled and no active shift exists, users see "Start Your Shift First!" error message.

### Cash Management

**Key**: `Enable Drawer Management`
**Default**: Enabled
**Description**: When enabled, drawer management features control cash availability validation during fund transfers. The system checks if users have sufficient cash in their drawer for transfer operations.
**Effect**: When enabled, prevents transfers if insufficient cash is available in user's drawer.

### Shift Closure Validations

**Key**: `Must Receive All Fund Transfers Before Closing Shift`
**Default**: Disabled
**Description**: When enabled, users must receive all pending fund transfers before they can close their shift.
**Effect**: Prevents shift closure if user has pending incoming fund transfers.

**Key**: `Must Wait Until Other User Accepts All Fund Transfers Before Closing Shift`
**Default**: Disabled
**Description**: When enabled, users must wait for recipients to accept all outgoing fund transfers before closing their shift.
**Effect**: Prevents shift closure if user has unaccepted outgoing fund transfers.

### Navigation and Workflow Options

The following configurations control which options appear in the cashier navigation menu, affecting the overall fund transfer workflow:

**Key**: `Legacy Handover is enabled`
**Default**: Varies
**Description**: Shows legacy handover options in cashier navigation menu.

**Key**: `Current Shift Handover is enabled`
**Default**: Varies
**Description**: Shows current shift handover option in cashier navigation menu.

**Key**: `Period Handover is enabled`
**Default**: Varies
**Description**: Shows period-based handover option in cashier navigation menu.

**Key**: `Completed Shift Handover is enabled`
**Default**: Varies
**Description**: Shows completed shift handover options in cashier navigation menu.

**Key**: `Recording Shift End Cash is Required Before Viewing Shift Reports`
**Default**: Varies
**Description**: Requires recording shift end cash before viewing shift reports.

**Key**: `Shift Shortage Bills are enabled`
**Default**: Varies
**Description**: Allows recording and tracking of shift shortage bills in cashier operations.

**Key**: `Shift Excess Bills are enabled`
**Default**: Varies
**Description**: Allows recording and tracking of shift excess bills in cashier operations.

## Required Privileges

### Admin
**Description**: Administrative access to system configuration
**Required For**: Viewing and accessing the Config button
**Effects**: Config button visibility in page header

## Common Use Cases

### Basic Setup
1. Enable drawer management for cash validation
2. Set appropriate bill number suffix if needed
3. Configure shift closure validations based on workflow requirements

### Restricted Environment
1. Enable "Restrict Float Transfer Until Shift Start"
2. Enable shift closure validations for better control
3. Configure navigation options to match organization workflow

### High-Security Setup
1. Enable all closure validation options
2. Enable drawer management
3. Restrict transfers until shift start
4. Configure all navigation validation options

## Troubleshooting

### "Start Your Shift First!" Error
- **Cause**: "Restrict Float Transfer Until Shift Start" is enabled but user has no active shift
- **Solution**: Start a shift first, or disable the restriction

### Transfer Blocked Due to Cash
- **Cause**: "Enable Drawer Management" is enabled but insufficient cash in drawer
- **Solution**: Add funds to drawer or disable drawer management

### Cannot Close Shift
- **Cause**: Closure validation configurations prevent shift ending
- **Solution**: Complete all pending transfers and handovers, or adjust validation settings

## Impact on System Performance

- All configurations are checked in real-time during transfers
- Bill number generation occurs once per transfer
- Validation checks may add slight delay to transfer process
- No significant performance impact expected under normal usage

## Related Pages
- Cashier Index: `/cashier/index.xhtml`
- Shift Management: Various shift-related pages
- Admin Configuration: `/admin/page_configuration_view.xhtml`