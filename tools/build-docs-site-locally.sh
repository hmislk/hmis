#!/bin/bash
# Build the docs site locally the same way publish-wiki-docs.yml does.
# Needs the wiki checked out at ../hmis.wiki and mkdocs-material plus
# mkdocs-redirects installed (versions as in the workflow).
# Usage: tools/build-docs-site-locally.sh   (MKDOCS=/path/to/mkdocs to override)
# Output: tmp/wikisite/site; serve it with: python3 -m http.server -d tmp/wikisite/site 8765
set -e
cd "$(dirname "$0")/.."
MKDOCS=${MKDOCS:-mkdocs}
case $MKDOCS in */*) MKDOCS=$(realpath "$MKDOCS");; esac
W=tmp/wikisite
rm -rf $W/docs $W/site $W/wiki-source $W/tools $W/mkdocs-extra $W/overrides
mkdir -p $W
rsync -a --exclude .git ../hmis.wiki/ $W/wiki-source/
cp mkdocs.yml $W/
cp -r tools $W/
[ -d mkdocs-extra ] && cp -r mkdocs-extra $W/
[ -d overrides ] && cp -r overrides $W/
cd $W
python3 tools/convert-wiki-for-mkdocs.py wiki-source docs > convert.log
[ -d mkdocs-extra ] && cp -r mkdocs-extra/. docs/
python3 tools/generate-wiki-nav.py docs mkdocs.yml mkdocs.generated.yml > gen.log
"$MKDOCS" build -f mkdocs.generated.yml > build.log 2>&1 || { tail build.log; exit 1; }
echo "warnings: $(grep -c WARNING build.log || true)"
head -12 gen.log
