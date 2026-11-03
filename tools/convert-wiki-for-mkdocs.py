#!/usr/bin/env python3
"""
Wiki-to-MkDocs Conversion Script
================================
Converts the GitHub Wiki source into MkDocs-compatible Markdown in docs/.

Handles:
1. Copy Wiki files (skip .git)
2. Normalise Home.md → index.md
3. Fix broken hybrid [[Label](url)](url) syntax
4. Convert [[Gollum]] style links to standard Markdown
5. Convert https://github.com/hmislk/hmis/wiki/Page-Name URLs to internal
   relative links so they work inside the MkDocs site
6. Wrap links containing ( ) in <angle brackets> so the Markdown parser
   doesn't truncate them
7. Fix image references (images/ folder preserved as-is)
8. Resolve every internal page link by page name (the Wiki namespace is
   flat) and rewrite it relative to the linking file, so bare names like
   ``[Login](Login)`` and links from sub-folder pages work
9. Adapt GitHub-style lists to Python-Markdown (blank line before a list,
   4-space nesting)
"""

from pathlib import Path
from urllib.parse import quote, unquote
import os
import re
import shutil
import sys


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def needs_angle_brackets(filename: str) -> bool:
    """Return True if the filename contains characters that would confuse
    the Markdown link-destination parser — most notably ( and )."""
    return "(" in filename or ")" in filename


def wiki_page_to_relative(page_name: str) -> str:
    """Convert a Wiki page name to a relative Markdown link for MkDocs.
    Filenames with ( ) are wrapped in <…> so Markdown doesn't truncate."""

    page_name = page_name.strip()
    if not page_name:
        return ""

    # Split off anchor
    anchor = ""
    if "#" in page_name:
        page_name, anchor = page_name.split("#", 1)
        page_name = page_name.strip()
        anchor = "#" + quote(anchor.strip().lower().replace(" ", "-"))

    if not page_name:
        return anchor if anchor else ""

    # Remove leading slash
    if page_name.startswith("/"):
        page_name = page_name[1:]

    # Add .md if not present
    if not page_name.lower().endswith((".md", ".markdown")):
        page_name = f"{page_name}.md"

    # Check for parens BEFORE URL-encoding
    use_brackets = needs_angle_brackets(page_name)

    # URL-quote special characters (keep / # - _ %)
    safe = quote(page_name, safe="/.#-_%")

    safe += anchor
    if use_brackets:
        safe = f"<{safe}>"

    return safe


# ---------------------------------------------------------------------------
# Step 1: Copy
# ---------------------------------------------------------------------------

def copy_wiki(source_dir: Path, docs_dir: Path) -> None:
    if docs_dir.exists():
        shutil.rmtree(docs_dir)
    docs_dir.mkdir(parents=True, exist_ok=True)

    for item in source_dir.iterdir():
        if item.name == ".git":
            continue
        destination = docs_dir / item.name
        if item.is_dir():
            shutil.copytree(item, destination)
        else:
            shutil.copy2(item, destination)


# ---------------------------------------------------------------------------
# Step 2: Home page
# ---------------------------------------------------------------------------

def normalise_home_page(docs_dir: Path) -> None:
    home = docs_dir / "Home.md"
    index = docs_dir / "index.md"
    if home.exists() and not index.exists():
        home.rename(index)

    # MkDocs builds a README.md as its folder's index page: Pharmacy/README.md
    # would take the /Pharmacy/ URL from Pharmacy.md, and the root README.md
    # is dropped next to index.md. MkDocs matches "README" case-sensitively,
    # while wiki links resolve by page name ignoring case, so "Readme.md"
    # builds as an ordinary page and links to it keep working.
    for readme in docs_dir.rglob("README.md"):
        readme.rename(readme.with_name("Readme.md"))


# ---------------------------------------------------------------------------
# Step 3: Link conversion
# ---------------------------------------------------------------------------

