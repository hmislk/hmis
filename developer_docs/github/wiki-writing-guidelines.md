# Wiki Writing Guidelines

## Purpose and Audience

### Target Audience
- **Wiki is for end users**, not developers
- Pharmacy staff, nurses, doctors, administrators
- Clinical and administrative personnel
- Non-technical healthcare professionals

### Focus
- **How to use features**, not how they were implemented
- User workflows and business processes
- Practical guidance for daily operations
- Problem-solving and troubleshooting

## Writing Style

### Language Guidelines

#### User-Centric Language
- ✅ **Good**: "How to substitute items in patient prescriptions"
- ❌ **Bad**: "Implementation of substitute functionality"

- ✅ **Good**: "Processing pharmacy stock transfers"
- ❌ **Bad**: "BillItem entity updates in stock transfer workflow"

#### Clear and Actionable
- Use **active voice**: "Click the Submit button"
- Use **imperative mood**: "Enter the patient ID"
- Avoid **passive constructions**: "The form should be filled" → "Fill the form"

#### Step-by-Step Instructions
- Number all procedural steps clearly
- One action per step
- Include expected outcomes
- Describe what users will see

**Example:**
```markdown
### How to Process a Prescription

1. Click **Pharmacy** in the main menu
2. Select **New Prescription** from the dropdown
3. Enter the patient's BHT number in the search field
4. Click **Search** - the patient details will appear below
5. Select medications from the available list
6. Click **Submit** to process the prescription
```

### Practical Examples
- Use **real scenarios** users encounter
- Reference **actual workflows** from the system
- Include **specific examples** with sample data
- Show **before/after states** when relevant

## Content Structure

### Required Sections

Every wiki page should include these sections where applicable:

#### 1. Overview
- Brief description of what the feature does
- Why users need this feature
- Business context and value
- 2-3 sentences maximum

**Example:**
```markdown
## Overview

The Stock Ledger Report provides a complete history of all transactions for any pharmaceutical item, including purchases, issues, returns, and adjustments. This report is essential for inventory audits, stock reconciliation, and investigating discrepancies in pharmacy stock levels.
```

#### 2. When to Use
- Specific scenarios and situations
- Business triggers for using the feature
- Common use cases
- Decision criteria

**Example:**
```markdown
## When to Use

Use the Stock Ledger Report when you need to:
- Investigate stock discrepancies or missing items
- Perform monthly or annual inventory audits
- Verify supplier returns and credits
- Track item movement between departments
- Reconcile physical stock counts with system records
```

#### 3. How to Use
- Detailed step-by-step procedures
- Navigation instructions from starting point
- Field-by-field explanations
- Button and action descriptions

**Example:**
```markdown
## How to Use

### Accessing the Report

1. Navigate to **Pharmacy** → **Reports** → **Stock Ledger**
2. The search form will appear with filter options

### Generating the Report

1. **Select Item**: Choose the pharmaceutical item from the dropdown
2. **Select Location**: Choose the store or pharmacy location
3. **Date Range**: Enter the from and to dates (optional - leave blank for all dates)
4. Click **Generate Report**
5. The report will display all transactions in chronological order
```

#### 4. Understanding Messages
- System messages and their meanings
- Success confirmations
- Warning messages and required actions
- Error messages and solutions

**Example:**
```markdown
## Understanding Messages

### Success Messages
- **"Report generated successfully"**: The report is ready and displayed below

### Warning Messages
- **"No transactions found for selected period"**: Try expanding the date range or verify the item has transaction history

### Error Messages
- **"Please select an item"**: You must choose a pharmaceutical item before generating the report
- **"Invalid date range"**: The 'From' date cannot be after the 'To' date
```

#### 5. Best Practices
- Tips for effective use
- Common workflows and patterns
- Performance considerations
- Data quality recommendations

#### 6. Troubleshooting
- Common problems and solutions
- Diagnostic steps
- When to contact support
- Known limitations

#### 7. Configuration (Admin)
- Settings that affect the feature
- User impact of configuration options
- Admin-only settings explanation
- Permissions and access control

#### 8. FAQ
- Frequently asked questions
- Quick answers to common queries
- Cross-references to related features

### Optional Sections

Include these when relevant:

- **Related Features**: Links to related wiki pages
- **Reports and Exports**: Output options and formats
- **Permissions**: Who can access this feature
- **Integration**: How this works with other modules
- **Keyboard Shortcuts**: Productivity tips

## What NOT to Include

### Technical Implementation Details

❌ **Avoid These**:
- Code snippets, file paths, line numbers
- Java class names, method signatures
- Database schema, table structures, column names
- SQL queries or JPQL statements
- EclipseLink configuration details
- JSF component IDs or backend beans
- Controller method names
- Entity relationship diagrams

❌ **Bad Example**:
```markdown
The BillItem entity is updated by the PharmacyController.processBill() method,
which calls the BillFacade.create() to persist the record to the bill table
using JPQL query "SELECT b FROM Bill b WHERE b.retired = false".
```

