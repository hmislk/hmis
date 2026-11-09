#!/bin/bash
# Detects this machine's OS/Java/Payara/domain/port/app setup and generates a
# personal $HOME/sync-and-redeploy.sh tailored to it.
#
# See developer_docs/deployment/local-sync-redeploy-script-guide.md for the
# full detection order and rationale.
#
# Usage: ./scripts/generate-sync-redeploy-script.sh [--yes] [--domain NAME]

set -u

YES=0
FORCED_DOMAIN=""
while [ $# -gt 0 ]; do
    case "$1" in
        --yes) YES=1 ;;
        --domain) shift; FORCED_DOMAIN="${1:-}" ;;
        *) echo "Unknown argument: $1" ; exit 1 ;;
    esac
    shift
done

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CONFIG_FILE="$HOME/.hmis-sync-redeploy.conf"
OUTPUT_SCRIPT="$HOME/sync-and-redeploy.sh"

echo "=== HMIS sync-and-redeploy generator ==="
echo ""

# --- 1. OS detection ---
OS_NAME="$(uname -s)"
echo "OS: $OS_NAME"

# --- 2. Java detection ---
# Portable grep -oE (not -P) throughout this script: PCRE lookbehind support
# depends on the locale being set to a UTF-8 one, which is not guaranteed on
# every machine (observed failure with LANG/LC_ALL unset).
POM_RELEASE="$(grep -A5 'maven-compiler-plugin' "$REPO_ROOT/pom.xml" | grep -oE '<release>[0-9]+' | grep -oE '[0-9]+' | head -1)"
if [ -z "$POM_RELEASE" ]; then
    POM_RELEASE="$(grep -A5 'maven-compiler-plugin' "$REPO_ROOT/pom.xml" | grep -oE '<source>[0-9]+' | grep -oE '[0-9]+' | head -1)"
fi
echo "pom.xml requires JDK release: ${POM_RELEASE:-unknown}"

JAVA_HOME_DETECTED="${JAVA_HOME:-}"
if [ -z "$JAVA_HOME_DETECTED" ]; then
    JAVA_BIN="$(command -v java || true)"
    if [ -n "$JAVA_BIN" ]; then
        JAVA_HOME_DETECTED="$(dirname "$(dirname "$(readlink -f "$JAVA_BIN")")")"
    fi
fi
echo "JAVA_HOME detected: ${JAVA_HOME_DETECTED:-NOT FOUND}"

# --- 3. Payara home detection ---
PAYARA_HOME_DETECTED="${PAYARA_HOME:-}"
if [ -z "$PAYARA_HOME_DETECTED" ]; then
    for candidate in "$HOME/payara" "/opt/payara5" "/opt/payara6"; do
        if [ -x "$candidate/bin/asadmin" ]; then
            PAYARA_HOME_DETECTED="$candidate"
            break
        fi
    done
fi
if [ -z "$PAYARA_HOME_DETECTED" ]; then
    for root in "$HOME" "/opt" "/usr/local"; do
        [ -d "$root" ] || continue
        FOUND="$(find "$root" -maxdepth 4 -iname asadmin -type f 2>/dev/null | head -1)"
        if [ -n "$FOUND" ]; then
            PAYARA_HOME_DETECTED="$(dirname "$(dirname "$FOUND")")"
            break
        fi
    done
fi
if [ -z "$PAYARA_HOME_DETECTED" ]; then
    echo "ERROR: Could not locate a Payara install. Set PAYARA_HOME and re-run."
    exit 1
fi
echo "Payara home: $PAYARA_HOME_DETECTED"
ASADMIN="$PAYARA_HOME_DETECTED/bin/asadmin"

# --- 4. Domain / ports detection ---
DOMAINS_DIR="$PAYARA_HOME_DETECTED/glassfish/domains"
mapfile -t DOMAIN_CANDIDATES < <(ls "$DOMAINS_DIR" 2>/dev/null)
if [ "${#DOMAIN_CANDIDATES[@]}" -eq 0 ]; then
    echo "ERROR: No domains found under $DOMAINS_DIR"
    exit 1