def _fix_hybrid_gollum_markdown(text: str) -> str:
    """Fix broken ``[[Label](url)](url)`` hybrid gollum+markdown syntax.

    Several wiki pages use this malformed pattern.  We collapse it to
    the inner ``[Label](url)`` by scanning character-by-character
    (regex can't handle the nested parens in wiki URLs like
    ``...(AMPs))``).
    """
    result = []
    i = 0
    n = len(text)

    while i < n:
        if text[i : i + 2] != "[[":
            result.append(text[i])
            i += 1
            continue

        # We found [[ — try to parse the hybrid pattern
        rest = text[i + 2 :]
        bracket_pos = rest.find("](")
        if bracket_pos <= 0:
            result.append(text[i])
            i += 1
            continue

        label = rest[:bracket_pos]
        url_start_pos = bracket_pos + 2  # after ](

        # Find the matching ')' for the URL by counting parentheses
        depth = 0
        url_end = url_start_pos
        while url_end < len(rest):
            ch = rest[url_end]
            if ch == "(":
                depth += 1
            elif ch == ")":
                if depth == 0:
                    break
                depth -= 1
            url_end += 1

        if url_end >= len(rest):
            result.append(text[i])
            i += 1
            continue

        inner_url = rest[url_start_pos:url_end]

        # After the inner markdown link we expect:  ) ] ( outer_url )
        #                                  url_end ^  ^     ^
        tail = rest[url_end:]  # starts with )
        if not (tail.startswith(")](")):
            result.append(text[i])
            i += 1
            continue

        # Extract outer_url
        outer_start = url_end + 3  # skip )](
        outer_rest = rest[outer_start:]
        depth = 0
        outer_end = 0
        while outer_end < len(outer_rest):
            ch = outer_rest[outer_end]
            if ch == "(":
                depth += 1
            elif ch == ")":
                if depth == 0:
                    break
                depth -= 1
            outer_end += 1

        outer_url = outer_rest[:outer_end]

        # Only fix if both inner and outer are wiki URLs
        wiki_prefix = "github.com/hmislk/hmis/wiki/"
        if wiki_prefix in inner_url and wiki_prefix in outer_url:
            result.append(f"[{label}]({inner_url})")
            # advance past the entire [[...]...](...) construct
            consumed = 2 + outer_start + outer_end + 1  # [[ + up to and including final )
            i += consumed
        else:
            result.append(text[i])
            i += 1

    return "".join(result)


def _convert_wiki_url_links(text: str) -> str:
    """Convert ``[label](wiki_url)`` links to internal relative links."""

    # The page name may hold one level of balanced parentheses, e.g.
    # "Health-Level-Seven(HL7)". The link's closing ")" is consumed so that
    # text after it, such as ")." in "(see [x](url)).", stays outside.
    wiki_url_pattern = re.compile(
        r"\[([^\]]*)\]\((https?://github\.com/hmislk/hmis/wiki/?"
        r"((?:[^\s\"<>()]|\([^\s\"<>()]*\))*))\)"
    )

    def replace(m: re.Match) -> str:
        label = m.group(1)
        raw_page = unquote(m.group(3))

        # Fix concatenated URLs (e.g. "Page-Namehttps://github.com/...")
        if "https://" in raw_page or "http://" in raw_page:
            # Split on http:// or https:// and take the first part
            raw_page = re.split(r"https?://", raw_page)[0]

        # Strip trailing ')' chars that belong to outer Markdown syntax
        while raw_page.endswith(")") and raw_page.count(")") > raw_page.count("("):
            raw_page = raw_page[:-1]

        # Handle wiki root link (empty page name → index)
        if not raw_page or raw_page in ("/", ""):
            return f"[{label}](index.md)"

        # Split off anchor
        anchor = ""
        if "#" in raw_page:
            raw_page, frag = raw_page.split("#", 1)
            anchor = "#" + quote(frag.strip().lower().replace(" ", "-"))

        page_name = raw_page.strip().rstrip("/")
        if page_name.endswith("/_edit"):
            page_name = page_name[: -len("/_edit")]

        if not page_name:
            return m.group(0)

        rel = wiki_page_to_relative(page_name)
        if anchor and "#" not in rel:
            rel = rel + anchor

        return f"[{label}]({rel})"

    return wiki_url_pattern.sub(replace, text)


def _convert_gollum_links(text: str) -> str:
    """Convert remaining ``[[Page]]`` and ``[[Label|Page]]`` Gollum links."""

    pattern = re.compile(r"\[\[([^\[\]]+?)\]\]")

    def replace(m: re.Match) -> str:
        content = m.group(1).strip()

        if "|" in content:
            label, target = content.split("|", 1)
            label = label.strip()
            target = target.strip()
        else:
            label = content
            target = content

        # If target is a URL, keep as regular markdown link
        if target.startswith("http://") or target.startswith("https://"):
            # Try converting if it's a wiki URL
            if "github.com/hmislk/hmis/wiki/" in target:
                m2 = re.search(
                    r"github\.com/hmislk/hmis/wiki/([^\s\"<>]+)", target
                )
                if m2:
                    page = unquote(m2.group(1))
                    # Strip unbalanced trailing )
                    while page.endswith(")") and page.count(")") > page.count("("):
                        page = page[:-1]
                    rel = wiki_page_to_relative(page)
                    return f"[{label}]({rel})"
            return f"[{label}]({target})"

        rel = wiki_page_to_relative(target)
        if not rel:
            return m.group(0)
        return f"[{label}]({rel})"

    return pattern.sub(replace, text)


