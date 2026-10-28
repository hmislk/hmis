#!/usr/bin/env python3
"""
Wiki Navigation Generator
=========================
Builds the MkDocs ``nav:`` from the wiki's own ``Wiki-Article-Index.md`` so
the documentation site menu follows whatever wiki editors maintain.

Usage:
    generate-wiki-nav.py <docs-dir> <base-config> <out-config>

Writes <out-config> as ``INHERIT: <base-config>`` plus a generated ``nav:``.
Build the site with ``mkdocs build -f <out-config>``.

Menu layout:
- Home
- User Manual: overview, the article index, then every ``##`` group of the
  article index (``###`` headings become sub-sections)
- Videos, Knowledge Base, Developer Guide, About: fixed pages, skipped when
  missing; the index group "Developer Documentation" goes to Developer Guide
- More Articles: every page not reached above, grouped by sub-folder or
  filename prefix, so every wiki page is reachable from the menu

A report of broken index links and pages missing from the index is written
to $GITHUB_STEP_SUMMARY when set, otherwise to stdout.
"""

from collections import Counter, defaultdict
from pathlib import Path
from urllib.parse import unquote
import json
import os
import re
import sys


ARTICLE_INDEX = "Wiki-Article-Index"
DEVELOPER_GROUP = "developer documentation"
MIN_GROUP_SIZE = 5
EXCLUDED_PAGES = {"index", "_footer", "_sidebar"}

VIDEO_PAGES = [("Video Library", "Video-Library")]
KNOWLEDGE_PAGES = [
    ("Overview", "Knowledgebase"),
    ("Functions", "Functions"),
    ("Clinical Knowledge", "Clinical-Knowledge"),
    ("Medicines", "Medicines"),
    ("Pharmaceutical Fundamentals", "Pharmaceutical-Fundamentals"),
]
DEVELOPER_PAGES = [
    ("Overview", "Developer-Manual"),
    ("Tech Stack", "Tech-Stack"),
    ("API", "API"),
    ("Design Documentation", "Design-Documentation"),
    ("Code Concepts", "Code-Concepts-for-Developers"),
    ("Theories for Developers", "Theories-for-developers"),
    ("Troubleshooting", "Troubleshooting"),
]
ABOUT_PAGES = [
    ("About Open HMIS", "About-Open-HMIS"),
    ("Road Map", "Road-map"),
    ("Getting Commercial Support", "Getting-Commercial-Support"),
    ("Wiki Automation Guide", "Wiki-Automation-Guide"),
]


# ---------------------------------------------------------------------------
# Page lookup
# ---------------------------------------------------------------------------

def list_doc_files(docs_dir: Path) -> list:
    """Docs-relative paths of every page MkDocs will build, root files first.

    MkDocs ignores a README.md that sits next to an index.md, so those are
    left out (the converter renames Home.md to index.md at the root)."""
    files = sorted(
        docs_dir.rglob("*.md"),
        key=lambda p: (len(p.relative_to(docs_dir).parts), str(p)),
    )
    result = []
    for path in files:
        rel = path.relative_to(docs_dir).as_posix()
        if rel.startswith("."):
            continue
        if path.name.lower() == "readme.md" and (path.parent / "index.md").exists():
            continue
        result.append(rel)
    return result


def build_page_map(files: list) -> dict:
    """Map lower-cased page name -> docs-relative path.

    The GitHub Wiki namespace is flat, so a link resolves by page name
    wherever the file sits. Root-level files win over sub-folder copies."""
    pages = {}
    for rel in files:
        pages.setdefault(Path(rel).stem.lower(), rel)
    # convert-wiki-for-mkdocs.py renames Home.md to index.md
    if "index" in pages:
        pages.setdefault("home", pages["index"])
    return pages


def resolve(target: str, pages: dict):
    """Return the docs path for a link target, or None if not a wiki page."""
    target = target.strip().strip("<>").strip()
    if not target:
        return None
    target = re.sub(r"^https?://github\.com/hmislk/hmis/wiki/?", "", target)
    if re.match(r"^[a-z]+:", target, re.I):
        return None
    target = unquote(target.split("#", 1)[0]).strip().rstrip("/")
    if target.lower().endswith(".md"):
        target = target[:-3]
    name = target.rsplit("/", 1)[-1].replace(" ", "-")
    if not name:
        return None
    return pages.get(name.lower())