fi

DOMAIN_NAME="$FORCED_DOMAIN"
if [ -z "$DOMAIN_NAME" ]; then
    if [ "${#DOMAIN_CANDIDATES[@]}" -eq 1 ]; then
        DOMAIN_NAME="${DOMAIN_CANDIDATES[0]}"
    else
        echo "Multiple domains found: ${DOMAIN_CANDIDATES[*]}"
        read -r -p "Which domain should this script target? " DOMAIN_NAME
    fi
fi
echo "Domain: $DOMAIN_NAME"

DOMAIN_XML="$DOMAINS_DIR/$DOMAIN_NAME/config/domain.xml"
if [ ! -f "$DOMAIN_XML" ]; then
    echo "ERROR: domain.xml not found at $DOMAIN_XML"
    exit 1
fi
# Attribute order in domain.xml is not guaranteed (observed: port before
# name), so grab the whole matching <network-listener> line first, then pull
# the numeric port out of it - this also naturally skips the unused
# default-config template lines, whose ports are ${...} placeholders instead
# of numbers.
ADMIN_PORT="$(grep 'name="admin-listener"' "$DOMAIN_XML" | grep -oE 'port="[0-9]+"' | grep -oE '[0-9]+' | head -1)"
HTTP_PORT="$(grep 'name="http-listener-1"' "$DOMAIN_XML" | grep -oE 'port="[0-9]+"' | grep -oE '[0-9]+' | head -1)"
ADMIN_PORT="${ADMIN_PORT:-4848}"
HTTP_PORT="${HTTP_PORT:-8080}"
echo "Admin port: $ADMIN_PORT"
echo "HTTP port: $HTTP_PORT"

# --- 5. Already-deployed app name/contextroot ---
APP_LIST="$("$ASADMIN" --port "$ADMIN_PORT" list-applications 2>/dev/null || true)"
APP_NAME="$(echo "$APP_LIST" | awk 'NR==1 && $1 != "" && $1 != "Nothing" {print $1}')"
if [ -z "$APP_NAME" ]; then
    echo "No application currently deployed on domain '$DOMAIN_NAME' — defaulting to name/contextroot 'rh'."
    APP_NAME="rh"
    CONTEXT_ROOT="rh"
else
    echo "Detected already-deployed application: $APP_NAME"
    CONTEXT_ROOT="$APP_NAME"
fi
read -r -p "Application name to deploy as [$APP_NAME]: " APP_NAME_INPUT
APP_NAME="${APP_NAME_INPUT:-$APP_NAME}"
read -r -p "Context root [$CONTEXT_ROOT]: " CONTEXT_ROOT_INPUT
CONTEXT_ROOT="${CONTEXT_ROOT_INPUT:-$CONTEXT_ROOT}"

# --- 6. Local JNDI names ---
LOCAL_TEST_PERSISTENCE="$REPO_ROOT/src/main/resources/META-INF/persistence_for_local_testing.xml"
MAIN_JNDI=""
AUDIT_JNDI=""
if [ -f "$LOCAL_TEST_PERSISTENCE" ]; then
    MAIN_JNDI="$(grep -oE '<jta-data-source>[^<]*' "$LOCAL_TEST_PERSISTENCE" | sed 's|<jta-data-source>||' | head -1)"
    AUDIT_JNDI="$(grep -oE '<jta-data-source>[^<]*' "$LOCAL_TEST_PERSISTENCE" | sed 's|<jta-data-source>||' | tail -1)"
fi
read -r -p "Local main JNDI name [$MAIN_JNDI]: " MAIN_JNDI_INPUT
MAIN_JNDI="${MAIN_JNDI_INPUT:-$MAIN_JNDI}"
read -r -p "Local audit JNDI name [$AUDIT_JNDI]: " AUDIT_JNDI_INPUT
AUDIT_JNDI="${AUDIT_JNDI_INPUT:-$AUDIT_JNDI}"

