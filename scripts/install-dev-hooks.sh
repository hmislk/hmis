#!/usr/bin/env bash
# ============================================================
# HMIS Developer Setup: Install git security hooks
# Run this once after cloning the repo:
#   chmod +x scripts/install-dev-hooks.sh && ./scripts/install-dev-hooks.sh
# ============================================================
set -e

REPO_ROOT="$(git rev-parse --show-toplevel)"
cd "$REPO_ROOT"

echo "=== HMIS Security Hook Installer ==="
echo ""

# ---- 1. Check / install gitleaks ----------------------------
if command -v gitleaks &>/dev/null; then
  echo "[OK] gitleaks found: $(gitleaks version)"
else
  echo "[INSTALL] gitleaks not found. Installing..."
  OS=$(uname -s | tr '[:upper:]' '[:lower:]')
  ARCH=$(uname -m)
  case "$ARCH" in
    x86_64)  ARCH="x64" ;;
    aarch64) ARCH="arm64" ;;
    arm*)    ARCH="armv7" ;;
  esac
  VERSION="8.21.2"
  URL="https://github.com/gitleaks/gitleaks/releases/download/v${VERSION}/gitleaks_${VERSION}_${OS}_${ARCH}.tar.gz"
  TMP=$(mktemp -d)
  curl -sL "$URL" -o "$TMP/gitleaks.tar.gz"
  tar xzf "$TMP/gitleaks.tar.gz" -C "$TMP" gitleaks
  mkdir -p "$HOME/bin"
  mv "$TMP/gitleaks" "$HOME/bin/gitleaks"
  chmod +x "$HOME/bin/gitleaks"
  export PATH="$HOME/bin:$PATH"
  echo "[OK] gitleaks installed to ~/bin/gitleaks"
  echo "     Add 'export PATH=\$HOME/bin:\$PATH' to your ~/.bashrc or ~/.zshrc"
fi

# ---- 2. Check / install pre-commit --------------------------
if command -v pre-commit &>/dev/null; then
  echo "[OK] pre-commit found: $(pre-commit --version)"
else
  echo "[INSTALL] pre-commit not found. Attempting pip install..."
  if command -v pip3 &>/dev/null; then
    pip3 install --user pre-commit
    echo "[OK] pre-commit installed"
  elif command -v pip &>/dev/null; then
    pip install --user pre-commit
    echo "[OK] pre-commit installed"
  else
    echo "[WARN] pip not found. Install pre-commit manually:"
    echo "       pip install pre-commit"
    echo "       OR: https://pre-commit.com/#install"
  fi
fi

# ---- 3. Install the pre-commit git hook ---------------------
if command -v pre-commit &>/dev/null; then
  pre-commit install
  echo "[OK] pre-commit hook installed in .git/hooks/pre-commit"
else
  # Fallback: install a minimal gitleaks-only pre-commit hook
  echo "[FALLBACK] Installing minimal gitleaks hook directly..."
  cat > "$REPO_ROOT/.git/hooks/pre-commit" << 'HOOK'
#!/usr/bin/env bash
# Minimal gitleaks pre-commit hook (fallback when pre-commit is not installed)
if command -v gitleaks &>/dev/null; then
  gitleaks protect --staged --config=.gitleaks.toml -v
  if [ $? -ne 0 ]; then
    echo ""
    echo "ERROR: gitleaks detected potential secrets in your staged changes."
    echo "Review the findings above. If a detection is a false positive,"
    echo "add an allowlist entry in .gitleaks.toml"
    exit 1
  fi
else
  echo "WARN: gitleaks not found. Run scripts/install-dev-hooks.sh to install."
fi
HOOK
  chmod +x "$REPO_ROOT/.git/hooks/pre-commit"
  echo "[OK] Minimal gitleaks hook installed in .git/hooks/pre-commit"
fi

# ---- 4. Install the changeset-guard pre-push hook -----------
# A thin wrapper that runs the tracked scripts/hooks/pre-push, so later
# changes to the hook or .github/changeset-guard-patterns.txt apply without
# reinstalling. On a branch without the hook (cut from a deploy branch) it runs
# origin/development's copy, and blocks the push if that cannot be loaded.
# Re-run this script to upgrade an older wrapper. An unrelated existing
# pre-push hook is left alone.
HOOKS_DIR="$(git rev-parse --git-path hooks)"
mkdir -p "$HOOKS_DIR"
PRE_PUSH="$HOOKS_DIR/pre-push"
GUARD_INSTALLED=0
if [ -f "$PRE_PUSH" ] && ! grep -q "HMIS changeset-guard pre-push" "$PRE_PUSH"; then
  echo "[WARN] $PRE_PUSH already exists and is not ours; not overwriting."
  echo "       Call scripts/hooks/pre-push from it to enable the changeset guard."
else
  cat > "$PRE_PUSH" << 'HOOK'
#!/usr/bin/env bash
# HMIS changeset-guard pre-push (installed by scripts/install-dev-hooks.sh)
HOOK_SCRIPT="$(git rev-parse --show-toplevel)/scripts/hooks/pre-push"
[ -f "$HOOK_SCRIPT" ] && exec bash "$HOOK_SCRIPT" "$@"
# The branch predates the hook (e.g. a hotfix cut from a deploy branch): run
# development's copy instead of skipping the check.
if [ "${HMIS_ALLOW_RISKY_PUSH:-}" = "1" ]; then
  echo "pre-push: HMIS_ALLOW_RISKY_PUSH=1 set, changeset guard skipped."
  exit 0
fi
# Stop Git Bash on Windows rewriting the "<rev>:<path>" argument of git show.
export MSYS_NO_PATHCONV=1
git fetch -q origin +development:refs/remotes/origin/development 2>/dev/null ||
  echo "pre-push: WARNING: could not fetch origin development (offline?); using the local copy." >&2
TMP_HOOK="$(mktemp)" || exit 1
trap 'rm -f "$TMP_HOOK"' EXIT
if ! git show origin/development:scripts/hooks/pre-push > "$TMP_HOOK" 2>/dev/null || [ ! -s "$TMP_HOOK" ]; then
  echo "pre-push: BLOCKED. The changeset guard hook could not be loaded from" >&2
  echo "origin/development. Run 'git fetch origin development' and retry." >&2
  exit 1
fi
echo "pre-push: scripts/hooks/pre-push is missing on this branch; using the copy from origin/development." >&2
HMIS_GUARD_FETCHED=1 bash "$TMP_HOOK" "$@"
HOOK
  chmod +x "$PRE_PUSH"
  GUARD_INSTALLED=1
  echo "[OK] changeset-guard pre-push hook installed in $PRE_PUSH"
fi

echo ""
echo "=== Done ==="
echo "Your commits will now be scanned for secrets before they are created."
if [ "$GUARD_INSTALLED" = "1" ]; then
  echo "Pushes that add risky files (screenshots, PDFs, dumps, tmp/ files) are"
  echo "blocked. Bypass one push only with: HMIS_ALLOW_RISKY_PUSH=1 git push"
else
  echo "[WARN] The changeset guard is NOT active on push: see the warning above."
fi
echo "To test: gitleaks detect --config=.gitleaks.toml -v"
