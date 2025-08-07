# Pharmacy Issue Configuration

This guide explains how administrators can configure the Pharmacy Issue module to control rate displays and system behavior.

## Overview

The Pharmacy Issue (Disposal) module uses configuration options to control:
- Which rates are displayed during disposal issue operations
- Which financial values appear in bill summaries and receipts
- How issue rates are determined for disposal transactions
- User interface customization for different organizational requirements

## Configuration Categories

### 1. Display Configuration Options

These configuration keys control what financial information is visible to users during disposal issue operations:

| Configuration Key | Default Value | Description |
|------------------|---------------|-------------|
| `Pharmacy Dispotals - Display Purchase Rate` | `true` | Shows/hides purchase rate in item selection autocomplete and bill items table |
| `Pharmacy Dispotals - Display Cost Rate` | `true` | Shows/hides cost rate in item selection autocomplete and bill items table |
| `Pharmacy Dispotals - Display Purchase Value` | `true` | Shows/hides purchase value calculations (item-level and bill totals) |
| `Pharmacy Dispotals - Display Retail Value` | `true` | Shows/hides retail value calculations (item-level and bill totals) |
| `Pharmacy Dispotals - Display Cost Value` | `true` | Shows/hides cost value calculations (item-level and bill totals) |
| `Pharmacy Dispotals - Display Issue Value` | `true` | Shows/hides issue value (actual disposal transfer amount) |

### 2. Rate Calculation Configuration

These options control how disposal issue rates are determined (only one should be true):

| Configuration Key | Default Value | Description |
|------------------|---------------|-------------|
| `Pharmacy Issue is by Purchase Rate` | `true` | Use purchase rate as the basis for disposal issue pricing |
| `Pharmacy Issue is by Cost Rate` | `false` | Use cost rate as the basis for disposal issue pricing |
| `Pharmacy Issue is by Retail Rate` | `false` | Use retail rate as the basis for disposal issue pricing |

### 3. Bill Number Generation Configuration

These options control how disposal issue bill numbers are generated:

| Configuration Key | Default Value | Description |
|------------------|---------------|-------------|
| `Bill Number Generation Strategy for Disposal Issue - Separate Bill Numbers for Logged Department` | `false` | Generate separate bill number series for each issuing department |
| `Bill Number Generation Strategy for Disposal Issue - Separate Bill Numbers for Issuing Department` | `false` | Generate separate bill number series for each receiving department |
| `Bill Number Generation Strategy for Disposal Issue - Separate Bill Numbers for Logged and Issuing Department Combination` | `false` | Generate separate bill number series for each from-to department combination |
| `Add the Institution Code to the Bill Number Generator` | `true` | Include institution code in generated bill numbers |
| `Disposal Issue can be done for the same department` | `false` | Allow disposal issues within the same department |

## How to Access Configuration Settings

### Method 1: Through Application Options

1. **Navigate to Administration**
   - Login as an administrator
   - Go to "System Administration" menu
   - Select "Application Options"

2. **Find Configuration Keys**
   - Use the search function to find "Pharmacy Dispotals" keys
   - Or browse through the configuration categories

3. **Update Settings**
   - Click on the configuration key you want to modify
   - Change the value (typically true/false for display options)
   - Save the changes

### Method 2: Direct Database Access

For advanced administrators with database access:

```sql
-- View current pharmacy dispensing configurations
SELECT * FROM config_option_application 
WHERE config_key LIKE '%Pharmacy Dispotals%' 
   OR config_key LIKE '%Pharmacy Issue%';

-- Update a configuration value
UPDATE config_option_application 
SET config_value = 'false' 
WHERE config_key = 'Pharmacy Dispotals - Display Purchase Rate';
```

## Configuration Scenarios

### Scenario 1: Hide Purchase Information
**Use Case**: Pharmacy wants to hide purchase-related information from dispensing staff

**Configuration:**
```
Pharmacy Dispotals - Display Purchase Rate = false
Pharmacy Dispotals - Display Purchase Value = false
```

**Result**: Purchase rates and values will not be visible during dispensing