# --- 7. Confirm ---
echo ""
echo "=== Detected configuration ==="
echo "Repo path:      $REPO_ROOT"
echo "OS:             $OS_NAME"
echo "JAVA_HOME:      ${JAVA_HOME_DETECTED:-<not set, will use PATH>}"
echo "Payara home:    $PAYARA_HOME_DETECTED"
echo "Domain:         $DOMAIN_NAME"
echo "Admin port:     $ADMIN_PORT"
echo "HTTP port:      $HTTP_PORT"
echo "App name:       $APP_NAME"
echo "Context root:   $CONTEXT_ROOT"
echo "Main JNDI:      $MAIN_JNDI"
echo "Audit JNDI:     $AUDIT_JNDI"
echo "Config file:    $CONFIG_FILE"
echo "Output script:  $OUTPUT_SCRIPT"
echo ""

if [ "$YES" -ne 1 ]; then
    read -r -p "Write this configuration and generate the script? [y/N] " CONFIRM
    case "$CONFIRM" in
        y|Y|yes|YES) ;;
        *) echo "Aborted, nothing written."; exit 0 ;;
    esac
fi

# --- 8. Write config file ---
cat > "$CONFIG_FILE" <<EOF
# Generated by scripts/generate-sync-redeploy-script.sh - do not commit this file
REPO_ROOT="$REPO_ROOT"
JAVA_HOME_DETECTED="$JAVA_HOME_DETECTED"
PAYARA_HOME="$PAYARA_HOME_DETECTED"
DOMAIN_NAME="$DOMAIN_NAME"
ADMIN_PORT="$ADMIN_PORT"
HTTP_PORT="$HTTP_PORT"
APP_NAME="$APP_NAME"
CONTEXT_ROOT="$CONTEXT_ROOT"
MAIN_JNDI="$MAIN_JNDI"
AUDIT_JNDI="$AUDIT_JNDI"
EOF
echo "Wrote $CONFIG_FILE"

# --- 9. Write runtime script ---
cat > "$OUTPUT_SCRIPT" <<'RUNTIME_EOF'
#!/bin/bash
# Generated by scripts/generate-sync-redeploy-script.sh - safe to re-generate, do not hand-edit.
set -u
CONFIG_FILE="$HOME/.hmis-sync-redeploy.conf"
if [ ! -f "$CONFIG_FILE" ]; then
    echo "ERROR: $CONFIG_FILE not found. Re-run scripts/generate-sync-redeploy-script.sh."
    exit 1
fi
# An environment MIN_FREE_MB (the documented one-command override) wins over the config file.
ENV_MIN_FREE_MB="${MIN_FREE_MB:-}"
# shellcheck source=/dev/null
source "$CONFIG_FILE"
[ -n "$ENV_MIN_FREE_MB" ] && MIN_FREE_MB="$ENV_MIN_FREE_MB"

# Usage: sync-and-redeploy.sh [--deploy-only]
#   --deploy-only  (or DEPLOY_ONLY=1) skips git sync and rebuild and re-deploys
#                  the WAR already in target/. Use it to recover after a deploy
#                  was killed (e.g. out of memory) - no 150 MB rebuild needed.
# MIN_FREE_MB (env or config file) is the free RAM required before deploying.
DEPLOY_ONLY="${DEPLOY_ONLY:-0}"
for arg in "$@"; do
    case "$arg" in
        --deploy-only) DEPLOY_ONLY=1 ;;
        *) echo "Unknown argument: $arg"; exit 1 ;;
    esac
done
MIN_FREE_MB="${MIN_FREE_MB:-3000}"
# Whole number of MB, no leading zero, at most 7 digits (under 10 TB), so the
# numeric test below can never overflow or error into the "OK" branch.
if ! [[ "$MIN_FREE_MB" =~ ^[1-9][0-9]{0,6}$ ]]; then
    echo "ERROR: MIN_FREE_MB must be a whole number of MB from 1 to 9999999, no leading zero (got '$MIN_FREE_MB')."
    exit 1
fi

PERSISTENCE_FILE="$REPO_ROOT/src/main/resources/META-INF/persistence.xml"