# ---------------------------------------------------------------------------
# Article index parsing
# ---------------------------------------------------------------------------

def find_links(line: str):
    """Yield (label, target) for each Markdown link in a line.

    Targets are scanned with balanced parentheses because wiki page names
    such as ``Health-Level-Seven(HL7)`` contain them."""
    i = 0
    while True:
        start = line.find("[", i)
        if start < 0:
            return
        mid = line.find("](", start)
        if mid < 0:
            return
        label = line[start + 1 : mid]
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
            return
        yield label.strip(), line[mid + 2 : end]
        i = end + 1


def parse_article_index(text: str, pages: dict):
    """Return (groups, broken).

    groups: list of (heading, items) where items holds (label, path) pages
    and (sub_heading, [(label, path), ...]) sub-sections.
    broken: list of (heading, label, target) for links to missing pages."""
    groups = []
    broken = []
    group = None
    section = None

    for line in text.splitlines():
        h2 = re.match(r"^##\s+(.+?)\s*$", line)
        h3 = re.match(r"^###\s+(.+?)\s*$", line)
        if h2:
            group = (h2.group(1), [])
            groups.append(group)
            section = None
            continue
        if h3 and group is not None:
            section = (h3.group(1), [])
            group[1].append(section)
            continue
        if group is None or not re.match(r"^\s*[-*+]\s", line):
            continue
        for label, target in find_links(line):
            path = resolve(target, pages)
            if path is None:
                if not re.match(r"^[a-z]+:", target.strip("<> "), re.I):
                    broken.append((group[0], label, target))
                continue
            (section or group)[1].append((label, path))

    # Drop empty sub-sections and groups
    cleaned = []
    for heading, items in groups:
        items = [
            it for it in items if not isinstance(it[1], list) or it[1]
        ]
        if items:
            cleaned.append((heading, items))
    return cleaned, broken


# ---------------------------------------------------------------------------
# Navigation assembly
# ---------------------------------------------------------------------------

def fixed_section(entries, pages: dict):
    items = []
    for label, name in entries:
        path = pages.get(name.lower())
        if path:
            items.append((label, path))
        else:
            print(f"WARNING: fixed menu page not found: {name}")
    return items


def collect_paths(items, into: set) -> None:
    for label, value in items:
        if isinstance(value, list):
            collect_paths(value, into)
        else:
            into.add(value)


def is_redirect_stub(path: Path) -> bool:
    """True for a short "This page has moved" page left behind after a rename.

    The page still builds, so old links keep working, but it stays out of
    the menu."""
    text = path.read_text(encoding="utf-8", errors="ignore")
    return len(text) < 600 and "this page has moved" in text.lower()


def page_title(path: str) -> str:
    return Path(path).stem.replace("-", " ").replace("_", " ").strip()


def group_key(path: str) -> str:
    parts = Path(path).parts
    if len(parts) > 1:
        return parts[0]
    return re.split(r"[-_‐ ]", Path(path).stem, maxsplit=1)[0]


def more_articles(remaining) -> list:
    """Group pages not otherwise in the menu by folder / filename prefix."""
    buckets = defaultdict(list)
    spellings = defaultdict(Counter)
    for path in remaining:
        key = group_key(path)
        buckets[key.lower()].append(path)
        spellings[key.lower()][key] += 1

    groups = []
    other = []
    for key, paths in buckets.items():
        if len(paths) >= MIN_GROUP_SIZE:
            # Prefer a capitalised spelling: "Pharmacy" over "pharmacy"
            label = max(spellings[key].items(),
                        key=lambda s: (s[0][:1].isupper(), s[1]))[0]
            groups.append((label, paths))
        else:
            other.extend(paths)

    def entries(paths):
        return sorted(
            ((page_title(p), p) for p in paths), key=lambda e: e[0].lower()
        )

    items = [(label, entries(paths)) for label, paths in
             sorted(groups, key=lambda g: g[0].lower())]
    if other:
        items.append(("Other", entries(other)))
    return items