### Scenario 2: Cost-Based Dispensing
**Use Case**: Hospital pharmacy wants to dispense at cost rate instead of retail rate

**Configuration:**
```
Pharmacy Issue is by Purchase Rate = false
Pharmacy Issue is by Cost Rate = true
Pharmacy Issue is by Retail Rate = false
```

**Result**: Dispensing will be charged at cost rate

### Scenario 3: Minimal Display
**Use Case**: Simple dispensing interface with only essential information

**Configuration:**
```
Pharmacy Dispotals - Display Purchase Rate = false
Pharmacy Dispotals - Display Cost Rate = false
Pharmacy Dispotals - Display Purchase Value = false
Pharmacy Dispotals - Display Retail Value = false
Pharmacy Dispotals - Display Cost Value = false
Pharmacy Dispotals - Display Transfer Value = true
```

**Result**: Only transfer value (net dispensing amount) is shown

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

### Performance Considerations
- Hiding complex calculations can improve page load times
- Fewer database queries when rates are not displayed
- Reduced network traffic for simplified interfaces

## Testing Configuration Changes

### 1. Test Environment Setup
Always test configuration changes in a development or test environment first:

1. Create a test pharmacy department
2. Set up test items with different rate structures
3. Create test users with appropriate privileges

### 2. Validation Steps
1. **Login as Dispensing User**: Verify the interface shows/hides expected elements
2. **Add Items to Bill**: Confirm calculations work correctly
3. **Check Bill Totals**: Ensure summaries reflect configuration
4. **Print Test Receipt**: Verify printed documents are correct
5. **Review Reports**: Check that reports respect configuration settings

### 3. User Acceptance Testing
1. Train key users on new interface
2. Get feedback on usability
3. Monitor for any calculation discrepancies
4. Adjust configuration based on user feedback

## Troubleshooting Configuration Issues

### Common Problems

**1. Changes Not Visible**
- **Cause**: Browser cache or application cache
- **Solution**: Clear browser cache and restart application server

**2. Calculation Errors**
- **Cause**: Conflicting rate configuration settings
- **Solution**: Ensure only one rate calculation method is set to true

**3. Display Issues**
- **Cause**: Misspelled configuration keys
- **Solution**: Verify exact spelling and case sensitivity

**4. Performance Problems**
- **Cause**: Too many rate calculations enabled
- **Solution**: Disable unnecessary display options

### Validation Queries

Use these database queries to verify configuration:

```sql
-- Check all pharmacy issue configurations
SELECT config_key, config_value, created_at, updated_at 
FROM config_option_application 
WHERE config_key LIKE '%Pharmacy%Issue%' 
   OR config_key LIKE '%Pharmacy%Dispotals%'
ORDER BY config_key;

-- Verify rate calculation settings
SELECT config_key, config_value 
FROM config_option_application 
WHERE config_key IN (
    'Pharmacy Issue is by Purchase Rate',
    'Pharmacy Issue is by Cost Rate', 
    'Pharmacy Issue is by Retail Rate'
);
```

## Best Practices

### 1. Configuration Management
- **Document Changes**: Keep a log of configuration modifications
- **Version Control**: Track configuration changes with dates and reasons
- **Backup Settings**: Export configuration before making changes

### 2. User Training
- **Notify Users**: Inform users about interface changes
- **Provide Training**: Update training materials for new configurations
- **Support Documentation**: Update user guides and help documentation

### 3. Monitoring
- **Performance Monitoring**: Watch for performance impacts after changes
- **Error Monitoring**: Check logs for configuration-related errors
- **User Feedback**: Regularly collect feedback on usability

### 4. Security Considerations
- **Access Control**: Limit configuration access to authorized administrators
- **Audit Trail**: Log all configuration changes with user information
- **Testing**: Always test in non-production environment first

---

**Related Topics:**
- [Pharmacy Issue](https://github.com/hmislk/hmis/wiki/Pharmacy-Issue)
- [Application Options](https://github.com/hmislk/hmis/wiki/Application-Options)
- [System Administration](https://github.com/hmislk/hmis/wiki/System-Administration)

[Back to Pharmacy](https://github.com/hmislk/hmis/wiki/Pharmacy)