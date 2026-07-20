# Replaces the ${JDBC_DATASOURCE} / ${JDBC_AUDIT_DATASOURCE} placeholders in
# persistence.xml with local JNDI names. Only used by the generated
# sync-and-redeploy.bat as a last-resort fallback when no stashed edit and no
# scripts/restore-local-jndi.sh backup pair exist to restore from instead.
#
# Usage: powershell -NoProfile -File set-local-jndi.ps1 -PersistenceFile <path> -MainJndi <name> -AuditJndi <name>
param(
    [Parameter(Mandatory = $true)][string]$PersistenceFile,
    [Parameter(Mandatory = $true)][string]$MainJndi,
    [Parameter(Mandatory = $true)][string]$AuditJndi
)

$content = Get-Content -Raw $PersistenceFile
$content = $content.Replace('${JDBC_DATASOURCE}', $MainJndi)
$content = $content.Replace('${JDBC_AUDIT_DATASOURCE}', $AuditJndi)
Set-Content -Path $PersistenceFile -Value $content -NoNewline