def build_nav(docs_dir: Path):
    files = list_doc_files(docs_dir)
    pages = build_page_map(files)
    index_path = pages.get(ARTICLE_INDEX.lower())
    if index_path is None:
        raise SystemExit(f"{ARTICLE_INDEX}.md not found in {docs_dir}")

    text = (docs_dir / index_path).read_text(encoding="utf-8", errors="ignore")
    groups, broken = parse_article_index(text, pages)

    user_manual = fixed_section(
        [("Overview", "User-Manual"), ("All Articles (Index)", ARTICLE_INDEX)],
        pages,
    )
    developer = fixed_section(DEVELOPER_PAGES, pages)
    for heading, items in groups:
        if heading.lower() == DEVELOPER_GROUP:
            developer.extend(items)
        else:
            user_manual.append((heading, items))

    nav = [("Home", "index.md")]
    nav.append(("User Manual", user_manual))
    for label, entries in (
        ("Videos", VIDEO_PAGES),
        ("Knowledge Base", KNOWLEDGE_PAGES),
    ):
        section = fixed_section(entries, pages)
        if section:
            nav.append((label, section))
    if developer:
        nav.append(("Developer Guide", developer))

    reached = set()
    collect_paths(nav, reached)
    about = fixed_section(ABOUT_PAGES, pages)
    collect_paths(about, reached)

    indexed = set()
    collect_paths([g for g in groups], indexed)

    remaining = sorted(
        path for path in files
        if path not in reached
        and Path(path).stem.lower() not in EXCLUDED_PAGES
        and not is_redirect_stub(docs_dir / path)
    )
    if remaining:
        nav.append(("More Articles", more_articles(remaining)))
    if about:
        nav.append(("About", about))

    not_indexed = sorted(
        path for path in files
        if path not in indexed
        and Path(path).stem.lower() not in EXCLUDED_PAGES
        and not is_redirect_stub(docs_dir / path)
    )
    return nav, broken, not_indexed, len(files)


# ---------------------------------------------------------------------------
# Output
# ---------------------------------------------------------------------------

def q(value: str) -> str:
    """Quote as a YAML double-quoted scalar (a JSON string is valid YAML)."""
    return json.dumps(value, ensure_ascii=False)


def nav_yaml(items, indent: int = 2) -> list:
    lines = []
    pad = " " * indent
    for label, value in items:
        if isinstance(value, list):
            lines.append(f"{pad}- {q(label)}:")
            lines.extend(nav_yaml(value, indent + 4))
        else:
            lines.append(f"{pad}- {q(label)}: {q(value)}")
    return lines


def write_report(broken, not_indexed, total, nav_pages) -> None:
    lines = [
        "## Wiki documentation menu",
        "",
        f"- Wiki pages: **{total}**",
        f"- Pages in the menu: **{nav_pages}**",
        f"- Broken links in `{ARTICLE_INDEX}`: **{len(broken)}**",
        f"- Pages not listed in `{ARTICLE_INDEX}` (shown under More Articles): "
        f"**{len(not_indexed)}**",
        "",
    ]
    if broken:
        lines += ["### Broken article-index links", "",
                  "| Section | Label | Target |", "|---|---|---|"]
        lines += [f"| {h} | {l} | `{t}` |" for h, l, t in broken]
        lines.append("")
    if not_indexed:
        lines += ["<details><summary>Pages not listed in the article index"
                  "</summary>", ""]
        lines += [f"- `{p}`" for p in not_indexed]
        lines += ["", "</details>", ""]

    report = "\n".join(lines) + "\n"
    summary = os.environ.get("GITHUB_STEP_SUMMARY")
    if summary:
        with open(summary, "a", encoding="utf-8") as fh:
            fh.write(report)
    print(report)


def main() -> int:
    if len(sys.argv) != 4:
        print("Usage: generate-wiki-nav.py <docs-dir> <base-config> <out-config>")
        return 1

    docs_dir = Path(sys.argv[1]).resolve()
    base_config = Path(sys.argv[2]).resolve()
    out_config = Path(sys.argv[3]).resolve()

    nav, broken, not_indexed, total = build_nav(docs_dir)

    inherit = os.path.relpath(base_config, out_config.parent)
    lines = [
        "# Generated by tools/generate-wiki-nav.py — do not edit.",
        f"INHERIT: {q(inherit)}",
        "nav:",
    ]
    lines += nav_yaml(nav)
    out_config.write_text("\n".join(lines) + "\n", encoding="utf-8")

    in_nav = set()
    collect_paths(nav, in_nav)
    write_report(broken, not_indexed, total, len(in_nav))
    print(f"Navigation written: {out_config}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
