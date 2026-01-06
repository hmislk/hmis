# Pharmacy Issue Configuration

This guide explains how administrators can configure the Pharmacy Issue module to control rate displays and system behavior.

## Overview

The Pharmacy Issue module uses configuration options to control:
- Which rates are displayed during disposal
- Which financial values appear in disposal summaries
- How rates are calculated and applied
- User interface customization options

## Configuration Categories

### 1. Display Configuration Options

These configuration keys control what financial information is visible to users during dispensing:

| Configuration Key | Default Value | Description |
|------------------|---------------|-------------|
| `Pharmacy Dispotals - Display Purchase Rate` | `true` | Shows/hides purchase rate in item selection and disposal items |
| `Pharmacy Dispotals - Display Cost Rate` | `true` | Shows/hides cost rate in item selection and disposal items |
| `Pharmacy Dispotals - Display Retail Rate` | `true` | Shows/hides retail rate in item selection and disposal items |
| `Pharmacy Dispotals - Display Purchase Value` | `true` | Shows/hides purchase value calculations |
| `Pharmacy Dispotals - Display Retail Value` | `true` | Shows/hides retail value calculations |
| `Pharmacy Dispotals - Display Cost Value` | `true` | Shows/hides cost value calculations |
| `Pharmacy Dispotals - Display Issue Value` | `true` | Shows/hides issue value (net disposal amount) |

### 2. Rate Calculation Configuration

These options control how disposal rates are determined:

| Configuration Key | Default Value | Description |
|------------------|---------------|-------------|
| `Pharmacy Disposal is by Purchase Rate` | `true` | Use purchase rate as the basis for disposal pricing |
| `Pharmacy Disposal is by Cost Rate` | `false` | Use cost rate as the basis for disposal pricing |
| `Pharmacy Disposal is by Retail Rate` | `false` | Use retail rate as the basis for disposal pricing |

## How to Access Configuration Settings

### Navigation Path

**Path:** Administration → Manage Institutions → Configuration Options

### Steps to Configure

1. **Navigate to Configuration**
   - Login as a system administrator
   - Go to "Administration" menu
   - Select "Manage Institutions"
   - Click on "Configuration Options"

2. **Find Configuration Keys**
   - Use the search function to find "Pharmacy Dispotals" keys
   - Or browse through the configuration categories
   - Look for keys starting with "Pharmacy Dispotals" or "Pharmacy Issue"

3. **Update Settings**
   - Click on the configuration key you want to modify
   - Change the value (typically true/false for display options)
   - Save the changes

## Configuration Scenarios

### Scenario 1: Hide Purchase Information
**Use Case**: Pharmacy wants to hide purchase-related information from disposal staff

**Configuration:**
```
Pharmacy Dispotals - Display Purchase Rate = false
Pharmacy Dispotals - Display Purchase Value = false
```

**Result**: Purchase rates and values will not be visible during disposal

### Scenario 2: Cost-Based Disposal
**Use Case**: Hospital pharmacy wants to record disposal at cost rate instead of retail rate

**Configuration:**
```
Pharmacy Disposal is by Purchase Rate = false
Pharmacy Disposal is by Cost Rate = true
Pharmacy Disposal is by Retail Rate = false
```

**Result**: Disposal will be recorded at cost rate

### Scenario 3: Minimal Display
**Use Case**: Simple disposal interface with only essential information

**Configuration:**
```
Pharmacy Dispotals - Display Purchase Rate = false
Pharmacy Dispotals - Display Cost Rate = false
Pharmacy Dispotals - Display Retail Rate = false
Pharmacy Dispotals - Display Purchase Value = false
Pharmacy Dispotals - Display Retail Value = false
Pharmacy Dispotals - Display Cost Value = false
Pharmacy Dispotals - Display Issue Value = true
```

**Result**: Only issue value (net disposal amount) is shown

### Scenario 4: Complete Financial View
**Use Case**: Teaching hospital where staff need to see all financial aspects

**Configuration:**
```
All display options = true
```

**Result**: All rates and values are visible for educational/analysis purposes

## Impact of Configuration Changes

### User Interface Changes
- **Item Selection Panel**: Rate fields appear/disappear based on display settings
- **Bill Items Table**: Columns show/hide according to configuration
- **Bill Details Panel**: Financial summaries adjust to show only configured values

### Calculation Behavior
- **Issue Rate Determination**: Controlled by the rate calculation configuration
- **Financial Reporting**: Reports respect display configuration settings
- **Print Documents**: Receipts and reports show only configured information


## Testing Configuration Changes

### Validation Steps
1. **Login as Pharmacy User**: Verify the interface shows/hides expected elements
2. **Add Items to Disposal**: Confirm calculations work correctly
3. **Check Disposal Totals**: Ensure summaries reflect configuration
4. **Print Test Receipt**: Verify printed documents are correct

### User Training
1. Train pharmacy staff on new interface
2. Get feedback on usability
3. Adjust configuration based on user feedback

## Troubleshooting Configuration Issues

### Common Problems

**1. Changes Not Visible**
- **Cause**: Browser cache or application cache
- **Solution**: Clear browser cache and restart application server

**2. Calculation Errors**
- **Cause**: Conflicting rate configuration settings
- **Solution**: Ensure only one disposal rate calculation method is set to true

**3. Display Issues**
- **Cause**: Misspelled configuration keys
- **Solution**: Verify exact spelling and case sensitivity



## Best Practices

### 1. Configuration Management
- **Document Changes**: Keep a log of configuration modifications
- **Version Control**: Track configuration changes with dates and reasons
- **Backup Settings**: Export configuration before making changes

### 2. User Training
- **Notify Users**: Inform users about interface changes
- **Provide Training**: Update training materials for new configurations
- **Support Documentation**: Update user guides and help documentation

### 3. User Feedback
- **Regular Review**: Collect feedback from pharmacy staff on interface usability
- **Monitor Usage**: Observe how the configuration changes affect daily workflows

---

**Related Topics:**
- [Pharmacy Issue](https://github.com/hmislk/hmis/wiki/Pharmacy-Issue)
- [Application Options](https://github.com/hmislk/hmis/wiki/Application-Options)
- [System Administration](https://github.com/hmislk/hmis/wiki/System-Administration)

[Back to Pharmacy](https://github.com/hmislk/hmis/wiki/Pharmacy)