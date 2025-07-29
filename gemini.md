
# Gemini Actions

## Close Issue, Push, Create PR, and Revert

1.  **Get Issue Number from Branch:**
    `git branch --show-current`
2.  **Close Issue with Comment:**
    `gh issue close <issue_number> --comment "<comment>"`
3.  **Push Changes:**
    `git push origin <branch_name>`
4.  **Create Pull Request:**
    `gh pr create --title "<title>" --body "<description>"`
5.  **Revert persistence.xml:**
    `git checkout -- src/main/resources/META-INF/persistence.xml`
