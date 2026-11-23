#!/usr/bin/env python3
"""
XHTML EL guard.

Fails when a changed .xhtml file has an EL expression opened inside another
one, for example

    disabled="#{not a.b or #{c.d}}"

EL does not nest #{...}; the inner "#{" is a parse error, so the page fails to
render for every user. The build does not render pages, so this is caught here.
Write one expression instead: disabled="#{not a.b or c.d}".

Usage:  check_xhtml_el.py <base-ref>        (checks .xhtml files added, changed or renamed since base)
        check_xhtml_el.py --files a.xhtml ...
"""
import subprocess
import sys


def changed_xhtml(base):
    # -z: NUL-separated, so non-ASCII paths are not quoted; --diff-filter=d: every
    # change except deletions, so renamed or copied pages are checked too.
    out = subprocess.run(
        ["git", "diff", "-z", "--name-only", "--diff-filter=d", base, "HEAD", "--", "*.xhtml"],
        capture_output=True, encoding="utf-8", errors="replace", check=True).stdout
    return [p for p in out.split("\0") if p.strip()]


def nested_el(text):
    """Yield (line, column) of each EL opener found inside an open EL expression."""
    depth = 0          # brace depth inside the current EL expression; 0 = outside EL
    quote = None       # quote character while inside a string literal in EL
    line, col = 1, 0
    i, n = 0, len(text)
    while i < n:
        ch = text[i]
        if ch == "\n":
            line, col = line + 1, 0
        else:
            col += 1
        if depth == 0:
            if ch in "#$" and i + 1 < n and text[i + 1] == "{":
                depth = 1
                i += 2
                col += 1
                continue
        else:
            if quote:
                if ch == "\\":
                    i += 2
                    col += 1
                    continue
                if ch == quote:
                    quote = None
            elif ch in "'\"":
                # A double quote that ends the XML attribute also ends a broken
                # expression; treat it as the end so one error is not reported twice.
                if ch == '"' and depth == 1 and text.count('"', i + 1, text.find("\n", i) if "\n" in text[i:] else n) == 0:
                    depth = 0
                else:
                    quote = ch
            elif ch in "#$" and i + 1 < n and text[i + 1] == "{":
                yield line, col
                depth += 1
                i += 2
                col += 1
                continue
            elif ch == "{":
                depth += 1
            elif ch == "}":
                depth -= 1
        i += 1


def main(argv):
    if len(argv) >= 2 and argv[1] == "--files":
        files = argv[2:]
    elif len(argv) == 2:
        files = changed_xhtml(argv[1])
    else:
        print(__doc__)
        return 2
    errors = 0
    for path in files:
        try:
            with open(path, encoding="utf-8", errors="replace") as f:
                text = f.read()
        except FileNotFoundError:
            continue
        for line, col in nested_el(text):
            errors += 1
            print(f"::error file={path},line={line},col={col}::EL expression opened inside another one "
                  f"(#{{...#{{...}}...}}). EL does not nest; the page will not render. "
                  f"Write a single expression.")
    print(f"Checked {len(files)} changed .xhtml file(s); {errors} nested EL expression(s).")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
