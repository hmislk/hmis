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

echo ""
echo "=== Done ==="
echo "Your commits will now be scanned for secrets before they are created."
echo "To test: gitleaks detect --config=.gitleaks.toml -v"
