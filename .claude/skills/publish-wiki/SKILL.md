---
name: publish-wiki
description: >
  Publish documentation from wiki-docs/ to the GitHub Wiki. Use when user documentation
  has been created and needs to be published to https://github.com/hmislk/hmis/wiki.
  Handles cloning the wiki repo, copying files, and pushing.
disable-model-invocation: true
allowed-tools: Bash, Read, Glob
---

# Publish to GitHub Wiki

Publish user documentation from `wiki-docs/` to the hmislk/hmis GitHub Wiki using the sibling directory approach.

## Arguments

- `$ARGUMENTS` - Optional: specific file path within wiki-docs/ to publish (publishes all if omitted)

## Steps

1. **Verify wiki-docs/ has content**:
   ```bash
   ls wiki-docs/
   ```

2. **Check if sibling wiki directory exists**:
   ```bash
   ls ../hmis.wiki/ 2>/dev/null || echo "NOT_FOUND"
   ```

3. **If NOT_FOUND, clone the wiki repo**:
   ```bash
   cd .. && git clone https://github.com/hmislk/hmis.wiki.git && cd hmis
   ```

4. **Update the wiki repo**:
   ```bash
   cd ../hmis.wiki && git pull origin master
   ```

5. **Copy files from wiki-docs/ to wiki repo** preserving directory structure

6. **Commit and push**:
   ```bash
   cd ../hmis.wiki
   git add .
   git commit -m "Add documentation - Published from wiki-docs/"
   git push origin master
   ```

7. **Report published pages** with wiki URLs: `https://github.com/hmislk/hmis/wiki/Page-Name`

## Important Notes

- Wiki page names come from file names (dashes become spaces)
- Always pull latest wiki before copying to avoid conflicts
- Publish IMMEDIATELY after creating docs, don't wait for PR merge
