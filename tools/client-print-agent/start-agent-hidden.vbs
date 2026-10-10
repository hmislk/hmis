' start-agent-hidden.vbs
' Launches raw-text-print-agent.ps1 with no visible console window.
' Placed in the current user's Startup folder so it runs automatically at logon.

Set fso = CreateObject("Scripting.FileSystemObject")
scriptDir = fso.GetParentFolderName(WScript.ScriptFullName)
psScript = scriptDir & "\raw-text-print-agent.ps1"

Set shell = CreateObject("WScript.Shell")
cmd = "powershell.exe -NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -File """ & psScript & """"
shell.Run cmd, 0, False
