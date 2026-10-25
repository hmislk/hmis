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
Merging the PR moves its issue to **Dev Completed** (board automation; verified on a merged PR).
**Done** belongs to QA after testing. Do not set it from development work.

### 4. Closing an Issue Without a PR
QA picks up work by board status, not by issue state. A merged PR moves its issues to "Dev Completed"; a manual `gh issue close` (explained, not a bug, duplicate, cannot reproduce) does not, so QA never sees it. After every manual close:
```bash
# Move to "Dev Completed" status, then read the status back
gh project item-edit --project-id PVT_kwDOAHw-zs4ApMln --id PROJECT_ITEM_ID --field-id PVTSSF_lADOAHw-zs4ApMlnzggpN3k --single-select-option-id 85d4d4c9
```
Add the issue to the board first if it is not on it.

## Status at every stage (QA works from this column)
QA picks up work by board status only. An issue left in Backlog or a sprint column is never tested,
whatever its PR or issue state is. Every step that changes an issue's state sets the status in the
same step and reads it back.

| Event | Status |
|---|---|
| Work starts (`start-issue`) | In Progress |
| PR opened | Reviewing & Merging |
| PR merged | Dev Completed (automation; check it) |
| Issue closed without a PR, or found already fixed on `development` | Dev Completed, with a comment saying what QA should re-check |
| Parked waiting for the reporter (needs-info) | Backlog |
| Parked for a human (needs-human) | leave as is; it is not ready for QA |

If you branch by hand instead of running `start-issue`, you skip the first row and the issue stays
in Backlog or a sprint column. Run the snippet below for each issue before you report it as done.

```bash
# STATUS_OPTION_ID: option id from "Status Option IDs" below
ISSUE_NODE=$(gh api repos/hmislk/hmis/issues/$N --jq '.node_id')
ITEM_ID=$(gh api graphql -f query='mutation($p:ID!,$c:ID!){addProjectV2ItemById(input:{projectId:$p,contentId:$c}){item{id}}}'   -f p=PVT_kwDOAHw-zs4ApMln -f c="$ISSUE_NODE" --jq '.data.addProjectV2ItemById.item.id')
gh api graphql -f query='mutation($p:ID!,$i:ID!,$o:String!){updateProjectV2ItemFieldValue(input:{projectId:$p,itemId:$i,fieldId:"PVTSSF_lADOAHw-zs4ApMlnzggpN3k",value:{singleSelectOptionId:$o}}){projectV2Item{id}}}'   -f p=PVT_kwDOAHw-zs4ApMln -f i="$ITEM_ID" -f o=$STATUS_OPTION_ID
# read back; quote this, not the mutation response
gh api graphql -f query='query($i:ID!){node(id:$i){... on ProjectV2Item{fieldValueByName(name:"Status"){... on ProjectV2ItemFieldSingleSelectValue{name}}}}}' -f i="$ITEM_ID" --jq '.data.node.fieldValueByName.name'
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
3. Move issues to "Reviewing & Merging" when PRs are created, and read the status back
4. Include comprehensive test plans in PR descriptions for QA team
