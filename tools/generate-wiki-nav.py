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

Index format (Wiki-Article-Index.md):
- the first ``#`` heading is the page title; later ``#`` headings are tabs
  (Get Started, User Guide, Administration, ...), ``##`` modules and ``###``
  sub-sections; links in list items are pages
- an italic line right under a heading is its description, and an
  ``<!-- icon: material/pill -->`` comment its icon (both invisible or
  harmless on GitHub); they feed the landing-page cards
- ``##`` groups before any tab heading (the old two-level format) go under
  "User Guide"

Menu layout:
- Home: a generated landing page (docs/index.md) with one card per tab and
  one per User Guide module
- one tab per index ``#`` heading, in index order
- Videos, About: fixed pages, unless the index already lists them
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
DEFAULT_TAB = "User Guide"
DEFAULT_ICON = "material/book-open-variant"
MIN_GROUP_SIZE = 5
LANDING_TITLE = "CareCode HMIS Documentation"
LANDING_INTRO = (
    "CareCode HMIS is an open-source hospital management information system. "
    "Pick where you want to start, or use the search box above to find a "
    "page by name."
)
EXCLUDED_PAGES = {"index", "_footer", "_sidebar"}

VIDEO_PAGES = [("Video Library", "Video-Library")]
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

    MkDocs ignores a README.md (case-sensitive) that sits next to an
    index.md, so those are left out. The converter renames README.md files to
    Readme.md, so this only matters for a docs folder it did not produce."""
    files = sorted(
        docs_dir.rglob("*.md"),
        key=lambda p: (len(p.relative_to(docs_dir).parts), str(p)),
    )
    result = []
    for path in files:
        rel = path.relative_to(docs_dir).as_posix()
        if rel.startswith("."):
            continue
        if path.name == "README.md" and (path.parent / "index.md").exists():
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


class Node:
    """A tab (``#``), module (``##``) or sub-section (``###``) of the index.

    items holds (label, path) pages and child Nodes, in index order."""

    def __init__(self, title: str):
        self.title = title
        self.description = ""
        self.icon = ""
        self.items = []

    def first_page(self):
        for item in self.items:
            if isinstance(item, Node):
                page = item.first_page()
                if page:
                    return page
            else:
                return item[1]
        return None

    def to_nav(self):
        out = []
        for item in self.items:
            if isinstance(item, Node):
                children = item.to_nav()
                if children:
                    out.append((item.title, children))
            else:
                out.append(item)
        return out


HEADING = re.compile(r"^(#{1,3})\s+(.+?)\s*#*\s*$")
ICON = re.compile(r"^<!--\s*icon:\s*(\S+)\s*-->$")
DESCRIPTION = re.compile(r"^(?:\*([^*].*?)\*|_([^_].*?)_)$")


def parse_article_index(text: str, pages: dict):
    """Return (tabs, broken).

    The first ``#`` heading is the page title. Later ``#`` headings are tabs,
    ``##`` modules, ``###`` sub-sections; list-item links are pages. An italic
    line right under a heading is its description and an
    ``<!-- icon: material/name -->`` comment its icon. ``##`` groups before
    any tab heading (the old two-level format) go under "User Guide".

    broken: list of (heading, label, target) for links to missing pages."""
    tabs = []
    broken = []
    seen_title = False
    tab = module = section = None
    current = None  # the heading that may still take a description or icon

    for line in text.splitlines():
        stripped = line.strip()
        h = HEADING.match(line)
        if h:
            level, title = len(h.group(1)), h.group(2)
            if level == 1:
                if not seen_title:
                    seen_title = True
                    current = None
                    continue
                tab = Node(title)
                tabs.append(tab)
                module = section = None
                current = tab
            elif level == 2:
                if tab is None:
                    tab = Node(DEFAULT_TAB)
                    tabs.append(tab)
                module = Node(title)
                tab.items.append(module)
                section = None
                current = module
            else:
                if module is None:
                    continue
                section = Node(title)
                module.items.append(section)
                current = section
            continue

        if not stripped:
            continue
        icon = ICON.match(stripped)
        if icon and current is not None:
            current.icon = icon.group(1)
            continue
        desc = DESCRIPTION.match(stripped)
        if desc and current is not None and not current.description \
                and not current.items:
            current.description = desc.group(1) or desc.group(2)
            continue
        current = None

        target_node = section or module or tab
        if target_node is None or not re.match(r"^\s*[-*+]\s", line):
            continue
        for label, target in find_links(line):
            path = resolve(target, pages)
            if path is None:
                if not re.match(r"^[a-z]+:", target.strip("<> "), re.I):
                    broken.append((target_node.title, label, target))
                continue
            target_node.items.append((label, path))

    return [tb for tb in tabs if tb.first_page()], broken


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
    tabs, broken = parse_article_index(text, pages)

    nav = [("Home", "index.md")]
    for tab in tabs:
        nav.append((tab.title, tab.to_nav()))
    if nav[1:]:
        nav[1][1].append(("All Articles (Index)", index_path))

    indexed = set()
    collect_paths(nav[1:], indexed)

    videos = [e for e in fixed_section(VIDEO_PAGES, pages) if e[1] not in indexed]
    if videos:
        nav.append(("Videos", videos))

    reached = set()
    collect_paths(nav, reached)
    about = [e for e in fixed_section(ABOUT_PAGES, pages) if e[1] not in reached]
    collect_paths(about, reached)

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
    return nav, tabs, broken, not_indexed, len(files)


# ---------------------------------------------------------------------------
# Landing page
# ---------------------------------------------------------------------------

def shortcode(icon: str) -> str:
    """``material/pill`` -> ``:material-pill:`` (pymdownx.emoji)."""
    return ":" + (icon or DEFAULT_ICON).replace("/", "-") + ":"


def md_link(path: str) -> str:
    target = path.replace(" ", "%20")
    return f"<{target}>" if "(" in target or ")" in target else target


def cards(nodes) -> list:
    lines = ['<div class="grid cards" markdown>', ""]
    for node in nodes:
        page = node.first_page()
        lines.append(f"-   {shortcode(node.icon)}{{ .lg .middle }} "
                     f"**[{node.title}]({md_link(page)})**")
        lines.append("")
        lines.append("    ---")
        lines.append("")
        if node.description:
            lines.append(f"    {node.description}")
            lines.append("")
    lines += ["</div>", ""]
    return lines


def write_landing_page(docs_dir: Path, tabs) -> None:
    """Replace docs/index.md (the converted wiki Home.md) with a landing page:
    one card per tab, then one card per module of the User Guide tab."""
    lines = [
        "---",
        "hide:",
        "  - navigation",
        "  - toc",
        "---",
        "",
        f"# {LANDING_TITLE}",
        "",
        LANDING_INTRO,
        "",
        "## Start here",
        "",
    ]
    lines += cards(tabs)
    for tab in tabs:
        modules = [m for m in tab.items if isinstance(m, Node) and m.first_page()]
        if tab.title.lower() == DEFAULT_TAB.lower() and modules:
            lines += [f"## {tab.title}", ""]
            if tab.description:
                lines += [tab.description, ""]
            lines += cards(modules)
    (docs_dir / "index.md").write_text("\n".join(lines), encoding="utf-8")


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

    nav, tabs, broken, not_indexed, total = build_nav(docs_dir)
    write_landing_page(docs_dir, tabs)

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
