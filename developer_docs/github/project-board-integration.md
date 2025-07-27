# GitHub Project Board Integration

## Project Information
- **Project**: "CareCode: HMIS Board" (Project #11)
- **Organization**: hmislk
- **URL**: https://github.com/orgs/hmislk/projects/11

## Required Authentication
Ensure GitHub CLI has project scope:
```bash
gh auth refresh --hostname github.com -s project
```

## Project Status Workflow
When working on issues, update their status in the project board:

### 1. Starting Development
When beginning work on an issue:
```bash
# Add issue to project (if not already added)
gh project item-add 11 --owner hmislk --url https://github.com/hmislk/hmis/issues/ISSUE_NUMBER

# Move to "In Progress" status
gh project item-edit --project-id PVT_kwDOAHw-zs4ApMln --id PROJECT_ITEM_ID --field-id PVTSSF_lADOAHw-zs4ApMlnzggpN3k --single-select-option-id 47fc9ee4
```

### 2. After Creating PR
When a pull request is created:
```bash
# Move to "Reviewing & Merging" status
gh project item-edit --project-id PVT_kwDOAHw-zs4ApMln --id PROJECT_ITEM_ID --field-id PVTSSF_lADOAHw-zs4ApMlnzggpN3k --single-select-option-id a087e083
```

### 3. After Merge/Completion
When the issue is fully resolved:
```bash
# Move to "Done" status
gh project item-edit --project-id PVT_kwDOAHw-zs4ApMln --id PROJECT_ITEM_ID --field-id PVTSSF_lADOAHw-zs4ApMlnzggpN3k --single-select-option-id 1bab9178
```

## Status Option IDs
- **Backlog**: `8a5edcf1`
- **Current Sprint**: `1cbc1930`
- **To Do**: `fbaa9d0f`
- **In Progress**: `47fc9ee4`
- **Reviewing & Merging**: `a087e083`
- **Dev Completed**: `85d4d4c9`
- **Ready For Testing**: `f9564514`
- **Testing In Progress**: `63aa4183`
- **Done**: `1bab9178`

## Project Constants
- **Project ID**: `PVT_kwDOAHw-zs4ApMln`
- **Status Field ID**: `PVTSSF_lADOAHw-zs4ApMlnzggpN3k`

## Finding Project Item ID
To get the project item ID for an issue:
```bash
gh api graphql -f query='
{
  search(query: "repo:hmislk/hmis is:issue ISSUE_NUMBER", type: ISSUE, first: 1) {
    nodes {
      ... on Issue {
        number
        projectItems(first: 10) {
          nodes {
            id
            project {
              number
            }
          }
        }
      }
    }
  }
}'
```

## Automatic Workflow
Claude should automatically:
1. Add issues to the project when starting development
2. Move issues to "In Progress" when development begins
3. Move issues to "Reviewing & Merging" when PRs are created
4. Include comprehensive test plans in PR descriptions for QA team
