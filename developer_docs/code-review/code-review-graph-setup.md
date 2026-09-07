# code-review-graph Setup Guide

## Overview

[code-review-graph](https://github.com/tirth8205/code-review-graph) is a Tree-sitter-based tool that builds a structural map of the codebase and gives Claude Code precise context, reducing token consumption by up to 6.8x while improving review quality.

It integrates with Claude Code as an **MCP server**, adding optional tools alongside Claude's existing capabilities. It does **not** replace or modify any built-in Claude Code functionality.

## Prerequisites

- Python 3.10+
- [uv](https://docs.astral.sh/uv/) (Python package manager)

## Installation

### Step 1: Install the package

```bash
pip install code-review-graph
```

### Step 2: Register as MCP server

```bash
code-review-graph install
```

This auto-registers the tool as an MCP server with Claude Code.

### Step 3: Restart Claude Code

Restart Claude Code so it picks up the new MCP server.

## Usage

### Build the code graph (first time)

In Claude Code, run:

```
/code-review-graph:build-graph
```

The initial build creates a structural map of the codebase. For the HMIS project (large Java codebase), this may take a minute or more. After the initial build, updates are **incremental** and fast.

### Review changes since last commit

```
/code-review-graph:review-delta
```

### Full PR review with blast-radius analysis

```
/code-review-graph:review-pr
```

### Query the graph directly

Once built, Claude Code automatically gains access to these tools:

| Tool | Purpose |
|------|---------|
| `build_or_update_graph_tool` | Build or incrementally update the graph |
| `get_impact_radius_tool` | Blast radius of changed files |
| `get_review_context_tool` | Token-optimized review context |
| `query_graph_tool` | Callers, callees, tests, imports, inheritance |
| `semantic_search_nodes_tool` | Search code entities by name |
| `find_large_functions_tool` | Find oversized functions/classes |

Claude will use these tools automatically when relevant. You don't need to invoke them manually.

## Configuration

The project includes a `.code-review-graphignore` file at the repository root that excludes build artifacts and other non-essential files from the graph.

The graph data is stored locally in `.code-review-graph/` inside the project directory as a SQLite file. No cloud services or external dependencies are required.

### Optional: Semantic search with embeddings

```bash
pip install code-review-graph[embeddings]
```

This adds vector embedding support for semantic code search via sentence-transformers.

## Rollback / Uninstall

If you want to completely remove code-review-graph, follow these steps in order:

### Step 1: Unregister the MCP server

```bash
code-review-graph uninstall
```

### Step 2: Uninstall the Python package

```bash
pip uninstall code-review-graph
```

### Step 3: Remove local graph data

```bash
rm -rf .code-review-graph/
```

### Step 4: Restart Claude Code

Restart Claude Code to complete the removal. After this, Claude Code returns to its default state with no traces of the tool remaining.

## Important Notes

- **Non-invasive**: This tool is purely additive. It does not modify Claude Code's built-in tools or capabilities.
- **Fully reversible**: Uninstalling removes all traces. No configuration files or settings are permanently altered.
- **Local only**: All data stays on your machine. No cloud services involved.
- **Java supported**: Java is one of 14 supported languages with full Tree-sitter grammar support.