✅ **Good Example**:
```markdown
When you save a prescription, the system creates a bill record and updates
the inventory levels automatically. The prescription will appear in the
patient's billing history immediately.
```

### Developer-Focused Content

❌ **Don't Include**:
- Debugging information or stack traces
- Development environment setup
- Version control or git workflows
- Deployment procedures
- Performance optimization code
- Testing procedures or test cases

### System Architecture

❌ **Avoid**:
- Backend process descriptions
- API endpoint documentation
- Microservice architecture details
- Database connection details
- Server configuration

## What TO Include

### User Interface Elements

✅ **Always Document**:
- Menu navigation paths
- Button labels and icons
- Form field labels and purposes
- Tab names and organization
- Dialog box options
- Dropdown menu choices

✅ **Example**:
```markdown
1. Click the **Edit** button (pencil icon) next to the prescription
2. In the **Medication** tab, locate the item to substitute
3. Click the **Substitute** button (circular arrows icon)
4. Select the replacement item from the **Available Substitutes** dropdown
```

### Error Messages and Meanings

✅ **Document All Messages**:
- Exact text of messages
- What triggered the message
- What the user should do
- Expected outcome after correction

### Business Process Workflows

✅ **Describe Workflows**:
- End-to-end business processes
- Multi-step procedures
- Approval flows
- Integration points between modules

### Configuration Options (User Perspective)

✅ **Explain Settings**:
- What each option controls
- Impact on user experience
- When to change settings
- Recommended values

## Formatting Guidelines

### Headings
- Use descriptive, action-oriented headings
- Follow hierarchy: H2 for sections, H3 for subsections
- Keep headings concise (5-7 words maximum)

### Lists
- Use numbered lists for sequential steps
- Use bullet points for non-sequential items
- Keep list items parallel in structure
- Limit nesting to 2 levels

### Emphasis
- **Bold** for UI elements: buttons, menus, field labels
- *Italic* for emphasis or new terms (first use only)
- `Code formatting` only for exact user input (not system concepts)
- > Blockquotes for important notes or warnings

### Notes and Warnings

Use these callout formats:

```markdown
> **Note:** This is general information that adds context.

> **Important:** This is critical information that users must know.

> **Warning:** This indicates potential data loss or serious issues.

> **Tip:** This is a helpful suggestion for better results.
```

### Screenshots and Images
- Include screenshots for complex UI workflows
- Annotate images with arrows or highlights
- Use descriptive alt text
- Keep images up to date with current UI

### Links
- Link to related wiki pages
- Use descriptive link text (not "click here")
- Verify all links before publishing
- Prefer relative links for internal wiki pages

## Documentation Template

Use this template as a starting point:

```markdown
# Feature Name

## Overview
Brief description of what the feature does and why users need it.

## When to Use
- Scenario 1: Description
- Scenario 2: Description
- Scenario 3: Description

## How to Use

### Accessing the Feature
1. Step-by-step navigation instructions

### Performing the Task
1. Detailed step-by-step procedure
2. Include expected outcomes
3. Note any conditional branches

## Understanding Messages

### Success Messages
- **"Message text"**: What it means and what to do next

### Warning Messages
- **"Message text"**: What caused it and how to resolve

### Error Messages
- **"Message text"**: Problem description and solution

## Best Practices
- Tip 1: Practical advice
- Tip 2: Efficiency suggestions
- Tip 3: Data quality recommendations

## Troubleshooting

### Problem 1: Description
**Symptoms:** What the user sees
**Cause:** Why it happens
**Solution:** Step-by-step fix

### Problem 2: Description
**Symptoms:** What the user sees
**Cause:** Why it happens
**Solution:** Step-by-step fix

## Configuration (Admin)
Description of relevant settings and their impact on users

## FAQ

**Q: Common question?**
A: Clear, concise answer with actionable guidance

**Q: Another question?**
A: Answer with examples or references

## Related Features
- [Related Feature 1](Link-to-Page)
- [Related Feature 2](Link-to-Page)
```

## Quality Checklist

Before publishing, verify:

- [ ] Written for end users, not developers
- [ ] No code snippets or technical implementation details
- [ ] All steps are clear and actionable
- [ ] Screenshots are current and annotated
- [ ] All UI elements are bolded correctly
- [ ] Links work and go to correct pages
- [ ] Error messages are documented
- [ ] Business context is explained
- [ ] Examples use realistic scenarios
- [ ] Grammar and spelling are correct
- [ ] Formatting is consistent
- [ ] All required sections are present

## Exception: Developer Documentation

**Only include technical details when**:
- Specifically requested for developer documentation
- Writing for the `developer_docs/` directory (not wiki)
- Creating API documentation for integration partners
- Documenting technical configuration for system administrators

For developer documentation, see [Developer Documentation Guidelines](../developer-documentation-standards.md).

---

## Additional Resources

- [Wiki Publishing Guide](wiki-publishing.md) - How to publish documentation
- [Project Board Integration](project-board-integration.md) - Linking docs to issues
- [Commit Conventions](../git/commit-conventions.md) - Documentation commits