def convert_links(text: str) -> str:
    """Apply all link conversions."""
    # Order: fix hybrids first, then wiki URLs, then remaining gollum
    text = _fix_hybrid_gollum_markdown(text)
    text = _convert_wiki_url_links(text)
    text = _convert_gollum_links(text)
    return text


# ---------------------------------------------------------------------------
# Step 4: Process files
# ---------------------------------------------------------------------------

def convert_markdown_files(docs_dir: Path) -> None:
    for path in docs_dir.rglob("*"):
        if not path.is_file():
            continue
        if path.suffix.lower() not in {".md", ".markdown"}:
            continue

        text = path.read_text(encoding="utf-8", errors="ignore")
        text = convert_links(text)
        path.write_text(text, encoding="utf-8")


# ---------------------------------------------------------------------------
# Step 5: Resolve internal links by page name
# ---------------------------------------------------------------------------

def build_page_map(docs_dir: Path) -> dict:
    """Map lower-cased page name -> docs file.

    The GitHub Wiki namespace is flat: ``[x](Login)`` finds Login.md in any
    folder. Root-level files win over sub-folder copies."""
    pages = {}
    files = sorted(
        (p for p in docs_dir.rglob("*")
         if p.is_file() and p.suffix.lower() in {".md", ".markdown"}),
        key=lambda p: (len(p.relative_to(docs_dir).parts), str(p)),
    )
    for path in files:
        pages.setdefault(path.stem.lower(), path)
    # Step 2 renamed Home.md to index.md
    if "index" in pages:
        pages.setdefault("home", pages["index"])
    return pages


def _resolve_link_target(target: str, source: Path, pages: dict):
    """Return the rewritten link target, or None to leave it unchanged."""
    raw = target.strip()
    bracketed = raw.startswith("<") and raw.endswith(">")
    if bracketed:
        raw = raw[1:-1].strip()
    elif " " in raw:
        return None  # a link title (`url "title"`) or not a link at all
    if not raw or raw.startswith(("#", "/")):
        return None
    if re.match(r"^[a-z][a-z0-9+.-]*:", raw, re.I):
        return None

    anchor = ""
    if "#" in raw:
        raw, frag = raw.split("#", 1)
        anchor = "#" + frag

    name = unquote(raw).rstrip("/").rsplit("/", 1)[-1]
    if name.lower().endswith((".md", ".markdown")):
        name = name.rsplit(".", 1)[0]
    elif re.search(r"\.[A-Za-z0-9]{2,5}$", name) and name.lower() not in pages:
        return None  # an image or attachment, not a page

    page = pages.get(name.lower())
    if page is None:
        return None

    rel = Path(os.path.relpath(page, source.parent)).as_posix()
    # Keep non-ASCII characters (e.g. "‐") as-is: MkDocs matches link
    # targets against file names without percent-decoding them.
    safe = rel.replace(" ", "%20") + anchor
    if needs_angle_brackets(rel):
        safe = f"<{safe}>"
    return safe


FENCE = re.compile(r"^\s*(`{3,}|~{3,})(.*)$")


class FenceTracker:
    """Track fenced code blocks by CommonMark rules: a block closes only on a
    fence of the same character, at least as long, with nothing after it, so
    a line of ``` inside a ```` block stays code."""

    def __init__(self):
        self.fence = None

    def in_code(self, line: str) -> bool:
        """Feed one line; return True if it is a fence or inside a block."""
        m = FENCE.match(line)
        if self.fence is None:
            if m and not (m.group(1)[0] == "`" and "`" in m.group(2)):
                self.fence = m.group(1)
                return True
            return False
        if (m and m.group(1)[0] == self.fence[0]
                and len(m.group(1)) >= len(self.fence)
                and not m.group(2).strip()):
            self.fence = None
        return True


def _resolve_image_target(target: str, source: Path, docs_dir: Path):
    """Return the rewritten image target, or None to leave it unchanged.

    GitHub resolves a relative image path such as ``images/x.png`` from the
    wiki root, even on a page in a sub-folder; MkDocs resolves it from the
    page's folder. Point such links at the root copy when only that exists."""
    raw = target.strip()
    if " " in raw or not raw or raw.startswith(("#", "/", "<")):
        return None
    if re.match(r"^[a-z][a-z0-9+.-]*:", raw, re.I):
        return None
    if (source.parent / unquote(raw)).exists():
        return None
    root_copy = docs_dir / unquote(raw)
    if not root_copy.is_file():
        return None
    return Path(os.path.relpath(root_copy, source.parent)).as_posix()


