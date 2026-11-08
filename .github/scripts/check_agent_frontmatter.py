#!/usr/bin/env python3
"""Lint the frontmatter of Claude Code subagents in .claude/agents/.

Claude Code silently ignores unknown or misspelt frontmatter fields, so a typo
just turns a setting off. This script fails on anything that would be ignored
or that breaks the rules in developer_docs/process/agent-authoring.md.

Usage: check_agent_frontmatter.py [repo_root]   (default: current directory)
Exit code 1 if any agent file has an error.
"""
import re
import sys
from pathlib import Path

import yaml  # PyYAML; the CI job installs it

KNOWN_FIELDS = {
    "name", "description", "tools", "disallowedTools", "model", "effort",
    "color", "skills", "hooks", "maxTurns",
}
# Real Claude Code fields that this repo does not allow (see agent-authoring.md).
BANNED_FIELDS = {
    "memory": "it silently adds Edit/Write to the agent",
    "isolation": "worktrees are banned in this repo",
    "permissionMode": "agents inherit the session's permission mode",
    "mcpServers": "agents get MCP tools only by naming them in 'tools'",
}
REQUIRED_FIELDS = ("name", "description", "tools", "model")
COLORS = {"red", "blue", "green", "yellow", "purple", "orange", "pink", "cyan"}
MODELS = {"haiku", "sonnet", "opus", "fable", "inherit"}
EFFORTS = {"low", "medium", "high", "xhigh", "max"}
MAX_DESCRIPTION = 300
WRITE_TOOLS = {"Edit", "Write", "NotebookEdit"}
READ_ONLY_SUFFIXES = ("-reviewer", "-guard", "-verifier")


def parse_frontmatter(text):
    """Return (dict, error) for the YAML frontmatter between the '---' lines."""
    if not text.startswith("---\n"):
        return None, "file does not start with a '---' frontmatter line"
    end = text.find("\n---\n", 4)
    if end < 0 and text.endswith("\n---"):
        end = len(text) - 4
    if end < 0:
        return None, "frontmatter has no closing '---' line"
    try:
        data = yaml.safe_load(text[4:end])
    except yaml.YAMLError as e:
        return None, "frontmatter is not valid YAML (quote a value that contains ': '): " + " ".join(str(e).split())
    if not isinstance(data, dict):
        return None, "frontmatter is not a 'key: value' mapping"
    return data, None


def as_list(value):
    if isinstance(value, list):
        return [str(v).strip() for v in value if v]
    if isinstance(value, str):
        return [v.strip() for v in value.split(",") if v.strip()]
    return []


def skill_status(root, name):
    """Return None if the skill is fine to preload, else the reason."""
    skill = root / ".claude" / "skills" / name / "SKILL.md"
    if not skill.is_file():
        return "does not exist in .claude/skills/"
    fm, _ = parse_frontmatter(skill.read_text(encoding="utf-8").replace("\r\n", "\n"))
    if fm and str(fm.get("disable-model-invocation", "")).lower() == "true":
        return "has disable-model-invocation: true and cannot be preloaded (Read its SKILL.md instead)"
    return None


def check(path, root):
    errors = []
    text = path.read_text(encoding="utf-8").replace("\r\n", "\n")
    fm, err = parse_frontmatter(text)
    if err:
        return [err], None
    for key in sorted(set(fm) & set(BANNED_FIELDS)):
        errors.append(f"banned by policy: '{key}' ({BANNED_FIELDS[key]})")
    for key in sorted(set(fm) - KNOWN_FIELDS - set(BANNED_FIELDS)):
        errors.append(f"unknown field '{key}' (allowed: {', '.join(sorted(KNOWN_FIELDS))})")
    for key in REQUIRED_FIELDS:
        if not fm.get(key):
            errors.append(f"missing required field '{key}'")
    desc = fm.get("description", "")
    if isinstance(desc, str):
        if len(desc) > MAX_DESCRIPTION:
            errors.append(f"description is {len(desc)} chars; keep it to {MAX_DESCRIPTION} (1-2 sentences)")
        if "<example>" in desc.lower():
            errors.append("description contains an <example> block; move examples into the body or drop them")
    else:
        errors.append("description must be a single string")
    color = fm.get("color")
    if color and color not in COLORS:
        errors.append(f"color '{color}' is not valid (use one of {', '.join(sorted(COLORS))})")
    model = fm.get("model")
    if model and not (model in MODELS or re.fullmatch(r"claude-[a-z0-9.-]+", str(model))):
        errors.append(f"model '{model}' is not valid (use {', '.join(sorted(MODELS))} or a full claude-* id)")
    effort = fm.get("effort")
    if effort and effort not in EFFORTS:
        errors.append(f"effort '{effort}' is not valid (use one of {', '.join(sorted(EFFORTS))})")
    tools = as_list(fm.get("tools"))
    if any(t == "Agent" or t.startswith("Agent(") for t in tools):
        errors.append("tools includes 'Agent'; subagents must not spawn subagents")
    name = str(fm.get("name", ""))
    if name.endswith(READ_ONLY_SUFFIXES):
        bad = sorted(WRITE_TOOLS & set(tools))
        if bad:
            errors.append(f"read-only role '{name}' lists write tools: {', '.join(bad)}")
    for skill in as_list(fm.get("skills")):
        reason = skill_status(root, skill)
        if reason:
            errors.append(f"skills entry '{skill}' {reason}")
    if "maxTurns" in fm and not re.fullmatch(r"[1-9]\d*", str(fm["maxTurns"])):
        errors.append("maxTurns must be a positive integer")
    return errors, name


def main():
    root = Path(sys.argv[1] if len(sys.argv) > 1 else ".").resolve()
    agents = sorted((root / ".claude" / "agents").rglob("*.md"))
    failed, names = 0, {}
    for path in agents:
        rel = path.relative_to(root)
        errors, name = check(path, root)
        if name:
            if name in names:
                errors.append(f"name '{name}' is also used by {names[name]}")
            names[name] = rel
        if errors:
            failed += 1
            for e in errors:
                print(f"::error file={rel}::{e}")
        else:
            print(f"ok   {rel}")
    print(f"{len(agents)} agent file(s) checked, {failed} with errors.")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