cd "$REPO_ROOT" || exit 1
echo "=== Repo: $REPO_ROOT ==="

if [ -n "${JAVA_HOME_DETECTED:-}" ]; then
    export JAVA_HOME="$JAVA_HOME_DETECTED"
    export PATH="$JAVA_HOME/bin:$PATH"
fi

if [ "$DEPLOY_ONLY" = "1" ]; then
    echo "Deploy-only: skipping git sync and build, using the existing WAR."
else
    # 1. Stash a local persistence.xml edit separately, so it never lands in a commit.
    STASHED_PERSISTENCE=0
    if ! git diff --quiet -- "$PERSISTENCE_FILE" 2>/dev/null; then
        echo "Stashing local persistence.xml edit..."
        git stash push -m "sync-and-redeploy: local JNDI" -- "$PERSISTENCE_FILE"
        STASHED_PERSISTENCE=1
    fi

    # 2. Commit anything else uncommitted.
    if [ -n "$(git status --porcelain)" ]; then
        echo "Committing other uncommitted changes..."
        git add -A
        git commit -m "WIP: auto-commit before sync-and-redeploy"
    fi

    # 3. Fetch and merge development. Stop on conflict - no auto-resolution.
    echo "Fetching origin/development..."
    git fetch origin development
    echo "Merging origin/development..."
    if ! git merge origin/development --no-edit; then
        echo ""
        echo "ERROR: merge conflict. Resolve manually, then re-run this script."
        echo "Repository left mid-merge intentionally - no automatic conflict resolution."
        exit 1
    fi

    # 4. Restore local JNDI.
    if [ "$STASHED_PERSISTENCE" -eq 1 ]; then
        echo "Restoring stashed persistence.xml edit..."
        git stash pop
    elif [ -f "$REPO_ROOT/.jndi-backup-main" ] && [ -f "$REPO_ROOT/.jndi-backup-audit" ]; then
        echo "Restoring local JNDI via restore-local-jndi.sh..."
        (cd "$REPO_ROOT" && ./scripts/restore-local-jndi.sh)
    else
        echo "Setting local JNDI from generator config..."
        sed -i "s|<jta-data-source>\${JDBC_DATASOURCE}</jta-data-source>|<jta-data-source>$MAIN_JNDI</jta-data-source>|" "$PERSISTENCE_FILE"
        sed -i "s|<jta-data-source>\${JDBC_AUDIT_DATASOURCE}</jta-data-source>|<jta-data-source>$AUDIT_JNDI</jta-data-source>|" "$PERSISTENCE_FILE"
    fi

    # 5. Build.
    MVN_CMD="mvn"
    if [ -x "$REPO_ROOT/detect-maven.sh" ]; then
        MVN_CMD="$REPO_ROOT/detect-maven.sh"
    fi
    echo "Building with $MVN_CMD ..."
    (cd "$REPO_ROOT" && "$MVN_CMD" clean package -DskipTests)
fi

# 6. Verify the WAR, rebuild once if it looks stale/empty.
WAR_FILE="$(ls -t "$REPO_ROOT"/target/*.war 2>/dev/null | head -1)"
MIN_WAR_SIZE=1048576
if [ -z "$WAR_FILE" ] || [ "$(stat -c%s "$WAR_FILE" 2>/dev/null || echo 0)" -lt "$MIN_WAR_SIZE" ]; then
    if [ "$DEPLOY_ONLY" = "1" ]; then
        echo "ERROR: --deploy-only needs a valid WAR in $REPO_ROOT/target but none was found."
        echo "Run without --deploy-only to build one."
        exit 1
    fi
    echo "WAR missing or suspiciously small, rebuilding once..."
    MVN_CMD="mvn"
    if [ -x "$REPO_ROOT/detect-maven.sh" ]; then
        MVN_CMD="$REPO_ROOT/detect-maven.sh"
    fi
    (cd "$REPO_ROOT" && "$MVN_CMD" clean package -DskipTests)
    WAR_FILE="$(ls -t "$REPO_ROOT"/target/*.war 2>/dev/null | head -1)"
    if [ -z "$WAR_FILE" ] || [ "$(stat -c%s "$WAR_FILE" 2>/dev/null || echo 0)" -lt "$MIN_WAR_SIZE" ]; then
        echo "ERROR: WAR still missing/too small after rebuild. Aborting deploy."
        exit 1
    fi
