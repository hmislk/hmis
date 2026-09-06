# Prints the numeric port for a named <network-listener> in a Payara domain.xml.
# Used by generate-sync-redeploy-script.bat instead of an inline -Command
# string, since embedding this regex logic in a batch-quoted PowerShell
# one-liner is fragile across cmd/PowerShell's separate escaping rules.
#
# Usage: powershell -NoProfile -File detect-domain-port.ps1 -DomainXmlPath <path> -ListenerName <name>
param(
    [Parameter(Mandatory = $true)][string]$DomainXmlPath,
    [Parameter(Mandatory = $true)][string]$ListenerName
)

# domain.xml attribute order is not guaranteed (observed: port before name),
# and default-config template lines share the same listener name with a
# ${...} placeholder port instead of a number - filter to the line(s) that
# actually contain a numeric port for this listener name.
$line = (Select-String -Path $DomainXmlPath -Pattern "name=`"$ListenerName`"").Line |
    Where-Object { $_ -match 'port="(\d+)"' } |
    Select-Object -First 1

if ($line -match 'port="(\d+)"') {
    Write-Output $Matches[1]
}
