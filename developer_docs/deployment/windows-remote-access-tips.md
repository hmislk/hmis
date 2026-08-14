# Windows Remote Access Tips (SSH / Payara Admin)

Practical gotchas hit while working with the HMIS server fleet from a Windows
dev machine. No hostnames, IPs, or credentials here by design — those live in
each developer's own `C:\Credentials\credentials.txt` and `~/.ssh/config`
(never in this repo — see CLAUDE.md's credentials rule).

## Git-Bash's `ssh` can't see keys in the Windows `ssh-agent` service

If you load a key with `ssh-add` from **PowerShell** (after
`Start-Service ssh-agent`), that key is stored behind the Windows named pipe
`\\.\pipe\openssh-ssh-agent`. Git-Bash's bundled MSYS `ssh`/`ssh-add`
(`/usr/bin/ssh`) is a different OpenSSH build and cannot reach that pipe —
`ssh-add -l` in Git-Bash will report "Could not open a connection to your
authentication agent" even though the key really is loaded.

Fix: from Git-Bash, call the **Windows-native** OpenSSH client directly
instead of the one on `PATH`:

```bash
/c/Windows/System32/OpenSSH/ssh.exe -F ~/.ssh/config <host-alias>
/c/Windows/System32/OpenSSH/ssh-add.exe -l   # to verify what's loaded
```

(Anything after `<host-alias>` is passed to the remote shell as a command —
leave it off entirely for an interactive session.)

Alternative if you'd rather avoid the Windows service entirely: start an
MSYS-native agent in the same Git-Bash session instead (`eval $(ssh-agent -s)`
then `ssh-add`) — no admin rights needed, but it only lasts that session.

## Payara admin console over a tunnel is plain HTTP, not HTTPS

On this fleet, secure admin isn't enabled, so the forwarded admin port serves
plain `http://`, not `https://`. Hitting it with `https://` (or `curl -k
https://...`) just times out — it's not a certificate problem, the port isn't
speaking TLS at all.

That's only safe because the traffic is flowing through the encrypted SSH
tunnel — never expose the admin port directly (e.g. a public port-forward) or
hit it in `http://` outside the tunnel; with secure admin off, credentials go
over the wire in clear text.

## `ControlMaster`/`ControlPersist` (SSH multiplexing) is unreliable here

The usual trick to avoid re-authenticating on every SSH command — adding
`ControlMaster auto` / `ControlPersist` to `~/.ssh/config` — has been tried on
Windows (OpenSSH-for-Windows 9.5p1) and failed with
`getsockname failed: Bad file descriptor`, dropping the first connection.
Don't rely on it without testing a newer OpenSSH-for-Windows build first.

## Driving remote Payara via `asadmin` instead of the browser console

Once you have an SSH tunnel open to a VM's admin port (`<forwarded-admin-port>`
on your machine forwarded to the Payara admin port on the VM), point your
**local** `asadmin` at the forwarded port instead of clicking through the
browser console. Needs local Payara installed with `asadmin` on `PATH`, or
call it by full path (e.g. `<PAYARA_HOME>/bin/asadmin.bat` on Windows).

Read-only checks are safe to run anytime:

```bash
asadmin --host localhost --port <forwarded-admin-port> --user admin \
  --passwordfile <temp-file-with-AS_ADMIN_PASSWORD> list-applications
```

`restart-domain` is different — it stops and restarts the DAS, so the admin
console and any concurrent `asadmin` commands drop until it's back up. Only
run it inside an approved maintenance window:

```bash
asadmin --host localhost --port <forwarded-admin-port> --user admin \
  --passwordfile <temp-file-with-AS_ADMIN_PASSWORD> restart-domain
```

Write the password file to a scratch/temp location outside the repo and
delete it immediately after — never leave `AS_ADMIN_PASSWORD` sitting in a
file, and never write it into this repo.
