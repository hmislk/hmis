# Conflict Resolution Prompt Files

## Complete List of Prompt Files Created

### 1. **coop-prod-merge-conflicts-resolution.md**
Main overview document with conflict categories and resolution strategies
- Updated to prioritize development (newer branch)
- Lists all conflict types and priorities

### 2. **pharmacy-controller-conflicts.md**
Detailed guidance for pharmacy controller conflicts
- Specific steps for each pharmacy controller
- Testing requirements for pharmacy workflows

### 3. **service-layer-conflicts.md**
Critical service layer conflicts (especially costing)
- Updated to prioritize development for costing
- PharmacyCostingService.java specific guidance

### 4. **deleted-files-decision.md**
Analysis of files deleted in coop-prod
- Updated to KEEP ALL development files
- Auto-resolution strategy for deleted files

### 5. **auto-resolve-strategy.md**
Commands for auto-resolving simple conflicts
- Step-by-step bash commands
- Safe auto-resolution categories

### 6. **remaining-conflicts-summary.md**
Current status after auto-resolution
- Lists 17 remaining critical conflicts
- Priority order for resolution

### 7. **costing-priority-conflicts.md** ⭐ **NEW**
Specific guidance for costing-related conflicts
- ALWAYS use development for costing
- Auto-resolution commands for costing files

### 8. **development-priority-strategy.md** ⭐ **NEW**
Updated overall strategy prioritizing development
- Complete auto-resolution command set
- Development-first merge philosophy

## Quick Reference by Conflict Type

### For Costing Conflicts → **costing-priority-conflicts.md**
### For Pharmacy Controllers → **pharmacy-controller-conflicts.md**
### For Deleted Files → **deleted-files-decision.md**
### For Auto-Resolution → **development-priority-strategy.md**
### For Current Status → **remaining-conflicts-summary.md**

## Key Update: Development Priority
All prompts updated to reflect that:
- **Coop-prod = OLD branch**
- **Development = NEWER branch**
- **Default = Use development version**
- **Costing = ALWAYS development**

## Auto-Resolution Command Summary
```bash
# Use development for ALL remaining conflicts
git checkout --ours .
git add .
```

**Exception**: Only manually review .gitignore and bills.sql for configuration merging.