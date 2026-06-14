#!/usr/bin/env python3

from pathlib import Path
from urllib.parse import quote
import re
import shutil
import sys


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


def page_to_markdown_link(page_name: str) -> str:
    page_name = page_name.strip()

    if not page_name:
        return ""

    anchor = ""

    if "#" in page_name:
        page_name, anchor = page_name.split("#", 1)
        anchor = "#" + quote(anchor.strip().lower().replace(" ", "-"))

    page_name = page_name.strip()

    if page_name.startswith("http://") or page_name.startswith("https://"):
        return page_name

    if page_name.startswith("/"):
        page_name = page_name[1:]

    if not page_name.lower().endswith((".md", ".markdown")):
        page_name = page_name.replace(" ", "-")
        page_name = f"{page_name}.md"

    return quote(page_name, safe="/.#-_%") + anchor


def convert_gollum_links(text: str) -> str:
    """
    Converts common GitHub Wiki / Gollum links to Markdown links.

    Supported examples:
    [[Page Name]]
    [[Display Text|Page Name]]
    [[Page Name#Section]]
    """

    pattern = re.compile(r"\[\[([^\[\]]+?)\]\]")

    def replace(match: re.Match) -> str:
        content = match.group(1).strip()

        if "|" in content:
            label, target = content.split("|", 1)
            label = label.strip()
            target = target.strip()
        else:
            label = content
            target = content

        link = page_to_markdown_link(target)

        if not link:
            return match.group(0)

        return f"[{label}]({link})"

    return pattern.sub(replace, text)


def normalise_home_page(docs_dir: Path) -> None:
    home = docs_dir / "Home.md"
    index = docs_dir / "index.md"

    if home.exists() and not index.exists():
        home.rename(index)


def convert_markdown_files(docs_dir: Path) -> None:
    for path in docs_dir.rglob("*"):
        if not path.is_file():
            continue

        if path.suffix.lower() not in [".md", ".markdown"]:
            continue

        text = path.read_text(encoding="utf-8", errors="ignore")
        text = convert_gollum_links(text)
        path.write_text(text, encoding="utf-8")


def create_index_if_missing(docs_dir: Path) -> None:
    index = docs_dir / "index.md"

    if index.exists():
        return

    pages = sorted(
        path.relative_to(docs_dir)
        for path in docs_dir.rglob("*")
        if path.is_file()
        and path.suffix.lower() in (".md", ".markdown")
        and path.name.lower() != "index.md"
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


def main() -> int:
    if len(sys.argv) != 3:
        print("Usage: convert-wiki-for-mkdocs.py <wiki-source-dir> <docs-dir>")
        return 1

    source_dir = Path(sys.argv[1]).resolve()
    docs_dir = Path(sys.argv[2]).resolve()

    if not source_dir.is_dir():
        print(f"Wiki source directory not found: {source_dir}")
        return 1

    copy_wiki(source_dir, docs_dir)
    normalise_home_page(docs_dir)
    convert_markdown_files(docs_dir)
    create_index_if_missing(docs_dir)

    print(f"Wiki converted successfully: {source_dir} -> {docs_dir}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