fi
echo "WAR ready: $WAR_FILE"

# 6b. Free-memory pre-check. 'deploy --force' undeploys the running app first, so a
# kill (out of memory) mid-deploy leaves NO app deployed. Never start an undeploy
# that cannot finish: abort here, while the old app is still running.
free_mb() {
    local v=""
    v="$(awk '/^MemAvailable:/ {print int($2/1024)}' /proc/meminfo 2>/dev/null)"
    if [ -z "$v" ] && command -v powershell.exe >/dev/null 2>&1; then
        v="$(powershell.exe -NoProfile -Command '[int]((Get-CimInstance Win32_OperatingSystem).FreePhysicalMemory/1024)' 2>/dev/null | tr -d '\r')"
    fi
    if [ -z "$v" ] && command -v free >/dev/null 2>&1; then
        v="$(free -m | awk '/^Mem:/ {print $7}')"
    fi
    echo "$v"
}
FREE_MB="$(free_mb)"
# Fail closed: anything that is not a plain number (empty, "N/A", an error text)
# aborts, because the old app is still running and must stay deployed.
if [ "${SKIP_MEMORY_CHECK:-0}" = "1" ]; then
    echo "WARNING: SKIP_MEMORY_CHECK=1 set; deploying WITHOUT the free-memory check (free memory read: '${FREE_MB}')."
elif ! [[ "$FREE_MB" =~ ^[0-9]{1,9}$ ]]; then
    echo ""
    echo "ERROR: could not measure free memory (got '${FREE_MB}'). Aborting BEFORE touching the running app."
    echo "Fix the memory reading (/proc/meminfo, PowerShell Get-CimInstance or 'free'), or, after checking RAM"
    echo "yourself, opt out explicitly: SKIP_MEMORY_CHECK=1 $0 --deploy-only"
    exit 1
elif [ "$((10#$FREE_MB))" -lt "$MIN_FREE_MB" ]; then
    echo ""
    echo "ERROR: only ${FREE_MB} MB RAM free, need at least ${MIN_FREE_MB} MB (MIN_FREE_MB) to deploy safely."
    echo "Aborting BEFORE touching the running app, so it stays deployed."
    echo "Options:"
    echo "  - Close the browser, Playwright and IDE to free RAM, then re-run with --deploy-only"
    echo "  - Build on a roomier machine and copy the WAR here, then run with --deploy-only"
    echo "  - Test on staging instead of this machine"
    echo "  - Override at your own risk: MIN_FREE_MB=<lower value> $0 --deploy-only"
    exit 1
else
    echo "Free memory OK: ${FREE_MB} MB (minimum ${MIN_FREE_MB} MB)."
fi

# 7. Deploy with the discovered name/contextroot - never derived from the WAR filename.
ASADMIN="$PAYARA_HOME/bin/asadmin"
echo "Deploying as name=$APP_NAME contextroot=$CONTEXT_ROOT on port $ADMIN_PORT ..."
"$ASADMIN" --port "$ADMIN_PORT" deploy --force=true --name "$APP_NAME" --contextroot "$CONTEXT_ROOT" "$WAR_FILE"

# 8. Report success/failure from server.log.
SERVER_LOG="$PAYARA_HOME/glassfish/domains/$DOMAIN_NAME/logs/server.log"
echo "Recent server.log entries:"
tail -n 30 "$SERVER_LOG" 2>/dev/null | grep -iE "deployed|deploy.*fail|exception" || true
echo "Done. Check http://localhost:$HTTP_PORT/$CONTEXT_ROOT/ if the app didn't report success above."
RUNTIME_EOF

chmod +x "$OUTPUT_SCRIPT"
echo "Wrote $OUTPUT_SCRIPT"
echo ""
echo "Run it with: $OUTPUT_SCRIPT"