def resolve_internal_links(text: str, source: Path, pages: dict,
                           docs_dir: Path = None) -> str:
    """Rewrite ``[label](target)`` page links to paths relative to *source*.

    Link targets are scanned with balanced parentheses because page names
    such as ``Health-Level-Seven(HL7)`` contain them. Fenced code blocks are
    left untouched, and so are image links, except root-relative ones on
    sub-folder pages (see ``_resolve_image_target``) when *docs_dir* is
    given."""
    out_lines = []
    fences = FenceTracker()
    for line in text.split("\n"):
        if fences.in_code(line) or "](" not in line:
            out_lines.append(line)
            continue

        result = []
        i = 0
        while True:
            mid = line.find("](", i)
            if mid < 0:
                break
            start = line.rfind("[", i, mid)
            depth = 0
            end = mid + 2
            while end < len(line):
                ch = line[end]
                if ch == "(":
                    depth += 1
                elif ch == ")":
                    if depth == 0:
                        break
                    depth -= 1
                end += 1
            if end >= len(line):
                break
            is_image = start > 0 and line[start - 1] == "!"
            new_target = None
            if is_image and docs_dir is not None:
                new_target = _resolve_image_target(
                    line[mid + 2 : end], source, docs_dir
                )
            elif not is_image:
                new_target = _resolve_link_target(
                    line[mid + 2 : end], source, pages
                )
            result.append(line[i : mid + 2])
            result.append(new_target if new_target else line[mid + 2 : end])
            i = end
        result.append(line[i:])
        out_lines.append("".join(result))
    return "\n".join(out_lines)


# ---------------------------------------------------------------------------
# Step 6: Lists (GitHub Markdown -> Python-Markdown)
# ---------------------------------------------------------------------------

LIST_ITEM = re.compile(r"^(\s*)([-*+]|\d+[.)])\s")


def fix_lists(text: str) -> str:
    """Adapt GitHub-style lists to Python-Markdown.

    - GitHub starts a list straight after a line of text; Python-Markdown
      needs a blank line first, otherwise the items merge into a paragraph.
    - GitHub nests with 2-space indents; Python-Markdown needs 4, otherwise
      nested items are flattened. Indents are doubled inside list blocks."""
    out = []
    fences = FenceTracker()
    in_list = False
    for line in text.split("\n"):
        if fences.in_code(line):
            out.append(line)
            continue

        m = LIST_ITEM.match(line)
        if m and not m.group(1):
            prev = out[-1] if out else ""
            if prev.strip() and not LIST_ITEM.match(prev) and not in_list:
                out.append("")
            in_list = True
        elif m and in_list:
            indent = len(m.group(1).expandtabs(4))
            if indent % 4:
                line = " " * (indent * 2) + line.lstrip()
        elif not line.strip():
            pass  # a blank line may sit between items of one list
        elif not line.startswith((" ", "\t")):
            in_list = False
        out.append(line)
    return "\n".join(out)


def postprocess_markdown_files(docs_dir: Path) -> None:
    pages = build_page_map(docs_dir)
    for path in docs_dir.rglob("*"):
        if not path.is_file():
            continue
        if path.suffix.lower() not in {".md", ".markdown"}:
            continue

        text = path.read_text(encoding="utf-8", errors="ignore")
        text = resolve_internal_links(text, path, pages, docs_dir)
        text = fix_lists(text)
        path.write_text(text, encoding="utf-8")


# ---------------------------------------------------------------------------
# Step 7: Index
# ---------------------------------------------------------------------------

def create_index_if_missing(docs_dir: Path) -> None:
    index = docs_dir / "index.md"
    if index.exists():
        return

    pages = sorted(
        p.relative_to(docs_dir)
        for p in docs_dir.rglob("*.md")
        if p.name.lower() != "index.md"
    )

    lines = [
        "# Documentation",
        "",
        "This site is generated automatically from the GitHub Wiki.",
        "",
        "## Pages",
        "",
    ]
    for page in pages:
        title = page.stem.replace("-", " ").replace("_", " ")
        link = quote(str(page), safe="/.#-_%")
        lines.append(f"- [{title}]({link})")

    index.write_text("\n".join(lines) + "\n", encoding="utf-8")


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def main() -> int:
    if len(sys.argv) != 3:
        print("Usage: convert-wiki-for-mkdocs.py <wiki-source-dir> <docs-dir>")
        return 1

    source_dir = Path(sys.argv[1]).resolve()
    docs_dir = Path(sys.argv[2]).resolve()

    if not source_dir.exists():
        print(f"Wiki source directory not found: {source_dir}")
        return 1

    copy_wiki(source_dir, docs_dir)
    normalise_home_page(docs_dir)
    convert_markdown_files(docs_dir)
    postprocess_markdown_files(docs_dir)
    create_index_if_missing(docs_dir)

    print(f"Wiki converted successfully: {source_dir} -> {docs_dir}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
